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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.Payload;
import com.hivemq.adapter.sdk.api.events.model.TypeIdentifier;
import com.hivemq.api.json.TimestampToDateConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bean to transport events across the API
 * @author Simon L Johnson
 */
public class EventImpl implements Event {

    public EventImpl(
            final @NotNull TypeIdentifier identifier,
            final @NotNull SEVERITY severity,
            final @NotNull String message,
            final @Nullable Payload payload,
            final @Nullable Long timestamp,
            final @Nullable TypeIdentifier associatedObject,
            final @Nullable TypeIdentifier source) {
        this.identifier = identifier;
        this.severity = severity;
        this.message = message;
        this.payload = payload;
        this.created = timestamp;
        this.timestamp = timestamp;
        this.associatedObject = associatedObject;
        this.source = source;
    }

    @JsonProperty("identifier")
    @Schema(description = "The unique id of the event object", required = true)
    private final @NotNull TypeIdentifier identifier;

    @JsonProperty("severity")
    @Schema(description = "The severity that this log is considered to be", required = true)
    private final @NotNull SEVERITY severity;

    @JsonProperty("message")
    @Schema(
            description =
                    "The message associated with the event. A message will be no more than 1024 characters in length",
            type = "string",
            required = true)
    private final @NotNull String message;

    @JsonProperty("payload")
    @Schema(description = "Object to denote the payload of the event")
    private final @Nullable Payload payload;

    @JsonProperty("created")
    @JsonSerialize(using = TimestampToDateConverter.Serializer.class)
    @JsonDeserialize(using = TimestampToDateConverter.Deserializer.class)
    @Schema(type = "string", format = "date-time", description = "Time the event was in date format", required = true)
    private final @Nullable Long created;

    @JsonProperty("timestamp")
    @Schema(description = "Time the event was generated in epoch format", required = true)
    private final @Nullable Long timestamp;

    @JsonProperty("associatedObject")
    @Schema(description = "The type-identifier associated with the event")
    private final @Nullable TypeIdentifier associatedObject;

    @JsonProperty("source")
    @Schema(description = "The type-identifier of the object who caused the event to be generated")
    private final @Nullable TypeIdentifier source;

    @Override
    public @NotNull SEVERITY getSeverity() {
        return severity;
    }

    @Override
    public @NotNull String getMessage() {
        return message;
    }

    @Override
    public @Nullable Payload getPayload() {
        return payload;
    }

    @Override
    public @NotNull Long getTimestamp() {
        // timestamp is nullable - suppress NullAway as interface requires @NotNull
        @SuppressWarnings("NullAway")
        final Long ts = timestamp;
        return ts == null ? 0L : ts;
    }

    @Override
    public @NotNull Long getCreated() {
        // created is nullable - suppress NullAway as interface requires @NotNull
        @SuppressWarnings("NullAway")
        final Long cr = created;
        return cr == null ? 0L : cr;
    }

    @Override
    public @Nullable TypeIdentifier getAssociatedObject() {
        return associatedObject;
    }

    @Override
    public @Nullable TypeIdentifier getSource() {
        return source;
    }

    @Override
    public @NotNull TypeIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public @NotNull String toString() {
        return message;
    }

    @Override
    public boolean equals(final @NotNull Object o) {
        if (this == o) return true;
        if (!(o instanceof EventImpl event)) return false;
        return Objects.equals(identifier, event.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
