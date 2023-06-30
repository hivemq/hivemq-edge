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
package com.hivemq.bootstrap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.netty.ChannelInitializerFactory;
import com.hivemq.bootstrap.netty.NettyTcpConfiguration;
import com.hivemq.bootstrap.netty.NettyUdpConfiguration;
import com.hivemq.bootstrap.netty.udp.UdpServerChannel;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.entity.ClientWriteBufferProperties;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.entity.MqttTcpListener;
import com.hivemq.configuration.service.entity.MqttTlsTcpListener;
import com.hivemq.configuration.service.entity.MqttTlsWebsocketListener;
import com.hivemq.configuration.service.entity.MqttWebsocketListener;
import com.hivemq.configuration.service.entity.MqttsnUdpListener;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.persistence.connection.ConnectionPersistence;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

public class HiveMQEdgeNettyBootstrap {
    private static final Logger log = LoggerFactory.getLogger(HiveMQEdgeNettyBootstrap.class);
    private final @NotNull ShutdownHooks shutdownHooks;
    private final @NotNull ListenerConfigurationService listenerConfigurationService;
    private final @NotNull ConnectionPersistence connectionPersistence;
    private final @NotNull ChannelInitializerFactory channelInitializerFactory;
    private final @NotNull NettyTcpConfiguration nettyTcpConfiguration;
    private final @NotNull NettyUdpConfiguration nettyUdpConfiguration;

    public static final ClientWriteBufferProperties DEFAULT_WRITE_BUFFER_PROPERTIES =
            new ClientWriteBufferProperties(64 * 1024, 32 * 1024);

    @Inject
    public HiveMQEdgeNettyBootstrap(
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ListenerConfigurationService listenerConfigurationService,
            final @NotNull ChannelInitializerFactory channelInitializerFactory,
            final @NotNull ConnectionPersistence connectionPersistence,
            final @NotNull NettyTcpConfiguration nettyTcpConfiguration,
            final @NotNull NettyUdpConfiguration nettyUdpConfiguration) {
        this.nettyTcpConfiguration = nettyTcpConfiguration;
        this.nettyUdpConfiguration = nettyUdpConfiguration;
        this.shutdownHooks = shutdownHooks;
        this.listenerConfigurationService = listenerConfigurationService;
        this.channelInitializerFactory = channelInitializerFactory;
        this.connectionPersistence = connectionPersistence;
    }

    public @NotNull ListenableFuture<List<ListenerStartupInformation>> bootstrapServer() {

        //Adding shutdown hook for graceful shutdown
        final int shutdownTimeout = InternalConfigurations.EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT_MILLISEC;
        final int channelsShutdownTimeout = InternalConfigurations.CONNECTION_PERSISTENCE_SHUTDOWN_TIMEOUT_MILLISEC;
        shutdownHooks.add(new NettyShutdownHook(nettyTcpConfiguration.getChildEventLoopGroup(),
                nettyTcpConfiguration.getParentEventLoopGroup(),
                nettyUdpConfiguration.getChildEventLoopGroup(),
                nettyUdpConfiguration.getParentEventLoopGroup(),
                shutdownTimeout,
                channelsShutdownTimeout,
                connectionPersistence));

        final List<BindInformation> futures = new ArrayList<>();

        addDefaultListeners();

        futures.addAll(bindMqttTcpListeners(listenerConfigurationService.getTcpListeners()));
        futures.addAll(bindMqttTlsTcpListeners(listenerConfigurationService.getTlsTcpListeners()));
        futures.addAll(bindMqttWebsocketListeners(listenerConfigurationService.getWebsocketListeners()));
        futures.addAll(bindMqttTlsWebsocketListeners(listenerConfigurationService.getTlsWebsocketListeners()));
        futures.addAll(bindMqttSnUdpListeners(listenerConfigurationService.getUdpListeners()));

        return aggregatedFuture(futures);
    }

    private void addDefaultListeners() {
        if (listenerConfigurationService.getListeners().isEmpty()) {
            listenerConfigurationService.addListener(new MqttTcpListener(1883, "0.0.0.0", "tcp-listener-1883", null));
        }
    }

