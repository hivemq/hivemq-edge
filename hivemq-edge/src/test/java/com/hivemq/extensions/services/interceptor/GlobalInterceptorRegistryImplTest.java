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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hivemq.extension.sdk.api.interceptor.connack.ConnackOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptorProvider;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @since 4.2.0
 */
public class GlobalInterceptorRegistryImplTest {

    private final @NotNull Interceptors interceptors = mock(Interceptors.class);

    private @NotNull GlobalInterceptorRegistryImpl globalInterceptorRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        globalInterceptorRegistry = new GlobalInterceptorRegistryImpl(interceptors);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_add_null_connect_inbound() {
        assertThatThrownBy(() -> globalInterceptorRegistry.setConnectInboundInterceptorProvider(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void test_add_null_connack_outbound() {
        assertThatThrownBy(() -> globalInterceptorRegistry.setConnackOutboundInterceptorProvider(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void test_add_connect_inbound_success() {
        globalInterceptorRegistry.setConnectInboundInterceptorProvider(e -> null);
        verify(interceptors).addConnectInboundInterceptorProvider(any(ConnectInboundInterceptorProvider.class));
    }

    @Test
    public void test_add_connack_outbound_success() {
        globalInterceptorRegistry.setConnackOutboundInterceptorProvider(e -> null);
        verify(interceptors).addConnackOutboundInterceptorProvider(any(ConnackOutboundInterceptorProvider.class));
    }
}
