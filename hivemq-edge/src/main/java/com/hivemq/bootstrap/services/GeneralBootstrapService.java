package com.hivemq.bootstrap.services;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

public interface GeneralBootstrapService {

    @NotNull MetricRegistry metricRegistry();


    @NotNull SystemInformation systemInformation();

    @NotNull ShutdownHooks shutdownHooks();

    @NotNull ConfigurationService configurationService();
}
