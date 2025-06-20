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
package com.hivemq.persistence.topicfilter;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.module.jakarta.xmlbind.JakartaXmlBindAnnotationIntrospector;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.persistence.topicfilter.xml.TopicFilterPersistenceEntity;
import com.hivemq.persistence.topicfilter.xml.TopicFilterXmlEntity;
import com.hivemq.persistence.util.JaxbUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

;

@Singleton
public class TopicFilterPersistenceReaderWriter {

    private static final Logger log = LoggerFactory.getLogger(TopicFilterPersistenceReaderWriter.class);
    public static final String PERSISTENCE_FILE_NAME = "topic-filters.xml";
    private final @NotNull File persistenceFile;
    private final @NotNull XmlMapper xmlMapper = new XmlMapper();

    @Inject
    public TopicFilterPersistenceReaderWriter(
            final @NotNull SystemInformation systemInformation) {
        this.persistenceFile = new File(systemInformation.getSecondaryHiveMQHomeFolder(), PERSISTENCE_FILE_NAME);
        xmlMapper.setAnnotationIntrospector(AnnotationIntrospector.pair(new JacksonXmlAnnotationIntrospector(),
                new JakartaXmlBindAnnotationIntrospector(TypeFactory.defaultInstance())));
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
    }

    public @NotNull List<TopicFilterPojo> readPersistence() {
        final TopicFilterPersistenceEntity persistenceEntity = readPersistenceXml();
        return convertToTopicFilters(persistenceEntity);
    }

    public synchronized void writePersistence(final @NotNull Collection<TopicFilterPojo> topicFilters) {
        if (persistenceFile.exists() && !persistenceFile.canWrite()) {
            log.error("Unable to write to persistence file {}, because it is not writable.", persistenceFile);
            throw new UnrecoverableException(false);
        }
        log.debug("Writing persistence file {}", persistenceFile);

        try {
            final TopicFilterPersistenceEntity persistenceEntity = convertToEntity(topicFilters);
            JaxbUtils.marshal(persistenceEntity, persistenceFile, marshaller -> {
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, TopicFilterPersistenceEntity.SCHEMA_LOCATION);
                marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION,
                        TopicFilterPersistenceEntity.NO_NAMESPACE_SCHEMA_LOCATION);
            });
        } catch (final JAXBException e) {
            log.error(
                    "Error while trying to persist the topic filters on disc. Exception happened during serialization of topic filters:",
                    e);
            throw new RuntimeException(e);
        } catch (final Exception e) {
            log.error("Error while trying to persist the topic filters on disc. Exception happened during writing of " +
                    PERSISTENCE_FILE_NAME +
                    ":", e);
            throw new RuntimeException(e);
        }
    }


    private synchronized @NotNull TopicFilterPersistenceEntity readPersistenceXml() {
        log.debug("Reading persistence file {}", persistenceFile);

        if (!persistenceFile.exists()) {
            log.debug("No tag persistence is yet present. Creating new empty persistence.");
            writePersistence(List.of());
            return new TopicFilterPersistenceEntity();
        }

        try {
            final String xml = Files.readString(persistenceFile.toPath());
            return JaxbUtils.unmarshal(xml, TopicFilterPersistenceEntity.class);
        } catch (final IOException | JAXBException e) {
            log.error(
                    "Critical Exception happened during reading of topic filter persistence. In case this happens during the startup HiveMQ Edge will shutdown. Original Exception: ",
                    e);
            throw new UnrecoverableException();
        }
    }

    private static @NotNull TopicFilterPersistenceEntity convertToEntity(final @NotNull Collection<TopicFilterPojo> tags) {
        final List<TopicFilterXmlEntity> tagsAsEntities =
                tags.stream().map(TopicFilterMapper::topicFilterEntityFromDomainTag).collect(Collectors.toList());
        return new TopicFilterPersistenceEntity(tagsAsEntities);

    }

    private @NotNull List<TopicFilterPojo> convertToTopicFilters(final @NotNull TopicFilterPersistenceEntity persistenceEntity) {
        return persistenceEntity.getTopicFilters()
                .stream()
                .map(TopicFilterMapper::topicFilterFromDomainTagEntity)
                .collect(Collectors.toList());
    }
}
