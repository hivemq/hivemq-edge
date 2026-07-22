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
import java.util.concurrent.atomic.AtomicLong;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Production {@link MessageDispatcher}: one dedicated daemon thread per handler that blocks in
 * {@link Mailbox#awaitNextMessage(long)} and feeds {@link MessageHandler#receive(MailboxMessage)} one message at
 * a time, in priority-band order. A parked thread consumes no CPU — it is woken by a {@code tell}, not by polling.
 * <p>
 * {@link MessageDispatcherHandle#close()} stops the loop and interrupts the thread; an in-flight {@code receive}
 * always completes first. The stop flag is observed either as the interrupt thrown by {@code awaitNextMessage}, or
 * — when a {@code tell} races in and {@code awaitNextMessage} returns that message instead of throwing — by the
 * re-check after the fetch, so a message arriving alongside {@code close()} is never delivered.
 */
public final class SystemDispatcher implements MessageDispatcher {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SystemDispatcher.class);

    private static final long POLL_TIMEOUT_MILLIS = 1_000L;
    private static final @NotNull AtomicLong THREAD_COUNTER = new AtomicLong();

    @Override
    public <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
            final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
        final DispatchLoop<MessageType> loop = new DispatchLoop<>(mailbox, handler);
        final Thread thread = new Thread(loop, "protocol-adapter-dispatcher-" + THREAD_COUNTER.incrementAndGet());
        thread.setDaemon(true);
        loop.bind(thread);
        thread.start();
        return loop;
    }

    private static final class DispatchLoop<MessageType extends MailboxMessage>
            implements Runnable, MessageDispatcherHandle {

        private final @NotNull Mailbox<MessageType> mailbox;
        private final @NotNull MessageHandler<MessageType> handler;
        private volatile boolean running = true;
        private volatile @Nullable Thread thread;

        private DispatchLoop(
                final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
            this.mailbox = mailbox;
            this.handler = handler;
        }

        private void bind(final @NotNull Thread thread) {
            this.thread = thread;
        }

        @Override
        public void run() {
            while (running) {
                final MessageType message;
                try {
                    message = mailbox.awaitNextMessage(POLL_TIMEOUT_MILLIS);
                } catch (final InterruptedException interrupted) {
                    // close() is the only interrupter; restore the flag and let the thread end.
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!running) {
                    // close() raced with an arriving message: awaitNextMessage returned it instead of throwing the
                    // interrupt. The loop is stopping, so the post-close message is dropped, not delivered.
                    break;
                }
                if (message != null) {
                    try {
                        handler.receive(message);
                    } catch (final Throwable exception) {
                        // The dispatch loop is the actor's heartbeat: a throwing handler must never kill it —
                        // a dead loop leaves a mailbox that accepts tells nobody processes and a stale snapshot
                        // that reads healthy forever (EDG-824 #7). Handlers guard their own state; this is the
                        // backstop.
                        log.error(
                                "Actor handler threw while processing a message; the dispatch loop continues",
                                exception);
                    }
                }
            }
        }

        @Override
        public void close() {
            running = false;
            final Thread current = thread;
            if (current != null) {
                current.interrupt();
            }
        }
    }
}
