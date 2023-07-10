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
package com.hivemq.configuration.service.impl;

import com.hivemq.configuration.service.DynamicConfigurationService;

/**
 * @author Simon L Johnson
 */
public class GatewayConfigurationServiceImpl implements DynamicConfigurationService {

    private boolean mutableConfigurationEnabled;
    private boolean configurationExportEnabled;

    @Override
    public boolean isMutableConfigurationEnabled() {
        return mutableConfigurationEnabled;
    }

    @Override
    public boolean isConfigurationExportEnabled() {
        return configurationExportEnabled;
    }

    public void setMutableConfigurationEnabled(final boolean mutableConfigurationEnabled) {
        this.mutableConfigurationEnabled = mutableConfigurationEnabled;
    }

    public void setConfigurationExportEnabled(final boolean configurationExportEnabled) {
        this.configurationExportEnabled = configurationExportEnabled;
    }

}
