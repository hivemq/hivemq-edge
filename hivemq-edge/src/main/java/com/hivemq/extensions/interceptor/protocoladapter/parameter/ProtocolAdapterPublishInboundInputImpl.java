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
package com.hivemq.extensions.interceptor.protocoladapter.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.parameter.ProtocolAdapterDynamicContext;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.parameter.ProtocolAdapterPublishInboundInput;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;

public class ProtocolAdapterPublishInboundInputImpl implements ProtocolAdapterPublishInboundInput, PluginTaskInput {

    private final @NotNull ProtocolAdapterInformationImpl adapterInformation;
    private final @NotNull PublishPacketImpl packet;
    private final @NotNull ProtocolAdapterDynamicContextImpl dynamicContext;

    public ProtocolAdapterPublishInboundInputImpl(
            final @NotNull ProtocolAdapterInformationImpl adapterInformation,
            final @NotNull PublishPacketImpl packet,
            final @NotNull ProtocolAdapterDynamicContextImpl dynamicContext) {
        this.adapterInformation = adapterInformation;
        this.packet = packet;
        this.dynamicContext = dynamicContext;
    }

    @Override
    public @NotNull PublishPacketImpl getPublishPacket() {
        return packet;
    }

    @Override
    public @NotNull ProtocolAdapterInformationImpl getProtocolAdapterInformation() {
        return adapterInformation;
    }

    @Override
    public @NotNull ProtocolAdapterDynamicContext getProtocolAdapterDynamicContext() {
        return dynamicContext;
    }

    public @NotNull ProtocolAdapterPublishInboundInputImpl update(final @NotNull ProtocolAdapterPublishInboundOutputImpl output) {
        return new ProtocolAdapterPublishInboundInputImpl(adapterInformation,
                output.getPublishPacket().copy(),
                dynamicContext);
    }

}
