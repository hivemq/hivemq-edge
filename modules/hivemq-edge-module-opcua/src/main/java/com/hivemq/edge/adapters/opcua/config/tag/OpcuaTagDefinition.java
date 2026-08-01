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
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpcuaTagDefinition implements TagDefinition {

    @JsonProperty(value = "node", required = true)
    @ModuleConfigField(
            title = "Destination Node ID",
            description = "identifier of the node on the OPC UA server. Example: \"ns=3;s=85/0:Temperature\"",
            required = true)
    private final @NotNull String node;

    @JsonProperty(value = "type")
    @ModuleConfigField(
            title = "Node type",
            description = "what the node is: an ordinary VALUE (default), a CONDITION (a single alarm), "
                    + "or an EVENT_SUBSCRIPTION (a query against a notifier, delivering events from many conditions)",
            defaultValue = "VALUE")
    private final @NotNull OpcuaTagType type;

    @JsonProperty(value = "conditionType")
    @ModuleConfigField(
            title = "Condition type",
            description = "for a CONDITION tag, which OPC UA condition type the node is (e.g. AlarmConditionType, "
                    + "ExclusiveLevelAlarmType). This decides the fields the tag publishes, and is verified "
                    + "against the device when the tag is subscribed. Declaring a supertype is allowed.",
            defaultValue = "AlarmConditionType")
    private final @NotNull OpcuaConditionType conditionType;

    @JsonProperty(value = "notifierNode")
    @ModuleConfigField(
            title = "Notifier node ID",
            description = "for a CONDITION tag, the node to subscribe to for its events. A condition is not "
                    + "itself an event notifier, so events are received from a notifier above it. Leave this "
                    + "empty to have it found by walking the address space from the condition; set it when "
                    + "the server does not publish the references that walk needs.")
    private final @Nullable String notifierNode;

    @JsonCreator
    public OpcuaTagDefinition(
            @JsonProperty(value = "node", required = true) final @NotNull String node,
            @JsonProperty(value = "type") final @Nullable OpcuaTagType type,
            @JsonProperty(value = "conditionType") final @Nullable OpcuaConditionType conditionType,
            @JsonProperty(value = "notifierNode") final @Nullable String notifierNode) {
        this.notifierNode = notifierNode == null || notifierNode.isBlank() ? null : notifierNode;
        this.node = node;
        // Absent in every tag written before the type existed, and the overwhelmingly common case since.
        this.type = type == null ? OpcuaTagType.VALUE : type;
        // The most general type that still carries the acknowledge/confirm machinery, so a tag that does not
        // name a type publishes the standard alarm fields rather than nothing.
        this.conditionType = conditionType == null ? OpcuaConditionType.ALARM_CONDITION : conditionType;
    }

    public OpcuaTagDefinition(
            final @NotNull String node,
            final @Nullable OpcuaTagType type,
            final @Nullable OpcuaConditionType conditionType) {
        this(node, type, conditionType, null);
    }

    public OpcuaTagDefinition(final @NotNull String node, final @Nullable OpcuaTagType type) {
        this(node, type, null, null);
    }

    public OpcuaTagDefinition(final @NotNull String node) {
        this(node, OpcuaTagType.VALUE, null, null);
    }

    public @NotNull String getNode() {
        return node;
    }

    public @NotNull OpcuaTagType getType() {
        return type;
    }

    public @NotNull OpcuaConditionType getConditionType() {
        return conditionType;
    }

    /**
     * The notifier to subscribe to. Null means "find it by walking the address space from the condition".
     */
    public @Nullable String getNotifierNode() {
        return notifierNode;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OpcuaTagDefinition that)) {
            return false;
        }
        return node.equals(that.node)
                && type == that.type
                && conditionType == that.conditionType
                && Objects.equals(notifierNode, that.notifierNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, type, conditionType, notifierNode);
    }

    @Override
    public @NotNull String toString() {
        return "OpcuaTagDefinition{node='" + node + "', type=" + type + ", conditionType=" + conditionType
                + ", notifierNode=" + notifierNode + "}";
    }
}
