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
package com.hivemq.persistence.mappings;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.api.model.JavaScriptConstants;
import com.hivemq.api.utils.MessageHandlingUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class NorthboundMapping implements PollingContext {

    private static final int DEFAULT_QOS = 2;
    private static final long DEFAULT_MESSAGE_EXPIRY = JavaScriptConstants.JS_MAX_SAFE_INTEGER;
    public static final MessageHandlingOptions DEFAULT_MESSAGE_HANDLING_OPTIONS =
            MessageHandlingOptions.MQTTMessagePerTag;

    private final @NotNull String topic;
    private final @NotNull String tagName;
    private final @NotNull MessageHandlingOptions messageHandlingOptions;
    private final boolean includeTagNames;
    private final boolean includeTimestamp;
    private final @NotNull List<MqttUserProperty> userProperties;
    private final int maxQoS;
    private final long messageExpiryInterval;

    public NorthboundMapping(
            final @NotNull String tagName,
            final @NotNull String topic,
            final int maxQoS,
            final long messageExpiryInterval,
            final @NotNull MessageHandlingOptions messageHandlingOptions,
            final boolean includeTagNames,
            final boolean includeTimestamp,
            final @NotNull List<MqttUserProperty> userProperties) {
        this.tagName = tagName;
        this.topic = topic;
        this.messageHandlingOptions = messageHandlingOptions;
        this.messageExpiryInterval = messageExpiryInterval;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.userProperties = userProperties;
        this.maxQoS = maxQoS;
    }

    public @NotNull String getMqttTopic() {
        return topic;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public int getMqttQos() {
        return maxQoS;
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

    public @Nullable Long getMessageExpiryInterval() {
        return this.messageExpiryInterval;
    }

    public static NorthboundMapping fromModel(final com.hivemq.edge.api.model.NorthboundMapping model) {
        return new NorthboundMapping(model.getTagName(),
                model.getTopic(),
                model.getMaxQoS() == null ? DEFAULT_QOS : model.getMaxQoS().ordinal(),
                model.getMessageExpiryInterval() == null ? DEFAULT_MESSAGE_EXPIRY : model.getMessageExpiryInterval(),
                model.getMessageHandlingOptions() == null ?
                        DEFAULT_MESSAGE_HANDLING_OPTIONS :
                        MessageHandlingUtils.convert(model.getMessageHandlingOptions()),
                model.getIncludeTagNames(),
                model.getIncludeTimestamp(),
                model.getUserProperties() == null ?
                        List.of() :
                        model.getUserProperties()
                                .stream()
                                .map(u -> new MqttUserProperty(u.getName(), u.getValue()))
                                .collect(Collectors.toList()));
    }
}
