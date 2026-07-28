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
import com.hivemq.protocols.v2.southbound.SouthboundCommand;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Everything the southbound delivery side learns, expressed as mailbox messages so the delivery state can live on
 * the wrapper's single dispatch thread and be reasoned about as ordinary single-threaded code.
 * <p>
 * Two of these come from the write aspect (which already runs on that thread and could call the delivery side
 * directly): routing them through the mailbox keeps the aspect ignorant of the delivery side entirely and removes
 * every re-entrancy question. Three come from off-thread — the broker's persistence futures and its
 * publish-available callback — through shims that do nothing but {@code tell} one of these and never touch
 * delivery state.
 * <p>
 * <b>Every one of these is {@link MailboxMessagePriority#DATA}, and that is load-bearing.</b> The mailbox is
 * strict FIFO <i>within</i> a priority band and drains higher bands first, so messages emitted in a meaningful
 * order — the aspect settling a write and then reporting itself unwritable — are only seen in that order while
 * they share a band. Promoting any one of these to a higher priority would silently invert that.
 */
public sealed interface ProtocolAdapterWrapperSouthboundMessage extends ProtocolAdapterWrapperMessage {

    @Override
    default @NotNull MailboxMessagePriority priority() {
        return MailboxMessagePriority.DATA;
    }

    /**
     * The write aspect reached a terminal outcome for the write it was given.
     *
     * @param tagName       the tag whose write settled.
     * @param deliveryToken the token the delivering channel stamped on the request; a settle whose token is not
     *                      the channel's current one belongs to a superseded delivery and is ignored.
     * @param outcome       the terminal outcome.
     * @param reason        the device's own words, or {@code null}.
     */
    record WriteSettled(
            @NotNull String tagName,
            long deliveryToken,
            @NotNull SouthboundWriteOutcome outcome,
            @Nullable String reason)
            implements ProtocolAdapterWrapperSouthboundMessage {}

    /**
     * The write aspect crossed its writability boundary — the signal that opens and closes a delivery window.
     *
     * @param tagName  the tag whose write aspect moved.
     * @param writable whether it can now accept a write.
     */
    record TagWritability(@NotNull String tagName, boolean writable)
            implements ProtocolAdapterWrapperSouthboundMessage {}

    /**
     * A requested read of a tag's durable command queue answered. Four outcomes, distinguished by which field is
     * set: a leased {@code command}; an {@code undeliverableCommandId} for a publish that could not be translated;
     * a {@code failure}; or none of them, meaning the queue read empty.
     * <p>
     * The untranslatable case is reported rather than handled at the store on purpose. Deciding what becomes of a
     * command is the delivery side's job and belongs on the dispatch thread beside every other disposition — it is
     * a dead-letter like any other, and it should read like one.
     *
     * @param tagName                the tag the read was issued for.
     * @param readToken              the token of the read being answered; a stale answer is ignored.
     * @param command                the leased command, or {@code null}.
     * @param undeliverableCommandId the id of a leased publish that cannot be delivered, or {@code null}.
     * @param undeliverableReason    why it cannot be delivered — the operator's only record of a destroyed command,
     *                               so it names the actual cause (an undecodable payload and a QoS 0 publish are
     *                               different faults with different fixes). {@code null} unless
     *                               {@code undeliverableCommandId} is set.
     * @param failure                why the read failed, or {@code null} when it did not.
     */
    record SouthboundRead(
            @NotNull String tagName,
            long readToken,
            @Nullable SouthboundCommand command,
            @Nullable String undeliverableCommandId,
            @Nullable String undeliverableReason,
            @Nullable Throwable failure)
            implements ProtocolAdapterWrapperSouthboundMessage {}

    /**
     * A requested depth check of a tag's durable command queue answered — the cross-check that catches a queue
     * reading empty while it still holds commands (a head hidden behind an ownerless in-flight marker).
     *
     * @param tagName   the tag the check was issued for.
     * @param readToken the token of the check being answered; a stale answer is ignored.
     * @param size      the queue depth the store reported.
     * @param failure   why the check failed, or {@code null} when it succeeded.
     */
    record SouthboundSize(
            @NotNull String tagName,
            long readToken,
            int size,
            @Nullable Throwable failure) implements ProtocolAdapterWrapperSouthboundMessage {}

    /**
     * A command may have arrived on a tag's command topic — a <b>latency hint</b> from the broker's
     * publish-available callback, never a correctness mechanism. That callback fires only on the queue's 0→1 size
     * transition, so it can be missed; the backstop poll is what guarantees the command is found. Acting on this
     * hint simply finds it sooner.
     *
     * @param tagName the tag whose queue may now be non-empty.
     */
    record SouthboundArrival(@NotNull String tagName) implements ProtocolAdapterWrapperSouthboundMessage {}
}
