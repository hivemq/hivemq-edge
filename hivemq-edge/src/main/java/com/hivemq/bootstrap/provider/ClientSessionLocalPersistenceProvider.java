package com.hivemq.bootstrap.provider;

import com.hivemq.bootstrap.factories.ClientSessionLocalPersistenceFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

import javax.inject.Inject;


public class ClientSessionLocalPersistenceProvider {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull ClientSessionMemoryLocalPersistence clientSessionMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull EventLog eventLog;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull PersistenceStartup persistenceStartup;

    @Inject
    ClientSessionLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull ClientSessionMemoryLocalPersistence clientQueueMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EventLog eventLog,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull PersistenceStartup persistenceStartup) {
        this.persistencesService = persistencesService;
        this.clientSessionMemoryLocalPersistence = clientQueueMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;
        this.eventLog = eventLog;
        this.metricsHolder = metricsHolder;
        this.persistenceStartup = persistenceStartup;
    }

    public @NotNull ClientSessionLocalPersistence get() {

        final ClientSessionLocalPersistenceFactory persistenceFactory =
                persistencesService.getClientSessionLocalPersistenceFactory();

        if (persistenceFactory == null) {
            return clientSessionMemoryLocalPersistence;
        }
        return persistenceFactory.buildClientSessionLocalPersistence(localPersistenceFileUtil,
                payloadPersistence,
                eventLog,
                metricsHolder,
                persistenceStartup
                );
    }
}
