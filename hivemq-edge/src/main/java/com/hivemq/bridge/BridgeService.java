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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class BridgeService {

    private static final Logger log = LoggerFactory.getLogger(BridgeService.class);

    private final @NotNull MessageForwarder messageForwarder;
    private final @NotNull BridgeMqttClientFactory bridgeMqttClientFactory;
    private final @NotNull ExecutorService executorService;
    private final @NotNull HiveMQEdgeRemoteService remoteService;

    private final @NotNull Map<String, Throwable> bridgeNameToLastError = new ConcurrentHashMap<>(0);

    private final @NotNull Map<String, MqttBridgeAndClient> activeBridgeNamesToClient = new ConcurrentHashMap<>();
    private final @NotNull Map<String, MqttBridge> allKnownBridgeConfigs = new ConcurrentHashMap<>();

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
        metricRegistry.registerGauge(HiveMQMetrics.BRIDGES_CURRENT.name(), allKnownBridgeConfigs::size);
        shutdownHooks.add(new BridgeShutdownHook(this));
        bridgeConfig.registerConsumer(this::updateBridges);
    }

    /**
     * Synchronizes ALL bridges from the config into runtime instances
     */
    public synchronized void updateBridges(final @NotNull List<MqttBridge> bridges) {

        final var bridgeIdToConfig = bridges.stream().collect(Collectors.toMap(MqttBridge::getId, Function.identity()));

        final var newBridgeIds = bridges.stream().map(MqttBridge::getId).collect(Collectors.toSet());

        final var toRemove = new HashSet<>(allKnownBridgeConfigs.keySet());
        toRemove.removeAll(newBridgeIds);

        final var toAdd = new HashSet<>(newBridgeIds);
        toAdd.removeAll(allKnownBridgeConfigs.keySet());

        final var toUpdate = new HashSet<>(allKnownBridgeConfigs.keySet());
        toUpdate.removeAll(toAdd);
        toUpdate.removeAll(toRemove);



        final long start = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("Updating {} active bridges connections from {} configured connections",
                    activeBridgeNamesToClient.size(),
                    toUpdate.size() + toAdd.size());
        }

        // first stop bridges as they might use the same clientId in case the id of a bridge was changed
        //remove any orphaned connections

        toRemove.forEach(bridgeId -> {
            final var active = activeBridgeNamesToClient.remove(bridgeId);
            allKnownBridgeConfigs.remove(bridgeId);
            if(active != null) {
                log.info("Removing bridge {}", bridgeId);
                internalStopBridge(active, true, List.of());
            } else {
                log.debug("Bridge {} not active", bridgeId);
            }
        });

        toUpdate.forEach(bridgeId -> {
            final var active = activeBridgeNamesToClient.get(bridgeId);
            final var newBridge = bridgeIdToConfig.get(bridgeId);
            if(active != null) {
                if(active.bridge().equals(newBridge)) {
                    log.debug("Not restarting bridge {} because config  is unchanged", bridgeId);
                } else {
                    log.info("Restarting bridge {} because config has changed", bridgeId);
                    allKnownBridgeConfigs.remove(bridgeId);
                    allKnownBridgeConfigs.put(bridgeId, newBridge);
                    internalStopBridge(active, true, List.of());
                    activeBridgeNamesToClient.put(
                            bridgeId,
                            new MqttBridgeAndClient(newBridge, internalStartBridge(newBridge)));
                }
            }
        });

        toAdd.forEach(bridgeId -> {
            final var newBridge = bridgeIdToConfig.get(bridgeId);
            log.info("Adding bridge {}", bridgeId);
            allKnownBridgeConfigs.put(bridgeId, newBridge);
            activeBridgeNamesToClient.put(
                    bridgeId,
                    new MqttBridgeAndClient(newBridge, internalStartBridge(newBridge)));
        });

        if (log.isTraceEnabled()) {
            log.trace("Updating bridges complete in {}ms", (System.currentTimeMillis() - start));
        }
    }


    public @Nullable Throwable getLastError(final @NotNull String bridgeName) {
        return bridgeNameToLastError.get(bridgeName);
    }

    public boolean isConnected(final @NotNull String bridgeName) {
        final var mqttBridgeAndClient = activeBridgeNamesToClient.get(bridgeName);
        if(mqttBridgeAndClient != null) {
            return mqttBridgeAndClient.mqttClient().isConnected();
        }
        return false;
    }

    public boolean isRunning(final @NotNull String bridgeName) {
        return activeBridgeNamesToClient.containsKey(bridgeName);
    }

    public synchronized void stopBridgeAndRemoveQueues(final @NotNull String bridgeName) {
        stopBridge(bridgeName, true, List.of());
    }

    public synchronized void stopBridge(
            final @NotNull String bridgeName,
            final boolean clearQueue,
            final @NotNull List<String> retainQueueForForwarders) {
        final var mqttBridgeAndClient = activeBridgeNamesToClient.remove(bridgeName);
        if (mqttBridgeAndClient != null) {
            log.info("Stopping bridge '{}'", bridgeName);
            internalStopBridge(mqttBridgeAndClient, clearQueue, retainQueueForForwarders);
        } else {
            log.debug("Not stopping bridge '{}' since it wasn't started", bridgeName);
        }
    }

    public synchronized boolean restartBridge(
            final @NotNull String bridgeId, final @Nullable MqttBridge newBridgeConfig) {
        final var bridgeToClient = activeBridgeNamesToClient.get(bridgeId);
        if (bridgeToClient != null) {
            log.info("Restarting bridge '{}'", bridgeId);
            final List<String> unchangedForwarders = newForwarderIds(newBridgeConfig);
            stopBridge(bridgeId, true, unchangedForwarders);
            final var mqttBridgeAndClient = new MqttBridgeAndClient(
                    newBridgeConfig,
                    internalStartBridge(newBridgeConfig != null ? newBridgeConfig : bridgeToClient.bridge()));
            activeBridgeNamesToClient.put(
                    bridgeId,
                    mqttBridgeAndClient);
            if(newBridgeConfig != null) {
                allKnownBridgeConfigs.put(bridgeId, newBridgeConfig);
            }
            return true;
        } else {
            log.debug("Not restarting bridge '{}' since it wasn't active", bridgeId);
            return false;
        }
    }

    public synchronized boolean startBridge(final @NotNull String bridgId) {
        final var bridge = allKnownBridgeConfigs.get(bridgId);
        if (bridge != null && !activeBridgeNamesToClient.containsKey(bridgId)) {
            log.info("Starting bridge '{}'", bridgId);
            final var mqttBridgeAndClient = new MqttBridgeAndClient(
                    bridge,
                    internalStartBridge(bridge));
            activeBridgeNamesToClient.put(
                    bridgId,
                    mqttBridgeAndClient);
            return true;
        } else {
            log.debug("Not starting bridge '{}' since it was already started", bridgId);
            return false;
        }
    }

    private synchronized void internalStopBridge(final @NotNull MqttBridgeAndClient bridgeAndClient,
                                                final boolean clearQueue,
                                                final @NotNull List<String> retainQueueForForwarders) {
        final var start = System.currentTimeMillis();
        final var bridgeId = bridgeAndClient.bridge().getId();
        final var client = bridgeAndClient.mqttClient();
        try {
            bridgeAndClient.mqttClient().stop();
        } finally {
            log.info("Stopped bridge '{}' in {}ms", bridgeId, (System.currentTimeMillis() - start));
            try {
                for (final MqttForwarder forwarder : client.getActiveForwarders()) {
                    final boolean clearQueueForThisForwarder =
                            clearQueue && !retainQueueForForwarders.contains(forwarder.getId());
                    messageForwarder.removeForwarder(forwarder, clearQueueForThisForwarder);
                }
                Checkpoints.checkpoint("mqtt-bridge-stopped");
            } catch (final Exception e) {
                log.error("Error removing bridge forwarders for '{}'", bridgeId, e);
            }
        }
    }

    private BridgeMqttClient internalStartBridgeMqttClient(final @NotNull MqttBridge bridge, final @NotNull BridgeMqttClient bridgeMqttClient) {
        final var start = System.currentTimeMillis();
        final var bridgeId = bridge.getId();
        final ListenableFuture<Void> future = bridgeMqttClient.start();
        Futures.addCallback(future, new FutureCallback<>() {
            public void onSuccess(@Nullable final Void result) {
                log.info("Bridge '{}' to remote broker {}:{} started in {}ms.",
                        bridge.getId(),
                        bridge.getHost(),
                        bridge.getPort(),
                        (System.currentTimeMillis() - start));
                bridgeNameToLastError.remove(bridge.getId());
                final HiveMQEdgeRemoteEvent startedEvent =
                        new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_STARTED);
                startedEvent.addUserData("cloudBridge",
                        String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
                startedEvent.addUserData("name", bridgeId);
                remoteService.fireUsageEvent(startedEvent);
                Checkpoints.checkpoint("mqtt-bridge-connected");
            }

            @Override
            public void onFailure(final @NotNull Throwable t) {
                log.error("Unable oo start bridge '{}'.", bridge.getId(), t);
                bridgeNameToLastError.put(bridge.getId(), t);
                final HiveMQEdgeRemoteEvent errorEvent =
                        new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_ERROR);
                errorEvent.addUserData("cloudBridge",
                        String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
                errorEvent.addUserData("cause", t.getMessage());
                errorEvent.addUserData("name", bridgeId);
                remoteService.fireUsageEvent(errorEvent);
            }
        }, executorService);

        return bridgeMqttClient;
    }

    private BridgeMqttClient internalStartBridge(final @NotNull MqttBridge bridge) {
        final var bridgeId = bridge.getId();
        log.debug("Starting bridge '{}'", bridgeId);
        final BridgeMqttClient bridgeMqttClient = bridgeMqttClientFactory.createRemoteClient(bridge);
        bridgeMqttClient.createForwarders().forEach(messageForwarder::addForwarder);
        Checkpoints.checkpoint("mqtt-bridge-forwarder-started");
        return internalStartBridgeMqttClient(bridge, bridgeMqttClient);
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

    private synchronized void stopAllBridges() {
        activeBridgeNamesToClient.values().forEach(bridge -> internalStopBridge(bridge, false, List.of()));
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

    public record MqttBridgeAndClient(MqttBridge bridge, BridgeMqttClient mqttClient) {}
}
