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
package com.hivemq.protocols.v2.manager;

import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.adapter;
import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.tag;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ActivateAdapter;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ConfigurationChanged;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.DeactivateAdapter;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerMessage.ProtocolAdapterManagerTick;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterDirection;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The supervisor actor driven through its mailbox on a {@link ManualDispatcher}: configuration
 * add/remove, the four gentlest transitions, REST live-goal and retry routing, unknown-type handling, manual-only
 * recovery, and the health summary. The wrapper is the {@link RecordingWrapperFactory} double so these tests prove
 * the manager's own logic; the real wrapper wiring has its own test.
 */
class ProtocolAdapterManagerTest {

    private FakeClock clock;
    private ManualDispatcher dispatcher;
    private Mailbox<ProtocolAdapterManagerMessage> mailbox;
    private ProtocolAdapterHandleRegistry handleRegistry;
    private RecordingWrapperFactory wrapperFactory;
    private ProtocolAdapterManager manager;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        dispatcher = new ManualDispatcher();
        mailbox = new DefaultMailbox<>();
        handleRegistry = new ProtocolAdapterHandleRegistry();
        wrapperFactory = new RecordingWrapperFactory();
        final ProtocolAdapterFactoryRegistry factories = new ProtocolAdapterFactoryRegistry(
                Set.of(new ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory(
                        ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID)));
        manager = new ProtocolAdapterManager(factories, handleRegistry, wrapperFactory, clock);
        dispatcher.attach(mailbox, manager);
        manager.bindSelf(mailbox);
    }

    @Test
    void configurationAdd_createsRegistersAndStartsTheAdapter() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));

        assertThat(wrapperFactory.createdAdapterIds()).containsExactly("a");
        assertThat(handleRegistry.find("a")).isNotNull();
        // The config-declared activation is applied to bring the freshly-created wrapper to its goal.
        assertThat(wrapperFactory.commands("a"))
                .last()
                .isInstanceOf(ProtocolAdapterWrapperCommand.ApplyActivation.class);
    }

    @Test
    void configurationRemove_stopsAndDiscardsARunningAdapter() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));
        wrapperFactory.setMachineState("a", ProtocolAdapterWrapperState.CONNECTED);

        send(new ConfigurationChanged(List.of()));

        assertThat(wrapperFactory.commands("a"))
                .hasAtLeastOneElementOfType(ProtocolAdapterWrapperCommand.StopAdapter.class);
        assertThat(handleRegistry.find("a")).isNull();

        // The wrapper reports stopped; teardown completes and there is no recreate.
        fireWrapperStopped("a");
        assertThat(handleRegistry.find("a")).isNull();
        assertThat(wrapperFactory.createdAdapterIds()).containsExactly("a");
    }

    @Test
    void configurationRemove_tearsDownImmediatelyWhenAlreadyStopped() {
        send(new ConfigurationChanged(List.of(adapter("a").build()))); // snapshot starts STOPPED

        send(new ConfigurationChanged(List.of()));

        assertThat(handleRegistry.find("a")).isNull();
        assertThat(wrapperFactory.commands("a")).noneMatch(ProtocolAdapterWrapperCommand.StopAdapter.class::isInstance);
    }

    @Test
    void restActivationAndDeactivation_forwardLiveGoalCommands() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));

        send(new ActivateAdapter("a", ProtocolAdapterDirection.SOUTHBOUND));
        send(new DeactivateAdapter("a", ProtocolAdapterDirection.NORTHBOUND));

        assertThat(wrapperFactory.commands("a"))
                .anyMatch(command -> command instanceof ProtocolAdapterWrapperCommand.ActivateDirection activate
                        && activate.direction() == ProtocolAdapterDirection.SOUTHBOUND)
                .anyMatch(command -> command instanceof ProtocolAdapterWrapperCommand.DeactivateDirection deactivate
                        && deactivate.direction() == ProtocolAdapterDirection.NORTHBOUND);
    }

    @Test
    void reloadWithUnchangedFlags_isNoChange_andPreservesTheRestLiveGoal() {
        final ProtocolAdapterEntity config = adapter("a").build();
        send(new ConfigurationChanged(List.of(config)));
        send(new DeactivateAdapter("a", ProtocolAdapterDirection.NORTHBOUND));
        final int commandsBefore = wrapperFactory.commands("a").size();

        send(new ConfigurationChanged(List.of(adapter("a").build())));

        // NO_CHANGE sends nothing, so the REST live goal (deactivated northbound) is not clobbered.
        assertThat(wrapperFactory.commands("a")).hasSize(commandsBefore);
    }

    @Test
    void reloadActivationOnly_appliesAnActivationCommand() {
        send(new ConfigurationChanged(
                List.of(adapter("a").southboundActivated(false).build())));
        final int commandsBefore = wrapperFactory.commands("a").size();

        send(new ConfigurationChanged(
                List.of(adapter("a").southboundActivated(true).build())));

        assertThat(wrapperFactory.commands("a")).hasSizeGreaterThan(commandsBefore);
        assertThat(wrapperFactory.commands("a"))
                .last()
                .isInstanceOf(ProtocolAdapterWrapperCommand.ApplyActivation.class);
    }

    @Test
    void reloadTagsOnly_appliesAnUpdateTagSetCommand() {
        send(new ConfigurationChanged(
                List.of(adapter("a").tags(tag("temperature").build()).build())));

        send(new ConfigurationChanged(List.of(adapter("a")
                .tags(tag("temperature").build(), tag("pressure").build())
                .build())));

        assertThat(wrapperFactory.commands("a"))
                .hasAtLeastOneElementOfType(ProtocolAdapterWrapperCommand.UpdateTagSet.class);
        assertThat(wrapperFactory.translateNodesAdapterIds()).contains("a");
    }

    @Test
    void reloadFullRecreate_stopsThenRecreates() {
        send(new ConfigurationChanged(
                List.of(adapter("a").adapterConfiguration(Map.of("host", "a")).build())));
        wrapperFactory.setMachineState("a", ProtocolAdapterWrapperState.CONNECTED);

        send(new ConfigurationChanged(
                List.of(adapter("a").adapterConfiguration(Map.of("host", "b")).build())));

        assertThat(wrapperFactory.commands("a"))
                .hasAtLeastOneElementOfType(ProtocolAdapterWrapperCommand.StopAdapter.class);
        assertThat(handleRegistry.find("a")).isNull();

        fireWrapperStopped("a");

        assertThat(wrapperFactory.createdAdapterIds()).containsExactly("a", "a");
        assertThat(handleRegistry.find("a")).isNotNull();
    }

    @Test
    void wrapperError_isRecordedWithoutAutomaticRecreate() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));
        final int createdBefore = wrapperFactory.createdAdapterIds().size();

        fireWrapperError("a", "boom");

        assertThat(wrapperFactory.createdAdapterIds()).hasSize(createdBefore);
        assertThat(handleRegistry.find("a")).isNotNull();
    }

    @Test
    void recovery_deactivateThenActivate_areForwardedAsLiveGoalCommands() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));

        send(new DeactivateAdapter("a", ProtocolAdapterDirection.BOTH));
        send(new ActivateAdapter("a", ProtocolAdapterDirection.BOTH));

        assertThat(wrapperFactory.commands("a"))
                .anyMatch(command -> command instanceof ProtocolAdapterWrapperCommand.DeactivateDirection deactivate
                        && deactivate.direction() == ProtocolAdapterDirection.BOTH)
                .anyMatch(command -> command instanceof ProtocolAdapterWrapperCommand.ActivateDirection activate
                        && activate.direction() == ProtocolAdapterDirection.BOTH);
    }

    @Test
    void retryTag_isForwardedToTheWrapper() {
        send(new ConfigurationChanged(List.of(adapter("a").build())));

        send(new ProtocolAdapterManagerMessage.RetryTag("a", "temperature"));

        assertThat(wrapperFactory.commands("a"))
                .anyMatch(command -> command instanceof ProtocolAdapterWrapperCommand.RetryTag retry
                        && retry.tagName().equals("temperature"));
    }

    @Test
    void unknownProtocolId_isSurfacedAsAnErrorHandleWithNoWrapper() {
        send(new ConfigurationChanged(
                List.of(adapter("ghost").protocolId("nope").build())));

        final ProtocolAdapterHandle handle = handleRegistry.find("ghost");
        assertThat(handle).isNotNull();
        final AdapterStatusSnapshot snapshot = handle.snapshot().get();
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.machineState()).isEqualTo(ProtocolAdapterWrapperState.ERROR);
        assertThat(snapshot.lastErrorReason()).contains("no registered adapter type");
        assertThat(wrapperFactory.createdAdapterIds()).doesNotContain("ghost");
    }

    @Test
    void restCommandToUnknownAdapter_isDroppedSafely() {
        send(new ActivateAdapter("ghost", ProtocolAdapterDirection.BOTH));

        assertThat(handleRegistry.find("ghost")).isNull();
    }

    @Test
    void managerTick_publishesAHealthSummaryFoldedFromTheRegistry() {
        send(new ConfigurationChanged(List.of(adapter("a").build(), adapter("b").build())));
        wrapperFactory.setMachineState("a", ProtocolAdapterWrapperState.CONNECTED);

        send(new ProtocolAdapterManagerTick(123L));

        final ProtocolAdapterManagerSnapshot summary = manager.healthSummary();
        assertThat(summary.totalAdapters()).isEqualTo(2);
        assertThat(summary.connectedAdapters()).isEqualTo(1);
        assertThat(summary.stoppedAdapters()).isEqualTo(1);
        assertThat(summary.lastUpdatedAtMillis()).isEqualTo(123L);
    }

    @Test
    void shutdownRequested_stopsEveryAdapter_closesItsContainer_andCompletesTheLatch() {
        send(new ConfigurationChanged(List.of(adapter("a").build(), adapter("b").build())));
        wrapperFactory.setMachineState("a", ProtocolAdapterWrapperState.CONNECTED);
        wrapperFactory.setMachineState("b", ProtocolAdapterWrapperState.CONNECTED);

        final CountDownLatch done = new CountDownLatch(1);
        send(new ProtocolAdapterManagerMessage.ShutdownRequested(done));

        // Both running adapters are told to stop and removed from the REST registry; the latch waits for them.
        assertThat(wrapperFactory.commands("a"))
                .hasAtLeastOneElementOfType(ProtocolAdapterWrapperCommand.StopAdapter.class);
        assertThat(wrapperFactory.commands("b"))
                .hasAtLeastOneElementOfType(ProtocolAdapterWrapperCommand.StopAdapter.class);
        assertThat(handleRegistry.find("a")).isNull();
        assertThat(handleRegistry.find("b")).isNull();
        assertThat(done.getCount()).isEqualTo(1);

        fireWrapperStopped("a");
        assertThat(done.getCount()).isEqualTo(1); // still waiting for "b"
        fireWrapperStopped("b");

        assertThat(done.getCount()).isZero(); // every adapter has wound down
        assertThat(wrapperFactory.closedAdapterIds()).containsExactlyInAnyOrder("a", "b"); // containers closed
    }

    @Test
    void shutdownRequested_withOnlyStoppedAdapters_tearsThemDownAndCompletesImmediately() {
        send(new ConfigurationChanged(List.of(adapter("a").build()))); // snapshot starts STOPPED

        final CountDownLatch done = new CountDownLatch(1);
        send(new ProtocolAdapterManagerMessage.ShutdownRequested(done));

        assertThat(done.getCount()).isZero(); // a stopped adapter is torn down at once
        assertThat(handleRegistry.find("a")).isNull();
        assertThat(wrapperFactory.closedAdapterIds()).contains("a");
    }

    private void send(final @NotNull ProtocolAdapterManagerMessage message) {
        mailbox.tell(message);
        dispatcher.drainAll();
    }

    private void fireWrapperStopped(final @NotNull String adapterId) {
        wrapperFactory.healthListener().wrapperStopped(adapterId);
        dispatcher.drainAll();
    }

    private void fireWrapperError(final @NotNull String adapterId, final @NotNull String reason) {
        wrapperFactory.healthListener().wrapperError(adapterId, reason);
        dispatcher.drainAll();
    }
}
