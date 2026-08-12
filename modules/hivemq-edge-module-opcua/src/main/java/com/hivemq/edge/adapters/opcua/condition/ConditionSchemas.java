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

import com.hivemq.adapter.sdk.api.schema.ObjectSchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.edge.adapters.opcua.config.tag.EventFieldSet;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaEventToJsonConverter;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * The two schemas of a condition tag.
 * <p>
 * A condition is the case that makes read and write schemas genuinely differ. Northbound it publishes a
 * transition report — the condition type's event fields. Southbound it accepts a command asking the server to
 * move the state machine. Neither shape is a projection of the other, so one schema cannot describe both.
 * <p>
 * Neither is discovered from the device. The read shape follows from the tag's declared condition type, and
 * the write shape is the same for every condition, so both are known before a connection exists.
 */
public final class ConditionSchemas {

    private ConditionSchemas() {}

    /**
     * Fields whose value is a localised text — published as {@code {locale, text}} rather than a bare string.
     * Listing them keeps the schema honest about a shape a consumer would otherwise have to discover.
     * <p>
     * The state fields are localised texts too, but they carry an {@code Id} as well and are described by
     * {@link OpcuaConditionType#TWO_STATE_FIELDS} instead.
     * <p>
     * {@code Comment} is the one that matters most here. It is declared {@code LocalizedText} on
     * {@code ConditionType} (OPC 10000-9 §5.5.2 Table 8), so it rides in every event of all 22 types — and
     * while it was missing from this set, every condition carrying a non-null comment published an object
     * against a schema promising a string. A valid payload that violates its own advertised contract is
     * worse than a wrong type on a rare field, because a schema-validating consumer rejects the message that
     * matters most: the one an operator just commented on.
     */
    private static final @NotNull Set<String> LOCALIZED_TEXT_FIELDS = Set.of(
            "Message",
            // ConditionType (§5.5.2 Table 8)
            "Comment",
            "ConditionClassName",
            // DialogConditionType (§5.6.2 Table 32)
            "Prompt");

    /**
     * Fields whose value is an <em>array</em> of localised texts, published as a JSON array of
     * {@code {locale, text}} objects.
     * <p>
     * Cardinality is part of the type, and the specification's tables state it: {@code ConditionSubClassName}
     * is {@code LocalizedText[]} (§5.5.2 Table 8) and {@code ResponseOptionSet} is {@code LocalizedText[]}
     * (§5.6.2 Table 32). The converter renders a Java array as a JSON array, so describing either as a
     * single value — string or object — is wrong whatever the element type says.
     */
    private static final @NotNull Set<String> LOCALIZED_TEXT_ARRAY_FIELDS =
            Set.of("ConditionSubClassName", "ResponseOptionSet");

    /**
     * Fields whose value is an array of node ids, published as a JSON array of node-id structures.
     * <p>
     * Only {@code ConditionSubClassId}, declared {@code NodeId[]} on {@code ConditionType} (§5.5.2 Table 8).
     * It sat in {@link #NODE_ID_FIELDS} and so was described as a single node id — right about the element,
     * wrong about the shape.
     */
    private static final @NotNull Set<String> NODE_ID_ARRAY_FIELDS = Set.of("ConditionSubClassId");

    /**
     * Fields whose value is a {@code StatusCode}, published as {@code {code, symbol}}.
     * <p>
     * {@code Quality} alone, and it is easy to mistake for a localised text: it reads like one, and every
     * other state-ish field on a condition is one. OPC 10000-9 §5.5.2 Table 8 is explicit —
     * {@code HasComponent,Variable,Quality,StatusCode,ConditionVariableType,Mandatory} — and being Mandatory
     * on {@code ConditionType} it rides in every event of all 22 types, so describing it wrongly mistyped a
     * field in every schema this class produces.
     * <p>
     * {@code symbol} is optional: the converter emits it only when the numeric code resolves to a known name.
     */
    private static final @NotNull Set<String> STATUS_CODE_FIELDS = Set.of("Quality");

