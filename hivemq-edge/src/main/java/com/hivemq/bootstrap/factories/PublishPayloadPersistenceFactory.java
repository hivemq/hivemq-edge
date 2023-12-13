package com.hivemq.bootstrap.factories;

import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

import java.util.concurrent.ScheduledExecutorService;

public interface PublishPayloadPersistenceFactory {

    @NotNull PublishPayloadPersistence buildPublishPayloadPersistence(@NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
                                                                      @NotNull MetricsHolder metricsHolder,
                                                                      @NotNull MessageDroppedService messageDroppedService,
                                                                      @NotNull ScheduledExecutorService scheduledExecutorService,
                                                                      @NotNull PersistenceStartup persistenceStartup,
                                                                      @NotNull PersistenceConfigurationService persistenceConfigurationService);
}
