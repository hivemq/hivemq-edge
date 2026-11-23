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
import static com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter.READ_MARGIN_MILLIS;
import static com.hivemq.edge.adapters.plc4x.impl.AbstractPlc4xAdapter.SAMPLER_TIMEOUT_MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.edge.adapters.plc4x.Plc4xException;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapterInformation;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7SpecificAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7ToMqttConfig;
import com.hivemq.protocols.AbstractSubscriptionSampler;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.apache.plc4x.java.api.PlcDriverManager;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.spi.messages.DefaultPlcReadResponse;
import org.apache.plc4x.java.spi.messages.utils.DefaultPlcResponseItem;
import org.apache.plc4x.java.spi.messages.utils.PlcResponseItem;
import org.apache.plc4x.java.spi.values.PlcDINT;
import org.apache.plc4x.java.spi.values.PlcList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/// Guards what a poll does when its run ends under it, how long a read may take, and what a partial answer
/// publishes: a poll of an ended run completes without data or error, a read is bounded by the sampler's timeout,
/// a read is not queued behind an unanswered one, a value whose publish may not have been taken is published again,
/// and one unreadable tag does not hide the others.
class AbstractPlc4xAdapterLifetimeTest {

    private static final @NotNull Duration TIMEOUT = Duration.ofSeconds(5);
    // A stop ends a poll waiting on a connect in milliseconds; 1 s is well inside TIMEOUT, the connection's own wait
    // for the gate, so a poll ended by that wait instead fails the test.
    private static final @NotNull Duration STOP_ENDS_POLL_WITHIN = Duration.ofSeconds(1);
    // Short enough to watch a poll run out of time; the read budget is this minus the adapter's read margin.
    private static final long SHORT_SAMPLER_TIMEOUT_MILLIS = 1_000;
    private static final @NotNull Plc4xTag TAG_A = tag("a");
    private static final @NotNull Plc4xTag TAG_B = tag("b");

