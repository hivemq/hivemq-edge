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

import com.hivemq.combining.model.DataCombiner;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.combining.DataCombinerEntity;
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

    @Inject
    public ConfigPersistence(
            final @NotNull ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }


    public @NotNull List<DataCombiner> allDataCombiners() {
        return configurationService.dataCombiningConfigurationService()
                .getAllConfigs()
                .stream()
                .map(DataCombiner::fromPersistence)
                .collect(Collectors.toList());
    }

    public synchronized void updateDataCombiner(
            final @NotNull DataCombiner dataCombiner) {
        final @NotNull List<DataCombiner> allDataCombiners = allDataCombiners();
        if (allDataCombiners.removeIf(instance -> dataCombiner.id().equals(instance.id()))) {
            allDataCombiners.add(dataCombiner);
        } else {
            log.error("Tried updating non existing data combiner '{}'.", dataCombiner.id());
        }
        updateAllDataCombiners(allDataCombiners);
    }

    public synchronized void updateAllDataCombiners(final @NotNull List<DataCombiner> dataCombiners) {
        final List<DataCombinerEntity> adapterEntities =
                dataCombiners.stream().map(DataCombiner::toPersistence).collect(Collectors.toList());
        configurationService.dataCombiningConfigurationService().setAllConfigs(adapterEntities);
    }

    public synchronized void addDataCombiner(final @NotNull DataCombiner dataCombiner) {
        final @NotNull List<DataCombiner> allDataCombiners = allDataCombiners();
        allDataCombiners.add(dataCombiner);
        updateAllDataCombiners(allDataCombiners);
    }

}
