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
package com.hivemq.protocols.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// The fields a method request carries *beside* `value`, and who decides them.
///
/// An adapter describes the device, so it describes what goes *inside* `value` -- a method's arguments. What a
/// request carries outside `value` is Edge's own request/response convention, configured by three flags on the
/// southbound mapping and offered for every protocol. So these fields are declared here, from the flags, and not
/// by any adapter.
///
/// Each flag says the mapping **will read** that field, which makes it required rather than merely allowed: that
/// is what turns a missing field into a schema failure, caught before anything reaches the device and reported
/// with the JSON path. A field whose flag is off is not declared at all, because the southbound transform is a
/// whitelist that drops it -- a schema accepting it would describe a field that can never arrive.
class SouthboundRequestFieldsTest {

    private static @NotNull Schema value() {
        return new SchemaBuilder()
                .startObject()
                .property("setpoint")
                .scalar(ScalarType.LONG)
                .writable()
                .endObject()
                .writable()
                .build();
    }

    private static @NotNull JsonNode southbound(
            final boolean methodCalls, final boolean correlationData, final boolean responseTopic) throws Exception {
        final var output = new TagSchemaCreationOutputImpl(methodCalls, correlationData, responseTopic);
        output.finish(new TagSchemaCreationOutput.DataPointSchema(value(), null, null));
        return output.getSchema(TagSchemaDirection.SOUTHBOUND);
    }

    @Test
    void withNoMapping_theDocumentIsValueOnly() throws Exception {
        // The no-arg constructor: discovery, browsing, the REST tag-schema endpoint. Also every southbound schema
        // that existed before method calls, so the addition has to be invisible to them.
        final var output = new TagSchemaCreationOutputImpl();
        output.finish(new TagSchemaCreationOutput.DataPointSchema(value(), null, null));
        final JsonNode schema = output.getSchema(TagSchemaDirection.SOUTHBOUND);

        assertThat(schema.get("properties").properties()).hasSize(1);
        assertThat(schema.get("properties").has("value")).isTrue();
        assertThat(schema.get("required").toString()).isEqualTo("[\"value\"]");
    }

    @Test
    void withTheFlagsOff_theDocumentIsValueOnly() throws Exception {
        // A mapping that is not a method mapping must produce the same document as no mapping at all: an ordinary
        // value write is unchanged by method calls existing.
        final JsonNode schema = southbound(false, false, false);

        assertThat(schema.get("properties").properties()).hasSize(1);
        assertThat(schema.get("properties").has("value")).isTrue();
    }

    @Test
    void methodCallsAlone_declaresAndRequiresMethodName() throws Exception {
        // methodName has no flag of its own: a request naming no method cannot be dispatched under any
        // configuration, so a switch for it would only offer a broken setup.
        final JsonNode schema = southbound(true, false, false);

        assertThat(schema.get("properties").has("methodName")).isTrue();
        assertThat(schema.get("required").toString()).contains("methodName");
    }

    @Test
    void methodCallsAlone_declaresNeitherOptionalField() throws Exception {
        // Not declared, rather than declared-and-optional. The transform drops a field whose flag is off, so a
        // caller sending it would pass validation and then be silently ignored.
        final JsonNode schema = southbound(true, false, false);

        assertThat(schema.get("properties").has("correlationData")).isFalse();
        assertThat(schema.get("properties").has("responseTopic")).isFalse();
    }

    @Test
    void theCorrelationFlag_requiresCorrelationDataAndLeavesTheOtherOut() throws Exception {
        // The flags are independent: naming one must not imply another.
        final JsonNode schema = southbound(true, true, false);

        assertThat(schema.get("required").toString()).contains("correlationData");
        assertThat(schema.get("properties").has("responseTopic")).isFalse();
    }

    @Test
    void theResponseTopicFlag_requiresResponseTopicAndLeavesTheOtherOut() throws Exception {
        final JsonNode schema = southbound(true, false, true);

        assertThat(schema.get("required").toString()).contains("responseTopic");
        assertThat(schema.get("properties").has("correlationData")).isFalse();
    }

    @Test
    void allThreeFlags_requireAllThreeFields() throws Exception {
        final JsonNode schema = southbound(true, true, true);

        assertThat(schema.get("required").toString())
                .contains("methodName")
                .contains("correlationData")
                .contains("responseTopic");
    }

    @Test
    void theRequestFields_renderAsSiblingsOfValue() throws Exception {
        // Siblings, not nested. The whole MQTT payload is read into one record whose methodName is a top-level
        // component, and an earlier version of this declared them a level too deep -- producing a schema that
        // required methodName inside value while every writer put it outside, so every request failed validation.
        final JsonNode schema = southbound(true, true, true);

        final JsonNode properties = schema.get("properties");
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("methodName")).isTrue();
        assertThat(properties.get("value").get("properties").has("methodName")).isFalse();
    }

    @Test
    void valueStaysRequired_whateverTheFlagsSay() throws Exception {
        final JsonNode schema = southbound(true, true, true);

        assertThat(schema.get("required").toString()).contains("value");
    }

    @Test
    void theRequestFields_areWritable() throws Exception {
        // A request's own fields are written by the caller, so they must not render readOnly. The schema builder
        // defaults every node to not-writable, which is why each one chains .writable() explicitly.
        final JsonNode schema = southbound(true, true, true);

        final JsonNode properties = schema.get("properties");
        assertThat(properties.get("methodName").path("readOnly").asBoolean(false))
                .isFalse();
        assertThat(properties.get("correlationData").path("readOnly").asBoolean(false))
                .isFalse();
        assertThat(properties.get("responseTopic").path("readOnly").asBoolean(false))
                .isFalse();
    }
}
