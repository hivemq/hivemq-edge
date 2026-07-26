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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The {@link ClientQueueSouthboundWriteBacklog} over a scripted in-memory stand-in for
 * {@link com.hivemq.persistence.clientqueue.ClientQueuePersistence}: it leases the queue head by prefetching and
 * serves it idempotently, deletes only on a terminal outcome and leases the next, keeps an abandoned lease cached
 * for redelivery, self-dead-letters an untranslatable publish, does not spin on a read failure, never loses a
 * wakeup that arrives while a read is in flight (empty or failing — the completion replays it), and releases its
 * callback on close — including the lease of a read that completes only after the close, and tolerating (as a
 * WARN no-op) a settle that arrives after it. The last test drives a real {@link SouthboundWriteQueue} over it,
 * end to end.
 */
class ClientQueueSouthboundWriteBacklogTest {

    private static final @NotNull String QUEUE_ID = "adapter-forwarder#a1/cmd/setpoint";

    private final @NotNull FakeClientQueue fake = new FakeClientQueue();

    @Test
    void commandsAlreadyQueued_surfaceOnConstruction_andHeadIsIdempotent() {
        fake.enqueue(publish(1, "a"));
        fake.enqueue(publish(2, "b"));

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        final SouthboundCommand first = backlog.head();
        assertThat(first).isNotNull();
        assertThat(first.value().getTagValue()).isEqualTo("a");
        // Idempotent: the same lease until deleted; only one readShared was issued for it.
        final SouthboundCommand again = backlog.head();
        assertThat(again).isNotNull();
        assertThat(again.id()).isEqualTo(first.id());
        assertThat(fake.reads).isEqualTo(1);
    }

    @Test
    void removeHead_deletesFromTheQueue_andLeasesTheNext_wakingTheListener() {
        fake.enqueue(publish(1, "a"));
        fake.enqueue(publish(2, "b"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        final AtomicInteger nudges = new AtomicInteger();
        backlog.onAvailable(nudges::incrementAndGet);
        final SouthboundCommand first = backlog.head();
        assertThat(first).isNotNull();

        backlog.removeHead(first.id());

        assertThat(fake.removed).containsExactly(first.id());
        final SouthboundCommand second = backlog.head();
        assertThat(second).isNotNull();
        assertThat(second.value().getTagValue()).isEqualTo("b");
        assertThat(nudges.get()).isEqualTo(1); // the next lease announced itself
    }

    @Test
    void deadLetterHead_deletesFromTheQueue() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();

        backlog.deadLetterHead(head.id(), "device rejected");

        assertThat(fake.removed).containsExactly(head.id());
        assertThat(backlog.head()).isNull();
    }

    @Test
    void abandonedLease_staysCached_forRedelivery() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();

        // An abandoned command needs no call at all: the lease simply stays.
        final SouthboundCommand redelivered = backlog.head();
        assertThat(redelivered).isNotNull();
        assertThat(redelivered.id()).isEqualTo(head.id());
        assertThat(fake.removed).isEmpty();
    }

    @Test
    void untranslatablePublish_isSelfDeadLettered_andTheNextIsLeased() {
        fake.enqueue(publish(1, "bad"));
        fake.enqueue(publish(2, "good"));

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("good");
        assertThat(fake.removed).hasSize(1); // the bad one was deleted, observably
    }

    @Test
    void deletingANonHeadCommandIsRejected() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNotNull();

