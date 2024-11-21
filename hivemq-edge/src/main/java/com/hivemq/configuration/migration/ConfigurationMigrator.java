package com.hivemq.configuration.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.FromEdgeMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.ioc.ConfigurationFileProvider;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.configuration.reader.LegacyConfigFileReaderWriter;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
@Singleton
public class ConfigurationMigrator {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationMigrator.class);
    private final @NotNull SystemInformation systemInformation;
    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull LegacyConfigFileReaderWriter<LegacyHiveMQConfigEntity, HiveMQConfigEntity>
            legacyConfigFileReaderWriter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Inject
    public ConfigurationMigrator(final @NotNull SystemInformation systemInformation, final ModuleLoader moduleLoader) {
        this.systemInformation = systemInformation;
        this.moduleLoader = moduleLoader;
        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);
        this.legacyConfigFileReaderWriter = new LegacyConfigFileReaderWriter<>(configurationFile,
                LegacyHiveMQConfigEntity.class,
                HiveMQConfigEntity.class);
    }

    public void migrate() {
        try {
            // TODO check whether the migration is needed
            final boolean needsMigration = true;

            if (!needsMigration) {
                return;
            }

            final EventServiceDelegateImpl eventService = new EventServiceDelegateImpl(new InMemoryEventImpl());
            moduleLoader.loadModules();
            final Map<String, ProtocolAdapterFactory<?>> factoryMap =
                    ProtocolAdapterFactoryManager.findAllAdapters(moduleLoader, eventService, true);
            final LegacyHiveMQConfigEntity legacyHiveMQConfigEntity = legacyConfigFileReaderWriter.readConfigFromXML();
            final Map<String, Object> protocolAdapterConfig = legacyHiveMQConfigEntity.getProtocolAdapterConfig();
            final List<ProtocolAdapterEntity> protocolAdapterEntities = protocolAdapterConfig.entrySet()
                    .stream()
                    .map(entry -> parseProtocolAdapterEntity(entry, factoryMap))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

            final HiveMQConfigEntity hiveMQConfigEntity = legacyHiveMQConfigEntity.to(protocolAdapterEntities);
            legacyConfigFileReaderWriter.writeConfigToXML(hiveMQConfigEntity);
        } catch (final Exception e) {
            //TODO
            e.printStackTrace();
        }
    }

    private Optional<ProtocolAdapterEntity> parseProtocolAdapterEntity(
            final Map.Entry<String, Object> stringObjectEntry,
            final Map<String, ProtocolAdapterFactory<?>> factoryMap) {
        final String protocolId = stringObjectEntry.getKey();
        final ProtocolAdapterFactory<?> protocolAdapterFactory = factoryMap.get(protocolId);
        if (protocolAdapterFactory == null) {
            // not much we can do here. We do not have the factory to get the necessary information from
            //TODO log
            log.error("Fucked");
            return Optional.empty();
        }

        if (protocolAdapterFactory instanceof LegacyConfigConversion) {
            final LegacyConfigConversion adapterFactory = (LegacyConfigConversion) protocolAdapterFactory;
            final ConfigTagsTuple configTagsTuple = adapterFactory.tryConvertLegacyConfig(objectMapper,
                    (Map<String, Object>) stringObjectEntry.getValue());
            System.err.println(configTagsTuple);

            final List<FromEdgeMappingEntity> fromEdgeMappingEntities = configTagsTuple.getPollingContexts()
                    .stream()
                    .map(FromEdgeMappingEntity::from)
                    .collect(Collectors.toList());

            final List<Map<String, Object>> tagsAsMaps = configTagsTuple.getTags()
                    .stream()
                    .map(tag -> objectMapper.convertValue(tag, new TypeReference<Map<String, Object>>() {
                    }))
                    .collect(Collectors.toList());

            return Optional.of(new ProtocolAdapterEntity(configTagsTuple.getAdapterId(),
                    protocolId,
                    objectMapper.convertValue(configTagsTuple.getConfig(), new TypeReference<>() {
                    }),
                    fromEdgeMappingEntities,
                    List.of(),
                    tagsAsMaps));
        } else {
            log.error("Fucked 2");
            return Optional.empty();
        }
    }


}
