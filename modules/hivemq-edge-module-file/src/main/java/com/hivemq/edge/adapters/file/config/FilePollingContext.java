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
package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.edge.adapters.file.payload.FileJsonPayloadCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FilePollingContext implements PollingContext {
    @JsonProperty(value = "destination", required = true)
    @ModuleConfigField(title = "Destination Topic",
                       description = "The topic to publish data on",
                       required = true,
                       format = ModuleConfigField.FieldType.MQTT_TOPIC)
    protected @Nullable String destination;

    @JsonProperty(value = "qos", required = true)
    @ModuleConfigField(title = "QoS",
                       description = "MQTT Quality of Service level",
                       required = true,
                       numberMin = 0,
                       numberMax = 2,
                       defaultValue = "0")
    protected int qos = 0;

    @JsonProperty(value = "messageHandlingOptions")
    @ModuleConfigField(title = "Message Handling Options",
                       description = "This setting defines the format of the resulting MQTT message, either a message per changed tag or a message per subscription that may include multiple data points per sample",
                       enumDisplayValues = {
                               "MQTT Message Per Device Tag",
                               "MQTT Message Per Subscription (Potentially Multiple Data Points Per Sample)"},
                       defaultValue = "MQTTMessagePerTag")
    protected @NotNull MessageHandlingOptions messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;

    @JsonProperty(value = "includeTimestamp")
    @ModuleConfigField(title = "Include Sample Timestamp In Publish?",
                       description = "Include the unix timestamp of the sample time in the resulting MQTT message",
                       defaultValue = "true")
    protected @NotNull Boolean includeTimestamp = Boolean.TRUE;

    @JsonProperty(value = "includeTagNames")
    @ModuleConfigField(title = "Include Tag Names In Publish?",
                       description = "Include the names of the tags in the resulting MQTT publish",
                       defaultValue = "false")
    protected @NotNull Boolean includeTagNames = Boolean.FALSE;

    @JsonProperty(value = "userProperties")
    @ModuleConfigField(title = "User Properties",
                       description = "Arbitrary properties to associate with the subscription",
                       arrayMaxItems = 10)
    private @NotNull List<UserProperty> userProperties = new ArrayList<>();

    @JsonProperty(value = "filePath", required = true)
    @ModuleConfigField(title = "The file path",
                       description = "The absolute path to the file that should be scraped.",
                       required = true)
    protected @NotNull String filePath;

    @JsonProperty(value = "contentType", required = true)
    @ModuleConfigField(title = "Content Type",
                       description = "The type of the content within the file.",
                       enumDisplayValues = {
                               "application/octet-stream",
                               "text/plain",
                               "application/json",
                               "application/xml",
                               "text/csv"},
                       required = true)
    protected @NotNull ContentType contentType;

    @JsonCreator
    public FilePollingContext(
            @JsonProperty("destination") @Nullable final String destination,
            @JsonProperty("qos") final int qos,
            @JsonProperty("userProperties") @Nullable List<UserProperty> userProperties,
            @JsonProperty("filePath") @NotNull String filePath,
            @JsonProperty("contentType") @NotNull ContentType contentType) {
        this.destination = destination;
        this.qos = qos;
        this.contentType = contentType;
        if (userProperties != null) {
            this.userProperties = userProperties;
        }
        this.filePath = filePath;
    }


    public @NotNull String getFilePath() {
        return filePath;
    }

    @Override
    public @Nullable String getMqttTopic() {
        return destination;
    }

    @Override
    public int getQos() {
        return qos;
    }

    @Override
    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    @Override
    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    @Override
    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    @Override
    public @NotNull List<UserProperty> getUserProperties() {
        return userProperties;
    }

    @Override
    public @Nullable JsonPayloadCreator getJsonPayloadCreator() {
        return FileJsonPayloadCreator.INSTANCE;
    }

    public @NotNull ContentType getContentType() {
        return contentType;
    }
}
