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
package com.hivemq.protocols;

import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.service.ConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;


/**
 * This class doesn't make use of caching in any way.
 * Config is kept in memory so reading operations are fast.
 * <p>
 * Beyond that this class is used to get all config interactions into one place for easier reqorks in the future.
 */
@Singleton
public class ConfigPersistence {

    private static final Logger log = LoggerFactory.getLogger(ConfigPersistence.class);

    private final @NotNull ConfigurationService configurationService;
    private final @NotNull ProtocolAdapterConfigConverter configConverter;

    @Inject
    public ConfigPersistence(
            final @NotNull ConfigurationService configurationService,
            final @NotNull ProtocolAdapterConfigConverter configConverter) {
        this.configurationService = configurationService;
        this.configConverter = configConverter;
    }

    public @NotNull List<ProtocolAdapterConfig> allAdapters() {
        return configurationService.protocolAdapterConfigurationService()
                .getAllConfigs()
                .stream()
                .map(configConverter::fromEntity)
                .collect(Collectors.toList());
    }

    public synchronized void updateAllAdapters(final @NotNull List<ProtocolAdapterConfig> adapterConfigs) {
        final List<ProtocolAdapterEntity> adapterEntities =
                adapterConfigs.stream().map(configConverter::toEntity).collect(Collectors.toList());
        configurationService.protocolAdapterConfigurationService().setAllConfigs(adapterEntities);
    }

    public synchronized void addAdapter(final @NotNull ProtocolAdapterConfig protocolAdapterConfig) {
        final @NotNull List<ProtocolAdapterConfig> protocolAdapterConfigs = allAdapters();
        protocolAdapterConfigs.add(protocolAdapterConfig);
        updateAllAdapters(protocolAdapterConfigs);
    }

    public synchronized void updateAdapter(
            final @NotNull ProtocolAdapterConfig protocolAdapterConfig) {
        final @NotNull List<ProtocolAdapterConfig> allAdapterConfigs = allAdapters();
        if (allAdapterConfigs.removeIf(instance -> protocolAdapterConfig.getAdapterId()
                .equals(instance.getAdapterId()))) {
            allAdapterConfigs.add(protocolAdapterConfig);
        } else {
            log.error("Tried updating non existing adapter {} of type {}",
                    protocolAdapterConfig.getAdapterId(),
                    protocolAdapterConfig.getProtocolId());
        }
        updateAllAdapters(allAdapterConfigs);
    }

    public synchronized void deleteAdapter(final @NotNull String adapterId, final @NotNull String protocolId) {
        final @NotNull List<ProtocolAdapterConfig> allAdapterConfigs = allAdapters();
        if (allAdapterConfigs.removeIf(instance -> adapterId.equals(instance.getAdapterId()))) {
            updateAllAdapters(allAdapterConfigs);
        } else {
            log.error("Tried deleting non existing adapter {} of type {}", adapterId, protocolId);
        }
    }
}
