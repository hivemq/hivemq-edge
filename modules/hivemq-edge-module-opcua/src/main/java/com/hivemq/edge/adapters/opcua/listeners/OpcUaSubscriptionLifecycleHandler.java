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

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.Constants;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaSubscriptionLifecycleHandler implements OpcUaSubscription.SubscriptionListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaSubscriptionLifecycleHandler.class);

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    final Map<OpcuaTag, Boolean> tagToFirstSeen = new ConcurrentHashMap<>();
    private final @NotNull Map<NodeId, OpcuaTag> nodeIdToTag;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull OpcUaClient client;
    private final @NotNull DataPointFactory dataPointFactory;
    final @NotNull OpcUaSpecificAdapterConfig config;

    public OpcUaSubscriptionLifecycleHandler(
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull EventService eventService,
            final @NotNull String adapterId,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaClient client,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull OpcUaSpecificAdapterConfig config) {
        this.config = config;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.tagStreamingService = tagStreamingService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.client = client;
        this.dataPointFactory = dataPointFactory;
        this.tags = tags;
        nodeIdToTag = tags.stream()
                .collect(Collectors.toMap(tag -> NodeId.parse(tag.getDefinition().getNode()), Function.identity()));
    }

    /**
     * Subscribes to the OPC UA client.
     * If a subscription ID is provided, it attempts to transfer the subscription.
     * If the transfer fails or no ID is provided, it creates a new subscription.
     * It then synchronizes the tags and monitored items.
     *
     * @param client                the OPC UA client
     * @return an Optional containing the created or transferred subscription, or empty if failed
     */
    public @NotNull Optional<OpcUaSubscription> subscribe(final @NotNull OpcUaClient client) {
        return createNewSubscription(client)
                .map(subscription -> {
                    subscription.setPublishingInterval((double) config.getOpcuaToMqttConfig().publishingInterval());
                    subscription.setSubscriptionListener(new OpcUaSubscriptionLifecycleHandler(protocolAdapterMetricsService, tagStreamingService, eventService, adapterId, tags, client, dataPointFactory, config));
                    if(syncTagsAndMonitoredItems(subscription, tags, config)) {
                        return subscription;
                    } else {
                        return null;
                    }
                });
    }

    /**
     * Creates a new OPC UA subscription.
     * If the subscription is created successfully, it returns an Optional containing the subscription.
     * If the subscription creation fails, it returns an empty Optional.
     *
     * @param client the OPC UA client
     * @return an Optional containing the created subscription or empty if creation failed
     */
    public static @NotNull Optional<OpcUaSubscription> createNewSubscription(final @NotNull OpcUaClient client) {
        log.debug("Creating new OPC UA subscription");
        final OpcUaSubscription subscription = new OpcUaSubscription(client);
        try {
            subscription.create();
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
    public static boolean syncTagsAndMonitoredItems(final @NotNull OpcUaSubscription subscription, final @NotNull List<OpcuaTag> tags, final @NotNull OpcUaSpecificAdapterConfig config) {

        final var nodeIdToTag = tags.stream().collect(Collectors.toMap(tag -> NodeId.parse(tag.getDefinition().getNode()), Function.identity()));
        final var nodeIdToMonitoredItem = subscription.getMonitoredItems().stream().collect(Collectors.toMap(monitoredItem -> monitoredItem.getReadValueId().getNodeId(), Function.identity()));

        final var monitoredItemsToRemove = nodeIdToMonitoredItem.entrySet().stream().filter(entry -> !nodeIdToTag.containsKey(entry.getKey())).map(Map.Entry::getValue).toList();
        final var monitoredItemsToAdd = nodeIdToTag.entrySet().stream().filter(entry -> !nodeIdToMonitoredItem.containsKey(entry.getKey())).map(Map.Entry::getValue).toList();

        //clear deleted monitored items
        if(!monitoredItemsToRemove.isEmpty()) {
            subscription.removeMonitoredItems(monitoredItemsToRemove);
            log.debug("Removed monitored items: {}", monitoredItemsToRemove.stream().map(item -> item.getReadValueId().getNodeId()));
        }

        //update existing monitored items
        subscription.getMonitoredItems().forEach(monitoredItem -> {
            //TODO: allow to configure these values per TAG!!!!
            monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().serverQueueSize()));
            monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().publishingInterval());
        });

        //add new monitored items
        if(!monitoredItemsToAdd.isEmpty()) {
            monitoredItemsToAdd.forEach(opcuaTag -> {
                final String nodeId = opcuaTag.getDefinition().getNode();
                final var monitoredItem = OpcUaMonitoredItem.newDataItem(NodeId.parse(nodeId));
                monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().serverQueueSize()));
                monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().publishingInterval());
                subscription.addMonitoredItem(monitoredItem);
            });
            log.debug("Added monitored items: {}", monitoredItemsToAdd.stream().map(item -> item.getDefinition().getNode()).toList());
        }

        try {
            subscription.synchronizeMonitoredItems();
            log.info("All monitored items synchronized successfully");
            return true;
        } catch (final UaException e) {
            log.error("Failed to synchronize monitored items: {} {}", e.getStatusCode(), e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void onKeepAliveReceived(final @NotNull OpcUaSubscription subscription) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_KEEPALIVE_COUNT);
    }

    @Override
    public void onTransferFailed(
            final @NotNull OpcUaSubscription brokenSubscription,
            final @NotNull StatusCode status) {
        // Transfer failed after a disconnect, the current subscription is broken.
        // We need to create a new subscription and recreate the monitored items.

        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT);

        log.error("Subscription Transfer failed, recreating subscription for adapter '{}'", adapterId);
        createNewSubscription(client)
                .ifPresentOrElse(
                        replacementSubscription -> {
                            // reconnect the listener with the new subscription
                            replacementSubscription.setSubscriptionListener(this);
                            syncTagsAndMonitoredItems(replacementSubscription, tags, config);
                        },
                        () -> {
                            log.error("Subscription Transfer failed, unable to create new subscription '{}'", adapterId);
                        }
                );
    }

    @Override
    public void onDataReceived(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcUaMonitoredItem> items,
            final @NotNull List<DataValue> values) {
        for (int i = 0; i < items.size(); i++) {
            final var tag = nodeIdToTag.get(items.get(i).getReadValueId().getNodeId());
            final String tn = tag.getName();
            if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Adapter '%s' took first sample for tag '%s'",
                                adapterId,
                                tn))
                        .fire();
            }
            try {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT);
                final String payload = extractPayload(client, values.get(i));
                tagStreamingService.feed(tn, List.of(dataPointFactory.createJsonDataPoint(tn, payload)));
            } catch (final Throwable e) {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                throw new RuntimeException(e);
            }
        }
    }

    private static @NotNull String extractPayload(final @NotNull OpcUaClient client, final @NotNull DataValue value)
            throws UaException {
        if (value.getValue().getValue() == null) {
            return "";
        }

        final ByteBuffer byteBuffer = OpcUaToJsonConverter.convertPayload(client.getDynamicEncodingContext(), value);
        final byte[] buffer = new byte[byteBuffer.remaining()];
        byteBuffer.get(buffer);
        return new String(buffer, StandardCharsets.UTF_8);
    }
}
