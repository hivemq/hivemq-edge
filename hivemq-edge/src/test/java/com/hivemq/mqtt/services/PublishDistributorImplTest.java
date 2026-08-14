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
package com.hivemq.mqtt.services;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.primitives.ImmutableIntArray;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import com.hivemq.mqtt.handler.publish.PublishStatus;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.SubscriberWithIdentifiers;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.clientqueue.QueuePolicy;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.clientsession.ClientSessionPersistence;
import com.hivemq.sampling.SamplingService;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import util.TestMessageUtil;
import util.TestSingleWriterFactory;

/**
 * @author Lukas Brandl
 */
public class PublishDistributorImplTest {

    private final @NotNull ClientQueuePersistence clientQueuePersistence = mock();
    private final @NotNull ClientSessionPersistence clientSessionPersistence = mock();
    private final @NotNull ConfigurationService configurationService = mock();
    private final @NotNull BridgeExtractor bridgeConfiguration = mock();
    private final @NotNull MqttBridge bridge = mock();
    private final @NotNull MqttConfigurationService mqttConfigurationService = mock();
    private final @NotNull SamplingService samplingService = mock();

    private @NotNull PublishDistributorImpl publishDistributor;
    private @NotNull SingleWriterService singleWriterService;

    private final @NotNull InternalConfigurationService internalConfigurationService =
            new InternalConfigurationServiceImpl();

    @BeforeEach
    public void setUp() throws Exception {
        when(configurationService.mqttConfiguration()).thenReturn(mqttConfigurationService);
        when(configurationService.bridgeExtractor()).thenReturn(bridgeConfiguration);
        singleWriterService = TestSingleWriterFactory.defaultSingleWriter(internalConfigurationService);
        publishDistributor = new PublishDistributorImpl(
                clientQueuePersistence, () -> clientSessionPersistence, configurationService, () -> samplingService);
        when(bridgeConfiguration.getBridges()).thenReturn(List.of(bridge));
    }

    @AfterEach
    public void tearDown() throws Exception {
        singleWriterService.stop();
    }

    @Test
    @Timeout(5)
    public void test_not_connected() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(new ClientSession(false, 1000L));

        final PublishStatus status = publishDistributor
                .sendMessageToSubscriber(
                        createPublish(QoS.AT_LEAST_ONCE), "client", 0, false, false, ImmutableIntArray.of(1))
                .get();

