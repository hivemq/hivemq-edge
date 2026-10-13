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

/// How the fields a method tag carries beside `value` reach a schema document.
///
/// An adapter names them in the `context` part of what it produces, and a part naming `methodName` is what makes
/// a tag a method tag. Nothing else can tell: the tag's kind lives in the adapter's own module and the shared
/// layer cannot name it, and the mapping settings that decide which fields a request carries are not reachable
/// where the document is assembled. The adapter knows, and says so in the data it hands over.
///
/// Both documents declare those fields **at the root, beside `value`** -- because that is where both messages
/// carry them. A caller publishes `methodName` and the two optional fields as siblings of `value`, and a
/// published response carries its fields the same way.
///
/// **The `context` object does not appear in either document, and that is the point.** It is how Edge hands a
/// request to an adapter: merged into the payload after the policy has run, after every schema has had its say.
/// An earlier version described `context` here instead, which described neither message -- a caller reading the
/// southbound document would have nested fields that Edge does not read there, and the mapping editor offered
/// `context.errorCode` where a response has `errorCode`.
class MethodFieldsInTheSchemasTest {

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

    /// The `context` part an OPC-UA method tag supplies: every field optional, `methodName` among them.
    private static @NotNull Schema methodFields() {
        return new SchemaBuilder()
                .startObject()
                .property("methodName")
                .scalar(ScalarType.STRING)
                .property("correlationData")
                .scalar(ScalarType.STRING)
                .property("responseTopic")
                .scalar(ScalarType.STRING)
                .property("errorCode")
                .scalar(ScalarType.STRING)
                .property("statusInfo")
                .scalar(ScalarType.STRING)
                .endObject()
                .build();
    }

    /// A `context` part from a tag that is not a method tag -- no `methodName`.
    private static @NotNull Schema ordinaryContext() {
        return new SchemaBuilder()
                .startObject()
                .property("units")
                .scalar(ScalarType.STRING)
                .endObject()
                .build();
    }

    private static @NotNull JsonNode document(
            final @NotNull TagSchemaCreationOutput.DataPointSchema parts, final @NotNull TagSchemaDirection direction)
            throws Exception {
        final var output = new TagSchemaCreationOutputImpl();
        output.finish(parts);
        return output.getSchema(direction);
    }

    @Test
    void northbound_declaresTheResponseFieldsAtTheRoot() throws Exception {
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                        TagSchemaDirection.NORTHBOUND)
                .get("properties");

        assertThat(properties.has("methodName"))
                .withFailMessage(
                        "the response fields must sit at the root, as the message carries them: %s", properties)
                .isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        assertThat(properties.has("errorCode")).isTrue();
        assertThat(properties.has("statusInfo")).isTrue();

        // Edge's hand-off to the adapter, not part of any message. A document naming it would describe neither.
        assertThat(properties.has("context"))
                .withFailMessage("`context` must not appear in a document describing a message: %s", properties)
                .isFalse();
    }

    @Test
    void northbound_doesNotPublishTheResponseTopic() throws Exception {
        // It says where an answer goes rather than anything about the answer, and the publisher consumes it to
        // choose the topic instead of putting it on the message.
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                        TagSchemaDirection.NORTHBOUND)
                .get("properties");

        assertThat(properties.has("responseTopic")).isFalse();
    }

    @Test
    void southbound_declaresTheRequestFieldsAtTheRoot() throws Exception {
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                        TagSchemaDirection.SOUTHBOUND)
                .get("properties");

        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("methodName")).isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        assertThat(properties.has("responseTopic")).isTrue();

        assertThat(properties.has("context"))
                .withFailMessage(
                        "`context` is merged after this schema runs, so it must not be declared: %s", properties)
                .isFalse();
    }

    @Test
    void southbound_omitsTheFailureFields() throws Exception {
        // A caller does not send a failure.
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                        TagSchemaDirection.SOUTHBOUND)
                .get("properties");

        assertThat(properties.has("errorCode")).isFalse();
        assertThat(properties.has("statusInfo")).isFalse();
    }

    @Test
    void southbound_requiresOnlyValue() throws Exception {
        // Every method field is optional. A request omitting one its mapping promised is refused before the
        // device is touched -- by Edge, which assembles the context -- rather than by this schema.
        final JsonNode schema = document(
                new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                TagSchemaDirection.SOUTHBOUND);

        assertThat(schema.get("required").toString()).isEqualTo("[\"value\"]");
    }

    @Test
    void aContextPartWithoutMethodName_staysNestedAsBefore() throws Exception {
        // Not a method tag. The northbound document keeps rendering a `context` property, which is what every
        // other adapter's context part has always produced.
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, ordinaryContext()),
                        TagSchemaDirection.NORTHBOUND)
                .get("properties");

        assertThat(properties.has("context")).isTrue();
        assertThat(properties.has("units")).isFalse();
    }

    @Test
    void aContextPartWithoutMethodName_leavesTheWriteDocumentAlone() throws Exception {
        final JsonNode schema = document(
                new TagSchemaCreationOutput.DataPointSchema(value(), null, ordinaryContext()),
                TagSchemaDirection.SOUTHBOUND);

        assertThat(schema.get("properties").properties()).hasSize(1);
        assertThat(schema.get("properties").has("value")).isTrue();
    }

    @Test
    void noContextPart_producesTheDocumentsItAlwaysDid() throws Exception {
        // Every tag of every other adapter: a bare value write, and a read envelope without `context`.
        final var parts = new TagSchemaCreationOutput.DataPointSchema(value(), null, null);

        final JsonNode write = document(parts, TagSchemaDirection.SOUTHBOUND);
        assertThat(write.get("properties").properties()).hasSize(1);

        final JsonNode read = document(parts, TagSchemaDirection.NORTHBOUND);
        assertThat(read.get("properties").has("context")).isFalse();
        assertThat(read.get("properties").has("methodName")).isFalse();
    }
}
