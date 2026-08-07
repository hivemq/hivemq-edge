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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
            assertThat(handler.threadNames().iterator().next()).startsWith("protocol-adapter-dispatcher-");
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
    void closeWaitsForTheInFlightMessage_soTeardownCanReleaseWhatTheHandlerIsStillUsing() {
        // close() is a quiescence point, not a request for one. Callers tear down an actor's collaborators
        // immediately after — the southbound plane releases the durable queue's in-flight markers there — and a
        // handler still running would re-acquire what teardown just released.
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final CountDownLatch entered = new CountDownLatch(1);
        final AtomicBoolean finished = new AtomicBoolean();
        final MessageDispatcherHandle handle = dispatcher.attach(mailbox, message -> {
            entered.countDown();
            busyWork();
            finished.set(true);
        });

        mailbox.tell(new TestMessage("in-flight", MailboxMessagePriority.EVENT));
        await().pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(5))
                .atMost(TIMEOUT)
                .until(() -> entered.getCount() == 0);

        handle.close();

        assertThat(finished).isTrue();
    }

    @Test
    void closeStillWaits_whenTheClosingThreadHasAlreadyBeenInterrupted() {
        // Teardown routinely runs on a just-interrupted thread: closing an actor's binding interrupts it, and that
        // actor's handler may in turn close a child's binding. Thread.join throws immediately when the caller's
        // flag is set, so without setting it aside the join silently degrades to a no-op in exactly the case it
        // was written for — and re-setting the flag would poison every later join in the same teardown.
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final CountDownLatch entered = new CountDownLatch(1);
        final AtomicBoolean finished = new AtomicBoolean();
        final MessageDispatcherHandle handle = dispatcher.attach(mailbox, message -> {
            entered.countDown();
            busyWork();
            finished.set(true);
        });

        mailbox.tell(new TestMessage("in-flight", MailboxMessagePriority.EVENT));
        await().pollDelay(Duration.ZERO)
                .pollInterval(Duration.ofMillis(5))
                .atMost(TIMEOUT)
                .until(() -> entered.getCount() == 0);

        Thread.currentThread().interrupt();
        try {
            handle.close();
            assertThat(finished).isTrue();
            // And the caller's own interrupt is handed back, not swallowed.
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted(); // clear, so the flag does not leak into the next test
        }
    }

    @Test
    void closingOwnBindingFromTheDispatchThreadDoesNotDeadlock() {
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final AtomicReference<MessageDispatcherHandle> self = new AtomicReference<>();
        final AtomicBoolean returned = new AtomicBoolean();
        final MessageDispatcherHandle handle = dispatcher.attach(mailbox, message -> {
            self.get().close(); // a handler that closes its own binding must not join itself
            returned.set(true);
        });
        self.set(handle);

        mailbox.tell(new TestMessage("self-close", MailboxMessagePriority.EVENT));

        await().atMost(TIMEOUT).until(returned::get);
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

    // ── the backstop's two halves (EDG-824 #12 / Sam round 2) ───────────────────────────────────────────────────
    // The loop is the actor's heartbeat, so an ordinary throwable — including the LinkageError a mispackaged adapter
    // jar raises, the very case the wide catch exists for — must never kill it. A fatal JVM condition is the
    // opposite: the handler boundary deliberately rethrows it, and this backstop re-swallowing it would keep the
    // loop dispatching on a JVM that cannot honour the work.

    @Test
    void anOrdinaryErrorFromTheHandler_isContainedAndTheLoopKeepsBeating() {
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        final FaultyHandler handler = new FaultyHandler(() -> new LinkageError("mispackaged adapter jar"));
        try (MessageDispatcherHandle handle = dispatcher.attach(mailbox, handler)) {
            mailbox.tell(new TestMessage("boom", MailboxMessagePriority.EVENT));
            mailbox.tell(new TestMessage("after", MailboxMessagePriority.EVENT));

            await().atMost(TIMEOUT).until(() -> handler.count() == 2);

            assertThat(handler.uncaught()).isNull();
            assertThat(handler.dispatchThread()).isNotNull();
            assertThat(handler.dispatchThread().isAlive()).isTrue();
        }
    }

    @Test
    void aFatalJvmConditionFromTheHandler_endsTheLoopInsteadOfBeingSwallowed() {
        final SystemDispatcher dispatcher = new SystemDispatcher();
        final DefaultMailbox<TestMessage> mailbox = new DefaultMailbox<>();
        // InternalError is a VirtualMachineError that can be raised without endangering the build JVM, so no forked
        // process is needed: the boundary's decision is what is under test, not the JVM's reaction to real OOM.
        final FaultyHandler handler = new FaultyHandler(() -> new InternalError("simulated fatal JVM condition"));
        try (MessageDispatcherHandle handle = dispatcher.attach(mailbox, handler)) {
            mailbox.tell(new TestMessage("boom", MailboxMessagePriority.EVENT));

            // the error escapes run() and terminates the dispatch thread, captured by the per-thread handler the
            // faulty handler installs (so the expected death does not litter the build log's stderr)
            await().atMost(TIMEOUT).until(() -> handler.uncaught() != null);
            assertThat(handler.uncaught()).isInstanceOf(InternalError.class);
            await().atMost(TIMEOUT).until(() -> !handler.dispatchThread().isAlive());

            // and the loop is genuinely gone: a later message is not processed
            mailbox.tell(new TestMessage("after", MailboxMessagePriority.EVENT));
            await().pollDelay(QUIET_WINDOW).atMost(TIMEOUT).untilAsserted(() -> assertThat(handler.count())
                    .isEqualTo(1));
        }
    }

    /**
     * Work the close-time interrupt cannot cut short. A blocking call would throw {@code InterruptedException} the
     * moment {@code close()} interrupts, so a handler built on one finishes early whether or not {@code close()}
     * joins — and the test would pass against a {@code close()} that does not wait at all.
     */
    private static void busyWork() {
        final long until = System.nanoTime() + Duration.ofSeconds(1).toNanos();
        while (System.nanoTime() < until) {
            Thread.onSpinWait();
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

    /**
     * Counts every message and raises the supplied {@link Error} for the one labelled {@code "boom"}. It installs an
     * uncaught-exception handler on the dispatch thread itself — no global default is touched — so a thread the test
     * expects to die of a fatal error is observed directly instead of through stderr.
     */
    private static final class FaultyHandler implements MessageHandler<TestMessage> {

        private final @NotNull Supplier<Error> fault;
        private final @NotNull AtomicInteger count = new AtomicInteger();
        private final @NotNull AtomicReference<Throwable> uncaught = new AtomicReference<>();
        private volatile @Nullable Thread dispatchThread;

        private FaultyHandler(final @NotNull Supplier<Error> fault) {
            this.fault = fault;
        }

        @Override
        public void receive(final @NotNull TestMessage message) {
            final Thread current = Thread.currentThread();
            dispatchThread = current;
            current.setUncaughtExceptionHandler((thread, throwable) -> uncaught.set(throwable));
            count.incrementAndGet();
            if ("boom".equals(message.label())) {
                throw fault.get();
            }
        }

        private int count() {
            return count.get();
        }

        private @Nullable Throwable uncaught() {
            return uncaught.get();
        }

        private @Nullable Thread dispatchThread() {
            return dispatchThread;
        }
    }
}
