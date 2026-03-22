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
package com.hivemq.edge.modules.adapters.telemetry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class EdgeTelemetryTagDefinition implements TagDefinition {

    @JsonProperty(value = "topicFilter", required = true)
    @ModuleConfigField(
            title = "Topic Filter",
            description = "The MQTT topic filter to count messages on (e.g. sensors/#)",
            required = true)
    private final @NotNull String topicFilter;

    @JsonCreator
    public EdgeTelemetryTagDefinition(
            @JsonProperty(value = "topicFilter", required = true) final @NotNull String topicFilter) {
        this.topicFilter = topicFilter;
    }

    public @NotNull String getTopicFilter() {
        return topicFilter;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof EdgeTelemetryTagDefinition that)) return false;
        return Objects.equals(topicFilter, that.topicFilter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topicFilter);
    }

    @Override
    public String toString() {
        return "EdgeTelemetryTagDefinition{topicFilter='" + topicFilter + "'}";
    }
}
