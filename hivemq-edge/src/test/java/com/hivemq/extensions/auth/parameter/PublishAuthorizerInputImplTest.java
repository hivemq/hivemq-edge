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
package com.hivemq.extensions.auth.parameter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.publish.PUBLISH;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.TestMessageUtil;

/**
 * @author Christoph Schäbel
 */
public class PublishAuthorizerInputImplTest {

    private Channel channel;
    private ClientConnection clientConnection;

    @BeforeEach
    public void before() {
        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
    }

    @Test
    public void test_publish_null() {

        assertThrows(
                NullPointerException.class, () -> new PublishAuthorizerInputImpl((PUBLISH) null, channel, "client"));
    }

    @Test
    public void test_channel_null() {
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");
        assertThatThrownBy(() -> new PublishAuthorizerInputImpl(publish, null, "client"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void test_clientid_null() {
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");
        assertThatThrownBy(() -> new PublishAuthorizerInputImpl(publish, channel, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void test_publish() {
        final PUBLISH publish = TestMessageUtil.createMqtt5Publish("topic");
        final PublishAuthorizerInputImpl input = new PublishAuthorizerInputImpl(publish, channel, "client");

        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPublishPacket());
    }

    @Test
    public void test_will_publish() {
        final CONNECT connect = TestMessageUtil.createMqtt5ConnectWithWill();
        final PublishAuthorizerInputImpl input =
                new PublishAuthorizerInputImpl(connect.getWillPublish(), channel, "client");

        assertNotNull(input.getClientInformation());
        assertNotNull(input.getConnectionInformation());
        assertNotNull(input.getPublishPacket());
    }
}
