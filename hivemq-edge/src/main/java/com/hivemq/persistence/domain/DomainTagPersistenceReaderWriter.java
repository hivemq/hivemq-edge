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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

public class DomainTagPersistenceReaderWriter {

    private static final Logger log = LoggerFactory.getLogger(DomainTagPersistenceReaderWriter.class);

    private final @NotNull File persistenceFile;
    private final @NotNull ObjectMapper objectMapper;
    private final Object lock = new Object();

    public DomainTagPersistenceReaderWriter(
            final @NotNull File persistenceFile, final @NotNull ObjectMapper objectMapper) {
        this.persistenceFile = persistenceFile;
        this.objectMapper = objectMapper;
    }

    public @NotNull List<DomainTag> readPersistence() {
        final DomainTagPersistenceEntity persistenceEntity = readPersistenceXml();
        return convertToDomainTags(persistenceEntity);
    }

    public void writePersistence(final @NotNull Collection<DomainTag> tags) {

        synchronized (lock) {
            if (persistenceFile.exists() && !persistenceFile.canWrite()) {
                log.error("Unable to write to persistence file {}", persistenceFile);
                // TODO likely wrong exception
                throw new UnrecoverableException(false);
            }

            try {
                log.debug("Writing persistence file {}", persistenceFile);
                final FileWriter fileWriter = new FileWriter(persistenceFile, StandardCharsets.UTF_8);
                writePersistence(fileWriter, tags);
            } catch (final IOException e) {
                log.error("Error writing file:", e);
                throw new UnrecoverableException(false);
            }
        }
    }


    private @NotNull Class<? extends DomainTagPersistenceEntity> getPersistenceEntityClass() {
        return DomainTagPersistenceEntity.class;
    }

    private @NotNull List<Class<?>> getInheritedEntityClasses() {
        return ImmutableList.of(DomainTagXmlEntity.class);
    }

    private @NotNull JAXBContext createContext() throws JAXBException {
        final Class<?>[] classes = ImmutableList.<Class<?>>builder()
                .add(getPersistenceEntityClass())
                .addAll(getInheritedEntityClasses())
                .build()
                .toArray(new Class<?>[0]);

        return JAXBContext.newInstance(classes);
    }


    private void writePersistence(@NotNull final Writer writer, final @NotNull Collection<DomainTag> tags) {
        synchronized (lock) {
            try {
                final JAXBContext context = createContext();
                final Marshaller marshaller = context.createMarshaller();
                final DomainTagPersistenceEntity persistenceEntity = convertToEntity(tags);
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(persistenceEntity, writer);
            } catch (final JAXBException e) {
                log.error("Original error message:", e);
                throw new UnrecoverableException(false);
            }
        }
    }

    private @NotNull DomainTagPersistenceEntity convertToEntity(final @NotNull Collection<DomainTag> tags) {
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

    private @NotNull DomainTagPersistenceEntity readPersistenceXml() {
        log.debug("Reading persistence file {}", persistenceFile);
        final List<ValidationEvent> validationErrors = new ArrayList<>();

        if(!persistenceFile.exists()){
            log.debug("No tag persistence is yet present. Creating new empty persistence.");
            writePersistence(List.of());
            return new DomainTagPersistenceEntity();
        }

        synchronized (lock) {
            try {
                final JAXBContext context = createContext();
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                final String persistenceContent = Files.readString(persistenceFile.toPath());
                final ByteArrayInputStream is =
                        new ByteArrayInputStream(persistenceContent.getBytes(StandardCharsets.UTF_8));
                final StreamSource streamSource = new StreamSource(is);

                unmarshaller.setEventHandler(e -> {
                    if (e.getSeverity() > ValidationEvent.ERROR) {
                        validationErrors.add(e);
                    }
                    return true;
                });

                final JAXBElement<? extends DomainTagPersistenceEntity> result =
                        unmarshaller.unmarshal(streamSource, getPersistenceEntityClass());

                if (!validationErrors.isEmpty()) {
                    throw new JAXBException("Parsing failed");
                }
                return result.getValue();

            } catch (final JAXBException | IOException e) {
                final StringBuilder messageBuilder = new StringBuilder();

                if (validationErrors.isEmpty()) {
                    messageBuilder.append("of the following error: ");
                    messageBuilder.append(requireNonNullElse(e.getCause(), e));
                } else {
                    messageBuilder.append("of the following errors:");
                    for (final ValidationEvent validationError : validationErrors) {
                        messageBuilder.append(System.lineSeparator()).append(toValidationMessage(validationError));
                    }
                }
                log.error("Not able to parse persistence file because {}", messageBuilder);
                throw new UnrecoverableException(false);
            } catch (final Exception e) {

                if (e.getCause() instanceof UnrecoverableException) {
                    if (((UnrecoverableException) e.getCause()).isShowException()) {
                        log.error("An unrecoverable Exception occurred. Exiting HiveMQ", e);
                        log.debug("Original error message:", e);
                    }
                    System.exit(1);
                }
                log.error("Could not read the persistence file {}. Exiting HiveMQ Edge.",
                        persistenceFile.getAbsolutePath());
                log.debug("Original error message:", e);
                throw new UnrecoverableException(false);
            }
        }
    }

    private @NotNull String toValidationMessage(final @NotNull ValidationEvent validationEvent) {
        final StringBuilder validationMessageBuilder = new StringBuilder();
        final ValidationEventLocator locator = validationEvent.getLocator();
        if (locator == null) {
            validationMessageBuilder.append("\t- XML schema violation caused by: \"")
                    .append(validationEvent.getMessage())
                    .append("\"");
        } else {
            validationMessageBuilder.append("\t- XML schema violation in line '")
                    .append(locator.getLineNumber())
                    .append("' and column '")
                    .append(locator.getColumnNumber())
                    .append("' caused by: \"")
                    .append(validationEvent.getMessage())
                    .append("\"");
        }
        return validationMessageBuilder.toString();
    }
}
