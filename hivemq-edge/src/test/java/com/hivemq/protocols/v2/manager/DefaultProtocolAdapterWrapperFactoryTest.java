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
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.protocols.v2.config.ProtocolAdapterEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestDataPointFactory;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport.TestProtocolAdapterFactory;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The production wrapper factory (design §8.2): it turns a read-only configuration entity into a fully-wired,
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
