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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slj.mqtt.sn.MqttsnConstants;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

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
    private @NotNull AllowEmptyClientIdentifierEntity
            allowEmptyClientIdentifierEntity = new AllowEmptyClientIdentifierEntity();

    @XmlElementRef(required = false)
    private @NotNull DiscoveryEntity discoveryEntity = new DiscoveryEntity();

    @XmlElementRef(required = false)
    private @NotNull AllowAnonymousPublishMinus1Entity
            allowAnonymousPublishMinus1Entity = new AllowAnonymousPublishMinus1Entity();

    @XmlElementRef(required = false)
    private @NotNull AllowWakingPingToHijackSessionEntity
            allowWakingPingToHijackSessionEntity = new AllowWakingPingToHijackSessionEntity();

    @XmlElementRef(required = false)
    private @NotNull TopicRegistrationsHeldDuringSleepEntity
            topicRegistrationsHeldDuringSleepEntity = new TopicRegistrationsHeldDuringSleepEntity();

    @XmlElement(name = "max-client-identifier-length", defaultValue = "23")
    private int maxClientIdentifierLength = MqttsnConstants.MAX_CLIENT_ID_LENGTH_v12;

    @XmlElement(name = "gateway-id", defaultValue = "1")
    private int gatewayId = 0x01;

    public @NotNull AllowEmptyClientIdentifierEntity getAllowEmptyClientIdentifierEntity() {
        return allowEmptyClientIdentifierEntity;
    }

    public int getMaxClientIdentifierLength() {
        return maxClientIdentifierLength;
    }

    @NotNull public List<MqttsnPredefinedTopicAliasEntity> getPredefinedTopicAliases() {
        return predefinedTopicAliases;
    }

    @NotNull public AllowAnonymousPublishMinus1Entity getAllowAnonymousPublishMinus1Entity() {
        return allowAnonymousPublishMinus1Entity;
    }

    @NotNull public AllowWakingPingToHijackSessionEntity getAllowWakingPingToHijackSessionEntity() {
        return allowWakingPingToHijackSessionEntity;
    }

    @NotNull public TopicRegistrationsHeldDuringSleepEntity getTopicRegistrationsHeldDuringSleepEntity() {
        return topicRegistrationsHeldDuringSleepEntity;
    }

    @NotNull public DiscoveryEntity getDiscoveryEntity() {
        return discoveryEntity;
    }

    public int getGatewayId() {
        return gatewayId;
    }
}
