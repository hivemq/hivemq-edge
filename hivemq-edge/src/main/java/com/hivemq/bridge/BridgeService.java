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
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.hivemq.bridge.config.LocalSubscription;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BridgeService {
    private static final @NotNull Logger log = LoggerFactory.getLogger(BridgeService.class);

    /**
     * How long {@link #updateBridges} waits for a bridge to stop before giving up on it and
     * disconnecting its client the hard way.
     * <p>
     * Generous because a bridge that is mid-reconnect or draining a queue should be allowed to
     * finish rather than be cut off; the forced disconnect below exists for the case where it never
     * will.
     */
    static final long STOP_TIMEOUT_SECONDS = 30;

    /**
     * How long the forced disconnect that follows a stop timeout is itself given.
     */
    static final long FORCED_DISCONNECT_TIMEOUT_SECONDS = 5;

    private final @NotNull MessageForwarder messageForwarder;
    private final @NotNull BridgeMqttClientFactory bridgeMqttClientFactory;
    private final @NotNull ExecutorService executorService;
    private final @NotNull HiveMQEdgeRemoteService remoteService;
    private final @NotNull Map<String, Throwable> bridgeNameToLastError;
    private final @NotNull Map<String, MqttBridgeAndClient> activeBridgeNamesToClient;
    private final @NotNull Map<String, MqttBridge> allKnownBridgeConfigs;

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
        this.bridgeNameToLastError = new ConcurrentHashMap<>();
        this.activeBridgeNamesToClient = new ConcurrentHashMap<>();
        this.allKnownBridgeConfigs = new ConcurrentHashMap<>();
        metricRegistry.registerGauge(HiveMQMetrics.BRIDGES_CURRENT.name(), allKnownBridgeConfigs::size);
        shutdownHooks.add(new BridgeShutdownHook(this));
        bridgeConfig.registerConsumer(this::updateBridges);
    }

    /**
     * How long to wait for a bridge to stop, in seconds.
     * <p>
     * Overridable so that a test can exercise the timeout path without waiting out the production
     * budget: the branch it guards is reached only by letting the wait expire, so a fixed 30 seconds
     * would mean 30 seconds of wall clock per test. Production behaviour is the default.
     */
    long stopTimeoutSeconds() {
        return STOP_TIMEOUT_SECONDS;
    }

    /**
     * How long to wait for the forced disconnect that follows a stop timeout, in seconds.
     */
    long forcedDisconnectTimeoutSeconds() {
        return FORCED_DISCONNECT_TIMEOUT_SECONDS;
    }

    /**
     * Synchronizes ALL bridges from the config into runtime instances
     */
    public synchronized void updateBridges(final @NotNull List<MqttBridge> bridges) {
        if (log.isInfoEnabled()) {
            log.info(
                    "Synchronizing bridge configurations: {} configured bridge(s), {} currently active",
                    bridges.size(),
                    activeBridgeNamesToClient.size());
        }

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
            log.debug(
                    "Bridge synchronization plan: {} to add, {} to update, {} to remove",
                    toAdd.size(),
                    toUpdate.size(),
                    toRemove.size());
        }

        // first stop bridges as they might use the same clientId in case the id of a bridge was changed
        // remove any orphaned connections
        toRemove.forEach(bridgeId -> {
            final var active = activeBridgeNamesToClient.remove(bridgeId);
            allKnownBridgeConfigs.remove(bridgeId);
            if (active != null) {
                log.info("Removing bridge {}", bridgeId);
                internalStopBridge(active, true, List.of());
            } else {
                log.debug("Bridge {} not active", bridgeId);
            }
            // The bridge is gone from the configuration, so any queues held for it while it could not
            // start may be reclaimed. Deliberately not done when a bridge merely stops: a node shutting
            // down would otherwise drop the hold on the way out and let a last clean-up pass delete the
            // messages the operator is about to come back for.
            messageForwarder.releaseReservedQueues(bridgeId);
        });

        toUpdate.forEach(bridgeId -> {
            final var active = activeBridgeNamesToClient.get(bridgeId);
            final var newBridge = bridgeIdToConfig.get(bridgeId);
            if (active != null && newBridge != null) {
                if (active.bridge().equals(newBridge)) {
                    log.debug("Not restarting bridge {} because config is unchanged", bridgeId);
                } else {
                    log.info("Restarting bridge {} because config has changed", bridgeId);
                    allKnownBridgeConfigs.put(bridgeId, newBridge);
                    internalStopBridge(active, true, List.of());
                    activeBridgeNamesToClient.put(
                            bridgeId, new MqttBridgeAndClient(newBridge, internalStartBridge(newBridge)));
                }
            }
        });

        toAdd.forEach(bridgeId -> {
            final var newBridge = bridgeIdToConfig.get(bridgeId);
            if (newBridge == null) {
                log.warn("Bridge config for '{}' not found, skipping", bridgeId);
                return;
            }
            log.info("Adding bridge '{}' ({}:{})", bridgeId, newBridge.getHost(), newBridge.getPort());
            allKnownBridgeConfigs.put(bridgeId, newBridge);
            activeBridgeNamesToClient.put(bridgeId, new MqttBridgeAndClient(newBridge, internalStartBridge(newBridge)));
        });

        final long durationMs = System.currentTimeMillis() - start;
        if (log.isInfoEnabled()) {
            log.info(
                    "Bridge synchronization completed in {} ms: {} added, {} updated, {} removed",
                    durationMs,
                    toAdd.size(),
                    toUpdate.size(),
                    toRemove.size());
        }
    }

    public @Nullable Throwable getLastError(final @NotNull String bridgeName) {
        return bridgeNameToLastError.get(bridgeName);
    }

    public boolean isConnected(final @NotNull String bridgeName) {
        final var client = activeBridgeNamesToClient.get(bridgeName);
        return client != null && client.mqttClient().isConnected();
    }

    public boolean isRunning(final @NotNull String bridgeName) {
        return activeBridgeNamesToClient.containsKey(bridgeName);
    }

    public void stopBridgeAndRemoveQueues(final @NotNull String bridgeName) {
        stopBridge(bridgeName, true, List.of());
        // "and remove queues" is the operator asking for them to go, which for a bridge that never
        // started means dropping the hold that keeps the clean-up off them.
        messageForwarder.releaseReservedQueues(bridgeName);
    }

    public synchronized void stopBridge(
            final @NotNull String bridgeName,
            final boolean clearQueue,
            final @NotNull List<String> retainQueueForForwarders) {
        final var client = activeBridgeNamesToClient.remove(bridgeName);
        if (client != null) {
            log.info("Stopping bridge '{}'", bridgeName);
            internalStopBridge(client, clearQueue, retainQueueForForwarders);
        } else {
            log.debug("Not stopping bridge '{}' since it wasn't started", bridgeName);
        }
    }

    public synchronized boolean restartBridge(
            final @NotNull String bridgeId, final @Nullable MqttBridge newBridgeConfig) {
        final var client = activeBridgeNamesToClient.get(bridgeId);
        if (client != null) {
            log.info("Restarting bridge '{}'", bridgeId);
            stopBridge(bridgeId, true, newForwarderIds(newBridgeConfig));
            final MqttBridge effectiveBridge = newBridgeConfig != null ? newBridgeConfig : client.bridge();
            activeBridgeNamesToClient.put(
                    bridgeId, new MqttBridgeAndClient(effectiveBridge, internalStartBridge(effectiveBridge)));
            if (newBridgeConfig != null) {
                allKnownBridgeConfigs.put(bridgeId, newBridgeConfig);
            }
            return true;
        } else {
            log.debug("Not restarting bridge '{}' since it wasn't active", bridgeId);
            return false;
        }
    }

    public synchronized boolean startBridge(final @NotNull String bridgeId) {
        final var bridge = allKnownBridgeConfigs.get(bridgeId);
        if (bridge != null && !activeBridgeNamesToClient.containsKey(bridgeId)) {
            log.info("Starting bridge '{}'", bridgeId);
            activeBridgeNamesToClient.put(bridgeId, new MqttBridgeAndClient(bridge, internalStartBridge(bridge)));
            return true;
        } else {
            log.debug("Not starting bridge '{}' since it was already started", bridgeId);
            return false;
        }
    }

    private BridgeMqttClient internalStartBridge(final @NotNull MqttBridge bridge) {
        final var bridgeId = bridge.getId();
        if (log.isDebugEnabled()) {
            log.debug(
                    "Initializing bridge '{}' with {} local subscription(s) and {} remote subscription(s)",
                    bridgeId,
                    bridge.getLocalSubscriptions().size(),
                    bridge.getRemoteSubscriptions().size());
        }
        final BridgeMqttClient bridgeMqttClient = bridgeMqttClientFactory.createRemoteClient(bridge);
        try {
            final var forwarders = bridgeMqttClient.createForwarders();
            if (log.isDebugEnabled()) {
                log.debug("Created {} forwarder(s) for bridge '{}'", forwarders.size(), bridgeId);
            }
            forwarders.forEach(messageForwarder::addForwarder);
            // The forwarders own the queues now, so any hold taken by an earlier failed start can go.
            // After the registrations, never before: releasing first would leave the queues unowned for
            // an instant, which is all a clean-up sweep needs to clear them.
            messageForwarder.releaseReservedQueues(bridgeId);
        } catch (final Exception e) {
            // A bridge that cannot register its forwarders must not take the rest of the
            // synchronization down with it: updateBridges iterates over every configured bridge, so an
            // exception escaping here would leave the bridges after this one unstarted. Reported the
            // same way an unsuccessful connect is, which is what surfaces it in the API and the UI.
            //
            // Nothing is cleared on this path, and the queues are held so that nothing else clears them
            // either: with no forwarder registered they read as unowned, and the periodic clean-up
            // deletes unowned forwarder queues within seconds. Without the hold, refusing to start a
            // bridge would destroy the very messages the refusal exists to protect. The hold is dropped
            // when the bridge starts or is removed.
            messageForwarder.reserveQueues(bridgeId, topicsByForwarderId(bridge));
            log.error("Bridge '{}' could not be started: {}", bridgeId, e.getMessage());
            log.debug("Forwarder registration failure details", e);
            bridgeNameToLastError.put(bridgeId, e);
            final HiveMQEdgeRemoteEvent errorEvent =
                    new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_ERROR);
            errorEvent.addUserData(
                    "cloudBridge", String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
            errorEvent.addUserData("cause", e.getMessage());
            errorEvent.addUserData("name", bridgeId);
            remoteService.fireUsageEvent(errorEvent);
            Checkpoints.checkpoint("mqtt-bridge-forwarder-start-failed");
            return bridgeMqttClient;
        }
        Checkpoints.checkpoint("mqtt-bridge-forwarder-started");
        return internalStartBridgeMqttClient(bridge, bridgeMqttClient);
    }

    private BridgeMqttClient internalStartBridgeMqttClient(
            final @NotNull MqttBridge bridge, final @NotNull BridgeMqttClient bridgeMqttClient) {
        final var start = System.currentTimeMillis();
        final var bridgeId = bridge.getId();
        Futures.addCallback(
                bridgeMqttClient.start(),
                new FutureCallback<>() {
                    @Override
                    public void onSuccess(@Nullable final Void result) {
                        log.info(
                                "Bridge '{}' to remote broker {}:{} started in {}ms.",
                                bridge.getId(),
                                bridge.getHost(),
                                bridge.getPort(),
                                (System.currentTimeMillis() - start));
                        bridgeNameToLastError.remove(bridge.getId());
                        final HiveMQEdgeRemoteEvent startedEvent =
                                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_STARTED);
                        startedEvent.addUserData(
                                "cloudBridge", String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
                        startedEvent.addUserData("name", bridgeId);
                        remoteService.fireUsageEvent(startedEvent);
                        Checkpoints.checkpoint("mqtt-bridge-connected");
                    }

                    @Override
                    public void onFailure(final @NotNull Throwable t) {
                        log.error(
                                "Unable to start bridge '{}' to {}:{}: {}",
                                bridge.getId(),
                                bridge.getHost(),
                                bridge.getPort(),
                                t.getMessage());
                        log.debug("Bridge start failure details", t);
                        bridgeNameToLastError.put(bridge.getId(), t);
                        final HiveMQEdgeRemoteEvent errorEvent =
                                new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_ERROR);
                        errorEvent.addUserData(
                                "cloudBridge", String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
                        errorEvent.addUserData("cause", t.getMessage());
                        errorEvent.addUserData("name", bridgeId);
                        remoteService.fireUsageEvent(errorEvent);
                    }
                },
                executorService);
        return bridgeMqttClient;
    }

    private synchronized void internalStopBridge(
            final @NotNull MqttBridgeAndClient bridgeAndClient,
            final boolean clearQueue,
            final @NotNull List<String> retainQueueForForwarders) {
        final var start = System.currentTimeMillis();
        final var bridgeId = bridgeAndClient.bridge().getId();
        final var client = bridgeAndClient.mqttClient();
        final int forwarderCount = client.getActiveForwarders().size();

        if (log.isDebugEnabled()) {
            log.debug(
                    "Stopping bridge '{}': {} forwarder(s), clearQueue={}, retainCount={}",
                    bridgeId,
                    forwarderCount,
                    clearQueue,
                    retainQueueForForwarders.size());
        }

        try {
            bridgeAndClient.mqttClient().stop().get(stopTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while stopping bridge '{}': {}", bridgeId, e.getMessage());
            log.debug("Interrupt exception details", e);
        } catch (final ExecutionException e) {
            log.warn("Execution error while stopping bridge '{}': {}", bridgeId, e.getMessage());
            log.debug("Execution exception details", e);
        } catch (final TimeoutException e) {
            log.warn(
                    "Timeout ({}s) while stopping bridge '{}', attempting forced disconnect",
                    stopTimeoutSeconds(),
                    bridgeId);
            log.debug("Timeout exception details", e);
            try {
                // Attempt forced disconnect on timeout - the underlying client may still have pending reconnections
                client.getMqtt5Client().disconnect().get(forcedDisconnectTimeoutSeconds(), TimeUnit.SECONDS);
                log.info("Forced disconnect of bridge '{}' succeeded", bridgeId);
            } catch (final Exception forcedDisconnectEx) {
                log.error(
                        "Forced disconnect of bridge '{}' failed, client may remain active: {}",
                        bridgeId,
                        forcedDisconnectEx.getMessage());
                log.debug("Forced disconnect exception details", forcedDisconnectEx);
            }
        } finally {
            final long durationMs = System.currentTimeMillis() - start;
            if (log.isInfoEnabled()) {
                log.info("Bridge '{}' stopped in {} ms", bridgeId, durationMs);
            }
            try {
                int removedCount = 0;
                for (final MqttForwarder forwarder : client.getActiveForwarders()) {
                    final boolean shouldClearQueue =
                            clearQueue && !retainQueueForForwarders.contains(forwarder.getId());
                    if (log.isTraceEnabled()) {
                        log.trace(
                                "Removing forwarder '{}' for bridge '{}', clearQueue={}",
                                forwarder.getId(),
                                bridgeId,
                                shouldClearQueue);
                    }
                    messageForwarder.removeForwarder(forwarder, shouldClearQueue);
                    removedCount++;
                }
                if (log.isDebugEnabled()) {
                    log.debug("Removed {} forwarder(s) for bridge '{}'", removedCount, bridgeId);
                }
                Checkpoints.checkpoint("mqtt-bridge-stopped");
            } catch (final Throwable e) {
                log.error("Error removing forwarders for bridge '{}': {}", bridgeId, e.getMessage());
                log.debug("Forwarder removal exception details", e);
            }
        }
    }

    /**
     * The topics each of a bridge's forwarders would register, keyed by forwarder id.
     * <p>
     * Merged rather than overwritten on a repeated key: two local subscriptions of one bridge can
     * derive the same forwarder id — that collision is why the bridge is being refused in the first
     * place — and the queues of both must be held, not just those of whichever came last.
     */
    private static @NotNull Map<String, List<String>> topicsByForwarderId(final @NotNull MqttBridge bridge) {
        final Map<String, List<String>> topicsByForwarderId = new HashMap<>();
        for (final LocalSubscription subscription : bridge.getLocalSubscriptions()) {
            topicsByForwarderId.merge(
                    BridgeMqttClient.createForwarderId(bridge.getId(), subscription),
                    List.copyOf(subscription.getFilters()),
                    (existing, added) -> ImmutableList.<String>builder()
                            .addAll(existing)
                            .addAll(added)
                            .build());
        }
        return topicsByForwarderId;
    }

    private @NotNull List<String> newForwarderIds(final @Nullable MqttBridge newBridgeConfig) {
        return newBridgeConfig != null
                ? newBridgeConfig.getLocalSubscriptions().stream()
                        .map(localSubscription ->
                                BridgeMqttClient.createForwarderId(newBridgeConfig.getId(), localSubscription))
                        .toList()
                : List.of();
    }

    private synchronized void stopAllBridges() {
        final int bridgeCount = activeBridgeNamesToClient.size();
        if (bridgeCount > 0) {
            log.info("Stopping all {} active bridge(s) for shutdown", bridgeCount);
            activeBridgeNamesToClient.values().forEach(bridge -> internalStopBridge(bridge, false, List.of()));
            log.info("All bridges stopped");
        } else {
            log.debug("No active bridges to stop");
        }
    }

    private record BridgeShutdownHook(@NotNull BridgeService bridgeService) implements HiveMQShutdownHook {
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
