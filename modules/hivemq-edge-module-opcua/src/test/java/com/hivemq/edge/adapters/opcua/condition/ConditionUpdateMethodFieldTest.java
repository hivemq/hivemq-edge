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

    private static @NotNull JsonNode json(final @NotNull String text) {
        try {
            return MAPPER.readTree(text);
        } catch (final Exception e) {
            throw new AssertionError("malformed test payload: " + text, e);
        }
    }
}
