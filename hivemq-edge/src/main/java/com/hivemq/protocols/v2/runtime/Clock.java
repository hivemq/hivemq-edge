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
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * Time, abstracted so the framework can be driven deterministically in tests (design §5.4).
 * <p>
 * Time enters an actor as a <b>message</b>, never as a callback that touches actor state: the clock's only job is
 * to {@link MailboxSender#tell(MailboxMessage) tell} a tick to the actor's mailbox every period. The actor then
 * fires its own {@link PriorityTimerQueue} on its own dispatch thread when it handles that tick — the clock never
 * fires a timer itself.
 * <p>
 * The caller supplies how to build its own tick message, so the scheduler stays generic: production uses
 * {@link SystemClock}; tests use {@link FakeClock}.
 */
public interface Clock {

    /**
     * @return the current time in milliseconds. In production this is wall-clock time; in tests it is the value
     *         advanced explicitly via {@link FakeClock#advance(long)}.
     */
    long nowMillis();

    /**
     * Periodically tell the target a tick message (~50 ms in production).
     *
     * @param periodMillis  the interval between ticks, in milliseconds; must be positive.
     * @param target        the send-only handle of the actor mailbox that receives the ticks.
     * @param tickMessage   builds a fresh tick each period; invoked once per tick, on the scheduler's thread.
     * @param <MessageType> the actor's tick message type.
     * @return a handle that stops this periodic tick when closed; closing it never affects other schedules.
     */
    <MessageType extends MailboxMessage> @NotNull AutoCloseable scheduleTick(
            long periodMillis, @NotNull MailboxSender<MessageType> target, @NotNull Supplier<MessageType> tickMessage);
}
