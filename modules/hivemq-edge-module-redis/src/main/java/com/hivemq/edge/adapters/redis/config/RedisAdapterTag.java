/*
 * Copyright 2024-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.redis.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RedisAdapterTag implements Tag {

    @JsonProperty(value = "name", required = true)
    @ModuleConfigField(title = "Name",
            description = "name of the tag to be used in mappings",
            format = ModuleConfigField.FieldType.MQTT_TAG,
            required = true)
    private final @NotNull String name;

    @JsonProperty(value = "description")
    @ModuleConfigField(title = "Description",
            description = "A human readable description of the tag")
    private final @NotNull String description;

    @JsonProperty(value = "definition", required = true)
    @ModuleConfigField(title = "Definition",
            description = "The actual definition of the tag on the device")
    private final @NotNull RedisAdapterTagDefinition definition;

    public RedisAdapterTag(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "description") final @Nullable String description,
            @JsonProperty(value = "definition", required = true) final @NotNull RedisAdapterTagDefinition definition) {
        this.name = name;
        this.description = Objects.requireNonNullElse(description, "no description present.");
        this.definition = definition;
    }

    @Override
    public @NotNull RedisAdapterTagDefinition getDefinition() {
        return definition;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @NotNull String getDescription() {
        return description;
    }

    @Override
    public @NotNull String toString() {
        return "RedisTag{" +
                "Name='" +
                name +
                '\'' +
                ", Description='" +
                description +
                '\'' +
                ", Definition=" +
                definition +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RedisAdapterTag redisAdapterTag = (RedisAdapterTag) o;
        return Objects.equals(name, redisAdapterTag.name) &&
                Objects.equals(description, redisAdapterTag.description) &&
                Objects.equals(definition, redisAdapterTag.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, definition);
    }
}


