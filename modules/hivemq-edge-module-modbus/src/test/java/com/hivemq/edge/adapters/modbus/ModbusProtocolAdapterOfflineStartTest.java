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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.digitalpetri.modbus.ExceptionCode;
import com.digitalpetri.modbus.FunctionCode;
import com.digitalpetri.modbus.exceptions.ModbusResponseException;
import com.digitalpetri.modbus.exceptions.UnknownUnitIdException;
import com.digitalpetri.modbus.pdu.ReadCoilsRequest;
import com.digitalpetri.modbus.pdu.ReadCoilsResponse;
import com.digitalpetri.modbus.server.ModbusRequestContext;
import com.digitalpetri.modbus.server.ModbusTcpServer;
import com.digitalpetri.modbus.server.ProcessImage;
import com.digitalpetri.modbus.server.ReadWriteModbusServices;
import com.digitalpetri.modbus.tcp.server.NettyTcpServerTransport;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointBuilder;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.modbus.config.ModbusAdu;
import com.hivemq.edge.adapters.modbus.config.ModbusSpecificAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.ModbusToMqttConfig;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTag;
import com.hivemq.edge.adapters.modbus.config.tag.ModbusTagDefinition;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.polling.PollingOutputImpl;
import com.hivemq.edge.modules.adapters.impl.polling.PollingTask;
import com.hivemq.protocols.AbstractSubscriptionSampler;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import com.hivemq.protocols.northbound.PerAdapterSampler;
import com.hivemq.util.NanoTimeProvider;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

/// A Modbus device that is off when the adapter starts must not leave the adapter in ERROR for good: the start
/// succeeds, polls fail at once while the device is away and count as polling errors like an outage does, data flows
/// once the device is reachable with no restart, and a stop ends the client's reconnect attempts. A connection that
/// drops after it was established gets the client's first reconnect attempt before a poll fails for it.
class ModbusProtocolAdapterOfflineStartTest {

    private static final @NotNull String ADAPTER_ID = "modbus-offline-start";
    private static final int UNIT_ID = 1;
    private static final int COIL_ADDRESS = 0;
    private static final int UNLIMITED_POLLING_ERRORS = -1;
    // The module's minimum; a refused loopback connect fails at once, well inside POLL_WAIT_SECONDS.
    private static final int CONNECT_TIMEOUT_MILLIS = 1_000;
    // Polls every 100 ms, so a test runs through many polls in a few seconds.
    private static final int POLLING_INTERVAL_MILLIS = 100;
    // Production's watchdog settings with the polling backoff capped at 200 ms instead of 10 minutes, so failed
    // polls follow each other quickly.
    private static final @NotNull PollingTask.Settings FAST_BACKOFF = new PollingTask.Settings(
            PollingTask.Settings.DEFAULTS.watchdogTimeoutMillis(),
            PollingTask.Settings.DEFAULTS.watchdogErrorsBeforeStop(),
            200);
    // The client's reconnect delay doubles from 1 s; 60 s covers several attempts after the device comes up.
    private static final @NotNull Duration RECONNECT_WAIT = Duration.ofSeconds(60);
    // A client still reconnecting tries again within 1 s and then 2 s; 5 s of silence shows that it stopped.
    private static final @NotNull Duration QUIET_WINDOW = Duration.ofSeconds(5);
    // A single poll against a local device answers in milliseconds; 10 s only bounds a hung read.
    private static final long POLL_WAIT_SECONDS = 10;
    // The client's first reconnect after a drop comes 1 s after it; 3 s covers it and the poll that follows, and is
    // long enough for an adapter that fails its polls at once to pass a limit of 1 at a 100 ms interval.
    private static final @NotNull Duration FIRST_RECONNECT_WINDOW = Duration.ofSeconds(3);
    // The sampler times a poll out at 10 s, past the client's first reconnect 1 s after a drop; a waiting poll timed
    // out after 600 ms ends well before that reconnect, as one that waits longer than 10 s for it does.
    private static final long WAITING_POLL_TIMEOUT_MILLIS = 600;
    // The sampler timeout the adapter is told in that test: no longer than WAITING_POLL_TIMEOUT_MILLIS, as production's
    // equals the sampler's 10 s, and far above a loopback read.
    private static final long SHORT_SAMPLER_TIMEOUT_MILLIS = 500;
    // A consumer that holds the publish longer than SHORT_SAMPLER_TIMEOUT_MILLIS, so the adapter cannot tell whether
    // the sampler took it.
    private static final long SLOW_CONSUMER_MILLIS = 700;
    // A device that rejects every read and closes the connection costs the adapter one reconnect, 1 s, per rejected
    // read; 20 s covers four of them with room for a slow machine.
    private static final @NotNull Duration REJECTIONS_WINDOW = Duration.ofSeconds(20);
    // The device closes the connection this long after it rejects a read, so the rejection reaches the client first.
    private static final long CLOSE_AFTER_REJECTION_MILLIS = 20;
    // The client sees a drop a few milliseconds after the device closes the connection; 5 s only bounds a hang.
    private static final @NotNull Duration DROP_SEEN_WITHIN = Duration.ofSeconds(5);

