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
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.edge.adapters.opcua.Constants;
import org.eclipse.milo.opcua.sdk.client.SessionActivityListener;
import org.eclipse.milo.opcua.sdk.client.UaSession;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.adapter.sdk.api.state.ProtocolAdapterState.ConnectionStatus.CONNECTED;
import static com.hivemq.edge.adapters.opcua.Constants.PROTOCOL_ID_OPCUA;

public class OpcUaSessionActivityListener implements SessionActivityListener {

    private static final Logger log = LoggerFactory.getLogger(OpcUaSessionActivityListener.class);

    private final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService;
    private final @NotNull EventService eventService;
    private final @NotNull String adapterId;
    private final @NotNull ProtocolAdapterState protocolAdapterState;

    public OpcUaSessionActivityListener(
            @NotNull final ProtocolAdapterMetricsService protocolAdapterMetricsService,
            @NotNull final EventService eventService,
            @NotNull final String adapterId,
            @NotNull final ProtocolAdapterState protocolAdapterState) {
        this.protocolAdapterMetricsService = protocolAdapterMetricsService;
        this.eventService = eventService;
        this.adapterId = adapterId;
        this.protocolAdapterState = protocolAdapterState;
    }

    @Override
    public void onSessionInactive(final @NotNull UaSession session) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_INACTIVE_COUNT);
        eventService.createAdapterEvent(adapterId, PROTOCOL_ID_OPCUA)
                .withSeverity(Event.SEVERITY.WARN)
                .withPayload(session.getSessionName() + '/' + session.getSessionId())
                .withMessage("Adapter '" + adapterId + "' session has been disconnected.")
                .fire();
        protocolAdapterState.setConnectionStatus(ProtocolAdapterState.ConnectionStatus.DISCONNECTED);
        log.info("OPC UA client of protocol adapter '{}' disconnected: {}", adapterId, session);
    }

    @Override
    public void onSessionActive(final @NotNull UaSession session) {
        protocolAdapterMetricsService.increment(Constants.METRIC_SESSION_ACTIVE_COUNT);
        protocolAdapterState.setConnectionStatus(CONNECTED);
        log.info("OPC UA client of protocol adapter '{}' connected: {}", adapterId, session);
    }
}
