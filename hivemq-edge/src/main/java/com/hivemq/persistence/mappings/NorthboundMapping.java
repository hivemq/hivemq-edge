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
import com.hivemq.api.model.JavaScriptConstants;
import com.hivemq.mqtt.message.QoS;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("EnumOrdinal")
public class NorthboundMapping {

    private static final int DEFAULT_QOS = QoS.EXACTLY_ONCE.ordinal();
    private static final @NotNull Long DEFAULT_MESSAGE_EXPIRY = JavaScriptConstants.JS_MAX_SAFE_INTEGER;

    private final @NotNull String topic;
    private final @NotNull String tagName;
    private final int maxQoS;
    private final @NotNull Boolean includeTagNames;
    private final @NotNull Boolean includeTimestamp;
    private final @NotNull Boolean includeMetadata;
    private final @NotNull List<MqttUserProperty> userProperties;
    private final @Nullable Long messageExpiryInterval;

    public NorthboundMapping(
            final @NotNull String tagName,
            final @NotNull String topic,
            final int maxQoS,
            final @Nullable Boolean includeTagNames,
            final @Nullable Boolean includeTimestamp,
            final @Nullable Boolean includeMetadata,
            final @Nullable List<MqttUserProperty> userProperties,
            final @Nullable Long messageExpiryInterval) {
        this.tagName = tagName;
        this.topic = topic;
        this.maxQoS = maxQoS;
        this.includeTagNames = includeTagNames != null && includeTagNames;
        this.includeTimestamp = includeTimestamp == null || includeTimestamp; // default is true
        this.includeMetadata = includeMetadata != null && includeMetadata;
        this.userProperties = userProperties != null ? userProperties : new ArrayList<>();
        this.messageExpiryInterval = messageExpiryInterval != null ? messageExpiryInterval : DEFAULT_MESSAGE_EXPIRY;
    }

    private static @NotNull MqttUserProperty userProp(final @NotNull com.hivemq.edge.api.model.MqttUserProperty u) {
        return new MqttUserProperty(u.getName(), u.getValue());
    }

    public static @NotNull NorthboundMapping fromModel(
            final @NotNull com.hivemq.edge.api.model.NorthboundMapping model) {
        return new NorthboundMapping(
                model.getTagName(),
                model.getTopic(),
                model.getMaxQoS() == null ? DEFAULT_QOS : model.getMaxQoS().ordinal(),
                model.getIncludeTagNames() != null && model.getIncludeTagNames(),
                model.getIncludeTimestamp() == null || model.getIncludeTimestamp(),
                model.getIncludeMetadata() != null && model.getIncludeMetadata(),
                model.getUserProperties() != null
                        ? model.getUserProperties().stream()
                                .map(NorthboundMapping::userProp)
                                .toList()
                        : List.of(),
                model.getMessageExpiryInterval() != null ? model.getMessageExpiryInterval() : DEFAULT_MESSAGE_EXPIRY);
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
        return MessageHandlingOptions.MQTTMessagePerTag;
    }

    public @NotNull Boolean getIncludeTimestamp() {
        return includeTimestamp;
    }

    public @NotNull Boolean getIncludeTagNames() {
        return includeTagNames;
    }

    public @NotNull Boolean getIncludeMetadata() {
        return includeMetadata;
    }

    public @NotNull List<MqttUserProperty> getUserProperties() {
        return userProperties;
    }

    public @Nullable Long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof final NorthboundMapping that) {
            return Objects.equals(tagName, that.tagName)
                    && Objects.equals(topic, that.topic)
                    && maxQoS == that.maxQoS
                    && Objects.equals(includeTagNames, that.includeTagNames)
                    && Objects.equals(includeTimestamp, that.includeTimestamp)
                    && Objects.equals(includeMetadata, that.includeMetadata)
                    && Objects.equals(userProperties, that.userProperties)
                    && Objects.equals(messageExpiryInterval, that.messageExpiryInterval);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tagName, topic, maxQoS, includeTagNames, includeTimestamp, userProperties, messageExpiryInterval);
    }

    @Override
    public @NotNull String toString() {
        return "NorthboundMapping{" + "topic='"
                + topic
                + '\''
                + ", tagName='"
                + tagName
                + "', includeTagNames="
                + includeTagNames
                + ", includeTimestamp="
                + includeTimestamp
                + ", includeMetadata="
                + includeMetadata
                + ", userProperties="
                + userProperties
                + ", maxQoS="
                + maxQoS
                + ", messageExpiryInterval="
                + messageExpiryInterval
                + '}';
    }
}
