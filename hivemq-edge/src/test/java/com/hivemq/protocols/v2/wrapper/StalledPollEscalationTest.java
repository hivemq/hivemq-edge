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

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.protocols.v2.view.TagStatus;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * A stalled poll is escalated, not eternally healthy (EDG-824 #15). A poll whose result never
 * arrives is failed at the command-timeout deadline and retried on the cadence; after three consecutive failures the
 * aspect escalates through re-verification — a device that answers resumes producing, a mute adapter leaves the
 * aspect parked in verification, where the coarse {@link TagStatus} honestly folds to {@code ERROR} instead of a
 * producing-looking {@code NORTHBOUND_ONLY}.
 */
class StalledPollEscalationTest {

    private static final long POLL_INTERVAL = 1_000L;
    private static final long POLL_RESULT_TIMEOUT = 500L;

    private static @NotNull WrapperTestFixture stallFixture() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .pollIntervalMillis(POLL_INTERVAL)
                .pollResultTimeoutMillis(POLL_RESULT_TIMEOUT)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        return fixture;
    }

    /** One full stall cycle: the cadence fires a poll, the mock never answers, the deadline fails it. */
    private static void stallOnce(final @NotNull WrapperTestFixture fixture) {
        fixture.advance(POLL_INTERVAL); // WAITING_FOR_POLL_DATAPOINT — the mock adapter never answers a poll
        fixture.advance(POLL_RESULT_TIMEOUT); // the result deadline fires
    }

    @Test
    void aSingleStalledPoll_isFailedAtTheDeadlineAndRetriedOnTheCadence() {
        final WrapperTestFixture fixture = stallFixture();

        stallOnce(fixture);

        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tag("temperature").failureCount()).isEqualTo(1);
        assertThat(fixture.tag("temperature").lastFailureReason()).contains("no poll result within");
        // one hiccup is a retry, not an alarm
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }

    @Test
    void persistentStall_onAMuteAdapter_escalatesTheCoarseStatusToError() {
        final WrapperTestFixture fixture = stallFixture();
        fixture.adapter.verifyDrop = true; // the adapter is mute: the escalation's re-verification never answers

        stallOnce(fixture);
        stallOnce(fixture);
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY); // not yet escalated

        stallOnce(fixture); // third consecutive failure: escalate through re-verification

        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_VERIFICATION");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.ERROR);
    }

    @Test
    void persistentStall_onADeviceThatStillVerifies_resumesThePollLoop() {
        final WrapperTestFixture fixture = stallFixture();

        stallOnce(fixture);
        stallOnce(fixture);
        stallOnce(fixture); // escalation fires — the mock answers the re-verification with success

        // the honest recovery: re-verified in place, back on the cadence, failures on record
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
        assertThat(fixture.tag("temperature").failureCount()).isEqualTo(3);
    }

    @Test
    void aValueBetweenStalls_resetsTheConsecutiveEscalationCounter() {
        final WrapperTestFixture fixture = stallFixture();
        fixture.adapter.verifyDrop = true; // would park the aspect if the escalation ever fired

        stallOnce(fixture);
        stallOnce(fixture);
        // a value arrives: the device is alive, the consecutive count starts over
        fixture.advance(POLL_INTERVAL);
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.drain();

        stallOnce(fixture);
        stallOnce(fixture);

        // only two consecutive failures since the value — no escalation
        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_POLL_INTERVAL");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }
}
