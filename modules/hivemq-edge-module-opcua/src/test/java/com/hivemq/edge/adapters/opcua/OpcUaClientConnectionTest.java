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

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Result;
import com.hivemq.edge.adapters.opcua.client.Success;
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

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

/**
 * Test for OPC UA Client Connection keep-alive functionality.
 * Tests that the health check correctly monitors keep-alive messages from the OPC UA server.
 */
public class OpcUaClientConnectionTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @Nullable OpcUaClientConnection opcUaClientConnection;
    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull FakeEventService eventService;
    private @NotNull ProtocolAdapterMetricsService metricsService;
    private @NotNull AtomicInteger keepAliveCount;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
        metricsService = mock(ProtocolAdapterMetricsService.class);
        keepAliveCount = new AtomicInteger(0);
    }

    @AfterEach
    void tearDown() {
        if (opcUaClientConnection != null) {
            opcUaClientConnection.destroy();
        }
    }

    @Test
    @Timeout(60)
    void whenSubscriptionIsActive_thenKeepAliveMessagesAreReceived() throws Exception {
        // Arrange
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                // 1 second publishing interval
                null,
                null);

        // Create a tag that maps to a node in the test server
        final OpcuaTag tag = new OpcuaTag("testTag",
                "Test tag for keep-alive",
                new OpcuaTagDefinition("ns=" +
                        opcUaServerExtension.getTestNamespace().getNamespaceIndex() +
                        ";i=10")); // Int32 node from TestNamespace

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

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        final AtomicBoolean reconnectionCallbackInvoked = new AtomicBoolean(false);

        opcUaClientConnection = new OpcUaClientConnection("test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                streamingService,
                dataPointFactory,
                eventService,
                metricsService,
                config,
                () -> reconnectionCallbackInvoked.set(true));

        // Parse config
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        // Act - Start the connection
        final boolean started = opcUaClientConnection.start(parsedConfig);

        // Assert
        assertThat(started).as("Connection should start successfully").isTrue();

        // Wait for connection to be established
        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus()).as(
                "Connection should be established").isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Wait for subscription to be created and initial keep-alives to be received
        // Keep-alive should be sent every few publishing intervals (maxKeepAliveCount * publishingInterval)
        // With 1 second publishing interval, keep-alives should arrive within a few seconds
        await().atMost(java.time.Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(opcUaClientConnection.isHealthy()).as(
                    "Connection should be healthy after keep-alive messages are received").isTrue();
        });

        // Verify that isHealthy() continues to return true while keep-alives are being received
        Thread.sleep(5000); // Wait 5 more seconds to ensure keep-alives continue
        assertThat(opcUaClientConnection.isHealthy()).as("Connection should remain healthy").isTrue();

        // Verify reconnection callback was not invoked
        assertThat(reconnectionCallbackInvoked.get()).as(
                "Reconnection callback should not be invoked during normal operation").isFalse();
    }

    @Test
    @Timeout(60)
    void whenMultipleTagsSubscribed_thenKeepAliveMessagesAreReceived() throws Exception {
        // Arrange
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 2000),
                // 2 second publishing interval
                null,
                null);

        // Create multiple tags
        final List<OpcuaTag> tags = List.of(new OpcuaTag("tag1",
                        "Int32 Tag",
                        new OpcuaTagDefinition("ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10")),
                new OpcuaTag("tag2",
                        "Int64 Tag",
                        new OpcuaTagDefinition("ns=" +
                                opcUaServerExtension.getTestNamespace().getNamespaceIndex() +
                                ";i=12")),
                new OpcuaTag("tag3",
                        "Double Tag",
                        new OpcuaTagDefinition("ns=" +
                                opcUaServerExtension.getTestNamespace().getNamespaceIndex() +
                                ";i=13")));

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

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        opcUaClientConnection = new OpcUaClientConnection("test-adapter-id",
                tags,
                protocolAdapterState,
                streamingService,
                dataPointFactory,
                eventService,
                metricsService,
                config,
                () -> {});

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        // Act
        final boolean started = opcUaClientConnection.start(parsedConfig);

        // Assert
        assertThat(started).as("Connection should start successfully").isTrue();

        await().untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus()).as(
                    "Connection should be established").isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        });

        // Wait for keep-alives with multiple subscriptions
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(opcUaClientConnection.isHealthy()).as(
                    "Connection should be healthy with multiple tags subscribed").isTrue();
        });
    }

    @Test
    @Timeout(30)
    void whenNoSubscriptionCreated_thenIsHealthyReturnsFalse() {
        // Arrange - Use empty tag list so no subscription is created
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

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

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        opcUaClientConnection = new OpcUaClientConnection("test-adapter-id",
                List.of(),
                // Empty tags
                protocolAdapterState,
                streamingService,
                dataPointFactory,
                eventService,
                metricsService,
                config,
                () -> {});

        // Act
        final boolean healthy = opcUaClientConnection.isHealthy();

        // Assert
        assertThat(healthy).as("Connection should not be healthy when no connection exists").isFalse();
    }

    @Test
    @Timeout(30)
    void whenConnectionStopped_thenIsHealthyReturnsFalse() throws Exception {
        // Arrange
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        final OpcuaTag tag = new OpcuaTag("testTag",
                "Test tag",
                new OpcuaTagDefinition("ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10"));

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

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        opcUaClientConnection = new OpcUaClientConnection("test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                streamingService,
                dataPointFactory,
                eventService,
                metricsService,
                config,
                () -> {});

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        opcUaClientConnection.start(parsedConfig);

        await().untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus()).as(
                    "Connection should be established").isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        });

        // Act - Stop the connection
        opcUaClientConnection.stop();

        // Assert
        await().untilAsserted(() -> {
            assertThat(opcUaClientConnection.isHealthy()).as(
                    "Connection should not be healthy after being stopped").isFalse();
        });
    }
}
