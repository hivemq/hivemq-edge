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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
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
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

@Singleton
public class ProtocolAdapterManager {
    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterManager.class);

    private final @NotNull Map<String, ProtocolAdapterWrapper<? extends ProtocolAdapter>> protocolAdapters =
            new ConcurrentHashMap<>();
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ModuleServicesImpl moduleServices;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull EventService eventService;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics;
    private final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator;
    private final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService;
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
            final @NotNull VersionProvider versionProvider,
            final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService,
            final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator,
            final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService,
            final @NotNull ExecutorService executorService) {
        this.metricRegistry = metricRegistry;
        this.moduleServices = moduleServices;
        this.configPersistence = new ConfigPersistence(configurationService);
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.remoteService = remoteService;
        this.eventService = eventService;
        this.versionProvider = versionProvider;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.protocolAdapterMetrics = protocolAdapterMetrics;
        this.jsonPayloadDefaultCreator = jsonPayloadDefaultCreator;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.executorService = executorService;
        protocolAdapterFactoryManager = new ProtocolAdapterFactoryManager(moduleLoader, eventService, protocolAdapterWritingService.writingEnabled());
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NotNull ListenableFuture<Void> start() {

        log.info("Starting adapters");
        protocolAdapterWritingService.addWritingChangedCallback(() -> {
            protocolAdapterFactoryManager.writingEnabledChanged(protocolAdapterWritingService.writingEnabled());
        });

        final ImmutableList.Builder<CompletableFuture<Void>> adapterFutures = ImmutableList.builder();

        for (final Map.Entry<String, Object> configSection : configPersistence.allAdapters().entrySet()) {
            final String adapterType = getKey(configSection.getKey());

            final Optional<ProtocolAdapterFactory<?>> protocolAdapterFactoryOptional = protocolAdapterFactoryManager
                    .get(adapterType); //FIXME should be used as actual optional

            if (protocolAdapterFactoryOptional.isEmpty()) {
                return Futures.immediateFailedFuture(new IllegalArgumentException("Protocol adapter for config " + adapterType + " not found."));
            }
            final ProtocolAdapterFactory<?> protocolAdapterFactory = protocolAdapterFactoryManager
                    .get(adapterType).get();
            final Object adapterXmlElement = configSection.getValue();
            final List<Map<String, Object>> adapterConfigs;
            if (adapterXmlElement instanceof List) {
                adapterConfigs = (List<Map<String, Object>>) adapterXmlElement;
            } else if (adapterXmlElement instanceof Map) {
                adapterConfigs = List.of((Map<String, Object>) adapterXmlElement);
            } else {
                return Futures.immediateFailedFuture(new IllegalArgumentException("Found invalid configuration element for adapter " + adapterType));
            }


            adapterConfigs.stream()
                    .map(persistenceMap -> AdapterConfigAndTags.fromAdapterConfigMap(persistenceMap,
                            writingEnabled(),
                            objectMapper,
                            protocolAdapterFactory))
                    .forEach(persistence -> {
                        log.info("Found configuration for adapter {} / {}", persistence.getAdapterConfig().getId(), adapterType);
                        persistence
                                .missingTags()
                                .ifPresent(missing -> {
                                    throw new IllegalArgumentException("Tags used in mappings but not configured in adapter " + persistence.getAdapterConfig().getId() +": "+ missing);
                                });

                        final ProtocolAdapterWrapper instance =
                                createAdapterInstance(protocolAdapterFactory, persistence, versionProvider.getVersion());
                        protocolAdapterMetrics.increaseProtocolAdapterMetric(instance.getAdapter()
                                .getProtocolAdapterInformation()
                                .getProtocolId());
                        final CompletableFuture<Void> future = start(instance);
                        adapterFutures.add(future);
                    });
        }

        configPersistence.updateAllAdapters(rewriteAdapterConfigurations(protocolAdapters.values()));

        return FutureConverter.toListenableFuture(CompletableFuture.allOf(adapterFutures.build()
                .toArray(new CompletableFuture[]{})));
    }

    private @NotNull Map<String, Object> rewriteAdapterConfigurations(Collection<? extends ProtocolAdapterWrapper> protocolAdapterWrappers) {
        final Map<String, Object> allAdapterConfigs = new HashMap<>();
        for (final ProtocolAdapterWrapper value : protocolAdapterWrappers) {
            AdapterConfigAndTags adapterConfigAndTags = new AdapterConfigAndTags(value.getConfigObject(), value.getTags());
            final ProtocolAdapterFactory<?> adapterFactory = value.getAdapterFactory();
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(adapterFactory.getClass().getClassLoader());
                allAdapterConfigs.compute(value.getAdapter().getProtocolAdapterInformation().getProtocolId(),
                        (s, o) -> {
                            List<Map<String, Object>> tagMaps = adapterConfigAndTags.getTags().stream()
                                    .map(tag -> objectMapper.convertValue(tag, new TypeReference<Map<String, Object>>() {}))
                                    .collect(Collectors.toList());
                            final Map<String, Object> configMap = adapterFactory.unconvertConfigObject(
                                    objectMapper,
                                    adapterConfigAndTags.getAdapterConfig());

                            Map<String, Object> combined = Map.of("config", configMap, "tags", tagMaps);
                            if (o == null) {
                                final List<Map<String, Object>> list = new ArrayList<>();
                                list.add(combined);
                                return list;
                            }
                            //noinspection unchecked
                            ((List<Map<String, Object>>) o).add(combined);
                            return o;
                        });
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }

        return allAdapterConfigs;
    }

    //legacy handling, hardcoded here, to not add legacy stuff into the adapter-sdk
    private static @NotNull String getKey(final @NotNull String key) {
        if (key.equals("ethernet-ip")) {
            return "eip";
        }
        if (key.equals("opc-ua-client")) {
            return "opcua";
        }
        if (key.equals("file_input")) {
            return "file";
        }
        return key;
    }

    public @NotNull boolean protocolAdapterFactoryExists(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return protocolAdapterFactoryManager.get(protocolAdapterType).isPresent();
    }

    public @NotNull CompletableFuture<Void> start(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        final Optional<ProtocolAdapterWrapper<? extends ProtocolAdapter>> adapterOptional =
                getAdapterById(protocolAdapterId);
        return adapterOptional.map(this::start)
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    @VisibleForTesting
    @NotNull
    CompletableFuture<Void> start(final @NotNull ProtocolAdapterWrapper<?> protocolAdapterWrapper) {
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
                //TODO: discuss if this is possible.
                startFailedStop(protocolAdapterWrapper);
            }
            return null;
        });
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        final Optional<ProtocolAdapterWrapper<?>> adapterOptional = getAdapterById(protocolAdapterId);
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
            startWritingFuture =
                    protocolAdapterWritingService.startWriting((WritingProtocolAdapter<WritingContext>) protocolAdapterWrapper.getAdapter(),
                            protocolAdapterWrapper.getProtocolAdapterMetricsService());
        } else {
            startWritingFuture = CompletableFuture.completedFuture(null);
        }
        return startWritingFuture;
    }

    private void schedulePolling(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        if (protocolAdapterWrapper.getAdapter() instanceof PollingProtocolAdapter) {
            if (log.isDebugEnabled()) {
                log.debug("Schedule polling for protocol adapter with id '{}'", protocolAdapterWrapper.getId());
            }
            final PollingProtocolAdapter<PollingContext> adapter =
                    (PollingProtocolAdapter<PollingContext>) protocolAdapterWrapper.getAdapter();
            adapter.getPollingContexts().forEach(adapterSubscription -> {
                //noinspection unchecked this is safe as we literally make a check on the adapter class before
                final PerSubscriptionSampler<PollingContext> sampler = new PerSubscriptionSampler<PollingContext>(
                        protocolAdapterWrapper,
                        objectMapper,
                        moduleServices.adapterPublishService(),
                        adapterSubscription,
                        eventService,
                        jsonPayloadDefaultCreator);
                protocolAdapterPollingService.schedulePolling(sampler);
            });
        }
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull ProtocolAdapterWrapper<?> protocolAdapterWrapper) {
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

    private void startFailedStop(final @NotNull ProtocolAdapterWrapper<?> protocolAdapterWrapper) {
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

    public synchronized @NotNull CompletableFuture<Void> addAdapter(
            final @NotNull String adapterType,
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> config,
            final @NotNull List<Map<String, Object>> tagMaps) {
        Preconditions.checkNotNull(adapterType);
        Preconditions.checkNotNull(adapterId);
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(tagMaps);

        if (getAdapterTypeById(adapterType).isEmpty()) {
            throw new IllegalArgumentException("invalid adapter type '" + adapterType + "'");
        }
        if (getAdapterById(adapterId).isPresent()) {
            throw new IllegalArgumentException("adapter already exists by id '" + adapterId + "'");
        }
        protocolAdapterMetrics.increaseProtocolAdapterMetric(adapterType);
        final CompletableFuture<Void> ret = addAdapterInternal(adapterType, config, tagMaps);
        configPersistence.addAdapter(adapterType, config, tagMaps);
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
        final CompletableFuture<Void> ret = addAdapterInternal(adapterType, config, List.of());
        configPersistence.addAdapter(adapterType, config, List.of());
        return ret;
    }

    public boolean deleteAdapter(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        String protocolId = protocolAdapters.get(id).getProtocolAdapterInformation().getProtocolId();
        boolean result = deleteAdapterInternal(id);
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

    public boolean updateAdapterConfigAndTags(final @NotNull String adapterId, final @NotNull Map<String, Object> config, final @NotNull List<Map<String, Object>> tagMaps) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId)
                .map(oldInstance -> {
                    final String protocolId = oldInstance.getProtocolAdapterInformation().getProtocolId();
                    deleteAdapterInternal(adapterId);
                    addAdapterInternal(protocolId, config, tagMaps);
                    configPersistence.updateAdapter(adapterId, config, tagMaps);
                    return true;
                })
                .orElse(false);
    }

    public boolean updateAdapterConfig(final @NotNull String adapterId, final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId)
                .map(oldInstance -> {
                    final String protocolId = oldInstance.getProtocolAdapterInformation().getProtocolId();
                    List<Map<String, Object>> tags = oldInstance.getTags().stream()
                            .map(tag -> objectMapper.convertValue(tag, new TypeReference<Map<String, Object>>() {}))
                            .collect(Collectors.toList());
                    deleteAdapterInternal(adapterId);
                    addAdapterInternal(protocolId, config, tags);
                    configPersistence.updateAdapter(adapterId, config, tags);
                    return true;
                })
                .orElse(false);
    }

    public boolean updateAdapterTags(final @NotNull String adapterId, final @NotNull List<Map<String, Object>> tags) {
        Preconditions.checkNotNull(adapterId);
        return getAdapterById(adapterId)
                .map(oldInstance -> {
                    final String protocolId = oldInstance.getProtocolAdapterInformation().getProtocolId();
                    final Map<String, Object> config =
                            objectMapper.convertValue(oldInstance, new TypeReference<>() {});
                    deleteAdapterInternal(adapterId);
                    addAdapterInternal(protocolId, config, tags);
                    configPersistence.updateAdapter(adapterId, config, tags);
                    return true;
                })
                .orElse(false);
    }

    public @NotNull Optional<ProtocolAdapterWrapper<? extends ProtocolAdapter>> getAdapterById(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        final Map<String, ProtocolAdapterWrapper<? extends ProtocolAdapter>> adapters = getProtocolAdapters();
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


    public @NotNull Map<String, ProtocolAdapterWrapper<? extends ProtocolAdapter>> getProtocolAdapters() {
        return protocolAdapters;
    }

    private @NotNull ProtocolAdapterWrapper createAdapterInstance(
            final @NotNull ProtocolAdapterFactory<?> protocolAdapterFactory,
            final @NotNull AdapterConfigAndTags config,
            final @NotNull String version) {

        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(protocolAdapterFactory.getClass().getClassLoader());

            final ProtocolAdapterMetricsService protocolAdapterMetricsService = new ProtocolAdapterMetricsServiceImpl(
                    protocolAdapterFactory.getInformation().getProtocolId(),
                    config.getAdapterConfig().getId(),
                    metricRegistry);


            final ProtocolAdapterStateImpl protocolAdapterState =
                    new ProtocolAdapterStateImpl(moduleServices.eventService(),
                            config.getAdapterConfig().getId(),
                            protocolAdapterFactory.getInformation().getProtocolId());

            final ModuleServicesPerModuleImpl moduleServicesPerModule =
                    new ModuleServicesPerModuleImpl(moduleServices.adapterPublishService(),
                            eventService,
                            protocolAdapterWritingService);
            final ProtocolAdapter protocolAdapter =
                    protocolAdapterFactory.createAdapter(protocolAdapterFactory.getInformation(),
                            new ProtocolAdapterInputImpl(
                                    config.getAdapterConfig(),
                                    config.getTags(),
                                    version,
                                    protocolAdapterState,
                                    moduleServicesPerModule,
                                    protocolAdapterMetricsService));
            // hen-egg problem. Rather solve this here as have not final fields in the adapter.
            moduleServicesPerModule.setAdapter(protocolAdapter);

            final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(
                    protocolAdapterMetricsService,
                    protocolAdapter,
                    protocolAdapterFactory,
                    protocolAdapterFactory.getInformation(),
                    protocolAdapterState,
                    config.getAdapterConfig(),
                    config.getTags());
            protocolAdapters.put(wrapper.getId(), wrapper);
            return wrapper;

        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private @NotNull CompletableFuture<Void> addAdapterInternal(
            final @NotNull String adapterType, final @NotNull Map<String, Object> config, final @NotNull List<Map<String, Object>> tagMaps) {

        synchronized (lock) {

            final ProtocolAdapterFactory<?> protocolAdapterFactory = protocolAdapterFactoryManager
                    .get(adapterType)
                    .orElseThrow(() -> new IllegalArgumentException("No factory found for adapter type " + adapterType));

            final AdapterConfigAndTags persistence =
                    AdapterConfigAndTags.fromAdapterConfigMap(
                            Map.of("config", config, "tags", tagMaps),
                            writingEnabled(),
                            objectMapper,
                            protocolAdapterFactory);

            persistence
                    .missingTags()
                    .ifPresent(missing -> {
                        throw new IllegalArgumentException("Tags used in mappings but used in adapter " + persistence.getAdapterConfig().getId() +": "+ missing);
                    });

            final ProtocolAdapterWrapper instance =
                    createAdapterInstance(protocolAdapterFactory, persistence, versionProvider.getVersion());

            return start(instance);
        }
    }

    public boolean writingEnabled() {
        return protocolAdapterWritingService.writingEnabled();
    }


    public @NotNull DomainTagAddResult addDomainTag(final @NotNull String adapterId, final @NotNull DomainTag domainTag) {
        return getAdapterById(adapterId)
                .map(adapter -> {
                    final List<Tag> tags = adapter.getTags();
                    final boolean alreadyExists = tags.stream().anyMatch(t -> t.getName().equals(domainTag.getTagName()));
                    if(!alreadyExists) {
                        final List<Map<String, Object>> tagMaps = tags.stream()
                                .map(t -> objectMapper.convertValue(t, new TypeReference<Map<String, Object>>() {
                                }))
                                .collect(Collectors.toList());
                        tagMaps.add(domainTag.toTagMap());
                        updateAdapterTags(adapterId, tagMaps);
                        return DomainTagAddResult.success();
                    } else {
                        return DomainTagAddResult.failed(ALREADY_EXISTS, adapterId);
                    }
                }).orElse(DomainTagAddResult.failed(ADAPTER_MISSING, adapterId));
    }

    public @NotNull DomainTagUpdateResult updateDomainTag(final @NotNull DomainTag domainTag) {
        String adapterId = domainTag.getAdapterId();
        return getAdapterById(adapterId)
                .map(adapter -> {
                    final List<Tag> tags = adapter.getTags();
                    final boolean alreadyExists = tags.removeIf(t -> t.getName().equals(domainTag.getTagName()));
                    if(alreadyExists) {
                        final List<Map<String, Object>> tagMaps = tags.stream()
                                .map(t -> objectMapper.convertValue(t, new TypeReference<Map<String, Object>>() {
                                }))
                                .collect(Collectors.toList());
                        tagMaps.add(domainTag.toTagMap());
                        updateAdapterTags(adapterId, tagMaps);
                        return DomainTagUpdateResult.success();
                    } else {
                        return DomainTagUpdateResult.failed(TAG_NOT_FOUND, adapterId);
                    }
                }).orElse(DomainTagUpdateResult.failed(ADAPTER_NOT_FOUND, adapterId));
    }

    public DomainTagUpdateResult updateDomainTags(final String adapterId, final Set<DomainTag> domainTags) {
        Set<String> tagNames = domainTags.stream().map(t -> t.getTagName()).collect(Collectors.toSet());
        return getAdapterById(adapterId)
                .map(adapter -> {
                    final List<Tag> tags = adapter.getTags();
                    final boolean alreadyExists = tags.removeIf(t -> tagNames.contains(t.getName()));
                    if(alreadyExists) {
                        final List<Map<String, Object>> tagMaps = tags.stream()
                                .map(t -> objectMapper.convertValue(t, new TypeReference<Map<String, Object>>() {
                                }))
                                .collect(Collectors.toList());
                        domainTags.forEach(dt -> tagMaps.add(dt.toTagMap()));
                        updateAdapterTags(adapterId, tagMaps);
                        return DomainTagUpdateResult.success();
                    } else {
                        return DomainTagUpdateResult.failed(TAG_NOT_FOUND, adapterId);
                    }
                }).orElse(DomainTagUpdateResult.failed(ADAPTER_NOT_FOUND, adapterId));
    }

    public DomainTagDeleteResult deleteDomainTag(final String adapterId, final String tagName) {
        return getAdapterById(adapterId)
                .map(adapter -> {
                    final List<Tag> tags = adapter.getTags();
                    final boolean exists = tags.removeIf(t -> t.getName().equals(tagName));
                    if(exists) {
                        final List<Map<String, Object>> tagMaps = tags.stream()
                                .map(t -> objectMapper.convertValue(t, new TypeReference<Map<String, Object>>() {}))
                                .collect(Collectors.toList());
                        updateAdapterTags(adapterId, tagMaps);
                        return DomainTagDeleteResult.success();
                    } else {
                        return DomainTagDeleteResult.failed(NOT_FOUND, adapterId);
                    }
                }).orElse(DomainTagDeleteResult.failed(NOT_FOUND, adapterId));
    }

    public List<DomainTag> getDomainTags() {
        return getProtocolAdapters().values().stream()
                .flatMap(adapter ->
                        adapter.getTags().stream().map(tag -> new DomainTag(
                                tag.getName(),
                                adapter.getId(),
                                adapter.getProtocolAdapterInformation().getProtocolId(),
                                tag.getDescription(),
                                objectMapper.convertValue(tag.getDefinition(), new TypeReference<>() {}))))
                .collect(Collectors.toList());
    }

    public Optional<List<DomainTag>> getTagsForAdapter(final String adapterId) {
        return getAdapterById(adapterId)
                .map(adapter ->
                        adapter.getTags().stream().map(tag -> new DomainTag(
                                tag.getName(),
                                adapter.getId(),
                                adapter.getProtocolAdapterInformation().getProtocolId(),
                                tag.getDescription(),
                                objectMapper.convertValue(tag.getDefinition(), new TypeReference<>() {})))
                                .collect(Collectors.toList()));
    }

    public Optional<DomainTag> getTag(final String tagName) {
        return getProtocolAdapters().values().stream()
                .map(adapter -> adapter.getTags()
                                        .stream()
                                        .filter(t -> t.getName().equals(tagName))
                                        .map(tag -> new DomainTag(tag.getName(),
                                                adapter.getId(),
                                                adapter.getProtocolAdapterInformation().getProtocolId(),
                                                tag.getDescription(),
                                                objectMapper.convertValue(tag.getDefinition(), new TypeReference<>() {
                                                })))
                                        .findFirst()
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    public static class ProtocolAdapterInputImpl<T extends ProtocolAdapterConfig> implements ProtocolAdapterInput<T> {
        public static final AdapterFactoriesImpl ADAPTER_FACTORIES = new AdapterFactoriesImpl();
        private final @NotNull T configObject;
        private final @NotNull String version;
        private final @NotNull ProtocolAdapterState protocolAdapterState;
        private final @NotNull ModuleServices moduleServices;
        private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
        private final @NotNull List<Tag> tags;

        public ProtocolAdapterInputImpl(
                final @NotNull T configObject,
                final @NotNull List<Tag> tags,
                final @NotNull String version,
                final @NotNull ProtocolAdapterState protocolAdapterState,
                final @NotNull ModuleServices moduleServices,
                final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
            this.configObject = configObject;
            this.version = version;
            this.protocolAdapterState = protocolAdapterState;
            this.moduleServices = moduleServices;
            this.protocolAdapterMetricsService = protocolAdapterMetricsService;
            this.tags = tags;
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
