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
package com.hivemq.extensions.interceptor.bridge.parameter;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.bridge.parameter.BridgePublishOutboundInput;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;

public class BridgePublishOutboundInputImpl implements BridgePublishOutboundInput, PluginTaskInput {

    private final @NotNull BridgeInformationImpl bridgeInformation;
    private final @NotNull PublishPacketImpl packet;

    public BridgePublishOutboundInputImpl(
            final @NotNull BridgeInformationImpl bridgeInformation, final @NotNull PublishPacketImpl packet) {
        this.bridgeInformation = bridgeInformation;
        this.packet = packet;
    }

    @Override
    public @NotNull PublishPacketImpl getPublishPacket() {
        return packet;
    }

    @Override
    public BridgeInformationImpl getBridgeInformation() {
        return bridgeInformation;
    }

    public @NotNull BridgePublishOutboundInputImpl update(final @NotNull BridgePublishOutboundOutputImpl output) {
        return new BridgePublishOutboundInputImpl(bridgeInformation, output.getPublishPacket().copy());
    }

}
