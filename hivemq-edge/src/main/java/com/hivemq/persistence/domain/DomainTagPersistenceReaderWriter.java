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
package com.hivemq.persistence.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.domain.xml.DomainTagPersistenceEntity;
import com.hivemq.persistence.domain.xml.DomainTagXmlEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class DomainTagPersistenceReaderWriter {

    private static final Logger log = LoggerFactory.getLogger(DomainTagPersistenceReaderWriter.class);

    private final @NotNull File persistenceFile;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull XmlMapper xmlMapper = new XmlMapper();


    public DomainTagPersistenceReaderWriter(
            final @NotNull File persistenceFile, final @NotNull ObjectMapper objectMapper) {
        this.persistenceFile = persistenceFile;
        this.objectMapper = objectMapper;
        xmlMapper.setAnnotationIntrospector(AnnotationIntrospector.pair(new JacksonXmlAnnotationIntrospector(),
                new JaxbAnnotationIntrospector(TypeFactory.defaultInstance())));
        xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        xmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
    }

    public @NotNull List<DomainTag> readPersistence() {
        final DomainTagPersistenceEntity persistenceEntity = readPersistenceXml();
        return convertToDomainTags(persistenceEntity);
    }

    public synchronized void writePersistence(final @NotNull Collection<DomainTag> tags) {
        if (persistenceFile.exists() && !persistenceFile.canWrite()) {
            log.error("Unable to write to persistence file {}", persistenceFile);
            // TODO likely wrong exception
            throw new UnrecoverableException(false);
        }
        log.debug("Writing persistence file {}", persistenceFile);

        try {
            final DomainTagPersistenceEntity persistenceEntity = convertToEntity(tags);
            final String xml = xmlMapper.writeValueAsString(persistenceEntity);
            Files.writeString(persistenceFile.toPath(), xml);
        } catch (final JsonProcessingException e) {
            log.error("Error while trying to persist the tags in disc. Exception happened during serialization of tags:",
                    e);
            throw new RuntimeException(e);
        } catch (final IOException e) {
            log.error("Error while trying to persist the tags in disc. Exception happened during writing of tag.xml:",
                    e);
            throw new RuntimeException(e);
        }
    }


    private synchronized @NotNull DomainTagPersistenceEntity readPersistenceXml() {
        log.debug("Reading persistence file {}", persistenceFile);

        if (!persistenceFile.exists()) {
            log.debug("No tag persistence is yet present. Creating new empty persistence.");
            writePersistence(List.of());
            return new DomainTagPersistenceEntity();
        }

        try {
            final String xml = Files.readString(persistenceFile.toPath());
            return xmlMapper.readValue(xml, DomainTagPersistenceEntity.class);
        } catch (final IOException e) {
            log.error(
                    "Critical Exception happened during reading of tag persistence. In case this happens during the startup HiveMQ Edge will shutdown. Original Exception: ",
                    e);
            throw new UnrecoverableException();
        }

    }

    private static @NotNull DomainTagPersistenceEntity convertToEntity(final @NotNull Collection<DomainTag> tags) {
        final List<DomainTagXmlEntity> tagsAsEntities =
                tags.stream().map(DomainTagMapper::domainTagEntityFromDomainTag).collect(Collectors.toList());
        return new DomainTagPersistenceEntity(tagsAsEntities);

    }

    private @NotNull List<DomainTag> convertToDomainTags(final @NotNull DomainTagPersistenceEntity persistenceEntity) {
        return persistenceEntity.getTags()
                .stream()
                .map(tagEntity -> DomainTagMapper.domainTagFromDomainTagEntity(tagEntity, objectMapper))
                .collect(Collectors.toList());
    }
}
