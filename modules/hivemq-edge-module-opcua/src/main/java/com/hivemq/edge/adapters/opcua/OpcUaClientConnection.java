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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.Failure;
import com.hivemq.edge.adapters.opcua.client.OpcUaClientConfigurator;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Result;
import com.hivemq.edge.adapters.opcua.client.Success;
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
import org.eclipse.milo.opcua.stack.core.types.structured.TransferSubscriptionsResponse;
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

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

class OpcUaClientConnection {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaClientConnection.class);
    private static final @NotNull Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private final @NotNull OpcUaSpecificAdapterConfig config;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;

    private final @NotNull AtomicReference<UInteger> lastKnownSubscriptionId;

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
        this.lastKnownSubscriptionId = lastSubscriptionId;
    }

    @NotNull Result<Void, Throwable> start() {
        final ParsedConfig parsedConfig;
        final var result = ParsedConfig.fromConfig(config);
        if (result instanceof final Failure<ParsedConfig, String> failure) {
            log.error("Failed to parse configuration for OPC UA client: {}", failure.failure());
            return Failure.of(new IllegalStateException(failure.failure()));
        } else if (result instanceof final Success<ParsedConfig, String> success) {
            parsedConfig = success.result();
        } else {
            return Failure.of(new IllegalStateException("Unexpected result type: " + result.getClass().getName()));
        }

        final var subscriptionIdOptional = Optional.ofNullable(lastKnownSubscriptionId.get());
        log.debug("Subscribing to OPC UA client with subscriptionId: {}", subscriptionIdOptional.orElse(null));
        final OpcUaClient client;
        final var faultListener = new OpcUaServiceFaultListener(protocolAdapterMetricsService, eventService, adapterId);
        final var activityListener = new OpcUaSessionActivityListener(protocolAdapterMetricsService, eventService, adapterId, protocolAdapterState);
        try {
            client = OpcUaClient
                .create(config.getUri(),
                        endpoints ->
                                endpoints.stream()
                                    .filter(e -> {
                                        final var requiredPolicy = config.getSecurity().policy().getSecurityPolicy().getUri();
                                        return requiredPolicy.equals(e.getSecurityPolicyUri());
                                    })
                                    .findFirst(),
                        ignore -> {},
                        new OpcUaClientConfigurator(adapterId, parsedConfig));
            client.addSessionActivityListener(activityListener);
            client.addFaultListener(faultListener);
            client.connect();
        } catch (final UaException e) {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            return Failure.of(e);
        }

        final var subscriptionOptional = subscribe(client, subscriptionIdOptional);

        if(subscriptionOptional.isEmpty()) {
            log.error("Failed to create or transfer OPC UA subscription. Closing client connection.");
            quietlyCloseClient(client, false,null, null);
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.ERROR);
            return Failure.of(new IllegalStateException("Failed to create or transfer OPC UA subscription"));
        }

        final var subscription = subscriptionOptional.get();
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        log.trace("Creating Subscription for OPC UA client");

        context.set(new ConnectionContext(subscription.getClient(), faultListener, activityListener));
        log.info("Client created and connected successfully");
        return Success.of(null);
    }

    void stop() {
        log.info("Stopping OPC UA client");
        final ConnectionContext ctx = context.get();
        if(ctx != null) {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            quietlyCloseClient(ctx.client(),true,  ctx.faultListener(), ctx.activityListener());
        }
    }

    void destroy() {
        log.info("Destroying OPC UA client");
        final ConnectionContext ctx = context.get();
        if(ctx != null) {
            protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
            quietlyCloseClient(ctx.client(), false, ctx.faultListener(), ctx.activityListener());
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
     * @param subscriptionOptional  an Optional containing the subscription ID if available
     * @return an Optional containing the created or transferred subscription, or empty if failed
     */
    private @NotNull Optional<OpcUaSubscription> subscribe(final @NotNull OpcUaClient client, final @NotNull Optional<UInteger> subscriptionOptional) {
            return subscriptionOptional
                    .flatMap(subscriptionId -> transferSubscription(client, subscriptionId))
                    .or(() -> createNewSubscription(client))
                    .flatMap(subscription -> {
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
    private @NotNull Optional<OpcUaSubscription> createNewSubscription(final @NotNull OpcUaClient client) {
        log.debug("Creating new OPC UA subscription");
        final OpcUaSubscription subscription = new OpcUaSubscription(client);
        subscription.setPublishingInterval((double) config.getOpcuaToMqttConfig().publishingInterval());
        subscription.setSubscriptionListener(new OpcUaSubscriptionListener(protocolAdapterMetricsService, tagStreamingService, eventService, adapterId, tags, client, dataPointFactory, GSON));
        try {
            subscription.create();
            return subscription
                    .getSubscriptionId()
                    .map(subscriptionId -> {
                        log.trace("New subscription ID: {}", subscriptionId);
                        lastKnownSubscriptionId.set(subscriptionId);
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
     * Transfers an existing subscription to the current client.
     * If the subscription is not found, it will return an empty Optional.
     *
     * @param client         the OPC UA client
     * @param subscriptionId the subscription ID to transfer
     * @return an Optional containing the transferred subscription or empty if not found
     */
    private static @NotNull Optional<OpcUaSubscription> transferSubscription(final @NotNull OpcUaClient client, final @NotNull UInteger subscriptionId) {
        log.debug("Transfer OPC UA subscription: {}", subscriptionId);
        final TransferSubscriptionsResponse response;
        try {
            response = client.transferSubscriptions(List.of(subscriptionId), true);
        } catch (final UaException e) {
            log.debug("OPC UA subscription not transferred to connection", e);
            return Optional.empty();
        }

        final var results = response.getResults();
        if (results != null && results.length > 0) {
            if (results[0].getStatusCode().isGood()) {
                return client.getSubscriptions().stream()
                        .filter(subscription ->
                                subscription
                                        .getSubscriptionId()
                                        .map(currentSubscriptionId -> currentSubscriptionId.equals(subscriptionId))
                                        .orElse(false))
                        .findFirst();
            } else {
                log.debug("OPC UA subscription not transferred to connection: {}", results[0].getStatusCode().toString());
                return Optional.empty();
            }
        } else {
            log.error("OPC UA subscription not transferred to connection: no results returned");
            return Optional.empty();
        }

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
    private static boolean syncTagsAndMonitoredItems(final @NotNull OpcUaSubscription subscription, final @NotNull List<OpcuaTag> tags, final @NotNull OpcUaSpecificAdapterConfig config) {

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

        if(!keepSubscription) {
            client.getSubscriptions().forEach(subscription -> quietlyDeleteSubscription(client, subscription));
        }
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
