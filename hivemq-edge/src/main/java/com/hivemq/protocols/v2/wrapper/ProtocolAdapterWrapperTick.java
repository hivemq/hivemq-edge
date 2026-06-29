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
package com.hivemq.protocols.v2.wrapper;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import org.jetbrains.annotations.NotNull;

/**
 * The wrapper's tick — time delivered as a message. The clock tells one of these to the
 * wrapper's mailbox every period; when the wrapper handles it, it drains its timer queue and dispatches pending
 * batches, on its own dispatch thread.
 * <p>
 * Band {@link MailboxMessagePriority#TICK}: above {@code DATA} (time stays on time under data floods) but below
 * {@code EVENT} (an already-enqueued acknowledgment is processed, and its watchdog canceled, before the tick that
 * would have fired it).
 *
 * @param nowMillis the clock time, in milliseconds, at which this tick was produced.
 */
public record ProtocolAdapterWrapperTick(long nowMillis) implements ProtocolAdapterWrapperMessage {

    @Override
    public @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.TICK;
    }
}
