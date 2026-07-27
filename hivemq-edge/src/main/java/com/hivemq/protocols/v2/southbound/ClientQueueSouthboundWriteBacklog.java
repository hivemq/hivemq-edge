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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The production {@link SouthboundWriteBacklog}: the durable MQTT client queue itself, adapted to the backlog's
 * synchronous head-without-remove contract by <b>prefetching</b>. {@link ClientQueuePersistence} is asynchronous
 * (futures through the single-writer), so this backlog leases the queue's head ahead of time — one
 * {@code readShared(limit 1)} marks it in-flight and caches it — and {@link #head()} serves the cached lease
 * idempotently until a terminal outcome deletes it ({@code removeShared}) and the next prefetch begins. An
 * abandoned command needs no call, exactly as the contract says: the lease simply stays cached, redelivered when
 * the delivering queue resumes.
 * <p>
 * Durability and at-least-once come from the client queue: a command is {@code removeShared}-deleted only on a
 * terminal outcome, and a crash discards the in-memory lease while the message stays queued — the restarted Edge
 * reads it again (the in-flight marker does not outlive the process; QoS 0 commands are removed on read by the
 * broker and are therefore at-most-once — QoS ≥ 1 is the durability precondition). The contract is strictly
 * at-least-once: a crash between the device acknowledgment and the queue delete replays the command on restart
 * and it executes again — the southbound path is deliberately fire-and-forget and keeps no executed-command
 * record to recognize a replay by.
 * <p>
 * An <b>untranslatable</b> publish (the {@link SouthboundPublishTranslator} returns {@code null} or throws) is
 * dead-lettered by the backlog itself — removed and logged, so a malformed command never wedges the tag. A failed
 * {@code readShared} is logged at {@code ERROR} and <b>not</b> retried in place (an immediate retry on the direct
 * executor could spin on a persistent failure).
 * <p>
 * <b>Wakeups are lossless.</b> The publish-available callback fires only on the queue's 0→1 size transition, so a
 * wakeup that arrives while a read is already in flight is the only signal that command will ever send — and the
 * in-flight read may have executed before the command was persisted and come back empty (or failed). Such a
 * wakeup is therefore remembered under the monitor and replayed as one more prefetch when the read completes;
 * dropping it would strand a durable command invisibly until an adapter recreate or an Edge restart.
 * <p>
 * <b>The 0→1 gate needs a safety net beyond wakeups.</b> Three read outcomes leave the queue non-empty with no
 * callback ever due (it never re-empties): a failed read, a read the store completed <i>empty</i> after dropping
 * a payload-broken head, and a read-task failure that left an ownerless in-flight marker hiding the head. Each
 * records evidence and escalates stepwise: an empty read cross-checks the store's {@code size} and, if non-empty,
 * re-reads once immediately (the broken-head case), then sweeps the queue's in-flight markers (the ownerless-lease
 * case). The sweep is re-guarded under the monitor immediately before it issues — skipped if a command was leased
 * meanwhile — so it only ever runs while this backlog holds no head. A blanket sweep is safe against the residual
 * single-writer race because this synthetic shared queue has exactly one consumer (this backlog): a transiently
 * un-marked command can only be re-leased here, where the head-gate holds it, and is {@code removeShared}-deleted
 * on disposal. Beyond the sweep, and for failed reads/size-checks/sweeps, a {@code rearmRequested} flag asks the
 * wrapper's tick (via {@link #rearmIfRequested()}) to retry — rate-bounded by the tick period, never a spin, and
 * costing nothing on a genuinely drained queue; each tick re-runs the ladder from the top so a store that heals
 * recovers on its own. All recovery latches reset when a lease succeeds.
 * <p>
 * Shutdown caveat (accepted): after the persistence shutdown grace period, submitted reads return futures that
 * never complete — a prefetch in that window stays {@code fetching} silently. The process is exiting; durable
 * commands replay on restart.
 * <p>
 * <b>Every persistence call is guarded against a synchronous throw.</b> {@link ClientQueuePersistence} reports
 * failures through its futures, but submitting the work can itself throw (a rejected submission while the
 * single-writer shuts down, say). Each such call here is followed by a step that must not be skipped, and skipping
 * it wedges the tag silently until an adapter recreate: an unguarded {@code readShared} leaves {@code fetching}
 * stuck true so no later prefetch or {@link #rearmIfRequested()} can ever run again; an unguarded
 * {@code removeShared} skips the follow-up prefetch and records no evidence, on a queue whose 0→1 callback can
 * never fire again; an unguarded callback deregistration skips the lease release in {@link #close()}. So each site
 * catches, keeps its follow-up step, and routes the failure into the same tick-paced recovery an asynchronous
 * failure takes. A {@link VirtualMachineError} still propagates — the VM is going down, and a stuck flag is the
 * least of it.
 * <p>
 * Thread-safety: {@link #head()}/{@link #removeHead}/{@link #deadLetterHead} run under the delivering queue's
 * monitor (lock order queue→backlog); the read callback and the publish-available callback run on persistence
 * threads. All state is guarded by this backlog's monitor, and both the registered wakeup and every
 * {@link ClientQueuePersistence} call are made <b>outside</b> it.
 */
public final class ClientQueueSouthboundWriteBacklog implements SouthboundWriteBacklog {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ClientQueueSouthboundWriteBacklog.class);

    private static final int READ_LIMIT = 1;

    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull String queueId;
    private final @NotNull SouthboundPublishTranslator translator;
    private final @NotNull String adapterId;
    private final @NotNull String tagName;

    private @Nullable Runnable wakeup;
    private @Nullable SouthboundCommand head;
    private boolean fetching;
    private boolean closed;

    /**
     * Set when a prefetch request arrives while a read is in flight; consumed by that read's completion, which
     * then issues one more prefetch. This is what makes wakeups lossless: the in-flight read may predate the
     * arriving command and return empty, and the 0→1-transition callback will never fire for that command again.
     */
    private boolean wakeupPending;

    /**
     * Evidence that the store still holds commands the event-driven path cannot surface (failed read, failed size
     * check, or an empty read that exhausted the immediate recovery steps); consumed by {@link #rearmIfRequested()}
     * on the wrapper's tick. Cleared whenever a lease succeeds.
     */
    private boolean rearmRequested;

    /** One-shot latch: an empty read with a non-empty store re-reads immediately once. Reset on a lease. */
    private boolean emptyRecheckDone;

    /** One-shot latch: the in-flight-marker sweep runs once per quiet period. Reset on a lease. */
    private boolean markerSweepDone;

    /**
     * Throttles the marker-sweep WARN to once per incident: the sweep itself re-fires each tick (it is the recovery
     * mechanism for a persistent marker fault), but a permanently-degraded tag would otherwise log a WARN every
     * tick. Set on the first sweep, reset on a lease.
     */
    private boolean sweepWarnLogged;

    /**
     * Registers on the queue's publish-available callback and prefetches immediately, so commands already queued
     * (e.g. across a restart) surface without waiting for a new arrival.
     *
     * @param clientQueuePersistence the durable client queue store.
     * @param queueId                the shared-subscription queue id this tag's commands arrive on.
     * @param translator             turns a queued publish into the value to write.
     * @param adapterId              the owning adapter's id, for logging.
     * @param tagName                the tag this backlog feeds, for logging.
     */
    public ClientQueueSouthboundWriteBacklog(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull String queueId,
            final @NotNull SouthboundPublishTranslator translator,
            final @NotNull String adapterId,
            final @NotNull String tagName) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.queueId = queueId;
        this.translator = translator;
        this.adapterId = adapterId;
        this.tagName = tagName;
        clientQueuePersistence.addPublishAvailableCallback(ignored -> prefetch(), queueId);
        prefetch();
    }

    @Override
    public synchronized @Nullable SouthboundCommand head() {
        return head;
    }

    @Override
    public void removeHead(final @NotNull String id) {
        deleteHead(id, SouthboundWriteOutcome.SUCCEEDED, null);
    }

    @Override
    public void deadLetterHead(final @NotNull String id, final @NotNull String reason) {
        log.warn(
                "Dead-lettering southbound command '{}' for tag '{}' on adapter '{}': {}",
                id,
                tagName,
                adapterId,
                reason);
        deleteHead(id, SouthboundWriteOutcome.FAILED, reason);
    }

    @Override
    public synchronized void onAvailable(final @NotNull Runnable wakeup) {
        this.wakeup = wakeup;
    }

    /**
     * Deregister from the queue's publish-available callback and drop the cached lease, <b>clearing its in-flight
     * marker</b>: a leased-but-undisposed head would otherwise stay invisible to a successor backlog in the same
     * process (an adapter recreate), stranding the command until a full restart. The queue itself is left
     * untouched — it is durable, and a successor backlog (or a restarted Edge) picks its contents up.
     */
    @Override
    public void close() {
        final SouthboundCommand leased;
        synchronized (this) {
            closed = true;
            leased = head;
            head = null;
        }
        try {
            clientQueuePersistence.removePublishAvailableCallback(queueId);
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable deregistrationFailure) {
            // Must not skip the lease release below: an unreleased lease is invisible to a successor backlog in
            // this process and strands the command until a full restart — the very bug this close() exists to fix.
            log.warn(
                    "Failed to deregister the publish-available callback of southbound queue '{}' (tag '{}', "
                            + "adapter '{}') — releasing the lease regardless",
                    queueId,
                    tagName,
                    adapterId,
                    deregistrationFailure);
        }
        if (leased != null) {
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeInFlightMarker(queueId, leased.id()));
        }
    }

    private void deleteHead(
            final @NotNull String id, final @NotNull SouthboundWriteOutcome outcome, final @Nullable String reason) {
        final boolean closedBeforeSettle;
        synchronized (this) {
            closedBeforeSettle = closed;
            if (!closedBeforeSettle) {
                if (head == null || !id.equals(head.id())) {
                    throw new IllegalStateException("dispose of a command that is not the head: " + id);
                }
                head = null;
            }
        }
        if (closedBeforeSettle) {
            // The write settled after close() — a recreate or tags-only drop raced the device acknowledgment.
            // close() already released the lease, so the command stays durably queued and a successor backlog
            // redelivers it (at-least-once); swallow the disposal rather than blow up the settling thread.
            log.warn(
                    "Southbound command '{}' for tag '{}' on adapter '{}' settled {}{} after its backlog closed — "
                            + "left queued for a successor",
                    id,
                    tagName,
                    adapterId,
                    outcome,
                    reason != null ? " (" + reason + ")" : "");
            return;
        }
        try {
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, id));
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable removalFailure) {
            // The delete was never submitted. The command is still queued, so at-least-once holds — but this
            // backlog has already dropped its head, and the queue never re-empties, so its 0→1 callback can never
            // fire again. Record the evidence so the tick re-reads instead of leaving the tag stranded.
            log.error(
                    "Failed to submit the delete of southbound command '{}' for tag '{}' on adapter '{}' — will "
                            + "retry the read on the next tick",
                    id,
                    tagName,
                    adapterId,
                    removalFailure);
            synchronized (this) {
                if (!closed) {
                    rearmRequested = true;
                }
            }
        }
        prefetch();
    }

    /**
     * Lease the queue's head if nothing is cached and no read is in flight — a no-op otherwise. Safe to call
     * repeatedly; triggered at construction, by the publish-available callback, and after each deletion.
     */
    private void prefetch() {
        synchronized (this) {
            if (closed || head != null) {
                return;
            }
            if (fetching) {
                wakeupPending = true;
                return;
            }
            fetching = true;
            wakeupPending = false;
        }
        try {
            final ListenableFuture<ImmutableList<PUBLISH>> read = clientQueuePersistence.readShared(
                    queueId, READ_LIMIT, InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES);
            Futures.addCallback(
                    read,
                    new FutureCallback<>() {
                        @SuppressWarnings("NullAway") // Guava FutureCallback.onSuccess has @Nullable param
                        @Override
                        public void onSuccess(final ImmutableList<PUBLISH> publishes) {
                            onRead(publishes);
                        }

                        @Override
                        public void onFailure(final @NotNull Throwable throwable) {
                            onReadFailure(throwable);
                        }
                    },
                    MoreExecutors.directExecutor());
        } catch (final VirtualMachineError fatal) {
            throw fatal;
        } catch (final Throwable submissionFailure) {
            // The read never became a future, so no callback will ever reset `fetching` — treat it exactly as a
            // failed read: clear the flag, record the evidence, and let the tick retry.
            onReadFailure(submissionFailure);
        }
    }

    private void onRead(final @Nullable ImmutableList<PUBLISH> publishes) {
        if (publishes == null || publishes.isEmpty()) {
            final boolean reread;
            synchronized (this) {
                fetching = false;
                reread = wakeupPending && !closed;
            }
            if (reread) {
                // A command arrived while this read was in flight: the empty result is stale, and the arrival's
                // wakeup was absorbed by the fetching guard — this re-read is its replay.
                prefetch();
            } else {
                // "Empty" is trusted only after the store confirms it: a payload-broken head or an ownerless
                // in-flight marker makes a read complete empty while commands still queue behind it.
                verifyStoreDrained();
            }
            return;
        }
        final PUBLISH publish = publishes.getFirst(); // READ_LIMIT = 1
        final boolean closedBeforeRead;
        synchronized (this) {
            closedBeforeRead = closed;
            if (closedBeforeRead) {
                fetching = false;
            }
        }
        if (closedBeforeRead) {
            // The read completed after close(): it leased the command broker-side, but nobody owns that lease
            // anymore — close() could only release the cached head, not a read still in flight. Release the
            // in-flight marker so a successor backlog can lease the command instead of it stranding.
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeInFlightMarker(queueId, publish.getUniqueId()));
            return;
        }
        final DataPoint value = translate(publish);
        if (value == null) {
            // Untranslatable: dead-letter it here and lease the next — a malformed command never wedges the tag.
            synchronized (this) {
                fetching = false;
            }
            log.warn(
                    "Southbound publish '{}' on topic '{}' for tag '{}' on adapter '{}' is untranslatable — "
                            + "dead-lettered",
                    publish.getUniqueId(),
                    publish.getTopic(),
                    tagName,
                    adapterId);
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, publish.getUniqueId()));
            prefetch();
            return;
        }
        final Runnable nudge;
        final boolean closedMeanwhile;
        synchronized (this) {
            fetching = false;
            closedMeanwhile = closed;
            if (closedMeanwhile) {
                nudge = null;
            } else {
                head = new SouthboundCommand(publish.getUniqueId(), value);
                // A successful lease proves the store is readable again: re-arm the recovery ladder.
                rearmRequested = false;
                emptyRecheckDone = false;
                markerSweepDone = false;
                sweepWarnLogged = false;
                nudge = wakeup;
            }
        }
        if (closedMeanwhile) {
            // close() landed between the entry check and here — same ownerless lease as above; release it.
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeInFlightMarker(queueId, publish.getUniqueId()));
            return;
        }
        if (nudge != null) {
            nudge.run();
        }
    }

    private void onReadFailure(final @NotNull Throwable throwable) {
        final boolean reread;
        synchronized (this) {
            fetching = false;
            if (closed) {
                return;
            }
            // No arrival may ever signal again (0→1 only) — ask the tick to retry, rate-bounded.
            rearmRequested = true;
            reread = wakeupPending;
        }
        // No unconditional in-place retry: an immediate retry on the direct executor could spin on a persistent
        // failure. A wakeup absorbed while this read was in flight is replayed immediately; otherwise the next
        // tick re-arms — each attempt consumes its trigger, so a persistently failing store retries once per
        // arrival or tick, never in a loop.
        log.error(
                "Failed to read the southbound queue '{}' for tag '{}' on adapter '{}' — will retry on the next "
                        + "tick or arrival",
                queueId,
                tagName,
                adapterId,
                throwable);
        if (reread) {
            prefetch();
        }
    }

    /**
     * The tick-driven safety net (reached from the wrapper's tick through the write plane): re-issue one prefetch
     * if a previous read left evidence of undelivered commands. A no-op in every other state. Any thread.
     * <p>
     * Each tick-driven attempt re-runs the ladder from the top: the one-shot recheck/sweep latches are cleared here
     * so a store that keeps a read empty across ticks (e.g. a persistent re-marking fault) gets a fresh recheck and
     * a fresh sweep every tick, and recovers on its own once the store heals — otherwise a single burned sweep would
     * wedge the tag at the tick-paced rung forever, since the latches otherwise reset only on a successful lease.
     * The tick period is the rate bound, so this is a paced retry, never a spin.
     */
    @Override
    public void rearmIfRequested() {
        synchronized (this) {
            if (closed || fetching || head != null || !rearmRequested) {
                return;
            }
            rearmRequested = false;
            emptyRecheckDone = false;
            markerSweepDone = false;
        }
        prefetch();
    }

    /**
     * An empty read is trusted only if the store agrees it is drained. On disagreement, escalate stepwise — one
     * immediate re-read (a payload-broken head was dropped by the store; the next command is leasable), then an
     * in-flight-marker sweep (an ownerless marker from a read that failed after marking hides the head; the sweep
     * is re-guarded so it runs only while this backlog holds no head — see {@link #onEmptyReadWithNonEmptyStore} —
     * and its own wakeup re-enters the normal prefetch path), then tick-paced re-arms. The latches reset when a
     * lease succeeds.
     */
    private void verifyStoreDrained() {
        synchronized (this) {
            if (closed || fetching || head != null) {
                return;
            }
        }
        Futures.addCallback(
                clientQueuePersistence.size(queueId, true),
                new FutureCallback<>() {
                    @SuppressWarnings("NullAway") // Guava FutureCallback.onSuccess has @Nullable param
                    @Override
                    public void onSuccess(final Integer size) {
                        if (size != null && size > 0) {
                            onEmptyReadWithNonEmptyStore(size);
                        }
                    }

                    @Override
                    public void onFailure(final @NotNull Throwable throwable) {
                        synchronized (ClientQueueSouthboundWriteBacklog.this) {
                            if (!closed) {
                                rearmRequested = true;
                            }
                        }
                    }
                },
                MoreExecutors.directExecutor());
    }

    private void onEmptyReadWithNonEmptyStore(final int size) {
        final int step;
        synchronized (this) {
            if (closed || fetching || head != null) {
                return; // the evidence is stale — a newer read is running or already leased
            }
            if (!emptyRecheckDone) {
                emptyRecheckDone = true;
                step = 0;
            } else if (!markerSweepDone) {
                markerSweepDone = true;
                step = 1;
            } else {
                rearmRequested = true;
                step = 2;
            }
        }
        switch (step) {
            case 0 -> prefetch();
            case 1 -> {
                // Re-check under the monitor immediately before the sweep. The step decision above proved
                // head==null, but released the monitor; a command may have been leased since — a real 0→1 arrival,
                // the healthy recovery. If so, skip the sweep: there is no ownerless marker to clear, and a blanket
                // sweep could clear the marker of that freshly-leased LIVE head. (A residual single-writer window
                // remains — a lease read submitted between this check and the sweep task — but it is harmless: this
                // synthetic shared queue has exactly one consumer, this backlog, so a transiently un-marked command
                // can only be re-leased here, where the head-gate already holds it, and terminal disposal
                // removeShared-deletes it.)
                final boolean stillOwnerless;
                final boolean firstWarn;
                synchronized (this) {
                    stillOwnerless = !closed && !fetching && head == null;
                    firstWarn = stillOwnerless && !sweepWarnLogged;
                    if (stillOwnerless) {
                        sweepWarnLogged = true;
                    }
                }
                if (!stillOwnerless) {
                    return; // a lease covered it (or we closed) — no ownerless marker, no sweep needed
                }
                // WARN once per incident: the sweep re-fires each tick (it is the recovery mechanism for a
                // persistent marker fault), but a permanently-degraded tag must not spam a WARN every 50ms tick.
                if (firstWarn) {
                    log.warn(
                            "Southbound queue '{}' for tag '{}' on adapter '{}' reads empty while the store holds {} "
                                    + "command(s) — releasing possibly stranded in-flight markers",
                            queueId,
                            tagName,
                            adapterId,
                            size);
                } else {
                    log.debug(
                            "Southbound queue '{}' for tag '{}' on adapter '{}' still reads empty with {} command(s) "
                                    + "held — re-sweeping stranded in-flight markers",
                            queueId,
                            tagName,
                            adapterId,
                            size);
                }
                // A successful sweep fires the store's publish-available callback, which re-enters prefetch — the
                // recovery. A FAILED sweep (the store rejected the op) must not lose the evidence: with both ladder
                // rungs already burned, nothing else would re-arm, and the 0→1 callback can never fire on a
                // non-empty queue — the tag would strand until recreate/restart. Record the evidence so the next
                // tick retries, exactly as a failed read or a failed size check does.
                Futures.addCallback(
                        clientQueuePersistence.removeAllInFlightMarkers(queueId),
                        new FutureCallback<>() {
                            @Override
                            public void onSuccess(final @Nullable Void ignored) {}

                            @Override
                            public void onFailure(final @NotNull Throwable throwable) {
                                log.error(
                                        "Failed to release in-flight markers for southbound queue '{}' (tag '{}', "
                                                + "adapter '{}') — will retry on the next tick",
                                        queueId,
                                        tagName,
                                        adapterId,
                                        throwable);
                                synchronized (ClientQueueSouthboundWriteBacklog.this) {
                                    if (!closed) {
                                        rearmRequested = true;
                                    }
                                }
                            }
                        },
                        MoreExecutors.directExecutor());
            }
            default -> {
                // Tick-paced from here: rearmRequested is set; each retry re-runs this ladder's evidence check.
            }
        }
    }

    private @Nullable DataPoint translate(final @NotNull PUBLISH publish) {
        try {
            return translator.translate(publish);
        } catch (final StackOverflowError failure) {
            // A pathological payload (e.g. absurd nesting) — attributable to the command, and the stack has
            // unwound by the time we are here: dead-letter it like any other untranslatable publish.
            log.debug("Southbound publish translation threw", failure);
            return null;
        } catch (final VirtualMachineError fatal) {
            throw fatal; // OOM and friends are not the command's fault — never swallow those
        } catch (final Throwable failure) {
            // Not just RuntimeException: an Error or a sneaky-thrown checked exception escaping here would
            // propagate into the future listener, leave `fetching` stuck true, and wedge the tag silently.
            log.debug("Southbound publish translation threw", failure);
            return null;
        }
    }
}
