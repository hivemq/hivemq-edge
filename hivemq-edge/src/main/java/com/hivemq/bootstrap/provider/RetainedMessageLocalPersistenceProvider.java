package com.hivemq.bootstrap.provider;

import com.hivemq.bootstrap.factories.RetainedMessageLocalPersistenceFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.memory.RetainedMessageMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

import javax.inject.Inject;


public class RetainedMessageLocalPersistenceProvider {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull RetainedMessageMemoryLocalPersistence retainedMessageMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull PersistenceStartup persistenceStartup;

    @Inject
    RetainedMessageLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull RetainedMessageMemoryLocalPersistence retainedMessageMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull PersistenceStartup persistenceStartup) {
        this.persistencesService = persistencesService;
        this.retainedMessageMemoryLocalPersistence = retainedMessageMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;
        this.persistenceStartup = persistenceStartup;
    }

    public @NotNull RetainedMessageLocalPersistence get() {

        final @Nullable RetainedMessageLocalPersistenceFactory persistenceFactory =
                persistencesService.getRetainedMessageLocalPersistenceFactory();

        if (persistenceFactory == null) {
            return retainedMessageMemoryLocalPersistence;
        }
        return persistenceFactory.buildRetainedMessageLocalPersistence(localPersistenceFileUtil,
                payloadPersistence,
                persistenceStartup);
    }
}
