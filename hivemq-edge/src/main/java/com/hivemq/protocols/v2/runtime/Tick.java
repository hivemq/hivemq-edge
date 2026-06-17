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
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import org.jetbrains.annotations.NotNull;

/**
 * A generic tick — time delivered as a message (D6 / design §5.4). The {@link Clock} tells one of these (or an
 * actor-specific tick) to an actor's mailbox every period; the actor drains its {@link PriorityTimerQueue} from
 * its own dispatch thread when it handles the tick.
 * <p>
 * The band is {@link MailboxMessagePriority#TICK}: time stays on time under data floods (above
 * {@link MailboxMessagePriority#DATA}) but never overtakes an already-enqueued acknowledgment (below
 * {@link MailboxMessagePriority#EVENT}), so a watchdog fires only when the awaited reply truly has not arrived.
 * <p>
 * Long-lived actors that carry extra context in their tick (the wrapper, the manager) declare their own tick
 * record in their own package; this generic {@code Tick} is what the bare {@link Clock} schedules by default and
 * what the runtime's own timing tests drive.
 *
 * @param nowMillis the clock time, in milliseconds, at which this tick was produced; the actor compares it to
 *                  {@link Clock#nowMillis()} at processing time to measure tick lag.
 */
public record Tick(long nowMillis) implements MailboxMessage {

    @Override
    public @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.TICK;
    }
}
