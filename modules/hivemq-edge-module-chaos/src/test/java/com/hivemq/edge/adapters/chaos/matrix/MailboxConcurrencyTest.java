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
package com.hivemq.edge.adapters.chaos.matrix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.protocols.v2.runtime.SystemDispatcher;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Mailbox thread-safety under the real actor runtime (S23): many producer threads {@code tell}
 * one mailbox while a single {@link SystemDispatcher} thread drains it. Proven with real threads and Awaitility —
 * never {@code Thread.sleep}. Every message is processed exactly once and sequentially (one {@code receive} at a
 * time), each producer's messages keep their order within each priority band, and a {@code CONTROL} message told
 * after a backlog is still delivered first.
 */
class MailboxConcurrencyTest {

    private static final int PRODUCERS = 8;
    private static final int MESSAGES_PER_PRODUCER = 250;

    @Test
    void manyProducers_oneDispatcherThread_processEveryMessageExactlyOnceAndSequentially() throws Exception {
        final Mailbox<Counted> mailbox = new DefaultMailbox<>();
        final RecordingHandler handler = new RecordingHandler();
        final SystemDispatcher dispatcher = new SystemDispatcher();
        try (final MessageDispatcherHandle _ = dispatcher.attach(mailbox, handler)) {
            final CountDownLatch start = new CountDownLatch(1);
            final List<Thread> producers = new ArrayList<>();
            for (int producer = 0; producer < PRODUCERS; producer++) {
                final int producerId = producer;
                final Thread thread = new Thread(() -> {
                    awaitQuietly(start);
                    for (int sequence = 0; sequence < MESSAGES_PER_PRODUCER; sequence++) {
                        // Alternate the band so per-producer FIFO is exercised within two bands at once.
                        final MailboxMessagePriority band =
                                sequence % 2 == 0 ? MailboxMessagePriority.DATA : MailboxMessagePriority.EVENT;
                        mailbox.tell(new Counted(producerId, sequence, band));
                    }
                });
                producers.add(thread);
                thread.start();
            }

            start.countDown(); // release all producers at once
            for (final Thread thread : producers) {
                thread.join();
            }

            final int expected = PRODUCERS * MESSAGES_PER_PRODUCER;
            await().atMost(Duration.ofSeconds(30)).until(() -> handler.received.size() == expected);

            // Exactly once.
            assertThat(handler.received).hasSize(expected);
            // Sequentially — the dispatcher never re-enters receive concurrently.
            assertThat(handler.maxConcurrentReceives.get()).isEqualTo(1);
            // Per-producer FIFO within each band.
            for (int producer = 0; producer < PRODUCERS; producer++) {
                assertMonotonicWithinBand(handler.received, producer, MailboxMessagePriority.DATA);
                assertMonotonicWithinBand(handler.received, producer, MailboxMessagePriority.EVENT);
            }
        }
    }

    @Test
    void controlMessageToldAfterADataBacklog_isDeliveredFirst() {
        final Mailbox<Counted> mailbox = new DefaultMailbox<>();
        mailbox.tell(new Counted(0, 0, MailboxMessagePriority.DATA));
        mailbox.tell(new Counted(0, 1, MailboxMessagePriority.DATA));
        mailbox.tell(new Counted(0, 2, MailboxMessagePriority.DATA));
        mailbox.tell(new Counted(0, 3, MailboxMessagePriority.CONTROL));

        final Counted first = mailbox.poll();

        assertThat(first).isNotNull();
        assertThat(first.priority()).isEqualTo(MailboxMessagePriority.CONTROL);
    }

    private static void assertMonotonicWithinBand(
            final @NotNull Queue<Counted> received, final int producer, final @NotNull MailboxMessagePriority band) {
        int previous = -1;
        for (final Counted message : received) {
            if (message.producer() == producer && message.priority() == band) {
                assertThat(message.sequence()).isGreaterThan(previous);
                previous = message.sequence();
            }
        }
    }

    private static void awaitQuietly(final @NotNull CountDownLatch latch) {
        try {
            final boolean ignored = latch.await(30, TimeUnit.SECONDS);
        } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting to start", interrupted);
        }
    }

    private record Counted(
            int producer, int sequence, @NotNull MailboxMessagePriority band) implements MailboxMessage {

        @Override
        public @NotNull MailboxMessagePriority priority() {
            return band;
        }
    }

    private static final class RecordingHandler implements MessageHandler<Counted> {

        private final @NotNull Queue<Counted> received = new ConcurrentLinkedQueue<>();
        private final @NotNull AtomicInteger inFlight = new AtomicInteger();
        private final @NotNull AtomicInteger maxConcurrentReceives = new AtomicInteger();

        @Override
        public void receive(final @NotNull Counted message) {
            final int concurrent = inFlight.incrementAndGet();
            maxConcurrentReceives.accumulateAndGet(concurrent, Math::max);
            received.add(message);
            inFlight.decrementAndGet();
        }
    }
}
