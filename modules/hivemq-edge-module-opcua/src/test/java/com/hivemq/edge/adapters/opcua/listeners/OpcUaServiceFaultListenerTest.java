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
 * Covers how {@link OpcUaServiceFaultListener} decides whether a fault from an OPC UA server is an
 * operator-visible error or the ordinary noise of a connection being closed.
 * <p>
 * Closing a connection deletes its subscription while its client is still live, so a publish already in
 * flight comes back {@code Bad_NoSubscription}. Reporting that at ERROR made every shutdown look like a
 * failure (EDG-942). A closing connection therefore calls {@code markClosed()} on the listener it owns, and
 * everything after that is reported quietly.
 * <p>
 * Two groups of tests. Those named {@code onServiceFault_*} check that decision across the cases that reach
 * it. Those named {@code forConnection_*} check that a listener serves exactly one connection: an adapter can
 * have two alive at once during a reconnect, and one falling quiet must not quieten the other.
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

    /** The adapter-level listener a connection would take its own copy from. */
    private @NotNull OpcUaServiceFaultListener template(
            final @NotNull Runnable reconnect, final boolean autoReconnect) {
        return new OpcUaServiceFaultListener(metrics, events, "adapter", reconnect, autoReconnect);
    }

    @Test
    void onServiceFault_whenCriticalOnAClosingConnection_firesNoEventAndDoesNotReconnect() {
        final AtomicBoolean reconnected = new AtomicBoolean();
        final OpcUaServiceFaultListener listener =
                template(() -> reconnected.set(true), true).forConnection();
        listener.markClosed();

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The whole point: a fault raised by the close itself is not an operator's problem, so nothing
        // reaches the event log that a shutdown would have to explain. And a connection already closing is
        // not the one to recover, so no reconnect is triggered from it either.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
        assertThat(reconnected).isFalse();
    }

    @Test
    void onServiceFault_whenCriticalOnALiveConnection_firesAnErrorEventAndReconnects() {
        final AtomicBoolean reconnected = new AtomicBoolean();
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener =
                template(() -> reconnected.set(true), true).forConnection();

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The same fault on a live connection is unchanged: still an error, still a reconnect. Without this
        // the quiet branch could swallow everything and the suite would not notice.
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
        assertThat(reconnected).isTrue();
    }

    @Test
    void onServiceFault_whileClosingWithAutoReconnectOff_isStillQuiet() {
        final OpcUaServiceFaultListener listener = template(() -> {}, false).forConnection();
        listener.markClosed();

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Whether a fault counts as critical depends on the operator's autoReconnect setting, so a check
        // inside the critical branch would leave the identical fault noisy for anyone who turned
        // auto-reconnect off. Whether a connection is closing has nothing to do with that setting.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void onServiceFault_whileClosing_stillCountsTheFault() {
        final OpcUaServiceFaultListener listener = template(() -> {}, true).forConnection();
        listener.markClosed();

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Quieter, not invisible. The metric is what a fault rate is measured from, and a subscription torn
        // down during a close is still a service fault that happened.
        verify(metrics).increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);
    }

    @Test
    void onServiceFault_whenNotCriticalOnALiveConnection_keepsItsWarning() {
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener = template(() -> {}, true).forConnection();

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // On a live connection nothing changes for a non-critical fault: still a WARN, still an event.
        verify(builder).withSeverity(Event.SEVERITY.WARN);
    }

    @Test
    void onServiceFault_whenNotCriticalOnAClosingConnection_isQuiet() {
        final OpcUaServiceFaultListener listener = template(() -> {}, true).forConnection();
        listener.markClosed();

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // A fault raised while the connection is closing is noise whatever its status code. The codes worth
        // distinguishing are the ones that say what to recover from, and nothing is recovering.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void markClosed_isIdempotent() {
        final OpcUaServiceFaultListener listener = template(() -> {}, true).forConnection();
        listener.markClosed();
        listener.markClosed();

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // stop() followed by destroy() reaches the same connection twice, so saying it again must not undo
        // it. A closed connection is never reopened.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void forConnection_givesEachConnectionAListenerThatFallsQuietOnItsOwn() {
        final OpcUaServiceFaultListener template = template(() -> {}, true);

        // The reason each connection needs its own. An adapter can have two alive at once -- a replacement
        // starting while the connection it replaces is still closing -- and only the closing one should fall
        // quiet. With a shared listener the closing connection would silence the live one's faults.
        final OpcUaServiceFaultListener onClosing = template.forConnection();
        final OpcUaServiceFaultListener onLive = template.forConnection();
        onClosing.markClosed();

        onClosing.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));
        verify(events, never()).createAdapterEvent(anyString(), anyString());

        final EventBuilder builder = stubbedEventBuilder();
        onLive.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }

    @Test
    void forConnection_leavesTheTemplateUnaffected() {
        final OpcUaServiceFaultListener template = template(() -> {}, true);
        final EventBuilder builder = stubbedEventBuilder();

        // Taking a copy and closing it must not reach back into what it was copied from, or one closing
        // connection would quieten every connection the adapter makes afterwards.
        template.forConnection().markClosed();
        template.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }
}
