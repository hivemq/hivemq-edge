/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.conformance;

import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class DrainOnCallDispatcher implements MessageDispatcher {
    private final @NotNull List<Binding<?>> bindings;

    public DrainOnCallDispatcher() {
        this.bindings = new ArrayList<>();
    }

    @Override
    public <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
            final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
        final Binding<MessageType> binding = new Binding<>(mailbox, handler);
        bindings.add(binding);
        return () -> bindings.remove(binding);
    }

    void drainAll() {
        long drained;
        do {
            drained = 0;
            for (final Binding<?> bin : bindings) {
                while (bin.drainOne()) {
                    drained++;
                }
            }
        } while (drained > 0);
    }

    private record Binding<T extends MailboxMessage>(
            @NotNull Mailbox<T> mailbox, @NotNull MessageHandler<T> handler) {
        boolean drainOne() {
            final @Nullable T msg = mailbox.poll();
            if (msg == null) {
                return false;
            }
            handler.receive(msg);
            return true;
        }
    }
}
