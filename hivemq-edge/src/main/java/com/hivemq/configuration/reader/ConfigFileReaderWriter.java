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
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.entity.api.ApiTlsEntity;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.configuration.entity.listener.TCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.UDPBroadcastListenerEntity;
import com.hivemq.configuration.entity.listener.UDPListenerEntity;
import com.hivemq.configuration.entity.listener.WebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.util.ThreadFactoryUtil;
import com.hivemq.util.render.EnvVarUtil;
import com.hivemq.util.render.FileFragmentUtil;
import com.hivemq.util.render.IfUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNullElse;

public class ConfigFileReaderWriter {

    private static final Logger log = LoggerFactory.getLogger(ConfigFileReaderWriter.class);
    static final String XSD_SCHEMA = "config.xsd";

    private final @NotNull ConfigurationFile configurationFile;
    protected volatile @NotNull HiveMQConfigEntity configEntity;
    private final Object lock = new Object();
    private boolean defaultBackupConfig = true;
    private volatile @Nullable ScheduledExecutorService scheduledExecutorService = null;
    private final @NotNull List<Configurator<?>> configurators;
    private final @NotNull Map<Path, Long> fragmentToModificationTime = new ConcurrentHashMap<>();

    private final @NotNull BridgeExtractor bridgeExtractor;
    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor;
    private final @NotNull AtomicLong lastWrite = new AtomicLong(0L);

    public ConfigFileReaderWriter(
            final @NotNull ConfigurationFile configurationFile,
            final @NotNull List<Configurator<?>> configurators) {
        this.configurationFile = configurationFile;
        this.configurators = configurators;
        this.bridgeExtractor = new BridgeExtractor(this);
        this.protocolAdapterExtractor = new ProtocolAdapterExtractor(this);
    }

    public HiveMQConfigEntity applyConfig() {
        if (configurationFile.file().isEmpty()) {
            log.error("No configuration file present. Shutting down HiveMQ Edge.");
            throw new UnrecoverableException(false);
        }

        final File configFile = configurationFile.file().get();
        final HiveMQConfigEntity hiveMQConfigEntity = readConfigFromXML(configFile);
        this.configEntity = hiveMQConfigEntity;
        setConfiguration(hiveMQConfigEntity);

        return hiveMQConfigEntity;
    }

    public @NotNull BridgeExtractor getBridgeExtractor() {
        return bridgeExtractor;
    }

    public @NotNull ProtocolAdapterExtractor getProtocolAdapterExtractor() {
        return protocolAdapterExtractor;
    }

