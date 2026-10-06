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
package com.hivemq.edge.adapters.opcua.southbound;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// What a southbound message carries, once DataHub has validated and transformed it.
///
/// `value` is the only field every tag kind uses: the value to write, or a condition command, or a method's
/// input arguments by name. The other three belong to a method call and are null for every other kind.
///
/// They sit beside `value` rather than inside it so that a method may declare an argument called `methodName`
/// without colliding with Edge's own field. The device names what is inside `value`; Edge names what is
/// outside it.
///
/// Unknown fields are ignored rather than rejected: the southbound field mapping is a whitelist that already
/// decides what reaches this record, and a message carrying something extra is the caller's business, not a
/// reason to refuse a well-formed request.
@JsonIgnoreProperties(ignoreUnknown = true)
public record OpcUaPayload(
        @JsonProperty("value") @NotNull JsonNode value,
        /// Which method to call. Required for a `METHOD` tag, null for every other kind.
        @JsonProperty("methodName") @Nullable String methodName,
        /// The caller's correlation token, echoed unchanged on the response. Opaque to Edge.
        @JsonProperty("correlationData") @Nullable String correlationData,
        /// Where the caller wants the response published, overriding the northbound mapping's own topic.
        @JsonProperty("responseTopic") @Nullable String responseTopic)
        implements WritingPayload {

    /// A payload that writes a value and calls no method.
    ///
    /// A static factory rather than a second constructor: Jackson selects a record's canonical constructor by
    /// itself, and adding an overload makes that choice ambiguous for no gain at the call sites.
    public static @NotNull OpcUaPayload ofValue(final @NotNull JsonNode value) {
        return new OpcUaPayload(value, null, null, null);
    }
}
