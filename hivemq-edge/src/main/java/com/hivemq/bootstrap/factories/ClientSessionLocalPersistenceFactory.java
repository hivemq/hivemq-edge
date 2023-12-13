package com.hivemq.bootstrap.factories;

import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

public interface ClientSessionLocalPersistenceFactory {

     @NotNull ClientSessionLocalPersistence buildClientSessionLocalPersistence(
             @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
             @NotNull PublishPayloadPersistence payloadPersistence,
             @NotNull EventLog eventLog,
             @NotNull MetricsHolder metricsHolder,
             @NotNull PersistenceStartup persistenceStartup);


}
