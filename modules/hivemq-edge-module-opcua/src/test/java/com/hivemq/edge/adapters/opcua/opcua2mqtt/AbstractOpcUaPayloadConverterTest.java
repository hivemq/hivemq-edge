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

import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapter;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterInformation;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import com.hivemq.edge.modules.adapters.impl.factories.AdapterFactoriesImpl;
import com.hivemq.edge.modules.api.events.model.EventBuilderImpl;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.mappings.NorthboundMapping;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("NullabilityAnnotations")
abstract class AbstractOpcUaPayloadConverterTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private final @NotNull ModuleServices moduleServices = mock();
    private final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> protocolAdapterInput = mock();
    private final @NotNull AdapterFactories adapterFactories = mock();
    private final @NotNull EventService eventService = mock();
    private final @NotNull Map<String, List<DataPoint>> receivedDataPoints = new ConcurrentHashMap<>();

    @BeforeEach
    public void before() {
        receivedDataPoints.clear();
        when(protocolAdapterInput.getProtocolAdapterState()).thenReturn(new ProtocolAdapterStateImpl(mock(),
                "id",
                "protocolId"));
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
        when(protocolAdapterInput.adapterFactories()).thenReturn(adapterFactories);
        when(protocolAdapterInput.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(eventService.createAdapterEvent(any(), any())).thenReturn(new EventBuilderImpl(event -> {}));
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService()).thenReturn(new ProtocolAdapterTagStreamingService() {
            @Override
            public void feed(final String tag, final List<DataPoint> dataPoints) {
                receivedDataPoints.put(tag, dataPoints);
            }
        });
        final AdapterFactories adapterFactories = mock(AdapterFactoriesImpl.class);
        when(adapterFactories.dataPointFactory()).thenReturn(new DataPointFactory() {
            @Override
            public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue);
            }

            @Override
            public @NotNull DataPoint createJsonDataPoint(
                    final @NotNull String tagName,
                    final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue, true);
            }
        });
        when(protocolAdapterInput.adapterFactories()).thenReturn(adapterFactories);
    }

    @NotNull
    protected OpcUaProtocolAdapter createAndStartAdapter(final @NotNull String subcribedNodeId)
            throws Exception {

        final OpcUaToMqttConfig opcuaToMqttConfig =
                new OpcUaToMqttConfig(null, null);
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
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
        final CompletableFuture<Void> startFuture = new CompletableFuture<>();
        final ProtocolAdapterStartOutput out = new ProtocolAdapterStartOutput() {
            @Override
            public void startedSuccessfully() {
                startFuture.complete(null);
            }

            @Override
            public void failStart(final @NotNull Throwable t, final @Nullable String errorMessage) {
                startFuture.completeExceptionally(t);
            }
        };
        protocolAdapter.start(in, out);
        startFuture.get();
        return protocolAdapter;
    }

    protected @NotNull Map<String, List<DataPoint>> expectAdapterPublish() {
        Awaitility.await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .timeout(Duration.ofSeconds(5))
                .until(() -> !receivedDataPoints.isEmpty());
        return receivedDataPoints;
    }

}
