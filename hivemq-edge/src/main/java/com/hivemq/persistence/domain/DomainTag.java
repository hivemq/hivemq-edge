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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hivemq.api.model.tags.DomainTagModel;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Immutable
public class DomainTag {

    private final @NotNull JsonNode tagAddress;
    private final @NotNull String tag;
    private final @NotNull String protocolId;
    private final @NotNull String description;

    public DomainTag(
            final @NotNull JsonNode tagAddress,
            final @NotNull String tag,
            final @NotNull String protocolId,
            final @NotNull String description) {
        this.tagAddress = tagAddress;
        this.tag = tag;
        this.protocolId = protocolId;
        this.description = description;
    }

    public static @NotNull DomainTag fromDomainTagEntity(final @NotNull DomainTagModel domainTag) {
        return new DomainTag(domainTag.getTagAddress().getAddress(),
                domainTag.getTag(),
                domainTag.getProtocolId(),
                domainTag.getDescription());
    }

    public static @NotNull DomainTag simpleAddress(final @NotNull String domainAddress, final @NotNull String tag) {
        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set("address", new TextNode(domainAddress));
        return new DomainTag(objectNode, tag, "someProtocolId" , "someDescription");
    }

    public @NotNull String getTag() {
        return tag;
    }

    public @NotNull JsonNode getTagAddress() {
        return tagAddress;
    }

    public @NotNull String getDescription() {
        return description;
    }

    public @NotNull String getProtocolId() {
        return protocolId;
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
        return tag.equals(domainTag.tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }
}