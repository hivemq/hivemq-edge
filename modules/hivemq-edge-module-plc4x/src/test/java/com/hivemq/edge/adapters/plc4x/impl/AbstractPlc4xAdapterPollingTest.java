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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolSpecificAdapterConfig;
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
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSSpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSToMqttConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7ToMqttConfig;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class AbstractPlc4xAdapterPollingTest {

    private static final S7SpecificAdapterConfig S7_CONFIG = new S7SpecificAdapterConfig(
            102,
            "device",
            S7SpecificAdapterConfig.ControllerType.S7_300,
            null,
            null,
            null,
            null,
            null,
            null,
            new S7ToMqttConfig(null, null, null));
    private static final ADSSpecificAdapterConfig ADS_CONFIG = new ADSSpecificAdapterConfig(
            48898, "device", 851, 48898, "1.2.3.4.5.6", "6.5.4.3.2.1", null, new ADSToMqttConfig(null, null, null));

    @Test
    void endpointReturningAfterRefusal_reconnectsWithoutManualIntervention() {
        final TestConnection refused = connection(Outcome.FAILS);
        final TestConnection returned = connection(Outcome.CONNECTS);
        final TestS7Adapter adapter = s7Adapter(List.of(), refused, returned);

        start(adapter);
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(refused.disconnects.get()).isEqualTo(1));

        final TrackingOutput output = new TrackingOutput();
        adapter.poll(mock(BatchPollingInput.class), output);

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(output.published.get()).isEqualTo(1));
        assertThat(adapter.createdConnections.get()).isEqualTo(2);
        assertThat(returned.isConnected()).isTrue();
    }

    @Test
    void failedConnectionAttempt_reportsDriverCauseAndNoEstablishedConnection() {
        final TestConnection refused = connection(Outcome.FAILS);
        final TestConnection refusedAgain = connection(Outcome.FAILS);
        final TestS7Adapter adapter = s7Adapter(List.of(), refused, refusedAgain);

        final List<ILoggingEvent> events = captureLogs(() -> {
            start(adapter);
            await().atMost(2, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(refused.disconnects.get()).isEqualTo(1));
            final TrackingOutput output = new TrackingOutput();
            adapter.poll(mock(BatchPollingInput.class), output);
            await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(refusedAgain.disconnects.get())
                    .isEqualTo(1));
            assertThat(output.failures.get()).isEqualTo(1);
        });

        assertThat(events.stream().map(ILoggingEvent::getFormattedMessage))
                .anyMatch(message -> message.contains("Connection attempt") && message.contains("Connection refused"));
        assertThat(adapter.currentConnection()).isNull();
    }

    @Test
    void connectionLogs_distinguishInitialFailureFromLostConnection() {
        final TestConnection connected = connection(Outcome.CONNECTS);
        final TestConnection reconnect = connection(Outcome.CONNECTS);
        final TestS7Adapter adapter = s7Adapter(List.of(), connected, reconnect);

        final List<ILoggingEvent> events = captureLogs(() -> {
            start(adapter);
            await().atMost(2, TimeUnit.SECONDS).until(connected::isConnected);
            connected.connected.set(false);
            adapter.poll(mock(BatchPollingInput.class), new TrackingOutput());
            await().atMost(2, TimeUnit.SECONDS)
                    .untilAsserted(() -> assertThat(connected.disconnects.get()).isEqualTo(1));
        });

        assertThat(events.stream().map(ILoggingEvent::getFormattedMessage))
                .anyMatch(message -> message.contains("connection") && message.contains("was lost"));
    }

    @Test
    void pollingErrorLimit_defaultsToTenForS7AndAds() {
        final TestS7Adapter s7 = s7Adapter(List.of(), connection(Outcome.CONNECTS));
        final TestAdsAdapter ads = adsAdapter(List.of(), connection(Outcome.CONNECTS));

        assertThat(s7.getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(ads.getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
    }

    @Test
    void concurrentPollsDuringOneConnectionAttempt_completeWithThatAttemptOutcome() {
        final TestConnection pending = connection(Outcome.WAITS_FOR_RELEASE);
        final TestS7Adapter adapter = s7Adapter(List.of(), pending);
        start(adapter);
        await().atMost(2, TimeUnit.SECONDS).until(() -> pending.started.getCount() == 0);

        final TrackingOutput first = new TrackingOutput();
        final TrackingOutput second = new TrackingOutput();
        adapter.poll(mock(BatchPollingInput.class), first);
        adapter.poll(mock(BatchPollingInput.class), second);
        pending.release.countDown();

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(first.published.get()).isEqualTo(1);
            assertThat(second.published.get()).isEqualTo(1);
        });
        assertThat(adapter.createdConnections.get()).isEqualTo(1);
        assertThat(first.completions.get()).isEqualTo(1);
        assertThat(second.completions.get()).isEqualTo(1);
    }

    @Test
    void reconnectingPoll_readsTagsWithoutWaitingForAnotherPoll() {
        final Plc4xTag tag = mock(Plc4xTag.class);
        final TestConnection refused = connection(Outcome.FAILS);
        final TestConnection returned = connection(Outcome.CONNECTS);
        final TestS7Adapter adapter = s7Adapter(List.of(tag), refused, returned);
        start(adapter);
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(refused.disconnects.get()).isEqualTo(1));

        adapter.pollAdapter(mock(BatchPollingInput.class), new TrackingOutput());

        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(returned.reads.get()).isEqualTo(1));
    }

    @Test
    void connectionStatus_isErrorUntilConnectionAttemptSucceeds() {
        final TestConnection pending = connection(Outcome.WAITS_FOR_RELEASE);
        final ProtocolAdapterState state = mock(ProtocolAdapterState.class);
        final TestS7Adapter adapter = s7Adapter(state, List.of(), pending);

        start(adapter);
        await().atMost(2, TimeUnit.SECONDS).until(() -> pending.started.getCount() == 0);
        verify(state, never()).setConnectionStatus(CONNECTED);
        pending.release.countDown();

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> verify(state).setConnectionStatus(CONNECTED));
    }

    @Test
    void failedAttempt_setsConnectionStatusToError() {
        final ProtocolAdapterState state = mock(ProtocolAdapterState.class);
        final TestS7Adapter adapter = s7Adapter(state, List.of(), connection(Outcome.FAILS));

        start(adapter);

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> verify(state).setConnectionStatus(ERROR));
    }

    @Test
    void stopDuringConnectionAttempt_disconnectsThePendingConnection() {
        final TestConnection pending = connection(Outcome.WAITS_FOR_DISCONNECT);
        final TestS7Adapter adapter = s7Adapter(List.of(), pending);
        start(adapter);
        await().atMost(2, TimeUnit.SECONDS).until(() -> pending.started.getCount() == 0);

        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(pending.disconnects.get()).isGreaterThanOrEqualTo(1);
            assertThat(adapter.currentConnection()).isNull();
            assertThat(adapter.isConnecting()).isFalse();
        });
    }

    @Test
    void s7AndAdsReconnectAfterAConnectionIsLost() {
        assertReconnectsAfterLoss(
                () -> s7Adapter(List.of(), connection(Outcome.CONNECTS), connection(Outcome.CONNECTS)));
        assertReconnectsAfterLoss(
                () -> adsAdapter(List.of(), connection(Outcome.CONNECTS), connection(Outcome.CONNECTS)));
    }

    private static void assertReconnectsAfterLoss(final Supplier<? extends TestAdapter> adapterSupplier) {
        final TestAdapter adapter = adapterSupplier.get();
        start(adapter);
        await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(adapter.currentConnection()).isNotNull().isInstanceOf(TestConnection.class));
        final TestConnection first = (TestConnection) adapter.currentConnection();
        await().atMost(2, TimeUnit.SECONDS).until(first::isConnected);
        first.connected.set(false);

        adapter.pollAdapter(mock(BatchPollingInput.class), new TrackingOutput());

        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(adapter.createdConnections().get()).isEqualTo(2);
            assertThat(first.disconnects.get()).isEqualTo(1);
        });
    }

    private static TestS7Adapter s7Adapter(final List<Plc4xTag> tags, final TestConnection... connections) {
        return s7Adapter(mock(ProtocolAdapterState.class), tags, connections);
    }

    private static TestS7Adapter s7Adapter(
            final ProtocolAdapterState state, final List<Plc4xTag> tags, final TestConnection... connections) {
        return new TestS7Adapter(input(S7_CONFIG, state, tags), connections);
    }

    private static TestAdsAdapter adsAdapter(final List<Plc4xTag> tags, final TestConnection... connections) {
        return new TestAdsAdapter(input(ADS_CONFIG, mock(ProtocolAdapterState.class), tags), connections);
    }

    @SuppressWarnings("unchecked")
    private static <T extends ProtocolSpecificAdapterConfig> ProtocolAdapterInput<T> input(
            final T config, final ProtocolAdapterState state, final List<Plc4xTag> tags) {
        final ProtocolAdapterInput<T> input = mock(ProtocolAdapterInput.class);
        when(input.getAdapterId()).thenReturn("adapter");
        when(input.getConfig()).thenReturn(config);
        when(input.getProtocolAdapterState()).thenReturn(state);
        when(input.getTags()).thenReturn((List) tags);
        return input;
    }

    private static void start(final TestAdapter adapter) {
        final ModuleServices services = mock(ModuleServices.class);
        when(services.eventService()).thenReturn(mock(EventService.class));
        final ProtocolAdapterStartInput input = () -> services;
        adapter.startAdapter(input, mock(ProtocolAdapterStartOutput.class));
    }

    private static List<ILoggingEvent> captureLogs(final Runnable body) {
        final Logger logger = (Logger) LoggerFactory.getLogger(TestS7Adapter.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            body.run();
            return List.copyOf(appender.list);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private interface TestAdapter {
        @NotNull
        AtomicInteger createdConnections();

        @Nullable
        Plc4xConnection<?> currentConnection();

        void startAdapter(@NotNull ProtocolAdapterStartInput input, @NotNull ProtocolAdapterStartOutput output);

        void pollAdapter(@NotNull BatchPollingInput input, @NotNull BatchPollingOutput output);
    }

    private static final class TestS7Adapter extends AbstractPlc4xAdapter<S7SpecificAdapterConfig, Plc4xToMqttMapping>
            implements TestAdapter {
        private final Queue<TestConnection> connections;
        final AtomicInteger createdConnections = new AtomicInteger();

        TestS7Adapter(final ProtocolAdapterInput<S7SpecificAdapterConfig> input, final TestConnection... connections) {
            super(information(), input);
            this.connections = new ArrayDeque<>(List.of(connections));
        }

        @Override
        protected @NotNull Plc4xConnection<S7SpecificAdapterConfig> createConnection() {
            createdConnections.incrementAndGet();
            return connections.remove();
        }

        @Override
        protected @NotNull String getProtocolHandler() {
            return "s7";
        }

        @Override
        protected @NotNull ReadType getReadType() {
            return ReadType.Read;
        }

        @Override
        public @Nullable Plc4xConnection<?> currentConnection() {
            return connection;
        }

        @Override
        public @NotNull AtomicInteger createdConnections() {
            return createdConnections;
        }

        @Override
        public void startAdapter(
                final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
            start(input, output);
        }

        @Override
        public void pollAdapter(final @NotNull BatchPollingInput input, final @NotNull BatchPollingOutput output) {
            poll(input, output);
        }
    }

    private static final class TestAdsAdapter extends AbstractPlc4xAdapter<ADSSpecificAdapterConfig, Plc4xToMqttMapping>
            implements TestAdapter {
        private final Queue<TestConnection> connections;
        final AtomicInteger createdConnections = new AtomicInteger();

        TestAdsAdapter(
                final ProtocolAdapterInput<ADSSpecificAdapterConfig> input, final TestConnection... connections) {
            super(information(), input);
            this.connections = new ArrayDeque<>(List.of(connections));
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        protected @NotNull Plc4xConnection<ADSSpecificAdapterConfig> createConnection() {
            createdConnections.incrementAndGet();
            return (Plc4xConnection) connections.remove();
        }

        @Override
        protected @NotNull String getProtocolHandler() {
            return "ads";
        }

        @Override
        protected @NotNull ReadType getReadType() {
            return ReadType.Read;
        }

        @Override
        public @Nullable Plc4xConnection<?> currentConnection() {
            return connection;
        }

        @Override
        public @NotNull AtomicInteger createdConnections() {
            return createdConnections;
        }

        @Override
        public void startAdapter(
                final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
            start(input, output);
        }

        @Override
        public void pollAdapter(final @NotNull BatchPollingInput input, final @NotNull BatchPollingOutput output) {
            poll(input, output);
        }
    }

    private static ProtocolAdapterInformation information() {
        final ProtocolAdapterInformation information = mock(ProtocolAdapterInformation.class);
        when(information.getProtocolId()).thenReturn("plc");
        return information;
    }

    private enum Outcome {
        CONNECTS,
        FAILS,
        WAITS_FOR_RELEASE,
        WAITS_FOR_DISCONNECT
    }

    private static final class TestConnection extends Plc4xConnection<S7SpecificAdapterConfig> {
        final AtomicBoolean connected = new AtomicBoolean();
        final AtomicInteger disconnects = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch disconnected = new CountDownLatch(1);
        private final Outcome outcome;

        TestConnection(final Outcome outcome) throws Plc4xException {
            super(mock(PlcDriverManager.class), S7_CONFIG, config -> "test");
            this.outcome = outcome;
        }

        @Override
        void startConnection(
                final @NotNull EventService eventService,
                final @NotNull String adapterId,
                final @NotNull String protocolId)
                throws Plc4xException {
            started.countDown();
            if (outcome == Outcome.FAILS) {
                throw new Plc4xException("Connection refused");
            }
            if (outcome == Outcome.WAITS_FOR_RELEASE) {
                await(release);
            }
            if (outcome == Outcome.WAITS_FOR_DISCONNECT) {
                await(disconnected);
                throw new Plc4xException("Connection attempt cancelled");
            }
            connected.set(true);
        }

        @Override
        public void disconnect() {
            disconnects.incrementAndGet();
            connected.set(false);
            disconnected.countDown();
            release.countDown();
        }

        @Override
        public boolean isConnected() {
            return connected.get();
        }

        @Override
        protected void lazyConnectionCheck() {
            // An established test connection passes the check, as an established driver connection does.
        }

        @Override
        public @NotNull CompletableFuture<? extends PlcReadResponse> read(final @NotNull List<Plc4xTag> tags) {
            reads.incrementAndGet();
            return CompletableFuture.completedFuture(mock(PlcReadResponse.class));
        }

        @Override
        protected @NotNull String getProtocol() {
            return "test";
        }

        @Override
        protected @NotNull String getTagAddressForSubscription(final @NotNull Plc4xTag tag) {
            return "tag";
        }

        private static void await(final CountDownLatch latch) throws Plc4xException {
            try {
                if (!latch.await(2, TimeUnit.SECONDS)) {
                    throw new Plc4xException("Connection attempt did not complete");
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new Plc4xException("Connection attempt interrupted", e);
            }
        }
    }

    private static TestConnection connection(final Outcome outcome) {
        try {
            return new TestConnection(outcome);
        } catch (final Plc4xException e) {
            throw new AssertionError(e);
        }
    }

    private static final class TrackingOutput implements BatchPollingOutput {
        final AtomicInteger completions = new AtomicInteger();
        final AtomicInteger failures = new AtomicInteger();
        final AtomicInteger published = new AtomicInteger();
        private final DataPointListBuilder publisher = mock(DataPointListBuilder.class);

        TrackingOutput() {
            doAnswer(ignored -> {
                        published.incrementAndGet();
                        completions.incrementAndGet();
                        return null;
                    })
                    .when(publisher)
                    .publish();
        }

        @Override
        public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {}

        @Override
        public void addDataPoint(final @NotNull DataPoint dataPoint) {}

        @Override
        public @NotNull DataPointListBuilder dataPointListPublisher() {
            return publisher;
        }

        @Override
        public void finish() {
            completions.incrementAndGet();
        }

        @Override
        public void fail(final @NotNull Throwable throwable, final String message) {
            failures.incrementAndGet();
            completions.incrementAndGet();
        }

        @Override
        public void fail(final @NotNull String message) {
            failures.incrementAndGet();
            completions.incrementAndGet();
        }
    }
}
