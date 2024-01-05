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

import com.hivemq.bootstrap.factories.ClientSessionSubscriptionLocalPersistenceFactory;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.PersistenceMode;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.local.ClientSessionSubscriptionLocalPersistence;
import com.hivemq.persistence.local.memory.ClientSessionSubscriptionMemoryLocalPersistence;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class ClientSessionSubscriptionLocalPersistenceProvider {

    private static final Logger log = LoggerFactory.getLogger(ClientSessionSubscriptionLocalPersistenceProvider.class);

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull ClientSessionSubscriptionLocalPersistence clientSessionMemoryLocalPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull PublishPayloadPersistence payloadPersistence;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;


    @Inject
    ClientSessionSubscriptionLocalPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull ClientSessionSubscriptionMemoryLocalPersistence clientSessionMemoryLocalPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull PublishPayloadPersistence payloadPersistence,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull PersistenceConfigurationService persistenceConfigurationService) {
        this.persistencesService = persistencesService;
        this.clientSessionMemoryLocalPersistence = clientSessionMemoryLocalPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.payloadPersistence = payloadPersistence;
        this.persistenceStartup = persistenceStartup;
        this.persistenceConfigurationService = persistenceConfigurationService;
    }

    public @NotNull ClientSessionSubscriptionLocalPersistence get() {

        final @Nullable ClientSessionSubscriptionLocalPersistenceFactory persistenceFactory =
                persistencesService.getClientSessionSubscriptionLocalPersistenceFactory();

        if (persistenceConfigurationService.getMode() == PersistenceMode.IN_MEMORY) {
            return clientSessionMemoryLocalPersistence;
        }

        if (persistenceFactory == null) {
            log.error(
                    "File Persistence is specified in config.xml, but no provider for a file persistence is available. Check that the commercial module is present in the module folder and a valid license is present in the license folder.");
            throw new UnrecoverableException();
        }

        return persistenceFactory.buildClientSessionSubscriptionLocalPersistence(localPersistenceFileUtil,
                payloadPersistence,
                persistenceStartup);
    }
}
