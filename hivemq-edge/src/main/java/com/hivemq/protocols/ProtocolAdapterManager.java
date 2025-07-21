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
import com.hivemq.adapter.sdk.api.tag.Tag;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ADAPTER_MISSING;
import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS;

@SuppressWarnings("unchecked")
@Singleton
public class ProtocolAdapterManager {
    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterManager.class);

    private final @NotNull Map<String, ProtocolAdapterWrapper> protocolAdapters = new ConcurrentHashMap<>();
    private final @NotNull Map<String, ProtocolAdapterConfig> knownConfigs = new ConcurrentHashMap<>();
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
    private final @NotNull AtomicBoolean refreshing = new AtomicBoolean(false);
    private final @NotNull ExecutorService executorService;

    @Inject
    public ProtocolAdapterManager(
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

    private @NotNull ProtocolAdapterWrapper createAdapterInternal(
            final @NotNull ProtocolAdapterConfig config, final @NotNull String version) {
        return protocolAdapters.computeIfAbsent(config.getAdapterId(), ignored -> {
            final String adapterType = getKey(config.getProtocolId());
            final Optional<ProtocolAdapterFactory<?>> protocolAdapterFactoryOptional =
                    protocolAdapterFactoryManager.get(adapterType);

            if (protocolAdapterFactoryOptional.isEmpty()) {
                throw new IllegalArgumentException("Protocol adapter for config " + adapterType + " not found.");
            }
            final ProtocolAdapterFactory<?> protocolAdapterFactory = protocolAdapterFactoryOptional.get();

            log.info("Found configuration for adapter {} / {}", config.getAdapterId(), adapterType);
            config.missingTags().ifPresent(missing -> {
                throw new IllegalArgumentException("Tags used in mappings but not configured in adapter " +
                        config.getAdapterId() +
                        ": " +
                        missing);
            });

            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(protocolAdapterFactory.getClass().getClassLoader());

                final ProtocolAdapterMetricsService protocolAdapterMetricsService = new ProtocolAdapterMetricsServiceImpl(
                        protocolAdapterFactory.getInformation().getProtocolId(),
                        config.getAdapterId(),
                        metricRegistry);

                final ProtocolAdapterStateImpl protocolAdapterState =
                        new ProtocolAdapterStateImpl(
                                moduleServices.eventService(),
                                config.getAdapterId(),
                                protocolAdapterFactory.getInformation().getProtocolId());

                final ModuleServicesPerModuleImpl moduleServicesPerModule =
                        new ModuleServicesPerModuleImpl(
                                moduleServices.adapterPublishService(),
                                eventService,
                                protocolAdapterWritingService,
                                tagManager);
                final ProtocolAdapter protocolAdapter =
                        protocolAdapterFactory.createAdapter(protocolAdapterFactory.getInformation(),
                                new ProtocolAdapterInputImpl(
                                        config.getProtocolId(),
                                        config.getAdapterId(),
                                        config.getAdapterConfig(),
                                        config.getTags(),
                                        config.getNorthboundMappings(),
                                        version,
                                        protocolAdapterState,
                                        moduleServicesPerModule,
                                        protocolAdapterMetricsService));
                // hen-egg problem. Rather solve this here as have not final fields in the adapter.
                moduleServicesPerModule.setAdapter(protocolAdapter);

                final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                        protocolAdapterMetricsService,
                        protocolAdapterWritingService,
                        protocolAdapterPollingService,
                        config,
                        protocolAdapter,
                        protocolAdapterFactory,
                        protocolAdapterFactory.getInformation(),
                        protocolAdapterState,
                        northboundConsumerFactory,
                        tagManager);

                protocolAdapterMetrics.increaseProtocolAdapterMetric(config.getProtocolId());
                knownConfigs.put(wrapper.getId(), config);
                return wrapper;

            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        });


    }

    private void deleteAdapterInternal(final @NotNull String adapterId) {
        final ProtocolAdapterWrapper adapterWrapper = protocolAdapters.remove(adapterId);
        if(adapterWrapper != null) {
            protocolAdapterMetrics.decreaseProtocolAdapterMetric(adapterWrapper.getAdapterInformation().getProtocolId());
            eventService.createAdapterEvent(adapterId, adapterWrapper.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage(String.format("Adapter '%s' was deleted from the system permanently.", adapterId))
                    .fire();
        } else {
            log.warn("Tried to delete adapter '{}' but it was not found in the system.", adapterId);
        }
    }

    public void start() {
        log.debug("Starting adapters");
        protocolAdapterConfig.registerConsumer(this::refresh);
    }

    public void refresh(final @NotNull List<ProtocolAdapterEntity> configs) {
        executorService.submit(() -> {
            if(refreshing.compareAndSet(false, true)) {
                try {
                    log.info("Refreshing adapters");

                    final Map<String, ProtocolAdapterConfig> protocolAdapterConfigs = configs
                            .stream()
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
                            log.debug("Deleting adapter '{}'", name);
                            stop(name, true).whenComplete((ignored, t) -> {
                                deleteAdapterInternal(name);
                            }).get();
                        } catch (final InterruptedException | ExecutionException e) {
                            failedAdapters.add(name);
                            log.error("Failed deleting adapter {}", name, e);
                        }
                    });

                    adaptersToBeCreated.forEach(name -> {
                        try {
                            log.debug("Creating adapter '{}'", name);
                            start(createAdapterInternal(protocolAdapterConfigs.get(name), versionProvider.getVersion())).get();
                        } catch (final InterruptedException | ExecutionException e) {
                            failedAdapters.add(name);
                            log.error("Failed adding adapter {}", name, e);
                        }
                    });

                    adaptersToBeUpdated.forEach(name -> {
                        try {
                            if(!protocolAdapterConfigs.get(name).equals(knownConfigs.get(name))) {
                                log.debug("Updating adapter '{}'", name);
                                stop(name, true)
                                        .thenApply(v -> {
                                            deleteAdapterInternal(name);
                                            return null;
                                        })
                                        .thenCompose(ignored ->
                                                start(
                                                        createAdapterInternal(protocolAdapterConfigs.get(name),
                                                                versionProvider.getVersion())))
                                        .get();
                            } else {
                                log.debug("Not-updating adapter '{}' since the config is unchanged", name);
                            }
                        } catch (final InterruptedException | ExecutionException e) {
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
                } finally {
                    refreshing.set(false);
                }
            } else {
                log.warn("Refresh already in progress, skipping this refresh call");
            }
        });
    }

    //legacy handling, hardcoded here, to not add legacy stuff into the adapter-sdk
    private static @NotNull String getKey(final @NotNull String key) {
        return switch (key) {
            case "ethernet-ip" -> "eip";
            case "opc-ua-client" -> "opcua";
            case "file_input" -> "file";
            default -> key;
        };
    }

    public boolean protocolAdapterFactoryExists(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return protocolAdapterFactoryManager.get(protocolAdapterType).isPresent();
    }

    public @NotNull CompletableFuture<Void> start(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        return getProtocolAdapterWrapperByAdapterId(protocolAdapterId)
                .map(this::start)
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    @VisibleForTesting
    @NotNull
    CompletableFuture<Void> start(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);

        log.info("Starting protocol-adapter '{}'.", protocolAdapterWrapper.getId());

        return protocolAdapterWrapper
                .startAsync(writingEnabled(), moduleServices)
                .thenApply(success -> {
                    if(success) {
                        log.info("Protocol-adapter '{}' started successfully.", protocolAdapterWrapper.getId());
                        final HiveMQEdgeRemoteEvent adapterCreatedEvent =
                                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED);
                        adapterCreatedEvent.addUserData("adapterType",
                                protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId());
                        remoteService.fireUsageEvent(adapterCreatedEvent);

                        eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                                        protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                                .withSeverity(Event.SEVERITY.INFO)
                                .withMessage(String.format("Adapter '%s' started OK.", protocolAdapterWrapper.getId()))
                                .fire();

                        return null;
                    } else {
                        log.warn("Protocol-adapter '{}' could not be started, reason: {}",
                                protocolAdapterWrapper.getId(), "unknown");
                        final HiveMQEdgeRemoteEvent adapterCreatedEvent =
                                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_ERROR);
                        adapterCreatedEvent.addUserData("adapterType",
                                protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId());
                        remoteService.fireUsageEvent(adapterCreatedEvent);

                        eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                                        protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                                .withSeverity(Event.SEVERITY.CRITICAL)
                                .withMessage("Error starting adapter '" + protocolAdapterWrapper.getId() + "'.")
                                .fire();
                        return null;
                    }
                });
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull String protocolAdapterId, final boolean destroy) {
        Preconditions.checkNotNull(protocolAdapterId);
        return getProtocolAdapterWrapperByAdapterId(protocolAdapterId)
                .map(wrapper -> stop(wrapper, destroy))
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    @VisibleForTesting
    @NotNull CompletableFuture<Void> stop(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper, final boolean destroy) {
        Preconditions.checkNotNull(protocolAdapterWrapper);
        log.info("Stopping protocol-adapter '{}'.", protocolAdapterWrapper.getId());

        return protocolAdapterWrapper
                .stopAsync(destroy)
                .thenApply(success -> {
                    if(success) {
                        log.info("Protocol-adapter '{}' stopped successfully.", protocolAdapterWrapper.getId());
                        protocolAdapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                        eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                                        protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                                .withSeverity(Event.SEVERITY.INFO)
                                .withMessage(String.format("Adapter '%s' stopped OK.", protocolAdapterWrapper.getId()))
                                .fire();
                        return null;
                    } else {
                        log.warn("Protocol-adapter '{}' was unable to stop cleanly", protocolAdapterWrapper.getId());
                        eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                                        protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                                .withSeverity(Event.SEVERITY.CRITICAL)
                                .withMessage("Error stopping adapter '" + protocolAdapterWrapper.getId() + "'.")
                                .fire();
                        return null;
                    }
                });
    }

    private void updateAdapter(final ProtocolAdapterConfig protocolAdapterConfig) {
        deleteAdapterInternal(protocolAdapterConfig.getAdapterId());
        syncFuture(start(createAdapterInternal(protocolAdapterConfig, versionProvider.getVersion())));
    }

    private boolean updateAdapterTags(final @NotNull String adapterId, final @NotNull List<? extends Tag> tags) {
        Preconditions.checkNotNull(adapterId);
        return getProtocolAdapterWrapperByAdapterId(adapterId)
                .map(oldInstance -> {
                    final String protocolId = oldInstance.getAdapterInformation().getProtocolId();
                    final ProtocolAdapterConfig protocolAdapterConfig = new ProtocolAdapterConfig(oldInstance.getId(),
                            protocolId,
                            oldInstance.getAdapterInformation().getCurrentConfigVersion(),
                            oldInstance.getConfigObject(),
                            oldInstance.getSouthboundMappings(),
                            oldInstance.getNorthboundMappings(),
                            tags);
                    updateAdapter(protocolAdapterConfig);
                    return true;
                })
                .orElse(false);
    }

    public @NotNull Optional<ProtocolAdapterWrapper> getProtocolAdapterWrapperByAdapterId(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        return Optional.ofNullable(protocolAdapters.get(id));
    }

    public @NotNull Optional<ProtocolAdapterInformation> getAdapterTypeById(final @NotNull String typeId) {
        Preconditions.checkNotNull(typeId);
        final ProtocolAdapterInformation information = getAllAvailableAdapterTypes().get(typeId);
        return Optional.ofNullable(information);
    }

    public @NotNull Map<String, ProtocolAdapterInformation> getAllAvailableAdapterTypes() {
        return protocolAdapterFactoryManager.getAllAvailableAdapterTypes();
    }


    public @NotNull Map<String, ProtocolAdapterWrapper> getProtocolAdapters() {
        return Map.copyOf(protocolAdapters);
    }


    public boolean writingEnabled() {
        return protocolAdapterWritingService.writingEnabled();
    }

    public @NotNull DomainTagAddResult addDomainTag(
            final @NotNull String adapterId, final @NotNull DomainTag domainTag) {
        return getProtocolAdapterWrapperByAdapterId(adapterId)
                .map(adapter -> {
                    final List<? extends Tag> tags = new ArrayList<>(adapter.getTags());
                    final boolean alreadyExists = tags.stream().anyMatch(t -> t.getName().equals(domainTag.getTagName()));
                    if (!alreadyExists) {
                        tags.add(configConverter.domaintTagToTag(adapter.getProtocolAdapterInformation().getProtocolId(),
                                domainTag));

                        updateAdapterTags(adapterId, tags);
                        return DomainTagAddResult.success();
                    } else {
                        return DomainTagAddResult.failed(ALREADY_EXISTS, adapterId);
                    }
                })
                .orElse(DomainTagAddResult.failed(ADAPTER_MISSING, adapterId));
    }

    public @NotNull List<DomainTag> getDomainTags() {
        return protocolAdapters.values()
                .stream()
                .flatMap(adapter -> adapter.getTags()
                        .stream()
                        .map(tag -> new DomainTag(tag.getName(),
                                adapter.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .collect(Collectors.toList());
    }

    public @NotNull Optional<DomainTag> getDomainTagByName(final @NotNull String tagName) {
        return protocolAdapters.values()
                .stream()
                .flatMap(adapter -> adapter.getTags()
                        .stream()
                        .filter(t -> t.getName().equals(tagName))
                        .map(tag -> new DomainTag(tag.getName(),
                                adapter.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .findFirst();
    }

    public @NotNull Optional<List<DomainTag>> getTagsForAdapter(final @NotNull String adapterId) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(adapter -> adapter.getTags()
                .stream()
                .map(tag -> new DomainTag(tag.getName(),
                        adapter.getId(),
                        tag.getDescription(),
                        configConverter.convertTagDefinitionToJsonNode(tag.getDefinition())))
                .collect(Collectors.toList()));
    }

    private static void syncFuture(final @NotNull Future<?> future) {
        try {
            future.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (final ExecutionException e) {
            log.error("Exception happened while async execution: ", e.getCause());
        }
    }
}
