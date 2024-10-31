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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.exceptions.ProtocolAdapterException;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
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
import com.hivemq.edge.modules.adapters.simulation.SimulationProtocolAdapterFactory;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.persistence.domain.DomainTag;
import com.hivemq.persistence.domain.DomainTagAddResult;
import com.hivemq.persistence.domain.DomainTagPersistence;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Singleton
public class ProtocolAdapterManager {
    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterManager.class);
    private final @NotNull Map<String, ProtocolAdapterFactory<?>> factoryMap = new ConcurrentHashMap<>();
    private final @NotNull Map<String, ProtocolAdapterWrapper<? extends ProtocolAdapter>> protocolAdapters =
            new ConcurrentHashMap<>();
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ModuleServicesImpl moduleServices;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull EventService eventService;
    private final @NotNull VersionProvider versionProvider;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics;
    private final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator;
    private final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService;
    private final @NotNull ExecutorService executorService;
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService;
    private final @NotNull DomainTagPersistence domainTagPersistence;

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
            final @NotNull ExecutorService executorService,
            final @NotNull ProtocolAdapterTagService protocolAdapterTagService,
            final @NotNull DomainTagPersistence domainTagPersistence) {
        this.configurationService = configurationService;
        this.metricRegistry = metricRegistry;
        this.moduleServices = moduleServices;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.moduleLoader = moduleLoader;
        this.remoteService = remoteService;
        this.eventService = eventService;
        this.versionProvider = versionProvider;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.protocolAdapterMetrics = protocolAdapterMetrics;
        this.jsonPayloadDefaultCreator = jsonPayloadDefaultCreator;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
        this.executorService = executorService;
        this.protocolAdapterTagService = protocolAdapterTagService;
        this.domainTagPersistence = domainTagPersistence;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NotNull ListenableFuture<Void> start() {

        protocolAdapterWritingService.addWritingChangedCallback(this::findAllAdapters);
        findAllAdapters();

        //iterate configs and start each adapter
        final Map<String, Object> allConfigs =
                configurationService.protocolAdapterConfigurationService().getAllConfigs();

        if (allConfigs.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        final ImmutableList.Builder<CompletableFuture<Void>> adapterFutures = ImmutableList.builder();

        for (final Map.Entry<String, Object> configSection : allConfigs.entrySet()) {
            final String adapterType = getKey(configSection.getKey());
            final ProtocolAdapterFactory<?> protocolAdapterFactory = getProtocolAdapterFactory(adapterType);
            if (protocolAdapterFactory == null) {
                if (log.isWarnEnabled()) {
                    log.warn("Protocol adapter for config {} not found.", adapterType);
                }
                continue;
            }
            final Object adapterXmlElement = configSection.getValue();
            final List<Map<String, Object>> adapterConfigs;
            if (adapterXmlElement instanceof List) {
                adapterConfigs = (List<Map<String, Object>>) adapterXmlElement;
            } else if (adapterXmlElement instanceof Map) {
                adapterConfigs = List.of((Map<String, Object>) adapterXmlElement);
            } else {
                //unknown data structure - continue (bad config)
                if (log.isWarnEnabled()) {
                    log.warn("Found invalid configuration element for adapter {}, skipping.", adapterType);
                }
                continue;
            }

            for (final Map<String, Object> adapterConfig : adapterConfigs) {
                final ProtocolAdapterWrapper instance =
                        createAdapterInstance(adapterType, adapterConfig, versionProvider.getVersion());
                protocolAdapterMetrics.increaseProtocolAdapterMetric(instance.getAdapter()
                        .getProtocolAdapterInformation()
                        .getProtocolId());
                final CompletableFuture<Void> future = start(instance);
                adapterFutures.add(future);
            }
        }

        rewriteAdapterConfigurations();

        return FutureConverter.toListenableFuture(CompletableFuture.allOf(adapterFutures.build()
                .toArray(new CompletableFuture[]{})));
    }

    private void rewriteAdapterConfigurations() {
        final Map<String, Object> allAdapterConfigs = new HashMap<>();
        for (final ProtocolAdapterWrapper value : protocolAdapters.values()) {
            final ProtocolAdapterConfig configObject = value.getConfigObject();
            final ProtocolAdapterFactory<?> adapterFactory = value.getAdapterFactory();
            final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(adapterFactory.getClass().getClassLoader());
                allAdapterConfigs.compute(value.getAdapter().getProtocolAdapterInformation().getProtocolId(),
                        (s, o) -> {
                            if (o == null) {
                                final List<Map<String, Object>> list = new ArrayList<>();
                                list.add(adapterFactory.unconvertConfigObject(objectMapper, configObject));
                                return list;
                            }
                            //noinspection unchecked
                            ((List<Map<String, Object>>) o).add(adapterFactory.unconvertConfigObject(objectMapper,
                                    configObject));
                            return o;
                        });
            } finally {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
        synchronized (lock) {
            configurationService.protocolAdapterConfigurationService().setAllConfigs(allAdapterConfigs);
        }
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

    @SuppressWarnings("rawtypes")
    private void findAllAdapters() {
        final List<Class<? extends ProtocolAdapterFactory>> implementations =
                moduleLoader.findImplementations(ProtocolAdapterFactory.class);

        implementations.add(SimulationProtocolAdapterFactory.class);

        for (final Class<? extends ProtocolAdapterFactory> factoryClass : implementations) {
            try {
                final ProtocolAdapterFactory<?> protocolAdapterFactory = findConstructorAndInitialize(factoryClass);
                if (log.isDebugEnabled()) {
                    log.debug("Discovered protocol adapter implementation {}.", factoryClass.getName());
                }
                final ProtocolAdapterInformation information = protocolAdapterFactory.getInformation();
                factoryMap.put(information.getProtocolId(), protocolAdapterFactory);
            } catch (final InvocationTargetException | InstantiationException | IllegalAccessException |
                           NoSuchMethodException e) {
                log.error("Not able to load module, reason: {}.", e.getMessage());
            }
        }

        log.info("Discovered {} protocol adapter-type(s): [{}].",
                factoryMap.size(),
                factoryMap.values()
                        .stream()
                        .map(protocolAdapterFactory -> "'" +
                                protocolAdapterFactory.getInformation().getProtocolName() +
                                "'")
                        .collect(Collectors.joining(", ")));
    }

    private ProtocolAdapterFactory<?> findConstructorAndInitialize(final @NotNull Class<? extends ProtocolAdapterFactory> factoryClass)
            throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Constructor<?>[] declaredConstructors = factoryClass.getDeclaredConstructors();
        // check all possible constructors to enable backwards compatibility
        for (final Constructor<?> declaredConstructor : declaredConstructors) {
            final Parameter[] parameters = declaredConstructor.getParameters();
            // likely custom protocol adapter implementations still have the old default no-arg constructor.
            if (parameters.length == 0) {
                return factoryClass.getDeclaredConstructor().newInstance();
            }

            // this should not be out in the wild, but this was the constructor format after adding bi-directional adapters
            if (parameters.length == 1 && parameters[0].getType().equals(boolean.class)) {
                return factoryClass.getDeclaredConstructor(boolean.class, ProtocolAdapterTagService.class)
                        .newInstance(writingEnabled(), protocolAdapterTagService);
            }

            // current format: ProtocolAdapterFactoryInput expandable interface that will be backwards co patible if methods get added.
            if (parameters.length == 1 && parameters[0].getType().equals(ProtocolAdapterFactoryInput.class)) {
                final ProtocolAdapterFactoryInput protocolAdapterFactoryInput =
                        new ProtocolAdapterFactoryInputImpl(writingEnabled(), protocolAdapterTagService, eventService);
                return factoryClass.getDeclaredConstructor(ProtocolAdapterFactoryInput.class)
                        .newInstance(protocolAdapterFactoryInput);
            }

            log.warn("No fitting constructor was found to initialize adapter factory class '{}'.", factoryClass);
        }
        throw new IllegalAccessException();
    }

    public @Nullable ProtocolAdapterFactory<?> getProtocolAdapterFactory(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return factoryMap.get(protocolAdapterType);
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
    CompletableFuture<Void> start(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);

        Map<String, DomainTagAddResult> duplicatedTags = new HashMap<>();
        protocolAdapterWrapper.getConfigObject().getTags().stream()
                .map(tag ->
                    new DomainTag(
                        tag.getName(),
                        protocolAdapterWrapper.getId(),
                        protocolAdapterWrapper.getAdapterInformation().getProtocolId(),
                        tag.getDescription()))
                .forEach(dTag -> {
                    DomainTagAddResult result = domainTagPersistence.addDomainTag(dTag);
                    if(result.getDomainTagPutStatus().equals(DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS)) {
                        duplicatedTags.put(dTag.getTagName(), result);
                    }
                });

        if(!duplicatedTags.isEmpty()) {
            List<String> tagIdToOwningAdapter = duplicatedTags.entrySet().stream()
                    .map(entry -> entry.getKey() + " => " + entry.getValue().getAdapterIdOfOwningAdapter())
                    .collect(Collectors.toList());

            log.error("The adapter {} contains tags already provided by other adapters: {}", protocolAdapterWrapper.getId(), tagIdToOwningAdapter);
            domainTagPersistence.adapterIsGone(protocolAdapterWrapper.getId());

            eventService
                    .createAdapterEvent(protocolAdapterWrapper.getId(), protocolAdapterWrapper.getAdapterInformation().getProtocolId())
                    .withMessage(
                            "Starting the adapter failed because it tried to register tag names owned by another adapter: " +
                                    tagIdToOwningAdapter)
                    .withSeverity(Event.SEVERITY.ERROR)
                    .fire();

            CompletableFuture.failedFuture(
                    new ProtocolAdapterException("Tried to register tags owned by another adapter: " + tagIdToOwningAdapter));
        }



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
                        jsonPayloadDefaultCreator,
                        protocolAdapterTagService);
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
            domainTagPersistence.adapterIsGone(protocolAdapterWrapper.getId());
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
            domainTagPersistence.adapterIsGone(protocolAdapterWrapper.getId());
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
            final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterType);
        Preconditions.checkNotNull(adapterId);
        if (getAdapterTypeById(adapterType).isEmpty()) {
            throw new IllegalArgumentException("invalid adapter type '" + adapterType + "'");
        }
        if (getAdapterById(adapterId).isPresent()) {
            throw new IllegalArgumentException("adapter already exists by id '" + adapterId + "'");
        }
        protocolAdapterMetrics.increaseProtocolAdapterMetric(adapterType);
        return addAdapterAndStartInRuntime(adapterType, config);
    }

    public boolean deleteAdapter(final @NotNull String id) {
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
                synchronized (lock) {
                    //ensure the instance releases any hard state
                    adapterWrapper.getAdapter().destroy();
                    final Map<String, Object> mainMap =
                            configurationService.protocolAdapterConfigurationService().getAllConfigs();
                    final List<Map<String, Object>> adapterList =
                            getAdapterListForType(adapterWrapper.getAdapterInformation().getProtocolId());
                    if (adapterList.removeIf(instance -> id.equals(instance.get("id")))) {
                        configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
                    }
                }
                return true;
            } finally {
                final String adapterId = adapterWrapper.getId();
                eventService.createAdapterEvent(adapterId,
                                adapterWrapper.getProtocolAdapterInformation().getProtocolId())
                        .withSeverity(Event.SEVERITY.WARN)
                        .withMessage(String.format("Adapter '%s' was deleted from the system permanently.", adapterId))
                        .fire();

            }
        }
        return false;
    }

    public boolean updateAdapter(final @NotNull String adapterId, final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterId);
        final Optional<ProtocolAdapterWrapper<? extends ProtocolAdapter>> adapterInstance = getAdapterById(adapterId);
        if (adapterInstance.isPresent()) {
            final ProtocolAdapterWrapper<? extends ProtocolAdapter> oldInstance = adapterInstance.get();
            deleteAdapter(oldInstance.getId());
            addAdapter(oldInstance.getProtocolAdapterInformation().getProtocolId(), oldInstance.getId(), config);
            return true;
        }
        return false;
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
        return factoryMap.values()
                .stream()
                .map(ProtocolAdapterFactory::getInformation)
                .collect(Collectors.toMap(ProtocolAdapterInformation::getProtocolId, o -> o));
    }


    public @NotNull Map<String, ProtocolAdapterWrapper<? extends ProtocolAdapter>> getProtocolAdapters() {
        return protocolAdapters;
    }

    private @NotNull ProtocolAdapterWrapper createAdapterInstance(
            final @NotNull String adapterType,
            final @NotNull Map<String, Object> config,
            final @NotNull String version) {

        final ProtocolAdapterFactory<?> protocolAdapterFactory = getProtocolAdapterFactory(adapterType);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(protocolAdapterFactory.getClass().getClassLoader());
            final ProtocolAdapterConfig configObject = protocolAdapterFactory.convertConfigObject(objectMapper, config, writingEnabled());


            final ProtocolAdapterMetricsService protocolAdapterMetricsService = new ProtocolAdapterMetricsServiceImpl(
                    protocolAdapterFactory.getInformation().getProtocolId(),
                    configObject.getId(),
                    metricRegistry);


            final ProtocolAdapterStateImpl protocolAdapterState =
                    new ProtocolAdapterStateImpl(moduleServices.eventService(),
                            configObject.getId(),
                            protocolAdapterFactory.getInformation().getProtocolId());

            final ModuleServicesPerModuleImpl moduleServicesPerModule =
                    new ModuleServicesPerModuleImpl(moduleServices.adapterPublishService(),
                            eventService,
                            moduleServices.protocolAdapterTagService(),
                            protocolAdapterWritingService);
            final ProtocolAdapter protocolAdapter =
                    protocolAdapterFactory.createAdapter(protocolAdapterFactory.getInformation(),
                            new ProtocolAdapterInputImpl(configObject,
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
                    configObject);
            protocolAdapters.put(wrapper.getId(), wrapper);
            return wrapper;

        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private @NotNull CompletableFuture<Void> addAdapterAndStartInRuntime(
            final @NotNull String adapterType, final @NotNull Map<String, Object> config) {

        synchronized (lock) {
            final ProtocolAdapterWrapper instance =
                    createAdapterInstance(adapterType, config, versionProvider.getVersion());

            //-- Write the protocol adapter back to the main config (through the proxy)
            final List<Map<String, Object>> adapterList = getAdapterListForType(adapterType);
            final Map<String, Object> mainMap =
                    configurationService.protocolAdapterConfigurationService().getAllConfigs();
            adapterList.add(config);
            configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
            return start(instance);
        }
    }

    private @NotNull List<Map<String, Object>> getAdapterListForType(final @NotNull String adapterType) {

        final Map<String, Object> mainMap = configurationService.protocolAdapterConfigurationService().getAllConfigs();
        final List<Map<String, Object>> adapterList;
        final Object o = mainMap.get(adapterType);
        if (o instanceof Map || o instanceof String || o == null) {
            adapterList = new ArrayList<>();
            if (o instanceof Map) {
                adapterList.add((Map) o);
            }
            mainMap.put(adapterType, adapterList);
        } else {
            adapterList = (List) o;
        }
        return adapterList;
    }

    public boolean writingEnabled() {
        return protocolAdapterWritingService.writingEnabled();
    }


    public static class ProtocolAdapterInputImpl<T extends ProtocolAdapterConfig> implements ProtocolAdapterInput<T> {
        public static final AdapterFactoriesImpl ADAPTER_FACTORIES = new AdapterFactoriesImpl();
        private final @NotNull T configObject;
        private final @NotNull String version;
        private final @NotNull ProtocolAdapterState protocolAdapterState;
        private final @NotNull ModuleServices moduleServices;
        private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;

        public ProtocolAdapterInputImpl(
                final @NotNull T configObject,
                final @NotNull String version,
                final @NotNull ProtocolAdapterState protocolAdapterState,
                final @NotNull ModuleServices moduleServices,
                final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
            this.configObject = configObject;
            this.version = version;
            this.protocolAdapterState = protocolAdapterState;
            this.moduleServices = moduleServices;
            this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        }

        @NotNull
        @Override
        public T getConfig() {
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
    }
}
