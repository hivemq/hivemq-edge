package com.hivemq.edge.modules.adapters.impl;

import com.google.common.base.Preconditions;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterState;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.api.events.EventUtils;
import com.hivemq.edge.modules.api.events.model.EventBuilder;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;

public class ProtocolAdapterStateImpl implements ProtocolAdapterState {
    private final @NotNull EventService eventService;
    protected @NotNull AtomicReference<RuntimeStatus> runtimeStatus =
            new AtomicReference<>(RuntimeStatus.STOPPED);
    protected @NotNull AtomicReference<ConnectionStatus> connectionStatus =
            new AtomicReference<>(ConnectionStatus.DISCONNECTED);
    protected @Nullable String errorMessage;

    public ProtocolAdapterStateImpl(final @NotNull EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    public boolean setConnectionStatus(@NotNull final ConnectionStatus connectionStatus) {
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
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            @Nullable final Throwable t,
            @Nullable final String errorMessage) {
        boolean changed = setConnectionStatus(ConnectionStatus.ERROR);
        reportErrorMessage(adapterId, protocolId, t, errorMessage, changed);
    }

    /**
     * Sets the last error message associated with the adapter runtime. This is can be sent through the API to
     * give an indication of the status of an adapter runtime.
     *
     * @param errorMessage
     */
    @Override
    public void reportErrorMessage(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            @Nullable final Throwable throwable,
            @Nullable final String errorMessage,
            final boolean sendEvent) {
        this.errorMessage = errorMessage == null ? throwable == null ? null : throwable.getMessage() : errorMessage;
        if (sendEvent) {
            eventService.fireEvent(eventBuilder(adapterId,
                    protocolId,
                    EventImpl.SEVERITY.ERROR).withMessage(String.format("Adapter '%s' encountered an error.",
                    adapterId)).withPayload(EventUtils.generateErrorPayload(throwable)).build());
        }
    }

    @Override
    public void setRuntimeStatus(@NotNull final RuntimeStatus runtimeStatus) {
        this.runtimeStatus.set(runtimeStatus);
    }

    @Override
    public @NotNull RuntimeStatus getRuntimeStatus() {
        return this.runtimeStatus.get();
    }


    protected @NotNull EventBuilder eventBuilder(
            final @NotNull String adapterId,
            final @NotNull String protocolId,
            final @NotNull EventImpl.SEVERITY severity) {
        EventBuilder builder = new EventBuilderImpl();
        builder.withTimestamp(System.currentTimeMillis());
        builder.withSource(TypeIdentifierImpl.create(TypeIdentifierImpl.TYPE.ADAPTER, adapterId));
        builder.withAssociatedObject(TypeIdentifierImpl.create(TypeIdentifierImpl.TYPE.ADAPTER_TYPE, protocolId));
        builder.withSeverity(severity);
        return builder;
    }


}
