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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.edge.adapters.opcua.method.MethodRequestFieldsOpcUa;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// What a southbound message carries, once DataHub has validated and transformed it.
///
/// Two parts, and the split is what each side owns:
///
///   - `value` is the only field every tag kind uses: the value to write, or a condition command, or a method's
///     input arguments by name. **The device names what is inside it.**
///   - For a method request, `value` additionally carries one field Edge put there,
///     `INTERNAL_REQUEST_PARAMETERS`, holding everything the request carries *besides* its arguments. **Edge
///     names what is in there.** [#parameters()] takes it out and [#value()] returns the arguments without
///     it, so no caller of either sees the other's namespace.
///
/// That field is absent for every tag kind but `METHOD`, and for a method mapping it is always present -- Edge
/// refuses a request it cannot assemble one from, before the device is touched. It exists only between Edge
/// writing it and this record reading it: no published message carries it and no schema declares it.
///
/// Unknown fields are ignored rather than rejected: the southbound field mapping is a whitelist that already
/// decides what reaches this record, and a message carrying something extra is the caller's business, not a
/// reason to refuse a well-formed request.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpcUaPayload(@JsonProperty("value") @NotNull JsonNode rawValue) implements WritingPayload {

    /// The parameters of one method request, read out of [#rawValue()] by [#parameters()].
    public record RequestParameters(
            /// Which method to call. Always present for a method mapping.
            @Nullable String methodName,
            /// The caller's correlation token, echoed unchanged on the response. Opaque to the adapter.
            @Nullable String correlationData,
            /// Where the caller wants the response published, overriding the northbound mapping's own topic.
            @Nullable String responseTopic) {}

    /// A payload that writes a value and calls no method.
    ///
    /// A static factory rather than a second constructor: Jackson selects a record's canonical constructor by
    /// itself, and adding an overload makes that choice ambiguous for no gain at the call sites.
    public static @NotNull OpcUaPayload ofValue(final @NotNull JsonNode value) {
        return new OpcUaPayload(value);
    }

    /// What the device names: a value to write, a condition command, or a method's input arguments.
    ///
    /// Edge's parameter field is removed, so a method tag's argument validation -- which closes the argument
    /// set and refuses any name the device did not declare -- never meets it.
    public @NotNull JsonNode value() {
        if (!(rawValue instanceof final ObjectNode object)
                || !object.has(MethodRequestFieldsOpcUa.INTERNAL_REQUEST_PARAMETERS)) {
            return rawValue;
        }
        final ObjectNode arguments = object.deepCopy();
        arguments.remove(MethodRequestFieldsOpcUa.INTERNAL_REQUEST_PARAMETERS);
        return arguments;
    }

    /// Everything the request carries besides its arguments, or null for a tag kind that carries none.
    ///
    /// Edge resolves these from three places a caller may put them -- an MQTT 5 property, a payload field, the
    /// mapping's configuration -- so an adapter sees one answer rather than three candidates. The adapter
    /// reads the method name and treats the rest as opaque: it echoes them and never acts on them. The reply
    /// address is not validated here, because "is this a publishable MQTT topic" is Edge's question.
    public @Nullable RequestParameters parameters() {
        if (!(rawValue instanceof final ObjectNode object)) {
            return null;
        }
        final JsonNode parameters = object.get(MethodRequestFieldsOpcUa.INTERNAL_REQUEST_PARAMETERS);
        if (parameters == null || !parameters.isObject()) {
            return null;
        }
        return new RequestParameters(
                text(parameters, MethodRequestFieldsOpcUa.METHOD_NAME),
                text(parameters, MethodRequestFieldsOpcUa.CORRELATION_DATA),
                text(parameters, MethodRequestFieldsOpcUa.RESPONSE_TOPIC));
    }

    private static @Nullable String text(final @NotNull JsonNode parameters, final @NotNull String field) {
        final JsonNode value = parameters.get(field);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    /// The method a request names, or null when it carries no parameters at all.
    public @Nullable String methodName() {
        final RequestParameters parameters = parameters();
        return parameters == null ? null : parameters.methodName();
    }

    /// The caller's token, or null when it sent none.
    public @Nullable String correlationData() {
        final RequestParameters parameters = parameters();
        return parameters == null ? null : parameters.correlationData();
    }

    /// The address the caller named for its answer, or null when it named none.
    public @Nullable String responseTopic() {
        final RequestParameters parameters = parameters();
        return parameters == null ? null : parameters.responseTopic();
    }
}
