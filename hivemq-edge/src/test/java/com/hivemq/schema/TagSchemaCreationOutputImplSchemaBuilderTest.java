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
package com.hivemq.schema;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.schema.SchemaBuilder;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.protocols.tag.TagSchemaCreationOutputImpl;
import com.hivemq.protocols.tag.TagSchemaDirection;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// The class-level timeout keeps a schema that never completes from stalling a test for the future's
// internal 30s orTimeout: every test here completes the future synchronously, so a hang is a bug.
@Timeout(5)
class TagSchemaCreationOutputImplSchemaBuilderTest {

    @Test
    void test_tagSchemaBuilder_build_completesTheFuture() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.LONG).title("RPM").build(), null, null));

        final JsonNode result = output.getSchema(TagSchemaDirection.NORTHBOUND);
        assertThat(result).isNotNull();
        assertThat(result.get("properties").get("value").get("type").asText()).isEqualTo("integer");
        assertThat(result.get("properties").get("value").get("title").asText()).isEqualTo("RPM");
    }

    @Test
    void test_tagSchemaBuilder_object_completesWithJsonSchema() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder()
                        .startObject()
                        .property("temperature")
                        .required()
                        .scalar(ScalarType.DOUBLE)
                        .title("Temperature")
                        .property("unit")
                        .scalar(ScalarType.STRING)
                        .readable(true)
                        .writable(false)
                        .endObject()
                        .build(),
                null,
                null));

        final JsonNode result = output.getSchema(TagSchemaDirection.NORTHBOUND);
        assertThat(result.get("type").asText()).isEqualTo("object");
        assertThat(result.get("properties").get("value").get("properties").has("temperature"))
                .isTrue();
        assertThat(result.get("properties").get("value").get("properties").has("unit"))
                .isTrue();
        assertThat(result.get("properties").get("value").get("required").get(0).asText())
                .isEqualTo("temperature");
    }

    @Test
    void test_southboundSchema_dropsTheNonWritableEnvelope() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.LONG).title("RPM").build(),
                new SchemaBuilder()
                        .startObject()
                        .property("unit")
                        .scalar(ScalarType.STRING)
                        .endObject()
                        .build(),
                null));

        final JsonNode result = output.getSchema(TagSchemaDirection.SOUTHBOUND);

        // Only the value survives; tagName / timestamp / metadata can never be written.
        final JsonNode properties = result.get("properties");
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("tagName")).isFalse();
        assertThat(properties.has("timestamp")).isFalse();
        assertThat(properties.has("metadata")).isFalse();
        assertThat(properties.get("value").get("type").asText()).isEqualTo("integer");
    }

    @Test
    void test_northboundSchema_keepsTheEnvelope() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.LONG).build(),
                new SchemaBuilder()
                        .startObject()
                        .property("unit")
                        .scalar(ScalarType.STRING)
                        .endObject()
                        .build(),
                null));

        final JsonNode properties =
                output.getSchema(TagSchemaDirection.NORTHBOUND).get("properties");

        // The northbound direction observes the full data shape — the counterpart to the southbound test above.
        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("tagName")).isTrue();
        assertThat(properties.has("timestamp")).isTrue();
        assertThat(properties.has("metadata")).isTrue();
    }

    @Test
    void test_context_appearsNorthboundOnly() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.LONG).build(),
                null,
                new SchemaBuilder()
                        .startObject()
                        .property("sourceNode")
                        .scalar(ScalarType.STRING)
                        .endObject()
                        .build()));

        final JsonNode northbound =
                output.getSchema(TagSchemaDirection.NORTHBOUND).get("properties");
        assertThat(northbound.has("context")).isTrue();
        assertThat(northbound.get("context").get("readOnly").asBoolean()).isTrue();
        // No metadata was provided, so the northbound envelope must not invent one.
        assertThat(northbound.has("metadata")).isFalse();

        final JsonNode southbound =
                output.getSchema(TagSchemaDirection.SOUTHBOUND).get("properties");
        assertThat(southbound.has("context")).isFalse();
    }

    @Test
    void test_southboundValue_equalsNorthboundValue_forPlainValueTags()
            throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        // A writable plain tag: the adapter marks its value schema's root writable; one member is read-only.
        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder()
                        .startObject()
                        .property("temperature")
                        .required()
                        .scalar(ScalarType.DOUBLE)
                        .property("unit")
                        .scalar(ScalarType.STRING)
                        .readable(true)
                        .writable(false)
                        .endObject()
                        .writable()
                        .build(),
                null,
                null));

        final JsonNode northboundValue = output.getSchema(TagSchemaDirection.NORTHBOUND)
                .get("properties")
                .get("value");
        final JsonNode southboundValue = output.getSchema(TagSchemaDirection.SOUTHBOUND)
                .get("properties")
                .get("value");

        // The frontend decides "read and write use the same schema" by comparing the value sub-schemas, so
        // for a plain value tag the two directions must render the value identically. Both render the
        // adapter's schema as-is: its root flags pass through untouched (no readOnly for this writable tag)
        // and per-field flags inside the value are preserved.
        assertThat(southboundValue).isEqualTo(northboundValue);
        assertThat(southboundValue.has("readOnly")).isFalse();
        assertThat(southboundValue.has("writeOnly")).isFalse();
        assertThat(southboundValue.get("properties").get("unit").get("readOnly").asBoolean())
                .isTrue();
    }

    @SuppressWarnings({"deprecation", "removal"})
    @Test
    void test_northboundSchema_pinnedToDeprecatedSdkCompositeSchema() throws ExecutionException, InterruptedException {
        // The SDK keeps toCompositeSchema for adapters built against an older SDK, and the dependency direction
        // prevents either side from delegating to the other. This pin fails the day one of the two copies drifts:
        // third-party adapters see the SDK copy, Edge serves the northbound schema.
        final var dps = new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder()
                        .startObject()
                        .property("temperature")
                        .required()
                        .scalar(ScalarType.DOUBLE)
                        .property("unit")
                        .scalar(ScalarType.STRING)
                        .readable(true)
                        .writable(false)
                        .endObject()
                        .build(),
                new SchemaBuilder()
                        .startObject()
                        .property("quality")
                        .scalar(ScalarType.LONG)
                        .endObject()
                        .build(),
                new SchemaBuilder()
                        .startObject()
                        .property("sourceNode")
                        .scalar(ScalarType.STRING)
                        .endObject()
                        .build());

        final var output = new TagSchemaCreationOutputImpl();
        output.finish(dps);

        assertThat(output.getSchema(TagSchemaDirection.NORTHBOUND))
                .isEqualTo(SchemaJsonRepresentation.INSTANCE.toCompositeSchema(dps));
    }

    @SuppressWarnings({"deprecation", "removal"})
    @Test
    void test_getFuture_returnsTheNorthboundSchema() throws ExecutionException, InterruptedException {
        // getFuture() is the compatibility surface for callers outside this repository (the hivemq-edge-test
        // integration tests). It must keep producing the northbound document it always produced. The southbound
        // DataHub resources used to read the schema through here — they now ask for SOUTHBOUND explicitly.
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.LONG).title("RPM").build(),
                new SchemaBuilder()
                        .startObject()
                        .property("unit")
                        .scalar(ScalarType.STRING)
                        .endObject()
                        .build(),
                null));

        assertThat(output.getFuture().get()).isEqualTo(output.getSchema(TagSchemaDirection.NORTHBOUND));
    }

    /**
     * The condition-command shape exactly as an adapter must build it: {@code .writable()} on the root and on
     * every writable member. {@link com.hivemq.adapter.sdk.api.schema.SchemaBuilder} defaults to
     * {@code writable = false}, and the mapping editor does not offer a read-only field as a write destination,
     * so omitting these calls yields a southbound schema with no destinations —
     * {@link #test_southboundSchema_unmarkedExplicitSchemaRendersReadOnly()} pins that trap.
     * <p>
     * This is also the fixture used by {@code MappingInstructionList.spec.cy.tsx}: the frontend test must see the
     * document this builder actually produces, so if this shape changes, that fixture changes with it.
     */
    private static @NotNull Schema conditionCommandSchema() {
        return new SchemaBuilder()
                .startObject()
                .property("eventId")
                .required()
                .scalar(ScalarType.STRING)
                .writable()
                .property("method")
                .required()
                .scalar(ScalarType.LONG)
                .writable()
                .property("comment")
                .scalar(ScalarType.STRING)
                .writable()
                .endObject()
                .writable()
                .build();
    }

    private static @NotNull Schema alarmEventSchema() {
        return new SchemaBuilder()
                .startObject()
                .property("active")
                .scalar(ScalarType.BOOLEAN)
                .property("severity")
                .scalar(ScalarType.LONG)
                .endObject()
                .build();
    }

    @SuppressWarnings({"deprecation", "removal"})
    @Test
    void test_repeatedCalls_produceEqualButIndependentDocuments() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();
        output.finish(
                new TagSchemaCreationOutput.DataPointSchema(alarmEventSchema(), null, null, conditionCommandSchema()));

        // Both accessors compose a document per call rather than handing out one shared instance. Composition is
        // a pure function of the DataPointSchema the adapter finished with, so every call must produce an equal
        // document — repeated calls are interchangeable in value.
        assertThat(output.getFuture().get()).isEqualTo(output.getFuture().get());
        assertThat(output.getSchema(TagSchemaDirection.NORTHBOUND))
                .isEqualTo(output.getSchema(TagSchemaDirection.NORTHBOUND));
        assertThat(output.getSchema(TagSchemaDirection.SOUTHBOUND))
                .isEqualTo(output.getSchema(TagSchemaDirection.SOUTHBOUND));

        // ...and they must be *separate* instances. ObjectNode is mutable: handing every caller the same node
        // would let one caller's edit rewrite what the others already hold. That was the situation while this
        // future carried a pre-composed ObjectNode; per-call composition is what removes it.
        final ObjectNode first = output.getSchema(TagSchemaDirection.NORTHBOUND);
        final ObjectNode second = output.getSchema(TagSchemaDirection.NORTHBOUND);
        assertThat(first).isNotSameAs(second);

        first.put("$id", "urn:mutated-by-a-caller");
        assertThat(second.has("$id")).isFalse();
        assertThat(output.getSchema(TagSchemaDirection.NORTHBOUND).has("$id")).isFalse();
        assertThat(output.getFuture().get().has("$id")).isFalse();
    }

    @Test
    void test_southboundSchema_explicitWriteSchemaIsUsedInsteadOfTheValue()
            throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        // A tag whose write shape is not a projection of its read shape — e.g. an OPC-UA condition tag, whose
        // northbound shape is the alarm event but whose write target is {eventId, method, comment}.
        output.finish(
                new TagSchemaCreationOutput.DataPointSchema(alarmEventSchema(), null, null, conditionCommandSchema()));

        final JsonNode writeValue = output.getSchema(TagSchemaDirection.SOUTHBOUND)
                .get("properties")
                .get("value");

        assertThat(writeValue.get("properties").has("eventId")).isTrue();
        assertThat(writeValue.get("properties").has("method")).isTrue();
        assertThat(writeValue.get("properties").has("comment")).isTrue();
        // The northbound value shape must not leak into the southbound direction.
        assertThat(writeValue.get("properties").has("active")).isFalse();
        assertThat(writeValue.get("properties").has("severity")).isFalse();

        // ...while the northbound direction still shows the alarm-event shape.
        final JsonNode readValue = output.getSchema(TagSchemaDirection.NORTHBOUND)
                .get("properties")
                .get("value");
        assertThat(readValue.get("properties").has("active")).isTrue();
        assertThat(readValue.get("properties").has("eventId")).isFalse();
    }

    @Test
    void test_southboundSchema_writableCommandFieldsAreNotRenderedReadOnly()
            throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();
        output.finish(
                new TagSchemaCreationOutput.DataPointSchema(alarmEventSchema(), null, null, conditionCommandSchema()));

        final JsonNode writeValue = output.getSchema(TagSchemaDirection.SOUTHBOUND)
                .get("properties")
                .get("value");

        // The whole point of an explicit southbound schema is to give a write somewhere to land. A consumer
        // treats readOnly as "not a destination", so neither the command object nor any of its members may
        // carry it — this is what makes the A&C mapping editor show three destinations rather than none.
        assertThat(writeValue.has("readOnly")).isFalse();
        final JsonNode properties = writeValue.get("properties");
        assertThat(properties.get("eventId").has("readOnly")).isFalse();
        assertThat(properties.get("method").has("readOnly")).isFalse();
        assertThat(properties.get("comment").has("readOnly")).isFalse();
    }

    @Test
    void test_southboundSchema_conditionCommandDocumentIsPinned() throws Exception {
        final var output = new TagSchemaCreationOutputImpl();
        output.finish(
                new TagSchemaCreationOutput.DataPointSchema(alarmEventSchema(), null, null, conditionCommandSchema()));

        // The exact document the REST endpoint serves for an A&C condition tag. It is pinned here because the
        // frontend fixture in MappingInstructionList.spec.cy.tsx is a copy of it: a hand-written fixture that
        // drifts from this output tests the mock rather than the feature (which is how the missing readOnly
        // annotations went unnoticed). Change one, change the other.
        //
        // Note the readOnly on the root: that flag is on the wrapper this class builds, not on the adapter's
        // command schema, and no consumer reads it (getPropertyListFrom only walks `properties`).
        final String expected = """
                {
                  "type": "object",
                  "properties": {
                    "value": {
                      "type": "object",
                      "properties": {
                        "eventId": { "type": "string" },
                        "method": { "type": "integer" },
                        "comment": { "type": "string" }
                      },
                      "required": [ "eventId", "method" ]
                    }
                  },
                  "required": [ "value" ],
                  "readOnly": true,
                  "$schema": "https://json-schema.org/draft/2019-09/schema"
                }
                """;

        assertThat(output.getSchema(TagSchemaDirection.SOUTHBOUND))
                .isEqualTo(new com.fasterxml.jackson.databind.ObjectMapper().readTree(expected));
    }

    @Test
    void test_southboundSchema_unmarkedExplicitSchemaRendersReadOnly() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        // The same command shape, built the way it reads most naturally — without a single .writable() call.
        // SchemaBuilder's default is writable = false, so every node renders readOnly and the mapping editor
        // offers nothing. This test exists to make that failure visible here rather than in the UI; it pins the
        // reason the SDK Javadoc on DataPointSchema.southboundSchema insists on .writable().
        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                alarmEventSchema(),
                null,
                null,
                new SchemaBuilder()
                        .startObject()
                        .property("eventId")
                        .required()
                        .scalar(ScalarType.STRING)
                        .property("method")
                        .required()
                        .scalar(ScalarType.LONG)
                        .property("comment")
                        .scalar(ScalarType.STRING)
                        .endObject()
                        .build()));

        final JsonNode writeValue = output.getSchema(TagSchemaDirection.SOUTHBOUND)
                .get("properties")
                .get("value");

        assertThat(writeValue.get("readOnly").asBoolean()).isTrue();
        final JsonNode properties = writeValue.get("properties");
        assertThat(properties.get("eventId").get("readOnly").asBoolean()).isTrue();
        assertThat(properties.get("method").get("readOnly").asBoolean()).isTrue();
        assertThat(properties.get("comment").get("readOnly").asBoolean()).isTrue();
    }

    @Test
    void test_tagSchemaBuilder_buildReturnsSchemaObject() {
        final SchemaBuilder builder = new SchemaBuilder();

        final var schema = builder.scalar(ScalarType.LONG).build();

        assertThat(schema).isNotNull();
        assertThat(schema.title()).isNull();
    }

    @Test
    void test_tagSchemaBuilder_statusRemainsSuccess() throws ExecutionException, InterruptedException {
        final var output = new TagSchemaCreationOutputImpl();

        output.finish(new TagSchemaCreationOutput.DataPointSchema(
                new SchemaBuilder().scalar(ScalarType.BOOLEAN).build(), null, null));

        output.getSchema(TagSchemaDirection.NORTHBOUND);
        assertThat(output.getStatus()).isEqualTo(TagSchemaCreationOutputImpl.Status.SUCCESS);
    }
}
