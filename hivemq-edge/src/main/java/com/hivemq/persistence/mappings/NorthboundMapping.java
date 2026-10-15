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

    // There is deliberately NO method-call setting on a northbound mapping at all, and that is the whole shape of
    // the design rather than an omission in one place.
    //
    // Southbound flags decide what Edge gathers into a request's `context`: whether this mapping carries method
    // calls, whether a correlation token is read, whether a caller may name its own reply address. Northbound then
    // acts on what the context contains and nothing else -- a method tag is one whose context declares
    // `methodName`, a response goes to the address the context carries and to getMqttTopic() when it carries none,
    // the token is written into both the payload and the MQTT 5 Correlation Data property, and `errorCode` and
    // `statusInfo` appear when a failure put them there.
    //
    // Two flags lived here and both were removed for the same reason: they offered a choice already made
    // elsewhere. A response-topic flag could only disagree with the southbound whitelist, which decides whether an
    // address reaches the adapter in the first place. And a write-correlation-data flag withheld the token from the
    // payload while the MQTT 5 property carried it anyway -- a distinction the per-subscriber publish encoder
    // already makes correctly, since a version-5 client gets the property and a version-3 client cannot.

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

    /**
     * Compares every field, which has to stay true of this method for a configuration edit to take effect.
     * <p>
     * <b>A field this method forgets is a field a user can change with nothing happening.</b> An edit restarts
     * an adapter only when the new mapping compares unequal to the running one, so an unconsidered field is
     * stored, compared by the persisted entity, and never reaches the adapter. That was a live gap until
     * {@code FieldMapping} gained value equality: before then the southbound comparison could never return
     * true, every edit restarted the adapter, and the gap was invisible (Miguel's M2 on PR 1765).
     */
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

    /**
     * Every field {@link #equals} compares, which previously was not true: {@code includeMetadata} was compared
     * and not hashed. Two mappings differing only in it were unequal with equal hash codes -- legal, but it
     * degrades any hash-based lookup and is the kind of drift that appears when the two are edited separately.
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                tagName,
                topic,
                maxQoS,
                includeTagNames,
                includeTimestamp,
                includeMetadata,
                userProperties,
                messageExpiryInterval);
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
