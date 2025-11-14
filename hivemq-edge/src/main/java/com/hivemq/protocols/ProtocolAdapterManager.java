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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ADAPTER_MISSING;
import static com.hivemq.persistence.domain.DomainTagAddResult.DomainTagPutStatus.ALREADY_EXISTS;

@SuppressWarnings("unchecked")
@Singleton
public class ProtocolAdapterManager {
    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterManager.class);

    private final @NotNull Map<String, ProtocolAdapterWrapper> protocolAdapters;
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
        this.protocolAdapters = new ConcurrentHashMap<>();
        this.executorService = Executors.newSingleThreadExecutor();
        Runtime.getRuntime().addShutdownHook(new Thread(executorService::shutdown));
        protocolAdapterWritingService.addWritingChangedCallback(() -> protocolAdapterFactoryManager.writingEnabledChanged(
                protocolAdapterWritingService.writingEnabled()));
    }

    // API, must be threadsafe

    private static void syncFuture(final @NotNull Future<?> future) {
        try {
            future.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while async execution: ", e.getCause());
        } catch (final ExecutionException e) {
            log.error("Exception happened while async execution: ", e.getCause());
        }
    }

    public static <T> @NotNull T runWithContextLoader(
            final @NotNull ClassLoader contextLoader,
            final @NotNull Supplier<T> wrapperSupplier) {
        final ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(contextLoader);
            return wrapperSupplier.get();
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    public void start() {
        if (log.isDebugEnabled()) {
            log.debug("Starting adapters");
        }
        protocolAdapterConfig.registerConsumer(this::refresh);
    }

    @VisibleForTesting
    public void shutdown() {
        protocolAdapters.values().forEach(entry -> {
            try {
                entry.stopAsync(true).get(5, TimeUnit.SECONDS);
            } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                log.error("Exception happened while shutting down adapter: ", e);
            }
        });
    }

    public void refresh(final @NotNull List<ProtocolAdapterEntity> configs) {
        executorService.submit(() -> {
            log.info("Refreshing adapters");

            final Map<String, ProtocolAdapterConfig> protocolAdapterConfigs = configs.stream()
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
                    if (log.isDebugEnabled()) {
                        log.debug("Deleting adapter '{}'", name);
                    }
                    stopAsync(name, true).whenComplete((ignored, t) -> deleteAdapterInternal(name)).get();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedAdapters.add(name);
                    log.error("Interrupted while deleting adapter {}", name, e);
                } catch (final ExecutionException e) {
                    failedAdapters.add(name);
                    log.error("Failed deleting adapter {}", name, e);
                }
            });

            adaptersToBeCreated.forEach(name -> {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Creating adapter '{}'", name);
                    }
                    startAsync(createAdapterInternal(protocolAdapterConfigs.get(name),
                            versionProvider.getVersion())).get();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedAdapters.add(name);
                    log.error("Interrupted while adding adapter {}", name, e);
                } catch (final ExecutionException e) {
                    failedAdapters.add(name);
                    log.error("Failed adding adapter {}", name, e);
                }
            });

            adaptersToBeUpdated.forEach(name -> {
                try {
                    final var wrapper = protocolAdapters.get(name);
                    if (wrapper == null) {
                        log.error(
                                "Existing adapters were modified while a refresh was ongoing, adapter with name '{}' was deleted and could not be updated",
                                name);
                    }
                    if (wrapper != null && !protocolAdapterConfigs.get(name).equals(wrapper.getConfig())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Updating adapter '{}'", name);
                        }
                        stopAsync(name, true).thenApply(v -> {
                                    deleteAdapterInternal(name);
                                    return null;
                                })
                                .thenCompose(ignored -> startAsync(createAdapterInternal(protocolAdapterConfigs.get(name),
                                        versionProvider.getVersion())))
                                .get();
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Not-updating adapter '{}' since the config is unchanged", name);
                        }
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failedAdapters.add(name);
                    log.error("Interrupted while updating adapter {}", name, e);
                } catch (final ExecutionException e) {
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
        });
    }

    //INTERNAL

    public boolean protocolAdapterFactoryExists(final @NotNull String protocolAdapterType) {
        Preconditions.checkNotNull(protocolAdapterType);
        return protocolAdapterFactoryManager.get(protocolAdapterType).isPresent();
    }

    public @NotNull CompletableFuture<Void> startAsync(final @NotNull String protocolAdapterId) {
        Preconditions.checkNotNull(protocolAdapterId);
        return getProtocolAdapterWrapperByAdapterId(protocolAdapterId)
                .map(this::startAsync)
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    public @NotNull CompletableFuture<Void> stopAsync(final @NotNull String protocolAdapterId, final boolean destroy) {
        Preconditions.checkNotNull(protocolAdapterId);
        return getProtocolAdapterWrapperByAdapterId(protocolAdapterId).map(wrapper -> stopAsync(wrapper, destroy))
                .orElseGet(() -> CompletableFuture.failedFuture(new ProtocolAdapterException("Adapter '" +
                        protocolAdapterId +
                        "'not found.")));
    }

    private @NotNull ProtocolAdapterWrapper createAdapterInternal(
            final @NotNull ProtocolAdapterConfig config,
            final @NotNull String version) {
        return protocolAdapters.computeIfAbsent(config.getAdapterId(), ignored -> {

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

            log.info("Found configuration for adapter {} / {}", config.getAdapterId(), adapterType);
            config.missingTags().ifPresent(missing -> {
                throw new IllegalArgumentException("Tags used in mappings but not configured in adapter " +
                        config.getAdapterId() +
                        ": " +
                        missing);
            });

            return runWithContextLoader(factory.getClass().getClassLoader(), () -> {

                final ProtocolAdapterMetricsService metricsService =
                        new ProtocolAdapterMetricsServiceImpl(configProtocolId, config.getAdapterId(), metricRegistry);

                final ProtocolAdapterStateImpl state =
                        new ProtocolAdapterStateImpl(moduleServices.eventService(), config.getAdapterId(), configProtocolId);

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
                return new ProtocolAdapterWrapper(metricsService,
                        protocolAdapterWritingService,
                        protocolAdapterPollingService,
                        config,
                        protocolAdapter,
                        factory,
                        factory.getInformation(),
                        state,
                        northboundConsumerFactory,
                        tagManager);
            });
        });
    }

    private void deleteAdapterInternal(final @NotNull String adapterId) {
        final ProtocolAdapterWrapper wrapper = protocolAdapters.remove(adapterId);
        if (wrapper != null) {
            protocolAdapterMetrics.decreaseProtocolAdapterMetric(wrapper.getAdapterInformation().getProtocolId());
            eventService.createAdapterEvent(adapterId, wrapper.getProtocolAdapterInformation().getProtocolId())
                    .withSeverity(Event.SEVERITY.WARN)
                    .withMessage("Adapter '" + adapterId + "' was deleted from the system permanently.")
                    .fire();
        } else {
            log.warn("Tried to delete adapter '{}' but it was not found in the system.", adapterId);
        }
    }

    @VisibleForTesting
    @NotNull CompletableFuture<Void> startAsync(final @NotNull ProtocolAdapterWrapper wrapper) {
        Preconditions.checkNotNull(wrapper);
        final String wid = wrapper.getId();
        log.info("Starting protocol-adapter '{}'.", wid);
        return wrapper
                .startAsync(writingEnabled(), moduleServices)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Protocol-adapter '{}' started successfully.", wid);
                        fireEvent(wrapper,
                                HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_STARTED,
                                Event.SEVERITY.INFO,
                                "Adapter '" + wid + "' started OK.");
                    } else {
                        log.warn("Protocol-adapter '{}' could not be started, reason: {}", wid, "unknown");
                        fireEvent(wrapper,
                                HiveMQEdgeRemoteEvent.EVENT_TYPE.ADAPTER_ERROR,
                                Event.SEVERITY.CRITICAL,
                                "Error starting adapter '" + wid + "'.");
                    }
                });
    }

    private void fireEvent(
            final @NotNull ProtocolAdapterWrapper wrapper,
            final @NotNull HiveMQEdgeRemoteEvent.EVENT_TYPE eventType,
            final @NotNull Event.SEVERITY severity,
            final @NotNull String message) {
        final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
        final HiveMQEdgeRemoteEvent event = new HiveMQEdgeRemoteEvent(eventType);
        event.addUserData("adapterType", protocolId);
        remoteService.fireUsageEvent(event);
        eventService.createAdapterEvent(wrapper.getId(), protocolId).withSeverity(severity).withMessage(message).fire();
    }

    @VisibleForTesting
    @NotNull CompletableFuture<Void> stopAsync(final @NotNull ProtocolAdapterWrapper wrapper, final boolean destroy) {
        Preconditions.checkNotNull(wrapper);
        log.info("Stopping protocol-adapter '{}'.", wrapper.getId());

        return wrapper
                .stopAsync(destroy)
                .thenApply(v -> {
                    wrapper.setRuntimeStatus(ProtocolAdapterState.RuntimeStatus.STOPPED);
                    return v;
                })
                .whenComplete((result, throwable) -> {
                    final Event.SEVERITY severity;
                    final String message;
                    final String wid = wrapper.getId();
                    final String protocolId = wrapper.getProtocolAdapterInformation().getProtocolId();
                    if (throwable == null) {
                        log.info("Protocol-adapter '{}' stopped successfully.", wid);
                        severity = Event.SEVERITY.INFO;
                        message = "Adapter '" + wid + "' stopped OK.";
                    } else {
                        log.warn("Protocol-adapter '{}' was unable to stop cleanly", wrapper.getId());
                        severity = Event.SEVERITY.CRITICAL;
                        message = "Error stopping adapter '" + wid + "'.";
                    }
                    eventService.createAdapterEvent(wid, protocolId).withSeverity(severity).withMessage(message).fire();
                });
    }

    private void updateAdapter(final ProtocolAdapterConfig protocolAdapterConfig) {
        deleteAdapterInternal(protocolAdapterConfig.getAdapterId());
        syncFuture(startAsync(createAdapterInternal(protocolAdapterConfig, versionProvider.getVersion())));
    }

    private boolean updateAdapterTags(final @NotNull String adapterId, final @NotNull List<? extends Tag> tags) {
        Preconditions.checkNotNull(adapterId);
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(wrapper -> {
            final var protocolId = wrapper.getAdapterInformation().getProtocolId();
            final var protocolAdapterConfig = new ProtocolAdapterConfig(wrapper.getId(),
                    protocolId,
                    wrapper.getAdapterInformation().getCurrentConfigVersion(),
                    wrapper.getConfigObject(),
                    wrapper.getSouthboundMappings(),
                    wrapper.getNorthboundMappings(),
                    tags);
            updateAdapter(protocolAdapterConfig);
            return true;
        }).orElse(false);
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
            final @NotNull String adapterId,
            final @NotNull DomainTag domainTag) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(wrapper -> {
            final var tags = new ArrayList<>(wrapper.getTags());
            final boolean alreadyExists = tags.stream().anyMatch(t -> t.getName().equals(domainTag.getTagName()));
            if (!alreadyExists) {
                tags.add(configConverter.domainTagToTag(wrapper.getProtocolAdapterInformation().getProtocolId(),
                        domainTag));

                updateAdapterTags(adapterId, tags);
                return DomainTagAddResult.success();
            } else {
                return DomainTagAddResult.failed(ALREADY_EXISTS, adapterId);
            }
        }).orElse(DomainTagAddResult.failed(ADAPTER_MISSING, adapterId));
    }

    public @NotNull List<DomainTag> getDomainTags() {
        return protocolAdapters.values()
                .stream()
                .flatMap(wrapper -> wrapper.getTags()
                        .stream()
                        .map(tag -> new DomainTag(tag.getName(),
                                wrapper.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .toList();
    }

    public @NotNull Optional<DomainTag> getDomainTagByName(final @NotNull String tagName) {
        return protocolAdapters.values()
                .stream()
                .flatMap(wrapper -> wrapper.getTags()
                        .stream()
                        .filter(t -> t.getName().equals(tagName))
                        .map(tag -> new DomainTag(tag.getName(),
                                wrapper.getId(),
                                tag.getDescription(),
                                configConverter.convertTagDefinitionToJsonNode(tag.getDefinition()))))
                .findFirst();
    }

    public @NotNull Optional<List<DomainTag>> getTagsForAdapter(final @NotNull String adapterId) {
        return getProtocolAdapterWrapperByAdapterId(adapterId).map(adapterToConfig -> adapterToConfig.getTags()
                .stream()
                .map(tag -> new DomainTag(tag.getName(),
                        adapterToConfig.getId(),
                        tag.getDescription(),
                        configConverter.convertTagDefinitionToJsonNode(tag.getDefinition())))
                .toList());
    }
}
