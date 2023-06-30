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
package com.hivemq.extensions.services.interceptor;

import com.google.common.base.Preconditions;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.ProtocolAdapterPublishInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.services.interceptor.EdgeInterceptorRegistry;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EdgeInterceptorRegistryImpl implements EdgeInterceptorRegistry {

    @NotNull
    private final Interceptors interceptors;

    @Inject
    public EdgeInterceptorRegistryImpl(@NotNull final Interceptors interceptors) {
        this.interceptors = interceptors;
    }

    @Override
    public void setBridgeInboundInterceptorProvider(@NotNull final BridgePublishInboundInterceptorProvider bridgeInboundInterceptorProvider) {
        Preconditions.checkNotNull(bridgeInboundInterceptorProvider,
                "Bridge inbound interceptor provider must never be null");
        interceptors.addBridgeInboundInterceptorProvider(bridgeInboundInterceptorProvider);
    }

    @Override
    public void setBridgeOutboundInterceptorProvider(@NotNull final BridgePublishOutboundInterceptorProvider bridgeOutboundInterceptorProvider) {
        Preconditions.checkNotNull(bridgeOutboundInterceptorProvider,
                "Bridge outbound nterceptor provider must never be null");
        interceptors.addBridgeOutboundInterceptorProvider(bridgeOutboundInterceptorProvider);
    }

    @Override
    public void setProtocolAdapterInboundInterceptorProvider(@NotNull final ProtocolAdapterPublishInboundInterceptorProvider protocolAdapterInboundInterceptorProvider) {
        Preconditions.checkNotNull(protocolAdapterInboundInterceptorProvider,
                "Protocol adapter inbound interceptor provider must never be null");
        interceptors.addProtocolAdapterInboundInterceptorProvider(protocolAdapterInboundInterceptorProvider);
    }
}
