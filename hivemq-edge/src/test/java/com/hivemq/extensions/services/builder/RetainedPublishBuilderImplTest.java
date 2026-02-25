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
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.general.UserProperties;
import com.hivemq.extension.sdk.api.packets.publish.PayloadFormatIndicator;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.builder.RetainedPublishBuilder;
import com.hivemq.extension.sdk.api.services.exception.DoNotImplementException;
import com.hivemq.extension.sdk.api.services.publish.Publish;
import com.hivemq.extension.sdk.api.services.publish.RetainedPublish;
import com.hivemq.extensions.packets.general.UserPropertiesImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.extensions.services.publish.RetainedPublishImpl;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
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
public class RetainedPublishBuilderImplTest {

    private RetainedPublishBuilder retainedPublishBuilder;
    private ConfigurationService configurationService;

    @BeforeEach
    public void setUp() throws Exception {
        configurationService = new TestConfigurationBootstrap().getConfigurationService();
        retainedPublishBuilder = new RetainedPublishBuilderImpl(configurationService);
    }

    @Test
    public void test_qos_validation() {
        configurationService.mqttConfiguration().setMaximumQos(QoS.AT_LEAST_ONCE);
        assertThatThrownBy(() -> new RetainedPublishBuilderImpl(configurationService).qos(Qos.EXACTLY_ONCE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void test_topic_validation() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.topic("#"));
    }

    @Test
    public void test_topic_validation_utf_8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.topic("topic" + '\u0001'));
    }

    @Test
    public void test_topic_validation_utf_8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.topic("topic" + '\uD800'));
    }

    @Test
    public void test_message_expiry_validation() {
        configurationService.mqttConfiguration().setMaxMessageExpiryInterval(10);
        assertThatThrownBy(() -> new RetainedPublishBuilderImpl(configurationService).messageExpiryInterval(11))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void test_message_expiry_less_than_zero() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.messageExpiryInterval(-1));
    }

    @Test
    public void test_null_qos() {

        assertThrows(NullPointerException.class, () -> retainedPublishBuilder.qos(null));
    }

    @Test
    public void test_null_topic() {

        assertThrows(NullPointerException.class, () -> retainedPublishBuilder.topic(null));
    }

    @Test
    public void test_null_user_property_key() {

        assertThrows(NullPointerException.class, () -> retainedPublishBuilder.userProperty(null, "value"));
    }

    @Test
    public void test_null_user_property_value() {

        assertThrows(NullPointerException.class, () -> retainedPublishBuilder.userProperty("key", null));
    }

    @Test
    public void test_from_invalid_publish_implementation() {

        assertThrows(
                DoNotImplementException.class,
                () -> retainedPublishBuilder.fromPublish(new TestPublish()).build());
    }

    @Test
    public void test_response_topic_validation_utf_8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.responseTopic("topic" + '\u0001'));
    }

    @Test
    public void test_response_topic_validation_utf_8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.responseTopic("topic" + '\uD800'));
    }

    @Test
    public void test_content_type_validation_utf_8_should_not() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.contentType("topic" + '\u0001'));
    }

    @Test
    public void test_content_type_validation_utf_8_must_not() {

        assertThrows(IllegalArgumentException.class, () -> retainedPublishBuilder.contentType("topic" + '\uD800'));
    }

    @Test
    public void test_user_property_name_validation_utf_8_should_not() {

        assertThrows(
                IllegalArgumentException.class, () -> retainedPublishBuilder.userProperty("topic" + '\u0001', "val"));
    }

    @Test
    public void test_user_property_name_validation_utf_8_must_not() {

        assertThrows(
                IllegalArgumentException.class, () -> retainedPublishBuilder.userProperty("topic" + '\uD800', "val"));
    }

    @Test
    public void test_user_property_value_validation_utf_8_should_not() {

        assertThrows(
                IllegalArgumentException.class, () -> retainedPublishBuilder.userProperty("key", "val" + '\u0001'));
    }

    @Test
    public void test_user_property_value_validation_utf_8_must_not() {

        assertThrows(
                IllegalArgumentException.class, () -> retainedPublishBuilder.userProperty("key", "val" + '\uD800'));
    }

    @Test
    public void test_user_property_name_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> retainedPublishBuilder.userProperty(RandomStringUtils.randomAlphanumeric(65536), "val"));
    }

    @Test
    public void test_user_property_value_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> retainedPublishBuilder.userProperty("name", RandomStringUtils.randomAlphanumeric(65536)));
    }

    @Test
    public void test_response_topic_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> retainedPublishBuilder.responseTopic(RandomStringUtils.randomAlphanumeric(65536)));
    }

    @Test
    public void test_content_type_too_long() {

        assertThrows(
                IllegalArgumentException.class,
                () -> retainedPublishBuilder.contentType(RandomStringUtils.randomAlphanumeric(65536)));
    }

    @Test
    public void test_from_retained_publish() {

        final UserPropertiesImpl userProperties = UserPropertiesImpl.of(ImmutableList.of(
                new MqttUserProperty("name", "value"),
                new MqttUserProperty("name", "value2"),
                new MqttUserProperty("name2", "val")));

        final RetainedPublishImpl retainedPublish = new RetainedPublishImpl(
                Qos.AT_MOST_ONCE,
                "topic",
                PayloadFormatIndicator.UTF_8,
                12345L,
                "response_topic",
                ByteBuffer.wrap("correlation_data".getBytes()),
                "content_type",
                ByteBuffer.wrap("test3".getBytes()),
                userProperties);

        final RetainedPublish built =
                retainedPublishBuilder.fromPublish(retainedPublish).build();

        assertEquals(retainedPublish.getQos(), built.getQos());
        assertEquals(retainedPublish.getRetain(), built.getRetain());
        assertEquals(retainedPublish.getTopic(), built.getTopic());
        assertEquals(retainedPublish.getPayloadFormatIndicator(), built.getPayloadFormatIndicator());
        assertEquals(retainedPublish.getMessageExpiryInterval(), built.getMessageExpiryInterval());
        assertEquals(retainedPublish.getResponseTopic(), built.getResponseTopic());
        assertEquals(retainedPublish.getCorrelationData(), built.getCorrelationData());
        assertEquals(retainedPublish.getContentType(), built.getContentType());
        assertEquals(retainedPublish.getPayload(), built.getPayload());
        assertEquals(
                retainedPublish.getUserProperties().asList().size(),
                built.getUserProperties().asList().size());

        assertEquals(Qos.AT_MOST_ONCE, built.getQos());
        assertTrue(built.getRetain());
        assertEquals("topic", built.getTopic());
        assertTrue(built.getPayloadFormatIndicator().isPresent());
        assertEquals(
                PayloadFormatIndicator.UTF_8, built.getPayloadFormatIndicator().get());
        assertTrue(built.getMessageExpiryInterval().isPresent());
        assertEquals(12345L, built.getMessageExpiryInterval().get().longValue());
        assertTrue(built.getResponseTopic().isPresent());
        assertEquals("response_topic", built.getResponseTopic().get());
        assertTrue(built.getCorrelationData().isPresent());
        assertEquals(
                ByteBuffer.wrap("correlation_data".getBytes()),
                built.getCorrelationData().get());
        assertTrue(built.getContentType().isPresent());
        assertEquals("content_type", built.getContentType().get());
        assertTrue(built.getPayload().isPresent());
        assertEquals(ByteBuffer.wrap("test3".getBytes()), built.getPayload().get());
        assertEquals(
                userProperties.asList().size(),
                built.getUserProperties().asList().size());
    }

    @Test
    public void test_from_publish_packet() {

        final PublishPacket publishPacket = new PublishPacketImpl(TestMessageUtil.createFullMqtt5Publish());
        final RetainedPublish built =
                retainedPublishBuilder.fromPublish(publishPacket).build();

        assertEquals(publishPacket.getQos(), built.getQos());
        assertEquals(publishPacket.getRetain(), built.getRetain());
        assertEquals(publishPacket.getTopic(), built.getTopic());
        assertEquals(publishPacket.getPayloadFormatIndicator(), built.getPayloadFormatIndicator());
        assertEquals(publishPacket.getMessageExpiryInterval(), built.getMessageExpiryInterval());
        assertEquals(publishPacket.getResponseTopic(), built.getResponseTopic());
        assertEquals(publishPacket.getCorrelationData(), built.getCorrelationData());
        assertEquals(publishPacket.getContentType(), built.getContentType());
        assertEquals(publishPacket.getPayload(), built.getPayload());
        assertEquals(
                publishPacket.getUserProperties().asList().size(),
                built.getUserProperties().asList().size());

        assertEquals(Qos.EXACTLY_ONCE, built.getQos());
        assertTrue(built.getRetain());
        assertEquals("topic", built.getTopic());
        assertTrue(built.getPayloadFormatIndicator().isPresent());
        assertEquals(
                PayloadFormatIndicator.UTF_8, built.getPayloadFormatIndicator().get());
        assertTrue(built.getMessageExpiryInterval().isPresent());
        assertEquals(360L, built.getMessageExpiryInterval().get().longValue());
        assertTrue(built.getResponseTopic().isPresent());
        assertEquals("response topic", built.getResponseTopic().get());
        assertTrue(built.getCorrelationData().isPresent());
        assertEquals(
                ByteBuffer.wrap("correlation data".getBytes()),
                built.getCorrelationData().get());
        assertTrue(built.getContentType().isPresent());
        assertEquals("content type", built.getContentType().get());
        assertTrue(built.getPayload().isPresent());
        assertEquals(ByteBuffer.wrap("payload".getBytes()), built.getPayload().get());
        assertEquals(2, built.getUserProperties().asList().size());
    }

    @Test
    public void test_from_publish_packet_implemented() {

        final PublishPacket publishPacket = new TestPublishPacket();
        assertThatThrownBy(
                        () -> retainedPublishBuilder.fromPublish(publishPacket).build())
                .isInstanceOf(DoNotImplementException.class);
    }

    @Test
    public void test_precondition_topic() {

        assertThrows(NullPointerException.class, () -> retainedPublishBuilder.build());
    }

    @Test
    public void test_minimum() {
        final RetainedPublish retainedPublish = retainedPublishBuilder
                .topic("topic")
                .payload(ByteBuffer.wrap("payload".getBytes()))
                .build();

        assertEquals(Qos.AT_MOST_ONCE, retainedPublish.getQos());
        assertEquals("topic", retainedPublish.getTopic());
        assertArrayEquals(
                "payload".getBytes(), retainedPublish.getPayload().get().array());
        assertEquals(Optional.empty(), retainedPublish.getPayloadFormatIndicator());
        assertTrue(retainedPublish.getMessageExpiryInterval().isPresent());
        assertEquals(
                configurationService.mqttConfiguration().maxMessageExpiryInterval(),
                retainedPublish.getMessageExpiryInterval().get().longValue());
        assertEquals(Optional.empty(), retainedPublish.getResponseTopic());
        assertEquals(Optional.empty(), retainedPublish.getCorrelationData());
        assertEquals(Optional.empty(), retainedPublish.getContentType());
        assertEquals(0, retainedPublish.getUserProperties().asList().size());
    }

    static class TestPublishPacket implements PublishPacket {

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
