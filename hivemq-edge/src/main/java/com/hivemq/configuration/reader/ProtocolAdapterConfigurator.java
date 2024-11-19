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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.service.ProtocolAdapterConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

public class ProtocolAdapterConfigurator {

    private final @NotNull ProtocolAdapterConfigurationService configurationService;

    public ProtocolAdapterConfigurator(final @NotNull ProtocolAdapterConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setConfigs(final @NotNull List<ProtocolAdapterEntity> protocolAdapterConfigs) {
        configurationService.setAllConfigs(protocolAdapterConfigs);
    }

    public void syncConfigs(final @NotNull List<ProtocolAdapterEntity> config) {
        config.clear();
        config.addAll(configurationService.getAllConfigs());
    }
}
