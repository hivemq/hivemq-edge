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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.AdapterConfigWithPollingContexts;
import com.hivemq.adapter.sdk.api.config.AdapterConfigWithWritingContexts;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.domain.DomainTagDeleteResult;
import com.hivemq.persistence.domain.DomainTagUpdateResult;
import com.hivemq.persistence.fieldmapping.FieldMappings;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ADAPTER_MISSING;
import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS;
import static com.hivemq.persistence.domain.DomainTagDeleteResult.DomainTagDeleteStatus.NOT_FOUND;
import static com.hivemq.persistence.domain.DomainTagUpdateResult.DomainTagUpdateStatus.ADAPTER_NOT_FOUND;
import static com.hivemq.persistence.domain.DomainTagUpdateResult.DomainTagUpdateStatus.TAG_NOT_FOUND;

@SuppressWarnings("unchecked")
@Singleton
public class ProtocolAdapterManager {
    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterManager.class);

    private final @NotNull Map<String, ProtocolAdapterWrapper> protocolAdapters = new ConcurrentHashMap<>();
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ModuleServicesImpl moduleServices;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterConfigConverter configConverter;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics;
    private final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator;
    private final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ExecutorService executorService;
    private final @NotNull ConfigPersistence configPersistence;
    private final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager;

    private final @NotNull Object lock = new Object();

    @Inject
    public ProtocolAdapterManager(
            final @NotNull ConfigurationService configurationService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ModuleServicesImpl moduleServices,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ModuleLoader moduleLoader,
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull EventService eventService,
            final @NotNull ConfigPersistence configPersistence,
            final @NotNull ProtocolAdapterConfigConverter configConverter,
            final @NotNull VersionProvider versionProvider,
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator,
            final @NotNull InternalProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ProtocolAdapterFactoryManager protocolAdapterFactoryManager,
            final @NotNull ExecutorService executorService) {
        this.metricRegistry = metricRegistry;
        this.moduleServices = moduleServices;
        this.configPersistence = configPersistence;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.remoteService = remoteService;
        this.eventService = eventService;
        this.configConverter = configConverter;
        this.versionProvider = versionProvider;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.protocolAdapterMetrics = protocolAdapterMetrics;
        this.jsonPayloadDefaultCreator = jsonPayloadDefaultCreator;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.executorService = executorService;
        this.protocolAdapterFactoryManager = protocolAdapterFactoryManager;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NotNull ListenableFuture<Void> start() {

        log.info("Starting adapters");
        protocolAdapterWritingService.addWritingChangedCallback(() -> protocolAdapterFactoryManager.writingEnabledChanged(
                protocolAdapterWritingService.writingEnabled()));

        final ImmutableList.Builder<CompletableFuture<Void>> adapterFutures = ImmutableList.builder();

        for (final ProtocolAdapterConfig adapterConfig : configPersistence.allAdapters()) {
            final String adapterType = getKey(adapterConfig.getProtocolId());
            final Optional<ProtocolAdapterFactory<?>> protocolAdapterFactoryOptional =
                    protocolAdapterFactoryManager.get(adapterType);
            if (protocolAdapterFactoryOptional.isEmpty()) {
                return Futures.immediateFailedFuture(new IllegalArgumentException("Protocol adapter for config " +
                        adapterType +
                        " not found."));
            }
            final ProtocolAdapterFactory<?> protocolAdapterFactory = protocolAdapterFactoryOptional.get();

            log.info("Found configuration for adapter {} / {}", adapterConfig.getAdapterId(), adapterType);
            adapterConfig.missingTags().ifPresent(missing -> {
                throw new IllegalArgumentException("Tags used in mappings but not configured in adapter " +
                        adapterConfig.getAdapterId() +
                        ": " +
                        missing);
            });

            final ProtocolAdapterWrapper instance =
                    createAdapterInstance(protocolAdapterFactory, adapterConfig, versionProvider.getVersion());
            protocolAdapterMetrics.increaseProtocolAdapterMetric(instance.getAdapter()
                    .getProtocolAdapterInformation()
                    .getProtocolId());
            final CompletableFuture<Void> future = start(instance);
            adapterFutures.add(future);
        }

        return FutureConverter.toListenableFuture(CompletableFuture.allOf(adapterFutures.build()
                .toArray(new CompletableFuture[]{})));
    }

    //legacy handling, hardcoded here, to not add legacy stuff into the adapter-sdk
    private static @NotNull String getKey(final @NotNull String key) {
        switch (key) {
            case "ethernet-ip":
                return "eip";
            case "opc-ua-client":
                return "opcua";
            case "file_input":
                return "file";
        }
        return key;
    }

    public boolean protocolAdapterFactoryExists(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return protocolAdapterFactoryManager.get(protocolAdapterType).isPresent();
    }

    public @NotNull CompletableFuture<Void> start(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        final Optional<ProtocolAdapterWrapper> adapterOptional = getAdapterById(protocolAdapterId);
        return adapterOptional.map(this::start)
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    @VisibleForTesting
    @NotNull
    CompletableFuture<Void> start(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);

        log.info("Starting protocol-adapter '{}'.", protocolAdapterWrapper.getId());
        final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
        protocolAdapterWrapper.start(new ProtocolAdapterStartInputImpl(moduleServices, eventService), output);
        return output.getStartFuture().thenComposeAsync(ignored -> {
            schedulePolling(protocolAdapterWrapper);
            return startWriting(protocolAdapterWrapper);
        }, executorService).<Void>thenApplyAsync(unused -> {
            eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                            protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.INFO)
                    .withMessage(String.format("Adapter '%s' started OK.", protocolAdapterWrapper.getId()))
                    .fire();

            log.info("Protocol-adapter '{}' started successfully.", protocolAdapterWrapper.getId());
            final HiveMQEdgeRemoteEvent adapterCreatedEvent =
                    new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED);
            adapterCreatedEvent.addUserData("adapterType",
                    protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId());
            remoteService.fireUsageEvent(adapterCreatedEvent);
            protocolAdapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
            return null;
        }, executorService).exceptionally(throwable -> {
            try {
                output.failStart(throwable, output.getMessage());
                handleStartupError(protocolAdapterWrapper.getAdapter(), output);
            } finally {
                startFailedStop(protocolAdapterWrapper);
            }
            return null;
        });
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        final Optional<ProtocolAdapterWrapper> adapterOptional = getAdapterById(protocolAdapterId);
        return adapterOptional.map(this::stop)
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    private @NotNull CompletableFuture<Void> startWriting(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        final CompletableFuture<Void> startWritingFuture;
        if (writingEnabled() && protocolAdapterWrapper.getAdapter() instanceof WritingProtocolAdapter) {
            if (log.isDebugEnabled()) {
                log.debug("Start writing for protocol adapter with id '{}'", protocolAdapterWrapper.getId());
            }

            final List<FieldMappings> fieldMappings = protocolAdapterWrapper.getFieldMappings();

            final List<ToEdgeMapping> toEdgeMappings = protocolAdapterWrapper.getToEdgeMappings();
            final List<InternalWritingContext> writingContexts = toEdgeMappings.stream()
                    .map(toEdgeMapping -> new WritingContextImpl(toEdgeMapping.getTagName(),
                            toEdgeMapping.getTopicFilter(),
                            toEdgeMapping.getMaxQoS(),
                            findFieldMapping(fieldMappings, toEdgeMapping, protocolAdapterWrapper.getId())))
                    .collect(Collectors.toList());

            startWritingFuture =
                    protocolAdapterWritingService.startWriting((WritingProtocolAdapter<WritingContext>) protocolAdapterWrapper.getAdapter(),
                            protocolAdapterWrapper.getProtocolAdapterMetricsService(),
                            writingContexts);
        } else {
            startWritingFuture = CompletableFuture.completedFuture(null);
        }
        return startWritingFuture;
    }

    private FieldMappings findFieldMapping(
            final List<FieldMappings> fieldMappings,
            final @NotNull ToEdgeMapping toEdgeMapping,
            final @NotNull String adapterId) {

        final Optional<FieldMappings> optionalFieldMappings = fieldMappings.stream()
                .filter(f -> f.getTagName().equals(toEdgeMapping.getTagName()) &&
                        f.getTopicFilter().equals(toEdgeMapping.getTopicFilter()))
                .findFirst();

        if (optionalFieldMappings.isEmpty()) {
            throw new IllegalStateException(
                    "No FieldMapping was found when trying to setup writing for protocol adapter: '" +
                            adapterId +
                            "'.");
        }

        return optionalFieldMappings.get();
    }

    private void schedulePolling(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        final ProtocolAdapter adapter = protocolAdapterWrapper.getAdapter();
        if (adapter instanceof PollingProtocolAdapter) {
            if (log.isDebugEnabled()) {
                log.debug("Schedule polling for protocol adapter with id '{}'", protocolAdapterWrapper.getId());
            }
            final List<FromEdgeMapping> pollingContexts = protocolAdapterWrapper.getFromEdgeMappings();
            pollingContexts.forEach(adapterSubscription -> {
                final PerSubscriptionSampler sampler = new PerSubscriptionSampler(protocolAdapterWrapper,
                        (PollingProtocolAdapter) adapter,
                        objectMapper,
                        moduleServices.adapterPublishService(),
                        adapterSubscription,
                        eventService,
                        jsonPayloadDefaultCreator);
                protocolAdapterPollingService.schedulePolling(sampler);
            });
        }
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);
        log.info("Stopping protocol-adapter '{}'.", protocolAdapterWrapper.getId());
        if (protocolAdapterWrapper.getAdapter() instanceof PollingProtocolAdapter) {
            if (log.isDebugEnabled()) {
                log.debug("Stopping polling for protocol adapter with id '{}'", protocolAdapterWrapper.getId());
            }
            protocolAdapterPollingService.stopPollingForAdapterInstance(protocolAdapterWrapper.getAdapter());
        }

        //no check for 'writing is enabled', as we have to stop it anyway since the license could have been removed in the meantime.
        final CompletableFuture<Void> stopWritingFuture;
        if (protocolAdapterWrapper.getAdapter() instanceof WritingProtocolAdapter) {
            if (log.isDebugEnabled()) {
                log.debug("Stopping writing for protocol adapter with id '{}'", protocolAdapterWrapper.getId());
            }
            stopWritingFuture =
                    protocolAdapterWritingService.stopWriting((WritingProtocolAdapter<WritingContext>) protocolAdapterWrapper.getAdapter());
        } else {
            stopWritingFuture = CompletableFuture.completedFuture(null);
        }

        return stopWritingFuture.thenComposeAsync(ignored -> {
            final ProtocolAdapterStopOutputImpl adapterStopOutput = new ProtocolAdapterStopOutputImpl();
            if (protocolAdapterWrapper.getRuntimeStatus() == ProtocolAdapterState.RuntimeStatus.STARTED) {
                protocolAdapterWrapper.stop(new ProtocolAdapterStopInputImpl(), adapterStopOutput);
                return adapterStopOutput.getOutputFuture();
            } else {
                return CompletableFuture.completedFuture(null);
            }

        }, executorService).<Void>thenApply(input -> {
            log.info("Protocol-adapter '{}' stopped successfully.", protocolAdapterWrapper.getId());
            protocolAdapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
            eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                            protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.INFO)
                    .withMessage(String.format("Adapter '%s' stopped OK.", protocolAdapterWrapper.getId()))
                    .fire();
            return null;
        }).exceptionally(throwable -> {
            if (log.isWarnEnabled()) {
                log.warn("Protocol-adapter '{}' was unable to stop cleanly", protocolAdapterWrapper.getId(), throwable);
            }
            eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                            protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.CRITICAL)
                    .withMessage("Error stopping adapter '" + protocolAdapterWrapper.getId() + "'.")
                    .fire();
            return null;
        });
    }

    private void startFailedStop(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        if (protocolAdapterWrapper.getAdapter() instanceof PollingProtocolAdapter) {
            protocolAdapterPollingService.stopPollingForAdapterInstance(protocolAdapterWrapper.getAdapter());
        }

        //no check for 'writing is enabled', as we have to stop it anyway since the license could have been removed in the meantime.
        final CompletableFuture<Void> stopWritingFuture;
        if (protocolAdapterWrapper.getAdapter() instanceof WritingProtocolAdapter) {
            stopWritingFuture =
                    protocolAdapterWritingService.stopWriting((WritingProtocolAdapter<WritingContext>) protocolAdapterWrapper.getAdapter());
        } else {
            stopWritingFuture = CompletableFuture.completedFuture(null);
        }

        stopWritingFuture.<Void>thenApply(input -> {
            if (log.isDebugEnabled()) {
                log.debug("Protocol-adapter forcefully '{}' stopped.", protocolAdapterWrapper.getId());
            }
            return null;
        });
    }

    protected void handleStartupError(
            final @NotNull ProtocolAdapter protocolAdapter, @NotNull final ProtocolAdapterStartOutputImpl output) {
        if (log.isWarnEnabled()) {
            log.warn("Protocol-adapter '{}' could not be started, reason: {}",
                    protocolAdapter.getId(),
                    output.getMessage(),
                    output.getThrowable());
        }
        eventService.createAdapterEvent(protocolAdapter.getId(),
                        protocolAdapter.getProtocolAdapterInformation().getProtocolId())
                .withSeverity(Event.SEVERITY.CRITICAL)
                .withMessage("Error starting adapter '" + protocolAdapter.getId() + "'.")
                .fire();

        final HiveMQEdgeRemoteEvent adapterCreatedEvent =
                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_ERROR);
        adapterCreatedEvent.addUserData("adapterType", protocolAdapter.getProtocolAdapterInformation().getProtocolId());
        remoteService.fireUsageEvent(adapterCreatedEvent);
    }


    public synchronized @NotNull CompletableFuture<Void> addAdapter(final @NotNull ProtocolAdapterConfig adapterConfig) {
        if (getAdapterTypeById(adapterConfig.getProtocolId()).isEmpty()) {
            throw new IllegalArgumentException("invalid adapter type '" + adapterConfig.getProtocolId() + "'");
        }
        if (getAdapterById(adapterConfig.getAdapterId()).isPresent()) {
            throw new IllegalArgumentException("adapter already exists by id '" + adapterConfig.getProtocolId() + "'");
        }
        protocolAdapterMetrics.increaseProtocolAdapterMetric(adapterConfig.getProtocolId());
        final CompletableFuture<Void> ret = addAdapterInternal(adapterConfig);

        configPersistence.addAdapter(adapterConfig);
        return ret;
    }

    public synchronized @NotNull CompletableFuture<Void> addAdapterWithoutTags(
            final @NotNull String adapterType,
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterType);
        Preconditions.checkNotNull(adapterId);
        Preconditions.checkNotNull(config);

        if (getAdapterTypeById(adapterType).isEmpty()) {
            throw new IllegalArgumentException("invalid adapter type '" + adapterType + "'");
        }
        if (getAdapterById(adapterId).isPresent()) {
            throw new IllegalArgumentException("adapter already exists by id '" + adapterId + "'");
        }
        protocolAdapterMetrics.increaseProtocolAdapterMetric(adapterType);
        final ProtocolSpecificAdapterConfig protocolSpecificAdapterConfig =
                configConverter.convertAdapterConfig(adapterType, config, writingEnabled());

        final List<FromEdgeMapping> fromEdgeMappings;
        if (protocolSpecificAdapterConfig instanceof AdapterConfigWithPollingContexts) {
            final AdapterConfigWithPollingContexts adapterConfigWithPollingContexts =
                    (AdapterConfigWithPollingContexts) protocolSpecificAdapterConfig;
            fromEdgeMappings = adapterConfigWithPollingContexts.getPollingContexts()
                    .stream()
                    .map(FromEdgeMapping::from)
                    .collect(Collectors.toList());
        } else {
            fromEdgeMappings = new ArrayList<>();
        }

        // we can not add toMappings as we do not have field mappings here yet.
        // actually this is correct from the user workflow
        final ProtocolAdapterConfig protocolAdapterConfig = new ProtocolAdapterConfig(adapterId,
                adapterType,
                protocolSpecificAdapterConfig,
                List.of(),
                fromEdgeMappings,
                List.of(),
                List.of());
        final CompletableFuture<Void> ret = addAdapterInternal(protocolAdapterConfig);
        configPersistence.addAdapter(protocolAdapterConfig);
        return ret;
    }

    public boolean deleteAdapter(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        final String protocolId = protocolAdapters.get(id).getProtocolAdapterInformation().getProtocolId();
        final boolean result = deleteAdapterInternal(id);
        configPersistence.deleteAdapter(id, protocolId);
        return result;
    }

    private boolean deleteAdapterInternal(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        final ProtocolAdapterWrapper adapterWrapper = protocolAdapters.remove(id);
        if (adapterWrapper != null) {
            protocolAdapterMetrics.decreaseProtocolAdapterMetric(adapterWrapper.getAdapterInformation()
                    .getProtocolId());
            try {
                // stop in any case as some resources must be cleaned up even if the adapter is still being started and is not yet in started state
                stop(adapterWrapper).get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (final ExecutionException e) {
                throw new RuntimeException(e);
            }

            try {
                //ensure the instance releases any hard state
                adapterWrapper.getAdapter().destroy();
                return true;
            } finally {
                final String adapterId = adapterWrapper.getId();
                eventService.createAdapterEvent(adapterId,
                                adapterWrapper.getProtocolAdapterInformation().getProtocolId())
                        .withSeverity(Event.SEVERITY.WARN)
                        .withMessage(String.format("Adapter '%s' was deleted from the system permanently.", adapterId))
                        .fire();

            }
        } else {
            log.error("Tried removing non existing adapter '{}'", id);
        }
        return false;
    }

    public boolean updateAdapterConfig(
            final @NotNull String adapterType,
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId).map(oldInstance -> {
            final ProtocolSpecificAdapterConfig protocolSpecificAdapterConfig =
                    configConverter.convertAdapterConfig(adapterType, config, writingEnabled());
            final List<FromEdgeMapping> fromEdgeMappings;
            if (protocolSpecificAdapterConfig instanceof AdapterConfigWithPollingContexts) {
                final AdapterConfigWithPollingContexts adapterConfigWithPollingContexts =
                        (AdapterConfigWithPollingContexts) protocolSpecificAdapterConfig;
                fromEdgeMappings = adapterConfigWithPollingContexts.getPollingContexts()
                        .stream()
                        .map(FromEdgeMapping::from)
                        .collect(Collectors.toList());
            } else {
                fromEdgeMappings = new ArrayList<>();
            }

            final List<ToEdgeMapping> toEdgeMappings;
            if (protocolSpecificAdapterConfig instanceof AdapterConfigWithWritingContexts) {
                final AdapterConfigWithWritingContexts adapterConfigWithPollingContexts =
                        (AdapterConfigWithWritingContexts) protocolSpecificAdapterConfig;
                toEdgeMappings = adapterConfigWithPollingContexts.getWritingContexts()
                        .stream()
                        .map(ToEdgeMapping::from)
                        .collect(Collectors.toList());
            } else {
                toEdgeMappings = new ArrayList<>();
            }

            final ProtocolAdapterConfig protocolAdapterConfig = new ProtocolAdapterConfig(adapterId,
                    adapterType,
                    protocolSpecificAdapterConfig,
                    toEdgeMappings,
                    fromEdgeMappings,
                    oldInstance.getTags(),
                    oldInstance.getFieldMappings());

            deleteAdapterInternal(adapterId);
            addAdapterInternal(protocolAdapterConfig);
            configPersistence.updateAdapter(protocolAdapterConfig);
            return true;
        }).orElse(false);
    }

    public boolean updateAdapterTags(final @NotNull String adapterId, final @NotNull List<Map<String, Object>> tags) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId).map(oldInstance -> {
            final String protocolId = oldInstance.getAdapterInformation().getProtocolId();
            final ProtocolAdapterConfig protocolAdapterConfig = new ProtocolAdapterConfig(oldInstance.getId(),
                    protocolId,
                    oldInstance.getConfigObject(),
                    oldInstance.getToEdgeMappings(),
                    oldInstance.getFromEdgeMappings(),
                    configConverter.mapsToTags(protocolId, tags),
                    oldInstance.getFieldMappings());
            deleteAdapterInternal(adapterId);
            addAdapterInternal(protocolAdapterConfig);
            configPersistence.updateAdapter(protocolAdapterConfig);
            return true;
        }).orElse(false);
    }

    public boolean updateAdapterFromMappings(final @NotNull String adapterId, final @NotNull List<FromEdgeMapping> fromEdgeMappings) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId).map(oldInstance -> {
            final String protocolId = oldInstance.getAdapterInformation().getProtocolId();
            final ProtocolAdapterConfig protocolAdapterConfig = new ProtocolAdapterConfig(oldInstance.getId(),
                    protocolId,
                    oldInstance.getConfigObject(),
                    oldInstance.getToEdgeMappings(),
                    fromEdgeMappings,
                    oldInstance.getTags(),
                    oldInstance.getFieldMappings());
            return protocolAdapterConfig.missingTags()
                    .map(missingTags -> {
                        log.error("Tags were missing; {}", missingTags);
                        return false;
                    })
                    .orElseGet(() -> {
                        deleteAdapterInternal(adapterId);
                        addAdapterInternal(protocolAdapterConfig);
                        configPersistence.updateAdapter(protocolAdapterConfig);
                        return true;
                    });
        }).orElse(false);
    }

    public boolean updateAdapterToMappings(final @NotNull String adapterId, final @NotNull List<ToEdgeMapping> toEdgeMappings) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId).map(oldInstance -> {
            final String protocolId = oldInstance.getAdapterInformation().getProtocolId();
            final ProtocolAdapterConfig protocolAdapterConfig = new ProtocolAdapterConfig(oldInstance.getId(),
                    protocolId,
                    oldInstance.getConfigObject(),
                    toEdgeMappings,
                    oldInstance.getFromEdgeMappings(),
                    oldInstance.getTags(),
                    oldInstance.getFieldMappings());
            return protocolAdapterConfig.missingTags()
                    .map(missingTags -> {
                        log.error("Tags were missing; {}", missingTags);
                        return false;
                    })
                    .orElseGet(() -> {
                        deleteAdapterInternal(adapterId);
                        addAdapterInternal(protocolAdapterConfig);
                        configPersistence.updateAdapter(protocolAdapterConfig);
                        return true;
                    });
        }).orElse(false);
    }

    public boolean updateAdapterFieldMappings(
            final @NotNull String adapterId, final @NotNull List<FieldMappings> fieldMappings) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId).map(oldInstance -> {
            final String protocolId = oldInstance.getProtocolAdapterInformation().getProtocolId();
            final ProtocolAdapterConfig protocolAdapterConfig = new ProtocolAdapterConfig(oldInstance.getId(),
                    protocolId,
                    oldInstance.getConfigObject(),
                    oldInstance.getToEdgeMappings(),
                    oldInstance.getFromEdgeMappings(),
                    oldInstance.getTags(),
                    fieldMappings);
            deleteAdapterInternal(adapterId);
            addAdapterInternal(protocolAdapterConfig);
            configPersistence.updateAdapter(protocolAdapterConfig);
            return true;
        }).orElse(false);
    }

    public @NotNull Optional<ProtocolAdapterWrapper> getAdapterById(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        final Map<String, ProtocolAdapterWrapper> adapters = getProtocolAdapters();
        return Optional.ofNullable(adapters.get(id));
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
        return protocolAdapters;
    }

    private @NotNull ProtocolAdapterWrapper createAdapterInstance(
            final @NotNull ProtocolAdapterFactory<?> protocolAdapterFactory,
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull String version) {

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(protocolAdapterFactory.getClass().getClassLoader());

            final ProtocolAdapterMetricsService protocolAdapterMetricsService = new ProtocolAdapterMetricsServiceImpl(
                    protocolAdapterFactory.getInformation().getProtocolId(),
                    config.getAdapterId(),
                    metricRegistry);


            final ProtocolAdapterStateImpl protocolAdapterState =
                    new ProtocolAdapterStateImpl(moduleServices.eventService(),
                            config.getAdapterId(),
                            protocolAdapterFactory.getInformation().getProtocolId());

            final ModuleServicesPerModuleImpl moduleServicesPerModule =
                    new ModuleServicesPerModuleImpl(moduleServices.adapterPublishService(),
                            eventService,
                            protocolAdapterWritingService);
            final ProtocolAdapter protocolAdapter =
                    protocolAdapterFactory.createAdapter(protocolAdapterFactory.getInformation(),
                            new ProtocolAdapterInputImpl(config.getAdapterId(),
                                    config.getAdapterConfig(),
                                    config.getTags(),
                                    version,
                                    protocolAdapterState,
                                    moduleServicesPerModule,
                                    protocolAdapterMetricsService));
            // hen-egg problem. Rather solve this here as have not final fields in the adapter.
            moduleServicesPerModule.setAdapter(protocolAdapter);

            final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(protocolAdapterMetricsService,
                    protocolAdapter,
                    protocolAdapterFactory,
                    protocolAdapterFactory.getInformation(),
                    protocolAdapterState,
                    config.getAdapterConfig(),
                    config.getTags(),
                    config.getToEdgeMappings(),
                    config.getFromEdgeMappings(),
                    config.getFieldMappings());
            protocolAdapters.put(wrapper.getId(), wrapper);
            return wrapper;

        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private @NotNull CompletableFuture<Void> addAdapterInternal(final @NotNull ProtocolAdapterConfig adapterConfig) {

        synchronized (lock) {
            final ProtocolAdapterFactory<?> protocolAdapterFactory =
                    protocolAdapterFactoryManager.get(adapterConfig.getProtocolId())
                            .orElseThrow(() -> new IllegalArgumentException("No factory found for adapter type " +
                                    adapterConfig.getProtocolId()));
            adapterConfig.missingTags().ifPresent(missing -> {
                throw new IllegalArgumentException("Tags used in mappings but used in adapter " +
                        adapterConfig.getAdapterId() +
                        ": " +
                        missing);
            });

            final ProtocolAdapterWrapper instance =
                    createAdapterInstance(protocolAdapterFactory, adapterConfig, versionProvider.getVersion());

            return start(instance);
        }
    }

    public boolean writingEnabled() {
        return protocolAdapterWritingService.writingEnabled();
    }


    public @NotNull DomainTagAddResult addDomainTag(
            final @NotNull String adapterId, final @NotNull DomainTag domainTag) {
        return getAdapterById(adapterId).map(adapter -> {
            final List<? extends Tag> tags = adapter.getTags();
            final boolean alreadyExists = tags.stream().anyMatch(t -> t.getName().equals(domainTag.getTagName()));
            if (!alreadyExists) {
                final List<Map<String, Object>> tagMaps =
                        tags.stream().map(configConverter::convertagTagsToMaps).collect(Collectors.toList());
                tagMaps.add(domainTag.toTagMap());
                updateAdapterTags(adapterId, tagMaps);
                return DomainTagAddResult.success();
            } else {
                return DomainTagAddResult.failed(ALREADY_EXISTS, adapterId);
            }
        }).orElse(DomainTagAddResult.failed(ADAPTER_MISSING, adapterId));
    }

    public @NotNull DomainTagUpdateResult updateDomainTag(final @NotNull DomainTag domainTag) {
        final String adapterId = domainTag.getAdapterId();
        return getAdapterById(adapterId).map(adapter -> {
            final List<? extends Tag> tags = adapter.getTags();
            final boolean alreadyExists = tags.removeIf(t -> t.getName().equals(domainTag.getTagName()));
            if (alreadyExists) {
                final List<Map<String, Object>> tagMaps =
                        tags.stream().map(configConverter::convertagTagsToMaps).collect(Collectors.toList());
                tagMaps.add(domainTag.toTagMap());
                updateAdapterTags(adapterId, tagMaps);
                return DomainTagUpdateResult.success();
            } else {
                return DomainTagUpdateResult.failed(TAG_NOT_FOUND, adapterId);
            }
        }).orElse(DomainTagUpdateResult.failed(ADAPTER_NOT_FOUND, adapterId));
    }

    public @NotNull DomainTagUpdateResult updateDomainTags(
            final @NotNull String adapterId, final @NotNull Set<DomainTag> domainTags) {
        return getAdapterById(adapterId).map(adapter -> {
            final List<Map<String, Object>> tagMaps =
                    domainTags.stream().map(DomainTag::toTagMap).collect(Collectors.toList());
            updateAdapterTags(adapterId, tagMaps);
            return DomainTagUpdateResult.success();
        }).orElse(DomainTagUpdateResult.failed(ADAPTER_NOT_FOUND, adapterId));
    }

    public @NotNull DomainTagDeleteResult deleteDomainTag(
            final @NotNull String adapterId, final @NotNull String tagName) {
        return getAdapterById(adapterId).map(adapter -> {
            final List<? extends Tag> tags = adapter.getTags();
            final boolean exists = tags.removeIf(t -> t.getName().equals(tagName));
            if (exists) {
                final List<Map<String, Object>> tagMaps =
                        tags.stream().map(configConverter::convertagTagsToMaps).collect(Collectors.toList());
                updateAdapterTags(adapterId, tagMaps);
                return DomainTagDeleteResult.success();
            } else {
                return DomainTagDeleteResult.failed(NOT_FOUND, adapterId);
            }
        }).orElse(DomainTagDeleteResult.failed(NOT_FOUND, adapterId));
    }

    public @NotNull List<DomainTag> getDomainTags() {
        return getProtocolAdapters().values()
                .stream()
                .flatMap(adapter -> adapter.getTags()
                        .stream()
                        .map(tag -> new DomainTag(tag.getName(),
                                adapter.getId(),
                                adapter.getProtocolAdapterInformation().getProtocolId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToMaps(tag.getDefinition()))))
                .collect(Collectors.toList());
    }

    public @NotNull Optional<DomainTag> getDomainTagByName(final @NotNull String tagName) {
        return getProtocolAdapters().values()
                .stream()
                .flatMap(adapter -> adapter.getTags()
                        .stream()
                        .filter(t -> t.getName().equals(tagName))
                        .map(tag -> new DomainTag(tag.getName(),
                                adapter.getId(),
                                adapter.getProtocolAdapterInformation().getProtocolId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToMaps(tag.getDefinition()))))
                .findFirst();
    }

    public @NotNull Optional<List<DomainTag>> getTagsForAdapter(final @NotNull String adapterId) {
        return getAdapterById(adapterId).map(adapter -> adapter.getTags()
                .stream()
                .map(tag -> new DomainTag(tag.getName(),
                        adapter.getId(),
                        adapter.getProtocolAdapterInformation().getProtocolId(),
                        tag.getDescription(),
                        configConverter.convertTagDefinitionToMaps(tag.getDefinition())))
                .collect(Collectors.toList()));
    }

    public @NotNull DomainTagAddResult addFieldMappings(
            final @NotNull String adapterId, final @NotNull FieldMappings fieldMappings) {
        return getAdapterById(adapterId).map(adapter -> {
            final @NotNull List<FieldMappings> presentMappings = adapter.getFieldMappings();
            final boolean alreadyExists =
                    presentMappings.stream().anyMatch(t -> t.getTopicFilter().equals(fieldMappings.getTopicFilter()));
            if (!alreadyExists) {
                final ArrayList<FieldMappings> newFieldMappings = new ArrayList<>(presentMappings);
                newFieldMappings.add(fieldMappings);
                updateAdapterFieldMappings(adapterId, newFieldMappings);
                return DomainTagAddResult.success();
            } else {
                return DomainTagAddResult.failed(ALREADY_EXISTS, adapterId);
            }
        }).orElse(DomainTagAddResult.failed(ADAPTER_MISSING, adapterId));
    }


    public @NotNull Optional<List<FieldMappings>> getFieldMappingsForAdapter(final @NotNull String adapterId) {
        final Optional<ProtocolAdapterWrapper> optionalProtocolAdapterWrapper = getAdapterById(adapterId);
        if (optionalProtocolAdapterWrapper.isEmpty()) {
            return Optional.empty();
        }
        final ProtocolAdapterWrapper protocolAdapterWrapper = optionalProtocolAdapterWrapper.get();

        return Optional.of(protocolAdapterWrapper.getFieldMappings());
    }


    public @NotNull List<FieldMappings> getFieldMappings() {
        return getProtocolAdapters().values()
                .stream()
                .flatMap(adapter -> adapter.getFieldMappings().stream())
                .collect(Collectors.toList());
    }


    public static class ProtocolAdapterInputImpl<T extends ProtocolSpecificAdapterConfig>
            implements ProtocolAdapterInput<T> {
        public static final AdapterFactoriesImpl ADAPTER_FACTORIES = new AdapterFactoriesImpl();
        private final String adapterId;
        private final @NotNull T configObject;
        private final @NotNull String version;
        private final @NotNull ProtocolAdapterState protocolAdapterState;
        private final @NotNull ModuleServices moduleServices;
        private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
        private final @NotNull List<Tag> tags;

        public ProtocolAdapterInputImpl(
                final @NotNull String adapterId,
                final @NotNull T configObject,
                final @NotNull List<Tag> tags,
                final @NotNull String version,
                final @NotNull ProtocolAdapterState protocolAdapterState,
                final @NotNull ModuleServices moduleServices,
                final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
            this.adapterId = adapterId;
            this.configObject = configObject;
            this.version = version;
            this.protocolAdapterState = protocolAdapterState;
            this.moduleServices = moduleServices;
            this.protocolAdapterMetricsService = protocolAdapterMetricsService;
            this.tags = tags;
        }

        @Override
        public @NotNull String getAdapterId() {
            return adapterId;
        }

        @Override
        public @NotNull T getConfig() {
            return configObject;
        }

        @Override
        public @NotNull String getVersion() {
            return version;
        }

        @Override
        public @NotNull ProtocolAdapterState getProtocolAdapterState() {
            return protocolAdapterState;
        }

        @Override
        public @NotNull ModuleServices moduleServices() {
            return moduleServices;
        }

        @Override
        public @NotNull AdapterFactories adapterFactories() {
            return ADAPTER_FACTORIES;
        }

        @Override
        public @NotNull ProtocolAdapterMetricsService getProtocolAdapterMetricsHelper() {
            return protocolAdapterMetricsService;
        }

        @Override
        public @NotNull List<Tag> getTags() {
            return tags;
        }
    }
}
