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
package com.hivemq.protocols.v2.wrapper;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.tag.TagAspectCoordinator;
import com.hivemq.protocols.v2.tag.TagAspectRuntimeCoordinator;
import com.hivemq.protocols.v2.tag.TagAspectSnapshotOnlyCoordinator;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.view.TagStatus;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Wires a real {@link ProtocolAdapterWrapper} against a {@link MockProtocolAdapter} on a {@link FakeClock} and a
 * {@link ManualDispatcher} (test plan) — the deterministic, sleep-free rig the adapter-machine tests
 * drive. Commands and injected events are told to the wrapper mailbox; {@link #drain()} delivers them in
 * priority-band order; {@link #advance(long)} moves time forward and delivers the resulting ticks. State is read
 * only through the published snapshot, honoring the actor model even in tests.
 */
final class WrapperTestFixture {

    final @NotNull String adapterId;
    final @NotNull FakeClock clock;
    final @NotNull ManualDispatcher dispatcher;
    final @NotNull MetricRegistry metricRegistry;
    final @NotNull Mailbox<ProtocolAdapterWrapperMessage> mailbox;
    final @NotNull ProtocolAdapterOutputFacade output;
    final @NotNull MockProtocolAdapter adapter;
    final @NotNull RecordingProtocolAdapterWrapperEventListener health;
    final @NotNull AtomicReference<AdapterStatusSnapshot> snapshotReference;
    final @NotNull ProtocolAdapterWrapper wrapper;
    final @NotNull List<NodeTagPair> nodes;

    private WrapperTestFixture(final @NotNull Builder builder) {
        this.adapterId = builder.adapterId;
        this.clock = new FakeClock();
        this.dispatcher = new ManualDispatcher();
        this.metricRegistry = new MetricRegistry();
        this.mailbox = new DefaultMailbox<>();
        this.output = new ProtocolAdapterOutputFacade(mailbox);
        this.adapter = new MockProtocolAdapter(adapterId, output);
        this.health = new RecordingProtocolAdapterWrapperEventListener();
        this.nodes = builder.nodes;

        final Map<String, TagAspectActivationPreference> activation =
                builder.activation != null ? builder.activation : defaultActivation(builder.nodes);
        final Set<String> readUsed = builder.readUsed != null ? builder.readUsed : allTagNames(builder.nodes);
        final Set<String> writeUsed = builder.writeUsed != null ? builder.writeUsed : new HashSet<>();

        final ProtocolAdapterMetrics metrics = new ProtocolAdapterMetrics(metricRegistry, adapterId, mailbox::size);
        // The default tag plane is the snapshot-only stand-in (adapter-machine tests); opt in to the running
        // coordinator to exercise the read and write aspect machines.
        final TagAspectCoordinator tagPlane;
        final TagAspectRuntimeCoordinator runningTagPlane;
        final TagAspectSnapshotOnlyCoordinator snapshotOnlyTagPlane;
        if (builder.runningCoordinator) {
            final Map<String, Long> pollIntervals = new HashMap<>();
            for (final NodeTagPair pair : builder.nodes) {
                final String tagName = pair.tag().name();
                pollIntervals.put(
                        tagName,
                        builder.pollIntervalsByTagName != null
                                ? builder.pollIntervalsByTagName.getOrDefault(tagName, builder.pollIntervalMillis)
                                : builder.pollIntervalMillis);
            }
            runningTagPlane = new TagAspectRuntimeCoordinator(
                    adapterId,
                    builder.nodes,
                    activation,
                    pollIntervals,
                    readUsed,
                    writeUsed,
                    builder.initialGoal,
                    builder.retryPolicy);
            snapshotOnlyTagPlane = null;
            tagPlane = runningTagPlane;
        } else {
            snapshotOnlyTagPlane = new TagAspectSnapshotOnlyCoordinator(builder.nodes, activation, readUsed, writeUsed);
            runningTagPlane = null;
            tagPlane = snapshotOnlyTagPlane;
        }
        final ProtocolAdapterWrapperContext context = new ProtocolAdapterWrapperContext(
                adapterId,
                adapter,
                mailbox,
                clock,
                builder.retryPolicy,
                builder.watchdogTimeoutMillis,
                builder.skipVerification,
                builder.initialGoal,
                activation,
                tagPlane,
                health,
                metrics);
        if (runningTagPlane != null) {
            runningTagPlane.bindRuntime(
                    context.clock(),
                    context.timers(),
                    context.batches(),
                    context.metrics(),
                    context.protocolAdapter()::verifyBatch);
        }
        if (snapshotOnlyTagPlane != null) {
            // The connect gate runs through the shared verification authority even with no aspect machines; bind
            // it to the adapter's verify seam so the adapter-machine tests still exercise the verification flow.
            snapshotOnlyTagPlane.bindVerifier(context.protocolAdapter()::verifyBatch);
        }
        this.snapshotReference = new AtomicReference<>();
        this.wrapper = new ProtocolAdapterWrapper(context, snapshotReference);
        dispatcher.attach(mailbox, wrapper);
        clock.scheduleTick(builder.tickPeriodMillis, mailbox, () -> new ProtocolAdapterWrapperTick(clock.nowMillis()));
    }

    static @NotNull Builder builder() {
        return new Builder();
    }

    // ── driving ──────────────────────────────────────────────────────────────────────────────────────────────

    void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
        mailbox.tell(message);
    }

    void drain() {
        dispatcher.drainAll();
    }

    void send(final @NotNull ProtocolAdapterWrapperMessage message) {
        tell(message);
        drain();
    }

    void advance(final long millis) {
        clock.advance(millis);
        drain();
    }

    /**
     * Deliver exactly one message — the highest-priority one currently queued — without draining the rest. Lets a
     * test observe priority-band ordering at the wrapper.
     */
    void deliverOne() {
        final ProtocolAdapterWrapperMessage message = mailbox.poll();
        if (message != null) {
            wrapper.receive(message);
        }
    }

    int pending() {
        return mailbox.size();
    }

    void activate(final @NotNull ProtocolAdapterDirection direction) {
        send(new ProtocolAdapterWrapperCommand.ActivateDirection(direction));
    }

    void deactivate(final @NotNull ProtocolAdapterDirection direction) {
        send(new ProtocolAdapterWrapperCommand.DeactivateDirection(direction));
    }

    void stopAdapter() {
        send(new ProtocolAdapterWrapperCommand.StopAdapter());
    }

    void retryTag(final @NotNull String tagName) {
        send(new ProtocolAdapterWrapperCommand.RetryTag(tagName));
    }

    /**
     * Submit a southbound write to a tag's write aspect — the "write arrives" trigger.
     */
    void submitWrite(final @NotNull String tagName, final @NotNull DataPoint value) {
        send(new ProtocolAdapterWrapperWriteRequest(nodeFor(tagName), value));
    }

    // ── observation (snapshot-only, per the actor model) ─────────────────────────────────────────────────────

    @NotNull
    AdapterStatusSnapshot snapshot() {
        return snapshotReference.get();
    }

    @NotNull
    ProtocolAdapterWrapperState state() {
        return snapshot().machineState();
    }

    @NotNull
    List<String> commands() {
        return adapter.commands;
    }

    /**
     * @return the protocol node behind the given tag — for injecting per-node values and errors via {@link #output}.
     */
    @NotNull
    Node nodeFor(final @NotNull String tagName) {
        for (final NodeTagPair pair : nodes) {
            if (pair.tag().name().equals(tagName)) {
                return pair.node();
            }
        }
        throw new IllegalArgumentException("no node for tag " + tagName);
    }

    /**
     * @return the published per-tag status for the given tag.
     */
    @NotNull
    TagStatusSnapshot tag(final @NotNull String tagName) {
        for (final TagStatusSnapshot tag : snapshot().tags()) {
            if (tag.tagName().equals(tagName)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("no tag status for " + tagName);
    }

    /**
     * @return the read aspect's current state name for the given tag, read from the published snapshot.
     */
    @NotNull
    String readState(final @NotNull String tagName) {
        return tag(tagName).readAspectStateName();
    }

    /**
     * @return the write aspect's current state name for the given tag, read from the published snapshot.
     */
    @NotNull
    String writeState(final @NotNull String tagName) {
        return tag(tagName).writeAspectStateName();
    }

    /**
     * @return the externally visible {@link TagStatus} folded from the given tag's published snapshot.
     */
    @NotNull
    TagStatus tagStatus(final @NotNull String tagName) {
        return TagStatus.of(tag(tagName));
    }

    long defensiveResets() {
        return metricRegistry
                .counter(ProtocolAdapterMetrics.ADAPTER_PREFIX + adapterId + ".defensive.resets")
                .getCount();
    }

    long stateTransitions() {
        return metricRegistry
                .counter(ProtocolAdapterMetrics.ADAPTER_PREFIX + adapterId + ".state.transitions")
                .getCount();
    }

    private static @NotNull Map<String, TagAspectActivationPreference> defaultActivation(
            final @NotNull List<NodeTagPair> nodes) {
        final Map<String, TagAspectActivationPreference> activation = new HashMap<>();
        for (final NodeTagPair pair : nodes) {
            activation.put(pair.tag().name(), TagAspectActivationPreference.defaults());
        }
        return activation;
    }

    private static @NotNull Set<String> allTagNames(final @NotNull List<NodeTagPair> nodes) {
        final Set<String> names = new HashSet<>();
        for (final NodeTagPair pair : nodes) {
            names.add(pair.tag().name());
        }
        return names;
    }

    static final class Builder {

        private @NotNull String adapterId = "test-adapter";
        private @NotNull List<NodeTagPair> nodes = List.of(WrapperTestSupport.pair("temperature"));
        private @NotNull ProtocolAdapterGoalState initialGoal = ProtocolAdapterGoalState.stopped();
        private @NotNull RetryPolicy retryPolicy = RetryPolicy.defaults();
        private long watchdogTimeoutMillis = 1000;
        private boolean skipVerification;
        private long tickPeriodMillis = 100;
        private boolean runningCoordinator;
        private long pollIntervalMillis = 1000;
        private @Nullable Map<String, Long> pollIntervalsByTagName;
        private @Nullable Map<String, TagAspectActivationPreference> activation;
        private @Nullable Set<String> readUsed;
        private @Nullable Set<String> writeUsed;

        @NotNull
        Builder adapterId(final @NotNull String adapterId) {
            this.adapterId = adapterId;
            return this;
        }

        @NotNull
        Builder nodes(final @NotNull List<NodeTagPair> nodes) {
            this.nodes = nodes;
            return this;
        }

        @NotNull
        Builder initialGoal(final @NotNull ProtocolAdapterGoalState initialGoal) {
            this.initialGoal = initialGoal;
            return this;
        }

        @NotNull
        Builder retryPolicy(final @NotNull RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        @NotNull
        Builder watchdogTimeoutMillis(final long watchdogTimeoutMillis) {
            this.watchdogTimeoutMillis = watchdogTimeoutMillis;
            return this;
        }

        @NotNull
        Builder skipVerification(final boolean skipVerification) {
            this.skipVerification = skipVerification;
            return this;
        }

        @NotNull
        Builder tickPeriodMillis(final long tickPeriodMillis) {
            this.tickPeriodMillis = tickPeriodMillis;
            return this;
        }

        /**
         * Drive the real read-aspect machines through a {@link TagAspectRuntimeCoordinator} instead of the
         * snapshot-only stand-in.
         */
        @NotNull
        Builder runningCoordinator() {
            this.runningCoordinator = true;
            return this;
        }

        @NotNull
        Builder pollIntervalMillis(final long pollIntervalMillis) {
            this.pollIntervalMillis = pollIntervalMillis;
            return this;
        }

        /**
         * Set per-tag poll cadences; a tag absent from the map falls back to {@link #pollIntervalMillis(long)}.
         */
        @NotNull
        Builder pollIntervals(final @NotNull Map<String, Long> pollIntervalsByTagName) {
            this.pollIntervalsByTagName = pollIntervalsByTagName;
            return this;
        }

        @NotNull
        Builder activation(final @NotNull Map<String, TagAspectActivationPreference> activation) {
            this.activation = activation;
            return this;
        }

        @NotNull
        Builder readUsed(final @NotNull Set<String> readUsed) {
            this.readUsed = readUsed;
            return this;
        }

        @NotNull
        Builder writeUsed(final @NotNull Set<String> writeUsed) {
            this.writeUsed = writeUsed;
            return this;
        }

        @NotNull
        WrapperTestFixture build() {
            return new WrapperTestFixture(this);
        }
    }
}
