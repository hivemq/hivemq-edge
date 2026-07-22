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

import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.ResolvedAttributes;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.AccessFlags;
import com.hivemq.adapter.sdk.api.v2.node.AccessTriState;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Workload Testing Adapter: a self-driving SDK-v2 device simulator. It generates a realistic data stream per node
 * (from the {@link WorkloadScenario}'s waveforms, sampled at each framework poll on the <b>real wall clock</b>) and
 * plays an autonomous fault timeline (connection drops/recoveries and per-tag faults) evaluated on every framework
 * call — so once configured at boot it needs no hot-reload and no external driver: it evolves its own behavior over
 * time, exactly as a real (mis)behaving device would.
 * <p>
 * Driven from the wrapper's single dispatch thread, so it holds no locks; timeline events are latched once by index.
 */
public final class WorkloadProtocolAdapter implements ProtocolAdapter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(WorkloadProtocolAdapter.class);

    /**
     * Unique build marker. Bump this token on every rebuild+inject so a test can prove the EXPECTED Workload jar was
     * loaded (defeats the {@code build/hivemq-environment/base} stale-jar cache trap that caused the retracted #8/#9/#10).
     * Emitted at {@link #start()} as {@code WL_BUILD} and journalled as {@code BUILD <token>}.
     */
    public static final @NotNull String BUILD = "wl-2026-07-22-v2b7";

    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterOutput output;
    private final @NotNull WorkloadScenario scenario;
    private final @NotNull Random noise;
    // shared across the dispatch thread and the subscription-push thread → thread-safe collections + volatile flags
    private final @NotNull Set<Integer> firedEvents = ConcurrentHashMap.newKeySet();
    private final @NotNull Set<String> faultedTags = ConcurrentHashMap.newKeySet();
    // tags whose device has SILENTLY frozen (no data, no error, connection stays up) — models real IoT sensor/firmware
    // freeze. Distinct from faultedTags (which raises a spontaneous nodeError). Used to attack the "no green while dead"
    // status-honesty invariant: does Edge ever surface a subscribed tag that stopped delivering without erroring?
    private final @NotNull Set<String> mutedTags = ConcurrentHashMap.newKeySet();
    private final @NotNull Map<String, Integer> verifyAttempts = new HashMap<>(); // dispatch-thread only (verifyBatch)
    private final @NotNull Map<String, Long> counters = new ConcurrentHashMap<>(); // per-node monotonic counter (wave "counter")
    private final @NotNull Map<String, Node> subscribed = new ConcurrentHashMap<>(); // the subscription SHADOW SET (nodeId → node)
    // Registry of every ORIGINAL Node instance the engine has handed this adapter (via poll/verify/subscribe/attribute
    // batches). The control channel resolves an injected "emit datapoint|nodeerror|verify|writeresult <nodeId>" through
    // this map so the callback carries the exact instance the engine tracks (its node→tag map is identity-keyed); a
    // fresh node would be silently dropped by findTagRuntime.
    private final @NotNull Map<String, Node> knownNodes = new ConcurrentHashMap<>();
    private @Nullable ScheduledExecutorService subExecutor; // the "device" push loop for subscribed nodes
    private @Nullable WorkloadControlChannel control; // deterministic test control (gates/callback-injection/journal)

    /** Route a result emission through the control channel's op-level gate (hold/release); run immediately if no control. */
    private void emit(final @NotNull String op, final @NotNull Runnable emission) {
        if (control != null) {
            control.emit(op, emission);
        } else {
            emission.run();
        }
    }

    /** Route through the per-node gate ({@code hold poll <node>}) as well as the op-level gate. */
    private void emit(final @NotNull String op, final @NotNull String nodeId, final @NotNull Runnable emission) {
        if (control != null) {
            control.emit(op, nodeId, emission);
        } else {
            emission.run();
        }
    }

    /** If a {@code block <op> <ms>} was armed, sleep inside the command — to test the control plane isn't blocked by the PA. */
    private void blockSleep(final @NotNull String op) {
        final long ms = control != null ? control.consumeBlock(op) : 0;
        if (ms > 0) {
            try {
                Thread.sleep(ms);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void journal(final @NotNull String event) {
        if (control != null) {
            control.journal(event);
        }
    }

    private long startMs;
    private int connectCount;                       // connection-cycle counter (journal observation hook)
    private volatile boolean downGoal;             // connection timeline goal: true = device down
    private volatile boolean announcedDisconnect;

    public WorkloadProtocolAdapter(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterOutput output,
            final @NotNull WorkloadScenario scenario) {
        this.adapterId = adapterId;
        this.output = output;
        this.scenario = scenario;
        this.noise = new Random(adapterId.hashCode()); // reproducible noise per adapter
    }

    @Override
    public @NotNull String adapterId() {
        return adapterId;
    }

    @Override
    public void start() {
        startMs = System.currentTimeMillis();
        // Reap a watcher left alive by a prior stop() (kept running so a test can inject callbacks AFTER STOPPED, NV-C07)
        // before opening a fresh session — otherwise a restart (reconfigure recreates the adapter) leaves the old watcher
        // polling the same .ctl file, and every subsequent command is executed by BOTH watchers (k+1 times after k restarts).
        if (control != null) {
            control.stop();
        }
        control = new WorkloadControlChannel(adapterId, scenario.controlDir(), output, knownNodes::get, startMs);
        control.start();
        log.warn("WL_BUILD id={} build={}", adapterId, BUILD); // prove which jar is loaded (stale-cache tripwire)
        journal("BUILD " + BUILD);
        journal("START");
        // a device push loop for subscribed nodes: pushes datapoints (and spontaneous errors) on its own timer,
        // calling the thread-safe output callback — the SDK's model for library-callback (push) adapters
        subExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            // daemon: if the adapter is not stopped cleanly (harness crash / abrupt shutdown) this thread must not
            // keep the JVM alive and hang CI teardown
            final Thread t = new Thread(r, "workload-sub-" + adapterId);
            t.setDaemon(true);
            return t;
        });
        subExecutor.scheduleAtFixedRate(this::pushSubscriptions, 500, 500, TimeUnit.MILLISECONDS);
        if (!"start-no-ack".equals(scenario.misbehave())) {
            output.started();
        } // else: suppress started() to leave the wrapper in WAITING_FOR_STARTED so its watchdog must fire
        if ("double-start".equals(scenario.misbehave())) {
            // CONTRACT VIOLATION: start() must NOT call back with both started() and error()
            output.error(ErrorScope.ADAPTER, "workload: MISBEHAVE — double callback (started + error) from start()");
        }
    }

    @Override
    public void stop() {
        journal("STOP");
        if (subExecutor != null) {
            subExecutor.shutdownNow();
            subExecutor = null;
        }
        subscribed.clear();
        // NOTE: the control channel is intentionally left RUNNING after stop() so a test can inject callbacks AFTER
        // STOPPED (NV-C07). Its watcher is a daemon thread and dies with the node.
        if (!"stop-no-ack".equals(scenario.misbehave())) {
            output.stopped();
        } // else: suppress stopped() to leave the wrapper in WAITING_FOR_STOPPED so its watchdog must fire
    }

    @Override
    public void connect() {
        journal("CONNECT cycle=" + (++connectCount));
        blockSleep("connect");
        applyTimeline();
        if (downGoal) {
            output.error(ErrorScope.CONNECTION, "workload: device down (timeline)");
            return;
        }
        if ("spurious-events".equals(scenario.misbehave())) {
            // CONTRACT VIOLATIONS: fire events the FSM never asked for / for a node it never handed us
            output.connected();
            output.connected();                                                              // duplicate connected()
            output.verifyResult(new WorkloadNode("ghost"), new VerifyOutcome.Success());     // verifyResult, no verifyBatch
            output.dataPoint(new WorkloadNode("ghost"), new WorkloadDataPoint("ghost", 42)); // dataPoint for an unknown node
            output.writeResult(new WorkloadNode("ghost"), true, null);                       // writeResult, no write pending
            return;
        }
        switch (scenario.connect()) {
            case "fail" -> {
                // marker for backoff/no-storm tests: one line per connect attempt, with a parseable timestamp
                log.warn("WL_CONNECT_ATTEMPT id={} t={}", adapterId, System.currentTimeMillis());
                output.error(ErrorScope.CONNECTION, "workload: connect failed (scenario)");
            }
            case "no-response" -> { /* hung connect — emit nothing, let the connect watchdog fire */ }
            case "drop" -> {
                output.connected();
                output.disconnected(); // connect, then immediately drop
            }
            default -> {
                announcedDisconnect = false;
                emit("connect", output::connected);
            }
        }
    }

    @Override
    public void disconnect() {
        journal("DISCONNECT");
        subscribed.clear(); // subscriptions are gone on disconnect; the wrapper re-issues addSubscriptionBatch on reconnect
        if (!"disconnect-no-ack".equals(scenario.misbehave())) {
            output.disconnected();
        } // else: suppress disconnected() to leave the wrapper in WAITING_FOR_DISCONNECTED so its watchdog must fire
    }

    @Override
    public void verifyBatch(final @NotNull List<Node> nodes) {
        nodes.forEach(n -> knownNodes.put(n.nodeId(), n)); // capture original instances for control-channel injection
        log.warn("WL_VERIFY_CALLED id={} nodes={} misbehave={}", adapterId, nodes.size(), scenario.misbehave());
        blockSleep("verify");
        if ("verify-partial".equals(scenario.misbehave())) {
            // CONTRACT VIOLATION (§11 verifyBatch MUST report every node): report ONLY the first node's result and
            // silently drop the rest. The dropped nodes should be caught by the §3.9 verification watchdog, not hang
            // forever. Logs which nodes were reported vs abandoned so the test can correlate.
            if (!nodes.isEmpty()) {
                final Node reported = nodes.get(0);
                output.verifyResult(reported, new VerifyOutcome.Success());
                log.warn("WL_VERIFY_PARTIAL id={} reported={} dropped={}", adapterId, reported.nodeId(), nodes.size() - 1);
            }
            return;
        }
        for (final Node node : nodes) {
            journal("VERIFY node=" + node.nodeId());
            final WorkloadScenario.Fault fault = scenario.faultFor(node.nodeId());
            final VerifyOutcome outcome = switch (fault.verify()) {
                case "permanent" -> new VerifyOutcome.PermanentFailure(reason(fault, "workload: permanent verify failure"));
                case "transient" -> new VerifyOutcome.TransientFailure(reason(fault, "workload: transient verify failure"));
                case "transient-then-success" -> {
                    final int attempt = verifyAttempts.merge(node.nodeId(), 1, Integer::sum);
                    yield attempt <= fault.transientCount()
                            ? new VerifyOutcome.TransientFailure(reason(fault, "workload: transient (attempt " + attempt + ")"))
                            : new VerifyOutcome.Success();
                }
                case "success-then-permanent" -> {
                    // passes the FIRST verification, then fails permanently — models a node that goes bad between
                    // connection cycles, so re-verify-on-reconnect must catch it
                    final int attempt = verifyAttempts.merge(node.nodeId(), 1, Integer::sum);
                    yield attempt <= 1
                            ? new VerifyOutcome.Success()
                            : new VerifyOutcome.PermanentFailure(reason(fault, "workload: node went bad on re-verify"));
                }
                case "no-response" -> null; // emit nothing — leave the read aspect verifying
                default -> new VerifyOutcome.Success();
            };
            if (outcome != null) {
                emit("verify", node.nodeId(), () -> output.verifyResult(node, outcome));
            }
        }
    }

    private static @NotNull String reason(final @NotNull WorkloadScenario.Fault fault, final @NotNull String fallback) {
        return fault.reason().isBlank() ? fallback : fault.reason();
    }

    /** The emitted value for a node: a per-emission monotonic counter for wave {@code "counter"}, else the waveform. */
    private @NotNull Object valueOf(final @NotNull String nodeId, final long elapsed) {
        if ("counter".equals(scenario.waveKind(nodeId))) {
            return counters.merge(nodeId, 1L, Long::sum); // 1,2,3,… — advances only when a reading is actually emitted
        }
        return scenario.valueFor(nodeId, elapsed, noise);
    }

    /** An extreme/edge-typed value for {@code poll:"literal"} testing — how does the pipeline serialize it northbound? */
    private static @NotNull Object literalValue(final @NotNull String kind) {
        return switch (kind) {
            case "max_long" -> Long.MAX_VALUE;                 // 9223372036854775807 — beyond double precision
            case "min_long" -> Long.MIN_VALUE;
            case "precise_long" -> 9007199254740993L;          // 2^53 + 1 — not representable as a double
            case "max_double" -> Double.MAX_VALUE;
            case "nan" -> Double.NaN;                          // not representable in standard JSON
            case "pos_inf" -> Double.POSITIVE_INFINITY;        // not representable in standard JSON
            case "neg_inf" -> Double.NEGATIVE_INFINITY;
            case "neg_zero" -> -0.0d;
            case "huge_string" -> "A".repeat(50_000);          // 50KB payload
            case "unicode" -> "🔥café±∞ مرحبا \"q\" \n\t end"; // emoji, RTL, quotes, control chars
            case "empty" -> "";
            case "bool" -> Boolean.TRUE;
            default -> kind;
        };
    }

    @Override
    public void pollBatch(final @NotNull List<Node> nodes) {
        nodes.forEach(n -> knownNodes.put(n.nodeId(), n)); // capture original instances for control-channel injection
        for (final Node n : nodes) {
            log.warn("WL_POLL id={} node={}", adapterId, n.nodeId()); // which nodes Edge actually polls (used-goal probe)
        }
        if ("throw-in-poll".equals(scenario.misbehave())) {
            throw new RuntimeException("workload: MISBEHAVE — exception thrown inside pollBatch()"); // does Edge isolate it?
        }
        blockSleep("poll"); // if a `block poll <ms>` is armed, sleep inside the command — control plane must stay responsive
        applyTimeline();
        if (downGoal) {
            if (!announcedDisconnect) {
                announcedDisconnect = true;
                output.disconnected();
            }
            return;
        }
        final long elapsed = System.currentTimeMillis() - startMs;
        for (final Node node : nodes) {
            journal("POLL node=" + node.nodeId());
            if (mutedTags.contains(node.nodeId())) {
                continue; // SILENT freeze: poll returns nothing, no error — a frozen sensor under a healthy connection
            }
            if (faultedTags.contains(node.nodeId())) {
                emit("poll", node.nodeId(), () -> output.nodeError(node, "workload: tag fault (timeline)", false)); // dynamic fault wins
                continue;
            }
            final WorkloadScenario.Fault fault = scenario.faultFor(node.nodeId());
            // NOTE: compute the value NOW (poll time), capture it in the emission — so a HELD poll carries its OLD value.
            if ("double".equals(fault.poll())) {
                // CONTRACT VIOLATION (NV-C05): emit TWO terminal datapoints for the SAME node in ONE poll operation.
                // PAW must not double-complete/double-count the one-shot poll or wedge on an impossible remaining count.
                final Object v = valueOf(node.nodeId(), elapsed);
                emit("poll", node.nodeId(), () -> output.dataPoint(node, new WorkloadDataPoint(node.nodeId(), v)));
                emit("poll", node.nodeId(), () -> output.dataPoint(node, new WorkloadDataPoint(node.nodeId(), v)));
                continue;
            }
            final Runnable emission = switch (fault.poll()) {
                case "error" -> () -> output.nodeError(node, reason(fault, "workload: poll error (scenario)"), false);
                case "no-response" -> null; // emit nothing for this node this cycle
                case "garbage" -> () -> output.dataPoint(node, new WorkloadDataPoint(node.nodeId(), "GARBAGE_NOT_A_NUMBER"));
                case "literal" -> () -> output.dataPoint(node, new WorkloadDataPoint(node.nodeId(), literalValue(fault.reason())));
                default -> {
                    final Object v = valueOf(node.nodeId(), elapsed);
                    yield () -> output.dataPoint(node, new WorkloadDataPoint(node.nodeId(), v));
                }
            };
            if (emission != null) {
                emit("poll", node.nodeId(), emission);
            }
        }
    }

    @Override
    public void addSubscriptionBatch(final @NotNull List<Node> nodes) {
        nodes.forEach(n -> knownNodes.put(n.nodeId(), n)); // capture original instances for control-channel injection
        for (final Node node : nodes) {
            subscribed.put(node.nodeId(), node); // INCREMENTAL — must never reset the existing shadow set
            journal("ADDSUB node=" + node.nodeId());
        }
        journal("RESOURCE subs=" + subscribed.size()); // shadow-set size timeline for subscription-leak tests (NV-F03)
        log.warn("WL_SUBSCRIBE_ADD id={} added={} shadowSetSize={}", adapterId, nodes.size(), subscribed.size());
    }

    @Override
    public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {
        // fire-and-forget, no response expected; a node that isn't subscribed is ignored silently
        for (final Node node : nodes) {
            journal("REMOVESUB node=" + node.nodeId());
            if (subscribed.remove(node.nodeId()) != null) {
                log.warn("WL_SUBSCRIBE_REMOVE id={} node={}", adapterId, node.nodeId());
            } else {
                log.warn("WL_SUBSCRIBE_IGNORE_ABSENT id={} node={}", adapterId, node.nodeId());
            }
        }
        journal("RESOURCE subs=" + subscribed.size()); // shadow-set size timeline for subscription-leak tests (NV-F03)
    }

    /** The subscribed-node push loop (runs on {@link #subExecutor}). Pushes datapoints, or a SPONTANEOUS nodeError. */
    private void pushSubscriptions() {
        try {
            if ("fake-node-flood".equals(scenario.misbehave())) {
                // CONTRACT VIOLATION: flood datapoints for FABRICATED nodes Edge never handed us (attacks the
                // opaque Tag-ref routing) — from a background thread, ~100/s of rogue events
                for (int i = 0; i < 50; i++) {
                    output.dataPoint(new WorkloadNode("ghost-" + i), new WorkloadDataPoint("ghost-" + i, i));
                }
                return;
            }
            if (subscribed.isEmpty()) {
                return;
            }
            applyTimeline();
            if (downGoal) {
                if (!announcedDisconnect) {
                    announcedDisconnect = true;
                    output.disconnected();
                }
                return;
            }
            final long elapsed = System.currentTimeMillis() - startMs;
            for (final Node node : List.copyOf(subscribed.values())) {
                final String id = node.nodeId();
                if (mutedTags.contains(id)) {
                    continue; // SILENT freeze: no data, no error — device looks alive on the wire but is dead
                }
                if (faultedTags.contains(id)) {
                    output.nodeError(node, "workload: subscription fault (timeline)", true); // spontaneous → power-cycle
                    continue;
                }
                final WorkloadScenario.Fault subFault = scenario.faultFor(id);
                // Subscription pushes route through the "subscribe" gate so a test can HOLD subscription establishment /
                // the first event ({@code hold subscribe [node]}) — unchanged behavior when no gate is armed.
                switch (subFault.poll()) {
                    case "error" -> output.nodeError(node, "workload: subscription error (scenario)", true); // spontaneous
                    case "no-response" -> { /* silent this cycle */ }
                    case "garbage" -> // push a wrong-typed (String) value over the subscription where a number is expected
                            emit("subscribe", id, () -> output.dataPoint(node, new WorkloadDataPoint(id, "GARBAGE_NOT_A_NUMBER")));
                    case "literal" -> // push a configured extreme/edge-typed value (reason names the kind)
                            emit("subscribe", id, () -> output.dataPoint(node, new WorkloadDataPoint(id, literalValue(subFault.reason()))));
                    default -> {
                        final Object v = valueOf(id, elapsed);
                        emit("subscribe", id, () -> output.dataPoint(node, new WorkloadDataPoint(id, v)));
                    }
                }
            }
        } catch (final Throwable t) {
            log.warn("WL_SUBSCRIPTION_PUSH error id={}: {}", adapterId, t.toString());
        }
    }

    @Override
    public void writeBatch(final @NotNull List<WriteEntry> entries) {
        for (final WriteEntry entry : entries) {
            // marker so a black-box test can prove whether an MQTT southbound message ever reaches the adapter's write
            log.warn("WL_WRITE_BATCH id={} tag={} t={}", adapterId, entry.node().nodeId(), System.currentTimeMillis());
            journal("WRITE node=" + entry.node().nodeId());
            final WorkloadScenario.Fault fault = scenario.faultFor(entry.node().nodeId());
            final Runnable emission = switch (fault.write()) {
                case "fail" -> () -> output.writeResult(entry.node(), false, reason(fault, "workload: write failed (scenario)"));
                case "no-response" -> null; // emit nothing — leave the write aspect awaiting a result
                default -> () -> output.writeResult(entry.node(), true, null);
            };
            if (emission != null) {
                emit("write", emission);
            }
        }
    }

    @Override
    public void browse(final int requestId, final @NotNull BrowseFilter filter, final int maxReferences) {
        output.browsePage(requestId, List.of(), null);
    }

    @Override
    public void browseNext(final int requestId, final @NotNull BrowseContinuation continuation) {
        output.browsePage(requestId, List.of(), null);
    }

    @Override
    public void readNodeAttributes(final int requestId, final @NotNull List<Node> nodes) {
        nodes.forEach(n -> knownNodes.put(n.nodeId(), n)); // capture original instances for control-channel injection
        final List<ResolvedAttributes> resolved = new ArrayList<>(nodes.size());
        for (final Node node : nodes) {
            resolved.add(new ResolvedAttributes(node, "workload:double",
                    AccessFlags.builder().readable(AccessTriState.YES).pollable(AccessTriState.YES).build(), ""));
        }
        output.readAttributesResult(requestId, resolved);
    }

    /** Fire every timeline event whose wall-clock instant has passed, once. */
    private synchronized void applyTimeline() {
        final long elapsed = System.currentTimeMillis() - startMs;
        final List<WorkloadScenario.Event> timeline = scenario.timeline();
        for (int i = 0; i < timeline.size(); i++) {
            if (firedEvents.contains(i)) {
                continue;
            }
            final WorkloadScenario.Event ev = timeline.get(i);
            if (ev.atMs() > elapsed) {
                continue;
            }
            firedEvents.add(i);
            switch (ev.action()) {
                case "disconnect" -> downGoal = true;
                case "reconnect" -> downGoal = false;
                case "fault" -> faultedTags.add(ev.tag());
                case "recover" -> faultedTags.remove(ev.tag());
                case "mute" -> mutedTags.add(ev.tag());     // silent freeze: stop delivering, raise NO error
                case "unmute" -> mutedTags.remove(ev.tag());
                default -> { }
            }
        }
    }
}

