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

import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ADAPTER_MISSING;
import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterTagStreamingServiceImpl;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.protocols.fsm.I18nProtocolAdapterMessage;
import com.hivemq.protocols.fsm.ProtocolAdapterManagerState;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import com.hivemq.util.ThreadFactoryUtil;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle of all protocol adapter instances.
 * <p>
 * Responsibilities:
 * <ol>
 *   <li>Creates, starts, stops, and deletes {@link ProtocolAdapterWrapper} instances</li>
 *   <li>Fires {@link Event}s on adapter lifecycle changes (start, stop, delete, config refresh)</li>
 *   <li>Handles configuration refreshes: diffs old vs new config and creates/updates/deletes adapters accordingly</li>
 *   <li>Delegates to {@link ProtocolAdapterFactoryManager} for adapter factory lookup</li>
 *   <li>Provides backward-compatible API for consumers that previously used
 *       {@link com.hivemq.protocols.ProtocolAdapterManager}</li>
 * </ol>
 * <p>
 * Threading Model:
 * <ul>
 *   <li>Adapter map uses {@link ConcurrentHashMap} for thread-safe access</li>
 *   <li>Configuration refresh runs on a single-threaded executor to serialize refresh operations</li>
 *   <li>Within a refresh, adapter create/update/delete operations run sequentially on that refresh thread</li>
 * </ul>
 */
