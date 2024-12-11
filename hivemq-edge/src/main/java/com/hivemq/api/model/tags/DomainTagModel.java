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
package com.hivemq.api.model.tags;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hivemq.edge.HiveMQEdgeConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.persistence.domain.DomainTag;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(name = "DomainTag")
public class DomainTagModel {

    @JsonProperty(value = "name", required = true)
    @Schema(description = "The name of the tag that identifies it within this edge instance.",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 65_535,
            format = "mqtt-tag")
    private final @NotNull String name;

    @JsonProperty(value = "description", required = true)
    @Schema(description = "A user created description for this tag.",
            maxLength = 10_000,
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull String description;

    @JsonProperty(value = "definition", required = true)
    @Schema(description = "A user created description for this tag.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private final @NotNull JsonNode definition;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DomainTagModel(
            @JsonProperty(value = "name", required = true) final @NotNull String name,
            @JsonProperty(value = "description", required = true) final @Nullable String description,
            @JsonProperty(value = "definition", required = true) final @Nullable JsonNode definition) {
        this.name = name;
        this.description = Objects.requireNonNullElse(description, "");
        this.definition = Objects.requireNonNullElse(definition, JsonNodeFactory.instance.objectNode());
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull JsonNode getDefinition() {
        return definition;
    }

    public static @NotNull DomainTagModel fromDomainTag(final @NotNull DomainTag domainTag) {
        return new DomainTagModel(domainTag.getTagName(),
                domainTag.getDescription(),
                domainTag.getDefinition());
    }

    @Override
    public @NotNull String toString() {
        return "DomainTagModel{" +
                "tag='" + name +
                '\'' +
                ", description='" +
                description +
                '\'' +
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
        final DomainTagModel that = (DomainTagModel) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
