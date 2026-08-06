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
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaEventToJsonConverter;
import com.hivemq.edge.adapters.opcua.northbound.OpcUaToJsonConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemServiceOperationResult;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
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

    /**
     * Set when the connection this handler belongs to has been closed, so work still in flight can stop.
     * <p>
     * A reconnect does not coordinate with a recovery already running: it stops the old connection and
     * builds a new one, which subscribes every tag from scratch. Whatever the old recovery was still doing
     * is then work on a client that has been disconnected and a subscription nobody holds — every remaining
     * call fails, and each failure is reported to the operator as a tag that cannot be subscribed, for tags
     * that are perfectly fine.
     * <p>
     * Checked between tags rather than inside the calls themselves. Most of the time the disconnected client
     * fails each call immediately, so the cost is spurious events rather than delay; but against a server
     * that is reachable and simply not answering — the state that produces a transfer failure in the first
     * place — each call waits its full timeout, and that is the case this bounds to one wait instead of one
     * per tag.
     */
    private final @NotNull AtomicBoolean abandoned = new AtomicBoolean();

    /**
     * Where a subscription rebuild runs, so it never occupies Milo's delivery queue.
     * <p>
     * Single-threaded on purpose: two rebuilds must not run at once, and one thread gives that by
     * construction rather than by locking. Daemon, so a rebuild in progress cannot hold the JVM open — the
     * work is an optimisation over letting the adapter's own reconnect rebuild everything, never something
     * worth delaying shutdown for.
     */
    private final @NotNull ExecutorService recoveryExecutor;

    /**
     * Whether a refresh requested by the <em>server</em> is still outstanding.
     * <p>
     * One {@code RefreshRequired} occurrence is delivered to every notifier item in the subscription, so this
     * collapses the copies into the single call the specification says they warrant. Only this path is
     * guarded: the connect-time and reconnect refreshes fire once by construction.
     */
    private final @NotNull AtomicBoolean refreshRequiredInFlight = new AtomicBoolean();

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
        this.recoveryExecutor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "opcua-subscription-recovery-" + adapterId);
            thread.setDaemon(true);
            return thread;
        });
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
            // The same split as when the item was created: an event item takes the event queue size and keeps
            // the sampling interval the SDK chose for it, a value item takes the value parameters. Applying
            // the value settings here would quietly undo that on the next synchronization.
            final OpcuaTag tag = tagOf(monitoredItem);
            if (tag != null && tag.getDefinition().getKind() != OpcuaTagKind.VALUE) {
                monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().eventQueueSize()));
                // Re-stated on every synchronization, for the same reason it is set at creation: OPC 10000-4
                // §5.13.1.2 requires a client subscribing for Events to ask for 0, and the field's default
                // is 1000.0 rather than 0.0.
                monitoredItem.setSamplingInterval(0.0);
            } else {
                monitoredItem.setQueueSize(uint(config.getOpcuaToMqttConfig().serverQueueSize()));
                monitoredItem.setSamplingInterval(config.getOpcuaToMqttConfig().publishingInterval());
            }
        });

        // add new monitored items
        if (!monitoredItemsToAdd.isEmpty()) {
            // A condition tag whose declared type does not match the device, or whose events have no notifier
            // to arrive on, is dropped here rather than subscribed. The check is per tag, so one bad tag
            // cannot stop the others -- or the adapter -- from starting.
            final List<VerifiedTag> verifiedTags = new ArrayList<>();
            for (int i = 0; i < monitoredItemsToAdd.size(); i++) {
                // Between tags rather than inside verify(): each tag costs up to two server round trips, so
                // a connection closed underneath us should not buy the remaining ones. Reported as a plain
                // failure, not as a tag problem -- these tags were never judged.
                if (abandoned.get()) {
                    log.info(
                            "Adapter '{}': abandoning monitored item sync, the connection was closed with {} of {} tags still to verify",
                            adapterId,
                            monitoredItemsToAdd.size() - i,
                            monitoredItemsToAdd.size());
                    return false;
                }
                verify(monitoredItemsToAdd.get(i)).ifPresent(verifiedTags::add);
            }

            verifiedTags.forEach(verified -> {
                final OpcuaTag opcuaTag = verified.tag();
                final NodeId nodeId = NodeId.parse(opcuaTag.getDefinition().getNode());
                // A condition is observed through its transitions, so it needs an event item carrying an
                // event filter; an ordinary value is observed directly through its Value attribute.
                final var monitoredItem =
                        switch (opcuaTag.getDefinition().getKind()) {
                            case CONDITION, EVENT_SUBSCRIPTION, REFRESH -> {
                                // All three are event items, but they name different nodes. A condition tag
                                // names the condition and its events arrive from a notifier above it, found
                                // by resolution. An event subscription tag names the notifier directly. A
                                // refresh tag names nothing of its own: it goes on the Server object, the
                                // root of the notifier hierarchy, because the events it wants are broadcast
                                // to every notifier item and cannot be selected by any filter.
                                final var eventItem = OpcUaMonitoredItem.newEventItem(
                                        eventItemNodeFor(opcuaTag, nodeId, verified.notifier()),
                                        eventFilterFor(opcuaTag, nodeId));
                                // Event parameters, not value parameters. queueSize means something different
                                // here: for an event item 1 asks for the smallest queue the server supports
                                // (OPC 10000-4 §7.21), where for a value item it means a single entry.
                                eventItem.setQueueSize(
                                        uint(config.getOpcuaToMqttConfig().eventQueueSize()));
                                // Stated rather than inherited. OPC 10000-4 §5.13.1.2 is a client-side
                                // requirement -- "A Client shall define a sampling interval of 0 if it
                                // subscribes for Events" -- and there is nothing to sample anyway, since an
                                // event is delivered when it fires. Milo's newEventItem factory does set
                                // 0.0 today, but the field's own default is 1000.0, so a refactor that
                                // built the item another way would violate a SHALL silently. One line
                                // makes the requirement visible where it applies.
                                eventItem.setSamplingInterval(0.0);
                                yield eventItem;
                            }
                            case VALUE -> {
                                final var dataItem = OpcUaMonitoredItem.newDataItem(nodeId);
                                dataItem.setQueueSize(
                                        uint(config.getOpcuaToMqttConfig().serverQueueSize()));
                                dataItem.setSamplingInterval(
                                        config.getOpcuaToMqttConfig().publishingInterval());
                                yield dataItem;
                            }
                        };
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
            return established(subscription);
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
                // Established on the same terms as the success path. The items that failed are lost; the
                // subscription they were being added to is not, and the ones that succeeded are streaming on
                // it. Reporting success without recording it would leave this handler's own state denying
                // the existence of a subscription the server is happily serving.
                return established(subscription);
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

    /**
     * Records a subscription as established, and does the work that follows from that.
     * <p>
     * Every path that reports monitored-item synchronization as successful goes through here, so a
     * {@code true} return and a recorded subscription cannot come apart. They did once: the partial-failure
     * branch returned {@code true} while skipping this, which left {@link #currentSubscription} null for the
     * life of the connection even though a subscription had been created and was streaming.
     * <p>
     * What that costs is not obvious from here, because nothing in this method is about refresh. The
     * subscription id is obtained when the subscription is <em>created</em>, well before any monitored item
     * exists; this reference is simply how the handler remembers it. Losing it means the handler's own state
     * denies a subscription the server is serving — so the connect-time refresh never fires, every
     * reconnect's refresh is silently skipped, and a southbound refresh request answers "no subscription is
     * established yet" about a subscription that demonstrably is. Any later reader asking "is there a
     * subscription?" would inherit the same wrong answer.
     *
     * @return always {@code true}, so a caller can {@code return established(subscription)}.
     */
    private boolean established(final @NotNull OpcUaSubscription subscription) {
        reportRevisedEventQueueSizes(subscription);
        currentSubscription.set(subscription);
        requestConditionRefresh(subscription);
        return true;
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
    /**
     * Marks this handler's connection as closed, so a recovery still running stops instead of finishing work
     * against a client that has been disconnected. Idempotent; safe to call from any thread.
     * <p>
     * {@code shutdown()} rather than {@code shutdownNow()}: a rebuild already under way is left to notice
     * the flag between tags and return, which is tidier than interrupting it mid-request. Nothing is awaited
     * — the caller is closing a connection whose replacement is already being built, and the executor's
     * thread is a daemon, so an unfinished rebuild cannot hold anything open.
     */
    public void abandon() {
        abandoned.set(true);
        recoveryExecutor.shutdown();
    }

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

    /**
     * Rebuilds the subscription after the server refused to transfer the old one to a new session.
     * <p>
     * The work is posted rather than done here. Milo delivers everything about a subscription through one
     * queue, one task at a time, and this callback runs on it — so anything done inline stops every
     * notification for that subscription, including the keep-alives Edge uses to decide the connection is
     * alive. Rebuilding is slow: {@code create()} is a blocking server call, and each condition tag then
     * costs two more with a ten-second ceiling apiece. Long enough that the health check would see stale
     * keep-alives and fire a reconnect against the recovery still running — the recovery's own duration
     * mistaken for a failure.
     * <p>
     * Posting returns the delivery queue immediately, so keep-alives keep flowing while the rebuild
     * proceeds. See EDG-878 for the wider question of whether this belongs here at all: the adapter's
     * reconnect path already rebuilds everything, and this method duplicates it.
     */
    @Override
    public void onTransferFailed(
            final @NotNull OpcUaSubscription brokenSubscription, final @NotNull StatusCode status) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_TRANSFER_FAILED_COUNT);
        log.error("Subscription Transfer failed, recreating subscription for adapter '{}'", adapterId);

        try {
            recoveryExecutor.execute(this::recreateSubscription);
        } catch (final RejectedExecutionException e) {
            // The connection was closed while this callback was in flight. A reconnect is already building a
            // replacement, so there is nothing to recover to.
            log.debug("Adapter '{}': not recreating the subscription, the connection is already closing", adapterId);
        }
    }

    /**
     * Creates a replacement subscription and re-establishes every monitored item on it. Runs on
     * {@link #recoveryExecutor}, never on Milo's delivery queue.
     */
    private void recreateSubscription() {
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
            // Four event types reach a monitored item regardless of its filter -- the three refresh types
            // (OPC 10000-9 §4.5) and the queue-overflow type (OPC 10000-4 §7.22). The where clause cannot
            // exclude them, so they are routed here: a refresh tag exists to publish them, and on any other
            // kind they are dropped. Published as a transition they would carry that tag's field list with
            // almost every value null, which reads as an alarm whose state is unknown.
            final boolean isControlEvent = isControlEvent(events.get(i));
            final boolean isRefreshTag = tag.getDefinition().getKind() == OpcuaTagKind.REFRESH;
            // RefreshRequired is the one control event that asks for something rather than reporting it, so
            // it is acted on before the publish decision -- on every kind of tag, including the ones that
            // drop it. Whether a user chose to see the event is unrelated to whether our alarm picture is
            // stale.
            if (isRefreshRequired(events.get(i))) {
                onRefreshRequired(subscription);
            }
            if (isControlEvent && !isRefreshTag) {
                continue;
            }
            if (!isControlEvent && isRefreshTag) {
                // Its filter admits nothing, so an ordinary event here would mean the server ignored the
                // where clause. Dropping rather than publishing keeps the tag's contract exact.
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
                // conditionType decides the published shape for both tag types, so decoding uses the same
                // field list the select clause was built from. Event fields arrive positionally against that
                // list, which is what keeps this correct.
                OpcUaEventToJsonConverter.convertPayload(
                        client.getDynamicEncodingContext(),
                        tag.getDefinition().getType(),
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
        // Any event item is worth refreshing, not only a condition tag: an event subscription tag monitors
        // conditions too, so a query-only adapter needs the refresh just as much. The call itself names no
        // node of ours -- both the object and the method are fixed by the specification -- so there is no
        // entry point to find, only a reason to bother asking.
        final boolean hasEventItems =
                tags.stream().anyMatch(tag -> tag.getDefinition().getKind() != OpcuaTagKind.VALUE);
        if (!hasEventItems) {
            return;
        }

        subscription.getSubscriptionId().ifPresent(subscriptionId -> {
            @SuppressWarnings("unused")
            final var unused = ConditionRefresh.request(client, subscriptionId)
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
     * Requests a refresh right now, for a southbound write to a refresh tag.
     * <p>
     * Distinct from {@link #requestConditionRefresh} — that one fires automatically after monitored items are
     * established and swallows its outcome, because a server refusing it is a degradation rather than a
     * failure. Here the caller asked, so the outcome is theirs to see.
     *
     * @return the status of the call, or empty when no subscription is live to refresh.
     */
    public @NotNull Optional<CompletableFuture<StatusCode>> requestConditionRefreshNow() {
        final OpcUaSubscription subscription = currentSubscription.get();
        if (subscription == null) {
            return Optional.empty();
        }
        return subscription.getSubscriptionId().map(subscriptionId -> ConditionRefresh.request(client, subscriptionId));
    }

    /**
     * The node an event item is placed on, which differs by kind.
     *
     * @param nodeId   the tag's own node, already parsed.
     * @param notifier the notifier resolved for a condition tag, null for the other kinds.
     */
    private static @NotNull NodeId eventItemNodeFor(
            final @NotNull OpcuaTag opcuaTag, final @NotNull NodeId nodeId, final @Nullable NodeId notifier) {
        return switch (opcuaTag.getDefinition().getKind()) {
            // A condition is not itself a notifier, so its events come from one above it.
            case CONDITION -> Objects.requireNonNull(notifier);
            // The Server object is a notifier by convention and the root of the notifier hierarchy, so an
            // item there sees the refresh bracket the server broadcasts to every notifier item.
            case REFRESH -> NodeIds.Server;
            // A query names its notifier directly; a value item never reaches here.
            case EVENT_SUBSCRIPTION, VALUE -> nodeId;
        };
    }

    /** The event filter for an event item, which differs by kind. */
    private static @NotNull EventFilter eventFilterFor(final @NotNull OpcuaTag opcuaTag, final @NotNull NodeId nodeId) {
        return switch (opcuaTag.getDefinition().getKind()) {
            case EVENT_SUBSCRIPTION -> queryFilterFor(opcuaTag);
            case REFRESH -> ConditionEventFilters.forRefresh();
            case CONDITION, VALUE ->
                ConditionEventFilters.forCondition(
                        nodeId, opcuaTag.getDefinition().getType());
        };
    }

    /**
     * The event filter for an event subscription tag, translated from its definition.
     * <p>
     * Each of the three narrowing dimensions is independently optional, so a tag that names none of them is a
     * legitimate request for everything the notifier carries. {@code conditionType} doubles as the type
     * filter here: on a query tag it says which events to accept rather than what the node is.
     */
    private static @NotNull EventFilter queryFilterFor(final @NotNull OpcuaTag opcuaTag) {
        final OpcuaTagDefinition definition = opcuaTag.getDefinition();
        return ConditionEventFilters.forQuery(
                parseOrNull(definition.getSourceNode()),
                parseOrNull(definition.getConditionNode()),
                definition.getFilterType(),
                definition.getType());
    }

    private static @Nullable NodeId parseOrNull(final @Nullable String nodeId) {
        return nodeId == null ? null : NodeId.parse(nodeId);
    }

    /**
     * The event types that reach a monitored item whether or not its filter admits them.
     * <p>
     * Three refresh types bracket or request a {@code ConditionRefresh} and are copied to <em>every</em>
     * notifier item in the subscription (OPC 10000-9 §4.5, §5.5.7); the overflow type is delivered only to
     * the one item whose queue overflowed (OPC 10000-4 §5.12.1.5, §7.22). Both families bypass the where
     * clause, which is why they are dropped here rather than filtered at the server.
     */
    private static final @NotNull Set<NodeId> CONTROL_EVENT_TYPES = Set.of(
            NodeIds.RefreshStartEventType,
            NodeIds.RefreshEndEventType,
            NodeIds.RefreshRequiredEventType,
            NodeIds.EventQueueOverflowEventType);

    /**
     * Acts on a {@code RefreshRequiredEventType} by asking the server to re-report its retained conditions.
     * <p>
     * The server sends this when it cannot guarantee the client is still in sync — its link to the underlying
     * system reset, an event queue overflowed and drained, a redundant pair failed over. OPC 10000-9 §4.5: "A
     * Client receiving this special Event should initiate a ConditionRefresh". Nothing else recovers from it:
     * our own session stays healthy throughout, so no reconnect path fires, and without this the alarm
     * picture stays stale until every affected condition happens to change state again.
     * <p>
     * <b>Coalesced, because one server-side event arrives many times.</b> Like the rest of the refresh family
     * it bypasses the where clause and is copied to every notifier item in the subscription, so an adapter
     * with ten condition tags sees ten copies of one occurrence. Refreshing per copy would be wrong twice
     * over: §4.5 says "ConditionRefresh applies to a Subscription [...] all Event Notifiers are refreshed",
     * so one call already covers them all, and the second call would collide with the first — the
     * specification defines {@code Bad_RefreshInProgress} for exactly that. The guard is released when the
     * call settles rather than when the burst ends, which is the conservative choice: a later
     * {@code RefreshRequired} is a fresh reason to resynchronise and must not be swallowed.
     */
    private void onRefreshRequired(final @NotNull OpcUaSubscription subscription) {
        if (!refreshRequiredInFlight.compareAndSet(false, true)) {
            return;
        }
        final Optional<UInteger> subscriptionId = subscription.getSubscriptionId();
        if (subscriptionId.isEmpty()) {
            refreshRequiredInFlight.set(false);
            return;
        }
        log.info(
                "Adapter '{}': the server reported that a condition refresh is required, so the current alarm picture is being re-requested",
                adapterId);
        // The guard is released on every path out of here, including a synchronous throw. Releasing it only
        // from whenComplete would leave it set forever if the request never produced a future -- and since
        // the guard's whole job is to make the compareAndSet above skip duplicate calls, a stuck guard
        // silently drops every RefreshRequired the server sends for the rest of the connection. Silent
        // because the skip is a bare return: no log, no event, and the alarm picture simply stops being
        // resynchronised.
        try {
            @SuppressWarnings("unused")
            final var unused = ConditionRefresh.request(client, subscriptionId.get())
                    .whenComplete((statusCode, throwable) -> {
                        refreshRequiredInFlight.set(false);
                        if (throwable != null) {
                            log.warn(
                                    "Adapter '{}': the server asked for a condition refresh but the request failed, so the alarm picture may stay incomplete until each alarm next changes",
                                    adapterId,
                                    throwable);
                        } else if (statusCode.isBad()) {
                            log.warn(
                                    "Adapter '{}': the server asked for a condition refresh and then refused it ({}), so the alarm picture may stay incomplete until each alarm next changes",
                                    adapterId,
                                    statusCode);
                        }
                    });
        } catch (final Exception e) {
            refreshRequiredInFlight.set(false);
            log.warn(
                    "Adapter '{}': the server asked for a condition refresh but the request could not be sent, so the alarm picture may stay incomplete until each alarm next changes",
                    adapterId,
                    e);
        }
    }

    /**
     * Whether a notification is the server asking for a refresh, as opposed to the other control events.
     * <p>
     * Separate from {@link #isControlEvent} because the two answer different questions: that one decides
     * whether to publish, this one decides whether to act.
     */
    private static boolean isRefreshRequired(final @NotNull Variant @NotNull [] eventFields) {
        return NodeIds.RefreshRequiredEventType.equals(eventTypeOf(eventFields));
    }

    /**
     * Whether a notification is one of the control events rather than a transition report.
     * <p>
     * {@code EventType} is read positionally: it is part of {@code BASE_EVENT_FIELDS}, which every select
     * clause begins with, so its index is the same for every tag whatever type it declares.
     */
    private static boolean isControlEvent(final @NotNull Variant @NotNull [] eventFields) {
        final NodeId typeId = eventTypeOf(eventFields);
        return typeId != null && CONTROL_EVENT_TYPES.contains(typeId);
    }

    /** The notification's {@code EventType}, or null when it is absent or not a node id. */
    private static @Nullable NodeId eventTypeOf(final @NotNull Variant @NotNull [] eventFields) {
        final int eventTypeIndex = OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventType");
        if (eventTypeIndex < 0 || eventTypeIndex >= eventFields.length) {
            return null;
        }
        final Variant eventType = eventFields[eventTypeIndex];
        return eventType != null && eventType.value() instanceof final NodeId typeId ? typeId : null;
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

    /**
     * Says what the server actually granted for each event item's queue, when it granted less than was asked.
     * <p>
     * The depth an event item ends up with is the server's decision, not ours — the specification lets it
     * bound the request either way, and implementations differ in what they consider reasonable. Since a
     * dropped event is a transition report that is never re-sent, an operator is better served knowing the
     * queue is shallower than configured than discovering it as missing alarms. Nothing is logged when the
     * request was met in full, which is the ordinary case.
     */
    private void reportRevisedEventQueueSizes(final @NotNull OpcUaSubscription subscription) {
        final int requested = config.getOpcuaToMqttConfig().eventQueueSize();
        subscription.getMonitoredItems().forEach(item -> {
            final OpcuaTag tag = tagOf(item);
            if (tag == null || tag.getDefinition().getKind() == OpcuaTagKind.VALUE) {
                return;
            }
            item.getRevisedQueueSize().ifPresent(revised -> {
                if (revised.longValue() < requested) {
                    log.warn(
                            "Adapter '{}': the server granted an event queue of {} for tag '{}', not the {} requested. Transitions arriving faster than the queue can hold are dropped, and an event is never re-sent.",
                            adapterId,
                            revised,
                            tag.getName(),
                            requested);
                }
            });
        });
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
        // Only a condition tag needs either check. A value tag needs neither, and an event subscription tag
        // names its notifier directly -- there is nothing to resolve, and no single declared type to verify,
        // since the point of the tag is that many conditions of possibly differing types pass its filter.
        if (opcuaTag.getDefinition().getKind() != OpcuaTagKind.CONDITION) {
            return Optional.of(new VerifiedTag(opcuaTag, null));
        }
        final String tagName = opcuaTag.getName();
        try {
            final ConditionTypeVerifier.Result result = ConditionTypeVerifier.verify(
                            client,
                            NodeId.parse(opcuaTag.getDefinition().getNode()),
                            opcuaTag.getDefinition().getType(),
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
        } catch (final TimeoutException e) {
            // Named separately because it says something different from every other failure here, and
            // because TimeoutException.getMessage() is null -- the generic branch below would report
            // "verification failed: null", which tells an operator nothing at all.
            //
            // The outcome is still a permanent drop until the adapter restarts, which is the honest thing to
            // say rather than to hide: unlike a type mismatch or a missing notifier, the tag may be
            // perfectly good and merely asked at a bad moment.
            reportUnsubscribableTag(
                    tagName,
                    "the server did not answer within " + CONDITION_TYPE_VERIFICATION_TIMEOUT_MS
                            + "ms, so the tag could not be verified. Unlike a type or notifier problem this "
                            + "may be transient: restart the adapter to try again");
            return Optional.empty();
        } catch (final Exception e) {
            reportUnsubscribableTag(tagName, "verification failed: " + describe(e));
            return Optional.empty();
        }
    }

    /**
     * An exception as something an operator can read. Falls back to the class name where there is no
     * message: several of the exceptions reachable here carry none, and a reason reading "null" is worse
     * than useless — it looks like a bug in Edge rather than a description of what happened.
     */
    private static @NotNull String describe(final @NotNull Exception e) {
        final String message = e.getMessage();
        return message != null && !message.isBlank() ? message : e.getClass().getSimpleName();
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
