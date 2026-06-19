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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import org.jetbrains.annotations.NotNull;

/**
 * A southbound write to deliver to a tag's write aspect (design §7.5) — the "write arrives" trigger, told to the
 * wrapper mailbox by an external producer (a future southbound mapping; tests drive it directly). The {@link Node}
 * is the correlation key; the wrapper routes the value to that node's write aspect, which requests the write when
 * it is resting at {@code WAITING_FOR_WRITE_REQUEST} and drops it otherwise (one write in flight at a time).
 * <p>
 * Band: {@link MailboxMessagePriority#DATA} — like data points, southbound payload yields to control,
 * acknowledgments, and time (design §5.1). It is its own kind of {@link ProtocolAdapterWrapperMessage}: it is not
 * a goal command, not a protocol-adapter event (it never flows through the adapter transition table), and not a
 * tick.
 *
 * @param node  the node to write to.
 * @param value the reused v1 value to write.
 */
public record ProtocolAdapterWrapperWriteRequest(
        @NotNull Node node, @NotNull DataPoint value) implements ProtocolAdapterWrapperMessage {

    @Override
    public @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.DATA;
    }
}
