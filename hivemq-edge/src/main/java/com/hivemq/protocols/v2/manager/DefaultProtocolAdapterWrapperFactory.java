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
package com.hivemq.protocols.v2.manager;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.services.ProtocolAdapterService;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.persistence.util.FutureUtils;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.northbound.NorthboundTagConsumerRegistry;
import com.hivemq.protocols.v2.runtime.AdapterFaults;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
import com.hivemq.protocols.v2.southbound.SouthboundBrokerRuntime;
import com.hivemq.protocols.v2.southbound.SouthboundMqttIntake;
import com.hivemq.protocols.v2.southbound.SouthboundWritePlane;
import com.hivemq.protocols.v2.tag.TagAspectRuntimeCoordinator;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterGoalState;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterOutputFacade;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapper;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperContext;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperTick;
import com.hivemq.protocols.v2.wrapper.TagAspectActivationPreference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Production {@link ProtocolAdapterWrapperFactory}: assembles the full wrapper/adapter actor for one
 * configuration, exactly as the wrapper test rig does but driven from the read-only configuration and the
 * injected runtime. For each adapter it
 * <ol>
 * <li>creates the wrapper mailbox and the tell-façade the protocol adapter reports through;</li>
 * <li>translates the configuration's tags into runtime {@link NodeTagPair}s (deserializing each {@code node-string}
 * into the type's node class) and the adapter configuration into a reused v1 {@link DataPoint};</li>
 * <li>asks the type's {@link ProtocolAdapterFactory} to construct the pure-mechanism adapter;</li>
 * <li>builds the running tag coordinator and the wrapper context with the config-declared goal, activation,
 * {@code used} derivation, retry policy, and watchdog timeout, then binds the coordinator to the actor runtime;</li>
 * <li>publishes the wrapper into a fresh snapshot reference, attaches it to the dispatcher, and schedules its
 * periodic tick.</li>
 * </ol>
 * The runtime collaborators ({@link Clock}, {@link MessageDispatcher}, {@link MetricRegistry},
 * {@link DataPointFactory}, the JSON {@link ObjectMapper}, and the tick period) are injected, so the same factory
 * serves production (a {@code SystemClock} / {@code SystemDispatcher}) and tests (a {@code FakeClock} /
 * {@code ManualDispatcher}).
 * <p>
 * Construction is <b>all-or-nothing</b>. Every resource that outlives the call — registered metrics, northbound
 * consumers, the dispatch binding, the tick schedule, the adapter's own dispatch bindings, the southbound writers —
 * is handed to a {@link ConstructionScope} the moment it exists. A failure at any later step releases them in reverse
 * order and rethrows, so a half-built adapter leaves nothing behind; on success the scope hands ownership to the
 * {@link ProtocolAdapterContainer} and keeps nothing, so nothing is closed twice.
 */
public final class DefaultProtocolAdapterWrapperFactory implements ProtocolAdapterWrapperFactory {

    private static final @NotNull Logger log = LoggerFactory.getLogger(DefaultProtocolAdapterWrapperFactory.class);

    private final @NotNull Clock clock;
    private final @NotNull MessageDispatcher dispatcher;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull ObjectMapper objectMapper;
    private final @Nullable TagManager tagManager;
    private final @Nullable NorthboundConsumerFactory northboundConsumerFactory;
    private final @Nullable SouthboundBrokerRuntime southboundBrokerRuntime;
    private final long tickPeriodMillis;

    /**
     * @param clock            the clock the wrapper timers and tick are scheduled against.
     * @param dispatcher       the dispatcher each wrapper mailbox is attached to.
     * @param metricRegistry   the shared registry per-adapter metrics are registered on.
     * @param dataPointFactory the reused v1 factory the protocol adapter builds its values with.
     * @param objectMapper     the JSON mapper that deserializes a {@code node-string} into the type's node class.
     * @param tickPeriodMillis the wrapper tick period, in milliseconds (~50 ms in production).
     */
    public DefaultProtocolAdapterWrapperFactory(
            final @NotNull Clock clock,
            final @NotNull MessageDispatcher dispatcher,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull ObjectMapper objectMapper,
            final long tickPeriodMillis) {
        this(clock, dispatcher, metricRegistry, dataPointFactory, objectMapper, tickPeriodMillis, null, null, null);
    }

