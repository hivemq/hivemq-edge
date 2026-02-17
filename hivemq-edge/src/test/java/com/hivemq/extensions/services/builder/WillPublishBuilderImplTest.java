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
package com.hivemq.extensions.services.builder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.util.Bytes;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.TestConfigurationBootstrap;
import util.TestMessageUtil;

/**
 * @author Florian Limpöck
 * @since 4.0.0
 */
@SuppressWarnings("NullabilityAnnotations")
public class WillPublishBuilderImplTest {

    private WillPublishBuilderImpl willPublishBuilder;

    @BeforeEach
    public void setUp() throws Exception {
        final ConfigurationService service = new TestConfigurationBootstrap().getConfigurationService();
        willPublishBuilder = new WillPublishBuilderImpl(service);
    }

    @Test
    public void test_from_publish_packet() {

        final PublishPacket testPublishPacket = new PublishPacketImpl(TestMessageUtil.createFullMqtt5Publish());

        final WillPublishPacket willPublishPacket =
                willPublishBuilder.fromPublish(testPublishPacket).build();

        assertEquals(testPublishPacket.getQos(), willPublishPacket.getQos());
        assertEquals(testPublishPacket.getRetain(), willPublishPacket.getRetain());
        assertEquals(testPublishPacket.getTopic(), willPublishPacket.getTopic());
        assertEquals(0, willPublishPacket.getPacketId());
        assertEquals(testPublishPacket.getPayloadFormatIndicator(), willPublishPacket.getPayloadFormatIndicator());
        assertEquals(testPublishPacket.getMessageExpiryInterval(), willPublishPacket.getMessageExpiryInterval());
        assertEquals(testPublishPacket.getResponseTopic(), willPublishPacket.getResponseTopic());
        assertEquals(testPublishPacket.getCorrelationData(), willPublishPacket.getCorrelationData());
        assertEquals(0, willPublishPacket.getSubscriptionIdentifiers().size());
        assertEquals(testPublishPacket.getContentType(), willPublishPacket.getContentType());
        assertEquals(testPublishPacket.getPayload(), willPublishPacket.getPayload());
        assertEquals(
                testPublishPacket.getUserProperties().asList().size(),
                willPublishPacket.getUserProperties().asList().size());
    }

    @Test
    public void test_from_publish_packet_implemented() {

        final TestPublishPacket testPublishPacket = new TestPublishPacket();
        assertThatThrownBy(() -> willPublishBuilder.fromPublish(testPublishPacket))
                .isInstanceOf(DoNotImplementException.class);
    }

    @Test
    public void test_from_publish() {

        final Publish publish = new PublishBuilderImpl(new TestConfigurationBootstrap().getConfigurationService())
                .topic("topic")
                .payload(ByteBuffer.wrap(new byte[] {1, 2, 3}))
                .qos(Qos.EXACTLY_ONCE)
                .retain(true)
                .contentType("TYPE")
                .correlationData(ByteBuffer.wrap(new byte[] {1, 2, 3, 4}))
                .responseTopic("responseTopic")
                .messageExpiryInterval(10)
                .payloadFormatIndicator(PayloadFormatIndicator.UTF_8)
                .userProperty("key", "value")
                .build();

        final WillPublishPacket willPublishPacket =
                willPublishBuilder.fromPublish(publish).build();

        assertEquals("topic", publish.getTopic());
        assertArrayEquals(new byte[] {1, 2, 3}, publish.getPayload().get().array());
        assertEquals(2, publish.getQos().getQosNumber());
        assertTrue(publish.getRetain());
        assertEquals("TYPE", publish.getContentType().get());
        assertArrayEquals(
                new byte[] {1, 2, 3, 4}, publish.getCorrelationData().get().array());
        assertEquals("responseTopic", publish.getResponseTopic().get());
        assertEquals(10L, publish.getMessageExpiryInterval().get().longValue());
        assertEquals(
                PayloadFormatIndicator.UTF_8,
                publish.getPayloadFormatIndicator().get());
        assertEquals("value", publish.getUserProperties().getFirst("key").get());

        assertEquals(publish.getQos(), willPublishPacket.getQos());
        assertEquals(publish.getRetain(), willPublishPacket.getRetain());
        assertEquals(publish.getTopic(), willPublishPacket.getTopic());
        assertEquals(0, willPublishPacket.getPacketId());
        assertEquals(publish.getPayloadFormatIndicator(), willPublishPacket.getPayloadFormatIndicator());
        assertEquals(publish.getMessageExpiryInterval(), willPublishPacket.getMessageExpiryInterval());
        assertEquals(publish.getResponseTopic(), willPublishPacket.getResponseTopic());
        assertEquals(publish.getCorrelationData(), willPublishPacket.getCorrelationData());
        assertEquals(0, willPublishPacket.getSubscriptionIdentifiers().size());
        assertEquals(publish.getContentType(), willPublishPacket.getContentType());
        assertEquals(publish.getPayload(), willPublishPacket.getPayload());
        assertEquals(
                publish.getUserProperties().asList().size(),
                willPublishPacket.getUserProperties().asList().size());
    }

