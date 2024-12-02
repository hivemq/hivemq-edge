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
package com.hivemq.bootstrap.netty;

import com.hivemq.bootstrap.netty.initializer.*;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.logging.EventLog;
import com.hivemq.security.ssl.NonSslHandler;
import com.hivemq.security.ssl.SslFactory;


import javax.inject.Inject;
import javax.inject.Provider;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Christoph Schäbel
 */
public class ChannelInitializerFactoryImpl implements ChannelInitializerFactory {

    @NotNull
    private final ChannelDependencies channelDependencies;

    @NotNull
    private final SslFactory sslFactory;

    @NotNull
    private final Provider<NonSslHandler> nonSslHandlerProvider;

    @NotNull
    private final EventLog eventLog;

    @Inject
    public ChannelInitializerFactoryImpl(final @NotNull ChannelDependencies channelDependencies,
                                         final @NotNull SslFactory sslFactory,
                                         final @NotNull Provider<NonSslHandler> nonSslHandlerProvider,
                                         final @NotNull EventLog eventLog) {
        this.channelDependencies = channelDependencies;
        this.sslFactory = sslFactory;
        this.nonSslHandlerProvider = nonSslHandlerProvider;
        this.eventLog = eventLog;
    }

    @NotNull
    public AbstractChannelInitializer getChannelInitializer(final @NotNull Listener listener) {

        checkNotNull(listener, "Listener must not be null");

        if (listener instanceof MqttTcpListener) {

            if (listener instanceof MqttTlsTcpListener) {
                return createTlsTcpInitializer((MqttTlsTcpListener) listener);
            } else {
                return createTcpInitializer((MqttTcpListener) listener);
            }
        }

        if (listener instanceof MqttWebsocketListener) {

            if (listener instanceof MqttTlsWebsocketListener) {
                return createTlsWebsocketInitializer((MqttTlsWebsocketListener) listener);
            } else {
                return createWebsocketInitializer((MqttWebsocketListener) listener);
            }
        }

        if (listener instanceof MqttsnUdpListener) {
            return createUdpInitializer((MqttsnUdpListener) listener);
        }

        throw new IllegalArgumentException("Unknown listener type");
    }

    @NotNull
    protected AbstractChannelInitializer createTcpInitializer(final @NotNull MqttTcpListener listener) {
        return new TcpChannelInitializer(channelDependencies, listener, nonSslHandlerProvider);
    }

    @NotNull
    protected AbstractChannelInitializer createUdpInitializer(final @NotNull MqttsnUdpListener listener) {
        return new UdpChannelInitializer(channelDependencies, listener, nonSslHandlerProvider);
    }

    @NotNull
    protected AbstractChannelInitializer createTlsTcpInitializer(final @NotNull MqttTlsTcpListener listener) {
        sslFactory.verifySslAtBootstrap(listener, listener.getTls());
        return new TlsTcpChannelInitializer(channelDependencies, listener, sslFactory);
    }

    @NotNull
    protected AbstractChannelInitializer createWebsocketInitializer(final @NotNull MqttWebsocketListener listener) {
        return new WebsocketChannelInitializer(channelDependencies, listener, nonSslHandlerProvider);
    }

    @NotNull
    protected AbstractChannelInitializer createTlsWebsocketInitializer(final @NotNull MqttTlsWebsocketListener listener) {
        sslFactory.verifySslAtBootstrap(listener, listener.getTls());
        return new TlsWebsocketChannelInitializer(channelDependencies, listener, sslFactory);
    }

}
