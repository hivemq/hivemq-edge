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

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.hivemq.bootstrap.factories.PublishPayloadPersistenceFactory;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.PersistenceConfigurationService;
import com.hivemq.configuration.service.PersistenceMode;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.ioc.annotation.Persistence;
import com.hivemq.persistence.payload.PublishPayloadNoopPersistenceImpl;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.util.LocalPersistenceFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;


public class PublishPayloadPersistenceProvider {

    private static final Logger log = LoggerFactory.getLogger(PublishPayloadPersistenceProvider.class);

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull PublishPayloadNoopPersistenceImpl noopPersistence;
    private final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull ListeningScheduledExecutorService payloadPersistenceExecutor;
    private final @NotNull PersistenceStartup persistenceStartup;
    private final @NotNull MetricsHolder metricsHolder;
    private final @NotNull PersistenceConfigurationService persistenceConfigurationService;
    private final @NotNull SystemInformation systemInformation;

    @Inject
    PublishPayloadPersistenceProvider(
            final @NotNull PersistencesService persistencesService,
            final @NotNull PublishPayloadNoopPersistenceImpl noopPersistence,
            final @NotNull LocalPersistenceFileUtil localPersistenceFileUtil,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull @Persistence ListeningScheduledExecutorService payloadPersistenceExecutor,
            final @NotNull PersistenceStartup persistenceStartup,
            final @NotNull MetricsHolder metricsHolder,
            final @NotNull PersistenceConfigurationService persistenceConfigurationService,
            final @NotNull SystemInformation systemInformation) {
        this.persistencesService = persistencesService;
        this.noopPersistence = noopPersistence;
        this.localPersistenceFileUtil = localPersistenceFileUtil;
        this.messageDroppedService = messageDroppedService;
        this.payloadPersistenceExecutor = payloadPersistenceExecutor;
        this.persistenceStartup = persistenceStartup;
        this.metricsHolder = metricsHolder;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.systemInformation = systemInformation;
    }

    public @NotNull PublishPayloadPersistence get() {

        final PublishPayloadPersistenceFactory persistenceFactory =
                persistencesService.getPublishPayloadPersistenceFactory();

        if (persistenceConfigurationService.getMode() == PersistenceMode.IN_MEMORY) {
            return noopPersistence;
        }

        if (persistenceFactory == null) {
            log.error(
                    "File Persistence is specified in config.xml, but no provider for a file persistence is available. Check that the commercial module is present in the module folder and a valid license is present in the license folder.");
            throw new UnrecoverableException();
        }


        return persistenceFactory.buildPublishPayloadPersistence(localPersistenceFileUtil,
                metricsHolder,
                messageDroppedService,
                payloadPersistenceExecutor,
                persistenceStartup,
                persistenceConfigurationService,
                systemInformation);
    }
}
