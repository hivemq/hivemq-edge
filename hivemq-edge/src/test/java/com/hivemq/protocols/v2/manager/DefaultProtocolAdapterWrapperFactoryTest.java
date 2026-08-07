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

import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.adapter;
import static com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.tag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.hivemq.adapter.sdk.api.ProtocolAdapterCategory;
import com.hivemq.adapter.sdk.api.ProtocolAdapterTag;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterCapability;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.protocols.northbound.NorthboundConsumerFactory;
import com.hivemq.protocols.northbound.NorthboundTagConsumer;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestDataPointFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestProtocolAdapterInformation;
import com.hivemq.protocols.v2.runtime.Clock;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.runtime.ProtocolAdapterMetrics;
import com.hivemq.protocols.v2.southbound.SouthboundBrokerRuntime;
import com.hivemq.protocols.v2.southbound.SouthboundMqttIntake;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The production wrapper factory: it turns a read-only configuration entity into a fully-wired,
 * dispatcher-attached wrapper that — driven by the same activation kick the manager sends — reaches
 * {@code CONNECTED}, including the {@code node-string} deserialization and the config-to-{@code DataPoint}
 * translation. Run on a {@link FakeClock} + {@link ManualDispatcher} with a synchronous protocol-adapter double.
 */
class DefaultProtocolAdapterWrapperFactoryTest {

    private FakeClock clock;
    private ManualDispatcher dispatcher;
    private DefaultProtocolAdapterWrapperFactory factory;
    private TestProtocolAdapterFactory sdkFactory;

    @BeforeEach
    void setUp() {
        clock = new FakeClock();
        dispatcher = new ManualDispatcher();
        factory = new DefaultProtocolAdapterWrapperFactory(
                clock, dispatcher, new MetricRegistry(), new TestDataPointFactory(), new ObjectMapper(), 100);
        sdkFactory = new TestProtocolAdapterFactory(ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID);
    }

    @Test
    void buildsAndStartsARealWrapperToConnected() {
        final ProtocolAdapterEntity entity = adapter("a")
                .northboundActivated(true)
                .northboundMapping("temperature", "plant/a/temperature")
                .build();
        final RecordingHealth health = new RecordingHealth();

        final ProtocolAdapterContainer managed = factory.create(entity, sdkFactory, health);
        assertThat(managed.isReal()).isTrue();

        // The manager would send exactly this to bring the freshly-created wrapper to its config-declared goal.
        managed.handle()
                .wrapperSender()
                .tell(new ProtocolAdapterWrapperCommand.ApplyActivation(
                        ProtocolAdapterConfigSupport.goalOf(entity),
                        ProtocolAdapterConfigSupport.activationOf(entity)));
        dispatcher.drainAll();

        final AdapterStatusSnapshot snapshot = managed.handle().snapshot().get();
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.machineState()).isEqualTo(ProtocolAdapterWrapperState.CONNECTED);
        assertThat(snapshot.tags()).extracting(TagStatusSnapshot::tagName).contains("temperature");
        assertThat(health.started).containsExactly("a");

