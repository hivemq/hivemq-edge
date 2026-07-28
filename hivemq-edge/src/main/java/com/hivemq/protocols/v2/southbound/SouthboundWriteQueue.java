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
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.function.LongSupplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One tag's delivery channel — the flow-control point of the southbound write path. The write aspect handles at
 * most one write at a time and never buffers; this channel paces the durable store to that window: it pushes the
 * head command to the adapter, holds everything behind it, and advances only when that write settles.
 * <p>
 * <b>Everything here runs on the wrapper's single dispatch thread.</b> That thread is the only writer, so there is
 * no monitor and no lock order — the whole class reads as ordinary sequential code. The fields behind the public
 * accessors are {@code volatile} purely so observers on other threads see them; nothing is ever mutated from
 * outside. Reaching
 * that was the point of routing the store's answers and the aspect's settlements through the mailbox: an earlier
 * version of this class was called from producer threads, persistence threads and the dispatch thread at once, and
 * paid for it with a monitor, a documented lock order, a disposal that had to run outside that monitor, and a
 * defensive catch to keep a throwing store from wedging the tag for good.
 * <p>
 * <b>A command is deleted from the store only on a terminal outcome</b> — the rule that makes delivery durable and
 * at-least-once. Each settled write is disposed by its outcome:
 * <ul>
 * <li>{@link SouthboundWriteOutcome#SUCCEEDED} → <b>commit</b>: delete, then read the next;</li>
 * <li>{@link SouthboundWriteOutcome#FAILED} → <b>dead-letter</b>: log the device's own reason and delete —
 *     redelivering a value the device rejects loops forever — then read the next;</li>
 * <li>{@link SouthboundWriteOutcome#ABORTED} → <b>kept</b>: no deletion at all. The adapter went away before a
 *     result; the command is still the store's head, and the window closes until the tag reports itself writable
 *     again, at which point the very same command is delivered again;</li>
 * <li>{@link SouthboundWriteOutcome#REJECTED_BUSY} → <b>kept</b>, counted, and retried on the next poll: the aspect
 *     was already serving a write this channel did not send, which its pacing makes impossible unless a second
 *     producer exists. {@link #windowViolations()} must read zero. The window deliberately stays <i>open</i> — a
 *     busy aspect never crosses its writability boundary, so nothing would ever reopen a window closed here.</li>
 * </ul>
 * <p>
 * <b>Finding the next command is a poll, not a notification.</b> {@link #onArrival()} is a hint the broker may or
 * may not send — its publish-available callback fires only on the queue's 0→1 transition, so it can be missed —
 * while {@link #onTick()} issues a read every {@link #POLL_TICKS} ticks whenever the window is open and nothing is
 * pending. Correctness rests on the poll; the hint only makes the common case fast. If a poll reads empty
 * {@link #EMPTY_READS_BEFORE_SIZE_CHECK} times in a row the channel cross-checks the store's depth, and a non-empty
 * store while it holds no head means a head hidden behind an ownerless in-flight marker — released by a sweep.
 */
public final class SouthboundWriteQueue {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SouthboundWriteQueue.class);

    /**
     * Ticks between backstop reads (~1 s at the 50 ms wrapper tick). This is the safety net, not the fast path:
     * the arrival hint normally finds a command within a tick, so the poll only has to be frequent enough to
     * recover a missed hint before anyone notices, while costing an idle tag one read per second.
     */
    static final int POLL_TICKS = 20;

    /** Consecutive empty reads after which the channel stops believing the store and checks its depth. */
    static final int EMPTY_READS_BEFORE_SIZE_CHECK = 3;

    /**
     * How many disposed command ids this channel remembers, so a command it has already committed or dead-lettered
     * can never be executed on the device a second time (see {@link #disposedIds}).
     * <p>
     * Small on purpose. The case this exists for is a <b>single</b> entry the store refuses to delete, and it only
     * becomes visible again after a marker sweep — which arms only after {@link #EMPTY_READS_BEFORE_SIZE_CHECK}
     * consecutive <b>empty</b> reads, i.e. when that entry is essentially the only thing left. A handful of slots
     * covers a few interleaved failures; remembering more would be a growing set on the dispatch thread guarding
     * against nothing.
     */
    static final int DISPOSED_IDS_REMEMBERED = 16;

    private final @NotNull String adapterId;
    private final @NotNull String tagName;
    private final @NotNull SouthboundWriteBacklog backlog;
    private final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender;

    /**
     * Mints every read, depth-check and delivery token this channel uses. It is the <b>plane's</b> counter, not this
     * channel's, and that matters: answers are routed to a channel by tag name alone, so a per-channel counter
     * restarting at zero would let a store answer outstanding across a channel rebuild match the successor's own
     * token and be consumed as if it were the successor's — leaving the successor's real lease ownerless.
     */
    private final @NotNull LongSupplier tokens;

    /** Where this channel reports the one southbound outcome that destroys a command. */
    private final @NotNull ProtocolAdapterMetrics metrics;

    /**
     * The node every command from this channel targets. Not final: a tags-only reload can re-point a tag at a
     * different node, and this channel follows it rather than being rebuilt — see {@link #retarget}.
     */
    private @NotNull Node node;

    /**
     * The leased head, held until a terminal outcome disposes of it.
     * <p>
     * {@code volatile} for <b>visibility only</b>, like every field below that a public accessor exposes. The
     * dispatch thread is the sole writer and needs no mutual exclusion, but observers — tests today, a status or
     * metrics surface tomorrow — read these from other threads, and a plain field gives them no guarantee of ever
     * seeing a write. Dropping the old monitor removed a guarantee that was doing two jobs; only one of them was
     * unnecessary.
     */
    private volatile @Nullable SouthboundCommand head;

    /** The token of the read we are waiting for, or {@code null} when no read is outstanding. */
    private @Nullable Long pendingReadToken;

    /** The token of the depth check we are waiting for, or {@code null} when none is outstanding. */
    private @Nullable Long pendingSizeToken;

    /** The token of the delivery in flight at the aspect, or {@code null} when none is. Observable. */
    private volatile @Nullable Long inFlightDeliveryToken;

    /**
     * Windows are born <b>closed</b>. The first writability report from the tag's own aspect opens it, so a command
     * arriving before the tag has ever verified waits in the store instead of bouncing off an aspect that could
     * only abort it.
     */
    private volatile boolean windowOpen;

    // Dispatch-thread only: never read from outside, so plain fields.
    private int ticksSincePoll;
    private int consecutiveEmptyReads;

    /**
     * The ids this channel has already disposed of — committed or dead-lettered — in insertion order, bounded to
     * {@link #DISPOSED_IDS_REMEMBERED}.
     * <p>
     * <b>A command this channel has disposed of must never reach the device again.</b> Disposal deletes it from the
     * store, so it should never come back; if it does, the delete did not take. That is a real path: a delete has a
     * bounded number of attempts, and exhausting them leaves the entry queued <i>with this channel's in-flight
     * marker still on it</i>. Reads then skip it, the queue looks empty, three empty reads arm the depth check, the
     * depth check sweeps the markers — and the entry becomes visible again. Without this memory the channel would
     * read it, deliver it, and <b>write it to the device again</b>; it would settle, fail to delete again, and the
     * cycle would repeat every few seconds until Edge restarts. At-least-once permits a duplicate execution; it does
     * not permit an unbounded loop of them against a physical device.
     * <p>
     * This is deliberately <i>not</i> the fix originally proposed for that finding — holding the head until the
     * delete confirms. That needs the store to answer deletes (a seventh message and a new channel state), and it
     * stalls the whole tag on a broken store rather than only the entry that cannot be removed. This achieves the
     * property that mattered — no second execution — while the tag keeps delivering everything behind the entry.
     * <p>
     * The memory is per-channel and in-process: after a restart the entry is visible again with nobody remembering
     * it, so it is executed once more. That is an ordinary at-least-once duplicate, not a loop.
     */
    private final @NotNull LinkedHashSet<String> disposedIds = new LinkedHashSet<>();

    /** The id of the last refusal, so a single stuck entry is reported loudly once rather than on every poll. */
    private @Nullable String lastRefusedCommandId;

    // Observable through the accessors below — see the note on `head`.
    private volatile long deliveries;
    private volatile long committed;
    private volatile long deadLettered;
    private volatile long keptForRedelivery;
    private volatile long windowViolations;
    private volatile long redeliveriesRefused;
    private volatile @Nullable String lastDeadLetterReason;

    /**
     * @param adapterId     the owning adapter's id, for logging.
     * @param tagName       the tag this channel delivers to.
     * @param node          the node every command from this channel targets.
     * @param backlog       the durable store to deliver from, one command at a time.
     * @param wrapperSender the wrapper mailbox write requests are told to.
     * @param tokens        the plane-wide token source; must not be per-channel (see the field's note).
     * @param metrics       the per-adapter metrics, where dead-letters are counted.
     */
    public SouthboundWriteQueue(
            final @NotNull String adapterId,
            final @NotNull String tagName,
            final @NotNull Node node,
            final @NotNull SouthboundWriteBacklog backlog,
            final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            final @NotNull LongSupplier tokens,
            final @NotNull ProtocolAdapterMetrics metrics) {
        this.adapterId = adapterId;
        this.tagName = tagName;
        this.node = node;
        this.backlog = backlog;
        this.wrapperSender = wrapperSender;
        this.tokens = tokens;
        this.metrics = metrics;
    }

    /**
     * Re-point this channel at a different node after a tags-only reload, keeping its store, its leased head and
     * any delivery already in flight.
     * <p>
     * The alternative — rebuilding the channel — is what this replaced, and it was wrong twice over. A tag's durable
     * queue is keyed by its <b>mapping topic</b>, never by its node, so a rebuilt channel reads back the very
     * commands the rebuild was supposed to discard and delivers them to the new node anyway; and because {@link Node}
     * correlates by identity across the adapter boundary and is deserialized afresh on every reload, the rebuild fired
     * on <b>every</b> reload rather than only on a genuine node change — redelivering the leased head, and so writing
     * to the device again, each time. Commands address the tag, the tag now addresses this node: following it is both
     * the honest reading and the cheap one.
     *
     * @param newNode the node this tag now addresses.
     */
    public void retarget(final @NotNull Node newNode) {
        node = newNode;
    }

    /**
     * Re-point this channel at a <b>different</b> node, destroying everything queued for the previous one.
     * <p>
     * This is the resolution of the question v2 got wrong and v3 initially only made honest: a tags-only reload can
     * move a tag from one node to another, and the tag's durable queue is keyed by its mapping <b>topic</b>, never by
     * the node — so a plain {@link #retarget} hands commands an operator authored for the old target straight to the
     * new one. Delivering a setpoint to a device nobody addressed is worse than losing it, so the commands are
     * destroyed and the destruction is recorded.
     * <p>
     * Every piece of correlation is dropped along with them, because each one could otherwise resurrect a destroyed
     * command against the new node: an outstanding read would answer with a command that no longer exists, and a
     * settle for the pre-change delivery would dispose a head that is already gone. Clearing the tokens turns both
     * into ordinary stale answers, which this class already ignores by design.
     * <p>
     * The window is left <b>closed</b>, exactly as a freshly created channel is: the rebuilt write aspect reopens it
     * when it has verified against the new node.
     *
     * @param newNode the node this tag now addresses.
     */
    @SuppressWarnings("NonAtomicVolatileUpdate") // sole writer is the dispatch thread — see onReadAnswer
    public void discardAndRetarget(final @NotNull Node newNode) {
        if (head != null) {
            // The one destroyed command this channel can account for precisely. The rest are counted only in the
            // store's own log line: clear() reports no count, and asking for one first would race the discard.
            deadLettered++;
            metrics.incrementWriteDeadLettered(tagName);
            lastDeadLetterReason = "the tag was re-pointed at a different node";
            log.warn(
                    "Dead-lettering southbound command '{}' for tag '{}' on adapter '{}': {}",
                    head.id(),
                    tagName,
                    adapterId,
                    lastDeadLetterReason);
        }
        head = null;
        pendingReadToken = null;
        pendingSizeToken = null;
        inFlightDeliveryToken = null;
        consecutiveEmptyReads = 0;
        disposedIds.clear(); // the ids they guarded are gone with the queue
        lastRefusedCommandId = null;
        windowOpen = false;
        backlog.discardAll();
        node = newNode;
    }

    /** Open the delivery window — the tag's write aspect reported itself writable. */
    public void openWindow() {
        if (windowOpen) {
            return;
        }
        windowOpen = true;
        deliverOrRead();
    }

    /**
     * Close the delivery window without tearing anything down: no further command is delivered, a write already in
     * flight still settles and is disposed of as usual, and the store keeps accumulating.
     */
    public void closeWindow() {
        windowOpen = false;
    }

    /** A command may have arrived — the broker's hint, or an in-memory store's offer. Look, but do not insist. */
    public void onArrival() {
        deliverOrRead();
    }

    /**
     * The wrapper ticked. Every {@link #POLL_TICKS}th tick is the backstop read that makes a missed arrival hint a
     * latency problem instead of a stranded command.
     */
    public void onTick() {
        if (++ticksSincePoll < POLL_TICKS) {
            return;
        }
        ticksSincePoll = 0;
        deliverOrRead();
    }

    /**
     * A read answered.
     *
     * @param readToken              the token of the read being answered; a stale one is ignored.
     * @param command                the leased head, or {@code null}.
     * @param undeliverableCommandId the id of a leased publish that cannot be delivered, or {@code null}.
     * @param undeliverableReason    why it cannot be delivered; only meaningful with an id.
     * @param failure                why the read failed, or {@code null}.
     */
    // The counters below are volatile for cross-thread visibility only; the dispatch thread is their sole writer, so
    // the non-atomic ++ ErrorProne flags here is safe. Introduce a second writer and that stops being true.
    @SuppressWarnings("NonAtomicVolatileUpdate")
    public void onReadAnswer(
            final long readToken,
            final @Nullable SouthboundCommand command,
            final @Nullable String undeliverableCommandId,
            final @Nullable String undeliverableReason,
            final @Nullable Throwable failure) {
        if (pendingReadToken == null || pendingReadToken != readToken) {
            return; // a stale answer, from a read this channel has already given up on
        }
        pendingReadToken = null;
        if (undeliverableCommandId != null) {
            // An undeliverable command is dead-lettered exactly like one the device refused: deleted so it can never
            // wedge the tag, with the log line as the only record — which is why the store's own reason is carried
            // through rather than restated here.
            deadLettered++;
            metrics.incrementWriteDeadLettered(tagName);
            lastDeadLetterReason =
                    undeliverableReason != null ? undeliverableReason : "the command could not be delivered";
            log.warn(
                    "Dead-lettering southbound command '{}' for tag '{}' on adapter '{}': {}",
                    undeliverableCommandId,
                    tagName,
                    adapterId,
                    lastDeadLetterReason);
            consecutiveEmptyReads = 0;
            rememberDisposed(undeliverableCommandId);
            backlog.delete(undeliverableCommandId);
            deliverOrRead();
            return;
        }
        if (failure != null) {
            // Nothing to do but wait for the next poll: the command, if any, was never removed.
            log.error(
                    "Failed to read the southbound queue of tag '{}' on adapter '{}' — retrying on the next poll",
                    tagName,
                    adapterId,
                    failure);
            return;
        }
        if (command == null) {
            if (++consecutiveEmptyReads >= EMPTY_READS_BEFORE_SIZE_CHECK && pendingSizeToken == null) {
                consecutiveEmptyReads = 0;
                pendingSizeToken = tokens.getAsLong();
                backlog.requestSize(pendingSizeToken);
            }
            return;
        }
        consecutiveEmptyReads = 0;
        if (disposedIds.contains(command.id())) {
            // Already committed or dead-lettered, so its delete did not take. Never deliver it: the device would
            // execute a command this channel has already reported as disposed, and would keep doing so on every
            // marker sweep. Retry the delete — if the store has recovered, that clears the entry for good.
            //
            // And then STOP, rather than reading on. A read leases what it returns, so a healthy store re-marks this
            // entry and the next read would find whatever is behind it; but a store that returns the same entry
            // again — which is exactly the malfunction being handled here — would turn "read on" into an unbounded
            // recursion on the dispatch thread. Waiting for the next poll costs one interval on a queue that is
            // already broken, and cannot spin whatever the store does.
            redeliveriesRefused++;
            metrics.incrementRedeliveryRefused(tagName);
            if (command.id().equals(lastRefusedCommandId)) {
                // The same entry surfacing again on a later sweep. One loud line per stuck command is the signal;
                // repeating it every poll interval for as long as the store stays broken would bury everything else.
                log.debug(
                        "Southbound command '{}' for tag '{}' on adapter '{}' surfaced again; still refusing it",
                        command.id(),
                        tagName,
                        adapterId);
            } else {
                lastRefusedCommandId = command.id();
                log.error(
                        "Southbound command '{}' for tag '{}' on adapter '{}' came back after this channel disposed "
                                + "of it — its delete did not take. Refusing to execute it a second time and retrying "
                                + "the delete. The entry is leaking in the client-queue persistence for this queue; "
                                + "redeliveriesRefused counts every further sighting.",
                        command.id(),
                        tagName,
                        adapterId);
            }
            backlog.delete(command.id());
            return;
        }
        head = command;
        deliverOrRead();
    }

    /**
     * A depth check answered. A store that says it holds commands while this channel holds no head means the head
     * is hidden — the ownerless-in-flight-marker case — and a sweep is the only way to see it again.
     *
     * @param readToken the token of the check being answered; a stale one is ignored.
     * @param size      the depth the store reported.
     * @param failure   why the check failed, or {@code null}.
     */
    public void onSizeAnswer(final long readToken, final int size, final @Nullable Throwable failure) {
        if (pendingSizeToken == null || pendingSizeToken != readToken) {
            return;
        }
        pendingSizeToken = null;
        if (failure != null || size <= 0 || head != null || pendingReadToken != null) {
            return;
        }
        log.warn(
                "Southbound queue of tag '{}' on adapter '{}' reads empty while the store holds {} command(s) — "
                        + "releasing possibly stranded in-flight markers",
                tagName,
                adapterId,
                size);
        backlog.releaseMarkers();
    }

    /**
     * The write aspect settled the write this channel delivered.
     *
     * @param deliveryToken the token the aspect echoed back.
     * @param outcome       the terminal outcome.
     * @param reason        the device's own words, or {@code null}.
     */
    @SuppressWarnings("NonAtomicVolatileUpdate") // sole writer is the dispatch thread — see onReadAnswer
    public void onSettled(
            final long deliveryToken, final @NotNull SouthboundWriteOutcome outcome, final @Nullable String reason) {
        if (inFlightDeliveryToken == null || inFlightDeliveryToken != deliveryToken) {
            // An abandoned delivery's answer arriving after its redelivery started. Acting on it would dispose a
            // command whose live delivery is still unsettled.
            log.debug(
                    "Ignoring a stale southbound settle ({}) for tag '{}' on adapter '{}'",
                    outcome,
                    tagName,
                    adapterId);
            return;
        }
        inFlightDeliveryToken = null;
        final SouthboundCommand settled = head;
        if (settled == null) {
            // Cannot happen: the head is only cleared here or by a channel drop, and a dropped channel is not
            // reachable. Kept as a guard so a future change cannot turn it into a null dereference.
            log.warn("Southbound settle ({}) for tag '{}' on adapter '{}' has no head", outcome, tagName, adapterId);
            return;
        }
        switch (outcome) {
            case ABORTED -> {
                // Never removed, so still the store's head — redelivered when the tag is writable again.
                keptForRedelivery++;
                windowOpen = false;
                return;
            }
            case REJECTED_BUSY -> {
                // The aspect was already writing something this channel did not send — a second write producer, or
                // a defect. The command is kept, and the window stays OPEN: the aspect never crosses its writability
                // boundary while merely busy, so no TagWritability(true) would ever arrive to reopen a closed one and
                // the tag would stop delivering for good.
                //
                // Redelivery therefore waits for the next deliverOrRead. On the durable store that is the backstop
                // poll, once per POLL_TICKS: the arrival hint fires only on the queue's 0→1 transition, and a queue
                // whose head this channel holds is never empty. The in-memory stand-in hints on every offer instead,
                // so there the retry runs at producer rate — bounded by the producer, never a spin, and that store
                // has no production caller. Either way the episode is finite: the aspect's own write-result deadline
                // settles whatever is occupying it.
                windowViolations++;
                log.warn(
                        "Southbound write for tag '{}' on adapter '{}' was rejected as busy — the write aspect is "
                                + "serving a write this channel did not send. The command is kept and retried on the "
                                + "next poll; windowViolations is now {}.",
                        tagName,
                        adapterId,
                        windowViolations);
                return;
            }
            case SUCCEEDED -> committed++;
            case FAILED -> {
                deadLettered++;
                metrics.incrementWriteDeadLettered(tagName);
                lastDeadLetterReason = reason != null ? reason : "device rejected the write";
                log.warn(
                        "Dead-lettering southbound command '{}' for tag '{}' on adapter '{}': {}",
                        settled.id(),
                        tagName,
                        adapterId,
                        lastDeadLetterReason);
            }
        }
        head = null;
        rememberDisposed(settled.id());
        backlog.delete(settled.id());
        deliverOrRead();
    }

    /** Record a disposed id, evicting the oldest past {@link #DISPOSED_IDS_REMEMBERED}. Dispatch thread only. */
    private void rememberDisposed(final @NotNull String commandId) {
        disposedIds.add(commandId);
        if (disposedIds.size() > DISPOSED_IDS_REMEMBERED) {
            final Iterator<String> oldest = disposedIds.iterator();
            oldest.next();
            oldest.remove();
        }
    }

    /**
     * Deliver the head if the window is open and nothing is in flight; otherwise read one if nothing is pending.
     * Safe to call as often as anything interesting happens — every path into it is guarded here rather than at
     * the call sites.
     */
    @SuppressWarnings("NonAtomicVolatileUpdate") // sole writer is the dispatch thread — see onReadAnswer
    private void deliverOrRead() {
        if (!windowOpen || inFlightDeliveryToken != null) {
            return;
        }
        if (head == null) {
            if (pendingReadToken == null) {
                pendingReadToken = tokens.getAsLong();
                backlog.requestRead(pendingReadToken);
            }
            return;
        }
        deliveries++;
        inFlightDeliveryToken = tokens.getAsLong();
        wrapperSender.tell(new ProtocolAdapterWrapperWriteRequest(node, tagName, head.value(), inFlightDeliveryToken));
    }

    /**
     * @return whether a write is currently outstanding at the adapter.
     */
    public boolean inFlight() {
        return inFlightDeliveryToken != null;
    }

    /**
     * @return whether the delivery window is closed, awaiting the tag's next writability report.
     */
    public boolean suspended() {
        return !windowOpen;
    }

    /**
     * @return the command currently leased from the store, or {@code null} when none is held.
     */
    public @Nullable SouthboundCommand head() {
        return head;
    }

    /**
     * @return the total number of writes delivered to the adapter (one at a time; redeliveries count again).
     */
    public long deliveries() {
        return deliveries;
    }

    /**
     * @return the number of writes the device acknowledged, committed (deleted) from the store.
     */
    public long committed() {
        return committed;
    }

    /**
     * @return the number of writes dead-lettered after a device rejection.
     */
    public long deadLettered() {
        return deadLettered;
    }

    /**
     * @return the reason carried by the most recent dead-letter — the device's own words where it supplied any.
     */
    public @Nullable String lastDeadLetterReason() {
        return lastDeadLetterReason;
    }

    /**
     * @return the number of in-flight writes abandoned (connection lost, deactivated) and kept for redelivery.
     */
    public long keptForRedelivery() {
        return keptForRedelivery;
    }

    /**
     * @return the number of delivered writes the aspect rejected as busy — a violation of the in-flight window of
     *         one, which this channel's pacing makes impossible. Must stay zero.
     */
    public long windowViolations() {
        return windowViolations;
    }

    /**
     * @return the number of times a command this channel had already disposed of came back from the store and was
     *         refused rather than executed again. Non-zero means the client-queue persistence is failing to delete
     *         entries for this queue — see {@code disposedIds}. Must stay zero.
     */
    public long redeliveriesRefused() {
        return redeliveriesRefused;
    }
}
