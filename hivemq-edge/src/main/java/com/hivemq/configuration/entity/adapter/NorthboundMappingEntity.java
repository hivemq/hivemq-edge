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
package com.hivemq.configuration.entity.adapter;

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.configuration.entity.EntityValidatable;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.persistence.mappings.NorthboundMapping;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NorthboundMappingEntity implements EntityValidatable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(NorthboundMappingEntity.class);

    private static final @NotNull MessageHandlingOptions NORTHBOUND_OPTS = MessageHandlingOptions.MQTTMessagePerTag;

    @XmlElement(name = "tagName", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "topic", required = true)
    private final @NotNull String topic;

    @XmlElement(name = "maxQos", required = true)
    private final int maxQoS;

    @XmlElement(name = "messageHandlingOptions", defaultValue = "MQTTMessagePerTag")
    private final @Nullable MessageHandlingOptions messageHandlingOptions;

    @XmlElement(name = "includeTagNames", required = true)
    private final @Nullable Boolean includeTagNames;

    @XmlElement(name = "includeTimestamp", required = true)
    private final @Nullable Boolean includeTimestamp;

    @XmlElementWrapper(name = "mqttUserProperties", required = true)
    @XmlElement(name = "mqttUserProperty")
    private final @NotNull List<MqttUserPropertyEntity> userProperties;

    @XmlElement(name = "messageExpiryInterval", required = true)
    private final @NotNull Long messageExpiryInterval;

    // no-arg constructor for JaxB
    public NorthboundMappingEntity() {
        tagName = "";
        topic = "";
        maxQoS = QoS.AT_LEAST_ONCE.getQosNumber();
        includeTagNames = false;
        includeTimestamp = true;
        userProperties = new ArrayList<>();
        messageExpiryInterval = Long.MAX_VALUE;
        messageHandlingOptions = NORTHBOUND_OPTS;
    }

    public NorthboundMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topic,
            final int maxQoS,
            final @Nullable MessageHandlingOptions ignore,
            final boolean includeTagNames,
            final boolean includeTimestamp,
            final @NotNull List<MqttUserPropertyEntity> userProperties,
            final @Nullable Long messageExpiryInterval) {
        this.tagName = tagName;
        this.topic = topic;
        this.maxQoS = maxQoS;
        log.warn(
                "The 'messageHandlingOptions' property in the 'northboundMapping' configuration is ignored. Always using 'MQTTMessagePerTag' handling.");
        this.messageHandlingOptions = null;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.userProperties = userProperties;
        this.messageExpiryInterval = messageExpiryInterval != null ? messageExpiryInterval : Long.MAX_VALUE;
    }

    public static @NotNull NorthboundMappingEntity fromPersistence(final @NotNull NorthboundMapping mapping) {
        return new NorthboundMappingEntity(
                mapping.getTagName(),
                mapping.getMqttTopic(),
                mapping.getMqttQos(),
                NORTHBOUND_OPTS,
                mapping.getIncludeTagNames(),
                mapping.getIncludeTimestamp(),
                mapping.getUserProperties().stream()
                        .map(NorthboundMappingEntity::userProp)
                        .toList(),
                mapping.getMessageExpiryInterval());
    }

    private static @NotNull MqttUserPropertyEntity userProp(final @NotNull MqttUserProperty p) {
        return new MqttUserPropertyEntity(p.getName(), p.getValue());
    }

    private static @NotNull MqttUserPropertyEntity userProp(
            final @NotNull com.hivemq.edge.api.model.MqttUserProperty p) {
        return new MqttUserPropertyEntity(p.getName(), p.getValue());
    }

    public static @NotNull NorthboundMappingEntity fromApi(
            final @NotNull com.hivemq.edge.api.model.NorthboundMapping mapping) {
        return new NorthboundMappingEntity(
                mapping.getTagName(),
                mapping.getTopic(),
                switch (mapping.getMaxQoS()) {
                    case AT_MOST_ONCE -> 0;
                    case AT_LEAST_ONCE -> 1;
                    case EXACTLY_ONCE -> 2;
                },
                NORTHBOUND_OPTS,
                mapping.getIncludeTagNames(),
                mapping.getIncludeTimestamp(),
                mapping.getUserProperties().stream()
                        .map(NorthboundMappingEntity::userProp)
                        .toList(),
                mapping.getMessageExpiryInterval());
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return NORTHBOUND_OPTS;
    }

    public boolean isIncludeTagNames() {
        return includeTagNames != null && includeTagNames;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp == null || includeTimestamp; // default is true
    }

    public @NotNull List<MqttUserPropertyEntity> getUserProperties() {
        return userProperties;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    @Override
    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        EntityValidatable.notEmpty(validationEvents, topic, "topic");
        EntityValidatable.notEmpty(validationEvents, tagName, "tagName");
        EntityValidatable.notMatch(
                validationEvents, () -> QoS.valueOf(maxQoS) != null, () -> "maxQos" + ' ' + maxQoS + " is invalid");
        EntityValidatable.notNull(validationEvents, messageHandlingOptions, "messageHandlingOptions");
        EntityValidatable.notNull(validationEvents, includeTagNames, "includeTagNames");
        EntityValidatable.notNull(validationEvents, includeTimestamp, "includeTimestamp");
        if (EntityValidatable.notNull(validationEvents, messageExpiryInterval, "messageExpiryInterval")) {
            EntityValidatable.notMatch(
                    validationEvents,
                    () -> messageExpiryInterval > 0,
                    () -> "messageExpiryInterval " + messageExpiryInterval + " is not greater than 0");
        }
    }

    public @NotNull NorthboundMapping toPersistence() {
        return new NorthboundMapping(
                tagName,
                topic,
                maxQoS,
                includeTagNames,
                includeTimestamp,
                userProperties.stream()
                        .map(p -> new MqttUserProperty(p.getName(), p.getValue()))
                        .toList(),
                messageExpiryInterval);
    }

    @Override
    public @NotNull String toString() {
        return "FromEdgeMappingEntity{" + "topic='"
                + topic
                + '\''
                + ", tagName='"
                + tagName
                + '\''
                + ", maxQoS="
                + maxQoS
                + ", includeTagNames="
                + includeTagNames
                + ", includeTimestamp="
                + includeTimestamp
                + ", userProperties="
                + userProperties
                + ", messageExpiryInterval="
                + messageExpiryInterval
                + '}';
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (o instanceof final NorthboundMappingEntity that) {
            return Objects.equals(tagName, that.tagName)
                    && Objects.equals(topic, that.topic)
                    && maxQoS == that.maxQoS
                    && Objects.equals(messageHandlingOptions, that.messageHandlingOptions)
                    && Objects.equals(includeTagNames, that.includeTagNames)
                    && Objects.equals(includeTimestamp, that.includeTimestamp)
                    && Objects.equals(userProperties, that.userProperties)
                    && Objects.equals(messageExpiryInterval, that.messageExpiryInterval);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                tagName,
                topic,
                maxQoS,
                messageHandlingOptions,
                includeTagNames,
                includeTimestamp,
                userProperties,
                messageExpiryInterval);
    }
}