    /**
     * Fields whose value is a node id, published as a structure rather than a parseable string.
     * <p>
     * The {@code ...Node} suffix in the specification's names is the type signature, not decoration: these
     * properties hold a reference to the variable, never its value. {@code SetpointNode} says where the
     * setpoint lives; a field named {@code Setpoint} would promise the number itself, which is not what the
     * server sends.
     * <p>
     * The suffix is a good hint but not the rule — the type tables are. {@code BranchId},
     * {@code ConditionClassId} and {@code TrustListId} are all {@code NodeId} without carrying it, so the
     * list is maintained from the tables rather than by pattern-matching the names.
     */
    private static final @NotNull Set<String> NODE_ID_FIELDS = Set.of(
            // The condition this event is about. Not to be confused with ConditionClassId below, which
            // classifies the condition (process, system, maintenance) rather than identifying it.
            OpcuaConditionType.CONDITION_ID,
            "SourceNode",
            "EventType",
            "ConditionClassId",
            "BranchId",
            "InputNode",
            "NormalState",
            "SetpointNode",
            "BaseSetpointNode",
            "TargetValueNode",
            // CertificateExpirationAlarmType (§5.8.24 Table 112): the type of the certificate that is
            // expiring, as a NodeId. The name carries no `Node` suffix, which is why it was missed and
            // described as a string -- the suffix is a hint, the type tables are the rule.
            "CertificateType",
            // OPC 10000-12 §7.8.2.11: the NodeId of the TrustList that is out of date. Mandatory on
            // TrustListOutOfDateAlarmType, and the one node-id field defined outside Part 9 -- which is why
            // the Part 9 sweep behind finding 11 did not catch it.
            "TrustListId");

    private static final @NotNull Set<String> NUMERIC_FIELDS = Set.of(
            "MaxTimeShelved",
            // ShelvedStateMachineType's UnshelveTime (OPC 10000-9 §5.8.17 Table 73), a Duration -- so a
            // number of milliseconds, like MaxTimeShelved above it.
            "UnshelveTime",
            "OnDelay",
            "OffDelay",
            "ReAlarmTime",
            "HighHighLimit",
            "HighLimit",
            "LowLimit",
            "LowLowLimit",
            "BaseHighHighLimit",
            "BaseHighLimit",
            "BaseLowLimit",
            "BaseLowLowLimit",
            "HighHighDeadband",
            "HighDeadband",
            "LowDeadband",
            "LowLowDeadband",
            "ExpirationLimit",
            // DiscrepancyAlarmType (§5.8.23 Table 111): ExpectedTime is a Duration, Tolerance a Double.
            "ExpectedTime",
            "Tolerance",
            // TrustListOutOfDateAlarmType (OPC 10000-12 §7.8.2.11): a Duration.
            "UpdateFrequency");

    /**
     * Fields whose value is an integer, published as a JSON integer rather than a fractional number.
     * <p>
     * Two groups, both integral in the specification's own type tables and neither of them meaningfully
     * fractional:
     * <ul>
     *   <li>the four dialog response indices, {@code Int32} on {@code DialogConditionType} (§5.6.2 Table 32),
     *       which index into {@code ResponseOptionSet};</li>
     *   <li>the severities and the re-alarm count. {@code Severity} is a {@code UInt16} on
     *       {@code BaseEventType}, {@code LastSeverity} a {@code UInt16} on {@code ConditionType}, the four
     *       limit severities {@code UInt16} on {@code LimitAlarmType} (Table 92), and
     *       {@code ReAlarmRepeatCount} an {@code Int16} on {@code AlarmConditionType} (Table 40).</li>
     * </ul>
     * The second group used to sit in {@link #NUMERIC_FIELDS} beside the limits and delays, whose
     * {@code Double} values genuinely are fractional. Nothing broke — JSON Schema's {@code number} admits
     * integers, so the payloads validated — but a generated client took a {@code double} for a severity, and
     * the declared contract accepted 700.5 as a valid alarm priority and 2.5 as a repeat count.
     */
    private static final @NotNull Set<String> INTEGER_FIELDS = Set.of(
            "CancelResponse",
            "DefaultResponse",
            "LastResponse",
            "OkResponse",
            "Severity",
            "LastSeverity",
            "ReAlarmRepeatCount",
            "SeverityHighHigh",
            "SeverityHigh",
            "SeverityLow",
            "SeverityLowLow");

