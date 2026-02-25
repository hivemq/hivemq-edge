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
package com.hivemq.extensions.services.publish;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.exception.RateLimitExceededException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.PublishToClientResult;
import com.hivemq.extensions.services.PluginServiceRateLimitService;
import com.hivemq.extensions.services.builder.PublishBuilderImpl;
import com.hivemq.extensions.services.executor.GlobalManagedExtensionExecutorService;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.services.PublishDistributor;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.TestConfigurationBootstrap;

/**
 * @author Lukas Brandl
 */
public class PublishServiceImplTest {

    private final @NotNull PluginServiceRateLimitService rateLimitService = mock();
    private final @NotNull ShutdownHooks shutdownHooks = mock();
    private final @NotNull PublishDistributor publishDistributor = mock();
    private final @NotNull LocalTopicTree topicTree = mock();

    private final HivemqId hiveMQId = new HivemqId();
    private final ConfigurationService fullConfigurationService =
            new TestConfigurationBootstrap().getConfigurationService();
    private @NotNull PublishServiceImpl publishService;
    private @NotNull DataGovernanceService dataGovernanceService;

    @BeforeEach
    public void setUp() throws Exception {
        when(rateLimitService.rateLimitExceeded()).thenReturn(false);
        @NotNull
        final GlobalManagedExtensionExecutorService managedPluginExecutorService =
                new GlobalManagedExtensionExecutorService(shutdownHooks);
        managedPluginExecutorService.postConstruct();
        dataGovernanceService = mock(DataGovernanceService.class);
        when(dataGovernanceService.applyAndPublish(any()))
                .thenReturn(Futures.immediateFuture(PublishingResult.DELIVERED));
        publishService = new PublishServiceImpl(
                rateLimitService,
                managedPluginExecutorService,
                publishDistributor,
                hiveMQId,
                topicTree,
                dataGovernanceService);
    }

    @Test
    public void test_publish_implemented_publish() throws Throwable {
        assertThatThrownBy(() -> publishService.publish(new TestPublish()).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(DoNotImplementException.class);
    }

    @Test
    public void test_publish_rate_limit_exceeded() throws Throwable {
        when(rateLimitService.rateLimitExceeded()).thenReturn(true);
        final Publish publish = new PublishBuilderImpl(fullConfigurationService)
                .topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes(UTF_8)))
                .build();

        assertThatThrownBy(() -> publishService.publish(publish).get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    public void test_publish_to_client_implemented_publish() throws Throwable {
        assertThatThrownBy(() -> publishService
                        .publishToClient(new TestPublish(), "client")
                        .get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(DoNotImplementException.class);
    }

    @Test
    public void test_publish_to_client_rate_limit_exceeded() throws Throwable {
        when(rateLimitService.rateLimitExceeded()).thenReturn(true);
        final Publish publish = new PublishBuilderImpl(fullConfigurationService)
                .topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes(UTF_8)))
                .build();

        assertThatThrownBy(
                        () -> publishService.publishToClient(publish, "client").get())
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @Timeout(10)
    public void test_publish() throws Throwable {
        final Publish publish = new PublishBuilderImpl(fullConfigurationService)
                .topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes(UTF_8)))
                .build();
        when(dataGovernanceService.applyAndPublish(any()))
                .thenReturn(Futures.immediateFuture(PublishingResult.DELIVERED));

        publishService.publish(publish).get();
        verify(dataGovernanceService).applyAndPublish(any());
    }

    @Test
    @Timeout(10)
    public void test_publish_to_client() throws Exception {
        final byte subscriptionFlags = SubscriptionFlag.getDefaultFlags(false, false, false);
        final Publish publish = new PublishBuilderImpl(fullConfigurationService)
                .topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes(UTF_8)))
                .build();
        when(topicTree.findSubscriber("client", "topic"))
                .thenReturn(new SubscriberWithIdentifiers("client", 1, subscriptionFlags, null));
        when(publishDistributor.sendMessageToSubscriber(
                        any(PUBLISH.class),
                        anyString(),
                        anyInt(),
                        anyBoolean(),
                        anyBoolean(),
                        any(ImmutableIntArray.class)))
                .thenReturn(Futures.immediateFuture(PublishStatus.DELIVERED));
        final PublishToClientResult result =
                publishService.publishToClient(publish, "client").get();
        assertEquals(PublishToClientResult.SUCCESSFUL, result);
    }

    @Test
    @Timeout(10)
    public void test_publish_to_client_not_subscribed() throws Exception {
        final Publish publish = new PublishBuilderImpl(fullConfigurationService)
                .topic("topic")
                .payload(ByteBuffer.wrap("message".getBytes(UTF_8)))
                .build();
        when(topicTree.findSubscriber("client", "topic")).thenReturn(null);
        final PublishToClientResult result =
                publishService.publishToClient(publish, "client").get();
        assertEquals(PublishToClientResult.NOT_SUBSCRIBED, result);
    }

    private static class TestPublish implements Publish {

        @Override
        public Qos getQos() {
            return null;
        }

        @Override
        public boolean getRetain() {
            return false;
        }

        @Override
        public @Nullable String getTopic() {
            return null;
        }

        @Override
        public @NotNull Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<Long> getMessageExpiryInterval() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<String> getResponseTopic() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<ByteBuffer> getCorrelationData() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<String> getContentType() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<ByteBuffer> getPayload() {
            return Optional.empty();
        }

        @Override
        public UserProperties getUserProperties() {
            return null;
        }
    }
}
