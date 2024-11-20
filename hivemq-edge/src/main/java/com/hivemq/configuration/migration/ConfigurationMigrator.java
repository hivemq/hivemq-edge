package com.hivemq.configuration.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.ioc.ConfigurationFileProvider;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.configuration.reader.LegacyConfigFileReaderWriter;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class ConfigurationMigrator {

    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull LegacyConfigFileReaderWriter<LegacyHiveMQConfigEntity> legacyConfigFileReaderWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ConfigurationMigrator(final @NotNull SystemInformation systemInformation, final ModuleLoader moduleLoader) {
        this.systemInformation = systemInformation;
        this.moduleLoader = moduleLoader;
        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);
        this.legacyConfigFileReaderWriter =
                new LegacyConfigFileReaderWriter<>(configurationFile, LegacyHiveMQConfigEntity.class);
    }

    public void migrate() {
        try {
            // check whether the migration is needed
            final boolean needsMigration = true;

            if (!needsMigration) {
                return;
            }

            System.err.println("DAFUQ");

            /*

            final List<Class<? extends ProtocolAdapterFactory>> implementations =
                    moduleLoader.findImplementations(ProtocolAdapterFactory.class);

            for (final Class<? extends ProtocolAdapterFactory> factory : implementations) {

            }

             */

            final EventServiceDelegateImpl eventService = new EventServiceDelegateImpl(new InMemoryEventImpl());
            moduleLoader.loadModules();
            final Map<String, ProtocolAdapterFactory<?>> factoryMap =
                    ProtocolAdapterFactoryManager.findAllAdapters(moduleLoader, eventService, true);


            final LegacyHiveMQConfigEntity legacyHiveMQConfigEntity = legacyConfigFileReaderWriter.readConfigFromXML();
            final Map<String, Object> protocolAdapterConfig = legacyHiveMQConfigEntity.getProtocolAdapterConfig();
            for (final Map.Entry<String, Object> stringObjectEntry : protocolAdapterConfig.entrySet()) {
                final String protocolId = stringObjectEntry.getKey();
                final ProtocolAdapterFactory<?> protocolAdapterFactory = factoryMap.get(protocolId);
                if (protocolAdapterFactory == null) {
                    // not much we can do here. We do not have the factory to get the necessary information from
                    //TODO log
                    continue;
                }

                if (protocolAdapterFactory instanceof LegacyConfigConversion) {
                    final LegacyConfigConversion adapterFactory = (LegacyConfigConversion) protocolAdapterFactory;
                    final ConfigTagsTuple configTagsTuple = adapterFactory.tryConvertLegacyConfig(objectMapper,
                            (Map<String, Object>) stringObjectEntry.getValue());
                    System.err.println(configTagsTuple);
                } else {
                    // we can not fix it

                }
            }

        } catch (final Exception e) {
            //TODO
            e.printStackTrace();
        }
    }


}
