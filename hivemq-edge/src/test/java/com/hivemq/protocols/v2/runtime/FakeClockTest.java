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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class FakeClockTest {

    @Test
    void advance_tellsExactlyTheDueTicks_stampedWithTheirFireTime() {
        final FakeClock clock = new FakeClock();
        final DefaultMailbox<Tick> mailbox = new DefaultMailbox<>();
        clock.scheduleTick(50, mailbox, () -> new Tick(clock.nowMillis()));

        clock.advance(120);

        assertThat(clock.nowMillis()).isEqualTo(120);
        assertThat(drainTickTimes(mailbox)).containsExactly(50L, 100L);
    }

    @Test
    void advance_belowTheFirstFireTime_tellsNothing() {
        final FakeClock clock = new FakeClock();
        final DefaultMailbox<Tick> mailbox = new DefaultMailbox<>();
        clock.scheduleTick(50, mailbox, () -> new Tick(clock.nowMillis()));

        clock.advance(30);
        assertThat(clock.nowMillis()).isEqualTo(30);
        assertThat(mailbox.isEmpty()).isTrue();

        clock.advance(20);
        assertThat(drainTickTimes(mailbox)).containsExactly(50L);
    }

    @Test
    void advance_interleavesMultipleSchedulesInFireTimeOrder() {
        final FakeClock clock = new FakeClock();
        final DefaultMailbox<Tick> mailbox = new DefaultMailbox<>();
        clock.scheduleTick(50, mailbox, () -> new Tick(clock.nowMillis()));
        clock.scheduleTick(30, mailbox, () -> new Tick(clock.nowMillis()));

        clock.advance(100);

        assertThat(drainTickTimes(mailbox)).containsExactly(30L, 50L, 60L, 90L, 100L);
    }

    @Test
    void closingAScheduleStopsItsTicks() throws Exception {
        final FakeClock clock = new FakeClock();
        final DefaultMailbox<Tick> mailbox = new DefaultMailbox<>();
        final AutoCloseable handle = clock.scheduleTick(50, mailbox, () -> new Tick(clock.nowMillis()));

        handle.close();
        clock.advance(1000);

        assertThat(mailbox.isEmpty()).isTrue();
    }

    @Test
    void initialTime_offsetsTheFirstFire() {
        final FakeClock clock = new FakeClock(1000);
        final DefaultMailbox<Tick> mailbox = new DefaultMailbox<>();
        clock.scheduleTick(50, mailbox, () -> new Tick(clock.nowMillis()));

        clock.advance(60);

        assertThat(drainTickTimes(mailbox)).containsExactly(1050L);
    }

    @Test
    void advance_neverFiresAnActorTimerQueue() {
        final FakeClock clock = new FakeClock();
        final PriorityTimerQueue timers = new PriorityTimerQueue();
        final AtomicBoolean fired = new AtomicBoolean();
        timers.schedule(50, () -> fired.set(true));

        // The clock is not wired to the queue: only an actor's tick handler drains timers.
        clock.advance(1000);

        assertThat(fired).isFalse();
        assertThat(timers.size()).isEqualTo(1);
    }

    @Test
    void rejectsNonPositivePeriodAndNegativeAdvance() {
        final FakeClock clock = new FakeClock();
        final DefaultMailbox<Tick> mailbox = new DefaultMailbox<>();

        assertThatThrownBy(() -> clock.scheduleTick(0, mailbox, () -> new Tick(0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> clock.advance(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static @NotNull List<Long> drainTickTimes(final @NotNull DefaultMailbox<Tick> mailbox) {
        final List<Long> times = new ArrayList<>();
        Tick tick = mailbox.poll();
        while (tick != null) {
            times.add(tick.nowMillis());
            tick = mailbox.poll();
        }
        return times;
    }
}
