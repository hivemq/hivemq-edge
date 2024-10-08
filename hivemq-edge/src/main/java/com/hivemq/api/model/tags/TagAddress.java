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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;


@Schema(name = "TagAddress")
public class TagAddress {

    @JsonProperty("address")
    @Schema(description = "The address for the data point on the device. The concrete format depends on the addressing scheme of the concrete protocol.")
    private final @NotNull JsonNode address;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TagAddress(@JsonProperty("address") final @NotNull JsonNode address) {
        this.address = address;
    }

    public static @NotNull TagAddress from(final @NotNull String address) {
        final ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.set("address", new TextNode(address));
        return new TagAddress(objectNode);
    }

    public @NotNull JsonNode getAddress() {
        return address;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final TagAddress that = (TagAddress) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return address.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return "TagAddress{" + "address='" + address + '\'' + '}';
    }
}
