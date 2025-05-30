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
package com.hivemq.configuration.entity;

import com.hivemq.configuration.entity.mqtt.*;
import org.jetbrains.annotations.NotNull;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * @author Dominik Obermaier
 */
@XmlRootElement(name = "mqtt")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class MqttConfigEntity {

    @XmlElementRef(required = false)
    private @NotNull QueuedMessagesConfigEntity queuedMessagesConfigEntity = new QueuedMessagesConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull RetainedMessagesConfigEntity retainedMessagesConfigEntity = new RetainedMessagesConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull WildcardSubscriptionsConfigEntity wildcardSubscriptionsConfigEntity = new WildcardSubscriptionsConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull QoSConfigEntity qoSConfigEntity = new QoSConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull TopicAliasConfigEntity topicAliasConfigEntity = new TopicAliasConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull MessageExpiryConfigEntity messageExpiryConfigEntity = new MessageExpiryConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull SessionExpiryConfigEntity sessionExpiryConfigEntity = new SessionExpiryConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull SubscriptionIdentifierConfigEntity subscriptionIdentifierConfigEntity = new SubscriptionIdentifierConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull SharedSubscriptionsConfigEntity sharedSubscriptionsConfigEntity = new SharedSubscriptionsConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull KeepAliveConfigEntity keepAliveConfigEntity = new KeepAliveConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull PacketsConfigEntity packetsConfigEntity = new PacketsConfigEntity();

    @XmlElementRef(required = false)
    private @NotNull ReceiveMaximumConfigEntity receiveMaximumConfigEntity = new ReceiveMaximumConfigEntity();

    public @NotNull KeepAliveConfigEntity getKeepAliveConfigEntity() {
        return keepAliveConfigEntity;
    }

    public @NotNull PacketsConfigEntity getPacketsConfigEntity() {
        return packetsConfigEntity;
    }

    public @NotNull ReceiveMaximumConfigEntity getReceiveMaximumConfigEntity() {
        return receiveMaximumConfigEntity;
    }

    public @NotNull QueuedMessagesConfigEntity getQueuedMessagesConfigEntity() {
        return queuedMessagesConfigEntity;
    }

    public @NotNull RetainedMessagesConfigEntity getRetainedMessagesConfigEntity() {
        return retainedMessagesConfigEntity;
    }

    public @NotNull WildcardSubscriptionsConfigEntity getWildcardSubscriptionsConfigEntity() {
        return wildcardSubscriptionsConfigEntity;
    }

    public @NotNull QoSConfigEntity getQoSConfigEntity() {
        return qoSConfigEntity;
    }

    public @NotNull TopicAliasConfigEntity getTopicAliasConfigEntity() {
        return topicAliasConfigEntity;
    }

    public @NotNull MessageExpiryConfigEntity getMessageExpiryConfigEntity() {
        return messageExpiryConfigEntity;
    }

    public @NotNull SessionExpiryConfigEntity getSessionExpiryConfigEntity() {
        return sessionExpiryConfigEntity;
    }

    public @NotNull SubscriptionIdentifierConfigEntity getSubscriptionIdentifierConfigEntity() {
        return subscriptionIdentifierConfigEntity;
    }

    public @NotNull SharedSubscriptionsConfigEntity getSharedSubscriptionsConfigEntity() {
        return sharedSubscriptionsConfigEntity;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MqttConfigEntity that = (MqttConfigEntity) o;
        return Objects.equals(getQueuedMessagesConfigEntity(), that.getQueuedMessagesConfigEntity()) &&
                Objects.equals(getRetainedMessagesConfigEntity(), that.getRetainedMessagesConfigEntity()) &&
                Objects.equals(getWildcardSubscriptionsConfigEntity(), that.getWildcardSubscriptionsConfigEntity()) &&
                Objects.equals(getQoSConfigEntity(), that.getQoSConfigEntity()) &&
                Objects.equals(getTopicAliasConfigEntity(), that.getTopicAliasConfigEntity()) &&
                Objects.equals(getMessageExpiryConfigEntity(), that.getMessageExpiryConfigEntity()) &&
                Objects.equals(getSessionExpiryConfigEntity(), that.getSessionExpiryConfigEntity()) &&
                Objects.equals(getSubscriptionIdentifierConfigEntity(), that.getSubscriptionIdentifierConfigEntity()) &&
                Objects.equals(getSharedSubscriptionsConfigEntity(), that.getSharedSubscriptionsConfigEntity()) &&
                Objects.equals(getKeepAliveConfigEntity(), that.getKeepAliveConfigEntity()) &&
                Objects.equals(getPacketsConfigEntity(), that.getPacketsConfigEntity()) &&
                Objects.equals(getReceiveMaximumConfigEntity(), that.getReceiveMaximumConfigEntity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQueuedMessagesConfigEntity(),
                getRetainedMessagesConfigEntity(),
                getWildcardSubscriptionsConfigEntity(),
                getQoSConfigEntity(),
                getTopicAliasConfigEntity(),
                getMessageExpiryConfigEntity(),
                getSessionExpiryConfigEntity(),
                getSubscriptionIdentifierConfigEntity(),
                getSharedSubscriptionsConfigEntity(),
                getKeepAliveConfigEntity(),
                getPacketsConfigEntity(),
                getReceiveMaximumConfigEntity());
    }
}
