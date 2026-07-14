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
 * The durable backlog of southbound commands for one tag — the system of record the {@link SouthboundWritePump}
 * drives. This is the seam over the MQTT client queue (production: {@code ClientQueuePersistence}); the pump reads
 * one command, forwards it, and only then tells the source what became of it. The backlog lives here, never in the
 * adapter (see {@code SOUTHBOUND_MULTI_WRITE.md}).
 * <p>
 * The contract is <b>read-without-remove, then dispose by outcome</b>:
 * <ul>
 * <li>{@link #poll()} returns the next command <b>without removing it</b>, so a crash before disposition replays it;
 *     it returns {@code null} while a previously polled command has not yet been disposed (single-outstanding read,
 *     matching the single-in-flight adapter);</li>
 * <li>{@link #commit} removes a delivered command;</li>
 * <li>{@link #deadLetter} removes an undeliverable command after routing it aside (e.g. {@code $invalid/…});</li>
 * <li>{@link #release} keeps an abandoned command for redelivery (the write was aborted before a result).</li>
 * </ul>
 * {@link #onAvailable} registers a wakeup so the pump need not poll — it is nudged when a command arrives (mirrors
 * {@code ClientQueuePersistence.addPublishAvailableCallback}). Implementations must invoke the wakeup <b>without
 * holding their own lock</b>, so the pump can call back into {@link #poll()} without deadlock.
 */
public interface SouthboundCommandSource {

    /**
     * @return the next available command without removing it, or {@code null} if none is available or a previously
     *         polled command has not yet been disposed.
     */
    @Nullable
    SouthboundCommand poll();

    /**
     * Remove a command that was delivered to the device.
     *
     * @param id the {@link SouthboundCommand#id()} previously returned by {@link #poll()}.
     */
    void commit(@NotNull String id);

    /**
     * Keep an abandoned command for later redelivery — the in-flight write was aborted (the connection dropped or the
     * tag deactivated) before a result. The command becomes pollable again.
     *
     * @param id the {@link SouthboundCommand#id()} previously returned by {@link #poll()}.
     */
    void release(@NotNull String id);

    /**
     * Route an undeliverable command aside (the device rejected a well-formed value — retrying loops forever) and
     * remove it.
     *
     * @param id     the {@link SouthboundCommand#id()} previously returned by {@link #poll()}.
     * @param reason the failure reason, for the dead-letter record.
     */
    void deadLetter(@NotNull String id, @NotNull String reason);

    /**
     * Register a wakeup invoked when a command becomes available, so the pump resumes without busy-polling. Must be
     * invoked outside the source's own lock.
     *
     * @param wakeup the callback to run when a command is available.
     */
    void onAvailable(@NotNull Runnable wakeup);
}
