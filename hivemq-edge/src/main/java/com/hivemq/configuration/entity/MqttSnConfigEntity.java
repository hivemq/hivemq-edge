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

import com.hivemq.configuration.entity.mqttsn.AllowAnonymousPublishMinus1Entity;
import com.hivemq.configuration.entity.mqttsn.AllowEmptyClientIdentifierEntity;
import com.hivemq.configuration.entity.mqttsn.AllowWakingPingToHijackSessionEntity;
import com.hivemq.configuration.entity.mqttsn.DiscoveryEntity;
import com.hivemq.configuration.entity.mqttsn.MqttsnPredefinedTopicAliasEntity;
import com.hivemq.configuration.entity.mqttsn.TopicRegistrationsHeldDuringSleepEntity;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
@XmlRootElement(name = "mqtt-sn")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class MqttSnConfigEntity {

    @XmlElementWrapper(name = "predefined-topics", required = true)
    @XmlElementRef(required = false)
    private @NotNull List<MqttsnPredefinedTopicAliasEntity> predefinedTopicAliases = new ArrayList<>();

    @XmlElementRef(required = false)
    private @NotNull AllowEmptyClientIdentifierEntity allowEmptyClientIdentifierEntity =
            new AllowEmptyClientIdentifierEntity();

    @XmlElementRef(required = false)
    private @NotNull DiscoveryEntity discoveryEntity = new DiscoveryEntity();

    @XmlElementRef(required = false)
    private @NotNull AllowAnonymousPublishMinus1Entity allowAnonymousPublishMinus1Entity =
            new AllowAnonymousPublishMinus1Entity();

    @XmlElementRef(required = false)
    private @NotNull AllowWakingPingToHijackSessionEntity allowWakingPingToHijackSessionEntity =
            new AllowWakingPingToHijackSessionEntity();

    @XmlElementRef(required = false)
    private @NotNull TopicRegistrationsHeldDuringSleepEntity topicRegistrationsHeldDuringSleepEntity =
            new TopicRegistrationsHeldDuringSleepEntity();

    @XmlElement(name = "max-client-identifier-length", defaultValue = "23")
    private int maxClientIdentifierLength = 23;

    @XmlElement(name = "gateway-id", defaultValue = "1")
    private int gatewayId = 0x01;

    /**
     * Not bound to XML. Set via the JAXB {@link #afterUnmarshal} callback so we can detect whether a
     * {@code <mqtt-sn>} block was actually present in the configuration. MQTT-SN is no longer supported; this is
     * used solely to emit a deprecation warning when the (now obsolete) block is still configured.
     */
    private transient boolean present = false;

    public @NotNull AllowEmptyClientIdentifierEntity getAllowEmptyClientIdentifierEntity() {
        return allowEmptyClientIdentifierEntity;
    }

    public int getMaxClientIdentifierLength() {
        return maxClientIdentifierLength;
    }

    @NotNull
    public List<MqttsnPredefinedTopicAliasEntity> getPredefinedTopicAliases() {
        return predefinedTopicAliases;
    }

    @NotNull
    public AllowAnonymousPublishMinus1Entity getAllowAnonymousPublishMinus1Entity() {
        return allowAnonymousPublishMinus1Entity;
    }

    @NotNull
    public AllowWakingPingToHijackSessionEntity getAllowWakingPingToHijackSessionEntity() {
        return allowWakingPingToHijackSessionEntity;
    }

    @NotNull
    public TopicRegistrationsHeldDuringSleepEntity getTopicRegistrationsHeldDuringSleepEntity() {
        return topicRegistrationsHeldDuringSleepEntity;
    }

    @NotNull
    public DiscoveryEntity getDiscoveryEntity() {
        return discoveryEntity;
    }

    public int getGatewayId() {
        return gatewayId;
    }

    /**
     * JAXB lifecycle callback, invoked after this element has been unmarshalled from XML. Marks the block as present
     * so callers can distinguish a configured (but obsolete) {@code <mqtt-sn>} block from the default instance.
     */
    @SuppressWarnings("unused")
    void afterUnmarshal(final @NotNull Unmarshaller unmarshaller, final @NotNull Object parent) {
        this.present = true;
    }

    /**
     * @return {@code true} if a {@code <mqtt-sn>} block was present in the parsed configuration.
     */
    public boolean isPresent() {
        return present;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MqttSnConfigEntity that)) return false;
        return getMaxClientIdentifierLength() == that.getMaxClientIdentifierLength()
                && getGatewayId() == that.getGatewayId()
                && Objects.equals(getPredefinedTopicAliases(), that.getPredefinedTopicAliases())
                && Objects.equals(getAllowEmptyClientIdentifierEntity(), that.getAllowEmptyClientIdentifierEntity())
                && Objects.equals(getDiscoveryEntity(), that.getDiscoveryEntity())
                && Objects.equals(getAllowAnonymousPublishMinus1Entity(), that.getAllowAnonymousPublishMinus1Entity())
                && Objects.equals(
                        getAllowWakingPingToHijackSessionEntity(), that.getAllowWakingPingToHijackSessionEntity())
                && Objects.equals(
                        getTopicRegistrationsHeldDuringSleepEntity(),
                        that.getTopicRegistrationsHeldDuringSleepEntity());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getPredefinedTopicAliases(),
                getAllowEmptyClientIdentifierEntity(),
                getDiscoveryEntity(),
                getAllowAnonymousPublishMinus1Entity(),
                getAllowWakingPingToHijackSessionEntity(),
                getTopicRegistrationsHeldDuringSleepEntity(),
                getMaxClientIdentifierLength(),
                getGatewayId());
    }
}
