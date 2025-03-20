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
import com.hivemq.configuration.entity.RestrictionsEntity;
import com.hivemq.configuration.entity.UsageTrackingConfigEntity;
import com.hivemq.configuration.service.UsageTrackingConfigurationService;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

public class UsageTrackingConfigurator implements Configurator<UsageTrackingConfigEntity> {

    private final @NotNull UsageTrackingConfigurationService usageTrackingConfigurationService;

    private volatile UsageTrackingConfigEntity configEntity;
    private volatile boolean initialized = false;

    @Inject
    public UsageTrackingConfigurator(final @NotNull UsageTrackingConfigurationService usageTrackingConfigurationService) {
        this.usageTrackingConfigurationService = usageTrackingConfigurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getUsageTracking())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult applyConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getUsageTracking();
        this.initialized = true;

        usageTrackingConfigurationService.setTrackingEnabled(
                configEntity.isEnabled());

        return ConfigResult.SUCCESS;
    }
}
