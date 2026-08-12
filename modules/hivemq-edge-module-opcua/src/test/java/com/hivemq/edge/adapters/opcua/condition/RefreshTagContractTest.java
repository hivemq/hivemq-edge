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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.edge.adapters.opcua.config.tag.BaseEventFieldSet;
import com.hivemq.edge.adapters.opcua.config.tag.EventFieldSet;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.SimpleAttributeOperand;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * A refresh tag's four field contracts, which must be one.
 * <p>
 * Review finding 11. They had drifted into three different answers: the monitored item's select clause asked
 * the server for {@code ConditionType}'s fields, while the schema, the event decoder and the rejected-field
 * report all used the tag's configured {@code type} — defaulting to {@code AlarmConditionType}.
 * <p>
 * The published shape survived that by luck rather than by design. The field list is built root-down, so the
 * alarm list begins with exactly the condition list and the extra entries simply decoded as null. What did not
 * survive is the contract: the tag advertised some fifty alarm fields it could never carry, and {@code type}
 * looked configurable while changing nothing that reached the server. The next entry inserted anywhere but the
 * end would have turned a cosmetic divergence into misaligned positional decoding.
 * <p>
 * The fix is {@link OpcuaTagDefinition#getPublishedFields()}: one accessor all four read.
 */
class RefreshTagContractTest {

    private static @NotNull OpcuaTagDefinition refreshTag(final @NotNull OpcuaConditionType declaredType) {
        return new OpcuaTagDefinition("ns=0;i=2253", OpcuaTagKind.REFRESH, declaredType);
    }

    @Test
    void theSelectedFieldsTheSchemaAndTheDecoderAgree() {
        // The three lists compared directly, because the decoder matches values to fields *positionally*
        // against the select clause -- an entry in one and not the other silently shifts every field after
        // it, and no test that checks each list on its own can see that.
        final OpcuaTagDefinition tag = refreshTag(OpcuaConditionType.ALARM_CONDITION);
        final EventFieldSet published = tag.getPublishedFields();

        final List<String> selectedByTheFilter = selectClauseFieldNames();
        final List<String> readByTheDecoder = published.selectedFields().stream()
                .filter(field -> !field.isStateId())
                .map(OpcuaConditionType.SelectedField::publishedAs)
                .toList();
        final List<String> promisedByTheSchema = schemaPropertyNames(published);

        assertThat(readByTheDecoder)
                .as("the decoder must read exactly what the filter asked for")
                .isEqualTo(selectedByTheFilter.stream().distinct().toList());
        assertThat(promisedByTheSchema)
                .as("and the schema must promise exactly that, no more")
                .containsExactlyInAnyOrderElementsOf(readByTheDecoder);
    }

    @Test
    void aNonDefaultTypeOnARefreshTagChangesNothing() {
        // `type` is accepted and ignored on a REFRESH tag. Control events are BaseEventType events -- they
        // have no alarm state -- so no declared type can describe them, and the filter does not ask the
        // server for those fields either. Silently honouring the field would advertise a shape the tag
        // cannot produce.
        for (final OpcuaConditionType declared : OpcuaConditionType.values()) {
            assertThat(refreshTag(declared).getPublishedFields())
                    .as("a refresh tag declaring '%s' still publishes the base event shape", declared.browseName())
                    .isEqualTo(BaseEventFieldSet.INSTANCE);
        }
    }

    @Test
    void aRefreshTagPublishesTheBaseEventFieldsAndNothingElse() {
        // Review-02 finding 10. The v01 pass settled a three-way drift between the select clause, the schema
        // and the decoder by pointing all three at ConditionType -- which settled it on a shape the events
        // cannot fill. RefreshStart, RefreshEnd, RefreshRequired and EventQueueOverflow all derive from
        // BaseEventType directly, so ConditionType's thirteen members were requested from the server,
        // decoded as null, and advertised in the schema. The comments beside the code said "select the base
        // event fields" throughout; this is that, stated as an assertion.
        assertThat(refreshTag(OpcuaConditionType.ALARM_CONDITION)
                        .getPublishedFields()
                        .allFields())
                .as("exactly BaseEventType's fields, in order")
                .isEqualTo(OpcuaConditionType.BASE_EVENT_FIELDS);
    }

    @Test
    void andNoneOfConditionTypesOwnMembersSurvive() {
        // Named individually because the review names them: these are the fields a consumer was being told
        // to expect from a control-event stream.
        final List<String> published = refreshTag(OpcuaConditionType.ALARM_CONDITION)
                .getPublishedFields()
                .allFields();

        assertThat(published)
                .doesNotContain(
                        "BranchId",
                        "Comment",
                        "ConditionClassId",
                        "ConditionName",
                        "EnabledState",
                        "Quality",
                        "Retain");
    }

    @Test
    void butTheEventIdAndEventTypeStayWhereTheHandlerReadsThem() {
        // The constraint that makes the narrowing safe. The handler reads EventId and EventType out of a
        // notification by cached index, and OpcUaSubscriptionEventFieldPositionsTest requires
        // BASE_EVENT_FIELDS to be the prefix of every select clause. A refresh tag now selects exactly that
        // list, so it is the one tag kind where prefix and whole are the same thing.
        final List<String> selected =
                refreshTag(OpcuaConditionType.CONDITION).getPublishedFields().selectedFields().stream()
                        .map(OpcuaConditionType.SelectedField::publishedAs)
                        .toList();

        assertThat(selected.subList(0, OpcuaConditionType.BASE_EVENT_FIELDS.size()))
                .isEqualTo(OpcuaConditionType.BASE_EVENT_FIELDS);
    }

    // ── review-03 finding 4: ConditionId is not a BaseEvent field ───────────────────────────────────

    @Test
    void aControlEventIsNotPromisedAConditionIdentity() {
        // The v02 pass kept ConditionId here on the grounds that it is one of BASE_EVENT_FIELDS -- which was
        // circular, because the same pass had put it in that list. BaseEventType defines no ConditionId. It
        // is the virtual operand OPC 10000-9 defines on ConditionType, and none of RefreshStart, RefreshEnd,
        // RefreshRequired or EventQueueOverflow is a condition.
        assertThat(refreshTag(OpcuaConditionType.ALARM_CONDITION)
                        .getPublishedFields()
                        .allFields())
                .as("a control event has no condition to identify")
                .doesNotContain(OpcuaConditionType.CONDITION_ID);

        assertThat(selectClauseFieldNames())
                .as("and it must not be asked of the server either")
                .doesNotContain(OpcuaConditionType.CONDITION_ID);
    }

    @Test
    void andTheRefreshSchemaDoesNotAdvertiseOne() {
        // The user-visible half: a schema-aware consumer was told to expect a condition identity on a stream
        // that cannot carry one.
        assertThat(schemaPropertyNames(
                        refreshTag(OpcuaConditionType.ALARM_CONDITION).getPublishedFields()))
                .doesNotContain(OpcuaConditionType.CONDITION_ID);
    }

    @Test
    void butEveryConditionStillCarriesItInTheSamePlace() {
        // Moved, not dropped. It is ConditionType's first own member, so it lands immediately after the base
        // event fields in every condition's shape -- the position it held while it was wrongly one of them,
        // which keeps the published key order of a condition event unchanged.
        for (final OpcuaConditionType type : List.of(
                OpcuaConditionType.CONDITION,
                OpcuaConditionType.ALARM_CONDITION,
                OpcuaConditionType.DIALOG_CONDITION)) {
            assertThat(type.allFields())
                    .as("%s publishes which condition the event is about", type.browseName())
                    .containsSubsequence(
                            OpcuaConditionType.BASE_EVENT_FIELDS.get(OpcuaConditionType.BASE_EVENT_FIELDS.size() - 1),
                            OpcuaConditionType.CONDITION_ID);
            assertThat(type.allFields().indexOf(OpcuaConditionType.CONDITION_ID))
                    .as("directly after the base fields, as before")
                    .isEqualTo(OpcuaConditionType.BASE_EVENT_FIELDS.size());
        }
    }

    @Test
    void andItIsSelectedAgainstTheTypeThatDefinesIt() {
        // The second half of the finding, and an inconsistency inside one file: the where clause built this
        // operand by hand with ConditionType, while the select clause hardcoded BaseEventType for every
        // field. OPC 10000-9 defines the operand as TypeDefinitionId = ConditionType, an empty browse path
        // and the NodeId attribute -- a strict server may refuse it written any other way.
        final SimpleAttributeOperand[] clauses = ConditionEventFilters.forCondition(
                        NodeId.parse("ns=2;s=Boiler1.HighTemp"), OpcuaConditionType.ALARM_CONDITION)
                .getSelectClauses();
        assertThat(clauses).isNotNull();

        final SimpleAttributeOperand conditionId = java.util.Arrays.stream(clauses)
                .filter(clause -> clause.getBrowsePath() == null || clause.getBrowsePath().length == 0)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no ConditionId operand in a condition's select clause"));

        assertThat(conditionId.getTypeDefinitionId()).isEqualTo(NodeIds.ConditionType);
        assertThat(conditionId.getAttributeId()).isEqualTo(AttributeId.NodeId.uid());

        // The control: an ordinary field is still written against BaseEventType, whose browse paths resolve
        // against any event type that has them.
        final SimpleAttributeOperand ordinary = java.util.Arrays.stream(clauses)
                .filter(clause -> clause.getBrowsePath() != null
                        && clause.getBrowsePath().length == 1
                        && "Severity".equals(clause.getBrowsePath()[0].getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no Severity operand"));
        assertThat(ordinary.getTypeDefinitionId()).isEqualTo(NodeIds.BaseEventType);
        assertThat(ordinary.getAttributeId()).isEqualTo(AttributeId.Value.uid());
    }

    @Test
    void theDeclaredTypeIsStillReadableForEveryOtherKind() {
        // The narrowing is specific to REFRESH. A condition or query tag's declared type is the whole point
        // of the field, so getPublishedType must be an identity there.
        for (final OpcuaTagKind kind :
                List.of(OpcuaTagKind.VALUE, OpcuaTagKind.CONDITION, OpcuaTagKind.EVENT_SUBSCRIPTION)) {
            final OpcuaTagDefinition tag =
                    new OpcuaTagDefinition("ns=2;s=Boiler1.HighTemp", kind, OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);
            assertThat(tag.getPublishedFields())
                    .as("%s must publish the type it declares", kind)
                    .isEqualTo(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);
        }
    }

    @Test
    void theRefreshSchemaDoesNotPromiseAlarmFields() {
        // The user-visible half of the finding: an operator adding a refresh tag saw a fifty-field alarm
        // schema for a tag that carries RefreshStart, RefreshEnd, RefreshRequired and EventQueueOverflow.
        final ObjectNode schema = SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.readSchema(
                refreshTag(OpcuaConditionType.ALARM_CONDITION).getPublishedFields()));
        final ObjectNode properties = (ObjectNode) schema.get("properties");

        assertThat(properties.has("ActiveState"))
                .as("a control event has no active state")
                .isFalse();
        assertThat(properties.has("HighLimit")).as("nor a limit").isFalse();
        assertThat(properties.has("EventType"))
                .as("but it does carry the base event fields, which is how it is recognised at all")
                .isTrue();
    }

    /** The field names the refresh tag's event filter actually asks the server for. */
    private static @NotNull List<String> selectClauseFieldNames() {
        final SimpleAttributeOperand[] clauses =
                ConditionEventFilters.forRefresh().getSelectClauses();
        assertThat(clauses).isNotNull();
        return java.util.Arrays.stream(clauses)
                .map(clause -> {
                    final var path = clause.getBrowsePath();
                    // An empty path is ConditionId: the event's own node id rather than a property beneath it.
                    return path == null || path.length == 0
                            ? OpcuaConditionType.CONDITION_ID
                            : String.valueOf(path[0].getName());
                })
                .collect(Collectors.toList());
    }

    private static @NotNull List<String> schemaPropertyNames(final @NotNull EventFieldSet type) {
        final ObjectNode schema =
                SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.readSchema(type));
        final List<String> names = new java.util.ArrayList<>();
        schema.get("properties").fieldNames().forEachRemaining(names::add);
        // Edge's own diagnostic companion, not a selected field.
        names.remove(com.hivemq.edge.adapters.opcua.northbound.OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS);
        return names;
    }
}
