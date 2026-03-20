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
import com.hivemq.configuration.entity.MqttSnConfigEntity;
import com.hivemq.configuration.entity.mqttsn.MqttsnPredefinedTopicAliasEntity;
import com.hivemq.configuration.service.MqttsnConfigurationService;
import com.hivemq.mqttsn.MqttsnTopicAlias;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MqttsnConfigurator implements Configurator<MqttSnConfigEntity> {

    private final @NotNull MqttsnConfigurationService mqttsnConfigurationService;
    private volatile @Nullable MqttSnConfigEntity configEntity;
    private volatile boolean initialized = false;

    public MqttsnConfigurator(final @NotNull MqttsnConfigurationService mqttsnConfigurationService) {
        this.mqttsnConfigurationService = mqttsnConfigurationService;
    }

    void setPredefinedTopicAliases(final @NotNull List<MqttsnPredefinedTopicAliasEntity> topicAliases) {
        for (final MqttsnPredefinedTopicAliasEntity a : topicAliases) {
            mqttsnConfigurationService.addPredefinedAlias(
                    new MqttsnTopicAlias(a.getTopicName(), a.getAlias(), MqttsnTopicAlias.TYPE.PREDEFINED));
        }
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if (initialized && hasChanged(this.configEntity, config.getMqttsnConfig())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getMqttsnConfig();
        this.initialized = true;

        final MqttSnConfigEntity entity = Objects.requireNonNull(this.configEntity);
        mqttsnConfigurationService.setGatewayId(entity.getGatewayId());
        setPredefinedTopicAliases(entity.getPredefinedTopicAliases());
        mqttsnConfigurationService.setMaxClientIdentifierLength(entity.getMaxClientIdentifierLength());
        mqttsnConfigurationService.setTopicRegistrationsHeldDuringSleepEnabled(
                entity.getTopicRegistrationsHeldDuringSleepEntity().isEnabled());
        mqttsnConfigurationService.setAllowWakingPingToHijackSessionEnabled(
                entity.getAllowWakingPingToHijackSessionEntity().isEnabled());
        mqttsnConfigurationService.setAllowEmptyClientIdentifierEnabled(
                entity.getAllowEmptyClientIdentifierEntity().isEnabled());
        mqttsnConfigurationService.setAllowAnonymousPublishMinus1Enabled(
                entity.getAllowAnonymousPublishMinus1Entity().isEnabled());

        // Discovery
        mqttsnConfigurationService.setDiscoveryEnabled(
                entity.getDiscoveryEntity().isEnabled());
        mqttsnConfigurationService.setDiscoveryBroadcastIntervalSeconds(
                entity.getDiscoveryEntity().getDiscoveryInterval());
        mqttsnConfigurationService.setDiscoveryBroadcastAddresses(
                entity.getDiscoveryEntity().getBroadcastAddresses());

        return ConfigResult.SUCCESS;
    }
}
