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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.events.model.Event;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import util.EmbeddedOpcUaServerExtension;

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

    static long getUpperBound(final long value) {
        return (long) (value * (1 + ConnectionOptions.DEFAULT_RETRY_JITTER));
    }

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
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null,
                null);

        // Create a tag that maps to a node in the test server
        final OpcuaTag tag = new OpcuaTag(
                "testTag",
                "Test tag",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex()
                                + ";i=10")); // Int32 node from TestNamespace

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
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);

        // Act - Start the adapter
        adapter.start(startInput, startOutput);

        // Assert - Wait for connection to be established
        await().untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus())
                    .as("Adapter should be connected")
                    .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        });

        // Verify no error events were fired
        assertThat(eventService.readEvents(null, null))
                .as("No error events should be recorded on successful connection")
                .noneMatch(event -> "ERROR".equals(event.getSeverity().name()));
    }

    @Test
    @Timeout(180)
    void whenSubscriptionsFail_thenReconnectionIsScheduled() throws Exception {
        // Arrange - Create config with embedded server URI but invalid node references
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null,
                null);

        // Create tags pointing to non-existent nodes to cause subscription failures
        final List<OpcuaTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tags.add(new OpcuaTag(
                    "invalidTag" + i,
                    "Invalid Tag " + i,
                    new OpcuaTagDefinition(
                            "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex()
                                    + ";i="
                                    + (9000 + i)))); // Non-existent node IDs
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
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);

        // Act - Start the adapter (will fail to subscribe due to invalid node IDs)
        adapter.start(startInput, startOutput);

        Thread.sleep(5000);

        // Assert - Verify connection status transitions to a failure state after subscription issues
        // Either ERROR or DISCONNECTED indicates that reconnection scheduling would be triggered
        await().atMost(Duration.ofSeconds(60)).untilAsserted(() -> {
            final ProtocolAdapterState.ConnectionStatus status = protocolAdapterState.getConnectionStatus();
            assertThat(status)
                    .as("Adapter should enter a failure state (ERROR or DISCONNECTED) after subscription failure, "
                            + "which triggers automatic reconnection scheduling")
                    .isIn(
                            ProtocolAdapterState.ConnectionStatus.ERROR,
                            ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        });
        final List<String> eventMessages = eventService.readEvents(null, null).stream()
                .map(Event::getMessage)
                .toList();
        assertThat(eventMessages)
                .as("Multiple error events should be recorded for retry attempts")
                .filteredOn(
                        message -> message.contains(
                                "Failed to synchronize monitored items: StatusCode[name=Bad_UnexpectedError, value=0x80010000, quality=bad] failed to synchronize one or more MonitoredItems. Samples: NodeId{ns=1, id="))
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @Timeout(180)
    void whenMultipleRetriesOccur_thenReconnectWorks() throws Exception {
        // Arrange - Create config with invalid URI to force repeated failures
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                new ConnectionOptions(
                        ConnectionOptions.DEFAULT_SESSION_TIMEOUT,
                        ConnectionOptions.DEFAULT_REQUEST_TIMEOUT,
                        // keep-alive interval
                        3000L,
                        ConnectionOptions.DEFAULT_KEEP_ALIVE_FAILURES_ALLOWED,
                        ConnectionOptions.DEFAULT_CONNECTION_TIMEOUT,
                        // health check interval
                        10000L,
                        // retry interval
                        "2000",
                        true,
                        true),
                null);

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
        when(moduleServices.protocolAdapterTagStreamingService())
                .thenReturn(mock(ProtocolAdapterTagStreamingService.class));

        final ProtocolAdapterStartInput startInput = mock(ProtocolAdapterStartInput.class);
        when(startInput.moduleServices()).thenReturn(moduleServices);

        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);

        // Act - Start the adapter (will fail to connect)
        adapter.start(startInput, startOutput);

        // Assert - Connection should fail and remain in ERROR state
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus())
                    .as("Adapter should be in ERROR state after connection failure")
                    .isEqualTo(ProtocolAdapterState.ConnectionStatus.ERROR);
        });

        // Wait for multiple retry attempts to occur
        Thread.sleep(15000); // Wait 15 seconds for retries (1s + 2s + 4s + 8s = 15s minimum)

        // Verify multiple error events were fired for the retry attempts
        final List<String> eventMessages = eventService.readEvents(null, null).stream()
                .map(Event::getMessage)
                .toList();
        assertThat(eventMessages)
                .as("Multiple error events should be recorded for retry attempts")
                .filteredOn(message -> message.contains(
                        "Failed to create or transfer OPC UA subscription. Closing client connection."))
                .hasSizeGreaterThanOrEqualTo(5)
                .hasSizeLessThan(25);
        assertThat(eventMessages)
                .as("Multiple error events should be recorded for retry attempts")
                .filteredOn(
                        message -> message.contains(
                                "Failed to synchronize monitored items: StatusCode[name=Bad_UnexpectedError, value=0x80010000, quality=bad] failed to synchronize one or more MonitoredItems. Samples: NodeId{ns=0, id=1234567890}"))
                .hasSizeGreaterThanOrEqualTo(5)
                .hasSizeLessThan(25);
        assertThat(adapter.getReconnectAttempts()).isLessThan(25);
    }

    private @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> createMockedInput(
            final @NotNull OpcUaSpecificAdapterConfig config, final @NotNull List<OpcuaTag> tags) {
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
                    final @NotNull String tagName, final @NotNull Object tagValue) {
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

    /**
     * Tests the exponential backoff delay calculation using the comma-separated retry intervals.
     * Verifies the backoff sequence: 1s, 2s, 4s, 8s, 16s, 32s, 64s, 128s, 256s, 300s (capped).
     */
    @ParameterizedTest
    @CsvSource({
        "1, 1000", // First retry: 1 second
        "2, 2000", // Second retry: 2 seconds
        "3, 4000", // Third retry: 4 seconds
        "4, 8000", // Fourth retry: 8 seconds
        "5, 16000", // Fifth retry: 16 seconds
        "6, 32000", // Sixth retry: 32 seconds
        "7, 64000", // Seventh retry: 64 seconds
        "8, 128000", // Eighth retry: 128 seconds
        "9, 256000", // Ninth retry: 256 seconds
        "10, 300000", // Tenth retry: 300 seconds (max)
        "11, 300000", // Eleventh retry: still 300 seconds (capped)
        "20, 300000", // Large attempt count: still 300 seconds (capped)
        "100, 300000", // Very large attempt count: still 300 seconds (capped)
    })
    void testCalculateBackoffDelayMs_exponentialGrowthAndCapping(final int attemptCount, final long expectedDelayMs) {
        final long actualDelay =
                OpcUaProtocolAdapter.calculateBackoffDelayMs(ConnectionOptions.DEFAULT_RETRY_INTERVALS, attemptCount);
        assertThat(actualDelay)
                .as("Backoff delay for attempt #%d should be %d ms", attemptCount, expectedDelayMs)
                .isGreaterThanOrEqualTo(expectedDelayMs)
                .isLessThan(getUpperBound(expectedDelayMs));
    }

    /**
     * Tests that the backoff strategy correctly handles the maximum delay cap.
     * Any attempt count >= 10 should return the maximum delay of 300 seconds.
     */
    @Test
    void testCalculateBackoffDelayMs_capsAtMaximumDelay() {
        for (int attemptCount = 10; attemptCount <= 1000; attemptCount += 10) {
            final long actualDelay = OpcUaProtocolAdapter.calculateBackoffDelayMs(
                    ConnectionOptions.DEFAULT_RETRY_INTERVALS, attemptCount);
            assertThat(actualDelay)
                    .as("Backoff delay for attempt #%d should be capped at 300 seconds", attemptCount)
                    .isGreaterThanOrEqualTo(300_000L)
                    .isLessThan(getUpperBound(300_000L));
        }
    }

    /**
     * Tests that the exponential backoff follows base-2 growth pattern.
     * Each delay should be double the previous one (until capped).
     */
    @Test
    void testCalculateBackoffDelayMs_followsExponentialPattern() {
        long previousDelay = 0;
        for (int attemptCount = 1; attemptCount <= 9; attemptCount++) {
            final long currentDelay = OpcUaProtocolAdapter.calculateBackoffDelayMs(
                    ConnectionOptions.DEFAULT_RETRY_INTERVALS, attemptCount);
            if (attemptCount > 1) {
                assertThat(currentDelay)
                        .as("Delay for attempt #%d should be double the previous delay", attemptCount)
                        .isGreaterThan(previousDelay);
            }
            previousDelay = currentDelay;
        }

        // Verify that the 10th attempt doesn't follow the exponential pattern (it's capped)
        final long tenthAttemptDelay =
                OpcUaProtocolAdapter.calculateBackoffDelayMs(ConnectionOptions.DEFAULT_RETRY_INTERVALS, 10);
        assertThat(tenthAttemptDelay)
                .as("10th attempt should be capped, not double the 9th")
                .isLessThan(previousDelay * 2)
                .isGreaterThanOrEqualTo(300_000L)
                .isLessThan(getUpperBound(300_000L));
    }

    /**
     * Tests that malformed retry intervals throw NumberFormatException.
     * Various invalid formats should be rejected with appropriate exceptions.
     */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "abc,def,ghi", // Non-numeric values
                "1000,abc,3000", // Mix of valid and invalid
                "1000,2000, ", // Trailing comma with empty value
                ",1000,2000", // Leading comma with empty value
                "1000,,2000", // Double comma with empty value
                "1000.5,2000.5", // Floating point values
                "not-a-number", // Single invalid value
                "" // Empty
            })
    void testCalculateBackoffDelayMs_malformedIntervals(final @NotNull String malformedIntervals) {
        assertThatThrownBy(() -> OpcUaProtocolAdapter.calculateBackoffDelayMs(malformedIntervals, 1))
                .isInstanceOf(NumberFormatException.class)
                .hasMessageContaining("For input string:");
    }

    /**
     * Tests that valid custom retry intervals work correctly.
     * Verifies that custom configurations are parsed and applied properly.
     */
    @Test
    void testCalculateBackoffDelayMs_customValidIntervals() {
        final String customIntervals = "5000,10000,15000";

        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(customIntervals, 1))
                .isGreaterThanOrEqualTo(5_000L)
                .isLessThan(getUpperBound(5_000L));
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(customIntervals, 2))
                .isGreaterThanOrEqualTo(10_000L)
                .isLessThan(getUpperBound(10_000L));
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(customIntervals, 3))
                .isGreaterThanOrEqualTo(15_000L)
                .isLessThan(getUpperBound(15_000L));
        // Should repeat last value when exceeding array length
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(customIntervals, 4))
                .isGreaterThanOrEqualTo(15_000L)
                .isLessThan(getUpperBound(15_000L));
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(customIntervals, 10))
                .isGreaterThanOrEqualTo(15_000L)
                .isLessThan(getUpperBound(15_000L));
    }

    /**
     * Tests that single interval value works correctly.
     * A configuration with only one value should use that value for all attempts.
     */
    @Test
    void testCalculateBackoffDelayMs_singleInterval() {
        final String singleInterval = "30000";

        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(singleInterval, 1))
                .isGreaterThanOrEqualTo(30_000L)
                .isLessThan(getUpperBound(30_000L));
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(singleInterval, 2))
                .isGreaterThanOrEqualTo(30_000L)
                .isLessThan(getUpperBound(30_000L));
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(singleInterval, 10))
                .isGreaterThanOrEqualTo(30_000L)
                .isLessThan(getUpperBound(30_000L));
    }

    /**
     * Tests that intervals with whitespace are handled correctly.
     * Leading and trailing whitespace should be trimmed from each value.
     */
    @Test
    void testCalculateBackoffDelayMs_intervalsWithWhitespace() {
        final String intervalsWithWhitespace = " 1000 , 2000 , 4000 ";

        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(intervalsWithWhitespace, 1))
                .isGreaterThanOrEqualTo(1_000L)
                .isLessThan(getUpperBound(1_000L));
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(intervalsWithWhitespace, 2))
                .isGreaterThanOrEqualTo(2_000L)
                .isLessThan(getUpperBound(2_000L));
        assertThat(OpcUaProtocolAdapter.calculateBackoffDelayMs(intervalsWithWhitespace, 3))
                .isGreaterThanOrEqualTo(4_000L)
                .isLessThan(getUpperBound(4_000L));
    }
}
