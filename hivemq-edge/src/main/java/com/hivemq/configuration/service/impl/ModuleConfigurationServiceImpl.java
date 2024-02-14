package com.hivemq.configuration.service.impl;

import com.hivemq.configuration.service.ModuleConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class ModuleConfigurationServiceImpl implements ModuleConfigurationService {

    private @NotNull Map<String, Object> configs = new HashMap<>();

    @Override
    public @NotNull Map<String, Object> getAllConfigs() {
        return configs;
    }

    @Override
    public void setAllConfigs(@NotNull final Map<String, Object> allConfigs) {
        this.configs = allConfigs;
    }
}
