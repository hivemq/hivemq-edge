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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The queue in front of one tag's write aspect — the flow-control point of the southbound write path. The write
 * aspect advertises an in-flight window of exactly <b>one</b> write and never buffers; this queue paces delivery
 * of the durable backlog to that window: it pushes the head command to the adapter, holds everything behind it,
 * and advances only when the delivered write settles with its terminal {@link SouthboundWriteOutcome}. The
 * backlog therefore lives in the durable {@link SouthboundWriteBacklog} — never in the adapter and never in this
 * class, which holds no commands of its own.
 * <p>
 * <b>A command is deleted from the backlog only on a terminal outcome</b> — the rule that makes delivery durable
 * and at-least-once: a crash between delivery and outcome leaves the command in place, to be redelivered. Each
 * settled write is disposed by its outcome:
 * <ul>
 * <li>{@link SouthboundWriteOutcome#SUCCEEDED} → {@link SouthboundWriteBacklog#removeHead commit} — delivered,
 *     journey over;</li>
 * <li>{@link SouthboundWriteOutcome#FAILED} → {@link SouthboundWriteBacklog#deadLetterHead dead-letter} — the
 *     device rejected a well-formed value; redelivering it loops forever;</li>
 * <li>{@link SouthboundWriteOutcome#ABORTED} → <b>kept at the head</b> and delivery suspends — the adapter went
 *     away (disconnect or deactivation) before a result; {@link #resume()} redelivers the very same command when
 *     the adapter is ready again;</li>
 * <li>{@link SouthboundWriteOutcome#REJECTED_BUSY} → kept, suspended, and counted — the write aspect saw a second
 *     in-flight write, a violation of the advertised window that this queue's pacing makes impossible;
 *     {@link #windowViolations()} must read zero.</li>
 * </ul>
 * {@link #suspend()} closes the window without tearing anything down: no further command is delivered (an
 * in-flight write still settles normally) while the backlog keeps absorbing the burst; {@link #resume()} reopens
 * delivery. Use it when the adapter is known not ready, so commands are not bounced off a write aspect that can
 * only abort them.
 * <p>
 * Thread-safety: {@link #suspend()}/{@link #resume()} and the backlog's wakeup may run on producer threads; the
 * completion callback runs on the adapter's dispatch thread. All state is guarded by this queue's monitor, and
 * the backlog's wakeup is invoked outside its own lock, so the lock order is always queue→backlog and there is no
 * deadlock. Critical sections enqueue to the mailbox at most (non-blocking); the backlog disposal of a settled
 * write — which reaches broker persistence, and in in-memory persistence mode may execute persistence work
 * caller-side — runs <b>outside</b> the monitor, with the in-flight guard left up so no concurrent delivery can
 * slip in meanwhile. A settle is consumed exactly once: a late duplicate settle of an abandoned attempt is
 * detected by its delivery token and ignored, so it can never dispose its redelivery.
 */
public final class SouthboundWriteQueue {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SouthboundWriteQueue.class);

    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;
    private final @NotNull Node node;
    private final @NotNull SouthboundWriteBacklog backlog;

    private @Nullable String inFlightId;

    /** Advances once per delivery; each completion carries its delivery's token so a stale settle is detectable. */
    private long deliveryToken;

    /** True from delivery until its first settle is consumed — the exactly-once gate for completions. */
    private boolean settleArmed;

    /**
     * Queues are born suspended: the first {@link #resume()} — in production the tag's first {@code tagWritable}
     * — opens the window. Starting open would leave a gap between the constructor registering the backlog wakeup
     * and the creator's {@code suspend()}, in which an arriving command could be delivered to a tag that never
     * verified.
     */
    private boolean suspended = true;

    private long deliveries;
    private long committed;
    private long deadLettered;
    private long keptForRedelivery;
    private long windowViolations;

    /**
     * @param wrapperSender the send-only handle to the owning adapter wrapper's mailbox.
     * @param node          the node every command from this queue targets (the correlation key).
     * @param backlog       the durable backlog to deliver from, one command at a time.
     */
    public SouthboundWriteQueue(
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final @NotNull Node node,
            final @NotNull SouthboundWriteBacklog backlog) {
        this.wrapperSender = wrapperSender;
        this.node = node;
        this.backlog = backlog;
        backlog.onAvailable(this::deliverNext);
    }

    /**
     * Close the delivery window: no further command is delivered until {@link #resume()}. An in-flight write is
     * untouched — it settles normally and its outcome is disposed of as usual. The backlog keeps accumulating.
     */
    public synchronized void suspend() {
        suspended = true;
    }

    /**
     * Reopen the delivery window — call when the adapter is ready (again). The head command, kept across an abort,
     * is (re)delivered.
     */
    public synchronized void resume() {
        suspended = false;
        deliverNext();
    }

    /**
     * Deliver the head command if the window is open and nothing is in flight — a no-op otherwise. Safe to call
     * repeatedly; invoked by the backlog's wakeup and after each settled write.
     */
    private synchronized void deliverNext() {
        if (suspended || inFlightId != null) {
            return;
        }
        final SouthboundCommand command = backlog.head();
        if (command == null) {
            return;
        }
        inFlightId = command.id();
        deliveries++;
        settleArmed = true;
        final long token = ++deliveryToken;
        wrapperSender.tell(new ProtocolAdapterWrapperWriteRequest(
                node, command.value(), (outcome, reason) -> onSettled(token, command, outcome, reason)));
    }

    private void onSettled(
            final long token,
            final @NotNull SouthboundCommand command,
            final @NotNull SouthboundWriteOutcome outcome,
            final @Nullable String reason) {
        synchronized (this) {
            if (token != deliveryToken || !settleArmed) {
                // A stale or duplicate settle — e.g. an abandoned attempt's result arriving after its redelivery
                // started. Acting on it would dispose (or double-dispose) a command whose live delivery is still
                // unsettled.
                log.warn(
                        "Ignoring a stale settle ({}) for southbound command '{}': the delivery it belongs to was "
                                + "already settled",
                        outcome,
                        command.id());
                return;
            }
            settleArmed = false;
            switch (outcome) {
                case ABORTED -> {
                    // The command was never removed, so it is still the head — kept for redelivery on resume().
                    inFlightId = null;
                    keptForRedelivery++;
                    suspended = true; // the adapter went away; wait for resume()
                    return;
                }
                case REJECTED_BUSY -> {
                    inFlightId = null;
                    windowViolations++;
                    suspended = true; // window violation — do not tight-loop
                    return;
                }
                case SUCCEEDED -> committed++;
                case FAILED -> deadLettered++;
            }
        }
        // Terminal disposal reaches broker persistence and must not run under the queue monitor (in in-memory
        // persistence mode the call can execute persistence work caller-side — holding the monitor through that
        // would block suspend()/resume()/readiness for the duration). inFlightId stays set meanwhile, so no
        // concurrent deliverNext can double-deliver.
        try {
            if (outcome == SouthboundWriteOutcome.SUCCEEDED) {
                backlog.removeHead(command.id());
            } else {
                // The device's own words travel into the dead-letter record.
                backlog.deadLetterHead(command.id(), reason != null ? reason : "device rejected the write");
            }
        } catch (final RuntimeException disposalFailure) {
            // The backlog contract forbids throwing from disposal, so this is defensive: if one ever does, we must
            // still release the in-flight slot below rather than leave inFlightId set forever — which would wedge
            // the tag (every future deliverNext no-ops on inFlightId != null) with no recovery path. The command's
            // fate is then uncertain (possibly still queued), so at-least-once holds — a redelivery or a successor
            // backlog covers it. Log and advance.
            log.error(
                    "Southbound backlog threw disposing command '{}' ({}) — releasing the delivery slot and "
                            + "advancing to keep the tag alive",
                    command.id(),
                    outcome,
                    disposalFailure);
        }
        synchronized (this) {
            inFlightId = null;
        }
        deliverNext();
    }

    /**
     * @return whether a write is currently outstanding at the adapter.
     */
    public synchronized boolean inFlight() {
        return inFlightId != null;
    }

    /**
     * @return whether the delivery window is closed, awaiting {@link #resume()}.
     */
    public synchronized boolean suspended() {
        return suspended;
    }

    /**
     * @return the total number of writes delivered to the adapter (one at a time; redeliveries count again).
     */
    public synchronized long deliveries() {
        return deliveries;
    }

    /**
     * @return the number of writes the device acknowledged, committed (removed) from the backlog.
     */
    public synchronized long committed() {
        return committed;
    }

    /**
     * @return the number of writes dead-lettered after a device rejection.
     */
    public synchronized long deadLettered() {
        return deadLettered;
    }

    /**
     * @return the number of in-flight writes abandoned (connection lost / deactivated) and kept at the head for
     *         redelivery.
     */
    public synchronized long keptForRedelivery() {
        return keptForRedelivery;
    }

    /**
     * @return the number of delivered writes the adapter rejected as busy — a violation of the advertised
     *         in-flight window of one, which this queue's pacing makes impossible. Must stay zero.
     */
    public synchronized long windowViolations() {
        return windowViolations;
    }
}
