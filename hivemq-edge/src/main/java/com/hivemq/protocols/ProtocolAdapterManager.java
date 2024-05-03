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
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.edge.model.TypeIdentifierImpl;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.edge.modules.adapters.PollingPerSubscriptionProtocolAdapter;
import com.hivemq.edge.modules.adapters.PollingProtocolAdapter;
import com.hivemq.edge.modules.adapters.factories.AdapterFactories;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelper;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsHelperImpl;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.adapters.simulation.SimulationProtocolAdapterFactory;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterFactory;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterState;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.edge.modules.api.events.model.EventBuilder;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.edge.modules.api.events.model.EventImpl;
import com.hivemq.edge.modules.config.CustomConfig;
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
    private final @NotNull Map<String, ProtocolAdapterWrapper> protocolAdapters = new ConcurrentHashMap<>();
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
            final @NotNull ProtocolAdapterMetrics protocolAdapterMetrics) {
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

        if (allConfigs.size() < 1) {
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

    public ProtocolAdapterFactory getProtocolAdapterFactory(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        final ProtocolAdapterFactory<?> protocolAdapterFactory = factoryMap.get(protocolAdapterType);
        return protocolAdapterFactory;
    }

    public CompletableFuture<Void> start(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        Optional<ProtocolAdapterWrapper> adapterOptional = getAdapterById(protocolAdapterId);
        if (!adapterOptional.isPresent()) {
            return CompletableFuture.failedFuture(null);
        } else {
            return start(adapterOptional.get());
        }
    }

    public CompletableFuture<Void> stop(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        Optional<ProtocolAdapterWrapper> adapterOptional = getAdapterById(protocolAdapterId);
        if (!adapterOptional.isPresent()) {
            return CompletableFuture.failedFuture(null);
        } else {
            return stop(adapterOptional.get());
        }
    }

    // TODO perhaps move logic into wrapped adapter
    public @NotNull CompletableFuture<Void> start(final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        Preconditions.checkNotNull(protocolAdapterWrapper);
        if (log.isInfoEnabled()) {
            log.info("Starting protocol-adapter \"{}\".", protocolAdapterWrapper.getId());
        }
        CompletableFuture<ProtocolAdapterStartOutput> startFuture;
        final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
        if (protocolAdapterWrapper.getRuntimeStatus() == ProtocolAdapterState.RuntimeStatus.STARTED) {
            startFuture = CompletableFuture.completedFuture(output);
        } else {
            startFuture =
                    protocolAdapterWrapper.start(new ProtocolAdapterStartInputImpl(protocolAdapterWrapper), output);
        }
        return startFuture.<Void>thenApply(input -> {
            if (!output.startedSuccessfully) {
                handleStartupError(protocolAdapterWrapper, output);
            } else {
                schedulePolling(protocolAdapterWrapper);
                if (output.message != null) {
                    if (log.isTraceEnabled()) {
                        log.trace("Protocol-adapter \"{}\" started: {}.",
                                protocolAdapterWrapper.getId(),
                                output.message);
                    }
                    HiveMQEdgeRemoteEvent adapterCreatedEvent =
                            new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED);
                    adapterCreatedEvent.addUserData("adapterType",
                            protocolAdapterWrapper.getProtocolAdapterInformation().getProtocolId());
                    remoteService.fireUsageEvent(adapterCreatedEvent);
                }
            }
            return null;
        }).exceptionally(throwable -> {
            output.failStart(throwable, output.message);
            handleStartupError(protocolAdapterWrapper, output);
            return null;
        });
    }

    private void schedulePolling(@NotNull ProtocolAdapterWrapper protocolAdapterWrapper) {
        if (protocolAdapterWrapper.getAdapter() instanceof PollingPerSubscriptionProtocolAdapter) {
            log.info("Scheduling polling for adapter {}", protocolAdapterWrapper.getId());
            final PollingPerSubscriptionProtocolAdapter adapter =
                    (PollingPerSubscriptionProtocolAdapter) protocolAdapterWrapper.getAdapter();
            adapter.getSubscriptions().forEach(adapterSubscription -> {
                final PerSubscriptionSampler sampler = new PerSubscriptionSampler(adapter,
                        protocolAdapterWrapper.getConfigObject(),
                        metricRegistry,
                        objectMapper,
                        moduleServices.adapterPublishService(),
                        adapterSubscription);
                protocolAdapterPollingService.schedulePolling(protocolAdapterWrapper, sampler);
            });
        } else if (protocolAdapterWrapper.getAdapter() instanceof PollingProtocolAdapter) {
            log.info("Scheduling polling for adapter {}", protocolAdapterWrapper.getId());
            final SubscriptionSampler sampler =
                    new SubscriptionSampler((PollingProtocolAdapter) protocolAdapterWrapper.getAdapter(),
                            protocolAdapterWrapper.getConfigObject(),
                            metricRegistry,
                            objectMapper,
                            moduleServices.adapterPublishService());
            protocolAdapterPollingService.schedulePolling(protocolAdapterWrapper, sampler);
        }
    }

    public CompletableFuture<Void> stop(final @NotNull ProtocolAdapterWrapper protocolAdapter) {
        Preconditions.checkNotNull(protocolAdapter);
        if (log.isInfoEnabled()) {
            log.info("Stopping protocol-adapter \"{}\".", protocolAdapter.getId());
        }
        CompletableFuture<Void> stopFuture;

        if (protocolAdapter instanceof PollingProtocolAdapter) {
            protocolAdapterPollingService.getPollingJobsForAdapter(protocolAdapter.getId())
                    .forEach(protocolAdapterPollingService::stopPolling);
        }

        if (protocolAdapter.getRuntimeStatus() == ProtocolAdapterState.RuntimeStatus.STOPPED) {
            stopFuture = CompletableFuture.completedFuture(null);
        } else {
            stopFuture = protocolAdapter.stop();
        }
        stopFuture.thenApply(input -> {
            if (log.isTraceEnabled()) {
                log.trace("Protocol-adapter \"{}\" stopped.", protocolAdapter.getId());
            }
            return null;
        }).exceptionally(throwable -> {
            if (log.isWarnEnabled()) {
                log.warn("Protocol-adapter \"{}\" was unable to stop cleanly", protocolAdapter.getId(), throwable);
            }
            return null;
        });
        return stopFuture;
    }

    protected void handleStartupError(
            final @NotNull ProtocolAdapter protocolAdapter, @NotNull final ProtocolAdapterStartOutputImpl output) {
        if (log.isWarnEnabled()) {
            log.warn("Protocol-adapter \"{}\" could not be started, reason: {}",
                    protocolAdapter.getId(), output.message,
                    output.getThrowable());
        }
        HiveMQEdgeRemoteEvent adapterCreatedEvent =
                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_ERROR);
        adapterCreatedEvent.addUserData("adapterType", protocolAdapter.getProtocolAdapterInformation().getProtocolId());
        remoteService.fireUsageEvent(adapterCreatedEvent);
    }

    public synchronized CompletableFuture<Void> addAdapter(
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

    public boolean deleteAdapter(final String id) {
        Preconditions.checkNotNull(id);
        Optional<ProtocolAdapterWrapper> adapterInstance = getAdapterById(id);
        if (adapterInstance.isPresent()) {
            protocolAdapterMetrics.decreaseProtocolAdapterMetric(adapterInstance.get()
                    .getAdapterInformation()
                    .getProtocolId());
            adapterInstance.get().stop();
            if (protocolAdapters.remove(id) != null) {
                try {
                    synchronized (lock) {
                        //ensure the instance releases any hard state
                        adapterInstance.get().destroy();
                        Map<String, Object> mainMap =
                                configurationService.protocolAdapterConfigurationService().getAllConfigs();
                        List<Map> adapterList =
                                getAdapterListForType(adapterInstance.get().getAdapterInformation().getProtocolId());
                        if (adapterList != null) {
                            if (adapterList.removeIf(instance -> id.equals(instance.get("id")))) {
                                configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
                            }
                        }
                    }
                    return true;
                } finally {
                    eventService.fireEvent(eventBuilder(EventImpl.SEVERITY.WARN, adapterInstance.get()).withMessage(
                            String.format("Adapter \"%s\" was deleted from the system permanently.",
                                    adapterInstance.get().getId())).build());
                }
            }
        }
        return false;
    }

    public boolean updateAdapter(final @NotNull String adapterId, final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterId);
        Optional<ProtocolAdapterWrapper> adapterInstance = getAdapterById(adapterId);
        if (adapterInstance.isPresent()) {
            ProtocolAdapterWrapper oldInstance = adapterInstance.get();
            deleteAdapter(oldInstance.getId());
            addAdapter(oldInstance.getProtocolAdapterInformation().getProtocolId(), oldInstance.getId(), config);
            return true;
        }
        return false;
    }

    public Optional<ProtocolAdapterWrapper> getAdapterById(final String id) {
        Preconditions.checkNotNull(id);
        Map<String, ProtocolAdapterWrapper> adapters = getProtocolAdapters();
        return Optional.ofNullable(adapters.get(id));
    }

    public Optional<ProtocolAdapterInformation> getAdapterTypeById(final String typeId) {
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


    public @NotNull Map<String, ProtocolAdapterWrapper> getProtocolAdapters() {
        return protocolAdapters;
    }

    protected @NotNull ProtocolAdapterWrapper createAdapterInstance(
            final @NotNull String adapterType,
            final @NotNull Map<String, Object> config,
            final @NotNull String version) {

        ProtocolAdapterFactory<?> protocolAdapterFactory = getProtocolAdapterFactory(adapterType);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(protocolAdapterFactory.getClass().getClassLoader());
            final CustomConfig configObject = protocolAdapterFactory.convertConfigObject(objectMapper, config);


            final ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper = new ProtocolAdapterMetricsHelperImpl(
                    protocolAdapterFactory.getInformation().getProtocolId(),
                    configObject.getId(),
                    metricRegistry);


            final ProtocolAdapterStateImpl protocolAdapterState =
                    new ProtocolAdapterStateImpl(moduleServices.eventService());

            final ModuleServicesPerModuleImpl moduleServicesPerModule =
                    new ModuleServicesPerModuleImpl(null, moduleServices, eventService);
            final ProtocolAdapter protocolAdapter =
                    protocolAdapterFactory.createAdapter(protocolAdapterFactory.getInformation(),
                            new ProtocolAdapterInputImpl(configObject,
                                    metricRegistry,
                                    version,
                                    protocolAdapterState,
                                    moduleServicesPerModule,
                                    protocolAdapterMetricsHelper));
            // hen-egg problem. Rather solve this here as have not final fields in the adapter.
            moduleServicesPerModule.setAdapter(protocolAdapter);

            ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(protocolAdapter,
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
            ProtocolAdapterWrapper instance = createAdapterInstance(adapterType, config, versionProvider.getVersion());

            //-- Write the protocol adapter back to the main config (through the proxy)
            List<Map> adapterList = getAdapterListForType(adapterType);
            Map<String, Object> mainMap = configurationService.protocolAdapterConfigurationService().getAllConfigs();
            adapterList.add(config);
            configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
            return start(instance);
        }
    }

    protected List<Map> getAdapterListForType(final String adapterType) {

        Map<String, Object> mainMap = configurationService.protocolAdapterConfigurationService().getAllConfigs();
        List<Map> adapterList = null;
        Object o = mainMap.get(adapterType);
        if (o instanceof Map || o instanceof String || o == null) {
            if (adapterList == null) {
                adapterList = new ArrayList<>();
            }
            if (o != null && o instanceof Map) {
                adapterList.add((Map) o);
            }
            mainMap.put(adapterType, adapterList);
        } else {
            adapterList = (List) o;
        }
        return adapterList;
    }


    public static class ProtocolAdapterInputImpl<T extends CustomConfig> implements ProtocolAdapterInput<T> {
        public static final AdapterFactoriesImpl ADAPTER_FACTORIES = new AdapterFactoriesImpl();
        private final @NotNull T configObject;
        private final @NotNull MetricRegistry metricRegistry;
        private final @NotNull String version;
        private final @NotNull ProtocolAdapterState protocolAdapterState;
        private final @NotNull ModuleServices moduleServices;
        private final @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper;

        public ProtocolAdapterInputImpl(
                final @NotNull T configObject,
                final @NotNull MetricRegistry metricRegistry,
                final @NotNull String version,
                final @NotNull ProtocolAdapterState protocolAdapterState,
                final @NotNull ModuleServices moduleServices,
                final @NotNull ProtocolAdapterMetricsHelper protocolAdapterMetricsHelper) {
            this.configObject = configObject;
            this.metricRegistry = metricRegistry;
            this.version = version;
            this.protocolAdapterState = protocolAdapterState;
            this.moduleServices = moduleServices;
            this.protocolAdapterMetricsHelper = protocolAdapterMetricsHelper;
        }

        @NotNull
        @Override
        public T getConfig() {
            return configObject;
        }

        @Override
        public @NotNull MetricRegistry getMetricRegistry() {
            return metricRegistry;
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
        public @NotNull ProtocolAdapterMetricsHelper getProtocolAdapterMetricsHelper() {
            return protocolAdapterMetricsHelper;
        }


    }

    private static class ProtocolAdapterStartOutputImpl implements ProtocolAdapterStartOutput {

        //default: all good
        boolean startedSuccessfully = true;
        @Nullable String message = null;
        @Nullable Throwable throwable;

        @Override
        public void startedSuccessfully(@NotNull final String message) {
            startedSuccessfully = true;
            this.message = message;
        }

        @Override
        public void failStart(@NotNull Throwable t, @NotNull final String errorMessage) {
            startedSuccessfully = false;
            this.throwable = t;
            this.message = errorMessage;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public boolean isStartedSuccessfully() {
            return startedSuccessfully;
        }

        public @Nullable String getMessage() {
            return message;
        }
    }

    private class ProtocolAdapterStartInputImpl implements ProtocolAdapterStartInput {

        private final @NotNull ProtocolAdapter protocolAdapter;

        private ProtocolAdapterStartInputImpl(final @NotNull ProtocolAdapter protocolAdapter) {
            this.protocolAdapter = protocolAdapter;
        }

        @Override
        public @NotNull ModuleServices moduleServices() {
            return new ModuleServicesPerModuleImpl(protocolAdapter, moduleServices, eventService);
        }
    }

    protected EventBuilder eventBuilder(
            final @NotNull EventImpl.SEVERITY severity, final @NotNull ProtocolAdapter adapter) {
        Preconditions.checkNotNull(severity);
        Preconditions.checkNotNull(adapter);
        EventBuilder builder = new EventBuilderImpl();
        builder.withTimestamp(System.currentTimeMillis());
        builder.withSource(TypeIdentifierImpl.create(TypeIdentifierImpl.TYPE.ADAPTER, adapter.getId()));
        builder.withSeverity(severity);
        return builder;
    }
}
