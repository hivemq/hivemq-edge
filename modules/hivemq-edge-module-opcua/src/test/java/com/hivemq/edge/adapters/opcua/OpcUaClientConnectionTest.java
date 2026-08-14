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
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Result;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

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

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
        metricsService = mock(ProtocolAdapterMetricsService.class);
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
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                // 1 second publishing interval
                null,
                null);

        // Create a tag that maps to a node in the test server
        final OpcuaTag tag = new OpcuaTag(
                "testTag",
                "Test tag for keep-alive",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex()
                                + ";i=10")); // Int32 node from TestNamespace

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        final AtomicBoolean reconnectionCallbackInvoked = new AtomicBoolean(false);

        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                streamingService,
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(
                        metricsService,
                        eventService,
                        "test-adapter-id",
                        () -> reconnectionCallbackInvoked.set(true),
                        true),
                ConnectionOwnership.alwaysCurrent());

        // Parse config
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        // Act - Start the connection
        final boolean started = opcUaClientConnection.start(parsedConfig);

        // Assert
        assertThat(started).as("Connection should start successfully").isTrue();

        // Wait for connection to be established
        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .as("Connection should be established")
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Wait for subscription to be created and initial keep-alives to be received
        // Keep-alive should be sent every few publishing intervals (maxKeepAliveCount * publishingInterval)
        // With 1 second publishing interval, keep-alives should arrive within a few seconds
        await().atMost(java.time.Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(opcUaClientConnection.isHealthy())
                    .as("Connection should be healthy after keep-alive messages are received")
                    .isTrue();
        });

        // Verify that isHealthy() continues to return true while keep-alives are being received
        Thread.sleep(5000); // Wait 5 more seconds to ensure keep-alives continue
        assertThat(opcUaClientConnection.isHealthy())
                .as("Connection should remain healthy")
                .isTrue();

        // Verify reconnection callback was not invoked
        assertThat(reconnectionCallbackInvoked.get())
                .as("Reconnection callback should not be invoked during normal operation")
                .isFalse();
    }

    /**
     * Review-09 finding 2. The ordering assertions below are review-08's and are unchanged; what this test
     * says differently is what happens when the build fails.
     * <p>
     * It used to assert {@code ERROR} and a discarded client. That made an ancillary metadata build fatal to
     * every OPC UA adapter, including value-only ones — a server that denies browsing the type hierarchy to
     * the configured identity would take a working adapter into permanent backoff after an upgrade. Nothing
     * at connect time consumes the tree: its only users build themselves on demand and Milo rebuilds it
     * lazily, so the honest cost is browse latency, and the honest report is connected-but-not-browse-ready.
     */
    @Test
    @Timeout(60)
    void whenBrowseMetadataCannotBeBuilt_theAdapterStillConnectsButIsNotBrowseReady() {
        final var testNamespace = Objects.requireNonNull(opcUaServerExtension.getTestNamespace());
        testNamespace.observeRefreshEvents();
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);
        final String conditionNode = testNamespace.addAcknowledgeableConditionNode("PreparationOrderAlarm", 9_800L);
        final OpcuaTag tag =
                new OpcuaTag("testTag", "Test tag", new OpcuaTagDefinition(conditionNode, OpcuaTagKind.CONDITION));

        final AtomicBoolean markedReady = new AtomicBoolean();
        final AtomicBoolean subscriptionExistedDuringPreparation = new AtomicBoolean();
        final AtomicBoolean eventItemExistedDuringPreparation = new AtomicBoolean();
        final AtomicBoolean refreshStartedDuringPreparation = new AtomicBoolean();
        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(metricsService, eventService, "test-adapter-id", () -> {}, true),
                ConnectionOwnership.alwaysCurrent(),
                ignored -> {
                    subscriptionExistedDuringPreparation.set(
                            !Objects.requireNonNull(opcUaServerExtension.getOpcUaServer())
                                    .getSubscriptions()
                                    .isEmpty());
                    eventItemExistedDuringPreparation.set(testNamespace.eventItemCount() > 0);
                    refreshStartedDuringPreparation.set(testNamespace.refreshBracketCount() > 0);
                    throw new IllegalStateException("metadata unavailable");
                },
                () -> markedReady.set(true));

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        assertThat(opcUaClientConnection.start(parsedConfig))
                .as("a metadata build the adapter does not need at connect time cannot fail the attempt")
                .isTrue();
        assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(markedReady).isTrue();
        assertThat(opcUaClientConnection.client())
                .as("the session is kept: its tags are verified and subscribed, and events flow")
                .isPresent();
        assertThat(opcUaClientConnection.hasBrowseMetadata())
                .as("but the connection says it did not manage the metadata, so browse is not claimed ready")
                .isFalse();

        // Review-08's ordering, unchanged. Preparation still runs before anything of the server's exists.
        assertThat(subscriptionExistedDuringPreparation)
                .as("metadata preparation must run before subscription creation")
                .isFalse();
        assertThat(eventItemExistedDuringPreparation)
                .as("metadata preparation must run before event monitored-item creation")
                .isFalse();
        assertThat(refreshStartedDuringPreparation)
                .as("metadata preparation must run before the automatic condition refresh")
                .isFalse();

        assertThat(eventService.readEvents(null, null)).anySatisfy(event -> {
            assertThat(event.getMessage()).contains("Failed to prepare OPC UA browse metadata");
            assertThat(event.getSeverity())
                    .as("a degraded capability, not a failed adapter")
                    .isEqualTo(Event.SEVERITY.WARN);
        });
    }

    @Test
    @Timeout(30)
    void stopDuringMetadataPreparation_cancelsTheBuildAndCannotPublishALateContext() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);
        final CountDownLatch preparationStarted = new CountDownLatch(1);
        final CountDownLatch preparationInterrupted = new CountDownLatch(1);
        final CountDownLatch neverReleased = new CountDownLatch(1);
        final AtomicBoolean markedReady = new AtomicBoolean();
        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(),
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(metricsService, eventService, "test-adapter-id", () -> {}, true),
                ConnectionOwnership.alwaysCurrent(),
                ignored -> awaitInterruption(preparationStarted, preparationInterrupted, neverReleased),
                () -> markedReady.set(true));
        final ParsedConfig parsedConfig = parsedConfig(config);

        final CompletableFuture<Boolean> start =
                CompletableFuture.supplyAsync(() -> opcUaClientConnection.start(parsedConfig));
        assertThat(preparationStarted.await(10, TimeUnit.SECONDS)).isTrue();

        CompletableFuture.runAsync(opcUaClientConnection::stop).get(5, TimeUnit.SECONDS);

        assertThat(preparationInterrupted.await(5, TimeUnit.SECONDS))
                .as("teardown must interrupt the metadata build rather than wait for its deadline")
                .isTrue();
        assertThat(start.get(5, TimeUnit.SECONDS)).isFalse();
        assertThat(markedReady).isFalse();
        assertThat(opcUaClientConnection.client()).isEmpty();
        assertThat(protocolAdapterState.getConnectionStatus())
                .as("a deliberate stop during startup is not a preparation failure")
                .isNotEqualTo(ProtocolAdapterState.ConnectionStatus.ERROR);
    }

    /**
     * The deadline still bites — it just no longer costs the adapter.
     * <p>
     * Two things are being pinned. That the expired build is actually interrupted rather than left running,
     * which is review-08's guarantee; and that expiry is survivable, because whoever next wants the tree will
     * rebuild it. The bound is the smaller of {@code connectionTimeoutMs} and the connection's own ceiling,
     * so the 2 s configured here is what applies.
     */
    @Test
    @Timeout(30)
    void metadataPreparationPastItsDeadline_isInterruptedButLeavesTheAdapterConnected() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                new ConnectionOptions(null, null, null, null, 2_000L, null, null, null, null));
        final CountDownLatch preparationStarted = new CountDownLatch(1);
        final CountDownLatch preparationInterrupted = new CountDownLatch(1);
        final CountDownLatch neverReleased = new CountDownLatch(1);
        final AtomicBoolean markedReady = new AtomicBoolean();
        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(),
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(metricsService, eventService, "test-adapter-id", () -> {}, true),
                ConnectionOwnership.alwaysCurrent(),
                ignored -> awaitInterruption(preparationStarted, preparationInterrupted, neverReleased),
                () -> markedReady.set(true));

        assertThat(opcUaClientConnection.start(parsedConfig(config))).isTrue();

        assertThat(preparationStarted.getCount()).isZero();
        assertThat(preparationInterrupted.await(5, TimeUnit.SECONDS))
                .as("the expired build must be interrupted, not abandoned to run on")
                .isTrue();
        assertThat(protocolAdapterState.getConnectionStatus())
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        assertThat(markedReady).isTrue();
        assertThat(opcUaClientConnection.client()).isPresent();
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isFalse();
        assertThat(eventService.readEvents(null, null)).anySatisfy(event -> {
            assertThat(event.getMessage()).contains("did not finish within 2000 milliseconds");
            assertThat(event.getSeverity()).isEqualTo(Event.SEVERITY.WARN);
        });
    }

    @Test
    @Timeout(60)
    void whenMultipleTagsSubscribed_thenKeepAliveMessagesAreReceived() throws Exception {
        // Arrange
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 2000),
                // 2 second publishing interval
                null,
                null);

        // Create multiple tags
        final List<OpcuaTag> tags = List.of(
                new OpcuaTag(
                        "tag1",
                        "Int32 Tag",
                        new OpcuaTagDefinition(
                                "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10")),
                new OpcuaTag(
                        "tag2",
                        "Int64 Tag",
                        new OpcuaTagDefinition(
                                "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=12")),
                new OpcuaTag(
                        "tag3",
                        "Double Tag",
                        new OpcuaTagDefinition(
                                "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=13")));

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                tags,
                protocolAdapterState,
                streamingService,
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(metricsService, eventService, "test-adapter-id", () -> {}, true),
                ConnectionOwnership.alwaysCurrent());

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        // Act
        final boolean started = opcUaClientConnection.start(parsedConfig);

        // Assert
        assertThat(started).as("Connection should start successfully").isTrue();

        await().untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus())
                    .as("Connection should be established")
                    .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        });

        // Wait for keep-alives with multiple subscriptions
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> {
            assertThat(opcUaClientConnection.isHealthy())
                    .as("Connection should be healthy with multiple tags subscribed")
                    .isTrue();
        });
    }

    @Test
    @Timeout(30)
    void whenNoSubscriptionCreated_thenIsHealthyReturnsFalse() {
        // Arrange - Use empty tag list so no subscription is created
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(),
                // Empty tags
                protocolAdapterState,
                streamingService,
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(metricsService, eventService, "test-adapter-id", () -> {}, true),
                ConnectionOwnership.alwaysCurrent());

        // Act
        final boolean healthy = opcUaClientConnection.isHealthy();

        // Assert
        assertThat(healthy)
                .as("Connection should not be healthy when no connection exists")
                .isFalse();
    }

    @Test
    @Timeout(30)
    void whenConnectionStopped_thenIsHealthyReturnsFalse() throws Exception {
        // Arrange
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);

        final OpcuaTag tag = new OpcuaTag(
                "testTag",
                "Test tag",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10"));

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);

        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                streamingService,
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(metricsService, eventService, "test-adapter-id", () -> {}, true),
                ConnectionOwnership.alwaysCurrent());

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        opcUaClientConnection.start(parsedConfig);

        await().untilAsserted(() -> {
            assertThat(protocolAdapterState.getConnectionStatus())
                    .as("Connection should be established")
                    .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        });

        // Act - Stop the connection
        opcUaClientConnection.stop();

        // Assert
        await().untilAsserted(() -> {
            assertThat(opcUaClientConnection.isHealthy())
                    .as("Connection should not be healthy after being stopped")
                    .isFalse();
        });
    }

    /**
     * EDG-688 regression test: a non-default {@code publishingInterval} configured on the adapter
     * must actually reach the OPC UA server. Before the fix, the adapter constructed the
     * {@link org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription} with Milo's
     * no-arg constructor (defaulting to 1000 ms) and only called {@code setPublishingInterval}
     * <em>after</em> {@code create()} had already gone on the wire, queuing a
     * {@code ModifySubscription} diff that was never flushed. The end effect was that every
     * subscription ran at the Milo default of 1000 ms regardless of what was configured.
     */
    @Test
    @Timeout(60)
    void configuredPublishingInterval_isAppliedOnTheServer() throws Exception {
        // Arrange — non-default, distinguishable-from-Milo-default value
        final int requestedPublishingIntervalMs = 250;
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, requestedPublishingIntervalMs),
                null,
                null);

        final OpcuaTag tag = new OpcuaTag(
                "testTag",
                "Test tag",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10"));

        final ProtocolAdapterTagStreamingService streamingService = mock(ProtocolAdapterTagStreamingService.class);
        final AtomicBoolean reconnectionCallbackInvoked = new AtomicBoolean(false);

        opcUaClientConnection = new OpcUaClientConnection(
                "test-adapter-id",
                List.of(tag),
                protocolAdapterState,
                streamingService,
                eventService,
                metricsService,
                config,
                new OpcUaServiceFaultListener(
                        metricsService,
                        eventService,
                        "test-adapter-id",
                        () -> reconnectionCallbackInvoked.set(true),
                        true),
                ConnectionOwnership.alwaysCurrent());

        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        final ParsedConfig parsedConfig = ((Success<ParsedConfig, String>) result).result();

        // Act
        final boolean started = opcUaClientConnection.start(parsedConfig);
        assertThat(started).as("Connection should start successfully").isTrue();

        await().untilAsserted(() -> assertThat(protocolAdapterState.getConnectionStatus())
                .as("Connection should be established")
                .isEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED));

        // Assert — the OPC UA server's subscription manager records the negotiated publishingInterval
        // for every active subscription. With the fix, this is the value we requested; without the
        // fix, the server would have received Milo's default of 1000 ms instead.
        await().untilAsserted(() -> {
            final var serverSubscriptions = java.util.Objects.requireNonNull(opcUaServerExtension.getOpcUaServer())
                    .getSubscriptions()
                    .values();
            assertThat(serverSubscriptions)
                    .as("server should see exactly one active subscription")
                    .hasSize(1);
            final double serverPublishingInterval =
                    serverSubscriptions.iterator().next().getPublishingInterval();
            assertThat(serverPublishingInterval)
                    .as(
                            "server-observed publishingInterval must equal the configured value, not Milo's 1000 ms default")
                    .isCloseTo(requestedPublishingIntervalMs, within(1.0));
        });
    }

    private static void awaitInterruption(
            final @NotNull CountDownLatch started,
            final @NotNull CountDownLatch interrupted,
            final @NotNull CountDownLatch neverReleased) {
        started.countDown();
        try {
            neverReleased.await();
        } catch (final InterruptedException e) {
            interrupted.countDown();
            Thread.currentThread().interrupt();
        }
    }

    private static @NotNull ParsedConfig parsedConfig(final @NotNull OpcUaSpecificAdapterConfig config) {
        final Result<ParsedConfig, String> result = ParsedConfig.fromConfig(config);
        assertThat(result).isInstanceOf(Success.class);
        return ((Success<ParsedConfig, String>) result).result();
    }
}
