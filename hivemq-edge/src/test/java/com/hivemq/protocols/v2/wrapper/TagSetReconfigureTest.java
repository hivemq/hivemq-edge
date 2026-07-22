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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The tags-only reconfigure transition on a live adapter (EDG-824 #2, the reconfigure wedge). A
 * {@code UpdateTagSet} rebuilds every tag aspect; the rebuilt aspects must be re-coupled to the adapter's live
 * connection phase and re-verify <b>in place</b> — a stably-CONNECTED adapter never reconnects on a tags-only
 * change, so without the phase replay every rebuilt tag would park in {@code WAITING_FOR_ADAPTER_READY} forever
 * and fold to an {@code ERROR} status while the adapter shows GREEN.
 */
class TagSetReconfigureTest {

    private static final long POLL_INTERVAL_MILLIS = 1_000L;

    private static @NotNull WrapperTestFixture fixtureWith(final @NotNull NodeTagPair pair) {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(pair))
                .pollIntervalMillis(POLL_INTERVAL_MILLIS)
                .build();
    }

    private static long count(final @NotNull List<String> commands, final @NotNull String command) {
        return commands.stream().filter(command::equals).count();
    }

    @Test
    void tagsOnlyReload_onAConnectedAdapter_reVerifiesInPlaceAndResumesProducing() {
        final NodeTagPair temperature = WrapperTestSupport.pair("temperature");
        final NodeTagPair pressure = WrapperTestSupport.pair("pressure");
        final WrapperTestFixture fixture = fixtureWith(temperature);

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        final long connectsBefore = count(fixture.commands(), "connect");

        // The reconfigure: add a second tag — a tags-only change on a stably-CONNECTED adapter.
        fixture.send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(temperature, pressure),
                Map.of(
                        "temperature", TagAspectActivationPreference.defaults(),
                        "pressure", TagAspectActivationPreference.defaults()),
                Set.of("temperature", "pressure"),
                Set.of(),
                POLL_INTERVAL_MILLIS));

        // Never reconnects...
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(count(fixture.commands(), "connect")).isEqualTo(connectsBefore);
        // ...and never parks: the rebuilt aspects re-verified in place and resumed operating.
        assertThat(fixture.readState("temperature")).isNotEqualTo("WAITING_FOR_ADAPTER_READY");
        assertThat(fixture.readState("pressure")).isNotEqualTo("WAITING_FOR_ADAPTER_READY");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tagStatus("pressure")).isEqualTo(TagStatus.NORTHBOUND_ONLY);

        // The surviving tag and the added tag both actually produce again: the next cadence polls both nodes and
        // the returned values are accepted back into the poll loop.
        fixture.advance(POLL_INTERVAL_MILLIS + 100); // cadence + one tick to dispatch the batch
        assertThat(count(fixture.commands(), "pollBatch")).isGreaterThanOrEqualTo(1);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");
        assertThat(fixture.readState("pressure")).isEqualTo("WAITING_FOR_POLL_DATAPOINT");
        fixture.output.dataPoint(temperature.node(), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.output.dataPoint(pressure.node(), WrapperTestSupport.dataPoint("pressure", "1.1"));
        fixture.drain();
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.readState("pressure")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tag("temperature").failureCount()).isZero();
        assertThat(fixture.tag("pressure").failureCount()).isZero();
    }

    @Test
    void pollIntervalOnlyReload_appliesTheNewCadence() {
        final NodeTagPair temperature = WrapperTestSupport.pair("temperature");
        final WrapperTestFixture fixture = fixtureWith(temperature);
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");

        // The reconfigure: same tag set, five-times-faster cadence.
        fixture.send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(temperature),
                Map.of("temperature", TagAspectActivationPreference.defaults()),
                Set.of("temperature"),
                Set.of(),
                POLL_INTERVAL_MILLIS / 5));

        // The first poll after the reload runs at the NEW 200 ms cadence: it fires well before the stale
        // 1000 ms cadence would have. (One poll only — the mock adapter does not answer polls by itself.)
        final long pollsBefore = count(fixture.commands(), "pollBatch");
        fixture.advance(2 * (POLL_INTERVAL_MILLIS / 5)); // two new cadences — well under the stale interval
        assertThat(count(fixture.commands(), "pollBatch")).isGreaterThan(pollsBefore);
    }

    @Test
    void tagsOnlyReload_whileStopped_staysParkedAndCouplesOnTheNextConnect() {
        final NodeTagPair temperature = WrapperTestSupport.pair("temperature");
        final NodeTagPair pressure = WrapperTestSupport.pair("pressure");
        final WrapperTestFixture fixture = fixtureWith(temperature);

        // No direction active: the adapter is at rest, the replayed phase is DISCONNECTED — nothing changes.
        fixture.send(new ProtocolAdapterWrapperCommand.UpdateTagSet(
                List.of(temperature, pressure),
                Map.of(
                        "temperature", TagAspectActivationPreference.defaults(),
                        "pressure", TagAspectActivationPreference.defaults()),
                Set.of("temperature", "pressure"),
                Set.of(),
                POLL_INTERVAL_MILLIS));
        assertThat(fixture.readState("temperature")).isEqualTo("DEACTIVATED");

        // The next connect cycle couples the rebuilt aspects normally.
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tagStatus("pressure")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }
}