        managed.close();
    }

    @Test
    void translateNodes_deserializesNodeStringsIntoTheTypesNodeClass() {
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("temperature")
                        .nodeString("{\"identifier\":\"node-1\"}")
                        .build())
                .build();

        final List<NodeTagPair> nodes = factory.translateNodes(entity, sdkFactory);

        assertThat(nodes).hasSize(1);
        assertThat(nodes.get(0).tag().name()).isEqualTo("temperature");
        assertThat(nodes.get(0).node().nodeId()).isEqualTo("node-1");
    }

    @Test
    void invalidNodeString_isReportedAsAConfigurationException() {
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("temperature").nodeString("}{ not json").build())
                .build();

        assertThatThrownBy(() -> factory.create(entity, sdkFactory, ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(ProtocolAdapterConfigException.class)
                .hasMessageContaining("temperature");
    }

    @Test
    void invalidNodeString_surfacesJacksonsParseDetailInTheMessage() {
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("temperature").nodeString("}{ not json").build())
                .build();

        // The outer context alone ("node-string is not a valid …") tells an operator nothing about WHAT is wrong
        // with the string; the parse detail must ride along in the message, not hide in the exception cause.
        assertThatThrownBy(() -> factory.translateNodes(entity, sdkFactory))
                .isInstanceOf(ProtocolAdapterConfigException.class)
                .hasMessageContaining("node-string is not a valid TestNode")
                .hasMessageContaining("Unexpected close marker");
    }

    @Test
    void aNodeClassRejectingAValue_surfacesItsExactValidationMessageNotJustTheOuterContext() {
        final ProtocolAdapterEntity entity = adapter("a")
                .tags(tag("temperature").nodeString("{\"mode\":\"Sideways\"}").build())
                .build();

        // A node class that validates its own fields (an enum-like value, a strict unknown-field setter) throws with
        // a precise message; that root cause must survive to the operator-facing error so a value typo is
        // distinguishable from a key typo.
        assertThatThrownBy(() -> factory.translateNodes(entity, new RejectingNodeFactory()))
                .isInstanceOf(ProtocolAdapterConfigException.class)
                .hasMessageContaining("node-string is not a valid RejectingNode")
                .hasMessageContaining("unknown mode 'Sideways' (expected one of Steady, Pulse)");
    }

    @Test
    void staysStoppedWhenNeitherDirectionIsActivated() {
        final ProtocolAdapterEntity entity = adapter("a")
                .northboundActivated(false)
                .southboundActivated(false)
                .build();

        final ProtocolAdapterContainer managed =
                factory.create(entity, sdkFactory, ProtocolAdapterWrapperEventListener.NONE);
        managed.handle()
                .wrapperSender()
                .tell(new ProtocolAdapterWrapperCommand.ApplyActivation(
                        ProtocolAdapterConfigSupport.goalOf(entity),
                        ProtocolAdapterConfigSupport.activationOf(entity)));
        dispatcher.drainAll();

        final AdapterStatusSnapshot snapshot = managed.handle().snapshot().get();
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.machineState()).isEqualTo(ProtocolAdapterWrapperState.STOPPED);

        managed.close();
    }

    // ── EDG-792: the framework owns every dispatch binding an adapter opens through the service dispatcher ──────────

    @Test
    void directAdapterAttachingInItsConstructor_hasItsBindingReleasedOnContainerClose() {
        final CountingDispatcher counting = new CountingDispatcher();
        final DefaultProtocolAdapterWrapperFactory factoryOnCounting = new DefaultProtocolAdapterWrapperFactory(
                clock, counting, new MetricRegistry(), new TestDataPointFactory(), new ObjectMapper(), 100);

        final ProtocolAdapterContainer managed = factoryOnCounting.create(
                adapter("a").build(), new DirectDispatchingFactory(true), ProtocolAdapterWrapperEventListener.NONE);

        // A direct (non-AutoCloseable) adapter attached its own mailbox through the framework dispatcher in its
        // constructor, so two bindings are live: the wrapper's and the adapter's.
        assertThat(counting.liveBindings()).isEqualTo(2);

        // Container teardown must release the adapter's binding even though the adapter is not AutoCloseable.
        managed.close();
        assertThat(counting.liveBindings()).isZero();
    }

    @Test
    void directAdapterAttachingAfterConstruction_hasThatBindingReleasedOnContainerClose() {
        final CountingDispatcher counting = new CountingDispatcher();
        final DefaultProtocolAdapterWrapperFactory factoryOnCounting = new DefaultProtocolAdapterWrapperFactory(
                clock, counting, new MetricRegistry(), new TestDataPointFactory(), new ObjectMapper(), 100);
        final DirectDispatchingFactory directFactory = new DirectDispatchingFactory(false);

        final ProtocolAdapterContainer managed =
                factoryOnCounting.create(adapter("a").build(), directFactory, ProtocolAdapterWrapperEventListener.NONE);

        // Only the wrapper is bound so far; the direct adapter attached nothing in its constructor.
        assertThat(counting.liveBindings()).isEqualTo(1);

        // The adapter stored the framework dispatcher and attaches a mailbox later — the framework still owns that
        // binding because the recording dispatcher stays live for the adapter's whole lifetime.
        directFactory.lastAdapter().attachLater();
        assertThat(counting.liveBindings()).isEqualTo(2);

        managed.close();
        assertThat(counting.liveBindings()).isZero();
    }

    @Test
    void attachThroughTheServiceDispatcherAfterContainerClose_isRejectedAndOpensNoBinding() {
        final CountingDispatcher counting = new CountingDispatcher();
        final DefaultProtocolAdapterWrapperFactory factoryOnCounting = new DefaultProtocolAdapterWrapperFactory(
                clock, counting, new MetricRegistry(), new TestDataPointFactory(), new ObjectMapper(), 100);
        final DirectDispatchingFactory directFactory = new DirectDispatchingFactory(false);

        final ProtocolAdapterContainer managed =
                factoryOnCounting.create(adapter("a").build(), directFactory, ProtocolAdapterWrapperEventListener.NONE);
        managed.close();
        assertThat(counting.liveBindings()).isZero();

        // A background adapter callback that stored the framework dispatcher and attaches after the adapter has been
        // discarded must not silently open a dispatch thread no owner would ever release: the closed recording
        // dispatcher rejects the late attach and opens no binding.
        assertThatThrownBy(() -> directFactory.lastAdapter().attachLater()).isInstanceOf(IllegalStateException.class);
        assertThat(counting.liveBindings()).isZero();
    }

    @Test
    void constructionThatFailsAfterRegisteringMetrics_deregistersThem_soTheAdapterIdStaysCreatable() {
        // The metrics object registers GAUGES, and a duplicate gauge registration throws. Leaking them on a failed
        // build therefore does not merely waste memory: every later attempt at the same adapter id dies at the
        // registration with "A metric named ... already exists", reporting that instead of the real fault, for the
        // life of the process. The id would be permanently uncreatable.
        //
        // The failure has to land AFTER the metrics are registered, which is why it is injected at the wrapper's own
        // dispatcher attach rather than in the adapter's constructor.
        final MetricRegistry registry = new MetricRegistry();
        final ThrowingOnWrapperAttachDispatcher throwing = new ThrowingOnWrapperAttachDispatcher();
        final DefaultProtocolAdapterWrapperFactory failing = new DefaultProtocolAdapterWrapperFactory(
                clock, throwing, registry, new TestDataPointFactory(), new ObjectMapper(), 100);

        assertThatThrownBy(() ->
                        failing.create(adapter("a").build(), sdkFactory, ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("attach refused");

        assertThat(registry.getNames()).noneMatch(name -> name.startsWith(ProtocolAdapterMetrics.ADAPTER_PREFIX));

        // And the proof that matters: the same id can be built again without tripping over its own residue — the
        // second attempt must fail for its own reason, not with "A metric named ... already exists".
        assertThatThrownBy(() ->
                        failing.create(adapter("a").build(), sdkFactory, ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("attach refused");
    }

    @Test
    void constructionThatFailsAfterOpeningABinding_releasesItBeforeRethrowing() {
        final CountingDispatcher counting = new CountingDispatcher();
        final DefaultProtocolAdapterWrapperFactory factoryOnCounting = new DefaultProtocolAdapterWrapperFactory(
                clock, counting, new MetricRegistry(), new TestDataPointFactory(), new ObjectMapper(), 100);

        assertThatThrownBy(() -> factoryOnCounting.create(
                        adapter("a").build(),
                        new FailingAfterAttachFactory(),
                        ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("boom");
        // The half-built adapter opened one binding through the framework dispatcher before throwing; it must be
        // released, and the wrapper's own binding is never opened because construction failed first.
        assertThat(counting.liveBindings()).isZero();
    }

    // ── EDG-824 finding 2 (Sam round 2): construction is all-or-nothing ─────────────────────────────────────────
    // Building an adapter acquires resources one step at a time — gauges on the shared MetricRegistry, a dispatch
    // thread, a tick schedule, southbound subscriptions. Before the construction scope, only the adapter
    // constructor's failure was handled (and only for RuntimeException), so a throw at any later step returned no
    // container and left everything earlier with no owner.

    /** Metric names the shared registry still carries for one adapter — zero once its construction has unwound. */
    private static long metricsFor(final @NotNull MetricRegistry registry, final @NotNull String adapterId) {
        return registry.getNames().stream()
                .filter(name -> name.startsWith(ProtocolAdapterMetrics.ADAPTER_PREFIX + adapterId + "."))
                .count();
    }

    @Test
    void anAdapterConstructorRaisingALinkageError_stillReleasesTheBindingItOpened() {
        final CountingDispatcher counting = new CountingDispatcher();
        final MetricRegistry registry = new MetricRegistry();
        final DefaultProtocolAdapterWrapperFactory factoryOnCounting = new DefaultProtocolAdapterWrapperFactory(
                clock, counting, registry, new TestDataPointFactory(), new ObjectMapper(), 100);

        // A mispackaged or version-skewed adapter jar throws an Error, not a RuntimeException — the exact case the
        // wide catch exists for, and the one the old RuntimeException-only guard let through.
        assertThatThrownBy(() -> factoryOnCounting.create(
                        adapter("a").build(),
                        new FailingAfterAttachFactory(() -> new LinkageError("mispackaged adapter jar")),
                        ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(LinkageError.class);

        assertThat(counting.liveBindings()).isZero();
        assertThat(metricsFor(registry, "a")).isZero();
    }

    @Test
    void aFailureAttachingTheWrapper_releasesTheMetricsAlreadyRegistered() {
        final MetricRegistry registry = new MetricRegistry();
        final DefaultProtocolAdapterWrapperFactory factoryOnFailingDispatcher =
                new DefaultProtocolAdapterWrapperFactory(
                        clock, new FailingDispatcher(), registry, new TestDataPointFactory(), new ObjectMapper(), 100);

        assertThatThrownBy(() -> factoryOnFailingDispatcher.create(
                        adapter("a").build(), sdkFactory, ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("dispatcher attach failed");

        // The metrics were registered two steps earlier; nothing but the scope owned them at the moment of the throw.
        assertThat(metricsFor(registry, "a")).isZero();
    }

    @Test
    void aFailureSchedulingTheTick_releasesTheMetricsAndTheDispatchBinding() {
        final CountingDispatcher counting = new CountingDispatcher();
        final MetricRegistry registry = new MetricRegistry();
        final DefaultProtocolAdapterWrapperFactory factoryOnFailingClock = new DefaultProtocolAdapterWrapperFactory(
                new ScriptedClock(clock, 1), counting, registry, new TestDataPointFactory(), new ObjectMapper(), 100);

        assertThatThrownBy(() -> factoryOnFailingClock.create(
                        adapter("a").build(), sdkFactory, ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tick scheduling failed");

        assertThat(metricsFor(registry, "a")).isZero();
        assertThat(counting.liveBindings()).isZero();
    }

    @Test
    void aFailureBuildingTheSecondNorthboundConsumer_removesTheFirstOneAlreadyAdded() {
        final MetricRegistry registry = new MetricRegistry();
        final TagManager tagManager = spy(new TagManager());
        final NorthboundTagConsumer firstConsumer = mock(NorthboundTagConsumer.class);
        when(firstConsumer.getTagName()).thenReturn("temperature");
        final NorthboundConsumerFactory consumerFactory = mock(NorthboundConsumerFactory.class);
        when(consumerFactory.build(any(), any(), any(), any(), any()))
                .thenReturn(firstConsumer)
                .thenThrow(new IllegalStateException("consumer build failed"));

        final DefaultProtocolAdapterWrapperFactory factoryWithFailingConsumers =
                new DefaultProtocolAdapterWrapperFactory(
                        clock,
                        dispatcher,
                        registry,
                        new TestDataPointFactory(),
                        new ObjectMapper(),
                        100,
                        tagManager,
                        consumerFactory,
                        null);

        // Two mappings: the first consumer is built and added to the tag manager, the second throws. The registry
        // object is never returned, so before the fix nothing owned the live first consumer — it kept receiving
        // every value fed for its tag, for an adapter that does not exist.
        assertThatThrownBy(() -> factoryWithFailingConsumers.create(
                        adapter("a")
                                .northboundMapping("temperature", "plant/a/temperature")
                                .northboundMapping("pressure", "plant/a/pressure")
                                .build(),
                        sdkFactory,
                        ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("consumer build failed");

        verify(tagManager).addConsumer(firstConsumer);
        verify(tagManager).removeConsumer(firstConsumer);
        assertThat(metricsFor(registry, "a")).isZero();
    }

    @Test
    void reclaimOrphanedSouthboundQueues_clearsOnlyUnownedInternalQueues() {
        final ClientQueuePersistence clientQueuePersistence = mock(ClientQueuePersistence.class);
        final String owned = SouthboundMqttIntake.queueId("a1", "plant/a/set");
        final String removedAdapter = SouthboundMqttIntake.queueId("gone", "plant/g/set");
        final String movedTopic = SouthboundMqttIntake.queueId("a1", "plant/old/set");
        final String ordinaryShared = "ordinary-group/some/topic";
        when(clientQueuePersistence.getSharedQueues())
                .thenReturn(
                        Futures.immediateFuture(ImmutableSet.of(owned, removedAdapter, movedTopic, ordinaryShared)));
        when(clientQueuePersistence.clear(anyString(), anyBoolean())).thenReturn(Futures.immediateFuture(null));
        final DefaultProtocolAdapterWrapperFactory factoryWithBroker = new DefaultProtocolAdapterWrapperFactory(
                clock,
                dispatcher,
                new MetricRegistry(),
                new TestDataPointFactory(),
                new ObjectMapper(),
                100,
                null,
                null,
                new SouthboundBrokerRuntime(mock(LocalTopicTree.class), clientQueuePersistence));

        factoryWithBroker.reclaimOrphanedSouthboundQueues(List.of(
                adapter("a1").southboundMapping("plant/a/set", "temperature").build()));

        // The corpses: an adapter deleted while Edge was down, and a mapping topic moved while Edge was down.
        verify(clientQueuePersistence).clear(removedAdapter, true);
        verify(clientQueuePersistence).clear(movedTopic, true);
        // The configured queue keeps its durable commands, and an ordinary shared queue is not this sweep's to touch.
        verify(clientQueuePersistence, never()).clear(owned, true);
        verify(clientQueuePersistence, never()).clear(ordinaryShared, true);
    }

    @Test
    void anAdapterIdWhoseConstructionFailedLate_canBeCreatedAgain() {
        final MetricRegistry registry = new MetricRegistry();
        // Fails once, then behaves — the shape of a transient wiring failure during a reload or a restart loop.
        final DefaultProtocolAdapterWrapperFactory factoryOnFlakyClock = new DefaultProtocolAdapterWrapperFactory(
                new ScriptedClock(clock, 1), dispatcher, registry, new TestDataPointFactory(), new ObjectMapper(), 100);

        assertThatThrownBy(() -> factoryOnFlakyClock.create(
                        adapter("a").build(), sdkFactory, ProtocolAdapterWrapperEventListener.NONE))
                .isInstanceOf(IllegalStateException.class);

        // The decisive assertion: a leaked gauge from the failed attempt makes this registration throw
        // "A metric named protocol-adapter-v2.adapter.a.mailbox.depth already exists" — the adapter id would be
        // permanently uncreatable until restart, with no configuration change able to recover it.
        final ProtocolAdapterContainer managed =
                factoryOnFlakyClock.create(adapter("a").build(), sdkFactory, ProtocolAdapterWrapperEventListener.NONE);
        assertThat(managed.isReal()).isTrue();

        managed.close();
        assertThat(metricsFor(registry, "a")).isZero();
    }

    /** A dispatcher that refuses every attach — models the wrapper binding failing mid-construction. */
    private static final class FailingDispatcher implements MessageDispatcher {

        @Override
        public <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
                final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
            throw new IllegalStateException("dispatcher attach failed");
        }
    }

    /**
     * A {@link Clock} that fails its first {@code n} tick schedules and then delegates, tracking how many tick
     * handles are still live so a test can prove a failed construction cancelled the one it had scheduled.
     */
    private static final class ScriptedClock implements Clock {

        private final @NotNull Clock delegate;
        private int failuresRemaining;

        private ScriptedClock(final @NotNull Clock delegate, final int failuresRemaining) {
            this.delegate = delegate;
            this.failuresRemaining = failuresRemaining;
        }

        @Override
        public long nowMillis() {
            return delegate.nowMillis();
        }

        @Override
        public <MessageType extends MailboxMessage> @NotNull AutoCloseable scheduleTick(
                final long periodMillis,
                final @NotNull MailboxSender<MessageType> target,
                final @NotNull Supplier<MessageType> tickMessage) {
            if (failuresRemaining > 0) {
                failuresRemaining--;
                throw new IllegalStateException("tick scheduling failed");
            }
            return delegate.scheduleTick(periodMillis, target, tickMessage);
        }
    }

    /** Refuses every binding — the test adapter opens none, so the first attach is the wrapper's, well after the
     * metrics are registered. */
    private static final class ThrowingOnWrapperAttachDispatcher implements MessageDispatcher {

        @Override
        public <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
                final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
            throw new IllegalStateException("attach refused");
        }
    }

    private static final class CountingDispatcher implements MessageDispatcher {

        private int attaches;
        private int detaches;

        @Override
        public <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
                final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
            attaches++;
            return () -> detaches++;
        }

        private int liveBindings() {
            return attaches - detaches;
        }
    }

    /**
     * A factory whose {@code createAdapter} builds a {@link DirectDispatchingAdapter} — a direct, non-AutoCloseable
     * adapter that opens its dispatch binding through the framework dispatcher. The last-built instance is captured so
     * a test can drive a post-construction attach on it.
     */
    private static final class DirectDispatchingFactory implements ProtocolAdapterFactory {

        private final @NotNull ProtocolAdapterInformation information =
                new TestProtocolAdapterInformation(ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID);
        private final boolean attachInConstructor;
        private @NotNull DirectDispatchingAdapter lastAdapter = new DirectDispatchingAdapter(null, false);

        private DirectDispatchingFactory(final boolean attachInConstructor) {
            this.attachInConstructor = attachInConstructor;
        }

        @Override
        public @NotNull ProtocolAdapterInformation information() {
            return information;
        }

        @Override
        public @NotNull ProtocolAdapter createAdapter(
                final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
            lastAdapter = new DirectDispatchingAdapter(input.services().dispatcher(), attachInConstructor);
            return lastAdapter;
        }

        private @NotNull DirectDispatchingAdapter lastAdapter() {
            return lastAdapter;
        }

        @Override
        public @NotNull Schema adapterConfigSchema() {
            return ProtocolAdapterManagerTestSupport.scalarSchema();
        }

        @Override
        public @NotNull Schema nodeDefinitionSchema() {
            return ProtocolAdapterManagerTestSupport.scalarSchema();
        }
    }

    /**
     * A factory whose {@code createAdapter} opens a dispatch binding through the framework dispatcher and then throws,
     * modelling a construction that fails after a binding was already opened — the framework must release it.
     */
    private static final class FailingAfterAttachFactory implements ProtocolAdapterFactory {

        private final @NotNull ProtocolAdapterInformation information =
                new TestProtocolAdapterInformation(ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID);
        private final @NotNull Supplier<Throwable> fault;

        private FailingAfterAttachFactory() {
            this(() -> new IllegalStateException("boom while constructing the adapter"));
        }

        /** @param fault the throwable the constructor raises — a {@link RuntimeException} or an {@link Error}. */
        private FailingAfterAttachFactory(final @NotNull Supplier<Throwable> fault) {
            this.fault = fault;
        }

        @Override
        public @NotNull ProtocolAdapterInformation information() {
            return information;
        }

        @Override
        public @NotNull ProtocolAdapter createAdapter(
                final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
            input.services().dispatcher().attach(new DefaultMailbox<DirectMessage>(), message -> {});
            final Throwable failure = fault.get();
            if (failure instanceof final Error error) {
                throw error;
            }
            throw (RuntimeException) failure;
        }

        @Override
        public @NotNull Schema adapterConfigSchema() {
            return ProtocolAdapterManagerTestSupport.scalarSchema();
        }

        @Override
        public @NotNull Schema nodeDefinitionSchema() {
            return ProtocolAdapterManagerTestSupport.scalarSchema();
        }
    }

    /**
     * A direct {@link ProtocolAdapter} — deliberately NOT {@link AutoCloseable} — that opens its dispatch binding
     * through the framework {@link MessageDispatcher} the SDK exposes via {@code input.services().dispatcher()}, exactly
     * as an author who does not use the template may. It attaches either in its constructor or, via {@link #attachLater},
     * after construction, to prove the framework releases the binding on container teardown though it cannot close the
     * adapter itself.
     */
    private static final class DirectDispatchingAdapter implements ProtocolAdapter {

        private final @Nullable MessageDispatcher dispatcher;

        private DirectDispatchingAdapter(
                final @Nullable MessageDispatcher dispatcher, final boolean attachInConstructor) {
            this.dispatcher = dispatcher;
            if (attachInConstructor) {
                attachLater();
            }
        }

        private void attachLater() {
            if (dispatcher != null) {
                dispatcher.attach(new DefaultMailbox<DirectMessage>(), message -> {});
            }
        }

        @Override
        public @NotNull String adapterId() {
            return "direct";
        }

        @Override
        public void start() {}

        @Override
        public void stop() {}

        @Override
        public void connect() {}

        @Override
        public void disconnect() {}

        @Override
        public void verifyBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void pollBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void addSubscriptionBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void removeSubscriptionBatch(final @NotNull List<Node> nodes) {}

        @Override
        public void writeBatch(final @NotNull List<WriteEntry> entries) {}

        @Override
        public void browse(final int requestId, final @NotNull BrowseFilter filter, final int maxReferences) {}

        @Override
        public void browseNext(final int requestId, final @NotNull BrowseContinuation continuation) {}

        @Override
        public void readNodeAttributes(final int requestId, final @NotNull List<Node> nodes) {}
    }

    /**
     * A trivial mailbox message the direct adapter double attaches a mailbox for.
     */
    private record DirectMessage() implements MailboxMessage {}

    /**
     * A node whose creator rejects a value — modeling an adapter's own node validation (the databases adapter's
     * split-mode rejection is the shipped example) — so a test can prove the validation message survives to the
     * configuration error instead of being swallowed as an unread exception cause.
     */
    private static final class RejectingNode extends Node {

        @JsonCreator
        private RejectingNode(@JsonProperty("mode") final @Nullable String mode) {
            throw new IllegalArgumentException("unknown mode '" + mode + "' (expected one of Steady, Pulse)");
        }

        @Override
        public @NotNull String nodeId() {
            return "rejecting";
        }

        @Override
        public @NotNull String nodeString() {
            return "{}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.noneOf(NodeProperty.class);
        }
    }

    /**
     * A factory double whose declared node class is the {@link RejectingNode}. Only {@code information().nodeClass()}
     * and {@code nodeDefinitionSchema()} matter to {@code translateNodes}; adapter creation is never reached.
     */
    private static final class RejectingNodeFactory implements ProtocolAdapterFactory {

        @Override
        public @NotNull ProtocolAdapterInformation information() {
            return new ProtocolAdapterInformation() {
                @Override
                public @NotNull String protocolId() {
                    return ProtocolAdapterManagerTestSupport.TEST_PROTOCOL_ID;
                }

                @Override
                public @NotNull String displayName() {
                    return "Rejecting Node Adapter";
                }

                @Override
                public @NotNull String description() {
                    return "A double whose node class rejects its value.";
                }

                @Override
                public @NotNull String version() {
                    return "1";
                }

                @Override
                public @NotNull String logoUrl() {
                    return "";
                }

                @Override
                public @NotNull String author() {
                    return "HiveMQ";
                }

                @Override
                public @NotNull ProtocolAdapterCategory category() {
                    return ProtocolAdapterCategory.SIMULATION;
                }

                @Override
                public @NotNull List<ProtocolAdapterTag> tags() {
                    return List.of();
                }

                @Override
                public @NotNull EnumSet<ProtocolAdapterCapability> capabilities() {
                    return EnumSet.noneOf(ProtocolAdapterCapability.class);
                }

                @Override
                public @NotNull Class<? extends Node> nodeClass() {
                    return RejectingNode.class;
                }

                @Override
                public int currentConfigVersion() {
                    return 2;
                }
            };
        }

        @Override
        public @NotNull ProtocolAdapter createAdapter(
                final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
            throw new UnsupportedOperationException("translateNodes never creates an adapter");
        }

        @Override
        public @NotNull Schema adapterConfigSchema() {
            return ProtocolAdapterManagerTestSupport.scalarSchema();
        }

        @Override
        public @NotNull Schema nodeDefinitionSchema() {
            return ProtocolAdapterManagerTestSupport.scalarSchema();
        }
    }

    private static final class RecordingHealth implements ProtocolAdapterWrapperEventListener {

        private final @NotNull List<String> started = new ArrayList<>();
        private final @NotNull List<String> stopped = new ArrayList<>();
        private final @NotNull List<String> errored = new ArrayList<>();
        private final @NotNull List<String> stopFailed = new ArrayList<>();
        private final @NotNull List<String> died = new ArrayList<>();

        @Override
        public void wrapperStarted(final @NotNull String adapterId) {
            started.add(adapterId);
        }

        @Override
        public void wrapperStopped(final @NotNull String adapterId) {
            stopped.add(adapterId);
        }

        @Override
        public void wrapperError(final @NotNull String adapterId, final @NotNull String reason) {
            errored.add(adapterId);
        }

        @Override
        public void wrapperStopFailed(final @NotNull String adapterId, final @NotNull String reason) {
            stopFailed.add(adapterId);
        }

        @Override
        public void wrapperDied(final @NotNull String adapterId, final @NotNull String reason) {
            died.add(adapterId);
        }
    }
}
