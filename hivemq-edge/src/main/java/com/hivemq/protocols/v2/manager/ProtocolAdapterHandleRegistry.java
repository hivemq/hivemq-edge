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
package com.hivemq.protocols.v2.manager;

import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.protocols.v2.southbound.SouthboundWritePlane;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The concurrent, REST-readable directory of an Edge instance's v2 adapters. Each entry is a
 * {@link ProtocolAdapterHandle} that exposes <b>only</b> the immutable status snapshot reference and the wrapper's
 * send-only mailbox handle — never the wrapper, its state, or its mailbox's receive side. REST threads and the
 * manager's own tick read snapshots and {@code tell} commands; nothing else crosses the boundary (the actor model).
 * <p>
 * The map is mutated only by the manager actor (register on create, remove on discard) but read from foreign
 * threads, so it is backed by a {@link ConcurrentHashMap}; the per-handle snapshot is an {@link AtomicReference}
 * published by the owning wrapper on its own dispatch thread.
 */
public final class ProtocolAdapterHandleRegistry {

    private final @NotNull ConcurrentMap<String, ProtocolAdapterHandle> handles = new ConcurrentHashMap<>();

    /**
     * @param adapterId the adapter instance id.
     * @return the handle for the adapter, or {@code null} if no adapter with that id is registered.
     */
    public @Nullable ProtocolAdapterHandle find(final @NotNull String adapterId) {
        return handles.get(adapterId);
    }

    /**
     * @return an immutable snapshot of all registered handles, safe to iterate from any thread.
     */
    public @NotNull Collection<ProtocolAdapterHandle> all() {
        return List.copyOf(handles.values());
    }

    /**
     * Register (or replace) the handle for its adapter id. Called only by the manager actor.
     *
     * @param handle the handle to register.
     */
    public void register(final @NotNull ProtocolAdapterHandle handle) {
        handles.put(handle.adapterId(), handle);
    }

    /**
     * Unregister the handle for the given adapter id, if any. Called only by the manager actor.
     *
     * @param adapterId the adapter instance id.
     */
    public void unregister(final @NotNull String adapterId) {
        handles.remove(adapterId);
    }

    /**
     * A REST-readable handle to one adapter: its id, the send-only handle of its wrapper mailbox, the reference
     * the wrapper publishes its immutable status snapshot into, and the adapter's southbound write plane — the
     * producer-facing intake southbound commands are offered to. For an adapter whose type has no
     * registered factory the wrapper sender is a no-op, the snapshot is a fixed {@code ERROR}, and there is no
     * write plane.
     *
     * @param adapterId           the adapter instance id.
     * @param wrapperSender       the send-only handle of the wrapper mailbox — {@code tell}-only, callable from any
     *                            thread.
     * @param snapshot            the reference holding the wrapper's latest immutable status snapshot.
     * @param southboundWritePlane the adapter's southbound delivery side (one queue per write-mapped tag), or
     *                            {@code null} for an adapter with no running wrapper.
     */
    public record ProtocolAdapterHandle(
            @NotNull String adapterId,
            @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
            @NotNull AtomicReference<AdapterStatusSnapshot> snapshot,
            @Nullable SouthboundWritePlane southboundWritePlane) {

        /**
         * A handle with no southbound write plane — for adapters with no running wrapper and tests that never
         * exercise the write path.
         *
         * @param adapterId     the adapter instance id.
         * @param wrapperSender the send-only handle of the wrapper mailbox.
         * @param snapshot      the reference holding the wrapper's latest immutable status snapshot.
         */
        public ProtocolAdapterHandle(
                final @NotNull String adapterId,
                final @NotNull MailboxSender<ProtocolAdapterWrapperMessage> wrapperSender,
                final @NotNull AtomicReference<AdapterStatusSnapshot> snapshot) {
            this(adapterId, wrapperSender, snapshot, null);
        }
    }
}
