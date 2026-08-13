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
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.client.ParsedConfig;
import com.hivemq.edge.adapters.opcua.client.Success;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import com.hivemq.edge.adapters.opcua.listeners.OpcUaServiceFaultListener;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterStateImpl;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * Review-05 finding 1: what a stop or a destroy can reach while a start is still establishing itself.
 * <p>
 * {@code start()} is slow and it holds the instance monitor for all of it — a connect, then up to three
 * blocking round trips per condition tag, each with a ten-second ceiling. Two separate things went wrong in
 * that window, and they need separate tests because they fail differently.
 * <p>
 * <b>The teardown could not get in.</b> {@code stop()} was {@code synchronized}, so it queued behind the very
 * work it was trying to shorten, and the operator saw a hang proportional to the tag count.
 * {@code destroy()} was not synchronized and did get through, but it looked for its handler in
 * {@code ConnectionContext} — which {@code start()} installs at the very end — so it found null and returned
 * having done nothing. Between them, {@link com.hivemq.edge.adapters.opcua.listeners.OpcUaSubscriptionLifecycleHandler#abandon()}
 * was unreachable on the connect path, and the check it feeds could never be observed true there.
 * <p>
 * <b>The start then published anyway.</b> Worse than the delay: a {@code destroy()} that completed against a
 * null context left the adapter believing it held nothing, while the start went on to install a live session
 * and subscription into an object the adapter had already discarded. Nothing could close it afterwards, and
 * it went on publishing events for the lifetime of the process.
 * <p>
 * <b>On forcing the ordering.</b> The first two tests hold the monitor directly rather than racing a real
 * start for it — stronger than the real case, which releases it eventually, and free of timing assumptions.
 * The third drives the narrow window between {@code subscribe()} returning and the context being installed,
 * by closing the connection from inside the event a dropped tag fires. That tag is deliberately the last one,
 * so the abandonment lands after the verification loop's final check and the subscription is still
 * established — which is precisely the ordering in which the old code reported success.
 */
class OpcUaConnectionTeardownTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @Nullable OpcUaClientConnection connection;
    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull FakeEventService eventService;

    @BeforeEach
    void setUp() {
        protocolAdapterState = new ProtocolAdapterStateImpl(mock(), "test-adapter-id", "opcua");
        eventService = new FakeEventService();
    }

    @AfterEach
    void tearDown() {
        if (connection != null) {
            connection.destroy();
        }
    }

    @Test
    @Timeout(60)
    void stopAbandonsTheHandlerWithoutWaitingForTheMonitor() throws Exception {
        final OpcUaClientConnection started = startedConnection();
        assertThat(started.handlerWasAbandoned())
                .as("precondition: a live connection has not been abandoned")
                .isFalse();

        assertAbandonedWhileTheMonitorIsHeld(started, started::stop);
    }

    @Test
    @Timeout(60)
    void destroyAbandonsTheHandlerWithoutWaitingForTheMonitor() throws Exception {
        final OpcUaClientConnection started = startedConnection();

        assertAbandonedWhileTheMonitorIsHeld(started, started::destroy);
        connection = null;
    }

    @Test
    @Timeout(60)
    void aConnectionClosedBeforeItStartsNeverConnects() {
        // The cheap half of the guard. destroy() before start() is reachable whenever the framework discards
        // an adapter whose first attempt has been queued but not yet run, and there is no reason to spend a
        // connect and a full verification pass on behalf of an adapter that is already gone.
        final OpcUaClientConnection fresh = newConnection(List.of(valueTag("temperature")));
        connection = fresh;
        fresh.destroy();

        assertThat(fresh.start(parsedConfig()))
                .as("a closed connection must refuse to start")
                .isFalse();
        assertThat(fresh.client()).as("and must not leave a client behind").isEmpty();
        assertThat(serverSubscriptions()).as("nor a subscription on the server").isZero();
    }

    @Test
    @Timeout(120)
    void aStartClosedAfterItsSubscriptionDiscardsTheClientRatherThanPublishingIt() throws Exception {
        // The orphan itself. Two tags, and the order is the point: the good one is verified and subscribed,
        // then the bad one is dropped and fires the event that closes the connection. Being last, no further
        // `abandoned` check follows it, so subscribe() goes on to establish the subscription and returns it
        // -- the one ordering in which the old code reached context.set() and reported success.
        final AtomicReference<OpcUaClientConnection> self = new AtomicReference<>();
        final FakeEventService closingOnDroppedTag = new FakeEventService() {
            @Override
            public void fireEvent(final @NotNull Event event) {
                super.fireEvent(event);
                if (Objects.requireNonNullElse(event.getMessage(), "").contains("did not subscribe tag")) {
                    // Same thread as start(), which already holds the monitor. The monitor is reentrant, so
                    // this is the teardown arriving mid-start rather than a deadlock.
                    Objects.requireNonNull(self.get()).destroy();
                }
            }
        };
        eventService = closingOnDroppedTag;

        // An event subscription tag pointing at a plain Int32 variable: a node that exists, and that the
        // session cannot subscribe to for events, so verification drops this tag alone and reports it.
        final OpcuaTag notANotifier = new OpcuaTag(
                "not-a-notifier",
                "",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10",
                        OpcuaTagKind.EVENT_SUBSCRIPTION));
        final OpcUaClientConnection racing = newConnection(List.of(valueTag("temperature"), notANotifier));
        self.set(racing);
        connection = racing;

        final boolean started = racing.start(parsedConfig());

        assertThat(closingOnDroppedTag.readEvents(null, null))
                .as("precondition: the bad tag must actually have been dropped, or nothing closed the connection")
                .anySatisfy(event -> assertThat(event.getMessage()).contains("did not subscribe tag"));
        assertThat(started)
                .as("a start overtaken by a destroy must not report success")
                .isFalse();
        assertThat(racing.client())
                .as("and must not publish the client it established")
                .isEmpty();
        // The consequence that made this High rather than cosmetic: an unreferenced session left publishing.
        //
        // Deliberately not "CONNECTED was never set". The session activity listener reports that the moment
        // Milo activates the session, which is honest and happens long before there is a subscription to
        // publish from. What must not survive is the state the old code left behind: a client still
        // connected, its subscription still on the server, and the adapter reporting itself connected on the
        // strength of an attempt it had already discarded.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(serverSubscriptions())
                    .as("the discarded attempt must take its subscription off the server with it")
                    .isZero();
            assertThat(protocolAdapterState.getConnectionStatus())
                    .as("and must not leave the adapter reporting a connection it discarded")
                    .isNotEqualTo(ProtocolAdapterState.ConnectionStatus.CONNECTED);
        });
        connection = null;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * Runs a teardown while this test thread holds the connection's monitor, and requires the abandonment to
     * land before the monitor is released.
     * <p>
     * The assertion has to happen <em>inside</em> the synchronized block. Checking afterwards would pass on
     * the old code too, since a {@code synchronized} stop only ever blocked until the monitor came free.
     */
    private static void assertAbandonedWhileTheMonitorIsHeld(
            final @NotNull OpcUaClientConnection target, final @NotNull Runnable teardown) throws Exception {

        final CompletableFuture<Void> torndown;
        synchronized (target) {
            torndown = CompletableFuture.runAsync(teardown);
            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(target.handlerWasAbandoned())
                    .as("the teardown must reach the handler without the monitor a start would hold")
                    .isTrue());
        }
        // Nothing about the fix, but a teardown that never completes would be its own defect.
        torndown.get(30, java.util.concurrent.TimeUnit.SECONDS);
    }

    private @NotNull OpcUaClientConnection startedConnection() {
        final OpcUaClientConnection fresh = newConnection(List.of(valueTag("temperature")));
        connection = fresh;
        assertThat(fresh.start(parsedConfig()))
                .as("precondition: the connection starts against the embedded server")
                .isTrue();
        return fresh;
    }

    private @NotNull OpcuaTag valueTag(final @NotNull String name) {
        return new OpcuaTag(
                name,
                "",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10"));
    }

    private @NotNull OpcUaClientConnection newConnection(final @NotNull List<OpcuaTag> tags) {
        return new OpcUaClientConnection(
                "test-adapter-id",
                tags,
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                eventService,
                mock(ProtocolAdapterMetricsService.class),
                config(),
                new OpcUaServiceFaultListener(
                        mock(ProtocolAdapterMetricsService.class), eventService, "test-adapter-id", () -> {}, true));
    }

    private @NotNull OpcUaSpecificAdapterConfig config() {
        return new OpcUaSpecificAdapterConfig(
                opcUaServerExtension.getServerUri(),
                false,
                null,
                null,
                null,
                new OpcUaToMqttConfig(1, 1000),
                null,
                null);
    }

    private @NotNull ParsedConfig parsedConfig() {
        final var result = ParsedConfig.fromConfig(config());
        assertThat(result).isInstanceOf(Success.class);
        return ((Success<ParsedConfig, String>) result).result();
    }

    private int serverSubscriptions() {
        return Objects.requireNonNull(opcUaServerExtension.getOpcUaServer())
                .getSubscriptions()
                .size();
    }
}
