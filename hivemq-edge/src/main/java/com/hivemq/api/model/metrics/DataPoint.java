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
package com.hivemq.api.model.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.hivemq.api.json.TimestampToDateConverter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Bean to transport metric DataPoints across the API
 * @author Simon L Johnson
 */
public class DataPoint {

    @JsonProperty("value")
    @Schema(description = "The value of the data point")
    private final @NotNull Long value;

    @JsonProperty("sampleTime")
    @JsonSerialize(using = TimestampToDateConverter.Serializer.class)
    @JsonDeserialize(using = TimestampToDateConverter.Deserializer.class)
    @Schema(type = "string",
            format = "date-time",
            description = "Time the data-point was generated",
            nullable = true)
    private final @NotNull Long sampleTime;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DataPoint(@JsonProperty("sampleTime") final @NotNull Long sampleTime,
                     @JsonProperty("value") final @NotNull Long value) {
        this.sampleTime = sampleTime;
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    public Long getSampleTime() {
        return sampleTime;
    }
}
