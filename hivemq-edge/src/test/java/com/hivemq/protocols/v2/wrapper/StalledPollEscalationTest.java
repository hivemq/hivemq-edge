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

import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
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

    /** Mirrors {@code TagAspectRead.STALE_AFTER_NO_VALUE_MILLIS} — pinned here so a change to it lands as a test
     * failure rather than a silent behaviour change. */
    private static final long STALE_AFTER = 5 * 60 * 1_000L;

    /**
     * The staleness verdict, read from the published snapshot: an OPERATING state that reports it is not operating
     * can only mean the aspect has passed its no-value deadline. There is no new status value to look for — that was
     * the point of folding it into the existing {@code ERROR}.
     */
    private static boolean readsStale(final @NotNull WrapperTestFixture fixture) {
        return "WAITING_FOR_POLL_INTERVAL".equals(fixture.readState("temperature"))
                && !fixture.tag("temperature").readAspectOperating();
    }

    /** One cycle where the device answers its poll on time, so a reading is published. */
    private static void publishOnce(final @NotNull WrapperTestFixture fixture) {
        for (int i = 0; i < 5 && !"WAITING_FOR_POLL_INTERVAL".equals(fixture.readState("temperature")); i++) {
            fixture.advance(POLL_RESULT_TIMEOUT); // let a pending re-verification settle first
        }
        fixture.advance(POLL_INTERVAL); // → WAITING_FOR_POLL_DATAPOINT
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.drain();
    }

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

    /** One cycle where the poll misses its result deadline and the device's answer then arrives LATE (after the
     * deadline, in WAITING_FOR_POLL_INTERVAL): the value is discarded and nothing is published. */
    private static void lateOnce(final @NotNull WrapperTestFixture fixture) {
        fixture.advance(POLL_INTERVAL); // WAITING_FOR_POLL_DATAPOINT
        fixture.advance(POLL_RESULT_TIMEOUT); // the deadline fires the poll → WAITING_FOR_POLL_INTERVAL
        // the device's answer arrives late, after the deadline — proof of life, but not a published reading
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.drain();
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

    // EDG-824 #15 (Sam #7): a device that answers every poll just AFTER its result deadline publishes nothing. The
    // late value is discarded and must NOT reset the escalation — otherwise the tag reads healthy forever while
    // producing no data. Persistent lateness therefore escalates exactly like a mute stall.
    @Test
    void persistentlyLateValues_escalate_becauseNothingIsEverPublished() {
        final WrapperTestFixture fixture = stallFixture();
        fixture.adapter.verifyDrop = true; // the re-verification the escalation triggers never answers

        lateOnce(fixture);
        lateOnce(fixture);
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY); // not yet escalated

        lateOnce(fixture); // third consecutive missed publish: escalate through re-verification

        assertThat(fixture.readState("temperature")).isEqualTo("WAITING_FOR_VERIFICATION");
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.ERROR);
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

    // ── the no-value deadline (Sam round 2, finding 5) ──────────────────────────────────────────────────────────
    // The escalation above asks "is the device still there?", and a device that answers cheap verification while
    // stalling every poll passes that question every round — so the tag returned to a producing-looking
    // NORTHBOUND_ONLY within a second of each escalation and spent essentially all its life reading healthy having
    // published nothing. The deadline asks the question that matters instead: has a reading actually arrived?

    @Test
    void aDeviceThatVerifiesButNeverAnswersAPoll_readsHealthyForAWhileThenIsReportedNotReadable() {
        final WrapperTestFixture fixture = stallFixture(); // the mock answers verification, never a poll

        // 100 cycles ≈ 150 s: escalation after escalation, every one of them satisfied by the re-verification.
        int healthyObservations = 0;
        for (int cycle = 0; cycle < 100; cycle++) {
            stallOnce(fixture);
            if (fixture.tagStatus("temperature") == TagStatus.NORTHBOUND_ONLY) {
                healthyObservations++;
            }
        }
        // The hole, measured: dozens of failures on record and the coarse status still reads healthy at most
        // sampling points. An operator polling the REST view would essentially never catch the escalation blip.
        assertThat(healthyObservations).isGreaterThan(50);
        assertThat(fixture.tag("temperature").failureCount()).isGreaterThan(50);
        assertThat(readsStale(fixture)).isFalse();

        // Carry on past the deadline: nothing has ever been published, so the tag is declared not readable — and
        // stays that way through every subsequent re-verification.
        int cycles = 100;
        while (!readsStale(fixture) && cycles < 400) {
            stallOnce(fixture);
            cycles++;
        }

        assertThat(readsStale(fixture)).as("the tag is declared not readable").isTrue();
        assertThat(fixture.clock.nowMillis()).isGreaterThanOrEqualTo(STALE_AFTER);
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.ERROR);

        // sticky: a re-verification the device happily answers does NOT restore the healthy status
        stallOnce(fixture);
        stallOnce(fixture);
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.ERROR);
    }

    @Test
    void theStalenessVerdictClearsOnlyWhenAReadingIsFinallyPublished() {
        final WrapperTestFixture fixture = stallFixture();
        int cycles = 0;
        while (!readsStale(fixture) && cycles < 400) {
            stallOnce(fixture);
            cycles++;
        }
        assertThat(readsStale(fixture)).isTrue();

        // the device starts answering again: one published reading is what restores the honest healthy status
        publishOnce(fixture);

        assertThat(readsStale(fixture)).isFalse();
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }

    @Test
    void aTagThatKeepsPublishing_neverGoesStale() {
        final WrapperTestFixture fixture = stallFixture();

        // Well past the deadline in wall-clock time, but every poll is answered — the deadline is about readings,
        // not about elapsed time, and a device reporting an unchanging value is perfectly healthy.
        for (int cycle = 0; cycle < 350; cycle++) {
            publishOnce(fixture);
        }

        assertThat(fixture.clock.nowMillis()).isGreaterThan(STALE_AFTER);
        assertThat(readsStale(fixture)).isFalse();
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }

    @Test
    void anOutageRestartsTheDeadline_soTimeTheAdapterWasDownIsNotHeldAgainstTheTag() {
        final WrapperTestFixture fixture = stallFixture();

        for (int cycle = 0; cycle < 150; cycle++) {
            stallOnce(fixture); // 225 s — deep into the deadline, not past it
        }
        assertThat(readsStale(fixture)).isFalse();

        fixture.output.disconnected();
        fixture.drain();
        fixture.advance(1_000); // the connection backoff fires: reconnect, re-verify, resume polling

        for (int cycle = 0; cycle < 150; cycle++) {
            stallOnce(fixture); // another 225 s
        }

        // Total elapsed is well past the deadline, but no single operating window was: the clock restarted when the
        // adapter came back, so a long outage does not make every tag report stale the instant it reconnects.
        assertThat(fixture.clock.nowMillis()).isGreaterThan(STALE_AFTER);
        assertThat(readsStale(fixture)).isFalse();
    }

    // ── polls that succeed and still publish nothing (Sam, round 3 finding 3) ───────────────────────────────────
    //
    // The deadline used to be evaluated only where a poll FAILED. A cooperative adapter that answers every poll on
    // time, but whose values the declared schema refuses — or that completes its poll with no values at all — never
    // took that path, so the tag published nothing indefinitely while reading NORTHBOUND_ONLY. The transport being
    // alive is not the question the deadline asks; whether a reading arrived is.

    private static final @NotNull ScalarSchema DOUBLE_0_TO_100 =
            new ScalarSchema(ScalarType.DOUBLE, 0, 100, null, null, false, true, false);

    private static @NotNull WrapperTestFixture typedStallFixture() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.typedPair("temperature", DOUBLE_0_TO_100)))
                .pollIntervalMillis(POLL_INTERVAL)
                .pollResultTimeoutMillis(POLL_RESULT_TIMEOUT)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        return fixture;
    }

    /** One cycle answered on time with a value the declared schema refuses: proof of life, nothing published. */
    private static void refusedOnce(final @NotNull WrapperTestFixture fixture) {
        fixture.advance(POLL_INTERVAL); // → WAITING_FOR_POLL_DATAPOINT
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", 250.0));
        fixture.drain();
    }

    /** One cycle the adapter completes on time with no values at all: a successful poll, and still no reading. */
    private static void emptyOnce(final @NotNull WrapperTestFixture fixture) {
        fixture.advance(POLL_INTERVAL); // → WAITING_FOR_POLL_DATAPOINT
        fixture.output.pollComplete(fixture.nodeFor("temperature"));
        fixture.drain();
    }

    @Test
    void onTimeValuesTheSchemaRefuses_pastTheDeadline_declareTheTagNotReadable() {
        final WrapperTestFixture fixture = typedStallFixture();

        for (int cycle = 0; cycle < 320; cycle++) {
            refusedOnce(fixture);
        }

        assertThat(fixture.clock.nowMillis()).isGreaterThan(STALE_AFTER);
        assertThat(fixture.tag("temperature").failureCount()).isGreaterThan(300);
        assertThat(fixture.tag("temperature").lastFailureReason()).contains("declared-schema violation");
        assertThat(readsStale(fixture))
                .as("nothing was ever published, so the tag is not readable")
                .isTrue();
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.ERROR);
    }

    @Test
    void onTimePollsThatCompleteWithNoValue_pastTheDeadline_declareTheTagNotReadable() {
        final WrapperTestFixture fixture = stallFixture();

        for (int cycle = 0; cycle < 320; cycle++) {
            emptyOnce(fixture);
        }

        // No failure is recorded anywhere — every poll succeeded. Only the no-value deadline can catch this one.
        assertThat(fixture.clock.nowMillis()).isGreaterThan(STALE_AFTER);
        assertThat(fixture.tag("temperature").failureCount()).isZero();
        assertThat(readsStale(fixture)).as("no reading has ever arrived").isTrue();
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.ERROR);
    }

    @Test
    void aRefusedOnlyTag_recoversWhenAConformingValueIsFinallyPublished() {
        final WrapperTestFixture fixture = typedStallFixture();
        for (int cycle = 0; cycle < 320; cycle++) {
            refusedOnce(fixture);
        }
        assertThat(readsStale(fixture)).isTrue();

        // The device is recalibrated and its readings now conform: one published value restores the honest status.
        fixture.advance(POLL_INTERVAL);
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", 21.5));
        fixture.drain();

        assertThat(readsStale(fixture)).isFalse();
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }

    @Test
    void aHealthyTagWhosePollIntervalExceedsTheDeadline_isNeverStale() {
        // The deadline is five minutes OR one whole poll cycle, whichever is longer. A device polled every six
        // minutes cannot publish within five however healthy it is; judging it on the shorter figure would declare
        // it unreadable during every single in-flight poll.
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.pair("temperature")))
                .pollIntervalMillis(6 * 60 * 1_000L) // a device polled every six minutes: slow, not broken
                .pollResultTimeoutMillis(POLL_RESULT_TIMEOUT)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        for (int cycle = 0; cycle < 4; cycle++) {
            fixture.advance(6 * 60 * 1_000L);
            assertThat(fixture.tag("temperature").readAspectOperating())
                    .as("healthy between the request and the answer")
                    .isTrue();
            fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
            fixture.drain();
            assertThat(readsStale(fixture)).isFalse();
        }
    }

    @Test
    void aQuietSubscribedTag_isNeverStale() {
        // The trap the deadline must avoid: a subscribed tag legitimately publishes nothing for hours when the
        // device value does not change. Silence there is correct behaviour, not a stalled exchange, so the deadline
        // is a polled-variant rule only.
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .runningCoordinator()
                .nodes(List.of(WrapperTestSupport.subscribablePair("temperature")))
                .pollIntervalMillis(POLL_INTERVAL)
                .pollResultTimeoutMillis(POLL_RESULT_TIMEOUT)
                .build();
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);

        // the first pushed value confirms the subscription — the tag is genuinely healthy at this point
        fixture.output.dataPoint(fixture.nodeFor("temperature"), WrapperTestSupport.dataPoint("temperature", "21"));
        fixture.drain();
        assertThat(fixture.readState("temperature")).isEqualTo("SUBSCRIBED");

        fixture.advance(4 * STALE_AFTER); // twenty quiet minutes: the device value simply never changes

        assertThat(fixture.tag("temperature").readAspectOperating()).isTrue();
        assertThat(fixture.tagStatus("temperature")).isEqualTo(TagStatus.NORTHBOUND_ONLY);
    }
}
