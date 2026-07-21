/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.edge.adapters.workload;

import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deterministic test-driven control channel for the {@link WorkloadProtocolAdapter} (backlog section 3). Enabled by
 * putting an absolute {@code "controlDir"} in the scenario JSON. Two files live under that dir:
 * <ul>
 *   <li><b>{@code <adapterId>.ctl}</b> — the TEST appends one command per line; the adapter polls for new lines.</li>
 *   <li><b>{@code <adapterId>.journal}</b> — the ADAPTER appends one line per received PA command / emitted callback,
 *       for the test to observe command order, batch membership, connection-cycle count, etc.</li>
 * </ul>
 * Commands (one per line):
 * <pre>
 *   hold &lt;poll|verify|connect|subscribe|write&gt;      // hold that op's result emissions until released
 *   release &lt;op&gt;                                     // emit ALL results held for that op (in captured order)
 *   releaseone &lt;op&gt;                                  // emit the OLDEST single held result for that op
 *   emit datapoint &lt;node&gt; &lt;value&gt;                   // inject a datapoint (value parsed long|double|string)
 *   emit connected | emit disconnected                 // inject a lifecycle callback
 *   emit nodeerror &lt;node&gt; &lt;spontaneous:true|false&gt;
 *   emit verify &lt;node&gt; &lt;success|permanent|transient&gt;
 *   emit writeresult &lt;node&gt; &lt;true|false&gt;
 * </pre>
 * Gate-aware emission: the adapter routes its normal result callbacks through {@link #emit(String, Runnable)} — when
 * that op is held, the emission is captured into a per-op FIFO instead of firing, and released later by the test. This
 * is pure test instrumentation; external REST/MQTT remains the primary oracle.
 */
public final class WorkloadControlChannel {

    private static final @NotNull Logger log = LoggerFactory.getLogger(WorkloadControlChannel.class);

    private final @NotNull String adapterId;
    private final @Nullable Path ctlFile;
    private final @Nullable Path journalFile;
    private final @NotNull ProtocolAdapterOutput output;
    private final long startMs;

    private final @NotNull Map<String, Boolean> held = new ConcurrentHashMap<>();      // key: "op" (all nodes) or "op:node"
    private final @NotNull Map<String, Deque<Runnable>> pending = new ConcurrentHashMap<>(); // key: "op:node"
    private final @NotNull Map<String, Long> blockMs = new ConcurrentHashMap<>();      // op -> ms to block the NEXT such command
    private long ctlOffset;              // bytes of the .ctl file already consumed (watcher thread only)
    private final @NotNull StringBuilder ctlCarry = new StringBuilder(); // partial trailing line across polls (watcher thread only)
    private @Nullable ScheduledExecutorService watcher;

    WorkloadControlChannel(final @NotNull String adapterId,
                           final @Nullable String controlDir,
                           final @NotNull ProtocolAdapterOutput output,
                           final long startMs) {
        this.adapterId = adapterId;
        this.output = output;
        this.startMs = startMs;
        if (controlDir == null || controlDir.isBlank()) {
            this.ctlFile = null;
            this.journalFile = null;
        } else {
            final Path dir = Path.of(controlDir).toAbsolutePath().normalize();
            if (!isControlDirAllowed(dir)) {
                // Defense-in-depth: this module is test-only and excluded from the GA distribution. Refuse to turn an
                // arbitrary configured path into a file-write primitive unless it is under a known-safe root — the JVM
                // temp dir, the process working directory, or the user home (which covers dev + CI workspaces where the
                // node directories live). Override with -Dhivemq.workload.control.allowAnyPath=true for unusual layouts.
                log.warn("WL_CTL disabled: controlDir '{}' is outside the allowed roots (java.io.tmpdir / user.dir / "
                        + "user.home); set -Dhivemq.workload.control.allowAnyPath=true to override.", dir);
                this.ctlFile = null;
                this.journalFile = null;
            } else {
                this.ctlFile = dir.resolve(adapterId + ".ctl");
                this.journalFile = dir.resolve(adapterId + ".journal");
                try {
                    Files.createDirectories(dir);
                    // Do NOT truncate: the journal must survive a PA restart (e.g. a reconfigure that recreates the
                    // adapter), otherwise cross-restart checks (retry-timer supersession, subscription-leak) lose
                    // history. The session separator lets a test scope its scan to the current life if needed.
                    Files.writeString(journalFile, "==WL_SESSION_START==\n", StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (final Exception e) {
                    log.warn("WL_CTL init failed id={}: {}", adapterId, e.toString());
                }
            }
        }
    }

    /**
     * Restricts where the control channel may create files. The Workload module is test-only and is not part of the GA
     * distribution; this guard means that even if it were ever accidentally packaged, a configured {@code controlDir}
     * cannot become an arbitrary-file-write primitive. Allowed: anything under {@code java.io.tmpdir}, the process
     * working directory ({@code user.dir}), or the user home ({@code user.home}) — which together cover dev and CI
     * workspaces where the test node directories live; override with {@code -Dhivemq.workload.control.allowAnyPath=true}.
     */
    private static boolean isControlDirAllowed(final @NotNull Path dir) {
        if (Boolean.getBoolean("hivemq.workload.control.allowAnyPath")) {
            return true;
        }
        for (final String root : new String[]{
                System.getProperty("java.io.tmpdir"), System.getProperty("user.dir"), System.getProperty("user.home")}) {
            if (root != null && !root.isBlank() && dir.startsWith(Path.of(root).toAbsolutePath().normalize())) {
                return true;
            }
        }
        return false;
    }

    boolean enabled() {
        return ctlFile != null;
    }

    void start() {
        if (!enabled()) {
            return;
        }
        watcher = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = new Thread(r, "workload-ctl-" + adapterId);
            t.setDaemon(true);
            return t;
        });
        watcher.scheduleAtFixedRate(this::poll, 100, 100, TimeUnit.MILLISECONDS);
    }

    void stop() {
        if (watcher != null) {
            watcher.shutdownNow();
            watcher = null;
        }
    }

    /** Append an observation line to the journal (no-op if control disabled). */
    void journal(final @NotNull String event) {
        if (journalFile == null) {
            return;
        }
        try {
            Files.writeString(journalFile, (System.currentTimeMillis() - startMs) + " " + event + "\n",
                    StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (final Exception ignored) {
        }
    }

    /** Op-level gate (no node) — used for {@code connect}. */
    void emit(final @NotNull String op, final @NotNull Runnable emission) {
        emit(op, "", emission);
    }

    /**
     * Gate-aware emission: held if the whole op is held ({@code hold poll}) OR this specific node is held
     * ({@code hold poll <node>}). Held emissions are captured per {@code op:node} FIFO and released later by the test.
     */
    void emit(final @NotNull String op, final @NotNull String node, final @NotNull Runnable emission) {
        final String key = op + ":" + node;
        if (enabled() && (Boolean.TRUE.equals(held.get(op)) || Boolean.TRUE.equals(held.get(key)))) {
            final Deque<Runnable> q = pending.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
            q.add(emission);
            journal("HELD key=" + key + " depth=" + q.size());
        } else {
            emission.run();
        }
    }

    /** If a {@code block <op> <ms>} was armed, consume it and return the ms to sleep inside that command (else 0). */
    long consumeBlock(final @NotNull String op) {
        if (!enabled()) {
            return 0;
        }
        final Long ms = blockMs.remove(op);
        if (ms != null && ms > 0) {
            journal("BLOCK op=" + op + " ms=" + ms);
            return ms;
        }
        return 0;
    }

    private void poll() {
        if (ctlFile == null) {
            return;
        }
        try {
            if (!Files.exists(ctlFile)) {
                return;
            }
            // Read only the bytes appended since the last poll — the .ctl file is append-only, so re-reading the whole
            // file every 100ms would grow O(n) over a long run. Carry any partial trailing line to the next poll.
            final long size = Files.size(ctlFile);
            if (size < ctlOffset) {         // truncated/rewritten — restart from the top
                ctlOffset = 0;
                ctlCarry.setLength(0);
            }
            if (size == ctlOffset) {        // nothing new appended
                return;
            }
            final ByteBuffer buf = ByteBuffer.allocate((int) (size - ctlOffset));
            try (final SeekableByteChannel ch = Files.newByteChannel(ctlFile, StandardOpenOption.READ)) {
                ch.position(ctlOffset);
                while (buf.hasRemaining() && ch.read(buf) > 0) {
                    // drain the appended range
                }
            }
            ctlOffset += buf.position();
            ctlCarry.append(new String(buf.array(), 0, buf.position(), StandardCharsets.UTF_8));
            int nl;
            while ((nl = ctlCarry.indexOf("\n")) >= 0) {
                final String line = ctlCarry.substring(0, nl).trim();
                ctlCarry.delete(0, nl + 1);
                if (!line.isEmpty() && !line.startsWith("#")) {
                    handle(line);
                }
            }
        } catch (final Exception e) {
            log.warn("WL_CTL poll error id={}: {}", adapterId, e.toString());
        }
    }

    private void handle(final @NotNull String line) {
        final String[] a = line.split("\\s+");
        journal("CTL " + line);
        try {
            switch (a[0]) {
                // hold <op> [node] — whole op, or one node
                case "hold" -> held.put(a.length > 2 ? a[1] + ":" + a[2] : a[1], Boolean.TRUE);
                case "release" -> {
                    if (a.length > 2) {                       // release <op> <node>
                        held.remove(a[1] + ":" + a[2]);
                        flush(a[1] + ":" + a[2]);
                    } else {                                  // release <op> — clear op gate + flush ALL its per-node FIFOs
                        held.remove(a[1]);
                        for (final String key : List.copyOf(pending.keySet())) {
                            if (key.equals(a[1] + ":") || key.startsWith(a[1] + ":")) {
                                held.remove(key);
                                flush(key);
                            }
                        }
                    }
                }
                case "releaseone" -> {
                    // Release the OLDEST single held emission for this op. With a node, target op:node exactly; without
                    // a node, node-scoped emissions live under op:<nodeId>, so scan every op:* queue (not just op:) and
                    // release one from the first non-empty one — otherwise "releaseone poll" would be a silent no-op.
                    if (a.length > 2) {
                        releaseOneFrom(a[1] + ":" + a[2]);
                    } else if (!releaseOneFrom(a[1] + ":")) {
                        for (final String key : List.copyOf(pending.keySet())) {
                            if (key.startsWith(a[1] + ":") && releaseOneFrom(key)) {
                                break;
                            }
                        }
                    }
                }
                case "block" -> blockMs.put(a[1], Long.parseLong(a[2])); // block <op> <ms>
                case "emit" -> emitInjected(a);
                default -> log.warn("WL_CTL unknown command id={}: {}", adapterId, line);
            }
        } catch (final Exception e) {
            log.warn("WL_CTL command failed id={} line='{}': {}", adapterId, line, e.toString());
        }
    }

    private void flush(final @NotNull String key) {
        final Deque<Runnable> q = pending.get(key);
        if (q != null) {
            for (Runnable r; (r = q.poll()) != null; ) {   // poll until empty; null-safe under concurrency
                r.run();
            }
        }
    }

    /** Poll+run one emission from the queue at {@code key} if present; returns true if one was released. */
    private boolean releaseOneFrom(final @NotNull String key) {
        final Deque<Runnable> q = pending.get(key);
        if (q != null) {
            final Runnable r = q.poll();   // null-safe under concurrency; do not gate on isEmpty()
            if (r != null) {
                r.run();
                return true;
            }
        }
        return false;
    }

    private void emitInjected(final @NotNull String[] a) {
        switch (a[1]) {
            case "datapoint" -> {
                final String node = a[2];
                journal("EMIT datapoint node=" + node + " value=" + a[3]);
                output.dataPoint(new WorkloadNode(node), new WorkloadDataPoint(node, parseValue(a[3])));
            }
            case "connected" -> {
                journal("EMIT connected");
                output.connected();
            }
            case "disconnected" -> {
                journal("EMIT disconnected");
                output.disconnected();
            }
            case "connectionerror" -> {
                // a connection-level error WHILE connected — the wrapper responds with disconnect() (reconnect path),
                // entering WAITING_FOR_DISCONNECTED_BEFORE_RECONNECT; used to exercise the disconnect watchdog.
                journal("EMIT connectionerror");
                output.error(com.hivemq.adapter.sdk.api.v2.model.ErrorScope.CONNECTION, "control: injected connection error");
            }
            case "nodeerror" -> {
                journal("EMIT nodeerror node=" + a[2] + " spontaneous=" + a[3]);
                output.nodeError(new WorkloadNode(a[2]), "control: injected nodeError", Boolean.parseBoolean(a[3]));
            }
            case "verify" -> {
                journal("EMIT verify node=" + a[2] + " outcome=" + a[3]);
                final VerifyOutcome outcome = switch (a[3]) {
                    case "permanent" -> new VerifyOutcome.PermanentFailure("control: injected permanent");
                    case "transient" -> new VerifyOutcome.TransientFailure("control: injected transient");
                    default -> new VerifyOutcome.Success();
                };
                output.verifyResult(new WorkloadNode(a[2]), outcome);
            }
            case "writeresult" -> {
                journal("EMIT writeresult node=" + a[2] + " ok=" + a[3]);
                output.writeResult(new WorkloadNode(a[2]), Boolean.parseBoolean(a[3]), null);
            }
            default -> log.warn("WL_CTL unknown emit id={}: {}", adapterId, String.join(" ", a));
        }
    }

    private static @NotNull Object parseValue(final @NotNull String s) {
        try {
            return Long.parseLong(s);
        } catch (final NumberFormatException ignored) {
        }
        try {
            return Double.parseDouble(s);
        } catch (final NumberFormatException ignored) {
        }
        return s;
    }
}
