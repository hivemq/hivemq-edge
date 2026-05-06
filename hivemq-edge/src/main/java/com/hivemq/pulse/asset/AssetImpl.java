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
package com.hivemq.pulse.asset;
import com.hivemq.edge.pulse.integration.api.asset.Asset;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record AssetImpl(
        @NotNull String id,
        @NotNull String topic,
        @NotNull String name,
        @Nullable String description,
        @NotNull String jsonSchema)
        implements Asset {

    public static Builder builder() {
        return new Builder();
    }

    public @NotNull AssetImpl withId(final @NotNull String id) {
        if (Objects.equals(this.id, id)) {
            return this;
        }
        return new AssetImpl(id, topic, name, description, jsonSchema);
    }

    public @NotNull AssetImpl withName(final @NotNull String name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new AssetImpl(id, topic, name, description, jsonSchema);
    }

    public @NotNull AssetImpl withDescription(final @Nullable String description) {
        if (Objects.equals(this.description, description)) {
            return this;
        }
        return new AssetImpl(id, topic, name, description, jsonSchema);
    }

    public @NotNull AssetImpl withTopic(final @NotNull String topic) {
        if (Objects.equals(this.topic, topic)) {
            return this;
        }
        return new AssetImpl(id, topic, name, description, jsonSchema);
    }

    public @NotNull AssetImpl withJsonSchema(final @NotNull String jsonSchema) {
        if (Objects.equals(this.jsonSchema, jsonSchema)) {
            return this;
        }
        return new AssetImpl(id, topic, name, description, jsonSchema);
    }

    public static class Builder {
        private @Nullable String id;
        private @Nullable String topic;
        private @Nullable String name;
        private @Nullable String description;
        private @Nullable String jsonSchema;

        public @NotNull Builder id(final @NotNull String id) {
            this.id = id;
            return this;
        }

        public @NotNull Builder topic(final @NotNull String topic) {
            this.topic = topic;
            return this;
        }

        public @NotNull Builder name(final @NotNull String name) {
            this.name = name;
            return this;
        }

        public @NotNull Builder description(final @NotNull String description) {
            this.description = description;
            return this;
        }

        public @NotNull Builder jsonSchema(final @NotNull String jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        public @NotNull AssetImpl build() {
            return new AssetImpl(
                    Objects.requireNonNull(id),
                    Objects.requireNonNull(topic),
                    Objects.requireNonNull(name),
                    description,
                    Objects.requireNonNull(jsonSchema));
        }
    }
}
