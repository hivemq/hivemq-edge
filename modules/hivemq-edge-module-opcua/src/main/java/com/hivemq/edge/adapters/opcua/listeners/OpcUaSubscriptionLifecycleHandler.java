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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemServiceOperationResult;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.UaSerializationException;
import org.eclipse.milo.opcua.stack.core.types.builtin.ByteString;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilter;
import org.eclipse.milo.opcua.stack.core.types.structured.EventFilterResult;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaSubscriptionLifecycleHandler implements OpcUaSubscription.SubscriptionListener {

    public static final long KEEP_ALIVE_SAFETY_MARGIN_MS = 5_000L;
    public static final long TYPE_REGISTRY_RESET_THROTTLE_MS = 10_000L;

    private static final long TYPE_REGISTRY_RESET_THROTTLE_NANOS =
            TimeUnit.MILLISECONDS.toNanos(TYPE_REGISTRY_RESET_THROTTLE_MS);
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaSubscriptionLifecycleHandler.class);
    private static final int MAX_MONITORED_ITEM_COUNT = 5;

    /**
     * How many {@code RefreshRequired} {@code EventId}s to remember when collapsing the copies of one
     * occurrence. See {@link #isFirstSightOf} for why this is bounded by count rather than by age.
     */
    private static final int MAX_REMEMBERED_REFRESH_REQUESTS = 64;

    /**
     * How many times in a row a refresh may be retried before the reason is dropped.
     * <p>
     * Only failures a retry could actually answer are counted — see {@link #requeueIfWorthRetrying} — so this
     * bounds a collision with a concurrent refresh, which resolves within a round trip. Three is enough for
     * that and small enough that a server refusing indefinitely costs three calls rather than a hot loop.
     */
    @VisibleForTesting
    static final int MAX_REFRESH_RETRIES = 3;

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
     * Whether a {@code ConditionRefresh} call is outstanding, for any reason at all.
     * <p>
     * <b>Every</b> mandatory refresh takes this guard — the connect-time one, the one after a reconnect, and
     * the one the server asks for. It used to guard only the server-requested path, on the reasoning that the
     * other two "fire once by construction". Each does fire once; what that argument misses is that they can
     * fire while <em>another</em> reason's call is still outstanding, and OPC 10000-9 §5.5.7 defines
     * {@code Bad_RefreshInProgress} for precisely that overlap. A reconnect landing on top of a
     * {@code RefreshRequired} still in flight had one of the two refused, and the automatic path had no
     * pending flag to leave set and no retry — so the reconnect could complete with no successful refresh,
     * against a requirement that says one follows <em>every</em> reconnect.
     * <p>
     * The southbound manual refresh is deliberately outside this: the caller asked, so a refusal is theirs to
     * see rather than something to queue behind work they did not ask for. It can still collide, which is
     * what {@link #consecutiveRefreshFailures} is for.
     */
    private final @NotNull AtomicBoolean refreshInFlight = new AtomicBoolean();

    /**
     * Why a refresh is owed that no call has covered yet, or null when none is.
     * <p>
     * The in-flight guard alone cannot express this. It answers "is a call outstanding", and a distinct reason
     * arriving while one is has to be remembered rather than dropped: the call already running was started for
     * an earlier reason and may well have been answered by the server before the later reason existed. Without
     * somewhere to record it, a second {@code RefreshRequired} was marked handled by {@link #isFirstSightOf}
     * and then discarded by the in-flight guard, so nothing was left to retry it and the alarm picture could
     * stay stale for the rest of the connection.
     * <p>
     * One slot rather than a queue, because §4.5 makes a {@code ConditionRefresh} subscription-wide: one call
     * covers every reason outstanding at the moment it starts, so several pending reasons genuinely do
     * collapse into one. What must not collapse is a reason that arrived <em>after</em> the covering call
     * began, and that is exactly what the slot being filled again expresses. The reason itself is carried only
     * so the log can say which; the call it produces is identical either way.
     */
    private final @NotNull AtomicReference<RefreshReason> pendingRefresh = new AtomicReference<>();

    /**
     * How many refresh attempts have failed in a row without one succeeding in between.
     * <p>
     * A refused or failed call consumed its reason and left nothing to retry it. That is wrong for the one
     * status a retry actually answers: {@code Bad_RefreshInProgress} says another refresh is running <em>right
     * now</em>, which is a transient fact — the colliding call is one round trip from finishing. Requeueing
     * costs one more call and gets the mandatory refresh the reconnect contract promises.
     * <p>
     * Bounded, because the alternative is a hot loop. A server that answers {@code Bad_RefreshInProgress}
     * indefinitely would otherwise be asked again the instant each refusal arrives, for the life of the
     * connection. Reset whenever a fresh reason is recorded or a call succeeds, so the bound applies to one
     * unbroken run of failures rather than to the connection.
     */
    private final @NotNull AtomicInteger consecutiveRefreshFailures = new AtomicInteger();

    /**
     * The {@code EventId}s of {@code RefreshRequired} occurrences already acted on, most recent last.
     * <p>
     * The in-flight guard above collapses copies only while a call is outstanding, which is not the same
     * question as whether two notifications are the same event — see {@link #isFirstSightOf}. Guarded by its
     * own monitor rather than made concurrent: {@code onEventReceived} is the only caller and Milo delivers
     * a subscription's notifications one at a time, so contention is theoretical and a
     * {@code LinkedHashSet} with an explicit bound is the clearer statement of what this holds.
     */
    private final @NotNull Set<ByteString> handledRefreshRequests = new LinkedHashSet<>();

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
                .flatMap(this::establishInitial);
    }

    /**
     * Installs a freshly created subscription on the initial connect, or disposes of it if it cannot carry
     * the tags.
     * <p>
     * The counterpart of {@link #establishReplacement} for the connect path, and package-private for the same
     * reason: reaching it honestly needs a {@code create()} that succeeds, which needs a live server.
     *
     * @return the subscription, or empty when it could not be established — in which case it has already been
     *         deleted, because an empty answer here is the last anyone sees of it.
     */
    @NotNull
    Optional<OpcUaSubscription> establishInitial(final @NotNull OpcUaSubscription subscription) {
        subscription.setSubscriptionListener(this);
        if (syncTagsAndMonitoredItems(subscription, tags, config)) {
            return Optional.of(subscription);
        }
        // Answering empty is not the same as never having created one. The subscription exists on the server
        // from the moment create() returned, and this reference is the last one -- the caller gets an empty
        // Optional and has nothing left to clean up with.
        discardSubscription(subscription);
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
    private boolean syncTagsAndMonitoredItems(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcuaTag> tags,
            final @NotNull OpcUaSpecificAdapterConfig config) {

        // Reconciled by tag, not by node id. Each monitored item carries its own tag in Milo's userObject
        // slot, so both sides of the comparison can name a tag directly and nothing has to be looked up.
        //
        // Node id is not an identity here. The configuration enforces unique tag *names* only, so two tags
        // may carry the same definition -- and keyed by node the second was silently discarded by a
        // `(first, second) -> first` merge before it could be subscribed. Measured, not inferred: two tags
        // on one node produced one monitored item, on both the value and condition paths, with no log and
        // no event. The user saw a green adapter and one of their tags never producing data.
        //
        // A second reason it was the wrong key, latent rather than live: a condition tag's item is created
        // on the notifier above the condition, not on the condition itself, so the tag's node and its item's
        // node differ. Nothing reaches that today, because every sync starts from a subscription with no
        // monitored items -- the initial connect creates one, and onTransferFailed creates a replacement
        // rather than reusing the broken one. Keying by tag removes the trap either way.
        //
        // The dispatch path stopped keying by node id in 6b83694c5; these two maps were the last of it.
        final Set<OpcuaTag> wanted = new LinkedHashSet<>(tags);
        final Set<OpcuaTag> subscribed = subscription.getMonitoredItems().stream()
                .map(this::tagOf)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // An item with no tag of ours is one we did not create -- it cannot be matched to configuration, so
        // it is left alone rather than removed.
        final var monitoredItemsToRemove = subscription.getMonitoredItems().stream()
                .filter(item -> {
                    final OpcuaTag tag = tagOf(item);
                    return tag != null && !wanted.contains(tag);
                })
                .toList();
        final var monitoredItemsToAdd =
                tags.stream().filter(tag -> !subscribed.contains(tag)).toList();

        // clear deleted monitored items
        if (!monitoredItemsToRemove.isEmpty()) {
            subscription.removeMonitoredItems(monitoredItemsToRemove);
            log.debug(
                    "Removed monitored items: {}",
                    monitoredItemsToRemove.stream()
                            .map(item -> item.getReadValueId().getNodeId())
                            .toList());
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
                // Already parsed, inside verify()'s per-tag boundary. Nothing here may throw: this loop is
                // outside that boundary, so a failure would abort the sync for every tag.
                final NodeId nodeId = verified.node();
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
                                        eventFilterFor(verified));
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
        reportRejectedSelectClauses(subscription);
        // Before the refresh, and it has to stay that way: the refresh reads the current subscription at the
        // moment it makes its call rather than being handed one, so the generation must already be installed.
        installSubscription(subscription);
        requestConditionRefresh(RefreshReason.ESTABLISHED);
        return true;
    }

    /**
     * Makes a subscription the current generation, and resets what belonged to the previous one.
     * <p>
     * The remembered {@code EventId}s are the state that has to move with it. They are a per-generation fact:
     * a new subscription is a new conversation with the server, which may well re-report a
     * {@code RefreshRequired} the old one already handled, and suppressing that as a duplicate would be
     * answering on behalf of a subscription that no longer exists. Carrying them across also let the set act
     * as a memory of the whole connection rather than of one subscription.
     * <p>
     * Separate from {@link #established} so that the two facts — "this is now the current subscription" and
     * "the previous generation's memory is gone" — cannot come apart, and so that a test can install a
     * subscription without also having to fake revised queue sizes and rejected select clauses.
     */
    private void installSubscription(final @NotNull OpcUaSubscription subscription) {
        synchronized (handledRefreshRequests) {
            handledRefreshRequests.clear();
        }
        currentSubscription.set(subscription);
    }

    /**
     * Installs a subscription as the current generation, for tests that need one without a server.
     * <p>
     * The counterpart of {@link #currentSubscriptionForTesting()}, and it exists for the same reason: the
     * generation is what several behaviours here are <em>about</em> — which subscription a refresh is sent
     * to, and which notifications are stale — and none of that is observable on a handler that has never
     * established one.
     */
    void installSubscriptionForTesting(final @NotNull OpcUaSubscription subscription) {
        installSubscription(subscription);
    }

    /**
     * Receives the server's "nothing has changed" notification.
     * <p>
     * Guarded like the two delivery callbacks, because it writes the one field {@link #isKeepAliveHealthy()}
     * answers from. A keep-alive is evidence that <em>a</em> subscription is alive, and a superseded one being
     * alive says nothing about the one that replaced it — so letting a dead generation's queued keep-alive
     * through would report the live subscription as healthy on the strength of the wrong one.
     */
    @Override
    public void onKeepAliveReceived(final @NotNull OpcUaSubscription subscription) {
        if (hasBeenReplaced(subscription)) {
            return;
        }
        lastKeepAliveTimestamp = System.currentTimeMillis();
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_KEEPALIVE_COUNT);
        subscription
                .getSubscriptionId()
                .ifPresent(sid -> log.debug("Keep-alive received for subscription {} of adapter '{}'", sid, adapterId));
    }

    /**
     * The subscription currently established, or null if none is. Visible for tests that need to observe the
     * outcome of a rebuild, which happens on {@link #recoveryExecutor} and so cannot be awaited directly.
     */
    @Nullable
    OpcUaSubscription currentSubscriptionForTesting() {
        return currentSubscription.get();
    }

    /**
     * Marks this handler's connection as closed, so a recovery still running stops instead of finishing work
     * against a client that has been disconnected. Idempotent; safe to call from any thread.
     * <p>
     * {@code shutdown()} rather than {@code shutdownNow()}: a rebuild already under way is left to notice
     * the flag and return, which is tidier than interrupting it mid-request — Milo's blocking calls are not
     * interruption-aware, so an interrupt could as easily leave a half-created subscription on the server as
     * stop anything. Nothing is awaited — the caller is closing a connection whose replacement is already
     * being built, and the executor's thread is a daemon, so an unfinished rebuild cannot hold anything open.
     * <p>
     * The cost of {@code shutdown()} is that a rebuild <em>queued</em> at this moment still runs, and the
     * review's alternative was to hold the submitted {@code Future} and cancel it. The flag it would race is
     * the same flag {@link #recreateSubscription} now reads on entry, so a cancellation would only ever win
     * the cases that check already covers, at the cost of a second piece of state to keep in step with it.
     */
    public void abandon() {
        abandoned.set(true);
        recoveryExecutor.shutdown();
    }

    /**
     * Whether {@link #abandon()} has been called — that is, whether the connection underneath this handler is
     * being torn down.
     * <p>
     * The counterpart of {@code abandon()} rather than a test accessor: the flag is what several behaviours
     * here are <em>about</em>, and a caller that can set it can reasonably ask what it is. Used by the
     * connection to answer whether a teardown reached its handler, which is the whole property of
     * publishing the handler before the verification it has to be able to interrupt.
     */
    public boolean isAbandoned() {
        return abandoned.get();
    }

    /**
     * Whether keep-alive messages are arriving within the expected timeout, which is computed from
     * {@code ConnectionOptions}. Used for health monitoring, to detect a subscription that has gone quiet.
     *
     * @return true if the last keep-alive was received within the computed timeout.
     */
    public boolean isKeepAliveHealthy() {
        return (System.currentTimeMillis() - lastKeepAliveTimestamp) < getKeepAliveTimeoutMs();
    }

    /**
     * When a notification was last accepted. Visible for the tests that pin which notifications may move it,
     * because {@link #isKeepAliveHealthy()} cannot stand in for it: the timeout has a fixed five-second floor
     * in {@link #KEEP_ALIVE_SAFETY_MARGIN_MS}, so asking the health question instead would cost five seconds
     * of wall clock per assertion about one field.
     */
    long lastKeepAliveTimestampForTesting() {
        return lastKeepAliveTimestamp;
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

        // Forget the broken subscription before trying to replace it, so nothing keeps using it if the
        // replacement never arrives. It is not merely stale: the server refused to transfer it, so its id
        // names nothing on the new session. Left in place -- as it was -- a failed rebuild leaves every
        // later reader believing a subscription is established: onSessionReactivated() would request a
        // refresh against the dead id, and a southbound write to a refresh tag would report success on a
        // call that cannot land. `established()` installs the replacement only once it is genuinely
        // established, so clearing here cannot race a successful rebuild into oblivion.
        //
        // compareAndSet rather than set: a rebuild that already finished has installed its replacement, and
        // that one must survive.
        //
        // And the result decides whether there is anything to rebuild. A failed swap says this callback is
        // about a subscription the handler has already moved on from -- either a replacement is established,
        // or an earlier callback about this same one already cleared it and its rebuild is under way. Either
        // way the work is done or in hand, and doing it again is not merely wasteful. The rebuild ends at
        // established(), which overwrites the reference: the healthy subscription installed a moment ago
        // would be forgotten while still holding its listener and its monitored items, so it would go on
        // publishing every transition a second time, on a server subscription nothing left here can delete.
        if (!currentSubscription.compareAndSet(brokenSubscription, null)) {
            log.debug(
                    "Adapter '{}': ignoring a transfer failure for a subscription that is no longer the current one; a replacement is already established or being built",
                    adapterId);
            return;
        }

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
     * <p>
     * <b>Nothing may escape this method.</b> It is the whole body of an executor task, so an exception thrown
     * out of it goes to the thread's uncaught handler — printed to {@code System.err} by a daemon thread named
     * after the adapter, and nowhere else. {@link #reportRecoveryFailed} is skipped, so the operator is never
     * told that the adapter has stopped monitoring; the executor quietly replaces the dead worker; and the
     * task counts as having run. That is how a rebuild racing a closed client — {@code getTransport()} is null
     * by then, so Milo's subscription constructor throws — used to look like nothing at all.
     * <p>
     * The {@code abandoned} checks bracket the one blocking step this method owns. Entry covers the task that
     * was still queued when the connection closed, which {@code shutdown()} runs anyway; the check inside
     * {@link #establishReplacement} covers the connection closing <em>during</em> the create, which the entry
     * check cannot see. Between tags is already handled by {@code syncTagsAndMonitoredItems}.
     */
    private void recreateSubscription() {
        if (abandoned.get()) {
            log.debug(
                    "Adapter '{}': not rebuilding the subscription, the connection was closed before the rebuild started",
                    adapterId);
            return;
        }
        try {
            newSubscription(client)
                    .publishingInterval(config.getOpcuaToMqttConfig().publishingInterval())
                    .create()
                    .ifPresentOrElse(
                            this::establishReplacement,
                            () -> reportRecoveryFailed("a replacement subscription could not be created"));
        } catch (final Exception e) {
            if (abandoned.get()) {
                // Expected, and not the operator's problem: the client this was building against has been
                // disconnected, and the reconnect that disconnected it is building a replacement of its own.
                log.debug(
                        "Adapter '{}': the subscription rebuild failed after the connection was closed, which is the ordinary outcome of that race",
                        adapterId,
                        e);
                return;
            }
            log.error("Adapter '{}': the subscription rebuild threw", adapterId, e);
            reportRecoveryFailed("the rebuild failed with " + e);
        }
    }

    /**
     * Installs a freshly created replacement, unless the connection went away while it was being created.
     * <p>
     * Package-private so the abandonment branch can be exercised: reaching it honestly needs a create that
     * succeeds, which needs a live server, and the case under test is one where the client is already gone.
     */
    void establishReplacement(final @NotNull OpcUaSubscription replacementSubscription) {
        if (abandoned.get()) {
            log.info(
                    "Adapter '{}': discarding a replacement subscription, the connection was closed while it was being created",
                    adapterId);
            discardSubscription(replacementSubscription);
            return;
        }
        // reconnect the listener with the new subscription
        replacementSubscription.setSubscriptionListener(this);
        if (syncTagsAndMonitoredItems(replacementSubscription, tags, config)) {
            return;
        }
        // Nothing streams on this one and nothing here will hold it: the false answer means either that no
        // monitored item survived at all, or that the connection closed part-way through. A partial success
        // takes the other branch and is established, so this cannot be throwing away a working subscription.
        discardSubscription(replacementSubscription);
        if (abandoned.get()) {
            log.debug(
                    "Adapter '{}': the subscription rebuild stopped because the connection was closed while its monitored items were being re-established",
                    adapterId);
            return;
        }
        reportRecoveryFailed("its monitored items could not be re-established");
    }

    /**
     * Detaches a subscription that will not be used, and asks the server to forget it.
     * <p>
     * Called from every path that creates a subscription and then decides against it, which is the shape the
     * failure has: the subscription exists on the server from the moment {@code create()} returns, whether or
     * not a single monitored item was ever established on it. Nothing else can clean it up, because
     * {@link #currentSubscription} only ever holds one that reached {@code established()} — an abandoned one
     * is unreachable from here the instant the method returns, and would sit on the server until the session
     * ends.
     * <p>
     * Three steps, in this order, and each is doing something the others do not.
     * <ol>
     *   <li>The listener comes off first. This object is the listener, so anything arriving in the meantime
     *       would be routed into a handler that has stopped — published as a transition on a tag whose
     *       subscription is being thrown away.</li>
     *   <li>The delete releases it on the <em>server</em>, which is the part nothing local can substitute
     *       for.</li>
     *   <li>{@code reset()} on failure, because Milo only deregisters on success: its {@code delete()} calls
     *       {@code reset()} when the server answers Good (or {@code Bad_SubscriptionIdInvalid}) and throws
     *       otherwise, so a subscription that could not be deleted stays registered with the client and its
     *       publishing manager, keeps its watchdog timer, and goes on receiving publish responses. The
     *       server-side subscription is beyond reach at that point, but the client-side one need not be.</li>
     * </ol>
     * A failed delete is otherwise logged and no more: the session it belongs to is being closed or has
     * already failed, and there is no second thing to try.
     */
    private void discardSubscription(final @NotNull OpcUaSubscription subscription) {
        subscription.setSubscriptionListener(null);
        try {
            subscription.delete();
        } catch (final Exception e) {
            log.warn(
                    "Adapter '{}': a discarded subscription could not be deleted, so it may remain on the server until the session ends",
                    adapterId,
                    e);
            subscription.reset();
        }
    }

    /**
     * Says that the rebuild after a failed transfer did not produce a usable subscription.
     * <p>
     * Reported rather than only logged because the adapter is left without one: no condition refresh will
     * fire, and a southbound refresh request will answer that nothing is established. The adapter's own
     * reconnect path is what recovers from here — this is the notice that it needs to.
     */
    private void reportRecoveryFailed(final @NotNull String what) {
        final String message = String.format(
                "Adapter '%s' could not rebuild its OPC UA subscription after the server refused to "
                        + "transfer the old one: %s. Conditions are not being monitored until the adapter "
                        + "reconnects.",
                adapterId, what);
        log.error(message);
        eventService
                .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withSeverity(Event.SEVERITY.ERROR)
                .withMessage(message)
                .fire();
    }

    /**
     * Receives value samples.
     * <p>
     * The generation guard matters more here than on the event path, and for a reason events do not have: a
     * value is a state rather than a transition, so a sample from a superseded subscription is not a duplicate
     * of something the replacement also reports — it is an <em>older</em> reading published after a newer one.
     * A data point carries no generation, and its own source timestamp is not what an arrival-ordered
     * consumer reads, so downstream state moves backwards with nothing in the payload saying why.
     */
    @Override
    public void onDataReceived(
            final @NotNull OpcUaSubscription subscription,
            final @NotNull List<OpcUaMonitoredItem> items,
            final @NotNull List<DataValue> values) {
        if (hasBeenReplaced(subscription)) {
            return;
        }
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
        if (hasBeenReplaced(subscription)) {
            return;
        }
        lastKeepAliveTimestamp = System.currentTimeMillis();
        final var dataPointsPublisher = tagStreamingService.dataPointsPublisher();
        for (int i = 0; i < items.size(); i++) {
            final var tag = tagOf(items.get(i));
            if (tag == null) {
                continue;
            }
            // Read once. Three of the four questions below are about this one notification's EventType, and
            // resolving it per question meant decoding the same field three times for every alarm delivered.
            final Variant[] eventFields = events.get(i);
            final NodeId eventType = eventTypeOf(eventFields);
            // Four event types reach a monitored item regardless of its filter -- the three refresh types
            // (OPC 10000-9 §4.5) and the queue-overflow type (OPC 10000-4 §7.22). The where clause cannot
            // exclude them, so they are routed here: a refresh tag exists to publish them, and on any other
            // kind they are dropped. Published as a transition they would carry that tag's field list with
            // almost every value null, which reads as an alarm whose state is unknown.
            final boolean isControlEvent = isControlEvent(eventType);
            final boolean isRefreshTag = tag.getDefinition().getKind() == OpcuaTagKind.REFRESH;
            // RefreshRequired is the one control event that asks for something rather than reporting it, so
            // it is acted on before the publish decision -- on every kind of tag, including the ones that
            // drop it. Whether a user chose to see the event is unrelated to whether our alarm picture is
            // stale.
            if (isRefreshRequired(eventType)) {
                onRefreshRequired(eventIdOf(eventFields));
            }
            // Queue overflow is likewise acted on before the publish decision, and for a sharper reason: it
            // arrives on the tag whose data was lost and on no other, so the publish decision below is the
            // last place it exists. See onQueueOverflow.
            if (isQueueOverflow(eventType)) {
                onQueueOverflow(tag);
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
                // getPublishedFields(), not getType(): for a REFRESH tag the two differ, and the select clause
                // was built from the former. Event fields arrive positionally against the select clause, so
                // decoding against any other list would attach values to the wrong names.
                OpcUaEventToJsonConverter.convertPayload(
                        client.getDynamicEncodingContext(),
                        tag.getDefinition().getPublishedFields(),
                        eventFields,
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
     * Whether a notification arrived on a subscription this handler no longer speaks for — either because a
     * different one has replaced it, or because the connection underneath it is gone.
     * <p>
     * <b>The abandoned half.</b> {@link #abandon()} is called at the start of a stop or a destroy, before the
     * client is disconnected, and Milo delivers a subscription's notifications through a queue. Whatever is
     * already on that queue is still delivered while the teardown runs, so without this check a connection
     * the adapter has let go of goes on publishing to MQTT — including, after a destroy, through a tag
     * streaming service the adapter is finished with. The generation check below cannot cover it: the
     * subscription being torn down <em>is</em> still the current one, which is exactly why nothing replaced
     * it.
     * <p>
     * A superseded subscription has no business publishing: its items were re-established on the replacement,
     * so anything still arriving on it is a transition the new generation reports as well — published twice,
     * from a subscription nothing here can delete. Its {@code RefreshRequired} is likewise about a
     * subscription the server has already refused to transfer.
     * <p>
     * Asked by every callback that mutates health, metrics, or output — {@link #onDataReceived},
     * {@link #onEventReceived} and {@link #onKeepAliveReceived}. That is deliberately all of them rather than
     * the ones with a known symptom: they are three deliveries of the same fact, and the reason the value path
     * went unguarded for a review cycle is that guarding "the path where it was noticed" is a rule about one
     * bug rather than about generations. {@link #onTransferFailed} is the fourth and needs nothing — its
     * {@code compareAndSet} already discriminates by generation, and has to be atomic besides.
     * <p>
     * <b>Only when a different subscription is current, never merely when none is.</b> The two are not the
     * same window and the distinction decides whether this is safe: {@link #established} records the
     * subscription <em>after</em> monitored-item synchronization, so between the server accepting an item and
     * that call there is a legitimate interval in which notifications arrive and {@link #currentSubscription}
     * is still null. Dropping those would lose real alarms at connect time — a worse failure than the
     * duplicate this prevents, and one with no symptom at all. Null therefore means "not yet known", which is
     * not evidence of anything, while a different object means this one has been superseded.
     */
    private boolean hasBeenReplaced(final @NotNull OpcUaSubscription subscription) {
        if (abandoned.get()) {
            log.debug("Adapter '{}': ignoring a notification, the connection it arrived on is closing", adapterId);
            return true;
        }
        final OpcUaSubscription current = currentSubscription.get();
        if (current == null || current == subscription) {
            return false;
        }
        log.debug("Adapter '{}': ignoring a notification from a subscription that has been replaced", adapterId);
        return true;
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
        if (currentSubscription.get() != null) {
            requestConditionRefresh(RefreshReason.RECONNECTED);
        }
    }

    /**
     * Records that a refresh is owed and starts one unless a call is already covering it.
     * <p>
     * The single entry point for all three mandatory reasons — a subscription established, a session
     * re-established, and the server asking. They used to be two separate mechanisms: the server-requested
     * path had the pending/in-flight coordinator below, and the two automatic ones called
     * {@code ConditionRefresh} directly and swallowed the outcome. Independent, they could overlap, and
     * §5.5.7's {@code Bad_RefreshInProgress} is what a server says about that — refusing one of the two, with
     * the automatic path having nowhere to record that its refresh had not happened.
     * <p>
     * Fire and forget, and deliberately never fatal. A server that does not implement
     * {@code ConditionRefresh}, or refuses it, still delivers live transitions perfectly well — losing the
     * initial picture is a degradation, not a reason to fail the subscription that was just established.
     */
    private void requestConditionRefresh(final @NotNull RefreshReason reason) {
        // Any event item is worth refreshing, not only a condition tag: an event subscription tag monitors
        // conditions too, so a query-only adapter needs the refresh just as much. The call itself names no
        // node of ours -- both the object and the method are fixed by the specification -- so there is no
        // entry point to find, only a reason to bother asking.
        final boolean hasEventItems =
                tags.stream().anyMatch(tag -> tag.getDefinition().getKind() != OpcuaTagKind.VALUE);
        if (!hasEventItems) {
            return;
        }
        // A reason from outside starts a fresh run of attempts. The bound exists to stop one unanswerable
        // request being retried forever, not to ration refreshes over the life of the connection.
        consecutiveRefreshFailures.set(0);
        // Recorded as outstanding work before any attempt to start it, and that order is the whole
        // correctness argument: a completion racing this can then only ever see the slot already filled.
        // Filling it after a failed attempt would leave the window where the completion checks for pending
        // work, finds none, releases the guard, and only then does this reason go in -- with nobody left to
        // drain it.
        pendingRefresh.set(reason);
        drainRefreshRequests();
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
            final @NotNull OpcuaTag opcuaTag, final @Nullable NodeId nodeId, final @Nullable NodeId notifier) {
        return switch (opcuaTag.getDefinition().getKind()) {
            // A condition is not itself a notifier, so its events come from one above it.
            case CONDITION -> Objects.requireNonNull(notifier);
            // The Server object is a notifier by convention and the root of the notifier hierarchy, so an
            // item there sees the refresh bracket the server broadcasts to every notifier item.
            case REFRESH -> NodeIds.Server;
            // A query names its notifier directly; a value item never reaches here.
            case EVENT_SUBSCRIPTION, VALUE -> Objects.requireNonNull(nodeId);
        };
    }

    /** The event filter for an event item, which differs by kind. Builds from ids already parsed. */
    private static @NotNull EventFilter eventFilterFor(final @NotNull VerifiedTag verified) {
        final OpcuaTagDefinition definition = verified.tag().getDefinition();
        return switch (definition.getKind()) {
            // Each of the three narrowing dimensions is independently optional, so a tag naming none of them
            // is a legitimate request for everything the notifier carries. `filterType` says which events to
            // accept; `type` says what shape to publish them in -- deliberately independent.
            case EVENT_SUBSCRIPTION ->
                ConditionEventFilters.forQuery(
                        verified.sourceNode(),
                        verified.conditionNode(),
                        definition.getFilterType(),
                        definition.getPublishedFields());
            case REFRESH -> ConditionEventFilters.forRefresh();
            case CONDITION, VALUE ->
                ConditionEventFilters.forCondition(
                        Objects.requireNonNull(verified.node()), definition.getPublishedFields());
        };
    }

    /**
     * The event types that reach a monitored item whether or not its filter admits them.
     * <p>
     * Three refresh types bracket or request a {@code ConditionRefresh} and are copied to <em>every</em>
     * notifier item in the subscription (OPC 10000-9 §4.5, §5.5.7); the overflow type is delivered only to
     * the item whose queue overflowed — OPC 10000-4 §7.22.3: "These Events are only published to the
     * MonitoredItems in the Subscription that produced the EventQueueOverflowEventType Event. These Events
     * bypass the whereClause." Both families bypass the where clause, which is why they are dropped here
     * rather than filtered at the server.
     * <p>
     * The overflow event reaches the client at all because OPC 10000-4 §5.13.1.5 places it in the item's
     * queue <em>in addition to</em> the configured size — at the front when {@code discardOldest} is true,
     * and never itself discarded. So a queue that overflowed still has room to say so.
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
     * specification defines {@code Bad_RefreshInProgress} for exactly that.
     * <p>
     * <b>Coalescing is not suppression, though.</b> A distinct occurrence arriving while a call is outstanding
     * is a fresh reason to resynchronise, and the call already running was started for an earlier one — the
     * server may have answered it before the later reason existed. So the occurrence is recorded as
     * {@link #refreshRequiredPending} rather than dropped, and {@link #drainRefreshRequests} starts the call it
     * asks for once the current one settles. This used to fall through a bare {@code return}: the id was
     * already in {@link #isFirstSightOf}'s memory by then, so nothing could retry it and nothing said so.
     */
    private void onRefreshRequired(final @Nullable ByteString eventId) {
        // Identity first, duration second. The two guards answer different questions: this one asks "have I
        // already handled this occurrence", the in-flight flag below asks "is a call already outstanding".
        // Only the first is a correct answer to a duplicate, because nothing bounds the copies of one
        // occurrence to a single publish batch -- the specification copies the event to every notifier item
        // and says nothing about when each copy is delivered. A call that completes between two batches
        // therefore releases the in-flight flag and lets the same occurrence start a second refresh, which
        // the server answers with Bad_RefreshInProgress or, worse, honours.
        if (eventId != null && !isFirstSightOf(eventId)) {
            return;
        }
        requestConditionRefresh(RefreshReason.SERVER_REQUESTED);
    }

    /**
     * Starts the refresh that pending work asks for, unless a call is already covering it.
     * <p>
     * Called from both ends: by {@link #onRefreshRequired} when an occurrence arrives, and by the completion
     * of every call this starts. Whichever of the two finds both a pending flag and a free in-flight guard
     * makes the call, so the work cannot be dropped by either racing the other.
     * <p>
     * The loop exists for one interleaving. A drain can win the in-flight guard and then find the pending flag
     * already claimed by a drain that has since finished — at which point it holds a guard it has no use for,
     * and a producer that arrived in between would have seen that guard held and returned. Releasing and
     * re-reading covers that producer. Each pass either starts a call or observes the flag settled, so it runs
     * at most twice in practice and cannot spin.
     * <p>
     * <b>No subscription is carried through here, and that is the fix rather than a simplification.</b> Both
     * flags are handler-global while a subscription is not: a transfer failure clears the current one and a
     * rebuild installs a replacement, so the object that delivered a {@code RefreshRequired} need not be the
     * one a refresh should be sent to by the time the guard frees. It was carried, and that made this
     * reachable — the old generation's completion claimed the new generation's pending work and called
     * {@code ConditionRefresh} with an id the server had already refused to transfer, consuming the flag on
     * the way. The new subscription then had a refresh asked of it, answered {@code Bad_SubscriptionIdInvalid}
     * against the dead one, and nothing left to retry it. Which subscription to refresh is decided where the
     * call is made, from {@link #currentSubscription}, so it is always the generation that is live now.
     */
    private void drainRefreshRequests() {
        while (pendingRefresh.get() != null) {
            if (!refreshInFlight.compareAndSet(false, true)) {
                // A call is running. It drains on completion, and it will see this reason.
                return;
            }
            final RefreshReason claimed = pendingRefresh.getAndSet(null);
            if (claimed != null) {
                sendConditionRefresh(claimed);
                return;
            }
            refreshInFlight.set(false);
        }
    }

    /**
     * Makes the {@code ConditionRefresh} call, and drains again once it settles.
     * <p>
     * The in-flight guard is released on every path out of here, including a synchronous throw. Releasing it
     * only from {@code whenComplete} would leave it set forever if the request never produced a future — and
     * since the guard's whole job is to make {@link #drainRefreshRequests} skip a call that is already
     * covered, a stuck guard silently drops every {@code RefreshRequired} the server sends for the rest of the
     * connection. Silent because the skip is a bare return: no log, no event, and the alarm picture simply
     * stops being resynchronised.
     * <p>
     * The subscription is read here, at the moment of the call, rather than carried from the notification that
     * asked for the refresh — see {@link #drainRefreshRequests}. Finding none is not a lost request: the only
     * way to be here without one is with a replacement being built, and {@link #established} refreshes every
     * subscription it installs. So the reason this occurrence was raised for is answered by the rebuild's own
     * refresh, which is a fuller answer than this call would have been.
     */
    private void sendConditionRefresh(final @NotNull RefreshReason reason) {
        final OpcUaSubscription subscription = currentSubscription.get();
        if (subscription == null) {
            log.debug(
                    "Adapter '{}': a condition refresh is owed because {}, but no subscription is established; the replacement being built will refresh as it is established",
                    adapterId,
                    reason.why());
            releaseAndDrain();
            return;
        }
        final Optional<UInteger> subscriptionId = subscription.getSubscriptionId();
        if (subscriptionId.isEmpty()) {
            releaseAndDrain();
            return;
        }
        log.info("Adapter '{}': re-requesting the current alarm picture because {}", adapterId, reason.why());
        try {
            @SuppressWarnings("unused")
            final var unused = ConditionRefresh.request(client, subscriptionId.get())
                    .whenComplete((statusCode, throwable) -> {
                        if (throwable != null) {
                            log.warn(
                                    "Adapter '{}': the condition refresh owed because {} failed, so the alarm picture may stay incomplete until each alarm next changes",
                                    adapterId,
                                    reason.why(),
                                    throwable);
                            requeueIfWorthRetrying(reason, true);
                        } else if (statusCode.isBad()) {
                            log.warn(
                                    "Adapter '{}': the server refused the condition refresh owed because {} ({}), so the alarm picture may stay incomplete until each alarm next changes",
                                    adapterId,
                                    reason.why(),
                                    statusCode);
                            requeueIfWorthRetrying(reason, isRefreshAlreadyRunning(statusCode));
                        } else {
                            consecutiveRefreshFailures.set(0);
                        }
                        releaseAndDrain();
                    });
        } catch (final Exception e) {
            log.warn(
                    "Adapter '{}': the condition refresh owed because {} could not be sent, so the alarm picture may stay incomplete until each alarm next changes",
                    adapterId,
                    reason.why(),
                    e);
            requeueIfWorthRetrying(reason, true);
            releaseAndDrain();
        }
    }

    /**
     * Whether the server refused because a refresh is already running on this subscription.
     * <p>
     * OPC 10000-9 §5.5.7. The one refusal that says nothing about whether the request was reasonable — only
     * that it arrived while another was outstanding, which stops being true within a round trip.
     */
    private static boolean isRefreshAlreadyRunning(final @NotNull StatusCode statusCode) {
        return statusCode.getValue() == StatusCodes.Bad_RefreshInProgress;
    }

    /**
     * Puts a failed reason back so the drain tries it again, unless a retry cannot help or too many have
     * already failed in a row.
     * <p>
     * Failing consumed the reason, and for most failures that is right: a server that does not implement
     * {@code ConditionRefresh} will not implement it on the second ask either, and retrying would spend
     * calls to learn the same thing. The exceptions are a transport failure and
     * {@code Bad_RefreshInProgress}, both of which describe the moment rather than the request.
     * <p>
     * {@code compareAndSet(null, …)} rather than {@code set}: a newer reason may have arrived while this call
     * was outstanding, and it is the fresher description of why a refresh is owed. Either way one call
     * follows, since §4.5 makes the refresh subscription-wide — so the two reasons genuinely do collapse.
     */
    private void requeueIfWorthRetrying(final @NotNull RefreshReason reason, final boolean retryCouldHelp) {
        if (!retryCouldHelp) {
            return;
        }
        if (consecutiveRefreshFailures.incrementAndGet() > MAX_REFRESH_RETRIES) {
            log.warn(
                    "Adapter '{}': giving up on the condition refresh owed because {} after {} consecutive failures; the alarm picture may stay incomplete until each alarm next changes",
                    adapterId,
                    reason.why(),
                    MAX_REFRESH_RETRIES);
            return;
        }
        pendingRefresh.compareAndSet(null, reason);
    }

    /**
     * Hands the in-flight guard back and picks up whatever is owed, including anything this call requeued.
     * <p>
     * A failed call still drains. The reason that is waiting asked the server to be re-read, not for this
     * particular attempt to succeed, and it outlives the attempt.
     */
    private void releaseAndDrain() {
        refreshInFlight.set(false);
        drainRefreshRequests();
    }

    /**
     * Why a {@code ConditionRefresh} is owed.
     * <p>
     * Carried through the coordinator for the log alone — the call the three produce is identical, because
     * §4.5 makes a refresh subscription-wide rather than something aimed at a particular reason. Worth
     * carrying anyway: "the server asked and then refused" and "we reconnected and the refresh was refused"
     * are very different things to find in a log, and before these paths were merged the message said which
     * by virtue of there being two separate methods.
     */
    private enum RefreshReason {
        ESTABLISHED("a subscription was established"),
        RECONNECTED("the session was re-established"),
        SERVER_REQUESTED("the server reported that a refresh is required");

        private final @NotNull String why;

        RefreshReason(final @NotNull String why) {
            this.why = why;
        }

        @NotNull
        String why() {
            return why;
        }
    }

    /**
     * Reports that the server dropped condition transitions for one tag, because its event queue was full.
     * <p>
     * <b>This is the only place the fact exists.</b> Overflow is not broadcast like the refresh bracket: OPC
     * 10000-4 §7.22.3 — "These Events are only published to the MonitoredItems in the Subscription that
     * produced the EventQueueOverflowEventType Event" — so it reaches the one item whose queue filled and no
     * other. A refresh tag's own item did not overflow, so nothing arrives there, and the routing below
     * would otherwise drop the notification on the condition or query tag that did overflow. The loss would
     * then be invisible in every direction: no MQTT message, no adapter event, no metric.
     * <p>
     * That matters more than an ordinary dropped notification because an event is a <em>transition report</em>
     * and is never re-sent. A reconnect does not recover it and neither does a {@code ConditionRefresh},
     * which re-reports current state and cannot reconstruct the transitions in between. A consumer's alarm
     * history has a hole in it and this is what says so.
     * <p>
     * Reported rather than published. Putting the overflow event on the tag's own topic would mean emitting
     * it under that tag's declared field list with every alarm field null — an alarm whose state is unknown,
     * which reads worse than silence and is why the control events are dropped from data tags in the first
     * place. A tag modelled for this is <a href="https://linear.app/hivemq/issue/EDG-856">EDG-856</a>;
     * until it exists the operator-visible event and the metric are the honest surface.
     */
    private void onQueueOverflow(final @NotNull OpcuaTag tag) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_EVENT_QUEUE_OVERFLOW_COUNT);
        final String message = String.format(
                "Adapter '%s' lost condition transitions for tag '%s': the server's event queue for it "
                        + "overflowed and older notifications were discarded. An event is never re-sent, so "
                        + "those transitions are gone -- a refresh restores the current state but not the "
                        + "history. Raise 'eventQueueSize' if this recurs.",
                adapterId, tag.getName());
        log.warn(message);
        eventService
                .createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withSeverity(Event.SEVERITY.WARN)
                .withMessage(message)
                .fire();
    }

    /**
     * Whether a notification is the server reporting that this item's event queue overflowed.
     * <p>
     * Separate from {@link #isControlEvent} for the same reason as {@link #isRefreshRequired}: that one
     * decides whether to publish, this one decides whether to report.
     */
    private static boolean isQueueOverflow(final @Nullable NodeId eventType) {
        return NodeIds.EventQueueOverflowEventType.equals(eventType);
    }

    /**
     * Whether a notification is the server asking for a refresh, as opposed to the other control events.
     * <p>
     * Separate from {@link #isControlEvent} because the two answer different questions: that one decides
     * whether to publish, this one decides whether to act.
     */
    private static boolean isRefreshRequired(final @Nullable NodeId eventType) {
        return NodeIds.RefreshRequiredEventType.equals(eventType);
    }

    /**
     * Whether this {@code RefreshRequired} occurrence has not been seen before, recording it if so.
     * <p>
     * {@code EventId} is what identifies an occurrence: OPC 10000-9 §4.5 has the server copy one
     * {@code RefreshRequired} to every notifier item in the subscription, and the copies of one occurrence
     * carry one id. So an adapter with ten condition tags sees ten notifications that are one event, and
     * telling them apart from ten genuinely separate ones is exactly this comparison.
     * <p>
     * Bounded by count rather than by age. The window only has to outlive the delivery of one occurrence's
     * copies, which is bounded by the number of monitored items in the subscription; sixty-four is
     * comfortably beyond any realistic count and costs nothing to hold. An id evicted early degrades to the
     * in-flight guard, which is where this started.
     */
    private boolean isFirstSightOf(final @NotNull ByteString eventId) {
        synchronized (handledRefreshRequests) {
            if (!handledRefreshRequests.add(eventId)) {
                return false;
            }
            // Insertion-ordered, so the first element is the oldest. One eviction per insertion keeps the
            // set at its bound without ever needing to walk it.
            if (handledRefreshRequests.size() > MAX_REMEMBERED_REFRESH_REQUESTS) {
                final var oldest = handledRefreshRequests.iterator();
                oldest.next();
                oldest.remove();
            }
            return true;
        }
    }

    /** The notification's {@code EventId}, or null when it is absent or not a byte string. */
    private static @Nullable ByteString eventIdOf(final @NotNull Variant @NotNull [] eventFields) {
        if (EVENT_ID_INDEX < 0 || EVENT_ID_INDEX >= eventFields.length) {
            return null;
        }
        final Variant eventId = eventFields[EVENT_ID_INDEX];
        return eventId != null && eventId.value() instanceof final ByteString bytes ? bytes : null;
    }

    /**
     * Whether a notification is one of the control events rather than a transition report.
     * <p>
     * Takes the resolved type rather than the field array, like its two siblings: all three ask a question
     * about the same {@code EventType}, and reading it once per notification is what keeps that visible.
     */
    private static boolean isControlEvent(final @Nullable NodeId eventType) {
        return eventType != null && CONTROL_EVENT_TYPES.contains(eventType);
    }

    /**
     * The notification's {@code EventType}, or null when it is absent or not a node id.
     * <p>
     * Read positionally: {@code EventType} is part of {@code BASE_EVENT_FIELDS}, which every select clause
     * begins with, so its index is the same for every tag whatever type it declares.
     */
    private static @Nullable NodeId eventTypeOf(final @NotNull Variant @NotNull [] eventFields) {
        if (EVENT_TYPE_INDEX < 0 || EVENT_TYPE_INDEX >= eventFields.length) {
            return null;
        }
        final Variant eventType = eventFields[EVENT_TYPE_INDEX];
        return eventType != null && eventType.value() instanceof final NodeId typeId ? typeId : null;
    }

    /**
     * Where {@code EventId} and {@code EventType} sit in every notification's value array.
     * <p>
     * Constants, because they are constant: {@code BASE_EVENT_FIELDS} is an immutable list, so these indices
     * cannot change once the class is loaded. Deriving them with {@code indexOf} inside the accessors meant a
     * linear scan with string comparison on every field read — and the event type was read three times per
     * notification, once for each question asked about it — on the delivery path that carries every alarm the
     * adapter receives.
     * <p>
     * They are positions in the <em>select clause</em>, which is what makes reading a value array by index
     * correct at all: every select clause begins with {@code BASE_EVENT_FIELDS} whatever type a tag declares,
     * and none of those ten fields carries an {@code Id} companion that would shift the ones after it.
     * {@code OpcUaSubscriptionEventFieldPositionsTest} pins that for all 22 types, because it is an
     * assumption this class depends on and does not own.
     */
    private static final int EVENT_ID_INDEX = OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventId");

    private static final int EVENT_TYPE_INDEX = OpcuaConditionType.BASE_EVENT_FIELDS.indexOf("EventType");

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

    /**
     * Says which selected fields the server rejected outright, so a permanently null field is not mistaken
     * for one the device merely does not implement.
     * <p>
     * A select clause is validated once, when the monitored item is created, and the server answers with one
     * status code per entry. OPC 10000-4 §7.22.3 scopes that to the permanent errors — "any errors which are
     * <em>true for all possible Events</em>" — so a bad code here means the field can never arrive: the
     * browse path resolves to nothing, or names something this type does not have.
     * <p>
     * Both cases publish null, and that is deliberate. A declared field the device does not implement
     * already arrives as null and is accepted as normal — a {@code LimitAlarmType} tag against a server
     * implementing one limit publishes sixteen, fifteen of them permanently null. Failing the subscription
     * over one rejected entry would cost a tag its other forty-nine fields for no gain. What is missing
     * without this log is only the <em>distinction</em>: a rejection is a configuration error someone can
     * fix by correcting the tag's declared type, while an unimplemented field is a device fact to live with.
     * <p>
     * Logged once per tag rather than once per field: a wrongly declared type rejects many entries at once,
     * and fifty lines saying the same thing would bury it.
     */
    private void reportRejectedSelectClauses(final @NotNull OpcUaSubscription subscription) {
        subscription.getMonitoredItems().forEach(item -> {
            final OpcuaTag tag = tagOf(item);
            if (tag == null || tag.getDefinition().getKind() == OpcuaTagKind.VALUE) {
                return;
            }
            item.getFilterResult()
                    .map(encoded -> encoded.decode(client.getStaticEncodingContext()))
                    .filter(EventFilterResult.class::isInstance)
                    .map(EventFilterResult.class::cast)
                    .ifPresent(result -> reportRejectedFields(tag, result.getSelectClauseResults()));
        });
    }

    /** Names the rejected entries of one tag's select clause, matched positionally against its fields. */
    private void reportRejectedFields(final @NotNull OpcuaTag tag, final StatusCode @Nullable [] results) {
        if (results == null) {
            return;
        }
        final List<OpcuaConditionType.SelectedField> fields = selectedFieldsFor(tag);
        final List<String> rejected = new ArrayList<>();
        for (int i = 0; i < results.length; i++) {
            final StatusCode status = results[i];
            if (status == null || !status.isBad()) {
                continue;
            }
            // The results match the select clause positionally, so position i names the field at i. An
            // entry beyond what we selected would mean the server answered a clause we did not send;
            // reporting the index alone is more honest than guessing at a name.
            final String field = i < fields.size() ? fields.get(i).publishedAs() : "#" + i;
            rejected.add(field + " (" + status + ")");
        }
        if (!rejected.isEmpty()) {
            log.warn(
                    "Adapter '{}': the server rejected {} of the {} fields selected for tag '{}', so they will be null on every event rather than only on transitions that do not carry them. This is a permanent answer about the type, not a device that happens not to implement them -- check the tag's declared type. Rejected: {}",
                    adapterId,
                    rejected.size(),
                    fields.size(),
                    tag.getName(),
                    String.join(", ", rejected));
        }
    }

    /** The fields selected for a tag, in the order the select clause was built. */
    private static @NotNull List<OpcuaConditionType.SelectedField> selectedFieldsFor(final @NotNull OpcuaTag tag) {
        return tag.getDefinition().getPublishedFields().selectedFields();
    }

    /**
     * A tag cleared for subscription, with every node id it needs already parsed.
     * <p>
     * The parsing belongs here rather than at item-construction time because {@link #verify} is the per-tag
     * boundary: everything inside it answers "do not subscribe <em>this</em> tag", while a throw outside it
     * aborts the whole synchronization and takes every healthy tag with it. A malformed {@code sourceNode}
     * on one query tag used to do exactly that — {@code NodeId.parse} raised from inside the loop that
     * builds monitored items, which is past the try/catch, so one typo in an optional narrowing filter
     * failed the adapter's start.
     *
     * @param node          the tag's own node. Null for a {@code REFRESH} tag, which names none of its own —
     *                      its item goes on the Server object and its filter reads no node id.
     * @param notifier      the notifier resolved for a condition tag, null for the other kinds.
     * @param sourceNode    an event subscription tag's source predicate, null when it has none.
     * @param conditionNode an event subscription tag's condition predicate, null when it has none.
     */
    private record VerifiedTag(
            @NotNull OpcuaTag tag,
            @Nullable NodeId node,
            @Nullable NodeId notifier,
            @Nullable NodeId sourceNode,
            @Nullable NodeId conditionNode) {}

    /**
     * Whether a tag may be subscribed, and on what: present for anything that is not a condition, and for a
     * condition whose declared type the device satisfies and whose notifier could be found.
     * <p>
     * Deliberately total — every failure, including a timeout or an interrupt, answers empty rather than
     * throwing. This runs inside adapter start, where an escaping exception would abort the whole sequence.
     */
    private @NotNull Optional<VerifiedTag> verify(final @NotNull OpcuaTag opcuaTag) {
        final OpcuaTagDefinition definition = opcuaTag.getDefinition();
        final String tagName = opcuaTag.getName();
        try {
            // Every configured node id is parsed here, inside the boundary, so nothing downstream can throw
            // while building monitored items. A REFRESH tag's own `node` is deliberately not parsed: it
            // names nothing -- the item goes on the Server object and the filter reads no node id -- so
            // rejecting the tag over a value that is never used would be a rejection with no consequence
            // behind it.
            final NodeId node =
                    definition.getKind() == OpcuaTagKind.REFRESH ? null : parseField(definition.getNode(), "node");

            // A switch expression rather than a chain of inequalities, so the compiler enforces that every
            // kind states its own answer. What each kind needs verified has nothing in common with the
            // others, and the old shape -- "everything that is not a CONDITION" -- quietly made VALUE and
            // REFRESH share whatever EVENT_SUBSCRIPTION happened to need. That is how they came to be
            // rejected over sourceNode and conditionNode: fields read by nothing but a query tag's where
            // clause. A fifth kind would have inherited the same by default; now it cannot compile without
            // saying what it wants.
            return switch (definition.getKind()) {
                case VALUE, REFRESH -> Optional.of(new VerifiedTag(opcuaTag, node, null, null, null));
                case EVENT_SUBSCRIPTION -> verifyEventSubscription(opcuaTag, node, tagName);
                case CONDITION -> verifyCondition(opcuaTag, Objects.requireNonNull(node), tagName);
            };
        } catch (final MalformedNodeIdException e) {
            reportUnsubscribableTag(tagName, describe(e));
            return Optional.empty();
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
     * Verifies an event subscription tag: its notifier must be a node this session may subscribe to, and its
     * two narrowing predicates must be node ids.
     * <p>
     * {@code sourceNode} and {@code conditionNode} are parsed <em>here</em> rather than alongside the tag's
     * own node, because this is the only kind that reads them — they become operands in the where clause and
     * exist nowhere else. Parsed for every non-condition kind, as they were, a stale or hand-authored value
     * dropped a perfectly good VALUE or REFRESH tag over a field its filter never consults. A migrated
     * configuration is exactly where such a leftover lives.
     */
    private @NotNull Optional<VerifiedTag> verifyEventSubscription(
            final @NotNull OpcuaTag opcuaTag, final @Nullable NodeId node, final @NotNull String tagName)
            throws InterruptedException, ExecutionException, TimeoutException {

        final OpcuaTagDefinition definition = opcuaTag.getDefinition();
        final NodeId sourceNode = parseField(definition.getSourceNode(), "sourceNode");
        final NodeId conditionNode = parseField(definition.getConditionNode(), "conditionNode");

        // An event subscription tag has no single declared type to verify -- the point of the tag is that
        // many conditions of differing types pass its filter -- but it does name a notifier directly, and
        // that node has to be one.
        final Optional<String> optionalUnsubscribableTag = NotifierResolver.checkSubscribable(
                        client, Objects.requireNonNull(node), tagName, "node")
                .get(CONDITION_TYPE_VERIFICATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (optionalUnsubscribableTag.isPresent()) {
            reportUnsubscribableTag(tagName, optionalUnsubscribableTag.get());
            return Optional.empty();
        }
        return Optional.of(new VerifiedTag(opcuaTag, node, null, sourceNode, conditionNode));
    }

    /**
     * Verifies a condition tag: the device's condition must satisfy the declared type, and a notifier to
     * receive its events from must be found.
     * <p>
     * A condition the server does not expose at all is the one case that is neither verified nor rejected on
     * the verifier's word alone — see {@link #acceptsUnverified}.
     */
    private @NotNull Optional<VerifiedTag> verifyCondition(
            final @NotNull OpcuaTag opcuaTag, final @NotNull NodeId node, final @NotNull String tagName)
            throws InterruptedException, ExecutionException, TimeoutException {

        final OpcuaTagDefinition definition = opcuaTag.getDefinition();
        final ConditionTypeVerifier.Result result = ConditionTypeVerifier.verify(
                        client, node, definition.getType(), tagName)
                .get(CONDITION_TYPE_VERIFICATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        if (result instanceof final ConditionTypeVerifier.Result.Rejected rejected) {
            reportUnsubscribableTag(tagName, rejected.reason());
            return Optional.empty();
        }
        if (result instanceof final ConditionTypeVerifier.Result.Unverifiable unverifiable
                && !acceptsUnverified(definition, tagName, unverifiable)) {
            return Optional.empty();
        }

        // A condition is not an event notifier, so without a notifier there is nowhere to subscribe and
        // the tag simply cannot be honoured. Same outcome as a type mismatch: this tag alone is dropped.
        final NotifierResolver.Result notifier = NotifierResolver.resolve(
                        client, node, definition.getNotifierNode(), tagName)
                .get(CONDITION_TYPE_VERIFICATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        if (notifier instanceof final NotifierResolver.Result.NotFound notFound) {
            reportUnsubscribableTag(tagName, notFound.reason());
            return Optional.empty();
        }
        final NotifierResolver.Result.Found found = (NotifierResolver.Result.Found) notifier;

        // A declared notifier is checked; a walked one is not, and the asymmetry is deliberate rather
        // than an oversight. The walk *is* the check -- it only ever returns a node whose
        // EventNotifier attribute it has already read and found to carry the SubscribeToEvents bit, so
        // asking again would be a second round trip for an answer already known. A declared one has had
        // no such check by construction: it exists precisely because the walk could not be relied on,
        // and it was previously "taken at its word", leaving nothing between a typo and a tag that
        // subscribes cleanly and then stays silent forever.
        if (definition.getNotifierNode() != null) {
            final Optional<String> optionalUnsubscribableTag = NotifierResolver.checkSubscribable(
                            client, found.notifier(), tagName, "notifierNode")
                    .get(CONDITION_TYPE_VERIFICATION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (optionalUnsubscribableTag.isPresent()) {
                reportUnsubscribableTag(tagName, optionalUnsubscribableTag.get());
                return Optional.empty();
            }
        }
        log.debug(
                "Adapter '{}': tag '{}' will receive events from notifier {} ({})",
                adapterId,
                tagName,
                found.notifier(),
                found.how());
        return Optional.of(new VerifiedTag(opcuaTag, node, found.notifier(), null, null));
    }

    /**
     * Whether to subscribe a condition the server does not expose, and therefore could not be checked.
     * <p>
     * OPC 10000-9 §4.3 permits a server to keep its Condition instances out of the AddressSpace and deliver
     * them through events alone, and the rest of Edge now supports that server: the writer calls the standard
     * type's MethodId with the ConditionId as ObjectId, and the tag's events arrive through a notifier rather
     * than through the condition node. Refusing the tag here made all of that unreachable.
     * <p>
     * <b>The gate is {@code notifierNode}, and it is not a formality.</b> A typo in {@code node} produces the
     * identical {@code Bad_NodeIdUnknown}, and waiving verification for it would trade a clear rejection at
     * start for a tag that subscribes cleanly and stays silent forever — the failure mode this module works
     * hardest to avoid, because nothing distinguishes it from an alarm that has not fired. Requiring the
     * notifier to be named means an operator on such a server states both halves of what they know, and an
     * ordinary misconfiguration — which has no reason to name a notifier — still fails loudly. It is also
     * exactly the field a hidden-instance server forces anyway: the notifier walk starts at the condition, so
     * it cannot run when the condition is not there to walk from.
     *
     * @return true when the tag may proceed unverified; false when it was reported and must be dropped.
     */
    private boolean acceptsUnverified(
            final @NotNull OpcuaTagDefinition definition,
            final @NotNull String tagName,
            final @NotNull ConditionTypeVerifier.Result.Unverifiable unverifiable) {

        if (definition.getNotifierNode() == null) {
            reportUnsubscribableTag(
                    tagName,
                    unverifiable.reason()
                            + ". If this server is one of those, set 'notifierNode' on the tag to name the "
                            + "node its events arrive from: Edge cannot walk to one from a condition that is "
                            + "not in the address space, and naming it is how you confirm the node id is "
                            + "meant to be unbrowsable rather than mistyped. Otherwise check 'node' — a "
                            + "wrong node id is refused with the same status");
            return false;
        }
        log.warn(
                "Adapter '{}': {}. Subscribing it unverified because the tag names its notifier explicitly. "
                        + "Edge cannot confirm the node is a condition, that it is of the declared type, or "
                        + "that it exists at all — if this tag never publishes, check 'node' first.",
                adapterId,
                unverifiable.reason());
        return true;
    }

    /**
     * A configured node id that is not one. Carries a message naming the field and the value, because those
     * are the two things an operator needs and neither is in {@code NodeId.parse}'s own complaint.
     */
    private static final class MalformedNodeIdException extends RuntimeException {

        @java.io.Serial
        private static final long serialVersionUID = 1L;

        private MalformedNodeIdException(final @NotNull String message) {
            super(message);
        }
    }

    /**
     * Parses one configured node id, naming the field if it will not parse.
     *
     * @param value the configured string, or null when the field was not set.
     * @param field the field's name in the tag definition, for the message.
     * @return the parsed node id, or null when the field was not set.
     * @throws MalformedNodeIdException when the value is set but is not a node id.
     */
    private static @Nullable NodeId parseField(final @Nullable String value, final @NotNull String field) {
        if (value == null) {
            return null;
        }
        try {
            return NodeId.parse(value);
        } catch (final Exception e) {
            throw new MalformedNodeIdException("its '" + field + "' is '" + value
                    + "', which is not a valid OPC UA node id. Expected something like 'ns=2;s=Boiler1.HighTemp'");
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
