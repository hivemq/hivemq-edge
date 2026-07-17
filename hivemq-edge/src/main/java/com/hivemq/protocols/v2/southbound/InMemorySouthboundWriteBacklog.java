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

import static java.util.Objects.requireNonNull;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An <b>interim, in-memory</b> {@link SouthboundWriteBacklog}: a bounded FIFO that models the durable MQTT client
 * queue's shape and overflow policy but <b>is not durable</b> — its contents are lost on restart. It exists so the
 * {@link SouthboundWriteQueue} and its tests have a real backlog until a {@code ClientQueuePersistence}-backed one
 * is wired. Do not use it where durability is required.
 * <p>
 * It honours the backlog contract precisely: {@link #head()} exposes the head without removing it;
 * {@link #removeHead}/{@link #deadLetterHead} delete it — nothing else does, so an abandoned command is simply
 * still there to be redelivered. On overflow the <b>newest</b> offered command is shed observably (the backlog's
 * bound is the back-pressure limit). The wakeup registered via {@link #onAvailable} is invoked outside this
 * object's monitor.
 */
public final class InMemorySouthboundWriteBacklog implements SouthboundWriteBacklog {

    private final int capacity;
    private final @NotNull Deque<SouthboundCommand> pending;
    private final @NotNull List<SouthboundCommand> committedCommands;
    private final @NotNull List<SouthboundCommand> deadLetters;

    private @Nullable Runnable wakeup;
    private long nextId;
    private long offered;
    private long droppedByOverflow;
    private long committed;
    private long deadLettered;

    /**
     * @param capacity the maximum number of pending commands; offers beyond it shed the newest.
     */
    public InMemorySouthboundWriteBacklog(final int capacity) {
        this.capacity = capacity;
        this.pending = new ArrayDeque<>();
        this.committedCommands = new ArrayList<>();
        this.deadLetters = new ArrayList<>();
    }

    /**
     * Offer a new command (the "an MQTT write arrived" trigger). Enqueued if there is room, else shed. Nudges the
     * delivering queue via its wakeup, invoked outside this object's monitor so the queue can read the head back
     * without deadlock.
     *
     * @param value the value to write.
     */
    public void offer(final @NotNull DataPoint value) {
        final Runnable nudge;
        synchronized (this) {
            offered++;
            if (pending.size() >= capacity) {
                droppedByOverflow++;
                return;
            }
            pending.addLast(new SouthboundCommand(Long.toString(nextId++), value));
            nudge = wakeup;
        }
        if (nudge != null) {
            nudge.run();
        }
    }

    @Override
    public synchronized @Nullable SouthboundCommand head() {
        return pending.peekFirst();
    }

    @Override
    public synchronized void removeHead(final @NotNull String id) {
        // requireHead proved the deque non-empty, so pollFirst cannot return null.
        requireHead(id);
        committedCommands.add(requireNonNull(pending.pollFirst()));
        committed++;
    }

    @Override
    public synchronized void deadLetterHead(final @NotNull String id, final @NotNull String reason) {
        requireHead(id);
        deadLetters.add(requireNonNull(pending.pollFirst()));
        deadLettered++;
    }

    @Override
    public synchronized void onAvailable(final @NotNull Runnable wakeup) {
        this.wakeup = requireNonNull(wakeup);
    }

    @Override
    public void close() {
        // Nothing beyond the stored commands to release — and those die with this object anyway (not durable).
    }

    private void requireHead(final @NotNull String id) {
        final SouthboundCommand head = pending.peekFirst();
        if (head == null || !id.equals(head.id())) {
            throw new IllegalStateException("dispose of a command that is not the head: " + id);
        }
    }

    public synchronized int pendingSize() {
        return pending.size();
    }

    public synchronized long offered() {
        return offered;
    }

    public synchronized long droppedByOverflow() {
        return droppedByOverflow;
    }

    public synchronized long committed() {
        return committed;
    }

    public synchronized long deadLettered() {
        return deadLettered;
    }

    /**
     * @return the commands committed (delivered and acknowledged), in commit order — used to verify exactly which
     *         commands were executed.
     */
    public synchronized @NotNull List<SouthboundCommand> committedCommands() {
        return List.copyOf(committedCommands);
    }

    public synchronized @NotNull List<SouthboundCommand> deadLetters() {
        return List.copyOf(deadLetters);
    }
}
