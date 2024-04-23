package com.hivemq.edge.modules.api.events.model;

import com.hivemq.api.model.core.Payload;
import com.hivemq.edge.model.TypeIdentifier;
import com.hivemq.edge.model.TypeIdentifierImpl;

public class EventBuilderImpl implements EventBuilder {
    private EventImpl.SEVERITY severity;
    private String message;
    private Payload payload;
    private Long timestamp;
    private TypeIdentifier associatedObject;
    private TypeIdentifier source;

    @Override
    public EventBuilder withSeverity(final EventImpl.SEVERITY severity) {
        this.severity = severity;
        return this;
    }

    @Override
    public EventBuilder withMessage(final String message) {
        this.message = message;
        return this;
    }

    @Override
    public EventBuilder withPayload(final Payload payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public EventBuilder withTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    @Override
    public EventBuilder withAssociatedObject(final TypeIdentifier associatedObject) {
        this.associatedObject = associatedObject;
        return this;
    }

    @Override
    public EventBuilder withSource(final TypeIdentifier source) {
        this.source = source;
        return this;
    }

    @Override
    public Event build() {
        return new EventImpl(TypeIdentifierImpl.generate(TypeIdentifier.TYPE.EVENT),
                severity,
                message,
                payload,
                timestamp,
                associatedObject,
                source);
    }
}
