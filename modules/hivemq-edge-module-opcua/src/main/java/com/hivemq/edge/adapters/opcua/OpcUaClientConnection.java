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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.OpcUaEndpointFilter;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSessionActivityListener;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaSubscriptionListener;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class OpcUaClientConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConnection.class);

    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;

    private final @NotNull AtomicReference<ConnectionContext> context = new AtomicReference<>();

    OpcUaClientConnection(
            final @NotNull String adapterId,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull ProtocolAdapterState protocolAdapterState,
            final @NotNull ProtocolAdapterTagStreamingService tagStreamingService,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
            final @NotNull OpcUaSpecificAdapterConfig config,
            final @NotNull AtomicReference<UInteger> lastSubscriptionId) {
        this.config = config;
        this.tagStreamingService = tagStreamingService;
        this.dataPointFactory = dataPointFactory;
        this.eventService = eventService;
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
        this.tags = tags;
    }

    synchronized boolean start(final ParsedConfig parsedConfig) {
        log.debug("Subscribing to OPC UA client");
        final OpcUaClient client;
        final var faultListener = new OpcUaServiceFaultListener(protocolAdapterMetricsService, eventService, adapterId);
        final var activityListener = new OpcUaSessionActivityListener(protocolAdapterMetricsService, eventService, adapterId, protocolAdapterState);
        final var endpointFilter = new OpcUaEndpointFilter(adapterId, config.getSecurity().policy().getSecurityPolicy().getUri(), config);
        try {
            client = OpcUaClient
                .create(
                        config.getUri(),
                        endpointFilter,
                        ignore -> {},
                        new OpcUaClientConfigurator(adapterId, parsedConfig));
            client.addFaultListener(faultListener);
            client.connect();
        } catch (final UaException e) {
            log.error("Unable to connect and subscribe to the OPC UA server for adapter '{}'", adapterId, e);
            eventService
                    .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withMessage("Unable to connect and subscribe to the OPC UA server for adapter '" + adapterId + "'")
                    .withSeverity(Event.SEVERITY.ERROR)
                    .fire();
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            return false;
        }

        final var subscriptionOptional = subscribe(client);

        if(subscriptionOptional.isEmpty()) {
            log.error("Failed to create or transfer OPC UA subscription. Closing client connection.");
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            eventService
                    .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withMessage("Failed to create or transfer OPC UA subscription. Closing client connection.")
                    .withSeverity(Event.SEVERITY.ERROR)
                    .fire();
            quietlyCloseClient(client, false, faultListener, activityListener);
            return false;
        }

        final var subscription = subscriptionOptional.get();
        log.trace("Creating Subscription for OPC UA client");

        context.set(new ConnectionContext(subscription.getClient(), faultListener, activityListener));
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        client.addSessionActivityListener(activityListener);
        log.info("Client created and connected successfully");
        return true;
    }

    synchronized void stop() {
        log.info("Stopping OPC UA client");
        final ConnectionContext ctx = context.get();
        if(ctx != null) {
            quietlyCloseClient(ctx.client(),true,  ctx.faultListener(), ctx.activityListener());
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    void destroy() {
        log.info("Destroying OPC UA client");
        final ConnectionContext ctx = context.get();
        if(ctx != null) {
            quietlyCloseClient(ctx.client(), false, ctx.faultListener(), ctx.activityListener());
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        }
    }

    @NotNull Optional<OpcUaClient> client() {
        final ConnectionContext ctx = context.get();
        if(ctx != null) {
            return Optional.of(ctx.client());
        }
        return Optional.empty();
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
    private @NotNull Optional<OpcUaSubscription> subscribe(final @NotNull OpcUaClient client) {
            return createNewSubscription(client)
                    .flatMap(subscription -> {
                        subscription.setPublishingInterval((double) config.getOpcuaToMqttConfig().publishingInterval());
                        subscription.setSubscriptionListener(new OpcUaSubscriptionListener(protocolAdapterMetricsService, tagStreamingService, eventService, adapterId, tags, client, dataPointFactory, config));
                        if(syncTagsAndMonitoredItems(subscription, tags, config)) {
                            return Optional.of(subscription);
                        } else {
                            return Optional.empty();
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

    private record ConnectionContext(@NotNull OpcUaClient client, @NotNull ServiceFaultListener faultListener, @NotNull SessionActivityListener activityListener) {
    }

    private static void quietlyDeleteSubscription(
            final @NotNull OpcUaClient client,
            final @NotNull OpcUaSubscription subscription) {
        try {
            subscription.delete();
        } catch (final Exception e) {
            log.warn("Failed to delete subscription {}: {}", subscription, e.getMessage());
        }
        try {
            client.removeSubscription(subscription);
        } catch (final Exception e) {
            log.warn("Failed to remove subscription {}: {}", subscription, e.getMessage());
        }
    }

    private static void quietlyCloseClient(
            final @NotNull OpcUaClient client,
            final boolean keepSubscription,
            final @Nullable ServiceFaultListener faultListener,
            final @Nullable SessionActivityListener activityListener) {

        client
            .getSubscriptions()
            .forEach(subscription -> {
                subscription.setSubscriptionListener(null);
                if(!keepSubscription) {
                    quietlyDeleteSubscription(client, subscription);
                }
            });
        if (faultListener != null) {
            try {
                client.removeFaultListener(faultListener);
            } catch (final Throwable e) {
                log.error("Failed to remove fault listener {}: {}", faultListener, e.getMessage());
            }
        }
        if (activityListener != null) {
            try {
                client.removeSessionActivityListener(activityListener);
            } catch (final Throwable e) {
                log.error("Failed to remove session activity listener {}: {}", activityListener, e.getMessage());
            }
        }

        try {
            client.disconnect();
        } catch (final UaException e) {
            log.error("Failed to disconnect: {}", e.getMessage());
        }
    }
}