    /// A read answered after the adapter stopped and started again belongs to the old run: its poll completes
    /// without data, and its value is not recorded in the new run's change cache, so the new run publishes it.
    @Test
    void readOfAnEndedRun_answeredAfterARestart_leavesTheNewRunsCacheEmpty() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SAMPLER_TIMEOUT_MILLIS);
        adapter.readsFailOnClose = false;
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        final CompletableFuture<PlcReadResponse> oldRunsRead = new CompletableFuture<>();
        adapter.answer = () -> oldRunsRead;
        final RecordingOutput oldRunsPoll = adapter.poll();

        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        oldRunsRead.complete(response(Map.of("a", ok(7))));

        assertThat(oldRunsPoll.published).isTrue();
        assertThat(oldRunsPoll.added).isEmpty();
        assertThat(oldRunsPoll.failMessage).isNull();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(7))));
        final RecordingOutput newRunsPoll = adapter.poll();
        await().atMost(TIMEOUT).until(newRunsPoll::completed);
        assertThat(newRunsPoll.added).containsExactly("a");
    }

    /// A poll waiting on a connection attempt when the adapter stops completes without data and without an error:
    /// a failure would reach the ended run's sampler and count against the adapter the next run shares.
    @Test
    void pollWaitingOnAConnectionAttempt_whenTheAdapterStops_completesWithoutAnError() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SAMPLER_TIMEOUT_MILLIS);
        adapter.connectGate = new CountDownLatch(1);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.connections.size() == 1);
        final RecordingOutput waiting = adapter.poll();

        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));

        // The stop's disconnect ends the connect, as the real connection's interrupt does: well inside the
        // connection's own wait for the gate.
        await().atMost(STOP_ENDS_POLL_WITHIN).until(waiting::completed);
        assertThat(waiting.failMessage).isNull();
        assertThat(waiting.added).isEmpty();
        assertThat(adapter.reads.get()).isZero();
        verify(adapter.state, never()).setConnectionStatus(ERROR);
    }

    /// A stop between the poll taking the connection and its read closes the connection, and the read throws; the
    /// poll belongs to the run that ended, so it completes without an error.
    @Test
    void readThatThrowsBecauseTheAdapterStopped_completesWithoutAnError() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SAMPLER_TIMEOUT_MILLIS);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> {
            adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));
            throw new IllegalStateException("The connection was closed");
        };

        final RecordingOutput reading = adapter.poll();

        assertThat(reading.completed()).isTrue();
        assertThat(reading.failMessage).isNull();
        assertThat(reading.added).isEmpty();
        verify(adapter.state, never()).setConnectionStatus(ERROR);
    }

    /// The disconnect of a stop fails the read in flight; the poll belongs to the run that ended before the
    /// disconnect, so it completes without an error.
    @Test
    void readInFlight_whenTheAdapterStops_completesWithoutAnError() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SAMPLER_TIMEOUT_MILLIS);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = CompletableFuture::new;
        final RecordingOutput reading = adapter.poll();

        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));

        assertThat(reading.completed()).isTrue();
        assertThat(reading.failMessage).isNull();
        assertThat(reading.added).isEmpty();
    }

    /// A publish that returns after the sampler's timeout may not have been taken, so its value is not recorded and
    /// the next poll publishes the unchanged value again.
    @Test
    void publishReturningAfterTheSamplersTimeout_isPublishedAgainByTheNextPoll() {
        // Long enough for a loopback read and its publish, short enough that holding the publish past it is quick.
        final long samplerTimeoutMillis = 600;
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), samplerTimeoutMillis);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(7))));
        final RecordingOutput late = new RecordingOutput();
        late.publishGate = new CountDownLatch(1);

        // Taken on the polling thread right before poll(), which takes its own start after it: the wait below
        // passes the adapter's sampler timeout whatever the delay before the polling thread runs.
        final CompletableFuture<Long> pollStartNanos = new CompletableFuture<>();
        final CompletableFuture<Void> polling = CompletableFuture.runAsync(() -> {
            pollStartNanos.complete(System.nanoTime());
            adapter.poll(late);
        });
        await().atMost(TIMEOUT)
                .pollInterval(Duration.ofMillis(10))
                .until(() -> pollStartNanos.isDone()
                        && System.nanoTime() - pollStartNanos.join()
                                > TimeUnit.MILLISECONDS.toNanos(samplerTimeoutMillis + 50));
        late.publishGate.countDown();
        await().atMost(TIMEOUT).until(polling::isDone);

        assertThat(late.publishGateExpired).isFalse();
        assertThat(late.added).containsExactly("a");
        final RecordingOutput next = adapter.poll();
        await().atMost(TIMEOUT).until(next::completed);
        assertThat(next.added).containsExactly("a");
    }

    /// A publish that throws fails the poll, and its value is published again by the next poll.
    @Test
    void publishThatThrows_failsThePollAndIsPublishedAgainByTheNextPoll() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SAMPLER_TIMEOUT_MILLIS);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(7))));
        final RecordingOutput failing = new RecordingOutput();
        failing.publishThrows = true;

        adapter.poll(failing);

        assertThat(failing.failMessage).contains("while publishing the values read");
        final RecordingOutput next = adapter.poll();
        await().atMost(TIMEOUT).until(next::completed);
        assertThat(next.added).containsExactly("a");
    }

    /// The adapter records a value as published only within its sampler timeout of poll(), so that timeout must not
    /// be longer than the time the sampler really waits for a poll's output.
    @Test
    void samplerTimeout_isNoLongerThanTheSamplers() {
        assertThat(SAMPLER_TIMEOUT_MILLIS).isLessThanOrEqualTo(AbstractSubscriptionSampler.POLL_OUTPUT_TIMEOUT_MILLIS);
    }

    /// A device that never answers fails the poll before the sampler's timeout, not after the driver's own limit,
    /// which for S7 is longer than the sampler waits.
    @Test
    void readTheDeviceNeverAnswers_failsThePollBeforeTheSamplersTimeout() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SHORT_SAMPLER_TIMEOUT_MILLIS);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = CompletableFuture::new;

        final long pollStartNanos = System.nanoTime();
        final RecordingOutput unanswered = adapter.poll();

        await().atMost(TIMEOUT).until(() -> unanswered.failMessage != null);
        assertThat(unanswered.failMessage).contains("did not answer the read in time");
        assertThat(unanswered.failedAtNanos - pollStartNanos)
                .isLessThan(TimeUnit.MILLISECONDS.toNanos(SHORT_SAMPLER_TIMEOUT_MILLIS));
    }

    /// A poll whose connection attempt took most of its time ends without reading and without an error: the answer
    /// would come after the sampler stopped waiting, and a value the device clears on read would be lost; the next
    /// poll reads.
    @Test
    void connectionEstablishedTooLateInThePoll_endsWithoutReadingAndTheNextPollReads() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SHORT_SAMPLER_TIMEOUT_MILLIS);
        adapter.connectGate = new CountDownLatch(1);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        await().atMost(TIMEOUT).until(() -> adapter.connections.size() == 1);

        final long pollStartNanos = System.nanoTime();
        final RecordingOutput waiting = adapter.poll();
        await().atMost(TIMEOUT)
                .pollInterval(Duration.ofMillis(10))
                .until(() -> System.nanoTime() - pollStartNanos
                        > TimeUnit.MILLISECONDS.toNanos(SHORT_SAMPLER_TIMEOUT_MILLIS - READ_MARGIN_MILLIS + 100));
        adapter.connectGate.countDown();

        await().atMost(TIMEOUT).until(waiting::completed);
        assertThat(waiting.failMessage).isNull();
        assertThat(waiting.added).isEmpty();
        assertThat(adapter.reads.get()).isZero();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(1))));
        final RecordingOutput next = adapter.poll();
        await().atMost(TIMEOUT).until(next::completed);
        assertThat(next.added).containsExactly("a");
    }

    /// The drivers answer one request at a time, so a poll that finds the previous read unanswered sends no read
    /// behind it: it reads once the driver answers that read, and an unanswered read costs one polling error, not
    /// two.
    @Test
    void pollWhileThePreviousReadIsUnanswered_readsOnceThePreviousReadEnds() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        final CompletableFuture<PlcReadResponse> unanswered = new CompletableFuture<>();
        adapter.answer = () -> unanswered;
        adapter.poll();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(2))));

        final RecordingOutput second = adapter.poll();
        assertThat(second.completed()).isFalse();
        assertThat(adapter.reads.get()).isEqualTo(1);
        unanswered.complete(response(Map.of("a", ok(1))));

        await().atMost(TIMEOUT).until(second::completed);
        assertThat(second.failMessage).isNull();
        assertThat(second.added).containsExactly("a");
        assertThat(adapter.reads.get()).isEqualTo(2);
    }

    /// A poll waiting on the previous read when the adapter stops completes without an error and sends no read: the
    /// stop's disconnect ends the previous read, and the waiting poll belongs to the run that ended.
    @Test
    void pollWaitingOnThePreviousRead_whenTheAdapterStops_completesWithoutAnError() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = CompletableFuture::new;
        adapter.poll();
        final RecordingOutput waiting = adapter.poll();
        assertThat(waiting.completed()).isFalse();

        adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));

        await().atMost(STOP_ENDS_POLL_WITHIN).until(waiting::completed);
        assertThat(waiting.failMessage).isNull();
        assertThat(waiting.added).isEmpty();
        assertThat(adapter.reads.get()).isEqualTo(1);
    }

    /// A response that holds no entry for a tag counts as that tag's failure, and the other tags are published.
    @Test
    void responseWithoutAnEntryForATag_publishesTheOtherTags() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A, TAG_B), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(1))));

        try (final CapturedLog log = new CapturedLog()) {
            final RecordingOutput polled = adapter.poll();

            assertThat(polled.failMessage).isNull();
            assertThat(polled.added).containsExactly("a");
            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("tag=b");
        }
    }

    /// A value that cannot be converted, as PLC4X throws for an array, counts as that tag's failure, and the other
    /// tags are published.
    @Test
    void valueThatCannotBeConverted_publishesTheOtherTags() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A, TAG_B), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of(
                "a",
                ok(1),
                "b",
                new DefaultPlcResponseItem<>(PlcResponseCode.OK, new PlcList(List.of(new PlcDINT(2)))))));

        try (final CapturedLog log = new CapturedLog()) {
            final RecordingOutput polled = adapter.poll();

            assertThat(polled.failMessage).isNull();
            assertThat(polled.added).containsExactly("a");
            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("tag=b");
        }
    }

    /// A new run warns again about a tag that still cannot be read: the warning is once per tag and run.
    @Test
    void tagStillFailingAfterARestart_isWarnedAboutAgain() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A, TAG_B), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(
                response(Map.of("a", ok(1), "b", new DefaultPlcResponseItem<>(PlcResponseCode.ACCESS_DENIED, null))));

        try (final CapturedLog log = new CapturedLog()) {
            adapter.poll();
            adapter.stop(mock(ProtocolAdapterStopInput.class), mock(ProtocolAdapterStopOutput.class));
            adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
            adapter.awaitConnected();
            final RecordingOutput afterRestart = adapter.poll();
            await().atMost(TIMEOUT).until(afterRestart::completed);

            assertThat(log.messages(Level.WARN))
                    .filteredOn(message -> message.contains("tag=b"))
                    .hasSize(2);
        }
    }

    /// A poll that reads a tag after polls that failed sets the adapter's status from ERROR back to CONNECTED.
    @Test
    void partialRead_afterAnError_setsTheStatusToConnected() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A, TAG_B), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        clearInvocations(adapter.state);
        when(adapter.state.getConnectionStatus()).thenReturn(ERROR);
        adapter.answer = () -> CompletableFuture.completedFuture(
                response(Map.of("a", ok(1), "b", new DefaultPlcResponseItem<>(PlcResponseCode.ACCESS_DENIED, null))));

        final RecordingOutput polled = adapter.poll();

        assertThat(polled.failMessage).isNull();
        verify(adapter.state).setConnectionStatus(CONNECTED);
    }

    /// One tag the device cannot read does not hide the others: the tags read are published, the poll succeeds, and
    /// the failing tag is warned about once until it is read again.
    @Test
    void partialRead_publishesTheTagsReadAndWarnsOncePerTag() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A, TAG_B), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(
                response(Map.of("a", ok(1), "b", new DefaultPlcResponseItem<>(PlcResponseCode.ACCESS_DENIED, null))));

        try (final CapturedLog log = new CapturedLog()) {
            final RecordingOutput first = adapter.poll();
            final RecordingOutput second = adapter.poll();

            assertThat(first.failMessage).isNull();
            assertThat(first.added).containsExactly("a");
            assertThat(second.added).containsExactly("a");
            assertThat(log.messages(Level.WARN))
                    .singleElement()
                    .asString()
                    .contains("tag=b")
                    .contains("responseCode=ACCESS_DENIED");

            adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(1), "b", ok(2))));
            final RecordingOutput recovered = adapter.poll();
            assertThat(recovered.added).containsExactlyInAnyOrder("a", "b");
            assertThat(log.messages(Level.INFO))
                    .anySatisfy(message ->
                            assertThat(message).contains("read again").contains("tag=b"));
        }
    }

    /// A poll in which no tag could be read is a polling error.
    @Test
    void readWithNoTagRead_failsThePoll() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A, TAG_B), SAMPLER_TIMEOUT_MILLIS);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of(
                "a",
                new DefaultPlcResponseItem<>(PlcResponseCode.NOT_FOUND, null),
                "b",
                new DefaultPlcResponseItem<>(PlcResponseCode.ACCESS_DENIED, null))));

        final RecordingOutput output = adapter.poll();

        assertThat(output.failMessage).contains("no tag could be read");
        assertThat(output.published).isFalse();
    }

    /// A tag answered without a value that can be published counts as not read: the other tags are published, the
    /// tag is warned about once, and a poll with no value at all fails.
    @Test
    void tagAnsweredWithoutAValue_countsAsNotRead() {
        final LifetimeAdapter adapter = new LifetimeAdapter(List.of(TAG_A, TAG_B), SAMPLER_TIMEOUT_MILLIS, false);
        adapter.start(startInput(), mock(ProtocolAdapterStartOutput.class));
        adapter.awaitConnected();
        adapter.answer = () -> CompletableFuture.completedFuture(response(Map.of("a", ok(1), "b", okWithoutValue())));

        try (final CapturedLog log = new CapturedLog()) {
            final RecordingOutput first = adapter.poll();
            final RecordingOutput second = adapter.poll();

            assertThat(first.failMessage).isNull();
            assertThat(first.added).containsExactly("a");
            assertThat(second.added).containsExactly("a");
            assertThat(log.messages(Level.WARN)).singleElement().asString().contains("tag=b");
        }

        adapter.answer =
                () -> CompletableFuture.completedFuture(response(Map.of("a", okWithoutValue(), "b", okWithoutValue())));
        final RecordingOutput none = adapter.poll();
        assertThat(none.failMessage).contains("no tag could be read");
    }

    private static @NotNull Plc4xTag tag(final @NotNull String name) {
        return new Plc4xTag(name, null, new Plc4xTagDefinition("%DB1:0", Plc4xDataType.DATA_TYPE.DINT));
    }

    private static @NotNull PlcResponseItem<PlcValue> ok(final int value) {
        return new DefaultPlcResponseItem<>(PlcResponseCode.OK, new PlcDINT(value));
    }

    private static @NotNull PlcResponseItem<PlcValue> okWithoutValue() {
        return new DefaultPlcResponseItem<>(PlcResponseCode.OK, null);
    }

    private static @NotNull PlcReadResponse response(final @NotNull Map<String, PlcResponseItem<PlcValue>> items) {
        return new DefaultPlcReadResponse(mock(PlcReadRequest.class), new LinkedHashMap<>(items));
    }

    private static @NotNull ProtocolAdapterStartInput startInput() {
        final ProtocolAdapterStartInput input = mock(ProtocolAdapterStartInput.class);
        final ModuleServices moduleServices = mock(ModuleServices.class);
        when(moduleServices.eventService()).thenReturn(mock(EventService.class));
        when(input.moduleServices()).thenReturn(moduleServices);
        return input;
    }

    /// An S7-configured adapter built with a chosen sampler timeout, whose connections connect when the gate opens
    /// and answer reads as the test says.
    private static final class LifetimeAdapter
            extends AbstractPlc4xAdapter<S7SpecificAdapterConfig, Plc4xToMqttMapping> {

        final @NotNull ProtocolAdapterState state;
        final @NotNull List<ScriptedConnection> connections = new CopyOnWriteArrayList<>();
        final @NotNull AtomicInteger reads = new AtomicInteger();
        // Set by the test thread. Read when a connection is created, on the thread that starts the attempt (the test
        // thread in start() or poll()).
        volatile @NotNull CountDownLatch connectGate = new CountDownLatch(0);
        // Set by the test thread, read by the thread that starts a read: the test thread in poll(), a connect thread
        // after a connection attempt or after the previous read.
        volatile @NotNull Supplier<CompletableFuture<PlcReadResponse>> answer =
                () -> CompletableFuture.completedFuture(response(Map.of()));
        // Set by the test thread, read by the thread that disconnects (the test thread in stop(), a poll that releases
        // a lost connection).
        volatile boolean readsFailOnClose = true;

        LifetimeAdapter(final @NotNull List<Plc4xTag> tags, final long samplerTimeoutMillis) {
            this(tags, samplerTimeoutMillis, true);
        }

        LifetimeAdapter(
                final @NotNull List<Plc4xTag> tags,
                final long samplerTimeoutMillis,
                final boolean publishChangedDataOnly) {
            this(tags, samplerTimeoutMillis, publishChangedDataOnly, mock(ProtocolAdapterState.class));
        }

        private LifetimeAdapter(
                final @NotNull List<Plc4xTag> tags,
                final long samplerTimeoutMillis,
                final boolean publishChangedDataOnly,
                final @NotNull ProtocolAdapterState state) {
            super(
                    S7ProtocolAdapterInformation.INSTANCE,
                    newInput(tags, publishChangedDataOnly, state),
                    samplerTimeoutMillis);
            this.state = state;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static @NotNull ProtocolAdapterInput<S7SpecificAdapterConfig> newInput(
                final @NotNull List<Plc4xTag> tags,
                final boolean publishChangedDataOnly,
                final @NotNull ProtocolAdapterState state) {
            final ProtocolAdapterInput<S7SpecificAdapterConfig> input = mock(ProtocolAdapterInput.class);
            when(input.getConfig())
                    .thenReturn(new S7SpecificAdapterConfig(
                            102,
                            "localhost",
                            S7SpecificAdapterConfig.ControllerType.S7_1500,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            new S7ToMqttConfig(null, null, publishChangedDataOnly)));
            when(input.getTags()).thenReturn((List) tags);
            when(input.getAdapterId()).thenReturn("lifetime");
            when(input.getProtocolAdapterState()).thenReturn(state);
            return input;
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
        protected @NotNull Plc4xConnection<S7SpecificAdapterConfig> createConnection() throws Plc4xException {
            final ScriptedConnection created = new ScriptedConnection(this);
            connections.add(created);
            return created;
        }

        @NotNull
        RecordingOutput poll() {
            final RecordingOutput output = new RecordingOutput();
            poll(output);
            return output;
        }

        void poll(final @NotNull RecordingOutput output) {
            poll(mock(BatchPollingInput.class), output);
        }

        void awaitConnected() {
            await().atMost(TIMEOUT).until(() -> {
                final Plc4xConnection<S7SpecificAdapterConfig> current = connection;
                return current != null && current.isConnected() && !isConnecting();
            });
        }
    }

    /// A connection that connects once the adapter's gate opens, unless it was closed first, and hands out the reads
    /// the adapter's answer supplies; closing it fails the unanswered reads, as the real connection does.
    private static final class ScriptedConnection extends Plc4xConnection<S7SpecificAdapterConfig> {

        private final @NotNull LifetimeAdapter adapter;
        private final @NotNull CountDownLatch gate;
        private final @NotNull List<CompletableFuture<PlcReadResponse>> handedOut = new CopyOnWriteArrayList<>();
        private final @NotNull CountDownLatch closed = new CountDownLatch(1);
        // Written by the connect thread, read by the thread that disconnects.
        private volatile @Nullable Thread connecting;
        // Written by the connect thread and by disconnect(), read by the adapter's poll threads.
        private volatile boolean connected;

        ScriptedConnection(final @NotNull LifetimeAdapter adapter) throws Plc4xException {
            super(PlcDriverManager.getDefault(), adapter.adapterConfig, _ -> "");
            this.adapter = adapter;
            this.gate = adapter.connectGate;
        }

        @Override
        void startConnection(
                final @NotNull EventService eventService,
                final @NotNull String adapterId,
                final @NotNull String protocolId)
                throws Plc4xException {
            connecting = Thread.currentThread();
            try {
                if (closed.getCount() == 0) {
                    throw new Plc4xException("closed while connecting");
                }
                if (!gate.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new Plc4xException("the test never opened the connect gate");
                }
            } catch (final InterruptedException _) {
                // disconnect() interrupts the connect, as the real connection does; S7 and ADS map it to a failure.
                throw new Plc4xException("interrupted while connecting");
            } finally {
                connecting = null;
            }
            if (closed.getCount() == 0) {
                throw new Plc4xException("closed while connecting");
            }
            connected = true;
        }

        @Override
        public boolean isConnected() {
            return connected;
        }

        @Override
        protected void lazyConnectionCheck() {}

        @Override
        @NotNull
        CompletableFuture<Void> connectTerminated() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public @NotNull CompletableFuture<? extends PlcReadResponse> read(final @NotNull List<Plc4xTag> tags) {
            adapter.reads.incrementAndGet();
            final CompletableFuture<PlcReadResponse> read = adapter.answer.get();
            handedOut.add(read);
            return read;
        }

        @Override
        public void disconnect() {
            connected = false;
            closed.countDown();
            final Thread connectThread = connecting;
            if (connectThread != null) {
                connectThread.interrupt();
            }
            if (adapter.readsFailOnClose) {
                for (final CompletableFuture<PlcReadResponse> read : handedOut) {
                    read.completeExceptionally(new Plc4xException("The connection was closed"));
                }
            }
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

    /// Records what a poll published or why it failed; its publish can be held back or made to throw.
    private static final class RecordingOutput implements BatchPollingOutput {

        // Written by the adapter's poll, connect or driver thread that publishes or fails, read by the test thread.
        final @NotNull List<String> added = new CopyOnWriteArrayList<>();
        // Set by the test thread before the poll, read by the publishing thread.
        volatile @Nullable CountDownLatch publishGate;
        volatile boolean publishThrows;
        // Written by the publishing or failing thread, read by the test thread.
        volatile boolean publishGateExpired;
        volatile boolean published;
        volatile @Nullable String failMessage;
        volatile long failedAtNanos;

        boolean completed() {
            return published || failMessage != null;
        }

        @Override
        public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {}

        @Override
        public void addDataPoint(final @NotNull DataPoint dataPoint) {}

        @Override
        public @NotNull DataPointListBuilder dataPointListPublisher() {
            return new DataPointListBuilder() {
                @Override
                @SuppressWarnings("unchecked")
                public @NotNull DataPointBuilder<DataPointListBuilder> addDataPoint(final @NotNull Tag tag) {
                    added.add(tag.getName());
                    return mock(DataPointBuilder.class);
                }

                @Override
                public void publish() {
                    final CountDownLatch gate = publishGate;
                    if (gate != null) {
                        try {
                            publishGateExpired = !gate.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
                        } catch (final InterruptedException _) {
                            Thread.currentThread().interrupt();
                            publishGateExpired = true;
                        }
                    }
                    if (publishThrows) {
                        throw new IllegalStateException("publish failed in test");
                    }
                    published = true;
                }
            };
        }

        @Override
        public void finish() {}

        @Override
        public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
            fail(errorMessage != null ? errorMessage : String.valueOf(t.getMessage()));
        }

        @Override
        public void fail(final @NotNull String errorMessage) {
            // Written before failMessage: a thread that reads failMessage non-null then sees this time.
            failedAtNanos = System.nanoTime();
            failMessage = errorMessage;
        }
    }

    /// Captures the adapter's log lines for one test.
    private static final class CapturedLog implements AutoCloseable {

        private final @NotNull Logger logger = (Logger) LoggerFactory.getLogger(LifetimeAdapter.class);
        private final @NotNull ListAppender<ILoggingEvent> appender = new ListAppender<>();

        CapturedLog() {
            appender.start();
            logger.addAppender(appender);
        }

        @NotNull
        List<String> messages(final @NotNull Level level) {
            return appender.list.stream()
                    .filter(event -> event.getLevel() == level)
                    .map(ILoggingEvent::getFormattedMessage)
                    .toList();
        }

        @Override
        public void close() {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
