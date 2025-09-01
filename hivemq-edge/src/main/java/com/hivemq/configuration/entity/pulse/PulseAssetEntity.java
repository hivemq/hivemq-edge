/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.configuration.entity.pulse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.UUIDAdapter;
import com.hivemq.pulse.asset.Asset;
import com.hivemq.util.ObjectMapperUtil;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "managed-asset", propOrder = {"id", "name", "description", "topic", "schema", "mapping"})
public class PulseAssetEntity {

    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(UUIDAdapter.class)
    private @NotNull UUID id;

    @XmlAttribute(name = "name", required = true)
    private @NotNull String name;

    @XmlAttribute(name = "description")
    private @Nullable String description;

    @XmlAttribute(name = "topic", required = true)
    private @NotNull String topic;

    @XmlElement(name = "schema", required = true)
    private @NotNull String schema;

    @XmlElement(name = "mapping", required = true)
    private @NotNull PulseAssetMappingEntity mapping;

    public PulseAssetEntity() {
        this(UUID.randomUUID(), "", null, "", "{}", new PulseAssetMappingEntity());
    }

    public PulseAssetEntity(
            @NotNull final UUID id,
            @NotNull final String name,
            @Nullable final String description,
            @NotNull final String topic,
            @NotNull final String schema,
            @NotNull final PulseAssetMappingEntity mapping) {
        this.name = name;
        this.id = id;
        this.description = description;
        this.topic = topic;
        this.schema = schema;
        this.mapping = mapping;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean equals(final @NotNull Asset remoteAsset) {
        if (!Objects.equals(id.toString(), remoteAsset.id())) {
            return false;
        }
        if (!Objects.equals(name, remoteAsset.name())) {
            return false;
        }
        if (!Objects.equals(topic, remoteAsset.topic())) {
            return false;
        }
        if (!Objects.equals(schema, remoteAsset.jsonSchema())) {
            try {
                final ObjectMapper objectMapper = ObjectMapperUtil.NO_PRETTY_PRINT_WITH_JAVA_TIME;
                if (!objectMapper.readTree(schema).equals(objectMapper.readTree(remoteAsset.jsonSchema()))) {
                    return false;
                }
            } catch (final Exception ignore) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final PulseAssetEntity that)) {
            return false;
        }
        return Objects.equals(name, that.name) &&
                Objects.equals(id, that.id) &&
                Objects.equals(description, that.description) &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(schema, that.schema) &&
                Objects.equals(mapping, that.mapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, description, topic, schema, mapping);
    }

    public @NotNull UUID getId() {
        return id;
    }

    public void setId(@NotNull final UUID id) {
        this.id = id;
    }

    public @NotNull String getName() {
        return name;
    }

    public void setName(@NotNull final String name) {
        this.name = name;
    }

    public @Nullable String getDescription() {
        return description;
    }

    public void setDescription(@Nullable final String description) {
        this.description = description;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public void setTopic(@NotNull final String topic) {
        this.topic = topic;
    }

    public @NotNull String getSchema() {
        return schema;
    }

    public void setSchema(@NotNull final String schema) {
        this.schema = schema;
    }

    public @NotNull PulseAssetMappingEntity getMapping() {
        return mapping;
    }

    public void setMapping(@NotNull final PulseAssetMappingEntity mapping) {
        this.mapping = mapping;
    }

    public @NotNull PulseAssetEntity withId(final @NotNull UUID id) {
        if (Objects.equals(this.id, id)) {
            return this;
        }
        return new PulseAssetEntity(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAssetEntity withName(final @NotNull String name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new PulseAssetEntity(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAssetEntity withDescription(final @Nullable String description) {
        if (Objects.equals(this.description, description)) {
            return this;
        }
        return new PulseAssetEntity(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAssetEntity withTopic(final @NotNull String topic) {
        if (Objects.equals(this.topic, topic)) {
            return this;
        }
        return new PulseAssetEntity(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAssetEntity withSchema(final @NotNull String schema) {
        if (Objects.equals(this.schema, schema)) {
            return this;
        }
        return new PulseAssetEntity(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAssetEntity withMapping(final @NotNull PulseAssetMappingEntity mapping) {
        if (Objects.equals(this.mapping, mapping)) {
            return this;
        }
        return new PulseAssetEntity(id, name, description, topic, schema, mapping);
    }

    public static class Builder {
        private @Nullable UUID id;
        private @Nullable String name;
        private @Nullable String description;
        private @Nullable String topic;
        private @Nullable String schema;
        private @Nullable PulseAssetMappingEntity mapping;

        public @NotNull Builder id(final @NotNull UUID id) {
            this.id = id;
            return this;
        }

        public @NotNull Builder name(final @NotNull String name) {
            this.name = name;
            return this;
        }

        public @NotNull Builder description(final @Nullable String description) {
            this.description = description;
            return this;
        }

        public @NotNull Builder topic(final @NotNull String topic) {
            this.topic = topic;
            return this;
        }

        public @NotNull Builder schema(final @NotNull String schema) {
            this.schema = schema;
            return this;
        }

        public @NotNull Builder mapping(final @NotNull PulseAssetMappingEntity mapping) {
            this.mapping = mapping;
            return this;
        }

        public @NotNull PulseAssetEntity build() {
            return new PulseAssetEntity(Objects.requireNonNull(id),
                    Objects.requireNonNull(name),
                    description,
                    Objects.requireNonNull(topic),
                    Objects.requireNonNull(schema),
                    Objects.requireNonNull(mapping));
        }
    }
}
