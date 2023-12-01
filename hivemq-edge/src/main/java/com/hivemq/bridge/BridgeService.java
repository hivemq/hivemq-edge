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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.common.shutdown.HiveMQShutdownHook;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.BridgeConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.model.HiveMQEdgeRemoteEvent;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.util.Checkpoints;
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
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Singleton
public class BridgeService {

    private static final Logger log = LoggerFactory.getLogger(BridgeService.class);

    private final @NotNull BridgeConfigurationService bridgeConfig;
    private final @NotNull MessageForwarder messageForwarder;
    private final @NotNull BridgeMqttClientFactory bridgeMqttClientFactory;
    private final @NotNull ExecutorService executorService;
    private final @NotNull HiveMQEdgeRemoteService remoteService;

    private final Map<String, BridgeMqttClient> bridgeToClientMap = new ConcurrentHashMap<>(0);
    private final Map<String, Throwable> lastErrors = new ConcurrentHashMap<>(0);

    @Inject
    public BridgeService(
            final @NotNull BridgeConfigurationService bridgeConfig,
            final @NotNull MessageForwarder messageForwarder,
            final @NotNull BridgeMqttClientFactory bridgeMqttClientFactory,
            final @NotNull ExecutorService executorService,
            final @NotNull HiveMQEdgeRemoteService remoteService,
            final @NotNull ShutdownHooks shutdownHooks) {
        this.bridgeConfig = bridgeConfig;
        this.messageForwarder = messageForwarder;
        this.bridgeMqttClientFactory = bridgeMqttClientFactory;
        this.executorService = executorService;
        this.remoteService = remoteService;
        shutdownHooks.add(new BridgeShutdownHook(this));
    }


    /**
     * Synchronizes ALL bridges from the config into runtime instances
     */
    public synchronized void updateBridges() {
        //add any new bridges
        final long start = System.currentTimeMillis();
        if (log.isTraceEnabled()) {
            log.trace("Updating bridges {} active connections from {} configured connections",
                    activeBridges().size(),
                    bridgeConfig.getBridges().size());
        }

        for (MqttBridge bridge : bridgeConfig.getBridges()) {
            if (bridgeToClientMap.containsKey(bridge.getId())) {
                continue;
            }
            startBridge(bridge.getId());
        }

        //remove any orphaned connections
        for (String currentBridge : activeBridges()) {
            Optional<MqttBridge> optional = getBridgeByName(currentBridge);
            if (optional.isEmpty()) {
                stopBridgeAndRemoveQueues(currentBridge);
            }
        }

        if (log.isTraceEnabled()) {
            log.trace("Updating bridges complete in {}ms", (System.currentTimeMillis() - start));
        }
    }

    private void stopAllBridges() {
        activeBridges().stream().forEach(bridgeName -> stopBridge(bridgeName, false, List.of()));
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
            BridgeMqttClient client = bridgeToClientMap.get(bridgeName);
            try {
                client.stop();
            } finally {
                log.info("Stopped MQTT bridge '{}' in {}ms", bridgeName, (System.currentTimeMillis() - start));
                bridgeToClientMap.remove(bridgeName);
                try {
                    for (final MqttForwarder forwarder : client.getActiveForwarders()) {
                        boolean clearQueueForThisForwarder =
                                clearQueue && !retainQueueForForwarders.contains(forwarder.getId());
                        messageForwarder.removeForwarder(forwarder, clearQueueForThisForwarder);
                    }
                } catch (Exception e) {
                    log.error("Error Removing MQTT bridge forwarders for '{}'", bridgeName, e);
                }
            }
        }
    }

    public void startBridge(@NotNull final String bridgeName) {
        final long start = System.currentTimeMillis();
        Optional<MqttBridge> bridgeOptional = getBridgeByName(bridgeName);
        if (bridgeOptional.isPresent()) {
            MqttBridge bridge = bridgeOptional.get();
            final BridgeMqttClient bridgeMqttClient;
            if (!bridgeToClientMap.containsKey(bridgeName)) {
                bridgeMqttClient = bridgeMqttClientFactory.createRemoteClient(bridge);
                bridgeToClientMap.put(bridgeName, bridgeMqttClient);
                for (MqttForwarder forwarder : bridgeMqttClient.createForwarders()) {
                    messageForwarder.addForwarder(forwarder);
                }
                Checkpoints.checkpoint("mqtt-bridge-forwarder-started");

            } else {
                bridgeMqttClient = bridgeToClientMap.get(bridgeName);
            }
            if (!bridgeMqttClient.isConnected()) {
                ListenableFuture<Void> future = bridgeMqttClient.start();
                Futures.addCallback(future, new FutureCallback<>() {
                    public void onSuccess(@Nullable Void result) {
                        log.info("Bridge '{}' to remote broker {}:{} started in {}ms.",
                                bridge.getId(),
                                bridge.getHost(),
                                bridge.getPort(),
                                (System.currentTimeMillis() - start));
                        lastErrors.remove(bridge.getId());
                        HiveMQEdgeRemoteEvent startedEvent =
                                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_STARTED);
                        startedEvent.addUserData("cloudBridge",
                                String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
                        startedEvent.addUserData("name", bridgeName);
                        remoteService.fireUsageEvent(startedEvent);
                        Checkpoints.checkpoint("mqtt-bridge-connected");
                    }

                    @Override
                    public void onFailure(final Throwable t) {
                        log.error("Unable To Start Bridge '{}'.", bridge.getId(), t);
                        lastErrors.put(bridge.getId(), t);
                        HiveMQEdgeRemoteEvent errorEvent =
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

    protected @NotNull Optional<MqttBridge> getBridgeByName(@NotNull final String bridgeName) {
        List<MqttBridge> bridges = bridgeConfig.getBridges();
        for (MqttBridge bridge : bridges) {
            if (bridge.getId().equals(bridgeName)) {
                return Optional.of(bridge);
            }
        }
        return Optional.empty();
    }

    public @NotNull Throwable getLastError(@NotNull final String bridgeName) {
        return lastErrors.get(bridgeName);
    }

    public boolean isConnected(@NotNull final String bridgeName) {
        Optional<MqttBridge> bridge = getBridgeByName(bridgeName);
        if (bridge.isPresent()) {
            BridgeMqttClient client;
            if ((client = bridgeToClientMap.get(bridge.get().getId())) != null) {
                return client.isConnected();
            }
        }
        return false;
    }

    public boolean isRunning(@NotNull final String bridgeName) {
        return activeBridges().contains(bridgeName);
    }

    public boolean restartBridge(
            final @NotNull String bridgeName, final @Nullable MqttBridge newBridgeConfig) {
        if (bridgeToClientMap.containsKey(bridgeName)) {
            final List<String> unchangedForwarders = newForwarderIds(newBridgeConfig);
            stopBridge(bridgeName, true, unchangedForwarders);
        }
        Optional<MqttBridge> bridge = getBridgeByName(bridgeName);
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
