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

import java.util.Objects;

public class OpcuaTagDefinition implements TagDefinition {

    @JsonProperty(value = "node", required = true)
    @ModuleConfigField(title = "Destination Node ID",
                       description = "identifier of the node on the OPC UA server. Example: \"ns=3;s=85/0:Temperature\"",
                       required = true)
    private final @NotNull String node;

    @JsonProperty(value = "collectAllProperties")
    @ModuleConfigField(title = "Collect all properties of the node",
                       description = "OPC UA defines a set of properties for each node. If this is enabled, all properties will be collected and sent to the MQTT broker.")
    private final @NotNull boolean collectAllProperties;

    @JsonCreator
    public OpcuaTagDefinition(
            @JsonProperty(value = "node", required = true) final @NotNull String node,
            @JsonProperty(value = "collectAllProperties", defaultValue = "false") final @Nullable Boolean collectAllProperties) {
        this.node = node;
        if(collectAllProperties == null) {
            this.collectAllProperties = false;
        } else {
            this.collectAllProperties = collectAllProperties;
        }
    }

    public @NotNull String getNode() {
        return node;
    }

    public boolean isCollectAllProperties() {
        return collectAllProperties;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final OpcuaTagDefinition that = (OpcuaTagDefinition) o;
        return isCollectAllProperties() == that.isCollectAllProperties() && Objects.equals(getNode(), that.getNode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNode(), isCollectAllProperties());
    }

    @Override
    public String toString() {
        return "OpcuaTagDefinition{" + "node='" + node + '\'' + ", collectAllProperties=" + collectAllProperties + '}';
    }
}