    public void applyConfigAndWatch(final long checkIntervalInMs) {
        if(scheduledExecutorService != null) {
            throw new IllegalStateException("Config watch was already started");
        }
        if (configurationFile.file().isEmpty()) {
            log.error("No configuration file present. Shutting down HiveMQ Edge.");
            throw new UnrecoverableException(false);
        }

        final File configFile = configurationFile.file().get();
        final long interval = (checkIntervalInMs > 0) ? checkIntervalInMs : 0;
        log.info("Rereading config file every {} ms", interval);

        final AtomicLong fileModified = new AtomicLong();
        final Map<Path, Long> fileModificationTimestamps;

        final HiveMQConfigEntity entity = applyConfig();
        fileModificationTimestamps = findFilesToWatch(entity);
        final AtomicLong fileModifiedTimestamp = new AtomicLong();
        try {
            fileModifiedTimestamp.set(Files.getLastModifiedTime(configFile.toPath()).toMillis());
        } catch (final IOException e) {
            throw new RuntimeException("Unable to read last modified time from " + configFile.getAbsolutePath(), e);
        }

        final ThreadFactory threadFactory = ThreadFactoryUtil.create("hivemq-edge-config-watch-%d");
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        scheduledExecutorService.scheduleAtFixedRate(
                () -> checkMonitoredFilesForChanges(configFile, fileModified, fileModificationTimestamps)
                , 0, interval, TimeUnit.MILLISECONDS);
        this.scheduledExecutorService = scheduledExecutorService;
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopWatching));
    }

    private void checkMonitoredFilesForChanges(
            final File configFile,
            final @NotNull AtomicLong fileModified,
            final @NotNull Map<Path, Long> fileModificationTimestamps) {
        try {
            final boolean devmode = "true".equals(System.getProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE));

            if(!devmode) {
                final Map<Path, Long> pathsToCheck = new HashMap<>(fragmentToModificationTime);

                pathsToCheck.putAll(fileModificationTimestamps);

                pathsToCheck.entrySet().forEach(pathToTs -> {
                    try {
                        if (Files.getFileAttributeView(pathToTs.getKey().toRealPath(LinkOption.NOFOLLOW_LINKS), BasicFileAttributeView.class).readAttributes().lastModifiedTime().toMillis() > pathToTs.getValue()) {
                            log.error("Restarting because a required file was updated: {}", pathToTs.getKey());
                            System.exit(0);
                        }
                    } catch (final IOException e) {
                        throw new RuntimeException("Unable to read last modified time for " + pathToTs.getKey(), e);
                    }
                });
            }
            final long modified = Files.getLastModifiedTime(configFile.toPath()).toMillis();
            if (modified > fileModified.get()) {
                fileModified.set(modified);
                final HiveMQConfigEntity hiveMQConfigEntity = readConfigFromXML(configFile);
                this.configEntity = hiveMQConfigEntity;
                if(!setConfiguration(hiveMQConfigEntity)) {
                    if(!devmode) {
                        log.error("Restarting because new config can't be hot-reloaded");
                        System.exit(0);
                    } else {
                        log.error("TESTMODE, NOT RESTARTING");
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Path, Long> findFilesToWatch(final HiveMQConfigEntity entity) {
        final Map<Path, Long> paths = new ConcurrentHashMap<>();

        entity.getBridgeConfig().forEach(cfg -> {
            final BridgeTlsEntity tls = cfg.getRemoteBroker().getTls();
            if(tls != null) {
                final KeystoreEntity keyStore = cfg.getRemoteBroker().getTls().getKeyStore();
                if(keyStore != null) {
                    final Path path = Paths.get(keyStore.getPath());
                    try {
                        paths.put(path, Files.getLastModifiedTime(path).toMillis());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                final TruststoreEntity trustStore = cfg.getRemoteBroker().getTls().getTrustStore();
                if(trustStore != null) {
                    final Path path = Paths.get(trustStore.getPath());
                    try {
                        paths.put(path, Files.getLastModifiedTime(path).toMillis());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        final ApiTlsEntity tls = entity.getApiConfig().getTls();
        if (tls != null && tls.getKeystoreEntity() != null) {
            final Path path = Paths.get(tls.getKeystoreEntity().getPath());
            try {
                paths.put(path, Files.getLastModifiedTime(path).toMillis());
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return paths;
    }

    public void stopWatching() {
        final ScheduledExecutorService scheduledExecutorService = this.scheduledExecutorService;
        if(scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    public void writeConfig() {
        writeConfigToXML(configurationFile, defaultBackupConfig);
    }

    public void writeConfigWithSync() {
        if (log.isTraceEnabled()) {
            log.trace("flushing configuration changes to entity layer");
        }
        try {
            syncConfiguration();
            if (configEntity.getGatewayConfig().isMutableConfigurationEnabled()) {
                writeConfig();
            }
        } catch (final Exception e){
            log.error("Configuration file sync failed: ", e);
        } finally {
            lastWrite.set(System.currentTimeMillis());
        }
    }

    public @NotNull Long getLastWrite() {
        return lastWrite.get();
    }

    public void setDefaultBackupConfig(final boolean defaultBackupConfig) {
        this.defaultBackupConfig = defaultBackupConfig;
    }

    public void writeConfig(final @NotNull ConfigurationFile file, final boolean rollConfig) {
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
                .add(FieldMappingEntity.class)
                .build()
                .toArray(new Class<?>[0]);

        final JAXBContext context = JAXBContext.newInstance(classes);
        return context;
    }

    private void writeConfigToXML(final @NotNull ConfigurationFile outputFile, final boolean rollConfig) {

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
                final File configFile = outputFile.file().get();
                log.debug("Writing configuration file {}", configFile.getAbsolutePath());
                //write the backup of the file before rewriting
                if (rollConfig) {
                    backupConfig(configFile, 5);
                }
                final FileWriter fileWriter = new FileWriter(outputFile.file().get(), StandardCharsets.UTF_8);
                writeConfigToXML(fileWriter);
            } catch (final IOException e) {
                log.error("Error writing file:", e);
                throw new UnrecoverableException(false);
            }
        }
    }

    protected void backupConfig(final @NotNull File configFile, final int maxBackFiles) throws IOException {
        int idx = 0;
        final String fileNameExclExt = com.hivemq.util.Files.getFileNameExcludingExtension(configFile.getName());
        final String fileExtension = com.hivemq.util.Files.getFileExtension(configFile.getName());
        final String copyPath = com.hivemq.util.Files.getFilePathExcludingFile(configFile.getAbsolutePath());

        String copyFilename = null;
        File copyFile = null;
        do {
            copyFilename = String.format("%s_%d.%s", fileNameExclExt, ++idx, fileExtension);
            copyFile = new File(copyPath, copyFilename);
        } while(idx < maxBackFiles && copyFile.exists());

        if(copyFile.exists()){

            //-- use the oldest available backup index
            final File[] backupFiles = new File(copyPath).listFiles(child -> child.isFile() &&
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

    public void writeConfigToXML(final @NotNull Writer writer) {
        synchronized (lock) {
            try {
                final JAXBContext context = createContext();
                final Marshaller marshaller = context.createMarshaller();
                final Schema schema = loadSchema();
                if (schema != null) {
                    marshaller.setSchema(schema);
                    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, XSD_SCHEMA);
                }
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.marshal(configEntity, writer);
            } catch (final JAXBException | IOException | SAXException e) {
                log.error("Original error message:", e);
                throw new UnrecoverableException(false);
            }
        }
    }

    private @NotNull HiveMQConfigEntity readConfigFromXML(final @NotNull File configFile) {

        log.info("Reading configuration file {}", configFile);
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
                String configFileContent = Files.readString(configFile.toPath());
                final FileFragmentUtil.FragmentResult fragmentResult = FileFragmentUtil.replaceFragmentPlaceHolders(configFileContent);

                fragmentToModificationTime.putAll(fragmentResult.getFragmentToModificationTime());

                configFileContent = fragmentResult.getRenderResult(); //must happen before env rendering so templates can be used with envs
                configFileContent = IfUtil.replaceIfPlaceHolders(configFileContent);
                configFileContent = EnvVarUtil.replaceEnvironmentVariablePlaceholders(configFileContent);

                final ByteArrayInputStream is =
                        new ByteArrayInputStream(configFileContent.getBytes(StandardCharsets.UTF_8));
                final StreamSource streamSource = new StreamSource(is);

                unmarshaller.setEventHandler(e -> {
                    if (e.getSeverity() >= ValidationEvent.ERROR) {
                        validationErrors.add(e);
                    }
                    return true;

                });

                final JAXBElement<? extends HiveMQConfigEntity> result =
                        unmarshaller.unmarshal(streamSource, getConfigEntityClass());

                if (!validationErrors.isEmpty()) {
                    throw new JAXBException("Parsing failed");
                }

                final HiveMQConfigEntity configEntity = result.getValue();

                if (configEntity == null) {
                    throw new JAXBException("Result is null");
                }

                configEntity.getProtocolAdapterConfig().forEach(e -> e.validate(validationErrors));

                configEntity.getDataCombinerEntities().forEach(e -> e.validate(validationErrors));


                if (!validationErrors.isEmpty()) {
                    throw new JAXBException("Parsing failed");
                }
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

    boolean setConfiguration(final @NotNull HiveMQConfigEntity config) {

        final List<String> requiresRestart =
                configurators.stream()
                        .filter(c -> c.needsRestartWithConfig(config))
                        .map(c -> c.getClass().getSimpleName())
                        .collect(Collectors.toList());

        if (requiresRestart.isEmpty()) {
            log.debug("Config can be applied");
            configurators.forEach(c -> c.applyConfig(config));
            bridgeExtractor.updateConfig(config);
            protocolAdapterExtractor.updateConfig(config);
            return true;
        } else {
            log.error("Config requires restart because of: {}", requiresRestart);
            return false;
        }

    }

    public void syncConfiguration() {
        Preconditions.checkNotNull(configEntity, "Configuration must be loaded to be synchronized");
        configurators.stream()
                .filter(c -> c instanceof Syncable)
                .forEach(c -> ((Syncable)c).sync(configEntity));

        bridgeExtractor.sync(configEntity);
        protocolAdapterExtractor.sync(configEntity);
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
        log.warn("No schema loaded for validation of config xml.");
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
