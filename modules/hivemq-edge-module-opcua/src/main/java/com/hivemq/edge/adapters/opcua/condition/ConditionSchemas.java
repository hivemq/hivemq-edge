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
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
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
     */
    private static final @NotNull Set<String> LOCALIZED_TEXT_FIELDS = Set.of(
            "Message",
            "AckedState",
            "ConfirmedState",
            "ActiveState",
            "EnabledState",
            "SuppressedState",
            "SilenceState",
            "LatchedState",
            "OutOfServiceState",
            "ShelvingState",
            "Quality",
            "LimitState");

    /** Fields whose value is a node id, published as a structure rather than a parseable string. */
    private static final @NotNull Set<String> NODE_ID_FIELDS = Set.of(
            "SourceNode",
            "EventType",
            "ConditionClassId",
            "ConditionSubClassId",
            "BranchId",
            "InputNode",
            "NormalState",
            "ExpectedState");

    private static final @NotNull Set<String> NUMERIC_FIELDS = Set.of(
            "Severity",
            "LastSeverity",
            "MaxTimeShelved",
            "OnDelay",
            "OffDelay",
            "ReAlarmTime",
            "ReAlarmRepeatCount",
            "HighHighLimit",
            "HighLimit",
            "LowLimit",
            "LowLowLimit",
            "BaseHighHighLimit",
            "BaseHighLimit",
            "BaseLowLimit",
            "BaseLowLowLimit",
            "SeverityHighHigh",
            "SeverityHigh",
            "SeverityLow",
            "SeverityLowLow",
            "HighHighDeadband",
            "HighDeadband",
            "LowDeadband",
            "LowLowDeadband",
            "ExpirationLimit");

    private static final @NotNull Set<String> BOOLEAN_FIELDS =
            Set.of("Retain", "SupportsFilteredRetain", "AudibleEnabled", "SuppressedOrShelved", "FirstInGroupFlag");

    /**
     * Fields whose OPC UA type is {@code ByteString} — bytes, not text. They travel as base64, and declaring
     * them {@code BINARY} is what puts {@code contentEncoding: base64} in the schema so a consumer is told
     * that rather than left to infer it from the shape of the string.
     */
    private static final @NotNull Set<String> BYTE_STRING_FIELDS = Set.of("EventId");

    /**
     * The northbound shape: every field the declared condition type carries.
     * <p>
     * Read-only throughout. A transition report is an observation — writing to it is not "acknowledging", which
     * is a method call described by {@link #writeSchema()} instead.
     * <p>
     * Every field is optional. The server fills what applies to a given transition and leaves the rest null, so
     * requiring any of them would describe a payload the device does not actually promise.
     */
    public static @NotNull Schema readSchema(final @NotNull OpcuaConditionType conditionType) {
        final ObjectSchemaBuilder<SchemaBuilder> object = new SchemaBuilder().startObject();
        for (final String field : conditionType.allFields()) {
            appendField(object, field);
        }
        return object.endObject().build();
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
     * The southbound shape, identical for every condition: {@code {method, eventId?, comment?, duration?}}.
     * <p>
     * Only {@code method} is required. Which of the others apply follows from it — {@code eventId} for the
     * three methods acting on a single transition, {@code duration} for {@code TimedShelve}, and neither for
     * the ten that act on the condition as a whole. A static schema cannot express that dependency, so the
     * fields are optional here and the adapter checks them per method before making the call.
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
                .endObject()
                // The command object itself is written, not read. Without this the root renders as readOnly
                // while its properties render as writeOnly, which describes something nobody can send.
                .writable()
                .readable(false)
                .build();
    }

    /**
     * Adds one event field, typed by what the northbound converter actually emits for it.
     * <p>
     * The types are not guessed per device: the converter's rendering is fixed, so a {@code LocalizedText}
     * always arrives as {@code {locale, text}} and a {@code NodeId} always as a structure. Anything not
     * recognised is left open rather than asserted to be a string.
     */
    private static void appendField(
            final @NotNull ObjectSchemaBuilder<SchemaBuilder> object, final @NotNull String field) {

        if (LOCALIZED_TEXT_FIELDS.contains(field)) {
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
        } else if (NODE_ID_FIELDS.contains(field)) {
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
                    .any()
                    .readable()
                    .writable(false)
                    .endProperty()
                    .endObject()
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        } else if (NUMERIC_FIELDS.contains(field)) {
            object.property(field)
                    .scalar(ScalarType.DOUBLE)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        } else if (BOOLEAN_FIELDS.contains(field)) {
            object.property(field)
                    .scalar(ScalarType.BOOLEAN)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        } else if (BYTE_STRING_FIELDS.contains(field)) {
            object.property(field)
                    .scalar(ScalarType.BINARY)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        } else {
            // SourceName, Time, ReceiveTime, ClientUserId, ConditionName and the rest render as strings;
            // anything unrecognised is left open rather than mistyped.
            object.property(field)
                    .scalar(ScalarType.STRING)
                    .nullable()
                    .readable()
                    .writable(false)
                    .endProperty();
        }
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