    /**
     * @param clock                    the clock the wrapper timers and tick are scheduled against.
     * @param dispatcher               the dispatcher each wrapper mailbox is attached to.
     * @param metricRegistry           the shared registry per-adapter metrics are registered on.
     * @param dataPointFactory         the reused v1 factory the protocol adapter builds its values with.
     * @param objectMapper             the JSON mapper that deserializes a {@code node-string} into the type's node
     *                                 class.
     * @param tickPeriodMillis         the wrapper tick period, in milliseconds (~50 ms in production).
     * @param tagManager               the shared tag manager used by MQTT northbound consumers.
     * @param northboundConsumerFactory builds MQTT consumers for v2 northbound mappings.
     * @param writingService           the reused writing service that drives southbound MQTT&rarr;adapter writes
     *                                 (EDG-824 #3); {@code null} disables southbound wiring (unit-test rigs).
     */
    public DefaultProtocolAdapterWrapperFactory(
            final @NotNull Clock clock,
            final @NotNull MessageDispatcher dispatcher,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull ObjectMapper objectMapper,
            final long tickPeriodMillis,
            final @Nullable TagManager tagManager,
            final @Nullable NorthboundConsumerFactory northboundConsumerFactory) {
        this(
                clock,
                dispatcher,
                metricRegistry,
                dataPointFactory,
                objectMapper,
                tickPeriodMillis,
                tagManager,
                northboundConsumerFactory,
                null);
    }

    /**
     * @param clock                    the clock the wrapper timers and tick are scheduled against.
     * @param dispatcher               the dispatcher each wrapper mailbox is attached to.
     * @param metricRegistry           the shared registry per-adapter metrics are registered on.
     * @param dataPointFactory         the reused v1 factory the protocol adapter builds its values with.
     * @param objectMapper             the JSON mapper that deserializes a {@code node-string} into the type's node
     *                                 class.
     * @param tickPeriodMillis         the wrapper tick period, in milliseconds (~50 ms in production).
     * @param tagManager               the shared tag manager used by MQTT northbound consumers.
     * @param northboundConsumerFactory builds MQTT consumers for v2 northbound mappings.
     * @param southboundBrokerRuntime  the broker collaborators the southbound write path stands on (topic tree,
     *                                 client queues, publish path, retained store); {@code null} (unit rigs) falls
     *                                 the southbound plane back to in-memory backlogs.
     */
    public DefaultProtocolAdapterWrapperFactory(
            final @NotNull Clock clock,
            final @NotNull MessageDispatcher dispatcher,
            final @NotNull MetricRegistry metricRegistry,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull ObjectMapper objectMapper,
            final long tickPeriodMillis,
            final @Nullable TagManager tagManager,
            final @Nullable NorthboundConsumerFactory northboundConsumerFactory,
            final @Nullable SouthboundBrokerRuntime southboundBrokerRuntime) {
        this.clock = clock;
        this.dispatcher = dispatcher;
        this.metricRegistry = metricRegistry;
        this.dataPointFactory = dataPointFactory;
        this.objectMapper = objectMapper;
        this.tagManager = tagManager;
        this.northboundConsumerFactory = northboundConsumerFactory;
        this.southboundBrokerRuntime = southboundBrokerRuntime;
        this.tickPeriodMillis = tickPeriodMillis;
    }

    @Override
    public @NotNull ProtocolAdapterContainer create(
            final @NotNull ProtocolAdapterEntity entity,
            final @NotNull ProtocolAdapterFactory factory,
            final @NotNull ProtocolAdapterWrapperEventListener healthListener) {
        final String adapterId = entity.getAdapterId();
        final ConstructionScope scope = new ConstructionScope(adapterId);
        try {
            return build(entity, factory, healthListener, scope);
        } catch (final Throwable failure) {
            // A fatal JVM condition is never scoped to one adapter, and releasing resources on a dying JVM is not
            // worth delaying it — propagate before containing anything (EDG-824 #12).
            AdapterFaults.rethrowIfFatal(failure);
            // Throwable, not RuntimeException: a mispackaged or version-skewed adapter jar throws a LinkageError from
            // its constructor, and that half-built adapter's metrics, consumers, dispatch threads and tick must still
            // be released. Without this the adapter id stays permanently uncreatable — a leaked gauge makes the next
            // attempt's registration throw (Sam round 2, finding 2).
            scope.closeAll();
            throw failure;
        }
    }

