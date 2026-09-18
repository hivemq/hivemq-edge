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
package com.hivemq.edge.adapters.opcua.browse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** {@link Deadline}: every wait of a browse measured against one point in time. */
class DeadlineTest {

    @Test
    void after_liesThatManySecondsAhead() {
        final long before = System.nanoTime();
        final Deadline deadline = Deadline.after(10);

        assertThat(deadline.nanos() - before).isBetween(TimeUnit.SECONDS.toNanos(9), TimeUnit.SECONDS.toNanos(11));
        assertThat(deadline.remainingMillis()).isBetween(9_000L, 10_000L);
    }

    @Test
    void remaining_neverBelowOneNanosecond() {
        final Deadline passed = new Deadline(System.nanoTime() - TimeUnit.SECONDS.toNanos(5));

        assertThat(passed.remainingNanos()).isEqualTo(1);
        assertThat(passed.remainingMillis()).isZero();
    }

    @Test
    void tryAcquire_freePermit_immediately() throws Exception {
        final Semaphore permit = new Semaphore(1);

        assertThat(Deadline.after(120).tryAcquire(permit)).isTrue();
        assertThat(permit.availablePermits()).isZero();
    }

    @Test
    void tryAcquire_heldPermit_givesUpAtTheDeadline() throws Exception {
        final Semaphore permit = new Semaphore(0);

        final long start = System.nanoTime();
        assertThat(new Deadline(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200)).tryAcquire(permit))
                .isFalse();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isBetween(150L, 5_000L);
    }

    @Test
    void tryAcquire_expiredDeadline_returnsAtOnce() throws Exception {
        final Semaphore permit = new Semaphore(0);

        final long start = System.nanoTime();
        assertThat(new Deadline(System.nanoTime() - 1).tryAcquire(permit)).isFalse();
        assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(1_000);
    }

    @Test
    void tryAcquire_isInterruptible() throws Exception {
        final Semaphore permit = new Semaphore(0);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final Thread waiter = new Thread(() -> {
            try {
                Deadline.after(120).tryAcquire(permit);
            } catch (final Throwable t) {
                failure.set(t);
            }
        });
        waiter.start();
        Thread.sleep(200);
        waiter.interrupt();
        waiter.join(5_000);

        assertThat(waiter.isAlive()).isFalse();
        assertThat(failure.get()).isInstanceOf(InterruptedException.class);
    }

    @Test
    void await_completedFuture_returnsItsValue() throws Exception {
        assertThat(Deadline.after(120).await(CompletableFuture.completedFuture("v")))
                .isEqualTo("v");
    }

    @Test
    void await_pendingFuture_timesOutAtTheDeadline() {
        final Deadline soon = new Deadline(System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(200));

        assertThatThrownBy(() -> soon.await(new CompletableFuture<String>())).isInstanceOf(TimeoutException.class);
    }

    @Test
    void await_failedFuture_propagatesTheCause() {
        final RuntimeException boom = new RuntimeException("boom");

        assertThatThrownBy(() -> Deadline.after(120).await(CompletableFuture.failedFuture(boom)))
                .isInstanceOf(ExecutionException.class)
                .cause()
                .isSameAs(boom);
    }

    @Test
    void await_expiredDeadline_stillReturnsACompletedFuture() throws Exception {
        // Milo's fake in the unit tests completes synchronously; get(1 ns) on a done future never waits.
        assertThat(new Deadline(System.nanoTime() - 1).await(CompletableFuture.completedFuture(7)))
                .isEqualTo(7);
    }
}
