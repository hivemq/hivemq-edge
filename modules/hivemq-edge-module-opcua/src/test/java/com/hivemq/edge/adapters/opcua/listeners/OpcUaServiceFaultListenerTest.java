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
 * The reporting half of {@link OpcUaServiceFaultListener}: whether a critical fault is an operator-visible
 * error or the ordinary noise of a connection being closed.
 * <p>
 * Closing a connection deletes its subscription while this listener is still attached, so a publish already
 * in flight answers {@code Bad_NoSubscription}. Reporting that at ERROR made every shutdown look like a
 * failure (EDG-942). These tests pin that the distinction is made, and that it is made on the teardown
 * question alone -- a fault outside teardown keeps every bit of its old behaviour.
 */
class OpcUaServiceFaultListenerTest {

    private final @NotNull ProtocolAdapterMetricsService metrics = mock(ProtocolAdapterMetricsService.class);
    private final @NotNull EventService events = mock(EventService.class, RETURNS_DEEP_STUBS);

    private @NotNull ServiceFault faultWith(final long statusCode) {
        final ServiceFault fault = mock(ServiceFault.class, RETURNS_DEEP_STUBS);
        when(fault.getResponseHeader().getServiceResult()).thenReturn(new StatusCode(statusCode));
        return fault;
    }

    @Test
    void onServiceFault_whenCriticalWhileStopping_firesNoEventAndDoesNotReconnect() {
        final AtomicBoolean reconnected = new AtomicBoolean();
        final OpcUaServiceFaultListener listener = new OpcUaServiceFaultListener(
                metrics, events, "adapter", () -> reconnected.set(true), true, () -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The whole point of the change: a fault raised by the close itself is not an operator's problem, so
        // nothing reaches the event log that a shutdown would have to explain.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
        assertThat(reconnected).isFalse();
    }

    @Test
    void onServiceFault_whenCriticalWhileRunning_firesAnErrorEventAndReconnects() {
        final AtomicBoolean reconnected = new AtomicBoolean();
        final EventBuilder builder = mock(EventBuilder.class, RETURNS_DEEP_STUBS);
        when(events.createAdapterEvent(anyString(), anyString())).thenReturn(builder);
        when(builder.withSeverity(any())).thenReturn(builder);
        when(builder.withPayload(any())).thenReturn(builder);
        when(builder.withMessage(anyString())).thenReturn(builder);

        final OpcUaServiceFaultListener listener = new OpcUaServiceFaultListener(
                metrics, events, "adapter", () -> reconnected.set(true), true, () -> false);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // The same fault outside teardown is unchanged: still an error, still a reconnect. Without this the
        // quiet branch could swallow everything and the suite would not notice.
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
        assertThat(reconnected).isTrue();
    }

    @Test
    void onServiceFault_whileStopping_stillCountsTheFault() {
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true, () -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Quieter, not invisible. The metric is what a fault rate is measured from, and a subscription torn
        // down during shutdown is still a service fault that happened.
        verify(metrics).increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);
    }

    @Test
    void onServiceFault_whileStoppingWithAutoReconnectOff_isStillQuiet() {
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, false, () -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // Whether a fault counts as critical depends on the operator's autoReconnect setting, so a teardown
        // check inside the critical branch would leave the identical shutdown fault noisy for anyone who
        // turned auto-reconnect off. Teardown is a property of the adapter, not of that setting.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void onServiceFault_whenNotCriticalWhileRunning_keepsItsWarning() {
        final EventBuilder builder = mock(EventBuilder.class, RETURNS_DEEP_STUBS);
        when(events.createAdapterEvent(anyString(), anyString())).thenReturn(builder);
        when(builder.withSeverity(any())).thenReturn(builder);
        when(builder.withPayload(any())).thenReturn(builder);
        when(builder.withMessage(anyString())).thenReturn(builder);

        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true, () -> false);

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // Outside teardown nothing changes for a non-critical fault: still a WARN, still an event.
        verify(builder).withSeverity(Event.SEVERITY.WARN);
    }

    @Test
    void onServiceFault_whenNotCriticalWhileStopping_isQuiet() {
        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true, () -> true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_Timeout));

        // A fault raised while the connection is being closed is teardown noise whatever its status code. The
        // codes worth distinguishing are the ones that say what to recover from, and nothing is recovering.
        verify(events, never()).createAdapterEvent(anyString(), anyString());
    }

    @Test
    void onServiceFault_defaultConstructor_reportsCriticalFaultsAsBefore() {
        final EventBuilder builder = mock(EventBuilder.class, RETURNS_DEEP_STUBS);
        when(events.createAdapterEvent(anyString(), anyString())).thenReturn(builder);
        when(builder.withSeverity(any())).thenReturn(builder);
        when(builder.withPayload(any())).thenReturn(builder);
        when(builder.withMessage(anyString())).thenReturn(builder);

        final OpcUaServiceFaultListener listener =
                new OpcUaServiceFaultListener(metrics, events, "adapter", () -> {}, true);

        listener.onServiceFault(faultWith(StatusCodes.Bad_NoSubscription));

        // A caller with no adapter to ask gets the louder answer, not the quieter one.
        verify(builder).withSeverity(Event.SEVERITY.ERROR);
    }
}
