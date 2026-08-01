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
import com.hivemq.edge.adapters.opcua.condition.ConditionEventFilters;
import com.hivemq.edge.adapters.opcua.condition.ConditionRefresh;
import com.hivemq.edge.adapters.opcua.condition.ConditionTypeVerifier;
import com.hivemq.edge.adapters.opcua.condition.NotifierResolver;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagType;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaEventToJsonConverter;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaSubscriptionLifecycleHandler implements OpcUaSubscription.SubscriptionListener {

    public static final long KEEP_ALIVE_SAFETY_MARGIN_MS = 5_000L;
    public static final long TYPE_REGISTRY_RESET_THROTTLE_MS = 10_000L;

    private static final long TYPE_REGISTRY_RESET_THROTTLE_NANOS =
            TimeUnit.MILLISECONDS.toNanos(TYPE_REGISTRY_RESET_THROTTLE_MS);
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaSubscriptionLifecycleHandler.class);
    private static final int MAX_MONITORED_ITEM_COUNT = 5;

    // Verification is one browse against an already-connected session. The bound only exists so a server that
    // never answers cannot stall adapter start indefinitely.
    private static final long CONDITION_TYPE_VERIFICATION_TIMEOUT_MS = 10_000L;

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull ProtocolAdapterTagStreamingService tagStreamingService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull Map<OpcuaTag, Boolean> tagToFirstSeen;
    private final @NotNull List<OpcuaTag> tags;
    private final @NotNull OpcUaClient client;
    private final @NotNull OpcUaSpecificAdapterConfig config;

    // Track last keep-alive timestamp for health monitoring
    private volatile long lastKeepAliveTimestamp;

    // The subscription currently in use, so a reconnect that keeps it can still ask for a condition refresh.
    // Set whenever monitored items are synchronized; replaced when a broken subscription is recreated.
    private final @NotNull AtomicReference<OpcUaSubscription> currentSubscription = new AtomicReference<>();

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
            // A condition tag whose declared type does not match the device, or whose events have no notifier
            // to arrive on, is dropped here rather than subscribed. The check is per tag, so one bad tag
            // cannot stop the others -- or the adapter -- from starting.
            final List<VerifiedTag> verifiedTags = new ArrayList<>();
            for (final OpcuaTag opcuaTag : monitoredItemsToAdd) {
                verify(opcuaTag).ifPresent(verifiedTags::add);
            }

            verifiedTags.forEach(verified -> {
                final OpcuaTag opcuaTag = verified.tag();
                final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
                // A condition is observed through its transitions, so it needs an event item carrying an
                // event filter; an ordinary value is observed directly through its Value attribute.
                final var monitoredItem =
                        switch (opcuaTag.getDefinition().getType()) {
                            case CONDITION, EVENT_SUBSCRIPTION ->
                                OpcUaMonitoredItem.newEventItem(
                                        // The item goes on the notifier, not on the condition: a condition is not
                                        // an event notifier. Which condition the events are about is decided by
                                        // the filter, not by the node subscribed to.
                                        Objects.requireNonNull(verified.notifier()),
                                        ConditionEventFilters.forCondition(
                                                nodeId, opcuaTag.getDefinition().getConditionType()));
                            case VALUE -> OpcUaMonitoredItem.newDataItem(nodeId);
                        };
                monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().serverQueueSize()));
                monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().publishingInterval());
                // The tag rides on the item itself, so a notification carries its own identity and nothing has
                // to be looked up. Several tags can share a node -- and several condition tags one notifier --
                // so the node id a lookup would key on is not unique enough to identify the tag.
                monitoredItem.setUserObject(opcuaTag);
                subscription.addMonitoredItem(monitoredItem);
            });
            log.debug(
                    "Added monitored items: {}",
                    verifiedTags.stream()
                            .map(verified -> verified.tag().getDefinition().getNode())
                            .toList());
        }

        try {
            subscription.synchronizeMonitoredItems();
            log.info("All monitored items synchronized successfully");
            currentSubscription.set(subscription);
            requestConditionRefresh(subscription);
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
            final var tag = tagOf(items.get(i));
            if (tag == null) {
                continue;
            }
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
     * Receives condition transitions.
     * <p>
     * The counterpart to {@link #onDataReceived}: same subscription, same parallel lists, but each item
     * carries an event's selected fields rather than a single value. An event is a transition report — the
     * server fires one when a condition's state changes — so unlike a value there is nothing to sample and
     * nothing to deduplicate; every notification is published.
     */
    @Override
    public void onEventReceived(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcUaMonitoredItem> items,
            final @NotNull List<Variant[]> events) {
        lastKeepAliveTimestamp = System.currentTimeMillis();
        final var dataPointsPublisher = tagStreamingService.dataPointsPublisher();
        for (int i = 0; i < items.size(); i++) {
            final var tag = tagOf(items.get(i));
            if (tag == null) {
                continue;
            }
            final String tn = tag.getName();
            if (null == tagToFirstSeen.putIfAbsent(tag, true)) {
                eventService
                        .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                        .withSeverity(Event.SEVERITY.INFO)
                        .withMessage(String.format("Adapter '%s' received the first event for tag '%s'", adapterId, tn))
                        .fire();
            }
            try {
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_RECEIVED_COUNT);

                final var dataPointBuilder = dataPointsPublisher.addDataPoint(tag);
                OpcUaEventToJsonConverter.convertPayload(
                        client.getDynamicEncodingContext(),
                        tag.getDefinition().getConditionType(),
                        events.get(i),
                        dataPointBuilder);
            } catch (final @NotNull UaSerializationException e) {
                // Same failure mode as the value path: a structure nested in an event field cannot be
                // decoded because the dynamic codec registry is incomplete. Drop this batch and reset.
                protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_DATA_ERROR_COUNT);
                log.warn(
                        "Adapter '{}': could not decode OPC UA event for tag '{}', dropping the current events and resetting the dynamic type registry",
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
    /**
     * Re-reports the retained conditions after a session was re-established with its subscription intact.
     * <p>
     * This covers the reconnect that {@link #onTransferFailed} does not see. When a client reconnects, it
     * first tries to transfer the existing subscription to the new session. If that <em>succeeds</em> nothing
     * is recreated — so no monitored items are synchronized, and the refresh that rides on that would never
     * happen. That is the common case for a brief network drop, and precisely when the alarm picture is most
     * likely to have moved unseen.
     * <p>
     * The subscription is already live on the new session by the time the session reports itself active, so
     * the burst has somewhere to go.
     */
    public void onSessionReactivated() {
        final OpcUaSubscription subscription = currentSubscription.get();
        if (subscription != null) {
            requestConditionRefresh(subscription);
        }
    }

    /**
     * Asks the server to re-report every retained condition, once the monitored items are in place.
     * <p>
     * This is the seam the refresh has to hang on. It runs on both paths that establish monitored items —
     * the initial subscribe and the recreation after a failed subscription transfer — so a refresh follows
     * every connect and every reconnect. {@code onSessionActive} would be too early: the session activates
     * before the items exist, and a refresh burst with nothing subscribed to receive it is wasted.
     * <p>
     * Fire and forget, and deliberately never fatal. A server that does not implement
     * {@code ConditionRefresh}, or refuses it, still delivers live transitions perfectly well — losing the
     * initial picture is a degradation, not a reason to fail the subscription that was just established.
     */
    private void requestConditionRefresh(final @NotNull OpcUaSubscription subscription) {
        // The method has to be called on an object that offers it, and ConditionRefresh is defined on
        // ConditionType — so any subscribed condition serves as the entry point. The burst it triggers covers
        // every retained condition on the subscription, not just this one.
        final Optional<OpcuaTag> anyCondition = tags.stream()
                .filter(tag -> tag.getDefinition().getType() == OpcuaTagType.CONDITION)
                .findFirst();
        if (anyCondition.isEmpty()) {
            return;
        }
        final NodeId conditionNode =
                NodeId.parse(anyCondition.get().getDefinition().getNode());

        subscription.getSubscriptionId().ifPresent(subscriptionId -> {
            @SuppressWarnings("unused")
            final var unused = ConditionRefresh.request(client, conditionNode, subscriptionId)
                    .whenComplete((statusCode, throwable) -> {
                        if (throwable != null) {
                            log.warn(
                                    "Adapter '{}': could not refresh conditions, so the current alarm picture may be incomplete until each alarm next changes",
                                    adapterId,
                                    throwable);
                        } else if (statusCode.isBad()) {
                            log.warn(
                                    "Adapter '{}': the server refused a condition refresh ({}), so the current alarm picture may be incomplete until each alarm next changes",
                                    adapterId,
                                    statusCode);
                        } else {
                            log.debug(
                                    "Adapter '{}': requested a condition refresh on subscription {}",
                                    adapterId,
                                    subscriptionId);
                        }
                    });
        });
    }

    /**
     * The tag a notification belongs to, carried on the monitored item itself.
     * <p>
     * Null only for an item we did not create, or one left over from a subscription that has been replaced.
     * Dropping such a notification is right; publishing it under a guessed tag would be worse.
     */
    private @Nullable OpcuaTag tagOf(final @NotNull OpcUaMonitoredItem item) {
        return item.getUserObject()
                .filter(OpcuaTag.class::isInstance)
                .map(OpcuaTag.class::cast)
                .orElse(null);
    }

    /** A tag cleared for subscription, with the notifier its events arrive on if it is a condition. */
    private record VerifiedTag(
            @NotNull OpcuaTag tag, @Nullable NodeId notifier) {}

    /**
     * Whether a tag may be subscribed, and on what: present for anything that is not a condition, and for a
     * condition whose declared type the device satisfies and whose notifier could be found.
     * <p>
     * Deliberately total — every failure, including a timeout or an interrupt, answers empty rather than
     * throwing. This runs inside adapter start, where an escaping exception would abort the whole sequence.
     */
    private @NotNull Optional<VerifiedTag> verify(final @NotNull OpcuaTag opcuaTag) {
        if (opcuaTag.getDefinition().getType() != OpcuaTagType.CONDITION) {
            return Optional.of(new VerifiedTag(opcuaTag, null));
        }
        final String tagName = opcuaTag.getName();
        try {
            final ConditionTypeVerifier.Result result = ConditionTypeVerifier.verify(
                            client,
                            NodeId.parse(opcuaTag.getDefinition().getNode()),
                            opcuaTag.getDefinition().getConditionType(),
                            tagName)
                    .get(CONDITION_TYPE_VERIFICATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (result instanceof final ConditionTypeVerifier.Result.Rejected rejected) {
                reportUnsubscribableTag(tagName, rejected.reason());
                return Optional.empty();
            }

            // A condition is not an event notifier, so without a notifier there is nowhere to subscribe and
            // the tag simply cannot be honoured. Same outcome as a type mismatch: this tag alone is dropped.
            final NotifierResolver.Result notifier = NotifierResolver.resolve(
                            client,
                            NodeId.parse(opcuaTag.getDefinition().getNode()),
                            opcuaTag.getDefinition().getNotifierNode(),
                            tagName)
                    .get(CONDITION_TYPE_VERIFICATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (notifier instanceof final NotifierResolver.Result.NotFound notFound) {
                reportUnsubscribableTag(tagName, notFound.reason());
                return Optional.empty();
            }
            final NotifierResolver.Result.Found found = (NotifierResolver.Result.Found) notifier;
            log.debug(
                    "Adapter '{}': tag '{}' will receive events from notifier {} ({})",
                    adapterId,
                    tagName,
                    found.notifier(),
                    found.how());
            return Optional.of(new VerifiedTag(opcuaTag, found.notifier()));
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            reportUnsubscribableTag(tagName, "verification was interrupted");
            return Optional.empty();
        } catch (final Exception e) {
            reportUnsubscribableTag(tagName, "verification failed: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Tells the operator which tag was dropped and why, in the log and as an adapter event. A tag that
     * silently never produces data is far harder to diagnose than one that says why.
     */
    private void reportUnsubscribableTag(final @NotNull String tagName, final @NotNull String reason) {
        log.warn("Adapter '{}': not subscribing tag '{}' — {}", adapterId, tagName, reason);
        eventService
                .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withSeverity(Event.SEVERITY.WARN)
                .withMessage(String.format("Adapter '%s' did not subscribe tag '%s': %s", adapterId, tagName, reason))
                .fire();
    }

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
