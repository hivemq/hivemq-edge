package com.hivemq.edge.modules.api.events.model;

import com.google.common.base.Preconditions;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.events.model.Event;
import com.hivemq.edge.modules.events.model.EventBuilder;
import com.hivemq.edge.modules.events.model.Payload;
import com.hivemq.edge.modules.events.model.TypeIdentifier;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class EventBuilderImpl implements EventBuilder {
    private @Nullable EventImpl.SEVERITY severity;
    private @Nullable String message;
    private @Nullable Payload payload;
    private @Nullable Long timestamp;
    private @Nullable TypeIdentifier associatedObject;
    private @Nullable TypeIdentifier source;

    @Override
    public @NotNull EventBuilder withSeverity(final EventImpl.@NotNull SEVERITY severity) {
        this.severity = severity;
        return this;
    }

    @Override
    public @NotNull EventBuilder withMessage(final @NotNull String message) {
        this.message = message;
        return this;
    }

    @Override
    public @NotNull EventBuilder withPayload(final @NotNull Payload payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public @NotNull EventBuilder withTimestamp(final @NotNull Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public @NotNull EventBuilder withAssociatedObject(final @NotNull TypeIdentifier associatedObject) {
        this.associatedObject = associatedObject;
        return this;
    }

    @Override
    public @NotNull EventBuilder withSource(final @NotNull TypeIdentifier source) {
        this.source = source;
        return this;
    }

    @Override
    public @NotNull Event build() {
        Preconditions.checkNotNull(severity);
        Preconditions.checkNotNull(message);

        return new EventImpl(TypeIdentifierImpl.generate(TypeIdentifier.Type.EVENT),
                severity,
                message,
                payload,
                timestamp,
                associatedObject,
                source);
    }
}
