package com.hivemq.bootstrap.provider;

import com.hivemq.bootstrap.factories.RetainedMessageLocalPersistenceFactory;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.PersistenceMode;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.memory.RetainedMessageMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.persistence.retained.RetainedMessageLocalPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class RetainedMessageLocalPersistenceProvider {

    private static final @NotNull Logger log = LoggerFactory.getLogger(RetainedMessageLocalPersistenceProvider.class);

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull RetainedMessageMemoryLocalPersistence retainedMessageMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;
    private final @NotNull SystemInformation systemInformation;

    @Inject
    RetainedMessageLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull RetainedMessageMemoryLocalPersistence retainedMessageMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull PersistenceConfigurationService persistenceConfigurationService,
            final @NotNull SystemInformation systemInformation) {
        this.persistencesService = persistencesService;
        this.retainedMessageMemoryLocalPersistence = retainedMessageMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;
        this.persistenceStartup = persistenceStartup;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.systemInformation = systemInformation;
    }

    public @NotNull RetainedMessageLocalPersistence get() {

        final @Nullable RetainedMessageLocalPersistenceFactory persistenceFactory =
                persistencesService.getRetainedMessageLocalPersistenceFactory();

        if (persistenceConfigurationService.getMode() == PersistenceMode.IN_MEMORY) {
            return retainedMessageMemoryLocalPersistence;
        }

        if (persistenceFactory == null) {
            log.error(
                    "File Persistence is specified in config.xml, but no provider for a file persistence is available. Check that the commercial module is present in the module folder and a valid license is present in the license folder.");
            throw new UnrecoverableException();
        }

        return persistenceFactory.buildRetainedMessageLocalPersistence(localPersistenceFileUtil,
                payloadPersistence,
                persistenceStartup,
                persistenceConfigurationService,
                systemInformation);
    }
}
