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

package com.hivemq.pulse.asset;

import com.hivemq.configuration.entity.pulse.PulseAssetEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class PulseAgentAsset {
    private @NotNull UUID id;

    private @NotNull String name;

    private @Nullable String description;

    private @NotNull String topic;

    private @NotNull String schema;

    private @NotNull PulseAgentAssetMapping mapping;

    public PulseAgentAsset() {
        this(UUID.randomUUID(), "", null, "", "{}", new PulseAgentAssetMapping());
    }

    public PulseAgentAsset(
            @NotNull final UUID id,
            @NotNull final String name,
            @Nullable final String description,
            @NotNull final String topic,
            @NotNull final String schema,
            @NotNull final PulseAgentAssetMapping mapping) {
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

    public static @NotNull PulseAgentAsset fromPersistence(final @NotNull PulseAssetEntity entity) {
        return new PulseAgentAsset(entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getTopic(),
                entity.getSchema(),
                PulseAgentAssetMapping.fromPersistence(entity.getMapping()));
    }

    public @NotNull PulseAssetEntity toPersistence() {
        return new PulseAssetEntity(id, name, description, topic, schema, mapping.toPersistence());
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (!(o instanceof final PulseAgentAsset that)) {
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

    public @NotNull PulseAgentAssetMapping getMapping() {
        return mapping;
    }

    public void setMapping(@NotNull final PulseAgentAssetMapping mapping) {
        this.mapping = mapping;
    }

    public @NotNull PulseAgentAsset withId(final @NotNull UUID id) {
        if (Objects.equals(this.id, id)) {
            return this;
        }
        return new PulseAgentAsset(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAgentAsset withName(final @NotNull String name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new PulseAgentAsset(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAgentAsset withDescription(final @Nullable String description) {
        if (Objects.equals(this.description, description)) {
            return this;
        }
        return new PulseAgentAsset(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAgentAsset withTopic(final @NotNull String topic) {
        if (Objects.equals(this.topic, topic)) {
            return this;
        }
        return new PulseAgentAsset(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAgentAsset withSchema(final @NotNull String schema) {
        if (Objects.equals(this.schema, schema)) {
            return this;
        }
        return new PulseAgentAsset(id, name, description, topic, schema, mapping);
    }

    public @NotNull PulseAgentAsset withMapping(final @NotNull PulseAgentAssetMapping mapping) {
        if (Objects.equals(this.mapping, mapping)) {
            return this;
        }
        return new PulseAgentAsset(id, name, description, topic, schema, mapping);
    }

    public static class Builder {
        private @Nullable UUID id;
        private @Nullable String name;
        private @Nullable String description;
        private @Nullable String topic;
        private @Nullable String schema;
        private @Nullable PulseAgentAssetMapping mapping;

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

        public @NotNull Builder mapping(final @NotNull PulseAgentAssetMapping mapping) {
            this.mapping = mapping;
            return this;
        }

        public @NotNull PulseAgentAsset build() {
            return new PulseAgentAsset(Objects.requireNonNull(id),
                    Objects.requireNonNull(name),
                    description,
                    Objects.requireNonNull(topic),
                    Objects.requireNonNull(schema),
                    Objects.requireNonNull(mapping));
        }
    }
}
