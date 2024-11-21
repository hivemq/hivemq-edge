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
package com.hivemq.edge.adapters.file.config.legacy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.edge.adapters.file.config.ContentType;
import com.hivemq.edge.adapters.file.payload.FileJsonPayloadCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LegacyFilePollingContext {
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

    @JsonProperty(value = "filePath", required = true)
    protected @NotNull String filePath;

    @JsonProperty(value = "contentType", required = true)
    protected @NotNull ContentType contentType;

    public @NotNull String getFilePath() {
        return filePath;
    }

    public @NotNull String getDestinationMqttTopic() {
        return destination;
    }

    public int getQos() {
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

    public @Nullable JsonPayloadCreator getJsonPayloadCreator() {
        return FileJsonPayloadCreator.INSTANCE;
    }

    public @NotNull ContentType getContentType() {
        return contentType;
    }
}
