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
package com.hivemq.edge.adapters.opcua.conformance;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.model.BrowseFilter;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
import com.hivemq.adapter.sdk.api.v2.model.WriteEntry;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.v2.services.ProtocolAdapterService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDG-737 — OPC-UA conformance of the SDK v2 foundation model. Drives a real OPC-UA adapter
 * ({@link OpcUaConformanceAdapter}) built on {@link com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter}
 * against an embedded Milo server, through the SDK v2 command/event contract, asserting the model carries real
 * OPC-UA interactions: connect/verify/poll (1), write (2), subscribe (3), browse (4).
 */
class OpcUaFoundationConformanceTest {

    // Embedded TestNamespace nodes register at ns=1 (see TestNamespace#addDynamicNodes).
    private static final @NotNull OpcUaConformanceNode INT32_NODE = new OpcUaConformanceNode("ns=1;i=11");
    private static final @NotNull OpcUaConformanceNode DOUBLE_NODE = new OpcUaConformanceNode("ns=1;i=13");
    // the standard OPC-UA Objects folder — the browse root.
    private static final @NotNull OpcUaConformanceNode OBJECTS_FOLDER = new OpcUaConformanceNode("ns=0;i=85");
    @RegisterExtension
    final @NotNull EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();
    private final @NotNull ConformanceDataPointFactory dataPointFactory = new ConformanceDataPointFactory();

