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
package com.hivemq.mqtt.handler.publish;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.impl.RestrictionsConfigurationServiceImpl;
import com.hivemq.extensions.handler.IncomingPublishHandler;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.persistence.local.IncomingMessageFlowInMemoryLocalPersistence;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistence.AddResult;
import com.hivemq.persistence.qos.IncomingMessageFlowPersistenceImpl;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * The inbound QoS 2 receive state, as the reproduction of BCD-600 on Edge. Before the fix the store held the whole
 * PUBLISH, payload included, from the PUBLISH until PUBREL, and a persistent-session disconnect left it there. The
 * store now holds the packet identifier only: a persistent session keeps it (the client may retransmit the PUBLISH
 * after reconnecting), a session that ends on disconnect drops it, and PUBREL releases it.
 */
public class PublishFlowHandlerQos2RetainedStateTest {

    private static final String CLIENT_ID = "client";
    private static final int PACKET_ID = 100;

    private final IncomingMessageFlowInMemoryLocalPersistence store = new IncomingMessageFlowInMemoryLocalPersistence(
            new RestrictionsConfigurationServiceImpl(), new MetricRegistry());

    @Test
    public void persistent_session_keeps_only_the_packet_identifier() {
        final EmbeddedChannel channel = channel(3600L);

        channel.writeInbound(qos2Publish(new byte[1024 * 1024]));
        channel.finish();

        assertThat(held(CLIENT_ID, PACKET_ID)).isTrue();
        assertThat(store.size()).isEqualTo(1);
    }

    @Test
    public void session_ending_on_disconnect_drops_the_packet_identifier() {
        final EmbeddedChannel channel = channel(0L);

        channel.writeInbound(qos2Publish("payload".getBytes(UTF_8)));
        channel.finish();

        assertThat(held(CLIENT_ID, PACKET_ID)).isFalse();
        assertThat(store.size()).isZero();
    }

    @Test
    public void pubrel_releases_the_packet_identifier() {
        final EmbeddedChannel channel = channel(3600L);

        channel.writeInbound(qos2Publish("payload".getBytes(UTF_8)));
        channel.writeInbound(new PUBREL(PACKET_ID));

        assertThat(held(CLIENT_ID, PACKET_ID)).isFalse();
        assertThat(store.size()).isZero();
    }

    private @NotNull EmbeddedChannel channel(final long sessionExpiryInterval) {
        InternalConfigurations.MAX_INFLIGHT_WINDOW_SIZE_MESSAGES = 5;
        final EmbeddedChannel channel = new EmbeddedChannel(new PublishFlowHandler(
                mock(PublishPollService.class),
                new IncomingMessageFlowPersistenceImpl(store),
                new OrderedTopicService(),
                mock(IncomingPublishHandler.class),
                mock(DropOutgoingPublishesHandler.class),
                mock(MqttServerDisconnector.class)));
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        clientConnection.setClientId(CLIENT_ID);
        clientConnection.setClientSessionExpiryInterval(sessionExpiryInterval);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        return channel;
    }

    private static @NotNull PUBLISH qos2Publish(final byte @NotNull [] payload) {
        return new PUBLISHFactory.Mqtt3Builder()
                .withHivemqId("hivemqId")
                .withTopic("topic")
                .withQoS(QoS.EXACTLY_ONCE)
                .withOnwardQos(QoS.EXACTLY_ONCE)
                .withPacketIdentifier(PACKET_ID)
                .withPayload(payload)
                .build();
    }

    /**
     * Whether the identifier is held, probed through the store's own API: {@code add} answers a held identifier with
     * {@code ALREADY_PRESENT}, and a probe that added it removes it again.
     */
    private boolean held(final String client, final int messageId) {
        final AddResult result = store.add(client, messageId);
        if (result == AddResult.ADDED) {
            store.remove(client, messageId);
        }
        return result == AddResult.ALREADY_PRESENT;
    }
}