    /** The four dialog indices, which are the only integers with something extra to say about themselves. */
    private static final @NotNull Set<String> RESPONSE_INDEX_FIELDS =
            Set.of("CancelResponse", "DefaultResponse", "LastResponse", "OkResponse");

    /**
     * Fields carrying a point in time, declared as an instant rather than as unconstrained text.
     * <p>
     * {@code Time} and {@code ReceiveTime} are {@code UtcTime} on {@code BaseEventType},
     * {@code ExpirationDate} a {@code DateTime} on {@code CertificateExpirationAlarmType} (§5.8.24 Table 112),
     * and {@code LastUpdateTime} a {@code UtcTime} on {@code TrustListOutOfDateAlarmType} (OPC 10000-12
     * §7.8.2.11). The converter renders all four as RFC 3339 instants.
     * <p>
     * They reached the {@code STRING} fallback before, which is true as far as JSON goes and useless as a
     * contract: it says a consumer may receive any text at all where the adapter in fact promises a
     * timestamp, so a generated model gets a {@code String} field and every consumer parses it by hand.
     */
    private static final @NotNull Set<String> TIMESTAMP_FIELDS =
            Set.of("Time", "ReceiveTime", "ExpirationDate", "LastUpdateTime");

    private static final @NotNull Set<String> BOOLEAN_FIELDS =
            Set.of("Retain", "SupportsFilteredRetain", "AudibleEnabled", "SuppressedOrShelved", "FirstInGroupFlag");

    /**
     * Fields whose OPC UA type is {@code ByteString} — bytes, not text. They travel as base64, and declaring
     * them {@code BINARY} is what puts {@code contentEncoding: base64} in the schema so a consumer is told
     * that rather than left to infer it from the shape of the string.
     */
    private static final @NotNull Set<String> BYTE_STRING_FIELDS = Set.of(
            "EventId",
            // AlarmConditionType (§5.8.2 Table 40): AudioDataType, which is a ByteString subtype.
            "AudibleSound",
            // CertificateExpirationAlarmType (§5.8.24 Table 112): the DER-encoded certificate.
            "Certificate");

    /**
     * Fields whose value is an {@code EUInformation} structure — the engineering unit of a rate-of-change
     * alarm, published as {@code {namespaceUri, unitId, displayName, description}}.
     * <p>
     * Named rather than left to the generic structure path for the same reason as {@code TimeZoneDataType}:
     * Milo decodes it into its own generated class rather than a {@code DynamicStructType}, so without an
     * explicit converter branch it fell through to {@code toString()} — publishing a Java rendering whose
     * stability depends on Milo's implementation, and discarding the four members that make the unit
     * machine-readable in the first place.
     */
    private static final @NotNull Set<String> ENGINEERING_UNITS_FIELDS = Set.of("EngineeringUnits");

    /**
     * The northbound shape: every field the declared condition type carries.
     * <p>
     * Read-only throughout. A transition report is an observation — writing to it is not "acknowledging", which
     * is a method call described by {@link #writeSchema()} instead.
     * <p>
     * Every field is optional. The server fills what applies to a given transition and leaves the rest null, so
     * requiring any of them would describe a payload the device does not actually promise.
     */
    public static @NotNull Schema readSchema(final @NotNull EventFieldSet publishedFields) {
        final ObjectSchemaBuilder<SchemaBuilder> object = new SchemaBuilder().startObject();
        for (final String field : publishedFields.allFields()) {
            Shape.shapeOf(field).append(object, field);
        }
        appendUnavailableFields(object);
        return object.endObject().build();
    }

