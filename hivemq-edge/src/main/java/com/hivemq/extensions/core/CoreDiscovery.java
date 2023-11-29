package com.hivemq.extensions.core;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.info.SystemInformationImpl;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CoreDiscovery {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CoreDiscovery.class);

    private final @NotNull PersistencesService persistencesService;
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ModuleLoader moduleLoader;

    public CoreDiscovery(
            final @NotNull PersistencesService persistencesService,
            final @NotNull SystemInformation systemInformation,
            final @NotNull ModuleLoader moduleLoader) {
        this.persistencesService = persistencesService;
        this.systemInformation = systemInformation;
        this.moduleLoader = moduleLoader;
    }

    public void loadAllCoreModules()
            throws InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        moduleLoader.loadModules();
        final List<Class<? extends CoreModuleMain>> implementations =
                moduleLoader.findImplementations(CoreModuleMain.class);
        for (Class<? extends CoreModuleMain> implementation : implementations) {
            loadAndStartMainClass(implementation);
        }
    }

    private void loadAndStartMainClass(Class<? extends CoreModuleMain> extensionMainClass)
            throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        final CoreModuleMain instance = extensionMainClass.getDeclaredConstructor().newInstance();
        CoreModuleServiceImpl coreModuleService = new CoreModuleServiceImpl(persistencesService);
        instance.start(coreModuleService);
    }
}

