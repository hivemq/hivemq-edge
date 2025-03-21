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
package com.hivemq.edge.modules.adapters.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractProtocolAdapterJsonPayload {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("timestamp")
    private final @Nullable Long timestamp;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JsonProperty("mqttUserProperties")
    private @Nullable List<MqttUserProperty> mqttUserProperties;

    public AbstractProtocolAdapterJsonPayload(final @Nullable Long timestamp) {
        this.timestamp = timestamp;
    }

    @Nullable
    public Long getTimestamp() {
        return timestamp;
    }

    @Nullable
    public List<MqttUserProperty> getMqttUserProperties() {
        return mqttUserProperties;
    }

    public void setMqttUserProperties(final @Nullable List<MqttUserProperty> mqttUserProperties) {
        this.mqttUserProperties = mqttUserProperties;
    }
}
