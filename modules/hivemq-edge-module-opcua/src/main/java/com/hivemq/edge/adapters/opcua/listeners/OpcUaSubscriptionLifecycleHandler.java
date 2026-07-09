/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.listeners;

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.Constants;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemServiceOperationResult;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaSubscriptionLifecycleHandler implements OpcUaSubscription.SubscriptionListener {

    public static final long KEEP_ALIVE_SAFETY_MARGIN_MS = 5_000L;
    public static final long TYPE_REGISTRY_RESET_THROTTLE_MS = 10_000L;

    private static final long TYPE_REGISTRY_RESET_THROTTLE_NANOS =
            TimeUnit.MILLISECONDS.toNanos(TYPE_REGISTRY_RESET_THROTTLE_MS);
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaSubscriptionLifecycleHandler.class);
    private static final int MAX_MONITORED_ITEM_COUNT = 5;

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull Map<OpcuaTag, Boolean> tagToFirstSeen;
    private final @NotNull Map<NodeId, OpcuaTag> nodeIdToTag;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull OpcUaClient client;
    private final @NotNull OpcUaSpecificAdapterConfig config;

    // Track last keep-alive timestamp for health monitoring
    private volatile long lastKeepAliveTimestamp;

    // Track last dynamic-type-registry reset so a permanently undecodable type cannot trigger a
    // full DataTypeTree browse per notification (EDG-776). Monotonic clock (nanoTime), seeded one
    // throttle window in the past so the first reset is never throttled.
    private final @NotNull AtomicLong lastTypeRegistryResetNanos =
            new AtomicLong(System.nanoTime() - TYPE_REGISTRY_RESET_THROTTLE_NANOS);

    public OpcUaSubscriptionLifecycleHandler(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull EventService eventService,
            final @NotNull String adapterId,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaClient client,
            final @NotNull OpcUaSpecificAdapterConfig config) {
        this.config = config;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.tagStreamingService = tagStreamingService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.client = client;
        this.tags = tags;
        this.tagToFirstSeen = new ConcurrentHashMap<>();
        this.lastKeepAliveTimestamp = System.currentTimeMillis();
        this.nodeIdToTag = tags.stream()
                .collect(Collectors.toMap(
                        tag -> NodeId.parse(tag.getDefinition().getNode()), Function.identity(), (first, second) -> {
                            log.warn(
                                    "Adapter '{}': duplicate node ID '{}' — tags '{}' and '{}' reference the same OPC UA node. Only '{}' will receive data.",
                                    adapterId,
                                    first.getDefinition().getNode(),
                                    first.getName(),
                                    second.getName(),
                                    first.getName());
                            return first;
                        }));
    }

    /**
     * Starts construction of a new OPC UA subscription. Configure with the builder's setters, then
     * call {@link Builder#create()} to push the subscription to the server.
     *
     * @param client the OPC UA client
     * @return a builder
     */
    public static @NotNull Builder newSubscription(final @NotNull OpcUaClient client) {
        return new Builder(client);
    }

    /** Fluent builder for an {@link OpcUaSubscription}. */
    public static final class Builder {

        private final @NotNull OpcUaClient client;
        private int publishingIntervalMs = 1000; // OPC UA / Milo default, kept here for documentation

        private Builder(final @NotNull OpcUaClient client) {
            this.client = client;
        }

        /**
         * Set the requested publishing interval in milliseconds.
         *
         * @param publishingIntervalMs the requested publishing interval in milliseconds
         * @return this builder, for chaining
         */
        public @NotNull Builder publishingInterval(final int publishingIntervalMs) {
            this.publishingIntervalMs = publishingIntervalMs;
            return this;
        }

        /**
         * Create the subscription on the server with the configured parameters. If the server revises
         * the requested publishing interval (e.g. to enforce a minimum), the revision is logged.
         *
         * @return the created subscription, or empty if the creation failed
         */
        public @NotNull Optional<OpcUaSubscription> create() {
            log.debug("Creating new OPC UA subscription with publishingInterval={}ms", publishingIntervalMs);
            final OpcUaSubscription subscription = new OpcUaSubscription(client);
            subscription.setPublishingInterval((double) publishingIntervalMs);
            try {
                subscription.create();
                final double revised = subscription.getPublishingInterval();
                if (Math.abs(revised - publishingIntervalMs) > 1.0) {
                    log.warn(
                            "OPC UA server revised publishingInterval: requested={}ms, revised={}ms",
                            publishingIntervalMs,
                            revised);
                } else {
                    log.info(
                            "OPC UA subscription created with publishingInterval={}ms (requested {}ms)",
                            revised,
                            publishingIntervalMs);
                }
                return subscription
                        .getSubscriptionId()
                        .map(subscriptionId -> {
                            log.trace("New subscription ID: {}", subscriptionId);
                            return subscription;
                        })
                        .or(() -> {
                            log.error("Subscription not created on the server");
                            return Optional.empty();
                        });
            } catch (final UaException e) {
                log.error("Failed to create subscription", e);
            }
            return Optional.empty();
        }
    }

    private static void extractPayload(
            final @NotNull OpcUaClient client,
            final @NotNull DataValue value,
            final @NotNull DataPointBuilder<?> builder)
            throws UaException {
        OpcUaToJsonConverter.convertPayload(client.getDynamicEncodingContext(), value, builder);
    }

    /**
     * Subscribes to the OPC UA client.
     * If a subscription ID is provided, it attempts to transfer the subscription.
     * If the transfer fails or no ID is provided, it creates a new subscription.
     * It then synchronizes the tags and monitored items.
     *
     * @param client the OPC UA client
     * @return an Optional containing the created or transferred subscription, or empty if failed
     */
    public @NotNull Optional<OpcUaSubscription> subscribe(final @NotNull OpcUaClient client) {
        return newSubscription(client)
                .publishingInterval(config.getOpcuaToMqttConfig().publishingInterval())
                .create()
                .map(subscription -> {
                    subscription.setSubscriptionListener(this);
                    if (syncTagsAndMonitoredItems(subscription, tags, config)) {
                        return subscription;
                    } else {
                        return null;
                    }
                });
    }

    /**
     * Synchronizes the tags and monitored items in the subscription.
     * It removes monitored items that are not in the tags list and adds new monitored items from the tags list.
     * It also updates existing monitored items with the configured queue size and sampling interval.
     *
     * @param subscription the OPC UA subscription
     * @param tags         the list of tags to synchronize
     * @param config       the configuration for the OPC UA adapter
     * @return true if synchronization was successful, false otherwise
     */
    private boolean syncTagsAndMonitoredItems(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaSpecificAdapterConfig config) {

        final var nodeIdToTag = tags.stream()
                .collect(Collectors.toMap(
                        tag -> NodeId.parse(tag.getDefinition().getNode()),
                        Function.identity(),
                        (first, second) -> first));
        final var nodeIdToMonitoredItem = subscription.getMonitoredItems().stream()
                .collect(Collectors.toMap(
                        monitoredItem -> monitoredItem.getReadValueId().getNodeId(), Function.identity()));

        final var monitoredItemsToRemove = nodeIdToMonitoredItem.entrySet().stream()
                .filter(entry -> !nodeIdToTag.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        final var monitoredItemsToAdd = nodeIdToTag.entrySet().stream()
                .filter(entry -> !nodeIdToMonitoredItem.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        // clear deleted monitored items
        if (!monitoredItemsToRemove.isEmpty()) {
            subscription.removeMonitoredItems(monitoredItemsToRemove);
            log.debug(
                    "Removed monitored items: {}",
                    monitoredItemsToRemove.stream()
                            .map(item -> item.getReadValueId().getNodeId()));
        }

        // update existing monitored items
        subscription.getMonitoredItems().forEach(monitoredItem -> {
            // TODO: allow to configure these values per TAG!!!!
            monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().serverQueueSize()));
            monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().publishingInterval());
        });

        // add new monitored items
        if (!monitoredItemsToAdd.isEmpty()) {
            monitoredItemsToAdd.forEach(opcuaTag -> {
                final String nodeId = opcuaTag.getDefinition().getNode();
                final var monitoredItem = OpcUaMonitoredItem.newDataItem(NodeId.parse(nodeId));
                monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().serverQueueSize()));
                monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().publishingInterval());
                subscription.addMonitoredItem(monitoredItem);
            });
            log.debug(
                    "Added monitored items: {}",
                    monitoredItemsToAdd.stream()
                            .map(item -> item.getDefinition().getNode())
                            .toList());
        }

        try {
            subscription.synchronizeMonitoredItems();
            log.info("All monitored items synchronized successfully");
            return true;
        } catch (final MonitoredItemSynchronizationException e) {
            final List<MonitoredItemServiceOperationResult> allResults = new ArrayList<>();
            allResults.addAll(e.getCreateResults());
            allResults.addAll(e.getModifyResults());
            allResults.addAll(e.getDeleteResults());

            final long successCount = allResults.stream()
                    .filter(MonitoredItemServiceOperationResult::isGood)
                    .count();
            final long failCount = allResults.stream().filter(r -> !r.isGood()).count();

            final String failedSample = allResults.stream()
                    .filter(r -> !r.isGood())
                    .map(MonitoredItemServiceOperationResult::monitoredItem)
                    .filter(Objects::nonNull)
                    .map(OpcUaMonitoredItem::getReadValueId)
                    .filter(Objects::nonNull)
                    .map(ReadValueId::getNodeId)
                    .filter(Objects::nonNull)
                    .map(NodeId::toString)
                    .limit(MAX_MONITORED_ITEM_COUNT)
                    .collect(Collectors.joining(", "));

            if (successCount > 0) {
                // Partial failure — continue with healthy items
                log.warn(
                        "Partial monitored item sync for adapter '{}': {} ok, {} failed. Samples: {}",
                        adapterId,
                        successCount,
                        failCount,
                        failedSample);
                eventService
                        .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withMessage("Partial subscription: " + successCount + " active, " + failCount
                                + " failed. Samples: " + failedSample)
                        .withSeverity(Event.SEVERITY.WARN)
                        .fire();
                return true;
            } else {
                // Total failure — no items succeeded
                final String message = "Failed to synchronize monitored items: " + e.getStatusCode() + " "
                        + e.getMessage() + ". Samples: " + failedSample;
                log.error(message, e);
                eventService
                        .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withMessage(message)
                        .withSeverity(Event.SEVERITY.ERROR)
                        .fire();
                return false;
            }
        }
    }

    @Override
    public void onKeepAliveReceived(final @NotNull OpcUaSubscription subscription) {
        lastKeepAliveTimestamp = System.currentTimeMillis();
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_KEEPALIVE_COUNT);
        subscription
                .getSubscriptionId()
                .ifPresent(sid -> log.debug("Keep-alive received for subscription {} of adapter '{}'", sid, adapterId));
    }

    /**
     * Checks if keep-alive messages are being received within the expected timeout.
     * The timeout is computed dynamically from ConnectionOptions.
     * Can be used for health monitoring to detect subscription issues.
     *
     * @return true if last keep-alive was received within the computed timeout, false otherwise
     */
    public boolean isKeepAliveHealthy() {
        return (System.currentTimeMillis() - lastKeepAliveTimestamp) < getKeepAliveTimeoutMs();
    }

    /**
     * Computes the keep-alive timeout based on ConnectionOptions.
     * The timeout allows for the configured number of missed keep-alives plus one
     * before considering the connection unhealthy, plus a safety margin.
     * Formula: keepAliveIntervalMs × (keepAliveFailuresAllowed + 1) + KEEP_ALIVE_SAFETY_MARGIN_MS
     *
     * @return the computed keep-alive timeout in milliseconds
     */
    public long getKeepAliveTimeoutMs() {
        final var opts = config.getConnectionOptions();
        return opts.keepAliveIntervalMs() * (opts.keepAliveFailuresAllowed() + 1) + KEEP_ALIVE_SAFETY_MARGIN_MS;
    }

    @Override
    public void onTransferFailed(
            final @NotNull OpcUaSubscription brokenSubscription, final @NotNull StatusCode status) {
        // Transfer failed after a disconnect, the current subscription is broken.
        // We need to create a new subscription and recreate the monitored items.

        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT);

        log.error("Subscription Transfer failed, recreating subscription for adapter '{}'", adapterId);
        newSubscription(client)
                .publishingInterval(config.getOpcuaToMqttConfig().publishingInterval())
                .create()
                .ifPresentOrElse(
                        replacementSubscription -> {
                            // reconnect the listener with the new subscription
                            replacementSubscription.setSubscriptionListener(this);
                            syncTagsAndMonitoredItems(replacementSubscription, tags, config);
                        },
                        () -> log.error(
                                "Subscription Transfer failed, unable to create new subscription '{}'", adapterId));
    }

    @Override
    public void onDataReceived(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcUaMonitoredItem> items,
            final @NotNull List<DataValue> values) {
        lastKeepAliveTimestamp = System.currentTimeMillis();
        final var dataPointsPublisher = tagStreamingService.dataPointsPublisher();
        for (int i = 0; i < items.size(); i++) {
            final var tag = Objects.requireNonNull(
                    nodeIdToTag.get(items.get(i).getReadValueId().getNodeId()));
            final String tn = tag.getName();
            if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                eventService
                        .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Adapter '%s' took first sample for tag '%s'", adapterId, tn))
                        .fire();
            }
            try {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT);

                final var dataPointBuilder = dataPointsPublisher.addDataPoint(tag);
                extractPayload(client, values.get(i), dataPointBuilder);
            } catch (final @NotNull UaSerializationException e) {
                // Typically "no codec registered" for a custom struct: Milo resets the dynamic codec
                // registry on every session (re)activation and rebuilds it best-effort — browse/read
                // failures under load leave it silently incomplete (EDG-776). Publishing the undecoded
                // binary body would corrupt the payload, so drop this notification batch (the server
                // resamples the monitored items) and reset the registry so the next notification
                // triggers a fresh rebuild.
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                log.warn(
                        "Adapter '{}': could not decode OPC UA value for tag '{}', dropping the current samples and resetting the dynamic type registry",
                        adapterId,
                        tn,
                        e);
                resetDynamicTypeRegistry();
                return;
            } catch (final Throwable e) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                throw new RuntimeException(e);
            }
        }
        dataPointsPublisher.publish();
    }

    /**
     * Resets the client's cached DataTypeTree, dynamic DataTypeManager and dynamic EncodingContext so
     * they are rebuilt from the server on next use. Throttled because each rebuild browses the
     * server's full DataType hierarchy.
     */
    private void resetDynamicTypeRegistry() {
        final long now = System.nanoTime();
        final long last = lastTypeRegistryResetNanos.get();
        // CAS so concurrent notification threads cannot both win the throttle window
        if (now - last >= TYPE_REGISTRY_RESET_THROTTLE_NANOS && lastTypeRegistryResetNanos.compareAndSet(last, now)) {
            client.resetDataTypeTree();
            client.resetDynamicDataTypeManager();
            client.resetDynamicEncodingContext();
        }
    }
}
