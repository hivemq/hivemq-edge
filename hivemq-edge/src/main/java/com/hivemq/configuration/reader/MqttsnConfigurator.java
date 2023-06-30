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

package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.MqttSnConfigEntity;
import com.hivemq.configuration.entity.mqttsn.MqttsnPredefinedTopicAliasEntity;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqttsn.MqttsnTopicAlias;

import java.util.List;

public class MqttsnConfigurator {

    private final @NotNull MqttsnConfigurationService mqttsnConfigurationService;

    public MqttsnConfigurator(final @NotNull MqttsnConfigurationService mqttsnConfigurationService) {
        this.mqttsnConfigurationService = mqttsnConfigurationService;
    }

    void setPredefinedTopicAliases(final @NotNull List<MqttsnPredefinedTopicAliasEntity> topicAliases) {
        for (final MqttsnPredefinedTopicAliasEntity a : topicAliases) {
            mqttsnConfigurationService.addPredefinedAlias(new MqttsnTopicAlias(a.getTopicName(), a.getAlias(),
                    MqttsnTopicAlias.TYPE.PREDEFINED));
        }
    }

    void setMqttsnConfig(@NotNull final MqttSnConfigEntity mqttsnConfig) {
        mqttsnConfigurationService.setGatewayId(mqttsnConfig.getGatewayId());
        setPredefinedTopicAliases(mqttsnConfig.getPredefinedTopicAliases());
        mqttsnConfigurationService.setMaxClientIdentifierLength(mqttsnConfig.getMaxClientIdentifierLength());
        mqttsnConfigurationService.setTopicRegistrationsHeldDuringSleepEnabled(mqttsnConfig.getTopicRegistrationsHeldDuringSleepEntity().isEnabled());
        mqttsnConfigurationService.setAllowWakingPingToHijackSessionEnabled(mqttsnConfig.getAllowWakingPingToHijackSessionEntity().isEnabled());
        mqttsnConfigurationService.setAllowEmptyClientIdentifierEnabled(mqttsnConfig.getAllowEmptyClientIdentifierEntity().isEnabled());
        mqttsnConfigurationService.setAllowAnonymousPublishMinus1Enabled(mqttsnConfig.getAllowAnonymousPublishMinus1Entity().isEnabled());

        //Discovery
        mqttsnConfigurationService.setDiscoveryEnabled(mqttsnConfig.getDiscoveryEntity().isEnabled());
        mqttsnConfigurationService.setDiscoveryBroadcastIntervalSeconds(mqttsnConfig.getDiscoveryEntity().getDiscoveryInterval());
        mqttsnConfigurationService.setDiscoveryBroadcastAddresses(mqttsnConfig.getDiscoveryEntity().getBroadcastAddresses());
    }
}
