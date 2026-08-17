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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
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
    private @NotNull ListAppender<ILoggingEvent> logged;
    private @NotNull Logger connectionLog;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
        metricsService = mock(ProtocolAdapterMetricsService.class);
        connectionLog = (Logger) LoggerFactory.getLogger(OpcUaClientConnection.class);
        logged = new ListAppender<>();
        logged.start();
        connectionLog.addAppender(logged);
    }

    @AfterEach
    void tearDown() {
        connectionLog.detachAppender(logged);
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
            // This fires from inside prepareClientForUse, which runs before the subscription, the context and
            // CONNECTED. Asserting a healthy connection there is a claim the attempt has not earned yet and
            // may never earn -- subscription creation can still fail, or a stop can land -- which would leave
            // an event saying the adapter is connected beside a status of ERROR or DISCONNECTED.
            assertThat(event.getMessage())
                    .as("the event may not assert a connection the attempt has not made yet")
                    .doesNotContain("The adapter is connected")
                    .contains("The connection attempt continues");
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

    /**
     * PR review 2026-08-15: an interrupt is about this thread, not about the metadata.
     * <p>
     * The other three failure arms continue without the metadata, correctly — a cancelled, timed-out or
     * failed build says something about the build, which the connection does not need. An interrupt says the
     * thread running the connect sequence has been asked to stop. Carrying on re-set the flag and went ahead
     * anyway, so every blocking step that followed — verification's futures, subscription creation, a lock —
     * threw at once, and the attempt failed further along with a less legible cause than the interrupt it
     * already knew about.
     */
    @Test
    @Timeout(60)
    void anInterruptedPreparationAbandonsTheAttemptRatherThanConnectingOnAnInterruptedThread() throws Exception {
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
                () -> {});

        final AtomicBoolean started = new AtomicBoolean(true);
        final CountDownLatch finished = new CountDownLatch(1);
        // Deliberately not stop() -- that sets `closed`, which was already handled. This is the bare case of
        // the connecting thread itself being interrupted while the build is outstanding.
        final Thread connecting = new Thread(() -> {
            started.set(Objects.requireNonNull(opcUaClientConnection).start(parsedConfig(config)));
            finished.countDown();
        });
        connecting.start();

        assertThat(preparationStarted.await(30, TimeUnit.SECONDS)).isTrue();
        connecting.interrupt();

        assertThat(finished.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(started)
                .as("the attempt must be abandoned, not carried on with the flag re-set")
                .isFalse();
        assertThat(preparationInterrupted.await(5, TimeUnit.SECONDS))
                .as("and the build it was waiting on must be interrupted rather than left running")
                .isTrue();
        assertThat(opcUaClientConnection.client()).isEmpty();

        // The assertion that discriminates, and it took two goes to find. An empty context only says Edge
        // never published the connection, which was true of the old ordering too; and the session does close
        // either way, so waiting for it proves nothing. What differs is whether the close was *awaited*.
        // quietlyCloseClient ends in OpcUaClient.disconnect(), which is disconnectAsync().get(); with the
        // interrupt flag already restored that get() throws at once, Milo wraps it as a UaException, and this
        // is where it lands -- an ERROR saying the teardown failed, over a session still closing behind an
        // attempt that has already returned and scheduled a retry.
        assertThat(logged.list)
                .as("the disconnect must be awaited, not abandoned to finish behind a retry")
                .noneMatch(event -> event.getLevel() == Level.ERROR
                        && event.getFormattedMessage().contains("Failed to disconnect"));
        assertThat(Objects.requireNonNull(opcUaServerExtension.getOpcUaServer())
                        .getSessionManager()
                        .getAllSessions())
                .as("so the session is already gone by the time the attempt returns")
                .isEmpty();

        assertThat(eventService.readEvents(null, null))
                .as("and an abandoned attempt says nothing about degraded browse on a connection it never made")
                .noneSatisfy(event -> assertThat(event.getMessage()).containsIgnoringCase("browse"));
    }

    /**
     * PR review 2026-08-15: the degraded state must be able to end without a reconnect.
     * <p>
     * The first build fails, so the connection comes up without browse metadata. A later attempt — driven in
     * production by the health check — succeeds, and the connection both records it and says so, which is
     * what lets the adapter reopen the browse endpoint.
     */
    @Test
    @Timeout(60)
    void aLaterMetadataAttemptOnAHealthyConnectionReopensBrowse() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);
        final AtomicBoolean failNextPreparation = new AtomicBoolean(true);
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
                ignored -> {
                    if (failNextPreparation.getAndSet(false)) {
                        throw new IllegalStateException("metadata unavailable");
                    }
                },
                () -> {});

        assertThat(opcUaClientConnection.start(parsedConfig(config))).isTrue();
        assertThat(opcUaClientConnection.hasBrowseMetadata())
                .as("precondition: the connection is up without its metadata")
                .isFalse();

        final CountDownLatch reopened = new CountDownLatch(1);
        opcUaClientConnection.retryBrowseMetadata(reopened::countDown);

        assertThat(reopened.await(30, TimeUnit.SECONDS))
                .as("a later successful build must tell the adapter, or browse stays shut for the connection")
                .isTrue();
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isTrue();
    }

    /**
     * A stalled rehydration must expire, or the guard against overlapping retries becomes a permanent latch.
     * <p>
     * {@code metadataRetryWorker} is what admits the next attempt, and only the worker clears it. An
     * unbounded build on a server that simply stops answering would hold it forever and browse would never
     * reopen — the same never-retried shape as the defect the retry exists to fix, one level down.
     * <p>
     * The deadline is two seconds here, from {@code connectionTimeoutMs}, and it is enforced by a timer
     * started with the worker rather than by the next caller. No {@code retryBrowseMetadata} call is made
     * while the build is stalled, which is the point: an earlier version was interrupted only because this
     * test happened to poll, and would have waited a whole health-check interval in production.
     */
    @Test
    @Timeout(60)
    void aStalledRehydrationExpiresOnItsOwnDeadlineNotTheNextHealthCheck() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                new ConnectionOptions(null, null, null, null, 2_000L, null, null, null, null));
        // Three acts: the connect-time build fails, the first rehydration stalls until its deadline
        // interrupts it, and the one after that succeeds.
        final AtomicInteger act = new AtomicInteger();
        final CountDownLatch stalledStarted = new CountDownLatch(1);
        final CountDownLatch stalledInterrupted = new CountDownLatch(1);
        final CountDownLatch neverReleased = new CountDownLatch(1);
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
                ignored -> {
                    switch (act.getAndIncrement()) {
                        case 0 -> throw new IllegalStateException("metadata unavailable");
                        case 1 -> {
                            // Aborts when interrupted, as a real build does: Milo turns the interrupt into a
                            // UaException rather than returning as though it had finished.
                            awaitInterruption(stalledStarted, stalledInterrupted, neverReleased);
                            throw new IllegalStateException("metadata build aborted");
                        }
                        default -> {
                            // succeeds
                        }
                    }
                },
                () -> {});

        assertThat(opcUaClientConnection.start(parsedConfig(config))).isTrue();
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isFalse();

        opcUaClientConnection.retryBrowseMetadata(() -> {});
        assertThat(stalledStarted.await(10, TimeUnit.SECONDS)).isTrue();

        // Deliberately no further retryBrowseMetadata call: the deadline has to arrive on its own. Polling
        // here is what previously made a caller-driven check look like a working timeout.
        assertThat(stalledInterrupted.await(15, TimeUnit.SECONDS))
                .as("the stalled build must be interrupted on its own deadline, with no caller to prompt it")
                .isTrue();

        // And the proof that the guard was released by the worker actually ending: a further attempt is
        // admitted and succeeds.
        final CountDownLatch reopened = new CountDownLatch(1);
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> {
            opcUaClientConnection.retryBrowseMetadata(reopened::countDown);
            assertThat(reopened.await(200, TimeUnit.MILLISECONDS)).isTrue();
        });
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isTrue();
    }

    /**
     * PR review 2026-08-16: a worker that will not take the interrupt must still hold the guard.
     * <p>
     * This is the case that made the old shape wrong. Milo's {@code NonBlockingLazy} waits for an in-progress
     * {@code getDataTypeTree()} with {@code CompletableFuture.join()}, which ignores interruption — so a
     * rehydration arriving while a southbound write is building the tree cannot be cancelled. Releasing the
     * guard when the *wait* expired let the next interval start a second worker against the same server while
     * the first was still alive and no longer reachable by teardown.
     * <p>
     * Modelled here by a preparation that swallows interruption, which is what {@code join()} does.
     */
    @Test
    @Timeout(60)
    void anUninterruptibleBuildIsNotJoinedByASecondWorker() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                new ConnectionOptions(null, null, null, null, 2_000L, null, null, null, null));
        final AtomicInteger live = new AtomicInteger();
        final AtomicInteger peakLive = new AtomicInteger();
        final AtomicBoolean firstBuild = new AtomicBoolean(true);
        final CountDownLatch release = new CountDownLatch(1);
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
                ignored -> {
                    if (firstBuild.getAndSet(false)) {
                        throw new IllegalStateException("metadata unavailable");
                    }
                    peakLive.accumulateAndGet(live.incrementAndGet(), Math::max);
                    try {
                        // Uninterruptible, exactly as CompletableFuture.join() is.
                        boolean done = false;
                        while (!done) {
                            try {
                                release.await();
                                done = true;
                            } catch (final InterruptedException swallowed) {
                                // join() records the interrupt and keeps waiting; so do we.
                            }
                        }
                    } finally {
                        live.decrementAndGet();
                    }
                },
                () -> {});

        assertThat(opcUaClientConnection.start(parsedConfig(config))).isTrue();
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isFalse();

        opcUaClientConnection.retryBrowseMetadata(() -> {});
        await().atMost(java.time.Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(live.get()).isOne());

        // Well past the two-second deadline, and every interval triggers again. Each pass interrupts once and
        // finds the worker still there; none may start another.
        for (int pass = 0; pass < 40; pass++) {
            opcUaClientConnection.retryBrowseMetadata(() -> {});
            Thread.sleep(100);
        }

        assertThat(peakLive.get())
                .as("a build that cannot be cancelled must not be joined by a second one")
                .isOne();

        release.countDown();
        await().atMost(java.time.Duration.ofSeconds(10))
                .untilAsserted(() -> assertThat(live.get()).isZero());
    }

    /**
     * A server that keeps refusing must not be asked on every health-check interval forever.
     * <p>
     * The trigger is periodic, so without a schedule a permanently restricted account buys a complete
     * recursive browse of the data-type hierarchy as often as every ten seconds, for the life of the
     * connection, for an answer that is not going to change soon. One prompt retry is free — that is the
     * transient case, and it is worth recovering within an interval — and the ones after it back off.
     */
    @Test
    @Timeout(60)
    void repeatedRehydrationFailuresBackOffInsteadOfBrowsingTheServerEveryInterval() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);
        final AtomicInteger preparations = new AtomicInteger();
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
                ignored -> {
                    preparations.incrementAndGet();
                    throw new IllegalStateException("metadata permanently unavailable");
                },
                () -> {});

        assertThat(opcUaClientConnection.start(parsedConfig(config))).isTrue();
        assertThat(preparations).as("the connect-time build").hasValue(1);

        // Triggered from inside the poll, because a single call outside it can be swallowed: the counter is
        // incremented by the worker, which is still the running worker for a moment afterwards, so a trigger
        // issued the instant the count is observed finds the guard held and returns having done nothing. The
        // health check is periodic, so re-triggering until it takes is also what production does.
        //
        // What this must not do is assert an exact intermediate count while still triggering. The first
        // failure is free, so two retries are admitted back to back: a poll can start the second worker
        // before it has observed the first, taking the count straight from one to three and stranding an
        // assertion that wanted to see two. Only the value it settles on is a fact about backoff.
        await().atMost(java.time.Duration.ofSeconds(20)).untilAsserted(() -> {
            opcUaClientConnection.retryBrowseMetadata(() -> {});
            assertThat(preparations.get())
                    .as("the connect-time build plus the one free retry and the one after it")
                    .isGreaterThanOrEqualTo(3);
        });

        // Now stop provoking it and let the backoff speak: further triggers must buy nothing.
        for (int trigger = 0; trigger < 5; trigger++) {
            opcUaClientConnection.retryBrowseMetadata(() -> {});
        }
        Thread.sleep(500);
        assertThat(preparations)
                .as("a backed-off connection must not browse the server again on every trigger")
                .hasValue(3);
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isFalse();
    }

    /**
     * The recovery is announced once, however hard the health check leans on it.
     * <p>
     * A guard-rail rather than a reproduction, and worth saying so: the window this protects — between
     * releasing the worker slot and marking the metadata present — was two statements wide, so hammering
     * cannot be relied on to hit it. What the test does pin is the invariant that made the window a bug: one
     * success publication and one callback per recovery, no matter how many triggers arrive during the build.
     */
    @Test
    @Timeout(60)
    void aRecoveryIsPublishedOnceEvenUnderConcurrentTriggers() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);
        final AtomicInteger preparations = new AtomicInteger();
        final AtomicBoolean firstBuild = new AtomicBoolean(true);
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
                ignored -> {
                    if (firstBuild.getAndSet(false)) {
                        throw new IllegalStateException("metadata unavailable");
                    }
                    preparations.incrementAndGet();
                    try {
                        Thread.sleep(300);
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                },
                () -> {});

        assertThat(opcUaClientConnection.start(parsedConfig(config))).isTrue();
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isFalse();

        final AtomicInteger hydratedCallbacks = new AtomicInteger();
        final List<Thread> triggers = new java.util.ArrayList<>();
        for (int t = 0; t < 4; t++) {
            final Thread trigger = new Thread(() -> {
                for (int i = 0; i < 200; i++) {
                    Objects.requireNonNull(opcUaClientConnection)
                            .retryBrowseMetadata(hydratedCallbacks::incrementAndGet);
                }
            });
            triggers.add(trigger);
            trigger.start();
        }
        for (final Thread trigger : triggers) {
            trigger.join();
        }

        // Waited on the callback rather than on hasBrowseMetadata(): the flag is set before the callback runs,
        // so awaiting the flag can observe the recovery a moment before the thing being counted has happened.
        // That is a race in the test rather than in the code, and it only loses under load.
        await().atMost(java.time.Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(hydratedCallbacks.get()).isGreaterThanOrEqualTo(1));
        // Then give anything spurious a chance to arrive before pinning the counts.
        Thread.sleep(500);
        assertThat(hydratedCallbacks.get()).as("one recovery, one callback").isOne();
        assertThat(preparations.get()).as("and one build behind it").isOne();
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isTrue();
    }

    @Test
    @Timeout(60)
    void andARetryOnAConnectionThatAlreadyHasItsMetadataDoesNothing() throws Exception {
        final OpcUaSpecificAdapterConfig config = new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);
        final AtomicInteger preparations = new AtomicInteger();
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
                ignored -> preparations.incrementAndGet(),
                () -> {});

        assertThat(opcUaClientConnection.start(parsedConfig(config))).isTrue();
        assertThat(opcUaClientConnection.hasBrowseMetadata()).isTrue();

        final AtomicBoolean reopened = new AtomicBoolean();
        opcUaClientConnection.retryBrowseMetadata(() -> reopened.set(true));

        Thread.sleep(500);
        assertThat(preparations)
                .as("the health check fires on every interval; a ready connection must not rebuild each time")
                .hasValue(1);
        assertThat(reopened).isFalse();
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
