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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class PriorityTimerQueueTest {

    @Test
    void firesDueTimersInFireTimeOrder_tiesBrokenByScheduleOrder() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        final List<String> fired = new ArrayList<>();
        queue.schedule(100, () -> fired.add("a@100"));
        queue.schedule(100, () -> fired.add("b@100"));
        queue.schedule(50, () -> fired.add("c@50"));

        queue.fireDue(100);

        assertThat(fired).containsExactly("c@50", "a@100", "b@100");
        assertThat(queue.size()).isZero();
    }

    @Test
    void fireDue_boundaryIsInclusive() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        final AtomicBoolean fired = new AtomicBoolean();
        queue.schedule(100, () -> fired.set(true));

        queue.fireDue(99);
        assertThat(fired).isFalse();
        assertThat(queue.size()).isEqualTo(1);

        queue.fireDue(100);
        assertThat(fired).isTrue();
        assertThat(queue.size()).isZero();
    }

    @Test
    void reentrantSchedule_firesWhenTheNewTimerIsAlreadyDue() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        final List<String> fired = new ArrayList<>();
        queue.schedule(50, () -> {
            fired.add("first@50");
            queue.schedule(40, () -> fired.add("reentrant@40"));
        });

        queue.fireDue(100);

        assertThat(fired).containsExactly("first@50", "reentrant@40");
        assertThat(queue.size()).isZero();
    }

    @Test
    void cancel_preventsFiringAndUpdatesSizeAndIsActive() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        final AtomicBoolean fired = new AtomicBoolean();
        final TimerHandle handle = queue.schedule(50, () -> fired.set(true));
        assertThat(handle.isActive()).isTrue();
        assertThat(queue.size()).isEqualTo(1);

        queue.cancel(handle);

        assertThat(handle.isActive()).isFalse();
        assertThat(queue.size()).isZero();
        queue.fireDue(100);
        assertThat(fired).isFalse();
    }

    @Test
    void cancelDuringFireDue_skipsTheCancelledTimer() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        final List<String> fired = new ArrayList<>();
        final AtomicReference<TimerHandle> later = new AtomicReference<>();
        queue.schedule(50, () -> {
            fired.add("first@50");
            queue.cancel(later.get());
        });
        later.set(queue.schedule(60, () -> fired.add("later@60")));

        queue.fireDue(100);

        assertThat(fired).containsExactly("first@50");
        assertThat(queue.size()).isZero();
    }

    @Test
    void handle_isInactiveAfterFiring() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        final TimerHandle handle = queue.schedule(50, () -> {});

        queue.fireDue(50);

        assertThat(handle.isActive()).isFalse();
    }

    @Test
    void cancellingAnAlreadyFiredOrForeignHandle_isANoOp() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        final TimerHandle handle = queue.schedule(50, () -> {});
        queue.fireDue(50);

        queue.cancel(handle);
        assertThat(queue.size()).isZero();

        final TimerHandle foreign = () -> true;
        queue.cancel(foreign);
        assertThat(queue.size()).isZero();
    }

    @Test
    void size_countsOnlyPendingTimers() {
        final PriorityTimerQueue queue = new PriorityTimerQueue();
        assertThat(queue.size()).isZero();
        queue.schedule(10, () -> {});
        queue.schedule(20, () -> {});
        assertThat(queue.size()).isEqualTo(2);
        queue.fireDue(10);
        assertThat(queue.size()).isEqualTo(1);
    }
}
