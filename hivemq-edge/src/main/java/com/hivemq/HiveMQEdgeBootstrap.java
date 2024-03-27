package com.hivemq;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.hivemq.api.resources.GenericAPIHolder;
import com.hivemq.bootstrap.HiveMQExceptionHandlerBootstrap;
import com.hivemq.bootstrap.LoggingBootstrap;
import com.hivemq.bootstrap.ioc.DaggerInjector;
import com.hivemq.bootstrap.ioc.Injector;
import com.hivemq.bootstrap.ioc.Persistences;
import com.hivemq.bootstrap.services.CompleteBootstrapService;
import com.hivemq.bootstrap.services.CompleteBootstrapServiceImpl;
import com.hivemq.bootstrap.services.GeneralBootstrapServiceImpl;
import com.hivemq.bootstrap.services.PersistenceBootstrapService;
import com.hivemq.bootstrap.services.PersistenceBootstrapServiceImpl;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.ConfigurationBootstrap;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQCapabilityService;
import com.hivemq.edge.impl.capability.CapabilityServiceImpl;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.exceptions.HiveMQEdgeStartupException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
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
    private @Nullable ConfigurationService configService;

    private final @NotNull PersistenceStartup persistenceStartup = new PersistenceStartup();
    private final @NotNull HandlerService handlerService = new HandlerService();
    private final @NotNull GenericAPIHolder genericAPIHolder = new GenericAPIHolder();
    private final @NotNull ShutdownHooks shutdownHooks = new ShutdownHooks();
    private final @NotNull HiveMQCapabilityService capabilityService = new CapabilityServiceImpl();
    private final @NotNull ConnectionPersistence connectionPersistence = new ConnectionPersistenceImpl();
    private final @NotNull PersistencesService persistencesService = new PersistencesService(persistenceStartup);
    private final @NotNull RestComponentsService restComponentsService =
            new RestComponentsServiceImpl(genericAPIHolder);
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
    }

    public @NotNull Injector bootstrap() throws HiveMQEdgeStartupException {
        metricRegistry.addListener(new MetricRegistryLogger());

        LoggingBootstrap.prepareLogging();

        // Embedded has already called init as it is required to read the config file.
        if (!systemInformation.isEmbedded()) {
            log.trace("Initializing HiveMQ home directory");
            //Create SystemInformation this early because logging depends on it
            systemInformation.init();
        }

        log.trace("Initializing Logging");
        LoggingBootstrap.initLogging(systemInformation.getConfigFolder());

        log.trace("Initializing Exception handlers");
        HiveMQExceptionHandlerBootstrap.addUnrecoverableExceptionHandler();

        //ungraceful shutdown does not delete tmp folders, so we clean them up on broker start
        log.trace("Cleaning up temporary folders");
        deleteTmpFolder(systemInformation.getDataFolder());

        if (configService == null) {
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
        Objects.requireNonNull(injector).persistences();
        try {
            persistenceStartup.finish();
        } catch (InterruptedException e) {
            throw new HiveMQEdgeStartupException(e);
        }

        // configService is always set in caller
        assert configService != null;

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
                .build();
        log.trace("Initialized injector in {}ms", (System.currentTimeMillis() - startDagger));
    }

    private void bootstrapCoreComponents() {
        log.info("Integrating Core Modules");
        // configService is always set in caller
        assert configService != null;


        try {
            commercialModuleLoaderDiscovery = new CommercialModuleLoaderDiscovery(moduleLoader);
            commercialModuleLoaderDiscovery.discoverModuleLoaderMainClasses();
            generalBootstrapService =
                    new GeneralBootstrapServiceImpl(shutdownHooks, metricRegistry, systemInformation, configService);
            commercialModuleLoaderDiscovery.generalBootstrap(generalBootstrapService);
        } catch (Exception e) {
            log.warn("Error on loading the commercial module loader.", e);
            throw new HiveMQEdgeStartupException(e);
        }
    }


    private void bootstrapPersistences() {
        Preconditions.checkNotNull(generalBootstrapService);
        Preconditions.checkNotNull(configService);
        Preconditions.checkNotNull(commercialModuleLoaderDiscovery);

        try {
            persistenceBootstrapService = PersistenceBootstrapServiceImpl.decorate(generalBootstrapService,
                    persistencesService,
                    capabilityService);
            commercialModuleLoaderDiscovery.persistenceBootstrap(persistenceBootstrapService);
        } catch (Exception e) {
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
        Preconditions.checkNotNull(persistenceBootstrapService);

        try {
            final CompleteBootstrapService completeBootstrapService = CompleteBootstrapServiceImpl.decorate(
                    persistenceBootstrapService,
                    persistences,
                    restComponentsService,
                    handlerService);
            commercialModuleLoaderDiscovery.completeBootstrap(completeBootstrapService);
        } catch (Exception e) {
            log.warn("Error on bootstraping persistences.", e);
            throw new HiveMQEdgeStartupException(e);
        }
    }


    private static void deleteTmpFolder(final @NotNull File dataFolder) {
        final String tmpFolder = dataFolder.getPath() + File.separator + "tmp";
        try {
            //ungraceful shutdown does not delete tmp folders, so we clean them up on broker start
            FileUtils.deleteDirectory(new File(tmpFolder));
        } catch (IOException e) {
            //No error because it's not business breaking
            log.warn("The temporary folder could not be deleted ({}).", tmpFolder);
            if (log.isDebugEnabled()) {
                log.debug("Original Exception: ", e);
            }
        }
    }

}