    @Test
    public void test_from_will_publish() {

        final PublishPacket testPublishPacket = new PublishPacketImpl(TestMessageUtil.createFullMqtt5Publish());

        final WillPublishPacket willPublishPacket1 =
                willPublishBuilder.fromPublish(testPublishPacket).build();

        final WillPublishBuilderImpl builder =
                new WillPublishBuilderImpl(new TestConfigurationBootstrap().getConfigurationService());

        final WillPublishPacket willPublishPacket2 =
                builder.fromWillPublish(willPublishPacket1).build();

        assertEquals(willPublishPacket1.getQos(), willPublishPacket2.getQos());
        assertEquals(willPublishPacket1.getRetain(), willPublishPacket2.getRetain());
        assertEquals(willPublishPacket1.getTopic(), willPublishPacket2.getTopic());
        assertEquals(willPublishPacket1.getPacketId(), willPublishPacket2.getPacketId());
        assertEquals(willPublishPacket1.getPayloadFormatIndicator(), willPublishPacket2.getPayloadFormatIndicator());
        assertEquals(willPublishPacket1.getMessageExpiryInterval(), willPublishPacket2.getMessageExpiryInterval());
        assertEquals(willPublishPacket1.getResponseTopic(), willPublishPacket2.getResponseTopic());
        assertEquals(willPublishPacket1.getCorrelationData(), willPublishPacket2.getCorrelationData());
        assertEquals(
                willPublishPacket1.getSubscriptionIdentifiers().size(),
                willPublishPacket2.getSubscriptionIdentifiers().size());
        assertEquals(willPublishPacket1.getContentType(), willPublishPacket2.getContentType());
        assertEquals(willPublishPacket1.getPayload(), willPublishPacket2.getPayload());
        assertEquals(
                willPublishPacket1.getUserProperties().asList().size(),
                willPublishPacket2.getUserProperties().asList().size());
    }

    @Test
    public void test_all_values() {

        final WillPublishPacket willPublishPacket = willPublishBuilder
                .topic("topic")
                .payload(ByteBuffer.wrap(new byte[] {1, 2, 3}))
                .qos(Qos.EXACTLY_ONCE)
                .retain(true)
                .contentType("TYPE")
                .correlationData(ByteBuffer.wrap(new byte[] {1, 2, 3, 4}))
                .responseTopic("responseTopic")
                .messageExpiryInterval(10)
                .payloadFormatIndicator(PayloadFormatIndicator.UTF_8)
                .userProperty("key", "value")
                .willDelay(123)
                .build();

        assertEquals("topic", willPublishPacket.getTopic());
        assertArrayEquals(new byte[] {1, 2, 3}, Bytes.getBytesFromReadOnlyBuffer(willPublishPacket.getPayload()));
        assertEquals(2, willPublishPacket.getQos().getQosNumber());
        assertTrue(willPublishPacket.getRetain());
        assertEquals("TYPE", willPublishPacket.getContentType().get());
        assertArrayEquals(
                new byte[] {1, 2, 3, 4}, Bytes.getBytesFromReadOnlyBuffer(willPublishPacket.getCorrelationData()));
        assertEquals("responseTopic", willPublishPacket.getResponseTopic().get());
        assertEquals(10L, willPublishPacket.getMessageExpiryInterval().get().longValue());
        assertEquals(
                PayloadFormatIndicator.UTF_8,
                willPublishPacket.getPayloadFormatIndicator().get());
        assertEquals(
                "value", willPublishPacket.getUserProperties().getFirst("key").get());
        assertEquals(123, willPublishPacket.getWillDelay());
    }

