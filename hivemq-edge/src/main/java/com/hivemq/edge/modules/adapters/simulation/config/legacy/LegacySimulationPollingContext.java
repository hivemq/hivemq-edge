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
package com.hivemq.edge.modules.adapters.simulation.config.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegacySimulationPollingContext {

    @JsonProperty(value = "destination", required = true)
    protected @NotNull String destination;

    @JsonProperty(value = "qos", required = true)
    protected int qos = 0;

    @JsonProperty(value = "messageHandlingOptions")
    protected @NotNull MessageHandlingOptions messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;

    @JsonProperty(value = "includeTimestamp")
    protected @NotNull Boolean includeTimestamp = Boolean.TRUE;

    @JsonProperty(value = "includeTagNames")
    protected @NotNull Boolean includeTagNames = Boolean.FALSE;

    @JsonProperty(value = "userProperties")
    private @NotNull List<MqttUserProperty> userProperties = new ArrayList<>();

    public @NotNull String getMqttTopic() {
        return destination;
    }

    public int getMqttQos() {
        return qos;
    }

    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    public @NotNull List<MqttUserProperty> getUserProperties() {
        return userProperties;
    }
}
