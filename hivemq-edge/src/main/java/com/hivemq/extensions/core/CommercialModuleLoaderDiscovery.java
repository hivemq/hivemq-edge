package com.hivemq.extensions.core;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CommercialModuleLoaderDiscovery {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CommercialModuleLoaderDiscovery.class);

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull ModuleLoader moduleLoader;

    public CommercialModuleLoaderDiscovery(
            final @NotNull PersistencesService persistencesService,
            final @NotNull SystemInformation systemInformation,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ModuleLoader moduleLoader) {
        this.persistencesService = persistencesService;
        this.systemInformation = systemInformation;
        this.metricRegistry = metricRegistry;
        this.shutdownHooks = shutdownHooks;
        this.moduleLoader = moduleLoader;
    }

    public void loadAllCoreModules()
            throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        moduleLoader.loadModules();
        final List<Class<? extends ModuleLoaderMain>> implementations =
                moduleLoader.findImplementations(ModuleLoaderMain.class);
        // TODO remove
        log.info("Found implementations {}", implementations);
        for (Class<? extends ModuleLoaderMain> implementation : implementations) {
            loadAndStartMainClass(implementation);
        }
    }

    private void loadAndStartMainClass(Class<? extends ModuleLoaderMain> extensionMainClass)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final ModuleLoaderMain instance = extensionMainClass.getDeclaredConstructor().newInstance();
        CoreModuleServiceImpl coreModuleService = new CoreModuleServiceImpl(persistencesService,
                systemInformation,
                metricRegistry,
                shutdownHooks,
                moduleLoader);
        instance.start(coreModuleService);
    }
}

