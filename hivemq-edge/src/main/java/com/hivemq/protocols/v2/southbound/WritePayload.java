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
package com.hivemq.protocols.v2.southbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import org.jetbrains.annotations.NotNull;

/**
 * The southbound MQTT payload for a v2 adapter write (EDG-824 #3): the conventional
 * {@code {"value": <json>}} shape every v1 module uses. The value is kept as raw JSON — the v2 write path carries it
 * to the adapter as a JSON {@code DataPoint}; interpreting it against the node is the protocol adapter's job.
 */
public class V2WritePayload implements WritingPayload {

    @JsonProperty("value")
    private final @NotNull JsonNode value;

    @JsonCreator
    public V2WritePayload(@JsonProperty("value") final @NotNull JsonNode value) {
        this.value = value;
    }

    public @NotNull JsonNode getValue() {
        return value;
    }
}
