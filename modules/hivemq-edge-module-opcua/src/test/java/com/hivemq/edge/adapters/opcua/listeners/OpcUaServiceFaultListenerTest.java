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
import java.util.function.BooleanSupplier;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The reporting half of {@link OpcUaServiceFaultListener}: whether a fault is an operator-visible error or the
 * ordinary noise of a connection being discarded.
 * <p>
 * Discarding a connection deletes its subscription while this listener is still attached, so a publish already
 * in flight answers {@code Bad_NoSubscription}. Reporting that at ERROR made every shutdown look like a
 * failure (EDG-942). These tests pin that the distinction is made, that it is made on the discard question
 * alone -- a fault on a live connection keeps every bit of its old behaviour -- and that the watch a
 * connection installs cannot outlive it or clobber its replacement's.
 */
class OpcUaServiceFaultListenerTest {

    // Constants rather than method references, and ErrorProne's UnnecessaryLambda suggestion is wrong here:
    // the listener withdraws a watch by identity, so these tests need two stable objects they can hand over
    // and later name again. A fresh method reference at each use would be a different object every time and
    // the withdrawal cases below would pass for the wrong reason.
    @SuppressWarnings("UnnecessaryLambda")
    private static final @NotNull BooleanSupplier LIVE = () -> false;

    @SuppressWarnings("UnnecessaryLambda")
    private static final @NotNull BooleanSupplier DISCARDED = () -> true;

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

    @Test
    void onServiceFault_whenCriticalOnADiscardedConnection_firesNoEventAndDoesNotReconnect() {
        final AtomicBoolean reconnected = new AtomicBoolean();
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> reconnected.set(true), true);
        listener.watch(DISCARDED);

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
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> reconnected.set(true), true);
        listener.watch(LIVE);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The same fault on a live connection is unchanged: still an error, still a reconnect. Without this
        // the quiet branch could swallow everything and the suite would not notice.
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
        assertThat(reconnected).isTrue();
    }

    @Test
    void onServiceFault_onADiscardedConnectionWithAutoReconnectOff_isStillQuiet() {
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, false);
        listener.watch(DISCARDED);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Whether a fault counts as critical depends on the operator's autoReconnect setting, so a discard
        // check inside the critical branch would leave the identical fault noisy for anyone who turned
        // auto-reconnect off. Being discarded is a property of the connection, not of that setting.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void onServiceFault_onADiscardedConnection_stillCountsTheFault() {
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true);
        listener.watch(DISCARDED);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Quieter, not invisible. The metric is what a fault rate is measured from, and a subscription torn
        // down during a close is still a service fault that happened.
        verify(metrics).increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);
    }

    @Test
    void onServiceFault_whenNotCriticalOnALiveConnection_keepsItsWarning() {
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true);
        listener.watch(LIVE);

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // On a live connection nothing changes for a non-critical fault: still a WARN, still an event.
        verify(builder).withSeverity(Event.SEVERITY.WARN);
    }

    @Test
    void onServiceFault_whenNotCriticalOnADiscardedConnection_isQuiet() {
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true);
        listener.watch(DISCARDED);

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // A fault raised while the connection is being discarded is noise whatever its status code. The codes
        // worth distinguishing are the ones that say what to recover from, and nothing is recovering.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void onServiceFault_withNobodyWatching_reportsCriticalFaultsAsBefore() {
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // No connection has claimed this listener, so there is nothing to say the fault is expected. The
        // louder answer is the safe default: silence would have to be earned by someone taking responsibility
        // for it.
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }

    @Test
    void unwatch_fromASupersededConnection_leavesTheReplacementsWatchAlone() {
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true);

        // The reconnect ordering: the replacement attaches before the connection it replaces has finished
        // closing, and that old connection then withdraws. A blind clear here would leave the listener
        // answering "live" for a connection that may since have been discarded -- silencing nothing, but
        // reporting a discarded replacement's shutdown noise as a fault all over again.
        listener.watch(LIVE);
        listener.watch(DISCARDED);
        listener.unwatch(LIVE);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void unwatch_fromTheCurrentConnection_restoresTheLouderDefault() {
        final EventBuilder builder = stubbedEventBuilder();
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true);

        // A connection that closes with no replacement takes its answer with it. What is left is nobody
        // watching, which reports rather than suppresses -- a listener must never keep quoting a connection
        // that has gone.
        listener.watch(DISCARDED);
        listener.unwatch(DISCARDED);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }
}
