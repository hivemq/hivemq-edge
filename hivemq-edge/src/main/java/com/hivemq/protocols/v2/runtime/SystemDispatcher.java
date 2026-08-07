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
 * {@link MessageDispatcherHandle#close()} stops the loop, interrupts the thread, and waits for an in-flight
 * {@code receive} to finish, so that closing a binding is a quiescence point a caller can tear down collaborators
 * behind. The stop flag is observed either as the interrupt thrown by {@code awaitNextMessage}, or
 * — when a {@code tell} races in and {@code awaitNextMessage} returns that message instead of throwing — by the
 * re-check after the fetch, so a message arriving alongside {@code close()} is never delivered.
 */
public final class SystemDispatcher implements MessageDispatcher {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SystemDispatcher.class);

    private static final long POLL_TIMEOUT_MILLIS = 1_000L;

    /** How long {@code close()} waits for the in-flight {@code receive} before giving up on it. */
    private static final long CLOSE_JOIN_TIMEOUT_MILLIS = 5_000L;

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
                        //
                        // Except for a fatal JVM condition, which the handler boundary deliberately rethrows: this
                        // backstop must not re-swallow it and keep dispatching on a JVM that cannot honour the work
                        // (Sam, round 2). The loop ends and the thread terminates with the error, which is what
                        // "not recoverable at adapter granularity" means.
                        AdapterFaults.rethrowIfFatal(exception);
                        log.error(
                                "Actor handler threw while processing a message; the dispatch loop continues",
                                exception);
                    }
                }
            }
        }

        /**
         * Stop the loop, interrupt the thread, and <b>wait for the in-flight {@code receive} to finish</b>.
         * <p>
         * The join is what makes {@code close()} a real quiescence point rather than a request for one. Callers
         * tear down an actor's collaborators immediately after closing its binding — the southbound delivery
         * plane, for instance, releases the durable queue's in-flight markers there — and every one of them
         * assumes no dispatch-thread work is still running. Without the join that assumption is simply false: the
         * loop only observes {@code running} between messages, so a {@code receive} already under way (a tick, and
         * with it a whole tag's poll and batch dispatch) runs on to completion afterwards and can re-acquire the
         * very resource teardown just released.
         * <p>
         * Bounded, because a handler that hangs must not hang shutdown too — a timeout leaves the old behaviour,
         * logged rather than silent. Skipped when the dispatch thread closes its own binding, which would
         * otherwise deadlock it against itself.
         * <p>
         * The caller's own interrupt status is <b>set aside for the duration of the join and restored
         * afterwards</b>. Teardown routinely runs on a thread that has just been interrupted — closing an actor's
         * binding interrupts it, and that actor's handler may itself be closing a child's binding — and
         * {@code Thread.join} throws immediately when the calling thread's flag is set. Without this, the join
         * would silently degrade to a no-op in exactly the case it was written for, and re-setting the flag would
         * poison every remaining join in the same teardown.
         */
        @Override
        public void close() {
            running = false;
            final Thread current = thread;
            if (current == null || current == Thread.currentThread()) {
                return;
            }
            current.interrupt();
            final boolean callerWasInterrupted = Thread.interrupted(); // clears the flag; restored below
            try {
                current.join(CLOSE_JOIN_TIMEOUT_MILLIS);
                if (current.isAlive()) {
                    log.warn(
                            "Dispatch thread '{}' did not finish its in-flight message within {} ms of close; "
                                    + "continuing teardown without it",
                            current.getName(),
                            CLOSE_JOIN_TIMEOUT_MILLIS);
                }
            } catch (final InterruptedException interrupted) {
                // Interrupted DURING the join — a second signal, not the one we set aside. Say so; the caller's
                // collaborators are about to be torn down behind a thread that may still be running.
                log.warn(
                        "Interrupted while waiting for dispatch thread '{}' to finish its in-flight message; "
                                + "continuing teardown without it",
                        current.getName());
                Thread.currentThread().interrupt();
            } finally {
                if (callerWasInterrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
