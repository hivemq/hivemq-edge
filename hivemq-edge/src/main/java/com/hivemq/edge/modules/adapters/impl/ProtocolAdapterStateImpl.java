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
package com.hivemq.edge.modules.adapters.impl;

import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class ProtocolAdapterStateImpl implements ProtocolAdapterState {
    private final @NotNull AtomicReference<RuntimeStatus> runtimeStatus;
    private final @NotNull AtomicReference<ConnectionStatus> connectionStatus;
    private final @NotNull AtomicReference<@Nullable String> lastErrorMessage;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull String protocolId;
    private final @NotNull AtomicReference<Consumer<ConnectionStatus>> connectionStatusListener;

    public ProtocolAdapterStateImpl(
            final @NotNull EventService eventService,
            final @NotNull String adapterId,
            final @NotNull String protocolId) {
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.protocolId = protocolId;
        this.runtimeStatus = new AtomicReference<>(RuntimeStatus.STOPPED);
        this.connectionStatus = new AtomicReference<>(ConnectionStatus.DISCONNECTED);
        this.lastErrorMessage = new AtomicReference<>(null);
        this.connectionStatusListener = new AtomicReference<>();
    }

    @Override
    public boolean setConnectionStatus(final @NotNull ConnectionStatus connectionStatus) {
        Preconditions.checkNotNull(connectionStatus);
        final var changed = this.connectionStatus.getAndSet(connectionStatus) != connectionStatus;
        if (changed) {
            final var listener = connectionStatusListener.get();
            if (listener != null) {
                listener.accept(connectionStatus);
            }
        }
        return changed;
    }

    @Override
    public @NotNull ConnectionStatus getConnectionStatus() {
        return connectionStatus.get();
    }

    /**
     * A convenience method that sets the ConnectionStatus to Error
     * and the errorMessage to that supplied.
     */
    @Override
    public void setErrorConnectionStatus(final @Nullable Throwable t, final @Nullable String errorMessage) {
        reportErrorMessage(t, errorMessage, setConnectionStatus(ConnectionStatus.ERROR));
    }

    @Override
    public void reportErrorMessage(
            final @Nullable Throwable throwable,
            final @Nullable String errorMessage,
            final boolean sendEvent) {
        // Sets the last error message associated with the adapter runtime.
        // This is can be sent through the API to give an indication of the
        // status of an adapter runtime.
        lastErrorMessage.set(errorMessage == null ? throwable == null ? null : throwable.getMessage() : errorMessage);
        if (sendEvent) {
            final var eventBuilder = eventService.createAdapterEvent(adapterId, protocolId)
                    .withSeverity(EventImpl.SEVERITY.ERROR)
                    .withMessage(String.format("Adapter '%s' encountered an error.", adapterId));
            if (throwable != null) {
                eventBuilder.withPayload(Payload.ContentType.PLAIN_TEXT, ExceptionUtils.getStackTrace(throwable));
            } else if (errorMessage != null) {
                eventBuilder.withPayload(Payload.ContentType.PLAIN_TEXT, errorMessage);
            }
            eventBuilder.fire();
        }
    }

    @Override
    public @NotNull RuntimeStatus getRuntimeStatus() {
        return runtimeStatus.get();
    }

    @Override
    public void setRuntimeStatus(final @NotNull RuntimeStatus status) {
        runtimeStatus.set(status);
    }

    @Override
    public @Nullable String getLastErrorMessage() {
        return lastErrorMessage.get();
    }

    public void setConnectionStatusListener(final @NotNull Consumer<ConnectionStatus> listener) {
        final ConnectionStatus currentStatus = connectionStatus.get();
        connectionStatusListener.set(listener);
        listener.accept(currentStatus);
    }
}
