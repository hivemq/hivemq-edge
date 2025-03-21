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

package com.hivemq.extensions.handler;

import com.google.common.collect.ImmutableList;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.PubrecOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.pubrec.parameter.PubrecOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.pubrec.ModifiablePubrecPacket;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.client.ClientContextImpl;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginOutputAsyncerImpl;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.PluginTaskExecutorServiceImpl;
import com.hivemq.extensions.executor.task.PluginTaskExecutor;
import com.hivemq.mqtt.handler.publish.PublishFlushHandler;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.reason.Mqtt5PubRecReasonCode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import util.IsolatedExtensionClassloaderUtil;
import util.TestConfigurationBootstrap;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PubrecInterceptorHandlerTest {

    @Rule
    public @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull ClientConnection clientConnection;
    private @NotNull PubrecInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, mock(PublishFlushHandler.class));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestResponseInformation(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setExtensionClientContext(clientContext);
        when(extension.getId()).thenReturn("plugin");

        final ConfigurationService configurationService =
                new TestConfigurationBootstrap().getConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new PubrecInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundPubrec(ctx, ((PUBREC) msg), promise);
            }
        });
        channel.pipeline().addLast("test2", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundPubrec(ctx, ((PUBREC) msg));
            }
        });
    }

    @Test(timeout = 5000)
    public void test_inbound_client_id_not_set() {
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId(null);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_channel_inactive() {
        channel.close();

        channel.pipeline().fireChannelRead(testPubrec());

        channel.runPendingTasks();

        assertNotNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_no_interceptors() {
        when(clientContext.getPubrecInboundInterceptors()).thenReturn(ImmutableList.of());
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBREC testPubrec = testPubrec();
        channel.writeInbound(testPubrec);
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }
        assertEquals(testPubrec.getReasonCode(), pubrec.getReasonCode());
    }

    @Test()
    public void test_inbound_modify() throws Exception {
        final PubrecInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }

        assertEquals("modified", pubrec.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_inbound_plugin_null() throws Exception {
        final PubrecInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }

        assertEquals("reason", pubrec.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_inbound_timeout_failed() throws Exception {
        final PubrecInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedInboundInterceptor.class);
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_exception() throws Exception {
        final PubrecInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionInboundInterceptor.class);
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_noPartialModificationWhenException() throws Exception {
        final PubrecInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedInboundInterceptor.class);
        final List<PubrecInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeInbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readInbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readInbound();
        }

        assertNotEquals("modified", pubrec.getReasonString());
        assertNotEquals(Mqtt5PubRecReasonCode.NOT_AUTHORIZED, pubrec.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_client_id_not_set() {
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId(null);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_channel_inactive() {
        channel.close();

        channel.pipeline().write(testPubrec());

        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_no_interceptors() {
        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(ImmutableList.of());
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBREC testPubrec = testPubrec();
        channel.writeOutbound(testPubrec);
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }
        assertEquals(testPubrec.getReasonCode(), pubrec.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_modify() throws Exception {
        final PubrecOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertEquals("modified", pubrec.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_outbound_plugin_null() throws Exception {
        final PubrecOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertEquals("reason", pubrec.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_outbound_timeout_failed() throws Exception {
        final PubrecOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedOutboundInterceptor.class);
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_exception() throws Exception {
        final PubrecOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionOutboundInterceptor.class);
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {
        final PubrecOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedOutboundInterceptor.class);
        final List<PubrecOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubrecOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);

        channel.writeOutbound(testPubrec());
        channel.runPendingTasks();
        PUBREC pubrec = channel.readOutbound();
        while (pubrec == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            pubrec = channel.readOutbound();
        }

        assertNotEquals("modified", pubrec.getReasonString());
        assertNotEquals(Mqtt5PubRecReasonCode.NOT_AUTHORIZED, pubrec.getReasonCode());
    }

    @NotNull
    private PUBREC testPubrec() {
        return new PUBREC(1, Mqtt5PubRecReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class TestModifyInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                final @NotNull PubrecInboundInput pubrecInboundInput,
                final @NotNull PubrecInboundOutput pubrecInboundOutput) {
            final ModifiablePubrecPacket pubrecPacket = pubrecInboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                final @NotNull PubrecInboundInput pubrecInboundInput,
                final @NotNull PubrecInboundOutput pubrecInboundOutput) {
            pubrecInboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                final @NotNull PubrecInboundInput pubrecInboundInput,
                final @NotNull PubrecInboundOutput pubrecInboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedInboundInterceptor implements PubrecInboundInterceptor {

        @Override
        public void onInboundPubrec(
                final @NotNull PubrecInboundInput pubrecInboundInput,
                final @NotNull PubrecInboundOutput pubrecInboundOutput) {
            final ModifiablePubrecPacket pubrecPacket = pubrecInboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
            pubrecPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }

    public static class TestModifyOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                final @NotNull PubrecOutboundInput pubrecOutboundInput,
                final @NotNull PubrecOutboundOutput pubrecOutboundOutput) {
            @Immutable final ModifiablePubrecPacket pubrecPacket = pubrecOutboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                final @NotNull PubrecOutboundInput pubrecOutboundInput,
                final @NotNull PubrecOutboundOutput pubrecOutboundOutput) {
            pubrecOutboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                final @NotNull PubrecOutboundInput pubrecOutboundInput,
                final @NotNull PubrecOutboundOutput pubrecOutboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedOutboundInterceptor implements PubrecOutboundInterceptor {

        @Override
        public void onOutboundPubrec(
                final @NotNull PubrecOutboundInput pubrecOutboundInput,
                final @NotNull PubrecOutboundOutput pubrecOutboundOutput) {
            final ModifiablePubrecPacket pubrecPacket = pubrecOutboundOutput.getPubrecPacket();
            pubrecPacket.setReasonString("modified");
            pubrecPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }
}
