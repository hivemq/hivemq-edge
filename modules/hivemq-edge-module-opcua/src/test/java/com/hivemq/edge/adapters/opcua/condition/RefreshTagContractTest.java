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
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.util.List;
import java.util.stream.Collectors;
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
 * The fix is {@link OpcuaTagDefinition#getPublishedType()}: one accessor all four read.
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
        final OpcuaConditionType published = tag.getPublishedType();

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
            assertThat(refreshTag(declared).getPublishedType())
                    .as("a refresh tag declaring '%s' still publishes the ConditionType shape", declared.browseName())
                    .isEqualTo(OpcuaConditionType.CONDITION);
        }
    }

    @Test
    void theDeclaredTypeIsStillReadableForEveryOtherKind() {
        // The narrowing is specific to REFRESH. A condition or query tag's declared type is the whole point
        // of the field, so getPublishedType must be an identity there.
        for (final OpcuaTagKind kind :
                List.of(OpcuaTagKind.VALUE, OpcuaTagKind.CONDITION, OpcuaTagKind.EVENT_SUBSCRIPTION)) {
            final OpcuaTagDefinition tag =
                    new OpcuaTagDefinition("ns=2;s=Boiler1.HighTemp", kind, OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);
            assertThat(tag.getPublishedType())
                    .as("%s must publish the type it declares", kind)
                    .isEqualTo(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);
        }
    }

    @Test
    void theRefreshSchemaDoesNotPromiseAlarmFields() {
        // The user-visible half of the finding: an operator adding a refresh tag saw a fifty-field alarm
        // schema for a tag that carries RefreshStart, RefreshEnd, RefreshRequired and EventQueueOverflow.
        final ObjectNode schema = SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.readSchema(
                refreshTag(OpcuaConditionType.ALARM_CONDITION).getPublishedType()));
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

    private static @NotNull List<String> schemaPropertyNames(final @NotNull OpcuaConditionType type) {
        final ObjectNode schema =
                SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.readSchema(type));
        final List<String> names = new java.util.ArrayList<>();
        schema.get("properties").fieldNames().forEachRemaining(names::add);
        // Edge's own diagnostic companion, not a selected field.
        names.remove(com.hivemq.edge.adapters.opcua.northbound.OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS);
        return names;
    }
}
