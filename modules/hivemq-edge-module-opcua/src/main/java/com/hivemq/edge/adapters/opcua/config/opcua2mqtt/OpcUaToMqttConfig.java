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
package com.hivemq.edge.adapters.opcua.config.opcua2mqtt;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;

public record OpcUaToMqttConfig(
        @JsonProperty("serverQueueSize")
        @ModuleConfigField(
                title = "OPC UA server queue size",
                description = "OPC UA queue size for this subscription on the server",
                numberMin = 1,
                defaultValue = "1")
        int serverQueueSize,

        @JsonProperty("publishingInterval")
        @ModuleConfigField(
                title = "OPC UA publishing interval [ms]",
                description = "OPC UA publishing interval in milliseconds for this subscription on the server",
                numberMin = 1,
                defaultValue = "1000")
        int publishingInterval,

        @JsonProperty("eventQueueSize")
        @ModuleConfigField(
                title = "OPC UA event queue size",
                description = "OPC UA queue size for event subscriptions, such as condition tags. Separate from "
                        + "the value queue size because the same field means something different for events: a "
                        + "queue size of 1 asks the server for the smallest event queue it supports, not for a "
                        + "single entry. Events are transition reports, so an entry dropped from the queue is "
                        + "never re-sent.",
                numberMin = 1,
                defaultValue = "64")
        int eventQueueSize) {

    /** Deep enough to absorb a refresh burst or several alarms in one publishing cycle. */
    public static final int DEFAULT_EVENT_QUEUE_SIZE = 64;

    @JsonCreator
    public OpcUaToMqttConfig {}

    /**
     * Retains the two-argument shape from before events existed, so a caller that predates condition tags
     * keeps compiling and gets the default event queue.
     */
    public OpcUaToMqttConfig(final int serverQueueSize, final int publishingInterval) {
        this(serverQueueSize, publishingInterval, DEFAULT_EVENT_QUEUE_SIZE);
    }

    public static @NotNull OpcUaToMqttConfig defaultOpcUaToMqttConfig() {
        return new OpcUaToMqttConfig(1, 1000, DEFAULT_EVENT_QUEUE_SIZE);
    }
}