    /**
     * Declares the companion object naming fields the server declined to give a value for.
     * <p>
     * A server may substitute a {@code StatusCode} for any field's value — OPC 10000-4 §7.22.3 says it
     * <em>shall</em>, giving {@code Bad_UserAccessDenied} as the example — so such a field is published as
     * null, which is what it is. Every field here is nullable already, so that alone would validate; what
     * this adds is the reason, keyed by field name.
     * <p>
     * The distinction is worth the key. "Null because this transition does not carry that field" and "null
     * because this session may not read it" are different facts, and only the second is a configuration
     * problem someone can fix. Absent entirely when nothing was withheld, which is the ordinary case.
     * <p>
     * Left open rather than enumerating the type's fields as properties: the keys are exactly the fields of
     * the enclosing payload, and repeating fifty of them to describe a rare diagnostic would double the
     * schema for no gain a consumer can use.
     */
    private static void appendUnavailableFields(final @NotNull ObjectSchemaBuilder<SchemaBuilder> object) {
        object.property(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS)
                .startObject()
                .endObject()
                .description("Fields the server declined to give a value for, keyed by field name, with the "
                        + "OPC-UA status code saying why (for example 'Bad_UserAccessDenied'). Those fields "
                        + "are published as null. Absent when nothing was withheld.")
                .nullable()
                .readable()
                .writable(false)
                .endProperty();
    }

    /**
     * The southbound shape of a refresh tag: {@code {method}}, and nothing else.
     * <p>
     * {@code ConditionRefresh} takes no arguments a user could supply — its only parameter is the
     * subscription id, which is Edge's to know, not theirs. The field exists so the command names an action
     * rather than being an empty object, and so a second action (OPC 10000-9 §5.5.8's
     * {@code ConditionRefresh2}, narrowing to one monitored item) could be added without changing the shape.
     */
    public static @NotNull Schema refreshCommandSchema() {
        return new SchemaBuilder()
                .startObject()
                .property(RefreshCommand.FIELD_METHOD)
                .required()
                .scalar(ScalarType.STRING)
                .description("The action to request. Only '" + RefreshCommand.METHOD_REFRESH
                        + "' is defined: ask the server to re-report every condition it currently retains on "
                        + "this adapter's subscription.")
                .writable()
                .readable(false)
                .endProperty()
                .endObject()
                // As with the condition command: the object itself is written, not read.
                .writable()
                .readable(false)
                .build();
    }

    /**
     * The southbound shape for a tag that cannot be written: an object with no writable property.
     * <p>
     * Used by the event subscription tag, which is a query against a notifier — there is no node to write to,
     * and no state machine to transition. Saying so explicitly is better than returning no write schema at
     * all: an absent schema reads as "not determined yet" and invites a caller to try anyway, while this one
     * describes a shape that accepts nothing. The write path refuses such a tag regardless; this is the
     * declaration, not the enforcement.
     */
    public static @NotNull Schema unwritableSchema() {
        // No properties, and additionalProperties false: the object permits nothing at all. A schema with no
        // properties but additionalProperties left open would accept any object, which is the opposite claim.
        return new SchemaBuilder()
                .startObject()
                .additionalProperties(false)
                .endObject()
                .build();
    }

