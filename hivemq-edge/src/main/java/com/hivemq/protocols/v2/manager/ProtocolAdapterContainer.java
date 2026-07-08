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

import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One adapter the manager owns: its REST-readable {@link ProtocolAdapterHandle} plus the teardown resources the
 * manager must close when the adapter is discarded or recreated — the dispatcher binding, the periodic tick, and
 * the per-adapter metrics. The last-applied configuration is held here too, so the manager can diff a reload
 * against the configuration actually running rather than against the live (REST-mutated) goal.
 * <p>
 * An adapter whose {@code protocol-id} has no registered factory, or whose configuration cannot be instantiated, is
 * represented as an {@link #unknown(ProtocolAdapterHandle, ProtocolAdapterEntity) unknown} managed adapter: it
 * carries the {@code ERROR} handle and the applied entity but no runtime resources, so {@link #close()} is a no-op.
 * <p>
 * Mutated only on the manager's single dispatch thread; it holds no locks.
 */
public final class ProtocolAdapterContainer implements AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterContainer.class);

    private final @NotNull ProtocolAdapterHandle handle;
    private final @Nullable MessageDispatcherHandle dispatcherHandle;
    private final @Nullable AutoCloseable adapterDispatcherHandle;
    private final @Nullable AutoCloseable tickHandle;
    private final @Nullable ProtocolAdapterMetrics metrics;
    private @NotNull ProtocolAdapterEntity appliedEntity;

    /**
     * Create a running managed adapter with its teardown resources.
     *
     * @param handle                  the REST-readable handle.
     * @param dispatcherHandle        the binding of the wrapper mailbox to the dispatcher.
     * @param adapterDispatcherHandle the teardown of the adapter itself: its own {@code close()} (if AutoCloseable)
     *                                and every dispatch binding it opened through the framework dispatcher.
     * @param tickHandle              the periodic wrapper tick schedule.
     * @param metrics                 the per-adapter metrics.
     * @param appliedEntity           the configuration this adapter is running.
     */
    public ProtocolAdapterContainer(
            final @NotNull ProtocolAdapterHandle handle,
            final @NotNull MessageDispatcherHandle dispatcherHandle,
            final @NotNull AutoCloseable adapterDispatcherHandle,
            final @NotNull AutoCloseable tickHandle,
            final @NotNull ProtocolAdapterMetrics metrics,
            final @NotNull ProtocolAdapterEntity appliedEntity) {
        this.handle = handle;
        this.dispatcherHandle = dispatcherHandle;
        this.adapterDispatcherHandle = adapterDispatcherHandle;
        this.tickHandle = tickHandle;
        this.metrics = metrics;
        this.appliedEntity = appliedEntity;
    }

    private ProtocolAdapterContainer(
            final @NotNull ProtocolAdapterHandle handle, final @NotNull ProtocolAdapterEntity entity) {
        this.handle = handle;
        this.dispatcherHandle = null;
        this.adapterDispatcherHandle = null;
        this.tickHandle = null;
        this.metrics = null;
        this.appliedEntity = entity;
    }

    /**
     * Create a managed adapter with no running wrapper — the representation of an unknown adapter type or an
     * un-instantiable configuration.
     *
     * @param handle the {@code ERROR} handle.
     * @param entity the configuration that could not be run.
     * @return the unknown managed adapter; {@link #close()} on it is a no-op.
     */
    public static @NotNull ProtocolAdapterContainer unknown(
            final @NotNull ProtocolAdapterHandle handle, final @NotNull ProtocolAdapterEntity entity) {
        return new ProtocolAdapterContainer(handle, entity);
    }

    /**
     * @return the REST-readable handle.
     */
    public @NotNull ProtocolAdapterHandle handle() {
        return handle;
    }

    /**
     * @return {@code true} when this adapter has a running wrapper (a registered factory built it); {@code false}
     *         for an unknown / un-instantiable adapter.
     */
    public boolean isReal() {
        return dispatcherHandle != null;
    }

    /**
     * @return the configuration this adapter is currently running — the basis for diffing the next reload.
     */
    public @NotNull ProtocolAdapterEntity appliedEntity() {
        return appliedEntity;
    }

    /**
     * Record the configuration now applied to this adapter (after a gentlest in-place transition).
     *
     * @param appliedEntity the configuration now running.
     */
    public void appliedEntity(final @NotNull ProtocolAdapterEntity appliedEntity) {
        this.appliedEntity = appliedEntity;
    }

    /**
     * Release the adapter's runtime resources: stop the periodic tick, detach the wrapper from the dispatcher, and
     * deregister the per-adapter metrics so a recreated adapter with the same id starts clean. Each step is best
     * effort; a failure in one never prevents the others. A no-op for an unknown adapter.
     */
    @Override
    public void close() {
        closeQuietly(tickHandle, "tick schedule");
        closeQuietly(dispatcherHandle, "dispatcher binding");
        closeQuietly(adapterDispatcherHandle, "adapter dispatcher bindings");
        closeQuietly(metrics, "metrics");
    }

    private void closeQuietly(final @Nullable AutoCloseable closeable, final @NotNull String what) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (final Exception exception) {
            log.warn("Failed to close the {} of adapter '{}'", what, handle.adapterId(), exception);
        }
    }
}
