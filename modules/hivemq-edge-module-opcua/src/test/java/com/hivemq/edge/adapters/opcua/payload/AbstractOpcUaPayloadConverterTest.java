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
package com.hivemq.edge.adapters.opcua.payload;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.hivemq.edge.adapters.opcua.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.OpcUaAdapterConfig.PayloadMode;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation;
import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartInput;
import com.hivemq.edge.modules.adapters.model.ProtocolAdapterStartOutput;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("NullabilityAnnotations")
abstract class AbstractOpcUaPayloadConverterTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private ModuleServices moduleServices;
    private ProtocolAdapterPublishService adapterPublishService;
    private TestProtocolAdapterPublishBuilder adapterPublishBuilder;

    @BeforeEach
    public void before() {
        setupMocks();
    }

    @NotNull
    protected OpcUaProtocolAdapter createAndStartAdapter(
            final @NotNull String subcribedNodeId, final PayloadMode payloadMode)
            throws InterruptedException, ExecutionException {
        final OpcUaAdapterConfig config =
                new OpcUaAdapterConfig("test-" + UUID.randomUUID(), opcUaServerExtension.getServerUri());
        config.setSubscriptions(List.of(new OpcUaAdapterConfig.Subscription(subcribedNodeId, "topic")));
        final OpcUaProtocolAdapter protocolAdapter = new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, config, new MetricRegistry());

        final ProtocolAdapterStartInput in = () -> moduleServices;
        final ProtocolAdapterStartOutput out = mock(ProtocolAdapterStartOutput.class);
        protocolAdapter.start(in, out).get();
        return protocolAdapter;
    }

    protected void setupMocks() {
        moduleServices = mock(ModuleServices.class);
        adapterPublishService = mock(ProtocolAdapterPublishService.class);
        when(moduleServices.adapterPublishService()).thenReturn(adapterPublishService);
        adapterPublishBuilder = new TestProtocolAdapterPublishBuilder();
        when(adapterPublishService.publish()).thenReturn(adapterPublishBuilder);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
    }

    protected @NotNull PUBLISH expectAdapterPublish() {
        Awaitility.await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .timeout(Duration.ofSeconds(5))
                .until(() -> adapterPublishBuilder.getPublishes().size() > 0);
        return adapterPublishBuilder.getPublishes().get(0);
    }

    private static class TestProtocolAdapterPublishBuilder implements ProtocolAdapterPublishBuilder {

        private final @NotNull PUBLISHFactory.Mqtt5Builder builder = new PUBLISHFactory.Mqtt5Builder();
        private final @NotNull ImmutableList.Builder<MqttUserProperty> userProperties = ImmutableList.builder();
        private final @NotNull List<PUBLISH> publishes = new ArrayList<>();

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withTopic(@NotNull final String mqttTopic) {
            builder.withTopic(mqttTopic);
            return this;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withPayload(final @NotNull byte[] payload) {
            builder.withPayload(payload);
            return this;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withQoS(final int qos) {
            builder.withQoS(requireNonNull(QoS.valueOf(qos)));
            return this;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withMessageExpiryInterval(final long messageExpiryInterval) {
            builder.withMessageExpiryInterval(messageExpiryInterval);
            return this;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withUserProperty(
                final @NotNull String name, final @NotNull String value) {
            userProperties.add(new MqttUserProperty(name, value));
            return this;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withRetain(final boolean retained) {
            builder.withRetain(retained);
            return this;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withContextInformation(
                @NotNull final String key,
                @NotNull final String value) {
            return this;
        }

        @Override
        public @NotNull CompletableFuture<PublishReturnCode> send() {

            publishes.add(builder.withHivemqId("hivemqId")
                    .withUserProperties(Mqtt5UserProperties.of(userProperties.build()))
                    .build());

            return CompletableFuture.completedFuture(PublishReturnCode.DELIVERED);
        }

        public @NotNull List<PUBLISH> getPublishes() {
            return publishes;
        }
    }
}