        assertEquals(PublishStatus.NOT_CONNECTED, status);
    }

    @Test
    @Timeout(5)
    public void test_session_expired() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(null);

        final PublishStatus status = publishDistributor
                .sendMessageToSubscriber(
                        createPublish(QoS.AT_LEAST_ONCE), "client", 0, false, false, ImmutableIntArray.of(1))
                .get();

        assertEquals(PublishStatus.NOT_CONNECTED, status);
    }

    @Test
    @Timeout(5)
    public void test_success() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientQueuePersistence.add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));

        final PublishStatus status = publishDistributor
                .sendMessageToSubscriber(
                        createPublish(QoS.AT_LEAST_ONCE), "client", 0, false, false, ImmutableIntArray.of(1))
                .get();

        verify(clientQueuePersistence).add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any());
        assertEquals(PublishStatus.DELIVERED, status);
    }

    @Test
    @Timeout(5)
    public void test_failed() throws ExecutionException, InterruptedException {
        when(clientSessionPersistence.getSession("client", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientQueuePersistence.add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFailedFuture(new RuntimeException("test")));

        final PublishStatus status = publishDistributor
                .sendMessageToSubscriber(
                        createPublish(QoS.AT_LEAST_ONCE), "client", 0, false, false, ImmutableIntArray.of(1))
                .get();

        verify(clientQueuePersistence).add(eq("client"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any());
        assertEquals(PublishStatus.FAILED, status);
    }

    /**
     * EDG-882 F-05. A queue ID is built from a share name, and a share name is the client's to choose:
     * {@code $share/$SAMPLER::customer/alerts} is a legal subscription that no part of Edge owns. It
     * must be queued like any other shared subscription — the configured limit, the configured overflow
     * strategy — and not turned into the ten-message ring that sampling uses, which would discard the
     * client's own messages under a policy meant for diagnostics.
     */
    @Test
    @Timeout(5)
    public void test_client_shared_subscription_named_like_a_sampler_gets_the_ordinary_policy()
            throws ExecutionException, InterruptedException {
        when(samplingService.isSamplerQueue("$SAMPLER::customer/alerts")).thenReturn(false);
        when(mqttConfigurationService.maxQueuedMessages()).thenReturn(1000L);
        when(clientQueuePersistence.add(
                        eq("$SAMPLER::customer/alerts"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));

        publishDistributor
                .sendMessageToSubscriber(
                        createPublish(QoS.AT_MOST_ONCE),
                        "$SAMPLER::customer/alerts",
                        0,
                        true,
                        false,
                        ImmutableIntArray.of(1))
                .get();

        verify(clientQueuePersistence)
                .add(
                        eq("$SAMPLER::customer/alerts"),
                        eq(true),
                        any(PUBLISH.class),
                        anyBoolean(),
                        eq(1000L),
                        eq(QueuePolicy.DEFAULT));
    }

    /** And a queue the sampling service does own still gets the ring: the fix must not disable it. */
    @Test
    @Timeout(5)
    public void test_a_real_sampler_queue_gets_the_sample_ring_policy()
            throws ExecutionException, InterruptedException {
        final String queueId = SamplingService.createQueueId("plant/line1");
        when(samplingService.isSamplerQueue(queueId)).thenReturn(true);
        when(clientQueuePersistence.add(eq(queueId), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));

        publishDistributor
                .sendMessageToSubscriber(
                        createPublish(QoS.AT_MOST_ONCE), queueId, 0, true, false, ImmutableIntArray.of(1))
                .get();

        verify(clientQueuePersistence)
                .add(
                        eq(queueId),
                        eq(true),
                        any(PUBLISH.class),
                        anyBoolean(),
                        eq(SamplingService.SAMPLER_QUEUE_LIMIT),
                        eq(QueuePolicy.SAMPLE_RING));
    }

    @Test
    @Timeout(5)
    public void test_success_shared() throws ExecutionException, InterruptedException {
        when(clientQueuePersistence.add(
                        eq("group/topic"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));

        final PublishStatus status = publishDistributor
                .sendMessageToSubscriber(
                        createPublish(QoS.AT_LEAST_ONCE), "group/topic", 0, true, false, ImmutableIntArray.of(1))
                .get();

        verify(clientQueuePersistence)
                .add(eq("group/topic"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any());
        assertEquals(PublishStatus.DELIVERED, status);
    }

    @Test
    @SuppressWarnings("FutureReturnValueIgnored")
    public void test_distribute_to_non_shared() {
        when(clientSessionPersistence.getSession("client1", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientSessionPersistence.getSession("client2", false)).thenReturn(new ClientSession(true, 1000L));
        when(clientQueuePersistence.add(eq("client1"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));
        when(clientQueuePersistence.add(eq("client2"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));

        final Map<String, SubscriberWithIdentifiers> subscribers = Map.of(
                "client1", new SubscriberWithIdentifiers("client1", 1, (byte) 0, null),
                "client2", new SubscriberWithIdentifiers("client2", 1, (byte) 0, null));

        publishDistributor.distributeToNonSharedSubscribers(
                subscribers, TestMessageUtil.createMqtt5Publish(), MoreExecutors.newDirectExecutorService());

        verify(clientQueuePersistence)
                .add(eq("client1"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any());
        verify(clientQueuePersistence)
                .add(eq("client2"), eq(false), any(PUBLISH.class), anyBoolean(), anyLong(), any());
    }

    @Test
    @SuppressWarnings("FutureReturnValueIgnored")
    public void test_distribute_to_shared_subs() {
        when(clientQueuePersistence.add(
                        eq("name/topic1"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));
        when(clientQueuePersistence.add(
                        eq("name/topic2"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any()))
                .thenReturn(Futures.immediateFuture(null));

        final Set<String> subscribers = Set.of("name/topic1", "name/topic2");

        publishDistributor.distributeToSharedSubscribers(
                subscribers, TestMessageUtil.createMqtt5Publish("topic"), MoreExecutors.newDirectExecutorService());

        verify(clientQueuePersistence)
                .add(eq("name/topic1"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any());
        verify(clientQueuePersistence)
                .add(eq("name/topic2"), eq(true), any(PUBLISH.class), anyBoolean(), anyLong(), any());
    }

    private PUBLISH createPublish(final @NotNull QoS qos) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withPacketIdentifier(0)
                .withQoS(qos)
                .withOnwardQos(qos)
                .withPayload("message".getBytes(UTF_8))
                .withTopic("topic")
                .withHivemqId("hivemqId")
                .build();
    }
}
