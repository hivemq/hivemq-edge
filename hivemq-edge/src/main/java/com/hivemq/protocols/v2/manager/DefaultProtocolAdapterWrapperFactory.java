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
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.services.ProtocolAdapterService;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.config.TagEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterHandleRegistry.ProtocolAdapterHandle;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.NevskyMetrics;
import com.hivemq.protocols.v2.runtime.RetryPolicy;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;

/**
 * Production {@link ProtocolAdapterWrapperFactory}: assembles the full wrapper/adapter actor for one configuration
 * (design §6, §8.2), exactly as the wrapper test rig does but driven from the read-only configuration and the
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
 */
public final class DefaultProtocolAdapterWrapperFactory implements ProtocolAdapterWrapperFactory {

    private final @NotNull Clock clock;
    private final @NotNull MessageDispatcher dispatcher;
    private final @NotNull MetricRegistry metricRegistry;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull ObjectMapper objectMapper;
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
        this.clock = clock;
        this.dispatcher = dispatcher;
        this.metricRegistry = metricRegistry;
        this.dataPointFactory = dataPointFactory;
        this.objectMapper = objectMapper;
        this.tickPeriodMillis = tickPeriodMillis;
    }

    @Override
    public @NotNull ProtocolAdapterContainer create(
            final @NotNull ProtocolAdapterEntity entity,
            final @NotNull ProtocolAdapterFactory factory,
            final @NotNull ProtocolAdapterWrapperEventListener healthListener) {
        final String adapterId = entity.getAdapterId();

        final Mailbox<ProtocolAdapterWrapperMessage> mailbox = new DefaultMailbox<>();
        final ProtocolAdapterOutput output = new ProtocolAdapterOutputFacade(mailbox);

        final List<NodeTagPair> nodes = translateNodes(entity, factory);
        final DataPoint adapterConfig =
                dataPointFactory.createJsonDataPoint(adapterId, entity.getAdapterConfiguration());
        final ProtocolAdapterService services = new WrapperServices(dataPointFactory, dispatcher);
        final ProtocolAdapterInput input = new WrapperInput(adapterId, adapterConfig, nodes, services);
        final ProtocolAdapter protocolAdapter = factory.createAdapter(input, output);

        final NevskyMetrics metrics = new NevskyMetrics(metricRegistry, adapterId, mailbox::size);
        final ProtocolAdapterGoalState goal = ProtocolAdapterConfigSupport.goalOf(entity);
        final Map<String, TagAspectActivationPreference> activation = ProtocolAdapterConfigSupport.activationOf(entity);
        final Set<String> readUsed = entity.getReadUsedTagNames();
        final Set<String> writeUsed = entity.getWriteUsedTagNames();
        final RetryPolicy retryPolicy = entity.getRetryPolicy().toRetryPolicy();

        final TagAspectRuntimeCoordinator tagPlane = new TagAspectRuntimeCoordinator(
                adapterId,
                nodes,
                activation,
                readUsed,
                writeUsed,
                goal,
                ProtocolAdapterConfigSupport.pollIntervalMillisOf(entity),
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
                metrics);
        tagPlane.bindRuntime(
                context.clock(),
                context.timers(),
                context.batches(),
                context.metrics(),
                context.protocolAdapter()::verifyBatch);

        final AtomicReference<AdapterStatusSnapshot> snapshot = new AtomicReference<>();
        final ProtocolAdapterWrapper wrapper = new ProtocolAdapterWrapper(context, snapshot);
        final MessageDispatcherHandle dispatcherHandle = dispatcher.attach(mailbox, wrapper);
        final AutoCloseable tickHandle =
                clock.scheduleTick(tickPeriodMillis, mailbox, () -> new ProtocolAdapterWrapperTick(clock.nowMillis()));

        final ProtocolAdapterHandle handle = new ProtocolAdapterHandle(adapterId, mailbox, snapshot);
        return new ProtocolAdapterContainer(handle, dispatcherHandle, tickHandle, metrics, entity);
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
                                + nodeClass.getSimpleName(),
                        exception);
            }
            nodes.add(NodeTagPair.create(
                    node, tag.getName(), factory.nodeDefinitionSchema(), tag.isPollable(), tag.isSubscribable()));
        }
        return nodes;
    }

    /**
     * The framework services handed to a constructed adapter (design §3.8): the reused v1 value factory and the
     * dispatcher its mailbox attaches to.
     */
    private record WrapperServices(
            @NotNull DataPointFactory dataPointFactory,
            @NotNull MessageDispatcher dispatcher) implements ProtocolAdapterService {}

    /**
     * Everything one adapter instance is constructed from (design §3.8): its id, the reused v1 configuration value,
     * the node/tag pairs it serves, and the framework services.
     */
    private record WrapperInput(
            @NotNull String adapterId,
            @NotNull DataPoint adapterConfig,
            @NotNull List<NodeTagPair> nodes,
            @NotNull ProtocolAdapterService services)
            implements ProtocolAdapterInput {}
}
