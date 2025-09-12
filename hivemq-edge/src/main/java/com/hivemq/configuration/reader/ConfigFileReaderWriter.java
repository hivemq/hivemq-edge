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
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.util.ThreadFactoryUtil;
import com.hivemq.util.render.EnvVarUtil;
import com.hivemq.util.render.IfUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventLocator;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.XMLConstants;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static com.hivemq.util.Files.getFileExtension;
import static com.hivemq.util.Files.getFileNameExcludingExtension;
import static com.hivemq.util.Files.getFilePathExcludingFile;
import static com.hivemq.util.render.FileFragmentUtil.replaceFragmentPlaceHolders;
import static java.util.Objects.requireNonNullElse;

public class ConfigFileReaderWriter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConfigFileReaderWriter.class);
    private static final @NotNull String CONFIG_FRAGMENT_PATH = "/fragment/config";
    private static final @NotNull String XSD_SCHEMA = "config.xsd";
    private static final int MAX_BACK_FILES = 5;
    private static final @Nullable Schema CONFIG_XSD;
    private static final @NotNull JAXBContext CONFIG_JAXB_CONTEXT;

    static {
        // load config.xsd
        final URL resource = ConfigFileReaderWriter.class.getResource("/" + XSD_SCHEMA);
        if (resource != null) {
            try {
                final URLConnection urlConnection = resource.openConnection();
                urlConnection.setUseCaches(false);
                try (final InputStream is = urlConnection.getInputStream()) {
                    CONFIG_XSD = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                            .newSchema(new StreamSource(is));
                }
            } catch (final Throwable e) {
                log.error("Cannot load configuration schema:", e);
                throw new UnrecoverableException(false);
            }
        } else {
            log.warn("No schema loaded for validation of config xml.");
            CONFIG_XSD = null;
        }

        // create Jaxb context and marshaller
        try {
            CONFIG_JAXB_CONTEXT =
                    JAXBContext.newInstance(ImmutableList.<Class<?>>builder()
                            .add(HiveMQConfigEntity.class)
                            // inherited
                            .add(TCPListenerEntity.class)
                            .add(WebsocketListenerEntity.class)
                            .add(TlsTCPListenerEntity.class)
                            .add(TlsWebsocketListenerEntity.class)
                            .add(UDPListenerEntity.class)
                            .add(UDPBroadcastListenerEntity.class)

                            .add(FieldMappingEntity.class)
                            .build()
                            .toArray(new Class<?>[0]));
        } catch (final Throwable e) {
            log.error("Cannot create the jaxb context:", e);
            throw new UnrecoverableException(false);
        }
    }

    private final @NotNull ConfigurationFile configFile;
    private final @NotNull List<Configurator<?>> configurators;
    private final @NotNull ConcurrentMap<Path, Long> fragmentToModificationTime;
    private final @NotNull BridgeExtractor bridgeExtractor;
    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor;
    private final @NotNull DataCombiningExtractor dataCombiningExtractor;
    private final @NotNull UnsExtractor unsExtractor;
    private final @NotNull List<ReloadableExtractor<?, ?>> extractors;
    private final @NotNull SystemInformation sysInfo;
    private final @NotNull AtomicLong lastWrite;
    private final @NotNull AtomicReference<HiveMQConfigEntity> configEntity;
    private final @NotNull Lock lock;
    private final @NotNull AtomicReference<ScheduledExecutorService> executorService;
    private boolean defaultBackupConfig;

    public ConfigFileReaderWriter(
            final @NotNull SystemInformation sysInfo,
            final @NotNull ConfigurationFile configFile,
            final @NotNull List<Configurator<?>> configurators) {
        this.sysInfo = sysInfo;
        this.configFile = configFile;
        this.configurators = configurators;
        this.extractors = List.of(this.bridgeExtractor = new BridgeExtractor(this),
                this.protocolAdapterExtractor = new ProtocolAdapterExtractor(this),
                this.dataCombiningExtractor = new DataCombiningExtractor(this),
                this.unsExtractor = new UnsExtractor(this));
        this.fragmentToModificationTime = new ConcurrentHashMap<>();
        this.configEntity = new AtomicReference<>();
        this.lastWrite = new AtomicLong();
        this.lock = new ReentrantLock();
        this.executorService = new AtomicReference<>();
        this.defaultBackupConfig = true;
    }

    private static @NotNull String toValidationMessage(final @NotNull ValidationEvent event) {
        final StringBuilder sb = new StringBuilder();
        final ValidationEventLocator locator = event.getLocator();
        if (locator == null) {
            sb.append("\t- XML schema violation caused by: \"").append(event.getMessage()).append("\"");
        } else {
            sb.append("\t- XML schema violation in line '")
                    .append(locator.getLineNumber())
                    .append("' and column '")
                    .append(locator.getColumnNumber())
                    .append("' caused by: \"")
                    .append(event.getMessage())
                    .append("\"");
        }
        return sb.toString();
    }

    private static @NotNull Map<Path, Long> findFilesToWatch(final @NotNull HiveMQConfigEntity entity) {
        final Map<Path, Long> paths = new ConcurrentHashMap<>();
        entity.getBridgeConfig().forEach(cfg -> {
            final BridgeTlsEntity tls = cfg.getRemoteBroker().getTls();
            if (tls != null) {
                final KeystoreEntity keyStore = tls.getKeyStore();
                if (keyStore != null) {
                    final Path path = Paths.get(keyStore.getPath());
                    try {
                        paths.put(path, Files.getLastModifiedTime(path).toMillis());
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                final TruststoreEntity trustStore = tls.getTrustStore();
                if (trustStore != null) {
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

    private static @NotNull Marshaller createMarshaller() throws JAXBException {
        final Marshaller marshaller = CONFIG_JAXB_CONTEXT.createMarshaller();
        if (CONFIG_XSD != null) {
            marshaller.setSchema(CONFIG_XSD);
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, XSD_SCHEMA);
        }
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        return marshaller;
    }

    private static @NotNull Unmarshaller createUnmarshaller(final @Nullable List<ValidationEvent> validationErrors)
            throws JAXBException {
        final Unmarshaller unmarshaller = CONFIG_JAXB_CONTEXT.createUnmarshaller();
        if (CONFIG_XSD != null) {
            unmarshaller.setSchema(CONFIG_XSD);
        }
        if (validationErrors != null) {
            unmarshaller.setEventHandler(e -> {
                if (e.getSeverity() >= ValidationEvent.ERROR) {
                    validationErrors.add(e);
                }
                return true;
            });
        }
        return unmarshaller;
    }

    public @NotNull DataCombiningExtractor getDataCombiningExtractor() {
        return dataCombiningExtractor;
    }

    public @NotNull BridgeExtractor getBridgeExtractor() {
        return bridgeExtractor;
    }

    public @NotNull ProtocolAdapterExtractor getProtocolAdapterExtractor() {
        return protocolAdapterExtractor;
    }

    public @NotNull UnsExtractor getUnsExtractor() {
        return unsExtractor;
    }

    public void setDefaultBackupConfig(final boolean defaultBackupConfig) {
        this.defaultBackupConfig = defaultBackupConfig;
    }

    public @NotNull HiveMQConfigEntity applyConfig() {
        if (!loadConfigFromXML(getConfigFileOrFail())) {
            log.error("Unable to apply the given configuration.");
            throw new UnrecoverableException(false);
        }
        return configEntity.get();
    }

    public void applyConfigAndWatch(final long checkIntervalInMs) {
        startWatching(getConfigFileOrFail(),
                (checkIntervalInMs > 0) ? checkIntervalInMs : 1000,
                this::applyConfig,
                this::checkMonitoredFilesForChanges);
    }

    public void writeConfigWithSync() {
        if (log.isTraceEnabled()) {
            log.trace("flushing configuration changes to entity layer");
        }
        try {
            // sync config
            final HiveMQConfigEntity entity = this.configEntity.get();
            Preconditions.checkNotNull(entity, "Configuration must be loaded to be synchronized");
            configurators.stream()
                    .filter(Syncable.class::isInstance)
                    .map(Syncable.class::cast)
                    .forEach(syncable -> syncable.sync(entity));
            extractors.forEach(extractor -> extractor.sync(entity));
            if (entity.getGatewayConfig().isMutableConfigurationEnabled()) {
                writeConfigToXML();
            }
        } catch (final Exception e) {
            log.error("Configuration file sync failed: ", e);
        } finally {
            lastWrite.set(System.currentTimeMillis());
        }
    }

    public long getLastWrite() {
        return lastWrite.get();
    }

    public void writeConfigToXML(final @NotNull Writer writer) {
        lock.lock();
        try {
            createMarshaller().marshal(configEntity.get(), writer);
        } catch (final Throwable e) {
            log.error("Original error message:", e);
            throw new UnrecoverableException(false);
        } finally {
            lock.unlock();
        }
    }

    @VisibleForTesting
    void writeConfigToXML() {
        writeConfigToXML(getConfigFileOrFail(), defaultBackupConfig, true);
    }

    @VisibleForTesting
    public void writeConfigToXML(final @NotNull File file, final boolean doBackup, final boolean checkExists) {
        if (checkExists && !file.exists() && !file.canWrite()) {
            log.error("Unable to write to supplied configuration file {}", file);
            throw new UnrecoverableException(false);
        }
        if (log.isDebugEnabled()) {
            log.debug("Writing configuration file {}", file.getAbsolutePath());
        }
        lock.lock();
        try {
            final HiveMQConfigEntity entity = this.configEntity.get();
            if (entity == null) {
                log.error("Unable to write uninitialized configuration.");
                throw new UnrecoverableException(false);
            }

            backupConfig(file, doBackup); // write the backup of the file before rewriting
            try (final FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writeConfigToXML(writer);
            }
        } catch (final IOException e) {
            log.error("Error writing file:", e);
            throw new UnrecoverableException(false);
        } finally {
            lock.unlock();
        }
    }

    private @NotNull File getConfigFileOrFail() {
        return configFile.file().orElseGet(() -> {
            log.error("No configuration file present. Shutting down HiveMQ Edge.");
            throw new UnrecoverableException(false);
        });
    }

    @VisibleForTesting
    boolean loadConfigFromXML(final @NotNull File configFile) {
        log.info("Reading configuration file {}", configFile);
        final List<ValidationEvent> validationErrors = Collections.synchronizedList(new ArrayList<>());

        lock.lock();
        try {

            // replace environment variable placeholders
            String content = Files.readString(configFile.toPath());
            final var fragment = replaceFragmentPlaceHolders(content, sysInfo.isConfigFragmentBase64Zip());
            content = fragment.getRenderResult(); //must happen before env rendering so templates can be used with envs
            content = IfUtil.replaceIfPlaceHolders(content);
            content = EnvVarUtil.replaceEnvironmentVariablePlaceholders(content);

            fragmentToModificationTime.putAll(fragment.getFragmentToModificationTime());

            try (final ByteArrayInputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))) {
                final JAXBElement<? extends HiveMQConfigEntity> unmarshalled =
                        createUnmarshaller(validationErrors).unmarshal(new StreamSource(is), HiveMQConfigEntity.class);
                if (!validationErrors.isEmpty()) {
                    throw new JAXBException("Parsing failed");
                }
                final HiveMQConfigEntity entity = unmarshalled.getValue();
                if (entity == null) {
                    throw new JAXBException("Result is null");
                }
                entity.getProtocolAdapterConfig().forEach(e -> e.validate(validationErrors));
                entity.getDataCombinerEntities().forEach(e -> e.validate(validationErrors));
                if (!validationErrors.isEmpty()) {
                    throw new JAXBException("Parsing failed");
                }

                configEntity.set(entity);
                return internalApplyConfig(entity);
            }
        } catch (final JAXBException | IOException e) {
            final StringBuilder sb = new StringBuilder();
            if (validationErrors.isEmpty()) {
                sb.append("of the following error: ");
                sb.append(requireNonNullElse(e.getCause(), e));
            } else {
                sb.append("of the following errors:");
                for (final ValidationEvent validationError : validationErrors) {
                    sb.append(System.lineSeparator()).append(toValidationMessage(validationError));
                }
            }
            log.error("Not able to parse configuration file because {}", sb);
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
            if (log.isDebugEnabled()) {
                log.debug("Original error message:", e);
            }
            throw new UnrecoverableException(false);
        } finally {
            lock.unlock();
        }
    }

    @VisibleForTesting
    boolean internalApplyConfig(final @NotNull HiveMQConfigEntity entity) {
        final List<String> requiresRestart = configurators.stream()
                .filter(c -> c.needsRestartWithConfig(entity))
                .map(c -> c.getClass().getSimpleName())
                .toList();
        if (!requiresRestart.isEmpty()) {
            log.error("Config requires restart because of: {}", requiresRestart);
            return false;
        }
        if (log.isDebugEnabled()) {
            log.debug("Config can be applied");
        }

        try {
            for (final Configurator<?> configurator : configurators) {
                final Configurator.ConfigResult result = configurator.applyConfig(entity);
                if (result == null) {
                    log.error("Config {} can not be applied because the result is not found.",
                            configurator.getClass().getSimpleName());
                    return false;
                }
                switch (result) {
                    case ERROR -> {
                        log.error("Config {} can not be applied because an unrecoverable error is found.",
                                configurator.getClass().getSimpleName());
                        return false;
                    }
                    case NEEDS_RESTART -> {
                        log.error("Config {} can not be applied because it requires restart.",
                                configurator.getClass().getSimpleName());
                        return false;
                    }
                }
            }

            for (final ReloadableExtractor<?, ?> extractor : extractors) {
                final Configurator.ConfigResult result = extractor.updateConfig(entity);
                if (result == null) {
                    log.error("Reloadable config {} can not be applied because the result is not found.",
                            extractor.getClass().getSimpleName());
                    return false;
                }
                switch (result) {
                    case ERROR -> {
                        log.error("Reloadable config {} can not be applied because an unrecoverable error is found.",
                                extractor.getClass().getSimpleName());
                        return false;
                    }
                    case NEEDS_RESTART -> {
                        log.error("Reloadable config {} can not be applied because it requires restart.",
                                extractor.getClass().getSimpleName());
                        return false;
                    }
                }
            }
            return true;
        } catch (final Throwable t) {
            log.error("An error occurred while applying the configuration.", t);
            return false;
        }
    }

    private void backupConfig(final @NotNull File configFile, final boolean enabled) throws IOException {
        if (!enabled) {
            return;
        }
        final String fileNameNoExt = getFileNameExcludingExtension(configFile.getName());
        final String fileExt = getFileExtension(configFile.getName());
        final File copyPath = new File(getFilePathExcludingFile(configFile.getAbsolutePath()));
        if (copyPath.exists() && copyPath.isDirectory()) {
            int idx = 1;
            File copyFile;
            do {
                final String copyFilename = fileNameNoExt + '_' + (idx++) + (fileExt != null ? "." + fileExt : "");
                copyFile = new File(copyPath, copyFilename);
            } while (idx < MAX_BACK_FILES && copyFile.exists());

            if (copyFile.exists()) {
                //-- use the oldest available backup index
                final File[] backupFiles = copyPath.listFiles(child -> child.isFile() &&
                        child.getName().startsWith(fileNameNoExt) &&
                        (fileExt == null || child.getName().endsWith(fileExt)));
                assert backupFiles != null;
                Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
                copyFile = backupFiles[0];
            }
            if (log.isDebugEnabled()) {
                log.debug("Rolling backup of configuration file to {}", copyFile.getName());
            }
            FileUtils.copyFile(configFile, copyFile);
        } else {
            log.error("Configuration folder {} does not exist or is not a directory", copyPath.getAbsolutePath());
            throw new UnrecoverableException(false);
        }
    }

    private void startWatching(
            final @NotNull File configFile,
            final long interval,
            final @NotNull Supplier<HiveMQConfigEntity> entitySupplier,
            final @NotNull ScheduledTask scheduledTask) {
        if (executorService.compareAndSet(null,
                Executors.newSingleThreadScheduledExecutor(ThreadFactoryUtil.create("hivemq-edge-config-watch-%d")))) {

            final HiveMQConfigEntity entity = entitySupplier.get();
            final Map<Path, Long> fileModificationTimestamps = findFilesToWatch(entity);
            final AtomicLong fileModified = new AtomicLong();
            try {
                fileModified.set(Files.getLastModifiedTime(configFile.toPath()).toMillis());
            } catch (final IOException e) {
                throw new RuntimeException("Unable to read last modified time from " + configFile.getAbsolutePath(), e);
            }

            log.info("Rereading config file every {} ms", interval);
            executorService.get()
                    .scheduleAtFixedRate(() -> scheduledTask.executePeriodicTask(configFile,
                            fileModified,
                            fileModificationTimestamps), 0, interval, TimeUnit.MILLISECONDS);
            Runtime.getRuntime().addShutdownHook(new Thread(this::stopWatching));
        } else {
            throw new IllegalStateException("Config watch was already started");
        }
    }

    private void stopWatching() {
        final ScheduledExecutorService es = executorService.getAndSet(null);
        if (es != null) {
            es.shutdownNow();
        }
    }

    private void checkMonitoredFilesForChanges(
            final @NotNull File configFile,
            final @NotNull AtomicLong fileModified,
            final @NotNull Map<Path, Long> fileModificationTimestamps) {
        try {
            final boolean isDevMode = "true".equals(System.getProperty(HiveMQEdgeConstants.DEVELOPMENT_MODE));
            if (!isDevMode) {
                final Map<Path, Long> pathsToCheck = new HashMap<>(fragmentToModificationTime);
                pathsToCheck.putAll(fileModificationTimestamps);
                pathsToCheck.forEach((key, value) -> {
                    try {
                        if (!key.toString().equals(CONFIG_FRAGMENT_PATH) &&
                                Files.getFileAttributeView(key.toRealPath(LinkOption.NOFOLLOW_LINKS),
                                        BasicFileAttributeView.class).readAttributes().lastModifiedTime().toMillis() >
                                        value) {
                            log.error("Restarting because a required file was updated: {}", key);
                            System.exit(0);
                        }
                    } catch (final IOException e) {
                        throw new RuntimeException("Unable to read last modified time for " + key, e);
                    }
                });
            }

            final long modified;
            if (new File(CONFIG_FRAGMENT_PATH).exists()) {
                modified = Files.getLastModifiedTime(new File(CONFIG_FRAGMENT_PATH).toPath()).toMillis();
            } else {
                log.warn("No fragment found, checking the full config, only used for testing");
                modified = Files.getLastModifiedTime(configFile.toPath()).toMillis();
            }
            if (modified > fileModified.get()) {
                fileModified.set(modified);
                if (!loadConfigFromXML(configFile)) {
                    if (!isDevMode) {
                        log.error("Restarting because new config can't be hot-reloaded");
                        System.exit(0);
                    } else {
                        log.error("TEST MODE, NOT RESTARTING");
                    }
                }
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    private interface ScheduledTask {
        void executePeriodicTask(
                final @NotNull File configFile,
                final @NotNull AtomicLong fileModified,
                final @NotNull Map<Path, Long> fileModificationTimestamps);
    }
}
