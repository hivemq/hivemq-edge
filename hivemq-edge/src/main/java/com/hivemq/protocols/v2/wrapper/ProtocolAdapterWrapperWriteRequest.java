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
 * A southbound write to deliver to a tag's write aspect — the "write arrives" trigger, told to the wrapper mailbox
 * by the channel in front of the aspect ({@link com.hivemq.protocols.v2.southbound.SouthboundWriteQueue}). The
 * {@link Node} is the correlation key; the wrapper routes the value to that node's write aspect, which requests the
 * write when it is resting at {@code WAITING_FOR_WRITE_REQUEST} and settles it immediately otherwise (one write in
 * flight at a time — the aspect never queues).
 * <p>
 * The request carries a {@code deliveryToken}, not a completion callback. The aspect echoes the token back in a
 * {@link ProtocolAdapterWrapperSouthboundMessage.WriteSettled} message when the write reaches a terminal outcome,
 * which is what lets the delivering channel tell its live delivery's answer from a superseded one: an abandoned
 * write's late acknowledgment must never dispose the command its own redelivery is still working on.
 * <p>
 * Band: {@link MailboxMessagePriority#DATA} — like data points, southbound payload yields to control,
 * acknowledgments, and time. Critically it shares that band with every
 * {@link ProtocolAdapterWrapperSouthboundMessage}, so requests and settlements are seen in the order they were
 * emitted.
 *
 * @param node          the node to write to — how the write aspect is found.
 * @param tagName       the tag the write is for — how the delivering channel is found. Both are carried because
 *                      the two sides of the path index by different keys, and the outcome must be addressable back
 *                      to the channel even when no aspect owns the node.
 * @param value         the reused v1 value to write.
 * @param deliveryToken the delivering channel's correlation for this delivery, echoed in the settlement.
 */
public record ProtocolAdapterWrapperWriteRequest(
        @NotNull Node node,
        @NotNull String tagName,
        @NotNull DataPoint value,
        long deliveryToken) implements ProtocolAdapterWrapperMessage {

    /**
     * The token of a write nobody is pacing — a test rig or an interim producer that submits a write without
     * tracking its outcome. The aspect still settles it (every write settles exactly once, always); no channel
     * recognizes the token, so the settlement is dropped where it lands.
     */
    public static final long UNTRACKED = -1L;

    /**
     * A write whose outcome nobody is waiting for.
     *
     * @param node    the node to write to.
     * @param tagName the tag the write is for.
     * @param value   the reused v1 value to write.
     */
    public ProtocolAdapterWrapperWriteRequest(
            final @NotNull Node node, final @NotNull String tagName, final @NotNull DataPoint value) {
        this(node, tagName, value, UNTRACKED);
    }

    @Override
    public @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.DATA;
    }
}
