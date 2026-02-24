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
package com.hivemq.edge.adapters.opcua.northbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
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
import com.hivemq.protocols.ProtocolAdapterStopOutputImpl;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

class OpcUaToJsonConverterMetadataIntegrationTest {

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
        when(protocolAdapterInput.getProtocolAdapterState())
                .thenReturn(new ProtocolAdapterStateImpl(mock(), "id", "protocolId"));
        when(protocolAdapterInput.moduleServices()).thenReturn(moduleServices);
        when(protocolAdapterInput.adapterFactories()).thenReturn(adapterFactories);
        when(protocolAdapterInput.getProtocolAdapterMetricsHelper())
                .thenReturn(mock(ProtocolAdapterMetricsService.class));
        when(eventService.createAdapterEvent(any(), any())).thenReturn(new EventBuilderImpl(event -> {}));
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService()).thenReturn(receivedDataPoints::put);
        final AdapterFactories adapterFactories = mock(AdapterFactoriesImpl.class);
        when(adapterFactories.dataPointFactory()).thenReturn(new DataPointFactory() {
            @Override
            public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue);
            }

            @Override
            public @NotNull DataPoint createJsonDataPoint(
                    final @NotNull String tagName, final @NotNull Object tagValue) {
                return new DataPointImpl(tagName, tagValue, true);
            }
        });
        when(protocolAdapterInput.adapterFactories()).thenReturn(adapterFactories);
    }

    @Test
    @Timeout(10)
    void whenIncludeMetadataEnabled_thenReceivedDataContainsTimestamps() throws Exception {
        final String nodeId =
                opcUaServerExtension.getTestNamespace().addNode("TestIntNode", NodeIds.Int32, () -> 42, 999);

        final OpcUaProtocolAdapter protocolAdapter = createAndStartAdapterWithMetadata(nodeId, true);

        await().until(() -> ProtocolAdapterState.ConnectionStatus.CONNECTED.equals(
                protocolAdapter.getProtocolAdapterState().getConnectionStatus()));

        final var received = expectAdapterPublish();
        protocolAdapter.stop(new ProtocolAdapterStopInput() {}, new ProtocolAdapterStopOutputImpl());

        assertThat(received).containsKey(nodeId);
        final List<DataPoint> dataPoints = received.get(nodeId);
        assertThat(dataPoints).hasSize(1);

        final String jsonValue = (String) dataPoints.getFirst().getTagValue();
        assertThat(jsonValue).contains("\"value\":");
        assertThat(jsonValue).contains("\"sourceTime\":");
        assertThat(jsonValue).contains("\"serverTime\":");
    }

    @Test
    @Timeout(10)
    void whenIncludeMetadataDisabled_thenReceivedDataDoesNotContainTimestamps() throws Exception {
        final String nodeId =
                opcUaServerExtension.getTestNamespace().addNode("TestIntNode2", NodeIds.Int32, () -> 42, 999);

        final OpcUaProtocolAdapter protocolAdapter = createAndStartAdapterWithMetadata(nodeId, false);

        await().until(() -> ProtocolAdapterState.ConnectionStatus.CONNECTED.equals(
                protocolAdapter.getProtocolAdapterState().getConnectionStatus()));

        final var received = expectAdapterPublish();
        protocolAdapter.stop(new ProtocolAdapterStopInput() {}, new ProtocolAdapterStopOutputImpl());

        assertThat(received).containsKey(nodeId);
        final List<DataPoint> dataPoints = received.get(nodeId);
        assertThat(dataPoints).hasSize(1);

        final String jsonValue = (String) dataPoints.getFirst().getTagValue();
        assertThat(jsonValue).contains("\"value\":");
        assertThat(jsonValue).doesNotContain("\"sourceTimestamp\":");
        assertThat(jsonValue).doesNotContain("\"serverTimestamp\":");
    }

    @NotNull
    private OpcUaProtocolAdapter createAndStartAdapterWithMetadata(
            final @NotNull String subscribedNodeId, final boolean includeMetadata) throws Exception {

        final OpcUaToMqttConfig opcuaToMqttConfig = new OpcUaToMqttConfig(1, 1000);
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                opcuaToMqttConfig,
                null,
                null,
                includeMetadata);

        when(protocolAdapterInput.getConfig()).thenReturn(config);
        when(protocolAdapterInput.getTags())
                .thenReturn(List.of(new OpcuaTag(subscribedNodeId, "", new OpcuaTagDefinition(subscribedNodeId))));

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

    private @NotNull Map<String, List<DataPoint>> expectAdapterPublish() {
        Awaitility.await()
                .pollInterval(10, TimeUnit.MILLISECONDS)
                .timeout(Duration.ofSeconds(5))
                .until(() -> !receivedDataPoints.isEmpty());
        return receivedDataPoints;
    }
}
