package com.hivemq.extensions.core;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public class CoreModuleServiceImpl implements CoreModuleService {

    private final @NotNull PersistencesService persistencesService;


    public CoreModuleServiceImpl(
            final @NotNull PersistencesService persistencesService) {
        this.persistencesService = persistencesService;
    }

    @Override
    public @NotNull PersistencesService persistenceService() {
        return persistencesService;
    }
}
