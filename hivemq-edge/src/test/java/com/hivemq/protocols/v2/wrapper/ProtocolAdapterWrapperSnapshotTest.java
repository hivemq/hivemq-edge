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

import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.CONNECTED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Snapshot publication and the goal-command bypass. The wrapper publishes an immutable
 * snapshot after every message, matching the machine state and carrying the per-tag status; and goal commands —
 * valid in every state — never reach the table, so they never trigger a defensive reset.
 */
class ProtocolAdapterWrapperSnapshotTest {

    @Test
    void snapshotIsPublishedAtConstructionAndAfterEveryMessage() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();

        assertThat(fixture.snapshot().machineState()).isEqualTo(STOPPED);
        assertThat(fixture.snapshot().adapterId()).isEqualTo("test-adapter");
        assertThat(fixture.snapshot().northboundActivated()).isFalse();

        fixture.adapter.connectReply = MockProtocolAdapter.Reply.DROP; // park to observe an intermediate snapshot
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.snapshot().machineState()).isEqualTo(WAITING_FOR_CONNECTED);
        assertThat(fixture.snapshot().northboundActivated()).isTrue();
    }

    @Test
    void connectedSnapshotMatchesTheMachineState() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        assertThat(fixture.snapshot().machineState()).isEqualTo(CONNECTED);
    }

    @Test
    void snapshotCarriesPerTagStatus() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .readUsed(Set.of("temperature"))
                .writeUsed(Set.of())
                .build();

        final List<TagStatusSnapshot> tags = fixture.snapshot().tags();

        assertThat(tags).hasSize(1);
        final TagStatusSnapshot tag = tags.get(0);
        assertThat(tag.tagName()).isEqualTo("temperature");
        assertThat(tag.readActivated()).isTrue();
        assertThat(tag.writeActivated()).isTrue();
        assertThat(tag.readUsed()).isTrue();
        assertThat(tag.writeUsed()).isFalse();
    }

    @Test
    void goalCommandsInEveryStateNeverTriggerTheDefensiveReset() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder().build();

        fixture.send(new ProtocolAdapterWrapperCommand.ActivateDirection(ProtocolAdapterDirection.NORTHBOUND));
        fixture.send(new ProtocolAdapterWrapperCommand.ActivateDirection(ProtocolAdapterDirection.SOUTHBOUND));
        fixture.send(new ProtocolAdapterWrapperCommand.DeactivateDirection(ProtocolAdapterDirection.NORTHBOUND));
        fixture.send(new ProtocolAdapterWrapperCommand.ApplyActivation(
                new ProtocolAdapterGoalState(true, true),
                Map.of("temperature", TagAspectActivationPreference.defaults())));
        fixture.send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(WrapperTestSupport.pair("temperature")),
                Map.of("temperature", TagAspectActivationPreference.defaults()),
                Set.of("temperature"),
                Set.of()));
        fixture.send(new ProtocolAdapterWrapperCommand.RetryTag("temperature"));
        fixture.send(new ProtocolAdapterWrapperCommand.StopAdapter());

        assertThat(fixture.defensiveResets()).isZero();
        assertThat(fixture.state()).isEqualTo(STOPPED);
    }
}