    /**
     * The construction proper. Every resource is registered with {@code scope} as it is acquired, so the caller's
     * single catch can unwind all of them; the scope is committed only once the container owns them.
     */
    private @NotNull ProtocolAdapterContainer build(
            final @NotNull ProtocolAdapterEntity entity,
            final @NotNull ProtocolAdapterFactory factory,
            final @NotNull ProtocolAdapterWrapperEventListener healthListener,
            final @NotNull ConstructionScope scope) {
        final String adapterId = entity.getAdapterId();

        final Mailbox<ProtocolAdapterWrapperMessage> mailbox = new DefaultMailbox<>();
        final ProtocolAdapterOutput output = new ProtocolAdapterOutputFacade(mailbox);

        final List<NodeTagPair> nodes = translateNodes(entity, factory);
        final DataPoint adapterConfig =
                dataPointFactory.createJsonDataPoint(adapterId, entity.getAdapterConfiguration());
        // Hand the adapter a dispatcher that records every binding it opens through the framework, so the framework
        // owns each dispatch thread for the adapter's whole lifetime — whether the adapter attaches its mailbox in its
        // constructor or later, and whether or not it is itself AutoCloseable. Registered BEFORE the adapter is
        // constructed: a constructor that opens a binding and then throws has it released by the scope.
        final RecordingDispatcher recordingDispatcher = scope.register(new RecordingDispatcher(dispatcher));
        final ProtocolAdapterService services = new WrapperServices(dataPointFactory, recordingDispatcher);
        final ProtocolAdapterInput input = new WrapperInput(adapterId, adapterConfig, nodes, services);
        final ProtocolAdapter protocolAdapter = factory.createAdapter(input, output);
        // The adapter exists, so its own teardown joins the scope ahead of the recorder — reverse order releases the
        // adapter's non-dispatch resources first and its bindings second, exactly as container teardown does.
        scope.register(adapterSelfClose(protocolAdapter));

        final ProtocolAdapterMetrics metrics =
                scope.register(new ProtocolAdapterMetrics(metricRegistry, adapterId, mailbox::size));
        // Registered EMPTY, then wired: the registry's own consumers are acquired one at a time, and a failure on any
        // of them must leave the ones already added to the tag manager owned by the scope. A registry that built them
        // in its constructor was never returned when it threw, so nothing could remove them (Sam round 3, finding 4).
        final NorthboundTagConsumerRegistry northboundConsumers =
                scope.registerOptional(createNorthboundConsumers(adapterId, factory));
        if (northboundConsumers != null) {
            northboundConsumers.updateMappings(entity.getNorthboundMappings());
        }
        final Consumer<DataPoint> northboundDataPointSink;
        if (northboundConsumers == null) {
            northboundDataPointSink = ignored -> {};
        } else {
            final TagManager activeTagManager = Objects.requireNonNull(tagManager);
            northboundDataPointSink = dataPoint -> activeTagManager.feed(List.of(dataPoint));
        }
        final ProtocolAdapterGoalState goal = ProtocolAdapterConfigSupport.goalOf(entity);
        final Map<String, TagAspectActivationPreference> activation = ProtocolAdapterConfigSupport.activationOf(entity);
        final Set<String> readUsed = entity.getReadUsedTagNames();
        final Set<String> writeUsed = entity.getWriteUsedTagNames();
        final RetryPolicy retryPolicy = entity.getRetryPolicy().toRetryPolicy();

        // The southbound delivery side: one suspended queue+backlog per write-mapped tag, opened and closed by the
        // write aspects' readiness notifications (the plane IS the readiness listener). With the broker runtime
        // present, the MQTT intake subscribes each mapping's topic and the backlogs lease from the durable client
        // queues; without it (unit rigs), the plane falls back to the interim in-memory backlogs.
        // Both join the scope as they are acquired, so a construction that throws afterwards — including the
        // LinkageError case the scope exists for — releases the intake's shared subscriptions and the plane's
        // backlogs and leases. The durable queues and their contents survive, as always.
        final SouthboundMqttIntake southboundIntake;
        final SouthboundWritePlane southboundWritePlane;
        if (southboundBrokerRuntime != null && !entity.getSouthboundMappings().isEmpty()) {
            southboundIntake = scope.register(new SouthboundMqttIntake(
                    adapterId, southboundBrokerRuntime, dataPointFactory, entity.getSouthboundMappings()));
            southboundWritePlane = scope.register(
                    new SouthboundWritePlane(
                            adapterId, mailbox, southboundIntake.backlogFactory(), nodes, writeUsed, metrics));
        } else {
            southboundIntake = null;
            southboundWritePlane = scope.register(new SouthboundWritePlane(
                    adapterId, mailbox, entity.getSouthboundWriteBacklogCapacity(), nodes, writeUsed, metrics));
        }
        final TagAspectRuntimeCoordinator tagPlane = new TagAspectRuntimeCoordinator(
                adapterId,
                nodes,
                activation,
                readUsed,
                writeUsed,
                goal,
                ProtocolAdapterConfigSupport.pollIntervalMillisOf(entity),
                entity.getCommandTimeoutMillis(),
                retryPolicy);
        final ProtocolAdapterWrapperContext context = new ProtocolAdapterWrapperContext(
                adapterId,
                protocolAdapter,
                mailbox,
                clock,
                retryPolicy,
                entity.getWatchdogTimeoutMillis(),
                entity.isSkipVerification(),
                goal,
                activation,
                tagPlane,
                healthListener,
                metrics,
                northboundDataPointSink);
        tagPlane.bindRuntime(
                context.clock(),
                context.timers(),
                context.batches(),
                context.metrics(),
                context.protocolAdapter()::verifyBatch,
                mailbox);
        // The delivery side becomes dispatch-thread state from here: the wrapper routes every southbound
        // message to it, and its backstop poll rides the wrapper's tick — the only timing surface in v2.
        context.bindSouthboundPlane(southboundWritePlane);

        final AtomicReference<AdapterStatusSnapshot> snapshot = new AtomicReference<>();
        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(context, snapshot);
        final MessageDispatcherHandle dispatcherHandle = scope.register(dispatcher.attach(mailbox, wrapper));
        final AutoCloseable tickHandle = scope.register(
                clock.scheduleTick(tickPeriodMillis, mailbox, () -> new ProtocolAdapterWrapperTick(clock.nowMillis())));
        // The container owns the teardown of everything the adapter attached through the framework dispatcher, so its
        // dispatch threads are released when the adapter is discarded, exactly as the wrapper's binding is. The
        // adapter's own close() (if it is AutoCloseable) runs first to release any non-dispatch resources; the
        // recording dispatcher then closes every remaining binding it opened, each at most once — so a template's
        // single self-closed binding is never double-closed and a non-AutoCloseable direct adapter's binding is
        // still released.
        final AutoCloseable adapterDispatcherHandle = adapterTeardown(protocolAdapter, recordingDispatcher);

        final ProtocolAdapterHandle handle =
                new ProtocolAdapterHandle(adapterId, mailbox, snapshot, southboundWritePlane);
        return scope.commit(new ProtocolAdapterContainer(
                handle,
                dispatcherHandle,
                adapterDispatcherHandle,
                tickHandle,
                metrics,
                northboundConsumers,
                southboundIntake,
                entity));
    }

