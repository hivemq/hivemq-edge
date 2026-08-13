/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.northbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.datapoint.DataPointWithMetadata;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.encoding.DefaultEncodingContext;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * EDG-835: a server may put a {@code StatusCode} where a field's value would go.
 * <p>
 * OPC 10000-4 §7.22.3 says it <em>shall</em> — "If a Value Attribute has an uncertain or bad StatusCode
 * associated with it then the Server shall provide the StatusCode instead of the Value Attribute", with
 * {@code Bad_UserAccessDenied} given as the example. So this is conforming behaviour, not a broken server,
 * and it can happen to any field of any declared type.
 * <p>
 * Published as-is, that status is indistinguishable from data: a {@code Severity} declared UInt16 becomes a
 * status object, and on a two-state field the status lands under {@code text} where a display string was
 * promised while {@code id} silently vanishes. Such a field is therefore published as null — which is what
 * it is — with the reason recorded under {@code unavailableFields}.
 */
class OpcUaEventToJsonConverterUnavailableFieldsTest {

    private static final @NotNull OpcuaConditionType ALARM =
            OpcuaConditionType.fromBrowseName("AlarmConditionType").orElseThrow();

    private static final @NotNull StatusCode ACCESS_DENIED = new StatusCode(StatusCodes.Bad_UserAccessDenied);

    @Test
    void whenNoFieldIsWithheld_thenTheCompanionKeyIsAbsent() {
        // The ordinary path: nothing is unavailable, so nothing says so.
        final JsonNode value = convert(allNull());

        assertThat(value.has(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS))
                .as("the key must not appear when the server withheld nothing")
                .isFalse();
    }

    @Test
    void whenAnOrdinaryFieldIsWithheld_thenItIsNullAndTheReasonIsNamed() {
        // Severity is declared UInt16. Published as-is, the status code would arrive as {code, symbol} where
        // a consumer expects a number.
        final JsonNode value = convert(withField("Severity", new Variant(ACCESS_DENIED)));

        assertThat(value.get("Severity").isNull())
                .as("a withheld field is published as null, not as the status standing in for it")
                .isTrue();
        assertThat(value.get(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS)
                        .get("Severity")
                        .textValue())
                .isEqualTo("Bad_UserAccessDenied");
    }

    @Test
    void whenAStateIsWithheld_thenTheStatusDoesNotLandUnderText() {
        // The worse of the two shapes: writeStateWithId would put the status object under `text`, and `id`
        // would vanish with it because there is no Boolean to fold in.
        final JsonNode value = convert(withField("ActiveState", new Variant(ACCESS_DENIED)));

        final JsonNode state = value.get("ActiveState");
        assertThat(state.isNull() || !state.path("text").isObject())
                .as("a status code must never be published under 'text'")
                .isTrue();
        assertThat(value.get(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS)
                        .get("ActiveState.text")
                        .textValue())
                .isEqualTo("Bad_UserAccessDenied");
    }

    // ── review-05 finding 7: which half of a two-state field was withheld ───────────────────────────

    @Test
    void whenOnlyTheIdIsWithheld_thenTheDiagnosticNamesTheIdAndTheTextSurvives() {
        // The finding, and the case that makes the shared key wrong rather than merely imprecise. The two
        // halves are made available independently, so a server may give the display text and withhold the
        // boolean. Named "ActiveState", the diagnostic said a field was unavailable while that field carried
        // a perfectly good "Active" -- and a consumer treating the map as a list of null fields threw it away.
        final Variant[] values = allNull();
        setField(values, "ActiveState", new Variant(new LocalizedText("en", "Active")));
        setStateId(values, "ActiveState", new Variant(ACCESS_DENIED));

        final JsonNode value = convert(values);

        assertThat(value.get("ActiveState").get("text").textValue())
                .as("the half the server did give must survive")
                .isEqualTo("Active");
        assertThat(value.get("ActiveState").has("id"))
                .as("and the half it withheld must not be invented")
                .isFalse();
        assertThat(value.get(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS)
                        .get("ActiveState.id")
                        .textValue())
                .as("the diagnostic must name the half that is missing, which is the one to branch on")
                .isEqualTo("Bad_UserAccessDenied");
    }

    @Test
    void whenOnlyTheTextIsWithheld_thenTheDiagnosticNamesTheTextAndTheIdSurvives() {
        // The mirror image, and the one that matters less operationally but has to be named correctly all
        // the same: `id` is what the documentation tells consumers to branch on, so a payload keeping it is
        // still usable and must not be reported as though it were not.
        final Variant[] values = allNull();
        setField(values, "ActiveState", new Variant(ACCESS_DENIED));
        setStateId(values, "ActiveState", new Variant(true));

        final JsonNode value = convert(values);

        assertThat(value.get("ActiveState").get("id").booleanValue()).isTrue();
        assertThat(value.get("ActiveState").has("text")).isFalse();
        assertThat(value.get(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS)
                        .get("ActiveState.text")
                        .textValue())
                .isEqualTo("Bad_UserAccessDenied");
    }

