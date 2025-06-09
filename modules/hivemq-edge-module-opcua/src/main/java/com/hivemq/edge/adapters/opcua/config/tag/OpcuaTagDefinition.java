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
package com.hivemq.edge.adapters.opcua.config.tag;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record OpcuaTagDefinition(
        @JsonProperty(value = "node", required = true) @ModuleConfigField(title = "Destination Node ID",
                                                                          description = "identifier of the node on the OPC UA server. Example: \"ns=3;s=85/0:Temperature\"",
                                                                          required = true) @NotNull String node)
        implements TagDefinition {

    @JsonCreator
    public OpcuaTagDefinition {
    }

    @Override
    public @NotNull String node() {
        return node;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final OpcuaTagDefinition that = (OpcuaTagDefinition) o;
        return node.equals(that.node);
    }

    @Override
    public @NotNull String toString() {
        return "OpcuaTagDefinition{" + "node='" + node + '\'' + '}';
    }
}
