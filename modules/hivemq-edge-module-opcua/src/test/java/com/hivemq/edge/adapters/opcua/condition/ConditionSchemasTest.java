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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * A condition tag is the case that makes read and write schemas genuinely differ, and the case where the read
 * shape depends on the declared type rather than on the device. These check both claims on the rendered JSON
 * Schema, which is what a consumer actually sees.
 */
class ConditionSchemasTest {

    private @NotNull ObjectNode render(final @NotNull com.hivemq.adapter.sdk.api.schema.Schema schema) {
        return SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(schema);
    }

    @Test
    void theReadSchemaCarriesEveryFieldOfTheDeclaredType() {
        final OpcuaConditionType levelAlarm =
                OpcuaConditionType.fromBrowseName("ExclusiveLevelAlarmType").orElseThrow();

        final ObjectNode json = render(ConditionSchemas.readSchema(levelAlarm));
        final ObjectNode properties = (ObjectNode) json.get("properties");

        assertThat(properties).isNotNull();
        for (final String field : levelAlarm.allFields()) {
            assertThat(properties.has(field))
                    .as("the read schema must describe '%s'", field)
                    .isTrue();
        }
    }

    @Test
    void aRicherTypeYieldsARicherReadSchema() {
        // The point of declaring the type: the schema promises exactly what the subscription will select.
        final ObjectNode plain = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()));
        final ObjectNode level = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("ExclusiveLevelAlarmType").orElseThrow()));

        assertThat(level.get("properties").has("HighLimit"))
                .as("a level alarm's schema must promise its limits")
                .isTrue();
        assertThat(plain.get("properties").has("HighLimit"))
                .as("a plain alarm's schema must not")
                .isFalse();
    }

    @Test
    void nodeReferencingFieldsAreTypedAsNodeIds() {
        // These carry a NodeId, not the referenced variable's value. Left unclassified they would fall through
        // to the string default, which describes something the server never sends -- and the schema is the
        // only place a consumer can learn the difference before writing a rule against the field.
        final ObjectNode deviation = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("ExclusiveDeviationAlarmType").orElseThrow()));

        for (final String field : java.util.List.of("SetpointNode", "BaseSetpointNode", "InputNode")) {
            assertThat(describesANodeId(deviation, field))
                    .as("'%s' must be typed as a node id, not a string", field)
                    .isTrue();
        }

        final ObjectNode discrepancy = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("DiscrepancyAlarmType").orElseThrow()));
        assertThat(describesANodeId(discrepancy, "TargetValueNode"))
                .as("'TargetValueNode' must be typed as a node id, not a string")
                .isTrue();

        // Not every node id carries the "Node" suffix. TrustListId is a NodeId per OPC 10000-12 §7.8.2.11,
        // and it is defined outside Part 9 -- which is why the Part 9 sweep that found the other node-id
        // fields missed it.
        final ObjectNode trustList = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("TrustListOutOfDateAlarmType").orElseThrow()));
        assertThat(describesANodeId(trustList, "TrustListId"))
                .as("'TrustListId' must be typed as a node id despite the name not saying so")
                .isTrue();
    }

    /**
     * Whether the schema types one field as a node id structure. Every event field is nullable, so each
     * renders as {@code anyOf [shape, null]} and the shape sits one level below the property.
     */
    private boolean describesANodeId(final @NotNull ObjectNode schema, final @NotNull String field) {
        final JsonNode property = schema.get("properties").get(field);
        if (property == null) {
            return false;
        }
        for (final JsonNode alternative : property.path("anyOf")) {
            if (alternative.path("properties").has("idType")) {
                return true;
            }
        }
        return false;
    }

    @Test
    void qualityIsTypedAsAStatusCodeNotALocalizedText() {
        // Quality reads like a state and sits among fields that genuinely are LocalizedText, but OPC 10000-9
        // §5.5.2 Table 8 types it StatusCode. It is Mandatory on ConditionType, so it rides in every event of
        // every one of the 22 types -- describing it as {locale, text} mistyped a field present in every
        // payload Edge publishes, and a schema-validating consumer would see a mismatch on all of them.
        for (final OpcuaConditionType type : OpcuaConditionType.values()) {
            final ObjectNode json = render(ConditionSchemas.readSchema(type));
            final JsonNode quality = json.get("properties").get("Quality");
            assertThat(quality)
                    .as("%s must describe Quality", type.browseName())
                    .isNotNull();

            final JsonNode shape = shapeOf(quality);
            assertThat(shape.path("properties").has("code"))
                    .as("%s must type Quality as a status code", type.browseName())
                    .isTrue();
            assertThat(shape.path("properties").has("locale"))
                    .as("%s must not type Quality as a localized text", type.browseName())
                    .isFalse();
        }
    }

    @Test
    void theQualityShapeMatchesWhatTheConverterEmits() {
        // The converter renders a StatusCode as {code, symbol}, with symbol present only when the numeric
        // code resolves to a known name. A schema that promised symbol unconditionally would be wrong for
        // every vendor-specific code.
        final ObjectNode json = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()));
        final JsonNode shape = shapeOf(json.get("properties").get("Quality"));

        assertThat(shape.path("properties").has("symbol")).isTrue();
        assertThat(shape.path("required"))
                .as("neither part of a status code is promised")
                .isEmpty();
    }

    /** The non-null alternative of a nullable property, which renders as {@code anyOf [shape, null]}. */
    private @NotNull JsonNode shapeOf(final @NotNull JsonNode property) {
        for (final JsonNode alternative : property.path("anyOf")) {
            if (alternative.has("properties")) {
                return alternative;
            }
        }
        return property;
    }

    @Test
    void twoStateFieldsCarryTheirBooleanId() {
        // The state's Value is a human-readable name whose wording depends on the session locale and the
        // vendor, so it is not something to branch on. The Id is the same state as a Boolean, is Mandatory on
        // TwoStateVariableType, and reaches us only because the select clause asks for the two-element path.
        final ObjectNode json = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()));

        for (final String field : java.util.List.of("ActiveState", "AckedState", "EnabledState")) {
            final JsonNode shape = shapeOf(json.get("properties").get(field));
            assertThat(shape.path("properties").has("id"))
                    .as("'%s' must promise its Boolean id", field)
                    .isTrue();
            assertThat(shape.path("properties").has("text"))
                    .as("'%s' must still promise its display text", field)
                    .isTrue();
        }
    }

    @Test
    void onlyTwoStateFieldsGetAnId() {
        // Message is a LocalizedText but not a state, and ShelvingState/LimitState are Objects with their own
        // state machines rather than two-state variables. An `id` on any of them would be a promise the
        // server never fills.
        final ObjectNode json = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()));

        assertThat(shapeOf(json.get("properties").get("Message"))
                        .path("properties")
                        .has("id"))
                .as("Message is a localized text, not a two-state field")
                .isFalse();
    }

    @Test
    void theReadAndWriteSchemasDiffer() {
        // If these agreed there would be no reason for the tag to carry two, and the southbound editor would
        // show the alarm's fields instead of the command's.
        final ObjectNode read = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()));
        final ObjectNode write = render(ConditionSchemas.writeSchema());

        assertThat(write).isNotEqualTo(read);
        assertThat(read.get("properties").has("EventId")).isTrue();
        assertThat(read.get("properties").has(ConditionUpdate.FIELD_METHOD)).isFalse();
    }

    @Test
    void theEventIdIsDeclaredAsBase64OnBothSides() {
        // An EventId is an opaque ByteString rendered as base64. Declaring it a plain string would leave a
        // consumer to infer the encoding from the shape of the value -- which cannot be done reliably, since
        // base64 and arbitrary text are indistinguishable by inspection. contentEncoding says it outright,
        // and it has to say the same thing on both sides or a client cannot echo the value back.
        final ObjectNode read = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()));
        final ObjectNode write = render(ConditionSchemas.writeSchema());

        final ObjectNode northbound = (ObjectNode) read.get("properties").get("EventId");
        assertThat(northbound.get("contentEncoding").asText()).isEqualTo("base64");

        final ObjectNode southbound = (ObjectNode) write.get("properties").get(ConditionUpdate.FIELD_EVENT_ID);
        assertThat(southbound.get("contentEncoding").asText()).isEqualTo("base64");
    }

    @Test
    void theWriteSchemaIsTheCommand() {
        final ObjectNode write = render(ConditionSchemas.writeSchema());
        final ObjectNode properties = (ObjectNode) write.get("properties");

        assertThat(properties.has(ConditionUpdate.FIELD_METHOD)).isTrue();
        assertThat(properties.has(ConditionUpdate.FIELD_EVENT_ID)).isTrue();
        assertThat(properties.has(ConditionUpdate.FIELD_COMMENT)).isTrue();
        assertThat(properties.has(ConditionUpdate.FIELD_DURATION)).isTrue();

        // The alarm's own fields have no place in a command.
        assertThat(properties.has("Severity")).isFalse();
        assertThat(properties.has("ActiveState")).isFalse();
    }

    @Test
    void onlyTheMethodIsRequiredToWrite() {
        // Ten of the fourteen methods take no arguments, so requiring eventId or duration would describe a
        // command most callers cannot send. The per-method check happens at call time instead.
        final ObjectNode write = render(ConditionSchemas.writeSchema());

        assertThat(write.get("required")).isNotNull();
        assertThat(write.get("required").toString()).contains(ConditionUpdate.FIELD_METHOD);
        assertThat(write.get("required").toString())
                .doesNotContain(ConditionUpdate.FIELD_EVENT_ID)
                .doesNotContain(ConditionUpdate.FIELD_DURATION);
    }

    @Test
    void theWriteSchemaIsWritableAtItsRoot() {
        // Caught by reading the rendered output: the properties were writeOnly while the root object was
        // readOnly, which describes a command that cannot be sent. Only visible in the JSON, not in the
        // property-level assertions.
        final ObjectNode write = render(ConditionSchemas.writeSchema());

        assertThat(write.has("readOnly"))
                .as("the command object must not be marked readOnly")
                .isFalse();
        assertThat(write.get("writeOnly")).isNotNull();
    }

    @Test
    void theReadSchemaIsReadableAtItsRoot() {
        final ObjectNode read = render(ConditionSchemas.readSchema(
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow()));

        assertThat(read.has("writeOnly"))
                .as("a transition report is an observation, not something to write")
                .isFalse();
    }

    @Test
    void theWriteSchemaNamesEveryMethod() {
        // The command is only usable if it says what may go in `method`.
        final String description = render(ConditionSchemas.writeSchema()).toString();
        for (final ConditionUpdate.Method method : ConditionUpdate.Method.values()) {
            assertThat(description)
                    .as("the write schema must mention '%s'", method.name())
                    .contains(method.name());
        }
    }
}
