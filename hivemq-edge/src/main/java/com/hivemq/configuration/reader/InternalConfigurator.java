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
import com.hivemq.configuration.entity.InternalConfigEntity;
import com.hivemq.configuration.entity.OptionEntity;
import com.hivemq.configuration.entity.bridge.MqttBridgeEntity;
import com.hivemq.configuration.service.InternalConfigurationService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class InternalConfigurator implements Configurator<InternalConfigEntity> {

    private final @NotNull InternalConfigurationService internalConfigurationService;

    private volatile InternalConfigEntity configEntity;
    private volatile boolean initialized = false;

    public InternalConfigurator(final @NotNull InternalConfigurationService internalConfigurationService) {
        this.internalConfigurationService = internalConfigurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getInternal())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult setConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getInternal();
        this.initialized = true;

        for (final OptionEntity optionEntity : configEntity.getOptions()) {
            internalConfigurationService.set(optionEntity.getKey(), optionEntity.getValue());
        }

        return ConfigResult.SUCCESS;
    }
}