    @Test
    void connectVerifyPoll_carryThroughTheFoundationModel() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);
        final List<Node> nodes = List.of(INT32_NODE, DOUBLE_NODE);

        adapter.verifyBatch(nodes);
        dispatcher.drainAll();
        assertThat(output.verifyOutcomes.keySet()).containsExactlyInAnyOrderElementsOf(nodes);
        assertThat(output.verifyOutcomes.values()).as("every declared node verifies as Success against the device")
                .allMatch(outcome -> outcome instanceof VerifyOutcome.Success);

        adapter.pollBatch(nodes);
        dispatcher.drainAll();
        assertThat(output.nodeErrors).as("no poll errors").isEmpty();
        assertThat(output.dataPoints.keySet()).containsExactlyInAnyOrderElementsOf(nodes);
        assertThat(output.dataPoints.get(INT32_NODE).getTagValue()).isInstanceOf(Number.class);
        assertThat(output.dataPoints.get(DOUBLE_NODE).getTagValue()).isInstanceOf(Number.class);

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.disconnected).as("disconnected()").isTrue();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    @Test
    void write_carriesSouthboundThroughTheFoundationModel() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        adapter.writeBatch(List.of(new WriteEntry(INT32_NODE, dataPointFactory.create("write", 4242))));
        dispatcher.drainAll();

        // Conformance is the southbound round-trip: write command -> device -> writeResult flows through the
        // model. The embedded TestNamespace nodes are getValue-callback backed and grant no UserAccessLevel to
        // the anonymous session, so the device itself declines the write — correct device behaviour, asserted
        // here as a returned result rather than a forced success (a read-back cannot reflect a write on these
        // synthetic nodes either, since reads return a random callback value).
        assertThat(output.writeResults).as("southbound write round-trips to a writeResult through the foundation model")
                .containsKey(INT32_NODE);
    }

    @Test
    void subscribe_incrementalAdd_deliversPushedValuesForEachNode() throws Exception {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        // incremental add: two separate batches; the second must not reset the first
        adapter.addSubscriptionBatch(List.of(INT32_NODE));
        dispatcher.drainAll();
        adapter.addSubscriptionBatch(List.of(DOUBLE_NODE));
        dispatcher.drainAll();

        assertThat(output.awaitDataPoint(INT32_NODE, TimeUnit.SECONDS.toMillis(10))).as(
                "a pushed value arrives for the first-subscribed node").isTrue();
        assertThat(output.awaitDataPoint(DOUBLE_NODE, TimeUnit.SECONDS.toMillis(10))).as(
                "the second incremental subscription also delivers — the first was not reset").isTrue();
        assertThat(output.dataPoints.get(INT32_NODE).getTagValue()).isInstanceOf(Number.class);

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.disconnected).as("disconnected()").isTrue();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    @Test
    void browse_returnsTheDeviceVariables_throughTheFoundationModel() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final OpcUaConformanceAdapter adapter = connectedAdapter(dispatcher, output);

        adapter.browse(new BrowseFilter(OBJECTS_FOLDER));
        dispatcher.drainAll();

        assertThat(output.browseResults).as("browse produced a result").isNotNull();
        final var discoveredNodeIds = requireNonNull(output.browseResults).stream()
                .map(entry -> entry.node().nodeId())
                .collect(Collectors.toSet());
        assertThat(discoveredNodeIds).as("the recursive, paginated browse surfaced the test variables")
                .contains(INT32_NODE.nodeId(), DOUBLE_NODE.nodeId());
        assertThat(output.browseResults).allMatch(BrowseResultEntry::selectable);

        adapter.disconnect();
        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.disconnected).as("disconnected()").isTrue();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    private @NotNull OpcUaConformanceAdapter connectedAdapter(
            final @NotNull DrainOnCallDispatcher dispatcher,
            final @NotNull RecordingOutput output) {
        final ProtocolAdapterInput input = ConformanceInput.create(dispatcher);
        final OpcUaConformanceAdapter adapter = new OpcUaConformanceAdapter(input, output, server.getServerUri());
        adapter.start();
        dispatcher.drainAll();
        assertThat(output.started).as("started()").isTrue();
        adapter.connect();
        dispatcher.drainAll();
        assertThat(output.errors).as("no connection error").isEmpty();
        assertThat(output.connected).as("connected()").isTrue();
        return adapter;
    }

    private static final class OpcUaConformanceNode extends Node {
        private final @NotNull String parseableNodeId;

        private OpcUaConformanceNode(final @NotNull String parseableNodeId) {
            this.parseableNodeId = parseableNodeId;
        }

        @Override
        public @NotNull String nodeId() {
            return parseableNodeId;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"nodeId\":\"" + parseableNodeId + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE, NodeProperty.TYPED);
        }
    }

    private static final class RecordingOutput implements ProtocolAdapterOutput {
        private final @NotNull Object lock = new Object();
        private final @NotNull List<String> errors = new ArrayList<>();
        private final @NotNull List<String> nodeErrors = new ArrayList<>();
        private final @NotNull Map<Node, VerifyOutcome> verifyOutcomes = new LinkedHashMap<>();
        private final @NotNull Map<Node, DataPoint> dataPoints = new LinkedHashMap<>();
        private final @NotNull Map<Node, Boolean> writeResults = new LinkedHashMap<>();
        private volatile boolean started;
        private volatile boolean stopped;
        private volatile boolean connected;
        private volatile boolean disconnected;
        private volatile @Nullable List<BrowseResultEntry> browseResults;

        @Override
        public void started() {
            started = true;
        }

        @Override
        public void stopped() {
            stopped = true;
        }

        @Override
        public void connected() {
            connected = true;
        }

        @Override
        public void disconnected() {
            disconnected = true;
        }

        @Override
        public void error(final @NotNull ErrorScope scope, final @NotNull String reason) {
            synchronized (lock) {
                errors.add(scope + ": " + reason);
            }
        }

        @Override
        public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
            synchronized (lock) {
                verifyOutcomes.put(node, outcome);
            }
        }

        @Override
        public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
            synchronized (lock) {
                dataPoints.put(node, value);
                lock.notifyAll();
            }
        }

        @Override
        public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
            synchronized (lock) {
                nodeErrors.add(node.nodeId() + ": " + reason);
            }
        }

        @Override
        public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
            synchronized (lock) {
                writeResults.put(node, success);
            }
        }

        @Override
        public void browseResult(final @NotNull List<BrowseResultEntry> entries) {
            browseResults = entries;
        }

        boolean awaitDataPoint(final @NotNull Node node, final long timeoutMillis) throws InterruptedException {
            final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            synchronized (lock) {
                while (!dataPoints.containsKey(node)) {
                    final long remainingMillis = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
                    if (remainingMillis <= 0) {
                        return false;
                    }
                    lock.wait(remainingMillis);
                }
                return true;
            }
        }
    }

    private record ConformanceInput(@NotNull String adapterId, @NotNull DataPoint adapterConfig,
                                    @NotNull List<NodeTagPair> nodes, @NotNull ProtocolAdapterService services)
            implements ProtocolAdapterInput {

        static @NotNull ConformanceInput create(
                final @NotNull MessageDispatcher dispatcher) {
            final ConformanceDataPointFactory factory = new ConformanceDataPointFactory();
            return new ConformanceInput("opcua-conformance",
                    factory.createJsonDataPoint("adapterConfig", JsonNodeFactory.instance.objectNode()),
                    List.of(),
                    new ConformanceService(factory, dispatcher));
        }
    }

    private record ConformanceService(@NotNull DataPointFactory dataPointFactory, @NotNull MessageDispatcher dispatcher)
            implements ProtocolAdapterService {
    }

    private static final class ConformanceDataPointFactory implements DataPointFactory {
        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, false);
        }

        @Override
        public @NotNull DataPoint createJsonDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, true);
        }

        private record SimpleDataPoint(@NotNull String tagName, @NotNull Object tagValue, boolean json)
                implements DataPoint {
            @Override
            public @NotNull Object getTagValue() {
                return tagValue;
            }

            @Override
            public boolean treatTagValueAsJson() {
                return json;
            }

            @Override
            public @NotNull String getTagName() {
                return tagName;
            }
        }
    }
}
