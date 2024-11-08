package com.hivemq.protocols;

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
 *
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
        configurationService.protocolAdapterConfigurationService().setAllConfigs(adapterConfigs);
    }

    public synchronized void addAdapter(final @NotNull String protocolId, final @NotNull Map<String, Object> config, final @NotNull List<Map<String, Object>> tagMaps) {
        final Map<String, Object> mainMap = allAdapters();
        final List<Map<String, Object>> adapterList = getAdapterListForType(mainMap, protocolId);
        adapterList.add(combine(config, tagMaps));
        updateAllAdapters(mainMap);
    }

    public synchronized void updateAdapter(final @NotNull String protocolId, final @NotNull Map<String, Object> config, final @NotNull List<Map<String, Object>> tagMaps) {
        final Map<String, Object> mainMap = allAdapters();
        final String adapterId = (String)config.get("id");
        final List<Map<String, Object>> adapterList = getAdapterListForType(mainMap, protocolId);
        if(adapterList.removeIf(instance -> adapterId.equals(((Map<String, Object>)instance.get("config")).get("id")))) {
            adapterList.add(combine(config, tagMaps));
        } else {
            log.error("Tried updating non existing adapter {} of type {}", adapterId, protocolId);
        }
        updateAllAdapters(mainMap);
    }

    public synchronized void deleteAdapter(final @NotNull String adapterId, final @NotNull String protocolId) {
        final Map<String, Object> mainMap = allAdapters();
        final List<Map<String, Object>> adapterList = getAdapterListForType(mainMap, protocolId);
        if (adapterList.removeIf(instance -> adapterId.equals(((Map<String, Object>)instance.get("config")).get("id")))) {
            updateAllAdapters(mainMap);
        } else {
            log.error("Tried deleting non existing adapter {} of type {}", adapterId, protocolId);
        }
    }

    public Map<String, Object> combine(final @NotNull Map<String, Object> config, final @NotNull List<Map<String, Object>> tagMaps) {
        return Map.of("config", config, "tags", tagMaps);
    }

    private @NotNull List<Map<String, Object>> getAdapterListForType(final @NotNull Map<String, Object> mainMap, final @NotNull String adapterType) {

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
