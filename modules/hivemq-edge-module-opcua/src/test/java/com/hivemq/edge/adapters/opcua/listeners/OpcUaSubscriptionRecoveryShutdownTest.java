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
package com.hivemq.edge.adapters.opcua.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaConditionType;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagKind;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What a subscription rebuild does when it loses its race with the connection being closed, and what it does
 * when it simply fails.
 * <p>
 * Review-02 finding 4. {@code recreateSubscription} is the entire body of an executor task and had neither a
 * check on {@code abandoned} nor a {@code catch}. Both gaps produce the same symptom, which is why they are
 * one finding: <b>the failure goes nowhere</b>. An exception out of the task reaches the thread's uncaught
 * handler, which prints it to {@code System.err} from a daemon thread and does nothing else —
 * {@code reportRecoveryFailed} is skipped, so the operator is never told the adapter has stopped monitoring
 * conditions, and the executor quietly replaces the dead worker so the pool looks healthy.
 * <p>
 * The forced module run in the review found exactly that: {@code NullPointerException ... getTransport() is
 * null} attributed to whichever test happened to be running when a leftover task fired, with the Gradle task
 * still green. So the two tests that go through the executor install a collector on the recovery thread and
 * assert it stays empty — a background failure has to fail a test rather than decorate an unrelated one.
 * <p>
 * The collector is set on that thread rather than as the JVM default, so nothing here can swallow an
 * exception belonging to another test. A worker that dies is replaced without it, but only after its own
 * exception has been recorded, which is the one this asserts about.
 */
class OpcUaSubscriptionRecoveryShutdownTest {

    private final @NotNull List<Throwable> uncaught = new CopyOnWriteArrayList<>();