    @Test
    public void test_qos_not_allowed() {
        final ConfigurationService service = new TestConfigurationBootstrap().getConfigurationService();
        service.mqttConfiguration().setMaximumQos(QoS.AT_MOST_ONCE);
        willPublishBuilder = new WillPublishBuilderImpl(service);
        assertThatThrownBy(() -> willPublishBuilder.qos(Qos.AT_LEAST_ONCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void test_retain_not_allowed() {
        final ConfigurationService service = new TestConfigurationBootstrap().getConfigurationService();
        service.mqttConfiguration().setRetainedMessagesEnabled(false);
        willPublishBuilder = new WillPublishBuilderImpl(service);
        assertThatThrownBy(() -> willPublishBuilder.retain(true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void test_topic_length_invalid() {
        final ConfigurationService service = new TestConfigurationBootstrap().getConfigurationService();
        service.restrictionsConfiguration().setMaxTopicLength(5);
        willPublishBuilder = new WillPublishBuilderImpl(service);
        assertThatThrownBy(() -> willPublishBuilder.topic("123456")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void test_response_topic_validation_utf_8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.responseTopic("topic" + '\u0001'));
    }

    @Test
    public void test_response_topic_validation_utf_8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.responseTopic("topic" + '\uD800'));
    }

    @Test
    public void test_content_type_validation_utf_8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.contentType("topic" + '\u0001'));
    }

    @Test
    public void test_content_type_validation_utf_8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.contentType("topic" + '\uD800'));
    }