    /**
     * The southbound shape, identical for every condition:
     * {@code {method, eventId?, comment?, duration?, selectedResponse?}}.
     * <p>
     * Only {@code method} is required. Which of the others apply follows from it — {@code eventId} for the
     * methods acting on a single transition, {@code duration} for {@code TimedShelve},
     * {@code selectedResponse} for {@code Respond}, and none of them for the methods that act on the
     * condition as a whole. {@link ConditionUpdate.Method.Arguments} is the list. A static schema cannot
     * express that dependency, so the fields are optional here and the adapter checks them per method before
     * making the call.
     */
    public static @NotNull Schema writeSchema() {
        return new SchemaBuilder()
                .startObject()
                .property(ConditionUpdate.FIELD_METHOD)
                .required()
                .scalar(ScalarType.STRING)
                .description("Which condition method to invoke: " + describeMethods())
                .writable()
                .readable(false)
                .endProperty()
                .property(ConditionUpdate.FIELD_EVENT_ID)
                .scalar(ScalarType.BINARY)
                .description("The EventId from the northbound message being responded to, echoed back "
                        + "unchanged. Required for ACKNOWLEDGE, CONFIRM and ADD_COMMENT, which act on one "
                        + "specific transition.")
                .writable()
                .readable(false)
                .endProperty()
                .property(ConditionUpdate.FIELD_COMMENT)
                .scalar(ScalarType.STRING)
                .description("Free text recorded by the server alongside the transition.")
                .writable()
                .readable(false)
                .endProperty()
                .property(ConditionUpdate.FIELD_DURATION)
                .scalar(ScalarType.DOUBLE)
                .description("Shelving time in milliseconds. Required for TIMED_SHELVE, and meaningless for "
                        + "every other method.")
                .writable()
                .readable(false)
                .endProperty()
                .property(ConditionUpdate.FIELD_SELECTED_RESPONSE)
                .scalar(ScalarType.LONG)
                // Bounded because the type is not. The specification makes SelectedResponse an Int32 and the
                // SDK has no scalar narrower than LONG, so without these a schema-valid document could carry
                // a value the protocol cannot express -- and the parser would have to be the only thing
                // standing between it and a Call. It still is, since nothing guarantees a caller validated
                // against this schema, but a generated client should not be able to build the bad request in
                // the first place.
                .minimum(0)
                .maximum(Integer.MAX_VALUE)
                .description("Which of a dialog's offered responses to give, as a zero-based index into the "
                        + "ResponseOptionSet published on the event, between 0 and " + Integer.MAX_VALUE
                        + " (OPC UA Int32). Required for RESPOND, and meaningless for every other method.")
                .writable()
                .readable(false)
                .endProperty()
                .endObject()
                // The command object itself is written, not read. Without this the root renders as readOnly
                // while its properties render as writeOnly, which describes something nobody can send.
                .writable()
                .readable(false)
                .build();
    }

    /**
     * The published JSON shape of one event field, and how to write it.
     * <p>
     * This is the one authoritative answer to "what does this field look like on the wire". Both halves of
     * the contract read it: the constant writes the schema through its own {@link FieldAppender}, and
     * {@code ConditionSchemasFieldShapeTest} feeds a representative value of that shape through the converter
     * and checks the result validates. Splitting the question across a chain of {@code Set.contains} tests is
     * what let fifteen fields drift — each set was maintained on its own, and nothing anywhere enumerated the
     * fields that belonged to none of them and silently became strings.
     * <p>
     * The appenders live on the constants because this is where the answer is decided. A shape and the schema it produces are one fact, and holding them apart is what created the
     * gap in the first place: a constant could be added to one and forgotten in the other.
     */
    enum Shape {
        LOCAL_TIME(Shape::appendLocalTime),
        STATE_MACHINE(Shape::appendStateMachine),
        TWO_STATE(Shape::appendTwoState),
        LOCALIZED_TEXT(Shape::appendLocalizedText),
        LOCALIZED_TEXT_ARRAY(Shape::appendLocalizedTextArray),
        NODE_ID(Shape::appendNodeId),
        NODE_ID_ARRAY(Shape::appendNodeIdArray),
        ENGINEERING_UNITS(Shape::appendEngineeringUnits),
        STATUS_CODE(Shape::appendStatusCode),
        NUMBER(Shape::appendNumber),
        INTEGER(Shape::appendInteger),
        INSTANT(Shape::appendInstant),
        BOOLEAN(Shape::appendBoolean),
        BYTE_STRING(Shape::appendByteString),
        STRING(Shape::appendString);

