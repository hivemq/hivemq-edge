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
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.parameter.ProtocolAdapterInboundProviderInput;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.parameter.ProtocolAdapterInformation;

public class ProtocolAdapterInboundProviderInputImpl implements ProtocolAdapterInboundProviderInput {

    private final @NotNull ServerInformation serverInformation;
    private final @NotNull ProtocolAdapterInformation adapterInformation;

    public ProtocolAdapterInboundProviderInputImpl(
            final @NotNull ServerInformation serverInformation,
            final @NotNull ProtocolAdapterInformation adapterInformation) {
        this.serverInformation = serverInformation;
        this.adapterInformation = adapterInformation;
    }

    @Override
    public @NotNull ServerInformation getServerInformation() {
        return serverInformation;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getProtocolAdapterInformation() {
        return adapterInformation;
    }
}