    private final @NotNull ProtocolAdapterState state = mock(ProtocolAdapterState.class);
    // When set, the device rejects every read with ILLEGAL_DATA_ADDRESS and then closes the connection.
    private final @NotNull AtomicBoolean rejectAndClose = new AtomicBoolean();
    private final @NotNull AtomicInteger rejections = new AtomicInteger();
    private int port;
    private @Nullable ModbusProtocolAdapter adapter;
    private @Nullable ModbusTcpServer device;
    private @Nullable ScheduledExecutorService pollingExecutor;

    @BeforeEach
    void setUp() throws IOException {
        // A port that was free a moment ago; nothing listens on it until the test switches the device on.
        try (final ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        adapter = null;
        device = null;
        pollingExecutor = null;
    }

    @AfterEach
    void tearDown() throws Exception {
        if (pollingExecutor != null) {
            pollingExecutor.shutdownNow();
        }
        if (adapter != null) {
            adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));
        }
        if (device != null) {
            device.stop();
        }
    }

    /// Start with the device off, then switch it on: the adapter publishes the coil's value without a restart.
    @Test
    void startsWithTheDeviceOffAndPublishesOnceItIsOn() throws Exception {
        final ModbusProtocolAdapter offline = startWithTheDeviceOff(UNLIMITED_POLLING_ERRORS);

        // While the device is off, a poll fails at once instead of waiting for the sampler's timeout.
        final BatchPollingOutput offlineOutput = mock(BatchPollingOutput.class);
        offline.poll(mock(BatchPollingInput.class), offlineOutput);
        verify(offlineOutput).fail(anyString());
        final ArgumentCaptor<Throwable> cause = ArgumentCaptor.forClass(Throwable.class);
        verify(state).setErrorConnectionStatus(cause.capture(), isNull());
        assertThat(cause.getValue()).isInstanceOf(ConnectException.class);

        device = startDevice(port);

        await().atMost(RECONNECT_WAIT).pollInterval(Duration.ofMillis(500)).until(() -> pollPublishesTrue(offline));
    }

    /// A stop after a start without a connection disconnects the client: no reconnect reaches the device afterwards.
    @Test
    void stopAfterAStartWithTheDeviceOffEndsTheReconnectAttempts() throws Exception {
        final ModbusProtocolAdapter offline = startWithTheDeviceOff(UNLIMITED_POLLING_ERRORS);
        final ProtocolAdapterStopOutput stopOutput = mock(ProtocolAdapterStopOutput.class);

        offline.stop(mock(ProtocolAdapterStopInput.class), stopOutput);
        verify(stopOutput).stoppedSuccessfully();

        try (final ServerSocket listener = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout((int) QUIET_WINDOW.toMillis());
            assertThatThrownBy(listener::accept).isInstanceOf(SocketTimeoutException.class);
        }
    }

    /// Through the sampler, polls while the device is off count as polling errors: an adapter with a limit is stopped
    /// once the limit is passed, as it is for an outage while running.
    @Test
    void anAdapterWithAnErrorLimitIsStoppedOnceTheDeviceStaysOffPastIt() {
        final int limit = 3;
        final ProtocolAdapterWrapper wrapper = startPolling(startWithTheDeviceOff(limit));

        verify(wrapper, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS))).stopAsync(false);
        verify(wrapper, atLeast(limit + 1)).setErrorConnectionStatus(any(), isNull());
    }

    /// Through the sampler, an adapter without an error limit keeps polling past the default limit of 10 errors
    /// while the device is off, and publishes once the device is on, with no restart.
    @Test
    void anAdapterWithoutAnErrorLimitKeepsPollingAndPublishesOnceTheDeviceIsOn() throws Exception {
        final TagManager tagManager = mock(TagManager.class);
        final ProtocolAdapterWrapper wrapper =
                startPolling(startWithTheDeviceOff(UNLIMITED_POLLING_ERRORS), tagManager);

        verify(wrapper, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS)).atLeast(12))
                .setErrorConnectionStatus(any(), isNull());

        device = startDevice(port);

        verify(tagManager, timeout(RECONNECT_WAIT.toMillis())).feed(anyList());
        verify(wrapper, after(POLLING_INTERVAL_MILLIS).never()).stopAsync(anyBoolean());
    }

    /// Through the sampler, the error of a poll while the device is off carries the connect failure next to the
    /// device's address, so the status tells a refusal from a timeout or an unknown host.
    @Test
    void aPollWhileTheDeviceIsOffReportsTheConnectFailure() {
        final ProtocolAdapterWrapper wrapper = startPolling(startWithTheDeviceOff(UNLIMITED_POLLING_ERRORS));
        final ArgumentCaptor<Throwable> connectFailure = ArgumentCaptor.forClass(Throwable.class);
        verify(state).setErrorConnectionStatus(connectFailure.capture(), isNull());

        final ArgumentCaptor<Throwable> pollError = ArgumentCaptor.forClass(Throwable.class);
        verify(wrapper, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS)))
                .setErrorConnectionStatus(pollError.capture(), isNull());
        assertThat(pollError.getValue().getMessage())
                .contains(
                        "host=127.0.0.1",
                        "port=" + port,
                        "cause=" + connectFailure.getValue().getMessage());
    }

    /// Through the sampler, an adapter with an error limit of 1 survives a connection the device closes while it keeps
    /// listening: the poll after the drop waits for the client's first reconnect instead of failing at once, and
    /// data flows again on the new connection.
    @Test
    void aDroppedConnectionIsReconnectedBeforeAnErrorLimitOfOneStopsTheAdapter() throws Exception {
        final List<Channel> connections = new CopyOnWriteArrayList<>();
        device = startDeviceOnAnyPort(connections, coilImage(true));
        final TagManager tagManager = mock(TagManager.class);
        final ProtocolAdapterWrapper wrapper = startPolling(startAdapter(1), tagManager);
        verify(tagManager, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS)))
                .feed(anyList());
        assertThat(connections).hasSize(1);

        assertThat(connections.getFirst().close().await(POLL_WAIT_SECONDS, TimeUnit.SECONDS))
                .isTrue();

        verify(wrapper, after(FIRST_RECONNECT_WINDOW.toMillis()).never()).stopAsync(anyBoolean());
        clearInvocations(tagManager);
        verify(tagManager, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS)))
                .feed(anyList());
        // A read answered after the drop shows that the device holds the new connection.
        assertThat(connections).hasSize(2);
    }

    /// With `publishChangedDataOnly`, a coil that changes while a connection is down reaches the
    /// consumer on the first healthy poll after the poll that waited on the reconnect timed out: the
    /// timed-out poll does not record the new value as published. The waiting poll is timed out the
    /// sampler's way, by `orTimeout` on its output, before the client's first reconnect; the adapter
    /// is told a sampler timeout no longer than that, as in production.
    @Test
    void aChangeWhileTheConnectionIsDownIsPublishedOnceTheWaitingPollTimedOut() throws Exception {
        final List<Channel> connections = new CopyOnWriteArrayList<>();
        final ProcessImage image = coilImage(false);
        device = startDeviceOnAnyPort(connections, image);
        final ModbusProtocolAdapter started =
                startAdapter(UNLIMITED_POLLING_ERRORS, true, SHORT_SAMPLER_TIMEOUT_MILLIS);
        assertThat(publishedValues(started)).containsExactly(BooleanNode.FALSE);
        assertThat(connections).hasSize(1);

        assertThat(connections.getFirst().close().await(POLL_WAIT_SECONDS, TimeUnit.SECONDS))
                .isTrue();
        image.with(tx -> tx.writeCoils(coils -> coils.put(COIL_ADDRESS, true)));
        final RecordingOutput waiting =
                await().atMost(DROP_SEEN_WITHIN).until(() -> waitingPoll(started), Objects::nonNull);
        final CompletableFuture<PollingOutputImpl.PollingResult> expired =
                waiting.getOutputFuture().orTimeout(WAITING_POLL_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        assertThatThrownBy(() -> expired.get(POLL_WAIT_SECONDS, TimeUnit.SECONDS))
                .hasCauseInstanceOf(TimeoutException.class);
        // The waiting poll ends once the reconnect does; the client is connected again from then on.
        waiting.ended.get(POLL_WAIT_SECONDS, TimeUnit.SECONDS);

        assertThat(publishedValues(started)).containsExactly(BooleanNode.TRUE);
    }

    /// Through the sampler, polling errors keep counting across reconnects until a read succeeds: a
    /// device that rejects every read with ILLEGAL_DATA_ADDRESS and closes the connection stops the
    /// adapter once its error limit is passed, though each rejection costs a reconnect.
    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void aDeviceThatRejectsEveryReadAndClosesStopsTheAdapterPastItsLimit(final int limit) throws Exception {
        device = startDeviceOnAnyPort(new CopyOnWriteArrayList<>(), coilImage(true));
        final TagManager tagManager = mock(TagManager.class);
        final ProtocolAdapterWrapper wrapper = startPolling(startAdapter(limit), tagManager);
        verify(tagManager, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS)))
                .feed(anyList());

        rejectAndClose.set(true);

        verify(wrapper, timeout(REJECTIONS_WINDOW.toMillis())).stopAsync(false);
        verify(wrapper, atLeast(limit + 1)).setErrorConnectionStatus(any(), isNull());
        assertThat(rejections).hasPositiveValue();
    }

    /// With `publishChangedDataOnly`, a coil that goes `false`, `true`, `false` reaches the consumer as all three
    /// values though the consumer holds the publish of `true` past the sampler's timeout: the adapter cannot tell
    /// whether that publish was taken, so it forgets the value instead of keeping the one before it.
    @Test
    void aSlowConsumerStillGetsACoilThatChangesBack() throws Exception {
        assertACoilThatChangesBackReachesTheConsumer(
                () -> Thread.sleep(SLOW_CONSUMER_MILLIS), SHORT_SAMPLER_TIMEOUT_MILLIS);
    }

    /// With `publishChangedDataOnly`, a coil that goes `false`, `true`, `false` reaches the consumer as all three
    /// values though the consumer fails after it received `true`: the publish throws, the adapter cannot tell what
    /// the consumer kept, so it forgets the value instead of keeping the one before it.
    @Test
    void aConsumerThatFailsStillGetsACoilThatChangesBack() throws Exception {
        assertACoilThatChangesBackReachesTheConsumer(
                () -> {
                    throw new IllegalStateException("The consumer failed");
                },
                ModbusProtocolAdapter.SAMPLER_TIMEOUT_MILLIS);
    }

    /// Publishes `false`, then `true` to `consumerOfTrue`, then checks that the next poll publishes `false` again.
    private void assertACoilThatChangesBackReachesTheConsumer(
            final @NotNull Consumer consumerOfTrue, final long samplerTimeoutMillis) throws Exception {
        final ProcessImage image = coilImage(false);
        device = startDeviceOnAnyPort(new CopyOnWriteArrayList<>(), image);
        final ModbusProtocolAdapter started = startAdapter(UNLIMITED_POLLING_ERRORS, true, samplerTimeoutMillis);
        assertThat(publishedValues(started)).containsExactly(BooleanNode.FALSE);

        image.with(tx -> tx.writeCoils(coils -> coils.put(COIL_ADDRESS, true)));
        final RecordingOutput ofTrue = new RecordingOutput(consumerOfTrue);
        started.poll(mock(BatchPollingInput.class), ofTrue);
        ofTrue.ended.get(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        awaitTheCallbackReturned(ofTrue);
        assertThat(valuesOf(ofTrue)).containsExactly(BooleanNode.TRUE);

        image.with(tx -> tx.writeCoils(coils -> coils.put(COIL_ADDRESS, false)));
        assertThat(publishedValues(started)).containsExactly(BooleanNode.FALSE);
    }

    /// With `publishChangedDataOnly`, the first poll after a stop and a start publishes the coil even
    /// when a poll of the previous run completes after the start: each run has its own change cache,
    /// so the late poll cannot record its value in the new run's.
    @Test
    void aPollCompletingAfterARestartDoesNotSuppressTheFirstSample() throws Exception {
        device = startDeviceOnAnyPort(new CopyOnWriteArrayList<>(), coilImage(true));
        final ModbusProtocolAdapter started = startAdapter(UNLIMITED_POLLING_ERRORS, true);
        final CountDownLatch consumed = new CountDownLatch(1);
        final RecordingOutput late = new RecordingOutput(() -> consumed.await(POLL_WAIT_SECONDS, TimeUnit.SECONDS));
        started.poll(mock(BatchPollingInput.class), late);
        late.getOutputFuture().get(POLL_WAIT_SECONDS, TimeUnit.SECONDS);

        restart(started);
        consumed.countDown();
        awaitTheCallbackReturned(late);

        assertThat(publishedValues(started)).containsExactly(BooleanNode.TRUE);
    }

    /// A poll waiting on the reconnect when the adapter restarts retires without data: its run ended, so it
    /// reports no polling error. The old run's sampler would otherwise count the stop's cancelled reconnect
    /// and, past its limit of 0, stop the adapter it shares with the new run.
    @Test
    void aPollWaitingOnTheReconnectWhenTheAdapterRestartsReportsNoError() throws Exception {
        final List<Channel> connections = new CopyOnWriteArrayList<>();
        device = startDeviceOnAnyPort(connections, coilImage(true));
        final ModbusProtocolAdapter started = startAdapter(0);
        assertThat(pollPublishesTrue(started)).isTrue();
        assertThat(connections.getFirst().close().await(POLL_WAIT_SECONDS, TimeUnit.SECONDS))
                .isTrue();
        final RecordingOutput waiting =
                await().atMost(DROP_SEEN_WITHIN).until(() -> waitingPoll(started), Objects::nonNull);

        restart(started);

        assertThat(waiting.getOutputFuture().get(POLL_WAIT_SECONDS, TimeUnit.SECONDS))
                .isEqualTo(PollingOutputImpl.PollingResult.NO_DATA);
        assertThat(pollPublishesTrue(started)).isTrue();
    }

    /// A read still in flight at a stop retires without data and leaves the connection status alone: the device stays
    /// silent, so the read fails after the stop, at the client's request timeout.
    @Test
    void aReadInFlightAtAStopReportsNoError() throws Exception {
        final List<Socket> accepted = new CopyOnWriteArrayList<>();
        try (final ServerSocket silent = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = silent.getLocalPort();
            Thread.ofVirtual().start(() -> acceptAndStaySilent(silent, accepted));
            final ModbusProtocolAdapter started = startAdapter(0);
            final RecordingOutput reading =
                    await().atMost(DROP_SEEN_WITHIN).until(() -> readingPoll(started), Objects::nonNull);

            started.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));

            assertThat(reading.getOutputFuture().get(POLL_WAIT_SECONDS, TimeUnit.SECONDS))
                    .isEqualTo(PollingOutputImpl.PollingResult.NO_DATA);
            verify(state, never()).setConnectionStatus(ERROR);
        } finally {
            for (final Socket socket : accepted) {
                socket.close();
            }
        }
    }

    /// The sampler timeout the adapter assumes is no longer than the sampler's own, so a publish the
    /// adapter records in its change cache was taken by the sampler.
    @Test
    void theAdaptersSamplerTimeoutIsNoLongerThanTheSamplers() {
        assertThat(ModbusProtocolAdapter.SAMPLER_TIMEOUT_MILLIS)
                .isLessThanOrEqualTo(AbstractSubscriptionSampler.POLL_OUTPUT_TIMEOUT_MILLIS);
    }

    /// Stops `started` and starts it again, as an operator's restart does, and checks that the start succeeds.
    private static void restart(final @NotNull ModbusProtocolAdapter started) {
        started.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));
        final ProtocolAdapterStartOutput restarted = mock(ProtocolAdapterStartOutput.class);
        started.start(mock(ProtocolAdapterStartInput.class), restarted);
        verify(restarted, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS))).startedSuccessfully();
    }

    /// Accepts every connection on `silent` into `accepted` and never answers, until `silent` closes.
    private static void acceptAndStaySilent(final @NotNull ServerSocket silent, final @NotNull List<Socket> accepted) {
        try {
            while (!silent.isClosed()) {
                accepted.add(silent.accept());
            }
        } catch (final IOException _) {
            // The test closed the socket.
        }
    }

    /// Builds the adapter with the given polling-error limit, starts it with nothing listening on its port, and
    /// checks that the start succeeds.
    private @NotNull ModbusProtocolAdapter startWithTheDeviceOff(final int maxPollingErrorsBeforeRemoval) {
        return startAdapter(maxPollingErrorsBeforeRemoval);
    }

    /// Builds the adapter with the given polling-error limit, publishing every value, starts it, and checks that the
    /// start succeeds.
    private @NotNull ModbusProtocolAdapter startAdapter(final int maxPollingErrorsBeforeRemoval) {
        return startAdapter(maxPollingErrorsBeforeRemoval, false);
    }

    /// Builds the adapter with the given polling-error limit and change-only publishing, starts it, and checks that
    /// the start succeeds.
    private @NotNull ModbusProtocolAdapter startAdapter(
            final int maxPollingErrorsBeforeRemoval, final boolean publishChangedDataOnly) {
        return startAdapter(
                maxPollingErrorsBeforeRemoval, publishChangedDataOnly, ModbusProtocolAdapter.SAMPLER_TIMEOUT_MILLIS);
    }

    /// Builds the adapter with the given polling-error limit, change-only publishing and assumed sampler
    /// timeout, starts it, and checks that the start succeeds.
    private @NotNull ModbusProtocolAdapter startAdapter(
            final int maxPollingErrorsBeforeRemoval,
            final boolean publishChangedDataOnly,
            final long samplerTimeoutMillis) {
        final ModbusTag tag = new ModbusTag(
                "coil", null, new ModbusTagDefinition(COIL_ADDRESS, ModbusAdu.COILS, UNIT_ID, false, null));
        final ModbusSpecificAdapterConfig config = new ModbusSpecificAdapterConfig(
                port,
                "127.0.0.1",
                CONNECT_TIMEOUT_MILLIS,
                new ModbusToMqttConfig(POLLING_INTERVAL_MILLIS, maxPollingErrorsBeforeRemoval, publishChangedDataOnly));
        @SuppressWarnings("unchecked")
        final ProtocolAdapterInput<ModbusSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn(ADAPTER_ID);
        when(input.getConfig()).thenReturn(config);
        when(input.getProtocolAdapterState()).thenReturn(state);
        when(input.getTags()).thenReturn(List.of(tag));
        final ModbusProtocolAdapter started =
                new ModbusProtocolAdapter(mock(ProtocolAdapterInformation.class), input, samplerTimeoutMillis);
        adapter = started;

        final ProtocolAdapterStartOutput startOutput = mock(ProtocolAdapterStartOutput.class);
        started.start(mock(ProtocolAdapterStartInput.class), startOutput);
        verify(startOutput, timeout(TimeUnit.SECONDS.toMillis(POLL_WAIT_SECONDS)))
                .startedSuccessfully();
        verify(startOutput, never()).failStart(any(), any());
        return started;
    }

    private @NotNull ProtocolAdapterWrapper startPolling(final @NotNull ModbusProtocolAdapter started) {
        return startPolling(started, mock(TagManager.class));
    }

    /// Polls the adapter the way Edge does, through its sampler and a polling task, and returns the mocked wrapper the
    /// sampler reports to.
    private @NotNull ProtocolAdapterWrapper startPolling(
            final @NotNull ModbusProtocolAdapter started, final @NotNull TagManager tagManager) {
        final ProtocolAdapterInformation information = mock(ProtocolAdapterInformation.class);
        when(information.getProtocolId()).thenReturn("modbus");
        final ProtocolAdapterWrapper wrapper = mock(ProtocolAdapterWrapper.class);
        when(wrapper.getId()).thenReturn(ADAPTER_ID);
        when(wrapper.getAdapter()).thenReturn(started);
        when(wrapper.getProtocolAdapterInformation()).thenReturn(information);
        final EventService eventService = mock(EventService.class, RETURNS_DEEP_STUBS);
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                runnable -> new Thread(runnable, "modbus-offline-start-test-polling"));
        pollingExecutor = executor;
        // The provider's constructor is package-private (it is injected); the mock answers the real clock.
        final NanoTimeProvider clock = mock(NanoTimeProvider.class);
        when(clock.nanoTime()).thenAnswer(_ -> System.nanoTime());
        final PollingTask task = new PollingTask(
                new PerAdapterSampler(wrapper, eventService, tagManager), executor, eventService, clock, FAST_BACKOFF);
        executor.execute(task);
        return wrapper;
    }

    /// Polls once and answers whether that poll published `true` for the coil; a failed poll answers `false`.
    private static boolean pollPublishesTrue(final @NotNull ModbusProtocolAdapter started) throws Exception {
        final CompletableFuture<Boolean> published = new CompletableFuture<>();
        final CompletableFuture<Boolean> value = new CompletableFuture<>();
        @SuppressWarnings("unchecked")
        final DataPointBuilder<DataPointListBuilder> point = mock(DataPointBuilder.class);
        doAnswer(invocation -> {
                    value.complete(invocation.getArgument(0));
                    return point;
                })
                .when(point)
                .value(anyBoolean());
        final DataPointListBuilder list = mock(DataPointListBuilder.class);
        when(list.addDataPoint(any())).thenReturn(point);
        doAnswer(_ -> published.complete(true)).when(list).publish();
        final BatchPollingOutput output = mock(BatchPollingOutput.class);
        when(output.dataPointListPublisher()).thenReturn(list);
        doAnswer(_ -> published.complete(false)).when(output).fail(anyString());
        doAnswer(_ -> published.complete(false)).when(output).fail(any(), any());

        started.poll(mock(BatchPollingInput.class), output);

        if (!published.get(POLL_WAIT_SECONDS, TimeUnit.SECONDS)) {
            return false;
        }
        assertThat(value.getNow(false)).isTrue();
        return true;
    }

    /// The values a poll published into `output`, none when it published nothing.
    private static @NotNull List<Object> valuesOf(final @NotNull PollingOutputImpl output) {
        return output.getDataSample().getDataPointList().stream()
                .map(DataPoint::getTagValue)
                .toList();
    }

    /// Waits until the thread that published into `output` has left the adapter, so whatever the adapter does after
    /// the publish, as recording values in its change cache, is done.
    private static void awaitTheCallbackReturned(final @NotNull RecordingOutput output) {
        final Thread callback = output.publisher.get();
        assertThat(callback).isNotNull();
        await().atMost(DROP_SEEN_WITHIN).until(() -> Arrays.stream(callback.getStackTrace())
                .noneMatch(frame -> isAdapterFrame(frame.getClassName())));
    }

    /// Whether a stack frame runs the adapter's code: the class itself, its lambdas or its nested classes.
    private static boolean isAdapterFrame(final @NotNull String className) {
        final String adapter = ModbusProtocolAdapter.class.getName();
        return className.equals(adapter) || className.startsWith(adapter + "$");
    }

    /// Polls once through the sampler's own output and answers the coil values published, none when
    /// it published nothing; a failed poll fails the call. Unlike the mock in `pollPublishesTrue`,
    /// this output can be timed out with `orTimeout`, as the sampler does.
    private static @NotNull List<Object> publishedValues(final @NotNull ModbusProtocolAdapter started)
            throws Exception {
        final PollingOutputImpl output =
                new PollingOutputImpl(new ProtocolAdapterDataSampleImpl(ADAPTER_ID), ADAPTER_ID);
        started.poll(mock(BatchPollingInput.class), output);
        output.getOutputFuture().get(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        return valuesOf(output);
    }

    /// Polls once and answers the poll's output when the poll waits on the client's reconnect: it neither failed nor
    /// started a read by the time `poll` returned. Answers `null` once the poll completes otherwise, to poll again.
    private static @Nullable RecordingOutput waitingPoll(final @NotNull ModbusProtocolAdapter started)
            throws Exception {
        final RecordingOutput output = new RecordingOutput();
        started.poll(mock(BatchPollingInput.class), output);
        if (!output.readStarted.get() && !output.getOutputFuture().isDone()) {
            return output;
        }
        try {
            output.getOutputFuture().get(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (final ExecutionException _) {
            // A read on the dropped connection or a poll before the drop was seen fails; the caller polls again.
        }
        return null;
    }

    /// Polls once and answers the poll's output when its read is still in flight by the time `poll` returns. Answers
    /// `null` once the poll completes otherwise (no connection yet), to poll again.
    private static @Nullable RecordingOutput readingPoll(final @NotNull ModbusProtocolAdapter started)
            throws Exception {
        final RecordingOutput output = new RecordingOutput();
        started.poll(mock(BatchPollingInput.class), output);
        if (output.readStarted.get() && !output.getOutputFuture().isDone()) {
            return output;
        }
        try {
            output.getOutputFuture().get(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (final ExecutionException _) {
            // A poll before the client connected fails; the caller polls again.
        }
        return null;
    }

    /// Starts the device on the adapter's port, which nothing listened on until now, with the coil set.
    private @NotNull ModbusTcpServer startDevice(final int port) throws Exception {
        return startDevice(port, new CopyOnWriteArrayList<>(), coilImage(true), new CompletableFuture<>());
    }

    /// Starts the device on a port the system picks and points `port`, the port the adapter is built with, at it;
    /// every connection it accepts is added to `connections`, so a test can close one while the device keeps
    /// listening.
    private @NotNull ModbusTcpServer startDeviceOnAnyPort(
            final @NotNull List<Channel> connections, final @NotNull ProcessImage image) throws Exception {
        final CompletableFuture<Integer> bound = new CompletableFuture<>();
        final ModbusTcpServer server = startDevice(0, connections, image, bound);
        port = bound.get(POLL_WAIT_SECONDS, TimeUnit.SECONDS);
        return server;
    }

    /// Starts the device on `port`, serving `image` to every unit, and completes `bound` with the port it listens on.
    /// While `rejectAndClose` is set, it rejects every coil read with ILLEGAL_DATA_ADDRESS and then closes the
    /// connection.
    private @NotNull ModbusTcpServer startDevice(
            final int port,
            final @NotNull List<Channel> connections,
            final @NotNull ProcessImage image,
            final @NotNull CompletableFuture<Integer> bound)
            throws Exception {
        final ModbusTcpServer server = ModbusTcpServer.create(
                NettyTcpServerTransport.create(cfg -> {
                    cfg.bindAddress = "127.0.0.1";
                    cfg.port = port;
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

                    @Override
                    public ReadCoilsResponse readCoils(
                            final ModbusRequestContext context, final int unitId, final ReadCoilsRequest request)
                            throws ModbusResponseException, UnknownUnitIdException {
                        if (!rejectAndClose.get()) {
                            return super.readCoils(context, unitId, request);
                        }
                        rejections.incrementAndGet();
                        final Channel connection = connections.getLast();
                        connection
                                .eventLoop()
                                .schedule(
                                        () -> {
                                            connection.close();
                                        },
                                        CLOSE_AFTER_REJECTION_MILLIS,
                                        TimeUnit.MILLISECONDS);
                        throw new ModbusResponseException(FunctionCode.READ_COILS, ExceptionCode.ILLEGAL_DATA_ADDRESS);
                    }
                });
        server.start();
        return server;
    }

    private static @NotNull ProcessImage coilImage(final boolean coil) {
        final ProcessImage image = new ProcessImage();
        image.with(tx -> tx.writeCoils(coils -> coils.put(COIL_ADDRESS, coil)));
        return image;
    }

    /// A poll's output that records when the adapter takes its publisher, which a read does first, and when the poll
    /// ends, by a publish or a failure, which can come after it timed out.
    private static final class RecordingOutput extends PollingOutputImpl {
        private final @NotNull AtomicBoolean readStarted = new AtomicBoolean();
        private final @NotNull CompletableFuture<Void> ended = new CompletableFuture<>();
        private final @NotNull AtomicReference<Thread> publisher = new AtomicReference<>();
        private final @NotNull Consumer consumer;

        private RecordingOutput() {
            this(() -> {});
        }

        /// `consumer` runs inside the publish, after the output is complete, as a consumer fed in line does.
        private RecordingOutput(final @NotNull Consumer consumer) {
            super(new ProtocolAdapterDataSampleImpl(ADAPTER_ID), ADAPTER_ID);
            this.consumer = consumer;
        }

        @Override
        public @NotNull DataPointListBuilder dataPointListPublisher() {
            readStarted.set(true);
            return super.dataPointListPublisher();
        }

        @Override
        public void finish() {
            super.finish();
            publisher.set(Thread.currentThread());
            try {
                consumer.consume();
            } catch (final InterruptedException _) {
                Thread.currentThread().interrupt();
            } finally {
                ended.complete(null);
            }
        }

        @Override
        public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
            super.fail(t, errorMessage);
            ended.complete(null);
        }

        @Override
        public void fail(final @NotNull String errorMessage) {
            super.fail(errorMessage);
            ended.complete(null);
        }
    }

    /// The consumer's work on a publish.
    @FunctionalInterface
    private interface Consumer {
        void consume() throws InterruptedException;
    }
}
