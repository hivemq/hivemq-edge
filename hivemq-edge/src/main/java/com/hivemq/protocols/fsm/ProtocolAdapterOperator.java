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
import com.google.common.collect.Sets;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ProtocolAdapterExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.VersionProvider;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import com.hivemq.protocols.ProtocolAdapterMetrics;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ProtocolAdapterOperator {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolAdapterOperator.class);

    private final @NotNull Map<String, ProtocolAdapterInstance> protocolAdapterMap;
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
    private volatile @NotNull ProtocolAdapterOperatorState state;

    public ProtocolAdapterOperator(
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
        this.protocolAdapterMap = new ConcurrentHashMap<>();
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
        this.state = ProtocolAdapterOperatorState.Idle;
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
        protocolAdapterWritingService.addWritingChangedCallback(() -> protocolAdapterFactoryManager.writingEnabledChanged(
                protocolAdapterWritingService.writingEnabled()));
    }

    public @NotNull ProtocolAdapterOperatorState getState() {
        return state;
    }

    public void start() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Starting adapters");
        }
        protocolAdapterConfig.registerConsumer(this::refresh);
    }

    public void refresh(final @NotNull List<ProtocolAdapterEntity> configs) {
        executorService.submit(() -> {
            state = ProtocolAdapterOperatorState.Running;
            LOGGER.info("Refreshing adapters");

            final Map<String, ProtocolAdapterConfig> protocolAdapterConfigs = configs.stream()
                    .map(configConverter::fromEntity)
                    .collect(Collectors.toMap(ProtocolAdapterConfig::getAdapterId, Function.identity()));

            final Set<String> oldProtocolAdapterIdSet = new HashSet<>(protocolAdapterMap.keySet());
            final Set<String> newProtocolAdapterIdSet = new HashSet<>(protocolAdapterConfigs.keySet());

            final Set<String> toBeDeletedProtocolAdapterIdSet =
                    new HashSet<>(Sets.difference(oldProtocolAdapterIdSet, newProtocolAdapterIdSet));
            final Set<String> toBeCreatedProtocolAdapterIdSet =
                    new HashSet<>(Sets.difference(newProtocolAdapterIdSet, oldProtocolAdapterIdSet));
            final Set<String> toBeUpdatedProtocolAdapterIdSet =
                    new HashSet<>(Sets.intersection(newProtocolAdapterIdSet, oldProtocolAdapterIdSet));

            final List<String> failedAdapters = new ArrayList<>();

            toBeDeletedProtocolAdapterIdSet.forEach(adapterId -> {
//                try {
//                    if (LOGGER.isDebugEnabled()) {
//                        LOGGER.debug("Deleting adapter '{}'", adapterId);
//                    }
//                    stopAsync(adapterId, true).whenComplete((ignored, t) -> deleteAdapterInternal(adapterId)).get();
//                } catch (final InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    failedAdapters.add(adapterId);
//                    LOGGER.error("Interrupted while deleting adapter {}", adapterId, e);
//                } catch (final ExecutionException e) {
//                    failedAdapters.add(adapterId);
//                    LOGGER.error("Failed deleting adapter {}", adapterId, e);
//                }
            });

            toBeCreatedProtocolAdapterIdSet.forEach(name -> {
//                try {
//                    if (LOGGER.isDebugEnabled()) {
//                        LOGGER.debug("Creating adapter '{}'", name);
//                    }
//                    startAsync(createAdapterInternal(protocolAdapterConfigs.get(name),
//                            versionProvider.getVersion())).get();
//                } catch (final InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    failedAdapters.add(name);
//                    LOGGER.error("Interrupted while adding adapter {}", name, e);
//                } catch (final ExecutionException e) {
//                    failedAdapters.add(name);
//                    LOGGER.error("Failed adding adapter {}", name, e);
//                }
            });

            toBeUpdatedProtocolAdapterIdSet.forEach(name -> {
//                try {
//                    final var wrapper = protocolAdapters.get(name);
//                    if (wrapper == null) {
//                        LOGGER.error(
//                                "Existing adapters were modified while a refresh was ongoing, adapter with name '{}' was deleted and could not be updated",
//                                name);
//                    }
//                    if (wrapper != null && !protocolAdapterConfigs.get(name).equals(wrapper.getConfig())) {
//                        if (LOGGER.isDebugEnabled()) {
//                            LOGGER.debug("Updating adapter '{}'", name);
//                        }
//                        stopAsync(name, true).thenApply(v -> {
//                                    deleteAdapterInternal(name);
//                                    return null;
//                                })
//                                .thenCompose(ignored -> startAsync(createAdapterInternal(protocolAdapterConfigs.get(name),
//                                        versionProvider.getVersion())))
//                                .get();
//                    } else {
//                        if (LOGGER.isDebugEnabled()) {
//                            LOGGER.debug("Not-updating adapter '{}' since the config is unchanged", name);
//                        }
//                    }
//                } catch (final InterruptedException e) {
//                    Thread.currentThread().interrupt();
//                    failedAdapters.add(name);
//                    LOGGER.error("Interrupted while updating adapter {}", name, e);
//                } catch (final ExecutionException e) {
//                    failedAdapters.add(name);
//                    LOGGER.error("Failed updating adapter {}", name, e);
//                }
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
            state = ProtocolAdapterOperatorState.Idle;
        });
    }
}
