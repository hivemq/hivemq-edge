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
package com.hivemq.bootstrap.netty.initializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static util.TlsTestUtil.createDefaultTLS;

import com.hivemq.bootstrap.netty.ChannelDependencies;
import com.hivemq.bootstrap.netty.ChannelInitializerFactoryImpl;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.security.ssl.NonSslHandler;
import com.hivemq.security.ssl.SslFactory;
import io.netty.channel.Channel;
import jakarta.inject.Provider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@SuppressWarnings("NullabilityAnnotations")
public class ChannelInitializerFactoryImplTest {

    private static final String TYPE_TCP = "TCP";
    private static final String TYPE_TLS_TCP = "TYPE_TLS_TCP";
    private static final String TYPE_WEBSOCKET = "TYPE_WEBSOCKET";
    private static final String TYPE_TLS_WEBSOCKET = "TYPE_TLS_WEBSOCKET";

    @Mock
    private ChannelDependencies channelDependencies;

    @Mock
    private SslFactory sslFactory;

    @Mock
    private Provider<NonSslHandler> nonSslHandlerProvider;

    @Mock
    private ConfigurationService fullConfigurationService;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;

    private ChannelInitializerFactoryImpl channelInitializerFactory;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        when(channelDependencies.getConfigurationService()).thenReturn(fullConfigurationService);
        when(channelDependencies.getRestrictionsConfigurationService()).thenReturn(restrictionsConfigurationService);
        when(restrictionsConfigurationService.incomingLimit()).thenReturn(0L);
        channelInitializerFactory =
                new TestChannelInitializerFactory(channelDependencies, sslFactory, nonSslHandlerProvider);
    }

    @Test
    public void test_get_tcp_initializer() {
        final MqttTcpListener mqttTcpListener = new MqttTcpListener(0, "0");

        final AbstractChannelInitializer initializer = channelInitializerFactory.getChannelInitializer(mqttTcpListener);

        assertEquals(TYPE_TCP, ((FakeAbstractChannelInitializer) initializer).getType());
    }

    @Test
    public void test_get_tls_tcp_initializer() {

        final Tls tls = createDefaultTLS();
        final MqttTlsTcpListener mqttTlsTcpListener = new MqttTlsTcpListener(0, "0", tls);

        final AbstractChannelInitializer initializer =
                channelInitializerFactory.getChannelInitializer(mqttTlsTcpListener);

        assertEquals(TYPE_TLS_TCP, ((FakeAbstractChannelInitializer) initializer).getType());
    }

    @Test
    public void test_get_websocket_initializer() {

        final MqttWebsocketListener mqttWebsocketListener =
                new MqttWebsocketListener.Builder().bindAddress("0").port(0).build();

        final AbstractChannelInitializer initializer =
                channelInitializerFactory.getChannelInitializer(mqttWebsocketListener);

        assertEquals(TYPE_WEBSOCKET, ((FakeAbstractChannelInitializer) initializer).getType());
    }

    @Test
    public void test_get_tls_websocket_initializer() {

        final Tls tls = createDefaultTLS();

        final MqttTlsWebsocketListener websocketListener = new MqttTlsWebsocketListener.Builder()
                .bindAddress("0")
                .port(0)
                .tls(tls)
                .build();

        final AbstractChannelInitializer initializer =
                channelInitializerFactory.getChannelInitializer(websocketListener);

        assertEquals(TYPE_TLS_WEBSOCKET, ((FakeAbstractChannelInitializer) initializer).getType());
    }

    @Test
    public void test_create_tcp() {

        channelInitializerFactory =
                new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider);
        final MqttTcpListener mqttTcpListener = new MqttTcpListener(0, "0");
        final AbstractChannelInitializer channelInitializer =
                channelInitializerFactory.getChannelInitializer(mqttTcpListener);
        assertTrue(channelInitializer instanceof TcpChannelInitializer);
    }

    @Test
    public void test_create_tcp_tls() {

        channelInitializerFactory =
                new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider);
        final Tls tls = createDefaultTLS();
        final MqttTlsTcpListener mqttTlsTcpListener = new MqttTlsTcpListener(0, "0", tls);
        final AbstractChannelInitializer channelInitializer =
                channelInitializerFactory.getChannelInitializer(mqttTlsTcpListener);
        assertTrue(channelInitializer instanceof TlsTcpChannelInitializer);
    }

    @Test
    public void test_create_websocket() {

        channelInitializerFactory =
                new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider);
        final MqttWebsocketListener mqttWebsocketListener =
                new MqttWebsocketListener.Builder().bindAddress("0").port(0).build();
        final AbstractChannelInitializer channelInitializer =
                channelInitializerFactory.getChannelInitializer(mqttWebsocketListener);
        assertTrue(channelInitializer instanceof WebsocketChannelInitializer);
    }

    @Test
    public void test_create_websocket_tls() {

        channelInitializerFactory =
                new ChannelInitializerFactoryImpl(channelDependencies, sslFactory, nonSslHandlerProvider);
        final Tls tls = createDefaultTLS();

        final MqttTlsWebsocketListener websocketListener = new MqttTlsWebsocketListener.Builder()
                .bindAddress("0")
                .port(0)
                .tls(tls)
                .build();
        final AbstractChannelInitializer channelInitializer =
                channelInitializerFactory.getChannelInitializer(websocketListener);
        assertTrue(channelInitializer instanceof TlsWebsocketChannelInitializer);
    }

    @SuppressWarnings("NullabilityAnnotations")
    private class TestChannelInitializerFactory extends ChannelInitializerFactoryImpl {

        TestChannelInitializerFactory(
                final ChannelDependencies channelDependencies,
                final SslFactory sslFactory,
                final Provider<NonSslHandler> nonSslHandlerProvider) {
            super(channelDependencies, sslFactory, nonSslHandlerProvider);
        }

        @NotNull
        @Override
        protected AbstractChannelInitializer createTcpInitializer(final @NotNull MqttTcpListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_TCP);
        }

        @NotNull
        @Override
        protected AbstractChannelInitializer createTlsTcpInitializer(final @NotNull MqttTlsTcpListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_TLS_TCP);
        }

        @NotNull
        @Override
        protected AbstractChannelInitializer createWebsocketInitializer(final @NotNull MqttWebsocketListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_WEBSOCKET);
        }

        @NotNull
        @Override
        protected AbstractChannelInitializer createTlsWebsocketInitializer(
                final @NotNull MqttTlsWebsocketListener listener) {
            return new FakeAbstractChannelInitializer(channelDependencies, TYPE_TLS_WEBSOCKET);
        }
    }

    private static class FakeAbstractChannelInitializer extends AbstractChannelInitializer {

        private final String type;

        public FakeAbstractChannelInitializer(final ChannelDependencies channelDependencies, final String type) {
            super(channelDependencies, new FakeListener());
            this.type = type;
        }

        @Override
        protected void addSpecialHandlers(final @NotNull Channel ch) {
            // no-op
        }

        public String getType() {
            return type;
        }
    }

    private static class FakeListener implements Listener {

        @Override
        public int getPort() {
            return 0;
        }

        @Override
        public void setPort(final int port) {}

        @Override
        public String getBindAddress() {
            return null;
        }

        @Override
        public String getReadableName() {
            return null;
        }

        @Override
        public @NotNull String getName() {
            return "listener";
        }

        @Override
        public @Nullable String getExternalHostname() {
            return null;
        }
    }
}
