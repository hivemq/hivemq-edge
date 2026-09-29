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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingInput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapter;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapterInformation;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * Covers the reconnect behaviour of the polling path: a poll that finds no usable connection must tear down whatever
 * is left and start a fresh connection attempt.
 * <p>
 * Tearing the connection down is what stops the PLC4X driver's internal retry loop and releases the Netty channels it
 * retains, so "was the old connection closed" is the assertion that guards against the memory leak; "was a new
 * connection created" is the one that guards against the adapter dead-ending after a refused start.
 */
class AbstractPlc4xAdapterReconnectTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    void test_poll_whenConnectionWasNeverEstablished_startsAFreshConnectionAttempt() {
        final TestableAdapter adapter = new TestableAdapter();
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));

        // The initial attempt failed, leaving no connection behind -- the customer's "PLC down at startup" case.
        awaitUntil(() -> adapter.connectionsCreated.get() == 1 && !adapter.isConnecting());
        assertThat(adapter.connection).isNull();

        final CapturingPollingOutput output = new CapturingPollingOutput();
        adapter.poll(mock(BatchPollingInput.class), output);

        awaitUntil(() -> adapter.connectionsCreated.get() == 2);
        assertThat(output.failMessage).contains("no connection was established");
    }

    @Test
    void test_poll_whenConnectionIsDead_closesItAndReconnects() throws Plc4xException {
        final TestableAdapter adapter = new TestableAdapter();
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        awaitUntil(() -> adapter.connectionsCreated.get() == 1 && !adapter.isConnecting());

        // A connection that exists but is no longer connected -- the customer's "PLC went away" case.
        final FakeConnection dead = new FakeConnection();
        adapter.connection = dead;

        final CapturingPollingOutput output = new CapturingPollingOutput();
        adapter.poll(mock(BatchPollingInput.class), output);

        // Closing the old connection is what stops the driver's retry loop and reclaims what it leaked.
        assertThat(dead.disconnected)
                .as("the dead connection must be closed, not abandoned")
                .isTrue();
        awaitUntil(() -> adapter.connectionsCreated.get() == 2);
        assertThat(output.failMessage).contains("the connection was lost");
    }

    @Test
    void test_poll_whileAConnectionAttemptIsInFlight_doesNotStartAnother() {
        final TestableAdapter adapter = new TestableAdapter();
        adapter.blockConnecting.set(true);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        awaitUntil(() -> adapter.connectionsCreated.get() == 1);

        adapter.poll(mock(BatchPollingInput.class), new CapturingPollingOutput());
        adapter.poll(mock(BatchPollingInput.class), new CapturingPollingOutput());

        assertThat(adapter.connectionsCreated.get())
                .as("an in-flight attempt must not be duplicated by concurrent polls")
                .isEqualTo(1);

        adapter.hangLatch.countDown();
    }

    private static void awaitUntil(final @NotNull BooleanSupplier condition) {
        final long deadline = System.nanoTime() + TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("interrupted while waiting", e);
            }
        }
        throw new AssertionError("condition not met within " + TIMEOUT);
    }

    private static @NotNull ProtocolAdapterStartInput startInput() {
        final ProtocolAdapterStartInput input = mock(ProtocolAdapterStartInput.class);
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        when(input.moduleServices()).thenReturn(moduleServices);
        return input;
    }

    /**
     * An S7 adapter whose connections never succeed, counting how many were created and whether they were closed.
     */
    private static final class TestableAdapter extends S7ProtocolAdapter {

        final @NotNull AtomicInteger connectionsCreated = new AtomicInteger();
        final @NotNull AtomicReference<Boolean> blockConnecting = new AtomicReference<>(false);
        final @NotNull CountDownLatch hangLatch = new CountDownLatch(1);

        @SuppressWarnings("unchecked")
        TestableAdapter() {
            super(S7ProtocolAdapterInformation.INSTANCE, newInput());
        }

        @SuppressWarnings("unchecked")
        private static @NotNull ProtocolAdapterInput<S7SpecificAdapterConfig> newInput() {
            final ProtocolAdapterInput<S7SpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
            when(input.getTags()).thenReturn((List) List.<Plc4xTag>of());
            when(input.getAdapterId()).thenReturn("testS7");
            when(input.getProtocolAdapterState()).thenReturn(mock(ProtocolAdapterState.class));
            when(input.moduleServices()).thenReturn(mock(ModuleServices.class));
            return input;
        }

        @Override
        protected @NotNull Plc4xConnection<S7SpecificAdapterConfig> createConnection() throws Plc4xException {
            connectionsCreated.incrementAndGet();
            if (blockConnecting.get()) {
                // A connection whose startConnection() never returns, so the adapter stays "connecting".
                // createConnection() itself must return promptly: it runs on the caller's thread.
                return new HangingConnection(hangLatch);
            }
            throw new Plc4xException("connection refused by test");
        }
    }

    /**
     * A connection whose start blocks until released, holding the adapter in the "connecting" state.
     */
    private static final class HangingConnection extends Plc4xConnection<S7SpecificAdapterConfig> {

        private final @NotNull CountDownLatch latch;

        HangingConnection(final @NotNull CountDownLatch latch) throws Plc4xException {
            super(PlcDriverManager.getDefault(), testConfig(), config -> "");
            this.latch = latch;
        }

        @Override
        void startConnection(
                final @NotNull EventService eventService,
                final @NotNull String adapterId,
                final @NotNull String protocolId)
                throws Plc4xException {
            try {
                latch.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new Plc4xException("released by test");
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void disconnect() {}

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
     * A connection that reports itself disconnected and records whether it was closed.
     */
    private static final class FakeConnection extends Plc4xConnection<S7SpecificAdapterConfig> {

        volatile boolean disconnected;

        FakeConnection() throws Plc4xException {
            super(PlcDriverManager.getDefault(), testConfig(), config -> "");
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public void disconnect() {
            disconnected = true;
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

    private static final class CapturingPollingOutput implements BatchPollingOutput {

        volatile @Nullable String failMessage;

        @Override
        public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {}

        @Override
        public void addDataPoint(final @NotNull DataPoint dataPoint) {}

        @Override
        public @NotNull DataPointListBuilder dataPointListPublisher() {
            return mock(DataPointListBuilder.class);
        }

        @Override
        public void finish() {}

        @Override
        public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
            this.failMessage = errorMessage;
        }

        @Override
        public void fail(final @NotNull String errorMessage) {
            this.failMessage = errorMessage;
        }
    }
}
