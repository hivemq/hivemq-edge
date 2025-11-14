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

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.adapters.opcua.Constants;
import org.eclipse.milo.opcua.sdk.client.ServiceFaultListener;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;

public class OpcUaServiceFaultListener implements ServiceFaultListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaServiceFaultListener.class);
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @Nullable Runnable reconnectionCallback;
    private final boolean reconnectOnServiceFault;

    public OpcUaServiceFaultListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @Nullable final Runnable reconnectionCallback,
            final boolean reconnectOnServiceFault) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.reconnectionCallback = reconnectionCallback;
        this.reconnectOnServiceFault = reconnectOnServiceFault;
    }

    @Override
    public void onServiceFault(final ServiceFault serviceFault) {
        final StatusCode statusCode = serviceFault.getResponseHeader().getServiceResult();
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);

        // Check if this is a critical fault requiring immediate reconnection
        if (reconnectOnServiceFault && isCriticalFault(statusCode)) {
            log.error("Critical OPC UA service fault detected for adapter '{}': {}",
                    adapterId,
                    statusCode);

            eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withSeverity(Event.SEVERITY.ERROR)
                    .withPayload(statusCode)
                    .withMessage("Critical Service Fault detected: " + statusCode + ". Triggering reconnection.")
                    .fire();

            // Trigger reconnection if callback is available
            if (reconnectionCallback != null) {
                log.info("Triggering reconnection for adapter '{}' due to critical service fault", adapterId);
                try {
                    reconnectionCallback.run();
                } catch (final Exception e) {
                    log.error("Failed to trigger reconnection for adapter '{}'", adapterId, e);
                }
            } else {
                log.warn("Cannot trigger reconnection for adapter '{}' - no callback available", adapterId);
            }
        } else {
            // Non-critical fault or feature disabled, just log
            log.warn("OPC UA service fault detected for adapter '{}': {}",
                    adapterId,
                    statusCode);

            eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                    .withSeverity(Event.SEVERITY.WARN)
                    .withPayload(statusCode)
                    .withMessage("Service Fault detected: " + statusCode)
                    .fire();
        }
    }

    /**
     * Determines if a status code represents a critical fault that requires reconnection.
     * Critical faults are those that indicate the session or subscription is no longer valid
     * and cannot recover without reconnection.
     *
     * @param statusCode the OPC UA status code from the service fault
     * @return true if this is a critical fault requiring reconnection
     */
    private boolean isCriticalFault(final @NotNull StatusCode statusCode) {
        final long code = statusCode.getValue();
        return code == StatusCodes.Bad_SessionIdInvalid ||
                code == StatusCodes.Bad_NoSubscription ||
                code == StatusCodes.Bad_SessionClosed ||
                code == StatusCodes.Bad_SecureChannelClosed ||
                code == StatusCodes.Bad_SubscriptionIdInvalid ||
                code == StatusCodes.Bad_IdentityTokenInvalid;
    }
}
