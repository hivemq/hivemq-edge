package com.hivemq.extensions.core;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;

public interface CoreModuleService {
    @NotNull PersistencesService persistenceService();

    @NotNull SystemInformation systemInformation();

    @NotNull MetricRegistry metricRegistry();

    @NotNull ShutdownHooks shutdownHooks();

    @NotNull ModuleLoader moduleLoader();

    @NotNull ConfigurationService getConfigService();
}
