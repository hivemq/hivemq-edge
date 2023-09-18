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
import com.hivemq.edge.model.HiveMQEdgeEvent;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.params.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.adapters.simulation.SimulationProtocolAdapterFactory;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterFactory;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterInformation;
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

    private final @NotNull Map<String, ProtocolAdapterFactory<?>> configToAdapterMap = new ConcurrentHashMap<>();
    private final @NotNull Map<String, AdapterInstance> protocolAdapters = new ConcurrentHashMap<>();
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull ModuleServicesImpl moduleServices;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ModuleLoader moduleLoader;
    private final @NotNull HiveMQEdgeRemoteService remoteService;

    private final @NotNull Object lock = new Object();

    @Inject
    public ProtocolAdapterManager(
            final @NotNull ConfigurationService configurationService,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull ModuleServicesImpl moduleServices,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ModuleLoader moduleLoader,
            final @NotNull HiveMQEdgeRemoteService remoteService) {
        this.configurationService = configurationService;
        this.metricRegistry = metricRegistry;
        this.moduleServices = moduleServices;
        this.objectMapper = ProtocolAdapterUtils.createProtocolAdapterMapper(objectMapper);
        this.moduleLoader = moduleLoader;
        this.remoteService = remoteService;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public @NotNull ListenableFuture<Void> start() {

        findAllAdapters();

        log.info("Discovered {} protocol adapter-type: [{}]",
                configToAdapterMap.size(),
                configToAdapterMap.values()
                        .stream()
                        .map(protocolAdapterFactory -> "'" +
                                protocolAdapterFactory.getInformation().getProtocolName() +
                                "'")
                        .collect(Collectors.joining(", ")));

        //iterate configs and start each adapter
        final Map<String, Object> allConfigs =
                configurationService.protocolAdapterConfigurationService().getAllConfigs();

        if (allConfigs.size() < 1) {
            return Futures.immediateFuture(null);
        }
        final ImmutableList.Builder<CompletableFuture<Void>> adapterFutures =
                ImmutableList.builder();

        for (Map.Entry<String, Object> configSection : allConfigs.entrySet()) {
            final String adapterType = configSection.getKey();
            final ProtocolAdapterFactory<?> protocolAdapterFactory = getProtocolAdapterFactory(adapterType);
            if (protocolAdapterFactory == null) {
                log.error("Protocol adapter for config {} not found", adapterType);
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
                log.warn("found invalid configuration element for adapter {}, skipping", adapterType);
                continue;
            }

            for (Map<String, Object> adapterConfig : adapterConfigs) {
                AdapterInstance instance = createAdapterInstance(adapterType, adapterConfig);
                CompletableFuture<Void> future = start(instance.getAdapter());
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
                log.debug("Discovered Protocol Adapter Implementation {}", facroryClass.getName());
                final ProtocolAdapterInformation information = protocolAdapterFactory.getInformation();
                configToAdapterMap.put(information.getProtocolId(), protocolAdapterFactory);
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                log.error("Not able to load module, reason: {}", e.getMessage());
            }
        }
    }

    public ProtocolAdapterFactory getProtocolAdapterFactory(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        final ProtocolAdapterFactory<?> protocolAdapterFactory = configToAdapterMap.get(protocolAdapterType);
        return protocolAdapterFactory;
    }

    public CompletableFuture<Void> start(final @NotNull ProtocolAdapter protocolAdapter) {
        Preconditions.checkNotNull(protocolAdapter);
        CompletableFuture<Void> startFuture;
        if (protocolAdapter.getRuntimeStatus() == ProtocolAdapter.RuntimeStatus.STARTED) {
            startFuture = CompletableFuture.completedFuture(null);
        } else {
            final ProtocolAdapterStartOutputImpl output = new ProtocolAdapterStartOutputImpl();
            startFuture = protocolAdapter.start(new ProtocolAdapterStartInputImpl(protocolAdapter), output);
            startFuture.thenApply(input -> {
                if (!output.startedSuccessfully) {
                    log.warn("Protocol adapter {} could not be started, reason: {}",
                            protocolAdapter.getProtocolAdapterInformation().getDisplayName(),
                            output.message);
                    HiveMQEdgeEvent adapterCreatedEvent = new HiveMQEdgeEvent(HiveMQEdgeEvent.EVENT_TYPE.ADAPTER_ERROR);
                    adapterCreatedEvent.addUserData("adapterType",
                            protocolAdapter.getProtocolAdapterInformation().getProtocolId());
                    remoteService.fireUsageEvent(adapterCreatedEvent);
                } else if (output.message != null) {
                    log.info("Protocol adapter {} started: {}",
                            protocolAdapter.getProtocolAdapterInformation().getDisplayName(),
                            output.message);
                    HiveMQEdgeEvent adapterCreatedEvent = new HiveMQEdgeEvent(HiveMQEdgeEvent.EVENT_TYPE.ADAPTER_STARTED);
                    adapterCreatedEvent.addUserData("adapterType",
                            protocolAdapter.getProtocolAdapterInformation().getProtocolId());
                    remoteService.fireUsageEvent(adapterCreatedEvent);
                }
                return null;
            }).exceptionally(throwable -> {
                log.warn("Protocol adapter failed {} could not be initd, reason: {}",
                        protocolAdapter.getProtocolAdapterInformation().getDisplayName(),
                        output.message);
                return null;
            });;
        }
        return startFuture;
    }

    public synchronized CompletableFuture<Void> addAdapter(
            final @NotNull String adapterType,
            final @NotNull String adapterId,
            final @NotNull Map<String, Object> config,
            boolean start) {
        Preconditions.checkNotNull(adapterType);
        Preconditions.checkNotNull(adapterId);
        if (getAdapterTypeById(adapterType).isEmpty()) {
            throw new IllegalArgumentException("invalid adapter type '" + adapterType + "'");
        }
        if (getAdapterById(adapterId).isPresent()) {
            throw new IllegalArgumentException("adapter already exists by id '" + adapterId + "'");
        }

        return addAdapterAndStartInRuntime(adapterType, config);
    }

    public boolean deleteAdapter(final String id) {
        Preconditions.checkNotNull(id);
        Optional<AdapterInstance> adapterInstance = getAdapterById(id);
        if (adapterInstance.isPresent()) {
            adapterInstance.get().getAdapter().stop().whenComplete(
                    (result, ex) -> adapterInstance.get().getAdapter().close());
            if (protocolAdapters.remove(id) != null) {
                synchronized(lock){
                    Map<String, Object> mainMap =
                            configurationService.protocolAdapterConfigurationService().getAllConfigs();
                    List<Map> adapterList = getAdapterListForType(adapterInstance.get().getAdapterInformation().getProtocolId());
                    if (adapterList != null) {
                        if(adapterList.removeIf(instance -> id.equals(instance.get("id")))){
                            configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    public boolean updateAdapter(final @NotNull String adapterId,
                                 final @NotNull Map<String, Object> config) {
        Preconditions.checkNotNull(adapterId);
        Optional<AdapterInstance> adapterInstance = getAdapterById(adapterId);
        if (adapterInstance.isPresent()) {
            AdapterInstance oldInstance = adapterInstance.get();
            deleteAdapter(oldInstance.getAdapter().getId());
            addAdapter(oldInstance.getAdapter().getProtocolAdapterInformation().getProtocolId(),
                    oldInstance.getAdapter().getId(), config, true);
            return true;
        }
        return false;
    }

    public Optional<AdapterInstance> getAdapterById(final String id) {
        Preconditions.checkNotNull(id);
        Map<String, AdapterInstance> adapters = getProtocolAdapters();
        return Optional.ofNullable(adapters.get(id));
    }

    public Optional<ProtocolAdapterInformation> getAdapterTypeById(final String typeId) {
        Preconditions.checkNotNull(typeId);
        ProtocolAdapterInformation information = getAllAvailableAdapterTypes().get(typeId);
        return Optional.ofNullable(information);
    }

    public @NotNull Map<String, ProtocolAdapterInformation> getAllAvailableAdapterTypes() {
        return configToAdapterMap.values()
                .stream()
                .map(ProtocolAdapterFactory::getInformation)
                .collect(Collectors.toMap(ProtocolAdapterInformation::getProtocolId, o -> o));
    }

    public @NotNull Map<String, AdapterInstance> getProtocolAdapters() {
        return protocolAdapters;
    }

    public ProtocolAdapterSchemaManager getSchemaManager(
            final @NotNull ProtocolAdapterInformation protocolAdapterInformation) {
        Preconditions.checkNotNull(protocolAdapterInformation);
        return new ProtocolAdapterSchemaManager(objectMapper, protocolAdapterInformation);
    }

    protected AdapterInstance createAdapterInstance(final String adapterType, final @NotNull Map<String, Object> config){

        ProtocolAdapterFactory<?> protocolAdapterFactory = getProtocolAdapterFactory(adapterType);
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(protocolAdapterFactory.getClass().getClassLoader());
            final CustomConfig configObject = protocolAdapterFactory.convertConfigObject(objectMapper, config);
            final ProtocolAdapter protocolAdapter =
                    protocolAdapterFactory.createAdapter(protocolAdapterFactory.getInformation(),
                            new ProtocolAdapterInputImpl(configObject, metricRegistry));

            AdapterInstance instance = new AdapterInstance(protocolAdapter,
                    protocolAdapterFactory,
                    protocolAdapterFactory.getInformation(),
                    configObject);
            protocolAdapters.put(instance.getAdapter().getId(), instance);
            return instance;

        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    protected CompletableFuture<Void> addAdapterAndStartInRuntime(final String adapterType, final @NotNull Map<String, Object> config){

        synchronized(lock){
            AdapterInstance instance = createAdapterInstance(adapterType, config);

            //-- Write the protocol adapter back to the main config (through the proxy)
            List<Map> adapterList = getAdapterListForType(adapterType);
            Map<String, Object> mainMap = configurationService.protocolAdapterConfigurationService().getAllConfigs();
            adapterList.add(config);
            configurationService.protocolAdapterConfigurationService().setAllConfigs(mainMap);
            CompletableFuture<Void> future = start(instance.getAdapter());
            return future;
        }
    }

    protected List<Map> getAdapterListForType(final String adapterType){

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
        private final @NotNull T configObject;
        private final @NotNull MetricRegistry metricRegistry;

        public ProtocolAdapterInputImpl(
                final @NotNull T configObject, final @NotNull MetricRegistry metricRegistry) {
            this.configObject = configObject;
            this.metricRegistry = metricRegistry;
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
    }

    private static class ProtocolAdapterStartOutputImpl implements ProtocolAdapterStartOutput {

        //default: all good
        boolean startedSuccessfully = true;
        @Nullable String message = null;

        @Override
        public void startedSuccessfully(@NotNull final String message) {
            startedSuccessfully = true;
            this.message = message;
        }

        @Override
        public void failStart(@NotNull Throwable t, @NotNull final String errorMessage) {
            startedSuccessfully = false;
            this.message = errorMessage;
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
            return new ModuleServicesPerModuleImpl(protocolAdapter, moduleServices);
        }
    }
}
