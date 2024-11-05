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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.domain.DomainTag;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;
import java.util.Objects;

@Schema(name = "DomainTag")
public class DomainTagModel {

    @JsonProperty("tagName")
    @Schema(description = "The name of the tag that identifies it within this edge instance.")
    private final @NotNull String tag;

    @JsonProperty("protocolId")
    @Schema(description = "The protocol id of the protocol for which this tag was created.")
    private final @NotNull String protocolId;

    @JsonProperty("description")
    @Schema(description = "A user created description for this tag.")
    private final @NotNull String description;

    @JsonProperty("description")
    @Schema(description = "A user created description for this tag.")
    private final @NotNull Map<String, Object> definition;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DomainTagModel(
            @JsonProperty("tagName") final @NotNull String tag,
            @JsonProperty("protocolId") final @NotNull String protocolId,
            @JsonProperty("description") final @NotNull String description,
            @JsonProperty("definition") final @NotNull Map<String, Object> definition) {
        this.tag = tag;
        this.protocolId = protocolId;
        this.description = description;
        this.definition = definition;
    }

    public @NotNull String getTag() {
        return tag;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public @NotNull Map<String, Object> getDefinition() {
        return definition;
    }

    public static @NotNull DomainTagModel fromDomainTag(final @NotNull DomainTag domainTag) {
        return new DomainTagModel(
                domainTag.getTagName(),
                domainTag.getProtocolId(),
                domainTag.getDescription(),
                domainTag.getDefinition());
    }

    @Override
    public String toString() {
        return "DomainTagModel{" +
                "tag='" +
                tag +
                '\'' +
                ", protocolId='" +
                protocolId +
                '\'' +
                ", description='" +
                description +
                '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DomainTagModel that = (DomainTagModel) o;
        return Objects.equals(tag, that.tag) &&
                Objects.equals(protocolId, that.protocolId) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tag, protocolId, description);
    }
}
