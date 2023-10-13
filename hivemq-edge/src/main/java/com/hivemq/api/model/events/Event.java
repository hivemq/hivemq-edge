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
package com.hivemq.api.model.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.api.json.TimestampToDateConverter;
import com.hivemq.edge.model.Identifiable;
import com.hivemq.edge.model.TypeIdentifier;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to transport events across the API
 * @author Simon L Johnson
 */
public class Event implements Identifiable {

    public enum SEVERITY {
        INFO, WARN, ERROR, CRITICAL
    }

    @JsonProperty("identifier")
    @Schema(description = "The unique id of the event object",
            required = true)
    private final @NotNull TypeIdentifier identifier;

    @JsonProperty("severity")
    @Schema(description = "The severity that this log is considered to be",
            required = true)
    private final @NotNull SEVERITY severity;

    @JsonProperty("message")
    @Schema(description = "The message associated with the event. A message will be no more than 1024 characters in length",
            type = "string",
            required = true)
    private final @NotNull String message;

    @JsonProperty("contentType")
    @Schema(description = "The content type of the payload that the event contains")
    private final @NotNull String contentType;

    @JsonProperty("payload")
    @Schema(description = "Extended details of the event, this could contain a longer textual description, error trace or null.")
    private final @NotNull Object payload;

    @JsonProperty("created")
    @JsonSerialize(using = TimestampToDateConverter.Serializer.class)
    @JsonDeserialize(using = TimestampToDateConverter.Deserializer.class)
    @Schema(type = "string",
            format = "date-time",
            description = "Time the event was in date format",
            required = true)
    private final @NotNull Long created;

    @JsonProperty("timestamp")
    @Schema(description = "Time the event was generated in epoch format",
            required = true)
    private final @NotNull Long timestamp;

    @JsonProperty("associatedObject")
    @Schema(description = "The type-identifier associated with the event")
    private final @NotNull TypeIdentifier associatedObject;

    @JsonProperty("source")
    @Schema(description = "The type-identifier of the object who caused the event to be generated")
    private final @NotNull TypeIdentifier source;


    public Event(
            @JsonProperty("identifier") final TypeIdentifier identifier,
            @JsonProperty("severity") final SEVERITY severity,
            @JsonProperty("message") final String message,
            @JsonProperty("contentType") final String contentType,
            @JsonProperty("payload") final Object payload,
            @JsonProperty("timestamp") final Long timestamp,
            @JsonProperty("associatedObject") final TypeIdentifier associatedObject,
            @JsonProperty("source") final TypeIdentifier source) {
        this.identifier = identifier;
        this.severity = severity;
        this.message = message;
        this.contentType = contentType;
        this.payload = payload;
        this.created = timestamp;
        this.timestamp = timestamp;
        this.associatedObject = associatedObject;
        this.source = source;
    }

    public @NotNull SEVERITY getSeverity() {
        return severity;
    }

    public @NotNull String getMessage() {
        return message;
    }

    public @Nullable String getContentType() {
        return contentType;
    }

    public @Nullable Object getPayload() {
        return payload;
    }

    public @NotNull Long getCreated() {
        return created;
    }

    public @NotNull Long getTimestamp() {
        return timestamp;
    }

    public @Nullable TypeIdentifier getAssociatedObject() {
        return associatedObject;
    }

    public @Nullable TypeIdentifier getSource() {
        return source;
    }

    @Override
    public TypeIdentifier getIdentifier() {
        return identifier;
    }
}
