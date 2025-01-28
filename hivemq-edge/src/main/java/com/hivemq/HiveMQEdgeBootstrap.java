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
package com.hivemq;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.hivemq.api.model.capabilities.Capability;
import com.hivemq.api.resources.GenericAPIHolder;
import com.hivemq.bootstrap.HiveMQExceptionHandlerBootstrap;
import com.hivemq.bootstrap.LoggingBootstrap;
import com.hivemq.bootstrap.ioc.DaggerInjector;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.bootstrap.ioc.Persistences;
import com.hivemq.bootstrap.services.EdgeCoreFactoryService;
import com.hivemq.bootstrap.services.GeneralBootstrapServiceImpl;
import com.hivemq.bootstrap.services.PersistenceBootstrapService;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.impl.capability.CapabilityServiceImpl;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.exceptions.HiveMQEdgeStartupException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.extensions.core.CommercialModuleLoaderDiscovery;
import com.hivemq.extensions.core.HandlerService;
import com.hivemq.extensions.core.PersistencesService;
import com.hivemq.extensions.core.RestComponentsService;
import com.hivemq.extensions.core.RestComponentsServiceImpl;
import com.hivemq.metrics.MetricRegistryLogger;
import com.hivemq.persistence.PersistenceStartup;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.connection.ConnectionPersistenceImpl;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class HiveMQEdgeBootstrap {

    private static final @NotNull Logger log = LoggerFactory.getLogger(HiveMQEdgeBootstrap.class);

    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull HivemqId hivemqId = new HivemqId();
    private final @NotNull PersistenceStartup persistenceStartup = new PersistenceStartup();
    private final @NotNull HandlerService handlerService = new HandlerService();
    private final @NotNull GenericAPIHolder genericAPIHolder = new GenericAPIHolder();
    private final @NotNull ShutdownHooks shutdownHooks = new ShutdownHooks();
    private final @NotNull HiveMQCapabilityService capabilityService = new CapabilityServiceImpl();
    private final @NotNull ConnectionPersistence connectionPersistence = new ConnectionPersistenceImpl();
    private final @NotNull PersistencesService persistencesService = new PersistencesService(persistenceStartup);
    private final @NotNull RestComponentsService restComponentsService =
            new RestComponentsServiceImpl(genericAPIHolder);
    private final @NotNull EdgeCoreFactoryService edgeCoreFactoryService;

    private volatile @Nullable ConfigurationService configService;
    private @Nullable CommercialModuleLoaderDiscovery commercialModuleLoaderDiscovery;
    private @Nullable GeneralBootstrapServiceImpl generalBootstrapService;
    private @Nullable PersistenceBootstrapService persistenceBootstrapService;
    private @Nullable Injector injector;

    public HiveMQEdgeBootstrap(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull SystemInformation systemInformation,
            final @NotNull ModuleLoader moduleLoader,
            final @Nullable ConfigurationService configService) {
        this.metricRegistry = metricRegistry;
        this.systemInformation = systemInformation;
        this.moduleLoader = moduleLoader;
        this.configService = configService;
        this.edgeCoreFactoryService = new EdgeCoreFactoryService();
    }

    public @NotNull Injector bootstrap() throws HiveMQEdgeStartupException {
        metricRegistry.addListener(new MetricRegistryLogger());

        if(systemInformation.isConfigWriteable()) {
            capabilityService.addCapability(new Capability("config-writeable",
                    "Config can be manipulated via the REST API",
                    "Changes to the configuration made via the REST API are persisted back into the config.xml."));
        }
        LoggingBootstrap.prepareLogging();

        // Embedded has already called init as it is required to read the config file.
        if (!systemInformation.isEmbedded()) {
            log.trace("Initializing HiveMQ home directory");
            //Create SystemInformation this early because logging depends on it
            systemInformation.init();
        }
        // load available modules after system information is bootstrapped.
        moduleLoader.loadModules();


        log.trace("Initializing Logging");
        LoggingBootstrap.initLogging(systemInformation.getConfigFolder());

        log.trace("Initializing Exception handlers");
        HiveMQExceptionHandlerBootstrap.addUnrecoverableExceptionHandler();

        //ungraceful shutdown does not delete tmp folders, so we clean them up on broker start
        log.trace("Cleaning up temporary folders");
        deleteTmpFolder(systemInformation.getDataFolder());

        // it is not null in case of integration tests
        // it is null when edge is started "usually"
        if (configService == null) {
            final ConfigurationMigrator migrator = new ConfigurationMigrator(systemInformation, moduleLoader);
            migrator.migrate();
            log.trace("Initializing configuration");
            configService = ConfigurationBootstrap.bootstrapConfig(systemInformation);
        }
        bootstrapCoreComponents();

        bootstrapInjector();
        final long startInit = System.currentTimeMillis();

        bootstrapPersistences();

        // make sure all persistences are bootstrapped.
        awaitPersistenceStartup();

        finishBootstrap();

        log.trace("Initializing classes");
        Objects.requireNonNull(injector).initEagerSingletons();
        log.trace("Initialized classes in {}ms", (System.currentTimeMillis() - startInit));
        return injector;
    }

    private void awaitPersistenceStartup() {
        Preconditions.checkNotNull(configService);
        Preconditions.checkNotNull(injector);
        // ensure that persistences are built
        injector.persistences();

        try {
            persistenceStartup.finish();
        } catch (final InterruptedException e) {
            throw new HiveMQEdgeStartupException(e);
        }
        log.info("HiveMQ Edge starts with Persistence Mode: '{}'",
                configService.persistenceConfigurationService().getMode());
    }

    private void bootstrapInjector() {
        log.trace("Initializing injector");
        final long startDagger = System.currentTimeMillis();
        injector = DaggerInjector.builder()
                .configurationService(configService)
                .systemInformation(systemInformation)
                .metricRegistry(metricRegistry)
                .persistenceService(persistencesService)
                .handlerService(handlerService)
                .persistenceStartUp(persistenceStartup)
                .moduleLoader(moduleLoader)
                .shutdownHooks(shutdownHooks)
                .capabilityService(capabilityService)
                .restComponentService(restComponentsService)
                .restComponentsHolder(genericAPIHolder)
                .connectionPersistence(connectionPersistence)
                .commercialModuleDiscovery(commercialModuleLoaderDiscovery)
                .generalBootstrapService(generalBootstrapService)
                .hivemqId(hivemqId)
                .edgeCoreFactoryService(edgeCoreFactoryService)
                .build();
        log.trace("Initialized injector in {}ms", (System.currentTimeMillis() - startDagger));
    }

    private void bootstrapCoreComponents() {
        log.info("Integrating Core Modules");
        // configService is always set in caller
        Preconditions.checkNotNull(configService);

        try {
            commercialModuleLoaderDiscovery = new CommercialModuleLoaderDiscovery(moduleLoader);
            generalBootstrapService =
                    new GeneralBootstrapServiceImpl(shutdownHooks, metricRegistry, systemInformation, configService, hivemqId, edgeCoreFactoryService);
            commercialModuleLoaderDiscovery.generalBootstrap(generalBootstrapService);
        } catch (final Exception e) {
            log.warn("Error on loading the commercial module loader.", e);
            throw new HiveMQEdgeStartupException(e);
        }
    }


    private void bootstrapPersistences() {
        Preconditions.checkNotNull(generalBootstrapService);
        Preconditions.checkNotNull(configService);
        Preconditions.checkNotNull(commercialModuleLoaderDiscovery);
        Preconditions.checkNotNull(injector);


        try {
            persistenceBootstrapService = injector.persistenceBootstrapService();
            commercialModuleLoaderDiscovery.persistenceBootstrap(persistenceBootstrapService);
        } catch (final Exception e) {
            log.warn("Error on bootstrapping persistences.", e);
            throw new HiveMQEdgeStartupException(e);
        }
    }

    private void finishBootstrap() {
        Preconditions.checkNotNull(injector);
        final Persistences persistences = injector.persistences();
        Preconditions.checkNotNull(persistences);
        Preconditions.checkNotNull(configService);
        Preconditions.checkNotNull(commercialModuleLoaderDiscovery);

        try {
            commercialModuleLoaderDiscovery.completeBootstrap(injector.completeBootstrapService());
        } catch (final Exception e) {
            log.warn("Error on bootstraping persistences.", e);
            throw new HiveMQEdgeStartupException(e);
        }
    }


    private static void deleteTmpFolder(final @NotNull File dataFolder) {
        final String tmpFolder = dataFolder.getPath() + File.separator + "tmp";
        try {
            //ungraceful shutdown does not delete tmp folders, so we clean them up on broker start
            FileUtils.deleteDirectory(new File(tmpFolder));
        } catch (final IOException e) {
            //No error because it's not business breaking
            log.warn("The temporary folder could not be deleted ({}).", tmpFolder);
            if (log.isDebugEnabled()) {
                log.debug("Original Exception: ", e);
            }
        }
    }

}
