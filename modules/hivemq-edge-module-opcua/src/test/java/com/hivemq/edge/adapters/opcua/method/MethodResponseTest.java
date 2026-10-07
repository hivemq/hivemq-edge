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
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.tag.TagDefinition;
import com.hivemq.datapoint.DataPointWithMetadata;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
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
        MethodResponse.write(builder, methodName, correlationData, responseTopic, result);
        builder.endDataPoint();
        return ((DataPointWithMetadata) builder.build("test-adapter")).getJsonNode();
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
        assertThat(json.get("context").get("methodName").textValue()).isEqualTo("Start");
        assertThat(json.get("context").has("errorCode")).isFalse();
    }

    @Test
    void successWithNoOutputs_carriesAnEmptyObjectNotNull() {
        // "It ran and had nothing to report" must be distinguishable from "it did not run". A Stop that returns
        // nothing is the common case, so a consumer meets this constantly.
        final JsonNode json = write("Stop", null, success(Map.of()));

        assertThat(json.get("value").isObject()).isTrue();
        assertThat(json.get("value")).isEmpty();
        assertThat(json.get("value").isNull()).isFalse();
    }

    @Test
    void failure_carriesValueNullAndTheStatusName() {
        final JsonNode json = write("Start", null, failure(StatusCodes.Bad_NotExecutable, Map.of()));

        // Present and null, not absent: a consumer reading `value` never has to ask whether the field exists
        // before asking what it holds.
        assertThat(json.has("value")).isTrue();
        assertThat(json.get("value").isNull()).isTrue();
        assertThat(json.get("context").get("errorCode").textValue()).isEqualTo("Bad_NotExecutable");
    }

    @Test
    void failureWithPerArgumentStatuses_namesTheArgumentsRatherThanTheirPositions() {
        final Map<String, StatusCode> statuses = new LinkedHashMap<>();
        statuses.put("setpoint", new StatusCode(StatusCodes.Bad_OutOfRange));
        statuses.put("ramp", StatusCode.GOOD);

        final JsonNode json = write("Start", null, failure(StatusCodes.Bad_InvalidArgument, statuses));

        final JsonNode arguments = json.get("context").get("statusInfo").get("arguments");
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

        assertThat(json.get("value").isNull()).isTrue();
        assertThat(json.get("context")
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

        assertThat(json.get("context").has("statusInfo")).isFalse();
    }

    @Test
    void correlationData_isEchoedWhenPresentAndOmittedWhenNot() {
        assertThat(write("Start", "mes-east-00417", success(Map.of()))
                        .get("context")
                        .get("correlationData")
                        .textValue())
                .isEqualTo("mes-east-00417");

        assertThat(write("Start", null, success(Map.of())).get("context").has("correlationData"))
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
        // this context -- it is the one entry that names a destination rather than describing the answer, so the
        // consumer consumes it instead of publishing it. Omitting it here left a request's own response topic
        // reaching the adapter correctly and then being silently dropped, so every answer went to the mapping's
        // configured topic however the caller addressed it. Nothing failed; the address was just ignored.
        assertThat(write("Start", null, "plant/mes-east", success(Map.of()))
                        .get("context")
                        .get("responseTopic")
                        .textValue())
                .isEqualTo("plant/mes-east");
    }

    @Test
    void noResponseTopic_leavesTheEntryOut() {
        // A fire-and-forget call. An absent entry is how the publisher is told to use its configured topic, so
        // writing an empty or null one would make "no address" indistinguishable from a malformed address.
        assertThat(write("Start", null, null, success(Map.of())).get("context").has("responseTopic"))
                .isFalse();
        assertThat(write("Start", null, "   ", success(Map.of())).get("context").has("responseTopic"))
                .isFalse();
    }

    @Test
    void responseTopic_isCarriedOnAFailureToo() {
        // A caller that asked for its answer somewhere still wants the refusal there, not on a topic it is not
        // listening to.
        assertThat(write("Start", "c-1", "plant/mes-east", failure(0x80AB0000L, Map.of()))
                        .get("context")
                        .get("responseTopic")
                        .textValue())
                .isEqualTo("plant/mes-east");
    }
}
