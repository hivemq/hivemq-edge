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
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.puback.ModifiablePubackPacket;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
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
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.reason.Mqtt5PubAckReasonCode;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
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

public class PubackInterceptorHandlerTest {

    @Rule
    public final @NotNull TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final @NotNull HiveMQExtensions hiveMQExtensions = mock(HiveMQExtensions.class);
    private final @NotNull HiveMQExtension extension = mock(HiveMQExtension.class);
    private final @NotNull ClientContextImpl clientContext = mock(ClientContextImpl.class);

    private @NotNull PluginTaskExecutor executor;
    private @NotNull EmbeddedChannel channel;
    private @NotNull PubackInterceptorHandler handler;

    @Before
    public void setUp() throws Exception {
        executor = new PluginTaskExecutor(new AtomicLong());
        executor.postConstruct();

        channel = new EmbeddedChannel();
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME)
                .set(new ClientConnection(channel, mock(PublishFlushHandler.class)));
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setRequestResponseInformation(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setExtensionClientContext(clientContext);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        when(extension.getId()).thenReturn("extension");

        final ConfigurationService configurationService =
                new TestConfigurationBootstrap().getConfigurationService();
        final PluginOutPutAsyncer asyncer = new PluginOutputAsyncerImpl(mock(ShutdownHooks.class));
        final PluginTaskExecutorService pluginTaskExecutorService =
                new PluginTaskExecutorServiceImpl(() -> executor, mock(ShutdownHooks.class));

        handler = new PubackInterceptorHandler(configurationService,
                asyncer,
                hiveMQExtensions,
                pluginTaskExecutorService);
        channel.pipeline().addLast("test1", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(
                    final @NotNull ChannelHandlerContext ctx,
                    final @NotNull Object msg,
                    final @NotNull ChannelPromise promise) {
                handler.handleOutboundPuback(ctx, ((PUBACK) msg), promise);
            }
        });
        channel.pipeline().addLast("test2", new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(final @NotNull ChannelHandlerContext ctx, final @NotNull Object msg) {
                handler.handleInboundPuback(ctx, ((PUBACK) msg));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        executor.stop();
    }

    @Test(timeout = 5000)
    public void test_inbound_client_id_not_set() {
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId(null);

        channel.writeInbound(testPuback());
        channel.runPendingTasks();

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_channel_inactive() {
        channel.close();

        channel.pipeline().write(testPuback());

        channel.runPendingTasks();

        assertNull(channel.readInbound());
    }

    @Test(timeout = 5000)
    public void test_inbound_no_interceptors() {
        when(clientContext.getPubackInboundInterceptors()).thenReturn(ImmutableList.of());
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBACK testPuback = testPuback();
        channel.writeInbound(testPuback);
        channel.runPendingTasks();
        PUBACK puback = channel.readInbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readInbound();
        }
        assertEquals(testPuback.getReasonCode(), puback.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_inbound_modify() throws Exception {
        final PubackInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubackInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readInbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readInbound();
        }

        assertEquals("modified", puback.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_inbound_plugin_null() throws Exception {
        final PubackInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyInboundInterceptor.class);
        final List<PubackInboundInterceptor> list = ImmutableList.of(interceptor);
        when(clientContext.getPubackInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeInbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readInbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readInbound();
        }

        assertEquals("reason", puback.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_inbound_timeout_failed() throws Exception {
        final PubackInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedInboundInterceptor.class);
        final List<PubackInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPuback());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_exception() throws Exception {
        final PubackInboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionInboundInterceptor.class);
        final List<PubackInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPuback());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();
        Thread.sleep(10);

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_inbound_noPartialModificationWhenException() throws Exception {
        final PubackInboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedInboundInterceptor.class);
        final List<PubackInboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackInboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeInbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readInbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readInbound();
        }

        assertNotEquals("modified", puback.getReasonString());
        assertNotEquals(Mqtt5PubAckReasonCode.NOT_AUTHORIZED, puback.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_client_id_not_set() {
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId(null);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_channel_inactive() {
        final ChannelHandlerContext context = channel.pipeline().context("test1");

        channel.close();
        channel.write(testPuback(), context.newPromise());

        channel.runPendingTasks();

        assertNull(channel.readOutbound());
    }

    @Test(timeout = 5000)
    public void test_outbound_no_interceptors() {
        when(clientContext.getPubackOutboundInterceptors()).thenReturn(ImmutableList.of());
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        final PUBACK puback = testPuback();
        channel.writeOutbound(puback);
        channel.runPendingTasks();
        PUBACK readPuback = channel.readOutbound();
        while (readPuback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            readPuback = channel.readOutbound();
        }
        assertEquals(readPuback.getReasonCode(), readPuback.getReasonCode());
    }

    @Test(timeout = 5000)
    public void test_outbound_modify() throws Exception {
        final PubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertEquals("modified", puback.getReasonString());
    }

    @Test(timeout = 5000)
    public void test_outbound_plugin_null() throws Exception {
        final PubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestModifyOutboundInterceptor.class);
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);
        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(null);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertEquals("reason", puback.getReasonString());
    }

    @Test(timeout = 10_000)
    public void test_outbound_timeout_failed() throws Exception {
        final PubackOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestTimeoutFailedOutboundInterceptor.class);
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPuback());

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_exception() throws Exception {
        final PubackOutboundInterceptor interceptor = IsolatedExtensionClassloaderUtil.loadInstance(
                temporaryFolder.getRoot().toPath(),
                TestExceptionOutboundInterceptor.class);
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        channel.runScheduledPendingTasks();

        assertTrue(channel.isActive());
    }

    @Test(timeout = 5000)
    public void test_outbound_noPartialModificationWhenException() throws Exception {
        final PubackOutboundInterceptor interceptor =
                IsolatedExtensionClassloaderUtil.loadInstance(temporaryFolder.getRoot().toPath(),
                        TestPartialModifiedOutboundInterceptor.class);
        final List<PubackOutboundInterceptor> list = ImmutableList.of(interceptor);

        when(clientContext.getPubackOutboundInterceptors()).thenReturn(list);
        when(hiveMQExtensions.getExtensionForClassloader(any())).thenReturn(extension);

        channel.writeOutbound(testPuback());
        channel.runPendingTasks();
        PUBACK puback = channel.readOutbound();
        while (puback == null) {
            channel.runPendingTasks();
            channel.runScheduledPendingTasks();
            puback = channel.readOutbound();
        }

        assertNotEquals("modified", puback.getReasonString());
        assertNotEquals(Mqtt5PubAckReasonCode.NOT_AUTHORIZED, puback.getReasonCode());
    }

    @NotNull
    private PUBACK testPuback() {
        return new PUBACK(1, Mqtt5PubAckReasonCode.UNSPECIFIED_ERROR, "reason", Mqtt5UserProperties.NO_USER_PROPERTIES);
    }

    public static class TestModifyInboundInterceptor implements PubackInboundInterceptor {

        @Override
        public void onInboundPuback(
                final @NotNull PubackInboundInput pubackInboundInput,
                final @NotNull PubackInboundOutput pubackInboundOutput) {
            @Immutable final ModifiablePubackPacket pubackPacket = pubackInboundOutput.getPubackPacket();
            pubackPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedInboundInterceptor implements PubackInboundInterceptor {

        @Override
        public void onInboundPuback(
                final @NotNull PubackInboundInput pubackInboundInput,
                final @NotNull PubackInboundOutput pubackInboundOutput) {
            pubackInboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionInboundInterceptor implements PubackInboundInterceptor {

        @Override
        public void onInboundPuback(
                final @NotNull PubackInboundInput pubackInboundInput,
                final @NotNull PubackInboundOutput pubackInboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedInboundInterceptor implements PubackInboundInterceptor {

        @Override
        public void onInboundPuback(
                final @NotNull PubackInboundInput pubackInboundInput,
                final @NotNull PubackInboundOutput pubackInboundOutput) {
            final ModifiablePubackPacket pubackPacket = pubackInboundOutput.getPubackPacket();
            pubackPacket.setReasonString("modified");
            pubackPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }

    public static class TestModifyOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(
                final @NotNull PubackOutboundInput pubackOutboundInput,
                final @NotNull PubackOutboundOutput pubackOutboundOutput) {
            @Immutable final ModifiablePubackPacket pubackPacket = pubackOutboundOutput.getPubackPacket();
            pubackPacket.setReasonString("modified");
        }
    }

    public static class TestTimeoutFailedOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(
                final @NotNull PubackOutboundInput pubackOutboundInput,
                final @NotNull PubackOutboundOutput pubackOutboundOutput) {
            pubackOutboundOutput.async(Duration.ofMillis(10));
        }
    }

    public static class TestExceptionOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(
                final @NotNull PubackOutboundInput pubackOutboundInput,
                final @NotNull PubackOutboundOutput pubackOutboundOutput) {
            throw new RuntimeException();
        }
    }

    public static class TestPartialModifiedOutboundInterceptor implements PubackOutboundInterceptor {

        @Override
        public void onOutboundPuback(
                final @NotNull PubackOutboundInput pubrecOutboundInput,
                final @NotNull PubackOutboundOutput pubackOutboundOutput) {
            final ModifiablePubackPacket pubackPacket = pubackOutboundOutput.getPubackPacket();
            pubackPacket.setReasonString("modified");
            pubackPacket.setReasonCode(AckReasonCode.NOT_AUTHORIZED);
            throw new RuntimeException();
        }
    }
}
