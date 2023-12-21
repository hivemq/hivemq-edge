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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.listener.TCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.UDPBroadcastListenerEntity;
import com.hivemq.configuration.entity.listener.UDPListenerEntity;
import com.hivemq.configuration.entity.listener.WebsocketListenerEntity;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.util.EnvVarUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNullElse;

public class ConfigFileReaderWriter {

    private static final Logger log = LoggerFactory.getLogger(ConfigFileReaderWriter.class);
    final static String XSD_SCHEMA = "config.xsd";

    private final @NotNull ConfigurationFile configurationFile;
    private final @NotNull ListenerConfigurator listenerConfigurator;
    private final @NotNull MqttConfigurator mqttConfigurator;
    private final @NotNull RestrictionConfigurator restrictionConfigurator;
    private final @NotNull SecurityConfigurator securityConfigurator;
    private final @NotNull PersistenceConfigurator persistenceConfigurator;
    private final @NotNull MqttsnConfigurator mqttsnConfigurator;
    private final @NotNull BridgeConfigurator bridgeConfigurator;
    private final @NotNull ApiConfigurator apiConfigurator;
    private final @NotNull UnsConfigurator unsConfigurator;
    private final @NotNull DynamicConfigConfigurator dynamicConfigConfigurator;
    private final @NotNull UsageTrackingConfigurator usageTrackingConfigurator;
    private final @NotNull ProtocolAdapterConfigurator protocolAdapterConfigurator;
    private final @NotNull InternalConfigurator internalConfigurator;
    protected @NotNull HiveMQConfigEntity configEntity;
    private final Object lock = new Object();
    private boolean defaultBackupConfig = true;

    public ConfigFileReaderWriter(
            final @NotNull ConfigurationFile configurationFile,
            final @NotNull RestrictionConfigurator restrictionConfigurator,
            final @NotNull SecurityConfigurator securityConfigurator,
            final @NotNull MqttConfigurator mqttConfigurator,
            final @NotNull ListenerConfigurator listenerConfigurator,
            final @NotNull PersistenceConfigurator persistenceConfigurator,
            final @NotNull MqttsnConfigurator mqttsnConfigurator,
            final @NotNull BridgeConfigurator bridgeConfigurator,
            final @NotNull ApiConfigurator apiConfigurator,
            final @NotNull UnsConfigurator unsConfigurator,
            final @NotNull DynamicConfigConfigurator dynamicConfigConfigurator,
            final @NotNull UsageTrackingConfigurator usageTrackingConfigurator,
            final @NotNull ProtocolAdapterConfigurator protocolAdapterConfigurator,
            final @NotNull InternalConfigurator internalConfigurator) {

        this.configurationFile = configurationFile;
        this.listenerConfigurator = listenerConfigurator;
        this.mqttConfigurator = mqttConfigurator;
        this.restrictionConfigurator = restrictionConfigurator;
        this.securityConfigurator = securityConfigurator;
        this.persistenceConfigurator = persistenceConfigurator;
        this.mqttsnConfigurator = mqttsnConfigurator;
        this.bridgeConfigurator = bridgeConfigurator;
        this.apiConfigurator = apiConfigurator;
        this.unsConfigurator = unsConfigurator;
        this.dynamicConfigConfigurator = dynamicConfigConfigurator;
        this.usageTrackingConfigurator = usageTrackingConfigurator;
        this.protocolAdapterConfigurator = protocolAdapterConfigurator;
        this.internalConfigurator = internalConfigurator;
    }

    public @NotNull HiveMQConfigEntity applyConfig() {
        return readConfigFromXML();
    }

    public void writeConfig() {
        writeConfigToXML(configurationFile, defaultBackupConfig);
    }

    public void setDefaultBackupConfig(final boolean defaultBackupConfig) {
        this.defaultBackupConfig = defaultBackupConfig;
    }

    public void writeConfig(@NotNull final ConfigurationFile file, boolean rollConfig) {
        writeConfigToXML(file, rollConfig);
    }

    @NotNull Class<? extends HiveMQConfigEntity> getConfigEntityClass() {
        return HiveMQConfigEntity.class;
    }

    @NotNull List<Class<?>> getInheritedEntityClasses() {
        return ImmutableList.of(
                /* ListenerEntity */
                TCPListenerEntity.class,
                WebsocketListenerEntity.class,
                TlsTCPListenerEntity.class,
                TlsWebsocketListenerEntity.class,
                UDPListenerEntity.class,
                UDPBroadcastListenerEntity.class);
    }

    protected JAXBContext createContext() throws JAXBException {
        final Class<?>[] classes = ImmutableList.<Class<?>>builder()
                .add(getConfigEntityClass())
                .addAll(getInheritedEntityClasses())
                .build()
                .toArray(new Class<?>[0]);

        final JAXBContext context = JAXBContext.newInstance(classes);
        return context;
    }

    private void writeConfigToXML(@NotNull final ConfigurationFile outputFile, boolean rollConfig) {

        synchronized (lock) {

            //-- Checks need to be inside sync block as could be created by the initialisation
            if (configEntity == null) {
                log.error("Unable to write uninitialized configuration.");
                throw new UnrecoverableException(false);
            }

            if (outputFile.file().isEmpty()) {
                log.error("No configuration file present.");
                throw new UnrecoverableException(false);
            }
            if (outputFile.file().get().exists() && !outputFile.file().get().canWrite()) {
                log.error("Unable to write to supplied configuration file {}", outputFile.file().get());
                throw new UnrecoverableException(false);
            }

            try {
                File configFile = outputFile.file().get();
                log.debug("Writing configuration file {}", configFile.getAbsolutePath());
                //write the backup of the file before rewriting
                if (rollConfig) {
                    backupConfig(configFile, 5);
                }
                FileWriter fileWriter = new FileWriter(outputFile.file().get(), StandardCharsets.UTF_8);
                writeConfigToXML(fileWriter);
            } catch (IOException e) {
                log.error("Error writing file:", e);
                throw new UnrecoverableException(false);
            }
        }
    }

