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
import com.hivemq.adapter.sdk.api.v2.messaging.Mailbox;
import com.hivemq.adapter.sdk.api.v2.messaging.MailboxMessage;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcher;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageDispatcherHandle;
import com.hivemq.adapter.sdk.api.v2.messaging.MessageHandler;
import com.hivemq.adapter.sdk.api.v2.model.BrowseResultEntry;
import com.hivemq.adapter.sdk.api.v2.model.ErrorScope;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.v2.model.ProtocolAdapterOutput;
import com.hivemq.adapter.sdk.api.v2.model.VerifyOutcome;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDG-737 — OPC-UA conformance of the SDK v2 foundation model. Drives a real OPC-UA adapter
 * ({@link OpcUaConformanceAdapter}) built on {@link com.hivemq.adapter.sdk.api.v2.template.AbstractProtocolAdapter}
 * against an embedded Milo server, through the SDK v2 command/event contract, asserting the model carries real
 * OPC-UA interactions. Scenario 1: connect → verify → poll. (Subscribe / browse / write are later scenarios.)
 */
class OpcUaFoundationConformanceTest {

    @RegisterExtension
    final EmbeddedOpcUaServerExtension server = new EmbeddedOpcUaServerExtension();

    // Embedded TestNamespace nodes register at ns=1 (see TestNamespace#addDynamicNodes).
    private static final @NotNull OpcUaConformanceNode INT32_NODE = new OpcUaConformanceNode("ns=1;i=11");
    private static final @NotNull OpcUaConformanceNode DOUBLE_NODE = new OpcUaConformanceNode("ns=1;i=13");

    @Test
    void connectVerifyPoll_carryThroughTheFoundationModel() {
        final DrainOnCallDispatcher dispatcher = new DrainOnCallDispatcher();
        final RecordingOutput output = new RecordingOutput();
        final ProtocolAdapterInput input = ConformanceInput.create("opcua-conformance", dispatcher);
        final OpcUaConformanceAdapter adapter =
                new OpcUaConformanceAdapter(input, output, server.getServerUri());
        final List<Node> nodes = List.of(INT32_NODE, DOUBLE_NODE);

        adapter.start();
        dispatcher.drainAll();
        assertThat(output.started).as("started() after start()").isTrue();

        adapter.connect();
        dispatcher.drainAll();
        assertThat(output.errors).as("no connection error").isEmpty();
        assertThat(output.connected).as("connected() after connect()").isTrue();

        adapter.verifyBatch(nodes);
        dispatcher.drainAll();
        assertThat(output.verifyOutcomes.keySet()).containsExactlyInAnyOrderElementsOf(nodes);
        assertThat(output.verifyOutcomes.values())
                .as("every declared node verifies as Success against the device")
                .allMatch(outcome -> outcome instanceof VerifyOutcome.Success);

        adapter.pollBatch(nodes);
        dispatcher.drainAll();
        assertThat(output.nodeErrors).as("no poll errors").isEmpty();
        assertThat(output.dataPoints.keySet()).containsExactlyInAnyOrderElementsOf(nodes);
        assertThat(output.dataPoints.get(INT32_NODE).getTagValue()).isInstanceOf(Number.class);
        assertThat(output.dataPoints.get(DOUBLE_NODE).getTagValue()).isInstanceOf(Number.class);

        adapter.stop();
        dispatcher.drainAll();
        assertThat(output.stopped).as("stopped() after stop()").isTrue();
    }

    // ── a concrete OPC-UA node: its identity is the parseable NodeId string ──────────────────────────

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

    // ── a deterministic dispatcher: drains on the calling thread (mirrors the SDK's ManualDispatcher) ─

    private static final class DrainOnCallDispatcher implements MessageDispatcher {
        private final @NotNull List<Binding<?>> bindings = new ArrayList<>();

        @Override
        public <MessageType extends MailboxMessage> @NotNull MessageDispatcherHandle attach(
                final @NotNull Mailbox<MessageType> mailbox, final @NotNull MessageHandler<MessageType> handler) {
            final Binding<MessageType> binding = new Binding<>(mailbox, handler);
            bindings.add(binding);
            return () -> bindings.remove(binding);
        }

        void drainAll() {
            boolean drainedSomething = true;
            while (drainedSomething) {
                drainedSomething = false;
                for (final Binding<?> binding : List.copyOf(bindings)) {
                    while (binding.drainOne()) {
                        drainedSomething = true;
                    }
                }
            }
        }

        private record Binding<MessageType extends MailboxMessage>(
                @NotNull Mailbox<MessageType> mailbox, @NotNull MessageHandler<MessageType> handler) {
            boolean drainOne() {
                final @Nullable MessageType message = mailbox.poll();
                if (message == null) {
                    return false;
                }
                handler.receive(message);
                return true;
            }
        }
    }

    // ── a recording output ───────────────────────────────────────────────────────────────────────

    private static final class RecordingOutput implements ProtocolAdapterOutput {
        private boolean started;
        private boolean stopped;
        private boolean connected;
        private boolean disconnected;
        private final @NotNull List<String> errors = new ArrayList<>();
        private final @NotNull List<String> nodeErrors = new ArrayList<>();
        private final @NotNull Map<Node, VerifyOutcome> verifyOutcomes = new LinkedHashMap<>();
        private final @NotNull Map<Node, DataPoint> dataPoints = new LinkedHashMap<>();

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
            errors.add(scope + ": " + reason);
        }

        @Override
        public void verifyResult(final @NotNull Node node, final @NotNull VerifyOutcome outcome) {
            verifyOutcomes.put(node, outcome);
        }

        @Override
        public void dataPoint(final @NotNull Node node, final @NotNull DataPoint value) {
            dataPoints.put(node, value);
        }

        @Override
        public void nodeError(final @NotNull Node node, final @NotNull String reason, final boolean spontaneous) {
            nodeErrors.add(node.nodeId() + ": " + reason);
        }

        @Override
        public void writeResult(final @NotNull Node node, final boolean success, final @Nullable String reason) {
        }

        @Override
        public void browseResult(final @NotNull List<BrowseResultEntry> entries) {
        }
    }

    // ── minimal construction input + services + datapoint factory ────────────────────────────────

    private record ConformanceInput(
            @NotNull String adapterId,
            @NotNull DataPoint adapterConfig,
            @NotNull List<NodeTagPair> nodes,
            @NotNull ProtocolAdapterService services) implements ProtocolAdapterInput {

        static @NotNull ConformanceInput create(
                final @NotNull String adapterId, final @NotNull MessageDispatcher dispatcher) {
            final ConformanceDataPointFactory factory = new ConformanceDataPointFactory();
            return new ConformanceInput(
                    adapterId,
                    factory.createJsonDataPoint("adapterConfig", JsonNodeFactory.instance.objectNode()),
                    List.of(),
                    new ConformanceService(factory, dispatcher));
        }
    }

    private record ConformanceService(@NotNull DataPointFactory dataPointFactory, @NotNull MessageDispatcher dispatcher)
            implements ProtocolAdapterService {
    }

    private static final class ConformanceDataPointFactory implements DataPointFactory {
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

        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, false);
        }

        @Override
        public @NotNull DataPoint createJsonDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new SimpleDataPoint(tagName, tagValue, true);
        }
    }
}
