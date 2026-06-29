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

import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessagePriority;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

class ManualDispatcherTest {

    @Test
    void drainsOnTheCallingThreadInPriorityOrder() {
        final ManualDispatcher dispatcher = new ManualDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final RecordingHandler handler = new RecordingHandler();
        dispatcher.attach(mailbox, handler);

        mailbox.tell(new TestMessage("data-1", MailboxMessagePriority.DATA));
        mailbox.tell(new TestMessage("data-2", MailboxMessagePriority.DATA));
        mailbox.tell(new TestMessage("control", MailboxMessagePriority.CONTROL));

        dispatcher.drainAll();

        assertThat(handler.labels()).containsExactly("control", "data-1", "data-2");
        assertThat(handler.threadNames()).containsExactly(Thread.currentThread().getName());
    }

    @Test
    void deliversMessagesToldBackDuringReceive() {
        final ManualDispatcher dispatcher = new ManualDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final List<String> received = new ArrayList<>();
        dispatcher.attach(mailbox, message -> {
            received.add(message.label());
            if ("first".equals(message.label())) {
                mailbox.tell(new TestMessage("follow-up", MailboxMessagePriority.EVENT));
            }
        });

        mailbox.tell(new TestMessage("first", MailboxMessagePriority.EVENT));
        dispatcher.drainAll();

        assertThat(received).containsExactly("first", "follow-up");
    }

    @Test
    void drainsEveryAttachedMailbox() {
        final ManualDispatcher dispatcher = new ManualDispatcher();
        final DefaultMailbox<TestMessage> mailboxA = new DefaultMailbox<>();
        final DefaultMailbox<TestMessage> mailboxB = new DefaultMailbox<>();
        final RecordingHandler handlerA = new RecordingHandler();
        final RecordingHandler handlerB = new RecordingHandler();
        dispatcher.attach(mailboxA, handlerA);
        dispatcher.attach(mailboxB, handlerB);

        mailboxA.tell(new TestMessage("a", MailboxMessagePriority.EVENT));
        mailboxB.tell(new TestMessage("b", MailboxMessagePriority.EVENT));
        dispatcher.drainAll();

        assertThat(handlerA.labels()).containsExactly("a");
        assertThat(handlerB.labels()).containsExactly("b");
    }

    @Test
    void closingAHandleStopsDeliveryToThatBinding() {
        final ManualDispatcher dispatcher = new ManualDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final RecordingHandler handler = new RecordingHandler();
        final MessageDispatcherHandle handle = dispatcher.attach(mailbox, handler);

        handle.close();
        mailbox.tell(new TestMessage("ignored", MailboxMessagePriority.EVENT));
        dispatcher.drainAll();

        assertThat(handler.labels()).isEmpty();
    }

    private record TestMessage(
            @NotNull String label, @NotNull MailboxMessagePriority band) implements MailboxMessage {
        @Override
        public @NotNull MailboxMessagePriority priority() {
            return band;
        }
    }

    private static final class RecordingHandler implements MessageHandler<TestMessage> {

        private final @NotNull List<String> labels = new ArrayList<>();
        private final @NotNull Set<String> threadNames = new LinkedHashSet<>();

        @Override
        public void receive(final @NotNull TestMessage message) {
            threadNames.add(Thread.currentThread().getName());
            labels.add(message.label());
        }

        private @NotNull List<String> labels() {
            return labels;
        }

        private @NotNull Set<String> threadNames() {
            return threadNames;
        }
    }
}