        /**
         * What the northbound converter emits for a field, decided by the specification's type tables.
         * <p>
         * Not guessed per device: the converter's rendering is fixed, so a {@code LocalizedText} always arrives
         * as {@code {locale, text}} and a {@code NodeId} always as a structure. The order of the tests is not
         * arbitrary — the state fields are localised texts that carry an {@code Id}, so they have to be
         * recognised before the plain localised-text set, and the array sets before their scalar counterparts.
         */
        public static @NotNull Shape shapeOf(final @NotNull String field) {
            if ("LocalTime".equals(field)) {
                return LOCAL_TIME;
            }
            if (OpcuaConditionType.STATE_MACHINE_FIELDS.contains(field)) {
                return STATE_MACHINE;
            }
            if (OpcuaConditionType.TWO_STATE_FIELDS.contains(field)) {
                return TWO_STATE;
            }
            if (LOCALIZED_TEXT_FIELDS.contains(field)) {
                return LOCALIZED_TEXT;
            }
            if (LOCALIZED_TEXT_ARRAY_FIELDS.contains(field)) {
                return LOCALIZED_TEXT_ARRAY;
            }
            if (NODE_ID_ARRAY_FIELDS.contains(field)) {
                return NODE_ID_ARRAY;
            }
            if (ENGINEERING_UNITS_FIELDS.contains(field)) {
                return ENGINEERING_UNITS;
            }
            if (NODE_ID_FIELDS.contains(field)) {
                return NODE_ID;
            }
            if (STATUS_CODE_FIELDS.contains(field)) {
                return STATUS_CODE;
            }
            if (TIMESTAMP_FIELDS.contains(field)) {
                return INSTANT;
            }
            if (NUMERIC_FIELDS.contains(field)) {
                return NUMBER;
            }
            if (INTEGER_FIELDS.contains(field)) {
                return INTEGER;
            }
            if (BOOLEAN_FIELDS.contains(field)) {
                return BOOLEAN;
            }
            if (BYTE_STRING_FIELDS.contains(field)) {
                return BYTE_STRING;
            }
            return STRING;
        }

        /**
         * How a field of this shape is written into the schema.
         * <p>
         * A field rather than a switch, and that is the whole of finding 2. This began as an
         * {@code if}/{@code else} chain ending in an {@code else} that caught everything left over and
         * declared it a string, so a shape nobody had handled did not go missing — it silently acquired the
         * wrong type, and fifteen fields did.
         * <p>
         * A {@code switch} would only have moved that risk. A statement over an enum is not required to be
         * exhaustive, and a missing case is an ErrorProne warning that leaves the build green — measured by
         * adding a constant and building, not assumed. Holding the appender here removes the question instead
         * of guarding it: there is no arm to forget because there are no arms, and a constant cannot be
         * declared without saying how it is written.
         */
        private final @NotNull FieldAppender appender;

        Shape(final @NotNull FieldAppender appender) {
            this.appender = appender;
        }

        /** Writes one field of this shape into the object being built. */
        void append(final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            appender.append(object, field);
        }

