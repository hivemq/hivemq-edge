package com.hivemq.bootstrap.provider;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.bootstrap.factories.PublishPayloadPersistenceFactory;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.payload.PublishPayloadNoopPersistenceImpl;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;

import javax.inject.Inject;


public class PublishPayloadPersistenceProvider {

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull PublishPayloadNoopPersistenceImpl noopPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final MessageDroppedService messageDroppedService;
    private final ListeningScheduledExecutorService payloadPersistenceExecutor;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final @NotNull MetricsHolder metricsHolder;

    @Inject
    PublishPayloadPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull PublishPayloadNoopPersistenceImpl noopPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull @Persistence ListeningScheduledExecutorService payloadPersistenceExecutor,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull MetricsHolder metricsHolder) {
        this.persistencesService = persistencesService;
        this.noopPersistence = noopPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.messageDroppedService = messageDroppedService;
        this.payloadPersistenceExecutor = payloadPersistenceExecutor;
        this.persistenceStartup = persistenceStartup;
        this.metricsHolder = metricsHolder;
    }

    public @NotNull PublishPayloadPersistence get() {

        final PublishPayloadPersistenceFactory persistenceFactory =
                persistencesService.getPublishPayloadPersistenceFactory();

        if (persistenceFactory == null) {
            return noopPersistence;
        }
        return persistenceFactory.buildPublishPayloadPersistence(localPersistenceFileUtil,
                metricsHolder,
                messageDroppedService,
                payloadPersistenceExecutor,
                persistenceStartup);
    }
}
