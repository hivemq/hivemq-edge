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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.DISCONNECTED;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import util.EmbeddedOpcUaServerExtension;

/**
 * Review-06 finding 1: a connection the adapter has replaced must not describe the one that replaced it.
 * <p>
 * {@link ProtocolAdapterState} is one slot per adapter; an {@code OpcUaClientConnection} is one attempt among
 * several. The two have different lifetimes, and the difference is not incidental —
 * {@link OpcUaProtocolAdapter#destroy()} releases the connection slot and closes the old connection on the
 * common pool, precisely so a teardown does not block the caller. The close therefore outlives the call, and
 * a configuration change is a destroy followed immediately by a start of the same instance.
 * <p>
 * So this ordering is reachable, and every step of it is ordinary:
 * <ol>
 *   <li>connection A is live and the adapter reports {@code CONNECTED};</li>
 *   <li>{@code destroy()} takes A out of the slot and starts closing it asynchronously;</li>
 *   <li>the framework starts the instance again; connection B connects and reports {@code CONNECTED};</li>
 *   <li>A's close finally returns and writes {@code DISCONNECTED} — describing B.</li>
 * </ol>
 * The health check reads that status, finds the healthy B disconnected and reconnects it: a monitoring gap
 * and a rebuild caused by an object that no longer exists. A status change also notifies the framework's
 * connection-status listener, so this is visible outside the adapter as well.
 * <p>
 * <b>On forcing the ordering.</b> Step 4 is a race in production and is not raced for here. Ownership is
 * asked of the adapter through a predicate, so a test can answer it directly — revoking ownership is exactly
 * what {@code destroy()} does by clearing the slot, and doing it explicitly makes "A's close lands after B
 * connected" a deterministic sequence rather than a timing window. The connections are real ones against the
 * embedded server, so what is under test is the real close path rather than a mock of it.
 * <p>
 * The two suppression tests have owned counterparts in the same class. A gate that never opens would satisfy
 * every assertion about a stale write while silently costing the adapter its ordinary status reporting, which
 * is the more likely way for this fix to be wrong than the defect it closes.
 */
class OpcUaStaleStatusTest {

    @RegisterExtension
    public final @NotNull EmbeddedOpcUaServerExtension opcUaServerExtension = new EmbeddedOpcUaServerExtension();

    private @NotNull ProtocolAdapterState protocolAdapterState;
    private @NotNull FakeEventService eventService;
    private @Nullable OpcUaClientConnection connection;

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

    // ── the close that lands after the replacement has connected ────────────────────────────────────