    private @NotNull List<BindInformation> bindMqttTcpListeners(final @NotNull List<MqttTcpListener> mqttTcpListeners) {
        log.trace("Checking TCP listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();

        for (final MqttTcpListener listener : mqttTcpListeners) {

            final ServerBootstrap b = createTcpBootstrap(nettyTcpConfiguration.getParentEventLoopGroup(),
                    nettyTcpConfiguration.getChildEventLoopGroup(),
                    listener);
            log.debug("Starting MQTT TCP listener on address {} and port {}",
                    listener.getBindAddress(),
                    listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    private @NotNull List<BindInformation> bindMqttTlsTcpListeners(final @NotNull List<MqttTlsTcpListener> mqttTlsTcpListeners) {
        log.trace("Checking TLS TCP listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();

        for (final MqttTlsTcpListener listener : mqttTlsTcpListeners) {

            final ServerBootstrap b = createTcpBootstrap(nettyTcpConfiguration.getParentEventLoopGroup(),
                    nettyTcpConfiguration.getChildEventLoopGroup(),
                    listener);
            log.debug("Starting MQTT TLS TCP listener on address {} and port {}",
                    listener.getBindAddress(),
                    listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    private @NotNull List<BindInformation> bindMqttWebsocketListeners(final @NotNull List<MqttWebsocketListener> mqttWebsocketListeners) {
        log.trace("Checking Websocket listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();

        for (final MqttWebsocketListener listener : mqttWebsocketListeners) {

            final ServerBootstrap b = createTcpBootstrap(nettyTcpConfiguration.getParentEventLoopGroup(),
                    nettyTcpConfiguration.getChildEventLoopGroup(),
                    listener);
            log.debug("Starting MQTT Websocket listener on address {} and port {}",
                    listener.getBindAddress(),
                    listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    private @NotNull List<BindInformation> bindMqttTlsWebsocketListeners(
            final @NotNull List<MqttTlsWebsocketListener> mqttTlsWebsocketListeners) {

        log.trace("Checking Websocket TLS listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();
        for (final MqttTlsWebsocketListener listener : mqttTlsWebsocketListeners) {

            final ServerBootstrap b = createTcpBootstrap(nettyTcpConfiguration.getParentEventLoopGroup(),
                    nettyTcpConfiguration.getChildEventLoopGroup(),
                    listener);
            log.debug("Starting MQTT Websocket TLS listener on address {} and port {}",
                    listener.getBindAddress(),
                    listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }

    private @NotNull List<BindInformation> bindMqttSnUdpListeners(final @NotNull List<MqttsnUdpListener> mqttsnUdpListeners) {
        log.trace("Checking UDP listeners");
        final ImmutableList.Builder<BindInformation> futures = ImmutableList.builder();
        for (final MqttsnUdpListener listener : mqttsnUdpListeners) {
            final AbstractBootstrap b = createUdpBootstrap(nettyUdpConfiguration.getParentEventLoopGroup(),
                    nettyUdpConfiguration.getChildEventLoopGroup(),
                    listener);
            log.debug("Starting MQTT-SN UDP listener on address {} and port {}",
                    listener.getBindAddress(),
                    listener.getPort());
            final ChannelFuture bind = b.bind(listener.getBindAddress(), listener.getPort());
            connectionPersistence.addServerChannel(listener.getName(), bind.channel());
            futures.add(new BindInformation(listener, bind));
        }
        return futures.build();
    }


    /**
     * Creates an aggregated future which allows to wait for all futures at once
     *
     * @param bindInformation a list of futures to aggregate
     * @return a {@link com.google.common.util.concurrent.ListenableFuture} which aggregates
     *         all given {@link io.netty.channel.ChannelFuture}s
     */
    private @NotNull ListenableFuture<List<ListenerStartupInformation>> aggregatedFuture(
            final @NotNull List<BindInformation> bindInformation) {

        final List<ListenableFuture<ListenerStartupInformation>> listenableFutures =
                bindInformation.stream().map(input -> {
                    final SettableFuture<ListenerStartupInformation> objectSettableFuture = SettableFuture.create();
                    input.getBindFuture().addListener(new UpdateGivenFutureListener(input, objectSettableFuture));
                    return objectSettableFuture;
                }).collect(Collectors.toList());
        return Futures.allAsList(listenableFutures);
    }

    private @NotNull ServerBootstrap createTcpBootstrap(
            final @NotNull EventLoopGroup bossGroup,
            final @NotNull EventLoopGroup workerGroup,
            final @NotNull Listener listener) {

        final ServerBootstrap tcpBootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(nettyTcpConfiguration.getServerSocketChannelClass())
                .childHandler(channelInitializerFactory.getChannelInitializer(listener))
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        setAdvancedOptions(tcpBootstrap);
        return tcpBootstrap;
    }

    private @NotNull AbstractBootstrap createUdpBootstrap(
            final @NotNull EventLoopGroup bossGroup,
            final @NotNull EventLoopGroup workerGroup,
            final @NotNull Listener listener) {

        return new ServerBootstrap().group(bossGroup)
                .childHandler(channelInitializerFactory.getChannelInitializer(listener))
//                .channel(UdpServerChannel.class)
                .channelFactory(() -> {
                    UdpServerChannel serverChannel = new UdpServerChannel(workerGroup);
                    return serverChannel;
                })
                .option(ChannelOption.AUTO_CLOSE, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_BROADCAST, true)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    /**
     * Sets all advanced properties
     *
     * @param b the server bootstrap
     */
    private void setAdvancedOptions(final @NotNull ServerBootstrap b) {

        final int sendBufferSize = InternalConfigurations.LISTENER_SOCKET_SEND_BUFFER_SIZE_BYTES;
        final int receiveBufferSize = InternalConfigurations.LISTENER_SOCKET_RECEIVE_BUFFER_SIZE_BYTES;

        if (sendBufferSize > -1) {
            b.childOption(ChannelOption.SO_SNDBUF, sendBufferSize);
        }
        if (receiveBufferSize > -1) {
            b.childOption(ChannelOption.SO_RCVBUF, receiveBufferSize);
        }

        final int writeBufferHigh = InternalConfigurations.LISTENER_CLIENT_WRITE_BUFFER_HIGH_THRESHOLD_BYTES;
        final int writeBufferLow = InternalConfigurations.LISTENER_CLIENT_WRITE_BUFFER_LOW_THRESHOLD_BYTES;

        final ClientWriteBufferProperties properties =
                validateWriteBufferProperties(new ClientWriteBufferProperties(writeBufferHigh, writeBufferLow));

        //it is assumed that the ClientWriteBufferProperties that the listener returns was validated by Validators.validateWriteBufferProperties()
        b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                new WriteBufferWaterMark(properties.getLowThresholdBytes(), properties.getHighThresholdBytes()));
    }

    @VisibleForTesting
    public static @NotNull ClientWriteBufferProperties validateWriteBufferProperties(
            @NotNull final ClientWriteBufferProperties writeBufferProperties) {

        checkNotNull(writeBufferProperties, "writeBufferProperties must not be null");

        if (validateWriteBufferThresholds(writeBufferProperties.getHighThresholdBytes(),
                writeBufferProperties.getLowThresholdBytes())) {
            return writeBufferProperties;
        }
        return DEFAULT_WRITE_BUFFER_PROPERTIES;
    }

    private static boolean validateWriteBufferThresholds(final int high, final int low) {
        if (low <= 0) {
            log.warn("write-buffer low-threshold must be greater than zero");
            return false;
        }
        if (high < low) {
            log.warn("write-buffer high-threshold must be greater than write-buffer low-threshold");
            return false;
        }
        return true;
    }

    @Immutable
    private static class UpdateGivenFutureListener implements ChannelFutureListener {

        private final @NotNull BindInformation bindInformation;
        private final @NotNull SettableFuture<ListenerStartupInformation> settableFuture;

        UpdateGivenFutureListener(
                final @NotNull BindInformation bindInformation,
                final @NotNull SettableFuture<ListenerStartupInformation> settableFuture) {
            this.bindInformation = bindInformation;
            this.settableFuture = settableFuture;
        }

        @Override
        public void operationComplete(final @NotNull ChannelFuture future) throws Exception {
            final Listener listener = bindInformation.getListener();
            InetSocketAddress address = (InetSocketAddress) future.channel().localAddress();
            if (address != null) {
                listener.setPort(address.getPort());
            }
            if (future.isSuccess()) {
                settableFuture.set(ListenerStartupInformation.successfulListenerStartup(listener.getPort(), listener));
            } else {
                settableFuture.set(ListenerStartupInformation.failedListenerStartup(listener.getPort(),
                        listener,
                        future.cause()));
            }
        }
    }
}
