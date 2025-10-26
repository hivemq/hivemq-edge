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
package com.hivemq.protocols;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.persistence.mappings.SouthboundMapping;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ADAPTER_FAILED_TO_START;
import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ADAPTER_MISSING;
import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.failedFuture;

@SuppressWarnings("unchecked")
@Singleton
public class ProtocolAdapterManager {
    private static final @NotNull Logger log = LoggerFactory.getLogger(ProtocolAdapterManager.class);

    // ThreadLocal flag to prevent refresh() from restarting adapters during hot-reload config updates
    // AtomicBoolean to skip next refresh() call during hot-reload config updates
    // Must be atomic (not ThreadLocal) because refresh() runs in a different thread (refreshExecutor)
    private static final @NotNull AtomicBoolean skipRefreshForAdapter = new AtomicBoolean(false);

    private final @NotNull Map<String, ProtocolAdapterWrapper> protocolAdapters;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ModuleServicesImpl moduleServices;
    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterConfigConverter configConverter;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull ProtocolAdapterPollingService pollingService;
    private final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics;
    private final @NotNull InternalProtocolAdapterWritingService writingService;
    private final @NotNull ProtocolAdapterFactoryManager factoryManager;
    private final @NotNull NorthboundConsumerFactory northboundConsumerFactory;
    private final @NotNull TagManager tagManager;
    private final @NotNull ProtocolAdapterExtractor config;
    private final @NotNull ExecutorService refreshExecutor;
    private final @NotNull ExecutorService sharedAdapterExecutor;
    private final @NotNull AtomicBoolean shutdownInitiated;

    @Inject
    public ProtocolAdapterManager(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ModuleServicesImpl moduleServices,
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterConfigConverter configConverter,
            final @NotNull VersionProvider versionProvider,
            final @NotNull ProtocolAdapterPollingService pollingService,
            final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics,
            final @NotNull InternalProtocolAdapterWritingService writingService,
            final @NotNull ProtocolAdapterFactoryManager factoryManager,
            final @NotNull NorthboundConsumerFactory northboundConsumerFactory,
            final @NotNull TagManager tagManager,
            final @NotNull ProtocolAdapterExtractor config,
            final @NotNull ExecutorService sharedAdapterExecutor,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.metricRegistry = metricRegistry;
        this.moduleServices = moduleServices;
        this.remoteService = remoteService;
        this.eventService = eventService;
        this.configConverter = configConverter;
        this.versionProvider = versionProvider;
        this.pollingService = pollingService;
        this.protocolAdapterMetrics = protocolAdapterMetrics;
        this.writingService = writingService;
        this.factoryManager = factoryManager;
        this.northboundConsumerFactory = northboundConsumerFactory;
        this.tagManager = tagManager;
        this.config = config;
        this.sharedAdapterExecutor = sharedAdapterExecutor;
        this.protocolAdapters = new ConcurrentHashMap<>();
        this.refreshExecutor = Executors.newSingleThreadExecutor();
        this.shutdownInitiated = new AtomicBoolean(false);
        shutdownHooks.add(new HiveMQShutdownHook() {
            @Override
            public @NotNull String name() {
                return "protocol-adapter-manager-shutdown";
            }

            @Override
            public void run() {
                shutdown();
            }
        });
        this.writingService.addWritingChangedCallback(() -> factoryManager.writingEnabledChanged(writingService.writingEnabled()));
    }

    public static <T> @NotNull T runWithContextLoader(
            final @NotNull ClassLoader ctxLoader,
            final @NotNull Supplier<T> snippet) {
        final Thread t = Thread.currentThread();
        final ClassLoader currentCtx = t.getContextClassLoader();
        try {
            t.setContextClassLoader(ctxLoader);
            return snippet.get();
        } finally {
            t.setContextClassLoader(currentCtx);
        }
    }

    private static boolean updateMappingsHotReload(
            final @NotNull ProtocolAdapterWrapper wrapper,
            final @NotNull String mappingType,
            final @NotNull Runnable updateOperation) {
        try {
            log.debug("Updating {} mappings for adapter '{}' via hot-reload", mappingType, wrapper.getId());
            updateOperation.run();
            log.info("Successfully updated {} mappings for adapter '{}' via hot-reload", mappingType, wrapper.getId());
            return true;
        } catch (final IllegalStateException e) {
            log.error("Cannot hot-reload {} mappings, adapter not in correct state: {}", mappingType, e.getMessage());
            return false;
        } catch (final Throwable e) {
            log.error("Exception happened while updating {} mappings via hot-reload: ", mappingType, e);
            return false;
        }
    }

