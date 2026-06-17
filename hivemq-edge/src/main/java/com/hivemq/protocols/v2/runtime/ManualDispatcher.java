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

import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * Test {@link MessageDispatcher}: {@link #attach(Mailbox, MessageHandler)} records the binding and
 * {@link #drainAll()} delivers every queued message on the <b>calling thread</b> — fully deterministic, pairs
 * with {@link FakeClock}. No background threads; nothing happens until {@code drainAll()} is called.
 */
public final class ManualDispatcher implements MessageDispatcher {

    private final @NotNull List<Binding<?>> bindings = new CopyOnWriteArrayList<>();

    @Override
    public <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
            final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
        final Binding<MessageType> binding = new Binding<>(mailbox, handler);
        bindings.add(binding);
        return () -> bindings.remove(binding);
    }

    /**
     * Drain every attached mailbox until all are empty, delivering one message per mailbox per pass in
     * priority-band order. Messages a {@code receive} tells back to any attached mailbox — including its own — are
     * picked up by a later pass, so the drain reaches a fixed point before returning.
     */
    public void drainAll() {
        boolean delivered = true;
        while (delivered) {
            delivered = false;
            for (final Binding<?> binding : bindings) {
                if (binding.deliverOne()) {
                    delivered = true;
                }
            }
        }
    }

    private static final class Binding<MessageType extends MailboxMessage> {

        private final @NotNull Mailbox<MessageType> mailbox;
        private final @NotNull MessageHandler<MessageType> handler;

        private Binding(
                final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
            this.mailbox = mailbox;
            this.handler = handler;
        }

        private boolean deliverOne() {
            final MessageType message = mailbox.poll();
            if (message == null) {
                return false;
            }
            handler.receive(message);
            return true;
        }
    }
}
