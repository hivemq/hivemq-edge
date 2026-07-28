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
package com.hivemq.protocols.v2.southbound;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundSize;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A mailbox stand-in that plays the part of the wrapper for the delivery side: it records the write requests a
 * channel emits, and queues the southbound messages a store answers with so a test can {@link #pump} them back the
 * way the real dispatch loop would.
 * <p>
 * The pumping is the point. In production the store's answers reach the channel only by going through the mailbox,
 * so a test that called the channel's handlers directly would be testing a shape the product does not have. Here
 * the test drives the same loop: something happens, messages accumulate, {@link #pump} delivers them in order.
 */
final class CapturingSender implements MailboxSender<ProtocolAdapterWrapperMessage> {

    /** Every write request the delivery side emitted, in order. */
    final @NotNull List<ProtocolAdapterWrapperWriteRequest> requests = new ArrayList<>();

    /** Southbound messages waiting to be delivered, in the order they were told. */
    private final @NotNull Deque<ProtocolAdapterWrapperSouthboundMessage> inbox = new ArrayDeque<>();

    /** Every southbound message told, kept after pumping so a test can assert on what a store answered. */
    private final @NotNull List<ProtocolAdapterWrapperSouthboundMessage> told = new ArrayList<>();

    @Override
    public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
        switch (message) {
            case final ProtocolAdapterWrapperWriteRequest write -> requests.add(write);
            case final ProtocolAdapterWrapperSouthboundMessage southbound -> {
                inbox.addLast(southbound);
                told.add(southbound);
            }
            default -> throw new IllegalStateException("unexpected wrapper message in a southbound test: " + message);
        }
    }

    /**
     * Deliver every queued southbound message to the channel, and everything they cause, until the mailbox is
     * empty — the dispatch loop, run to quiescence.
     *
     * @param queue the channel under test.
     * @return how many messages were delivered.
     */
    int pump(final @NotNull SouthboundWriteQueue queue) {
        int delivered = 0;
        while (!inbox.isEmpty()) {
            final ProtocolAdapterWrapperSouthboundMessage message = inbox.pollFirst();
            delivered++;
            switch (message) {
                case final SouthboundRead read ->
                    queue.onReadAnswer(
                            read.readToken(),
                            read.command(),
                            read.undeliverableCommandId(),
                            read.undeliverableReason(),
                            read.failure());
                case final SouthboundSize size -> queue.onSizeAnswer(size.readToken(), size.size(), size.failure());
                case final ProtocolAdapterWrapperSouthboundMessage.SouthboundArrival ignored -> queue.onArrival();
                case final ProtocolAdapterWrapperSouthboundMessage.WriteSettled settled ->
                    queue.onSettled(settled.deliveryToken(), settled.outcome(), settled.reason());
                case final ProtocolAdapterWrapperSouthboundMessage.TagWritability writability -> {
                    if (writability.writable()) {
                        queue.openWindow();
                    } else {
                        queue.closeWindow();
                    }
                }
            }
        }
        return delivered;
    }

    /**
     * Deliver every queued southbound message to the plane, and everything they cause, until the mailbox is empty.
     *
     * @param plane the delivery side under test.
     * @return how many messages were delivered.
     */
    int pump(final @NotNull SouthboundWritePlane plane) {
        int delivered = 0;
        while (!inbox.isEmpty()) {
            plane.onMessage(inbox.pollFirst());
            delivered++;
        }
        return delivered;
    }

    /** Settle the last delivered write as the adapter would, with no reason. */
    void settleLast(final @NotNull SouthboundWriteOutcome outcome) {
        settleLast(outcome, null);
    }

    /**
     * Settle the last delivered write as the adapter would, carrying the device's own words. Queued rather than
     * applied, so it travels the mailbox exactly as a real settlement does — {@link #pump} delivers it.
     *
     * @param outcome the outcome to settle with.
     * @param reason  the device's failure reason, or {@code null} when it supplied none.
     */
    void settleLast(final @NotNull SouthboundWriteOutcome outcome, final @Nullable String reason) {
        final ProtocolAdapterWrapperWriteRequest last = requests.getLast();
        inbox.addLast(new ProtocolAdapterWrapperSouthboundMessage.WriteSettled(
                last.tagName(), last.deliveryToken(), outcome, reason));
    }

    /**
     * @return whether any southbound message is waiting to be pumped.
     */
    boolean hasPending() {
        return !inbox.isEmpty();
    }

    /**
     * @return every read answer told so far, pumped or not — for asserting on what a store answered.
     */
    @NotNull
    List<SouthboundRead> reads() {
        return told.stream()
                .filter(SouthboundRead.class::isInstance)
                .map(SouthboundRead.class::cast)
                .toList();
    }
}
