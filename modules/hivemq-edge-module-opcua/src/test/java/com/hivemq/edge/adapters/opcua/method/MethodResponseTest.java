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
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.datapoint.DataPointWithMetadata;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.Matrix;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// The shape of a response message, as a consumer sees it.
///
/// Everything here is about what a consumer can rely on: that `value` is always present, that its being null
/// means something different from its being empty, and that failure detail is named rather than positional.
class MethodResponseTest {

    private static final @NotNull Tag TAG = new Tag() {
        @Override
        public @NotNull TagDefinition getDefinition() {
            return new TagDefinition() {};
        }

        @Override
        public @NotNull String getName() {
            return "boiler1";
        }

        @Override
        public @NotNull String getDescription() {
            return "";
        }
    };

    /// Builds a datapoint the way the adapter does, and returns the JSON it holds.
    private static @NotNull JsonNode write(
            final @NotNull String methodName, final String correlationData, final @NotNull MethodCallResult result) {

        return write(methodName, correlationData, null, result);
    }

    /// As above, for a request that named its own response topic.
    private static @NotNull JsonNode write(
            final @NotNull String methodName,
            final String correlationData,
            final String responseTopic,
            final @NotNull MethodCallResult result) {

        final var builder =
                (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(TAG, b -> null);
        // The builtin types are all these tests use. A custom structure would need the client's own context,
        // which is what production passes.
        MethodResponse.write(
                builder, methodName, correlationData, responseTopic, result, DefaultEncodingContext.INSTANCE);
        builder.endDataPoint();
        return ((DataPointWithMetadata) builder.build("test-adapter")).getJsonNode();
    }

    /// The response's parameters, which travel inside `value` in the field that crosses the adapter border.
    /// Edge lifts them to the message root and removes the field, so no published message has this shape --
    /// these tests are about what the adapter hands over, not about what a subscriber sees.
    private static @NotNull JsonNode parameters(final @NotNull JsonNode datapoint) {
        return datapoint.get("value").get(MethodRequestFieldsOpcUa.INTERNAL_RESPONSE_PARAMETERS);
    }

    private static @NotNull MethodCallResult success(final @NotNull Map<String, Variant> outputs) {
        return new MethodCallResult(StatusCode.GOOD, outputs, Map.of());
    }

    private static @NotNull MethodCallResult failure(
            final long code, final @NotNull Map<String, StatusCode> argumentStatuses) {
        return new MethodCallResult(new StatusCode(code), Map.of(), argumentStatuses);
    }

    @Test
    void success_carriesTheOutputArgumentsByName() {
        final Map<String, Variant> outputs = new LinkedHashMap<>();
        outputs.put("started", Variant.of(true));
        outputs.put("actualSetpoint", Variant.of(178));

        final JsonNode json = write("Start", null, success(outputs));

        assertThat(json.get("value").get("started").booleanValue()).isTrue();
        assertThat(json.get("value").get("actualSetpoint").intValue()).isEqualTo(178);
        assertThat(parameters(json).get("methodName").textValue()).isEqualTo("Start");
        assertThat(parameters(json).has("errorCode")).isFalse();
    }

    @Test
    void successWithNoOutputs_carriesAnEmptyObjectNotNull() {
        // "It ran and had nothing to report" must be distinguishable from "it did not run". A Stop that returns
        // nothing is the common case, so a consumer meets this constantly.
        final JsonNode json = write("Stop", null, success(Map.of()));

        // Indistinguishable from a failure at this point -- both carry only the parameter object -- and that is
        // correct: the difference is `errorCode`, which a success does not set. Edge reads that and publishes
        // `value` as an empty object here and as null for a failure, which is where the two part company.
        assertThat(json.get("value").isObject()).isTrue();
        assertThat(json.get("value").properties().stream().map(Map.Entry::getKey))
                .containsExactly("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8");
        assertThat(parameters(json).has("errorCode"))
                .withFailMessage("a success must set no errorCode: %s", json)
                .isFalse();
    }

    @Test
    void failure_carriesValueNullAndTheStatusName() {
        final JsonNode json = write("Start", null, failure(StatusCodes.Bad_NotExecutable, Map.of()));

        // A failure writes no output arguments, so `value` holds the parameters and nothing else. **The null
        // a subscriber sees is Edge's doing, not the adapter's:** NorthboundTagConsumer lifts the parameters
        // out and collapses `value` to null once it is empty, which is what keeps "the call failed" distinct
        // from "the method succeeded and declares no outputs". That distinction is pinned in
        // NorthboundTagConsumerContextTest; here `value` is still the object the parameters travel in.
        assertThat(json.get("value").isObject()).isTrue();
        assertThat(json.get("value").properties().stream().map(Map.Entry::getKey))
                .containsExactly("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8");
        assertThat(parameters(json).get("errorCode").textValue()).isEqualTo("Bad_NotExecutable");
    }

    @Test
    void failureWithPerArgumentStatuses_namesTheArgumentsRatherThanTheirPositions() {
        final Map<String, StatusCode> statuses = new LinkedHashMap<>();
        statuses.put("setpoint", new StatusCode(StatusCodes.Bad_OutOfRange));
        statuses.put("ramp", StatusCode.GOOD);

        final JsonNode json = write("Start", null, failure(StatusCodes.Bad_InvalidArgument, statuses));

        final JsonNode arguments = parameters(json).get("statusInfo").get("arguments");
        assertThat(arguments.get("setpoint").textValue()).isEqualTo("Bad_OutOfRange");
        // The good one is kept: it says the rest were evaluated, which "setpoint was rejected" alone does not.
        assertThat(arguments.get("ramp").textValue()).isEqualTo("Good");
    }

    @Test
    void failureWithDeviceOutput_carriesItUnderStatusInfo() {
        // A failed call may still return values -- how a PLC usually says why it refused. They are the device's
        // explanation, not the method's result, so they are not in `value`.
        final Map<String, Variant> outputs = new LinkedHashMap<>();
        outputs.put("errorNumber", Variant.of(17));
        final MethodCallResult result =
                new MethodCallResult(new StatusCode(StatusCodes.Bad_NotExecutable), outputs, Map.of());

        final JsonNode json = write("Start", null, result);

        assertThat(json.get("value").properties().stream().map(Map.Entry::getKey))
                .withFailMessage("the device's explanation must not be an output argument: %s", json)
                .containsExactly("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8");
        assertThat(parameters(json)
                        .get("statusInfo")
                        .get("deviceOutput")
                        .get("errorNumber")
                        .intValue())
                .isEqualTo(17);
    }

    @Test
    void failureWithNoDetail_omitsStatusInfoEntirely() {
        // statusInfo with one key is normal; statusInfo absent means the device supplied neither kind of detail.
        final JsonNode json = write("Start", null, failure(StatusCodes.Bad_NotExecutable, Map.of()));

        assertThat(parameters(json).has("statusInfo")).isFalse();
    }

    @Test
    void correlationData_isEchoedWhenPresentAndOmittedWhenNot() {
        assertThat(parameters(write("Start", "mes-east-00417", success(Map.of())))
                        .get("correlationData")
                        .textValue())
                .isEqualTo("mes-east-00417");

        assertThat(parameters(write("Start", null, success(Map.of()))).has("correlationData"))
                .isFalse();
    }

    @Test
    void outputTypes_arePublishedAsTheirJsonTypes() {
        // Deferring to toString would publish a number as a quoted string, and a consumer validating against the
        // generated schema would be rejected for a value Edge produced.
        final Map<String, Variant> outputs = new LinkedHashMap<>();
        outputs.put("flag", Variant.of(true));
        outputs.put("count", Variant.of(42));
        outputs.put("ratio", Variant.of(1.5d));
        outputs.put("label", Variant.of("x"));

        final JsonNode value = write("Start", null, success(outputs)).get("value");

        assertThat(value.get("flag").isBoolean()).isTrue();
        assertThat(value.get("count").isNumber()).isTrue();
        assertThat(value.get("ratio").isNumber()).isTrue();
        assertThat(value.get("label").isTextual()).isTrue();
    }

    @Test
    void theTypesWithNoJavaEquivalent_arePublishedAsTheSchemaDescribesThem() {
        // The decisive test. These are the types a private dispatch does not cover, and they used to reach the
        // message as Java debug text: an unsigned 32-bit 7 as "7", a ByteString as
        // "ByteString[bytes=010203]", a DateTime as "DateTime{date=..., instant=...}". Every one contradicted
        // the generated schema, which promises a number, base64 text and an ISO instant -- so a consumer
        // validating a successful response was rejected for a value Edge itself produced.
        //
        // Published through the same converter as a variable's value now, which is what makes a method's output
        // of a given OPC UA type look like a variable's value of that type rather than merely being asserted to.
        final Map<String, Variant> outputs = new LinkedHashMap<>();
        outputs.put("counter", Variant.of(uint(7)));
        outputs.put("blob", Variant.of(ByteString.of(new byte[] {1, 2, 3})));
        outputs.put("at", Variant.of(new DateTime(Instant.parse("2026-10-08T12:00:00Z"))));

        final JsonNode value = write("Status", null, success(outputs)).get("value");

        assertThat(value.get("counter").isNumber()).isTrue();
        assertThat(value.get("counter").intValue()).isEqualTo(7);
        assertThat(value.get("blob").textValue()).isEqualTo(Base64.getEncoder().encodeToString(new byte[] {1, 2, 3}));
        assertThat(value.get("at").textValue()).isEqualTo("2026-10-08T12:00:00Z");
    }

    @Test
    void anArrayOutput_isPublishedAsAJsonArray() {
        // Without an array branch this fell through to toString and published a Java array identity --
        // `[Ljava.lang.Integer;@1a2b3c` -- for a method whose schema promised an array of integers.
        final JsonNode value = write("AddArray", null, success(Map.of("out", Variant.of(new Integer[] {11, 12, 13}))))
                .get("value");

        assertThat(value.get("out").isArray()).isTrue();
        assertThat(value.get("out").get(0).intValue()).isEqualTo(11);
        assertThat(value.get("out").get(2).intValue()).isEqualTo(13);
    }

    @Test
    void anEmptyArrayOutput_isPublishedAsAnEmptyArray() {
        final JsonNode value = write("Take", null, success(Map.of("out", Variant.of(new Integer[0]))))
                .get("value");

        assertThat(value.get("out").isArray()).isTrue();
        assertThat(value.get("out")).isEmpty();
    }

    @Test
    void aNestedArrayOutput_keepsItsShape() {
        // A device returns a multi-dimensional value as a Matrix, not a nested Java array -- Milo refuses to put
        // the latter in a Variant at all. So the published shape has to come from the Matrix's nested form.
        final JsonNode value = write(
                        "Grid", null, success(Map.of("out", Variant.of(new Matrix(new Integer[][] {{1, 2}, {3, 4}})))))
                .get("value");

        assertThat(value.get("out").get(0).isArray()).isTrue();
        assertThat(value.get("out").get(1).get(1).intValue()).isEqualTo(4);
    }

    @Test
    void aStringArrayOutput_keepsItsElementsAsStrings() {
        final JsonNode value = write("Names", null, success(Map.of("out", Variant.of(new String[] {"a", "b"}))))
                .get("value");

        assertThat(value.get("out").get(0).textValue()).isEqualTo("a");
        assertThat(value.get("out").get(1).isTextual()).isTrue();
    }

    @Test
    void responseTopic_isCarriedSoThePublisherCanRouteOnIt() {
        // The decisive one. NorthboundTagConsumer chooses the response's topic by reading `responseTopic` from
        // these parameters -- it is the one entry naming a destination rather than describing the answer, so the
        // consumer consumes it instead of publishing it. Omitting it here left a request's own response topic
        // reaching the adapter correctly and then being silently dropped, so every answer went to the mapping's
        // configured topic however the caller addressed it. Nothing failed; the address was just ignored.
        assertThat(parameters(write("Start", null, "plant/mes-east", success(Map.of())))
                        .get("responseTopic")
                        .textValue())
                .isEqualTo("plant/mes-east");
    }

    @Test
    void noResponseTopic_leavesTheEntryOut() {
        // A fire-and-forget call. An absent entry is how the publisher is told to use its configured topic, so
        // writing an empty or null one would make "no address" indistinguishable from a malformed address.
        assertThat(parameters(write("Start", null, null, success(Map.of()))).has("responseTopic"))
                .isFalse();
        assertThat(parameters(write("Start", null, "   ", success(Map.of()))).has("responseTopic"))
                .isFalse();
    }

    @Test
    void aRefusalEdgeMakesItself_looksExactlyLikeADeviceFailure() {
        // The decisive property of answering pre-dispatch refusals. A caller matching answers to requests must not
        // need to know where in Edge a refusal happened: an unknown method name, refused by Edge before the call is
        // sent, has to arrive in the same shape as a refusal the device reported. Both are built from a
        // MethodCallResult with no outputs, so both carry the parameters alone and an errorCode -- and Edge
        // publishes both with `value` as null.
        final JsonNode edgeRefusal = write("Cube", "c-1", failure(0x80750000L, Map.of()));
        final JsonNode deviceRefusal = write("Cube", "c-1", failure(0x81110000L, Map.of()));

        assertThat(edgeRefusal.get("value").properties().stream().map(Map.Entry::getKey))
                .containsExactly("INTERNAL_RESPONSE_PARAMETERS_H9G7R9B6S0V5O8R8");
        assertThat(parameters(edgeRefusal).get("errorCode").textValue()).isEqualTo("Bad_MethodInvalid");
        assertThat(parameters(edgeRefusal).get("correlationData").textValue()).isEqualTo("c-1");

        // Same field set, different status. That sameness is the contract.
        assertThat(parameters(edgeRefusal).properties().stream().map(java.util.Map.Entry::getKey))
                .containsExactlyElementsOf(parameters(deviceRefusal).properties().stream()
                        .map(java.util.Map.Entry::getKey)
                        .toList());
    }

    @Test
    void aRefusalOfARequestThatNamedNoMethod_stillCarriesTheField() {
        // A request with no methodName has none to echo. The field is written as empty rather than omitted, so every
        // response has the same shape and a consumer never has to ask whether methodName exists.
        final JsonNode json = write("", "c-2", failure(0x80750000L, Map.of()));

        assertThat(parameters(json).get("methodName").textValue()).isEmpty();
        assertThat(parameters(json).get("errorCode").textValue()).isEqualTo("Bad_MethodInvalid");
    }

    @Test
    void responseTopic_isCarriedOnAFailureToo() {
        // A caller that asked for its answer somewhere still wants the refusal there, not on a topic it is not
        // listening to.
        assertThat(parameters(write("Start", "c-1", "plant/mes-east", failure(0x80AB0000L, Map.of())))
                        .get("responseTopic")
                        .textValue())
                .isEqualTo("plant/mes-east");
    }
}
