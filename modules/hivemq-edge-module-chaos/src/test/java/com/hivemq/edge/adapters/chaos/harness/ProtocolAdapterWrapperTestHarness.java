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
package com.hivemq.edge.adapters.chaos.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.edge.adapters.chaos.ChaosNode;
import com.hivemq.edge.adapters.chaos.ChaosProtocolAdapter;
import com.hivemq.edge.adapters.chaos.ChaosScript;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.tag.TagAspectRuntimeCoordinator;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.view.TagStatus;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterDirection;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterGoalState;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterOutputFacade;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapper;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperContext;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperTick;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import com.hivemq.protocols.v2.wrapper.TagAspectActivationPreference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wires a real {@link ProtocolAdapterWrapper} actor — with the running tag-aspect coordinator — against a
 * {@link ChaosProtocolAdapter} on a {@link FakeClock} and a {@link ManualDispatcher}. It is the
 * deterministic, sleep-free rig the scenario matrix drives: commands and the simulator's scripted replies travel
 * through the wrapper mailbox exactly as in production, time advances one tick at a time, and the wrapper's state
 * is observed only through the published snapshot — honoring the actor model even in tests.
 * <p>
 * <b>Lifecycle.</b> Configure the harness ({@link #configure(List)} and the other setters) before driving it; the
 * actor is built lazily on the first driving call, after which configuration is frozen. Each <em>tick</em>
 * ({@link #advance(int)}) is one {@code tickPeriodMillis} window: the clock tells the wrapper its tick, the
 * simulator fires any deferred behavior due at that tick (enqueued alongside the wrapper tick, so an
 * acknowledgment {@code EVENT} is delivered before the {@code TICK}), and the dispatcher drains to a
 * fixed point.
 */
public final class ProtocolAdapterWrapperTestHarness {

    private static final @NotNull Schema STRING_SCHEMA =
            new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false);

    private final @NotNull ChaosScript script;

    private @NotNull String adapterId = "chaos-adapter";
    private @NotNull List<NodeTagPair> nodes = List.of(polledTag("temperature"));
    private @Nullable Set<String> readUsedConfig;
    private @Nullable Set<String> writeUsedConfig;
    private @Nullable Map<String, TagAspectActivationPreference> activationConfig;
    private @NotNull ProtocolAdapterGoalState initialGoal = ProtocolAdapterGoalState.stopped();
    private @NotNull RetryPolicy retryPolicy = RetryPolicy.defaults();
    private long watchdogTimeoutMillis = 1000;
    private long pollIntervalMillis = 1000;
    private final @NotNull Map<String, Long> pollIntervalsByTagName = new HashMap<>();
    private long tickPeriodMillis = 100;
    private boolean skipVerification;

    private @Nullable ActorRuntime runtime;

    private ProtocolAdapterWrapperTestHarness(final @NotNull ChaosScript script) {
        this.script = script;
    }

    /**
     * @param script the behavior script the simulator follows.
     * @return a new harness; configure it, then drive it.
     */
    public static @NotNull ProtocolAdapterWrapperTestHarness with(final @NotNull ChaosScript script) {
        return new ProtocolAdapterWrapperTestHarness(script);
    }

    // ── node/tag factories (build the configured pairs) ─────────────────────────────────────────────────────────

    /**
     * @param name the tag name (also the {@link ChaosNode} id).
     * @return a pollable (not subscribable) node/tag pair backed by a {@link ChaosNode}.
     */
    public static @NotNull NodeTagPair polledTag(final @NotNull String name) {
        return NodeTagPair.create(new ChaosNode(name), name, STRING_SCHEMA, true, false);
    }

    /**
     * @param name the tag name (also the {@link ChaosNode} id).
     * @return a subscribable (not pollable) node/tag pair backed by a {@link ChaosNode}.
     */
    public static @NotNull NodeTagPair subscribableTag(final @NotNull String name) {
        return NodeTagPair.create(new ChaosNode(name), name, STRING_SCHEMA, false, true);
    }

    // ── configuration (before the actor is built) ───────────────────────────────────────────────────────────────

    /**
     * @param nodes the node/tag pairs the adapter serves.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness configure(final @NotNull List<NodeTagPair> nodes) {
        requireNotBuilt();
        this.nodes = List.copyOf(nodes);
        return this;
    }

    /**
     * @param adapterId the adapter instance id.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness adapterId(final @NotNull String adapterId) {
        requireNotBuilt();
        this.adapterId = adapterId;
        return this;
    }

    /**
     * @param tagNames the tags a northbound mapping consumes (the read-side {@code used} condition). Defaults to
     *                 every tag.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness readUsed(final @NotNull Set<String> tagNames) {
        requireNotBuilt();
        this.readUsedConfig = Set.copyOf(tagNames);
        return this;
    }

    /**
     * @param tagNames the tags a southbound mapping produces to (the write-side {@code used} condition). Defaults
     *                 to none.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness writeUsed(final @NotNull Set<String> tagNames) {
        requireNotBuilt();
        this.writeUsedConfig = Set.copyOf(tagNames);
        return this;
    }

    /**
     * @param activation the per-tag activation preferences. Defaults to both aspects activated for every tag.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness activation(
            final @NotNull Map<String, TagAspectActivationPreference> activation) {
        requireNotBuilt();
        this.activationConfig = Map.copyOf(activation);
        return this;
    }

    /**
     * @param skipVerification whether the adapter reaches {@code CONNECTED} without a verification round.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness skipVerification(final boolean skipVerification) {
        requireNotBuilt();
        this.skipVerification = skipVerification;
        return this;
    }

    /**
     * @param retryPolicy the connection backoff policy.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness retryPolicy(final @NotNull RetryPolicy retryPolicy) {
        requireNotBuilt();
        this.retryPolicy = retryPolicy;
        return this;
    }

    /**
     * @param watchdogTimeoutMillis the per-state acknowledgment watchdog timeout, in milliseconds.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness watchdogTimeoutMillis(final long watchdogTimeoutMillis) {
        requireNotBuilt();
        this.watchdogTimeoutMillis = watchdogTimeoutMillis;
        return this;
    }

    /**
     * @param pollIntervalMillis the poll cadence for polled read aspects, in milliseconds.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness pollIntervalMillis(final long pollIntervalMillis) {
        requireNotBuilt();
        this.pollIntervalMillis = pollIntervalMillis;
        return this;
    }

    /**
     * Override one tag's poll cadence, leaving every other tag at the adapter-wide default. Proves per-tag poll
     * scheduling: tags with different intervals poll at their own cadence.
     *
     * @param tagName the tag whose poll cadence to set.
     * @param millis  the tag's poll interval, in milliseconds.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness pollInterval(final @NotNull String tagName, final long millis) {
        requireNotBuilt();
        this.pollIntervalsByTagName.put(tagName, millis);
        return this;
    }

    /**
     * @param tickPeriodMillis the wall-clock duration of one harness tick, in milliseconds.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness tickPeriodMillis(final long tickPeriodMillis) {
        requireNotBuilt();
        this.tickPeriodMillis = tickPeriodMillis;
        return this;
    }

    // ── driving (each builds the actor on first use) ────────────────────────────────────────────────────────────

    /**
     * Activate the northbound direction — a live-goal command.
     *
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness activateNorthbound() {
        return activate(ProtocolAdapterDirection.NORTHBOUND);
    }

    /**
     * Deactivate the northbound direction.
     *
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness deactivateNorthbound() {
        return deactivate(ProtocolAdapterDirection.NORTHBOUND);
    }

    /**
     * Activate the southbound direction.
     *
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness activateSouthbound() {
        return activate(ProtocolAdapterDirection.SOUTHBOUND);
    }

    /**
     * Deactivate the southbound direction.
     *
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness deactivateSouthbound() {
        return deactivate(ProtocolAdapterDirection.SOUTHBOUND);
    }

    /**
     * Activate both directions in one command, so the first connect verifies every active aspect in a single
     * shared {@code verifyBatch}.
     *
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness activateBoth() {
        return activate(ProtocolAdapterDirection.BOTH);
    }

    private @NotNull ProtocolAdapterWrapperTestHarness activate(final @NotNull ProtocolAdapterDirection direction) {
        final ActorRuntime rt = runtime();
        rt.goal = rt.goal.withActivated(direction);
        send(new ProtocolAdapterWrapperCommand.ActivateDirection(direction));
        return this;
    }

    private @NotNull ProtocolAdapterWrapperTestHarness deactivate(final @NotNull ProtocolAdapterDirection direction) {
        final ActorRuntime rt = runtime();
        rt.goal = rt.goal.withDeactivated(direction);
        send(new ProtocolAdapterWrapperCommand.DeactivateDirection(direction));
        return this;
    }

    /**
     * Simulate a config reload that flips a tag's activation preference (an {@code ACTIVATION_ONLY} difference): update the preference and tell the wrapper one {@code ApplyActivation} carrying the current
     * direction goal and the new activation map — no reconnect.
     *
     * @param tagName   the tag whose preference changes.
     * @param aspect    which aspect(s) to flip.
     * @param activated the new activation value.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness applyTagActivation(
            final @NotNull String tagName, final @NotNull TagAspectSelector aspect, final boolean activated) {
        final ActorRuntime rt = runtime();
        final TagAspectActivationPreference current =
                rt.activation.getOrDefault(tagName, TagAspectActivationPreference.defaults());
        boolean read = current.readActivated();
        boolean write = current.writeActivated();
        switch (aspect) {
            case READ -> read = activated;
            case WRITE -> write = activated;
            case BOTH -> {
                read = activated;
                write = activated;
            }
        }
        rt.activation.put(tagName, new TagAspectActivationPreference(read, write));
        send(new ProtocolAdapterWrapperCommand.ApplyActivation(rt.goal, Map.copyOf(rt.activation)));
        return this;
    }

    /**
     * Simulate a tags-only config reload: tell the wrapper one {@code UpdateTagSet} carrying the
     * current node set and activation with the given {@code used} sets — the coordinator re-verifies the new set
     * without ever reconnecting the adapter.
     *
     * @param readUsed  the tags a northbound mapping now consumes.
     * @param writeUsed the tags a southbound mapping now produces to.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness updateTags(
            final @NotNull Set<String> readUsed, final @NotNull Set<String> writeUsed) {
        final ActorRuntime rt = runtime();
        send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                nodes,
                Map.copyOf(rt.activation),
                resolvedPollIntervals(nodes),
                Set.copyOf(readUsed),
                Set.copyOf(writeUsed)));
        return this;
    }

    /**
     * Simulate a tags-only config reload that changes the tag set (adds or removes tags): tell the wrapper one
     * {@code UpdateTagSet} carrying the new node set. The coordinator diffs in place — survivors keep polling, added
     * tags verify against the live connection, removed tags are torn down — and never reconnects.
     *
     * @param newNodes  the new node/tag pairs.
     * @param readUsed  the tags a northbound mapping now consumes.
     * @param writeUsed the tags a southbound mapping now produces to.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness updateTags(
            final @NotNull List<NodeTagPair> newNodes,
            final @NotNull Set<String> readUsed,
            final @NotNull Set<String> writeUsed) {
        final ActorRuntime rt = runtime();
        this.nodes = List.copyOf(newNodes);
        final Map<String, TagAspectActivationPreference> newActivation = new HashMap<>();
        for (final NodeTagPair pair : newNodes) {
            final String name = pair.tag().name();
            newActivation.put(name, rt.activation.getOrDefault(name, TagAspectActivationPreference.defaults()));
        }
        rt.activation.clear();
        rt.activation.putAll(newActivation);
        send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                newNodes,
                Map.copyOf(newActivation),
                resolvedPollIntervals(newNodes),
                Set.copyOf(readUsed),
                Set.copyOf(writeUsed)));
        return this;
    }

    /**
     * Retry a tag out of permanent verification failure — a runtime-only command.
     *
     * @param tagName the tag to retry.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness retryTag(final @NotNull String tagName) {
        runtime();
        send(new ProtocolAdapterWrapperCommand.RetryTag(tagName));
        return this;
    }

    /**
     * Submit a southbound write to a tag's write aspect — the "write arrives" trigger.
     *
     * @param tagName the tag to write to.
     * @param value   the value to write.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness submitWrite(
            final @NotNull String tagName, final @NotNull DataPoint value) {
        runtime();
        send(new ProtocolAdapterWrapperWriteRequest(nodeFor(tagName), value));
        return this;
    }

    /**
     * Stop the adapter — the supervisor's removal/shutdown command.
     *
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness stop() {
        final ActorRuntime rt = runtime();
        rt.goal = ProtocolAdapterGoalState.stopped();
        send(new ProtocolAdapterWrapperCommand.StopAdapter());
        return this;
    }

    /**
     * Advance time by {@code ticks} harness ticks, delivering each tick and the simulator's scripted replies in
     * priority-band order.
     *
     * @param ticks the number of harness ticks to advance.
     * @return this harness.
     */
    public @NotNull ProtocolAdapterWrapperTestHarness advance(final int ticks) {
        final ActorRuntime rt = runtime();
        for (int i = 0; i < ticks; i++) {
            rt.clock.advance(tickPeriodMillis); // enqueue the wrapper TICK
            rt.adapter.onTick(); // fire due deferred behaviors (EVENTs) alongside the tick
            rt.dispatcher.drainAll(); // EVENT before TICK, drain to a fixed point
        }
        return this;
    }

    // ── observation (snapshot-only, per the actor model) ────────────────────────────────────────────────────────

    /**
     * @return the wrapper's current machine state, read from the published snapshot.
     */
    public @NotNull ProtocolAdapterWrapperState wrapperState() {
        return snapshot().machineState();
    }

    /**
     * @return the latest published adapter status snapshot.
     */
    public @NotNull AdapterStatusSnapshot snapshot() {
        return runtime().snapshotReference.get();
    }

    /**
     * @param tagName the tag to look up.
     * @return the externally visible {@link TagStatus} folded from the tag's published snapshot.
     */
    public @NotNull TagStatus tagStatus(final @NotNull String tagName) {
        return TagStatus.of(tag(tagName));
    }

    /**
     * @param tagName the tag to look up.
     * @return the tag's published per-tag snapshot.
     */
    public @NotNull TagStatusSnapshot tag(final @NotNull String tagName) {
        for (final TagStatusSnapshot tag : snapshot().tags()) {
            if (tag.tagName().equals(tagName)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("no tag status for " + tagName);
    }

    /**
     * @param tagName the tag to look up.
     * @return the read aspect's current state name.
     */
    public @NotNull String readState(final @NotNull String tagName) {
        return tag(tagName).readAspectStateName();
    }

    /**
     * @param tagName the tag to look up.
     * @return the write aspect's current state name.
     */
    public @NotNull String writeState(final @NotNull String tagName) {
        return tag(tagName).writeAspectStateName();
    }

    /**
     * @return the commands the wrapper has issued to the adapter, in order.
     */
    public @NotNull List<String> commandsSent() {
        return runtime().adapter.commands();
    }

    /**
     * @return the events the adapter has reported back through the tell-façade, in order.
     */
    public @NotNull List<String> eventsSeen() {
        return runtime().output.events();
    }

    /**
     * @return the error reasons the supervisor was notified of.
     */
    public @NotNull List<String> errorNotifications() {
        return runtime().health.errorReasons;
    }

    /**
     * Assert the wrapper issued exactly the given command sequence to the adapter.
     *
     * @param expected the expected commands, in order.
     */
    public void assertSequence(final @NotNull String... expected) {
        assertThat(commandsSent()).containsExactly(expected);
    }

    /**
     * @param tagName the tag to look up.
     * @return the protocol node behind the given tag.
     */
    public @NotNull Node nodeFor(final @NotNull String tagName) {
        for (final NodeTagPair pair : nodes) {
            if (pair.tag().name().equals(tagName)) {
                return pair.node();
            }
        }
        throw new IllegalArgumentException("no node for tag " + tagName);
    }

    // ── internals ───────────────────────────────────────────────────────────────────────────────────────────────

    private void send(final @NotNull ProtocolAdapterWrapperMessage message) {
        final ActorRuntime rt = runtime();
        rt.mailbox.tell(message);
        rt.dispatcher.drainAll();
    }

    private void requireNotBuilt() {
        if (runtime != null) {
            throw new IllegalStateException("the harness is already running; configure it before driving it");
        }
    }

    private @NotNull ActorRuntime runtime() {
        ActorRuntime rt = runtime;
        if (rt == null) {
            rt = new ActorRuntime();
            runtime = rt;
        }
        return rt;
    }

    private @NotNull Set<String> allTagNames() {
        final Set<String> names = new HashSet<>();
        for (final NodeTagPair pair : nodes) {
            names.add(pair.tag().name());
        }
        return names;
    }

    private @NotNull Map<String, TagAspectActivationPreference> defaultActivation() {
        final Map<String, TagAspectActivationPreference> activation = new HashMap<>();
        for (final NodeTagPair pair : nodes) {
            activation.put(pair.tag().name(), TagAspectActivationPreference.defaults());
        }
        return activation;
    }

    private @NotNull Map<String, Long> resolvedPollIntervals(final @NotNull List<NodeTagPair> forNodes) {
        final Map<String, Long> intervals = new HashMap<>();
        for (final NodeTagPair pair : forNodes) {
            final String name = pair.tag().name();
            intervals.put(name, pollIntervalsByTagName.getOrDefault(name, pollIntervalMillis));
        }
        return intervals;
    }

    /**
     * The actor and its collaborators, built once from the harness configuration and then frozen. Mirrors the
     * wiring of the wrapper-package test fixture, but always drives the running tag-aspect coordinator and the
     * {@link ChaosProtocolAdapter}.
     */
    private final class ActorRuntime {

        private final @NotNull FakeClock clock = new FakeClock();
        private final @NotNull ManualDispatcher dispatcher = new ManualDispatcher();
        private final @NotNull MetricRegistry metricRegistry = new MetricRegistry();
        private final @NotNull Mailbox<ProtocolAdapterWrapperMessage> mailbox = new DefaultMailbox<>();
        private final @NotNull RecordingOutput output;
        private final @NotNull ChaosProtocolAdapter adapter;
        private final @NotNull RecordingHealthListener health = new RecordingHealthListener();
        private final @NotNull AtomicReference<AdapterStatusSnapshot> snapshotReference = new AtomicReference<>();
        private final @NotNull ProtocolAdapterWrapper wrapper;

        private @NotNull ProtocolAdapterGoalState goal = initialGoal;
        private final @NotNull Map<String, TagAspectActivationPreference> activation;

        private ActorRuntime() {
            final Map<String, TagAspectActivationPreference> resolvedActivation =
                    activationConfig != null ? new HashMap<>(activationConfig) : defaultActivation();
            final Set<String> readUsed = readUsedConfig != null ? readUsedConfig : allTagNames();
            final Set<String> writeUsed = writeUsedConfig != null ? writeUsedConfig : new HashSet<>();
            this.activation = resolvedActivation;

            this.output = new RecordingOutput(new ProtocolAdapterOutputFacade(mailbox));
            this.adapter = new ChaosProtocolAdapter(adapterId, output, script);
            final ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(metricRegistry, adapterId, mailbox::size);
            final TagAspectRuntimeCoordinator coordinator = new TagAspectRuntimeCoordinator(
                    adapterId, nodes, activation, resolvedPollIntervals(nodes), readUsed, writeUsed, goal, retryPolicy);
            final ProtocolAdapterWrapperContext context = new ProtocolAdapterWrapperContext(
                    adapterId,
                    adapter,
                    mailbox,
                    clock,
                    retryPolicy,
                    watchdogTimeoutMillis,
                    skipVerification,
                    goal,
                    activation,
                    coordinator,
                    health,
                    metrics);
            coordinator.bindRuntime(
                    context.clock(),
                    context.timers(),
                    context.batches(),
                    context.metrics(),
                    context.protocolAdapter()::verifyBatch);
            this.wrapper = new ProtocolAdapterWrapper(context, snapshotReference);
            dispatcher.attach(mailbox, wrapper);
            clock.scheduleTick(tickPeriodMillis, mailbox, () -> new ProtocolAdapterWrapperTick(clock.nowMillis()));
        }
    }

    /**
     * A {@link ProtocolAdapterOutput} that records each callback's name then delegates to the real tell-façade, so
     * a test can assert the events the simulator reported back.
     */
    private static final class RecordingOutput implements ProtocolAdapterOutput {

        private final @NotNull ProtocolAdapterOutput delegate;
        private final @NotNull List<String> events = new ArrayList<>();

        private RecordingOutput(final @NotNull ProtocolAdapterOutput delegate) {
            this.delegate = delegate;
        }

        private @NotNull List<String> events() {
            return events;
        }

        @Override
        public void started() {
            events.add("started");
            delegate.started();
        }

        @Override
        public void stopped() {
            events.add("stopped");
            delegate.stopped();
        }

        @Override
        public void connected() {
            events.add("connected");
            delegate.connected();
        }

        @Override
        public void disconnected() {
            events.add("disconnected");
            delegate.disconnected();
        }

        @Override
        public void error(final @NotNull ErrorScope scope, final @NotNull String reason) {
            events.add("error");
            delegate.error(scope, reason);
        }

        @Override
        public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
            events.add("verifyResult");
            delegate.verifyResult(node, outcome);
        }

        @Override
        public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
            events.add("dataPoint");
            delegate.dataPoint(node, value);
        }

        @Override
        public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
            events.add("nodeError");
            delegate.nodeError(node, reason, spontaneous);
        }

        @Override
        public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
            events.add("writeResult");
            delegate.writeResult(node, success, reason);
        }

        @Override
        public void browseResult(final @NotNull List<BrowseResultEntry> entries) {
            events.add("browseResult");
            delegate.browseResult(entries);
        }
    }

    /**
     * Records the supervisor notifications the wrapper emits, so a test can assert a start, stop, or error was
     * reported.
     */
    private static final class RecordingHealthListener implements ProtocolAdapterWrapperEventListener {

        private final @NotNull List<String> started = new ArrayList<>();
        private final @NotNull List<String> stopped = new ArrayList<>();
        private final @NotNull List<String> errorReasons = new ArrayList<>();

        @Override
        public void wrapperStarted(final @NotNull String adapterId) {
            started.add(adapterId);
        }

        @Override
        public void wrapperStopped(final @NotNull String adapterId) {
            stopped.add(adapterId);
        }

        @Override
        public void wrapperError(final @NotNull String adapterId, final @NotNull String reason) {
            errorReasons.add(reason);
        }
    }
}
