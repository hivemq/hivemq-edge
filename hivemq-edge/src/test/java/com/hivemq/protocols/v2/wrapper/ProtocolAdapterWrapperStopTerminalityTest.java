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
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.ERROR;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.STOPPED;
import static com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState.WAITING_FOR_CONNECTION_RETRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.protocols.v2.view.AdapterStatusColor;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * A stop that never lands must have a terminal state (EDG-824 QA finding #19). The goal-driven
 * {@code stop()} out of {@code ERROR} is issued at most once per adapter life, so the three shapes of an
 * uncompletable stop — no acknowledgment, an {@code error(ADAPTER)} answer, and a throw — all settle in
 * {@code ERROR} instead of cycling back into {@code WAITING_FOR_STOPPED}.
 * <p>
 * The cycle they used to form was invisible from outside: the machine left {@code ERROR} within the same
 * {@code receive}, before a snapshot was ever published, so the REST status read {@code YELLOW_STOPPING} forever
 * while an ERROR line was logged every watchdog period. Only the two answer-less shapes were paced by the watchdog;
 * the other two re-commanded the stop with no delay at all.
 */
class ProtocolAdapterWrapperStopTerminalityTest {

    private static final long WATCHDOG_MILLIS = 500;

    /**
     * An adapter parked in its connect-retry loop, then commanded to stop by deactivating its only direction —
     * Estefania's scenario, and the one that reaches {@code stop()} without ever having been connected.
     */
    private static @NotNull WrapperTestFixture retryingFixtureCommandedToStop() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .watchdogTimeoutMillis(WATCHDOG_MILLIS)
                .build();
        fixture.adapter.connectReply = MockProtocolAdapter.Reply.FAIL_CONNECTION;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(WAITING_FOR_CONNECTION_RETRY);
        return fixture;
    }

    private static int stops(final @NotNull WrapperTestFixture fixture) {
        return Collections.frequency(fixture.commands(), "stop");
    }

    /**
     * Once settled, nothing further is commanded and nothing further is logged: advancing far past the point of
     * settlement leaves the stop count and the ERROR notifications where they were.
     */
    private static void assertNothingMoreHappens(final @NotNull WrapperTestFixture fixture) {
        final int stopsAtSettlement = stops(fixture);
        final int errorsAtSettlement = fixture.health.errorReasons.size();
        for (int period = 0; period < 20; period++) {
            fixture.advance(WATCHDOG_MILLIS);
        }
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(stops(fixture)).isEqualTo(stopsAtSettlement);
        assertThat(fixture.health.errorReasons).hasSize(errorsAtSettlement);
    }

    @Test
    void aStopThatIsNeverAcknowledged_settlesInError_insteadOfStoppingForever() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;

        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS); // the stop watchdog: reset to ERROR, one goal-driven retry
        fixture.advance(WATCHDOG_MILLIS); // that retry's watchdog: back to ERROR, and this time it stays

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(AdapterStatusColor.of(fixture.state())).isEqualTo(AdapterStatusColor.RED_ERROR);
        assertThat(fixture.snapshot().lastErrorReason()).contains("watchdog timeout while in WAITING_FOR_STOPPED");
        // The whole budget, pinned: the goal-driven stop and its one retry, each paired with the best-effort stop
        // enterError makes on the way in. An adapter is asked to stop four times in its life, never more.
        assertThat(stops(fixture)).isEqualTo(4);
        assertNothingMoreHappens(fixture);
    }

    @Test
    void aStopAnsweredWithAnAdapterError_settlesInError_withoutSpinningTheDispatchLoop() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.FAIL_ADAPTER;

        // No watchdog paces this shape: the error answer arrives immediately, so an unbounded retry re-commands the
        // stop as fast as the dispatch thread can drain — and a draining fixture would never return. Deliver a
        // bounded number of messages instead, so the runaway shows up as a mailbox that refills itself.
        fixture.tell(new ProtocolAdapterWrapperCommand.DeactivateDirection(ProtocolAdapterDirection.NORTHBOUND));
        for (int delivery = 0; delivery < 200; delivery++) {
            fixture.deliverOne();
        }

        assertThat(fixture.pending()).isZero(); // the loop is not feeding its own mailbox
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.snapshot().lastErrorReason()).contains("stop failed");
        assertNothingMoreHappens(fixture);
    }

    @Test
    void aStopThatThrows_settlesInError_withoutSpinningTheDispatchLoop() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopThrows = true;

        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS); // the tick that carries the one goal-driven retry

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.snapshot().lastErrorReason()).contains("adapter threw IllegalStateException");
        assertNothingMoreHappens(fixture);
    }

    @Test
    void reactivatingADirectionWhileTheStopIsPending_stillSettlesInError() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;
        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);

        // The goal wanting the adapter connected again does not rescue a machine whose adapter will not stop: ERROR
        // is manual-recovery territory, and a recreate is the way out.
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS);

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(AdapterStatusColor.of(fixture.state())).isEqualTo(AdapterStatusColor.RED_ERROR);
    }

    @Test
    void settlingInError_tellsTheSupervisorExactlyOnceThatTheStopFailed() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;

        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.health.stopFailures).isEmpty(); // the stop is still outstanding
        fixture.advance(WATCHDOG_MILLIS);
        assertThat(fixture.health.stopFailures).isEmpty(); // the one goal-driven retry is outstanding

        fixture.advance(WATCHDOG_MILLIS); // that retry blows its deadline too: the stop has failed
        assertThat(fixture.health.stopFailures)
                .singleElement()
                .asString()
                .contains("watchdog timeout while in WAITING_FOR_STOPPED");

        // Not once per watchdog period — the supervisor is told once and left alone.
        for (int period = 0; period < 20; period++) {
            fixture.advance(WATCHDOG_MILLIS);
        }
        assertThat(fixture.health.stopFailures).hasSize(1);
    }

    @Test
    void stoppingAnAdapterThatAlreadyFailedToStop_isAnsweredImmediately_notLeftHanging() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;
        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS);
        fixture.advance(WATCHDOG_MILLIS);
        assertThat(fixture.health.stopFailures).hasSize(1);

        // A discard or full recreate commanded after the machine settled: the manager sends StopAdapter and waits
        // for a stopped() the machine will never issue. It must be answered, not ignored.
        fixture.stopAdapter();

        assertThat(fixture.health.stopFailures).hasSize(2);
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.stopped).isEmpty();
    }

    @Test
    void stoppingAnAdapterThatSettledWhileItsGoalWantedConnected_isAnsweredToo() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;
        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS); // ERROR, one goal-driven retry issued and outstanding

        // A direction is re-activated while that retry is still in flight, so the machine settles in ERROR with the
        // goal wanting it connected: no stop failure is reported, because nothing is waiting on one yet.
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS);
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.stopFailures).isEmpty();

        // Now the adapter is discarded. The attempt was never marked failed, but it is spent — the machine will
        // issue nothing, so the discard must still be answered instead of hanging.
        fixture.stopAdapter();

        assertThat(fixture.health.stopFailures).hasSize(1);
        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.stopped).isEmpty();
    }

    @Test
    void aDisconnectThatIsNeverAcknowledged_alsoSettles_andReportsTheStopFailure() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .watchdogTimeoutMillis(WATCHDOG_MILLIS)
                .build();
        fixture.adapter.disconnectReply = MockProtocolAdapter.Reply.DROP;
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);

        // The route to STOPPED from CONNECTED goes through disconnect() first: its watchdog must not open a second
        // way to cycle forever.
        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS); // disconnect watchdog → ERROR, one goal-driven stop issued
        fixture.advance(WATCHDOG_MILLIS); // that stop's watchdog → settled

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.stopFailures).hasSize(1);
        assertNothingMoreHappens(fixture);
    }

    @Test
    void aStopThatIsHonored_isUnaffected_andTheNextLifeGetsItsOwnAttempt() {
        final WrapperTestFixture fixture = WrapperTestFixture.builder()
                .watchdogTimeoutMillis(WATCHDOG_MILLIS)
                .build();

        // First life: an ERROR the adapter can be stopped out of, then a fresh start.
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        fixture.output.started(); // unexpected in CONNECTED → defensive reset → ERROR
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(ERROR);
        fixture.stopAdapter();
        assertThat(fixture.state()).isEqualTo(STOPPED);

        // Second life: the budget spent stopping the first one must not carry over.
        fixture.activate(ProtocolAdapterDirection.NORTHBOUND);
        assertThat(fixture.state()).isEqualTo(CONNECTED);
        fixture.output.started();
        fixture.drain();
        assertThat(fixture.state()).isEqualTo(ERROR);
        fixture.stopAdapter();
        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.health.stopFailures).isEmpty();
    }

    @Test
    void aSlowButHonestStop_insideTheTwoAttemptBudget_stillReachesStopped() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;
        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS); // the watchdog resets to ERROR and re-commands the stop

        // The adapter was slow, not broken: its acknowledgment lands while the second attempt is still outstanding.
        // The budget is 2 × watchdog-timeout-millis, and a stop inside it is honored exactly as a prompt one is.
        fixture.output.stopped();
        fixture.drain();

        assertThat(fixture.state()).isEqualTo(STOPPED);
        assertThat(fixture.health.stopFailures).isEmpty();
        assertThat(fixture.health.stopped).containsExactly(fixture.adapterId);
    }

    @Test
    void aLateStoppedAfterSettling_isAbsorbed_notTreatedAsARecovery() {
        final WrapperTestFixture fixture = retryingFixtureCommandedToStop();
        fixture.adapter.stopReply = MockProtocolAdapter.Reply.DROP;
        fixture.deactivate(ProtocolAdapterDirection.NORTHBOUND);
        fixture.advance(WATCHDOG_MILLIS);
        fixture.advance(WATCHDOG_MILLIS);
        assertThat(fixture.state()).isEqualTo(ERROR);

        // The adapter finally answers, long after the supervisor was told the stop failed and may already have torn
        // it down. ERROR absorbs it: a late acknowledgment does not restore wrapper/adapter consistency, and
        // flipping to STOPPED here would contradict a teardown decision already taken on the manager's thread.
        fixture.output.stopped();
        fixture.drain();

        assertThat(fixture.state()).isEqualTo(ERROR);
        assertThat(fixture.health.stopped).isEmpty();
    }
}
