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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An <b>interim, in-memory</b> {@link SouthboundCommandSource}: a bounded FIFO that models the durable MQTT client
 * queue's shape and overflow policy but <b>is not durable</b> — its contents are lost on restart. It exists so the
 * {@link SouthboundWritePump} and its tests have a real source until a {@code ClientQueuePersistence}-backed source
 * is wired (see {@code SOUTHBOUND_MULTI_WRITE.md} §12). Do not use it where durability is required.
 * <p>
 * It honours the source contract precisely: {@link #poll()} exposes the head without removing it and only one
 * command is outstanding at a time; {@link #commit}/{@link #deadLetter} remove the head; {@link #release} leaves it
 * at the head to be redelivered. On overflow the <b>newest</b> offered command is shed (the queue's bound is the
 * back-pressure limit). The wakeup registered via {@link #onAvailable} is invoked outside this object's monitor.
 */
public final class InMemorySouthboundCommandSource implements SouthboundCommandSource {

    private final int capacity;
    private final @NotNull Deque<SouthboundCommand> pending = new ArrayDeque<>();
    private final @NotNull List<SouthboundCommand> committedCommands = new ArrayList<>();
    private final @NotNull List<SouthboundCommand> deadLetters = new ArrayList<>();

    private @Nullable String heldId;
    private @Nullable Runnable wakeup;
    private long nextId;
    private long offered;
    private long droppedByOverflow;
    private long committed;
    private long released;
    private long deadLettered;

    /**
     * @param capacity the maximum number of pending commands; offers beyond it shed the newest.
     */
    public InMemorySouthboundCommandSource(final int capacity) {
        this.capacity = capacity;
    }

    /**
     * Offer a new command (the "an MQTT write arrived" trigger). Enqueued if there is room, else shed. Nudges the
     * pump via its wakeup, invoked outside this object's monitor so the pump can poll back without deadlock.
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
    public synchronized @Nullable SouthboundCommand poll() {
        if (heldId != null) {
            return null; // a command is already outstanding; single-in-flight
        }
        final SouthboundCommand head = pending.peekFirst();
        if (head == null) {
            return null;
        }
        heldId = head.id();
        return head;
    }

    @Override
    public synchronized void commit(final @NotNull String id) {
        requireHeld(id);
        final SouthboundCommand done = pending.pollFirst();
        heldId = null;
        committed++;
        if (done != null) {
            committedCommands.add(done);
        }
    }

    @Override
    public synchronized void release(final @NotNull String id) {
        requireHeld(id);
        heldId = null; // the head stays in place, to be polled again
        released++;
    }

    @Override
    public synchronized void deadLetter(final @NotNull String id, final @NotNull String reason) {
        requireHeld(id);
        final SouthboundCommand dead = pending.pollFirst();
        heldId = null;
        deadLettered++;
        if (dead != null) {
            deadLetters.add(dead);
        }
    }

    @Override
    public synchronized void onAvailable(final @NotNull Runnable wakeup) {
        this.wakeup = wakeup;
    }

    private void requireHeld(final @NotNull String id) {
        if (!id.equals(heldId)) {
            throw new IllegalStateException("dispose of a command that is not the outstanding one: " + id);
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

    public synchronized long released() {
        return released;
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
