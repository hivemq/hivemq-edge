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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriver;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * Covers the connect path: the connection object is owned before any I/O, the connect runs under a timeout that
 * always applies, and on timeout, failure or disconnect the object is closed and the connect interrupted, so no
 * driver connect or retry loop outlives the attempt.
 */
class Plc4xConnectionTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(5);
    private static final @NotNull Duration CONNECT_TIMEOUT = Duration.ofMillis(200);

    @Test
    void test_startConnection_whenTheDeviceRefuses_carriesTheDriversCauseAndClosesTheConnection() throws Exception {
        final PlcConnection plcConnection = mock(PlcConnection.class);
        doThrow(new PlcConnectionException("Connection refused: localhost/127.0.0.1:102"))
                .when(plcConnection)
                .connect();
        final TestConnection connection = new TestConnection(driverManager(plcConnection));

        assertThatThrownBy(() -> connection.startConnection(mock(EventService.class), "testS7", "s7"))
                .isInstanceOf(Plc4xException.class)
                .hasMessageContaining("Connection refused")
                .hasCauseInstanceOf(PlcConnectionException.class);
        assertThat(connection.isConnected()).isFalse();
        verify(plcConnection).close();
        assertThat(connection.connectTerminated()).isDone();
    }

    @Test
    void test_startConnection_whenTheDeviceAccepts_keepsTheConnection() throws Exception {
        final PlcConnection plcConnection = mock(PlcConnection.class);
        when(plcConnection.isConnected()).thenReturn(true);
        final TestConnection connection = new TestConnection(driverManager(plcConnection));

        connection.startConnection(mock(EventService.class), "testS7", "s7");

        assertThat(connection.isConnected()).isTrue();
        verify(plcConnection, times(0)).close();
    }

    @Test
    void test_startConnection_whenTheConnectNeverReturns_timesOutClosesAndInterruptsIt() throws Exception {
        final HangingConnect hanging = new HangingConnect();
        final TestConnection connection = new TestConnection(driverManager(hanging.connection));
        final long started = System.nanoTime();

        assertThatThrownBy(
                        () -> connection.startConnection(mock(EventService.class, RETURNS_DEEP_STUBS), "testS7", "s7"))
                .isInstanceOf(Plc4xException.class)
                .hasMessageContaining("Timed out");

        assertThat(Duration.ofNanos(System.nanoTime() - started))
                .as("the timeout applies even though nothing ever completes the connect")
                .isLessThan(TIMEOUT);
        verify(hanging.connection).close();
        await().atMost(TIMEOUT).untilTrue(hanging.interrupted);
        await().atMost(TIMEOUT).until(() -> connection.connectTerminated().isDone());
        assertThat(connection.isConnected()).isFalse();
    }

    @Test
    void test_disconnect_whileConnecting_closesAndInterruptsTheConnectWithoutWaitingForIt() throws Exception {
        final HangingConnect hanging = new HangingConnect();
        final TestConnection connection = new TestConnection(driverManager(hanging.connection), Duration.ofMinutes(1));
        final CompletableFuture<Throwable> attempt = CompletableFuture.supplyAsync(() -> {
            try {
                connection.startConnection(mock(EventService.class), "testS7", "s7");
                return null;
            } catch (final Plc4xException e) {
                return e;
            }
        });
        assertThat(hanging.entered.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isTrue();

        connection.disconnect();

        verify(hanging.connection).close();
        await().atMost(TIMEOUT).untilTrue(hanging.interrupted);
        assertThat(attempt.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isInstanceOf(Plc4xException.class)
                .hasMessageContaining("closed while connecting");
        assertThat(connection.isConnected()).isFalse();
    }

    @Test
    void test_startConnection_afterDisconnect_refusesToConnect() throws Exception {
        final PlcConnection plcConnection = mock(PlcConnection.class);
        final TestConnection connection = new TestConnection(driverManager(plcConnection));

        connection.disconnect();

        assertThatThrownBy(() -> connection.startConnection(mock(EventService.class), "testS7", "s7"))
                .isInstanceOf(Plc4xException.class)
                .hasMessageContaining("closed before it was started");
        verify(plcConnection, times(0)).connect();
    }

    @Test
    void test_lazyConnectionCheck_whenTheConnectionWasLost_failsInsteadOfReconnectingWithoutATimeout()
            throws Exception {
        final PlcConnection plcConnection = mock(PlcConnection.class);
        when(plcConnection.isConnected()).thenReturn(true);
        final TestConnection connection = new TestConnection(driverManager(plcConnection));
        connection.startConnection(mock(EventService.class), "testS7", "s7");
        when(plcConnection.isConnected()).thenReturn(false);

        assertThatThrownBy(connection::lazyConnectionCheck).isInstanceOf(IllegalStateException.class);
        verify(plcConnection, times(1)).connect();
    }

    private static @NotNull PlcDriverManager driverManager(final @NotNull PlcConnection plcConnection)
            throws PlcConnectionException {
        final PlcDriver driver = mock(PlcDriver.class);
        when(driver.getConnection(anyString())).thenReturn(plcConnection);
        final PlcDriverManager driverManager = mock(PlcDriverManager.class);
        when(driverManager.getDriverForUrl(anyString())).thenReturn(driver);
        return driverManager;
    }

    /** A driver connection whose connect() blocks until interrupted, as against a silent S7 endpoint. */
    private static final class HangingConnect {

        final @NotNull PlcConnection connection = mock(PlcConnection.class);
        final @NotNull CountDownLatch entered = new CountDownLatch(1);
        final @NotNull AtomicBoolean interrupted = new AtomicBoolean();

        HangingConnect() throws PlcConnectionException {
            doAnswer(invocation -> {
                        entered.countDown();
                        try {
                            new CountDownLatch(1).await();
                        } catch (final InterruptedException e) {
                            interrupted.set(true);
                            throw new PlcConnectionException(e);
                        }
                        return null;
                    })
                    .when(connection)
                    .connect();
        }
    }

    private static final class TestConnection extends Plc4xConnection<S7SpecificAdapterConfig> {

        TestConnection(final @NotNull PlcDriverManager driverManager) throws Plc4xException {
            this(driverManager, CONNECT_TIMEOUT);
        }

        TestConnection(final @NotNull PlcDriverManager driverManager, final @NotNull Duration connectTimeout)
                throws Plc4xException {
            super(
                    driverManager,
                    new S7SpecificAdapterConfig(
                            102,
                            "localhost",
                            S7SpecificAdapterConfig.ControllerType.S7_1500,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null),
                    config -> "",
                    connectTimeout);
        }

        @Override
        protected @NotNull String getProtocol() {
            return "s7";
        }

        @Override
        protected @NotNull String getTagAddressForSubscription(final @NotNull Plc4xTag tag) {
            return tag.getDefinition().getTagAddress();
        }
    }
}
