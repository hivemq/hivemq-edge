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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * EDG-835: {@code method} is named by a string, and only by a string.
 * <p>
 * Both write schemas declare it {@code STRING}, and the schema is the contract Edge publishes. An earlier
 * version also accepted an integer — {@code 0} for {@code ACKNOWLEDGE}, {@code 13} for {@code SUPPRESS} —
 * and called it "the wire form", which it never was: no server sees those numbers, since the wire carries a
 * NodeId resolved from the browse name. They identified a method only inside Edge's own enum, were promised
 * by no schema, and {@code RefreshCommand} rejected them even while its javadoc called itself deliberately
 * the same shape.
 */
class ConditionUpdateMethodFieldTest {

    private static final @NotNull ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void aMethodNamedByStringIsAccepted() {
        assertThat(ConditionUpdate.fromJson(json("{\"eventId\": \"aGk=\", \"method\": \"ACKNOWLEDGE\"}"))
                        .method())
                .isEqualTo(ConditionUpdate.Method.ACKNOWLEDGE);
    }

    @Test
    void theNameIsMatchedCaseInsensitively() {
        // A hand-written command should not fail on capitalisation alone.
        assertThat(ConditionUpdate.fromJson(json("{\"eventId\": \"aGk=\", \"method\": \"acknowledge\"}"))
                        .method())
                .isEqualTo(ConditionUpdate.Method.ACKNOWLEDGE);
    }

    @Test
    void aNumericMethodIsRejected() {
        // 0 used to mean ACKNOWLEDGE. Nothing documents that, so it is refused rather than silently honoured.
        assertThatThrownBy(() -> ConditionUpdate.fromJson(json("{\"eventId\": \"aGk=\", \"method\": 0}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("method");
    }

    @Test
    void anUnknownNameIsRejectedAndListsTheKnownOnes() {
        // The error is what an operator sees after a failed write, so it has to say what would have worked.
        assertThatThrownBy(() -> ConditionUpdate.fromJson(json("{\"eventId\": \"aGk=\", \"method\": \"ACK\"}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ACKNOWLEDGE");
    }

    @Test
    void aMissingMethodIsRejected() {
        assertThatThrownBy(() -> ConditionUpdate.fromJson(json("{\"eventId\": \"aGk=\"}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── the comment field, whose three intents are distinguished by shape ────────────────────────────

    @Test
    void anObjectCommentIsRejectedRatherThanCoercedToAnErase() {
        // Review finding 12, and the dangerous one. `asText()` returns the *empty string* for an object or an
        // array -- and an empty comment is not "no comment", it is the specification's deliberate erase form
        // (OPC 10000-9 §5.7.3: "To reset the comment, an empty text with a locale shall be provided"). So
        // {"comment": {}}, which nobody means, silently wiped the condition's existing comment.
        //
        // Not a local mistake either: §5.5.2 fires a fresh event on any comment change, so the erase is
        // broadcast to every other client watching that alarm, and the previous operator's note is gone from
        // the audit trail with no way to recover it.
        assertThatThrownBy(() -> ConditionUpdate.fromJson(
                        json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\", " + "\"comment\": {}}")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("comment")
                .hasMessageContaining("must be a string");
    }

    @Test
    void anArrayCommentIsRejected() {
        assertThatThrownBy(() -> ConditionUpdate.fromJson(
                        json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\", " + "\"comment\": [\"a\"]}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aNumericCommentIsRejected() {
        // Jackson would render this as "42". Harmless-looking, but the parser is documented as rejecting a
        // command it cannot understand rather than guessing, and a number is not a note.
        assertThatThrownBy(() -> ConditionUpdate.fromJson(
                        json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\", " + "\"comment\": 42}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aBooleanCommentIsRejected() {
        assertThatThrownBy(() -> ConditionUpdate.fromJson(
                        json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\", " + "\"comment\": true}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anAbsentCommentMeansLeaveTheExistingOneAlone() {
        final ConditionUpdate update =
                ConditionUpdate.fromJson(json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\"}"));

        assertThat(update.comment()).isNull();
    }

    @Test
    void anExplicitNullCommentAlsoMeansLeaveItAlone() {
        // Serialisers that emit nulls for unset fields are common, so reading this as an erase would surprise
        // a caller who never thought about the field.
        final ConditionUpdate update = ConditionUpdate.fromJson(
                json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\", \"comment\": null}"));

        assertThat(update.comment()).isNull();
    }

    @Test
    void anEmptyStringCommentIsStillTheDeliberateErase() {
        // The distinction the strictness exists to protect. An empty string is the only way to clear a stale
        // comment, so it must keep working -- narrowing the field must not collapse the three intents into
        // two.
        final ConditionUpdate update = ConditionUpdate.fromJson(
                json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\", \"comment\": \"\"}"));

        assertThat(update.comment()).isEmpty();
    }

    @Test
    void anOrdinaryCommentIsCarriedThrough() {
        final ConditionUpdate update = ConditionUpdate.fromJson(
                json("{\"method\": \"ACKNOWLEDGE\", \"eventId\": \"aGk=\", \"comment\": \"Erwin has seen this\"}"));

        assertThat(update.comment()).isEqualTo("Erwin has seen this");
    }

    private static @NotNull JsonNode json(final @NotNull String text) {
        try {
            return MAPPER.readTree(text);
        } catch (final Exception e) {
            throw new AssertionError("malformed test payload: " + text, e);
        }
    }
}
