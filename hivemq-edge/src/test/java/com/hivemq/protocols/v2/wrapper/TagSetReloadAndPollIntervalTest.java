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
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.view.TagStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Core-module coverage of the tag-set diff in {@link com.hivemq.protocols.v2.tag.TagAspectRuntimeCoordinator}, driven
 * through a real wrapper actor against the hand-rolled {@code MockProtocolAdapter} on a {@code FakeClock}: a tags-only
 * reload preserves a surviving tag in place (no reconnect, no re-verification), tears down a removed tag, and verifies
 * an added tag against the live connection; and each tag is polled at its own configured cadence.
 */
class TagSetReloadAndPollIntervalTest {

    private static long count(final @NotNull WrapperTestFixture fixture, final @NotNull String command) {
        return fixture.commands().stream().filter(command::equals).count();
    }

    @Test
    void tagsOnlyReload_survivorKeepsPolling_removedTornDown_addedVerifiedAgainstLiveConnection() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .pollIntervalMillis(1000)
                .nodes(List.of(WrapperTestSupport.pair("temperature"), WrapperTestSupport.pair("pressure")))
                .readUsed(Set.of("temperature", "pressure"))
                .build();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(count(fixture, "connect")).isEqualTo(1);
        assertThat(count(fixture, "verifyBatch")).isEqualTo(1); // one connect-gate batch covered both tags
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        // Tags-only reload dropping "pressure"; "temperature" is unchanged.
        fixture.send(updateTagSet(List.of(WrapperTestSupport.pair("temperature")), Set.of("temperature")));

        assertThat(count(fixture, "connect")).isEqualTo(1); // never reconnects
        assertThat(count(fixture, "verifyBatch")).isEqualTo(1); // the survivor did NOT re-verify
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.snapshot().tags()).hasSize(1); // the removed tag is gone

        // The survivor kept its poll schedule across the reload — it polls when its interval elapses.
        fixture.advance(1000);
        assertThat(fixture.commands()).contains("pollBatch");
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");

        // Tags-only reload adding "pressure" back while connected.
        fixture.send(updateTagSet(
                List.of(WrapperTestSupport.pair("temperature"), WrapperTestSupport.pair("pressure")),
                Set.of("temperature", "pressure")));

        assertThat(count(fixture, "connect")).isEqualTo(1); // still no reconnect
        assertThat(count(fixture, "verifyBatch")).isEqualTo(2); // a second batch for the added node only
        assertThat(fixture.readState("pressure")).isEqualTo("WAITING_FOR_POLL_INTERVAL"); // added tag verified
        assertThat(fixture.tagStatus("pressure")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_DATAPOINT"); // survivor untouched
    }

    @Test
    void perTagPollIntervals_areScheduledIndependently() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("fast"), WrapperTestSupport.pair("slow")))
                .readUsed(Set.of("fast", "slow"))
                .pollIntervals(Map.of("fast", 1000L, "slow", 5000L))
                .build();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.readState("fast")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.readState("slow")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        fixture.advance(1000); // only "fast" is due
        assertThat(fixture.readState("fast")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");
        assertThat(fixture.readState("slow")).isEqualTo("WAITING_FOR_POLL_INTERVAL"); // not overpolled

        fixture.advance(4000); // 5000 ms total: "slow" is now due
        assertThat(fixture.readState("slow")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");
    }

    private static @NotNull ProtocolAdapterWrapperCommand.UpdateTagSet updateTagSet(
            final @NotNull List<NodeTagPair> nodes, final @NotNull Set<String> readUsed) {
        final Map<String, TagAspectActivationPreference> activation = new HashMap<>();
        final Map<String, Long> pollIntervals = new HashMap<>();
        for (final NodeTagPair pair : nodes) {
            activation.put(pair.tag().name(), TagAspectActivationPreference.defaults());
            pollIntervals.put(pair.tag().name(), 1000L);
        }
        return new ProtocolAdapterWrapperCommand.UpdateTagSet(nodes, activation, pollIntervals, readUsed, Set.of());
    }
}
