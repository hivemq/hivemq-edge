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
package com.hivemq.edge.adapters.opcua.opcua2mqtt;

import com.google.common.collect.ImmutableList;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.ProtocolPublishResult;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttMapping;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("NullabilityAnnotations")
abstract class AbstractOpcUaPayloadConverterTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ModuleServices moduleServices = mock();
    private final @NotNull ProtocolAdapterPublishService adapterPublishService = mock();
    private final @NotNull TestProtocolAdapterPublishBuilder adapterPublishBuilder =
            new TestProtocolAdapterPublishBuilder();
    private final @NotNull ProtocolAdapterInput<OpcUaAdapterConfig> protocolAdapterInput = mock();
    private final @NotNull AdapterFactories adapterFactories = mock();
    private final @NotNull EventService eventService = mock();
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService = mock();


    @BeforeEach
    public void before() {
        when(protocolAdapterInput.getProtocolAdapterState()).thenReturn(new ProtocolAdapterStateImpl(mock(),
                "id",
                "protocolId"));
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
        when(protocolAdapterInput.adapterFactories()).thenReturn(adapterFactories);
        when(protocolAdapterInput.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(adapterPublishService.createPublish()).thenReturn(adapterPublishBuilder);
        when(moduleServices.adapterPublishService()).thenReturn(adapterPublishService);
        when(eventService.createAdapterEvent(any(), any())).thenReturn(new EventBuilderImpl(event -> {}));
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagService()).thenReturn(protocolAdapterTagService);
    }

    @NotNull
    protected OpcUaProtocolAdapter createAndStartAdapter(final @NotNull String subcribedNodeId)
            throws Exception {

        final OpcUaToMqttConfig opcuaToMqttConfig =
                new OpcUaToMqttConfig(List.of(new OpcUaToMqttMapping(subcribedNodeId,
                        "topic",
                        null,
                        null,
                        null,
                        null)));
        final OpcUaAdapterConfig config = new OpcUaAdapterConfig("test-" + UUID.randomUUID(),
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                opcuaToMqttConfig,
                null);

        when(protocolAdapterInput.getConfig()).thenReturn(config);
        when(protocolAdapterInput.getTags()).thenReturn(List.of(new OpcuaTag(subcribedNodeId, "", new OpcuaTagDefinition(subcribedNodeId))));
        final OpcUaProtocolAdapter protocolAdapter =
                new OpcUaProtocolAdapter(OpcUaProtocolAdapterInformation.INSTANCE, protocolAdapterInput);

        final ProtocolAdapterStartInput in = () -> moduleServices;
        CompletableFuture<Void> startFuture = new CompletableFuture<>();
        final ProtocolAdapterStartOutput out = new ProtocolAdapterStartOutput() {
            @Override
            public void startedSuccessfully() {
                startFuture.complete(null);
            }

            @Override
            public void failStart(@NotNull final Throwable t, @Nullable final String errorMessage) {
                startFuture.completeExceptionally(t);
            }
        };
        protocolAdapter.start(in, out);
        startFuture.get();
        return protocolAdapter;
    }

    protected @NotNull PUBLISH expectAdapterPublish() {
        Awaitility.await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .timeout(Duration.ofSeconds(5))
                .until(() -> !adapterPublishBuilder.getPublishes().isEmpty());
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
        public @NotNull ProtocolAdapterPublishBuilder withAdapter(@NotNull final ProtocolAdapter adapter) {
            return this;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder withContextInformation(
                @NotNull final String key, @NotNull final String value) {
            return this;
        }

        @Override
        public @NotNull CompletableFuture<ProtocolPublishResult> send() {

            publishes.add(builder.withHivemqId("hivemqId")
                    .withUserProperties(Mqtt5UserProperties.of(userProperties.build()))
                    .build());

            return CompletableFuture.completedFuture(ProtocolPublishResult.DELIVERED);
        }


        public @NotNull List<PUBLISH> getPublishes() {
            return publishes;
        }
    }
}
