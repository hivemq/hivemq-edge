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

import java.util.PriorityQueue;
import org.jetbrains.annotations.NotNull;

/**
 * The single timer queue an actor owns (design §5.5): every time-based event — connection backoff, per-node poll
 * schedule, subscription retry, verification retry, watchdog — is one entry sorted by fire time. On a tick the
 * actor calls {@link #fireDue(long)} on its <b>own dispatch thread</b>; each due timer runs its callback, which
 * feeds a typed event to the state machine.
 * <p>
 * This queue is unrelated to the mailbox's priority bands: timer expiries are not mailbox messages — they are
 * generated inside the tick handler, on the dispatch thread, and never queue. Every method is owner-thread only;
 * the queue does no locking.
 * <p>
 * Cancellation is lazy: {@link #cancel(TimerHandle)} flags the entry and {@link #size()} stops counting it
 * immediately, but the entry is discarded only when it surfaces at the head during {@link #fireDue(long)}.
 */
public final class PriorityTimerQueue {

    private final @NotNull PriorityQueue<ScheduledTimer> timers = new PriorityQueue<>();
    private long sequenceCounter;
    private int activeCount;

    /**
     * Schedule {@code onFire} to run the next time {@link #fireDue(long)} is called with a {@code nowMillis} at or
     * past {@code fireAtMillis}.
     *
     * @param fireAtMillis the absolute time, in {@link Clock#nowMillis()} units, at or after which to fire.
     * @param onFire       the callback; runs on the actor's dispatch thread inside {@link #fireDue(long)}.
     * @return a handle for cancelling the timer.
     */
    public @NotNull TimerHandle schedule(final long fireAtMillis, final @NotNull Runnable onFire) {
        final ScheduledTimer timer = new ScheduledTimer(fireAtMillis, sequenceCounter++, onFire);
        timers.add(timer);
        activeCount++;
        return timer;
    }

    /**
     * Cancel a pending timer. Cancelling an already-fired or already-cancelled handle, or a handle from another
     * queue, is a no-op.
     *
     * @param handle the handle returned by {@link #schedule(long, Runnable)}.
     */
    public void cancel(final @NotNull TimerHandle handle) {
        if (handle instanceof final ScheduledTimer timer && timer.active) {
            timer.active = false;
            activeCount--;
        }
    }

    /**
     * Pop and run every entry whose fire time is at or before {@code nowMillis}, in fire-time order (ties broken
     * by schedule order). A callback may itself {@link #schedule(long, Runnable)} or {@link #cancel(TimerHandle)};
     * a freshly scheduled timer that is already due fires within the same call.
     *
     * @param nowMillis the current time, in {@link Clock#nowMillis()} units.
     */
    public void fireDue(final long nowMillis) {
        while (true) {
            final ScheduledTimer next = timers.peek();
            if (next == null || next.fireAtMillis > nowMillis) {
                break;
            }
            timers.poll();
            if (next.active) {
                next.active = false;
                activeCount--;
                next.onFire.run();
            }
        }
    }

    /**
     * @return the number of pending (scheduled, not yet fired or cancelled) timers.
     */
    public int size() {
        return activeCount;
    }

    private static final class ScheduledTimer implements TimerHandle, Comparable<ScheduledTimer> {

        private final long fireAtMillis;
        private final long sequence;
        private final @NotNull Runnable onFire;
        private boolean active = true;

        private ScheduledTimer(final long fireAtMillis, final long sequence, final @NotNull Runnable onFire) {
            this.fireAtMillis = fireAtMillis;
            this.sequence = sequence;
            this.onFire = onFire;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public int compareTo(final @NotNull ScheduledTimer other) {
            final int byFireTime = Long.compare(fireAtMillis, other.fireAtMillis);
            return byFireTime != 0 ? byFireTime : Long.compare(sequence, other.sequence);
        }
    }
}
