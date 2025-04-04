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
package com.hivemq.bridge;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.metrics.HiveMQMetrics;
import com.hivemq.util.Checkpoints;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Singleton
public class BridgeService {

    private static final Logger log = LoggerFactory.getLogger(BridgeService.class);

    private final @NotNull MessageForwarder messageForwarder;
    private final @NotNull BridgeMqttClientFactory bridgeMqttClientFactory;
    private final @NotNull ExecutorService executorService;
    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull MetricRegistry metricRegistry;

    private final Map<String, BridgeMqttClient> bridgeToClientMap = new ConcurrentHashMap<>(0);
    private final Map<String, Throwable> lastErrors = new ConcurrentHashMap<>(0);

    private volatile @Nullable List<MqttBridge> bridges;

    @Inject
    public BridgeService(
            final @NotNull BridgeExtractor bridgeConfig,
            final @NotNull MessageForwarder messageForwarder,
            final @NotNull BridgeMqttClientFactory bridgeMqttClientFactory,
            final @NotNull ExecutorService executorService,
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull MetricRegistry metricRegistry) {
        this.messageForwarder = messageForwarder;
        this.bridgeMqttClientFactory = bridgeMqttClientFactory;
        this.executorService = executorService;
        this.remoteService = remoteService;
        this.metricRegistry = metricRegistry;
        metricRegistry.registerGauge(HiveMQMetrics.BRIDGES_CURRENT.name(), () -> bridgeToClientMap.keySet().size());
        shutdownHooks.add(new BridgeShutdownHook(this));
        bridgeConfig.registerConsumer(this::updateBridges);
    }


    /**
     * Synchronizes ALL bridges from the config into runtime instances
     */
    public synchronized void updateBridges(final @NotNull List<MqttBridge> bridges) {
        this.bridges = new CopyOnWriteArrayList<>(bridges);
        log.info("Refreshing bridges " +bridges);

        final long start = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("Updating bridges {} active connections from {} configured connections",
                    activeBridges().size(),
                    bridges.size());
        }

        // first stop bridges as they might use the same clientId in case the id of a bridge was changed
        //remove any orphaned connections
        for (final String currentBridge : activeBridges()) {
            final Optional<MqttBridge> optional = getBridgeByName(currentBridge);
            if (optional.isEmpty()) {
                log.info("Removing bridge {}", currentBridge);
                stopBridgeAndRemoveQueues(currentBridge);
            }
        }

        //add any new bridges
        for (final MqttBridge bridge : bridges) {
            if (bridgeToClientMap.containsKey(bridge.getId())) {
                continue;
            }
            log.info("Adding bridge {}", bridge.getId());
            startBridge(bridge.getId());
        }

