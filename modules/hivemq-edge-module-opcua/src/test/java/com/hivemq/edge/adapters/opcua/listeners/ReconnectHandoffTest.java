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
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The reconnect handoff, decided rather than raced.
 * <p>
 * Review-03 finding 10. The property — a reconnect produces exactly one condition refresh, whichever thread
 * arrives first — was checked by racing two threads a quarter of a million times. That is a probabilistic
 * oracle: the old defect was measured at roughly one hit per 70,000 rounds, so a 250,000-round run had about
 * {@code exp(-250000/70000) ≈ 3%} chance of observing nothing and passing against the very bug it was written
 * for. It also spent about fifteen seconds of every CI run testing scheduler luck.
 * <p>
 * With the decision extracted into {@link ReconnectHandoff} it is a pure function of state, so the orderings
 * are enumerable and each is asserted directly. What enumeration alone cannot show is that there is no
 * <em>third</em> ordering — every interleaving of two atomic steps is fine, so a test that cannot pause one
 * of them mid-step is assuming the atomicity it means to prove. That is what
 * {@link #neitherHalfCanRunWhileTheOtherIsMidTransition()} is for, and it is the one that fails against the
 * volatile-plus-AtomicBoolean implementation this replaced — every time, rather than three times in a
 * hundred.
 */
class ReconnectHandoffTest {

    @Test
    void whenTheCallbackIsInstalledFirst_thenTheReconnectRunsIt() {
        final ReconnectHandoff handoff = new ReconnectHandoff();
        final AtomicInteger refreshes = new AtomicInteger();

        assertThat(handoff.install(refreshes::incrementAndGet))
                .as("nothing is owed yet, so installing runs nothing")
                .isNull();
        run(handoff.reconnected());

        assertThat(refreshes).hasValue(1);
    }

    @Test
    void whenTheReconnectArrivesFirst_thenInstallingRunsIt() {
        // The window this exists for: the listener is registered as soon as the client exists, while the
        // handler it delegates to is built only after the subscription is established -- and for condition
        // tags that step makes blocking round trips per tag, so it is not small.
        final ReconnectHandoff handoff = new ReconnectHandoff();
        final AtomicInteger refreshes = new AtomicInteger();

        assertThat(handoff.reconnected())
                .as("there is nothing to call yet, so the debt is recorded instead")
                .isNull();
        run(handoff.install(refreshes::incrementAndGet));

        assertThat(refreshes).hasValue(1);
    }

    @Test
    void aDebtIsPaidOnceHoweverManyReconnectsRaisedIt() {
        // One flag, not a count: a ConditionRefresh is subscription-wide, so several reconnects before the
        // handler exists collapse into the one refresh that covers them all.
        final ReconnectHandoff handoff = new ReconnectHandoff();
        final AtomicInteger refreshes = new AtomicInteger();

        assertThat(handoff.reconnected()).isNull();
        assertThat(handoff.reconnected()).isNull();
        assertThat(handoff.reconnected()).isNull();
        run(handoff.install(refreshes::incrementAndGet));

        assertThat(refreshes).hasValue(1);
    }

    @Test
    void andIsNotPaidTwice() {
        // The debt is consumed by the install that claims it. A second install -- which the connection does
        // not do today, but nothing here should depend on that -- must not replay it.
        final ReconnectHandoff handoff = new ReconnectHandoff();
        final AtomicInteger refreshes = new AtomicInteger();

        assertThat(handoff.reconnected()).isNull();
        run(handoff.install(refreshes::incrementAndGet));
        assertThat(handoff.install(refreshes::incrementAndGet))
                .as("the debt was already settled")
                .isNull();

        assertThat(refreshes).hasValue(1);
    }

    @Test
    void everyLaterReconnectRunsTheInstalledCallback() {
        final ReconnectHandoff handoff = new ReconnectHandoff();
        final AtomicInteger refreshes = new AtomicInteger();

        assertThat(handoff.install(refreshes::incrementAndGet)).isNull();
        run(handoff.reconnected());
        run(handoff.reconnected());
        run(handoff.reconnected());

        assertThat(refreshes).hasValue(3);
    }

    @Test
    void neitherHalfCanRunWhileTheOtherIsMidTransition() throws Exception {
        // The assumption every test above rests on, and the only one that distinguishes this implementation
        // from the one it replaced. The defect was never a wrong ordering -- both orderings are correct --
        // it was that the two halves could interleave:
        //
        //   1. the session thread reads the callback and finds it null
        //   2. the connection thread stores the callback
        //   3. the connection thread tests the owed flag, still false, and returns
        //   4. the session thread sets the owed flag
        //
        // leaving a debt recorded against a callback already installed, with nobody to run it. Here the
        // session thread is held at step 1 and the connection thread is shown to be unable to reach step 2.
        final CountDownLatch inside = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final ReconnectHandoff handoff = new ReconnectHandoff() {
            @Override
            void insideCriticalSection() {
                // Only the first entrant pauses; the one released below must not park again.
                if (inside.getCount() > 0) {
                    inside.countDown();
                    try {
                        assertThat(release.await(30, TimeUnit.SECONDS)).isTrue();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError("interrupted inside the critical section", e);
                    }
                }
            }
        };

        final AtomicInteger refreshes = new AtomicInteger();
        final ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            final Future<?> session = threads.submit(() -> run(handoff.reconnected()));
            assertThat(inside.await(30, TimeUnit.SECONDS))
                    .as("the session thread should be paused inside its critical section")
                    .isTrue();

            final Future<?> connection = threads.submit(() -> run(handoff.install(refreshes::incrementAndGet)));

            // The heart of it. With two atomics this completes immediately -- storing the callback and
            // reading a not-yet-set flag, which is steps 2 and 3 of the interleaving above.
            assertThat(awaitCompletion(connection))
                    .as("installing must not proceed while a reconnect is mid-transition")
                    .isFalse();

            release.countDown();
            session.get(30, TimeUnit.SECONDS);
            connection.get(30, TimeUnit.SECONDS);

            assertThat(refreshes)
                    .as("and the reconnect is still honoured exactly once")
                    .hasValue(1);
        } finally {
            release.countDown();
            threads.shutdownNow();
        }
    }

    /** Whether a task finishes within a window long enough to be sure it is blocked rather than merely slow. */
    private static boolean awaitCompletion(final @NotNull Future<?> task) throws Exception {
        try {
            task.get(500, TimeUnit.MILLISECONDS);
            return true;
        } catch (final java.util.concurrent.TimeoutException expected) {
            return false;
        }
    }

    /** Runs what the handoff answered with, which is null when nothing is owed. */
    private static void run(final Runnable owed) {
        if (owed != null) {
            owed.run();
        }
    }
}
