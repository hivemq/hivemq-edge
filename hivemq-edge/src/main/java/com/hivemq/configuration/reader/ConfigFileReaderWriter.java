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

import static com.hivemq.util.render.FileFragmentUtil.replaceFragmentPlaceHolders;
import static java.util.Objects.requireNonNullElse;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.fieldmapping.FieldMappingEntity;
import com.hivemq.configuration.entity.listener.TCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsTCPListenerEntity;
import com.hivemq.configuration.entity.listener.TlsWebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.UDPBroadcastListenerEntity;
import com.hivemq.configuration.entity.listener.UDPListenerEntity;
import com.hivemq.configuration.entity.listener.WebsocketListenerEntity;
import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.util.render.EnvVarUtil;
import com.hivemq.util.render.IfUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventLocator;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.glassfish.jaxb.runtime.v2.runtime.IllegalAnnotationsException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("FutureReturnValueIgnored")
public class ConfigFileReaderWriter {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ConfigFileReaderWriter.class);
    private static final @NotNull String XSD_SCHEMA = "config.xsd";
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
            CONFIG_JAXB_CONTEXT = JAXBContext.newInstance(ImmutableList.<Class<?>>builder()
                    .add(HiveMQConfigEntity.class)
                    // inherited
                    .add(TCPListenerEntity.class)
                    .add(WebsocketListenerEntity.class)
                    .add(TlsTCPListenerEntity.class)
                    .add(TlsWebsocketListenerEntity.class)
                    .add(UDPListenerEntity.class)
                    .add(UDPBroadcastListenerEntity.class)
                    .add(FieldMappingEntity.class)
                    .add(TruststoreEntity.class)
                    .add(KeystoreEntity.class)
                    .build()
                    .toArray(new Class<?>[0]));
        } catch (final Throwable e) {
            if (e instanceof final IllegalAnnotationsException iae) {
                log.error("Cannot create the jaxb context: {}", iae.getErrors(), e);
            } else {
                log.error("Cannot create the jaxb context", e);
            }
            throw new UnrecoverableException(false);
        }
    }

    private final @NotNull ConfigurationFile configFile;
    private final @NotNull List<Configurator<?>> configurators;
    private final @NotNull ConfigFileWatcher watcher;

    private final @NotNull BridgeExtractor bridgeExtractor;
    private final @NotNull ProtocolAdapterExtractor protocolAdapterExtractor;
    private final @NotNull DataCombiningExtractor dataCombiningExtractor;
    private final @NotNull AssetMappingExtractor assetMappingExtractor;
    private final @NotNull PulseExtractor pulseExtractor;
    private final @NotNull UnsExtractor unsExtractor;
    private final @NotNull List<ReloadableExtractor<?, ?>> extractors;
    private final @NotNull List<Consumer<ConfigFileReaderWriter>> postApplyCallbacks;
    private final @NotNull SystemInformation sysInfo;
    private final @NotNull AtomicLong lastWrite;
    private final @NotNull AtomicReference<HiveMQConfigEntity> configEntity;

    /**
     * The {@code ${ENV:...}} placeholders of the configuration file as it was written, so that writing
     * the configuration back out restores them instead of the values they stand for; see
     * {@link EnvVarUtil#restorePlaceholders}. Replaced only when a configuration is actually accepted.
     */
    private final @NotNull AtomicReference<EnvVarUtil.CollectedPlaceholders> envPlaceholders;

    private final @NotNull Lock lock;

    private boolean defaultBackupConfig;

    public ConfigFileReaderWriter(
            final @NotNull SystemInformation sysInfo,
            final @NotNull ConfigurationFile configFile,
            final @NotNull List<Configurator<?>> configurators) {
        this.sysInfo = sysInfo;
        this.configFile = configFile;
        this.configurators = configurators;
        this.bridgeExtractor = new BridgeExtractor(this);
        this.protocolAdapterExtractor = new ProtocolAdapterExtractor(this);
        this.dataCombiningExtractor = new DataCombiningExtractor(this);
        this.assetMappingExtractor = new AssetMappingExtractor(this);
        this.pulseExtractor = new PulseExtractor(this);
        this.unsExtractor = new UnsExtractor(this);
        this.extractors = List.of(
                this.bridgeExtractor,
                this.protocolAdapterExtractor,
                this.dataCombiningExtractor,
                this.assetMappingExtractor,
                this.pulseExtractor,
                this.unsExtractor);
        this.postApplyCallbacks = new CopyOnWriteArrayList<>();
        this.configEntity = new AtomicReference<>();
        this.envPlaceholders = new AtomicReference<>(EnvVarUtil.CollectedPlaceholders.NONE);
        this.lastWrite = new AtomicLong();
        this.lock = new ReentrantLock();
        this.watcher = new ConfigFileWatcher(lock, configFile, this::loadConfigFromXML);
        this.defaultBackupConfig = true;
    }

    private static @NotNull String toValidationMessage(final @NotNull ValidationEvent event) {
        final StringBuilder sb = new StringBuilder();
        final ValidationEventLocator locator = event.getLocator();
        if (locator == null) {
            sb.append("\t- XML schema violation caused by: \"")
                    .append(event.getMessage())
                    .append("\"");
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

    private static @NotNull Marshaller createMarshaller() throws JAXBException {
        final Marshaller marshaller = CONFIG_JAXB_CONTEXT.createMarshaller();
        if (CONFIG_XSD != null) {
            marshaller.setSchema(CONFIG_XSD);
            marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, XSD_SCHEMA);
        }
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
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

    public @NotNull AssetMappingExtractor getAssetMappingExtractor() {
        return assetMappingExtractor;
    }

    public @NotNull BridgeExtractor getBridgeExtractor() {
        return bridgeExtractor;
    }

    public @NotNull ProtocolAdapterExtractor getProtocolAdapterExtractor() {
        return protocolAdapterExtractor;
    }

    public @NotNull PulseExtractor getPulseExtractor() {
        return pulseExtractor;
    }

    public @NotNull UnsExtractor getUnsExtractor() {
        return unsExtractor;
    }

    public void registerPostApplyCallback(final @NotNull Consumer<ConfigFileReaderWriter> callback) {
        postApplyCallbacks.add(callback);
    }

    public void setDefaultBackupConfig(final boolean defaultBackupConfig) {
        this.defaultBackupConfig = defaultBackupConfig;
    }

    /** The configuration this node currently runs on; null before the first successful load. */
    @VisibleForTesting
    @Nullable
    HiveMQConfigEntity getConfigEntity() {
        return configEntity.get();
    }

    public @NotNull HiveMQConfigEntity applyConfig() {
        final ReloadOutcome outcome = loadConfigFromXML(getConfigFileOrFail());
        if (outcome != ReloadOutcome.APPLIED) {
            // A REJECTED_INVALID load has already logged the validation errors inside loadConfigFromXML;
            // a second, generic line here would bury them. Only NEEDS_RESTART has nothing more specific.
            if (outcome == ReloadOutcome.NEEDS_RESTART) {
                log.error("Unable to apply the given configuration.");
            }
            throw new UnrecoverableException(false);
        }
        final HiveMQConfigEntity entity = configEntity.get();
        if (entity == null) {
            throw new UnrecoverableException(false);
        }
        return entity;
    }

    public void applyConfigAndWatch(final long checkIntervalInMs) {
        watcher.start(getConfigFileOrFail(), (checkIntervalInMs > 0) ? checkIntervalInMs : 1000, this::applyConfig);
    }

    @VisibleForTesting
    void setRestartHook(final @NotNull Runnable restartHook) {
        watcher.setRestartHook(restartHook);
    }

    @VisibleForTesting
    void stopWatching() {
        watcher.stop();
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
            // Stamped only once everything above has succeeded. It is what the configuration service
            // reports as the last update time, and a refused write used to move it as well -- so the node
            // reported the configuration as freshly written at the moment its own log said config.xml no
            // longer matched it (EDG-949).
            lastWrite.set(System.currentTimeMillis());
        } catch (final UnrecoverableException refused) {
            // A deliberate refusal: something on the write path would have had to put a credential on disk,
            // or replace a good configuration file with one whose protections it could not reproduce, and
            // stopped instead. The reason is in the error logged immediately above this one by whichever
            // check made the decision; what is added here is the consequence, because nothing else says it.
            //
            // The caller is not told. A REST change that cannot be persisted still answers success, and
            // this log line is the only place an operator can learn that config.xml no longer matches the
            // node. Telling the caller instead means deciding what every configuration endpoint should
            // return when the file cannot be written, which is a larger change than this one (EDG-882
            // review v04).
            log.error("The configuration was not written to config.xml -- the reason is the error logged just"
                    + " above. This node keeps running on the configuration it already holds and is correct,"
                    + " but config.xml no longer matches it: a restart would come up on the older"
                    + " configuration, and the change that triggered this write would be lost.");
        } catch (final Exception e) {
            log.error("Configuration file sync failed: ", e);
        }
    }

    public long getLastWrite() {
        return lastWrite.get();
    }

    public void writeConfigToXML(final @NotNull Writer writer) {
        lock.lock();
        try {
            // Marshalled to a string first so that the environment-variable placeholders can be put back
            // before anything reaches the file. Rendering happens once, on the whole file, before it is
            // parsed, so what is held in memory -- and what a marshaller would write -- is the resolved
            // value: writing it out put a bridge password supplied through ${ENV:...} into config.xml in
            // plain text, on any REST change to any subsystem (EDG-882 QA round 2).
            final StringWriter marshalled = new StringWriter();
            createMarshaller().marshal(configEntity.get(), marshalled);
            writer.write(EnvVarUtil.restorePlaceholders(
                    marshalled.toString(),
                    Objects.requireNonNullElse(envPlaceholders.get(), EnvVarUtil.CollectedPlaceholders.NONE)));
            writer.flush();
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

            // Rendered in full before the file is opened, because opening it truncates it and the
            // marshaller validates against the schema as it writes. Anything it rejects -- a value that
            // does not fit its element, a constraint a REST call did not enforce -- used to leave the
            // operator with a config.xml emptied or cut off mid-element, while the REST call that
            // triggered the write answered 200 and the node kept running on the configuration it still
            // had in memory. The next restart is where they would find out (EDG-882 QA round 3).
            final StringWriter rendered = new StringWriter();
            writeConfigToXML(rendered);

            // The real path, not the configured one: replacing a path is replacing whatever the last
            // component *is*, so when config.xml is a symbolic link -- a mounted configuration
            // directory, an operator's link into a versioned tree -- the move would delete the link and
            // leave a regular file in its place. Writing in place followed it, which is the behaviour
            // being preserved here; resolving it also keeps the partial file in the same real directory,
            // which is what ATOMIC_MOVE needs.
            final Path configured = file.toPath();
            final Path target = Files.exists(configured) ? configured.toRealPath() : configured;
            // Only a file this process could have written in place. Replacing by move needs permission on
            // the directory, not on the file, so without this a configuration an operator write-protected
            // -- root-owned, or read-only on purpose -- would be replaced anyway, and quietly change owner
            // in the process. Writing in place refused it, and so does this (EDG-882 review v04).
            if (Files.exists(target) && !Files.isWritable(target)) {
                log.error(
                        "The configuration file {} is not writable by this node, so the configuration has not"
                                + " been persisted. The replacement is written beside it and moved onto it, which"
                                + " needs no permission on the file itself -- overwriting one this node may not"
                                + " change is not something to do quietly. This node keeps running on the"
                                + " configuration it already holds.",
                        target);
                throw new UnrecoverableException(false);
            }
            // Read before anything is created or rotated: it is a pure read, and it is the most likely of
            // the refusals below. The backup itself is taken inside the replace, once the replacement is
            // complete on disk and carries its protections, and just before the move -- so a write that is
            // refused at any point consumes no backup slot. Rotating first meant that on a node where every
            // write is refused, four refusals turned every backup into a copy of the same unchanged file
            // and the earlier history was gone, on exactly the node where an operator wants it (EDG-949).
            final ProtectedFileReplacer.PreservedAttributes preserved =
                    ProtectedFileReplacer.preservedAttributesOf(target);
            // The time the replacement carries onto the target -- a rename keeps it -- read while it is
            // still the working file. Read from the target after the move it could be an operator's
            // save that landed in between, and stamping that would hide their edit from the watcher.
            final AtomicLong staged = new AtomicLong(-1);
            ProtectedFileReplacer.replaceCarryingProtections(
                    target,
                    preserved,
                    partial -> {
                        try (final FileWriter writer = new FileWriter(partial.toFile(), StandardCharsets.UTF_8)) {
                            writer.write(rendered.toString());
                            writer.flush();
                        }
                        staged.set(Files.getLastModifiedTime(partial).toMillis());
                    },
                    () -> ConfigBackupRotation.backup(file, doBackup));
            // What the watcher would otherwise see as a change and reload: this node's own write.
            watcher.writtenByThisNode(target, staged.get());
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

    /**
     * Outcome of a configuration (re)load. {@link #REJECTED_INVALID} is a file that could not be read,
     * parsed or validated -- recoverable on a reload, by keeping the configuration already applied --
     * as opposed to {@link #NEEDS_RESTART}, a file that parsed and applied but cannot take effect
     * without a restart.
     */
    public enum ReloadOutcome {
        APPLIED,
        NEEDS_RESTART,
        REJECTED_INVALID
    }

    @VisibleForTesting
    ReloadOutcome loadConfigFromXML(final @NotNull File configFile) {
        log.info("Reading configuration file {}", configFile);
        final List<ValidationEvent> validationErrors = Collections.synchronizedList(new ArrayList<>());

        lock.lock();
        // Kept for the diagnostic in the catch below, which needs the placeholders as the operator wrote
        // them rather than what they resolved to.
        String beforeRendering = null;
        try {

            // replace environment variable placeholders
            String content = Files.readString(configFile.toPath());
            final var fragment = replaceFragmentPlaceHolders(content, sysInfo.isConfigFragmentBase64Zip());
            content = fragment.getRenderResult(); // must happen before env rendering so templates can be used with envs
            content = IfUtil.replaceIfPlaceHolders(content);
            // Collected before rendering, so that writing the configuration back out restores the
            // operator's placeholders instead of the secrets they stand for -- see
            // EnvVarUtil.restorePlaceholders. Held locally until the configuration has actually been
            // accepted: a file that fails to parse must not replace the placeholders of the
            // configuration still running, or the next write would marshal that older configuration --
            // with its resolved secrets -- against a map that no longer describes it.
            final EnvVarUtil.CollectedPlaceholders placeholders = EnvVarUtil.collectPlaceholders(content);
            beforeRendering = content;
            content = EnvVarUtil.replaceEnvironmentVariablePlaceholders(content);

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
                // Only now: this configuration is the one that will be written back out, so these are
                // the placeholders that describe it.
                envPlaceholders.set(placeholders);
                // And only now the fragments it pulls in are the ones to watch. Replaced, not added to:
                // the set used to grow on every reload and never shrink, so a fragment taken out of the
                // configuration and deleted from disk was still watched, and the watcher died trying to
                // read it (EDG-949).
                watcher.watchFragments(fragment.getFragmentToModificationTime());
                return internalApplyConfig(entity) ? ReloadOutcome.APPLIED : ReloadOutcome.NEEDS_RESTART;
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
            reportValuesThatAreNotText(beforeRendering);
            return ReloadOutcome.REJECTED_INVALID;
        } catch (final UnrecoverableException notRenderable) {
            // An environment variable that is not set, or a fragment that cannot be loaded. The renderer
            // has already logged which; what is added here is that the file is rejected rather than the
            // node exited -- on a reload the node keeps its configuration, at startup applyConfig still
            // refuses to start (EDG-949 review).
            log.error(
                    "The configuration file {} could not be rendered, see the error above: a placeholder or"
                            + " fragment in it cannot be resolved",
                    configFile.getAbsolutePath());
            return ReloadOutcome.REJECTED_INVALID;
        } catch (final Exception e) {
            if (e.getCause() instanceof final UnrecoverableException unrecoverableException) {
                if (unrecoverableException.isShowException()) {
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

    /**
     * Names the environment variables that made the configuration unparseable, when that is what happened.
     * <p>
     * A value is put into the file as raw text before it is parsed, so one containing {@code <} or
     * {@code &} makes the document malformed -- and the parser then reports a line the operator never
     * wrote, about content they cannot see, with no hint that a variable is responsible. A credential
     * containing an ampersand is the ordinary way to arrive here (EDG-882 review v04).
     * <p>
     * Only when the parse actually failed, and only the variable names: a configuration that deliberately
     * brings in markup through a variable keeps working and says nothing, and no value reaches the log.
     */
    private static void reportValuesThatAreNotText(final @Nullable String beforeRendering) {
        if (beforeRendering == null) {
            return;
        }
        final List<String> names = EnvVarUtil.variablesWhoseValueIsNotText(beforeRendering);
        if (names.isEmpty()) {
            return;
        }
        log.error(
                "These environment variables resolve to a value containing '<' or '&': {}. Their values are put"
                        + " into the configuration as raw XML before it is parsed, so a value containing either"
                        + " only works when the value itself is well-formed XML -- a credential containing an"
                        + " ampersand cannot be supplied this way. That is the likely cause of the parse error"
                        + " above.",
                names);
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
                    log.error(
                            "Config {} can not be applied because the result is not found.",
                            configurator.getClass().getSimpleName());
                    return false;
                }
                switch (result) {
                    case ERROR -> {
                        log.error(
                                "Config {} can not be applied because an unrecoverable error is found.",
                                configurator.getClass().getSimpleName());
                        return false;
                    }
                    case NEEDS_RESTART -> {
                        log.error(
                                "Config {} can not be applied because it requires restart.",
                                configurator.getClass().getSimpleName());
                        return false;
                    }
                    default -> {}
                }
            }

            for (final ReloadableExtractor<?, ?> extractor : extractors) {
                final Configurator.ConfigResult result = extractor.updateConfig(entity);
                if (result == null) {
                    log.error(
                            "Reloadable config {} can not be applied because the result is not found.",
                            extractor.getClass().getSimpleName());
                    return false;
                }
                switch (result) {
                    case ERROR -> {
                        log.error(
                                "Reloadable config {} can not be applied because an unrecoverable error is found.",
                                extractor.getClass().getSimpleName());
                        return false;
                    }
                    case NEEDS_RESTART -> {
                        log.error(
                                "Reloadable config {} can not be applied because it requires restart.",
                                extractor.getClass().getSimpleName());
                        return false;
                    }
                    default -> {}
                }
            }
            postApplyCallbacks.forEach(callback -> callback.accept(this));
            return true;
        } catch (final Throwable t) {
            log.error("An error occurred while applying the configuration.", t);
            return false;
        }
    }
}
