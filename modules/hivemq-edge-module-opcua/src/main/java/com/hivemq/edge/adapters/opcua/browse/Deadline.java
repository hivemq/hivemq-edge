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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jetbrains.annotations.NotNull;

/**
 * The point in time by which a browse must be done, and every wait measured against it: the permit, each
 * Browse and BrowseNext response, the pauses between retries. Having one type for it is what makes "no wait
 * outlives the budget" checkable at a glance instead of at four call sites.
 *
 * @param nanos the deadline on the {@link System#nanoTime()} clock
 */
record Deadline(long nanos) {

    static @NotNull Deadline after(final long seconds) {
        return new Deadline(System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds));
    }

    /** Nanoseconds left, never below 1 so that a timed wait on an expired deadline still returns promptly. */
    long remainingNanos() {
        return Math.max(1, nanos - System.nanoTime());
    }

    long remainingMillis() {
        return TimeUnit.NANOSECONDS.toMillis(remainingNanos());
    }

    /** Waits for a permit at most until the deadline; interruptible. */
    boolean tryAcquire(final @NotNull Semaphore permit) throws InterruptedException {
        return permit.tryAcquire(remainingNanos(), TimeUnit.NANOSECONDS);
    }

    /** The future's value, or {@link TimeoutException} when the deadline passes first; interruptible. */
    <T> T await(final @NotNull CompletableFuture<T> future)
            throws ExecutionException, InterruptedException, TimeoutException {
        return future.get(remainingNanos(), TimeUnit.NANOSECONDS);
    }
}
