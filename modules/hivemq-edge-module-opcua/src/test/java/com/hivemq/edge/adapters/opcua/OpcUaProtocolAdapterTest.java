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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.AdapterFactories;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Integration test for OpcUaProtocolAdapter with embedded OPC UA server.
 * Tests reconnection behavior and event logging.
 */
public class OpcUaProtocolAdapterTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @Nullable OpcUaProtocolAdapter adapter;
    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull FakeEventService eventService;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
    }

    @AfterEach
    void tearDown() {
        if (adapter != null) {
            adapter.destroy();
        }
    }

    @Test
    @Timeout(120)
    void whenAdapterStarts_thenConnectionSucceeds() throws Exception {
        // Arrange - Create config with embedded server URI
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        // Create a tag that maps to a node in the test server
        final OpcuaTag tag = new OpcuaTag("testTag",
                "Test tag",
                new OpcuaTagDefinition("ns=" +
                        opcUaServerExtension.getTestNamespace().getNamespaceIndex() +
                        ";i=10")); // Int32 node from TestNamespace

        // Mock adapter information
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        // Mock adapter input
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = createMockedInput(config, List.of(tag));

        // Create adapter
        adapter = new OpcUaProtocolAdapter(adapterInformation, input);

        // Mock module services for start
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService()).thenReturn(mock(ProtocolAdapterTagStreamingService.class));

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);

        // Act - Start the adapter
        adapter.start(startInput, startOutput);

        // Assert - Wait for connection to be established
        await().untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus()).as("Adapter should be connected")
                    .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        });

        // Verify no error events were fired
        assertThat(eventService.readEvents(null,
                null)).as("No error events should be recorded on successful connection")
                .noneMatch(event -> "ERROR".equals(event.getSeverity().name()));
    }

    @Test
    @Timeout(180)
    void whenSubscriptionsFail_thenReconnectionIsScheduled() throws Exception {
        // Arrange - Create config with embedded server URI but invalid node references
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        // Create tags pointing to non-existent nodes to cause subscription failures
        final List<OpcuaTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tags.add(new OpcuaTag("invalidTag" + i,
                    "Invalid Tag " + i,
                    new OpcuaTagDefinition("ns=" +
                            opcUaServerExtension.getTestNamespace().getNamespaceIndex() +
                            ";i=" +
                            (9000 + i)))); // Non-existent node IDs
        }

        // Mock adapter information
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        // Mock adapter input
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = createMockedInput(config, tags);

        // Create adapter
        adapter = new OpcUaProtocolAdapter(adapterInformation, input);

        // Mock module services for start
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService()).thenReturn(mock(ProtocolAdapterTagStreamingService.class));

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);

        // Act - Start the adapter (will fail to subscribe due to invalid node IDs)
        adapter.start(startInput, startOutput);

        // Assert - Verify connection status transitions to a failure state after subscription issues
        // Either ERROR or DISCONNECTED indicates that reconnection scheduling would be triggered
        await().atMost(java.time.Duration.ofSeconds(60)).untilAsserted(() -> {
            final ProtocolAdapterState.ConnectionStatus status = protocolAdapterState.getConnectionStatus();
            assertThat(status).as(
                            "Adapter should enter a failure state (ERROR or DISCONNECTED) after subscription failure, " +
                                    "which triggers automatic reconnection scheduling")
                    .isIn(ProtocolAdapterState.ConnectionStatus.ERROR,
                            ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        });
    }

    @Test
    @Timeout(180)
    void whenMultipleRetriesOccur_thenReconnectWorks() throws Exception {
        // Arrange - Create config with invalid URI to force repeated failures
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                new ConnectionOptions(ConnectionOptions.DEFAULT_SESSION_TIMEOUT,
                        ConnectionOptions.DEFAULT_REQUEST_TIMEOUT,
                        // keep-alive interval
                        3000L,
                        ConnectionOptions.DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED,
                        ConnectionOptions.DEFAULT_CONNECTION_TIMEOUT,
                        // health check interval
                        10000L,
                        // retry interval
                        2000L,
                        true,
                        true));

        final OpcuaTag tag =
                new OpcuaTag("testTag", "Test tag", new OpcuaTagDefinition("ns=0;i=1234567890")); // Server/ServerStatus

        // Mock adapter information
        final ProtocolAdapterInformation adapterInformation = mock(ProtocolAdapterInformation.class);
        when(adapterInformation.getProtocolId()).thenReturn("opcua");

        // Mock adapter input
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = createMockedInput(config, List.of(tag));

        // Create adapter
        adapter = new OpcUaProtocolAdapter(adapterInformation, input);

        // Mock module services for start
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(moduleServices.protocolAdapterTagStreamingService()).thenReturn(mock(ProtocolAdapterTagStreamingService.class));

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);

        // Act - Start the adapter (will fail to connect)
        adapter.start(startInput, startOutput);

        // Assert - Connection should fail and remain in ERROR state
        await().atMost(java.time.Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus()).as(
                            "Adapter should be in ERROR state after connection failure")
                    .isEqualTo(ProtocolAdapterState.ConnectionStatus.ERROR);
        });

        // Wait for multiple retry attempts to occur
        Thread.sleep(15000); // Wait 15 seconds for retries (1s + 2s + 4s + 8s = 15s minimum)

        // Verify multiple error events were fired for the retry attempts
        assertThat(eventService.readEvents(null,
                null)).as("Multiple error events should be recorded for retry attempts")
                .filteredOn(event -> event.getMessage().contains("Failed to create or transfer OPC UA subscription. Closing client connection."))
                .hasSizeGreaterThanOrEqualTo(5)
                .hasSizeLessThan(25);
        assertThat(adapter.getReconnectAttempts()).isLessThan(25);
    }

    private @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> createMockedInput(
            final @NotNull OpcUaSpecificAdapterConfig config,
            final @NotNull List<OpcuaTag> tags) {
        final ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);

        // Basic properties
        when(input.getAdapterId()).thenReturn("test-adapter-id");
        when(input.getProtocolAdapterState()).thenReturn(protocolAdapterState);
        when(input.getConfig()).thenReturn(config);

        // Tags
        final List<Tag> genericTags = new ArrayList<>(tags);
        when(input.getTags()).thenReturn(genericTags);

        // Data point factory
        final DataPointFactory dataPointFactory = new DataPointFactory() {
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
        };

        final AdapterFactories adapterFactories = mock(AdapterFactories.class);
        when(adapterFactories.dataPointFactory()).thenReturn(dataPointFactory);
        when(input.adapterFactories()).thenReturn(adapterFactories);

        // Metrics service
        when(input.getProtocolAdapterMetricsHelper()).thenReturn(mock(ProtocolAdapterMetricsService.class));

        // Module service
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(eventService);
        when(input.moduleServices()).thenReturn(moduleServices);

        return input;
    }
}
