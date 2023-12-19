package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.InternalConfigEntity;
import com.hivemq.configuration.entity.OptionEntity;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class InternalConfigurator {

    private final @NotNull InternalConfigurationService internalConfigurationService;

    public InternalConfigurator(final @NotNull InternalConfigurationService internalConfigurationService) {
        this.internalConfigurationService = internalConfigurationService;
    }

    public void setConfig(final @NotNull InternalConfigEntity internalConfigEntity) {
        for (final OptionEntity optionEntity : internalConfigEntity.getOptions()) {
            internalConfigurationService.set(optionEntity.getKey(), optionEntity.getValue());
        }
    }
}
