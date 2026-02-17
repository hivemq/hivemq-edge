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
package com.hivemq.configuration.entity.combining;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DataCombiningDestinationEntity {

    @JsonProperty(value = "assetId")
    @XmlElement(name = "assetId")
    private @Nullable String assetId;

    @JsonProperty(value = "topic", required = true)
    @XmlElement(name = "topic", required = true)
    private @NotNull String topic;

    @JsonProperty(value = "schema", required = true)
    @XmlElement(name = "schema", required = true)
    private @NotNull String schema;

    // no-arg for jaxb
    public DataCombiningDestinationEntity() {}

    public DataCombiningDestinationEntity(
            @Nullable final String assetId, @NotNull final String topic, @NotNull final String schema) {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(schema, "schema must not be null");
        this.assetId = assetId;
        this.schema = schema;
        this.topic = topic;
    }

    public @NotNull String getSchema() {
        return schema;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    @Override
    public @NotNull String toString() {
        return "DataCombiningDestinationEntity{" + "topic='" + topic + '\'' + ", schema='" + schema + '\'' + '}';
    }

    @Override
    public boolean equals(final @NotNull Object o) {
        if (!(o instanceof final DataCombiningDestinationEntity that)) {
            return false;
        }
        return Objects.equals(assetId, that.assetId)
                && Objects.equals(topic, that.topic)
                && Objects.equals(schema, that.schema);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, topic, schema);
    }

    public @Nullable String getAssetId() {
        return assetId;
    }
}
