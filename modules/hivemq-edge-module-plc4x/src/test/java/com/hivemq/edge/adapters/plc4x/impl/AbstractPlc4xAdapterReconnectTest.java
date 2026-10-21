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

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.Plc4XSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.types.ads.ADSProtocolAdapter;
import com.hivemq.edge.adapters.plc4x.types.ads.ADSProtocolAdapterInformation;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapter;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapterInformation;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.modules.adapters.impl.polling.PollingOutputImpl;
import com.hivemq.edge.modules.adapters.impl.polling.PollingTask;
import com.hivemq.edge.modules.adapters.impl.polling.batch.BatchPollingInputImpl;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingSampler;
import com.hivemq.util.NanoTimeProvider;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Covers the reconnect behaviour of the polling path: a poll that finds no usable connection must tear down whatever
 * is left, start a fresh connection attempt and complete with that attempt's outcome -- a polling error when it
 * fails, a read when it succeeds.
 * <p>
 * Tearing the connection down is what stops the PLC4X driver's internal retry loop and releases the Netty channels it
 * retains, so "was the old connection closed" is the assertion that guards against the memory leak. "Was a new
 * connection created" guards against the adapter dead-ending after a refused start, and "did the reconnecting poll
 * read" against data resuming only one polling backoff after the device returned.
 */
