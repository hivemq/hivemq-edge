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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperty;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.configuration.HivemqId;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemotePublishConsumerTest {

    private @NotNull BridgeInterceptorHandler bridgeInterceptorHandler;
    private @NotNull MetricRegistry metricRegistry;

    @BeforeEach
    void setup() {
        bridgeInterceptorHandler = mock(BridgeInterceptorHandler.class);
        metricRegistry = new MetricRegistry();
    }

    @Test
    public void whenFullPublish_thenAllFieldsSetCorrectly() {
        final RemotePublishConsumer consumer = setupConsumer(false, "{#}", List.of(), 2, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        verifyFullPublish(publish, "test/topic");
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.remote.publish.received.count").getCount());
        assertEquals(1, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.local.publish.count").getCount());
    }

    @Test
    public void whenPublishAndDestinationChanged_thenNewTopicSet() {
        final RemotePublishConsumer consumer =
                setupConsumer(false, "prefix/{2}/suffix", List.of(), 2, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        verifyFullPublish(publish, "prefix/topic/suffix");
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.remote.publish.received.count").getCount());
        assertEquals(1, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.local.publish.count").getCount());
    }

    @Test
    public void whenRetainPreserveAndRetain_thenRetainSet() {
        final RemotePublishConsumer consumer = setupConsumer(true, "{#}", List.of(), 2, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        assertTrue(publish.isRetain());
    }

    @Test
    public void whenHopCountExceeded_ThenNotPublish() {
        final RemotePublishConsumer consumer = setupConsumer(false, "{#}", List.of(), 2, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish().extend()
                .userProperties()
                .add("hmq-bridge-hop-count", "1")
                .applyUserProperties()
                .build();
        consumer.accept(originalPublish);

        verify(bridgeInterceptorHandler, never()).interceptOrDelegateInbound(any(), any(), any());

        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.remote.publish.received.count").getCount());
        assertEquals(0, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.local.publish.count").getCount());
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.remote.publish.loop-hops-exceeded.count")
                        .getCount());
    }

    @Test
    public void whenCustomProps_ThenCustomPropsArePresentPub() {
        final RemotePublishConsumer consumer = setupConsumer(false,
                "{#}",
                List.of(CustomUserProperty.of("customk1", "customv1"), CustomUserProperty.of("customk2", "customv2")),
                2,
                PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        assertEquals(5, publish.getUserProperties().asList().size());
        final MqttUserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("testk1", prop1.getName());
        assertEquals("testv1", prop1.getValue());
        final MqttUserProperty prop2 = publish.getUserProperties().asList().get(1);
        assertEquals("testk2", prop2.getName());
        assertEquals("testv2", prop2.getValue());
        //custom
        final MqttUserProperty prop3 = publish.getUserProperties().asList().get(2);
        assertEquals("customk1", prop3.getName());
        assertEquals("customv1", prop3.getValue());
        final MqttUserProperty prop4 = publish.getUserProperties().asList().get(3);
        assertEquals("customk2", prop4.getName());
        assertEquals("customv2", prop4.getValue());
        //also hop count
        final MqttUserProperty prop5 = publish.getUserProperties().asList().get(4);
        assertEquals("hmq-bridge-hop-count", prop5.getName());
        assertEquals("1", prop5.getValue());
    }

    @Test
    public void whenNoProps_ThenOnlyHopCountPresent() {
        final RemotePublishConsumer consumer = setupConsumer(false, "{#}", List.of(), 2, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish().extend().userProperties(Mqtt5UserProperties.of()).build();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        assertEquals(1, publish.getUserProperties().asList().size());
        final MqttUserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("hmq-bridge-hop-count", prop1.getName());
        assertEquals("1", prop1.getValue());
    }

    @Test
    public void whenMaxQoSExceeded_ThenPublishWithLessQoS() {
        final RemotePublishConsumer consumer = setupConsumer(true, "{#}", List.of(), 1, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        assertEquals(1, publish.getQoS().getQosNumber());
        assertEquals(1, publish.getOnwardQoS().getQosNumber());
    }


    @Test
    public void whenMinimalPub_ThenAllFieldsSetCorrectly() {
        final RemotePublishConsumer consumer = setupConsumer(false, "{#}", List.of(), 2, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = Mqtt5Publish.builder()
                .topic("test/topic")
                .payload("payload".getBytes(UTF_8))
                .qos(MqttQos.AT_MOST_ONCE)
                .build();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        assertEquals("test/topic", publish.getTopic());
        assertEquals("payload", new String(publish.getPayload()));
        assertEquals(0, publish.getQoS().getQosNumber());
        assertFalse(publish.isRetain());
        assertEquals(MESSAGE_EXPIRY_INTERVAL_NOT_SET, publish.getMessageExpiryInterval());
        assertNull(publish.getPayloadFormatIndicator());
        assertNull(publish.getContentType());
        assertNull(publish.getResponseTopic());
        assertNull(publish.getCorrelationData());
        assertEquals(1, publish.getUserProperties().asList().size());
        final MqttUserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("hmq-bridge-hop-count", prop1.getName());
        assertEquals("1", prop1.getValue());
    }

    @Test
    public void whenHopPropIsNaN_ThenResetHop() {
        final RemotePublishConsumer consumer = setupConsumer(false, "{#}", List.of(), 2, PublishReturnCode.DELIVERED);
        final Mqtt5Publish originalPublish = createPublish().extend()
                .userProperties(Mqtt5UserProperties.of(Mqtt5UserProperty.of("hmq-bridge-hop-count", "not-a-number")))
                .build();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        assertEquals(1, publish.getUserProperties().asList().size());
        final MqttUserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("hmq-bridge-hop-count", prop1.getName());
        assertEquals("1", prop1.getValue());
    }

    @Test
    public void whenNoMathchingSub_thenCountMetrics() {
        final RemotePublishConsumer consumer =
                setupConsumer(false, "{#}", List.of(), 2, PublishReturnCode.NO_MATCHING_SUBSCRIBERS);
        final Mqtt5Publish originalPublish = createPublish();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        verifyFullPublish(publish, "test/topic");
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.remote.publish.received.count").getCount());
        assertEquals(1, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.local.publish.count").getCount());
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.local.publish.no-subscriber-present.count")
                        .getCount());
    }

    @Test
    public void whenPubFailed_thenCountMetrics() {
        final RemotePublishConsumer consumer = setupConsumer(false, "{#}", List.of(), 2, PublishReturnCode.FAILED);
        final Mqtt5Publish originalPublish = createPublish();
        consumer.accept(originalPublish);

        final ArgumentCaptor<PUBLISH> captor = ArgumentCaptor.forClass(PUBLISH.class);
        verify(bridgeInterceptorHandler).interceptOrDelegateInbound(captor.capture(), any(), any());
        final PUBLISH publish = captor.getValue();

        verifyFullPublish(publish, "test/topic");
        assertEquals(1,
                metricRegistry.counter("com.hivemq.edge.bridge.testbridge.remote.publish.received.count").getCount());
        assertEquals(0, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.local.publish.count").getCount());
        assertEquals(1, metricRegistry.counter("com.hivemq.edge.bridge.testbridge.local.publish.failed.count").getCount());
    }

    private @NotNull RemotePublishConsumer setupConsumer(
            final boolean preserveRetain,
            final @NotNull String destination,
            final @NotNull List<CustomUserProperty> customProps,
            final int maxQoS,
            final @NotNull PublishReturnCode publishReturnCode) {
        when(bridgeInterceptorHandler.interceptOrDelegateInbound(any(), any(), any())).thenReturn(Futures.immediateFuture(publishReturnCode));
        final RemoteSubscription remoteSubscription =
                new RemoteSubscription(List.of("#"), destination, customProps, preserveRetain, maxQoS);
        final MqttBridge bridge = new MqttBridge.Builder().withId("testbridge")
                .withHost("1")
                .withClientId("testcid")
                .withRemoteSubscriptions(List.of(remoteSubscription))
                .build();

        return new RemotePublishConsumer(remoteSubscription,
                bridgeInterceptorHandler,
                bridge,
                MoreExecutors.newDirectExecutorService(),
                new HivemqId(),
                new PerBridgeMetrics("testbridge", metricRegistry));
    }

    private @NotNull Mqtt5Publish createPublish() {
        return Mqtt5Publish.builder()
                .topic("test/topic")
                .retain(true)
                .contentType("content-type")
                .correlationData("corrdata".getBytes(UTF_8))
                .messageExpiryInterval(123)
                .payload("payload".getBytes(UTF_8))
                .payloadFormatIndicator(Mqtt5PayloadFormatIndicator.UTF_8)
                .responseTopic("resp/topic")
                .qos(MqttQos.EXACTLY_ONCE)
                .userProperties()
                .add("testk1", "testv1")
                .add("testk2", "testv2")
                .applyUserProperties()
                .build();
    }

    private void verifyFullPublish(PUBLISH publish, final @NotNull String topic) {
        assertEquals(topic, publish.getTopic());
        assertEquals("payload", new String(publish.getPayload()));
        assertEquals(2, publish.getQoS().getQosNumber());
        assertEquals(2, publish.getOnwardQoS().getQosNumber());
        assertFalse(publish.isRetain());
        assertEquals(123, publish.getMessageExpiryInterval());
        assertEquals(com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator.UTF_8,
                publish.getPayloadFormatIndicator());
        assertEquals("content-type", publish.getContentType());
        assertEquals("resp/topic", publish.getResponseTopic());
        assertEquals("corrdata", new String(publish.getCorrelationData()));
        assertEquals(3, publish.getUserProperties().asList().size());
        final MqttUserProperty prop1 = publish.getUserProperties().asList().get(0);
        assertEquals("testk1", prop1.getName());
        assertEquals("testv1", prop1.getValue());
        final MqttUserProperty prop2 = publish.getUserProperties().asList().get(1);
        assertEquals("testk2", prop2.getName());
        assertEquals("testv2", prop2.getValue());
        //also hop count
        final MqttUserProperty prop3 = publish.getUserProperties().asList().get(2);
        assertEquals("hmq-bridge-hop-count", prop3.getName());
        assertEquals("1", prop3.getValue());
    }
}
