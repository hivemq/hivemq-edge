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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The durable backlog of southbound commands for one tag — the system of record behind a
 * {@link SouthboundWriteQueue}. This is the storage seam over the MQTT client queue (production:
 * {@code ClientQueuePersistence}); the queue in front of the write aspect delivers the head command and tells the
 * backlog what became of it. The backlog holds the burst; the adapter never buffers.
 * <p>
 * The contract is <b>head-without-remove, then delete only on a terminal outcome</b>:
 * <ul>
 * <li>{@link #head()} returns the next command <b>without removing it</b> — a crash before its outcome is known
 *     replays it on restart (at-least-once);</li>
 * <li>{@link #removeHead} deletes a command the device acknowledged (the commit);</li>
 * <li>{@link #deadLetterHead} deletes an undeliverable command;</li>
 * <li>an <b>abandoned</b> command needs no call at all: it was never removed, so it stays at the head and is
 *     redelivered when delivery resumes.</li>
 * </ul>
 * {@link #onAvailable} registers a wakeup fired when a command arrives, so the delivering queue need not poll
 * (mirrors {@code ClientQueuePersistence.addPublishAvailableCallback}). Implementations must invoke the wakeup
 * <b>without holding their own lock</b>, so the queue can call straight back into {@link #head()} without
 * deadlock.
 * <p>
 * <b>Disposal must not throw in normal operation.</b> {@link #removeHead}/{@link #deadLetterHead} run on the
 * adapter's single dispatch thread, off the delivering queue's monitor; the only expected throw is an
 * {@link IllegalStateException} for a non-head id, which is a programming error. A throw for any other reason
 * (e.g. a persistence op failing synchronously) is a contract violation — the queue defensively releases the
 * delivery slot and advances rather than wedging the tag, but implementations must not rely on that: dispose
 * quietly and surface failures asynchronously (log, retry) instead.
 */
public interface SouthboundWriteBacklog extends AutoCloseable {

    /**
     * @return the head command without removing it, or {@code null} when the backlog is empty. Repeated calls
     *         return the same command until it is removed or dead-lettered.
     */
    @Nullable
    SouthboundCommand head();

    /**
     * Delete a command the device acknowledged — the commit that ends its at-least-once journey.
     *
     * @param id the {@link SouthboundCommand#id()} of the current head, as returned by {@link #head()}.
     */
    void removeHead(final @NotNull String id);

    /**
     * Route an undeliverable command aside (the device rejected a well-formed value — redelivering loops forever)
     * and delete it.
     *
     * @param id     the {@link SouthboundCommand#id()} of the current head, as returned by {@link #head()}.
     * @param reason the failure reason, for the dead-letter record.
     */
    void deadLetterHead(final @NotNull String id, final @NotNull String reason);

    /**
     * Register a wakeup invoked when a command becomes available, so the delivering queue resumes without
     * busy-polling. Must be invoked outside the backlog's own lock.
     *
     * @param wakeup the callback to run when a command is available.
     */
    void onAvailable(final @NotNull Runnable wakeup);

    /**
     * Tick-driven maintenance hook: re-issue a read if this backlog recorded evidence of undelivered commands the
     * event-driven path cannot recover on its own (the broker's publish-available callback fires only on the
     * queue's 0→1 size transition, so a failed or store-emptied read on a non-empty queue would otherwise strand
     * it). A cheap no-op unless such evidence exists — an idle drained tag costs a few field reads per tick. Any
     * thread may call it. Implementations that cannot lose signals ignore it.
     */
    default void rearmIfRequested() {}

    /**
     * Release whatever the backlog holds onto beyond its stored commands — callbacks, leases. A durable backlog's
     * <b>storage</b> is deliberately untouched: it outlives the backlog object by design (that is the durability),
     * and a successor picks its contents up. The in-memory stand-in has nothing to release.
     */
    @Override
    void close();
}
