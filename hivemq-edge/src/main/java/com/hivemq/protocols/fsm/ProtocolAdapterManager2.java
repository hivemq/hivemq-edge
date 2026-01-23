/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.protocols.fsm;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
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
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import com.hivemq.protocols.ProtocolAdapterInputImpl;
import com.hivemq.protocols.ProtocolAdapterMetrics;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        protocolAdapterWritingService.addWritingChangedCallback(() -> protocolAdapterFactoryManager.writingEnabledChanged(
                protocolAdapterWritingService.writingEnabled()));
    }

    public boolean isBusy() {
        // With ConcurrentHashMap, we don't have explicit locks
        // The refresh operation runs on a single-threaded executor
        return false;
    }

    public void register() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting adapters");
        }
        protocolAdapterConfig.registerConsumer(this::refresh);
    }

    public @NotNull Optional<ProtocolAdapterWrapper2> getProtocolAdapterWrapperByAdapterId(final @NotNull String adapterId) {
        Preconditions.checkNotNull(adapterId);
        return Optional.ofNullable(protocolAdapterMap.get(adapterId));
    }

    protected @NotNull Set<String> getProtocolAdapterIdSet() {
        return new HashSet<>(protocolAdapterMap.keySet());
    }

    protected void createProtocolAdapter(final @NotNull ProtocolAdapterConfig config, final @NotNull String version) {
        Preconditions.checkNotNull(config);
        final String adapterId = config.getAdapterId();

        // Check if adapter already exists (fast path)
        if (protocolAdapterMap.containsKey(adapterId)) {
            return;
        }

        final String configProtocolId = config.getProtocolId();
        // legacy handling, hardcoded here, to not add legacy stuff into the adapter-sdk
        final String adapterType = switch (configProtocolId) {
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
            throw new IllegalArgumentException("Tags used in mappings but not configured in adapter " +
                    adapterType +
                    ": " +
                    missingTag);
        });

        final ProtocolAdapterWrapper2 wrapper =
                ClassLoaderUtils.runWithContextLoader(factory.getClass().getClassLoader(), () -> {
                    final ProtocolAdapterMetricsService metricsService = new ProtocolAdapterMetricsServiceImpl(
                            configProtocolId,
                            config.getAdapterId(),
                            metricRegistry);
                    final ProtocolAdapterStateImpl state =
                            new ProtocolAdapterStateImpl(moduleServices.eventService(),
                                    config.getAdapterId(),
                                    configProtocolId);
                    final ModuleServicesPerModuleImpl perModule =
                            new ModuleServicesPerModuleImpl(moduleServices.adapterPublishService(),
                                    eventService,
                                    protocolAdapterWritingService,
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
                    return new ProtocolAdapterWrapper2(protocolAdapter);
                });

        // Use putIfAbsent to handle race conditions - if another thread added it first, we discard ours
        protocolAdapterMap.putIfAbsent(adapterId, wrapper);
    }

    protected @NotNull Optional<ProtocolAdapterWrapper2> deleteProtocolAdapterWrapperByAdapterId(final @NotNull String adapterId) {
        Preconditions.checkNotNull(adapterId);
        return Optional.ofNullable(protocolAdapterMap.remove(adapterId));
    }

    public void start(final @NotNull String adapterId) throws ProtocolAdapterException {
        final var optionalWrapper = getProtocolAdapterWrapperByAdapterId(adapterId);
        if (optionalWrapper.isEmpty()) {
            throw new ProtocolAdapterException(I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_NOT_FOUND.get(
                    Map.of(ADAPTER_ID, adapterId)));
        }
        optionalWrapper.get().start();
    }

    public void stop(final @NotNull String adapterId, final boolean destroy) throws ProtocolAdapterException {
        final var optionalWrapper = getProtocolAdapterWrapperByAdapterId(adapterId);
        if (optionalWrapper.isEmpty()) {
            throw new ProtocolAdapterException(I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_NOT_FOUND.get(
                    Map.of(ADAPTER_ID, adapterId)));
        }
        optionalWrapper.get().stop(destroy);
    }

    protected void deleteProtocolAdapterByAdapterId(final @NotNull String adapterId) {
        deleteProtocolAdapterWrapperByAdapterId(adapterId).ifPresentOrElse(wrapper -> {
            final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
            protocolAdapterMetrics.decreaseProtocolAdapterMetric(protocolId);
            eventService.createAdapterEvent(adapterId, protocolId)
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage(I18nProtocolAdapterMessage.PROTOCOL_ADAPTER_MANAGER_PROTOCOL_ADAPTER_DELETED.get(Map.of(
                            ADAPTER_ID,
                            adapterId)))
                    .fire();
        }, () -> LOGGER.warn("Tried to delete adapter '{}' but it was not found in the system.", adapterId));
    }

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

                final Set<String> failedAdapterSet = new HashSet<>();

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
                        .toArray(new CompletableFuture[0])).get();

                CompletableFuture.allOf(toBeCreatedProtocolAdapterIdSet.stream()
                        .map(adapterId -> CompletableFuture.runAsync(() -> {
                            try {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Creating adapter '{}'", adapterId);
                                }
                                createProtocolAdapter(protocolAdapterConfigs.get(adapterId),
                                        versionProvider.getVersion());
                                start(adapterId);
                            } catch (final Exception e) {
                                failedAdapterSet.add(adapterId);
                                LOGGER.error("Failed creating adapter {}", adapterId, e);
                            }
                        }))
                        .toList()
                        .toArray(new CompletableFuture[0])).get();

                CompletableFuture.allOf(toBeUpdatedProtocolAdapterIdSet.stream()
                        .map(adapterId -> CompletableFuture.runAsync(() -> {
                            try {
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Updating adapter '{}'", adapterId);
                                }
                                stop(adapterId, true);
                                deleteProtocolAdapterByAdapterId(adapterId);
                                createProtocolAdapter(protocolAdapterConfigs.get(adapterId),
                                        versionProvider.getVersion());
                                start(adapterId);
                            } catch (final Exception e) {
                                failedAdapterSet.add(adapterId);
                                LOGGER.error("Failed updating adapter {}", adapterId, e);
                            }
                        }))
                        .toList()
                        .toArray(new CompletableFuture[0])).get();

                if (failedAdapterSet.isEmpty()) {
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
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Interrupted while refreshing adapters", e);
            } catch (final ExecutionException e) {
                LOGGER.error("Failed refreshing adapters", e);
            }
        });
    }
}
