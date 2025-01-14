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

import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.service.ProtocolAdapterConfigurationService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ProtocolAdapterConfigurator implements Syncable<List<ProtocolAdapterEntity>> {

    private final @NotNull ProtocolAdapterConfigurationService configurationService;

    private volatile List<ProtocolAdapterEntity> configEntity;
    private volatile boolean initialized = false;

    public ProtocolAdapterConfigurator(final @NotNull ProtocolAdapterConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public ConfigResult setConfig(final @NotNull List<ProtocolAdapterEntity> configEntity) {
        if(initialized && hasChanged(this.configEntity, configEntity)) {
            return ConfigResult.NEEDS_RESTART;
        }
        this.configEntity = configEntity;
        this.initialized = true;

        configurationService.setAllConfigs(new ArrayList<>(configEntity));

        return ConfigResult.SUCCESS;
    }

    @Override
    public void sync(final @NotNull List<ProtocolAdapterEntity> config) {
        config.clear();
        config.addAll(configurationService.getAllConfigs());
    }
}
