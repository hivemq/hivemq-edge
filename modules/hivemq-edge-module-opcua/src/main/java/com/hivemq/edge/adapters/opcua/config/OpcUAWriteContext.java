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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.WriteContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ALL")
public class OpcUAWriteContext implements WriteContext {

    @JsonProperty(value = "source", required = true)
    @ModuleConfigField(title = "Source Topic",
                       description = "The topic from which the data are received.",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    protected @Nullable String source;

    @JsonProperty(value = "qos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       required = true,
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    protected int qos = 0;

    @JsonProperty("writingIntervalMillis")
    @ModuleConfigField(title = "Polling Interval [ms]",
                       description = "Minimum time in millisecond between consecutive writes for this mapping. " +
                               "This is intended to protect constrained devices from overloading.",
                       numberMin = 0,
                       required = true,
                       defaultValue = "1000")
    private int writingIntervalMillis = 1000; //1 second

    @JsonProperty("node")
    @ModuleConfigField(title = "Destination Node ID",
                       description = "identifier of the node on the OPC-UA server. Example: \"ns=3;s=85/0:Temperature\"",
                       required = true)
    private @NotNull String destination = "";

    @Override
    public @Nullable String getSourceMqttTopic() {
        return source;
    }

    @Override
    public int getQos() {
        return qos;
    }

    @Override
    public long getWritingInterval() {
        return writingIntervalMillis;
    }

    public @NotNull String getDestination() {
        return destination;
    }
}
