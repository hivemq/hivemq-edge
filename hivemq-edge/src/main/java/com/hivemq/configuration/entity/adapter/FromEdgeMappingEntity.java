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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.mappings.FromEdgeMapping;
import com.hivemq.persistence.mappings.fieldmapping.FieldMapping;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.helpers.ValidationEventImpl;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FromEdgeMappingEntity {

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

    @XmlElement(name = "fieldMapping")
    private final @Nullable FieldMappingEntity fieldMapping;

    // no-arg constructor for JaxB
    public FromEdgeMappingEntity() {
        topic = "";
        tagName = "";
        messageHandlingOptions = MessageHandlingOptions.MQTTMessagePerTag;
        includeTagNames = false;
        includeTimestamp = true;
        maxQoS = 1;
        userProperties = new ArrayList<>();
        messageExpiryInterval = Long.MAX_VALUE;
        fieldMapping = null;
    }

    public FromEdgeMappingEntity(
            @NotNull final String tagName,
            @NotNull final String topic,
            final int maxQoS,
            final @NotNull MessageHandlingOptions messageHandlingOptions,
            final boolean includeTagNames,
            final boolean includeTimestamp,
            final @NotNull List<MqttUserPropertyEntity> userProperties,
            final long messageExpiryInterval,
            final @Nullable FieldMappingEntity fieldMapping) {
        this.tagName = tagName;
        this.topic = topic;
        this.maxQoS = maxQoS;
        this.messageHandlingOptions = messageHandlingOptions;
        this.includeTagNames = includeTagNames;
        this.includeTimestamp = includeTimestamp;
        this.userProperties = userProperties;
        this.messageExpiryInterval = messageExpiryInterval;
        this.fieldMapping = fieldMapping;
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

    public @Nullable FieldMappingEntity getFieldMapping() {
        return fieldMapping;
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

    public static @NotNull FromEdgeMappingEntity from(final @NotNull FromEdgeMapping fromEdgeMapping) {
        final List<MqttUserPropertyEntity> mqttUserPropertyEntities = fromEdgeMapping.getUserProperties()
                .stream()
                .map(mqttUserProperty -> new MqttUserPropertyEntity(mqttUserProperty.getName(),
                        mqttUserProperty.getValue()))
                .collect(Collectors.toList());

        return new FromEdgeMappingEntity(
                fromEdgeMapping.getTagName(),
                fromEdgeMapping.getMqttTopic(),
                fromEdgeMapping.getMqttQos(),
                fromEdgeMapping.getMessageHandlingOptions(),
                fromEdgeMapping.getIncludeTagNames(),
                fromEdgeMapping.getIncludeTimestamp(),
                mqttUserPropertyEntities,
                fromEdgeMapping.getMessageExpiryInterval(),
                FieldMappingEntity.from(fromEdgeMapping.getFieldMapping()));
    }

    public @NotNull FromEdgeMapping toFromEdgeMapping(ObjectMapper mapper) {
        final List<MqttUserProperty> mqttUserProperties = this.getUserProperties()
                .stream()
                .map(mqttUserPropertyEntity -> new MqttUserProperty(mqttUserPropertyEntity.getName(),
                        mqttUserPropertyEntity.getValue()))
                .collect(Collectors.toList());

        final FieldMapping fieldMapping = this.getFieldMapping() != null ? this.getFieldMapping().to(mapper) : null;

        return new FromEdgeMapping(
                this.getTagName(),
                this.getTopic(),
                this.getMaxQoS(),
                this.getMessageExpiryInterval(),
                this.getMessageHandlingOptions(),
                this.isIncludeTagNames(),
                this.isIncludeTimestamp(),
                mqttUserProperties,
                fieldMapping);
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
                ", fieldMapping=" +
                fieldMapping +
                '}';
    }

    public static @NotNull FromEdgeMappingEntity fromPollingContext(PollingContext ctx) {
        final List<MqttUserPropertyEntity> mqttUserProperties = ctx.getUserProperties()
                .stream()
                .map(mqttUserPropertyEntity -> new MqttUserPropertyEntity(mqttUserPropertyEntity.getName(),
                        mqttUserPropertyEntity.getValue()))
                .collect(Collectors.toList());

        return new FromEdgeMappingEntity(
                ctx.getTagName(),
                ctx.getMqttTopic(),
                ctx.getMqttQos(),
                ctx.getMessageHandlingOptions(),
                ctx.getIncludeTagNames(),
                ctx.getIncludeTimestamp(),
                mqttUserProperties,
                ctx.getMessageExpiryInterval(),
                null);
    }
}
