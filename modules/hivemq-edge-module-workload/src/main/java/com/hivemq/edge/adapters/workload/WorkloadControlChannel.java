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
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
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

    // How often the watcher polls the .ctl command file. Kept short so test commands (hold/release/emit) take effect
    // near-instantly — the control channel must feel synchronous to a test even though it is file-driven.
    private static final long CTL_POLL_INTERVAL_MILLIS = 100L;

    // The ops the gate/block grammar accepts. A typo'd op ("hold pol") would otherwise arm a gate nothing ever
    // consults — journaled as a normal CTL line, silently useless. Validated in handle().
    private static final @NotNull Set<String> KNOWN_OPS = Set.of("poll", "verify", "connect", "subscribe", "write");

    private final @NotNull String adapterId;
    private final @Nullable Path ctlFile;
    private final @Nullable Path journalFile;
    private final @NotNull ProtocolAdapterOutput output;
    // Resolves a nodeId to the ORIGINAL Node instance the engine is tracking. The engine keys its node→tag-runtime map
    // by object identity (Node has no equals/hashCode), so an injected callback must reuse the exact instance the
    // adapter was handed — a fresh `new WorkloadNode(id)` would be a different object and the engine would silently
    // drop
    // the callback (findTagRuntime returns null). Falls back to a fresh node when the id is genuinely unknown, which is
    // exactly what the "unknown/ghost node" tests want (an untracked node whose callback must be ignored).
    private final @NotNull Function<String, Node> nodeResolver;
    private final long startMs;

    private final @NotNull Map<String, Boolean> held = new ConcurrentHashMap<>(); // key: "op" (all nodes) or "op:node"
    private final @NotNull Map<String, Deque<Runnable>> pending = new ConcurrentHashMap<>(); // key: "op:node"
    private final @NotNull Map<String, Long> blockMs =
            new ConcurrentHashMap<>(); // op -> ms to block the NEXT such command
    private long ctlOffset; // bytes of the .ctl file already consumed (watcher thread only)
    private final @NotNull StringBuilder ctlCarry =
            new StringBuilder(); // partial trailing line across polls (watcher thread only)
    private @Nullable Object
            ctlFileKey; // identity of the .ctl file the offset refers to (inode/creation time; watcher + ctor)
    // Serialises the gate mutations that span BOTH `held` and `pending`: emit's (check-held → enqueue) must be atomic
    // with respect to release's (clear-held → flush). Without it a release landing between emit's check and its enqueue
    // drops the emission into a queue that has already been flushed, so the callback is silently lost (test hangs).
    private final @NotNull Object gateLock = new Object();
    private @Nullable ScheduledExecutorService watcher;
    // Set by stop(): this session is over. journal()/emit() go quiet/pass-through so a late call from the old adapter
    // instance cannot append phantom lines after the NEXT session's separator or park emissions in a dead channel.
    private volatile boolean closed;

    WorkloadControlChannel(
            final @NotNull String adapterId,
            final @Nullable String controlDir,
            final @NotNull ProtocolAdapterOutput output,
            final @NotNull Function<String, Node> nodeResolver,
            final long startMs) {
        this.adapterId = adapterId;
        this.output = output;
        this.nodeResolver = nodeResolver;
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
                // node directories live). Override with -Dhivemq.workload.control.allowAnyPath=true for unusual
                // layouts.
                log.warn(
                        "WL_CTL disabled: controlDir '{}' is outside the allowed roots (java.io.tmpdir / user.dir / "
                                + "user.home); set -Dhivemq.workload.control.allowAnyPath=true to override.",
                        dir);
                this.ctlFile = null;
                this.journalFile = null;
            } else {
                this.ctlFile = dir.resolve(adapterId + ".ctl");
                this.journalFile = dir.resolve(adapterId + ".journal");
                try {
                    Files.createDirectories(dir);
                    // Start reading the command file from its CURRENT end, not byte 0. The .ctl file is append-only and
                    // survives a PA restart (reconfigure recreates the adapter), so a new session that started at 0
                    // would
                    // re-read and RE-EXECUTE every historical command against the freshly-started adapter. A new
                    // session
                    // must only act on commands appended AFTER it started.
                    this.ctlOffset = Files.exists(ctlFile) ? Files.size(ctlFile) : 0;
                    this.ctlFileKey = fileIdentity(ctlFile);
                    // Do NOT truncate: the journal must survive a PA restart (e.g. a reconfigure that recreates the
                    // adapter), otherwise cross-restart checks (retry-timer supersession, subscription-leak) lose
                    // history. The session separator lets a test scope its scan to the current life if needed; the
                    // startMs suffix makes each life's separator UNIQUE, so a test can await *this* session's start
                    // instead of counting byte-identical separators across an unbounded journal.
                    Files.writeString(
                            journalFile,
                            "==WL_SESSION_START== startMs=" + startMs + "\n",
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND);
                    if (ctlOffset > 0) {
                        // Handover trace: this session will never execute the first ctlOffset bytes of the .ctl (they
                        // belong to prior lives, or fell into the restart window). Without this line a command lost to
                        // the window vanishes with no diagnostic anywhere.
                        journal("CTL_SKIPPED bytes=" + ctlOffset);
                    }
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
        // Compare REAL paths, not lexical ones: on macOS /tmp is a symlink to /private/tmp while java.io.tmpdir is
        // /var/folders/…, so a lexical startsWith falsely REJECTS the most natural temp path ("/tmp/…") and the whole
        // control channel silently disables on every dev machine while passing on Linux CI. Resolving both sides
        // through toRealPath() (deepest existing ancestor for the not-yet-created dir) makes the guard symlink-honest
        // in both directions.
        final Path candidate = realPathOrSelf(dir);
        // "/tmp" is an explicit root: on macOS java.io.tmpdir is /var/folders/…/T/ and /tmp resolves to /private/tmp
        // — under NO other root — so the most natural temp path would be rejected on every dev Mac while passing on
        // Linux CI (where java.io.tmpdir IS /tmp, making the entry redundant but harmless).
        for (final String root : new String[] {
            System.getProperty("java.io.tmpdir"),
            System.getProperty("user.dir"),
            System.getProperty("user.home"),
            "/tmp"
        }) {
            if (root != null && !root.isBlank()) {
                final Path rootPath =
                        realPathOrSelf(Path.of(root).toAbsolutePath().normalize());
                if (candidate.startsWith(rootPath)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Resolve symlinks via the deepest EXISTING ancestor (the dir itself may not exist yet); lexical fallback. */
    private static @NotNull Path realPathOrSelf(final @NotNull Path p) {
        Path existing = p;
        Path tail = null;
        while (existing != null && !Files.exists(existing)) {
            tail = tail == null
                    ? existing.getFileName()
                    : existing.getFileName().resolve(tail);
            existing = existing.getParent();
        }
        if (existing == null) {
            return p;
        }
        try {
            final Path real = existing.toRealPath();
            return tail == null ? real : real.resolve(tail);
        } catch (final Exception e) {
            return p;
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
        watcher.scheduleAtFixedRate(
                this::poll, CTL_POLL_INTERVAL_MILLIS, CTL_POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    synchronized void stop() {
        if (watcher != null) {
            watcher.shutdownNow();
            // Wait briefly for an in-flight poll() to finish: the interrupt closes its channel read, but lines it
            // ALREADY parsed into ctlCarry would otherwise keep executing handle() concurrently with (or after) the
            // reap — injecting stale commands into a session that is being replaced, with the journal writes silently
            // swallowed (ClosedByInterruptException). Microseconds in practice.
            try {
                if (!watcher.awaitTermination(1, TimeUnit.SECONDS)) {
                    log.warn("WL_CTL_STRAGGLER id={}: watcher did not terminate within 1s of stop()", adapterId);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            watcher = null;
        }
        // Handover trace: armed gates and parked emissions do NOT survive into the next session — journal what this
        // reap discards, so a test that releases after a restart sees WHY nothing fired (instead of a "CTL release"
        // line that reads as executed against emissions that are silently gone). Snapshot under the gate lock, write
        // the journal lines OUTSIDE it (journal() is file I/O; holding the gate across it stalls the emitters).
        final List<String> discarded = new ArrayList<>();
        synchronized (gateLock) {
            for (final Map.Entry<String, Deque<Runnable>> e : pending.entrySet()) {
                if (!e.getValue().isEmpty()) {
                    discarded.add("DISCARDED key=" + e.getKey() + " count="
                            + e.getValue().size());
                }
            }
            for (final String key : held.keySet()) {
                discarded.add("DISCARDED gate=" + key);
            }
            pending.clear();
            held.clear();
        }
        discarded.forEach(this::journal);
        // This session is OVER: go quiet. A late journal("...") from the old adapter instance (e.g. a stop() re-issued
        // by the wrapper's error loop) must not append phantom lines after the NEXT session's separator.
        closed = true;
    }

    /** Append an observation line to the journal (no-op if control disabled or this session is over). */
    void journal(final @NotNull String event) {
        if (journalFile == null || closed) {
            return;
        }
        try {
            Files.writeString(
                    journalFile,
                    (System.currentTimeMillis() - startMs) + " " + event + "\n",
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
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
        // Decide-and-enqueue atomically vs release/flush (see gateLock). If held, park under the lock and return; if
        // not, run the emission OUTSIDE the lock (it calls back into the engine and must not run while holding the
        // gate). A closed channel never parks: its gates are gone, and an emission parked in a dead channel would be
        // lost silently — pass it through instead.
        synchronized (gateLock) {
            if (enabled() && !closed && (Boolean.TRUE.equals(held.get(op)) || Boolean.TRUE.equals(held.get(key)))) {
                final Deque<Runnable> q = pending.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
                q.add(emission);
                journal("HELD key=" + key + " depth=" + q.size());
                return;
            }
        }
        emission.run();
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
            // A REPLACED file (delete+recreate between polls) must restart from byte 0 even when the new content is
            // as large as the old offset — a size-only check silently resumes mid-file (skipped commands + a garbage
            // fragment that can even parse as a DIFFERENT valid command). File identity (inode/creation time) catches
            // the replace; the size check below still catches an in-place truncation.
            final Object identity = fileIdentity(ctlFile);
            if (identity != null) {
                // Only a change from a KNOWN identity is a replace — the file's first appearance (ctlFileKey null,
                // offset already 0) must not journal a spurious CTL_FILE_REPLACED a test could wrongly assert on.
                if (ctlFileKey != null && !identity.equals(ctlFileKey)) {
                    ctlOffset = 0;
                    ctlCarry.setLength(0);
                    journal("CTL_FILE_REPLACED");
                }
                ctlFileKey = identity;
            }
            if (size < ctlOffset) { // truncated in place — restart from the top
                ctlOffset = 0;
                ctlCarry.setLength(0);
            }
            if (size == ctlOffset) { // nothing new appended
                return;
            }
            final ByteBuffer buf = ByteBuffer.allocate((int) (size - ctlOffset));
            try (final SeekableByteChannel ch = Files.newByteChannel(ctlFile, StandardOpenOption.READ)) {
                ch.position(ctlOffset);
                while (buf.hasRemaining() && ch.read(buf) > 0) {
                    // drain the appended range
                }
            }
            // TOCTOU guard: the identity check above and the read just now are separate syscalls — a replace landing
            // between them means the bytes were read from the NEW file at the OLD offset (mid-file fragment; executing
            // it could both skip and DOUBLE-execute commands once the next poll resets to 0). If the identity changed
            // across the read, discard this read entirely and let the next poll take the replace path.
            if (!java.util.Objects.equals(fileIdentity(ctlFile), identity)) {
                return;
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

    /** File identity for replace-detection: POSIX inode when available, else creation time; null if unreadable. */
    private static @Nullable Object fileIdentity(final @NotNull Path file) {
        try {
            final BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            final Object key = attrs.fileKey();
            return key != null ? key : attrs.creationTime();
        } catch (final Exception e) {
            return null;
        }
    }

    private void handle(final @NotNull String line) {
        final String[] a = line.split("\\s+");
        journal("CTL " + line);
        try {
            // Reject a typo'd op up front — "hold pol" would otherwise arm a gate nothing consults, journaled as a
            // normal CTL line: the silent-no-op failure class this channel exists to eliminate.
            final boolean gateCommand =
                    "hold".equals(a[0]) || "release".equals(a[0]) || "releaseone".equals(a[0]) || "block".equals(a[0]);
            if (gateCommand && (a.length < 2 || !KNOWN_OPS.contains(a[1]))) {
                log.warn("WL_CTL unknown op id={} line='{}' (known: {})", adapterId, line, KNOWN_OPS);
                journal("CTL_REJECTED unknown-op line=" + line);
                return;
            }
            // "connect" is the one node-LESS op (its emissions key under "connect:", never "connect:<node>") — a
            // node-scoped connect gate would arm dead grammar that captures nothing, silently.
            if (gateCommand && a.length > 2 && "connect".equals(a[1])) {
                log.warn("WL_CTL connect gates are node-less id={} line='{}'", adapterId, line);
                journal("CTL_REJECTED node-scoped-connect line=" + line);
                return;
            }
            switch (a[0]) {
                // hold <op> [node] — whole op, or one node. Guarded by gateLock so it is atomic vs a concurrent emit().
                case "hold" -> {
                    synchronized (gateLock) {
                        held.put(a.length > 2 ? a[1] + ":" + a[2] : a[1], Boolean.TRUE);
                    }
                }
                case "release" -> {
                    // Guarded by gateLock: clear-held + flush must be atomic vs emit's check-held + enqueue, else an
                    // emission can land in a queue that was already flushed and never fire.
                    synchronized (gateLock) {
                        if (a.length > 2) { // release <op> <node>
                            held.remove(a[1] + ":" + a[2]);
                            flush(a[1] + ":" + a[2]);
                        } else { // release <op> — clear op gate + ALL its per-node gates/FIFOs.
                            // Iterate the UNION of held and pending keys: a per-node gate that captured nothing yet
                            // exists only in `held` (queues are created lazily at first capture) — clearing from
                            // pending.keySet() alone leaves such a gate armed after "release <op>", and the next
                            // capture under it strands forever.
                            held.remove(a[1]);
                            final Set<String> keys = new LinkedHashSet<>(pending.keySet());
                            keys.addAll(held.keySet());
                            for (final String key : keys) {
                                if (key.startsWith(a[1] + ":")) {
                                    held.remove(key);
                                    flush(key);
                                }
                            }
                        }
                    }
                }
                case "releaseone" -> {
                    // Release the OLDEST single held emission for this op. With a node, target op:node exactly; without
                    // a node, node-scoped emissions live under op:<nodeId>, so scan every op:* queue (not just op:) and
                    // release one from the first non-empty one — otherwise "releaseone poll" would be a silent no-op.
                    // Guarded by gateLock for the same reason as release.
                    synchronized (gateLock) {
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
            for (Runnable r; (r = q.poll()) != null; ) { // poll until empty; null-safe under concurrency
                r.run();
            }
        }
    }

    /** Poll+run one emission from the queue at {@code key} if present; returns true if one was released. */
    private boolean releaseOneFrom(final @NotNull String key) {
        final Deque<Runnable> q = pending.get(key);
        if (q != null) {
            final Runnable r = q.poll(); // null-safe under concurrency; do not gate on isEmpty()
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
                final Resolved r = resolveNode(node);
                journal("EMIT datapoint node=" + node + " value=" + a[3] + " resolved=" + r.resolved());
                output.dataPoint(r.node(), new WorkloadDataPoint(node, parseValue(a[3])));
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
                output.error(
                        com.hivemq.adapter.sdk.api.v2.model.ErrorScope.CONNECTION,
                        "control: injected connection error");
            }
            case "nodeerror" -> {
                final Resolved r = resolveNode(a[2]);
                journal("EMIT nodeerror node=" + a[2] + " spontaneous=" + a[3] + " resolved=" + r.resolved());
                output.nodeError(r.node(), "control: injected nodeError", Boolean.parseBoolean(a[3]));
            }
            case "verify" -> {
                final Resolved r = resolveNode(a[2]);
                journal("EMIT verify node=" + a[2] + " outcome=" + a[3] + " resolved=" + r.resolved());
                final VerifyOutcome outcome =
                        switch (a[3]) {
                            case "permanent" -> new VerifyOutcome.PermanentFailure("control: injected permanent");
                            case "transient" -> new VerifyOutcome.TransientFailure("control: injected transient");
                            default -> new VerifyOutcome.Success();
                        };
                output.verifyResult(r.node(), outcome);
            }
            case "writeresult" -> {
                final Resolved r = resolveNode(a[2]);
                journal("EMIT writeresult node=" + a[2] + " ok=" + a[3] + " resolved=" + r.resolved());
                output.writeResult(r.node(), Boolean.parseBoolean(a[3]), null);
            }
            default -> {
                log.warn("WL_CTL unknown emit id={}: {}", adapterId, String.join(" ", a));
                journal("CTL_REJECTED unknown-emit line=" + String.join(" ", a));
            }
        }
    }

    /** Resolution outcome: the Node to inject with, and whether it is the engine's tracked instance. */
    private record Resolved(@NotNull Node node, boolean resolved) {}

    /**
     * Resolve a nodeId to the ORIGINAL Node instance the adapter was handed (so the engine's identity-keyed lookup
     * finds its tag runtime and the injected callback actually lands). If the id is unknown to the adapter — e.g. a
     * deliberately-untracked "ghost"/"intruder" node — return a fresh node, which the engine correctly ignores. The
     * outcome is journaled ({@code resolved=true|false}) because the engine's drop is silent: without the flag, an
     * injection that missed (typo, write-only tag, post-reload window) is indistinguishable from one that landed.
     */
    private @NotNull Resolved resolveNode(final @NotNull String nodeId) {
        final Node resolved = nodeResolver.apply(nodeId);
        return resolved != null ? new Resolved(resolved, true) : new Resolved(new WorkloadNode(nodeId), false);
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
