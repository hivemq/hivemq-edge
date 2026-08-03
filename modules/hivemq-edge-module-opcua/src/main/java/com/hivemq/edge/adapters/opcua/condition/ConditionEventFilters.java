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

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import java.util.ArrayList;
import java.util.List;
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
import org.eclipse.milo.opcua.stack.core.types.structured.ElementOperand;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.LiteralOperand;
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the {@link EventFilter} attached to an event-mode MonitoredItem.
 * <p>
 * An event filter has two clauses. The <em>SelectClause</em> names the fields each event should carry, by
 * browse path — there is no {@code SELECT *}, so the fields are enumerated ({@link ConditionFields#SELECTED}).
 * The <em>WhereClause</em> decides which events get through at all; it is what confines a condition tag to
 * its own condition.
 * <p>
 * For a condition tag the filter is fixed: select the declared type's fields, and accept only events from
 * that one condition. For an event subscription tag it is configurable — the select clause driven by a
 * projection type, and the where clause by up to three optional predicates.
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

    /**
     * The filter for an event subscription tag: a query against a notifier.
     * <p>
     * Where a condition tag names one condition, this names a notifier and describes which of its traffic to
     * accept. Three predicates, each optional and independently omittable, combined with {@code And}:
     * <ul>
     *   <li>{@code sourceNode} — only events <em>about</em> this process object;</li>
     *   <li>{@code conditionNode} — only events from this one condition;</li>
     *   <li>{@code filterType} — only events of this type or a subtype of it.</li>
     * </ul>
     * Omitting all three is legitimate and means the firehose: everything the notifier carries.
     * <p>
     * The select clause is driven by {@code publishedType}, deliberately independent of the filter. Filtering
     * narrowly while publishing a broader shape is safe — every selected field exists on everything that
     * passed — while filtering broadly and publishing a narrower shape is allowed and yields nulls where an
     * event is not that specific type.
     *
     * @param sourceNode    the source to narrow to, or null for all sources.
     * @param conditionNode the condition to narrow to, or null for all conditions.
     * @param filterType    the event type to narrow to, or null for all event types.
     * @param publishedType the type whose fields each event carries.
     */
    public static @NotNull EventFilter forQuery(
            final @Nullable NodeId sourceNode,
            final @Nullable NodeId conditionNode,
            final @Nullable OpcuaConditionType filterType,
            final @NotNull OpcuaConditionType publishedType) {

        final List<ContentFilterElement> predicates = new ArrayList<>();
        if (sourceNode != null) {
            predicates.add(equals(fieldOperand(NodeIds.BaseEventType, "SourceNode"), literal(sourceNode)));
        }
        if (conditionNode != null) {
            predicates.add(equals(conditionIdOperand(), literal(conditionNode)));
        }
        if (filterType != null) {
            // OfType takes the type itself as its single operand -- it asks whether the event IS of that type
            // or a subtype, rather than comparing a field against a value.
            predicates.add(new ContentFilterElement(
                    FilterOperator.OfType, new ExtensionObject[] {literal(filterType.nodeId())}));
        }
        return new EventFilter(selectClauses(publishedType), combine(predicates));
    }

    /**
     * The filter for a refresh tag: select the base event fields, and admit nothing.
     * <p>
     * The where clause compares two different literals, so no ordinary event can pass it. What arrives
     * anyway is precisely the set the protocol refuses to withhold — {@code RefreshStartEventType},
     * {@code RefreshEndEventType} and {@code RefreshRequiredEventType} (OPC 10000-9 §4.5), and
     * {@code EventQueueOverflowEventType} for this item's own queue (OPC 10000-4 §7.22). Filtering
     * <em>for</em> them is impossible, so the honest construction is to filter everything else out.
     * <p>
     * The select clause is the {@code ConditionType} field set rather than a narrower one: these events
     * carry only {@code BaseEventType}'s fields, and asking for more costs nothing — an absent field comes
     * back null. Using a type in the enum keeps one code path for building select clauses.
     */
    public static @NotNull EventFilter forRefresh() {
        final ExtensionObject alwaysFalseLeft =
                ExtensionObject.encode(DefaultEncodingContext.INSTANCE, new LiteralOperand(new Variant(0)));
        final ExtensionObject alwaysFalseRight =
                ExtensionObject.encode(DefaultEncodingContext.INSTANCE, new LiteralOperand(new Variant(1)));
        final ContentFilter admitsNothing = new ContentFilter(new ContentFilterElement[] {
            new ContentFilterElement(FilterOperator.Equals, new ExtensionObject[] {alwaysFalseLeft, alwaysFalseRight})
        });
        return new EventFilter(selectClauses(OpcuaConditionType.CONDITION), admitsNothing);
    }

    /**
     * Combines the predicates into one {@code ContentFilter}, rooted at element 0.
     * <p>
     * A {@code ContentFilter} is a <em>flat array</em>, not a tree: a boolean operator does not nest its
     * operands, it refers to other elements by their index in the same array. Which element is the root is
     * therefore a matter of position, and the specification fixes it at the front:
     * <blockquote>
     * "The filter is evaluated by evaluating the first entry in the element array [...] If an element cannot
     * be traced back to the starting element it is ignored." — OPC 10000-4 §7.7.1
     * </blockquote>
     * So the {@code And} chain occupies indices {@code 0..n-2} and the leaf predicates follow at
     * {@code n-1..2n-2}. Building it the other way round — leaves first, root appended last — yields an array
     * a server accepts without complaint and then evaluates as its <em>first</em> predicate alone, silently
     * ignoring the rest as unreachable. That failure is invisible: no error, and a tag that over-publishes
     * rather than one that goes quiet.
     */
    private static @NotNull ContentFilter combine(final @NotNull List<ContentFilterElement> predicates) {
        if (predicates.isEmpty()) {
            // No narrowing at all: everything the notifier carries. A null where clause is the spec's way of
            // saying that, and is not the same as an empty element array.
            return new ContentFilter(null);
        }
        if (predicates.size() == 1) {
            // One predicate is already its own root at index 0, so there is nothing to combine.
            return new ContentFilter(predicates.toArray(new ContentFilterElement[0]));
        }
        final int count = predicates.size();
        final int firstLeaf = count - 1;
        final ContentFilterElement[] elements = new ContentFilterElement[2 * count - 1];

        // Right-associated fold: And(k) takes leaf k and whatever remains, so And(0) is the root at index 0
        // and each And points forward at the next. The last one takes the final two leaves directly.
        for (int k = 0; k < count - 1; k++) {
            final int right = (k < count - 2) ? k + 1 : firstLeaf + count - 1;
            elements[k] = new ContentFilterElement(
                    FilterOperator.And, new ExtensionObject[] {elementOperand(firstLeaf + k), elementOperand(right)});
        }
        for (int i = 0; i < count; i++) {
            elements[firstLeaf + i] = predicates.get(i);
        }
        return new ContentFilter(elements);
    }

    private static @NotNull ContentFilterElement equals(
            final @NotNull ExtensionObject left, final @NotNull ExtensionObject right) {
        return new ContentFilterElement(FilterOperator.Equals, new ExtensionObject[] {left, right});
    }

    /** Refers to another element of the same {@code ContentFilter} by its index. */
    private static @NotNull ExtensionObject elementOperand(final int index) {
        return ExtensionObject.encode(DefaultEncodingContext.INSTANCE, new ElementOperand(uint(index)));
    }

    /** Names a field of an event by type and browse path, read as its {@code Value} attribute. */
    private static @NotNull ExtensionObject fieldOperand(
            final @NotNull NodeId typeDefinition, final @NotNull String browseName) {
        return ExtensionObject.encode(
                DefaultEncodingContext.INSTANCE,
                new SimpleAttributeOperand(
                        typeDefinition,
                        new QualifiedName[] {new QualifiedName(0, browseName)},
                        AttributeId.Value.uid(),
                        null));
    }

    /**
     * The {@code ConditionId} operand: the condition node's own NodeId, addressed as the {@code NodeId}
     * attribute of {@code ConditionType} with an empty browse path — the operand refers to the event's
     * condition node itself rather than to a field inside it.
     */
    private static @NotNull ExtensionObject conditionIdOperand() {
        return ExtensionObject.encode(
                DefaultEncodingContext.INSTANCE,
                new SimpleAttributeOperand(
                        NodeIds.ConditionType, new QualifiedName[0], AttributeId.NodeId.uid(), null));
    }

    private static @NotNull ExtensionObject literal(final @NotNull NodeId value) {
        return ExtensionObject.encode(DefaultEncodingContext.INSTANCE, new LiteralOperand(new Variant(value)));
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
