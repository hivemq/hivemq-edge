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
package com.hivemq.bridge.mqtt;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import util.TestMessageUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@SuppressWarnings("OptionalGetWithoutIsPresent")
class RemoteMqttForwarderTest {

    private final @NotNull BridgeMqttClient bridgeClient = mock(BridgeMqttClient.class);
    private final @NotNull Mqtt5AsyncClient mqtt5AsyncClient = mock(Mqtt5AsyncClient.class);
    private final @NotNull MetricRegistry metricRegistry = new MetricRegistry();
    private final @NotNull ExecutorService executorService = MoreExecutors.newDirectExecutorService();
    private final @NotNull AtomicBoolean callbackCalled = new AtomicBoolean(false);
    private final @NotNull RemoteMqttForwarder forwarder =
            createForwarder(callbackCalled, false, "{#}", List.of(), List.of(), 2);


    @BeforeEach
    void setup() {
        when(bridgeClient.isConnected()).thenReturn(true);
        when(bridgeClient.getMqtt5Client()).thenReturn(mqtt5AsyncClient);
        when(mqtt5AsyncClient.publish(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    public void whenLocalMessage_ThenPublishToDestinationWithCorrectFields() {
        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());

        assertTrue(callbackCalled.get());
        assertEquals(1, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.count").getCount());
        assertEquals(0,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.failed.count").getCount());

        final Mqtt5Publish publish = captor.getValue();

        verifyFullPublish(publish, "topic");
    }

    @Test
    public void whenLocalMessageAndDestinationChanged_ThenPublishToNewDestination() {
        final RemoteMqttForwarder forwarder =
                createForwarder(callbackCalled, false, "prefix/{1}/suffix", List.of(), List.of(), 2);
        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());

        assertTrue(callbackCalled.get());
        assertEquals(1, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.count").getCount());
        assertEquals(0,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.failed.count").getCount());

        final Mqtt5Publish publish = captor.getValue();

        verifyFullPublish(publish, "prefix/topic/suffix");
    }

    @Test
    public void whenLocalMessageRetainAndPreserveRetain_ThenPublishWithRetain() {
        final RemoteMqttForwarder forwarder = createForwarder(callbackCalled, true, "{#}", List.of(), List.of(), 2);

        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());
        final Mqtt5Publish publish = captor.getValue();

        assertTrue(callbackCalled.get());
        assertTrue(publish.isRetain());
    }

    @Test
    public void whenForwarderStopped_ThenNotPublishButCallbackIsCalled() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final RemoteMqttForwarder forwarder = createForwarder(called, false, "{#}", List.of(), List.of(), 2);
        forwarder.start();
        forwarder.stop();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        verify(mqtt5AsyncClient, never()).publish(any());
        assertTrue(called.get());
    }

    @Test
    public void whenHopCountExceeded_ThenNotPublishButCallbackIsCalled() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final RemoteMqttForwarder forwarder = createForwarder(called, false, "{#}", List.of(), List.of(), 2);

        forwarder.start();

        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        final ImmutableList.Builder<MqttUserProperty> props = ImmutableList.builder();
        props.addAll(localPublish.getUserProperties().asList());
        props.add(MqttUserProperty.of("hmq-bridge-hop-count", "1"));
        localPublish.setUserProperties(Mqtt5UserProperties.build(props));
        forwarder.onMessage(localPublish, "testqueue");

        verify(mqtt5AsyncClient, never()).publish(any());
        assertTrue(called.get());

        assertEquals(0, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.count").getCount());
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.loop-hops-exceeded.count")
                        .getCount());
    }

    @Test
    public void whenExcluded_ThenNotPublishButCallbackIsCalled() {
        final AtomicBoolean called = new AtomicBoolean(false);
        final RemoteMqttForwarder forwarder = createForwarder(called, false, "{#}", List.of("topic"), List.of(), 2);

        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        verify(mqtt5AsyncClient, never()).publish(any());
        assertTrue(called.get());

        assertEquals(0, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.count").getCount());
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.excluded.count").getCount());
    }

    @Test
    public void whenCustomProps_ThenCustomPropsArePresentInRemotePub() {
        final RemoteMqttForwarder forwarder = createForwarder(callbackCalled,
                false,
                "{#}",
                List.of(),
                List.of(CustomUserProperty.of("customk1", "customv1"), CustomUserProperty.of("customk2", "customv2")),
                2);

        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());
        final Mqtt5Publish publish = captor.getValue();

        assertTrue(callbackCalled.get());

        assertEquals(5, publish.getUserProperties().asList().size());
        final Mqtt5UserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("user1", prop1.getName().toString());
        assertEquals("property1", prop1.getValue().toString());
        final Mqtt5UserProperty prop2 = publish.getUserProperties().asList().get(1);
        assertEquals("user2", prop2.getName().toString());
        assertEquals("property2", prop2.getValue().toString());
        //custom
        final Mqtt5UserProperty prop3 = publish.getUserProperties().asList().get(2);
        assertEquals("customk1", prop3.getName().toString());
        assertEquals("customv1", prop3.getValue().toString());
        final Mqtt5UserProperty prop4 = publish.getUserProperties().asList().get(3);
        assertEquals("customk2", prop4.getName().toString());
        assertEquals("customv2", prop4.getValue().toString());
        //also hop count
        final Mqtt5UserProperty prop5 = publish.getUserProperties().asList().get(4);
        assertEquals("hmq-bridge-hop-count", prop5.getName().toString());
        assertEquals("1", prop5.getValue().toString());


    }

    @Test
    public void whenNoProps_ThenOnlyHopCountPresentInRemotePub() {
        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        localPublish.setUserProperties(Mqtt5UserProperties.NO_USER_PROPERTIES);
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());
        final Mqtt5Publish publish = captor.getValue();

        assertTrue(callbackCalled.get());

        assertEquals(1, publish.getUserProperties().asList().size());
        final Mqtt5UserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("hmq-bridge-hop-count", prop1.getName().toString());
        assertEquals("1", prop1.getValue().toString());
    }

    @Test
    public void whenMinimalPub_ThenAllFieldsSetCorrectly() {
        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createMqtt3Publish();
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());
        final Mqtt5Publish publish = captor.getValue();

        assertTrue(callbackCalled.get());

        assertEquals("topic", publish.getTopic().toString());
        assertEquals("payload", new String(publish.getPayloadAsBytes()));
        assertEquals(1, publish.getQos().getCode());
        assertFalse(publish.isRetain());
        assertFalse(publish.getMessageExpiryInterval().isPresent());
        assertFalse(publish.getPayloadFormatIndicator().isPresent());
        assertFalse(publish.getContentType().isPresent());
        assertFalse(publish.getResponseTopic().isPresent());
        assertFalse(publish.getCorrelationData().isPresent());
        assertEquals(1, publish.getUserProperties().asList().size());
        final Mqtt5UserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("hmq-bridge-hop-count", prop1.getName().toString());
        assertEquals("1", prop1.getValue().toString());
    }

