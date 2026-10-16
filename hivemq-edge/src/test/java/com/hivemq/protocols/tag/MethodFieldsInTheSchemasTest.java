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

/// How a method tag's parameters reach a schema document.
///
/// An adapter declares them inside its value schema, in one field per direction --
/// `INTERNAL_REQUEST_PARAMETERS` southbound, `INTERNAL_RESPONSE_PARAMETERS` northbound. That field is how the
/// parameters cross the adapter boundary: Edge writes it immediately before handing the payload over, and the
/// adapter takes it out immediately after.
///
/// **A document declares those parameters at its root, beside `value`, and not the field that carried them**
/// -- because that is where both messages carry them. A caller publishes `methodName` as a sibling of
/// `value`, and a published response carries its parameters the same way.
///
/// The decisive property is that **the lifting names no field**. Each direction's own value schema says which
/// parameters it carries and which are required, so the runtime iterates and declares what it finds. Earlier
/// versions held a list of names per direction, which is what made every change to the set a change in three
/// places.
class MethodFieldsInTheSchemasTest {

    /// A southbound value schema as a method adapter supplies it: the device's arguments, plus the one field
    /// carrying the request's parameters. `methodName` is required inside it; the other two are not.
    private static @NotNull Schema requestValue() {
        final var value = new SchemaBuilder().startObject();
        value.property("setpoint").scalar(ScalarType.LONG).writable();
        final var parameters =
                value.property("INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8").startObject();
        parameters.property("methodName").required().scalar(ScalarType.STRING);
        parameters.property("correlationData").scalar(ScalarType.STRING);
        parameters.property("responseTopic").scalar(ScalarType.STRING);
        parameters.endObject();
        return value.endObject().writable().build();
    }

    /// A northbound value schema as a method adapter supplies it: the device's outputs, plus the response's
    /// parameters. All four optional, and `responseTopic` deliberately absent -- Edge consumes the address to
    /// choose the publish topic rather than publishing it.
    private static @NotNull Schema responseValue() {
        final var value = new SchemaBuilder().startObject();
        value.property("setpoint").scalar(ScalarType.LONG);
        final var parameters =
                value.property("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8").startObject();
        parameters.property("methodName").scalar(ScalarType.STRING);
        parameters.property("correlationData").scalar(ScalarType.STRING);
        parameters.property("errorCode").scalar(ScalarType.STRING);
        parameters.property("statusInfo").scalar(ScalarType.STRING);
        parameters.endObject();
        return value.endObject().build();
    }

    /// A value schema from a tag that is not a method tag: no parameter field.
    private static @NotNull Schema ordinaryValue() {
        return new SchemaBuilder()
                .startObject()
                .property("setpoint")
                .scalar(ScalarType.LONG)
                .writable()
                .endObject()
                .writable()
                .build();
    }

    /// A `context` part, which belongs to the protocol and is no longer where Edge puts anything.
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
    void northbound_declaresTheResponseParametersAtTheRoot() throws Exception {
        final JsonNode document = document(
                new TagSchemaCreationOutput.DataPointSchema(responseValue(), null, null),
                TagSchemaDirection.NORTHBOUND);
        final JsonNode properties = document.get("properties");

        assertThat(properties.has("methodName"))
                .withFailMessage(
                        "the response parameters must sit at the root, as the message carries them: %s", properties)
                .isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        assertThat(properties.has("errorCode")).isTrue();
        assertThat(properties.has("statusInfo")).isTrue();

        // The carrier is Edge's hand-off to the adapter, not part of any message, and `value` keeps only what
        // the device named.
        assertThat(properties.path("value").path("properties").has("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8"))
                .withFailMessage("the carrier must not survive into the document: %s", properties)
                .isFalse();
        assertThat(properties.path("value").path("properties").has("setpoint")).isTrue();
    }

