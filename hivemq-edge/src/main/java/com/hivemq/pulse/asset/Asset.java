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


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an asset with an id, topic, name, and JSON schema.
 */
public record Asset(@NotNull String id, @NotNull String topic, @NotNull String name, @NotNull String jsonSchema) {

    /**
     * Returns a new builder for Asset.
     */
    public static Builder builder() {
        return new Builder();
    }

    public @NotNull Asset withId(final @NotNull String id) {
        if (Objects.equals(this.id, id)) {
            return this;
        }
        return new Asset(id, topic, name, jsonSchema);
    }

    public @NotNull Asset withName(final @NotNull String name) {
        if (Objects.equals(this.name, name)) {
            return this;
        }
        return new Asset(id, topic, name, jsonSchema);
    }

    public @NotNull Asset withTopic(final @NotNull String topic) {
        if (Objects.equals(this.topic, topic)) {
            return this;
        }
        return new Asset(id, topic, name, jsonSchema);
    }

    public @NotNull Asset withJsonSchema(final @NotNull String jsonSchema) {
        if (Objects.equals(this.jsonSchema, jsonSchema)) {
            return this;
        }
        return new Asset(id, topic, name, jsonSchema);
    }

    /**
     * Builder for {@link Asset}.
     */
    public static class Builder {
        private @Nullable String id;
        private @Nullable String topic;
        private @Nullable String name;
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

        public @NotNull Builder jsonSchema(final @NotNull String jsonSchema) {
            this.jsonSchema = jsonSchema;
            return this;
        }

        public @NotNull Asset build() {
            return new Asset(Objects.requireNonNull(id),
                    Objects.requireNonNull(topic),
                    Objects.requireNonNull(name),
                    Objects.requireNonNull(jsonSchema));
        }
    }
}
