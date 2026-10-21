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
package com.hivemq.edge.adapters.opcua.southbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.edge.adapters.opcua.method.MethodRequestFieldsOpcUa;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/// Whether a southbound payload carries Edge's request parameters, which is how the adapter learns what the
/// mapping's method-call setting says.
///
/// **Why this is tested on its own.** The adapter refuses two configurations in which the tag's kind and that
/// setting disagree: a METHOD tag whose request carries no parameters, answered with an error code; and a
/// non-method tag whose request carries them, which fails the write. Both rest entirely on this one predicate,
/// and nothing but this adapter can evaluate the pair -- the kind is OPC UA's vocabulary and the setting is
/// Edge's, so the generic side cannot check it without being told what a METHOD tag is.
///
/// Reaching the refusals themselves needs a connected client and a tag registry, so the predicate is pinned
/// here and the refusals are exercised by the integration tests. What would break silently is this: a payload
/// shape that reads as "no parameters" when parameters were sent would turn a working method call into a
/// refusal naming the wrong cause.
///
/// Reported by QA as P19 on EDG-1116.
class RequestParametersPresenceTest {

    private static @NotNull OpcUaPayload payloadWithParameters() {
        final ObjectNode value = JsonNodeFactory.instance.objectNode();
        final ObjectNode parameters = value.putObject(MethodRequestFieldsOpcUa.INTERNAL_REQUEST_PARAMETERS);
        parameters.put(MethodRequestFieldsOpcUa.METHOD_NAME, "Start");
        value.put("setpoint", 180);
        return new OpcUaPayload(value);
    }

    @Test
    void aRequestEdgeBuilt_carriesItsParameters() {
        assertThat(payloadWithParameters().parameters())
                .withFailMessage("a method request must be recognisable as one")
                .isNotNull();
        assertThat(payloadWithParameters().methodName()).isEqualTo("Start");
    }

    @Test
    void anOrdinaryValueWrite_carriesNone() {
        // What every southbound write that exists today looks like. Taking this for a method request would
        // refuse every value write on the adapter.
        final ObjectNode value = JsonNodeFactory.instance.objectNode();
        value.put("setpoint", 180);

        assertThat(new OpcUaPayload(value).parameters())
                .withFailMessage("a plain value write must not look like a method request")
                .isNull();
    }

    @Test
    void aScalarValue_carriesNone() {
        // A write whose value is not an object at all -- the common shape for a single variable. The predicate
        // must answer rather than throw, because it runs before anything else in the write path.
        assertThat(new OpcUaPayload(JsonNodeFactory.instance.numberNode(42)).parameters())
                .isNull();
    }

    @Test
    void aMethodTagRequestWithTheSettingOff_isIndistinguishableFromAValueWrite() {
        // The decisive case, and the reason the adapter has to make this judgement. With the mapping's
        // method-call setting off, Edge attaches nothing -- so what arrives on a METHOD tag is shaped exactly
        // like an ordinary value write. The adapter cannot infer the operator's intent from the payload; it
        // knows only that a METHOD tag received a request naming no method, which is the misconfiguration it
        // now reports with the setting named.
        final ObjectNode asItArrivesWithTheSettingOff = JsonNodeFactory.instance.objectNode();
        asItArrivesWithTheSettingOff.put("setpoint", 180);

        assertThat(new OpcUaPayload(asItArrivesWithTheSettingOff).parameters()).isNull();
        assertThat(new OpcUaPayload(asItArrivesWithTheSettingOff).methodName()).isNull();
    }

    @Test
    void theParametersAreKeptOutOfWhatTheDeviceSees() {
        // The counterpart: the predicate is true, and `value` still holds only what the device named. A
        // refusal built from this payload must not leak Edge's own field into a message or a device call.
        assertThat(payloadWithParameters().value().has(MethodRequestFieldsOpcUa.INTERNAL_REQUEST_PARAMETERS))
                .withFailMessage("Edge's parameter field must not reach the device")
                .isFalse();
        assertThat(payloadWithParameters().value().get("setpoint").intValue()).isEqualTo(180);
    }
}
