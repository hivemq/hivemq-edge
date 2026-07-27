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

import static java.util.Objects.requireNonNull;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundArrival;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundSize;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * An <b>interim, in-memory</b> {@link SouthboundWriteBacklog}: a bounded FIFO that models the durable MQTT client
 * queue's shape and overflow policy but <b>is not durable</b> — its contents are lost on restart. It exists so
 * adapters wired without a broker runtime, and the tests, have a real store behind the delivery channel. Do not use
 * it where durability is required.
 * <p>
 * It honours the store contract exactly: a read leases the head <b>without removing it</b>, so an abandoned command
 * is simply still there to be read again, and only {@link #delete} removes anything. On overflow the <b>newest</b>
 * offered command is shed observably — the bound is the back-pressure limit.
 * <p>
 * Unlike the delivery side, this class is synchronized: {@link #offer} is called from producer threads while the
 * reads come from the wrapper's dispatch thread. The monitor covers the deque and the counters only; every answer
 * is told to the mailbox outside it.
 */
public final class InMemorySouthboundWriteBacklog implements SouthboundWriteBacklog {

    private final int capacity;
    private final @NotNull String tagName;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;
    private final @NotNull Deque<SouthboundCommand> pending = new ArrayDeque<>();
    private final @NotNull List<SouthboundCommand> deletedCommands = new ArrayList<>();

    private boolean closed;
    private long nextId;
    private long offered;
    private long droppedByOverflow;

    /**
     * @param capacity      the maximum number of pending commands; offers beyond it shed the newest.
     * @param tagName       the tag this backlog feeds — the key every answer is addressed to.
     * @param wrapperSender the wrapper mailbox every answer is told to.
     */
    public InMemorySouthboundWriteBacklog(
            final int capacity,
            final @NotNull String tagName,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender) {
        this.capacity = capacity;
        this.tagName = tagName;
        this.wrapperSender = wrapperSender;
    }

    /**
     * Offer a new command — the "an MQTT write arrived" trigger for a store the broker does not feed. Enqueued if
     * there is room, else shed. Hints the delivery side outside this object's monitor, exactly as the broker's
     * publish-available callback does for the durable store.
     *
     * @param value the value to write.
     */
    public void offer(final @NotNull DataPoint value) {
        synchronized (this) {
            if (closed) {
                // A dropped channel must not be resurrected by a late offer — nothing to deliver from here.
                return;
            }
            offered++;
            if (pending.size() >= capacity) {
                droppedByOverflow++;
                return;
            }
            pending.addLast(new SouthboundCommand(Long.toString(nextId++), value));
        }
        wrapperSender.tell(new SouthboundArrival(tagName));
    }

    @Override
    public void requestRead(final long readToken) {
        final SouthboundCommand head;
        synchronized (this) {
            head = closed ? null : pending.peekFirst();
        }
        wrapperSender.tell(new SouthboundRead(tagName, readToken, head, null, null));
    }

    @Override
    public void requestSize(final long readToken) {
        final int size;
        synchronized (this) {
            size = closed ? 0 : pending.size();
        }
        wrapperSender.tell(new SouthboundSize(tagName, readToken, size, null));
    }

    @Override
    public synchronized void delete(final @NotNull String commandId) {
        if (closed) {
            return; // a settle racing close(): the channel was dropped, nothing to dispose
        }
        final SouthboundCommand head = pending.peekFirst();
        if (head == null || !commandId.equals(head.id())) {
            // The delivery side only ever deletes the command it currently holds as head, so reaching this is a
            // caller bug, not a store failure — the one throw the contract permits, and one worth keeping loud.
            throw new IllegalStateException("delete of a command that is not the head: " + commandId);
        }
        deletedCommands.add(requireNonNull(pending.pollFirst()));
    }

    @Override
    public void releaseMarkers() {
        // Nothing to release: this store hands out no broker-side leases.
    }

    @Override
    public synchronized void close() {
        // Release the pending commands so a stale readiness signal (a dropped or replaced channel whose old aspect
        // still reports itself writable) cannot deliver a command that was meant to be discarded. Reads then answer
        // empty and offer/delete become no-ops. The deletion record stays for test observability. A durable store
        // leaves its storage untouched; this one is not durable, so dropping the pending contents is correct — they
        // die with the object regardless.
        closed = true;
        pending.clear();
    }

    public synchronized int pendingSize() {
        return pending.size();
    }

    public synchronized long offered() {
        return offered;
    }

    public synchronized long droppedByOverflow() {
        return droppedByOverflow;
    }

    /**
     * @return the commands deleted — committed or dead-lettered — in deletion order. Which of the two a deletion
     *         was is the delivery channel's business, and its counters report it; the store only removes.
     */
    public synchronized @NotNull List<SouthboundCommand> deletedCommands() {
        return List.copyOf(deletedCommands);
    }

    /**
     * @return whether this backlog has been closed — a dropped or replaced channel.
     */
    public synchronized boolean isClosed() {
        return closed;
    }
}
