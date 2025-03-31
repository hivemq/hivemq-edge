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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NorthboundMapping implements PollingContext {

    private static final int DEFAULT_QOS = 2;
    private static final long DEFAULT_MESSAGE_EXPIRY = JavaScriptConstants.JS_MAX_SAFE_INTEGER;

    private final @NotNull String topic;
    private final @NotNull String tagName;
    private final @NotNull MessageHandlingOptions messageHandlingOptions;
    private final Boolean includeTagNames;
    private final Boolean includeTimestamp;
    private final @NotNull List<MqttUserProperty> userProperties;
    private final int maxQoS;
    private final long messageExpiryInterval;

    public NorthboundMapping(
            final @NotNull String tagName,
            final @NotNull String topic,
            final int maxQoS,
            final long messageExpiryInterval,
            final @NotNull MessageHandlingOptions messageHandlingOptions,
            final Boolean includeTagNames,
            final Boolean includeTimestamp,
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
                MessageHandlingOptions.MQTTMessagePerTag,
                model.getIncludeTagNames() != null && model.getIncludeTagNames(),
                model.getIncludeTimestamp() != null && model.getIncludeTimestamp(),
                model.getUserProperties() == null ?
                        List.of() :
                        model.getUserProperties()
                                .stream()
                                .map(u -> new MqttUserProperty(u.getName(), u.getValue()))
                                .collect(Collectors.toList()));
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final NorthboundMapping that = (NorthboundMapping) o;
        return Objects.equals(getIncludeTagNames(), that.getIncludeTagNames()) &&
                Objects.equals(getIncludeTimestamp(), that.getIncludeTimestamp()) &&
                maxQoS == that.maxQoS &&
                Objects.equals(getMessageExpiryInterval(),that.getMessageExpiryInterval()) &&
                Objects.equals(topic, that.topic) &&
                Objects.equals(getTagName(), that.getTagName()) &&
                getMessageHandlingOptions() == that.getMessageHandlingOptions() &&
                Objects.equals(getUserProperties(), that.getUserProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic,
                getTagName(),
                getMessageHandlingOptions(),
                getIncludeTagNames(),
                getIncludeTimestamp(),
                getUserProperties(),
                maxQoS,
                getMessageExpiryInterval());
    }

    @Override
    public String toString() {
        return "NorthboundMapping{" +
                "topic='" +
                topic +
                '\'' +
                ", tagName='" +
                tagName +
                '\'' +
                ", messageHandlingOptions=" +
                messageHandlingOptions +
                ", includeTagNames=" +
                includeTagNames +
                ", includeTimestamp=" +
                includeTimestamp +
                ", userProperties=" +
                userProperties +
                ", maxQoS=" +
                maxQoS +
                ", messageExpiryInterval=" +
                messageExpiryInterval +
                '}';
    }
}