    @Override
    public void discardSouthboundQueues(
            final @NotNull ProtocolAdapterEntity entity, final @NotNull Set<String> tagNames) {
        if (southboundBrokerRuntime == null || tagNames.isEmpty()) {
            return;
        }
        log.info(
                "Discarding the southbound command queues of tag(s) {} on recreated v2 adapter '{}': the tag now "
                        + "addresses a different node, or its mapping topic moved, so commands queued under the "
                        + "previous configuration must not be executed against the new one.",
                tagNames,
                entity.getAdapterId());
        for (final SouthboundMappingEntity mapping : entity.getSouthboundMappings()) {
            if (tagNames.contains(mapping.getTagName())) {
                clearQueue(entity.getAdapterId(), mapping.getTopic());
            }
        }
    }

    @Override
    public void discardSouthboundQueues(final @NotNull ProtocolAdapterEntity entity) {
        if (southboundBrokerRuntime == null || entity.getSouthboundMappings().isEmpty()) {
            return;
        }
        final String adapterId = entity.getAdapterId();
        log.info(
                "Discarding the southbound command queues of removed v2 adapter '{}': any command still queued for it "
                        + "is destroyed here, because nothing will consume it again.",
                adapterId);
        for (final SouthboundMappingEntity mapping : entity.getSouthboundMappings()) {
            // Rebuilt from the configuration rather than read off the intake: an adapter that failed to construct one
            // (or that has been ERROR ever since) still owns whatever its predecessor queued under the same id.
            clearQueue(adapterId, mapping.getTopic());
        }
    }

