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
package com.hivemq.edge.modules.api.events.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.events.model.TypeIdentifier;
import com.hivemq.api.model.core.PayloadImpl;
import com.hivemq.edge.model.TypeIdentifierImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public class EventBuilderImpl implements EventBuilder {

    private static final @NotNull ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private @Nullable EventImpl.SEVERITY severity;
    private @Nullable String message;
    private @Nullable Payload payload;
    private @Nullable Long timestamp;
    private @Nullable TypeIdentifier associatedObject;
    private @Nullable TypeIdentifier source;
    private final @NotNull Consumer<Event> fireConsumer;

    public EventBuilderImpl(final @NotNull Consumer<Event> fireConsumer) {
        this.fireConsumer = fireConsumer;
    }


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
    public @NotNull EventBuilder withPayload(
            final Payload.@NotNull ContentType contentType,
            final @NotNull String content) {
        this.payload = PayloadImpl.from(contentType, content);
        return this;
    }

    @Override
    public @NotNull EventBuilder withPayload( final @NotNull Object data) {
        this.payload = PayloadImpl.fromObject(OBJECT_MAPPER, data);
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

    @Override
    public void fire() {
        fireConsumer.accept(build());
    }
}