    @Test
    void southbound_declaresTheRequestParametersAtTheRoot() throws Exception {
        final JsonNode document = document(
                new TagSchemaCreationOutput.DataPointSchema(responseValue(), null, null, requestValue()),
                TagSchemaDirection.SOUTHBOUND);
        final JsonNode properties = document.get("properties");

        assertThat(properties.has("value")).isTrue();
        assertThat(properties.has("methodName")).isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        assertThat(properties.has("responseTopic")).isTrue();

        assertThat(properties.path("value").path("properties").has("INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8"))
                .withFailMessage("the carrier must not survive into the document: %s", properties)
                .isFalse();
    }

    /// Lifting the parameters out keeps the rest of `value` intact, annotations included.
    ///
    /// Splitting one object into two means constructing the `value` that remains, and a construction carries
    /// only what it is told to. Everything structural was copied -- properties, the required set, the
    /// additional-properties policy, the read/write/nullable flags -- and `title` and `description` were not,
    /// so a value schema that named and explained itself came out anonymous. The other tests here assert
    /// structure, which is why this went unseen.
    @Test
    void liftingTheParametersKeepsTheValuesOwnTitleAndDescription() throws Exception {
        final var value = new SchemaBuilder().startObject();
        value.property("setpoint").scalar(ScalarType.LONG).writable();
        value.property("INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8")
                .startObject()
                .property("methodName")
                .required()
                .scalar(ScalarType.STRING)
                .endObject();
        final Schema described = value.endObject()
                .title("Boiler controls")
                .description("The arguments the Start method takes")
                .writable()
                .build();

        final JsonNode southbound = document(
                        new TagSchemaCreationOutput.DataPointSchema(responseValue(), null, null, described),
                        TagSchemaDirection.SOUTHBOUND)
                .get("properties")
                .get("value");

        assertThat(southbound.path("title").asText()).isEqualTo("Boiler controls");
        assertThat(southbound.path("description").asText()).isEqualTo("The arguments the Start method takes");
    }

    /// A closed `value` stays closed, and closes `value` rather than the document around it.
    ///
    /// `MethodSchemas.requestSchema` closes the value object -- a name it does not list is not an argument of
    /// any method on the node, so a request carrying one is wrong however it is spelled, and a misspelled
    /// argument is refused at the validation gate instead of reaching the device. Reconstructing `value` has to
    /// carry that over, and has to put it in the right place: the builder's property stage offers a method of
    /// the same name which is shorthand for ending the property and closing the *enclosing* object, so one
    /// wrong receiver silently reopens `value` and closes the document root instead.
    ///
    /// The other helpers in this class leave the flag at its default of open, which is why nothing here could
    /// tell the two placements apart.
    @Test
    void aClosedValueStaysClosedAndTheDocumentRootStaysOpen() throws Exception {
        final var requestValue = new SchemaBuilder().startObject();
        requestValue.property("setpoint").scalar(ScalarType.LONG).writable();
        requestValue
                .property("INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8")
                .startObject()
                .property("methodName")
                .required()
                .scalar(ScalarType.STRING)
                .endObject();
        final Schema closedRequest =
                requestValue.additionalProperties(false).endObject().writable().build();

        final var responseValue = new SchemaBuilder().startObject();
        responseValue.property("setpoint").scalar(ScalarType.LONG);
        responseValue
                .property("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8")
                .startObject()
                .property("methodName")
                .scalar(ScalarType.STRING)
                .endObject();
        final Schema closedResponse =
                responseValue.additionalProperties(false).endObject().build();

        final JsonNode southbound = document(
                new TagSchemaCreationOutput.DataPointSchema(closedResponse, null, null, closedRequest),
                TagSchemaDirection.SOUTHBOUND);
        final JsonNode northbound = document(
                new TagSchemaCreationOutput.DataPointSchema(closedResponse, null, null, closedRequest),
                TagSchemaDirection.NORTHBOUND);

        // Only `false` is rendered -- an absent flag means an open object -- so the assertions are asymmetric
        // on purpose: present and false on `value`, absent at the root.
        for (final JsonNode document : new JsonNode[] {southbound, northbound}) {
            assertThat(document.path("properties")
                            .path("value")
                            .path("additionalProperties")
                            .asBoolean(true))
                    .withFailMessage(
                            "`value` must stay closed: %s",
                            document.path("properties").path("value"))
                    .isFalse();
            assertThat(document.has("additionalProperties"))
                    .withFailMessage("the document root must stay open, it gained the flag `value` lost: %s", document)
                    .isFalse();
        }
    }

