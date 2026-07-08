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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.schema.Schema;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.v2.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.v2.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.DefaultMailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.adapter.sdk.api.v2.model.BrowseContinuation;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestDataPointFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestProtocolAdapterInformation;
import com.hivemq.protocols.v2.runtime.FakeClock;
import com.hivemq.protocols.v2.runtime.ManualDispatcher;
import com.hivemq.protocols.v2.view.AdapterStatusSnapshot;
import com.hivemq.protocols.v2.view.TagStatusSnapshot;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperCommand;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperEventListener;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperState;
import java.util.ArrayList;
import java.util.List;
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

    /**
     * A {@link MessageDispatcher} double that counts the bindings it hands out and the ones later closed, so a test can
     * assert every binding an adapter opened is released on teardown and a failed construction leaves none behind.
     */
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

        @Override
        public @NotNull ProtocolAdapterInformation information() {
            return information;
        }

        @Override
        public @NotNull ProtocolAdapter createAdapter(
                final @NotNull ProtocolAdapterInput input, final @NotNull ProtocolAdapterOutput output) {
            input.services().dispatcher().attach(new DefaultMailbox<DirectMessage>(), message -> {});
            throw new IllegalStateException("boom while constructing the adapter");
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

    private static final class RecordingHealth implements ProtocolAdapterWrapperEventListener {

        private final @NotNull List<String> started = new ArrayList<>();
        private final @NotNull List<String> stopped = new ArrayList<>();
        private final @NotNull List<String> errored = new ArrayList<>();

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
    }
}
