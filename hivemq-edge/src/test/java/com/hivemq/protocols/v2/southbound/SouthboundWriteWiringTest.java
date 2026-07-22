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
package com.hivemq.protocols.v2.southbound;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.schema.ScalarSchema;
import com.hivemq.adapter.sdk.api.schema.ScalarType;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.v2.node.Node;
import com.hivemq.adapter.sdk.api.v2.node.NodeProperty;
import com.hivemq.adapter.sdk.api.v2.node.NodeTagPair;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.modules.adapters.metrics.ProtocolAdapterMetricsServiceImpl;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.InternalWritingContext;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import com.hivemq.protocols.v2.manager.ProtocolAdapterManagerTestSupport;
import com.hivemq.protocols.v2.northbound.ProtocolAdapterPublishIdentity;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperMessage;
import com.hivemq.protocols.v2.wrapper.ProtocolAdapterWrapperWriteRequest;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The v2 southbound write path has a producer (EDG-824 #3): the registry subscribes the
 * southbound mappings through the reused writing service, and the bridge forwards each arriving MQTT write into the
 * wrapper mailbox as a {@link ProtocolAdapterWrapperWriteRequest} — from where the (already-tested) write aspect
 * batches it into the adapter's {@code writeBatch}.
 */
class SouthboundWriteWiringTest {

    private static final @NotNull String ADAPTER_ID = "pump-1";

    // ── the bridge: MQTT write in, wrapper mailbox write request out ───────────────────────

    @Test
    void write_forKnownTag_tellsAWriteRequestToTheWrapperMailboxAndAcks() {
        final TestNode node = new TestNode("setpoint");
        final RecordingSender sender = new RecordingSender();
        final SouthboundWriteBridge bridge = bridge(sender, Map.of("setpoint", node));
        final RecordingWritingOutput output = new RecordingWritingOutput();

        bridge.write(input("setpoint", new V2WritePayload(new DoubleNode(21.5))), output);

        assertThat(sender.told).hasSize(1);
        final ProtocolAdapterWrapperWriteRequest request = (ProtocolAdapterWrapperWriteRequest) sender.told.get(0);
        assertThat(request.node()).isSameAs(node);
        assertThat(request.value().getTagName()).isEqualTo("setpoint");
        assertThat(output.finished).isTrue();
        assertThat(output.failure).isNull();
    }

    @Test
    void write_forUnknownTag_failsTheOutputAndTellsNothing() {
        final RecordingSender sender = new RecordingSender();
        final SouthboundWriteBridge bridge = bridge(sender, Map.of());
        final RecordingWritingOutput output = new RecordingWritingOutput();

        bridge.write(input("ghost", new V2WritePayload(new DoubleNode(1))), output);

        assertThat(sender.told).isEmpty();
        assertThat(output.finished).isFalse();
        assertThat(output.failure).contains("ghost");
    }

    @Test
    void payloadClass_isTheConventionalValueShape() {
        assertThat(bridge(new RecordingSender(), Map.of()).getMqttPayloadClass())
                .isEqualTo(V2WritePayload.class);
    }

    // ── the registry: lifecycle against the reused writing service ─────────────────────────

    @Test
    void mappings_areStartedOnTheWritingServiceAndStoppedOnClose() {
        final RecordingWritingService writingService = new RecordingWritingService();

        final SouthboundWriterRegistry registry =
                registry(writingService, List.of(new SouthboundMappingEntity("plant/pump-1/setpoint", "setpoint")));

        assertThat(writingService.startedContexts).hasSize(1);
        assertThat(writingService.startedContexts.get(0).getTopicFilter()).isEqualTo("plant/pump-1/setpoint");
        assertThat(writingService.startedContexts.get(0).getTagName()).isEqualTo("setpoint");
        assertThat(writingService.startedAdapter).isNotNull();
        assertThat(writingService.startedAdapter.getId()).isEqualTo(ADAPTER_ID);

        registry.close();
        assertThat(writingService.stopCalls).isEqualTo(1);
    }

    @Test
    void noMappings_startNothing() {
        final RecordingWritingService writingService = new RecordingWritingService();

        final SouthboundWriterRegistry registry = registry(writingService, List.of());

        assertThat(writingService.startedContexts).isEmpty();
        registry.close();
        assertThat(writingService.stopCalls).isZero();
    }

    @Test
    void updateMappings_restartsTheWiringWithTheNewSet() {
        final RecordingWritingService writingService = new RecordingWritingService();
        final SouthboundWriterRegistry registry =
                registry(writingService, List.of(new SouthboundMappingEntity("plant/pump-1/setpoint", "setpoint")));

        registry.updateMappings(
                List.of(new SouthboundMappingEntity("plant/pump-1/mode", "setpoint")),
                List.of(pair(new TestNode("setpoint"))));

        assertThat(writingService.stopCalls).isEqualTo(1);
        assertThat(writingService.startedContexts).hasSize(1);
        assertThat(writingService.startedContexts.get(0).getTopicFilter()).isEqualTo("plant/pump-1/mode");
    }

    // ── fixtures ───────────────────────────────────────────────────────────────────────────

    private static @NotNull SouthboundWriteBridge bridge(
            final @NotNull RecordingSender sender, final @NotNull Map<String, Node> nodes) {
        return new SouthboundWriteBridge(identity(), sender, new TestDataPointFactory(), nodes);
    }

    private static @NotNull SouthboundWriterRegistry registry(
            final @NotNull InternalProtocolAdapterWritingService writingService,
            final @NotNull List<SouthboundMappingEntity> mappings) {
        return new SouthboundWriterRegistry(
                ADAPTER_ID,
                new ProtocolAdapterManagerTestSupport.TestProtocolAdapterInformation("test"),
                writingService,
                metricsService(),
                new RecordingSender(),
                new TestDataPointFactory(),
                List.of(pair(new TestNode("setpoint"))),
                mappings);
    }

    private static @NotNull ProtocolAdapterPublishIdentity identity() {
        return new ProtocolAdapterPublishIdentity(
                ADAPTER_ID, new ProtocolAdapterManagerTestSupport.TestProtocolAdapterInformation("test"));
    }

    private static @NotNull ProtocolAdapterMetricsService metricsService() {
        return new ProtocolAdapterMetricsServiceImpl("test", ADAPTER_ID, new MetricRegistry());
    }

    private static @NotNull NodeTagPair pair(final @NotNull TestNode node) {
        return NodeTagPair.create(
                node,
                node.nodeId(),
                new ScalarSchema(ScalarType.STRING, null, null, null, null, false, true, false),
                true,
                false);
    }

    private static @NotNull WritingInput input(final @NotNull String tagName, final @NotNull V2WritePayload payload) {
        return new WritingInput() {
            @Override
            public @NotNull WritingPayload getWritingPayload() {
                return payload;
            }

            @Override
            public @NotNull WritingContext getWritingContext() {
                return new WritingContext() {
                    @Override
                    public @NotNull String getTagName() {
                        return tagName;
                    }

                    @Override
                    public @NotNull String getTopicFilter() {
                        return "plant/" + tagName;
                    }
                };
            }
        };
    }

    private static final class TestNode extends Node {

        private final @NotNull String identifier;

        TestNode(final @NotNull String identifier) {
            this.identifier = identifier;
        }

        @Override
        public @NotNull String nodeId() {
            return identifier;
        }

        @Override
        public @NotNull String nodeString() {
            return "{\"identifier\":\"" + identifier + "\"}";
        }

        @Override
        public @NotNull EnumSet<NodeProperty> properties() {
            return EnumSet.of(NodeProperty.UNIQUE);
        }
    }

    private record TestDataPoint(
            @NotNull String tagName, @NotNull Object value, boolean json) implements DataPoint {
        @Override
        public @NotNull Object getTagValue() {
            return value;
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

    private static final class TestDataPointFactory implements DataPointFactory {
        @Override
        public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new TestDataPoint(tagName, tagValue, false);
        }

        @Override
        public @NotNull DataPoint createJsonDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
            return new TestDataPoint(tagName, tagValue, true);
        }
    }

    private static final class RecordingSender
            implements com.hivemq.adapter.sdk.api.v2.messaging.MailboxSender<ProtocolAdapterWrapperMessage> {

        private final @NotNull List<ProtocolAdapterWrapperMessage> told = new ArrayList<>();

        @Override
        public void tell(final @NotNull ProtocolAdapterWrapperMessage message) {
            told.add(message);
        }
    }

    private static final class RecordingWritingOutput implements WritingOutput {

        private boolean finished;
        private @Nullable String failure;

        @Override
        public void finish() {
            finished = true;
        }

        @Override
        public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
            failure = String.valueOf(errorMessage);
        }

        @Override
        public void fail(final @NotNull String errorMessage) {
            failure = errorMessage;
        }
    }

    private static final class RecordingWritingService implements InternalProtocolAdapterWritingService {

        private @Nullable WritingProtocolAdapter startedAdapter;
        private @NotNull List<InternalWritingContext> startedContexts = List.of();
        private int stopCalls;

        @Override
        public boolean writingEnabled() {
            return true;
        }

        @Override
        public @NotNull CompletableFuture<Boolean> startWritingAsync(
                final @NotNull WritingProtocolAdapter writingProtocolAdapter,
                final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
                final @NotNull List<InternalWritingContext> writingContexts) {
            this.startedAdapter = writingProtocolAdapter;
            this.startedContexts = List.copyOf(writingContexts);
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public void stopWriting(
                final @NotNull WritingProtocolAdapter writingProtocolAdapter,
                final @NotNull List<InternalWritingContext> writingContexts) {
            stopCalls++;
        }

        @Override
        public void addWritingChangedCallback(final @NotNull WritingChangedCallback callback) {}
    }
}
