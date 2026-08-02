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

    @JsonProperty(value = "kind")
    @ModuleConfigField(
            title = "Node kind",
            description = "what the node is: an ordinary VALUE (default), a CONDITION (a single alarm), "
                    + "or an EVENT_SUBSCRIPTION (a query against a notifier, delivering events from many conditions)",
            defaultValue = "VALUE")
    private final @NotNull OpcuaTagKind kind;

    @JsonProperty(value = "type")
    @ModuleConfigField(
            title = "Node type",
            description = "the type whose structure the tag's northbound output has (e.g. AlarmConditionType, "
                    + "ExclusiveLevelAlarmType). This is the input to schema generation. For a CONDITION node "
                    + "it is also verified against the device when the tag is subscribed, and declaring a "
                    + "supertype of what the device offers is allowed. For an EVENT_SUBSCRIPTION node nothing "
                    + "is verified — many conditions of differing types may pass the filter — so a field an "
                    + "event does not carry is published as null.",
            defaultValue = "AlarmConditionType")
    private final @NotNull OpcuaConditionType type;

    @JsonProperty(value = "notifierNode")
    @ModuleConfigField(
            title = "Notifier node ID",
            description = "for a CONDITION tag, the node to subscribe to for its events. A condition is not "
                    + "itself an event notifier, so events are received from a notifier above it. Leave this "
                    + "empty to have it found by walking the address space from the condition; set it when "
                    + "the server does not publish the references that walk needs.")
    private final @Nullable String notifierNode;

    @JsonProperty(value = "sourceNode")
    @ModuleConfigField(
            title = "Source node ID",
            description = "for an EVENT_SUBSCRIPTION tag, deliver only events about this source — the process "
                    + "object a condition watches, such as a sensor. Leave empty for every source the "
                    + "notifier covers.")
    private final @Nullable String sourceNode;

    @JsonProperty(value = "conditionNode")
    @ModuleConfigField(
            title = "Condition node ID",
            description = "for an EVENT_SUBSCRIPTION tag, deliver only events from this one condition. Leave "
                    + "empty for every condition the notifier covers.")
    private final @Nullable String conditionNode;

    @JsonProperty(value = "filterType")
    @ModuleConfigField(
            title = "Filter type",
            description = "for an EVENT_SUBSCRIPTION tag, deliver only events of this type or a subtype of "
                    + "it. Independent of the published shape, which the node's type decides: filtering "
                    + "narrowly while publishing a broader shape is safe, and filtering broadly while "
                    + "publishing a narrower shape is allowed and yields nulls. Leave empty for every event "
                    + "type the notifier carries.")
    private final @Nullable OpcuaConditionType filterType;

    @JsonCreator
    public OpcuaTagDefinition(
            @JsonProperty(value = "node", required = true) final @NotNull String node,
            @JsonProperty(value = "kind") final @Nullable OpcuaTagKind kind,
            @JsonProperty(value = "type") final @Nullable OpcuaConditionType type,
            @JsonProperty(value = "notifierNode") final @Nullable String notifierNode,
            @JsonProperty(value = "sourceNode") final @Nullable String sourceNode,
            @JsonProperty(value = "conditionNode") final @Nullable String conditionNode,
            @JsonProperty(value = "filterType") final @Nullable OpcuaConditionType filterType) {
        this.notifierNode = blankToNull(notifierNode);
        this.sourceNode = blankToNull(sourceNode);
        this.conditionNode = blankToNull(conditionNode);
        this.filterType = filterType;
        this.node = node;
        // Absent in every tag written before the kind existed, and the overwhelmingly common case since.
        this.kind = kind == null ? OpcuaTagKind.VALUE : kind;
        // The most general type that still carries the acknowledge/confirm machinery, so a tag that does not
        // name a type publishes the standard alarm fields rather than nothing.
        this.type = type == null ? OpcuaConditionType.ALARM_CONDITION : type;
    }

    /** An omitted node id and one typed as whitespace mean the same thing: no narrowing on that dimension. */
    private static @Nullable String blankToNull(final @Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public OpcuaTagDefinition(
            final @NotNull String node,
            final @Nullable OpcuaTagKind kind,
            final @Nullable OpcuaConditionType type,
            final @Nullable String notifierNode) {
        this(node, kind, type, notifierNode, null, null, null);
    }

    public OpcuaTagDefinition(
            final @NotNull String node, final @Nullable OpcuaTagKind kind, final @Nullable OpcuaConditionType type) {
        this(node, kind, type, null);
    }

    public OpcuaTagDefinition(final @NotNull String node, final @Nullable OpcuaTagKind kind) {
        this(node, kind, null, null);
    }

    public OpcuaTagDefinition(final @NotNull String node) {
        this(node, OpcuaTagKind.VALUE, null, null);
    }

    public @NotNull String getNode() {
        return node;
    }

    /** What the node is — an ordinary value, a single condition, or a query against a notifier. */
    public @NotNull OpcuaTagKind getKind() {
        return kind;
    }

    /**
     * The type whose structure the northbound output has — the input to schema generation, and the field
     * list the select clause and the event decoder both work from.
     */
    public @NotNull OpcuaConditionType getType() {
        return type;
    }

    /**
     * The notifier to subscribe to. Null means "find it by walking the address space from the condition".
     */
    public @Nullable String getNotifierNode() {
        return notifierNode;
    }

    /** For an EVENT_SUBSCRIPTION tag: narrow to one source, or null for every source. */
    public @Nullable String getSourceNode() {
        return sourceNode;
    }

    /** For an EVENT_SUBSCRIPTION tag: narrow to one condition, or null for every condition. */
    public @Nullable String getConditionNode() {
        return conditionNode;
    }

    /**
     * For an EVENT_SUBSCRIPTION tag: deliver only events of this type or a subtype. Null means no type
     * predicate at all, which is why this is returned exactly as configured rather than defaulted — there is
     * no sensible default to substitute, and inventing one would also change {@code equals} on a write/read
     * cycle, breaking a {@link TagDefinition}'s use as a stable key.
     */
    public @Nullable OpcuaConditionType getFilterType() {
        return filterType;
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
                && kind == that.kind
                && type == that.type
                && Objects.equals(notifierNode, that.notifierNode)
                && Objects.equals(sourceNode, that.sourceNode)
                && Objects.equals(conditionNode, that.conditionNode)
                && filterType == that.filterType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(node, kind, type, notifierNode, sourceNode, conditionNode, filterType);
    }

    @Override
    public @NotNull String toString() {
        return "OpcuaTagDefinition{node='" + node + "', kind=" + kind + ", type=" + type + ", notifierNode="
                + notifierNode + ", sourceNode=" + sourceNode + ", conditionNode=" + conditionNode
                + ", filterType=" + filterType + "}";
    }
}
