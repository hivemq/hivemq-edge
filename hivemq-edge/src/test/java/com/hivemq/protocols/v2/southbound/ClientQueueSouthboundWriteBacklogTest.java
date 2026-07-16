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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.protocols.v2.tag.SouthboundWriteOutcome;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The {@link ClientQueueSouthboundWriteBacklog} over a scripted in-memory stand-in for
 * {@link com.hivemq.persistence.clientqueue.ClientQueuePersistence}: it leases the queue head by prefetching and
 * serves it idempotently, deletes only on a terminal outcome and leases the next, keeps an abandoned lease cached
 * for redelivery, self-dead-letters an untranslatable publish, does not spin on a read failure, and releases its
 * callback on close. The last test drives a real {@link SouthboundWriteQueue} over it, end to end.
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
    void close_releasesTheCallback_andDropsTheLease() {
        fake.enqueue(publish(1, "a"));
        final ClientQueueSouthboundWriteBacklog backlog = newBacklog();
        assertThat(backlog.head()).isNotNull();

        backlog.close();

        assertThat(fake.callbacks).isEmpty();
        assertThat(backlog.head()).isNull();
        assertThat(fake.pending()).isEqualTo(1); // the durable storage is untouched — that IS the durability
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
            delivered.add(sender.requests.get(sender.requests.size() - 1).value().getTagValue());
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
        private boolean failNextRead;

        private void enqueue(final @NotNull PUBLISH publish) {
            queue.addLast(publish);
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
            for (final PUBLISH publish : queue) {
                if (!leased.contains(publish.getUniqueId())) {
                    leased.add(publish.getUniqueId());
                    return Futures.immediateFuture(ImmutableList.of(publish));
                }
            }
            return Futures.immediateFuture(ImmutableList.of());
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
        public void addPublishAvailableCallback(
                final @NotNull PublishAvailableCallback callback, final @NotNull String queueId) {
            callbacks.put(queueId, callback);
        }

        @Override
        public void removePublishAvailableCallback(final @NotNull String queueId) {
            callbacks.remove(queueId);
        }
    }

    /** A send-only mailbox stand-in that records each write request and lets the test settle it as the adapter would. */
    private static final class CapturingSender implements MailboxSender<ProtocolAdapterWrapperMessage> {

        private final @NotNull List<ProtocolAdapterWrapperWriteRequest> requests = new ArrayList<>();

        @Override
        public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
            requests.add((ProtocolAdapterWrapperWriteRequest) message);
        }

        private void settleLast(final @NotNull SouthboundWriteOutcome outcome) {
            requests.get(requests.size() - 1).completion().settle(outcome);
        }
    }

    private record TestDataPoint(
            @NotNull String tagName, @NotNull Object value) implements DataPoint {

        @Override
        public @NotNull Object getTagValue() {
            return value;
        }

        @Override
        public @NotNull String getTagName() {
            return tagName;
        }
    }

    private static final class TestNode extends Node {

        private final @NotNull String identifier;

        private TestNode(final @NotNull String identifier) {
            this.identifier = identifier;
        }

        @Override
        public @NotNull String nodeId() {
            return identifier;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"identifier\":\"" + identifier + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE);
        }
    }
}
