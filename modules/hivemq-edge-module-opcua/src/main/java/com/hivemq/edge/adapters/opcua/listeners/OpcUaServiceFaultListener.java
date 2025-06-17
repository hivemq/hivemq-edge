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
import org.eclipse.milo.opcua.stack.core.types.structured.ServiceFault;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;

public class OpcUaServiceFaultListener implements ServiceFaultListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaServiceFaultListener.class);
    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;

    public OpcUaServiceFaultListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterId = adapterId;
    }

    @Override
    public void onServiceFault(final ServiceFault serviceFault) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SUBSCRIPTION_SERVICE_FAULT_COUNT);
        log.info("OPC UA client of protocol adapter '{}' detected a service fault: {}", adapterId, serviceFault);
        eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withSeverity(Event.SEVERITY.ERROR)
                .withPayload(serviceFault.getResponseHeader().getServiceResult())
                .withMessage("A Service Fault was Detected.")
                .fire();
    }
}
