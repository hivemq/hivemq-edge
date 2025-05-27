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
package com.hivemq.persistence.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Immutable
public class DomainTag {

    private final @NotNull String tagName;
    private final @NotNull String adapterId;
    private final @Nullable String description;
    private final @NotNull JsonNode definition;

    public DomainTag(
            final @NotNull String tagName,
            final @NotNull String adapterId,
            final @Nullable String description,
            final @NotNull JsonNode definition) {
        this.tagName = tagName;
        this.adapterId = adapterId;
        this.description = description;
        this.definition = definition;
    }

    public static @NotNull DomainTag fromDomainTagEntity(
            final @NotNull com.hivemq.edge.api.model.DomainTag domainTag,
            final @NotNull String adapterId,
            final @NotNull ObjectMapper objectMapper) {
        return new DomainTag(
                domainTag.getName(),
                adapterId,
                domainTag.getDescription(),
                objectMapper.valueToTree(domainTag.getDefinition()));
    }

    public @NotNull com.hivemq.edge.api.model.DomainTag toModel() {
        return new com.hivemq.edge.api.model.DomainTag().name(this.tagName)
                .description(this.description)
                .definition(this.definition);
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getAdapterId() {
        return adapterId;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull JsonNode getDefinition() {
        return definition;
    }

    public @NotNull Map<String, Object> toTagMap() {
        return Map.of("name", tagName, "description", description != null ? description : "", "definition", definition);
    }

    // only tag is used as duplicates based on this field are not allowed.
    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final DomainTag domainTag = (DomainTag) o;
        return tagName.equals(domainTag.tagName);
    }

    @Override
    public int hashCode() {
        return tagName.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "DomainTag{" +
                "tagName='" +
                tagName +
                '\'' +
                ", adapterId='" +
                adapterId +
                '\'' +
                ", description='" +
                description +
                '\'' +
                ", definition=" +
                definition +
                '}';
    }
}
