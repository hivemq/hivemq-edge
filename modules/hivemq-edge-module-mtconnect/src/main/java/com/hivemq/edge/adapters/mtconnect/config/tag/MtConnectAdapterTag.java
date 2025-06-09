/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.mtconnect.config.tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class MtConnectAdapterTag implements Tag {
    @JsonProperty(value = "name", required = true)
    @ModuleConfigField(title = "name",
                       description = "name of the tag to be used in mappings",
                       format = ModuleConfigField.FieldType.MQTT_TAG,
                       required = true)
    private final @NotNull String name;

    @JsonProperty(value = "description")
    @ModuleConfigField(title = "description", description = "A human readable description of the tag")
    private final @NotNull String description;

    @JsonProperty(value = "definition", required = true)
    @ModuleConfigField(title = "definition", description = "The actual definition of the tag on the device")
    private final @NotNull MtConnectAdapterTagDefinition definition;

    public MtConnectAdapterTag(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "description") final @Nullable String description,
            @JsonProperty(value = "definition",
                          required = true) final @NotNull MtConnectAdapterTagDefinition definition) {
        this.name = name;
        this.description = Objects.requireNonNullElse(description, "no description present.");
        this.definition = definition;
    }

    @Override
    public @NotNull MtConnectAdapterTagDefinition definition() {
        return definition;
    }

    @Override
    public @NotNull String name() {
        return name;
    }

    @Override
    public @NotNull String description() {
        return description;
    }

    @Override
    public @NotNull String toString() {
        return "MtConnectAdapterTag {" +
                "name='" +
                name +
                '\'' +
                ", description='" +
                description +
                '\'' +
                ", definition=" +
                definition +
                '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MtConnectAdapterTag tag = (MtConnectAdapterTag) o;
        return Objects.equals(name, tag.name) &&
                Objects.equals(description, tag.description) &&
                Objects.equals(definition, tag.definition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, definition);
    }
}