    @Test
    void aRequiredParameterStaysRequiredAfterLifting() throws Exception {
        // `methodName` is required inside the request's parameter object, so it is required at the root of the
        // document describing that request. Requiredness has to travel with the parameter or the document
        // would permit a request nothing can dispatch.
        final JsonNode document = document(
                new TagSchemaCreationOutput.DataPointSchema(responseValue(), null, null, requestValue()),
                TagSchemaDirection.SOUTHBOUND);

        assertThat(document.get("required")).isNotNull();
        assertThat(document.get("required").toString()).contains("methodName").contains("value");
    }

    @Test
    void anOptionalParameterIsNotMadeRequired() throws Exception {
        // The mirror of the test above, and the one that matters for MQTT 5: the token and the reply address
        // may each arrive as a message property with nothing in the payload, so a document requiring them
        // would refuse exactly those requests.
        final JsonNode document = document(
                new TagSchemaCreationOutput.DataPointSchema(responseValue(), null, null, requestValue()),
                TagSchemaDirection.SOUTHBOUND);

        final String required = document.get("required").toString();
        assertThat(required).doesNotContain("correlationData").doesNotContain("responseTopic");
    }

    @Test
    void northbound_doesNotDeclareTheResponseTopic() throws Exception {
        // It says where an answer goes rather than anything about the answer, so the response schema does not
        // declare it -- while the payload carries it anyway, for Edge to route on. That mismatch is sound only
        // because nothing validates the payload while the carrier is present.
        final JsonNode properties = document(
                        new TagSchemaCreationOutput.DataPointSchema(responseValue(), null, null),
                        TagSchemaDirection.NORTHBOUND)
                .get("properties");

        assertThat(properties.has("responseTopic")).isFalse();
    }

    @Test
    void aTagWithNoParameters_producesTheDocumentsItAlwaysDid() throws Exception {
        // Every tag of every other adapter: nothing is lifted, and `value` is untouched.
        final var parts = new TagSchemaCreationOutput.DataPointSchema(ordinaryValue(), null, null);

        final JsonNode write = document(parts, TagSchemaDirection.SOUTHBOUND);
        assertThat(write.get("properties").properties()).hasSize(1);
        assertThat(write.get("properties").has("value")).isTrue();

        final JsonNode read = document(parts, TagSchemaDirection.NORTHBOUND);
        assertThat(read.get("properties").has("methodName")).isFalse();
        assertThat(read.get("properties").path("value").path("properties").has("setpoint"))
                .isTrue();
    }

    @Test
    void aContextPart_isPublishedAsItAlwaysWas() throws Exception {
        // `context` is the protocol's namespace and Edge no longer puts anything in it, so an adapter's own
        // part renders exactly as every other adapter's always has: nested, northbound only.
        final var parts = new TagSchemaCreationOutput.DataPointSchema(ordinaryValue(), null, ordinaryContext());

        final JsonNode read = document(parts, TagSchemaDirection.NORTHBOUND);
        assertThat(read.get("properties").has("context")).isTrue();
        assertThat(read.get("properties").has("units")).isFalse();

        final JsonNode write = document(parts, TagSchemaDirection.SOUTHBOUND);
        assertThat(write.get("properties").has("context")).isFalse();
    }
}
