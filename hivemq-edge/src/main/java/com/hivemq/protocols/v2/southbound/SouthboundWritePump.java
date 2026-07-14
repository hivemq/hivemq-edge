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
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The southbound-write producer for one tag (option D): it reads commands from a {@link SouthboundCommandSource} and
 * forwards them to the adapter <b>one at a time</b>, advancing only when the current write settles. The adapter is
 * therefore never asked to hold more than one write, and the backlog stays in the durable source — not in the
 * adapter and not in this pump. See {@code SOUTHBOUND_MULTI_WRITE.md}.
 * <p>
 * On each settled write the pump disposes the source command by outcome, giving at-least-once delivery:
 * <ul>
 * <li>{@link SouthboundWriteOutcome#SUCCEEDED} → {@link SouthboundCommandSource#commit commit} (delivered);</li>
 * <li>{@link SouthboundWriteOutcome#FAILED} → {@link SouthboundCommandSource#deadLetter deadLetter} (a well-formed
 *     value the device rejected — retrying loops forever);</li>
 * <li>{@link SouthboundWriteOutcome#ABORTED} → {@link SouthboundCommandSource#release release} and <b>pause</b> — the
 *     adapter went away (disconnect/deactivation); the command is kept for redelivery and pumping resumes on
 *     {@link #resume()} when the adapter is ready again;</li>
 * <li>{@link SouthboundWriteOutcome#REJECTED_BUSY} → {@code release} and pause — the single-in-flight invariant was
 *     violated (a correct pump never causes this); counted as an alarm.</li>
 * </ul>
 * Thread-safety: {@link #resume()} and the source's wakeup may run on producer threads; {@code onSettled} runs on the
 * adapter's dispatch thread. All state is guarded by this pump's monitor, and the source's wakeup is invoked outside
 * its own lock, so the lock order is always pump→source and there is no deadlock. The critical sections only enqueue
 * to the mailbox (non-blocking).
 */
public final class SouthboundWritePump {

    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;
    private final @NotNull Node node;
    private final @NotNull SouthboundCommandSource source;

    private @Nullable SouthboundCommand inFlight;
    private boolean paused;
    private long forwarded;
    private long delivered;
    private long deadLettered;
    private long aborted;
    private long rejectedByAdapter;

    /**
     * @param wrapperSender the send-only handle to the owning adapter wrapper's mailbox.
     * @param node          the node every command from this pump targets (the correlation key).
     * @param source        the durable backlog to drain, one command at a time.
     */
    public SouthboundWritePump(
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final @NotNull Node node,
            final @NotNull SouthboundCommandSource source) {
        this.wrapperSender = wrapperSender;
        this.node = node;
        this.source = source;
        source.onAvailable(this::pump);
    }

    /**
     * Forward the next command if the adapter is free and not paused — a no-op otherwise. Safe to call repeatedly;
     * invoked by the source's wakeup and after each settled write.
     */
    public synchronized void pump() {
        if (paused || inFlight != null) {
            return;
        }
        final SouthboundCommand command = source.poll();
        if (command == null) {
            return;
        }
        inFlight = command;
        forwarded++;
        wrapperSender.tell(
                new ProtocolAdapterWrapperWriteRequest(node, command.value(), outcome -> onSettled(command, outcome)));
    }

    /**
     * Resume after a pause caused by an aborted or rejected write — call when the adapter is ready again (reconnected
     * or reactivated). The kept command is redelivered.
     */
    public synchronized void resume() {
        paused = false;
        pump();
    }

    private synchronized void onSettled(
            final @NotNull SouthboundCommand command, final @NotNull SouthboundWriteOutcome outcome) {
        inFlight = null;
        switch (outcome) {
            case SUCCEEDED -> {
                source.commit(command.id());
                delivered++;
                pump();
            }
            case FAILED -> {
                source.deadLetter(command.id(), "device rejected the write");
                deadLettered++;
                pump();
            }
            case ABORTED -> {
                source.release(command.id());
                aborted++;
                paused = true; // the adapter went away; wait for resume()
            }
            case REJECTED_BUSY -> {
                source.release(command.id());
                rejectedByAdapter++;
                paused = true; // invariant violation — do not tight-loop
            }
        }
    }

    /**
     * @return whether a write is currently outstanding at the adapter.
     */
    public synchronized boolean inFlight() {
        return inFlight != null;
    }

    /**
     * @return whether the pump is paused awaiting {@link #resume()} after an abort/rejection.
     */
    public synchronized boolean paused() {
        return paused;
    }

    /**
     * @return the total number of writes forwarded to the adapter (one at a time).
     */
    public synchronized long forwarded() {
        return forwarded;
    }

    /**
     * @return the number of writes the device acknowledged and the source committed.
     */
    public synchronized long delivered() {
        return delivered;
    }

    /**
     * @return the number of writes dead-lettered after a device rejection.
     */
    public synchronized long deadLettered() {
        return deadLettered;
    }

    /**
     * @return the number of in-flight writes abandoned (connection lost / deactivated) and kept for redelivery.
     */
    public synchronized long aborted() {
        return aborted;
    }

    /**
     * @return the number of forwarded writes the adapter rejected as busy — must stay zero, since this pump keeps a
     *         single write in flight.
     */
    public synchronized long rejectedByAdapter() {
        return rejectedByAdapter;
    }
}
