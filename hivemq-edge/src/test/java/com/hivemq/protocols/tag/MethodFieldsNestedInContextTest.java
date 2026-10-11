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
/// Both documents declare those fields **nested under `context`**, which is the shape that crosses the adapter
/// boundary in either direction: Edge assembles the object before a request reaches the data policy, the
/// generated transform puts it in the payload beside `value`, and the adapter returns one of the same shape.
///
/// They used to be lifted to the document root instead, because that was where they sat on the wire. The
/// southbound side no longer works that way -- a caller's own fields are described by the schema attached to
/// the topic filter, and Edge gathers them into a context on the way past -- so the document now describes
/// what it is actually applied to, which is the message after the transform.
///
/// **A published response is still flat**, because `NorthboundTagConsumer` lifts the fields onto it. Nothing
/// validates a response against the northbound document, whose only reader is the REST endpoint serving it to
/// the mapping editor and the data combiner, so that mismatch is a usability question about those two rather
/// than a correctness one.
class MethodFieldsNestedInContextTest {

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
    void northbound_nestsTheResponseFieldsUnderContext() throws Exception {
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                        TagSchemaDirection.NORTHBOUND)
                .get("properties");

        // Nested, not at the root: this is the shape the adapter returns, and one shape now describes both
        // directions across that boundary.
        assertThat(properties.has("context"))
                .withFailMessage("the response fields were not nested under `context`: %s", properties)
                .isTrue();
        assertThat(properties.has("methodName")).isFalse();

        final JsonNode context = properties.get("context").get("properties");
        assertThat(context.has("methodName")).isTrue();
        assertThat(context.has("correlationData")).isTrue();
        assertThat(context.has("errorCode")).isTrue();
        assertThat(context.has("statusInfo")).isTrue();
    }

    @Test
    void northbound_doesNotPublishTheResponseTopic() throws Exception {
        // It says where an answer goes rather than anything about the answer, and the publisher consumes it to
        // choose the topic instead of putting it on the message.
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                        TagSchemaDirection.NORTHBOUND)
                .get("properties");

        // Asserted inside the context, where the other response fields are: at the root it would hold
        // vacuously now that nothing is lifted there.
        assertThat(properties.get("context").get("properties").has("responseTopic"))
                .isFalse();
    }

    @Test
    void southbound_nestsTheRequestFieldsUnderContext() throws Exception {
        final JsonNode document = document(
                new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                TagSchemaDirection.SOUTHBOUND);
        final JsonNode properties = document.get("properties");

        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("context")).isTrue();
        assertThat(properties.has("methodName")).isFalse();

        final JsonNode context = properties.get("context");
        assertThat(context.get("properties").has("methodName")).isTrue();
        assertThat(context.get("properties").has("correlationData")).isTrue();
        assertThat(context.get("properties").has("responseTopic")).isTrue();

        // The method name is required inside the context, the others are not: Edge assembles this object and
        // refuses a request it cannot name a method for, so a context without one does not exist -- while a
        // reply address is request-only and a caller may send no token at all.
        assertThat(context.get("required")).isNotNull();
        assertThat(context.get("required").toString()).isEqualTo("[\"methodName\"]");
    }

    @Test
    void southbound_omitsTheFailureFields() throws Exception {
        // A caller does not send a failure.
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(value(), null, methodFields()),
                        TagSchemaDirection.SOUTHBOUND)
                .get("properties");

        // Inside the context, for the same reason as the response topic above: an assertion at the root would
        // hold whatever the context contained.
        final JsonNode context = properties.get("context").get("properties");
        assertThat(context.has("errorCode")).isFalse();
        assertThat(context.has("statusInfo")).isFalse();
    }

    @Test
    void southbound_requiresOnlyValue() throws Exception {
        // Every lifted field is optional. A request omitting one it promised is refused by the adapter, with a
        // message naming the omission, rather than by schema validation.
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
