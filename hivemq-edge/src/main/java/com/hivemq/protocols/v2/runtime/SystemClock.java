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

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;

/**
 * Production {@link Clock}: wall-clock time plus a single {@link ScheduledExecutorService} that tells ticks.
 * <p>
 * The scheduler thread's only action is {@code target.tell(tickMessage.get())} — it never touches actor state,
 * so the actor stays single-threaded. One daemon thread serves every schedule; closing a per-schedule handle
 * cancels just that schedule, and {@link #close()} shuts the whole clock down.
 */
public final class SystemClock implements Clock, AutoCloseable {

    private static final @NotNull AtomicLong THREAD_COUNTER = new AtomicLong();

    private final @NotNull ScheduledExecutorService scheduler;

    public SystemClock() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "nevsky-clock-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public long nowMillis() {
        return System.currentTimeMillis();
    }

    @Override
    public <MessageType extends MailboxMessage> @NotNull AutoCloseable scheduleTick(
            final long periodMillis,
            final @NotNull MailboxSender<MessageType> target,
            final @NotNull Supplier<MessageType> tickMessage) {
        if (periodMillis <= 0) {
            throw new IllegalArgumentException("tick period must be positive, but was " + periodMillis);
        }
        final ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
                () -> target.tell(tickMessage.get()), periodMillis, periodMillis, TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
