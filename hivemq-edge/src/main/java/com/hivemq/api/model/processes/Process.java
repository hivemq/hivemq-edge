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
package com.hivemq.api.model.processes;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.api.json.TimestampToDateConverter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to transport process details across the API
 * @author Simon L Johnson
 */
public class Process {

    @JsonProperty("processId")
    @Schema(description = "The id of the process")
    private final @NotNull String processId;

    @JsonProperty("created")
    @JsonSerialize(using = TimestampToDateConverter.Serializer.class)
    @JsonDeserialize(using = TimestampToDateConverter.Deserializer.class)
    @Schema(type = "string",
            format = "date-time",
            description = "Time the event was generated",
            nullable = true)
    private final @NotNull Long created;

    @JsonProperty("sourceId")
    @Schema(description = "The source of the process")
    private final @NotNull String sourceId;

    @JsonProperty("isInError")
    @Schema(description = "Whether the process is considered in error state")
    private final @NotNull Boolean isInError;

    public Process(@JsonProperty("processId") final @NotNull String processId,
                   @JsonProperty("created") final @NotNull Long created,
                   @JsonProperty("sourceId") final @NotNull String sourceId,
                   @JsonProperty("isInError") final @NotNull Boolean isInError) {
        this.processId = processId;
        this.created = created;
        this.sourceId = sourceId;
        this.isInError = isInError;
    }

    public @NotNull String getProcessId() {
        return processId;
    }

    public @NotNull Long getCreated() {
        return created;
    }

    public @NotNull String getSourceId() {
        return sourceId;
    }

    public @NotNull Boolean getInError() {
        return isInError;
    }
}
