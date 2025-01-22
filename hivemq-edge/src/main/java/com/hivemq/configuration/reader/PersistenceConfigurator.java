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
import com.hivemq.configuration.entity.PersistenceEntity;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.PersistenceMode;
import org.jetbrains.annotations.NotNull;

/**
 * @author Lukas Brandl
 */
public class PersistenceConfigurator implements Configurator<PersistenceEntity>{

    @NotNull
    private final PersistenceConfigurationService persistenceConfigurationService;

    private volatile PersistenceEntity configEntity;
    private volatile boolean initialized = false;

    public PersistenceConfigurator(final @NotNull PersistenceConfigurationService persistenceConfigurationService) {
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getPersistenceConfig())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult setConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getPersistenceConfig();
        this.initialized = true;

        persistenceConfigurationService.setMode(PersistenceMode.valueOf(
                configEntity.getMode().name()));

        return ConfigResult.SUCCESS;
    }
}
