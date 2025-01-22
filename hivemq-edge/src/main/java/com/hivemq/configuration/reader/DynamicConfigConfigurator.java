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

import com.hivemq.configuration.entity.DynamicConfigEntity;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.bridge.MqttBridgeEntity;
import com.hivemq.configuration.service.DynamicConfigurationService;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;

public class DynamicConfigConfigurator implements Configurator<DynamicConfigEntity> {

    private final @NotNull DynamicConfigurationService dynamicConfigService;

    private volatile DynamicConfigEntity configEntity;
    private volatile boolean initialized = false;

    @Inject
    public DynamicConfigConfigurator(final @NotNull DynamicConfigurationService dynamicConfigService) {
        this.dynamicConfigService = dynamicConfigService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getGatewayConfig())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult setConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getGatewayConfig();
        this.initialized = true;

        dynamicConfigService.setConfigurationExportEnabled(
                configEntity.isConfigurationExportEnabled());

        dynamicConfigService.setMutableConfigurationEnabled(
                configEntity.isMutableConfigurationEnabled());

        return ConfigResult.SUCCESS;
    }
}
