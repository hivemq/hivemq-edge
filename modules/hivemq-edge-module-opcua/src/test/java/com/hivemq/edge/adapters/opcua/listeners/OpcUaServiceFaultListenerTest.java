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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.events.model.EventBuilder;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.adapters.opcua.Constants;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The reporting half of {@link OpcUaServiceFaultListener}: whether a fault is an operator-visible error or the
 * ordinary noise of a connection being discarded.
 * <p>
 * Discarding a connection deletes its subscription while its client is still live, so a publish already in
 * flight answers {@code Bad_NoSubscription}. Reporting that at ERROR made every shutdown look like a failure
 * (EDG-942). A fault names neither client nor session, so a listener cannot ask about the origin of the fault
 * -- it can only know the connection it was built for, which is what {@code forConnection} is for.
 */
class OpcUaServiceFaultListenerTest {

    private final @NotNull ProtocolAdapterMetricsService metrics = mock(ProtocolAdapterMetricsService.class);
    private final @NotNull EventService events = mock(EventService.class, RETURNS_DEEP_STUBS);

    private @NotNull ServiceFault faultWith(final long statusCode) {
        final ServiceFault fault = mock(ServiceFault.class, RETURNS_DEEP_STUBS);
        when(fault.getResponseHeader().getServiceResult()).thenReturn(new StatusCode(statusCode));
        return fault;
    }

    private @NotNull EventBuilder stubbedEventBuilder() {
        final EventBuilder builder = mock(EventBuilder.class, RETURNS_DEEP_STUBS);
        when(events.createAdapterEvent(anyString(), anyString())).thenReturn(builder);
        when(builder.withSeverity(any())).thenReturn(builder);
        when(builder.withPayload(any())).thenReturn(builder);
        when(builder.withMessage(anyString())).thenReturn(builder);
        return builder;
    }

    private @NotNull OpcUaServiceFaultListener template(
            final @NotNull Runnable reconnect, final boolean autoReconnect) {
        return new OpcUaServiceFaultListener(metrics, events, "adapter", reconnect, autoReconnect);
    }

    @Test
    void onServiceFault_whenCriticalOnADiscardedConnection_firesNoEventAndDoesNotReconnect() {
        final AtomicBoolean reconnected = new AtomicBoolean();
        final OpcUaServiceFaultListener listener =
                template(() -> reconnected.set(true), true).forConnection(() -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The whole point: a fault raised by the close itself is not an operator's problem, so nothing
        // reaches the event log that a shutdown would have to explain. And a connection already discarded is
        // not the one to recover, so no reconnect is triggered from it either.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
        assertThat(reconnected).isFalse();
    }

    @Test
    void onServiceFault_whenCriticalOnALiveConnection_firesAnErrorEventAndReconnects() {
        final AtomicBoolean reconnected = new AtomicBoolean();
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener =
                template(() -> reconnected.set(true), true).forConnection(() -> false);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The same fault on a live connection is unchanged: still an error, still a reconnect. Without this
        // the quiet branch could swallow everything and the suite would not notice.
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
        assertThat(reconnected).isTrue();
    }

    @Test
    void onServiceFault_onADiscardedConnectionWithAutoReconnectOff_isStillQuiet() {
        final OpcUaServiceFaultListener listener = template(() -> {}, false).forConnection(() -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Whether a fault counts as critical depends on the operator's autoReconnect setting, so a discard
        // check inside the critical branch would leave the identical fault noisy for anyone who turned
        // auto-reconnect off. Being discarded is a property of the connection, not of that setting.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void onServiceFault_onADiscardedConnection_stillCountsTheFault() {
        final OpcUaServiceFaultListener listener = template(() -> {}, true).forConnection(() -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Quieter, not invisible. The metric is what a fault rate is measured from, and a subscription torn
        // down during a close is still a service fault that happened.
        verify(metrics).increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);
    }

    @Test
    void onServiceFault_whenNotCriticalOnALiveConnection_keepsItsWarning() {
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener = template(() -> {}, true).forConnection(() -> false);

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // On a live connection nothing changes for a non-critical fault: still a WARN, still an event.
        verify(builder).withSeverity(Event.SEVERITY.WARN);
    }

    @Test
    void onServiceFault_whenNotCriticalOnADiscardedConnection_isQuiet() {
        final OpcUaServiceFaultListener listener = template(() -> {}, true).forConnection(() -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // A fault raised while the connection is being discarded is noise whatever its status code. The codes
        // worth distinguishing are the ones that say what to recover from, and nothing is recovering.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void onServiceFault_onAnUnboundTemplate_reportsCriticalFaultsAsBefore() {
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener = template(() -> {}, true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The adapter's template belongs to no connection, so nothing can say the fault is expected. The
        // louder answer is the safe default: silence has to be earned by a connection taking responsibility
        // for it. In production a template is never attached to a client, only derived from.
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }

    @Test
    void forConnection_bindsEachCopyToItsOwnConnection() {
        final OpcUaServiceFaultListener shared = template(() -> {}, true);

        // The defect this design removes. An adapter can have two connections alive at once -- a replacement
        // starting while the connection it replaces is still closing -- and both derive from one template.
        // Each copy answers for its own connection, so a discarded one going quiet cannot silence a live one.
        final OpcUaServiceFaultListener onDiscarded = shared.forConnection(() -> true);
        final OpcUaServiceFaultListener onLive = shared.forConnection(() -> false);

        onDiscarded.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));
        verify(events, never()).createAdapterEvent(anyString(), anyString());

        final EventBuilder builder = stubbedEventBuilder();
        onLive.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }

    @Test
    void forConnection_derivingAgainDoesNotDisturbAnEarlierCopy() {
        final OpcUaServiceFaultListener shared = template(() -> {}, true);
        final OpcUaServiceFaultListener onLive = shared.forConnection(() -> false);

        // Order independence is what makes this safe against a slow start. Deriving a second copy after the
        // first -- the replacement attaching while the superseded connection is still starting -- must not
        // reach back into the first. With a single shared pointer it did, and a late-starting discarded
        // connection could silence the live one.
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener unused = shared.forConnection(() -> true);
        onLive.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }
}
