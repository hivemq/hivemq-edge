package com.hivemq.bootstrap.services;

import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;

public interface PersistenceBootstrapService extends GeneralBootstrapService {

    @NotNull ConfigurationService configurationService();

    @NotNull PersistencesService persistenceService();

    @NotNull HiveMQCapabilityService capabilityService();


    static PersistenceBootstrapService decorate(
            final @NotNull GeneralBootstrapService generalBootstrapService,
            final @NotNull PersistencesService persistencesService,
            final @NotNull HiveMQCapabilityService capabilityService) {
        return PersistenceBootstrapServiceImpl.decorate(generalBootstrapService,
                persistencesService,
                capabilityService);
    }

}
