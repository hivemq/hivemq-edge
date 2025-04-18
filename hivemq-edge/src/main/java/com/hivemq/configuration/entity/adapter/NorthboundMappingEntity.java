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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.persistence.mappings.NorthboundMapping;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.helpers.ValidationEventImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NorthboundMappingEntity {

    @XmlElement(name = "topic", required = true)
    private final @NotNull String topic;

    @XmlElement(name = "tagName", required = true)
    private final @NotNull String tagName;

    @XmlElement(name = "maxQos", required = true)
    private final int maxQoS;

    @XmlElement(name = "messageHandlingOptions", required = true)
    private final @NotNull MessageHandlingOptions messageHandlingOptions;

    @XmlElement(name = "includeTagNames", required = true)
    private final boolean includeTagNames;

    @XmlElement(name = "includeTimestamp", required = true)
    private final boolean includeTimestamp;

    @XmlElementWrapper(name = "mqttUserProperties", required = true)
    @XmlElement(name = "mqttUserProperty")
    private final @NotNull List<MqttUserPropertyEntity> userProperties;

    @XmlElement(name = "messageExpiryInterval", required = true)
    private final @NotNull long messageExpiryInterval;

    // no-arg constructor for JaxB
    public NorthboundMappingEntity() {
        topic = "";
        tagName = "";
        messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;
        includeTagNames = false;
        includeTimestamp = true;
        maxQoS = 1;
        userProperties = new ArrayList<>();
        messageExpiryInterval = Long.MAX_VALUE;
    }

    public NorthboundMappingEntity(
            final @NotNull String tagName,
            final @NotNull String topic,
            final int maxQoS,
            final @NotNull MessageHandlingOptions messageHandlingOptions,
            final boolean includeTagNames,
            final boolean includeTimestamp,
            final @NotNull List<MqttUserPropertyEntity> userProperties,
            final long messageExpiryInterval) {
        this.tagName = tagName;
        this.topic = topic;
        this.maxQoS = maxQoS;
        this.messageHandlingOptions = messageHandlingOptions;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.userProperties = userProperties;
        this.messageExpiryInterval = messageExpiryInterval;;
    }

    public @NotNull String getTagName() {
        return tagName;
    }

    public @NotNull String getTopic() {
        return topic;
    }

    public @NotNull MessageHandlingOptions getMessageHandlingOptions() {
        return messageHandlingOptions;
    }

    public boolean isIncludeTagNames() {
        return includeTagNames;
    }

    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }

    public @NotNull List<MqttUserPropertyEntity> getUserProperties() {
        return userProperties;
    }

    public int getMaxQoS() {
        return maxQoS;
    }

    @NotNull public long getMessageExpiryInterval() {
        return messageExpiryInterval;
    }

    public void validate(final @NotNull List<ValidationEvent> validationEvents) {
        if (topic == null || topic.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "topic is missing", null));
        }
        if (tagName == null || tagName.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR, "tagName is missing", null));
        }
    }

    public static @NotNull NorthboundMappingEntity fromPersistence(final @NotNull NorthboundMapping northboundMapping) {
        final List<MqttUserPropertyEntity> mqttUserPropertyEntities = northboundMapping.getUserProperties()
                .stream()
                .map(mqttUserProperty -> new MqttUserPropertyEntity(mqttUserProperty.getName(),
                        mqttUserProperty.getValue()))
                .collect(Collectors.toList());

        return new NorthboundMappingEntity(
                northboundMapping.getTagName(),
                northboundMapping.getMqttTopic(),
                northboundMapping.getMqttQos(),
                northboundMapping.getMessageHandlingOptions(),
                northboundMapping.getIncludeTagNames(),
                northboundMapping.getIncludeTimestamp(),
                mqttUserPropertyEntities,
                northboundMapping.getMessageExpiryInterval());
    }

    public static @NotNull NorthboundMappingEntity fromApi(final @NotNull com.hivemq.edge.api.model.NorthboundMapping northboundMapping) {
        final List<MqttUserPropertyEntity> mqttUserPropertyEntities = northboundMapping.getUserProperties()
                .stream()
                .map(mqttUserProperty -> new MqttUserPropertyEntity(mqttUserProperty.getName(),
                        mqttUserProperty.getValue()))
                .collect(Collectors.toList());

        final var qos = switch (northboundMapping.getMaxQoS()) {
            case AT_MOST_ONCE -> 0;
            case AT_LEAST_ONCE -> 1;
            case EXACTLY_ONCE -> 2;
        };

        return new NorthboundMappingEntity(
                northboundMapping.getTagName(),
                northboundMapping.getTopic(),
                qos,
                MessageHandlingOptions.MQTTMessagePerTag,
                northboundMapping.getIncludeTagNames(),
                northboundMapping.getIncludeTimestamp(),
                mqttUserPropertyEntities,
                northboundMapping.getMessageExpiryInterval() != null ? northboundMapping.getMessageExpiryInterval() : Long.MAX_VALUE);
    }

    public @NotNull NorthboundMapping toPersistence() {
        final List<MqttUserProperty> mqttUserProperties = this.getUserProperties()
                .stream()
                .map(mqttUserPropertyEntity -> new MqttUserProperty(mqttUserPropertyEntity.getName(),
                        mqttUserPropertyEntity.getValue()))
                .collect(Collectors.toList());
        return new NorthboundMapping(
                this.getTagName(),
                this.getTopic(),
                this.getMaxQoS(),
                this.getMessageExpiryInterval(),
                this.getMessageHandlingOptions(),
                this.isIncludeTagNames(),
                this.isIncludeTimestamp(),
                mqttUserProperties);
    }

    public @NotNull com.hivemq.edge.api.model.NorthboundMapping toApi() {
        final List<com.hivemq.edge.api.model.MqttUserProperty> mqttUserProperties = this.getUserProperties()
                .stream()
                .map(mqttUserPropertyEntity ->
                        (com.hivemq.edge.api.model.MqttUserProperty) com.hivemq.edge.api.model.MqttUserProperty.builder()
                            .name(mqttUserPropertyEntity.getName())
                            .value(mqttUserPropertyEntity.getValue())
                        .build())
                .toList();

        ;
        com.hivemq.edge.api.model.QoS maxQos = switch (this.getMaxQoS()) {
            case 0 -> com.hivemq.edge.api.model.QoS.AT_MOST_ONCE;
            case 1 -> com.hivemq.edge.api.model.QoS.AT_LEAST_ONCE;
            case 2 -> com.hivemq.edge.api.model.QoS.EXACTLY_ONCE;
            default -> com.hivemq.edge.api.model.QoS.AT_MOST_ONCE;
        };

        return com.hivemq.edge.api.model.NorthboundMapping
                .builder()
                .tagName(this.getTagName())
                .topic(this.getTopic())
                .maxQoS(maxQos)
                .messageExpiryInterval(this.getMessageExpiryInterval())
                .includeTagNames(this.isIncludeTagNames())
                .includeTimestamp(this.isIncludeTimestamp())
                .userProperties(mqttUserProperties)
                .build();
    }

    @Override
    public String toString() {
        return "FromEdgeMappingEntity{" +
                "topic='" +
                topic +
                '\'' +
                ", tagName='" +
                tagName +
                '\'' +
                ", maxQoS=" +
                maxQoS +
                ", messageHandlingOptions=" +
                messageHandlingOptions +
                ", includeTagNames=" +
                includeTagNames +
                ", includeTimestamp=" +
                includeTimestamp +
                ", userProperties=" +
                userProperties +
                ", messageExpiryInterval=" +
                messageExpiryInterval +
                '}';
    }

    public static @NotNull NorthboundMappingEntity fromPollingContext(PollingContext ctx) {
        final List<MqttUserPropertyEntity> mqttUserProperties = ctx.getUserProperties()
                .stream()
                .map(mqttUserPropertyEntity -> new MqttUserPropertyEntity(mqttUserPropertyEntity.getName(),
                        mqttUserPropertyEntity.getValue()))
                .collect(Collectors.toList());

        return new NorthboundMappingEntity(
                ctx.getTagName(),
                ctx.getMqttTopic(),
                ctx.getMqttQos(),
                ctx.getMessageHandlingOptions(),
                ctx.getIncludeTagNames(),
                ctx.getIncludeTimestamp(),
                mqttUserProperties,
                ctx.getMessageExpiryInterval());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final NorthboundMappingEntity that = (NorthboundMappingEntity) o;
        return getMaxQoS() == that.getMaxQoS() &&
                isIncludeTagNames() == that.isIncludeTagNames() &&
                isIncludeTimestamp() == that.isIncludeTimestamp() &&
                getMessageExpiryInterval() == that.getMessageExpiryInterval() &&
                Objects.equals(getTopic(), that.getTopic()) &&
                Objects.equals(getTagName(), that.getTagName()) &&
                getMessageHandlingOptions() == that.getMessageHandlingOptions() &&
                Objects.equals(getUserProperties(), that.getUserProperties());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTopic(),
                getTagName(),
                getMaxQoS(),
                getMessageHandlingOptions(),
                isIncludeTagNames(),
                isIncludeTimestamp(),
                getUserProperties(),
                getMessageExpiryInterval());
    }
}
