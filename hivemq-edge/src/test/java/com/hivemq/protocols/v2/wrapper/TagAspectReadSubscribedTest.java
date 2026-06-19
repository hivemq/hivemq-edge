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

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The subscribed read aspect (design §7.4; scenarios S10, S11 at unit level). The aspect subscribes on
 * verification, the first pushed value confirms it; a command-response loss backs off and re-adds, while a
 * spontaneous loss power-cycles the aspect through verification. Driven on {@code FakeClock} +
 * {@code ManualDispatcher} through the running coordinator, observed only through the published snapshot.
 */
class TagAspectReadSubscribedTest {

    private static @NotNull WrapperTestFixture subscribedFixture() {
        return WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.subscribablePair("temperature")))
                .build();
    }

    private static long count(final @NotNull WrapperTestFixture fixture, final @NotNull String command) {
        return fixture.commands().stream().filter(command::equals).count();
    }

    private static @NotNull WrapperTestFixture subscribedAndConfirmed() {
        final WrapperTestFixture fixture = subscribedFixture();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(100); // a tick dispatches the queued add-subscription batch
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.drain();
        return fixture;
    }

    @Test
    void subscribeThenFirstValueConfirmsSubscribed() {
        final WrapperTestFixture fixture = subscribedFixture();

        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        // Verified on connect: the add-subscription is requested; waiting for the first value to confirm it.
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_SUBSCRIPTION");

        fixture.advance(100); // a tick dispatches the queued add-subscription batch
        assertThat(fixture.commands()).contains("addSubscriptionBatch");

        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.drain();
        assertThat(fixture.readState("temperature")).isEqualTo("SUBSCRIBED");

        // Subsequent pushed values keep it operating.
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "22"));
        fixture.drain();
        assertThat(fixture.readState("temperature")).isEqualTo("SUBSCRIBED");
    }

    @Test
    void commandResponseLoss_backsOffThenReAdds() {
        final WrapperTestFixture fixture = subscribedAndConfirmed();
        assertThat(fixture.readState("temperature")).isEqualTo("SUBSCRIBED");
        final long verifyBatchesBefore = count(fixture, "verifyBatch");

        fixture.output.nodeError(fixture.nodeFor("temperature"), "subscription dropped", false);
        fixture.drain();
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_SUBSCRIPTION_RETRY");

        fixture.advance(1000); // the subscription backoff elapses: re-add, no re-verification
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_SUBSCRIPTION");
        assertThat(count(fixture, "addSubscriptionBatch")).isEqualTo(2);
        assertThat(count(fixture, "verifyBatch")).isEqualTo(verifyBatchesBefore); // no re-verify on a command loss
    }

    @Test
    void spontaneousLoss_powerCyclesThroughVerification() {
        final WrapperTestFixture fixture = subscribedAndConfirmed();
        assertThat(fixture.readState("temperature")).isEqualTo("SUBSCRIBED");
        final long verifyBatchesBefore = count(fixture, "verifyBatch");
        fixture.adapter.verifyDrop = true; // hold the power-cycle's re-verification so it is observable

        fixture.output.nodeError(fixture.nodeFor("temperature"), "device reset", true);
        fixture.drain();

        // A spontaneous loss re-verifies (design §7.4) — the aspect parks in verification, not subscription retry.
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_VERIFICATION");
        assertThat(count(fixture, "verifyBatch")).isEqualTo(verifyBatchesBefore + 1);
    }
}
