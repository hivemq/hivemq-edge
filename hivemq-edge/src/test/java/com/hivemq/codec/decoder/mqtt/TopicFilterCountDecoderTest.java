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
package com.hivemq.codec.decoder.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.reason.Mqtt5DisconnectReasonCode;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.unsubscribe.UNSUBSCRIBE;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import util.TestConfigurationBootstrap;
import util.TestMqttDecoder;

/**
 * The topic-filter limit of SUBSCRIBE and UNSUBSCRIBE (restriction {@code max-topic-filters-per-packet}) in the four
 * decoders that read filters until the packet ends. Each filter on the wire here is 4 bytes (SUBSCRIBE) or 3 bytes
 * (UNSUBSCRIBE), so a packet within any packet-size limit can carry far more filters than the limit allows.
 */
public class TopicFilterCountDecoderTest {

    private enum Shape {
        MQTT5_SUBSCRIBE(ProtocolVersion.MQTTv5, true),
        MQTT5_UNSUBSCRIBE(ProtocolVersion.MQTTv5, false),
        MQTT3_SUBSCRIBE(ProtocolVersion.MQTTv3_1_1, true),
        MQTT3_UNSUBSCRIBE(ProtocolVersion.MQTTv3_1_1, false);

        final @NotNull ProtocolVersion protocolVersion;
        final boolean subscribe;

        Shape(final @NotNull ProtocolVersion protocolVersion, final boolean subscribe) {
            this.protocolVersion = protocolVersion;
            this.subscribe = subscribe;
        }
    }

    private EmbeddedChannel channel;

    @AfterEach
    public void tearDown() {
        if (channel != null) {
            channel.close();
        }
    }

    @ParameterizedTest
    @EnumSource(Shape.class)
    public void at_the_limit_the_packet_is_decoded(final @NotNull Shape shape) {
        createChannel(shape.protocolVersion, 5);

        channel.writeInbound(encode(shape, 5));

        assertThat(decodedFilterCount(shape)).isEqualTo(5);
        assertThat(channel.isActive()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Shape.class)
    public void one_filter_over_the_limit_disconnects_without_decoding(final @NotNull Shape shape) {
        createChannel(shape.protocolVersion, 5);

        channel.writeInbound(encode(shape, 6));

        assertRefused(shape);
    }

    @ParameterizedTest
    @EnumSource(Shape.class)
    public void default_limit_is_10000(final @NotNull Shape shape) {
        createChannel(shape.protocolVersion, RestrictionsConfigurationService.MAX_TOPIC_FILTERS_PER_PACKET_DEFAULT);
        channel.writeInbound(encode(shape, 10_000));
        assertThat(decodedFilterCount(shape)).isEqualTo(10_000);

        createChannel(shape.protocolVersion, RestrictionsConfigurationService.MAX_TOPIC_FILTERS_PER_PACKET_DEFAULT);
        channel.writeInbound(encode(shape, 10_001));
        assertRefused(shape);
    }

    /**
     * The reproduction of BCD-764 on Edge: before the limit, one SUBSCRIBE of 200,000 filters (800 kB on the wire)
     * decoded into 200,000 subscriptions with the channel left open.
     */
    @Test
    public void subscribe_with_200000_filters_is_refused() {
        createChannel(ProtocolVersion.MQTTv5, RestrictionsConfigurationService.MAX_TOPIC_FILTERS_PER_PACKET_DEFAULT);

        channel.writeInbound(encode(Shape.MQTT5_SUBSCRIBE, 200_000));

        assertRefused(Shape.MQTT5_SUBSCRIBE);
    }

    private void createChannel(final @NotNull ProtocolVersion protocolVersion, final int limit) {
        if (channel != null) {
            channel.close();
        }
        final ConfigurationService config = new TestConfigurationBootstrap().getConfigurationService();
        config.restrictionsConfiguration().setMaxTopicFiltersPerPacket(limit);
        channel = new EmbeddedChannel(TestMqttDecoder.create(config));
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        clientConnection.setProtocolVersion(protocolVersion);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
    }

    private int decodedFilterCount(final @NotNull Shape shape) {
        if (shape.subscribe) {
            final SUBSCRIBE subscribe = channel.readInbound();
            assertThat(subscribe).isNotNull();
            return subscribe.getTopics().size();
        }
        final UNSUBSCRIBE unsubscribe = channel.readInbound();
        assertThat(unsubscribe).isNotNull();
        return unsubscribe.getTopics().size();
    }

    private void assertRefused(final @NotNull Shape shape) {
        assertThat((Object) channel.readInbound()).isNull();
        channel.runPendingTasks();
        if (shape.protocolVersion == ProtocolVersion.MQTTv5) {
            final DISCONNECT disconnect = channel.readOutbound();
            assertThat(disconnect).isNotNull();
            assertThat(disconnect.getReasonCode()).isEqualTo(Mqtt5DisconnectReasonCode.QUOTA_EXCEEDED);
        } else {
            assertThat((Object) channel.readOutbound()).isNull();
        }
        assertThat(channel.isActive()).isFalse();
    }

    private @NotNull ByteBuf encode(final @NotNull Shape shape, final int filters) {
        final boolean mqtt5 = shape.protocolVersion == ProtocolVersion.MQTTv5;
        // each filter: 2-byte length, the one-byte filter 'a', and for SUBSCRIBE one options/QoS byte
        final int perFilter = shape.subscribe ? 4 : 3;
        final int remainingLength = 2 + (mqtt5 ? 1 : 0) + filters * perFilter;
        final ByteBuf buf = channel.alloc().buffer();
        buf.writeByte(shape.subscribe ? 0b1000_0010 : 0b1010_0010);
        writeVariableByteInteger(buf, remainingLength);
        buf.writeShort(1); // packet identifier
        if (mqtt5) {
            buf.writeByte(0); // properties length
        }
        for (int i = 0; i < filters; i++) {
            buf.writeShort(1);
            buf.writeByte('a');
            if (shape.subscribe) {
                buf.writeByte(0); // QoS 0
            }
        }
        return buf;
    }

    private static void writeVariableByteInteger(final @NotNull ByteBuf buf, final int value) {
        int remaining = value;
        do {
            int encodedByte = remaining % 128;
            remaining /= 128;
            if (remaining > 0) {
                encodedByte |= 0x80;
            }
            buf.writeByte(encodedByte);
        } while (remaining > 0);
    }
}
