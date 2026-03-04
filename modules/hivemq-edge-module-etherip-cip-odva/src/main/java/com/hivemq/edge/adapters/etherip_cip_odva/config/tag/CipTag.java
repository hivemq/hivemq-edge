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
package com.hivemq.edge.adapters.etherip_cip_odva.config.tag;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.etherip_cip_odva.config.CipDataType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CipTag implements Tag, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @JsonProperty(value = "name", required = true)
    @ModuleConfigField(
            title = "name",
            description = "name of the tag to be used in mappings",
            format = ModuleConfigField.FieldType.MQTT_TAG,
            required = true)
    private final @NotNull String name;

    @JsonProperty(value = "description")
    @ModuleConfigField(title = "description", description = "A human readable description of the tag", required = true)
    private final @NotNull String description;

    @JsonProperty(value = "definition", required = true)
    @ModuleConfigField(title = "definition", description = "The actual definition of the tag on the device")
    private final @NotNull CipTagDefinition definition;

    private final String conciseString;

    public CipTag(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "description") final @Nullable String description,
            @JsonProperty(value = "definition", required = true) final @NotNull CipTagDefinition definiton) {
        this.name = name;
        this.description = Objects.requireNonNullElse(description, "no description present.");
        this.definition = definiton;
        this.conciseString = buildConciseStringRepresentation(name, definiton);
    }

    private static String buildConciseStringRepresentation(@NotNull String name, @NotNull CipTagDefinition definiton) {
        return String.format(
                "%s(%s[%d@%d.%d])%s",
                name,
                definiton.getDataType(),
                definiton.getNumberOfElements(),
                definiton.getBatchByteIndex(),
                definiton.getBatchBitIndex(),
                definiton.getAddress());
    }

    @Override
    public @NotNull CipTagDefinition getDefinition() {
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
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CipTag cipTag = (CipTag) o;
        return name.equals(cipTag.name) && definition.equals(cipTag.definition);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + definition.hashCode();
        return result;
    }

    public String toConciseString() {
        return conciseString;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("description", description)
                .append("definition", definition)
                .toString();
    }

    public boolean isComposite() {
        return definition.getDataType() == CipDataType.COMPOSITE;
    }
}
