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

@Schema(name = "DomainTag")
public class DomainTagModel {

    @JsonProperty("tagDefinition")
    @Schema(description = "The address for the data point on the device.")
    private final @NotNull JsonNode tagDefinition;

    @JsonProperty("tagName")
    @Schema(description = "The name of the tag that identifies it within this edge instance.")
    private final @NotNull String tag;

    @JsonProperty("protocolId")
    @Schema(description = "The protocol id of the protocol for which this tag was created.")
    private final @NotNull String protocolId;

    @JsonProperty("description")
    @Schema(description = "A user created description for this tag.")
    private final @NotNull String description;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DomainTagModel(
            @JsonProperty("tagDefinition") final @NotNull JsonNode tagDefinition,
            @JsonProperty("tagName") final @NotNull String tag,
            @JsonProperty("protocolId") final @NotNull String protocolId,
            @JsonProperty("description") final @NotNull String description) {
        this.tagDefinition = tagDefinition;
        this.tag = tag;
        this.protocolId = protocolId;
        this.description = description;
    }

    public @NotNull String getTag() {
        return tag;
    }

    public @NotNull JsonNode getTagDefinition() {
        return tagDefinition;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
    }

    public static @NotNull DomainTagModel fromDomainTag(final @NotNull DomainTag domainTag) {
        return new DomainTagModel(domainTag.getTagDefinition(),
                domainTag.getTagName(),
                "someProtocolId",
                "someDescription");
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final DomainTagModel that = (DomainTagModel) o;
        return tagDefinition.equals(that.tagDefinition) &&
                tag.equals(that.tag) &&
                protocolId.equals(that.protocolId) &&
                description.equals(that.description);
    }

    @Override
    public int hashCode() {
        int result = tagDefinition.hashCode();
        result = 31 * result + tag.hashCode();
        result = 31 * result + protocolId.hashCode();
        result = 31 * result + description.hashCode();
        return result;
    }

    @Override
    public @NotNull String toString() {
        return "DomainTagModel{" +
                "description='" +
                description +
                '\'' + ", tagAddress=" + tagDefinition +
                ", tag='" +
                tag +
                '\'' +
                ", protocolId='" +
                protocolId +
                '\'' +
                '}';
    }
}
