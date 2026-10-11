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
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// What a southbound message carries, once DataHub has validated and transformed it.
///
/// Two parts, and the split is what each side owns:
///
///   - `value` is the only field every tag kind uses: the value to write, or a condition command, or a method's
///     input arguments by name. **The device names what is inside it.**
///   - `context` is everything a method request carries *besides* its arguments. **Edge names what is in
///     there**, assembles it before the message reaches the data policy, and the adapter reads one field of it
///     and returns the rest untouched.
///
/// `context` is null for every tag kind but `METHOD`, and for a method mapping it is always present -- Edge
/// refuses a request it cannot assemble one from, before the device is touched.
///
/// Unknown fields are ignored rather than rejected: the southbound field mapping is a whitelist that already
/// decides what reaches this record, and a message carrying something extra is the caller's business, not a
/// reason to refuse a well-formed request.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpcUaPayload(
        @JsonProperty("value") @NotNull JsonNode value,
        @JsonProperty("context") @Nullable RequestContext context) implements WritingPayload {

    /// Everything a method request carries besides its arguments.
    ///
    /// Assembled by Edge from three places a caller may put them -- an MQTT 5 property, a payload field, the
    /// mapping's own configuration -- so that by the time an adapter sees this, there is one answer rather than
    /// three candidates. The adapter reads {@link #methodName()} and treats the other two as opaque: it echoes
    /// them on the response and never acts on them.
    ///
    /// The reply address is **not** validated here or anywhere in the adapter. "Is this a publishable MQTT
    /// topic" is a question about MQTT, answered where the answer will be published, which is Edge.
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequestContext(
            /// Which method to call. Always present for a method mapping.
            @JsonProperty("methodName") @Nullable String methodName,
            /// The caller's correlation token, echoed unchanged on the response. Opaque to the adapter.
            @JsonProperty("correlationData") @Nullable String correlationData,
            /// Where the caller wants the response published, overriding the northbound mapping's own topic.
            @JsonProperty("responseTopic") @Nullable String responseTopic) {}

    /// A payload that writes a value and calls no method.
    ///
    /// A static factory rather than a second constructor: Jackson selects a record's canonical constructor by
    /// itself, and adding an overload makes that choice ambiguous for no gain at the call sites.
    public static @NotNull OpcUaPayload ofValue(final @NotNull JsonNode value) {
        return new OpcUaPayload(value, null);
    }

    /// The method a request names, or null when it carries no context at all.
    public @Nullable String methodName() {
        return context == null ? null : context.methodName();
    }

    /// The caller's token, or null when it sent none.
    public @Nullable String correlationData() {
        return context == null ? null : context.correlationData();
    }

    /// The address the caller named for its answer, or null when it named none.
    public @Nullable String responseTopic() {
        return context == null ? null : context.responseTopic();
    }
}
