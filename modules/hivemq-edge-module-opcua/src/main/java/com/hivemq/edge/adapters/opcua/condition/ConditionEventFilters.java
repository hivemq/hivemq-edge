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
package com.hivemq.edge.adapters.opcua.condition;

import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ExtensionObject;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.enumerated.FilterOperator;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.ContentFilterElement;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.LiteralOperand;
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand;
import org.jetbrains.annotations.NotNull;

/**
 * Builds the {@link EventFilter} attached to an event-mode MonitoredItem.
 * <p>
 * An event filter has two clauses. The <em>SelectClause</em> names the fields each event should carry, by
 * browse path — there is no {@code SELECT *}, so the fields are enumerated ({@link ConditionFields#SELECTED}).
 * The <em>WhereClause</em> decides which events get through at all; it is what confines a condition tag to
 * its own condition.
 * <p>
 * For a condition tag the filter is fixed: select the standard condition fields, and accept only events
 * sourced from the condition itself. When the query tag arrives, this is the builder that becomes configurable — the select clause driven
 * by a projection type and the where clause by a source/condition/type filter.
 */
public final class ConditionEventFilters {

    private ConditionEventFilters() {}

    /**
     * The type whose fields are selected. {@code BaseEventType} (ns=0;i=2041) is the root of the event type
     * hierarchy; naming it means the browse paths are resolved against any event type that has them, and
     * fields that a particular event lacks come back null rather than failing the subscription.
     */
    private static final @NotNull NodeId BASE_EVENT_TYPE = NodeId.parse("ns=0;i=2041");

    /**
     * The filter for a condition tag: the declared type's fields, narrowed to one condition.
     * <p>
     * The where clause is what makes the tag mean "this condition". The MonitoredItem is placed on a notifier
     * — a condition is not itself an event notifier — and a notifier carries the traffic of everything
     * beneath it, so without this predicate a condition tag would publish every alarm in its area.
     *
     * @param conditionNodeId the condition the tag names.
     * @param conditionType   the declared type, whose fields are selected.
     */
    public static @NotNull EventFilter forCondition(
            final @NotNull NodeId conditionNodeId, final @NotNull OpcuaConditionType conditionType) {
        return new EventFilter(selectClauses(conditionType), conditionIs(conditionNodeId));
    }

    /**
     * A where clause accepting only events raised by the given condition.
     * <p>
     * The comparison is against {@code ConditionId}, which <em>is</em> the condition node's own NodeId — the
     * same value the tag was configured with, so what an operator named and what the filter matches are one
     * thing. It is addressed as the {@code NodeId} attribute of {@code ConditionType} with an empty browse
     * path: the operand refers to the event's condition node itself rather than to a field inside it.
     * <p>
     * Not {@code SourceNode}. That is the <em>ConditionSource</em> — the sensor or machine the alarm is
     * about, which the specification explicitly allows to be a separate node. Filtering on it works only
     * against servers that happen to set the two equal, and silently drops every event from those that do
     * not.
     */
    private static @NotNull ContentFilter conditionIs(final @NotNull NodeId conditionNodeId) {
        final ExtensionObject conditionId = ExtensionObject.encode(
                DefaultEncodingContext.INSTANCE,
                new SimpleAttributeOperand(
                        NodeIds.ConditionType, new QualifiedName[0], AttributeId.NodeId.uid(), null));
        final ExtensionObject literal = ExtensionObject.encode(
                DefaultEncodingContext.INSTANCE, new LiteralOperand(new Variant(conditionNodeId)));

        return new ContentFilter(new ContentFilterElement[] {
            new ContentFilterElement(FilterOperator.Equals, new ExtensionObject[] {conditionId, literal})
        });
    }

    private static @NotNull SimpleAttributeOperand @NotNull [] selectClauses(
            final @NotNull OpcuaConditionType conditionType) {
        return conditionType.allFields().stream()
                .map(ConditionEventFilters::selectField)
                .toArray(SimpleAttributeOperand[]::new);
    }

    /**
     * Selects one field by browse path, reading its {@code Value} attribute. {@code indexRange} is null: the
     * whole value is wanted, not a slice of an array.
     */
    private static @NotNull SimpleAttributeOperand selectField(final @NotNull String browseName) {
        return new SimpleAttributeOperand(
                BASE_EVENT_TYPE, new QualifiedName[] {new QualifiedName(0, browseName)}, AttributeId.Value.uid(), null);
    }
}
