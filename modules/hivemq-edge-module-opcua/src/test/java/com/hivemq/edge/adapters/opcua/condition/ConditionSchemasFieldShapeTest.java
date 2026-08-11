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
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ushort;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaEventToJsonConverter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;
import org.eclipse.milo.opcua.stack.core.types.structured.TimeZoneDataType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The advertised read schema against the JSON the converter actually emits, for every field of all 22 types.
 * <p>
 * Review finding 2: fifteen fields had a schema that did not describe their payload. They were not exotic —
 * {@code Comment} is declared on {@code ConditionType}, so it rides in every event of every type, and a
 * perfectly valid condition carrying a non-null comment published a {@code {locale, text}} object against a
 * schema promising a string. The cause was structural rather than fifteen separate oversights:
 * {@code appendField} ended in a fallback that <em>declared</em> the field a string while its comment claimed
 * to be leaving it open, so a field nobody had classified silently acquired a wrong type instead of a missing
 * one.
 * <p>
 * Two independent checks, because either alone can be satisfied by wrong code:
 * <ul>
 *   <li>{@link #everyFieldIsClassifiedAsItsSpecificationDatatypeSays()} pins each field's shape against a
 *       hand-maintained transcription of the specification's type tables. This is the anchor — it is the only
 *       assertion here that does not derive its expectation from Edge's own code, and it is what would have
 *       caught the original fifteen.</li>
 *   <li>{@link #everyFieldsConverterOutputMatchesItsSchema()} feeds a representative value of each declared
 *       shape through the converter and checks the emitted node against the rendered schema. This catches the
 *       other direction: a field classified correctly but whose schema arm and converter branch disagree.</li>
 * </ul>
 * Both are exhaustive over every field of every type, and the table is checked for exhaustiveness in both
 * directions, so a field added to {@link OpcuaConditionType} cannot slip through unclassified.
 */
class ConditionSchemasFieldShapeTest {

    /**
     * Every selected field, with the datatype the specification declares for it and the shape that implies.
     * <p>
     * Transcribed from OPC 10000-9 (the condition types) and OPC 10000-12 §7.8.2.11 ({@code
     * TrustListOutOfDateAlarmType}, the one type defined outside Part 9), plus OPC 10000-5 §6.4.2 for the
     * {@code BaseEventType} members every event carries. The datatype string is carried for the failure
     * message: when this test fails, the useful thing to read is what the specification says the field is,
     * not which set it landed in.
     */
    private static final @NotNull Map<String, Expected> SPECIFICATION = specification();

    private record Expected(
            @NotNull String datatype, @NotNull ConditionSchemas.Shape shape) {}

    private static @NotNull Map<String, Expected> specification() {
        final Map<String, Expected> table = new LinkedHashMap<>();

        // ── BaseEventType — OPC 10000-5 §6.4.2, carried by every event of every type ──────────────────
        table.put("EventId", new Expected("ByteString", ConditionSchemas.Shape.BYTE_STRING));
        table.put("EventType", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("SourceNode", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("SourceName", new Expected("String", ConditionSchemas.Shape.STRING));
        table.put("Time", new Expected("UtcTime", ConditionSchemas.Shape.INSTANT));
        table.put("ReceiveTime", new Expected("UtcTime", ConditionSchemas.Shape.INSTANT));
        table.put("LocalTime", new Expected("TimeZoneDataType", ConditionSchemas.Shape.LOCAL_TIME));
        table.put("Message", new Expected("LocalizedText", ConditionSchemas.Shape.LOCALIZED_TEXT));
        table.put("Severity", new Expected("UInt16", ConditionSchemas.Shape.INTEGER));
        // Not a member of any type: the event's own node id, selected with an empty browse path against the
        // NodeId attribute. See OpcuaConditionType.CONDITION_ID.
        table.put("ConditionId", new Expected("NodeId (the event node itself)", ConditionSchemas.Shape.NODE_ID));

        // ── ConditionType — §5.5.2 Table 8 ───────────────────────────────────────────────────────────
        table.put("BranchId", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("ClientUserId", new Expected("String", ConditionSchemas.Shape.STRING));
        table.put("Comment", new Expected("LocalizedText", ConditionSchemas.Shape.LOCALIZED_TEXT));
        table.put("ConditionClassId", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("ConditionClassName", new Expected("LocalizedText", ConditionSchemas.Shape.LOCALIZED_TEXT));
        table.put("ConditionName", new Expected("String", ConditionSchemas.Shape.STRING));
        table.put("ConditionSubClassId", new Expected("NodeId[]", ConditionSchemas.Shape.NODE_ID_ARRAY));
        table.put(
                "ConditionSubClassName", new Expected("LocalizedText[]", ConditionSchemas.Shape.LOCALIZED_TEXT_ARRAY));
        table.put("EnabledState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        table.put("LastSeverity", new Expected("UInt16", ConditionSchemas.Shape.INTEGER));
        table.put("Quality", new Expected("StatusCode", ConditionSchemas.Shape.STATUS_CODE));
        table.put("Retain", new Expected("Boolean", ConditionSchemas.Shape.BOOLEAN));
        table.put("SupportsFilteredRetain", new Expected("Boolean", ConditionSchemas.Shape.BOOLEAN));

        // ── AcknowledgeableConditionType — §5.7.2 ────────────────────────────────────────────────────
        table.put("AckedState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        table.put("ConfirmedState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));

        // ── AlarmConditionType — §5.8.2 Table 40 ─────────────────────────────────────────────────────
        table.put("ActiveState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        table.put("AudibleEnabled", new Expected("Boolean", ConditionSchemas.Shape.BOOLEAN));
        // AudioDataType is a ByteString subtype, so it travels as base64 like EventId.
        table.put("AudibleSound", new Expected("AudioDataType", ConditionSchemas.Shape.BYTE_STRING));
        table.put("FirstInGroupFlag", new Expected("Boolean", ConditionSchemas.Shape.BOOLEAN));
        table.put("InputNode", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("LatchedState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        table.put("MaxTimeShelved", new Expected("Duration", ConditionSchemas.Shape.NUMBER));
        table.put("OffDelay", new Expected("Duration", ConditionSchemas.Shape.NUMBER));
        table.put("OnDelay", new Expected("Duration", ConditionSchemas.Shape.NUMBER));
        table.put("OutOfServiceState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        table.put("ReAlarmRepeatCount", new Expected("Int16", ConditionSchemas.Shape.INTEGER));
        table.put("ReAlarmTime", new Expected("Duration", ConditionSchemas.Shape.NUMBER));
        table.put("SilenceState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        table.put("SuppressedOrShelved", new Expected("Boolean", ConditionSchemas.Shape.BOOLEAN));
        // Review-02 finding 11. AlarmConditionType §5.8.2 Table 40, Optional; the machine's current state is
        // read one level down like LimitState's, so the published shape is the same.
        table.put("ShelvingState", new Expected("ShelvedStateMachineType", ConditionSchemas.Shape.STATE_MACHINE));
        // ShelvedStateMachineType's own property, §5.8.17 Table 73, and Mandatory there -- so wherever the
        // machine is exposed at all this is too. A Duration, like MaxTimeShelved.
        table.put("UnshelveTime", new Expected("Duration", ConditionSchemas.Shape.NUMBER));
        table.put("SuppressedState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));

        // ── OffNormalAlarmType — §5.8.20 ─────────────────────────────────────────────────────────────
        // A NodeId despite the name: it points at the variable holding the value considered normal, so it
        // belongs with InputNode rather than with the two-state fields it reads like.
        table.put("NormalState", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));

        // ── CertificateExpirationAlarmType — §5.8.24 Table 112 ───────────────────────────────────────
        table.put("Certificate", new Expected("ByteString", ConditionSchemas.Shape.BYTE_STRING));
        table.put("CertificateType", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("ExpirationDate", new Expected("DateTime", ConditionSchemas.Shape.INSTANT));
        table.put("ExpirationLimit", new Expected("Duration", ConditionSchemas.Shape.NUMBER));

        // ── DialogConditionType — §5.6.2 Table 32 ────────────────────────────────────────────────────
        table.put("CancelResponse", new Expected("Int32", ConditionSchemas.Shape.INTEGER));
        table.put("DefaultResponse", new Expected("Int32", ConditionSchemas.Shape.INTEGER));
        table.put("DialogState", new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        table.put("LastResponse", new Expected("Int32", ConditionSchemas.Shape.INTEGER));
        table.put("OkResponse", new Expected("Int32", ConditionSchemas.Shape.INTEGER));
        table.put("Prompt", new Expected("LocalizedText", ConditionSchemas.Shape.LOCALIZED_TEXT));
        table.put("ResponseOptionSet", new Expected("LocalizedText[]", ConditionSchemas.Shape.LOCALIZED_TEXT_ARRAY));

        // ── DiscrepancyAlarmType — §5.8.23 ───────────────────────────────────────────────────────────
        table.put("ExpectedTime", new Expected("Duration", ConditionSchemas.Shape.NUMBER));
        table.put("TargetValueNode", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("Tolerance", new Expected("Double", ConditionSchemas.Shape.NUMBER));

        // ── LimitAlarmType — §5.8.18 Table 92 ────────────────────────────────────────────────────────
        for (final String limit : List.of(
                "BaseHighHighLimit",
                "BaseHighLimit",
                "BaseLowLimit",
                "BaseLowLowLimit",
                "HighDeadband",
                "HighHighDeadband",
                "HighHighLimit",
                "HighLimit",
                "LowDeadband",
                "LowLimit",
                "LowLowDeadband",
                "LowLowLimit")) {
            table.put(limit, new Expected("Double", ConditionSchemas.Shape.NUMBER));
        }
        for (final String severity : List.of("SeverityHigh", "SeverityHighHigh", "SeverityLow", "SeverityLowLow")) {
            table.put(severity, new Expected("UInt16", ConditionSchemas.Shape.INTEGER));
        }

        // ── ExclusiveLimitAlarmType — §5.8.19.3 Table 96 ─────────────────────────────────────────────
        // An Object with a state machine rather than a value, so what is published is its CurrentState with
        // the active state's NodeId as `id`.
        table.put("LimitState", new Expected("ExclusiveLimitStateMachineType", ConditionSchemas.Shape.STATE_MACHINE));

        // ── the deviation alarms ─────────────────────────────────────────────────────────────────────
        table.put("BaseSetpointNode", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("SetpointNode", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));

        // ── the rate-of-change alarms ────────────────────────────────────────────────────────────────
        table.put("EngineeringUnits", new Expected("EUInformation", ConditionSchemas.Shape.ENGINEERING_UNITS));

        // ── NonExclusiveLimitAlarmType — §5.8.19.4 Table 97 ──────────────────────────────────────────
        for (final String state : List.of("HighHighState", "HighState", "LowLowState", "LowState")) {
            table.put(state, new Expected("TwoStateVariableType", ConditionSchemas.Shape.TWO_STATE));
        }

        // ── TrustListOutOfDateAlarmType — OPC 10000-12 §7.8.2.11 ─────────────────────────────────────
        table.put("LastUpdateTime", new Expected("UtcTime", ConditionSchemas.Shape.INSTANT));
        table.put("TrustListId", new Expected("NodeId", ConditionSchemas.Shape.NODE_ID));
        table.put("UpdateFrequency", new Expected("Duration", ConditionSchemas.Shape.NUMBER));

        return Map.copyOf(table);
    }

    /** Every field any of the 22 types selects, which is what both exhaustive checks walk. */
    private static @NotNull Set<String> allSelectedFields() {
        final Set<String> fields = new TreeSet<>();
        for (final OpcuaConditionType type : OpcuaConditionType.values()) {
            fields.addAll(type.allFields());
        }
        return fields;
    }

    @Test
    void theSpecificationTableCoversExactlyTheFieldsEdgeSelects() {
        // Both directions. A field added to OpcuaConditionType without an entry here would otherwise be
        // classified by whichever set it happened to land in -- or, failing all of them, silently declared a
        // string, which is precisely how the original fifteen arose. An entry with no field is a
        // transcription left behind by a rename.
        assertThat(allSelectedFields())
                .as("every selected field needs a specification datatype recorded for it")
                .allSatisfy(field -> assertThat(SPECIFICATION)
                        .as("no specification datatype recorded for '%s'", field)
                        .containsKey(field));
        assertThat(SPECIFICATION.keySet())
                .as("this table must not describe fields Edge does not select")
                .isSubsetOf(allSelectedFields());
    }

    @Test
    void everyFieldIsClassifiedAsItsSpecificationDatatypeSays() {
        // The anchor. Every other assertion in this file derives its expectation from Edge's own code, so
        // wrong code satisfies them; this one compares against the specification's tables and does not.
        for (final Map.Entry<String, Expected> entry : SPECIFICATION.entrySet()) {
            final String field = entry.getKey();
            final Expected expected = entry.getValue();
            assertThat(ConditionSchemas.Shape.shapeOf(field))
                    .as("'%s' is declared %s by the specification", field, expected.datatype())
                    .isEqualTo(expected.shape());
        }
    }

    @Test
    void everyFieldsConverterOutputMatchesItsSchema() {
        // The other direction: a field can be classified correctly and still have a schema arm that
        // disagrees with the converter branch, because the two are written separately.
        for (final OpcuaConditionType type : OpcuaConditionType.values()) {
            final ObjectNode schema =
                    SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.readSchema(type));
            final JsonNode properties = schema.get("properties");

            for (final String field : type.allFields()) {
                final ConditionSchemas.Shape shape = ConditionSchemas.Shape.shapeOf(field);
                final JsonNode emitted = emit(type, field, shape);

                assertThat(properties.has(field))
                        .as("%s: the schema must describe '%s'", type.browseName(), field)
                        .isTrue();
                assertShapeMatches(type, field, shape, emitted, nonNullAlternative(properties.get(field)));
            }
        }
    }

    @Test
    void theFallbackToStringIsOnlyReachedByFieldsThatAreStrings() {
        // The branch that caused the finding. It is an assertion now rather than a catch-all, so anything
        // landing in it has to genuinely be a string on the wire.
        for (final String field : allSelectedFields()) {
            if (ConditionSchemas.Shape.shapeOf(field) != ConditionSchemas.Shape.STRING) {
                continue;
            }
            assertThat(SPECIFICATION.get(field).datatype())
                    .as(
                            "'%s' falls through to the string default, so it must really be a string or a "
                                    + "timestamp -- anything else is the finding-2 defect returning",
                            field)
                    .isIn("String", "UtcTime", "DateTime");
        }
    }

    @Test
    void commentIsAnObjectOnEveryTypeBecauseEveryTypeCarriesIt() {
        // Worth its own test because of the blast radius. Comment is declared on ConditionType, so it is in
        // every event of all 22 types -- typed as a string, a single valid comment violated the tag's own
        // advertised schema, and a validating consumer would reject exactly the message an operator had just
        // annotated.
        for (final OpcuaConditionType type : OpcuaConditionType.values()) {
            assertThat(type.allFields()).contains("Comment");

            final JsonNode emitted = emit(type, "Comment", ConditionSchemas.Shape.LOCALIZED_TEXT);
            assertThat(emitted.isObject())
                    .as("%s: a comment is emitted as {locale, text}", type.browseName())
                    .isTrue();
            assertThat(emitted.get("text").textValue()).isEqualTo("Erwin has seen this");

            final ObjectNode schema =
                    SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.readSchema(type));
            final JsonNode shape = nonNullAlternative(schema.get("properties").get("Comment"));
            assertThat(shape.path("properties").has("text"))
                    .as("%s: and the schema must say so", type.browseName())
                    .isTrue();
        }
    }

    @Test
    void engineeringUnitsIsStructuredRatherThanStringified() {
        // Review finding 13. Milo decodes EUInformation into its own generated class rather than a
        // DynamicStructType, so without an explicit converter branch it fell through to toString() -- a Java
        // rendering whose format is Milo's business, with the four members that make a unit machine-readable
        // collapsed into it.
        final OpcuaConditionType rateOfChange = OpcuaConditionType.fromBrowseName("ExclusiveRateOfChangeAlarmType")
                .orElseThrow();

        final JsonNode emitted = emit(rateOfChange, "EngineeringUnits", ConditionSchemas.Shape.ENGINEERING_UNITS);

        assertThat(emitted.get("namespaceUri").textValue())
                .isEqualTo("http://www.opcfoundation.org/UA/units/un/cefact");
        assertThat(emitted.get("unitId").intValue()).isEqualTo(4408652);
        assertThat(emitted.get("displayName").get("text").textValue()).isEqualTo("°C");
        assertThat(emitted.get("description").get("text").textValue()).isEqualTo("degree Celsius");
    }

    @Test
    void audibleSoundIsSelectedAndDeclaredBinary() {
        // Review finding 14. The enum drives the select clause, the decoder and the schema alike, so a field
        // missing from it cannot appear at all -- AudibleEnabled was published while the sound it refers to
        // could never be obtained.
        final OpcuaConditionType alarm =
                OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow();

        assertThat(alarm.allFields()).contains("AudibleSound");

        final JsonNode emitted = emit(alarm, "AudibleSound", ConditionSchemas.Shape.BYTE_STRING);
        assertThat(emitted.textValue()).as("a ByteString travels as base64").isEqualTo("AAECAw==");

        final ObjectNode schema =
                SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(ConditionSchemas.readSchema(alarm));
        final JsonNode shape = nonNullAlternative(schema.get("properties").get("AudibleSound"));
        assertThat(shape.path("contentEncoding").asText())
                .as("declared BINARY, so a consumer is told it is base64 rather than left to infer it")
                .isEqualTo("base64");
    }

    // ── machinery ───────────────────────────────────────────────────────────────────────────────────

    /** Asserts that one emitted value has the shape its schema promises. */
    private static void assertShapeMatches(
            final @NotNull OpcuaConditionType type,
            final @NotNull String field,
            final @NotNull ConditionSchemas.Shape shape,
            final @NotNull JsonNode emitted,
            final @NotNull JsonNode schemaShape) {

        final String where = type.browseName() + "." + field;
        switch (shape) {
            case LOCAL_TIME -> {
                assertThat(emitted.isObject()).as("%s is an object", where).isTrue();
                assertThat(emitted.has("offset"))
                        .as("%s carries an offset", where)
                        .isTrue();
                assertThat(schemaShape.path("properties").has("offset")).isTrue();
            }
            case STATE_MACHINE, TWO_STATE -> {
                assertThat(emitted.isObject()).as("%s is an object", where).isTrue();
                assertThat(emitted.has("text"))
                        .as("%s carries display text", where)
                        .isTrue();
                assertThat(emitted.has("id"))
                        .as("%s carries a machine-readable id", where)
                        .isTrue();
                assertThat(schemaShape.path("properties").has("id")).isTrue();
                // The difference between the two: a two-state id is a boolean, a state machine's is a node id.
                if (shape == ConditionSchemas.Shape.TWO_STATE) {
                    assertThat(emitted.get("id").isBoolean())
                            .as("%s's id is a boolean", where)
                            .isTrue();
                } else {
                    assertThat(emitted.get("id").isObject())
                            .as("%s's id is a node id -- four states cannot be told apart by true/false", where)
                            .isTrue();
                }
            }
            case LOCALIZED_TEXT -> {
                assertThat(emitted.isObject()).as("%s is an object", where).isTrue();
                assertThat(emitted.has("text")).as("%s carries text", where).isTrue();
                assertThat(schemaShape.path("properties").has("text")).isTrue();
            }
            case LOCALIZED_TEXT_ARRAY -> {
                assertThat(emitted.isArray()).as("%s is an array", where).isTrue();
                assertThat(emitted.get(0).has("text")).isTrue();
                assertThat(schemaShape.path("items").path("properties").has("text"))
                        .as("%s's schema must describe an array of localized texts", where)
                        .isTrue();
            }
            case NODE_ID -> {
                assertThat(emitted.isObject()).as("%s is an object", where).isTrue();
                assertThat(emitted.has("idType"))
                        .as("%s carries an idType", where)
                        .isTrue();
                assertThat(schemaShape.path("properties").has("idType")).isTrue();
            }
            case NODE_ID_ARRAY -> {
                assertThat(emitted.isArray()).as("%s is an array", where).isTrue();
                assertThat(emitted.get(0).has("idType")).isTrue();
                assertThat(schemaShape.path("items").path("properties").has("idType"))
                        .as("%s's schema must describe an array of node ids", where)
                        .isTrue();
            }
            case ENGINEERING_UNITS -> {
                assertThat(emitted.isObject()).as("%s is an object", where).isTrue();
                assertThat(emitted.has("unitId"))
                        .as("%s carries a unit id", where)
                        .isTrue();
                assertThat(schemaShape.path("properties").has("unitId")).isTrue();
            }
            case STATUS_CODE -> {
                assertThat(emitted.isObject()).as("%s is an object", where).isTrue();
                assertThat(emitted.has("code"))
                        .as("%s carries a numeric code", where)
                        .isTrue();
                assertThat(schemaShape.path("properties").has("code")).isTrue();
            }
            case NUMBER -> {
                assertThat(emitted.isNumber()).as("%s is a number", where).isTrue();
                assertDeclaredType(where, schemaShape, "number");
            }
            case INTEGER -> {
                assertThat(emitted.isIntegralNumber())
                        .as("%s is an integer", where)
                        .isTrue();
                assertDeclaredType(where, schemaShape, "integer");
            }
            case BOOLEAN -> {
                assertThat(emitted.isBoolean()).as("%s is a boolean", where).isTrue();
                assertDeclaredType(where, schemaShape, "boolean");
            }
            case BYTE_STRING -> {
                assertThat(emitted.isTextual()).as("%s is base64 text", where).isTrue();
                assertDeclaredType(where, schemaShape, "string");
                assertThat(schemaShape.path("contentEncoding").asText())
                        .as("%s must be declared base64", where)
                        .isEqualTo("base64");
            }
            case STRING -> {
                assertThat(emitted.isTextual()).as("%s is a string", where).isTrue();
                assertDeclaredType(where, schemaShape, "string");
            }
        }
    }

    /**
     * Asserts a scalar property's declared type.
     * <p>
     * A nullable scalar renders its type as an <em>array</em> — {@code "type": ["string", "null"]} — while a
     * nullable object or array renders as {@code anyOf}. Every event field is nullable, so the array form is
     * what these actually are; reading {@code type} as text on one silently yields the empty string, which
     * would make this assertion pass for anything.
     */
    // ── review-02 finding 14: what the schema says the value is ────────────────────────────────────

    @Test
    void aTimestampIsDeclaredAnInstantRatherThanArbitraryText() {
        // The four temporal fields reached the STRING fallback, which is true as far as JSON goes and
        // useless as a contract: it tells a consumer it may receive any text at all, where the adapter in
        // fact promises an RFC 3339 instant. Nothing was ever wrong on the wire -- the converter has always
        // rendered these as ISO -- so this is entirely about what the schema is willing to say.
        final ObjectNode properties = propertiesOf(OpcuaConditionType.CERTIFICATE_EXPIRATION_ALARM);

        for (final String field : List.of("Time", "ReceiveTime", "ExpirationDate")) {
            final JsonNode shape = nonNullAlternative(properties.get(field));
            assertDeclaredType(field, shape, "string");
            assertThat(shape.path("format").asText())
                    .as("%s is a timestamp, and the schema now says which kind of string it is", field)
                    .isEqualTo("date-time");
        }
    }

    @Test
    void aSeverityIsDeclaredAnIntegerRatherThanANumber() {
        // Severity is a UInt16 on BaseEventType and the four limit severities are UInt16 on LimitAlarmType.
        // Declared NUMBER, the schema accepted 700.5 as a valid alarm priority, and a generated client took
        // a double for something that indexes a priority scale.
        final ObjectNode properties = propertiesOf(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);

        for (final String field : List.of(
                "Severity", "LastSeverity", "SeverityHigh", "SeverityHighHigh", "SeverityLow", "SeverityLowLow")) {
            assertDeclaredType(field, nonNullAlternative(properties.get(field)), "integer");
        }
    }

    @Test
    void andSoIsTheReAlarmRepeatCount() {
        // An Int16 on AlarmConditionType (Table 40). A count of 2.5 re-alarms is not a thing to accept.
        assertDeclaredType(
                "ReAlarmRepeatCount",
                nonNullAlternative(
                        propertiesOf(OpcuaConditionType.ALARM_CONDITION).get("ReAlarmRepeatCount")),
                "integer");
    }

    @Test
    void butAGenuineDoubleIsStillANumber() {
        // The control. The limits, deadbands and delays really are Double in the specification, and
        // narrowing those to integers would be the same defect in the opposite direction -- a schema that
        // refuses a limit of 90.5.
        final ObjectNode properties = propertiesOf(OpcuaConditionType.EXCLUSIVE_LEVEL_ALARM);

        for (final String field : List.of("HighLimit", "HighDeadband", "OnDelay", "MaxTimeShelved")) {
            assertDeclaredType(field, nonNullAlternative(properties.get(field)), "number");
        }
    }

    @Test
    void andAFieldThatReallyIsTextStillIsText() {
        // The other control, and the reason the STRING fallback still exists at all.
        final ObjectNode properties = propertiesOf(OpcuaConditionType.CONDITION);

        for (final String field : List.of("SourceName", "ConditionName", "ClientUserId")) {
            assertDeclaredType(field, nonNullAlternative(properties.get(field)), "string");
            assertThat(nonNullAlternative(properties.get(field)).has("format"))
                    .as("%s is not a timestamp and must not claim to be one", field)
                    .isFalse();
        }
    }

    private static @NotNull ObjectNode propertiesOf(final @NotNull OpcuaConditionType type) {
        return (ObjectNode) SchemaJsonRepresentation.INSTANCE
                .toJsonSchemaDocument(ConditionSchemas.readSchema(type))
                .get("properties");
    }

    private static void assertDeclaredType(
            final @NotNull String where, final @NotNull JsonNode schemaShape, final @NotNull String expected) {

        final JsonNode type = schemaShape.get("type");
        assertThat(type).as("%s must declare a type", where).isNotNull();
        if (type.isArray()) {
            final List<String> declared = new java.util.ArrayList<>();
            type.forEach(entry -> declared.add(entry.asText()));
            assertThat(declared)
                    .as("%s is declared %s (and nullable, as every event field is)", where, expected)
                    .contains(expected)
                    .contains("null");
        } else {
            assertThat(type.asText()).as("%s is declared %s", where, expected).isEqualTo(expected);
        }
    }

    /**
     * Runs one field through the converter with a representative value of its declared shape, and returns the
     * node emitted under that field's key.
     */
    private static @NotNull JsonNode emit(
            final @NotNull OpcuaConditionType type,
            final @NotNull String field,
            final @NotNull ConditionSchemas.Shape shape) {

        final List<OpcuaConditionType.SelectedField> selected = type.selectedFields();
        final Variant[] values = new Variant[selected.size()];
        for (int i = 0; i < selected.size(); i++) {
            final OpcuaConditionType.SelectedField entry = selected.get(i);
            if (!entry.publishedAs().equals(field)) {
                continue;
            }
            values[i] = entry.isStateId() ? stateIdFor(shape) : valueFor(shape);
        }

        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);
        OpcUaEventToJsonConverter.convertPayload(DefaultEncodingContext.INSTANCE, type, values, builder);

        final JsonNode emitted = builder.build("test-adapter").getTagValue().get(field);
        assertThat(emitted)
                .as("%s.%s must be published", type.browseName(), field)
                .isNotNull();
        return emitted;
    }

    /** A value of the kind the specification says this field carries. */
    private static @NotNull Variant valueFor(final @NotNull ConditionSchemas.Shape shape) {
        return switch (shape) {
            case LOCAL_TIME -> new Variant(new TimeZoneDataType((short) 120, true));
            // A state machine's readable half is its CurrentState, a LocalizedText, exactly like a two-state
            // field's value. They differ only in what the Id beneath them is.
            case STATE_MACHINE, TWO_STATE -> new Variant(new LocalizedText("en", "Active"));
            case LOCALIZED_TEXT -> new Variant(new LocalizedText("en", "Erwin has seen this"));
            case LOCALIZED_TEXT_ARRAY ->
                new Variant(new LocalizedText[] {new LocalizedText("en", "Yes"), new LocalizedText("en", "No")});
            case NODE_ID -> new Variant(NodeId.parse("ns=2;s=Boiler1.Temperature"));
            case NODE_ID_ARRAY -> new Variant(new NodeId[] {NodeId.parse("ns=2;i=42")});
            case ENGINEERING_UNITS ->
                new Variant(new EUInformation(
                        "http://www.opcfoundation.org/UA/units/un/cefact",
                        4408652,
                        new LocalizedText("en", "°C"),
                        new LocalizedText("en", "degree Celsius")));
            case STATUS_CODE -> new Variant(new StatusCode(StatusCodes.Good));
            case NUMBER -> new Variant(90.0);
            // Deliberately a UInt16 rather than a plain int: Severity and the four limit severities arrive
            // from the server as unsigned shorts, and the point of declaring them integral is that the
            // converter's rendering of that type is a JSON integer.
            case INTEGER -> new Variant(ushort(700));
            case INSTANT -> new Variant(new DateTime());
            case BOOLEAN -> new Variant(true);
            case BYTE_STRING -> new Variant(new ByteString(new byte[] {0, 1, 2, 3}));
            case STRING -> new Variant("Boiler1.Temperature");
        };
    }

    /** The {@code Id} beneath a state: a Boolean for a two-state field, a NodeId for a state machine. */
    private static @NotNull Variant stateIdFor(final @NotNull ConditionSchemas.Shape shape) {
        return shape == ConditionSchemas.Shape.STATE_MACHINE
                ? new Variant(NodeId.parse("ns=0;i=9329"))
                : new Variant(true);
    }

    /** The non-null alternative of a nullable property, which renders as {@code anyOf [shape, null]}. */
    private static @NotNull JsonNode nonNullAlternative(final @NotNull JsonNode property) {
        for (final JsonNode alternative : property.path("anyOf")) {
            if (!"null".equals(alternative.path("type").asText())) {
                return alternative;
            }
        }
        return property;
    }
}
