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
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
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
import net.javacrumbs.futureconverter.java8guava.FutureConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
            final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator) {
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
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NotNull ListenableFuture<Void> start() {

        findAllAdapters();

        if (log.isInfoEnabled()) {
            log.info("Discovered {} protocol adapter-type(s): [{}].",
                    factoryMap.size(),
                    factoryMap.values()
                            .stream()
                            .map(protocolAdapterFactory -> "'" +
                                    protocolAdapterFactory.getInformation().getProtocolName() +
                                    "'")
                            .collect(Collectors.joining(", ")));
        }

        //iterate configs and start each adapter
        final Map<String, Object> allConfigs =
                configurationService.protocolAdapterConfigurationService().getAllConfigs();

        if (allConfigs.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        final ImmutableList.Builder<CompletableFuture<Void>> adapterFutures = ImmutableList.builder();

        for (Map.Entry<String, Object> configSection : allConfigs.entrySet()) {
            final String adapterType = configSection.getKey();
            final ProtocolAdapterFactory<?> protocolAdapterFactory = getProtocolAdapterFactory(adapterType);
            if (protocolAdapterFactory == null) {
                if (log.isWarnEnabled()) {
                    log.warn("Protocol adapter for config {} not found.", adapterType);
                }
                continue;
            }
            Object adapterXmlElement = configSection.getValue();
            List<Map<String, Object>> adapterConfigs;
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

            for (Map<String, Object> adapterConfig : adapterConfigs) {
                ProtocolAdapterWrapper instance =
                        createAdapterInstance(adapterType, adapterConfig, versionProvider.getVersion());
                protocolAdapterMetrics.increaseProtocolAdapterMetric(instance.getAdapter()
                        .getProtocolAdapterInformation()
                        .getProtocolId());
                CompletableFuture<Void> future = start(instance);
                adapterFutures.add(future);
            }
        }

        return FutureConverter.toListenableFuture(CompletableFuture.allOf(adapterFutures.build()
                .toArray(new CompletableFuture[]{})));
    }

    @SuppressWarnings("rawtypes")
    private void findAllAdapters() {
        final List<Class<? extends ProtocolAdapterFactory>> implementations =
                moduleLoader.findImplementations(ProtocolAdapterFactory.class);

        implementations.add(SimulationProtocolAdapterFactory.class);

        for (Class<? extends ProtocolAdapterFactory> facroryClass : implementations) {
            try {
                final ProtocolAdapterFactory<?> protocolAdapterFactory =
                        facroryClass.getDeclaredConstructor().newInstance();
                if (log.isDebugEnabled()) {
                    log.debug("Discovered protocol adapter implementation {}.", facroryClass.getName());
                }
                final ProtocolAdapterInformation information = protocolAdapterFactory.getInformation();
                factoryMap.put(information.getProtocolId(), protocolAdapterFactory);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                log.error("Not able to load module, reason: {}.", e.getMessage());
            }
        }
    }

    public @Nullable ProtocolAdapterFactory<?> getProtocolAdapterFactory(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return factoryMap.get(protocolAdapterType);
    }

    public @NotNull CompletableFuture<Void> start(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        Optional<ProtocolAdapterWrapper<? extends ProtocolAdapter>> adapterOptional = getAdapterById(protocolAdapterId);
        return adapterOptional.map(this::start)
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        Optional<ProtocolAdapterWrapper<?>> adapterOptional = getAdapterById(protocolAdapterId);
        return adapterOptional.map(this::stop)
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    public @NotNull CompletableFuture<Void> start(final @NotNull ProtocolAdapterWrapper<? extends ProtocolAdapter> protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);
        if (log.isInfoEnabled()) {
            log.info("Starting protocol-adapter '{}'.", protocolAdapterWrapper.getId());
        }
        CompletableFuture<Boolean> startFuture;
        final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
        if (protocolAdapterWrapper.getRuntimeStatus() == ProtocolAdapterState.RuntimeStatus.STARTED) {
            startFuture = CompletableFuture.completedFuture(true);
        } else {
            protocolAdapterWrapper.start(new ProtocolAdapterStartInputImpl(moduleServices,
                    protocolAdapterWrapper,
                    eventService), output);
            startFuture = output.getStartFuture();
        }
        return startFuture.<Void>thenApply(startedSuccessfuly -> {
            if (!startedSuccessfuly) {
                handleStartupError(protocolAdapterWrapper, output);
            } else {
                schedulePolling(protocolAdapterWrapper);
                protocolAdapterWrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STARTED);
                eventService.createAdapterEvent(protocolAdapterWrapper.getId(),
                                protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId())
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Adapter '%s' started OK.", protocolAdapterWrapper.getId()))
                        .fire();

                if (log.isTraceEnabled()) {
                    log.trace("Protocol-adapter '{}' started.", protocolAdapterWrapper.getId());
                }
                HiveMQEdgeRemoteEvent adapterCreatedEvent =
                        new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED);
                adapterCreatedEvent.addUserData("adapterType",
                        protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId());
                remoteService.fireUsageEvent(adapterCreatedEvent);

            }
            return null;
        }).exceptionally(throwable -> {
            output.failStart(throwable, output.getMessage());
            handleStartupError(protocolAdapterWrapper, output);
            return null;
        });
    }

    private void schedulePolling(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        if (protocolAdapterWrapper.getAdapter() instanceof PollingProtocolAdapter) {
            log.info("Scheduling polling for adapter {}", protocolAdapterWrapper.getId());
            final PollingProtocolAdapter adapter = (PollingProtocolAdapter) protocolAdapterWrapper.getAdapter();
            adapter.getPollingContexts().forEach(adapterSubscription -> {
                //noinspection unchecked this is safe as we literally make a check on the adapter class before
                final PerSubscriptionSampler<? extends PollingContext> sampler = new PerSubscriptionSampler<>(
                        protocolAdapterWrapper,
                        metricRegistry,
                        objectMapper,
                        moduleServices.adapterPublishService(),
                        (PollingContext) adapterSubscription,
                        eventService,
                        jsonPayloadDefaultCreator);
                protocolAdapterPollingService.schedulePolling(protocolAdapterWrapper, sampler);
            });
        }
    }

    public @NotNull CompletableFuture<Void> stop(final @NotNull ProtocolAdapterWrapper<? extends ProtocolAdapter> protocolAdapter) {
        Preconditions.checkNotNull(protocolAdapter);
        if (log.isInfoEnabled()) {
            log.info("Stopping protocol-adapter '{}'.", protocolAdapter.getId());
        }
        CompletableFuture<Void> stopFuture;

        if (protocolAdapter instanceof PollingProtocolAdapter) {
            protocolAdapterPollingService.getPollingJobsForAdapter(protocolAdapter.getId())
                    .forEach(protocolAdapterPollingService::stopPolling);
        }

        if (protocolAdapter.getRuntimeStatus() == ProtocolAdapterState.RuntimeStatus.STOPPED) {
            stopFuture = CompletableFuture.completedFuture(null);
        } else {
            final ProtocolAdapterStopOutputImpl adapterStopOutput = new ProtocolAdapterStopOutputImpl();
            stopFuture = adapterStopOutput.getOutputFuture();
            protocolAdapter.stop(new ProtocolAdapterStopInputImpl(), adapterStopOutput);
        }
        stopFuture.thenApply(input -> {
            if (log.isTraceEnabled()) {
                log.trace("Protocol-adapter '{}' stopped.", protocolAdapter.getId());
            }
            protocolAdapter.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
            eventService.createAdapterEvent(protocolAdapter.getId(),
                            protocolAdapter.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.INFO)
                    .withMessage(String.format("Adapter '%s' stopped OK.", protocolAdapter.getId()))
                    .fire();

            return null;
        }).exceptionally(throwable -> {
            if (log.isWarnEnabled()) {
                log.warn("Protocol-adapter '{}' was unable to stop cleanly", protocolAdapter.getId(), throwable);
            }
            eventService.createAdapterEvent(protocolAdapter.getId(),
                            protocolAdapter.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.CRITICAL)
                    .withMessage("Error stopping adapter '" + protocolAdapter.getId() + "'.")
                    .fire();
            return null;
        });
        return stopFuture;
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

        HiveMQEdgeRemoteEvent adapterCreatedEvent =
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

    public @NotNull CompletableFuture<Boolean> deleteAdapter(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        Optional<ProtocolAdapterWrapper<? extends ProtocolAdapter>> adapterInstance = getAdapterById(id);
        if (adapterInstance.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        protocolAdapterMetrics.decreaseProtocolAdapterMetric(adapterInstance.get()
                .getAdapterInformation()
                .getProtocolId());
        protocolAdapterPollingService.stopPollingForAdapterInstance(adapterInstance.get());
        final ProtocolAdapterStopOutputImpl adapterStopOutput = new ProtocolAdapterStopOutputImpl();
        adapterInstance.get().stop(new ProtocolAdapterStopInputImpl(), adapterStopOutput);

        return adapterStopOutput.getOutputFuture().handle((aVoid, throwable) -> {
            final String adapterId = adapterInstance.get().getId();
            if (throwable != null) {
                log.warn(
                        "An exception was raised while stopping adapter '{}' before deleting the adapter. The adapter will be tried to be deleted anyway.",
                        adapterId,
                        throwable);
            }
            if (protocolAdapters.remove(id) != null) {
                try {
                    synchronized (lock) {
                        //ensure the instance releases any hard state
                        adapterInstance.get().destroy();
                        Map<String, Object> mainMap =
                                configurationService.protocolAdapterConfigurationService().getAllConfigs();
                        List<Map<String, ?>> adapterList =
                                getAdapterListForType(adapterInstance.get().getAdapterInformation().getProtocolId());
                        if (adapterList.removeIf(instance -> id.equals(instance.get("id")))) {
                            configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
                        }
                    }
                } finally {
                    eventService.createAdapterEvent(adapterId,
                                    adapterInstance.get().getProtocolAdapterInformation().getProtocolId())
                            .withSeverity(Event.SEVERITY.WARN)
                            .withMessage(String.format("Adapter '%s' was deleted from the system permanently.",
                                    adapterId))
                            .fire();

                }
            }
            return true;
        });
    }

    public @NotNull CompletableFuture<Boolean> updateAdapter(
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterId);
        Optional<ProtocolAdapterWrapper<? extends ProtocolAdapter>> adapterInstance = getAdapterById(adapterId);
        if (adapterInstance.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        ProtocolAdapterWrapper<? extends ProtocolAdapter> oldInstance = adapterInstance.get();

        return deleteAdapter(oldInstance.getId()).thenCompose(aVoid -> addAdapter(oldInstance.getProtocolAdapterInformation().getProtocolId(),
                oldInstance.getId(),
                config).thenApply(otherVoid -> true)
        );
    }

    public @NotNull Optional<ProtocolAdapterWrapper<? extends ProtocolAdapter>> getAdapterById(final @NotNull String id) {
        Preconditions.checkNotNull(id);
        Map<String, ProtocolAdapterWrapper<? extends ProtocolAdapter>> adapters = getProtocolAdapters();
        return Optional.ofNullable(adapters.get(id));
    }

    public @NotNull Optional<ProtocolAdapterInformation> getAdapterTypeById(final @NotNull String typeId) {
        Preconditions.checkNotNull(typeId);
        ProtocolAdapterInformation information = getAllAvailableAdapterTypes().get(typeId);
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

    protected @NotNull ProtocolAdapterWrapper<? extends ProtocolAdapter> createAdapterInstance(
            final @NotNull String adapterType,
            final @NotNull Map<String, Object> config,
            final @NotNull String version) {

        ProtocolAdapterFactory<?> protocolAdapterFactory = getProtocolAdapterFactory(adapterType);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(protocolAdapterFactory.getClass().getClassLoader());
            final ProtocolAdapterConfig configObject = protocolAdapterFactory.convertConfigObject(objectMapper, config);


            final ProtocolAdapterMetricsService protocolAdapterMetricsService = new ProtocolAdapterMetricsServiceImpl(
                    protocolAdapterFactory.getInformation().getProtocolId(),
                    configObject.getId(),
                    metricRegistry);


            final ProtocolAdapterStateImpl protocolAdapterState =
                    new ProtocolAdapterStateImpl(moduleServices.eventService(),
                            configObject.getId(),
                            protocolAdapterFactory.getInformation().getProtocolId());

            final ModuleServicesPerModuleImpl moduleServicesPerModule =
                    new ModuleServicesPerModuleImpl(null, moduleServices, eventService);
            final ProtocolAdapter protocolAdapter =
                    protocolAdapterFactory.createAdapter(protocolAdapterFactory.getInformation(),
                            new ProtocolAdapterInputImpl(configObject,
                                    version,
                                    protocolAdapterState,
                                    moduleServicesPerModule,
                                    protocolAdapterMetricsService));
            // hen-egg problem. Rather solve this here as have not final fields in the adapter.
            moduleServicesPerModule.setAdapter(protocolAdapter);

            ProtocolAdapterWrapper<? extends ProtocolAdapter> wrapper = new ProtocolAdapterWrapper<>(protocolAdapter,
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

    protected @NotNull CompletableFuture<Void> addAdapterAndStartInRuntime(
            final @NotNull String adapterType, final @NotNull Map<String, Object> config) {

        synchronized (lock) {
            ProtocolAdapterWrapper<? extends ProtocolAdapter> instance =
                    createAdapterInstance(adapterType, config, versionProvider.getVersion());

            //-- Write the protocol adapter back to the main config (through the proxy)
            List<Map<String, ?>> adapterList = getAdapterListForType(adapterType);
            Map<String, Object> mainMap = configurationService.protocolAdapterConfigurationService().getAllConfigs();
            adapterList.add(config);
            configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
            return start(instance);
        }
    }

    protected @NotNull List<Map<String, ?>> getAdapterListForType(final @NotNull String adapterType) {
        final Map<String, Object> mainMap = configurationService.protocolAdapterConfigurationService().getAllConfigs();
        List<Map<String, ?>> adapterList = new ArrayList<>();
        final Object o = mainMap.get(adapterType);
        if (o instanceof Map) {
            adapterList.add((Map) o);
            mainMap.put(adapterType, adapterList);
        } else if (o instanceof String || o == null) {
            mainMap.put(adapterType, adapterList);
        } else if (o instanceof List) {
            adapterList = (List) o;
        }
        return adapterList;
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
