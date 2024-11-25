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
package com.hivemq.configuration.migration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.FromEdgeMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.TagEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.ioc.ConfigurationFileProvider;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.configuration.reader.LegacyConfigFileReaderWriter;
import com.hivemq.edge.impl.events.EventServiceDelegateImpl;
import com.hivemq.edge.impl.events.InMemoryEventImpl;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
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

    public static final String XSLT_INPUT = "\n" +
            "<xsl:stylesheet version=\"1.0\"\n" +
            "                xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n" +
            "    <xsl:template match=\"/\">\n" +
            "            <xsl:for-each select=\"/hivemq/protocol-adapters/*[not(starts-with(local-name(),'protocol-adapter'))]\">\n" +
            "                        <xsl:value-of select=\"name()\"/>" +
            "            </xsl:for-each>\n" +
            "    </xsl:template>\n" +
            "</xsl:stylesheet>\n";


    @Inject
    public ConfigurationMigrator(
            final @NotNull SystemInformation systemInformation, final @NotNull ModuleLoader moduleLoader) {
        this.systemInformation = systemInformation;
        this.moduleLoader = moduleLoader;
        final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);
        this.legacyConfigFileReaderWriter = new LegacyConfigFileReaderWriter<>(configurationFile,
                LegacyHiveMQConfigEntity.class,
                HiveMQConfigEntity.class);
        objectMapper.configure(MapperFeature.AUTO_DETECT_GETTERS, false);
    }

    public void migrate() {
        try {
            final ConfigurationFile configurationFile = ConfigurationFileProvider.get(systemInformation);
            if (!needsMigration(configurationFile)) {
                log.info("No configuration migration needed.");
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
            log.error("[CONFIG MIGRATION] An exception was raised during automatic migration of the configuration file.");
            log.debug("Original Exception:", e);
        }
    }

    private @NotNull Optional<ProtocolAdapterEntity> parseProtocolAdapterEntity(
            final Map.Entry<String, Object> stringObjectEntry,
            final Map<String, ProtocolAdapterFactory<?>> factoryMap) {
        final String protocolId = stringObjectEntry.getKey();
        final ProtocolAdapterFactory<?> protocolAdapterFactory = factoryMap.get(protocolId);
        if (protocolAdapterFactory == null) {
            // not much we can do here. We do not have the factory to get the necessary information from
            log.error(
                    "[CONFIG MIGRATION] While migration the configuration, no protocol factory for protocolId '{}' was found. This adapter will be skipped and must be migrated by hand.",
                    protocolId);
            return Optional.empty();
        }

        if (protocolAdapterFactory instanceof LegacyConfigConversion) {
            final LegacyConfigConversion adapterFactory = (LegacyConfigConversion) protocolAdapterFactory;
            final ConfigTagsTuple configTagsTuple = adapterFactory.tryConvertLegacyConfig(objectMapper,
                    (Map<String, Object>) stringObjectEntry.getValue());

            final List<FromEdgeMappingEntity> fromEdgeMappingEntities = configTagsTuple.getPollingContexts()
                    .stream()
                    .map(FromEdgeMappingEntity::from)
                    .collect(Collectors.toList());

            final List<TagEntity> tagEntities = configTagsTuple.getTags()
                    .stream().map(tag -> TagEntity.fromAdapterTag(tag, objectMapper))
                    .collect(Collectors.toList());

            return Optional.of(new ProtocolAdapterEntity(configTagsTuple.getAdapterId(),
                    protocolId,
                    objectMapper.convertValue(configTagsTuple.getConfig(), new TypeReference<>() {
                    }),
                    fromEdgeMappingEntities, List.of(), tagEntities,
                    // field mappings are always empty as they did not exist before.
                    List.of()));
        } else {
            log.error(
                    "[CONFIG MIGRATION] A legacy config for protocolId '{}' was found during migration, but the adapter factory does not implement the necessary interface '{}' for automatic migration.",
                    protocolId,
                    LegacyConfigConversion.class.getSimpleName());
            return Optional.empty();
        }
    }


    @VisibleForTesting
    static boolean needsMigration(final @NotNull ConfigurationFile configurationFile) {
        try {
            final File configFile = configurationFile.file().get();
            final Source xmlSource = new StreamSource(configFile);
            final Source xsltSource =
                    new StreamSource(new ByteArrayInputStream(XSLT_INPUT.getBytes(StandardCharsets.UTF_8)));
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer(xsltSource);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            final StringWriter stringWriter = new StringWriter();
            final StreamResult result = new StreamResult(stringWriter);
            transformer.transform(xmlSource, result);
            return !stringWriter.getBuffer().toString().isBlank();
        } catch (final TransformerException e) {
            log.error(
                    "[CONFIG MIGRATION] Exception while determining whether a config migration is needed. No automatic config migration will happen.");
            log.debug("Original Exception:", e);
            return false;
        }
    }

}
