package com.hivemq.bootstrap.provider;

import com.hivemq.bootstrap.factories.ClientQueueLocalPersistenceFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.clientqueue.ClientQueueLocalPersistence;
import com.hivemq.persistence.local.memory.ClientQueueMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

import javax.inject.Inject;


public class ClientQueueLocalPersistenceProvider {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull ClientQueueMemoryLocalPersistence clientQueueMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull PersistenceStartup persistenceStartup;


    @Inject
    ClientQueueLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull ClientQueueMemoryLocalPersistence clientQueueMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull PersistenceStartup persistenceStartup) {
        this.persistencesService = persistencesService;
        this.clientQueueMemoryLocalPersistence = clientQueueMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;

        this.messageDroppedService = messageDroppedService;
        this.persistenceStartup = persistenceStartup;
    }

    public @NotNull ClientQueueLocalPersistence get() {

        final ClientQueueLocalPersistenceFactory persistenceFactory =
                persistencesService.getClientQueueLocalPersistenceFactory();

        if (persistenceFactory == null) {
            return clientQueueMemoryLocalPersistence;
        }

        return persistenceFactory.buildClientSessionLocalPersistence(localPersistenceFileUtil,
                payloadPersistence,
                messageDroppedService,
                persistenceStartup);
    }
}
