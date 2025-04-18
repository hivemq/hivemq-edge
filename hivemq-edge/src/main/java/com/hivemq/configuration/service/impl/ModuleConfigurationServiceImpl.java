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

import com.hivemq.configuration.service.ModuleConfigurationService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ModuleConfigurationServiceImpl implements ModuleConfigurationService {

    private @NotNull Map<String, Object> configs = new HashMap<>();

    @Override
    public @NotNull Map<String, Object> getAllConfigs() {
        return configs;
    }

    @Override
    public void setAllConfigs(final @NotNull Map<String, Object> allConfigs) {
        this.configs = allConfigs;
    }
}
