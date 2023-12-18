/*
 * Copyright 2019-present HiveMQ GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hivemq.bootstrap.provider;

import com.hivemq.bootstrap.factories.ClientSessionLocalPersistenceFactory;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.PersistenceMode;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.logging.EventLog;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class ClientSessionLocalPersistenceProvider {

    private static final Logger log = LoggerFactory.getLogger(ClientSessionLocalPersistenceProvider.class);

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull ClientSessionMemoryLocalPersistence clientSessionMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull EventLog eventLog;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;

    @Inject
    ClientSessionLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull ClientSessionMemoryLocalPersistence clientQueueMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull EventLog eventLog,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull PersistenceConfigurationService persistenceConfigurationService) {
        this.persistencesService = persistencesService;
        this.clientSessionMemoryLocalPersistence = clientQueueMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;
        this.eventLog = eventLog;
        this.metricsHolder = metricsHolder;
        this.persistenceStartup = persistenceStartup;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    public @NotNull ClientSessionLocalPersistence get() {

        final ClientSessionLocalPersistenceFactory persistenceFactory =
                persistencesService.getClientSessionLocalPersistenceFactory();

        if (persistenceConfigurationService.getMode() == PersistenceMode.IN_MEMORY) {
            return clientSessionMemoryLocalPersistence;
        }

        if (persistenceFactory == null) {
            log.error(
                    "File Persistence is specified in config.xml, but no provider for a file persistence is available. Check that the commercial module is present in the module folder and a valid license is present in the license folder.");
            throw new UnrecoverableException();
        }


        return persistenceFactory.buildClientSessionLocalPersistence(localPersistenceFileUtil,
                payloadPersistence,
                eventLog,
                metricsHolder,
                persistenceStartup
                );
    }
}