    @Test
    @Timeout(120)
    void aSupersededConnectionsCloseMustNotReportTheReplacementDisconnected() {
        // The finding itself. A is genuinely established -- it has a client, a session and a subscription on
        // the server -- and is then replaced. Its close still has all of that to unwind, which is why it takes
        // long enough to land after B has connected.
        final AtomicBoolean owned = new AtomicBoolean(true);
        final OpcUaClientConnection superseded = startedConnection(ConnectionOwnership.currentUntilRevoked(owned));
        assertThat(protocolAdapterState.getConnectionStatus())
                .as("precondition: the connection reported itself connected while it was the adapter's")
                .isEqualTo(CONNECTED);

        // What destroy() does when it clears the slot: from here on this connection speaks for nobody.
        owned.set(false);
        // And what the replacement does on its way up. Set directly rather than by starting a second
        // connection, so the assertion below is about the close alone and not about which of two connections
        // wrote last.
        protocolAdapterState.setConnectionStatus(CONNECTED);

        superseded.destroy();
        connection = null;

        assertThat(protocolAdapterState.getConnectionStatus())
                .as("a replaced connection's close must not disconnect the connection that replaced it")
                .isEqualTo(CONNECTED);
        assertThat(superseded.client())
                .as("and must still have closed itself -- the status is suppressed, not the teardown")
                .isEmpty();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(serverSubscriptions())
                .as("nor may it leave its subscription on the server")
                .isZero());
    }

    @Test
    @Timeout(120)
    void butTheCloseOfTheCurrentConnectionStillReportsDisconnected() {
        // The counterpart, and the one that fails if the gate is simply always shut. An ordinary stop of the
        // connection the adapter still holds has to report the disconnection, because there is nothing else
        // that would.
        final OpcUaClientConnection current = startedConnection(ConnectionOwnership.alwaysCurrent());

        current.destroy();
        connection = null;

        assertThat(protocolAdapterState.getConnectionStatus())
                .as("closing the adapter's own connection must still report it disconnected")
                .isEqualTo(DISCONNECTED);
    }

    // ── the start that finishes after it has been superseded ────────────────────────────────────────

    @Test
    @Timeout(120)
    void aSupersededStartThatDiscardsItselfMustNotReportTheReplacementDisconnected() {
        // The other write the finding names, on the path added by review-05 finding 1: a start that is closed
        // between establishing its subscription and publishing it discards the client and reports
        // DISCONNECTED, to correct the CONNECTED its own session activation announced. That correction is
        // right while the connection is the adapter's and wrong once it is not -- by then the status it
        // corrects is the replacement's.
        //
        // The window is driven the way OpcUaConnectionTeardownTest drives it: a tag that verification drops
        // fires an event, and closing the connection from inside that event lands the teardown after the
        // verification loop's last check, with the subscription still established. Being last is what makes
        // start() go on to reach the discard branch.
        final AtomicReference<OpcUaClientConnection> self = new AtomicReference<>();
        final FakeEventService closingOnDroppedTag = new FakeEventService() {
            @Override
            public void fireEvent(final @NotNull Event event) {
                super.fireEvent(event);
                if (Objects.requireNonNullElse(event.getMessage(), "").contains("did not subscribe tag")) {
                    Objects.requireNonNull(self.get()).destroy();
                }
            }
        };
        eventService = closingOnDroppedTag;

        final OpcUaClientConnection superseded =
                newConnection(List.of(valueTag("temperature"), notANotifier()), ConnectionOwnership.neverCurrent());
        self.set(superseded);
        connection = superseded;
        // The replacement, already connected: exactly the state this connection must not overwrite.
        protocolAdapterState.setConnectionStatus(CONNECTED);

        final boolean started = superseded.start(parsedConfig());

        assertThat(closingOnDroppedTag.readEvents(null, null))
                .as("precondition: the bad tag must actually have been dropped, or nothing closed the connection")
                .anySatisfy(event -> assertThat(event.getMessage()).contains("did not subscribe tag"));
        assertThat(started)
                .as("precondition: the discard branch is the one that must have been taken")
                .isFalse();
        assertThat(protocolAdapterState.getConnectionStatus())
                .as("a superseded start discarding itself must not disconnect the connection that replaced it")
                .isEqualTo(CONNECTED);
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> assertThat(serverSubscriptions())
                .as("and must still take its own subscription off the server")
                .isZero());
        connection = null;
    }

    @Test
    @Timeout(120)
    void butACurrentStartThatDiscardsItselfStillCorrectsTheStatus() {
        // The counterpart again, and the behaviour review-05 finding 1 added. Without this the adapter is left
        // reporting CONNECTED -- announced by its own session activation -- for a client it has thrown away.
        final AtomicReference<OpcUaClientConnection> self = new AtomicReference<>();
        final FakeEventService closingOnDroppedTag = new FakeEventService() {
            @Override
            public void fireEvent(final @NotNull Event event) {
                super.fireEvent(event);
                if (Objects.requireNonNullElse(event.getMessage(), "").contains("did not subscribe tag")) {
                    Objects.requireNonNull(self.get()).destroy();
                }
            }
        };
        eventService = closingOnDroppedTag;

        final OpcUaClientConnection current =
                newConnection(List.of(valueTag("temperature"), notANotifier()), ConnectionOwnership.alwaysCurrent());
        self.set(current);
        connection = current;

        assertThat(current.start(parsedConfig()))
                .as("precondition: the discard branch is the one that must have been taken")
                .isFalse();

        assertThat(protocolAdapterState.getConnectionStatus())
                .as("a discarded start must not leave the adapter reporting the connection it threw away")
                .isNotEqualTo(CONNECTED);
        connection = null;
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private @NotNull OpcUaClientConnection startedConnection(
            final @NotNull Predicate<OpcUaClientConnection> ownership) {
        final OpcUaClientConnection fresh = newConnection(List.of(valueTag("temperature")), ownership);
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

    /** An event subscription tag on a plain Int32 variable: it exists, and cannot carry events. */
    private @NotNull OpcuaTag notANotifier() {
        return new OpcuaTag(
                "not-a-notifier",
                "",
                new OpcuaTagDefinition(
                        "ns=" + opcUaServerExtension.getTestNamespace().getNamespaceIndex() + ";i=10",
                        OpcuaTagKind.EVENT_SUBSCRIPTION));
    }

    private @NotNull OpcUaClientConnection newConnection(
            final @NotNull List<OpcuaTag> tags, final @NotNull Predicate<OpcUaClientConnection> ownership) {
        return new OpcUaClientConnection(
                "test-adapter-id",
                tags,
                protocolAdapterState,
                mock(ProtocolAdapterTagStreamingService.class),
                eventService,
                mock(ProtocolAdapterMetricsService.class),
                config(),
                new OpcUaServiceFaultListener(
                        mock(ProtocolAdapterMetricsService.class), eventService, "test-adapter-id", () -> {}, true),
                ownership);
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
