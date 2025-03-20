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

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.MqttConfigEntity;
import com.hivemq.configuration.entity.MqttSnConfigEntity;
import com.hivemq.configuration.entity.mqttsn.MqttsnPredefinedTopicAliasEntity;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqttsn.MqttsnTopicAlias;

import java.util.List;

public class MqttsnConfigurator implements Configurator<MqttSnConfigEntity>{

    private final @NotNull MqttsnConfigurationService mqttsnConfigurationService;
    private volatile MqttSnConfigEntity configEntity;
    private volatile boolean initialized = false;

    public MqttsnConfigurator(final @NotNull MqttsnConfigurationService mqttsnConfigurationService) {
        this.mqttsnConfigurationService = mqttsnConfigurationService;
    }

    void setPredefinedTopicAliases(final @NotNull List<MqttsnPredefinedTopicAliasEntity> topicAliases) {
        for (final MqttsnPredefinedTopicAliasEntity a : topicAliases) {
            mqttsnConfigurationService.addPredefinedAlias(new MqttsnTopicAlias(a.getTopicName(), a.getAlias(),
                    MqttsnTopicAlias.TYPE.PREDEFINED));
        }
    }


    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getMqttsnConfig())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getMqttsnConfig();
        this.initialized = true;

        mqttsnConfigurationService.setGatewayId(configEntity.getGatewayId());
        setPredefinedTopicAliases(configEntity.getPredefinedTopicAliases());
        mqttsnConfigurationService.setMaxClientIdentifierLength(configEntity.getMaxClientIdentifierLength());
        mqttsnConfigurationService.setTopicRegistrationsHeldDuringSleepEnabled(configEntity.getTopicRegistrationsHeldDuringSleepEntity().isEnabled());
        mqttsnConfigurationService.setAllowWakingPingToHijackSessionEnabled(configEntity.getAllowWakingPingToHijackSessionEntity().isEnabled());
        mqttsnConfigurationService.setAllowEmptyClientIdentifierEnabled(configEntity.getAllowEmptyClientIdentifierEntity().isEnabled());
        mqttsnConfigurationService.setAllowAnonymousPublishMinus1Enabled(configEntity.getAllowAnonymousPublishMinus1Entity().isEnabled());

        //Discovery
        mqttsnConfigurationService.setDiscoveryEnabled(configEntity.getDiscoveryEntity().isEnabled());
        mqttsnConfigurationService.setDiscoveryBroadcastIntervalSeconds(configEntity.getDiscoveryEntity().getDiscoveryInterval());
        mqttsnConfigurationService.setDiscoveryBroadcastAddresses(configEntity.getDiscoveryEntity().getBroadcastAddresses());

        return ConfigResult.SUCCESS;
    }
}
