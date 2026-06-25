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
package com.hivemq.protocols.v2.runtime;

import org.jetbrains.annotations.NotNull;

/**
 * Stateful exponential backoff over a {@link RetryPolicy}. It is owned and stepped by a single
 * actor on its dispatch thread; it does no scheduling itself — the caller schedules a timer for
 * {@link #nextDelayMillis()} on the actor's {@link PriorityTimerQueue}.
 * <p>
 * With {@link RetryPolicy#defaults()} the delays are 1000, 1410, 1988, 2803, &hellip; each successive delay being
 * the previous one times the policy factor, rounded to the nearest millisecond and capped at the policy ceiling.
 */
public final class Backoff {

    private final @NotNull RetryPolicy policy;
    private long nextDelayMillis;
    private int retriesTaken;

    public Backoff(final @NotNull RetryPolicy policy) {
        this.policy = policy;
        reset();
    }

    /**
     * Hand out the next delay and advance the sequence. Counts as one retry against
     * {@link RetryPolicy#maximumRetries()}.
     *
     * @return the delay to wait before the next attempt, in milliseconds.
     */
    public long nextDelayMillis() {
        final long delay = nextDelayMillis;
        retriesTaken++;
        nextDelayMillis = Math.min(Math.round(delay * policy.factor()), policy.ceilingMillis());
        return delay;
    }

    /**
     * Return to the start of the sequence: the next {@link #nextDelayMillis()} yields
     * {@link RetryPolicy#initialMillis()} again and the retry count is cleared. Called when an attempt succeeds.
     */
    public void reset() {
        nextDelayMillis = policy.initialMillis();
        retriesTaken = 0;
    }

    /**
     * @return {@code true} once {@link RetryPolicy#maximumRetries()} delays have been handed out — the point at
     *         which the caller stops retrying and escalates (for the adapter machine: to {@code ERROR}).
     */
    public boolean exhausted() {
        return retriesTaken >= policy.maximumRetries();
    }
}
