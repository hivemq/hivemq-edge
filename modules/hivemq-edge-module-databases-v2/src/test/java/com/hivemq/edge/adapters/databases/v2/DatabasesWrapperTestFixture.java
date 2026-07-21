/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.databases.v2;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
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
import com.hivemq.protocols.v2.wrapper.TagAspectActivationPreference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

/**
 * Wires a real {@link ProtocolAdapterWrapper} — with the real polled read-aspect machines through a
 * {@link TagAspectRuntimeCoordinator} — against a real {@link DatabasesProtocolAdapter} on a {@link FakeClock} and a
 * single shared {@link ManualDispatcher}. Because JDBC calls are synchronous, {@code drainAll()} executes the whole
 * command/acknowledgment conversation — including the real query against a container — on the calling thread to a
 * fixed point, so poll cadence is fully deterministic even against a real database.
 */
final class DatabasesWrapperTestFixture implements AutoCloseable {

    private static final long TICK_PERIOD_MILLIS = 100;

    final @NotNull FakeClock clock = new FakeClock();
    final @NotNull ManualDispatcher dispatcher = new ManualDispatcher();
    final @NotNull List<DataPoint> northboundDataPoints = new ArrayList<>();

    private final @NotNull Mailbox<ProtocolAdapterWrapperMessage> mailbox = new DefaultMailbox<>();
    private final @NotNull AtomicReference<AdapterStatusSnapshot> snapshotReference = new AtomicReference<>();
    private final @NotNull List<NodeTagPair> nodes;
    private final @NotNull AutoCloseable tickSchedule;
    private final long pollIntervalMillis;

    DatabasesWrapperTestFixture(
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> configuration,
            final @NotNull List<NodeTagPair> nodes,
            final long pollIntervalMillis) {
        this.nodes = nodes;
        this.pollIntervalMillis = pollIntervalMillis;
        final ProtocolAdapterOutputFacade output = new ProtocolAdapterOutputFacade(mailbox);
        final DatabasesProtocolAdapter adapter = new DatabasesProtocolAdapter(
                DatabasesAdapterTestFixtures.input(
                        adapterId,
                        dispatcher,
                        new DatabasesAdapterTestFixtures.TestDataPointFactory(),
                        configuration,
                        nodes),
                output);
        final Map<String, TagAspectActivationPreference> activation = new HashMap<>();
        final Set<String> readUsed = new HashSet<>();
        for (final NodeTagPair pair : nodes) {
            activation.put(pair.tag().name(), TagAspectActivationPreference.defaults());
            readUsed.add(pair.tag().name());
        }
        final ProtocolAdapterGoalState goal = ProtocolAdapterGoalState.stopped();
        final ProtocolAdapterMetrics metrics =
                new ProtocolAdapterMetrics(new MetricRegistry(), adapterId, mailbox::size);
        final TagAspectRuntimeCoordinator coordinator = new TagAspectRuntimeCoordinator(
                adapterId, nodes, activation, readUsed, Set.of(), goal, pollIntervalMillis, RetryPolicy.defaults());
        final ProtocolAdapterWrapperContext context = new ProtocolAdapterWrapperContext(
                adapterId,
                adapter,
                mailbox,
                clock,
                RetryPolicy.defaults(),
                60_000,
                false,
                goal,
                activation,
                coordinator,
                ProtocolAdapterWrapperEventListener.NONE,
                metrics,
                northboundDataPoints::add);
        coordinator.bindRuntime(
                context.clock(),
                context.timers(),
                context.batches(),
                context.metrics(),
                context.protocolAdapter()::verifyBatch);
        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(context, snapshotReference);
        dispatcher.attach(mailbox, wrapper);
        this.tickSchedule = clock.scheduleTick(
                TICK_PERIOD_MILLIS, mailbox, () -> new ProtocolAdapterWrapperTick(clock.nowMillis()));
    }

    // ── driving ──────────────────────────────────────────────────────────────────────────────────

    void activateNorthbound() {
        mailbox.tell(new ProtocolAdapterWrapperCommand.ActivateDirection(ProtocolAdapterDirection.NORTHBOUND));
        dispatcher.drainAll();
    }

    /**
     * Advance one full poll interval and drain to a fixed point — the poll (a real query) executes synchronously
     * within the drain.
     */
    void advanceOnePollInterval() {
        clock.advance(pollIntervalMillis);
        dispatcher.drainAll();
    }

    // ── observation (snapshot-only) ──────────────────────────────────────────────────────────────

    @NotNull
    ProtocolAdapterWrapperState state() {
        return snapshotReference.get().machineState();
    }

    @NotNull
    TagStatusSnapshot tag(final @NotNull String tagName) {
        for (final TagStatusSnapshot tag : snapshotReference.get().tags()) {
            if (tag.tagName().equals(tagName)) {
                return tag;
            }
        }
        throw new IllegalArgumentException("no tag status for " + tagName);
    }

    @NotNull
    String readState(final @NotNull String tagName) {
        return tag(tagName).readAspectStateName();
    }

    @NotNull
    TagStatus tagStatus(final @NotNull String tagName) {
        return TagStatus.of(tag(tagName));
    }

    @Override
    public void close() throws Exception {
        mailbox.tell(new ProtocolAdapterWrapperCommand.StopAdapter());
        dispatcher.drainAll();
        tickSchedule.close();
    }
}