    @Test
    public void test_user_property_name_validation_utf_8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.userProperty("topic" + '\u0001', "val"));
    }

    @Test
    public void test_user_property_name_validation_utf_8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.userProperty("topic" + '\uD800', "val"));
    }

    @Test
    public void test_user_property_value_validation_utf_8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.userProperty("key", "val" + '\u0001'));
    }

    @Test
    public void test_user_property_value_validation_utf_8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.userProperty("key", "val" + '\uD800'));
    }

    @Test
    public void test_qos_null() {

        assertThrows(NullPointerException.class, () -> willPublishBuilder.qos(null));
    }

    @Test
    public void test_topic_null() {

        assertThrows(NullPointerException.class, () -> willPublishBuilder.topic(null));
    }

    @Test
    public void test_topic_empty() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.topic(""));
    }

    @Test
    public void test_topic_invalid() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.topic("asd" + '\u0000'));
    }

    @Test
    public void test_topic_malformed_should_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.topic("asd" + '\u0001'));
    }

    @Test
    public void test_topic_malformed_must_not() {

        assertThrows(IllegalArgumentException.class, () -> willPublishBuilder.topic("asd" + '\uD800'));
    }

    @Test
    public void test_from_invalid_publish_impl() {

        assertThrows(DoNotImplementException.class, () -> willPublishBuilder.fromPublish(new TestPublish()));
    }

    @Test
    public void test_from_invalid_will_publish_impl() {

        assertThrows(
                DoNotImplementException.class, () -> willPublishBuilder.fromWillPublish(new TestWillPublishPacket()));
    }

    @Test
    public void test_user_property_name_null() {

        assertThrows(NullPointerException.class, () -> willPublishBuilder.userProperty(null, "val"));
    }

    @Test
    public void test_user_property_value_null() {

        assertThrows(NullPointerException.class, () -> willPublishBuilder.userProperty("name", null));
    }

    @Test
    public void test_user_property_name_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> willPublishBuilder.userProperty(RandomStringUtils.randomAlphanumeric(65536), "val"));
    }

    @Test
    public void test_user_property_value_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> willPublishBuilder.userProperty("name", RandomStringUtils.randomAlphanumeric(65536)));
    }

    @Test
    public void test_response_topic_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> willPublishBuilder.responseTopic(RandomStringUtils.randomAlphanumeric(65536)));
    }

    @Test
    public void test_content_type_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> willPublishBuilder.contentType(RandomStringUtils.randomAlphanumeric(65536)));
    }

    class TestPublishPacket implements PublishPacket {

        @Override
        public boolean getDupFlag() {
            return false;
        }

        @NotNull
        @Override
        public Qos getQos() {
            return Qos.EXACTLY_ONCE;
        }

        @Override
        public boolean getRetain() {
            return true;
        }

        @NotNull
        @Override
        public String getTopic() {
            return "topic";
        }

        @Override
        public int getPacketId() {
            return 0;
        }

        @NotNull
        @Override
        public Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
            return Optional.of(PayloadFormatIndicator.UTF_8);
        }

        @NotNull
        @Override
        public Optional<Long> getMessageExpiryInterval() {
            return Optional.of(12345L);
        }

        @NotNull
        @Override
        public Optional<String> getResponseTopic() {
            return Optional.of("response_topic");
        }

        @NotNull
        @Override
        public Optional<ByteBuffer> getCorrelationData() {
            return Optional.of(ByteBuffer.wrap("correlation_data".getBytes()));
        }

        @Override
        public List<Integer> getSubscriptionIdentifiers() {
            return null;
        }

        @NotNull
        @Override
        public Optional<String> getContentType() {
            return Optional.of("content_type");
        }

        @NotNull
        @Override
        public Optional<ByteBuffer> getPayload() {
            return Optional.of(ByteBuffer.wrap("test3".getBytes()));
        }

        @NotNull
        @Override
        public UserProperties getUserProperties() {
            return UserPropertiesImpl.of(ImmutableList.of(
                    new MqttUserProperty("name", "value"),
                    new MqttUserProperty("name", "value2"),
                    new MqttUserProperty("name2", "val")));
        }

        @Override
        public long getTimestamp() {
            return System.currentTimeMillis();
        }
    }

    private class TestWillPublishPacket implements WillPublishPacket {

        @Override
        public long getWillDelay() {
            return 0;
        }

        @Override
        public boolean getDupFlag() {
            return false;
        }

        @Override
        public Qos getQos() {
            return null;
        }

        @Override
        public boolean getRetain() {
            return false;
        }

        @Override
        public String getTopic() {
            return null;
        }

        @Override
        public int getPacketId() {
            return 0;
        }

        @Override
        public Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
            return Optional.empty();
        }

        @Override
        public Optional<Long> getMessageExpiryInterval() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getResponseTopic() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getCorrelationData() {
            return Optional.empty();
        }

        @Override
        public List<Integer> getSubscriptionIdentifiers() {
            return null;
        }

        @Override
        public Optional<String> getContentType() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getPayload() {
            return Optional.empty();
        }

        @Override
        public UserProperties getUserProperties() {
            return null;
        }

        @Override
        public long getTimestamp() {
            return System.currentTimeMillis();
        }
    }

    private class TestPublish implements Publish {

        @Override
        public Qos getQos() {
            return null;
        }

        @Override
        public boolean getRetain() {
            return false;
        }

        @Override
        public String getTopic() {
            return null;
        }

        @Override
        public Optional<PayloadFormatIndicator> getPayloadFormatIndicator() {
            return Optional.empty();
        }

        @Override
        public Optional<Long> getMessageExpiryInterval() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getResponseTopic() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getCorrelationData() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getContentType() {
            return Optional.empty();
        }

        @Override
        public Optional<ByteBuffer> getPayload() {
            return Optional.empty();
        }

        @Override
        public UserProperties getUserProperties() {
            return null;
        }
    }
}
