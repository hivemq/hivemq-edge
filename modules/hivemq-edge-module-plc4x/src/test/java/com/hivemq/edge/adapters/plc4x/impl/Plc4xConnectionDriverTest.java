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
package com.hivemq.edge.adapters.plc4x.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttConfig;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The connect path against the real PLC4X 0.13.1 S7 and ADS drivers and a local endpoint that accepts TCP and never
 * answers. A disconnect, as {@code stop()} issues it, lands at the points of the driver's connect where its own order
 * of steps matters: before the TCP connect completes, and after the S7 retry task is installed but before the S7
 * protocol layer exists. In both, the connect must end and nothing it opened may stay open.
 */
class Plc4xConnectionDriverTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(10);
    private static final int RUNS = 3;

    private @NotNull SilentEndpoint endpoint;

    @BeforeEach
    void setUp() throws IOException {
        endpoint = new SilentEndpoint();
    }

    @AfterEach
    void tearDown() throws IOException {
        endpoint.close();
    }

    @Test
    void test_s7_disconnectBeforeTheTcpConnectCompletes_leavesNoConnectionToTheDevice() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            final DriverConnection connection = s7();
            final CompletableFuture<Void> attempt = startAsync(connection);
            awaitRunningConnect(connection);

            connection.disconnect();

            await().atMost(TIMEOUT).until(() -> connection.connectTerminated().isDone());
            attempt.get();
            await().atMost(TIMEOUT)
                    .alias("no TCP connection to the device outlives the attempt")
                    .until(() -> endpoint.open.get() == 0);
        }
    }

    @Test
    void test_ads_disconnectBeforeTheTcpConnectCompletes_leavesNoConnectionToTheDevice() throws Exception {
        for (int run = 0; run < RUNS; run++) {
            final DriverConnection connection = ads();
            final CompletableFuture<Void> attempt = startAsync(connection);
            awaitRunningConnect(connection);

            connection.disconnect();

            await().atMost(TIMEOUT).until(() -> connection.connectTerminated().isDone());
            attempt.get();
            await().atMost(TIMEOUT)
                    .alias("no TCP connection to the device outlives the attempt")
                    .until(() -> endpoint.open.get() == 0);
        }
    }

    @Test
    void test_s7_disconnectAfterTheRetryTaskIsInstalled_endsTheConnectAndClosesIt() throws Exception {
        final Field retryTask = Class.forName("org.apache.plc4x.java.s7.readwrite.protocol.S7HPlcConnection")
                .getDeclaredField("scf");
        retryTask.setAccessible(true);
        final long driverThreadsBefore = driverProtocolThreads();
        for (int run = 0; run < RUNS; run++) {
            final DriverConnection connection = s7();
            final CompletableFuture<Void> attempt = startAsync(connection);
            final PlcConnection driver = awaitRunningConnect(connection);
            // The window between the retry task and the protocol layer is well under a millisecond: spin.
            final long deadline = System.nanoTime() + TIMEOUT.toNanos();
            while (retryTask.get(driver) == null && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertThat(retryTask.get(driver)).isNotNull();

            connection.disconnect();

            await().atMost(TIMEOUT)
                    .alias("a connect interrupted before the S7 protocol layer exists still ends")
                    .until(() -> connection.connectTerminated().isDone());
            attempt.get();
            await().atMost(TIMEOUT)
                    .alias("no TCP connection to the device outlives the attempt")
                    .until(() -> endpoint.open.get() == 0);
        }
        await().atMost(TIMEOUT)
                .alias("the S7 protocol layer's threads end with the attempts that started them")
                .until(() -> driverProtocolThreads() <= driverThreadsBefore);
    }

    /** The S7 protocol layer's event threads, which only closing the connection after it exists ends. */
    private static long driverProtocolThreads() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(thread -> thread.getName().startsWith("plc4x-evt-"))
                .count();
    }

    private static @NotNull PlcConnection awaitRunningConnect(final @NotNull DriverConnection connection) {
        final long deadline = System.nanoTime() + TIMEOUT.toNanos();
        PlcConnection running = connection.runningConnect();
        while (running == null && System.nanoTime() < deadline) {
            Thread.onSpinWait();
            running = connection.runningConnect();
        }
        assertThat(running).isNotNull();
        return running;
    }

    private static @NotNull CompletableFuture<Void> startAsync(final @NotNull DriverConnection connection) {
        return CompletableFuture.runAsync(() -> {
            try {
                connection.startConnection(mock(EventService.class, RETURNS_DEEP_STUBS), "test", connection.protocol);
            } catch (final Plc4xException expected) {
                // The disconnect ends the attempt.
            }
        });
    }

    private @NotNull DriverConnection s7() throws Plc4xException {
        return new DriverConnection("s7", endpoint.port(), "controller-type=S7_1500");
    }

    private @NotNull DriverConnection ads() throws Plc4xException {
        return new DriverConnection(
                "ads",
                endpoint.port(),
                "target-ams-net-id=127.0.0.1.1.1&target-ams-port=851&source-ams-net-id=127.0.0.1.1.2"
                        + "&source-ams-port=65534");
    }

    /** Accepts TCP and never answers; counts the connections it holds open. */
    private static final class SilentEndpoint implements AutoCloseable {

        final @NotNull AtomicInteger open = new AtomicInteger();
        private final @NotNull ServerSocket server = new ServerSocket(0);

        SilentEndpoint() throws IOException {
            final Thread acceptor = new Thread(
                    () -> {
                        while (!server.isClosed()) {
                            try {
                                final Socket socket = server.accept();
                                open.incrementAndGet();
                                final Thread reader = new Thread(
                                        () -> {
                                            try (socket;
                                                    InputStream in = socket.getInputStream()) {
                                                while (in.read() >= 0) {
                                                    // discard
                                                }
                                            } catch (final IOException ignored) {
                                                // closed by the peer
                                            } finally {
                                                open.decrementAndGet();
                                            }
                                        },
                                        "silent-endpoint-reader");
                                reader.setDaemon(true);
                                reader.start();
                            } catch (final IOException e) {
                                return;
                            }
                        }
                    },
                    "silent-endpoint");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return server.getLocalPort();
        }

        @Override
        public void close() throws IOException {
            server.close();
        }
    }

    private static final class DriverConnection extends Plc4xConnection<Plc4XSpecificAdapterConfig<Plc4xToMqttConfig>> {

        final @NotNull String protocol;

        DriverConnection(final @NotNull String protocol, final int port, final @NotNull String query)
                throws Plc4xException {
            super(
                    PlcDriverManager.getDefault(),
                    new Plc4XSpecificAdapterConfig<>(port, "127.0.0.1") {
                        @Override
                        public @Nullable Plc4xToMqttConfig getPlc4xToMqttConfig() {
                            return null;
                        }
                    },
                    config -> query,
                    Duration.ofMinutes(1));
            this.protocol = protocol;
        }

        @Override
        protected @NotNull String getProtocol() {
            return protocol;
        }

        @Override
        protected @NotNull String getTagAddressForSubscription(final @NotNull Plc4xTag tag) {
            return "";
        }
    }
}