    @Test
    void whenBothHalvesAreWithheldForDifferentReasons_thenBothReasonsSurvive() {
        // The second half of the defect. Under one key, `putIfAbsent` kept whichever arrived first and
        // discarded the other -- so a field withheld for two different reasons reported one of them, chosen
        // by select-clause order rather than by anything meaningful.
        final Variant[] values = allNull();
        setField(values, "ActiveState", new Variant(ACCESS_DENIED));
        setStateId(values, "ActiveState", new Variant(new StatusCode(StatusCodes.Bad_NotReadable)));

        final JsonNode value = convert(values);
        final JsonNode unavailable = value.get(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS);

        assertThat(value.get("ActiveState").isNull())
                .as("with neither half given, the field really is null")
                .isTrue();
        assertThat(unavailable.get("ActiveState.text").textValue()).isEqualTo("Bad_UserAccessDenied");
        assertThat(unavailable.get("ActiveState.id").textValue()).isEqualTo("Bad_NotReadable");
    }

    @Test
    void anOrdinaryFieldIsStillNamedWithoutAMember() {
        // The property the change must not cost. Only a composite has halves to distinguish, so every other
        // field keeps the key it always had -- a suffix on Severity would name a member it does not have.
        final JsonNode unavailable = convert(withField("Severity", new Variant(ACCESS_DENIED)))
                .get(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS);

        assertThat(unavailable.has("Severity")).isTrue();
        assertThat(unavailable.properties())
                .as("no member suffix may appear on a field that is not a composite")
                .noneSatisfy(entry -> assertThat(entry.getKey()).startsWith("Severity."));
    }

    @Test
    void qualityIsNotTreatedAsWithheld() {
        // Quality's declared type IS a StatusCode (OPC 10000-9 §5.5.2 Table 8), so a status code there is
        // the value. Blanking it would lose a field present in every event of every type.
        final JsonNode value = convert(withField("Quality", new Variant(ACCESS_DENIED)));

        assertThat(value.has(OpcUaEventToJsonConverter.UNAVAILABLE_FIELDS))
                .as("Quality carries a status code by design; it is not a substitution")
                .isFalse();
        assertThat(value.get("Quality").path("code").isNumber())
                .as("Quality is still published as a status code")
                .isTrue();
    }

    @Test
    void aWithheldFieldDoesNotDisturbTheFieldsAroundIt() {
        // The fold between a state and its Id is positional, so a null substituted mid-array must not shift
        // anything after it.
        final Variant[] values = allNull();
        setField(values, "Severity", new Variant(ACCESS_DENIED));
        setField(values, "ActiveState", new Variant(new LocalizedText("en", "Active")));
        setStateId(values, "ActiveState", new Variant(true));

        final JsonNode value = convert(values);

        assertThat(value.get("ActiveState").get("text").textValue()).isEqualTo("Active");
        assertThat(value.get("ActiveState").get("id").booleanValue()).isTrue();
        assertThat(value.get("Severity").isNull()).isTrue();
    }

    /** A value array of the right length with every entry null, as a server sends for an empty transition. */
    private static @NotNull Variant[] allNull() {
        return new Variant[ALARM.selectedFields().size()];
    }

    private static @NotNull Variant[] withField(final @NotNull String field, final @NotNull Variant value) {
        final Variant[] values = allNull();
        setField(values, field, value);
        return values;
    }

    /** Sets the value at the position the select clause gives this field. */
    private static void setField(
            final @NotNull Variant[] values, final @NotNull String field, final @NotNull Variant value) {
        values[indexOf(List.of(field))] = value;
    }

    private static void setStateId(
            final @NotNull Variant[] values, final @NotNull String field, final @NotNull Variant value) {
        values[indexOf(List.of(field, "Id"))] = value;
    }

    private static int indexOf(final @NotNull List<String> path) {
        final List<OpcuaConditionType.SelectedField> fields = ALARM.selectedFields();
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i).path().equals(path)) {
                return i;
            }
        }
        throw new AssertionError("no field selected at path " + path);
    }

    /** Runs one event's field values through the converter and returns the emitted {@code value} node. */
    private static @NotNull JsonNode convert(final @NotNull Variant[] values) {
        final var builder = (DataPointWithMetadata.DataPointBuilderImpl<Void>) DataPointWithMetadata.<Void>builder(
                new OpcuaTag("test-tag", "", new OpcuaTagDefinition("ns=2;i=1001")), b -> null);

        OpcUaEventToJsonConverter.convertPayload(DefaultEncodingContext.INSTANCE, ALARM, values, builder);

        return builder.build("test-adapter").getTagValue();
    }
}
