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
package com.hivemq.protocols.fsm;

import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ADAPTER_MISSING;
import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapter2;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import com.hivemq.protocols.ProtocolAdapterInputImpl;
import com.hivemq.protocols.ProtocolAdapterMetrics;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
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
 *   <li>Creates, starts, stops, and deletes {@link ProtocolAdapterWrapper2} instances</li>
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
 *   <li>Within a refresh, individual adapter create/delete operations may run concurrently</li>
 * </ul>
 */
@Singleton
public class ProtocolAdapterManager2 {
    public static final String ADAPTER_ID = "adapterId";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterManager2.class);
    // ConcurrentHashMap provides thread-safe access without explicit locking
    private final @NotNull Map<String, ProtocolAdapterWrapper2> protocolAdapterMap = new ConcurrentHashMap<>();
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ModuleServicesImpl moduleServices;
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

    @Inject
    public ProtocolAdapterManager2(
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ModuleServicesImpl moduleServices,
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
            final @NotNull ProtocolAdapterExtractor protocolAdapterConfig) {
        this.metricRegistry = metricRegistry;
        this.moduleServices = moduleServices;
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
        this.executorService = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
        protocolAdapterWritingService.addWritingChangedCallback(() ->
                protocolAdapterFactoryManager.writingEnabledChanged(protocolAdapterWritingService.writingEnabled()));
    }

    public boolean isBusy() {
        return false;
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
                wrapper.stop(true);
            } catch (final Exception e) {
                LOGGER.error("Exception happened while shutting down adapter '{}': ", wrapper.getAdapterId(), e);
            }
        });
    }

    // ===== Adapter Lookup =====

    public @NotNull Optional<ProtocolAdapterWrapper2> getProtocolAdapterWrapperByAdapterId(
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
    public @NotNull Map<String, ProtocolAdapterWrapper2> getProtocolAdapters() {
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
        deleteProtocolAdapterByAdapterId(config.getAdapterId());
        createProtocolAdapter(config, versionProvider.getVersion());
        try {
            start(config.getAdapterId());
        } catch (final ProtocolAdapterException e) {
            LOGGER.error("Failed to start adapter '{}' after update", config.getAdapterId(), e);
        }
    }

    // ===== Synchronous Start/Stop =====

    /**
     * Start an adapter by ID.
     * <p>
     * Delegates to {@link ProtocolAdapterWrapper2#start()} and fires an adapter event
     * indicating success or failure.
     *
     * @param adapterId the adapter to start
     * @throws ProtocolAdapterException if the adapter is not found or fails to start
     */
    public void start(final @NotNull String adapterId) throws ProtocolAdapterException {
        final var optionalWrapper = getProtocolAdapterWrapperByAdapterId(adapterId);
        if (optionalWrapper.isEmpty()) {
            throw new ProtocolAdapterException(
                    I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_NOT_FOUND.get(
                            Map.of(ADAPTER_ID, adapterId)));
        }
        final ProtocolAdapterWrapper2 wrapper = optionalWrapper.get();
        final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
        final boolean success = wrapper.start();

        if (success) {
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.INFO)
                    .withMessage("Adapter started successfully")
                    .fire();
        } else {
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withMessage("Adapter failed to start")
                    .fire();
            throw new ProtocolAdapterException("Failed to start adapter: " + adapterId);
        }
    }

    /**
     * Stop an adapter by ID.
     *
     * @param adapterId the adapter to stop
     * @param destroy   whether to call {@link ProtocolAdapter2#destroy()} after stopping
     * @throws ProtocolAdapterException if the adapter is not found
     */
    public void stop(final @NotNull String adapterId, final boolean destroy) throws ProtocolAdapterException {
        final var optionalWrapper = getProtocolAdapterWrapperByAdapterId(adapterId);
        if (optionalWrapper.isEmpty()) {
            throw new ProtocolAdapterException(
                    I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_NOT_FOUND.get(
                            Map.of(ADAPTER_ID, adapterId)));
        }
        final ProtocolAdapterWrapper2 wrapper = optionalWrapper.get();
        final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
        final boolean success = wrapper.stop(destroy);

        if (success) {
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.INFO)
                    .withMessage("Adapter stopped successfully")
                    .fire();
        } else {
            eventService
                    .createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage("Adapter stopped with errors")
                    .fire();
        }
    }

    // ===== Async Start/Stop (for REST API backward compatibility) =====

    /**
     * Async wrapper for {@link #start(String)} — matches the old manager's API.
     */
    public @NotNull CompletableFuture<Void> startAsync(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        return CompletableFuture.runAsync(() -> {
            try {
                start(protocolAdapterId);
            } catch (final ProtocolAdapterException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Async wrapper for {@link #stop(String, boolean)} — matches the old manager's API.
     */
    public @NotNull CompletableFuture<Void> stopAsync(final @NotNull String protocolAdapterId, final boolean destroy) {
        Preconditions.checkNotNull(protocolAdapterId);
        return CompletableFuture.runAsync(() -> {
            try {
                stop(protocolAdapterId, destroy);
            } catch (final ProtocolAdapterException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ===== Adapter Creation/Deletion =====

    protected void createProtocolAdapter(final @NotNull ProtocolAdapterConfig config, final @NotNull String version) {
        Preconditions.checkNotNull(config);
        final String adapterId = config.getAdapterId();

        // Check if adapter already exists (fast path)
        if (protocolAdapterMap.containsKey(adapterId)) {
            return;
        }

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

        final ProtocolAdapterWrapper2 wrapper =
                ClassLoaderUtils.runWithContextLoader(factory.getClass().getClassLoader(), () -> {
                    final ProtocolAdapterMetricsService metricsService = new ProtocolAdapterMetricsServiceImpl(
                            configProtocolId, config.getAdapterId(), metricRegistry);
                    final ProtocolAdapterStateImpl state = new ProtocolAdapterStateImpl(
                            moduleServices.eventService(), config.getAdapterId(), configProtocolId);
                    final ModuleServicesPerModuleImpl perModule = new ModuleServicesPerModuleImpl(
                            moduleServices.adapterPublishService(),
                            eventService,
                            protocolAdapterWritingService,
                            tagManager);
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
                    protocolAdapterMetrics.increaseProtocolAdapterMetric(configProtocolId);
                    final ProtocolAdapter2 adapter2 = factory.createProtocolAdapter2(protocolAdapter, perModule);
                    return new ProtocolAdapterWrapper2(
                            adapter2,
                            config,
                            factory,
                            factory.getInformation(),
                            metricsService,
                            state,
                            protocolAdapterPollingService,
                            eventService,
                            tagManager,
                            northboundConsumerFactory,
                            protocolAdapterWritingService);
                });

        // Use putIfAbsent to handle race conditions - if another thread added it first, we discard ours
        protocolAdapterMap.putIfAbsent(adapterId, wrapper);
    }

    protected @NotNull Optional<ProtocolAdapterWrapper2> deleteProtocolAdapterWrapperByAdapterId(
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
                                    .withMessage(
                                            I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_DELETED
                                                    .get(Map.of(ADAPTER_ID, adapterId)))
                                    .fire();
                        },
                        () -> LOGGER.warn(
                                "Tried to delete adapter '{}' but it was not found in the system.", adapterId));
    }

    // ===== Configuration Refresh =====

    protected void refresh(final @NotNull List<ProtocolAdapterEntity> configs) {
        executorService.submit(() -> {
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

                final Set<String> failedAdapterSet = ConcurrentHashMap.newKeySet();

                CompletableFuture.allOf(toBeDeletedProtocolAdapterIdSet.stream()
                                .map(adapterId -> CompletableFuture.runAsync(() -> {
                                    try {
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("Deleting adapter '{}'", adapterId);
                                        }
                                        stop(adapterId, true);
                                        deleteProtocolAdapterByAdapterId(adapterId);
                                    } catch (final Exception e) {
                                        failedAdapterSet.add(adapterId);
                                        LOGGER.error("Failed deleting adapter {}", adapterId, e);
                                    }
                                }))
                                .toList()
                                .toArray(new CompletableFuture[0]))
                        .get();

                CompletableFuture.allOf(toBeCreatedProtocolAdapterIdSet.stream()
                                .map(adapterId -> CompletableFuture.runAsync(() -> {
                                    try {
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("Creating adapter '{}'", adapterId);
                                        }
                                        createProtocolAdapter(
                                                protocolAdapterConfigs.get(adapterId), versionProvider.getVersion());
                                        start(adapterId);
                                    } catch (final Exception e) {
                                        failedAdapterSet.add(adapterId);
                                        LOGGER.error("Failed creating adapter {}", adapterId, e);
                                    }
                                }))
                                .toList()
                                .toArray(new CompletableFuture[0]))
                        .get();

                CompletableFuture.allOf(toBeUpdatedProtocolAdapterIdSet.stream()
                                .map(adapterId -> CompletableFuture.runAsync(() -> {
                                    try {
                                        if (LOGGER.isDebugEnabled()) {
                                            LOGGER.debug("Updating adapter '{}'", adapterId);
                                        }
                                        stop(adapterId, true);
                                        deleteProtocolAdapterByAdapterId(adapterId);
                                        createProtocolAdapter(
                                                protocolAdapterConfigs.get(adapterId), versionProvider.getVersion());
                                        start(adapterId);
                                    } catch (final Exception e) {
                                        failedAdapterSet.add(adapterId);
                                        LOGGER.error("Failed updating adapter {}", adapterId, e);
                                    }
                                }))
                                .toList()
                                .toArray(new CompletableFuture[0]))
                        .get();

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
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while refreshing adapters", e);
            } catch (final ExecutionException e) {
                LOGGER.error("Failed refreshing adapters", e);
            }
        });
    }

    // ===== Static Utility (backward compat) =====

    /**
     * Runs a supplier with a specific classloader set as the thread's context classloader.
     * Delegates to {@link ClassLoaderUtils#runWithContextLoader}.
     */
    public static <T> @NotNull T runWithContextLoader(
            final @NotNull ClassLoader contextLoader, final @NotNull java.util.function.Supplier<T> supplier) {
        return ClassLoaderUtils.runWithContextLoader(contextLoader, supplier);
    }
}
