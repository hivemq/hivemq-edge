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
package com.hivemq.configuration.reader;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.entity.listener.TCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.UDPBroadcastListenerEntity;
import com.hivemq.configuration.entity.listener.UDPListenerEntity;
import com.hivemq.configuration.entity.listener.WebsocketListenerEntity;
import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import com.hivemq.util.render.EnvVarUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventLocator;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNullElse;

public class LegacyConfigFileReaderWriter<LEGACY_CONFIG_CLASS, CURRENT_CONFIG_CLASS> {

    private static final Logger log = LoggerFactory.getLogger(LegacyConfigFileReaderWriter.class);
    private final @NotNull ConfigurationFile configurationFile;
    private final @NotNull Class<LEGACY_CONFIG_CLASS> legacyConfigClass;
    private final @NotNull Class<CURRENT_CONFIG_CLASS> currentConfigClassClass;

    public LegacyConfigFileReaderWriter(
            final @NotNull ConfigurationFile configurationFile,
            final @NotNull Class<LEGACY_CONFIG_CLASS> legacyConfigClass,
            final @NotNull Class<CURRENT_CONFIG_CLASS> currentConfigClassClass) {
        this.configurationFile = configurationFile;
        this.legacyConfigClass = legacyConfigClass;
        this.currentConfigClassClass = currentConfigClassClass;
    }

    @NotNull
    List<Class<?>> getInheritedEntityClasses() {
        return ImmutableList.of(
                /* ListenerEntity */
                TCPListenerEntity.class,
                WebsocketListenerEntity.class,
                TlsTCPListenerEntity.class,
                TlsWebsocketListenerEntity.class,
                UDPListenerEntity.class,
                UDPBroadcastListenerEntity.class);
    }

    protected @NotNull JAXBContext createContext() throws JAXBException {
        final Class<?>[] classes = ImmutableList.<Class<?>>builder()
                .add(legacyConfigClass)
                .add(currentConfigClassClass)
                .addAll(getInheritedEntityClasses())
                .add(FieldMappingEntity.class)
                .build()
                .toArray(new Class<?>[0]);

        return JAXBContext.newInstance(classes);
    }

    public synchronized void writeConfigToXML(final @NotNull CURRENT_CONFIG_CLASS config) {
        try {
            final File configFile = configurationFile.file().get();
            log.debug("Writing configuration file {}", configFile.getAbsolutePath());
            final FileWriter fileWriter = new FileWriter(configurationFile.file().get(), StandardCharsets.UTF_8);
            writeConfigToXML(fileWriter, config);
        } catch (final IOException e) {
            log.error("Error writing file:", e);
            throw new UnrecoverableException(false);
        }
    }

    private synchronized void writeConfigToXML(
            final @NotNull Writer writer,
            final @NotNull CURRENT_CONFIG_CLASS config) {
        try {
            final JAXBContext context = createContext();
            final Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(config, writer);
        } catch (final JAXBException e) {
            log.error("Original error message:", e);
            throw new UnrecoverableException(false);
        }

    }

    public synchronized @NotNull LEGACY_CONFIG_CLASS readConfigFromXML() {
        if (configurationFile.file().isEmpty()) {
            log.error("No configuration file present. Shutting down HiveMQ Edge.");
            throw new UnrecoverableException(false);
        }
        final File configFile = configurationFile.file().get();
        log.debug("Reading configuration file {}", configFile);
        final List<ValidationEvent> validationErrors = new ArrayList<>();

        try {
            final JAXBContext context = createContext();
            final Unmarshaller unmarshaller = context.createUnmarshaller();
            //replace environment variable placeholders
            String configFileContent = Files.readString(configFile.toPath());
            configFileContent = EnvVarUtil.replaceEnvironmentVariablePlaceholders(configFileContent);
            final ByteArrayInputStream is =
                    new ByteArrayInputStream(configFileContent.getBytes(StandardCharsets.UTF_8));
            final StreamSource streamSource = new StreamSource(is);

            unmarshaller.setEventHandler(e -> {
                if (e.getSeverity() > ValidationEvent.ERROR) {
                    validationErrors.add(e);
                }
                return true;
            });

            final JAXBElement<? extends LEGACY_CONFIG_CLASS> result =
                    unmarshaller.unmarshal(streamSource, legacyConfigClass);

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
            log.error("Not able to parse configuration file because {}", messageBuilder);
            throw new UnrecoverableException(false);
        } catch (final Exception e) {

            if (e.getCause() instanceof UnrecoverableException) {
                if (((UnrecoverableException) e.getCause()).isShowException()) {
                    log.error("An unrecoverable Exception occurred. Exiting HiveMQ", e);
                    log.debug("Original error message:", e);
                }
                System.exit(1);
            }
            log.error("Could not read the configuration file {}. Exiting HiveMQ Edge.", configFile.getAbsolutePath());
            log.debug("Original error message:", e);
            throw new UnrecoverableException(false);
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
