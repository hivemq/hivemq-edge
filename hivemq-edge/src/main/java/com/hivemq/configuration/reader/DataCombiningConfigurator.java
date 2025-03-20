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
import com.hivemq.configuration.entity.combining.DataCombinerEntity;
import com.hivemq.configuration.service.DataCombiningConfigurationService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DataCombiningConfigurator implements Syncable<List<DataCombinerEntity>> {

    private final @NotNull DataCombiningConfigurationService configurationService;
    private volatile @NotNull List<DataCombinerEntity> configEntity;
    private volatile boolean initialized = false;

    public DataCombiningConfigurator(final @NotNull DataCombiningConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        return initialized && hasChanged(this.configEntity, config.getDataCombinerEntities());
    }

    @Override
    public @NotNull ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getDataCombinerEntities();
        this.initialized = true;

        configurationService.setAllConfigs(new ArrayList<>(configEntity));

        return ConfigResult.SUCCESS;
    }

    @Override
    public void sync(final @NotNull HiveMQConfigEntity config) {
        config.getDataCombinerEntities().clear();
        config.getDataCombinerEntities().addAll(configurationService.getAllConfigs());
    }
}
