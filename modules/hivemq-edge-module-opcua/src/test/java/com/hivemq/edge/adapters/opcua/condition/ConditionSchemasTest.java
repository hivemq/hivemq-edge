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