    /** Destroy one mapping's durable queue. Best effort: this runs on the manager's dispatch thread mid-teardown. */
    private void clearQueue(final @NotNull String adapterId, final @NotNull String topic) {
        final String queueId = SouthboundMqttIntake.queueId(adapterId, topic);
        try {
            FutureUtils.addExceptionLogger(Objects.requireNonNull(southboundBrokerRuntime)
                    .clientQueuePersistence()
                    .clear(queueId, true));
        } catch (final Exception failure) {
            // Never fatal: a throw here would abandon the teardown steps after it, and a queue that survives is a
            // leak rather than a correctness fault.
            log.warn("Failed to discard the southbound queue '{}' of adapter '{}'", queueId, adapterId, failure);
        }
    }

    /** Builds the empty registry; the caller registers it with the scope and only then wires its mappings. */
    private @Nullable NorthboundTagConsumerRegistry createNorthboundConsumers(
            final @NotNull String adapterId, final @NotNull ProtocolAdapterFactory factory) {
        if (tagManager == null || northboundConsumerFactory == null) {
            return null;
        }
        return new NorthboundTagConsumerRegistry(
                adapterId,
                factory.information(),
                tagManager,
                northboundConsumerFactory,
                new ProtocolAdapterMetricsServiceImpl(factory.information().protocolId(), adapterId, metricRegistry));
    }

    /** Builds the empty registry; the caller registers it with the scope and only then starts its writing. */
    private @Nullable SouthboundWriterRegistry createSouthboundWriters(
            final @NotNull String adapterId,
            final @NotNull ProtocolAdapterFactory factory,
            final @NotNull Mailbox<ProtocolAdapterWrapperMessage> mailbox,
            final @NotNull List<NodeTagPair> nodes) {
        if (writingService == null) {
            return null;
        }
        return new SouthboundWriterRegistry(
                adapterId,
                factory.information(),
                writingService,
                new ProtocolAdapterMetricsServiceImpl(factory.information().protocolId(), adapterId, metricRegistry),
                mailbox,
                dataPointFactory,
                nodes);
    }

    @Override
    public @NotNull List<NodeTagPair> translateNodes(
            final @NotNull ProtocolAdapterEntity entity, final @NotNull ProtocolAdapterFactory factory) {
        final Class<? extends Node> nodeClass = factory.information().nodeClass();
        final List<NodeTagPair> nodes = new ArrayList<>(entity.getTags().size());
        for (final TagEntity tag : entity.getTags()) {
            final Node node;
            try {
                node = objectMapper.readValue(tag.getNodeString(), nodeClass);
            } catch (final JsonProcessingException exception) {
                throw new ProtocolAdapterConfigException(
                        "adapter ["
                                + entity.getAdapterId()
                                + "] tag ["
                                + tag.getName()
                                + "] node-string is not a valid "
                                + nodeClass.getSimpleName()
                                + ": "
                                + parseFailureDetail(exception),
                        exception);
            }
            // The access model is enforced here (EDG-824 #14): the pair carries the tag's EFFECTIVE transports —
            // a transport is usable only when declared on the tag AND permitted by the access flags. The read
            // variant (polled vs subscribed) is then selected from what is actually permitted.
            nodes.add(NodeTagPair.create(
                    node,
                    tag.getName(),
                    factory.nodeDefinitionSchema(),
                    ProtocolAdapterConfigSupport.effectivePollable(tag),
                    ProtocolAdapterConfigSupport.effectiveSubscribable(tag)));
        }
        return nodes;
    }

