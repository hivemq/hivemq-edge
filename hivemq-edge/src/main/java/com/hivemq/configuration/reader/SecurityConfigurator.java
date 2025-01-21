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
import com.hivemq.configuration.entity.SecurityConfigEntity;
import com.hivemq.configuration.service.SecurityConfigurationService;
import org.jetbrains.annotations.NotNull;

public class SecurityConfigurator implements Configurator<SecurityConfigEntity>{

    protected final @NotNull SecurityConfigurationService securityConfigurationService;

    private volatile SecurityConfigEntity configEntity;
    private volatile boolean initialized = false;

    public SecurityConfigurator(final @NotNull SecurityConfigurationService securityConfigurationService) {
        this.securityConfigurationService = securityConfigurationService;
    }

    @Override
    public boolean needsRestartWithConfig(final HiveMQConfigEntity config) {
        if(initialized && hasChanged(this.configEntity, config.getSecurityConfig())) {
            return true;
        }
        return false;
    }

    @Override
    public ConfigResult setConfig(final @NotNull HiveMQConfigEntity config) {
        this.configEntity = config.getSecurityConfig();
        this.initialized = true;

        securityConfigurationService.setAllowServerAssignedClientId(configEntity.getAllowEmptyClientIdEntity().isEnabled());
        securityConfigurationService.setValidateUTF8(configEntity.getUtf8ValidationEntity().isEnabled());
        securityConfigurationService.setPayloadFormatValidation(configEntity.getPayloadFormatValidationEntity().isEnabled());
        securityConfigurationService.setAllowRequestProblemInformation(configEntity.getAllowRequestProblemInformationEntity().isEnabled());

        return ConfigResult.SUCCESS;
    }


}
