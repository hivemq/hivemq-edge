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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private volatile int processedLines;
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
            final Path dir = Path.of(controlDir);
            this.ctlFile = dir.resolve(adapterId + ".ctl");
            this.journalFile = dir.resolve(adapterId + ".journal");
            try {
                Files.createDirectories(dir);
                // Do NOT truncate: the journal must survive a PA restart (e.g. a reconfigure that recreates the adapter),
                // otherwise cross-restart oracles (retry-timer supersession NV-D05, subscription leak NV-F03) lose history.
                // A session separator lets a test scope its scan to the current life if needed.
                Files.writeString(journalFile, "==WL_SESSION_START==\n", StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (final Exception e) {
                log.warn("WL_CTL init failed id={}: {}", adapterId, e.toString());
            }
        }
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
            pending.computeIfAbsent(key, k -> new ArrayDeque<>()).add(emission);
            journal("HELD key=" + key + " depth=" + pending.get(key).size());
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
            final List<String> lines = Files.readAllLines(ctlFile, StandardCharsets.UTF_8);
            for (int i = processedLines; i < lines.size(); i++) {
                final String line = lines.get(i).trim();
                processedLines = i + 1;
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
                    final String key = a.length > 2 ? a[1] + ":" + a[2] : a[1] + ":";
                    final Deque<Runnable> q = pending.get(key);
                    if (q != null && !q.isEmpty()) {
                        q.poll().run();
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
            while (!q.isEmpty()) {
                q.poll().run();
            }
        }
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
