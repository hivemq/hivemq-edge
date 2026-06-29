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

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Deterministic test {@link Clock}. {@link #advance(long)} moves {@link #nowMillis()} forward and tells the due
 * tick messages, in fire-time order — <b>nothing else</b>.
 * <p>
 * The clock never fires a timer: a {@link PriorityTimerQueue} is drained exclusively inside the owning actor's
 * tick handler, on the dispatch thread. Pairing a {@code FakeClock} with a {@link ManualDispatcher} gives a fully
 * deterministic, sleep-free test: advancing the clock enqueues ticks, and draining the dispatcher delivers them.
 * <p>
 * Before each tick is told, {@link #nowMillis()} is set to that tick's fire time, so a supplier such as
 * {@code () -> new Tick(clock.nowMillis())} stamps the correct logical time. Not thread-safe: drive it from one
 * test thread.
 */
public final class FakeClock implements Clock {

    private long nowMillis;
    private final @NotNull List<TickRegistration<?>> registrations = new ArrayList<>();

    public FakeClock() {
        this(0L);
    }

    public FakeClock(final long initialMillis) {
        this.nowMillis = initialMillis;
    }

    @Override
    public long nowMillis() {
        return nowMillis;
    }

    @Override
    public <MessageType extends MailboxMessage> @NotNull AutoCloseable scheduleTick(
            final long periodMillis,
            final @NotNull MailboxSender<MessageType> target,
            final @NotNull Supplier<MessageType> tickMessage) {
        if (periodMillis <= 0) {
            throw new IllegalArgumentException("tick period must be positive, but was " + periodMillis);
        }
        final TickRegistration<MessageType> registration =
                new TickRegistration<>(periodMillis, target, tickMessage, nowMillis + periodMillis);
        registrations.add(registration);
        return () -> registration.active = false;
    }

    /**
     * Move time forward by {@code millis}, telling every tick that becomes due along the way in fire-time order
     * (ties broken by schedule order). {@link #nowMillis()} is advanced to each tick's fire time before that tick
     * is told, then to the final target time once no tick remains due.
     *
     * @param millis the duration to advance by, in milliseconds; must not be negative.
     */
    public void advance(final long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("cannot advance by a negative duration: " + millis);
        }
        final long targetMillis = nowMillis + millis;
        while (true) {
            final TickRegistration<?> earliest = earliestDueRegistration(targetMillis);
            if (earliest == null) {
                break;
            }
            nowMillis = earliest.nextFireAtMillis;
            earliest.fire();
            earliest.nextFireAtMillis += earliest.periodMillis;
        }
        nowMillis = targetMillis;
    }

    private @Nullable TickRegistration<?> earliestDueRegistration(final long targetMillis) {
        TickRegistration<?> earliest = null;
        // index-based so a tick that re-schedules during fire() (appending) cannot throw a concurrent modification
        for (int i = 0; i < registrations.size(); i++) {
            final TickRegistration<?> registration = registrations.get(i);
            if (!registration.active || registration.nextFireAtMillis > targetMillis) {
                continue;
            }
            if (earliest == null || registration.nextFireAtMillis < earliest.nextFireAtMillis) {
                earliest = registration;
            }
        }
        return earliest;
    }

    private static final class TickRegistration<MessageType extends MailboxMessage> {

        private final long periodMillis;
        private final @NotNull MailboxSender<MessageType> target;
        private final @NotNull Supplier<MessageType> tickMessage;
        private long nextFireAtMillis;
        private boolean active = true;

        private TickRegistration(
                final long periodMillis,
                final @NotNull MailboxSender<MessageType> target,
                final @NotNull Supplier<MessageType> tickMessage,
                final long firstFireAtMillis) {
            this.periodMillis = periodMillis;
            this.target = target;
            this.tickMessage = tickMessage;
            this.nextFireAtMillis = firstFireAtMillis;
        }

        private void fire() {
            target.tell(tickMessage.get());
        }
    }
}
