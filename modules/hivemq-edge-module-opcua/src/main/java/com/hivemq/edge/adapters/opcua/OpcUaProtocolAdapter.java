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
package com.hivemq.edge.adapters.opcua;

import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryInput;
import com.hivemq.adapter.sdk.api.discovery.ProtocolAdapterDiscoveryOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationInput;
import com.hivemq.adapter.sdk.api.schema.TagSchemaCreationOutput;
import com.hivemq.adapter.sdk.api.state.ProtocolAdapterState;
import com.hivemq.adapter.sdk.api.writing.WritingInput;
import com.hivemq.adapter.sdk.api.writing.WritingOutput;
import com.hivemq.adapter.sdk.api.writing.WritingPayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.tag.OpcuaTag;
import com.hivemq.edge.adapters.opcua.mqtt2opcua.OpcUaPayload;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpcUaProtocolAdapter implements WritingProtocolAdapter {
    private static final @NotNull Logger log = LoggerFactory.getLogger(OpcUaProtocolAdapter.class);

    private final @NotNull ProtocolAdapterInformation adapterInformation;
    private final @NotNull ProtocolAdapterState protocolAdapterState;
    private final @NotNull String adapterId;
    private final @Nullable OpcUaConnection opcUaConnection;

    public OpcUaProtocolAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<OpcUaSpecificAdapterConfig> input) {
        this.adapterId = input.getAdapterId();
        this.adapterInformation = adapterInformation;
        this.protocolAdapterState = input.getProtocolAdapterState();
        this.opcUaConnection = new OpcUaConnection(
                input.getConfig().getUri(),
                input.getTags().stream().map(tag -> (OpcuaTag)tag).toList(),
                input.getConfig().getOpcuaToMqttConfig(),
                input.moduleServices().protocolAdapterTagStreamingService(),
                input.adapterFactories().dataPointFactory(),
                input.moduleServices().eventService(),
                adapterId,
                protocolAdapterState);
    }

    @Override
    public @NotNull String getId() {
        return adapterId;
    }

    @Override
    public void start(
            final @NotNull ProtocolAdapterStartInput input, final @NotNull ProtocolAdapterStartOutput output) {
        log.info("Starting OpcUa protocol adapter {}", adapterId);
        opcUaConnection
            .start()
            .whenComplete((ignored, throwable) ->{
                if(throwable != null) {
                    log.error("Unable to connect and subscribe to the OPC UA server", throwable);
                    output.failStart(throwable, "Unable to connect and subscribe to the OPC UA server");
                } else {
                    log.info("Successfully started OpcUa protocol adapter {}", adapterId);
                    output.startedSuccessfully();
                }
            });
    }

    @Override
    public void stop(final @NotNull ProtocolAdapterStopInput input, final @NotNull ProtocolAdapterStopOutput output) {
        log.info("Stopping OpcUa protocol adapter {}", adapterId);
        opcUaConnection
            .stop()
            .whenComplete((ignored, throwable) ->{
                if(throwable != null) {
                    log.error("Unable to stop the connection to the OPC UA server", throwable);
                    output.failStop(throwable, "Unable to stop the connection to the OPC UA server");
                } else {
                    log.info("Successfully stopped OpcUa protocol adapter {}", adapterId);
                    output.stoppedSuccessfully();
                }
            });
    }

    @Override
    public void discoverValues(
            final @NotNull ProtocolAdapterDiscoveryInput input, final @NotNull ProtocolAdapterDiscoveryOutput output) {
        //TODO implement discovery
        output.finish();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public void write(final @NotNull WritingInput input, final @NotNull WritingOutput output) {
//        final OpcUaClientWrapper opcUaClientWrapperTemp = opcUaClientWrapper;
//        final WritingContext writeContext = input.getWritingContext();
//        if (opcUaClientWrapperTemp != null) {
//            tags.stream()
//                    .filter(tag -> tag.getName().equals(writeContext.getTagName()))
//                    .findFirst()
//                    .ifPresentOrElse(def -> opcUaClientWrapperTemp.write(input, output, (OpcuaTag) def),
//                            () -> output.fail("Subscription for protocol adapter failed because the used tag '" +
//                                    writeContext.getTagName() +
//                                    "' was not found. For the subscription to work the tag must be created via REST API or the UI."));
//        } else {
//            log.warn("Tried executing write while client wasn't started");
//        }
    }

    @Override
    public void createTagSchema(
            final @NotNull TagSchemaCreationInput input, final @NotNull TagSchemaCreationOutput output) {
        //TODO implement createTagSchema
    }

    @Override
    public @NotNull Class<? extends WritingPayload> getMqttPayloadClass() {
        return OpcUaPayload.class;
    }

    @VisibleForTesting
    public @NotNull ProtocolAdapterState getProtocolAdapterState() {
        return protocolAdapterState;
    }


}