        assertThatThrownBy(() -> backlog.removeHead("not-the-head")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> backlog.deadLetterHead("not-the-head", "reason"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readFailure_doesNotSpin_theNextArrivalRecovers() {
        fake.failNextRead = true;
        fake.enqueue(publish(1, "a"));

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        // The construction-time read failed; no in-place retry.
        assertThat(backlog.head()).isNull();
        assertThat(fake.reads).isEqualTo(1);

        // A new arrival fires the publish-available callback and recovers the lease.
        fake.enqueue(publish(2, "b"));
        fake.firePublishAvailable();
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a"); // FIFO: the older command still leases first
    }

    @Test
    void aWakeupDuringAnInFlightEmptyRead_isNotLost_theCompletionReplaysIt() {
        // EDG-813 review B1. The read is issued against an empty queue; the command arrives — and fires its one
        // and only publish-available callback (the broker signals only the 0→1 size transition) — while that read
        // is still undelivered. The stale empty result must not be the end of the story: dropping the absorbed
        // wakeup strands the command durably and invisibly until an adapter recreate or an Edge restart.
        fake.deferNextRead = true;
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog(); // construction-time read, in flight, empty

        fake.enqueue(publish(1, "a"));
        fake.firePublishAvailable(); // absorbed by the fetching guard — must be remembered
        fake.firePublishAvailable(); // a second wakeup in the same window must coalesce, not stack re-reads
        assertThat(backlog.head()).isNull(); // nothing leased while the read is in flight
        assertThat(fake.reads).isEqualTo(1);

        fake.completeDeferredRead(); // delivers the stale EMPTY result

        // The completion replayed the absorbed wakeup: exactly one follow-up read leased the command.
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a");
        assertThat(fake.reads).isEqualTo(2);
    }

    @Test
    void aWakeupDuringAnInFlightFailingRead_isNotLost_theFailureReplaysIt() {
        // Same lost-wakeup shape, failure flavor: the wakeup arrives while the read is in flight, and the read
        // then fails. Without the replay, "the next arriving command re-triggers the prefetch" never happens —
        // the queue is no longer empty, so no later arrival fires the 0→1 callback.
        fake.deferNextRead = true;
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog(); // construction-time read, in flight

        fake.enqueue(publish(1, "a"));
        fake.firePublishAvailable(); // absorbed by the fetching guard — must be remembered

        fake.failDeferredRead();

        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a");
        assertThat(fake.reads).isEqualTo(2); // one replay per absorbed wakeup — never a retry loop
    }

    @Test
    void theLostWakeupWindow_reopensAfterEveryDisposal_andIsStillCovered() {
        // The window is not a construction-time special: every deleteHead → prefetch that drains the queue
        // reopens it. Steady state: a command completes, the follow-up read is in flight against the now-empty
        // queue, and the next command arrives inside that read.
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        final SouthboundCommand first = backlog.head();
        assertThat(first).isNotNull();

        fake.deferNextRead = true;
        backlog.removeHead(first.id()); // the post-disposal prefetch is now in flight, and it evaluated EMPTY

        fake.enqueue(publish(2, "b"));
        fake.firePublishAvailable(); // absorbed
        fake.completeDeferredRead(); // stale empty delivered

        final SouthboundCommand second = backlog.head();
        assertThat(second).isNotNull();
        assertThat(second.value().getTagValue()).isEqualTo("b");
        assertThat(fake.reads).isEqualTo(3); // construction, post-disposal, replay
    }

    @Test
    void aFailedRead_withNoConcurrentArrival_isRetriedByTheTick_untilTheStoreRecovers() {
        // QA finding N1(a): a transient read failure on a non-empty queue used to strand it forever — the
        // publish-available callback fires only on the 0→1 size transition, which never recurs. The tick re-arms.
        fake.enqueue(publish(1, "a"));
        fake.failNextRead = true;
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNull();
        assertThat(fake.reads).isEqualTo(1);

        // First tick: the retry fails too (store still down) — evidence re-recorded, no spin in between.
        fake.failNextRead = true;
        backlog.rearmIfRequested();
        assertThat(backlog.head()).isNull();
        assertThat(fake.reads).isEqualTo(2);

        // Second tick: the store recovered — the command leases with no arrival ever needed.
        backlog.rearmIfRequested();
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a");
        assertThat(fake.reads).isEqualTo(3);

        // A tick with no evidence is free: no read is issued.
        backlog.removeHead(head.id());
        final int readsAfterDrain = fake.reads;
        backlog.rearmIfRequested();
        backlog.rearmIfRequested();
        assertThat(fake.reads).isEqualTo(readsAfterDrain);
    }

    @Test
    void anEmptyReadWithANonEmptyStore_isRecheckedImmediately_theBrokenHeadCase() {
        // QA finding N1(b): a payload-broken head makes the store complete a read EMPTY after dropping it, with
        // commands still queued behind — the size cross-check catches the lie and one immediate re-read leases.
        fake.enqueue(publish(2, "b"));
        fake.emptyReads = 1;

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("b");
        assertThat(fake.reads).isEqualTo(2); // the lying read + the recheck — no tick involved
        assertThat(fake.markerSweeps).isZero();
    }

    @Test
    void anOwnerlessLease_isRecoveredByTheMarkerSweep_theStrandedLeaseCase() {
        // QA finding N1(c): a read task that failed after in-flight marking leaves a lease nobody owns, hiding
        // the head from every read. Escalation: recheck (still empty) → sweep the queue's markers (we hold no
        // lease here, so every marker is ownerless) → the sweep's wakeup re-enters the normal path.
        final PUBLISH stranded = publish(1, "a");
        fake.enqueue(stranded);
        fake.strandLease(stranded);

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a");
        assertThat(fake.markerSweeps).isEqualTo(1);
        assertThat(fake.reads).isEqualTo(3); // skip-read, recheck, post-sweep lease
    }

    @Test
    void anExhaustedRecoveryLadder_fallsBackToTickPacedRearms_andLatchesResetOnALease() {
        // Three lying empty reads exhaust recheck and sweep; the fourth attempt must wait for a tick — never an
        // unbounded immediate loop. A successful lease re-arms the whole ladder for the next incident.
        fake.enqueue(publish(1, "a"));
        fake.emptyReads = 3;

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNull(); // ladder exhausted: recheck, sweep-triggered read both lied
        assertThat(fake.reads).isEqualTo(3);
        assertThat(fake.markerSweeps).isEqualTo(1);

        backlog.rearmIfRequested(); // the tick
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(fake.reads).isEqualTo(4);

        // The lease reset the latches: a second incident gets its immediate recheck again.
        backlog.removeHead(head.id());
        fake.enqueue(publish(2, "b"));
        fake.emptyReads = 1;
        fake.firePublishAvailable();
        final SouthboundCommand second = backlog.head();
        assertThat(second).isNotNull();
        assertThat(second.value().getTagValue()).isEqualTo("b");
    }

    @Test
    void aFailedSizeCheck_fallsBackToTheTick() {
        fake.enqueue(publish(1, "a"));
        fake.emptyReads = 1;
        fake.failSizeChecks = true;
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNull(); // evidence unknown — nothing immediate

        fake.failSizeChecks = false;
        backlog.rearmIfRequested(); // the tick retries; this read tells the truth
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a");
    }

    @Test
    void aFailedMarkerSweep_recordsEvidence_soTheTickRetries_ratherThanStranding() {
        // QA round-2 finding: a FAILED removeAllInFlightMarkers was only exception-logged and set no evidence.
        // With both ladder rungs already burned and the 0→1 callback unable to fire on a non-empty queue, the tag
        // stranded until recreate/restart. The failed sweep must record evidence like a failed read or size check.
        fake.enqueue(publish(1, "a"));
        fake.emptyReads = 2; // construction: read → recheck → sweep
        fake.failNextMarkerSweep = true;

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNull();
        assertThat(fake.markerSweeps).isEqualTo(1); // the sweep was attempted and failed

        // The store heals; because the failed sweep recorded evidence, the tick retries and leases — no strand.
        backlog.rearmIfRequested();
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a");
    }

    @Test
    void aStep2Livelock_isBrokenByEachTickReRunningTheLadder_notOnlyByALease() {
        // QA round-2 finding: the recheck/sweep latches reset only on a successful lease, so a store that keeps a
        // read empty past step 2 (a persistent re-marking fault) stayed wedged at the tick-paced rung forever —
        // surviving even the store's own recovery, because nothing re-tried the recheck or the sweep. Each tick
        // must re-run the ladder from the top.
        fake.enqueue(publish(1, "a"));
        fake.emptyReads = 6; // construction burns 3 (read/recheck/sweep-read); one full tick re-run burns 3 more

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNull();
        assertThat(fake.markerSweeps).isEqualTo(1); // construction reached step 2 with one sweep

        // A tick with the store STILL lying re-runs the ladder afresh — a fresh recheck and a fresh sweep, not a
        // jump straight to the burned tick-rung. Proven by a second sweep with no intervening lease.
        backlog.rearmIfRequested();
        assertThat(backlog.head()).isNull();
        assertThat(fake.markerSweeps).isEqualTo(2);

        // The store heals; the next tick leases.
        backlog.rearmIfRequested();
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("a");
    }

    @Test
    void aThrowableFromTheTranslator_deadLettersTheCommand_neverWedgesTheTag() {
        // QA finding N2: only RuntimeException used to be caught — an Error or sneaky-thrown checked exception
        // from the translator seam left `fetching` stuck true forever, silently. Now it dead-letters like any
        // untranslatable payload and the tag keeps flowing.
        fake.enqueue(publish(1, "assert"));
        fake.enqueue(publish(2, "good"));

        final ClientQueueSouthboundWriteBacklog backlog = new ClientQueueSouthboundWriteBacklog(
                fake,
                QUEUE_ID,
                publish -> {
                    final byte[] payload = publish.getPayload();
                    final String value = payload == null ? "" : new String(payload, UTF_8);
                    if ("assert".equals(value)) {
                        throw new AssertionError("scripted translator Error");
                    }
                    return new TestDataPoint("setpoint", value);
                },
                "a1",
                "setpoint");

        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("good");
        assertThat(fake.removed).hasSize(1); // the Error-throwing command was dead-lettered, observably
    }

    @Test
    void close_releasesTheCallbackAndTheLease_soASuccessorBacklogCanTakeOver() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        final SouthboundCommand leased = backlog.head();
        assertThat(leased).isNotNull();

        backlog.close();

        assertThat(fake.callbacks).isEmpty();
        assertThat(backlog.head()).isNull();
        assertThat(fake.pending()).isEqualTo(1); // the durable storage is untouched — that IS the durability

        // An adapter recreate in the same process: the successor backlog leases the very same command — the closed
        // backlog released its in-flight marker, so the head is not stranded until a restart.
        final ClientQueueSouthboundWriteBacklog successor = newBacklog();
        final SouthboundCommand released = successor.head();
        assertThat(released).isNotNull();
        assertThat(released.id()).isEqualTo(leased.id());
    }

    @Test
    void aReadCompletingAfterClose_releasesItsOwnLease_soASuccessorBacklogCanTakeOver() {
        fake.enqueue(publish(1, "a"));
        fake.deferNextRead = true;
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNull(); // the construction-time read is still outstanding

        // close() can only release the cached head — there is none; the outstanding read still leases broker-side.
        backlog.close();
        fake.completeDeferredRead();

        // The landed read detected the close and released the ownerless lease: a successor leases the command.
        final ClientQueueSouthboundWriteBacklog successor = newBacklog();
        final SouthboundCommand released = successor.head();
        assertThat(released).isNotNull();
        assertThat(released.value().getTagValue()).isEqualTo("a");
        assertThat(fake.removed).isEmpty(); // released, never deleted — the storage is untouched
    }

    @Test
    void aSettleArrivingAfterClose_isANoOp_theCommandStaysQueuedForASuccessor() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        final SouthboundCommand leased = backlog.head();
        assertThat(leased).isNotNull();

        // A recreate races the device acknowledgment: the backlog closes before the write settles.
        backlog.close();

        // The late disposal must not blow up the settling thread — and must not delete the command.
        assertThatCode(() -> backlog.removeHead(leased.id())).doesNotThrowAnyException();
        assertThatCode(() -> backlog.deadLetterHead(leased.id(), "late")).doesNotThrowAnyException();
        assertThat(fake.removed).isEmpty();
        assertThat(fake.pending()).isEqualTo(1); // still there for the successor to redeliver
    }