    private @NotNull OpcUaClient client;
    private @NotNull FakeEventService events;
    private @NotNull OpcUaSubscriptionLifecycleHandler handler;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        events = new FakeEventService();
        handler = handler(client, events);
    }

    @AfterEach
    void tearDown() {
        handler.abandon();
    }

    @Test
    void aRebuildQueuedBeforeTheConnectionClosedDoesNothingAfterIt() throws Exception {
        // abandon() uses shutdown(), which refuses new tasks but still runs the ones already queued -- and
        // that is deliberate, because a rebuild already under way is better left to notice the flag than
        // interrupted mid-request against a library whose blocking calls do not watch for it. What was
        // missing is the flag being read at all. The queued task therefore ran against a client whose
        // transport had been cleared, threw, and reported nothing.
        final CountDownLatch occupied = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        recoveryExecutor(handler).execute(() -> {
            collectUncaughtOnThisThread();
            occupied.countDown();
            awaitQuietly(release);
        });
        assertThat(occupied.await(10, TimeUnit.SECONDS))
                .as("precondition: the recovery thread is busy, so the rebuild has to queue behind it")
                .isTrue();

        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        record(handler, broken);
        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));

        handler.abandon();
        release.countDown();
        assertThat(recoveryExecutor(handler).awaitTermination(10, TimeUnit.SECONDS))
                .as("the executor drains what it had already queued")
                .isTrue();

        assertThat(mockingDetails(client).getInvocations())
                .as("a closed connection is not something to build a subscription against")
                .isEmpty();
        assertThat(uncaught)
                .as("and nothing may be thrown at a daemon thread's uncaught handler")
                .isEmpty();
        assertThat(events.readEvents(null, null))
                .as("closing a connection is not a recovery failure to report: the reconnect that closed it is"
                        + " already building a replacement")
                .isEmpty();
    }

    @Test
    void aRebuildThatThrowsIsReportedToTheOperatorInsteadOfEscaping() throws Exception {
        // The exception fence. Against a client with no transport -- which is what a rebuild racing a close
        // finds, and what a mock gives for free -- Milo's subscription constructor throws. Before, that left
        // the operator with a silently unmonitored adapter and a stack trace in the output of whichever test
        // was running at the time.
        recoveryExecutor(handler).execute(this::collectUncaughtOnThisThread);
        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        record(handler, broken);

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        awaitRecoveryQueue(handler);

        assertThat(events.readEvents(null, null))
                .as("an adapter that has stopped monitoring conditions has to say so")
                .anySatisfy(event -> {
                    assertThat(event.getSeverity()).isEqualTo(Event.SEVERITY.ERROR);
                    assertThat(event.getMessage()).contains("could not rebuild its OPC UA subscription");
                });
        assertThat(uncaught)
                .as("the report replaces the uncaught exception rather than accompanying it")
                .isEmpty();
    }

    @Test
    void aReplacementCreatedAfterTheConnectionClosedIsDiscardedRatherThanInstalled() throws Exception {
        // The window the entry check cannot see: create() is a blocking server call, so the connection can go
        // away while it is in flight and leave a genuine subscription in hand. Installing it would attach this
        // handler as listener to a subscription on a closed session, and -- worse -- the reference is never
        // recorded, so nothing left here could ever delete it from the server.
        //
        // Driven directly because reaching this honestly needs a create that succeeds, which needs a live
        // server, and the case under test is one where the client is already gone.
        final OpcUaSubscription replacement = mock(OpcUaSubscription.class);
        handler.abandon();

        handler.establishReplacement(replacement);

        verify(replacement).setSubscriptionListener(null);
        verify(replacement).delete();
        assertThat(handler.currentSubscriptionForTesting())
                .as("nothing built after the connection closed may become the current subscription")
                .isNull();
    }

    @Test
    void andAFailedDeleteOfThatReplacementIsSurvivable() throws Exception {
        // The subscription is already unreachable from here and the session is closing, so there is no second
        // thing to try -- but throwing would put the exception straight back on the daemon thread this
        // finding is about.
        final OpcUaSubscription replacement = mock(OpcUaSubscription.class);
        doThrow(new IllegalStateException("the channel is gone"))
                .when(replacement)
                .delete();
        handler.abandon();

        handler.establishReplacement(replacement);

        verify(replacement).setSubscriptionListener(null);
    }

    @Test
    void aReplacementCreatedWhileTheConnectionIsOpenIsStillInstalled() throws Exception {
        // The positive control. The discard must not swallow the ordinary case, which is the whole purpose of
        // the rebuild.
        final OpcUaSubscription replacement = mock(OpcUaSubscription.class);

        try {
            handler.establishReplacement(replacement);
        } catch (final RuntimeException syncingAgainstAMockSubscription) {
            // Expected and irrelevant: synchronizing monitored items needs a real subscription. The listener
            // is attached before that runs, which is all this asserts -- what the sync then does is finding 5.
        }

        verify(replacement).setSubscriptionListener(handler);
        verify(replacement, never()).delete();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    private void collectUncaughtOnThisThread() {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> uncaught.add(throwable));
    }

    private static void awaitQuietly(final @NotNull CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Waits until the recovery executor has run everything queued before this call. */
    private static void awaitRecoveryQueue(final @NotNull OpcUaSubscriptionLifecycleHandler handler) throws Exception {
        recoveryExecutor(handler).submit(() -> {}).get(10, TimeUnit.SECONDS);
    }

    private static @NotNull ExecutorService recoveryExecutor(final @NotNull OpcUaSubscriptionLifecycleHandler handler) {
        return field(handler, "recoveryExecutor", ExecutorService.class);
    }

    @SuppressWarnings("unchecked")
    private static void record(
            final @NotNull OpcUaSubscriptionLifecycleHandler handler, final @NotNull OpcUaSubscription subscription) {
        ((AtomicReference<OpcUaSubscription>) field(handler, "currentSubscription", AtomicReference.class))
                .set(subscription);
    }

    private static <T> @NotNull T field(
            final @NotNull OpcUaSubscriptionLifecycleHandler handler,
            final @NotNull String name,
            final @NotNull Class<T> type) {
        try {
            final Field field = OpcUaSubscriptionLifecycleHandler.class.getDeclaredField(name);
            field.setAccessible(true);
            return type.cast(field.get(handler));
        } catch (final ReflectiveOperationException e) {
            // LinkageError rather than AssertionError: this fails only when the field has been renamed or
            // removed, which is a broken assumption about the class rather than a failed assertion about its
            // behaviour.
            throw new LinkageError("the handler no longer has a '" + name + "' field", e);
        }
    }

    private static @NotNull OpcUaSubscriptionLifecycleHandler handler(
            final @NotNull OpcUaClient client, final @NotNull FakeEventService events) {

        final OpcuaTag tag = new OpcuaTag(
                "boiler-high-temp",
                "a condition tag",
                new OpcuaTagDefinition(
                        "ns=2;s=Boiler1.HighTemp", OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION));
        return new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                mock(ProtocolAdapterTagStreamingService.class),
                events,
                "test-adapter",
                List.of(tag),
                client,
                new OpcUaSpecificAdapterConfig(
                        "opc.tcp://localhost:4840",
                        false,
                        null,
                        null,
                        null,
                        OpcUaToMqttConfig.defaultOpcUaToMqttConfig(),
                        null,
                        ConnectionOptions.defaultConnectionOptions()));
    }
}