@Singleton
public class ProtocolAdapterManager {
    private static final String ADAPTER_ID = "adapterId";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterManager.class);
    private static final int ADAPTER_LIFECYCLE_CORE_POOL_SIZE = 4;
    private static final int ADAPTER_LIFECYCLE_MAX_POOL_SIZE = 10;
    private static final int ADAPTER_LIFECYCLE_QUEUE_CAPACITY = 20;
    private static final int SHUTDOWN_STOP_TIMEOUT_SECONDS = 5;
    // ConcurrentHashMap provides thread-safe access without explicit locking
    private final @NotNull Map<String, ProtocolAdapterWrapper> protocolAdapterMap = new ConcurrentHashMap<>();
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterConfigConverter configConverter;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager;
    private final @NotNull NorthboundConsumerFactory northboundConsumerFactory;
    private final @NotNull TagManager tagManager;
    private final @NotNull ProtocolAdapterExtractor protocolAdapterConfig;
    private final @NotNull ExecutorService executorService;
    private final @NotNull ExecutorService adapterLifecycleExecutor;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull AtomicInteger refreshTasksInProgress = new AtomicInteger(0);
    private final @NotNull AtomicReference<ProtocolAdapterManagerState> managerState =
            new AtomicReference<>(ProtocolAdapterManagerState.Idle);

    @Inject
    public ProtocolAdapterManager(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterConfigConverter configConverter,
            final @NotNull VersionProvider versionProvider,
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager,
            final @NotNull NorthboundConsumerFactory northboundConsumerFactory,
            final @NotNull TagManager tagManager,
            final @NotNull ProtocolAdapterExtractor protocolAdapterConfig,
            final @NotNull ProtocolAdapterPublishService adapterPublishService) {
        this.metricRegistry = metricRegistry;
        this.remoteService = remoteService;
        this.eventService = eventService;
        this.configConverter = configConverter;
        this.versionProvider = versionProvider;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.protocolAdapterMetrics = protocolAdapterMetrics;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.protocolAdapterFactoryManager = protocolAdapterFactoryManager;
        this.northboundConsumerFactory = northboundConsumerFactory;
        this.tagManager = tagManager;
        this.protocolAdapterConfig = protocolAdapterConfig;
        this.adapterPublishService = adapterPublishService;
        this.executorService = Executors.newSingleThreadExecutor();
        this.adapterLifecycleExecutor = new ThreadPoolExecutor(
                ADAPTER_LIFECYCLE_CORE_POOL_SIZE,
                ADAPTER_LIFECYCLE_MAX_POOL_SIZE,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(ADAPTER_LIFECYCLE_QUEUE_CAPACITY),
                ThreadFactoryUtil.create("protocol-adapter-lifecycle-%d"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            adapterLifecycleExecutor.shutdown();
        }));
        protocolAdapterWritingService.addWritingChangedCallback(() ->
                protocolAdapterFactoryManager.writingEnabledChanged(protocolAdapterWritingService.writingEnabled()));
    }

    /**
     * Indicates whether one or more refresh operations are currently queued or running.
     * <p>
     * The value becomes {@code true} before a refresh task is handed to the executor and
     * returns to {@code false} only after the refresh task finishes (successfully or with
     * an error).
     *
     * @return {@code true} when refresh work is in progress, otherwise {@code false}
     */
    public boolean isBusy() {
        return refreshTasksInProgress.get() > 0;
    }

    /**
     * Returns the high-level manager state.
     *
     * @return {@link ProtocolAdapterManagerState#Running} while refresh work is queued/running; otherwise {@link ProtocolAdapterManagerState#Idle}
     */
    public @NotNull ProtocolAdapterManagerState getState() {
        return Objects.requireNonNull(managerState.get());
    }

    /**
     * Start the adapter manager — registers the config consumer to begin receiving adapter configs.
     * This replaces the old {@code ProtocolAdapterManager.start()} method.
     */
    public void start() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting adapters");
        }
        protocolAdapterConfig.registerConsumer(this::refresh);
    }

    /**
     * Shutdown all running adapters.
     */
    @VisibleForTesting
    public void shutdown() {
        protocolAdapterMap.values().forEach(wrapper -> {
            try {
                wrapper.stopAsync(true).get(SHUTDOWN_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                if (!wrapper.getState().isIdle()) {
                    LOGGER.error("Exception happened while shutting down adapter: ", e);
                }
            }
        });
        // Release the manager-owned executor threads so embedded Edge instances (tests) don't
        // leak parked threads across runs. The JVM-exit shutdown hook is kept as a fallback and
        // is idempotent.
        adapterLifecycleExecutor.shutdown();
        executorService.shutdown();
        try {
            adapterLifecycleExecutor.awaitTermination(SHUTDOWN_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            executorService.awaitTermination(SHUTDOWN_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===== Adapter Lookup =====

    public @NotNull Optional<ProtocolAdapterWrapper> getProtocolAdapterWrapperByAdapterId(
            final @NotNull String adapterId) {
        Preconditions.checkNotNull(adapterId);
        return Optional.ofNullable(protocolAdapterMap.get(adapterId));
    }

    protected @NotNull Set<String> getProtocolAdapterIdSet() {
        return new HashSet<>(protocolAdapterMap.keySet());
    }

    /**
     * Returns a defensive copy of all protocol adapters.
     * Used by REST API endpoints that need to iterate over all adapters.
     *
     * @return an unmodifiable copy of the adapter map
     */
    public @NotNull Map<String, ProtocolAdapterWrapper> getProtocolAdapters() {
        return Map.copyOf(protocolAdapterMap);
    }

    // ===== Factory/Type Lookups (delegate to ProtocolAdapterFactoryManager) =====

    public boolean protocolAdapterFactoryExists(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return protocolAdapterFactoryManager.get(protocolAdapterType).isPresent();
    }

    public @NotNull Optional<ProtocolAdapterInformation> getAdapterTypeById(final @NotNull String typeId) {
        Preconditions.checkNotNull(typeId);
        return Optional.ofNullable(getAllAvailableAdapterTypes().get(typeId));
    }

    public @NotNull Map<String, ProtocolAdapterInformation> getAllAvailableAdapterTypes() {
        return protocolAdapterFactoryManager.getAllAvailableAdapterTypes();
    }

    public boolean writingEnabled() {
        return protocolAdapterWritingService.writingEnabled();
    }

    // ===== Domain Tag Operations =====

    public @NotNull DomainTagAddResult addDomainTag(
            final @NotNull String adapterId, final @NotNull DomainTag domainTag) {
        return getProtocolAdapterWrapperByAdapterId(adapterId)
                .map(wrapper -> {
                    final var tags = new ArrayList<>(wrapper.getTags());
                    final boolean alreadyExists =
                            tags.stream().anyMatch(t -> t.getName().equals(domainTag.getTagName()));
                    if (!alreadyExists) {
                        tags.add(configConverter.domainTagToTag(
                                wrapper.getProtocolAdapterInformation().getProtocolId(), domainTag));
                        updateAdapterTags(adapterId, tags);
                        return DomainTagAddResult.success();
                    } else {
                        return DomainTagAddResult.failed(ALREADY_EXISTS, adapterId);
                    }
                })
                .orElse(DomainTagAddResult.failed(ADAPTER_MISSING, adapterId));
    }

    public @NotNull List<DomainTag> getDomainTags() {
        return protocolAdapterMap.values().stream()
                .flatMap(wrapper -> wrapper.getTags().stream()
                        .map(tag -> new DomainTag(
                                tag.getName(),
                                wrapper.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .toList();
    }

    public @NotNull Optional<DomainTag> getDomainTagByName(final @NotNull String tagName) {
        return protocolAdapterMap.values().stream()
                .flatMap(wrapper -> wrapper.getTags().stream()
                        .filter(t -> t.getName().equals(tagName))
                        .map(tag -> new DomainTag(
                                tag.getName(),
                                wrapper.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .findFirst();
    }

    public @NotNull Optional<List<DomainTag>> getTagsForAdapter(final @NotNull String adapterId) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(wrapper -> wrapper.getTags().stream()
                .map(tag -> new DomainTag(
                        tag.getName(),
                        wrapper.getId(),
                        tag.getDescription(),
                        configConverter.convertTagDefinitionToJsonNode(tag.getDefinition())))
                .toList());
    }

    private boolean updateAdapterTags(final @NotNull String adapterId, final @NotNull List<? extends Tag> tags) {
        Preconditions.checkNotNull(adapterId);
        return getProtocolAdapterWrapperByAdapterId(adapterId)
                .map(wrapper -> {
                    final var protocolId = wrapper.getAdapterInformation().getProtocolId();
                    final var newConfig = new ProtocolAdapterConfig(
                            wrapper.getId(),
                            protocolId,
                            wrapper.getAdapterInformation().getCurrentConfigVersion(),
                            wrapper.getConfigObject(),
                            wrapper.getSouthboundMappings(),
                            wrapper.getNorthboundMappings(),
                            tags);
                    updateAdapter(newConfig);
                    return true;
                })
                .orElse(false);
    }

    private void updateAdapter(final @NotNull ProtocolAdapterConfig config) {
        try {
            updateProtocolAdapterAtomically(config.getAdapterId(), config, versionProvider.getVersion());
        } catch (final ProtocolAdapterException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while async execution: ", cause);
            } else {
                LOGGER.error("Exception happened while async execution: ", cause == null ? e : cause);
            }
        }
    }

    // ===== Synchronous Start/Stop =====

    /**
     * Start an adapter by ID.
     * <p>
     * Delegates to {@link ProtocolAdapterWrapper#start()} and fires an adapter event
     * indicating success or failure.
     *
     * @param adapterId the adapter to start
     * @throws ProtocolAdapterException if the adapter is not found or fails to start
     */
    public void start(final @NotNull String adapterId) throws ProtocolAdapterException {
        LOGGER.info("Starting protocol-adapter '{}'.", adapterId);
        final var optionalWrapper = getProtocolAdapterWrapperByAdapterId(adapterId);
        if (optionalWrapper.isEmpty()) {
            throw new ProtocolAdapterException(
                    I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_NOT_FOUND.get(
                            Map.of(ADAPTER_ID, adapterId)));
        }
        final ProtocolAdapterWrapper wrapper = optionalWrapper.get();
        final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
        final boolean success = wrapper.start();

        if (success) {
            LOGGER.info("Protocol-adapter '{}' started successfully.", adapterId);
            final HiveMQEdgeRemoteEvent event =
                    new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED);
            event.addUserData("adapterType", protocolId);
            remoteService.fireUsageEvent(event);
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.INFO)
                    .withMessage("Adapter '" + adapterId + "' started OK.")
                    .fire();
        } else {
            LOGGER.warn("Protocol-adapter '{}' could not be started, reason: {}", adapterId, "unknown");
            final HiveMQEdgeRemoteEvent event =
                    new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_ERROR);
            event.addUserData("adapterType", protocolId);
            remoteService.fireUsageEvent(event);
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.CRITICAL)
                    .withMessage("Error starting adapter '" + adapterId + "'.")
                    .fire();
            throw new ProtocolAdapterException("Failed to start adapter: " + adapterId);
        }
    }

    /**
     * Stop an adapter by ID.
     *
     * @param adapterId the adapter to stop
     * @param destroy   whether to call {@link ProtocolAdapter#destroy()} after stopping
     * @throws ProtocolAdapterException if the adapter is not found
     */
    public void stop(final @NotNull String adapterId, final boolean destroy) throws ProtocolAdapterException {
        final var optionalWrapper = getProtocolAdapterWrapperByAdapterId(adapterId);
        if (optionalWrapper.isEmpty()) {
            throw new ProtocolAdapterException(
                    I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_NOT_FOUND.get(
                            Map.of(ADAPTER_ID, adapterId)));
        }
        stopWrapper(optionalWrapper.get(), adapterId, destroy);
    }

    /**
     * Stop (and optionally destroy) a specific wrapper instance and fire the corresponding adapter event. Unlike
     * {@link #stop(String, boolean)} this acts on the given instance rather than re-resolving it from the map,
     * which {@link #updateProtocolAdapterAtomically} relies on: by the time the displaced wrapper is stopped the
     * map already points at its replacement.
     */
    private void stopWrapper(
            final @NotNull ProtocolAdapterWrapper wrapper, final @NotNull String adapterId, final boolean destroy) {
        LOGGER.info("Stopping protocol-adapter '{}'.", adapterId);
        final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
        final boolean success = wrapper.stop(destroy);

        if (success) {
            LOGGER.info("Protocol-adapter '{}' stopped successfully.", adapterId);
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.INFO)
                    .withMessage("Adapter '" + adapterId + "' stopped OK.")
                    .fire();
        } else {
            LOGGER.warn("Protocol-adapter '{}' was unable to stop cleanly", adapterId);
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.CRITICAL)
                    .withMessage("Error stopping adapter '" + adapterId + "'.")
                    .fire();
        }
    }

    // ===== Async Start/Stop (for REST API backward compatibility) =====

    /**
     * Async wrapper for {@link #start(String)} — matches the old manager's API.
     */
    public @NotNull CompletableFuture<Void> startAsync(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        if (!protocolAdapterMap.containsKey(protocolAdapterId)) {
            return CompletableFuture.failedFuture(
                    new ProtocolAdapterException("Adapter not found: " + protocolAdapterId));
        }
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        start(protocolAdapterId);
                    } catch (final ProtocolAdapterException e) {
                        throw new RuntimeException(e);
                    }
                },
                adapterLifecycleExecutor);
    }

    /**
     * Async wrapper for {@link #stop(String, boolean)} — matches the old manager's API.
     */
    public @NotNull CompletableFuture<Void> stopAsync(final @NotNull String protocolAdapterId, final boolean destroy) {
        Preconditions.checkNotNull(protocolAdapterId);
        return CompletableFuture.runAsync(
                () -> {
                    try {
                        stop(protocolAdapterId, destroy);
                    } catch (final ProtocolAdapterException e) {
                        throw new RuntimeException(e);
                    }
                },
                adapterLifecycleExecutor);
    }

    // ===== Adapter Creation/Deletion =====

    protected void createProtocolAdapter(final @NotNull ProtocolAdapterConfig config, final @NotNull String version) {
        Preconditions.checkNotNull(config);
        final String adapterId = config.getAdapterId();
        protocolAdapterMap.computeIfAbsent(adapterId, ignored -> {
            final ProtocolAdapterWrapper wrapper = buildProtocolAdapterWrapper(config, version);
            protocolAdapterMetrics.increaseProtocolAdapterMetric(config.getProtocolId());
            return wrapper;
        });
    }

    /**
     * Build a fully-wired {@link ProtocolAdapterWrapper} for {@code config} without inserting it into
     * {@link #protocolAdapterMap} or mutating adapter metrics. Keeping construction side-effect free is what
     * lets an update build the replacement <i>before</i> evicting the old instance, so the adapter id is never
     * momentarily absent from the map — see {@link #updateProtocolAdapterAtomically} (EDG-602).
     */
    private @NotNull ProtocolAdapterWrapper buildProtocolAdapterWrapper(
            final @NotNull ProtocolAdapterConfig config, final @NotNull String version) {
        final String configProtocolId = config.getProtocolId();
        // legacy handling, hardcoded here, to not add legacy stuff into the adapter-sdk
        final String adapterType =
                switch (configProtocolId) {
                    case "ethernet-ip" -> "eip";
                    case "opc-ua-client" -> "opcua";
                    case "file_input" -> "file";
                    default -> configProtocolId;
                };

        final Optional<ProtocolAdapterFactory<?>> maybeFactory = protocolAdapterFactoryManager.get(adapterType);
        if (maybeFactory.isEmpty()) {
            throw new IllegalArgumentException("Protocol adapter for config " + adapterType + " not found.");
        }
        final ProtocolAdapterFactory<?> factory = maybeFactory.get();

        LOGGER.info("Found configuration for adapter {} / {}", config.getAdapterId(), adapterType);
        config.missingTags().ifPresent(missingTag -> {
            throw new IllegalArgumentException(
                    "Tags used in mappings but not configured in adapter " + adapterType + ": " + missingTag);
        });

        return runWithContextLoader(factory.getClass().getClassLoader(), () -> {
            final ProtocolAdapterMetricsService metricsService =
                    new ProtocolAdapterMetricsServiceImpl(configProtocolId, config.getAdapterId(), metricRegistry);
            final ProtocolAdapterStateImpl state =
                    new ProtocolAdapterStateImpl(eventService, config.getAdapterId(), configProtocolId);
            final var streamingService = new ProtocolAdapterTagStreamingServiceImpl(
                    config.getAdapterId(), tagManager, dataPointBuilder -> {});
            final ModuleServicesPerModuleImpl perModule = new ModuleServicesPerModuleImpl(
                    adapterPublishService, eventService, protocolAdapterWritingService, streamingService);
            final ProtocolAdapter protocolAdapter = factory.createAdapter(
                    factory.getInformation(),
                    new ProtocolAdapterInputImpl(
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
            return new ProtocolAdapterWrapper(
                    protocolAdapter,
                    config,
                    factory,
                    factory.getInformation(),
                    metricsService,
                    state,
                    protocolAdapterPollingService,
                    eventService,
                    perModule,
                    tagManager,
                    northboundConsumerFactory,
                    protocolAdapterWritingService,
                    adapterLifecycleExecutor);
        });
    }

    /**
     * Replace a running adapter's wrapper with one built from {@code config} without ever removing the adapter
     * id from {@link #protocolAdapterMap}. The replacement is constructed first, then swapped in with a single
     * {@code put}; only afterwards is the displaced instance stopped and destroyed. A concurrent REST read
     * therefore always resolves a wrapper — the old tags before the swap, the new tags after — and never the
     * 404 that the previous delete-then-recreate sequence exposed (EDG-602). If construction fails the old
     * wrapper is left untouched and running, and the throw is surfaced to the caller as an update failure.
     * <p>
     * The adapter metric is intentionally left untouched: an update does not change how many adapters of this
     * protocol exist.
     */
    private void updateProtocolAdapterAtomically(
            final @NotNull String adapterId, final @NotNull ProtocolAdapterConfig config, final @NotNull String version)
            throws ProtocolAdapterException {
        final ProtocolAdapterWrapper newWrapper = buildProtocolAdapterWrapper(config, version);
        final ProtocolAdapterWrapper previous = protocolAdapterMap.put(adapterId, newWrapper);
        if (previous != null) {
            // The displaced instance is no longer reachable through the map; stop and destroy it by reference so
            // stop(adapterId, ...) cannot act on the freshly-installed wrapper instead.
            stopWrapper(previous, adapterId, true);
        }
        start(adapterId);
    }

    protected @NotNull Optional<ProtocolAdapterWrapper> deleteProtocolAdapterWrapperByAdapterId(
            final @NotNull String adapterId) {
        Preconditions.checkNotNull(adapterId);
        return Optional.ofNullable(protocolAdapterMap.remove(adapterId));
    }

    protected void deleteProtocolAdapterByAdapterId(final @NotNull String adapterId) {
        deleteProtocolAdapterWrapperByAdapterId(adapterId)
                .ifPresentOrElse(
                        wrapper -> {
                            final String protocolId =
                                    wrapper.getProtocolAdapterInformation().getProtocolId();
                            protocolAdapterMetrics.decreaseProtocolAdapterMetric(protocolId);
                            eventService
                                    .createAdapterEvent(adapterId, protocolId)
                                    .withSeverity(Event.SEVERITY.WARN)
                                    .withMessage("Adapter '" + adapterId + "' was deleted from the system permanently.")
                                    .fire();
                        },
                        () -> LOGGER.warn(
                                "Tried to delete adapter '{}' but it was not found in the system.", adapterId));
    }

    // ===== Configuration Refresh =====

    /**
     * Refreshes protocol adapters from the latest configuration snapshot.
     * <p>
     * The operation is serialized on a dedicated single-thread executor. Any concurrent calls are queued
     * and processed in order.
     *
     * @param configs the latest adapter configuration entities
     */
    public void refresh(final @NotNull List<ProtocolAdapterEntity> configs) {
        refreshTasksInProgress.incrementAndGet();
        managerState.set(ProtocolAdapterManagerState.Running);
        try {
            executorService.execute(() -> {
                LOGGER.info("Refreshing adapters");
                try {
                    final Map<String, ProtocolAdapterConfig> protocolAdapterConfigs = configs.stream()
                            .map(configConverter::fromEntity)
                            .collect(Collectors.toMap(ProtocolAdapterConfig::getAdapterId, Function.identity()));

                    final Set<String> oldProtocolAdapterIdSet = getProtocolAdapterIdSet();
                    final Set<String> newProtocolAdapterIdSet = new HashSet<>(protocolAdapterConfigs.keySet());

                    final Set<String> toBeDeletedProtocolAdapterIdSet =
                            new HashSet<>(Sets.difference(oldProtocolAdapterIdSet, newProtocolAdapterIdSet));
                    final Set<String> toBeCreatedProtocolAdapterIdSet =
                            new HashSet<>(Sets.difference(newProtocolAdapterIdSet, oldProtocolAdapterIdSet));
                    final Set<String> toBeUpdatedProtocolAdapterIdSet =
                            new HashSet<>(Sets.intersection(newProtocolAdapterIdSet, oldProtocolAdapterIdSet));

                    final Set<String> failedAdapterSet = new HashSet<>();
                    refreshDeletedAdapters(toBeDeletedProtocolAdapterIdSet, failedAdapterSet);
                    refreshCreatedAdapters(toBeCreatedProtocolAdapterIdSet, protocolAdapterConfigs, failedAdapterSet);
                    refreshUpdatedAdapters(toBeUpdatedProtocolAdapterIdSet, protocolAdapterConfigs, failedAdapterSet);

                    if (failedAdapterSet.isEmpty()) {
                        eventService
                                .configurationEvent()
                                .withSeverity(Event.SEVERITY.INFO)
                                .withMessage("Configuration has been successfully updated")
                                .fire();
                    } else {
                        eventService
                                .configurationEvent()
                                .withSeverity(Event.SEVERITY.CRITICAL)
                                .withMessage("Reloading of configuration failed")
                                .fire();
                    }
                } catch (final Exception e) {
                    LOGGER.error("Failed refreshing adapters", e);
                } finally {
                    final int remainingTasks = refreshTasksInProgress.decrementAndGet();
                    if (remainingTasks == 0) {
                        managerState.set(ProtocolAdapterManagerState.Idle);
                    }
                }
            });
        } catch (final RejectedExecutionException e) {
            final int remainingTasks = refreshTasksInProgress.decrementAndGet();
            if (remainingTasks == 0) {
                managerState.set(ProtocolAdapterManagerState.Idle);
            }
            if (executorService.isShutdown()) {
                LOGGER.debug("Adapter refresh task submission rejected, executor is shutting down.");
            } else {
                throw e;
            }
        } catch (final RuntimeException e) {
            final int remainingTasks = refreshTasksInProgress.decrementAndGet();
            if (remainingTasks == 0) {
                managerState.set(ProtocolAdapterManagerState.Idle);
            }
            throw e;
        }
    }

    private void refreshDeletedAdapters(
            final @NotNull Set<String> adapterIds, final @NotNull Set<String> failedAdapterSet) {
        for (final String adapterId : adapterIds) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Deleting adapter '{}'", adapterId);
                }
                stop(adapterId, true);
                deleteProtocolAdapterByAdapterId(adapterId);
            } catch (final Exception e) {
                failedAdapterSet.add(adapterId);
                if (e.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted while deleting adapter {}", adapterId, e);
                } else {
                    LOGGER.error("Failed deleting adapter {}", adapterId, e);
                }
            }
        }
    }

    private void refreshCreatedAdapters(
            final @NotNull Set<String> adapterIds,
            final @NotNull Map<String, ProtocolAdapterConfig> protocolAdapterConfigs,
            final @NotNull Set<String> failedAdapterSet) {
        for (final String adapterId : adapterIds) {
            try {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Creating adapter '{}'", adapterId);
                }
                final ProtocolAdapterConfig protocolAdapterConfig = protocolAdapterConfigs.get(adapterId);
                if (protocolAdapterConfig == null) {
                    LOGGER.error("Config for adapter '{}' not found, skipping creation", adapterId);
                    continue;
                }
                createProtocolAdapter(protocolAdapterConfig, versionProvider.getVersion());
                start(adapterId);
            } catch (final Exception e) {
                failedAdapterSet.add(adapterId);
                if (e.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted while adding adapter {}", adapterId, e);
                } else {
                    LOGGER.error("Failed adding adapter {}", adapterId, e);
                }
            }
        }
    }

    private void refreshUpdatedAdapters(
            final @NotNull Set<String> adapterIds,
            final @NotNull Map<String, ProtocolAdapterConfig> protocolAdapterConfigs,
            final @NotNull Set<String> failedAdapterSet) {
        for (final String adapterId : adapterIds) {
            try {
                final ProtocolAdapterWrapper wrapper = protocolAdapterMap.get(adapterId);
                if (wrapper == null) {
                    failedAdapterSet.add(adapterId);
                    LOGGER.error(
                            "Existing adapters were modified while a refresh was ongoing, adapter with name '{}' was deleted and could not be updated",
                            adapterId);
                    continue;
                }
                final ProtocolAdapterConfig protocolAdapterConfig = protocolAdapterConfigs.get(adapterId);
                if (protocolAdapterConfig == null) {
                    LOGGER.error("Config for adapter '{}' not found, skipping update", adapterId);
                    continue;
                }
                if (protocolAdapterConfig.equals(wrapper.getConfig())) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Not-updating adapter '{}' since the config is unchanged", adapterId);
                    }
                    continue;
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Updating adapter '{}'", adapterId);
                }
                // Swap the wrapper in place: build the replacement, atomically replace the map entry, then stop
                // the displaced instance. The adapter id stays resolvable throughout, so a concurrent REST read
                // never observes the transient 404 the old delete-then-recreate sequence could expose (EDG-602).
                updateProtocolAdapterAtomically(adapterId, protocolAdapterConfig, versionProvider.getVersion());
            } catch (final Exception e) {
                failedAdapterSet.add(adapterId);
                if (e.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Interrupted while updating adapter {}", adapterId, e);
                } else {
                    LOGGER.error("Failed updating adapter {}", adapterId, e);
                }
            }
        }
    }

    // ===== Static Utility (backward compat) =====

    /**
     * Runs a supplier with a specific classloader set as the thread's context classloader.
     */
    public static <T> @NotNull T runWithContextLoader(
            final @NotNull ClassLoader contextLoader, final @NotNull Supplier<T> supplier) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(contextLoader);
            return supplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    @VisibleForTesting
    public @NotNull TagManager getTagManager() {
        return tagManager;
    }
}
