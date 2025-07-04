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
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static java.util.Objects.requireNonNullElse;

public record OpcUaToMqttConfig (@JsonProperty("serverQueueSize")
                                 @ModuleConfigField(title = "OPC UA server queue size",
                                                    description = "OPC UA queue size for this subscription on the server",
                                                    numberMin = 1,
                                                    defaultValue = "1")
                                 int serverQueueSize,
                                 @JsonProperty("publishingInterval")
                                 @ModuleConfigField(title = "OPC UA publishing interval [ms]",
                                                    description = "OPC UA publishing interval in milliseconds for this subscription on the server",
                                                    numberMin = 1,
                                                    defaultValue = "1000")
                                 int publishingInterval
                                 ) {

    @JsonCreator
    public OpcUaToMqttConfig{
    }

}
