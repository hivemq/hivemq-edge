package com.hivemq.bootstrap.provider;

import com.hivemq.bootstrap.factories.ClientSessionSubscriptionLocalPersistenceFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionSubscriptionMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

import javax.inject.Inject;


public class ClientSessionSubscriptionLocalPersistenceProvider {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull ClientSessionSubscriptionLocalPersistence clientSessionMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull PersistenceStartup persistenceStartup;

    @Inject
    ClientSessionSubscriptionLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull ClientSessionSubscriptionMemoryLocalPersistence clientSessionMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull PersistenceStartup persistenceStartup) {
        this.persistencesService = persistencesService;
        this.clientSessionMemoryLocalPersistence = clientSessionMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;
        this.persistenceStartup = persistenceStartup;
    }

    public @NotNull ClientSessionSubscriptionLocalPersistence get() {

        final @Nullable ClientSessionSubscriptionLocalPersistenceFactory persistenceFactory =
                persistencesService.getClientSessionSubscriptionLocalPersistenceFactory();

        if (persistenceFactory == null) {
            return clientSessionMemoryLocalPersistence;
        }
        return persistenceFactory.buildClientSessionSubscriptionLocalPersistence(localPersistenceFileUtil,
                payloadPersistence,
                persistenceStartup);
    }
}
