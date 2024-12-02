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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.atomic.AtomicReference;

public class ProtocolAdapterStateImpl implements ProtocolAdapterState {
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull String protocolId;
    protected @NotNull AtomicReference<RuntimeStatus> runtimeStatus = new AtomicReference<>(RuntimeStatus.STOPPED);
    protected @NotNull AtomicReference<ConnectionStatus> connectionStatus =
            new AtomicReference<>(ConnectionStatus.DISCONNECTED);
    protected @Nullable String lastErrorMessage;

    public ProtocolAdapterStateImpl(final @NotNull EventService eventService,
                                    final @NotNull String adapterId,
                                    final @NotNull String protocolId) {
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.protocolId = protocolId;
    }

    @Override
    public boolean setConnectionStatus(final @NotNull ConnectionStatus connectionStatus) {
        Preconditions.checkNotNull(connectionStatus);
        return this.connectionStatus.getAndSet(connectionStatus) != connectionStatus;
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
    public void setErrorConnectionStatus(
            @Nullable final Throwable t,
            @Nullable final String errorMessage) {
        boolean changed = setConnectionStatus(ConnectionStatus.ERROR);
        reportErrorMessage( t, errorMessage, changed);
    }

    /**
     * Sets the last error message associated with the adapter runtime. This is can be sent through the API to
     * give an indication of the status of an adapter runtime.
     *
     * @param errorMessage
     */
    @Override
    public void reportErrorMessage(
            @Nullable final Throwable throwable,
            @Nullable final String errorMessage,
            final boolean sendEvent) {
        this.lastErrorMessage = errorMessage == null ? throwable == null ? null : throwable.getMessage() : errorMessage;
        if (sendEvent) {
            eventService.createAdapterEvent(adapterId, protocolId)
                    .withSeverity(EventImpl.SEVERITY.ERROR)
                    .withMessage(String.format("Adapter '%s' encountered an error.", adapterId))
                    .withPayload(Payload.ContentType.PLAIN_TEXT, ExceptionUtils.getStackTrace(throwable))
                    .fire();
        }
    }

    @Override
    public void setRuntimeStatus(final @NotNull RuntimeStatus runtimeStatus) {
        this.runtimeStatus.set(runtimeStatus);
    }

    @Override
    public @NotNull RuntimeStatus getRuntimeStatus() {
        return this.runtimeStatus.get();
    }

    @Override
    public @Nullable String getLastErrorMessage() {
        return lastErrorMessage;
    }

}
