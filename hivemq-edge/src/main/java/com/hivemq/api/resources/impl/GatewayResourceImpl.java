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

import com.google.common.collect.ImmutableList;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.config.ApiListener;
import com.hivemq.api.config.HttpsListener;
import com.hivemq.api.model.components.*;
import com.hivemq.api.resources.FrontendApi;
import com.hivemq.api.resources.GatewayApi;
import com.hivemq.api.utils.ApiUtils;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.HiveMQEdgeRemoteService;
import com.hivemq.edge.ModulesAndExtensionsService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterManager;
import org.glassfish.jersey.internal.util.collection.StringIgnoreCaseKeyComparator;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.OutputStreamWriter;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class GatewayResourceImpl extends AbstractApi implements GatewayApi {

    private final @NotNull ConfigurationService configurationService;

    @Inject
    public GatewayResourceImpl(
            final @NotNull ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public @NotNull Response getXmlConfiguration() {
        return Response.ok((StreamingOutput) output ->
                configurationService.writeConfiguration(new OutputStreamWriter(
                    output))).build();
    }

    @Override
    public @NotNull Response getListeners() {

        //-- Netty configured objects
        ImmutableList.Builder<Listener> builder = new ImmutableList.Builder<>();
        configurationService.listenerConfiguration()
                .getListeners()
                .stream()
                .map(this::convertListener)
                .forEachOrdered(builder::add);

        //-- Api objects
        configurationService.apiConfiguration()
                .getListeners()
                .stream()
                .map(this::convertApiListener)
                .forEachOrdered(builder::add);

        return Response.ok(new ListenerList(builder.build())).build();
    }

    private Listener convertListener(com.hivemq.configuration.service.entity.Listener listener) {
        return new Listener(listener.getName(),
                listener.getBindAddress(),
                listener.getPort(),
                listener.getReadableName(),
                listener.getExternalHostname(),
                getTransportForPort(listener.getPort()),
                getProtocolForPort(listener.getPort()));
    }

    private Listener convertApiListener(ApiListener listener) {

        String protocol = getProtocolForPort(listener.getPort());
        String listenerName = String.format("%s-listener-%s", protocol, listener.getPort());

        return new Listener(listenerName,
                listener.getBindAddress(),
                listener.getPort(),
                String.format("Api %s Listener", protocol.toLowerCase()), null,
                getTransportForPort(listener.getPort()),
                getProtocolForPort(listener.getPort()));
    }


    private String getProtocolForPort(int port) {
        //-- Uses IANA ports to map, otherwise its unknown
        //-- TODO Add element to config for protocol
        switch(port){
            case 8080:
            case 80:
            case 3000:
                return "http";
            case 8443:
            case 443:
                return "https";
            case 1883:
                return "mqtt";
            case 8883:
                return "mqtt (secure)";
            case 2442:
                return "mqtt-sn";
            default:
                return null;
        }
    }

    private Listener.TRANSPORT getTransportForPort(int port) {
        //-- Uses IANA ports to map, otherwise its unknown
        //-- TODO Add element to config for protocol
        switch(port){
            case 8080:
            case 80:
            case 3000:
            case 8443:
            case 443:
            case 1883:
            case 8883:
                return Listener.TRANSPORT.TCP;
            case 2442:
                return Listener.TRANSPORT.UDP;
            default:
                return null;
        }
    }
}
