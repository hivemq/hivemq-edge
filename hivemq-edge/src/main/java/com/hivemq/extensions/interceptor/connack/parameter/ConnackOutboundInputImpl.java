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
package com.hivemq.extensions.interceptor.connack.parameter;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.connack.parameter.ConnackOutboundInput;
import com.hivemq.extensions.executor.task.PluginTaskInput;
import com.hivemq.extensions.packets.connack.ConnackPacketImpl;

/**
 * @author Florian Limpöck
 * @author Silvio Giebl
 * @since 4.2.0
 */
@Immutable
public class ConnackOutboundInputImpl implements ConnackOutboundInput, PluginTaskInput {

    private final @NotNull ClientInformation clientInformation;
    private final @NotNull ConnectionInformation connectionInformation;
    private final @NotNull ConnackPacketImpl connackPacket;

    public ConnackOutboundInputImpl(
            final @NotNull ClientInformation clientInformation,
            final @NotNull ConnectionInformation connectionInformation,
            final @NotNull ConnackPacketImpl connackPacket) {

        this.clientInformation = clientInformation;
        this.connectionInformation = connectionInformation;
        this.connackPacket = connackPacket;
    }

    @Override
    public @NotNull ClientInformation getClientInformation() {
        return clientInformation;
    }

    @Override
    public @NotNull ConnectionInformation getConnectionInformation() {
        return connectionInformation;
    }

    @Override
    public @NotNull ConnackPacketImpl getConnackPacket() {
        return connackPacket;
    }

    public @NotNull ConnackOutboundInputImpl update(final @NotNull ConnackOutboundOutputImpl output) {
        return new ConnackOutboundInputImpl(clientInformation, connectionInformation, output.getConnackPacket().copy());
    }
}
