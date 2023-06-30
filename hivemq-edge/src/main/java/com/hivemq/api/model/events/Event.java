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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to transport log lines across the API
 * @author Simon L Johnson
 */
public class Event {

    @JsonProperty("eventSourceName")
    @Schema(description = "The name of the protocol adapter or bridge that generated to the log")
    private final @NotNull String eventSourceName;

    @JsonProperty("created")
    @JsonSerialize(using = TimestampToDateConverter.Serializer.class)
    @JsonDeserialize(using = TimestampToDateConverter.Deserializer.class)
    @Schema(type = "string",
            format = "date-time",
            description = "Time the event was generated",
            nullable = true)
    private final @NotNull Long created;

    @JsonProperty("logLine")
    @Schema(description = "The details of the log")
    private final @NotNull String logLine;


    public Event(@JsonProperty("eventSourceName") final @NotNull String eventSourceName,
                 @JsonProperty("created") final @NotNull Long created,
                 @JsonProperty("logLine") final @NotNull String logLine) {
        this.eventSourceName = eventSourceName;
        this.created = created;
        this.logLine = logLine;
    }

    public @NotNull String getEventSourceName() {
        return eventSourceName;
    }

    public @NotNull Long getCreated() {
        return created;
    }

    public @NotNull String getLogLine() {
        return logLine;
    }
}