    @Test
    public void whenMaxQoSExceeded_ThenPublishWithLessQoS() {
        final RemoteMqttForwarder forwarder = createForwarder(callbackCalled, true, "{#}", List.of(), List.of(), 1);
        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());
        final Mqtt5Publish publish = captor.getValue();

        assertTrue(callbackCalled.get());
        assertEquals(1, publish.getQos().getCode());
    }

    @Test
    public void whenHopPropIsNaN_ThenResetHop() {
        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        localPublish.setUserProperties(Mqtt5UserProperties.of(MqttUserProperty.of("hmq-bridge-hop-count",
                "not-a-number")));
        forwarder.onMessage(localPublish, "testqueue");

        final ArgumentCaptor<Mqtt5Publish> captor = ArgumentCaptor.forClass(Mqtt5Publish.class);
        verify(mqtt5AsyncClient).publish(captor.capture());
        final Mqtt5Publish publish = captor.getValue();

        assertTrue(callbackCalled.get());

        assertEquals(1, publish.getUserProperties().asList().size());
        final Mqtt5UserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("hmq-bridge-hop-count", prop1.getName().toString());
        assertEquals("1", prop1.getValue().toString());
    }

    @Test
    public void whenException_ThenCountMetric() {
        forwarder.start();
        when(mqtt5AsyncClient.publish(any())).thenThrow(new RuntimeException("Test Exception"));

        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");

        assertTrue(callbackCalled.get());
        assertEquals(0, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.count").getCount());
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.forward.publish.failed.count").getCount());
    }

    @Test
    void whenClientIsDisconnected_thenPublishesAreQueued() {
        when(bridgeClient.isConnected()).thenReturn(false);
        forwarder.start();
        final PUBLISH localPublish = TestMessageUtil.createFullMqtt5Publish();
        forwarder.onMessage(localPublish, "testqueue");
        when(bridgeClient.isConnected()).thenReturn(true);
        forwarder.onMessage(localPublish, "testqueue");

        verify(mqtt5AsyncClient, times(2)).publish(any());
    }

    @NotNull
    private RemoteMqttForwarder createForwarder(
            final @NotNull AtomicBoolean callbackCalled,
            final boolean preserveRetain,
            final @NotNull String destination,
            final @NotNull List<String> excludes,
            final @NotNull List<CustomUserProperty> customProps,
            final int maxQoS) {
        final LocalSubscription localSubscription =
                new LocalSubscription(List.of("#"), destination, excludes, customProps, preserveRetain, maxQoS, 1000L);
        final MqttBridge bridge = new MqttBridge.Builder().withId("testbridge")
                .withHost("1")
                .withClientId("testcid")
                .withLocalSubscriptions(List.of(localSubscription))
                .build();

        final RemoteMqttForwarder forwarder = new RemoteMqttForwarder("testid",
                bridge,
                localSubscription,
                bridgeClient,
                new PerBridgeMetrics("testbridge", metricRegistry),
                new TestInterceptorHandler());
        forwarder.setExecutorService(executorService);
        forwarder.setAfterForwardCallback((qos, uniqueId, queueId, cancelled) -> {
            if (queueId.equals("testqueue")) {
                callbackCalled.set(true);
            }
        });
        return forwarder;
    }

    private void verifyFullPublish(Mqtt5Publish publish, final @NotNull String topic) {
        assertEquals(topic, publish.getTopic().toString());
        assertEquals("payload", new String(publish.getPayloadAsBytes()));
        assertEquals(2, publish.getQos().getCode());
        assertFalse(publish.isRetain());
        assertEquals(360, publish.getMessageExpiryInterval().getAsLong());
        assertEquals(Mqtt5PayloadFormatIndicator.UTF_8, publish.getPayloadFormatIndicator().get());
        assertEquals("content type", publish.getContentType().get().toString());
        assertEquals("response topic", publish.getResponseTopic().get().toString());
        assertEquals("correlation data", StandardCharsets.UTF_8.decode(publish.getCorrelationData().get()).toString());
        assertEquals(3, publish.getUserProperties().asList().size());
        final Mqtt5UserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("user1", prop1.getName().toString());
        assertEquals("property1", prop1.getValue().toString());
        final Mqtt5UserProperty prop2 = publish.getUserProperties().asList().get(1);
        assertEquals("user2", prop2.getName().toString());
        assertEquals("property2", prop2.getValue().toString());
        //also hop count
        final Mqtt5UserProperty prop3 = publish.getUserProperties().asList().get(2);
        assertEquals("hmq-bridge-hop-count", prop3.getName().toString());
        assertEquals("1", prop3.getValue().toString());
    }

    private static class TestInterceptorHandler implements BridgeInterceptorHandler {
        @Override
        public @NotNull ListenableFuture<PublishReturnCode> interceptOrDelegateInbound(
                @NotNull final PUBLISH publish,
                @NotNull final ExecutorService executorService,
                @NotNull final MqttBridge bridge) {
            return Futures.immediateFuture(PublishReturnCode.DELIVERED);
        }

        @Override
        public @NotNull ListenableFuture<InterceptorResult> interceptOrDelegateOutbound(
                final @NotNull PUBLISH publish,
                final @NotNull ExecutorService executorService,
                final @NotNull MqttBridge bridge) {
            return Futures.immediateFuture(new InterceptorResult(InterceptorOutcome.SUCCESS, publish));
        }
    }
}