    protected void backupConfig(@NotNull File configFile, int maxBackFiles) throws IOException {
        int idx = 0;
        String fileNameExclExt = com.hivemq.util.Files.getFileNameExcludingExtension(configFile.getName());
        String fileExtension = com.hivemq.util.Files.getFileExtension(configFile.getName());
        String copyPath = com.hivemq.util.Files.getFilePathExcludingFile(configFile.getAbsolutePath());

        String copyFilename = null;
        File copyFile = null;
        do {
            copyFilename = String.format("%s_%d.%s", fileNameExclExt, ++idx, fileExtension);
            copyFile = new File(copyPath, copyFilename);
        } while(idx < maxBackFiles && copyFile.exists());

        if(copyFile.exists()){

            //-- use the oldest available backup index
            File[] backupFiles = new File(copyPath).listFiles(child -> child.isFile() &&
                    child.getName().startsWith(fileNameExclExt) &&
                    child.getName().endsWith(fileExtension));
            Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
            copyFile = backupFiles[0];
        }
        if(log.isDebugEnabled()){
            log.debug("Rolling backup of configuration file to {}", copyFile.getName());
        }
        FileUtils.copyFile(configFile, copyFile);
    }

    public void writeConfigToXML(@NotNull final Writer writer) {
        synchronized (lock) {
            try {
                JAXBContext context = createContext();
                Marshaller marshaller = context.createMarshaller();
                final Schema schema = loadSchema();
                if (schema != null) {
                    marshaller.setSchema(schema);
                    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, XSD_SCHEMA);
                }
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(configEntity, writer);
            } catch (JAXBException | IOException | SAXException e) {
                log.error("Original error message:", e);
                throw new UnrecoverableException(false);
            }
        }
    }

    private @NotNull HiveMQConfigEntity readConfigFromXML() {
        if (configurationFile.file().isEmpty()) {
            log.error("No configuration file present. Shutting down HiveMQ Edge.");
            throw new UnrecoverableException(false);
        }

        final File configFile = configurationFile.file().get();
        log.debug("Reading configuration file {}", configFile);
        final List<ValidationEvent> validationErrors = new ArrayList<>();

        synchronized (lock) {
            try {
                final JAXBContext context = createContext();
                final Unmarshaller unmarshaller = context.createUnmarshaller();
                final Schema schema = loadSchema();
                if (schema != null) {
                    unmarshaller.setSchema(schema);
                }

                //replace environment variable placeholders
                String configFileContent = new String(Files.readAllBytes(configFile.toPath()), StandardCharsets.UTF_8);
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

                final JAXBElement<? extends HiveMQConfigEntity> result =
                        unmarshaller.unmarshal(streamSource, getConfigEntityClass());

                if (!validationErrors.isEmpty()) {
                    throw new JAXBException("Parsing failed");
                }

                configEntity = result.getValue();
                if (configEntity == null) {
                    throw new JAXBException("Result is null");
                }
                setConfiguration(configEntity);
                return configEntity;

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
                log.error("Could not read the configuration file {}. Exiting HiveMQ Edge.",
                        configFile.getAbsolutePath());
                log.debug("Original error message:", e);
                throw new UnrecoverableException(false);
            }
        }
    }

    void setConfiguration(@NotNull final HiveMQConfigEntity config) {
        listenerConfigurator.setListenerConfig(config.getMqttListenerConfig(), config.getMqttsnListenerConfig());
        mqttConfigurator.setMqttConfig(config.getMqttConfig());
        restrictionConfigurator.setRestrictionsConfig(config.getRestrictionsConfig());
        securityConfigurator.setSecurityConfig(config.getSecurityConfig());
        persistenceConfigurator.setPersistenceConfig(config.getPersistenceConfig());
        mqttsnConfigurator.setMqttsnConfig(config.getMqttsnConfig());
        bridgeConfigurator.setBridgeConfig(config.getBridgeConfig());
        apiConfigurator.setApiConfig(config.getApiConfig());
        protocolAdapterConfigurator.setConfigs(config.getProtocolAdapterConfig());
        apiConfigurator.setApiConfig(config.getApiConfig());
        unsConfigurator.setUnsConfig(config.getUns());
        dynamicConfigConfigurator.setConfig(config.getGatewayConfig());
        usageTrackingConfigurator.setConfig(config.getUsageTracking());
        internalConfigurator.setConfig(config.getInternal());
    }

    public void syncConfiguration() {
        Preconditions.checkNotNull(configEntity, "Configuration must be loaded to be synchronized");
        unsConfigurator.syncUnsConfig(configEntity.getUns());
        bridgeConfigurator.syncBridgeConfig(configEntity.getBridgeConfig());
        protocolAdapterConfigurator.syncConfigs(configEntity.getProtocolAdapterConfig());
    }

    protected Schema loadSchema() throws IOException, SAXException {
        final URL resource = ConfigFileReaderWriter.class.getResource("/" + XSD_SCHEMA);
        if (resource != null) {
            try (final InputStream is = uncachedStream(resource)) {
                final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                final Schema schema = sf.newSchema(new StreamSource(is));
                return schema;
            }
        }
        return null;
    }

    private @NotNull InputStream uncachedStream(final @NotNull URL xsd) throws IOException {
        final URLConnection urlConnection = xsd.openConnection();
        urlConnection.setUseCaches(false);
        return urlConnection.getInputStream();
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
