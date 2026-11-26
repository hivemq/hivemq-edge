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
package com.hivemq.edge.adapters.modbus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/// The client's watch on its own connection: when an established connection drops, the client publishes a wait that
/// its first reconnect attempt ends, with success, with the attempt's failure, or with a failure when the client is
/// disconnected first, so a poll waiting on it always completes.
class ModbusClientReconnectTest {

    // The module's minimum; a refused loopback connect fails at once.
    private static final int CONNECT_TIMEOUT_MILLIS = 1_000;
    // The client's first reconnect comes 1 s after a drop and a loopback connect answers at once; 10 s only bounds a
    // hung attempt.
    private static final long WAIT_SECONDS = 10;
    // The device records an accepted connection, and the client publishes the wait when it sees the drop, a few
    // milliseconds after the event; 5 s only bounds a hang.
    private static final @NotNull Duration SEEN_WITHIN = Duration.ofSeconds(5);

    private final @NotNull List<Channel> connections = new CopyOnWriteArrayList<>();
    private int port;
    private @Nullable ModbusTcpServer device;
    private @Nullable ModbusClient client;

    @BeforeEach
    void setUp() throws Exception {
        connections.clear();
        device = startDevice();
        final ModbusClient connected = new ModbusClient(new ModbusSpecificAdapterConfig(
                port, "127.0.0.1", CONNECT_TIMEOUT_MILLIS, new ModbusToMqttConfig(null, null, null)));
        client = connected;
        connected.connect().toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
        assertThat(connected.reconnectAfterDrop()).isNull();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.disconnect().toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
        }
        if (device != null) {
            device.stop();
        }
    }

    /// The device keeps listening after it closes the connection: the first reconnect ends the wait with success and
    /// the client is connected again.
    @Test
    void theFirstReconnectAfterADropEndsTheWaitOnceConnected() throws Exception {
        final ModbusClient connected = Objects.requireNonNull(client);
        dropTheConnection();
        final CompletableFuture<Void> wait = awaitTheWait(connected);

        wait.get(WAIT_SECONDS, TimeUnit.SECONDS);

        assertThat(connected.isConnected()).isTrue();
        assertThat(connected.lastConnectFailure()).isNull();
        assertThat(connected.reconnectAfterDrop()).isNull();
    }

    /// The device goes away: the first reconnect is refused, the wait fails with that refusal, and the refusal is the
    /// client's last connect failure.
    @Test
    void aRefusedFirstReconnectAfterADropFailsTheWaitWithTheRefusal() throws Exception {
        final ModbusClient connected = Objects.requireNonNull(client);
        Objects.requireNonNull(device).stop();
        device = null;
        final CompletableFuture<Void> wait = awaitTheWait(connected);

        assertThatThrownBy(() -> wait.get(WAIT_SECONDS, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(ConnectException.class);
        assertThat(connected.lastConnectFailure()).isInstanceOf(ConnectException.class);
        assertThat(connected.reconnectAfterDrop()).isNull();
    }

    /// A disconnect before the first reconnect fails the wait, so a poll waiting on it does not wait for a reconnect
    /// that never comes.
    @Test
    void aDisconnectBeforeTheFirstReconnectFailsTheWait() throws Exception {
        final ModbusClient connected = Objects.requireNonNull(client);
        dropTheConnection();
        final CompletableFuture<Void> wait = awaitTheWait(connected);

        connected.disconnect().toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
        client = null;

        assertThatThrownBy(() -> wait.get(WAIT_SECONDS, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
        assertThat(connected.reconnectAfterDrop()).isNull();
    }

    /// A read after the client disconnected fails instead of answering `null`, so a poll that reads several tags fails
    /// as a whole and records none of their values as published.
    @Test
    void aReadWhileNotConnectedFails() throws Exception {
        final ModbusClient connected = Objects.requireNonNull(client);
        connected.disconnect().toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS);
        client = null;

        assertThatThrownBy(() -> connected.readCoils(0, 1).toCompletableFuture().get(WAIT_SECONDS, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    /// Closes the device's side of the one connection the client holds; the device keeps listening. The client's
    /// connect can complete before the device's pipeline callback records the accepted channel, so the channel is
    /// awaited first.
    private void dropTheConnection() throws InterruptedException {
        await().atMost(SEEN_WITHIN).until(() -> connections.size() == 1);
        assertThat(connections).hasSize(1);
        assertThat(connections.getFirst().close().await(WAIT_SECONDS, TimeUnit.SECONDS))
                .isTrue();
    }

    private static @NotNull CompletableFuture<Void> awaitTheWait(final @NotNull ModbusClient connected) {
        return await().atMost(SEEN_WITHIN).until(connected::reconnectAfterDrop, Objects::nonNull);
    }

    /// Starts a device on a port the system picks, sets `port` to it, and answers every unit; every connection it
    /// accepts is added to `connections`.
    private @NotNull ModbusTcpServer startDevice() throws Exception {
        final ProcessImage image = new ProcessImage();
        final CompletableFuture<Integer> bound = new CompletableFuture<>();
        final ModbusTcpServer server = ModbusTcpServer.create(
                NettyTcpServerTransport.create(cfg -> {
                    cfg.bindAddress = "127.0.0.1";
                    cfg.port = 0;
                    cfg.pipelineCustomizer = pipeline -> connections.add(pipeline.channel());
                    cfg.bootstrapCustomizer = bootstrap -> bootstrap.handler(new ChannelInboundHandlerAdapter() {
                        @Override
                        public void channelActive(final @NotNull ChannelHandlerContext context) {
                            bound.complete(
                                    ((InetSocketAddress) context.channel().localAddress()).getPort());
                            context.fireChannelActive();
                        }
                    });
                }),
                new ReadWriteModbusServices() {
                    @Override
                    protected Optional<ProcessImage> getProcessImage(final int unitId) {
                        return Optional.of(image);
                    }
                });
        server.start();
        port = bound.get(WAIT_SECONDS, TimeUnit.SECONDS);
        return server;
    }
}
