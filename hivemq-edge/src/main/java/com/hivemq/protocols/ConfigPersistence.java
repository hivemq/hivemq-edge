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

import com.hivemq.configuration.entity.adapter.FieldMappingsEntity;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class doesn't make use of caching in any way.
 * Config is kept in memory so reading operations are fast.
 * <p>
 * Beyond that this class is used to get all config interactions into one place for easier reqorks in the future.
 */
public class ConfigPersistence {

    private static final Logger log = LoggerFactory.getLogger(ConfigPersistence.class);

    private final @NotNull ConfigurationService configurationService;

    public ConfigPersistence(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public @NotNull Map<String, Object> allAdapters() {
        return new HashMap<>(configurationService.protocolAdapterConfigurationService().getAllConfigs());
    }

    public synchronized void updateAllAdapters(final @NotNull Map<String, Object> adapterConfigs) {
        adapterConfigs.entrySet()
                .stream()
                .flatMap(e -> ((List<Map<String, Object>>) e.getValue()).stream())
                .forEach(v -> {
                    if (!v.containsKey("config")) {
                        throw new IllegalArgumentException("Missing config");
                    }
                });
        configurationService.protocolAdapterConfigurationService().setAllConfigs(adapterConfigs);
    }

    public synchronized void addAdapter(
            final @NotNull String protocolId,
            final @NotNull Map<String, Object> config,
            final @NotNull List<Map<String, Object>> tagMaps,
            final @NotNull List<FieldMappingsEntity> fieldMappingsEntities) {
        final Map<String, Object> mainMap = allAdapters();
        final List<Map<String, Object>> adapterList = getAdapterListForType(mainMap, protocolId);
        adapterList.add(combine(config, tagMaps, fieldMappingsEntities));
        updateAllAdapters(mainMap);
    }

    public synchronized void updateAdapter(
            final @NotNull String protocolId,
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> config,
            final @NotNull List<Map<String, Object>> tagMaps,
            final @NotNull List<FieldMappingsEntity> fieldMappingsEntities) {
        final Map<String, Object> mainMap = allAdapters();
        final List<Map<String, Object>> adapterList = getAdapterListForType(mainMap, protocolId);
        if (adapterList.removeIf(instance -> adapterId.equals(((Map<String, Object>) instance.get("config")).get("id")))) {
            adapterList.add(combine(config, tagMaps, fieldMappingsEntities));
        } else {
            log.error("Tried updating non existing adapter {} of type {}", adapterId, protocolId);
        }
        updateAllAdapters(mainMap);
    }

    public synchronized void deleteAdapter(final @NotNull String adapterId, final @NotNull String protocolId) {
        final Map<String, Object> mainMap = allAdapters();
        final List<Map<String, Object>> adapterList = getAdapterListForType(mainMap, protocolId);
        if (adapterList.removeIf(instance -> adapterId.equals(((Map<String, Object>) instance.get("config")).get("id")))) {
            updateAllAdapters(mainMap);
        } else {
            log.error("Tried deleting non existing adapter {} of type {}", adapterId, protocolId);
        }
    }

    public Map<String, Object> combine(
            final @NotNull Map<String, Object> config,
            final @NotNull List<Map<String, Object>> tagMaps,
            final @NotNull List<FieldMappingsEntity> fieldMappingsEntities) {
        return Map.of("config", config, "tags", tagMaps, "fieldMappings", fieldMappingsEntities);
    }

    private @NotNull List<Map<String, Object>> getAdapterListForType(
            final @NotNull Map<String, Object> mainMap, final @NotNull String adapterType) {

        final List<Map<String, Object>> adapterList;
        final Object o = mainMap.get(adapterType);
        if (o instanceof Map || o instanceof String || o == null) {
            adapterList = new ArrayList<>();
            if (o instanceof Map) {
                adapterList.add((Map) o);
            }
            mainMap.put(adapterType, adapterList);
        } else {
            adapterList = (List) o;
        }
        return adapterList;
    }

}
