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
package com.hivemq.protocols.v2.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Real threads + Awaitility, never {@code Thread.sleep}: one dedicated dispatch thread processes messages
 * sequentially and stays parked (no busy-poll) until told.
 */
class SystemDispatcherTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(5);
    private static final @NotNull Duration QUIET_WINDOW = Duration.ofMillis(300);

    @Test
    void processesMessagesSequentiallyInTellOrderOnOneThread() {
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final RecordingHandler handler = new RecordingHandler();
        try (MessageDispatcherHandle handle = dispatcher.attach(mailbox, handler)) {
            for (int i = 0; i < 5; i++) {
                mailbox.tell(new TestMessage("m" + i, MailboxMessagePriority.EVENT));
            }

            await().atMost(TIMEOUT).until(() -> handler.count() == 5);

            assertThat(handler.labels()).containsExactly("m0", "m1", "m2", "m3", "m4");
            assertThat(handler.threadNames()).hasSize(1);
            assertThat(handler.threadNames().iterator().next()).startsWith("nevsky-dispatcher-");
        }
    }

    @Test
    void closeStopsTheLoop() {
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final RecordingHandler handler = new RecordingHandler();
        final MessageDispatcherHandle handle = dispatcher.attach(mailbox, handler);

        mailbox.tell(new TestMessage("before-close", MailboxMessagePriority.EVENT));
        await().atMost(TIMEOUT).until(() -> handler.count() == 1);

        handle.close();
        mailbox.tell(new TestMessage("after-close", MailboxMessagePriority.EVENT));

        await().pollDelay(QUIET_WINDOW).atMost(TIMEOUT).untilAsserted(() -> assertThat(handler.count())
                .isEqualTo(1));
    }

    @Test
    void parkedDispatcherStaysQuietUntilTold() {
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final RecordingHandler handler = new RecordingHandler();
        try (MessageDispatcherHandle handle = dispatcher.attach(mailbox, handler)) {
            // No tell yet: the handler must never be invoked while the mailbox is empty (no busy-poll).
            await().pollDelay(QUIET_WINDOW).atMost(TIMEOUT).untilAsserted(() -> assertThat(handler.count())
                    .isZero());

            // A tell wakes the parked thread promptly.
            mailbox.tell(new TestMessage("wake", MailboxMessagePriority.EVENT));
            await().atMost(TIMEOUT).until(() -> handler.count() == 1);
        }
    }

    private record TestMessage(
            @NotNull String label, @NotNull MailboxMessagePriority band) implements MailboxMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return band;
        }
    }

    private static final class RecordingHandler implements MessageHandler<TestMessage> {

        private final @NotNull List<String> labels = new CopyOnWriteArrayList<>();
        private final @NotNull Set<String> threadNames = ConcurrentHashMap.newKeySet();
        private final @NotNull AtomicInteger count = new AtomicInteger();

        @Override
        public void receive(final @NotNull TestMessage message) {
            threadNames.add(Thread.currentThread().getName());
            labels.add(message.label());
            count.incrementAndGet();
        }

        private int count() {
            return count.get();
        }

        private @NotNull List<String> labels() {
            return labels;
        }

        private @NotNull Set<String> threadNames() {
            return threadNames;
        }
    }
}
