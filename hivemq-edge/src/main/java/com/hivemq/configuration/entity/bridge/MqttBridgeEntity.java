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
package com.hivemq.configuration.entity.bridge;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "mqtt-bridge")
@XmlAccessorType(XmlAccessType.NONE)
public class MqttBridgeEntity {

    @XmlElement(name = "id", required = true)
    private @Nullable String id;

    @XmlElementRef
    private @NotNull RemoteBrokerEntity remoteBroker = new RemoteBrokerEntity();

    @XmlElementWrapper(name = "remote-subscriptions", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<RemoteSubscriptionEntity> remoteSubscriptions = new ArrayList<>();

    @XmlElementWrapper(name = "forwarded-topics", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<ForwardedTopicEntity> forwardedTopics = new ArrayList<>();

    @XmlElementRef
    private @NotNull LoopPreventionEntity loopPrevention = new LoopPreventionEntity();

    @XmlElement(name = "persist")
    private boolean persist = true;

    public @NotNull RemoteBrokerEntity getRemoteBroker() {
        return remoteBroker;
    }

    public @NotNull List<RemoteSubscriptionEntity> getRemoteSubscriptions() {
        return remoteSubscriptions;
    }

    public @NotNull List<ForwardedTopicEntity> getForwardedTopics() {
        return forwardedTopics;
    }

    public @Nullable String getId() {
        return id;
    }

    public @NotNull LoopPreventionEntity getLoopPrevention() {
        return loopPrevention;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setRemoteBroker(final RemoteBrokerEntity remoteBroker) {
        this.remoteBroker = remoteBroker;
    }

    public void setRemoteSubscriptions(final List<RemoteSubscriptionEntity> remoteSubscriptions) {
        this.remoteSubscriptions = remoteSubscriptions;
    }

    public void setForwardedTopics(final List<ForwardedTopicEntity> forwardedTopics) {
        this.forwardedTopics = forwardedTopics;
    }

    public void setLoopPrevention(final LoopPreventionEntity loopPrevention) {
        this.loopPrevention = loopPrevention;
    }

    public boolean getPersist() {
        return persist;
    }

    public void setPersist(final boolean persist) {
        this.persist = persist;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final MqttBridgeEntity that = (MqttBridgeEntity) o;
        return getPersist() == that.getPersist() &&
                Objects.equals(getId(), that.getId()) &&
                Objects.equals(getRemoteBroker(), that.getRemoteBroker()) &&
                Objects.equals(getRemoteSubscriptions(), that.getRemoteSubscriptions()) &&
                Objects.equals(getForwardedTopics(), that.getForwardedTopics()) &&
                Objects.equals(getLoopPrevention(), that.getLoopPrevention());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(),
                getRemoteBroker(),
                getRemoteSubscriptions(),
                getForwardedTopics(),
                getLoopPrevention(),
                getPersist());
    }
}
