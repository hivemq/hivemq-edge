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

/// How fields an adapter declares *beside* `value` reach the southbound document.
///
/// The northbound view has always rendered siblings of `value` -- `metadata` and `context` sit beside it there.
/// Southbound had only `value`, which left an adapter unable to describe a command's own fields: an OPC-UA method
/// request names its method outside `value`, because the device names what is inside and Edge names what is
/// outside. These pin that an envelope schema renders as siblings, and that its `required` set survives -- which is
/// what lets a mapping make a field mandatory and have a request omitting it refused by schema validation.
class SouthboundEnvelopeSchemaTest {

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

    private static @NotNull Schema envelope() {
        return new SchemaBuilder()
                .startObject()
                .property("methodName")
                .required()
                .scalar(ScalarType.STRING)
                .writable()
                .property("correlationData")
                .scalar(ScalarType.STRING)
                .writable()
                .endObject()
                .writable()
                .build();
    }

    private static @NotNull JsonNode southbound(final @NotNull TagSchemaCreationOutput.DataPointSchema dps)
            throws Exception {
        final var output = new TagSchemaCreationOutputImpl();
        output.finish(dps);
        return output.getSchema(TagSchemaDirection.SOUTHBOUND);
    }

    @Test
    void withNoEnvelope_theDocumentIsValueOnly() throws Exception {
        // Every southbound schema that exists today. The addition has to be invisible to them.
        final JsonNode schema = southbound(new TagSchemaCreationOutput.DataPointSchema(value(), null, null));

        assertThat(schema.get("properties").properties()).hasSize(1);
        assertThat(schema.get("properties").has("value")).isTrue();
        assertThat(schema.get("required").toString()).isEqualTo("[\"value\"]");
    }

    @Test
    void envelopeProperties_renderAsSiblingsOfValue() throws Exception {
        final JsonNode schema =
                southbound(new TagSchemaCreationOutput.DataPointSchema(value(), null, null, value(), envelope()));

        final JsonNode properties = schema.get("properties");
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("methodName")).isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        // Siblings, not nested: a method request carries methodName beside value on the wire, and the schema has to
        // say the same or it describes a shape nothing produces.
        assertThat(properties.get("value").get("properties").has("methodName")).isFalse();
    }

    @Test
    void aRequiredEnvelopeField_isRequiredInTheDocument() throws Exception {
        // The decisive one. This is what turns a missing field into a schema failure -- caught before anything
        // reaches the device, with the JSON path named -- rather than a fault discovered deeper in or not at all.
        final JsonNode schema =
                southbound(new TagSchemaCreationOutput.DataPointSchema(value(), null, null, value(), envelope()));

        assertThat(schema.get("required").toString()).contains("methodName");
    }

    @Test
    void anOptionalEnvelopeField_isNotRequired() throws Exception {
        final JsonNode schema =
                southbound(new TagSchemaCreationOutput.DataPointSchema(value(), null, null, value(), envelope()));

        assertThat(schema.get("required").toString()).doesNotContain("correlationData");
        assertThat(schema.get("properties").has("correlationData")).isTrue();
    }

    @Test
    void valueStaysRequired_whateverTheEnvelopeSays() throws Exception {
        final JsonNode schema =
                southbound(new TagSchemaCreationOutput.DataPointSchema(value(), null, null, value(), envelope()));

        assertThat(schema.get("required").toString()).contains("value");
    }
}
