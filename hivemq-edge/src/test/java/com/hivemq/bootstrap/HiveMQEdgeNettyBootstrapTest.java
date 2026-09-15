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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static util.TlsTestUtil.createDefaultTLS;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.netty.ChannelInitializerFactoryImpl;
import com.hivemq.bootstrap.netty.NettyTcpConfiguration;
import com.hivemq.bootstrap.netty.NettyUdpConfiguration;
import com.hivemq.bootstrap.netty.initializer.AbstractChannelInitializer;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.entity.*;
import com.hivemq.configuration.service.impl.listener.ListenerConfigurationService;
import com.hivemq.persistence.connection.ConnectionPersistence;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.RandomPortGenerator;

@SuppressWarnings("NullabilityAnnotations")
public class HiveMQEdgeNettyBootstrapTest {

    private HiveMQEdgeNettyBootstrap hiveMQNettyBootstrap;

    @Mock
    private ShutdownHooks shutdownHooks;

    @Mock
    private ListenerConfigurationService listenerConfigurationService;

    @Mock
    private ChannelInitializerFactoryImpl channelInitializerFactoryImpl;

    @Mock
    private ConnectionPersistence connectionPersistence;

    @Mock
    private AbstractChannelInitializer abstractChannelInitializer;

    private final int randomPort = RandomPortGenerator.get();

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup(1);
        hiveMQNettyBootstrap = new HiveMQEdgeNettyBootstrap(
                shutdownHooks,
                listenerConfigurationService,
                channelInitializerFactoryImpl,
                connectionPersistence,
                new NettyTcpConfiguration(NioServerSocketChannel.class, eventLoopGroup, eventLoopGroup),
                new NettyUdpConfiguration(NioDatagramChannel.class, eventLoopGroup, eventLoopGroup));

        when(channelInitializerFactoryImpl.getChannelInitializer(any(Listener.class)))
                .thenReturn(abstractChannelInitializer);
    }

    @Test
    public void bootstrapServer_whenNoListenersProvided_thenSuccessfulBootstrap() {

        when(listenerConfigurationService.getTcpListeners()).thenReturn(Lists.newArrayList());
        when(listenerConfigurationService.getTlsTcpListeners()).thenReturn(Lists.newArrayList());
        when(listenerConfigurationService.getWebsocketListeners()).thenReturn(Lists.newArrayList());
        when(listenerConfigurationService.getTlsWebsocketListeners()).thenReturn(Lists.newArrayList());

        final var unused = hiveMQNettyBootstrap.bootstrapServer();
    }

    @Test
    public void bootstrapServer_whenTCPListenerProvided_thenSuccessfulBootstrap() throws Exception {

        setupTcpListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture =
                hiveMQNettyBootstrap.bootstrapServer();

        // check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }

    @Test
    public void bootstrapServer_whenTCPListenerWithTLSProvided_thenSuccessfulBootstrap() throws Exception {
        setupTlsTcpListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture =
                hiveMQNettyBootstrap.bootstrapServer();

        // check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }

    @Test
    public void bootstrapServer_whenWebsocketListenerProvided_thenSuccessfulBootstrap() throws Exception {
        setupWebsocketListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture =
                hiveMQNettyBootstrap.bootstrapServer();

        // check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }

    @Test
    public void bootstrapServer_whenWebsocketListenerWithTLSProvided_thenSuccessfulBootstrap() throws Exception {
        setupTlsWebsocketListener(randomPort);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture =
                hiveMQNettyBootstrap.bootstrapServer();

        // check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(1, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
    }

    /**
     * Four ports, each checked free on its own.
     * <p>
     * The test below used to take one checked port and assume the three above it were free as well. Those
     * three were never checked, and on a build agent running several test JVMs at once one of them
     * regularly is not free: netty's bind fails, the listener comes back through
     * {@code failedListenerStartup}, and the assertion reports {@code expected: <true> but was: <false>}
     * against a listener whose port the test never chose. Nothing about the failure says "port", which is
     * why it reads as an unrelated flake.
     */
    private static int[] fourFreePorts() {
        final Set<Integer> ports = new LinkedHashSet<>();
        while (ports.size() < 4) {
            ports.add(RandomPortGenerator.get());
        }
        return ports.stream().mapToInt(Integer::intValue).toArray();
    }

    @Test
    public void bootstrapServer_whenDifferentListenersProvided_thenSuccessfulBootstrap() throws Exception {

        final int[] ports = fourFreePorts();
        setupTcpListener(ports[0]);
        setupTlsTcpListener(ports[1]);
        setupWebsocketListener(ports[2]);
        setupTlsWebsocketListener(ports[3]);

        final ListenableFuture<List<ListenerStartupInformation>> listenableFuture =
                hiveMQNettyBootstrap.bootstrapServer();

        // check for netty shutdown hook
        verify(shutdownHooks, atLeastOnce()).add(any(NettyShutdownHook.class));

        assertEquals(4, listenableFuture.get().size());
        assertTrue(listenableFuture.get().get(0).isSuccessful());
        assertTrue(listenableFuture.get().get(1).isSuccessful());
        assertTrue(listenableFuture.get().get(2).isSuccessful());
        assertTrue(listenableFuture.get().get(3).isSuccessful());
    }

    private MqttTlsWebsocketListener createTlsWebsocketListener(final int givenPort) {
        final Tls tls = createDefaultTLS();
        final String bindAddress = "0.0.0.0";

        return new MqttTlsWebsocketListener.Builder()
                .bindAddress(bindAddress)
                .port(givenPort)
                .tls(tls)
                .build();
    }

    private MqttTcpListener createTcpListener(final int givenPort) {
        return new MqttTcpListener(givenPort, "127.0.0.1");
    }

    private MqttTlsTcpListener createTlsTcpListener(final int givenPort) {
        final Tls tls = createDefaultTLS();
        final String bindAddress = "0.0.0.0";

        return new MqttTlsTcpListener(givenPort, bindAddress, tls);
    }

    private MqttWebsocketListener createWebsocketListener(final int givenPort) {
        final String bindAddress = "0.0.0.0";
        final MqttWebsocketListener mqttWebsocketListener = new MqttWebsocketListener.Builder()
                .bindAddress(bindAddress)
                .port(givenPort)
                .build();
        return mqttWebsocketListener;
    }

    private void setupTlsWebsocketListener(final int givenPort) {
        final List<MqttTlsWebsocketListener> mqttTlsWebsocketListeners =
                Lists.newArrayList(createTlsWebsocketListener(givenPort));
        when(listenerConfigurationService.getTlsWebsocketListeners()).thenReturn(mqttTlsWebsocketListeners);
    }

    private void setupTcpListener(final int givenPort) {
        final List<MqttTcpListener> mqttTcpListeners = Lists.newArrayList(createTcpListener(givenPort));
        when(listenerConfigurationService.getTcpListeners()).thenReturn(mqttTcpListeners);
    }

    private void setupTlsTcpListener(final int givenPort) {

        final List<MqttTlsTcpListener> mqttTlsTcpListeners = Lists.newArrayList(createTlsTcpListener(givenPort));
        when(listenerConfigurationService.getTlsTcpListeners()).thenReturn(mqttTlsTcpListeners);
    }

    private void setupWebsocketListener(final int givenPort) {

        final List<MqttWebsocketListener> mqttWebsocketListeners =
                Lists.newArrayList(createWebsocketListener(givenPort));
        when(listenerConfigurationService.getWebsocketListeners()).thenReturn(mqttWebsocketListeners);
    }
}