class AbstractPlc4xAdapterReconnectTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void test_poll_whilePlcRefusesFromStart_failsWithTheAttemptsOutcome() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);
        assertThat(adapter.connection).isNull();

        final CapturingPollingOutput output = adapter.poll();

        await().atMost(TIMEOUT).until(() -> output.failMessage != null);
        assertThat(output.failMessage)
                .contains("no connection could be established")
                .contains("connection refused by test");
        assertThat(output.failCause)
                .as("an unreachable device is reported without a stack trace")
                .isNull();
        assertThat(adapter.connectionsCreated.get()).isEqualTo(2);
        assertThat(adapter.isConnecting()).isFalse();
        assertThat(adapter.connection).isNull();
    }

    @Test
    void test_poll_whenPlcReturns_readsInTheSamePoll() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        adapter.behaviour.set(Behaviour.SUCCEED);
        final CapturingPollingOutput output = adapter.poll();

        await().atMost(TIMEOUT).until(() -> output.published);
        assertThat(output.failMessage).isNull();
        final FakeConnection<?> established = adapter.lastConnection.get();
        assertThat(adapter.connection).isSameAs(established);
        assertThat(established.checked.get())
                .as("the poll that reconnects must use the connection, not wait for the next poll")
                .isEqualTo(1);
    }

    @Test
    void test_poll_whenConnectionIsLost_closesItAndReconnects() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.SUCCEED);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);
        final FakeConnection<?> lost = adapter.lastConnection.get();

        // The PLC went away: the connection exists but is no longer connected.
        lost.connected = false;
        final CapturingPollingOutput output = adapter.poll();

        assertThat(lost.disconnected)
                .as("the dead connection must be closed, not abandoned")
                .isTrue();
        verify(adapter.state).setConnectionStatus(ERROR);
        await().atMost(TIMEOUT).until(() -> output.published);
        assertThat(output.failMessage).isNull();
        assertThat(adapter.connectionsCreated.get()).isEqualTo(2);
        assertThat(adapter.connection).isSameAs(adapter.lastConnection.get());
    }

    @Test
    void test_poll_whileAConnectionAttemptIsInFlight_waitsForItInsteadOfStartingAnother() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.HANG);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.connectionsCreated.get() == 1);

        final CapturingPollingOutput first = adapter.poll();
        final CapturingPollingOutput second = adapter.poll();

        assertThat(adapter.connectionsCreated.get())
                .as("an in-flight attempt must not be duplicated by concurrent polls")
                .isEqualTo(1);
        assertThat(first.failMessage).isNull();
        assertThat(first.published).isFalse();

        adapter.hangLatch.countDown();
        await().atMost(TIMEOUT).until(() -> first.failMessage != null && second.failMessage != null);
        assertThat(adapter.isConnecting()).isFalse();
    }

    @Test
    void test_poll_whenNoAttemptCanBeStarted_failsAndLeavesNoAttemptPending() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        adapter.behaviour.set(Behaviour.CANNOT_CREATE);
        final CapturingPollingOutput output = adapter.poll();

        assertThat(output.failMessage).contains("No connection attempt could be started");
        assertThat(adapter.isConnecting()).isFalse();
        assertThat(adapter.connection).isNull();
    }

    @Test
    void test_poll_whenStartConnectionThrowsUnchecked_closesTheConnectionAndFails() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        adapter.behaviour.set(Behaviour.THROW_UNCHECKED);
        final CapturingPollingOutput output = adapter.poll();

        await().atMost(TIMEOUT).until(() -> output.failMessage != null);
        assertThat(adapter.lastConnection.get().disconnected).isTrue();
        assertThat(adapter.isConnecting()).isFalse();
        assertThat(adapter.connection).isNull();
    }

    @Test
    void test_poll_whenTheReconnectedConnectionFailsItsCheck_failsThePollAtOnce() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        // The attempt succeeds, then the connection check throws before the read: the poll must still complete.
        adapter.behaviour.set(Behaviour.SUCCEED_THEN_CHECK_FAILS);
        final CapturingPollingOutput output = adapter.poll();

        await().atMost(TIMEOUT).until(() -> output.failMessage != null);
        assertThat(output.failMessage).contains("while reading from the device");
        assertThat(output.failCause).hasMessageContaining("check failed in test");
    }

    @Test
    void test_poll_whenAStalePollTearsDownAfterAnotherReconnected_keepsTheNewConnection() throws Exception {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.SUCCEED);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);
        final FakeConnection<?> lost = adapter.lastConnection.get();
        lost.connected = false;

        // Poll A sees the lost connection and stalls before tearing it down; poll B tears it down and reconnects.
        final CountDownLatch pollAChecked = new CountDownLatch(1);
        final CountDownLatch releasePollA = new CountDownLatch(1);
        lost.stallFirstCheck(pollAChecked, releasePollA);
        final CompletableFuture<CapturingPollingOutput> pollA = CompletableFuture.supplyAsync(adapter::poll);
        assertThat(pollAChecked.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS))
                .isTrue();
        final CapturingPollingOutput pollB = adapter.poll();
        await().atMost(TIMEOUT).until(() -> pollB.published);
        final FakeConnection<?> reconnected = adapter.lastConnection.get();
        assertThat(adapter.connection).isSameAs(reconnected);

        releasePollA.countDown();
        final CapturingPollingOutput outputA = pollA.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        await().atMost(TIMEOUT).until(() -> outputA.published || outputA.failMessage != null);
        assertThat(adapter.connection)
                .as("a stale teardown must not drop the connection another poll has just established")
                .isSameAs(reconnected);
        assertThat(reconnected.disconnected).isFalse();
        assertThat(adapter.connectionsCreated.get()).isEqualTo(2);
    }

    @Test
    void test_stop_beforeAnAttemptConnects_closesTheConnectionTheAttemptEstablishes() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.SUCCEED_WHEN_RELEASED);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.connectionsCreated.get() == 1);
        final FakeConnection<?> attempted = adapter.lastConnection.get();

        // Stop runs while the attempt has not connected yet; the attempt then connects.
        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));
        adapter.hangLatch.countDown();
        adapter.awaitAttemptsFinished(1);

        assertThat(attempted.connected)
                .as("a connection established after stop must be closed, not left to the driver's retry loop")
                .isFalse();
        assertThat(adapter.connection).isNull();
    }

    @Test
    void test_pollingTask_withAnErrorLimit_stopsTheAdapterAfterConsecutiveFailedAttempts() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        // As documented for every polling adapter: consecutive polling failures beyond the limit stop the adapter.
        final RecordingSampler sampler = new RecordingSampler(adapter, 2);
        try (final RunningTask ignored = new RunningTask(sampler)) {
            await().atMost(TIMEOUT).untilTrue(sampler.stopped);
            assertThat(sampler.errors.get()).isEqualTo(3);
        }
    }

    @Test
    void test_pollingTask_withUnlimitedRetries_recoversOnTheFirstPollAfterThePlcReturns() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        final RecordingSampler sampler = new RecordingSampler(adapter, -1);
        try (final RunningTask ignored = new RunningTask(sampler)) {
            await().atMost(TIMEOUT).until(() -> sampler.errors.get() >= 3);
            final int pollsBeforeReturn = sampler.polls.get();
            adapter.behaviour.set(Behaviour.SUCCEED);

            await().atMost(TIMEOUT).until(() -> sampler.successes.get() >= 1);
            assertThat(sampler.stopped.get()).isFalse();
            // At most one poll was already refused when the device returned; the next one reconnects and reads.
            assertThat(sampler.firstSuccessfulPoll.get() - pollsBeforeReturn)
                    .as("the first poll after the device returns reconnects and reads")
                    .isLessThanOrEqualTo(2);
        }
    }

    @Test
    void test_poll_whenPlcReturns_readsTheTagsInTheSamePoll() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE, List.of(mock(Plc4xTag.class)));
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        adapter.behaviour.set(Behaviour.SUCCEED);
        adapter.poll();

        await().atMost(TIMEOUT).until(() -> adapter.lastConnection.get().reads.get() == 1);
        assertThat(adapter.connectionsCreated.get()).isEqualTo(2);
    }

    @Test
    void test_poll_whileAnAttemptThatSucceedsIsInFlight_completesEveryWaitingPollWithARead() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.SUCCEED_WHEN_RELEASED);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.connectionsCreated.get() == 1);

        final CapturingPollingOutput first = adapter.poll();
        final CapturingPollingOutput second = adapter.poll();
        adapter.hangLatch.countDown();

        await().atMost(TIMEOUT).until(() -> first.published && second.published);
        assertThat(first.failMessage).isNull();
        assertThat(second.failMessage).isNull();
        assertThat(adapter.connectionsCreated.get()).isEqualTo(1);
    }

    @Test
    void test_connectionStatus_isConnectedOnlyOnceTheAttemptSucceeds() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.SUCCEED_WHEN_RELEASED);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.connectionsCreated.get() == 1);
        verify(adapter.state, never()).setConnectionStatus(CONNECTED);

        adapter.hangLatch.countDown();

        await().atMost(TIMEOUT).untilAsserted(() -> verify(adapter.state).setConnectionStatus(CONNECTED));
    }

    @Test
    void test_start_whenTheFirstAttemptFails_setsConnectionStatusToError() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitAttemptsFinished(1);

        verify(adapter.state).setConnectionStatus(ERROR);
        verify(adapter.state, never()).setConnectionStatus(CONNECTED);
    }

    @Test
    void test_logs_distinguishAFailedAttemptFromALostConnection() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.REFUSE);
        final List<String> messages = captureLogs(() -> {
            adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
            adapter.awaitAttemptsFinished(1);
            adapter.behaviour.set(Behaviour.SUCCEED);
            final CapturingPollingOutput reconnected = adapter.poll();
            await().atMost(TIMEOUT).until(() -> reconnected.published);
            adapter.lastConnection.get().connected = false;
            adapter.poll();
            adapter.awaitAttemptsFinished(3);
        });

        assertThat(messages)
                .anyMatch(message -> message.contains("Connection attempt for adapter 'testS7' failed")
                        && message.contains("connection refused by test"))
                .anyMatch(message -> message.contains("The connection of adapter 'testS7' was lost"));
    }

    @Test
    void test_stop_whileAnAttemptIsConnecting_closesItAndLeavesNoAttemptPending() {
        final TestableAdapter adapter = new TestableAdapter(Behaviour.FAIL_WHEN_DISCONNECTED);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.connectionsCreated.get() == 1);
        final FakeConnection<?> connecting = adapter.lastConnection.get();

        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));

        adapter.awaitAttemptsFinished(1);
        assertThat(connecting.disconnected).isTrue();
        assertThat(adapter.connection).isNull();
    }

    @Test
    void test_poll_onAnAdsAdapter_closesALostConnectionAndReconnects() {
        final TestableAdsAdapter adapter = new TestableAdsAdapter();
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.attemptsFinished.get() == 1 && !adapter.isConnecting());
        final FakeConnection<?> lost = adapter.lastConnection.get();
        lost.connected = false;

        final CapturingPollingOutput output = new CapturingPollingOutput();
        adapter.poll(mock(BatchPollingInput.class), output);

        await().atMost(TIMEOUT).until(() -> output.published);
        assertThat(lost.disconnected).isTrue();
        assertThat(adapter.connectionsCreated.get()).isEqualTo(2);
    }

    private static @NotNull List<String> captureLogs(final @NotNull Runnable body) {
        final Logger logger = (Logger) LoggerFactory.getLogger(TestableAdapter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            body.run();
            return appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static @NotNull ProtocolAdapterStartInput startInput() {
        final ProtocolAdapterStartInput input = mock(ProtocolAdapterStartInput.class);
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        when(input.moduleServices()).thenReturn(moduleServices);
        return input;
    }

    private enum Behaviour {
        /** startConnection() fails at once, as against a port that refuses. */
        REFUSE,
        /** startConnection() never returns until released, holding the adapter in the "connecting" state. */
        HANG,
        /** startConnection() succeeds. */
        SUCCEED,
        /** startConnection() succeeds, then the connection check before a read throws. */
        SUCCEED_THEN_CHECK_FAILS,
        /** startConnection() waits until released, then succeeds. */
        SUCCEED_WHEN_RELEASED,
        /** startConnection() throws an unchecked exception. */
        THROW_UNCHECKED,
        /** startConnection() waits until the connection is disconnected, then fails. */
        FAIL_WHEN_DISCONNECTED,
        /** createConnection() itself throws, so no attempt can be started. */
        CANNOT_CREATE
    }

    /**
     * An S7 adapter whose connections behave as {@link #behaviour} says.
     */
    private static final class TestableAdapter extends S7ProtocolAdapter {

        final @NotNull AtomicReference<Behaviour> behaviour;
        final @NotNull AtomicInteger connectionsCreated = new AtomicInteger();
        final @NotNull AtomicInteger attemptsFinished = new AtomicInteger();
        final @NotNull AtomicReference<FakeConnection<S7SpecificAdapterConfig>> lastConnection =
                new AtomicReference<>();
        final @NotNull CountDownLatch hangLatch = new CountDownLatch(1);
        final @NotNull ProtocolAdapterState state;

        TestableAdapter(final @NotNull Behaviour behaviour) {
            this(behaviour, List.of());
        }

        TestableAdapter(final @NotNull Behaviour behaviour, final @NotNull List<Plc4xTag> tags) {
            this(behaviour, tags, mock(ProtocolAdapterState.class));
        }

        private TestableAdapter(
                final @NotNull Behaviour behaviour,
                final @NotNull List<Plc4xTag> tags,
                final @NotNull ProtocolAdapterState state) {
            super(S7ProtocolAdapterInformation.INSTANCE, newInput(tags, state));
            this.behaviour = new AtomicReference<>(behaviour);
            this.state = state;
        }

        @SuppressWarnings("unchecked")
        private static @NotNull ProtocolAdapterInput<S7SpecificAdapterConfig> newInput(
                final @NotNull List<Plc4xTag> tags, final @NotNull ProtocolAdapterState state) {
            final ProtocolAdapterInput<S7SpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
            when(input.getTags()).thenReturn((List) tags);
            when(input.getAdapterId()).thenReturn("testS7");
            when(input.getProtocolAdapterState()).thenReturn(state);
            when(input.moduleServices()).thenReturn(mock(ModuleServices.class));
            return input;
        }

        @Override
        protected @NotNull Plc4xConnection<S7SpecificAdapterConfig> createConnection() throws Plc4xException {
            final Behaviour current = behaviour.get();
            if (current == Behaviour.CANNOT_CREATE) {
                throw new Plc4xException("invalid connection configuration");
            }
            connectionsCreated.incrementAndGet();
            final FakeConnection<S7SpecificAdapterConfig> created =
                    new FakeConnection<>(testConfig(), current, hangLatch, attemptsFinished);
            lastConnection.set(created);
            return created;
        }

        @NotNull
        CapturingPollingOutput poll() {
            final CapturingPollingOutput output = new CapturingPollingOutput();
            poll(mock(BatchPollingInput.class), output);
            return output;
        }

        /** Waits until the given number of attempts has finished and the adapter is no longer connecting. */
        void awaitAttemptsFinished(final int count) {
            await().atMost(TIMEOUT).until(() -> attemptsFinished.get() >= count && !isConnecting());
            assertThat(attemptsFinished.get()).isEqualTo(count);
        }
    }

    /**
     * An ADS adapter whose connections always connect, to show the shared base reconnects ADS as it does S7.
     */
    private static final class TestableAdsAdapter extends ADSProtocolAdapter {

        final @NotNull AtomicInteger connectionsCreated = new AtomicInteger();
        final @NotNull AtomicInteger attemptsFinished = new AtomicInteger();
        final @NotNull AtomicReference<FakeConnection<ADSSpecificAdapterConfig>> lastConnection =
                new AtomicReference<>();

        TestableAdsAdapter() {
            super(ADSProtocolAdapterInformation.INSTANCE, newInput());
        }

        @SuppressWarnings("unchecked")
        private static @NotNull ProtocolAdapterInput<ADSSpecificAdapterConfig> newInput() {
            final ProtocolAdapterInput<ADSSpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
            when(input.getTags()).thenReturn((List) List.<Plc4xTag>of());
            when(input.getAdapterId()).thenReturn("testAds");
            when(input.getProtocolAdapterState()).thenReturn(mock(ProtocolAdapterState.class));
            return input;
        }

        @Override
        protected @NotNull Plc4xConnection<ADSSpecificAdapterConfig> createConnection() throws Plc4xException {
            connectionsCreated.incrementAndGet();
            final FakeConnection<ADSSpecificAdapterConfig> created = new FakeConnection<>(
                    new ADSSpecificAdapterConfig(
                            48898, "localhost", 851, 48898, "1.2.3.4.5.6", "6.5.4.3.2.1", null, null),
                    Behaviour.SUCCEED,
                    new CountDownLatch(0),
                    attemptsFinished);
            lastConnection.set(created);
            return created;
        }
    }

    /**
     * A connection whose start refuses, hangs or succeeds, recording whether it was checked and closed.
     */
    private static final class FakeConnection<C extends Plc4XSpecificAdapterConfig<?>> extends Plc4xConnection<C> {

        private final @NotNull Behaviour behaviour;
        private final @NotNull CountDownLatch hangLatch;
        private final @NotNull AtomicInteger attemptsFinished;
        private final @NotNull CountDownLatch disconnectLatch = new CountDownLatch(1);
        final @NotNull AtomicInteger checked = new AtomicInteger();
        final @NotNull AtomicInteger reads = new AtomicInteger();
        volatile boolean connected;
        volatile boolean disconnected;

        FakeConnection(
                final @NotNull C config,
                final @NotNull Behaviour behaviour,
                final @NotNull CountDownLatch hangLatch,
                final @NotNull AtomicInteger attemptsFinished)
                throws Plc4xException {
            super(PlcDriverManager.getDefault(), config, ignored -> "");
            this.behaviour = behaviour;
            this.hangLatch = hangLatch;
            this.attemptsFinished = attemptsFinished;
        }

        @Override
        void startConnection(
                final @NotNull EventService eventService,
                final @NotNull String adapterId,
                final @NotNull String protocolId)
                throws Plc4xException {
            try {
                switch (behaviour) {
                    case SUCCEED, SUCCEED_THEN_CHECK_FAILS -> connected = true;
                    case SUCCEED_WHEN_RELEASED -> {
                        try {
                            hangLatch.await();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        connected = true;
                    }
                    case THROW_UNCHECKED -> throw new IllegalStateException("unexpected failure in test");
                    case FAIL_WHEN_DISCONNECTED -> {
                        try {
                            disconnectLatch.await();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        throw new Plc4xException("disconnected while connecting");
                    }
                    case HANG -> {
                        try {
                            hangLatch.await();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        throw new Plc4xException("released by test");
                    }
                    default -> throw new Plc4xException("connection refused by test");
                }
            } finally {
                attemptsFinished.incrementAndGet();
            }
        }

        private volatile @Nullable CountDownLatch stallSignal;
        private volatile @Nullable CountDownLatch stallRelease;

        /** Makes the next isConnected() call signal {@code signal} and wait for {@code release}. */
        void stallFirstCheck(final @NotNull CountDownLatch signal, final @NotNull CountDownLatch release) {
            this.stallRelease = release;
            this.stallSignal = signal;
        }

        @Override
        public boolean isConnected() {
            final CountDownLatch toSignal = stallSignal;
            final CountDownLatch toAwait = stallRelease;
            if (toSignal != null && toAwait != null) {
                stallSignal = null;
                toSignal.countDown();
                try {
                    toAwait.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return connected;
        }

        @Override
        protected void lazyConnectionCheck() {
            checked.incrementAndGet();
            if (behaviour == Behaviour.SUCCEED_THEN_CHECK_FAILS) {
                throw new IllegalStateException("check failed in test");
            }
        }

        @Override
        public @NotNull CompletableFuture<? extends PlcReadResponse> read(final @NotNull List<Plc4xTag> tags) {
            reads.incrementAndGet();
            return CompletableFuture.completedFuture(mock(PlcReadResponse.class));
        }

        @Override
        public void disconnect() {
            connected = false;
            disconnected = true;
            disconnectLatch.countDown();
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

    private static @NotNull S7SpecificAdapterConfig testConfig() {
        return new S7SpecificAdapterConfig(
                102,
                "localhost",
                S7SpecificAdapterConfig.ControllerType.S7_1500,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Drives the adapter the way the polling framework does and records the outcomes the framework reports: each
     * success, each error, and whether it gave up once maxErrorsBeforeRemoval was exceeded.
     */
    private static final class RecordingSampler implements ProtocolAdapterPollingSampler {

        private final @NotNull TestableAdapter adapter;
        private final int maxErrorsBeforeRemoval;
        private final @NotNull UUID id = UUID.randomUUID();

        @SuppressWarnings("JavaUtilDate")
        private final @NotNull Date created = new Date();

        final @NotNull AtomicInteger polls = new AtomicInteger();
        final @NotNull AtomicInteger successes = new AtomicInteger();
        final @NotNull AtomicInteger firstSuccessfulPoll = new AtomicInteger();
        final @NotNull AtomicInteger errors = new AtomicInteger();
        final @NotNull AtomicBoolean stopped = new AtomicBoolean();
        private volatile @Nullable ScheduledFuture<?> future;

        RecordingSampler(final @NotNull TestableAdapter adapter, final int maxErrorsBeforeRemoval) {
            this.adapter = adapter;
            this.maxErrorsBeforeRemoval = maxErrorsBeforeRemoval;
        }

        @Override
        public @NotNull CompletableFuture<?> execute() {
            final int poll = polls.incrementAndGet();
            final PollingOutputImpl output =
                    new PollingOutputImpl(new ProtocolAdapterDataSampleImpl(getAdapterId()), getAdapterId());
            adapter.poll(new BatchPollingInputImpl(), output);
            return output.getOutputFuture().thenRun(() -> {
                firstSuccessfulPoll.compareAndSet(0, poll);
                successes.incrementAndGet();
            });
        }

        @Override
        public void error(final @NotNull Throwable t, final boolean continuing) {
            errors.incrementAndGet();
            if (!continuing) {
                stopped.set(true);
            }
        }

        @Override
        public int getMaxErrorsBeforeRemoval() {
            return maxErrorsBeforeRemoval;
        }

        @Override
        public long getInitialDelay() {
            return 0;
        }

        @Override
        public long getPeriod() {
            return 10;
        }

        @Override
        public @NotNull TimeUnit getUnit() {
            return TimeUnit.MILLISECONDS;
        }

        @Override
        public void close() {}

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public @NotNull UUID getId() {
            return id;
        }

        @Override
        public @NotNull Date getCreated() {
            return created;
        }

        @Override
        public @NotNull String getAdapterId() {
            return "testS7";
        }

        @Override
        public @NotNull String getProtocolId() {
            return "s7";
        }

        @Override
        public @Nullable ScheduledFuture<?> getScheduledFuture() {
            return future;
        }

        @Override
        public void setScheduledFuture(final @NotNull ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public @NotNull ProtocolAdapter getAdapter() {
            return adapter;
        }
    }

    /** The real polling task on its own scheduler, stopped and shut down on close. */
    private static final class RunningTask implements AutoCloseable {

        private final @NotNull ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final @NotNull PollingTask task;

        RunningTask(final @NotNull ProtocolAdapterPollingSampler sampler) {
            final NanoTimeProvider realTime = mock(NanoTimeProvider.class);
            when(realTime.nanoTime()).thenAnswer(invocation -> System.nanoTime());
            task = new PollingTask(sampler, scheduler, mock(EventService.class, RETURNS_DEEP_STUBS), realTime);
            scheduler.execute(task);
        }

        @Override
        public void close() {
            task.stopScheduling();
            scheduler.shutdownNow();
        }
    }

    private static final class CapturingPollingOutput implements BatchPollingOutput {

        volatile @Nullable String failMessage;
        volatile @Nullable Throwable failCause;
        volatile boolean published;

        @Override
        public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {}

        @Override
        public void addDataPoint(final @NotNull DataPoint dataPoint) {}

        @Override
        public @NotNull DataPointListBuilder dataPointListPublisher() {
            final DataPointListBuilder builder = mock(DataPointListBuilder.class);
            doAnswer(invocation -> {
                        published = true;
                        return null;
                    })
                    .when(builder)
                    .publish();
            return builder;
        }

        @Override
        public void finish() {}

        @Override
        public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
            this.failCause = t;
            this.failMessage = errorMessage != null ? errorMessage : String.valueOf(t.getMessage());
        }

        @Override
        public void fail(final @NotNull String errorMessage) {
            this.failMessage = errorMessage;
        }
    }
}
