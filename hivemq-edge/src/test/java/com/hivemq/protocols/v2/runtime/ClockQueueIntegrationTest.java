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

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * S24 precursor: timers scheduled on an actor's queue fire in order, inside that actor's tick handler, as the
 * clock advances past them — proving the clock-never-fires / drain-on-tick contract end to end with a
 * {@link FakeClock} + {@link ManualDispatcher} (no real threads, no sleeps).
 */
class ClockQueueIntegrationTest {

    @Test
    void timersFireInOrderInsideTheTickHandler_asTheClockAdvances() {
        final FakeClock clock = new FakeClock();
        final ManualDispatcher dispatcher = new ManualDispatcher();
        final PriorityTimerQueue timers = new PriorityTimerQueue();
        final DefaultMailbox<Tick> mailbox = new DefaultMailbox<>();
        dispatcher.attach(mailbox, new TickHandlingActor(timers));

        final List<Long> fired = new ArrayList<>();
        timers.schedule(1000, () -> fired.add(1000L));
        timers.schedule(1400, () -> fired.add(1400L));
        timers.schedule(2000, () -> fired.add(2000L));

        clock.scheduleTick(100, mailbox, () -> new Tick(clock.nowMillis()));
        clock.advance(2000);
        dispatcher.drainAll();

        assertThat(fired).containsExactly(1000L, 1400L, 2000L);
        assertThat(timers.size()).isZero();
    }

    /** A minimal actor that owns a timer queue and drains it from its own tick handler. */
    private static final class TickHandlingActor implements MessageHandler<Tick> {

        private final @NotNull PriorityTimerQueue timers;

        private TickHandlingActor(final @NotNull PriorityTimerQueue timers) {
            this.timers = timers;
        }

        @Override
        public void receive(final @NotNull Tick tick) {
            timers.fireDue(tick.nowMillis());
        }
    }
}