    @Test
    void aRealQueueOverTheBacklog_drainsTheDurableQueue_inFifoOrder() {
        fake.enqueue(publish(1, "a"));
        fake.enqueue(publish(2, "b"));
        fake.enqueue(publish(3, "c"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        final CapturingSender sender = new CapturingSender();
        final SouthboundWriteQueue queue = new SouthboundWriteQueue(sender, new TestNode("setpoint"), backlog);
        queue.resume(); // the plane opens the window on tagWritable; here the test does

        final List<Object> delivered = new ArrayList<>();
        while (queue.inFlight()) {
            delivered.add(sender.requests.getLast().value().getTagValue());
            sender.settleLast(SouthboundWriteOutcome.SUCCEEDED);
        }

        assertThat(delivered).containsExactly("a", "b", "c");
        assertThat(queue.committed()).isEqualTo(3);
        assertThat(fake.pending()).isZero(); // every command deleted from the durable queue on its terminal outcome
        assertThat(queue.windowViolations()).isZero();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    private @NotNull ClientQueueSouthboundWriteBacklog newBacklog() {
        return new ClientQueueSouthboundWriteBacklog(fake, QUEUE_ID, translator(), "a1", "setpoint");
    }

    /** UTF-8 payload → value; the payload "bad" is untranslatable. */
    private static @NotNull SouthboundPublishTranslator translator() {
        return publish -> {
            final byte[] payload = publish.getPayload();
            final String value = payload == null ? "" : new String(payload, UTF_8);
            return "bad".equals(value) ? null : new TestDataPoint("setpoint", value);
        };
    }

    private static @NotNull PUBLISH publish(final long publishId, final @NotNull String payload) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withTopic("cmd/setpoint")
                .withPublishId(publishId)
                .withHivemqId("hivemqId")
                .withPayload(payload.getBytes(UTF_8))
                .build();
    }

    /**
     * A scripted stand-in for the durable client queue, mirroring the shared-subscription semantics the backlog
     * relies on: {@code readShared} leases (marks in-flight, so a repeated read skips it), {@code removeShared}
     * deletes by unique id, and the publish-available callback is registered per queue id. Futures complete
     * immediately on the calling thread.
     */
    private static final class FakeClientQueue extends UnsupportedClientQueuePersistence {

        private final @NotNull Deque<PUBLISH> queue = new ArrayDeque<>();
        private final @NotNull Set<String> leased = new HashSet<>();
        private final @NotNull Map<String, PublishAvailableCallback> callbacks = new HashMap<>();
        private final @NotNull List<String> removed = new ArrayList<>();
        private int reads;
        private int markerSweeps;
        private boolean failNextRead;
        private boolean deferNextRead;
        private int emptyReads;
        private boolean failSizeChecks;
        private boolean failNextMarkerSweep;
        private @Nullable SettableFuture<ImmutableList<PUBLISH>> deferredRead;
        private @Nullable ImmutableList<PUBLISH> deferredResult;

        private void enqueue(final @NotNull PUBLISH publish) {
            queue.addLast(publish);
        }

        /** Lease a message to nobody — models a read task that failed after in-flight marking (ownerless lease). */
        private void strandLease(final @NotNull PUBLISH publish) {
            leased.add(publish.getUniqueId());
        }

        /**
         * Deliver the deferred read with the result it evaluated when it was issued — the real store executes the
         * read task at submission order and delivers the future later, so a command enqueued in between is NOT in
         * the result. That gap is where the lost-wakeup races live.
         */
        private void completeDeferredRead() {
            final SettableFuture<ImmutableList<PUBLISH>> read = requireNonNull(deferredRead);
            final ImmutableList<PUBLISH> result = requireNonNull(deferredResult);
            deferredRead = null;
            deferredResult = null;
            read.set(result);
        }

        /** Fail the deferred read now — a read that was in flight when its store broke. */
        private void failDeferredRead() {
            final SettableFuture<ImmutableList<PUBLISH>> read = requireNonNull(deferredRead);
            deferredRead = null;
            deferredResult = null;
            read.setException(new RuntimeException("scripted read failure"));
        }

        private void firePublishAvailable() {
            final PublishAvailableCallback callback = callbacks.get(QUEUE_ID);
            if (callback != null) {
                callback.onPublishAvailable(QUEUE_ID);
            }
        }

        private int pending() {
            return queue.size();
        }

        @Override
        public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readShared(
                final @NotNull String sharedSubscription, final int messageLimit, final long byteLimit) {
            reads++;
            if (failNextRead) {
                failNextRead = false;
                return Futures.immediateFailedFuture(new RuntimeException("scripted read failure"));
            }
            if (emptyReads > 0) {
                // Models the store completing a read EMPTY while the queue is non-empty (a payload-broken head
                // dropped by checkPayloadReference, or an ownerless in-flight marker hiding the head).
                emptyReads--;
                return Futures.immediateFuture(ImmutableList.of());
            }
            if (deferNextRead) {
                deferNextRead = false;
                deferredRead = SettableFuture.create();
                deferredResult = leaseFirstUnleased();
                return deferredRead;
            }
            return Futures.immediateFuture(leaseFirstUnleased());
        }

        @Override
        public @NotNull ListenableFuture<Integer> size(final @NotNull String queueId, final boolean shared) {
            if (failSizeChecks) {
                return Futures.immediateFailedFuture(new RuntimeException("scripted size failure"));
            }
            return Futures.immediateFuture(queue.size());
        }

        @Override
        public @NotNull ListenableFuture<Void> removeAllInFlightMarkers(final @NotNull String sharedSubscription) {
            markerSweeps++;
            if (failNextMarkerSweep) {
                // A store that rejected the op (e.g. an Xodus exclusive-txn abort): the future fails and, as the
                // real persistence does, the publish-available callback is NOT fired.
                failNextMarkerSweep = false;
                return Futures.immediateFailedFuture(new RuntimeException("scripted marker-sweep failure"));
            }
            leased.clear();
            firePublishAvailable(); // as the real persistence does after a sweep
            return Futures.immediateFuture(null);
        }

        private @NotNull ImmutableList<PUBLISH> leaseFirstUnleased() {
            for (final PUBLISH publish : queue) {
                if (leased.add(publish.getUniqueId())) {
                    return ImmutableList.of(publish);
                }
            }
            return ImmutableList.of();
        }

        @Override
        public @NotNull ListenableFuture<Void> removeShared(
                final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
            queue.removeIf(publish -> publish.getUniqueId().equals(uniqueId));
            leased.remove(uniqueId);
            removed.add(uniqueId);
            return Futures.immediateFuture(null);
        }

        @Override
        public @NotNull ListenableFuture<Void> removeInFlightMarker(
                final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
            leased.remove(uniqueId);
            firePublishAvailable();
            return Futures.immediateFuture(null);
        }

        @Override
        public void addPublishAvailableCallback(
                final @NotNull PublishAvailableCallback callback, final @NotNull String queueId) {
            callbacks.put(queueId, callback);
        }

        @Override
        public void removePublishAvailableCallback(final @NotNull String queueId) {
            callbacks.remove(queueId);
        }
    }
}
