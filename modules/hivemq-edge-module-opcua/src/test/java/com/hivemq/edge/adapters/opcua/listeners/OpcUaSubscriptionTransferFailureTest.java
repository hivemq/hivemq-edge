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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * What the handler believes about its subscription after the server refuses to transfer one.
 * <p>
 * Review finding 5. {@code onTransferFailed} scheduled a rebuild but never cleared the reference to the
 * subscription that had just been refused, and the reference is replaced only once a <em>replacement</em>
 * reaches {@code established()}. So if the rebuild produced nothing — creation refused, or every monitored
 * item rejected — the handler went on naming a subscription whose id means nothing on the new session.
 * <p>
 * Everything downstream then inherits that wrong answer: {@code onSessionReactivated} requests a refresh
 * against the dead id, and a southbound write to a refresh tag reports success on a call that cannot land.
 * The adapter looks healthy while monitoring nothing, which is the same class of symptom as the partial-sync
 * defect the second audit found — a green adapter that has quietly stopped doing its job.
 * <p>
 * Driven white-box: reaching {@code established()} honestly needs a live server to create a subscription and
 * synchronize items against, and the case under test is precisely the one where that fails.
 */
class OpcUaSubscriptionTransferFailureTest {

    @Test
    void aRefusedTransferClearsTheSubscriptionItRefused() {
        final OpcUaSubscriptionLifecycleHandler handler = handler();
        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        record(handler, broken);

        assertThat(handler.currentSubscriptionForTesting())
                .as("precondition: the handler holds the subscription that is about to be refused")
                .isSameAs(broken);

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));

        assertThat(handler.currentSubscriptionForTesting())
                .as("a subscription the server refused to transfer names nothing on the new session")
                .isNull();
    }

    @Test
    void andASouthboundRefreshThenSaysThereIsNothingToRefresh() {
        // The user-visible half. Before this, a refresh command reported success against an id the server had
        // already disowned -- an answer worse than the honest failure, because it says the alarm picture was
        // resynchronised when nothing happened.
        final OpcUaSubscriptionLifecycleHandler handler = handler();
        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        record(handler, broken);

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));

        assertThat(handler.requestConditionRefreshNow())
                .as("no subscription is established, and saying so is the point")
                .isEmpty();
    }

    @Test
    void aReplacementThatArrivedFirstIsNotClearedByALateCallback() {
        // compareAndSet rather than set. The rebuild runs on its own executor, so a callback arriving after
        // it finished must not undo it -- that would turn a recovered adapter back into a broken one.
        final OpcUaSubscriptionLifecycleHandler handler = handler();
        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        final OpcUaSubscription replacement = mock(OpcUaSubscription.class);
        record(handler, replacement);

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));

        assertThat(handler.currentSubscriptionForTesting())
                .as("the replacement must survive a callback about the subscription it replaced")
                .isSameAs(replacement);
    }

    // ── review-02 finding 3: and it must not be replaced a second time either ───────────────────────

    @Test
    void andNoSecondRebuildIsStartedForIt() throws Exception {
        // The half the assertion above could not see. Leaving the replacement in the slot was only the first
        // requirement; the callback went on to schedule a rebuild regardless, on the strength of a
        // compareAndSet whose result nobody read. That rebuild ends at established(), which *overwrites* the
        // slot -- so the healthy replacement is dropped from the handler's view while still holding its
        // listener and its monitored items. It keeps publishing every transition a second time, on a server
        // subscription nothing left here can delete.
        //
        // Asserted through the client rather than through the slot, because the slot is exactly what stops
        // showing the problem once the second rebuild lands.
        final OpcUaClient client = mock(OpcUaClient.class);
        final OpcUaSubscriptionLifecycleHandler handler = handler(client);
        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        record(handler, mock(OpcUaSubscription.class));

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        awaitRecoveryQueue(handler);

        assertThat(mockingDetails(client).getInvocations())
                .as("no rebuild may be attempted for a subscription the handler has already moved on from")
                .isEmpty();
    }

    @Test
    void aGenuineTransferFailureStillStartsOne() throws Exception {
        // The guard must not swallow the case it exists to narrow. When the callback does name the current
        // subscription there is nothing established to protect, and the rebuild is the whole point of the
        // callback. This is also what stops the test above from passing vacuously: the same assertion, on the
        // same mock, comes out the other way.
        final OpcUaClient client = mock(OpcUaClient.class);
        final OpcUaSubscriptionLifecycleHandler handler = handler(client);
        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        record(handler, broken);

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        awaitRecoveryQueue(handler);

        assertThat(mockingDetails(client).getInvocations())
                .as("the rebuild this callback exists to start")
                .isNotEmpty();
    }

    @Test
    void aSecondCallbackAboutTheSameBrokenSubscriptionIsIgnored() throws Exception {
        // The commonest shape of the late callback, and the one that needs no replacement to have been
        // installed at all: the first call clears the slot to null, so the second and third find nothing
        // matching and must not queue rebuilds of their own behind the one already running.
        final OpcUaClient client = mock(OpcUaClient.class);
        final OpcUaSubscriptionLifecycleHandler handler = handler(client);
        final OpcUaSubscription broken = mock(OpcUaSubscription.class);
        record(handler, broken);

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        awaitRecoveryQueue(handler);
        final int afterOneRebuild = mockingDetails(client).getInvocations().size();
        assertThat(afterOneRebuild)
                .as("precondition: the first callback did start a rebuild")
                .isNotZero();

        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        handler.onTransferFailed(broken, new StatusCode(StatusCodes.Bad_SubscriptionIdInvalid));
        awaitRecoveryQueue(handler);

        assertThat(mockingDetails(client).getInvocations())
                .as("one rebuild for one broken subscription, however many times the server says so")
                .hasSize(afterOneRebuild);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /**
     * Waits until the recovery executor has run everything queued before this call.
     * <p>
     * The executor is single-threaded and FIFO, so a task submitted now completes only after every task
     * submitted earlier has. That turns "nothing was scheduled" from a race into an assertion.
     */
    private static void awaitRecoveryQueue(final @NotNull OpcUaSubscriptionLifecycleHandler handler) throws Exception {
        recoveryExecutor(handler).submit(() -> {}).get(10, TimeUnit.SECONDS);
    }

    private static @NotNull ExecutorService recoveryExecutor(final @NotNull OpcUaSubscriptionLifecycleHandler handler) {
        try {
            final Field field = OpcUaSubscriptionLifecycleHandler.class.getDeclaredField("recoveryExecutor");
            field.setAccessible(true);
            return (ExecutorService) field.get(handler);
        } catch (final ReflectiveOperationException e) {
            throw new LinkageError("the handler no longer rebuilds on a 'recoveryExecutor'", e);
        }
    }

    /**
     * Puts a subscription in the slot {@code established()} would fill.
     * <p>
     * Reflection because there is no honest way in: {@code established()} is reached only through a
     * successful create-and-synchronize against a real server, and these tests are about what happens when
     * that fails.
     */
    private static void record(
            final @NotNull OpcUaSubscriptionLifecycleHandler handler, final @NotNull OpcUaSubscription subscription) {
        try {
            final Field field = OpcUaSubscriptionLifecycleHandler.class.getDeclaredField("currentSubscription");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            final AtomicReference<OpcUaSubscription> slot = (AtomicReference<OpcUaSubscription>) field.get(handler);
            slot.set(subscription);
        } catch (final ReflectiveOperationException e) {
            // LinkageError rather than AssertionError: this fails only when the field has been renamed or
            // removed, which is a broken assumption about the class rather than a failed assertion about
            // its behaviour -- and the message is what tells the next reader which of the two it is.
            throw new LinkageError("the handler no longer holds its subscription in 'currentSubscription'", e);
        }
    }

    private static @NotNull OpcUaSubscriptionLifecycleHandler handler() {
        return handler(mock(OpcUaClient.class));
    }

    private static @NotNull OpcUaSubscriptionLifecycleHandler handler(final @NotNull OpcUaClient client) {
        final OpcuaTag tag = new OpcuaTag(
                "boiler-high-temp",
                "a condition tag",
                new OpcuaTagDefinition(
                        "ns=2;s=Boiler1.HighTemp", OpcuaTagKind.CONDITION, OpcuaConditionType.ALARM_CONDITION));
        return new OpcUaSubscriptionLifecycleHandler(
                mock(ProtocolAdapterMetricsService.class),
                mock(ProtocolAdapterTagStreamingService.class),
                new FakeEventService(),
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
