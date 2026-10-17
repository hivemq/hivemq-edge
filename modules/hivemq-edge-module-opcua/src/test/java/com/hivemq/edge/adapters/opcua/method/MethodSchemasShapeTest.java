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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return MethodSignature.ofObject(
                "Square",
                NODE,
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
        // The three envelope fields are siblings of `value` in the MQTT payload: a caller publishes them at the
        // payload root, and the whole payload is read into OpcUaPayload, whose methodName/correlationData/
        // responseTopic are top-level components. Declared here they would render inside `value`, demanding them
        // one level below where every writer puts them.
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
    void theRequestParameters_nameWhatARequestCarries_andRequireTheMethodName() {
        // The counterpart to the test above: these do not belong in `value` on the wire, and each direction
        // declares its own set exactly rather than a union of both. A request carries three, and `methodName`
        // is the one it cannot do without -- no MQTT property supplies a method name.
        final JsonNode parameters = json(MethodSchemas.requestSchema(List.of(method())))
                .get("properties")
                .get("INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8");
        final JsonNode properties = parameters.get("properties");

        assertThat(properties.has("methodName")).isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        assertThat(properties.has("responseTopic")).isTrue();
        assertThat(properties.has("errorCode"))
                .withFailMessage("a response-only field is declared on a request: %s", parameters)
                .isFalse();
        assertThat(properties.has("statusInfo"))
                .withFailMessage("a response-only field is declared on a request: %s", parameters)
                .isFalse();

        assertThat(parameters.get("required")).hasSize(1);
        assertThat(parameters.get("required").get(0).textValue()).isEqualTo("methodName");
    }

    @Test
    void theRequestParameters_areWritable_becauseACallerSendsThem() {
        // The builder defaults a property to not writable, so omitting `writable()` rendered `readOnly: true`
        // on all three -- and on `methodName` beside `required`, which tells a form to demand a field it must
        // not offer. Nothing enforces the annotation on this path today, which is why no request was rejected
        // and the omission went unseen through two reviews; it is read by whoever builds a form from the
        // schema, and Edge's lifting carries each property's schema into the published document verbatim.
        //
        // `readOnly` is rendered only when the property is not writable, so the assertion is its absence.
        final JsonNode properties = json(MethodSchemas.requestSchema(List.of(method())))
                .get("properties")
                .get("INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8")
                .get("properties");

        for (final String parameter : List.of("methodName", "correlationData", "responseTopic")) {
            assertThat(properties.get(parameter).has("readOnly"))
                    .withFailMessage(
                            "'%s' is a request field, so it must not be read-only: %s",
                            parameter, properties.get(parameter))
                    .isFalse();
        }
    }

    @Test
    void theResponseParameters_stayReadOnly_becauseASubscriberOnlyObservesThem() {
        // The mirror of the test above, and the reason the fix is not "make everything writable": a response
        // is something Edge publishes, so its fields are observed rather than sent.
        //
        // Via `anyOf` because the response root is nullable -- a failed call publishes `value` as null -- so
        // the properties sit in the object branch of the rendered union.
        final JsonNode properties = json(MethodSchemas.responseSchema(List.of(method())))
                .get("anyOf")
                .get(0)
                .get("properties")
                .get("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8")
                .get("properties");

        for (final String parameter : List.of("methodName", "correlationData", "errorCode", "statusInfo")) {
            assertThat(properties.get(parameter).path("readOnly").asBoolean(false))
                    .withFailMessage(
                            "'%s' is a response field, so it must stay read-only: %s",
                            parameter, properties.get(parameter))
                    .isTrue();
        }
    }

    @Test
    void everyOutputIsNullable_andNoInputIs() {
        // An output is published as null when this client cannot decode it -- the body the device sent moves
        // to statusInfo.undecodedOutputs instead. That can happen whatever the output's declared type, so
        // stating it only for the structured ones would understate it.
        //
        // An input is the opposite: a caller sending null has not supplied the argument, and the adapter
        // refuses that rather than inventing a value.
        final JsonNode outputs = json(MethodSchemas.responseSchema(List.of(method())))
                .get("anyOf")
                .get(0)
                .get("properties");
        // A nullable scalar states a two-element `type` -- `["integer", "null"]` -- rather than an `anyOf`,
        // which is how the nullable *root* of the response document renders.
        assertThat(outputs.get("output1"))
                .withFailMessage("the fixture must declare the output: %s", outputs)
                .isNotNull();
        assertThat(typesOf(outputs.get("output1")))
                .withFailMessage("an output must admit null: %s", outputs.get("output1"))
                .contains("null");

        final JsonNode inputs =
                json(MethodSchemas.requestSchema(List.of(method()))).get("properties");
        assertThat(typesOf(inputs.get("input1")))
                .withFailMessage("an input must not admit null: %s", inputs.get("input1"))
                .doesNotContain("null");
    }

    /// The types a rendered property admits, however its kind spells nullability.
    ///
    /// The renderer differs by schema kind: a nullable **scalar** states a two-entry `type`
    /// (`["integer", "null"]`), while a nullable **object** or **array** renders an `anyOf` whose second
    /// branch is `{"type": "null"}`. Reading only one spelling made a test pass for a property of the other
    /// kind without checking anything.
    private static @NotNull List<String> typesOf(final @NotNull JsonNode property) {
        final List<String> types = new ArrayList<>();
        final JsonNode anyOf = property.get("anyOf");
        if (anyOf != null) {
            anyOf.forEach(branch -> types.addAll(typesOf(branch)));
            return types;
        }
        final JsonNode type = property.get("type");
        if (type == null) {
            return types;
        }
        if (type.isArray()) {
            type.forEach(entry -> types.add(entry.textValue()));
        } else {
            types.add(type.textValue());
        }
        return types;
    }

    @Test
    void theResponseParameters_nameWhatAResponseCarries_andRequireNothing() {
        // The mirror, and the asymmetry is deliberate. A response declares four, all optional: the failure
        // fields appear only on a failure and the token only when the caller sent one. `responseTopic` is
        // absent although the payload carries it -- Edge consumes it to choose the publish topic, and a
        // published response must not carry a routing instruction for itself.
        // The root is nullable -- a failed call publishes `value` as null -- so the properties sit in the
        // object branch of the rendered `anyOf` rather than at the top.
        final JsonNode parameters = json(MethodSchemas.responseSchema(List.of(method())))
                .get("anyOf")
                .get(0)
                .get("properties")
                .get("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8");
        final JsonNode properties = parameters.get("properties");

        assertThat(properties.has("methodName")).isTrue();
        assertThat(properties.has("correlationData")).isTrue();
        assertThat(properties.has("errorCode")).isTrue();
        assertThat(properties.has("statusInfo")).isTrue();
        assertThat(properties.has("responseTopic"))
                .withFailMessage("responseTopic must stay out of the response schema: %s", parameters)
                .isFalse();

        assertThat(parameters.has("required"))
                .withFailMessage("a response's parameters require something: %s", parameters)
                .isFalse();
    }

    @Test
    void aUInt64Argument_statesItsLowerBoundAndNoUpperOne() {
        // The gate used to state neither. Both limits came from Milo's `ULong` constants, which are
        // BigIntegers, and the builder takes a bound only as a Long or a Double -- so both were dropped and the
        // argument was described as any integer, negatives included.
        //
        // The minimum is stateable and now stated. The maximum is not: 2^64-1 does not fit a signed long, and
        // forcing it would truncate to -1 and refuse every value. The converter bounds it instead, in
        // arbitrary precision, before the device sees it.
        final JsonNode argument = json(MethodSchemas.requestSchema(List.of(MethodSignature.ofObject(
                        "M",
                        NODE,
                        NODE,
                        List.of(new Argument("count", NodeIds.UInt64, -1, null, LocalizedText.NULL_VALUE)),
                        List.of()))))
                .get("properties")
                .get("count");

        assertThat(argument.get("type").textValue()).isEqualTo("integer");
        assertThat(argument.get("minimum"))
                .withFailMessage("a UInt64 is never negative, and the gate can say so: %s", argument)
                .isNotNull();
        assertThat(argument.get("minimum").asLong()).isZero();
        assertThat(argument.has("maximum"))
                .withFailMessage("any maximum here is a truncated one, which refuses every value: %s", argument)
                .isFalse();
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
    void everyTypeTheSchemaStatesConcretely_isEncodableByTheAdapter() {
        // The finding this closes: six builtin types were described concretely -- LocalizedText as a string,
        // StatusCode as an integer -- so a request passed the gate and was then refused by the converter,
        // which had no case for them. The adapter now encodes all six, and because it accepts two forms for
        // each, the schema says `any`: the model's node kinds are any, scalar, object and array, with no
        // alternation, so naming one form would refuse the other.
        final var twoFormTypes = List.of(
                NodeIds.LocalizedText,
                NodeIds.NodeId,
                NodeIds.ExpandedNodeId,
                NodeIds.QualifiedName,
                NodeIds.StatusCode,
                NodeIds.XmlElement);

        for (final NodeId type : twoFormTypes) {
            final JsonNode property = json(MethodSchemas.requestSchema(List.of(MethodSignature.ofObject(
                            "M",
                            NODE,
                            NODE,
                            List.of(new Argument("a", type, -1, null, LocalizedText.NULL_VALUE)),
                            List.of()))))
                    .get("properties")
                    .get("a");

            assertThat(property.has("type"))
                    .withFailMessage("%s must be described as any, was %s", type, property)
                    .isFalse();
            assertThat(property.get("description").textValue())
                    .withFailMessage("%s must tell a caller what to send, since the schema cannot", type)
                    .isNotEmpty();
        }
    }

    @Test
    void requestSchema_withNoArgumentsAtAll_permitsNothing() {
        // A method taking no arguments. A closed object with no properties is the true statement about it; leaving
        // it open would accept any object, which is the opposite claim.
        final MethodSignature noArguments = MethodSignature.ofObject("Stop", NODE, NODE, List.of(), List.of());

        final JsonNode schema = json(MethodSchemas.requestSchema(List.of(noArguments)));

        // Closed, plus no argument property -- which together say "only the parameter object is allowed in
        // here". The parameter object is the single property, and it is Edge's rather than the device's, so it
        // is named and skipped instead of being counted as an argument.
        assertThat(schema.get("additionalProperties").booleanValue()).isFalse();
        assertThat(schema.get("properties").properties().stream().map(Map.Entry::getKey))
                .containsExactly("INTERNAL_REQUEST_PARAMETERS_H9G7R9B6S0V5O8R8");
    }

    @Test
    void requestSchema_stillAcceptsAnArgumentTwoMethodsDisagreeAbout() {
        // Closing the object must not cost the union anything. An argument whose type two methods declare
        // differently is described as `any` rather than dropped, so it is still a listed property and a request
        // carrying it is still valid.
        final MethodSignature asInt = MethodSignature.ofObject(
                "A",
                NODE,
                NODE,
                List.of(new Argument("mode", NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE)),
                List.of());
        final MethodSignature asText = MethodSignature.ofObject(
                "B",
                NODE,
                NODE,
                List.of(new Argument("mode", NodeIds.String, -1, null, LocalizedText.NULL_VALUE)),
                List.of());

        final JsonNode schema = json(MethodSchemas.requestSchema(List.of(asInt, asText)));

        assertThat(schema.get("properties").has("mode")).isTrue();
        assertThat(schema.get("additionalProperties").booleanValue()).isFalse();
    }

    @Test
    void anArgumentWhoseRankFixesNoShape_isDescribedAsAny() {
        // OPC 10000-3 table 28 gives a value rank six meanings, and three of them fix no shape this model can
        // state: 0 (an array of one or more dimensions), -2 (a scalar or an array of any depth) and -3 (a
        // scalar or a one-dimensional array). There is no union here, so "a scalar or an array" has no form, and
        // rank 0 promises an array of a depth the server declined to fix.
        //
        // `any` is the honest answer -- the same one already given to an argument of a custom structure and to
        // one two methods declare differently. What it replaces is worse than vague: these were described as
        // scalars, so for rank 0, whose only legal values are arrays, every legal value was invalid and the
        // method could not be called.
        for (final int rank : new int[] {0, -2, -3}) {
            final MethodSignature method = MethodSignature.ofObject(
                    "Start",
                    NODE,
                    NODE,
                    List.of(new Argument("v", NodeIds.Int32, rank, null, LocalizedText.NULL_VALUE)),
                    List.of());

            final JsonNode property = json(MethodSchemas.requestSchema(List.of(method)))
                    .get("properties")
                    .get("v");

            assertThat(property.has("type"))
                    .withFailMessage("rank %d was given a type rather than `any`: %s", rank, property)
                    .isFalse();
        }
    }

    @Test
    void aScalarRank_isStillDescribedAsItsType() {
        // Rank -1 is the one negative rank that genuinely means scalar, and it keeps its type: widening it to
        // `any` would throw away a constraint the device did state.
        final MethodSignature method = MethodSignature.ofObject(
                "Start",
                NODE,
                NODE,
                List.of(new Argument("v", NodeIds.Int32, -1, null, LocalizedText.NULL_VALUE)),
                List.of());

        final JsonNode property = json(MethodSchemas.requestSchema(List.of(method)))
                .get("properties")
                .get("v");

        assertThat(property.get("type").textValue()).isEqualTo("integer");
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