        /**
         * OPC 10000-5: the offset in minutes between the event's {@code Time} and the time where it was issued,
         * and whether that offset already includes the daylight-saving correction. Optional on
         * {@code BaseEventType}, so a server that does not supply it publishes null like any other absent field.
         */
        private static void appendLocalTime(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startObject()
                    .property("offset")
                    .scalar(ScalarType.LONG)
                    .description("Minutes between the event's 'Time' (UTC) and the local time where the "
                            + "event was issued.")
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("daylightSavingInOffset")
                    .scalar(ScalarType.BOOLEAN)
                    .description("True when 'offset' already includes the daylight-saving correction. False "
                            + "means it does not, and DST may or may not have been in effect.")
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /**
         * A state machine is an Object with no value of its own, so what is published is its {@code CurrentState}
         * — the display text — with the NodeId of the active state node as {@code id}. The same shape as a
         * two-state field, but {@code id} is a node id rather than a Boolean, because a machine has more than two
         * states to distinguish.
         */
        private static void appendStateMachine(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startObject()
                    .property("locale")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("text")
                    .scalar(ScalarType.STRING)
                    .description("The current state's name in the session's locale — for a limit alarm, "
                            + "which limit is violated. Wording varies by locale and by vendor, so prefer "
                            + "'id' when deciding anything.")
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("id")
                    .startObject()
                    .property("idType")
                    .scalar(ScalarType.LONG)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("id")
                    .any()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("namespaceIndex")
                    .scalar(ScalarType.LONG)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .description("The node id of the active state — the machine-readable half of this "
                            + "field, stable across locales and vendors.")
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /**
         * A two-state field carries its Boolean {@code Id} alongside the display text. The text is what the server
         * calls the state in the session's locale; {@code id} is the same state as a Boolean, and is what a
         * consumer should branch on — {@code "Active"}/{@code "Aktiv"}/{@code "ACTIVE"} are all the same true.
         */
        private static void appendTwoState(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startObject()
                    .property("locale")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("text")
                    .scalar(ScalarType.STRING)
                    .description("The state's name in the session's locale. Wording varies by locale and by "
                            + "vendor, so prefer 'id' when deciding anything.")
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("id")
                    .scalar(ScalarType.BOOLEAN)
                    .description("The state as a Boolean — the machine-readable half of this field. Absent "
                            + "if the server did not return it, though it is mandatory on the type.")
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /** A localised text, published as {@code {locale, text}} rather than a bare string. */
        private static void appendLocalizedText(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startObject()
                    .property("locale")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("text")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /** An array of localised texts. Cardinality is part of the type, so the array is part of the shape. */
        private static void appendLocalizedTextArray(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startArray()
                    .startObject()
                    .property("locale")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("text")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .endArray()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /** A node id, published as a structure rather than a parseable string. */
        private static void appendNodeId(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startObject()
                    .property("idType")
                    .scalar(ScalarType.LONG)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("id")
                    .any()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("namespaceIndex")
                    .scalar(ScalarType.LONG)
                    .description("The namespace index, meaningful only against the server that sent it: the "
                            + "index-to-URI table belongs to the server and may be renumbered between "
                            + "sessions.")
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /** An array of node ids — only {@code ConditionSubClassId}, declared {@code NodeId[]} on ConditionType. */
        private static void appendNodeIdArray(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startArray()
                    .startObject()
                    .property("idType")
                    .scalar(ScalarType.LONG)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("id")
                    .any()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("namespaceIndex")
                    .scalar(ScalarType.LONG)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .endArray()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /**
         * An {@code EUInformation}: the authority defining the unit, the unit's id within it, and the symbol and
         * full name as localised texts. The pair {@code namespaceUri}/{@code unitId} is what makes an engineering
         * unit machine-readable, which stringifying the structure destroyed.
         */
        private static void appendEngineeringUnits(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startObject()
                    .property("namespaceUri")
                    .scalar(ScalarType.STRING)
                    .description("The URI of the authority defining the unit — by default the UNECE "
                            + "Recommendation 20 code list.")
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("unitId")
                    .scalar(ScalarType.LONG)
                    .description("The unit's identifier within that authority's list.")
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("displayName")
                    .startObject()
                    .property("locale")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("text")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .description("The unit's symbol, for example \"°C\".")
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("description")
                    .startObject()
                    .property("locale")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("text")
                    .scalar(ScalarType.STRING)
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .description("The unit's full name, for example \"degree Celsius\".")
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /** A {@code StatusCode}, published as {@code {code, symbol}}. Only {@code Quality} is declared one. */
        private static void appendStatusCode(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .startObject()
                    .property("code")
                    .scalar(ScalarType.LONG)
                    .description("The numeric status code.")
                    .readable()
                    .writable(false)
                    .endProperty()
                    .property("symbol")
                    .scalar(ScalarType.STRING)
                    .description("The status code's symbolic name, absent when the code is not a known one.")
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /** A number that may be fractional: the limits, the deadbands, the durations and the severities. */
        private static void appendNumber(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .scalar(ScalarType.DOUBLE)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /**
         * An integer rather than a number, for the fields the specification declares integral. The
         * description is only added for the dialog indices, which are the ones whose meaning the name does
         * not carry; a severity needs no gloss.
         */
        private static void appendInteger(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            final var property = object.property(field).scalar(ScalarType.LONG);
            if (RESPONSE_INDEX_FIELDS.contains(field)) {
                property.description("An index into 'ResponseOptionSet'.");
            }
            property.nullable().readable().writable(false).endProperty();
        }

        /**
         * A point in time, declared as such rather than as unconstrained text.
         * <p>
         * The converter renders these as RFC 3339, so the payload is unchanged — what changes is that the
         * schema now says so, and a generated model gets a timestamp type instead of a string a consumer
         * has to know to parse.
         */
        private static void appendInstant(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .scalar(ScalarType.INSTANT)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /** A boolean, with {@link ConditionSchemas#describeBoolean} supplying the meaning the name lacks. */
        private static void appendBoolean(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .scalar(ScalarType.BOOLEAN)
                    .description(describeBoolean(field))
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /**
         * Bytes, not text. Declaring {@code BINARY} is what puts {@code contentEncoding: base64} in the schema, so
         * a consumer is told that rather than left to infer it from the shape of the string.
         */
        private static void appendByteString(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .scalar(ScalarType.BINARY)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }

        /**
         * A genuine string: {@code SourceName}, {@code ClientUserId}, {@code ConditionName}, and the timestamps.
         * <p>
         * <b>An assertion, not a fallback</b>, and the difference is the whole of finding 2. This used to be the
         * {@code else} of a chain, described in its own comment as "left open rather than mistyped" while
         * declaring STRING — so a field nobody had classified was not left open at all, it was asserted to be a
         * string. Fifteen arrived here that way, {@code Comment} among them, on {@code ConditionType} and so in
         * every event of all 22 types. It is now the appender of {@link Shape#STRING} and nothing else can
         * reach it, and {@code ConditionSchemasFieldShapeTest} fails if any field so classified is not a
         * string on the wire.
         */
        private static void appendString(
                final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {
            object.property(field)
                    .scalar(ScalarType.STRING)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }
    }

    /** Writes one field's schema. One implementation per {@link Shape}, held by the constant itself. */
    @FunctionalInterface
    interface FieldAppender {

        void append(@NotNull ObjectSchemaBuilder<SchemaBuilder> object, @NotNull String field);
    }

    /**
     * What a boolean field means, where the name alone does not say.
     * <p>
     * {@code Retain} is the one that matters. It is the specification's terminal signal for a branch — OPC
     * 10000-9 §5.5.2: "when a Client receives an Event with the Retain flag set to False, the Client should
     * consider this as a ConditionBranch that is no longer of interest" — and Edge deliberately does not act
     * on it. That follows from the conduit decision: Edge relays transitions and keeps no alarm list, so
     * there is nothing here to retire. Retiring the entry is the consumer's job, and a field presented as a
     * bare nullable boolean beside {@code AudibleEnabled} gives them no way to know that.
     */
    private static @NotNull String describeBoolean(final @NotNull String field) {
        return switch (field) {
            case "Retain" ->
                "False means this branch is no longer of interest and a consumer maintaining an alarm list "
                        + "should retire it (OPC 10000-9 §5.5.2). Edge does not act on it: it relays "
                        + "transitions and keeps no list of its own.";
            case "SupportsFilteredRetain" ->
                "Whether the server supports per-client Retain filtering. It changes what Retain means in "
                        + "every event from this condition, so read it before relying on Retain.";
            case "SuppressedOrShelved" ->
                "True while the alarm is suppressed or shelved, and so not currently of operator interest.";
            case "AudibleEnabled" -> "Whether the server has an audible sound configured for this alarm.";
            case "FirstInGroupFlag" -> "True when this alarm was the first in its alarm group to activate.";
            default -> "";
        };
    }

    private static @NotNull String describeMethods() {
        final StringBuilder methods = new StringBuilder();
        for (final ConditionUpdate.Method method : ConditionUpdate.Method.values()) {
            if (!methods.isEmpty()) {
                methods.append(", ");
            }
            methods.append(method.name());
        }
        return methods.toString();
    }
}
