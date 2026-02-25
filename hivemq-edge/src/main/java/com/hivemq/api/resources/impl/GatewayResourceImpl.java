/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.api.resources.impl;

import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.model.components.Listener;
import com.hivemq.api.model.components.ListenerList;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.api.GatewayEndpointApi;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import org.jetbrains.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public class GatewayResourceImpl extends AbstractApi implements GatewayEndpointApi {

    private final @NotNull ConfigurationService configurationService;

    @Inject
    public GatewayResourceImpl(final @NotNull ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public @NotNull Response getXmlConfiguration() {
        return Response.ok((StreamingOutput)
                        output -> configurationService.writeConfiguration(new OutputStreamWriter(output)))
                .build();
    }

    @Override
    public @NotNull Response getListeners() {

        // -- Netty configured objects
        final ImmutableList.Builder<Listener> builder = new ImmutableList.Builder<>();
        configurationService.listenerConfiguration().getListeners().stream()
                .map(this::convertListener)
                .forEachOrdered(builder::add);

        // -- Api objects
        configurationService.apiConfiguration().getListeners().stream()
                .map(this::convertApiListener)
                .forEachOrdered(builder::add);

        return Response.ok(new ListenerList(builder.build())).build();
    }

    private Listener convertListener(final com.hivemq.configuration.service.entity.Listener listener) {
        return new Listener(
                listener.getName(),
                listener.getBindAddress(),
                listener.getPort(),
                listener.getReadableName(),
                listener.getExternalHostname(),
                getTransportForPort(listener.getPort()),
                getProtocolForPort(listener.getPort()));
    }

    private Listener convertApiListener(final ApiListener listener) {
        final String protocol = requireNonNullElse(getProtocolForPort(listener.getPort()), "unknown")
                .toLowerCase();
        final String name = protocol + "-listener-" + listener.getPort();
        final String description = "Api " + protocol + " Listener";
        return new Listener(
                name,
                listener.getBindAddress(),
                listener.getPort(),
                description,
                null,
                requireNonNullElse(getTransportForPort(listener.getPort()), Listener.TRANSPORT.TCP),
                requireNonNullElse(getProtocolForPort(listener.getPort()), "mqtt"));
    }

    private String getProtocolForPort(final int port) {
        // -- Uses IANA ports to map, otherwise its unknown
        // -- TODO Add element to config for protocol
        return switch (port) {
            case 8080, 80, 3000 -> "http";
            case 8443, 443 -> "https";
            case 1883 -> "mqtt";
            case 8883 -> "mqtt (secure)";
            case 2442 -> "mqtt-sn";
            default -> null;
        };
    }

    private Listener.TRANSPORT getTransportForPort(final int port) {
        // -- Uses IANA ports to map, otherwise its unknown
        // -- TODO Add element to config for protocol
        return switch (port) {
            case 8080, 80, 3000, 8443, 443, 1883, 8883 -> Listener.TRANSPORT.TCP;
            case 2442 -> Listener.TRANSPORT.UDP;
            default -> null;
        };
    }
}
