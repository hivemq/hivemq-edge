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
package com.hivemq.edge.adapters.opcua.method;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.schema.SchemaJsonRepresentation;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.structured.Argument;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// At which level a method tag's schemas describe their fields.
///
/// A `DataPointSchema` member describes the **contents** of the aspect it is given for, not a whole message:
/// `TagSchemaCreationOutputImpl` wraps `valueSchema` and `southboundSchema` in a `value` property of its own, and
/// renders `context` under `context`. An adapter that declares the envelope itself therefore lands one level too
/// deep.
///
/// These pin that, because getting it wrong is silent in unit tests and fatal in a running Edge: a request schema
/// that declared its own envelope made every southbound write fail validation with `$.value: required property
/// 'methodName' not found`, while every half of the feature remained individually green. `methodName` is the
/// canary -- it is required, so a schema that names it at the wrong level rejects every request.
class MethodSchemasShapeTest {

    private static final @NotNull NodeId NODE = new NodeId(2, "Boiler1.M");

    private static @NotNull MethodSignature method() {
        return new MethodSignature(
                "Square",
                NODE,
                List.of(new Argument("input1", NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE)),
                List.of(new Argument("output1", NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE)));
    }

    private static @NotNull JsonNode json(final @NotNull com.hivemq.adapter.sdk.api.schema.Schema schema) {
        return SchemaJsonRepresentation.INSTANCE.toJsonSchemaDocument(schema);
    }

    @Test
    void requestSchema_describesTheArgumentsAtItsRoot() {
        final JsonNode properties =
                json(MethodSchemas.requestSchema(List.of(method()))).get("properties");

        assertThat(properties.has("input1")).isTrue();
    }

    @Test
    void requestSchema_declaresNoEnvelopeOfItsOwn() {
        // The three envelope fields are siblings of `value` in the MQTT payload -- the whole payload is read into
        // OpcUaPayload, whose methodName/correlationData/responseTopic are top-level components, and
        // MethodRequestInstructions writes them to the payload root. Declared here they would render inside
        // `value`, demanding them one level below where every writer puts them.
        final JsonNode properties =
                json(MethodSchemas.requestSchema(List.of(method()))).get("properties");

        assertThat(properties.has("methodName")).isFalse();
        assertThat(properties.has("correlationData")).isFalse();
        assertThat(properties.has("responseTopic")).isFalse();
        assertThat(properties.has("value")).isFalse();
    }

    @Test
    void responseSchema_describesTheOutputsAtItsRoot() {
        // The root is nullable -- a failed call publishes `value` as null -- which renders as
        // `anyOf: [{object...}, {null}]` rather than a plain object, so the properties are in the object branch.
        final JsonNode schema = json(MethodSchemas.responseSchema(List.of(method())));
        final JsonNode objectBranch = schema.get("anyOf").get(0);

        assertThat(objectBranch.get("type").textValue()).isEqualTo("object");
        assertThat(schema.get("anyOf").get(1).get("type").textValue()).isEqualTo("null");

        final JsonNode properties = objectBranch.get("properties");
        assertThat(properties.has("output1")).isTrue();
        assertThat(properties.has("value")).isFalse();
        assertThat(properties.has("methodName")).isFalse();
    }

    @Test
    void responseContextSchema_carriesWhatMethodResponsePutsInContext() {
        // The counterpart: these four do not belong in `value`, and MethodResponse writes them to the datapoint's
        // context. Only methodName is always there, so only it is required.
        final JsonNode schema = json(MethodSchemas.responseContextSchema());
        final JsonNode properties = schema.get("properties");

        assertThat(properties.has("methodName")).isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        assertThat(properties.has("errorCode")).isTrue();
        assertThat(properties.has("statusInfo")).isTrue();

        final var required = schema.get("required");
        assertThat(required).isNotNull();
        assertThat(required.toString()).contains("methodName");
        assertThat(required.toString()).doesNotContain("errorCode");
    }

    @Test
    void requestSchema_rejectsAnArgumentNameNoMethodDeclares() {
        // The decisive one. Listing properties in JSON Schema is permissive by default, so the union stated every
        // argument's type while silently admitting any other name beside them -- and a misspelled argument then
        // passed validation, reached the adapter, and was refused there instead of at the gate meant to catch it.
        final JsonNode schema = json(MethodSchemas.requestSchema(List.of(method())));

        assertThat(schema.get("additionalProperties").booleanValue()).isFalse();
        assertThat(schema.get("properties").has("input1")).isTrue();
    }

    @Test
    void requestSchema_withNoArgumentsAtAll_permitsNothing() {
        // A method taking no arguments. A closed object with no properties is the true statement about it; leaving
        // it open would accept any object, which is the opposite claim.
        final MethodSignature noArguments = new MethodSignature("Stop", NODE, List.of(), List.of());

        final JsonNode schema = json(MethodSchemas.requestSchema(List.of(noArguments)));

        // `properties` is always rendered, empty here. Empty properties plus additionalProperties false is what
        // "permits nothing" looks like in JSON Schema -- only `{}` satisfies it.
        assertThat(schema.get("additionalProperties").booleanValue()).isFalse();
        assertThat(schema.get("properties")).isEmpty();
    }

    @Test
    void requestSchema_stillAcceptsAnArgumentTwoMethodsDisagreeAbout() {
        // Closing the object must not cost the union anything. An argument whose type two methods declare
        // differently is described as `any` rather than dropped, so it is still a listed property and a request
        // carrying it is still valid.
        final MethodSignature asInt = new MethodSignature(
                "A", NODE, List.of(new Argument("mode", NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE)), List.of());
        final MethodSignature asText = new MethodSignature(
                "B",
                NODE,
                List.of(new Argument("mode", NodeIds.String, -1, null, LocalizedText.NULL_VALUE)),
                List.of());

        final JsonNode schema = json(MethodSchemas.requestSchema(List.of(asInt, asText)));

        assertThat(schema.get("properties").has("mode")).isTrue();
        assertThat(schema.get("additionalProperties").booleanValue()).isFalse();
    }

    @Test
    void responseSchema_staysOpen() {
        // The asymmetry is deliberate. The request schema is a contract a caller must meet, so closing it refuses
        // what no method declares. The response schema describes what Edge produces, and closing that would be a
        // promise about Edge's own output that a later field would break.
        final JsonNode schema = json(MethodSchemas.responseSchema(List.of(method())));
        final JsonNode objectBranch = schema.get("anyOf").get(0);

        assertThat(objectBranch.has("additionalProperties")).isFalse();
    }
}
