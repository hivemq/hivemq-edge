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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
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
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.PlcDriver;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
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
        await().atMost(TIMEOUT)
                .untilAsserted(() -> verify(hanging.connection, atLeastOnce()).close());
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

        await().atMost(TIMEOUT)
                .untilAsserted(() -> verify(hanging.connection, atLeastOnce()).close());
        await().atMost(TIMEOUT).untilTrue(hanging.interrupted);
        assertThat(attempt.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isInstanceOf(Plc4xException.class)
                .hasMessageContaining("closed while connecting");
        assertThat(connection.isConnected()).isFalse();
    }

    @Test
    void test_disconnect_beforeTheDriverInstallsItsRetryTask_stillClosesTheConnectOnceItEnds() throws Exception {
        final DriverOrderedConnect driver = new DriverOrderedConnect();
        final TestConnection connection = new TestConnection(driverManager(driver.connection), Duration.ofMinutes(1));
        final CompletableFuture<Throwable> attempt = startAsync(connection);
        assertThat(driver.tcpUp.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isTrue();

        // STOP lands after the TCP connect and before the driver installs its retry task: that close fails.
        connection.disconnect();
        driver.proceed.countDown();

        await().atMost(TIMEOUT).until(() -> connection.connectTerminated().isDone());
        assertThat(attempt.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)).isInstanceOf(Plc4xException.class);
        await().atMost(TIMEOUT)
                .alias("the retry task the driver installed after the failed close must be stopped")
                .untilAsserted(() -> assertThat(driver.retryLoopRunning).isFalse());
    }

    @Test
    void test_timeout_beforeTheDriverInstallsItsRetryTask_stillClosesTheConnectOnceItEnds() throws Exception {
        final DriverOrderedConnect driver = new DriverOrderedConnect();
        final TestConnection connection = new TestConnection(driverManager(driver.connection));
        final CompletableFuture<Throwable> attempt = startAsync(connection);
        assertThat(driver.tcpUp.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isTrue();

        assertThat(attempt.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isInstanceOf(Plc4xException.class)
                .hasMessageContaining("Timed out");
        assertThat(connection.connectTerminated())
                .as("a connect that ignores the interrupt has not ended, so no other may start")
                .isNotDone();
        driver.proceed.countDown();

        await().atMost(TIMEOUT).until(() -> connection.connectTerminated().isDone());
        await().atMost(TIMEOUT)
                .alias("the retry task the driver installed after the failed close must be stopped")
                .untilAsserted(() -> assertThat(driver.retryLoopRunning).isFalse());
    }

    @Test
    void test_disconnect_beforeTheConnectTaskRuns_endsTheAttemptSoAnotherCanStart() throws Exception {
        final ThreadPoolExecutor executor =
                new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        final CountDownLatch busy = new CountDownLatch(1);
        executor.execute(() -> {
            try {
                busy.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        try {
            final PlcConnection plcConnection = mock(PlcConnection.class);
            final TestConnection connection =
                    new TestConnection(driverManager(plcConnection), Duration.ofMinutes(1), executor);
            final CompletableFuture<Throwable> attempt = startAsync(connection);
            await().atMost(TIMEOUT).until(() -> executor.getQueue().size() == 1);

            connection.disconnect();

            await().atMost(TIMEOUT)
                    .alias("a connect cancelled before it ran has ended, or no later attempt may start")
                    .until(() -> connection.connectTerminated().isDone());
            assertThat(attempt.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                    .isInstanceOf(Plc4xException.class)
                    .hasMessageContaining("closed while connecting");
            busy.countDown();
            verify(plcConnection, times(0)).connect();
        } finally {
            busy.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void test_timeout_whileASucceededConnectIsHandedOver_closesTheConnection() throws Exception {
        // The connect succeeds, but its result reaches the waiter only after the waiter has timed out.
        final CountDownLatch resultMayArrive = new CountDownLatch(1);
        final ThreadPoolExecutor executor =
                new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>()) {
                    @Override
                    protected <V> RunnableFuture<V> newTaskFor(final @NotNull Callable<V> callable) {
                        return new FutureTask<>(callable) {
                            @Override
                            protected void set(final V value) {
                                awaitIgnoringInterrupts(resultMayArrive);
                                super.set(value);
                            }
                        };
                    }
                };
        try {
            final PlcConnection plcConnection = mock(PlcConnection.class);
            when(plcConnection.isConnected()).thenReturn(true);
            final TestConnection connection =
                    new TestConnection(driverManager(plcConnection), CONNECT_TIMEOUT, executor);

            assertThatThrownBy(
                            () -> connection.startConnection(mock(EventService.class, RETURNS_DEEP_STUBS), "t", "s7"))
                    .isInstanceOf(Plc4xException.class)
                    .hasMessageContaining("Timed out");
            resultMayArrive.countDown();

            await().atMost(TIMEOUT)
                    .alias("a connection that connected after the attempt was abandoned is closed")
                    .untilAsserted(() -> verify(plcConnection, atLeastOnce()).close());
            assertThat(connection.isConnected()).isFalse();
        } finally {
            resultMayArrive.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void test_timeout_withASlowDriverClose_endsTheAttemptBeforeTheCloseReturns() throws Exception {
        // ADS's close() waits up to 2 s for its event loop; until the attempt has ended, the adapter refuses new
        // attempts and each refused poll counts as a polling error.
        final HangingConnect hanging = new HangingConnect();
        final CountDownLatch closeMayReturn = new CountDownLatch(1);
        doAnswer(invocation -> {
                    closeMayReturn.await();
                    return null;
                })
                .when(hanging.connection)
                .close();
        final TestConnection connection = new TestConnection(driverManager(hanging.connection));
        try {
            assertThatThrownBy(
                            () -> connection.startConnection(mock(EventService.class, RETURNS_DEEP_STUBS), "t", "s7"))
                    .isInstanceOf(Plc4xException.class)
                    .hasMessageContaining("Timed out");

            await().atMost(TIMEOUT)
                    .alias("the attempt has ended once connect() returned, while the driver is still closing")
                    .until(() -> connection.connectTerminated().isDone());
            await().atMost(TIMEOUT)
                    .untilAsserted(() -> verify(hanging.connection).close());
        } finally {
            closeMayReturn.countDown();
        }
    }

    @Test
    void test_disconnect_whileAReadIsInFlight_failsTheRead() throws Exception {
        // The driver drops the reads of a connection it closes; without this the poll's output waits for the
        // sampler's 10 s timeout.
        final PlcConnection plcConnection = mock(PlcConnection.class, RETURNS_DEEP_STUBS);
        when(plcConnection.isConnected()).thenReturn(true);
        when(plcConnection.getMetadata().isReadSupported()).thenReturn(true);
        final PlcReadRequest request = plcConnection.readRequestBuilder().build();
        doReturn(new CompletableFuture<PlcReadResponse>()).when(request).execute();
        final TestConnection connection = new TestConnection(driverManager(plcConnection));
        connection.startConnection(mock(EventService.class), "testS7", "s7");
        final CompletableFuture<? extends PlcReadResponse> read =
                connection.read(List.of(mock(Plc4xTag.class, RETURNS_DEEP_STUBS)));
        assertThat(read).isNotDone();

        connection.disconnect();

        assertThat(read).isCompletedExceptionally();
        assertThatThrownBy(read::join).hasCauseInstanceOf(Plc4xException.class);
    }

    @Test
    void test_read_whenTheDriverThrowsOnSend_keepsNoReadInFlight() throws Exception {
        final PlcConnection plcConnection = mock(PlcConnection.class, RETURNS_DEEP_STUBS);
        when(plcConnection.isConnected()).thenReturn(true);
        when(plcConnection.getMetadata().isReadSupported()).thenReturn(true);
        final PlcReadRequest request = plcConnection.readRequestBuilder().build();
        doThrow(new IllegalStateException("cannot send")).when(request).execute();
        final TestConnection connection = new TestConnection(driverManager(plcConnection));
        connection.startConnection(mock(EventService.class), "testS7", "s7");

        for (int poll = 0; poll < 3; poll++) {
            assertThatThrownBy(() -> connection.read(List.of(mock(Plc4xTag.class, RETURNS_DEEP_STUBS))))
                    .isInstanceOf(IllegalStateException.class);
        }

        assertThat(connection.readsInFlight())
                .as("a read never sent is not tracked")
                .isZero();
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

    /** Neither throws on an interrupt nor clears the flag, as Netty's awaitUninterruptibly; gives up after TIMEOUT. */
    private static void awaitIgnoringInterrupts(final @NotNull CountDownLatch latch) {
        final long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (latch.getCount() > 0 && System.nanoTime() < deadline) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
        }
    }

    private static @NotNull CompletableFuture<Throwable> startAsync(final @NotNull TestConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                connection.startConnection(mock(EventService.class, RETURNS_DEEP_STUBS), "testS7", "s7");
                return null;
            } catch (final Plc4xException e) {
                return e;
            }
        });
    }

    /**
     * A driver connection ordered as the PLC4X 0.13.1 S7 driver's: connect() opens the TCP channel (not interruptible
     * here), then installs a retry task that reconnects once a second, then fails if interrupted; close() fails before
     * the retry task exists, and stops it once it does.
     */
    private static final class DriverOrderedConnect {

        final @NotNull PlcConnection connection = mock(PlcConnection.class);
        final @NotNull CountDownLatch tcpUp = new CountDownLatch(1);
        final @NotNull CountDownLatch proceed = new CountDownLatch(1);
        volatile boolean retryTaskInstalled;
        volatile boolean retryLoopRunning;

        DriverOrderedConnect() throws Exception {
            doAnswer(invocation -> {
                        tcpUp.countDown();
                        // The TCP phase ignores interrupts and keeps the flag, as Netty's awaitUninterruptibly does.
                        awaitIgnoringInterrupts(proceed);
                        retryTaskInstalled = true;
                        retryLoopRunning = true;
                        if (Thread.currentThread().isInterrupted()) {
                            throw new PlcConnectionException(new InterruptedException());
                        }
                        return null;
                    })
                    .when(connection)
                    .connect();
            doAnswer(invocation -> {
                        if (!retryTaskInstalled) {
                            throw new NullPointerException("the retry task is not scheduled yet");
                        }
                        retryLoopRunning = false;
                        return null;
                    })
                    .when(connection)
                    .close();
        }
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
            this(driverManager, connectTimeout, Plc4xConnection.connectExecutor());
        }

        TestConnection(
                final @NotNull PlcDriverManager driverManager,
                final @NotNull Duration connectTimeout,
                final @NotNull ExecutorService executor)
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
                    connectTimeout,
                    executor);
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