    /**
     * The human-usable detail of a node-string parse failure, woven into the configuration error so an operator can
     * tell a key typo from a value typo without a stack trace: a node class that rejects a field or value throws from
     * its creator/setter and that message (the root cause) is the precise one; plain malformed JSON has no cause, so
     * Jackson's own description (without its location suffix) is the best available.
     */
    private static @NotNull String parseFailureDetail(final @NotNull JsonProcessingException exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        final String rootCauseMessage = rootCause.getMessage();
        if (rootCause != exception && rootCauseMessage != null) {
            return rootCauseMessage;
        }
        final String originalMessage = exception.getOriginalMessage();
        return originalMessage != null ? originalMessage : exception.getClass().getSimpleName();
    }

    /**
     * The teardown of everything a successfully-built adapter owns on the framework side: the adapter's own
     * {@link AutoCloseable} (if it is one — a template adapter, or any adapter that releases resources on teardown),
     * then every dispatch binding it opened through the framework dispatcher. Closing both never double-closes a live
     * binding — the recorded bindings close at most once — so a template's single binding, closed first by the
     * adapter's own {@code close()}, is skipped by the recorder's pass. Best effort: a failing adapter close still
     * lets the recording dispatcher release the dispatch threads.
     *
     * @param protocolAdapter     the constructed adapter, closed if it is {@link AutoCloseable}.
     * @param recordingDispatcher the dispatcher that recorded every binding the adapter opened.
     * @return the composite teardown the container closes when the adapter is discarded.
     */
    private static @NotNull AutoCloseable adapterTeardown(
            final @NotNull ProtocolAdapter protocolAdapter, final @NotNull RecordingDispatcher recordingDispatcher) {
        final AutoCloseable selfClose = adapterSelfClose(protocolAdapter);
        return () -> {
            try {
                selfClose.close();
            } finally {
                recordingDispatcher.close();
            }
        };
    }

    /**
     * The adapter's own teardown alone: its {@link AutoCloseable#close()} if it is one, otherwise nothing. Used both
     * inside {@link #adapterTeardown} and on its own by the {@link ConstructionScope}, where the recording dispatcher
     * is already registered separately and must not be closed twice in the same unwind.
     */
    private static @NotNull AutoCloseable adapterSelfClose(final @NotNull ProtocolAdapter protocolAdapter) {
        return () -> {
            if (protocolAdapter instanceof AutoCloseable closeable) {
                closeable.close();
            }
        };
    }

    /**
     * The ownership scope of one adapter construction (EDG-824 finding 2, Sam round 2).
     * <p>
     * Building an adapter acquires resources that outlive the call — gauges on the shared {@link MetricRegistry},
     * consumers on the shared {@link TagManager}, a dispatch thread, a tick schedule, southbound subscriptions — one
     * step at a time. Before this scope existed only the adapter constructor's failure was handled, so a throw at any
     * later step returned no container and left every earlier resource with no owner. The leaked metrics were the
     * worst of them: a stale gauge makes the next registration for that id throw, which renders the adapter id
     * <b>permanently uncreatable</b> until restart.
     * <p>
     * Each resource is registered the instant it exists. {@link #closeAll} releases them in reverse acquisition order,
     * best effort — one failing close must not mask the construction failure being propagated, nor skip the
     * resources acquired before it. {@link #commit} transfers ownership to the container and empties the scope, so a
     * committed resource is never closed twice.
     * <p>
     * Not thread-safe, and does not need to be: one scope belongs to one {@code create} call on the manager thread.
     */
    private static final class ConstructionScope {

        private final @NotNull String adapterId;
        private final @NotNull Deque<AutoCloseable> acquired = new ArrayDeque<>();

        private ConstructionScope(final @NotNull String adapterId) {
            this.adapterId = adapterId;
        }