    /**
     * Enables skipping the next refresh operation for hot-reload config updates.
     * This prevents the refresh() callback from restarting adapters when the config
     * change originates from a hot-reload operation.
     */
    public static void enableSkipNextRefresh() {
        skipRefreshForAdapter.set(true);
    }

    public static void disableSkipNextRefresh() {
        skipRefreshForAdapter.set(false);
    }

    public void start() {
        if (log.isDebugEnabled()) {
            log.debug("Starting adapters");
        }
        config.registerConsumer(this::refresh);
    }

    public void refresh(final @NotNull List<ProtocolAdapterEntity> configs) {
        refreshExecutor.submit(() -> {
            // Atomically check and clear skip flag (hot-reload in progress)
            if (skipRefreshForAdapter.getAndSet(false)) {
                log.debug("Skipping refresh because hot-reload config update is in progress");
                return;
            }

            log.info("Refreshing adapters");

            final Map<String, ProtocolAdapterConfig> protocolAdapterConfigs = configs.stream()
                    .map(configConverter::fromEntity)
                    .collect(Collectors.toMap(ProtocolAdapterConfig::getAdapterId, Function.identity()));

            final List<String> loadListOfAdapterNames = new ArrayList<>(protocolAdapterConfigs.keySet());

            final List<String> adaptersToBeDeleted = new ArrayList<>(protocolAdapters.keySet());
            adaptersToBeDeleted.removeAll(loadListOfAdapterNames);
            final List<String> adaptersToBeCreated = new ArrayList<>(loadListOfAdapterNames);
            adaptersToBeCreated.removeAll(protocolAdapters.keySet());
            final List<String> adaptersToBeUpdated = new ArrayList<>(protocolAdapters.keySet());
            adaptersToBeUpdated.removeAll(adaptersToBeCreated);
            adaptersToBeUpdated.removeAll(adaptersToBeDeleted);

            final List<String> failedAdapters = new ArrayList<>();
            adaptersToBeDeleted.forEach(name -> {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Deleting adapter '{}'", name);
                    }
                    stopAsync(name).handle((result, throwable) -> {
                        deleteAdapterInternal(name);
                        return null;
                    }).get();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedAdapters.add(name);
                    log.error("Interrupted while deleting adapter {}", name, e);
                } catch (final Throwable e) {
                    failedAdapters.add(name);
                    log.error("Failed deleting adapter {}", name, e);
                }
            });
            adaptersToBeCreated.forEach(name -> {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating adapter '{}'", name);
                    }
                    startAsync(createAdapterInternal(protocolAdapterConfigs.get(name),
                            versionProvider.getVersion())).get();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedAdapters.add(name);
                    log.error("Interrupted while adding adapter {}", name, e);
                } catch (final Throwable e) {
                    failedAdapters.add(name);
                    log.error("Failed adding adapter {}", name, e);
                }
            });
            adaptersToBeUpdated.forEach(name -> {
                try {
                    final var wrapper = protocolAdapters.get(name);
                    if (wrapper == null) {
                        log.error(
                                "Existing adapters were modified while a refresh was ongoing, adapter with name '{}' was deleted and could not be updated",
                                name);
                    }
                    if (wrapper != null && !protocolAdapterConfigs.get(name).equals(wrapper.getConfig())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Updating adapter '{}'", name);
                        }
                        stopAsync(name).thenApply(v -> {
                                    deleteAdapterInternal(name);
                                    return null;
                                })
                                .thenCompose(ignored -> startAsync(createAdapterInternal(protocolAdapterConfigs.get(name),
                                        versionProvider.getVersion())))
                                .get();
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Not-updating adapter '{}' since the config is unchanged", name);
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedAdapters.add(name);
                    log.error("Interrupted while updating adapter {}", name, e);
                } catch (final Throwable e) {
                    failedAdapters.add(name);
                    log.error("Failed updating adapter {}", name, e);
                }
            });

            if (failedAdapters.isEmpty()) {
                eventService.configurationEvent()
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage("Configuration has been successfully updated")
                        .fire();
            } else {
                eventService.configurationEvent()
                        .withSeverity(Event.SEVERITY.CRITICAL)
                        .withMessage("Reloading of configuration failed")
                        .fire();
            }
        });
    }

    public boolean protocolAdapterFactoryExists(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return factoryManager.get(protocolAdapterType).isPresent();
    }

    public @NotNull CompletableFuture<Void> startAsync(final @NotNull String adapterId) {
        Preconditions.checkNotNull(adapterId);
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(this::startAsync)
                .orElseGet(() -> failedFuture(new ProtocolAdapterException("Adapter '" + adapterId + "'not found.")));
    }

    public @NotNull CompletableFuture<Void> stopAsync(final @NotNull String adapterId) {
        Preconditions.checkNotNull(adapterId);
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(this::stopAsync)
                .orElseGet(() -> failedFuture(new ProtocolAdapterException("Adapter '" + adapterId + "'not found.")));
    }

    public @NotNull Optional<ProtocolAdapterWrapper> getProtocolAdapterWrapperByAdapterId(final @NotNull String adapterId) {
        Preconditions.checkNotNull(adapterId);
        return Optional.ofNullable(protocolAdapters.get(adapterId));
    }

    public @NotNull Optional<ProtocolAdapterInformation> getAdapterTypeById(final @NotNull String typeId) {
        Preconditions.checkNotNull(typeId);
        return Optional.ofNullable(getAllAvailableAdapterTypes().get(typeId));
    }

    public @NotNull Map<String, ProtocolAdapterInformation> getAllAvailableAdapterTypes() {
        return factoryManager.getAllAvailableAdapterTypes();
    }

    public @NotNull Map<String, ProtocolAdapterWrapper> getProtocolAdapters() {
        return Map.copyOf(protocolAdapters);
    }

    public boolean isWritingEnabled() {
        return writingService.writingEnabled();
    }

    public @NotNull DomainTagAddResult addDomainTag(
            final @NotNull String adapterId,
            final @NotNull DomainTag domainTag) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(wrapper -> {
            final var tags = new ArrayList<>(wrapper.getTags());
            final boolean alreadyExists = tags.stream().anyMatch(t -> t.getName().equals(domainTag.getTagName()));
            if (alreadyExists) {
                return DomainTagAddResult.failed(ALREADY_EXISTS, adapterId);
            }

            try {
                final var convertedTag =
                        configConverter.domainTagToTag(wrapper.getProtocolAdapterInformation().getProtocolId(),
                                domainTag);

                // Use hot-reload to add tag without restarting the adapter
                log.debug("Adding tag '{}' to adapter '{}' via hot-reload", domainTag.getTagName(), adapterId);
                wrapper.addTagHotReload(convertedTag, eventService);

                log.info("Successfully added tag '{}' to adapter '{}' via hot-reload",
                        domainTag.getTagName(),
                        adapterId);
                return DomainTagAddResult.success();
            } catch (final IllegalStateException e) {
                log.error("Cannot hot-reload tag, adapter not in correct state: {}", e.getMessage());
                return DomainTagAddResult.failed(ADAPTER_FAILED_TO_START, adapterId);
            } catch (final Throwable e) {
                log.error("Exception happened while adding tag via hot-reload: ", e);
                return DomainTagAddResult.failed(ADAPTER_FAILED_TO_START, adapterId);
            }
        }).orElse(DomainTagAddResult.failed(ADAPTER_MISSING, adapterId));
    }

    public boolean updateNorthboundMappingsHotReload(
            final @NotNull String adapterId,
            final @NotNull List<NorthboundMapping> northboundMappings) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(wrapper -> updateMappingsHotReload(wrapper,
                "northbound",
                () -> wrapper.updateMappingsHotReload(northboundMappings, null, eventService))).orElse(false);
    }

    public boolean updateSouthboundMappingsHotReload(
            final @NotNull String adapterId,
            final @NotNull List<SouthboundMapping> southboundMappings) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(wrapper -> updateMappingsHotReload(wrapper,
                "southbound",
                () -> wrapper.updateMappingsHotReload(null, southboundMappings, eventService))).orElse(false);
    }

    public @NotNull List<DomainTag> getDomainTags() {
        return protocolAdapters.values()
                .stream()
                .flatMap(wrapper -> wrapper.getTags()
                        .stream()
                        .map(tag -> new DomainTag(tag.getName(),
                                wrapper.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .toList();
    }

    public @NotNull Optional<DomainTag> getDomainTagByName(final @NotNull String tagName) {
        return protocolAdapters.values()
                .stream()
                .flatMap(wrapper -> wrapper.getTags()
                        .stream()
                        .filter(t -> t.getName().equals(tagName))
                        .map(tag -> new DomainTag(tag.getName(),
                                wrapper.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .findFirst();
    }

    public @NotNull Optional<List<DomainTag>> getTagsForAdapter(final @NotNull String adapterId) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(adapterToConfig -> adapterToConfig.getTags()
                .stream()
                .map(tag -> new DomainTag(tag.getName(),
                        adapterToConfig.getId(),
                        tag.getDescription(),
                        configConverter.convertTagDefinitionToJsonNode(tag.getDefinition())))
                .toList());
    }

    private void shutdown() {
        if (shutdownInitiated.compareAndSet(false, true)) {
            shutdownRefreshExecutor();

            log.info("Initiating shutdown of Protocol Adapter Manager");
            final List<ProtocolAdapterWrapper> adaptersToStop = new ArrayList<>(protocolAdapters.values());
            if (adaptersToStop.isEmpty()) {
                log.debug("No adapters to stop during shutdown");
                return;
            }

            // initiate stop for all adapters
            log.info("Stopping {} protocol adapters during shutdown", adaptersToStop.size());
            final List<CompletableFuture<Void>> stopFutures = new ArrayList<>();
            for (final ProtocolAdapterWrapper wrapper : adaptersToStop) {
                try {
                    log.debug("Initiating stop for adapter '{}'", wrapper.getId());
                    stopFutures.add(wrapper.stopAsync());
                } catch (final Exception e) {
                    log.error("Error initiating stop for adapter '{}' during shutdown", wrapper.getId(), e);
                }
            }
            // wait for all adapters to stop, with timeout
            try {
                CompletableFuture.allOf(stopFutures.toArray(new CompletableFuture[0])).get(15, TimeUnit.SECONDS);
                log.info("All adapters stopped successfully during shutdown");
            } catch (final TimeoutException e) {
                log.warn("Timeout waiting for adapters to stop during shutdown");
                for (int i = 0; i < stopFutures.size(); i++) {
                    if (!stopFutures.get(i).isDone()) {
                        log.warn("Adapter '{}' did not complete stop operation", adaptersToStop.get(i).getId());
                    }
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for adapters to stop during shutdown", e);
            } catch (final Throwable e) {
                log.error("Error occurred while stopping adapters during shutdown", e.getCause());
            }
            log.info("Protocol Adapter Manager shutdown completed");
        }
    }

    private void shutdownRefreshExecutor() {
        final String name = "protocol-adapter-manager-refresh";
        final int timeoutSeconds = 5;
        log.debug("Shutting {} executor service", name);
        refreshExecutor.shutdown();
        try {
            if (!refreshExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                log.warn("Executor service {} did not terminate in {}s, forcing shutdown", name, timeoutSeconds);
                refreshExecutor.shutdownNow();
                if (!refreshExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                    log.error("Executor service {} still has running tasks after forced shutdown", name);
                }
            } else {
                log.debug("Executor service {} shut down successfully", name);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for executor service {} to terminate", name);
            refreshExecutor.shutdownNow();
        }
    }

    private @NotNull ProtocolAdapterWrapper createAdapterInternal(
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull String version) {
        return protocolAdapters.computeIfAbsent(config.getAdapterId(), ignored -> {
            final String configProtocolId = config.getProtocolId();
            // legacy handling, hardcoded here, to not add legacy stuff into the adapter-sdk
            final String adapterType = switch (configProtocolId) {
                case "ethernet-ip" -> "eip";
                case "opc-ua-client" -> "opcua";
                case "file_input" -> "file";
                default -> configProtocolId;
            };

            final Optional<ProtocolAdapterFactory<?>> maybeFactory = factoryManager.get(adapterType);
            if (maybeFactory.isEmpty()) {
                throw new IllegalArgumentException("Protocol adapter for config " + adapterType + " not found.");
            }
            final ProtocolAdapterFactory<?> factory = maybeFactory.get();
            log.info("Found configuration for adapter {} / {}", config.getAdapterId(), adapterType);
            config.missingTags().ifPresent(missing -> {
                throw new IllegalArgumentException("Tags used in mappings but not configured in adapter " +
                        config.getAdapterId() +
                        ": " +
                        missing);
            });

            return runWithContextLoader(factory.getClass().getClassLoader(), () -> {
                final ProtocolAdapterMetricsService metricsService =
                        new ProtocolAdapterMetricsServiceImpl(configProtocolId, config.getAdapterId(), metricRegistry);
                final ProtocolAdapterStateImpl state = new ProtocolAdapterStateImpl(moduleServices.eventService(),
                        config.getAdapterId(),
                        configProtocolId);
                final ModuleServicesPerModuleImpl perModule =
                        new ModuleServicesPerModuleImpl(moduleServices.adapterPublishService(),
                                eventService,
                                writingService,
                                tagManager);
                final ProtocolAdapter protocolAdapter = factory.createAdapter(factory.getInformation(),
                        new ProtocolAdapterInputImpl(configProtocolId,
                                config.getAdapterId(),
                                config.getAdapterConfig(),
                                config.getTags(),
                                config.getNorthboundMappings(),
                                version,
                                state,
                                perModule,
                                metricsService));
                // hen-egg problem. Rather solve this here as have not final fields in the adapter.
                perModule.setAdapter(protocolAdapter);
                protocolAdapterMetrics.increaseProtocolAdapterMetric(configProtocolId);
                return new ProtocolAdapterWrapper(metricsService,
                        writingService,
                        pollingService,
                        config,
                        protocolAdapter,
                        factory,
                        factory.getInformation(),
                        state,
                        northboundConsumerFactory,
                        tagManager,
                        sharedAdapterExecutor);
            });
        });
    }

    private void deleteAdapterInternal(final @NotNull String adapterId) {
        final ProtocolAdapterWrapper wrapper = protocolAdapters.remove(adapterId);
        if (wrapper != null) {
            protocolAdapterMetrics.decreaseProtocolAdapterMetric(wrapper.getAdapterInformation().getProtocolId());
            eventService.createAdapterEvent(adapterId, wrapper.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage("Adapter '" + adapterId + "' was deleted from the system permanently.")
                    .fire();
        } else {
            log.warn("Tried to delete adapter '{}' but it was not found in the system.", adapterId);
        }
    }

    @VisibleForTesting
    @NotNull CompletableFuture<Void> startAsync(final @NotNull ProtocolAdapterWrapper wrapper) {
        Preconditions.checkNotNull(wrapper);
        final String wid = wrapper.getId();
        log.info("Starting protocol-adapter '{}'.", wid);
        return requireNonNull(wrapper.startAsync(moduleServices)).whenComplete((result, throwable) -> {
            if (throwable == null) {
                log.info("Protocol-adapter '{}' started successfully.", wid);
                fireStartEvent(wrapper,
                        HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED,
                        Event.SEVERITY.INFO,
                        "Adapter '" + wid + "' started OK.");
            } else {
                log.warn("Protocol-adapter '{}' could not be started, reason: {}", wid, "unknown");
                fireStartEvent(wrapper,
                        HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_ERROR,
                        Event.SEVERITY.CRITICAL,
                        "Error starting adapter '" + wid + "'.");
            }
        });
    }

    private void fireStartEvent(
            final @NotNull ProtocolAdapterWrapper wrapper,
            final @NotNull HiveMQEdgeRemoteEvent.EVENT_TYPE eventType,
            final @NotNull Event.SEVERITY severity,
            final @NotNull String message) {
        final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
        final HiveMQEdgeRemoteEvent event = new HiveMQEdgeRemoteEvent(eventType);
        event.addUserData("adapterType", protocolId);
        remoteService.fireUsageEvent(event);
        eventService.createAdapterEvent(wrapper.getId(), protocolId).withSeverity(severity).withMessage(message).fire();
    }

    @VisibleForTesting
    @NotNull CompletableFuture<Void> stopAsync(final @NotNull ProtocolAdapterWrapper wrapper) {
        Preconditions.checkNotNull(wrapper);
        log.info("Stopping protocol-adapter '{}'.", wrapper.getId());
        return requireNonNull(wrapper.stopAsync()).whenComplete((result, throwable) -> {
            final Event.SEVERITY severity;
            final String message;
            final String wid = wrapper.getId();
            final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
            if (throwable == null) {
                log.info("Protocol-adapter '{}' stopped successfully.", wid);
                severity = Event.SEVERITY.INFO;
                message = "Adapter '" + wid + "' stopped OK.";
            } else {
                log.warn("Protocol-adapter '{}' was unable to stop cleanly", wrapper.getId());
                severity = Event.SEVERITY.CRITICAL;
                message = "Error stopping adapter '" + wid + "'.";
            }
            wrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
            eventService.createAdapterEvent(wid, protocolId).withSeverity(severity).withMessage(message).fire();
        });
    }
}
