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
import com.hivemq.protocols.v2.tag.SouthboundWriteCompletion;
import org.jetbrains.annotations.NotNull;

/**
 * A southbound write to deliver to a tag's write aspect — the "write arrives" trigger, told to the
 * wrapper mailbox by an external producer (a future southbound mapping; tests drive it directly). The {@link Node}
 * is the correlation key; the wrapper routes the value to that node's write aspect, which requests the write when
 * it is resting at {@code WAITING_FOR_WRITE_REQUEST} and rejects it otherwise (one write in flight at a time — the
 * aspect never queues).
 * <p>
 * The {@code completion} is the option-D back-pressure seam: it is settled once with the write's terminal outcome
 * (or immediately, rejected, when one is already in flight), so a producer can hold the next write until the
 * current one completes and let the backlog wait in its durable queue. Fire-and-forget callers that do not
 * back-pressure use the {@code (node, value)} form, which discards the outcome.
 * <p>
 * Band: {@link MailboxMessagePriority#DATA} — like data points, southbound payload yields to control,
 * acknowledgments, and time. It is its own kind of {@link ProtocolAdapterWrapperMessage}: it is not
 * a goal command, not a protocol-adapter event (it never flows through the adapter transition table), and not a
 * tick.
 *
 * @param node       the node to write to.
 * @param value      the reused v1 value to write.
 * @param completion the one-shot back-pressure signal, settled with the write's outcome.
 */
public record ProtocolAdapterWrapperWriteRequest(
        @NotNull Node node,
        @NotNull DataPoint value,
        @NotNull SouthboundWriteCompletion completion) implements ProtocolAdapterWrapperMessage {

    /**
     * A fire-and-forget write that discards its outcome — for callers that do not back-pressure.
     *
     * @param node  the node to write to.
     * @param value the reused v1 value to write.
     */
    public ProtocolAdapterWrapperWriteRequest(final @NotNull Node node, final @NotNull DataPoint value) {
        this(node, value, SouthboundWriteCompletion.IGNORED);
    }

    @Override
    public @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.DATA;
    }
}
