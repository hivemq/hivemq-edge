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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.streaming.ProtocolAdapterTagStreamingService;
import com.hivemq.edge.adapters.opcua.FakeEventService;
import com.hivemq.edge.adapters.opcua.config.ConnectionOptions;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTagDefinition;
import java.util.List;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemServiceOperationResult;
import org.eclipse.milo.opcua.sdk.client.subscriptions.MonitoredItemSynchronizationException;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaMonitoredItem;
import org.eclipse.milo.opcua.sdk.client.subscriptions.OpcUaSubscription;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * What becomes of a subscription that was created and then turned out to be unusable.
 * <p>
 * Review-02 finding 5. A subscription exists on the server from the moment {@code create()} returns, whether
 * or not a single monitored item is ever established on it. When the item synchronization then failed
 * outright, the code reported the failure and returned — leaving the subscription on the server, this handler
 * still attached as its listener, and <b>no reference to it anywhere</b>. {@code currentSubscription} only
 * ever holds one that reached {@code established()}, so nothing left in the adapter could find it to clean it
 * up, and it would sit there until the session ended. Every failed reconnect could add another.
 * <p>
 * The review scoped this to the recovery path. It is the same code shape in {@code subscribe()}, the initial
 * connect: on a false answer it returned {@code null} into a {@code map()}, so the caller received an empty
 * {@code Optional} and had nothing to clean up with either. Both are covered here, and the disposal is one
 * shared helper as the review asked.
 */
class OpcUaSubscriptionDisposalTest {

    private @NotNull OpcUaClient client;
    private @NotNull FakeEventService events;
    private @NotNull OpcUaSubscriptionLifecycleHandler handler;

    @BeforeEach
    void setUp() {
        client = mock(OpcUaClient.class);
        events = new FakeEventService();
        handler = handler(client, events);
    }

    @Test
    void aReplacementWhoseItemsAllFailedIsDeletedRatherThanLeftOnTheServer() throws Exception {
        final OpcUaSubscription replacement = subscriptionWhoseSyncFailsEntirely();

        handler.establishReplacement(replacement);

        verify(replacement).delete(); // the only thing that releases it on the server
        assertThat(handler.currentSubscriptionForTesting())
                .as("precondition of the leak: nothing records it, so nothing else could ever delete it")
                .isNull();
    }

    @Test
    void andTheListenerComesOffBeforeItGoes() throws Exception {
        // Order matters rather than merely tidiness: this handler is the listener, so a notification arriving
        // between the failure and the delete would be published as an ordinary transition on a tag whose
        // subscription is being thrown away.
        final OpcUaSubscription replacement = subscriptionWhoseSyncFailsEntirely();

        handler.establishReplacement(replacement);

        final var order = org.mockito.Mockito.inOrder(replacement);
        order.verify(replacement).setSubscriptionListener(handler);
        order.verify(replacement).setSubscriptionListener(null);
        order.verify(replacement).delete();
    }

    @Test
    void andTheOperatorIsStillToldTheRebuildFailed() throws Exception {
        // Disposing of it is not a substitute for reporting it. The adapter is left without a subscription,
        // and the adapter's own reconnect is what recovers from here.
        final OpcUaSubscription replacement = subscriptionWhoseSyncFailsEntirely();

        handler.establishReplacement(replacement);

        assertThat(events.readEvents(null, null)).anySatisfy(event -> {
            assertThat(event.getSeverity()).isEqualTo(Event.SEVERITY.ERROR);
            assertThat(event.getMessage()).contains("could not rebuild its OPC UA subscription");
        });
    }

    @Test
    void aSubscriptionThatFailsOnTheInitialConnectIsDeletedToo() throws Exception {
        // The same shape, in the path the review did not name. subscribe() answered an empty Optional, which
        // reads as "no subscription was created" -- but one was, and that Optional was the last reference to
        // it.
        final OpcUaSubscription created = subscriptionWhoseSyncFailsEntirely();

        assertThat(handler.establishInitial(created)).isEmpty();

        verify(created).setSubscriptionListener(null);
        verify(created).delete();
    }

    @Test
    void aDeleteTheServerRefusesStillTakesItOutOfTheClient() throws Exception {
        // Milo only deregisters on success: its delete() calls reset() when the server answers Good and
        // throws otherwise, so a subscription that could not be deleted stays registered with the client and
        // its publishing manager, keeps its watchdog timer, and goes on receiving publish responses. The
        // server-side subscription is beyond reach at that point; the client-side one is not.
        final OpcUaSubscription replacement = subscriptionWhoseSyncFailsEntirely();
        doThrow(new IllegalStateException("the channel is gone"))
                .when(replacement)
                .delete();

        handler.establishReplacement(replacement);

        verify(replacement).reset();
    }

    @Test
    void aPartiallyEstablishedSubscriptionIsNotThrownAway() throws Exception {
        // The positive control, and the one that would be a serious regression to get wrong. A sync where
        // some items succeeded returns true and is established -- it is streaming those items, and deleting
        // it would take working tags off the wire to tidy up after the ones that failed.
        final OpcUaSubscription partial = subscriptionWhoseSyncPartlySucceeds();

        handler.establishReplacement(partial);

        verify(partial, never()).delete();
        assertThat(handler.currentSubscriptionForTesting())
                .as("a partial success is still a success")
                .isSameAs(partial);
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────

    /** A subscription whose synchronization fails with nothing good in it at all. */
    private static @NotNull OpcUaSubscription subscriptionWhoseSyncFailsEntirely() throws Exception {
        return subscriptionWhoseSyncFailsWith(StatusCode.BAD);
    }

    /** A subscription whose synchronization fails, but with one item already established on the server. */
    private static @NotNull OpcUaSubscription subscriptionWhoseSyncPartlySucceeds() throws Exception {
        return subscriptionWhoseSyncFailsWith(StatusCode.GOOD, StatusCode.BAD);
    }

    private static @NotNull OpcUaSubscription subscriptionWhoseSyncFailsWith(final @NotNull StatusCode... results)
            throws Exception {

        final OpcUaSubscription subscription = mock(OpcUaSubscription.class);
        when(subscription.getMonitoredItems()).thenReturn(List.of());
        final List<MonitoredItemServiceOperationResult> createResults = java.util.Arrays.stream(results)
                .map(status -> new MonitoredItemServiceOperationResult(mock(OpcUaMonitoredItem.class), status, status))
                .toList();
        doThrow(new MonitoredItemSynchronizationException(
                        "the server refused the items", createResults, List.of(), List.of()))
                .when(subscription)
                .synchronizeMonitoredItems();
        return subscription;
    }

    /**
     * A VALUE tag, deliberately. Verification of a condition tag browses the device, which a mock client
     * cannot answer, and none of that is what these tests are about.
     */
    private static @NotNull OpcUaSubscriptionLifecycleHandler handler(
            final @NotNull OpcUaClient client, final @NotNull FakeEventService events) {

        final OpcuaTag tag = new OpcuaTag(
                "boiler-temperature", "an ordinary value tag", new OpcuaTagDefinition("ns=2;s=Boiler1.Temperature"));
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
