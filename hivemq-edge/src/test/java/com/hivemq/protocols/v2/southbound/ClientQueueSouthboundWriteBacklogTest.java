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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundArrival;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundRead;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperSouthboundMessage.SouthboundSize;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The durable store over a scripted stand-in for {@code ClientQueuePersistence}. It holds no delivery state at all
 * now: every method records an intent and every answer leaves as a mailbox message, so what these tests pin is the
 * translation between the two vocabularies — and that no failure mode of the persistence layer can leave the
 * delivery side waiting for an answer that never comes.
 */
class ClientQueueSouthboundWriteBacklogTest {

    private static final @NotNull String QUEUE_ID = "adapter-forwarder#a1/cmd/setpoint";
    private static final @NotNull String TAG = "setpoint";

    private final @NotNull FakeClientQueue fake = new FakeClientQueue();
    private final @NotNull RecordingSender sender = new RecordingSender();

    @Test
    void aReadLeasesTheHeadAndAnswersWithIt_withoutRemovingIt() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        backlog.requestRead(7);

        assertThat(sender.reads).hasSize(1);
        final SouthboundRead read = sender.reads.getFirst();
        assertThat(read.readToken()).isEqualTo(7);
        assertThat(read.failure()).isNull();
        assertThat(read.command()).isNotNull();
        assertThat(read.command().value().getTagValue()).isEqualTo("a");
        assertThat(fake.pending()).isEqualTo(1); // leased, not deleted — that is the at-least-once contract
    }

    @Test
    void anEmptyQueueAnswersWithNoCommandAndNoFailure() {
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        backlog.requestRead(1);

        assertThat(sender.reads).hasSize(1);
        assertThat(sender.reads.getFirst().command()).isNull();
        assertThat(sender.reads.getFirst().failure()).isNull();
    }

    @Test
    void aFailedReadIsReportedAsAFailure_notSilence() {
        fake.failNextRead = true;
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        backlog.requestRead(1);

        assertThat(sender.reads).hasSize(1);
        assertThat(sender.reads.getFirst().failure()).isNotNull();
    }

    @Test
    void aSynchronousSubmissionFailureIsReportedAsAFailedRead_soTheChannelIsNeverLeftWaiting() {
        // The persistence reports failures through its futures, but submitting the work can itself throw — a
        // rejected submission while the single-writer shuts down. An unanswered read would hold the channel's read
        // slot for good; answering it turns the fault into one retried poll.
        fake.throwOnNextRead = true;
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        assertThatCode(() -> backlog.requestRead(1)).doesNotThrowAnyException();

        assertThat(sender.reads).hasSize(1);
        assertThat(sender.reads.getFirst().failure()).isNotNull();
    }

    @Test
    void anUntranslatablePublishIsReportedByName_notDecidedAboutHere() {
        final PUBLISH untranslatable = publish(1, "bad");
        fake.enqueue(untranslatable);
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        backlog.requestRead(1);

        // Decoding is a pure function and happens here; deciding the command's fate is the delivery side's job, so
        // the answer names the command and this store deletes nothing on its own.
        final SouthboundRead answer = sender.reads.getFirst();
        assertThat(answer.command()).isNull();
        assertThat(answer.failure()).isNull();
        assertThat(answer.undeliverableCommandId()).isEqualTo(untranslatable.getUniqueId());
        assertThat(answer.undeliverableReason()).isEqualTo("the command could not be decoded");
        assertThat(fake.removed).isEmpty();
        assertThat(fake.pending()).isEqualTo(1);
    }

    @Test
    void aQos0CommandIsRefused_neverDelivered() {
        // QoS >= 1 is the durability precondition of the whole southbound contract, and a subscription's QoS does
        // not upgrade an incoming publish — so a QoS 0 command reaches the queue, is handed out and removed by one
        // read, and can vanish with no trace. Executing it would promise at-least-once for something the store can
        // lose. It is refused at the first point the original publish QoS is visible, so no device write happens.
        final PUBLISH atMostOnce = qos0Publish(1, "a");
        fake.enqueue(atMostOnce);
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        backlog.requestRead(1);

        final SouthboundRead answer = sender.reads.getFirst();
        assertThat(answer.command()).isNull(); // nothing is ever delivered to the aspect
        assertThat(answer.failure()).isNull();
        assertThat(answer.undeliverableCommandId()).isEqualTo(atMostOnce.getUniqueId());
        assertThat(answer.undeliverableReason()).contains("QoS 0");
    }

    @Test
    void aQos0CommandDoesNotBlockTheQos1CommandBehindIt() {
        fake.enqueue(qos0Publish(1, "dropped"));
        fake.enqueue(publish(2, "delivered"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        // The delivery side dead-letters the refused command — a delete it issues like any other — and reads on.
        backlog.requestRead(1);
        final SouthboundRead refused = sender.reads.getFirst();
        assertThat(refused.undeliverableCommandId()).isNotNull();
        backlog.delete(refused.undeliverableCommandId());
        backlog.requestRead(2);

        final SouthboundRead next = sender.reads.get(1);
        assertThat(next.command()).isNotNull();
        assertThat(next.command().value().getTagValue()).isEqualTo("delivered");
    }

    @Test
    void deleteRemovesTheCommandFromTheQueue() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        backlog.requestRead(1);
        final SouthboundCommand leased = sender.reads.getFirst().command();
        assertThat(leased).isNotNull();

        backlog.delete(leased.id());

        assertThat(fake.removed).containsExactly(leased.id());
        assertThat(fake.pending()).isZero();
    }

    @Test
    void aSynchronousDeleteFailureIsLoggedRatherThanThrown_theCommandStaysQueued() {
        // delete runs on the dispatch thread: an escaping throwable would fault the whole adapter. The command
        // simply stays queued, so at-least-once holds and the backstop poll finds it again.
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        fake.throwOnNextRemove = true;

        assertThatCode(() -> backlog.delete("hivemqId_pub_1")).doesNotThrowAnyException();

        assertThat(fake.pending()).isEqualTo(1);
    }

    @Test
    void aSizeRequestAnswersTheDepth() {
        fake.enqueue(publish(1, "a"));
        fake.enqueue(publish(2, "b"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();

        backlog.requestSize(3);

        assertThat(sender.sizes).hasSize(1);
        assertThat(sender.sizes.getFirst().readToken()).isEqualTo(3);
        assertThat(sender.sizes.getFirst().size()).isEqualTo(2);
    }

    @Test
    void releaseMarkersFreesAStrandedLease_soTheHeadIsVisibleAgain() {
        // A read task that failed after in-flight marking leaves a lease nobody owns, hiding the head from every
        // read. The sweep is the only way back.
        final PUBLISH stranded = publish(1, "a");
        fake.enqueue(stranded);
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        fake.strandLease(stranded); // after construction — construction itself sweeps
        backlog.requestRead(1);
        assertThat(sender.reads.getFirst().command()).isNull(); // hidden

        backlog.releaseMarkers();
        backlog.requestRead(2);

        assertThat(sender.reads.get(1).command()).isNotNull();
    }

    @Test
    void constructionSweepsTheMarkers_soASuccessorInheritsACleanQueue() {
        // A marker is what makes a command invisible: reads skip marked entries. Markers are cleared when a command
        // is deleted, or by close()'s sweep — neither of which covers one applied AFTER that sweep, which is what a
        // read still in flight at teardown does. The delivery side cannot recover it either: its own reads skip the
        // entry, and its depth cross-check only arms after three consecutive EMPTY reads, which a queue holding
        // other commands never produces. Sweeping at construction makes the successor's starting state clean
        // however its predecessor ended.
        final PUBLISH orphaned = publish(1, "a");
        fake.enqueue(orphaned);
        fake.strandLease(orphaned); // a marker left behind by whatever ran before us

        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        backlog.requestRead(1);

        assertThat(sender.reads.getFirst().command()).isNotNull();
    }

    @Test
    void closeSweepsEveryMarkerUnconditionally_soASuccessorCanTakeOver() {
        // One unconditional sweep covers both lease races at once: the head this backlog leased, and the lease of
        // a read still in flight when close landed. Neither is visible to a successor otherwise, and either would
        // strand its command until a full restart.
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog first = newBacklog();
        first.requestRead(1);
        assertThat(sender.reads.getFirst().command()).isNotNull();

        first.close();

        assertThat(fake.callbackQueueIds()).isEmpty();
        final RecordingSender successorSender = new RecordingSender();
        final ClientQueueSouthboundWriteBacklog successor =
                new ClientQueueSouthboundWriteBacklog(fake, QUEUE_ID, translator(), "a1", TAG, successorSender);
        successor.requestRead(1);
        assertThat(successorSender.reads.getFirst().command()).isNotNull();
    }

    @Test
    void aSynchronousDeregistrationFailureStillSweeps() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        backlog.requestRead(1);
        fake.throwOnCallbackDeregistration = true;

        assertThatCode(backlog::close).doesNotThrowAnyException();

        assertThat(fake.leasedCount()).isZero();
    }

    @Test
    void theArrivalCallbackIsAHint_toldStraightToTheMailbox() {
        newBacklog();

        fake.firePublishAvailable();

        assertThat(sender.arrivals).hasSize(1);
        assertThat(sender.arrivals.getFirst().tagName()).isEqualTo(TAG);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    private @NotNull ClientQueueSouthboundWriteBacklog newBacklog() {
        return new ClientQueueSouthboundWriteBacklog(fake, QUEUE_ID, translator(), "a1", TAG, sender);
    }

    /** UTF-8 payload → value; the payload "bad" is untranslatable. */
    private static @NotNull SouthboundPublishTranslator translator() {
        return publish -> {
            final byte[] payload = publish.getPayload();
            final String value = payload == null ? "" : new String(payload, UTF_8);
            return "bad".equals(value) ? null : new TestDataPoint(TAG, value);
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

    private static @NotNull PUBLISH qos0Publish(final long publishId, final @NotNull String payload) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_MOST_ONCE)
                .withOnwardQos(QoS.AT_MOST_ONCE)
                .withTopic("cmd/setpoint")
                .withPublishId(publishId)
                .withHivemqId("hivemqId")
                .withPayload(payload.getBytes(UTF_8))
                .build();
    }

    /** Sorts the store's answers by kind so each assertion reads for itself. */
    private static final class RecordingSender implements MailboxSender<ProtocolAdapterWrapperMessage> {

        private final @NotNull List<SouthboundRead> reads = new ArrayList<>();
        private final @NotNull List<SouthboundSize> sizes = new ArrayList<>();
        private final @NotNull List<SouthboundArrival> arrivals = new ArrayList<>();

        @Override
        public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
            switch (message) {
                case final SouthboundRead read -> reads.add(read);
                case final SouthboundSize size -> sizes.add(size);
                case final SouthboundArrival arrival -> arrivals.add(arrival);
                default -> throw new IllegalStateException("unexpected message from a store: " + message);
            }
        }
    }

    /**
     * A scripted stand-in for the durable client queue, mirroring the shared-subscription semantics the store
     * relies on: {@code readShared} leases (marks in-flight, so a repeated read skips it), {@code removeShared}
     * deletes by unique id, and the publish-available callback is registered per queue id. Futures complete
     * immediately on the calling thread.
     */
    private static final class FakeClientQueue extends UnsupportedClientQueuePersistence {

        private final @NotNull Deque<PUBLISH> queue = new ArrayDeque<>();
        private final @NotNull Set<String> leased = new HashSet<>();
        private final @NotNull Map<String, PublishAvailableCallback> callbacks = new HashMap<>();
        private final @NotNull List<String> removed = new ArrayList<>();
        private boolean failNextRead;
        private boolean throwOnNextRead;
        private boolean throwOnNextRemove;
        private boolean throwOnCallbackDeregistration;

        private void enqueue(final @NotNull PUBLISH publish) {
            queue.addLast(publish);
        }

        /** Lease a message to nobody — models a read task that failed after in-flight marking. */
        private void strandLease(final @NotNull PUBLISH publish) {
            leased.add(publish.getUniqueId());
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

        private int leasedCount() {
            return leased.size();
        }

        private @NotNull Set<String> callbackQueueIds() {
            return Set.copyOf(callbacks.keySet());
        }

        @Override
        public @NotNull ListenableFuture<ImmutableList<PUBLISH>> readShared(
                final @NotNull String sharedSubscription, final int messageLimit, final long byteLimit) {
            if (throwOnNextRead) {
                throwOnNextRead = false;
                throw new RuntimeException("scripted synchronous read-submission failure");
            }
            if (failNextRead) {
                failNextRead = false;
                return Futures.immediateFailedFuture(new RuntimeException("scripted read failure"));
            }
            for (final PUBLISH publish : queue) {
                if (leased.add(publish.getUniqueId())) {
                    return Futures.immediateFuture(ImmutableList.of(publish));
                }
            }
            return Futures.immediateFuture(ImmutableList.of());
        }

        @Override
        public @NotNull ListenableFuture<Integer> size(final @NotNull String queueId, final boolean shared) {
            return Futures.immediateFuture(queue.size());
        }

        @Override
        public @NotNull ListenableFuture<Void> removeAllInFlightMarkers(final @NotNull String sharedSubscription) {
            leased.clear();
            return Futures.immediateFuture(null);
        }

        @Override
        public @NotNull ListenableFuture<Void> removeShared(
                final @NotNull String sharedSubscription, final @NotNull String uniqueId) {
            if (throwOnNextRemove) {
                throwOnNextRemove = false;
                throw new RuntimeException("scripted synchronous delete-submission failure");
            }
            queue.removeIf(publish -> publish.getUniqueId().equals(uniqueId));
            leased.remove(uniqueId);
            removed.add(uniqueId);
            return Futures.immediateFuture(null);
        }

        @Override
        public void addPublishAvailableCallback(
                final @NotNull PublishAvailableCallback callback, final @NotNull String queueId) {
            callbacks.put(queueId, callback);
        }

        @Override
        public void removePublishAvailableCallback(final @NotNull String queueId) {
            if (throwOnCallbackDeregistration) {
                throwOnCallbackDeregistration = false;
                throw new RuntimeException("scripted synchronous deregistration failure");
            }
            callbacks.remove(queueId);
        }
    }
}
