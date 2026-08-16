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
package com.hivemq.edge.adapters.opcua.condition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Review-05 finding 5: the command surface, enumerated from the code rather than restated.
 * <p>
 * The attached user command reference describes a feature two revisions old — fourteen methods, a numeric
 * "Wire" value for each, and a command shape of {@code {method, eventId?, comment?, duration?}}. Fifteen
 * methods ship, the numeric form is refused by the parser, and {@code selectedResponse} is a fifth field.
 * A reader following that table sends {@code {"method": 0, …}} and is rejected before any server is
 * contacted.
 * <p>
 * That document cannot be checked by a build — it lives in Linear. What <em>can</em> be checked is the
 * thing it is supposed to describe, and this is it: the accepted surface derived from
 * {@link ConditionUpdate.Method} rather than written out, so a list that has to be maintained by hand can be
 * read off a passing test instead of off the implementation. The other half of the pairing already exists —
 * {@code ConditionSchemasTest.theWriteSchemaNamesEveryMethod} pins the published schema against the same
 * enum — so between them the schema, the parser and the enum cannot drift apart silently. Only the prose can,
 * and this is what it should be derived from.
 * <p>
 * Deliberately not a duplicate of {@code ConditionUpdateMethodFieldTest}, which pins the <em>rules</em> for
 * one representative method: that a string is accepted, that case does not matter, that a number is not. This
 * pins the <em>extent</em> — that the rules hold for every method there is, and for each argument shape.
 */
class CommandSurfaceContractTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    /** A well-formed command for each argument shape — the examples a reference should print. */
    private static final @NotNull Map<ConditionUpdate.Method.Arguments, String> WORKED_EXAMPLES = new EnumMap<>(Map.of(
            ConditionUpdate.Method.Arguments.EVENT_AND_COMMENT,
            "{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGVsbG8gd29ybGQ=\", \"comment\": \"Checked\"}",
            ConditionUpdate.Method.Arguments.NONE,
            "{\"method\": \"ENABLE\"}",
            ConditionUpdate.Method.Arguments.DURATION,
            "{\"method\": \"TIMED_SHELVE\", \"duration\": 60000}",
            ConditionUpdate.Method.Arguments.SELECTED_RESPONSE,
            "{\"method\": \"RESPOND\", \"selectedResponse\": 1}"));

    @Test
    void everyMethodIsReachableByItsOwnName() {
        // The extent of the surface. Written out, this list is what a reference has to reproduce; derived,
        // it cannot be one revision behind the code the way the attached one is.
        //
        // Each command carries its own shape's arguments rather than the method name alone, because the
        // parser requires them -- and that is the point rather than an inconvenience. "Send ACKNOWLEDGE" is
        // not a usable instruction; "send ACKNOWLEDGE with the eventId from the message you are answering"
        // is, and a reference that prints only the first produces a rejected write.
        for (final ConditionUpdate.Method method : ConditionUpdate.Method.values()) {
            assertThat(ConditionUpdate.fromJson(json(commandFor(method, method.name())))
                            .method())
                    .as("'%s' must be reachable by the name the reference would print", method.name())
                    .isEqualTo(method);
        }
    }

    @Test
    void andByItsNameInAnyCasing() {
        // Hand-written commands are the case that matters here, and a reference printing ACKNOWLEDGE while
        // an operator types acknowledge should not be the difference between working and not.
        for (final ConditionUpdate.Method method : ConditionUpdate.Method.values()) {
            final String lowercased = method.name().toLowerCase(Locale.ROOT);
            assertThat(ConditionUpdate.fromJson(json(commandFor(method, lowercased)))
                            .method())
                    .isEqualTo(method);
        }
    }

    /**
     * The smallest command that will actually be accepted for a method — its name plus whatever its argument
     * shape requires.
     * <p>
     * Derived from {@link ConditionUpdate.Method#arguments()} rather than tabulated per method, so a method
     * that changes shape is carried along and a <em>new</em> shape fails
     * {@link #everyArgumentShapeHasAWorkedExampleThatParses} rather than silently falling through to a bare
     * command here.
     */
    private static @NotNull String commandFor(
            final @NotNull ConditionUpdate.Method method, final @NotNull String asWritten) {

        return switch (method.arguments()) {
            case EVENT_AND_COMMENT -> "{\"method\": \"" + asWritten + "\", \"eventId\": \"aGVsbG8gd29ybGQ=\"}";
            case DURATION -> "{\"method\": \"" + asWritten + "\", \"duration\": 60000}";
            case SELECTED_RESPONSE -> "{\"method\": \"" + asWritten + "\", \"selectedResponse\": 1}";
            case NONE -> "{\"method\": \"" + asWritten + "\"}";
        };
    }

    @Test
    void noMethodIsReachableByANumber() {
        // The "Wire" column, refuted for every row rather than for one. Those integers were Edge's own
        // invention -- no server ever sees them, since the wire carries a NodeId resolved from the browse
        // name -- and they were removed. A reader following the table is rejected locally, before any server
        // is contacted, which is a confusing way to learn that a documented form does not exist.
        final ConditionUpdate.Method[] methods = ConditionUpdate.Method.values();
        for (int ordinal = 0; ordinal < methods.length; ordinal++) {
            final int position = ordinal;
            assertThatThrownBy(() -> ConditionUpdate.fromJson(json("{\"method\": " + position + "}")))
                    .as("no numeric form may be accepted, including the position of %s", methods[position].name())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(ConditionUpdate.FIELD_METHOD);
        }
    }

    @Test
    void everyArgumentShapeHasAWorkedExampleThatParses() {
        // The examples a reference should print, checked by running them through the parser rather than by
        // being read. Four shapes, four examples: the one that was missing is SELECTED_RESPONSE, which is
        // exactly the field the attached reference omits.
        assertThat(WORKED_EXAMPLES.keySet())
                .as("a fifth argument shape needs a fifth example, not just a fifth enum constant")
                .containsExactlyInAnyOrder(ConditionUpdate.Method.Arguments.values());

        WORKED_EXAMPLES.forEach((shape, example) -> assertThat(
                        ConditionUpdate.fromJson(json(example)).method().arguments())
                .as("the %s example must be a command for a method of that shape: %s", shape, example)
                .isEqualTo(shape));
    }

    @Test
    void theWorkedExamplesCarryTheArgumentsTheirShapeNames() {
        // Parsing is not enough: an example that parses but drops its argument would still mislead. Each
        // shape's own field has to survive the parse.
        final ConditionUpdate acknowledge =
                ConditionUpdate.fromJson(json(WORKED_EXAMPLES.get(ConditionUpdate.Method.Arguments.EVENT_AND_COMMENT)));
        assertThat(acknowledge.eventId()).isNotNull();
        assertThat(acknowledge.comment()).isEqualTo("Checked");

        assertThat(ConditionUpdate.fromJson(json(WORKED_EXAMPLES.get(ConditionUpdate.Method.Arguments.DURATION)))
                        .duration())
                .isEqualTo(60_000d);

        assertThat(ConditionUpdate.fromJson(
                                json(WORKED_EXAMPLES.get(ConditionUpdate.Method.Arguments.SELECTED_RESPONSE)))
                        .selectedResponse())
                .isEqualTo(1);
    }

    @Test
    void everyMethodThatTakesNoArgumentsIsStillJustAMethod() {
        // Ten of the fifteen, and the reason a reference can print them as a bare list: nothing else is
        // needed to send one. Asserted over the enum rather than over a count, so adding an argument-less
        // method needs no edit here and adding an argument to one does.
        final long argumentLess = Arrays.stream(ConditionUpdate.Method.values())
                .filter(method -> method.arguments() == ConditionUpdate.Method.Arguments.NONE)
                .peek(method -> assertThat(ConditionUpdate.fromJson(json("{\"method\": \"" + method.name() + "\"}"))
                                .method())
                        .isEqualTo(method))
                .count();
        assertThat(argumentLess)
                .as("if this changes, the reference's list of bare commands changes with it")
                .isEqualTo(10L);
    }

    // ── the refresh command, which is a command surface of its own ───────────────────────────────────

    @Test
    void theRefreshCommandAcceptsItsOneDocumentedForm() {
        // RefreshCommand.validate had no unit test at all -- only an integration test that reaches it
        // through a southbound write. It is a published command shape and belongs here beside the other one.
        RefreshCommand.validate(
                json("{\"" + RefreshCommand.FIELD_METHOD + "\": \"" + RefreshCommand.METHOD_REFRESH + "\"}"));
    }

    @Test
    void andAcceptsItInAnyCasing() {
        RefreshCommand.validate(json("{\"method\": \"refresh\"}"));
    }

    @Test
    void butNotANumericOne() {
        // The same refutation as for a condition command, and the reason the two are worth stating together:
        // RefreshCommand's javadoc calls itself "deliberately shaped like ConditionUpdate", so a reader who
        // has seen one reference expects the other to behave the same way.
        assertThatThrownBy(() -> RefreshCommand.validate(json("{\"method\": 0}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(RefreshCommand.FIELD_METHOD);
    }

    @Test
    void norACondition() {
        // A user who writes ACKNOWLEDGE to a refresh tag has confused two tags. Silently refreshing instead
        // would be a worse answer than an error.
        assertThatThrownBy(() -> RefreshCommand.validate(json("{\"method\": \"ACKNOWLEDGE\"}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(RefreshCommand.METHOD_REFRESH);
    }

    private static @NotNull JsonNode json(final @NotNull String raw) {
        try {
            return MAPPER.readTree(raw);
        } catch (final Exception e) {
            throw new AssertionError("the test's own payload is not valid JSON: " + raw, e);
        }
    }
}