        private <T extends AutoCloseable> @NotNull T register(final @NotNull T resource) {
            acquired.push(resource);
            return resource;
        }

        /** Registers an optional collaborator — {@code null} when the edition or test rig does not wire it. */
        private <T extends AutoCloseable> @Nullable T registerOptional(final @Nullable T resource) {
            if (resource != null) {
                acquired.push(resource);
            }
            return resource;
        }

        private <T> @NotNull T commit(final @NotNull T container) {
            acquired.clear();
            return container;
        }

        private void closeAll() {
            while (!acquired.isEmpty()) {
                final AutoCloseable resource = acquired.pop();
                try {
                    resource.close();
                } catch (final Throwable closeFailure) {
                    AdapterFaults.rethrowIfFatal(closeFailure);
                    log.warn(
                            "Failed to release a {} while unwinding the failed construction of v2 adapter '{}'",
                            resource.getClass().getSimpleName(),
                            adapterId,
                            closeFailure);
                }
            }
        }
    }

    /**
     * A {@link MessageDispatcher} that delegates to the real dispatcher while recording every binding it hands out, so
     * the framework can release each one on teardown regardless of whether the adapter is {@link AutoCloseable}.
     * Attach and close are serialized under this dispatcher's monitor: once {@link #close()} has released the adapter's
     * bindings, a later {@code attach()} — a background adapter callback racing with or following the adapter's discard
     * — is rejected rather than silently opening a binding no owner would ever release. Every recorded binding closes
     * at most once, so closing is safe even after the adapter has already closed one itself (as a template adapter
     * does).
     */
    private static final class RecordingDispatcher implements MessageDispatcher, AutoCloseable {

        private final @NotNull MessageDispatcher delegate;
        private final @NotNull List<IdempotentHandle> handles = new ArrayList<>();
        private boolean closed;

        private RecordingDispatcher(final @NotNull MessageDispatcher delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
                final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
            if (closed) {
                // The framework has already released this adapter's bindings; opening one now would leak a dispatch
                // thread no later owner closes. Reject the attach-after-close rather than record an unowned binding.
                throw new IllegalStateException(
                        "the framework dispatcher is closed: the adapter that opened it has been discarded");
            }
            final IdempotentHandle handle = new IdempotentHandle(delegate.attach(mailbox, handler));
            handles.add(handle);
            return handle;
        }

        /**
         * Mark the dispatcher closed and release every binding opened through it, each at most once. Called on a
         * construction failure to release a half-built adapter's threads, and again on container teardown to release a
         * successfully-built adapter's — the per-binding idempotence makes the second pass a no-op for anything a
         * template adapter already closed itself. After this runs, {@link #attach} rejects further bindings.
         */
        @Override
        public synchronized void close() {
            closed = true;
            for (final IdempotentHandle handle : handles) {
                handle.close();
            }
        }
    }

    /**
     * A dispatcher handle that closes its delegate at most once, so the framework can safely close it both from the
     * adapter's own teardown and from {@link RecordingDispatcher#close()} without double-closing a live binding.
     */
    private static final class IdempotentHandle implements MessageDispatcherHandle {

        private final @NotNull MessageDispatcherHandle delegate;
        private boolean closed;

        private IdempotentHandle(final @NotNull MessageDispatcherHandle delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void close() {
            if (closed) {
                return;
            }
            closed = true;
            try {
                delegate.close();
            } catch (final RuntimeException ignored) {
                // Best effort: a failing detach must not mask teardown or the original construction failure.
            }
        }
    }

    /**
     * The framework services handed to a constructed adapter: the reused v1 value factory and the
     * dispatcher its mailbox attaches to.
     */
    private record WrapperServices(
            @NotNull DataPointFactory dataPointFactory,
            @NotNull MessageDispatcher dispatcher) implements ProtocolAdapterService {}

    /**
     * Everything one adapter instance is constructed from: its id, the reused v1 configuration value,
     * the node/tag pairs it serves, and the framework services.
     */
    private record WrapperInput(
            @NotNull String adapterId,
            @NotNull DataPoint adapterConfig,
            @NotNull List<NodeTagPair> nodes,
            @NotNull ProtocolAdapterService services)
            implements ProtocolAdapterInput {}
}
