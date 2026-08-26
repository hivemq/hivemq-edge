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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
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

        // Claimed before the gate below can open, so that both ways out of the synchronization are safe.
        //
        // The gate is a latch: it only ever transitions on the first call, and on that call
        // allKnownBridgeConfigs is still empty, so toRemove and toUpdate are empty and toAdd is every
        // configured bridge. Each of them claims its own queues inside internalStartBridge -- but only
        // when its turn comes, and the finally below runs whether or not every turn came. Anything
        // escaping synchronizeBridges part way (an Error, or a throw from a path internalStartBridge's
        // catch does not cover) therefore used to open the gate with the bridges after it neither
        // registered nor held, and their backlog from before the restart was reclaimable within minutes
        // (EDG-882 review v02, R2-03).
        //
        // Restricted to toAdd rather than every inactive bridge: it is the whole at-risk set by the
        // reasoning above, and a hold is not free -- IncomingSubscribeService refuses a client's
        // SUBSCRIBE to a held queue's group, which on every later call, with the gate already open,
        // would cost something and buy nothing.
        //
        // internalStartBridge reserves under the same id, so the hold is superseded rather than
        // duplicated, and released once that bridge's forwarders have registered. A bridge whose turn
        // never came keeps it, exactly as a bridge whose start failed does: held until it starts or
        // leaves the configuration. That is a leak in place of a deletion, which is the trade this
        // ticket exists to make.
        for (final String bridgeId : toAdd) {
            final var bridge = bridgeIdToConfig.get(bridgeId);
            if (bridge != null) {
                messageForwarder.reserveQueues(bridgeId, topicsByForwarderId(bridge));
            }
        }

        try {
            synchronizeBridges(bridgeIdToConfig, toRemove, toUpdate, toAdd);
        } finally {
            // From here on, a forwarder queue nobody owns is genuinely orphaned. Before it, ownership
            // had not been claimed yet -- the clean-up service is scheduled during persistence
            // bootstrap, well before this runs -- and a sweep in that window deletes the queues of
            // every bridge on the node while they wait to be started.
            //
            // In a finally block because the alternative failure is silent and permanent: anything
            // escaping the synchronization left the gate closed for the life of the node, and forwarder
            // queues were then never reclaimed at all -- a storage leak in place of a message loss.
            // Safe in both directions now that the queues above are held before it can run.
            messageForwarder.markBridgeConfigurationApplied();
        }

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

    private void synchronizeBridges(
            final @NotNull Map<String, MqttBridge> bridgeIdToConfig,
            final @NotNull Set<String> toRemove,
            final @NotNull Set<String> toUpdate,
            final @NotNull Set<String> toAdd) {
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
            if (newBridge == null) {
                return;
            }
            // Recorded whether or not the bridge is running. Only restartBridge used to write this map,
            // so a bridge stopped through the API kept the configuration it was stopped with: a later
            // start ran a stale subscription set, and the next reload then saw the difference as a
            // change and cleared the queues of the subscriptions that "disappeared" (EDG-882 QA round 1).
            final var previous = allKnownBridgeConfigs.put(bridgeId, newBridge);
            if (active == null) {
                if (newBridge.equals(previous)) {
                    return;
                }
                if (bridgeNameToLastError.containsKey(bridgeId)) {
                    // The bridge failed to start and its configuration has just changed: an operator
                    // editing a bridge that is reporting an error is asking for it to be tried again,
                    // and until this branch existed the correction was recorded and never acted on --
                    // the queues held for the failed bridge stayed held and nothing forwarded them
                    // (EDG-882 QA round 3).
                    log.info("Retrying bridge '{}' with the corrected configuration", bridgeId);
                    final var retried = internalStartBridge(newBridge);
                    if (retried != null) {
                        activeBridgeNamesToClient.put(bridgeId, new MqttBridgeAndClient(newBridge, retried));
                    }
                } else {
                    log.info(
                            "Bridge '{}' is not running; its new configuration takes effect when it is started",
                            bridgeId);
                }
                return;
            }
            if (active.bridge().equals(newBridge)) {
                log.debug("Not restarting bridge {} because config is unchanged", bridgeId);
            } else {
                log.info("Restarting bridge {} because config has changed", bridgeId);
                // Through restartBridge rather than stop-with-an-empty-retain-list, which cleared
                // every queue of the bridge whatever had changed: editing one subscription's filter
                // threw away the messages queued for all the others, and a bridge with one busy
                // subscription and one being tuned lost the busy one's backlog on every save
                // (EDG-882 F-07). restartBridge keeps the queues of the forwarders that survive
                // into the new configuration, and holds them across the hand-over.
                restartBridge(bridgeId, newBridge);
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
            final var client = internalStartBridge(newBridge);
            if (client != null) {
                activeBridgeNamesToClient.put(bridgeId, new MqttBridgeAndClient(newBridge, client));
            }
        });
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

    /**
     * Synchronized like every other path that mutates a reservation: the stop and the release must be
     * one transition, or a start running concurrently can have the hold it just took released out from
     * under it and its queues reaped while it is still coming up (EDG-882 QA round 3).
     */
    public synchronized void stopBridgeAndRemoveQueues(final @NotNull String bridgeName) {
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
            // Cleared here because bridgeNameToLastError is also written by the connect failure of a
            // bridge that did start, and was cleared only by a later successful connect. A bridge whose
            // remote was unreachable and which the operator then stopped kept its error, and the next
            // hot reload read that as "failed to start, and the configuration has just been corrected"
            // and started a bridge the operator had deliberately stopped (EDG-882 review v02, R2-07).
            // Only on the path where a bridge was actually running: a refused bridge never gets here,
            // and its error is the reason the API reports for why it is not running.
            bridgeNameToLastError.remove(bridgeName);
            internalStopBridge(client, clearQueue, retainQueueForForwarders);
        } else {
            log.debug("Not stopping bridge '{}' since it wasn't started", bridgeName);
        }
    }

    /**
     * Stops a bridge and starts it again, keeping the queues of every forwarder that survives into
     * {@code newBridgeConfig}.
     * <p>
     * Those queues change hands here, and between the two generations nothing owns them: the old
     * forwarders are removed and released by the stop, and the replacements are not registered until
     * the start. The periodic clean-up runs on its own schedule and clears forwarder queues it finds
     * unowned, so a sweep landing in that gap deletes exactly the messages this restart was careful
     * not to clear — and the bridge comes back to an empty queue with nothing logged. The gap is
     * short, the sweep is every few seconds, and the customer this ticket came from restarts bridges
     * to change a filter.
     * <p>
     * So the queues are held across the hand-over: claimed before the old generation lets go, dropped
     * by {@link #internalStartBridge} once the replacements have registered — and if the start fails,
     * the hold simply stays in place, which is what a bridge that cannot start needs anyway.
     */
    public synchronized boolean restartBridge(
            final @NotNull String bridgeId, final @Nullable MqttBridge newBridgeConfig) {
        final var client = activeBridgeNamesToClient.get(bridgeId);
        if (client != null) {
            log.info("Restarting bridge '{}'", bridgeId);
            // Exactly the retained set: the forwarder ids of the replacement configuration are the ones
            // stopBridge is told not to clear, and an id determines its topics because it is a digest
            // over them. Nothing is held when there is no replacement, which is the case in which the
            // stop clears every queue anyway.
            if (newBridgeConfig != null) {
                messageForwarder.reserveQueues(bridgeId, topicsByForwarderId(newBridgeConfig));
            }
            stopBridge(bridgeId, true, newForwarderIds(newBridgeConfig));
            final MqttBridge effectiveBridge = newBridgeConfig != null ? newBridgeConfig : client.bridge();
            final var replacement = internalStartBridge(effectiveBridge);
            if (replacement != null) {
                activeBridgeNamesToClient.put(bridgeId, new MqttBridgeAndClient(effectiveBridge, replacement));
            }
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
            final var client = internalStartBridge(bridge);
            if (client != null) {
                activeBridgeNamesToClient.put(bridgeId, new MqttBridgeAndClient(bridge, client));
            }
            return true;
        } else {
            log.debug("Not starting bridge '{}' since it was already started", bridgeId);
            return false;
        }
    }

    /**
     * Starts one bridge, or reports why it could not start.
     *
     * @return the client, or {@code null} when the bridge could not even be constructed — the caller
     *     must not record a client it does not have, because every reader of
     *     {@code activeBridgeNamesToClient} dereferences it.
     */
    private @Nullable BridgeMqttClient internalStartBridge(final @NotNull MqttBridge bridge) {
        final var bridgeId = bridge.getId();
        if (log.isDebugEnabled()) {
            log.debug(
                    "Initializing bridge '{}' with {} local subscription(s) and {} remote subscription(s)",
                    bridgeId,
                    bridge.getLocalSubscriptions().size(),
                    bridge.getRemoteSubscriptions().size());
        }
        // Hold this bridge's queues for the length of the start. The clean-up service is scheduled
        // during persistence bootstrap, before any bridge has registered a forwarder, so at node start
        // its persisted queues are visible and unowned — and the clean-up deletes forwarder queues
        // nobody owns. The hold covers the rest of that window and is dropped the moment the forwarders
        // take over below; a start that fails replaces it with one that stands until the configuration
        // is corrected. Superseding an existing hold is exactly right on the restart path, which took
        // one before it stopped the previous generation.
        messageForwarder.reserveQueues(bridgeId, topicsByForwarderId(bridge));
        BridgeMqttClient bridgeMqttClient = null;
        final List<MqttForwarder> registered = new ArrayList<>();
        try {
            // Inside the try, one statement further in than it used to be. Constructing the client reads
            // the TLS material, so a mistyped keystore path threw straight past the guard below: the
            // exception escaped updateBridges and every bridge after this one in the iteration was never
            // started, exactly what the comment in the catch says cannot happen (EDG-882 QA round 1).
            bridgeMqttClient = bridgeMqttClientFactory.createRemoteClient(bridge);
            final var forwarders = bridgeMqttClient.createForwarders();
            if (log.isDebugEnabled()) {
                log.debug("Created {} forwarder(s) for bridge '{}'", forwarders.size(), bridgeId);
            }
            for (final MqttForwarder forwarder : forwarders) {
                messageForwarder.addForwarder(forwarder);
                registered.add(forwarder);
            }
            // The forwarders own the queues now, so any hold taken by an earlier failed start can go.
            // After the registrations, never before: releasing first would leave the queues unowned for
            // an instant, which is all a clean-up sweep needs to clear them.
            messageForwarder.releaseReservedQueues(bridgeId);
        } catch (final Exception e) {
            // Un-register the forwarders of this bridge that did register before the failure. Without
            // this, a bridge reported as failed left live forwarders polling its persisted queues into
            // an MQTT client that was never started and would never reconnect -- the messages were
            // drained out of persistence and dropped. The bridge-level counterpart of the per-forwarder
            // rollback in MessageForwarderImpl.addForwarder (EDG-882 F-10, QA round 2).
            //
            // clearQueue = false: nothing is destroyed, and the hold taken above keeps the periodic
            // clean-up off those queues until the configuration is corrected.
            for (final MqttForwarder forwarder : registered) {
                try {
                    messageForwarder.removeForwarder(forwarder, false);
                } catch (final Throwable rollbackFailure) {
                    log.error(
                            "Could not un-register forwarder '{}' of failed bridge '{}'",
                            forwarder.getId(),
                            bridgeId,
                            rollbackFailure);
                }
            }
            // A bridge that cannot register its forwarders must not take the rest of the
            // synchronization down with it: updateBridges iterates over every configured bridge, so an
            // exception escaping here would leave the bridges after this one unstarted. Reported the
            // same way an unsuccessful connect is, which is what surfaces it in the API and the UI.
            //
            // Nothing is cleared on this path, and the hold taken above simply stays: with no forwarder
            // registered the queues read as unowned, and the periodic clean-up deletes unowned forwarder
            // queues within seconds. Without it, refusing to start a bridge would destroy the very
            // messages the refusal exists to protect. It is dropped when the bridge starts or is removed.
            log.error("Bridge '{}' could not be started: {}", bridgeId, e.getMessage());
            log.debug("Forwarder registration failure details", e);
            bridgeNameToLastError.put(bridgeId, e);
            // PerBridgeMetrics registers its counters in the BridgeMqttClient constructor and clearAll
            // runs only in stop(), which this path never reaches -- so a refused bridge left its metrics
            // registered, reporting zeros for something that is not running (EDG-882 review v02, R2-13).
            // Cosmetic, because MetricRegistry.counter() is get-or-create and a retry re-registers the
            // same instruments, but a gauge that reads zero is worse than one that is absent.
            if (bridgeMqttClient != null) {
                bridgeMqttClient.clearMetrics();
            }
            final HiveMQEdgeRemoteEvent errorEvent =
                    new HiveMQEdgeRemoteEvent(HiveMQEdgeRemoteEvent.EVENT_TYPE.BRIDGE_ERROR);
            errorEvent.addUserData(
                    "cloudBridge", String.valueOf(bridge.getHost().endsWith("hivemq.cloud")));
            errorEvent.addUserData("cause", e.getMessage());
            errorEvent.addUserData("name", bridgeId);
            remoteService.fireUsageEvent(errorEvent);
            Checkpoints.checkpoint("mqtt-bridge-forwarder-start-failed");
            // Null, not the client: the rollback above un-registered every forwarder this bridge had
            // managed to add, so what is left is an object that was never started and owns nothing.
            // Recording it made isRunning() report the bridge as STARTED and turned the API's START
            // command into a permanent no-op, because startBridge refuses a bridge that is already in
            // the map -- the operator was left with a bridge that says it is running, forwards nothing,
            // and cannot be started (EDG-882 QA round 3). The queues are unaffected either way: the
            // reservation taken above holds them until the bridge starts or leaves the configuration.
            return null;
        }
        Checkpoints.checkpoint("mqtt-bridge-forwarder-started");
        // Non-null on this path by construction: the only assignment is the first statement of the try,
        // and any failure of it leaves through the catch above.
        return internalStartBridgeMqttClient(bridge, Objects.requireNonNull(bridgeMqttClient));
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
                        // A cancellation is this generation being stopped on purpose, not a bridge that
                        // could not start. BridgeMqttClient.stop() cancels the pending start future, and
                        // this callback then runs asynchronously -- after stopBridge has already cleared
                        // bridgeNameToLastError -- so recording it left a bridge that goes on to connect
                        // normally reporting an error through the API, under a log line saying it could
                        // not be started. Reachable on every update of a bridge whose remote is down,
                        // because an update is now one transition through restartBridge rather than a
                        // removal and an addition (EDG-882, seen on a real node).
                        if (t instanceof CancellationException) {
                            if (log.isDebugEnabled()) {
                                log.debug("Start of bridge '{}' was cancelled because it was stopped", bridge.getId());
                            }
                            return;
                        }
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
                int failedCount = 0;
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
                    // Per forwarder, not around the loop. Catching outside it meant the first forwarder
                    // whose teardown threw took every forwarder after it down with it -- they stayed in
                    // the ownership index, so their queues were never reclaimed and the bridge could not
                    // be restarted under the same ids. removeForwarder now completes its own teardown
                    // before reporting, so what arrives here is a forwarder already released; continuing
                    // is what lets the rest of the bridge be released too (EDG-882 review v03, R3-04).
                    try {
                        messageForwarder.removeForwarder(forwarder, shouldClearQueue);
                        removedCount++;
                    } catch (final Throwable e) {
                        failedCount++;
                        log.error(
                                "Removing forwarder '{}' of bridge '{}' did not complete cleanly; continuing with the rest",
                                forwarder.getId(),
                                bridgeId,
                                e);
                    }
                }
                if (failedCount > 0) {
                    log.warn(
                            "Bridge '{}' released {} forwarder(s), {} of which reported a teardown failure",
                            bridgeId,
                            removedCount + failedCount,
                            failedCount);
                } else if (log.isDebugEnabled()) {
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