        if (log.isTraceEnabled()) {
            log.trace("Updating bridges complete in {}ms", (System.currentTimeMillis() - start));
        }
    }

    private void stopAllBridges() {
        activeBridges().forEach(bridgeName -> stopBridge(bridgeName, false, List.of()));
    }


    public synchronized void stopBridgeAndRemoveQueues(final @NotNull String bridgeName) {
        stopBridge(bridgeName, true, List.of());
    }


    public synchronized void stopBridge(
            final @NotNull String bridgeName,
            final boolean clearQueue,
            final @NotNull List<String> retainQueueForForwarders) {
        final long start = System.currentTimeMillis();
        log.info("Stopping MQTT bridge '{}'", bridgeName);
        if (bridgeToClientMap.containsKey(bridgeName)) {
            final BridgeMqttClient client = bridgeToClientMap.get(bridgeName);
            try {
                client.stop();
            } finally {
                log.info("Stopped MQTT bridge '{}' in {}ms", bridgeName, (System.currentTimeMillis() - start));
                bridgeToClientMap.remove(bridgeName);
                try {
                    for (final MqttForwarder forwarder : client.getActiveForwarders()) {
                        final boolean clearQueueForThisForwarder =
                                clearQueue && !retainQueueForForwarders.contains(forwarder.getId());
                        messageForwarder.removeForwarder(forwarder, clearQueueForThisForwarder);
                    }
                } catch (final Exception e) {
                    log.error("Error Removing MQTT bridge forwarders for '{}'", bridgeName, e);
                }
            }
        }
    }

    public void startBridge(final @NotNull String bridgeName) {
        final long start = System.currentTimeMillis();
        final Optional<MqttBridge> bridgeOptional = getBridgeByName(bridgeName);
        if (bridgeOptional.isPresent()) {
            final MqttBridge bridge = bridgeOptional.get();
            final BridgeMqttClient bridgeMqttClient;
            if (!bridgeToClientMap.containsKey(bridgeName)) {
                bridgeMqttClient = bridgeMqttClientFactory.createRemoteClient(bridge);
                bridgeToClientMap.put(bridgeName, bridgeMqttClient);
                for (final MqttForwarder forwarder : bridgeMqttClient.createForwarders()) {
                    messageForwarder.addForwarder(forwarder);
                }
                Checkpoints.checkpoint("mqtt-bridge-forwarder-started");

            } else {
                bridgeMqttClient = bridgeToClientMap.get(bridgeName);
            }
            if (!bridgeMqttClient.isConnected()) {
                final ListenableFuture<Void> future = bridgeMqttClient.start();
                Futures.addCallback(future, new FutureCallback<>() {
                    public void onSuccess(@Nullable final Void result) {
                        log.info("Bridge '{}' to remote broker {}:{} started in {}ms.",
                                bridge.getId(),
                                bridge.getHost(),
                                bridge.getPort(),
                                (System.currentTimeMillis() - start));
                        lastErrors.remove(bridge.getId());
                        final HiveMQEdgeRemoteEvent startedEvent =
                                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_STARTED);
                        startedEvent.addUserData("cloudBridge",
                                String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
                        startedEvent.addUserData("name", bridgeName);
                        remoteService.fireUsageEvent(startedEvent);
                        Checkpoints.checkpoint("mqtt-bridge-connected");
                    }

                    @Override
                    public void onFailure(final @NotNull Throwable t) {
                        log.error("Unable To Start Bridge '{}'.", bridge.getId(), t);
                        lastErrors.put(bridge.getId(), t);
                        final HiveMQEdgeRemoteEvent errorEvent =
                                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_ERROR);
                        errorEvent.addUserData("cloudBridge",
                                String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
                        errorEvent.addUserData("cause", t.getMessage());
                        errorEvent.addUserData("name", bridgeName);
                        remoteService.fireUsageEvent(errorEvent);
                    }
                }, executorService);
            }
        }
    }

    //simply copy to handle iteration modification
    protected @NotNull Set<String> activeBridges() {
        return Collections.unmodifiableSet(bridgeToClientMap.keySet());
    }

    protected @NotNull Optional<MqttBridge> getBridgeByName(final @NotNull String bridgeName) {
        final var tmpBridges = bridges;
        if (tmpBridges != null) {
            for (final MqttBridge bridge : tmpBridges) {
                if (bridge.getId().equals(bridgeName)) {
                    return Optional.of(bridge);
                }
            }
        }
        return Optional.empty();
    }

    public @Nullable Throwable getLastError(final @NotNull String bridgeName) {
        return lastErrors.get(bridgeName);
    }

    public boolean isConnected(final @NotNull String bridgeName) {
        final Optional<MqttBridge> bridge = getBridgeByName(bridgeName);
        if (bridge.isPresent()) {
            final BridgeMqttClient client;
            if ((client = bridgeToClientMap.get(bridge.get().getId())) != null) {
                return client.isConnected();
            }
        }
        return false;
    }

    public boolean isRunning(final @NotNull String bridgeName) {
        return activeBridges().contains(bridgeName);
    }

    public boolean restartBridge(
            final @NotNull String bridgeName, final @Nullable MqttBridge newBridgeConfig) {
        if (bridgeToClientMap.containsKey(bridgeName)) {
            final List<String> unchangedForwarders = newForwarderIds(newBridgeConfig);
            stopBridge(bridgeName, true, unchangedForwarders);
        }
        final Optional<MqttBridge> bridge = getBridgeByName(bridgeName);
        if (bridge.isPresent()) {
            startBridge(bridgeName);
            return true;
        }
        return false;
    }

    private @NotNull List<String> newForwarderIds(final @Nullable MqttBridge newBridgeConfig) {
        if (newBridgeConfig == null) {
            return List.of();
        }

        return newBridgeConfig.getLocalSubscriptions()
                .stream()
                .map(localSubscription -> BridgeMqttClient.createForwarderId(newBridgeConfig.getId(),
                        localSubscription))
                .collect(Collectors.toList());
    }

    private static class BridgeShutdownHook implements HiveMQShutdownHook {

        private final @NotNull BridgeService bridgeService;

        private BridgeShutdownHook(final @NotNull BridgeService bridgeService) {
            this.bridgeService = bridgeService;
        }

        @Override
        public @NotNull String name() {
            return "MQTT Bridge shutdown";
        }

        @Override
        public void run() {
            bridgeService.stopAllBridges();
        }

        @Override
        public @NotNull Priority priority() {
            return Priority.HIGH;
        }
    }
}
