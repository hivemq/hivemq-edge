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
package com.hivemq.codec.encoder;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.codec.encoder.mqtt3.Mqtt3ConnackEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3DisconnectEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3PubackEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3PubcompEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3PublishEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3PubrecEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3PubrelEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3SubackEncoder;
import com.hivemq.codec.encoder.mqtt3.Mqtt3UnsubackEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5AuthEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5ConnackEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5DisconnectEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PubCompEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PubackEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PublishEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PubrecEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PubrelEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5SubackEncoder;
import com.hivemq.codec.encoder.mqtt5.Mqtt5UnsubackEncoder;
import com.hivemq.configuration.service.SecurityConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.Message;
import com.hivemq.mqtt.message.PINGRESP;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.auth.AUTH;
import com.hivemq.mqtt.message.connack.CONNACK;
import com.hivemq.mqtt.message.disconnect.DISCONNECT;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.puback.PUBACK;
import com.hivemq.mqtt.message.pubcomp.PUBCOMP;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.pubrec.PUBREC;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.unsuback.UNSUBACK;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * This factory is used to create encoders and encode messages.
 *
 * @author Waldemar Ruck
 * @since 4.0
 */
@Singleton
public class EncoderFactory {

    private static final Logger log = LoggerFactory.getLogger(EncoderFactory.class);

    private final @NotNull Mqtt5EncoderFactory mqtt5Instance;
    private final @NotNull Mqtt3EncoderFactory mqtt3Instance;

    @Inject
    public EncoderFactory(
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull SecurityConfigurationService securityConfigurationService,
            final @NotNull MqttServerDisconnector mqttServerDisconnector) {
        mqtt5Instance = new Mqtt5EncoderFactory(messageDroppedService, securityConfigurationService);
        mqtt3Instance = new Mqtt3EncoderFactory(mqttServerDisconnector);
    }

    /**
     * Finds the {@link MqttEncoder} encoder and encodes the {@link Message} message.
     *
     * @param clientConnection the {@link ClientConnection} of the client
     * @param msg              the {@link Message} to encode
     * @param out              the {@link ByteBuf} into which the encoded message will be written
     */
    public void encode(
            final @NotNull ClientConnection clientConnection, final @NotNull Message msg, final @NotNull ByteBuf out) {

        final MqttEncoder encoder = getEncoder(msg, clientConnection);
        if (encoder != null) {
            encoder.encode(clientConnection, msg, out);
        } else {
            log.error("No encoder found for msg: {} ", msg.getType());
        }
    }

    /**
     * This method finds the Mqtt encoder depending on the message and the protocol version.
     *
     * @param msg              the {@link Message} is used to identify the encoder
     * @param clientConnection the {@link ClientConnection} of the mqtt client
     * @return {@link MqttEncoder} encoder depends on the message and protocol
     */
    protected @Nullable MqttEncoder getEncoder(
            final @NotNull Message msg, final @NotNull ClientConnection clientConnection) {

        if (clientConnection.getProtocolVersion() == ProtocolVersion.MQTTv5) {
            return mqtt5Instance.getEncoder(msg);
        } else {
            return mqtt3Instance.getEncoder(msg);
        }
    }

    protected @NotNull ByteBuf allocateBuffer(
            final @NotNull ClientConnection clientConnection, final @NotNull Message msg, final boolean preferDirect) {

        final MqttEncoder encoder = getEncoder(msg, clientConnection);
        if (encoder != null) {
            final int bufferSize = encoder.bufferSize(clientConnection, msg);
            if (preferDirect) {
                return clientConnection.getChannel().alloc().ioBuffer(bufferSize);
            } else {
                return clientConnection.getChannel().alloc().heapBuffer(bufferSize);
            }
        }

        if (preferDirect) {
            return clientConnection.getChannel().alloc().ioBuffer();
        } else {
            return clientConnection.getChannel().alloc().heapBuffer();
        }
    }

    /**
     * Factory for Mqtt5 encoders.
     */
    private static class Mqtt5EncoderFactory {

        private final @NotNull Mqtt5PublishEncoder mqtt5PublishEncoder;
        private final @NotNull Mqtt5DisconnectEncoder mqtt5DisconnectEncoder;
        private final @NotNull Mqtt5SubackEncoder mqtt5SubackEncoder;
        private final @NotNull Mqtt5ConnackEncoder mqtt5ConnackEncoder;
        private final @NotNull Mqtt5PubackEncoder mqtt5PubackEncoder;
        private final @NotNull Mqtt5PubrecEncoder mqtt5PubrecEncoder;
        private final @NotNull Mqtt5PubrelEncoder mqtt5PubrelEncoder;
        private final @NotNull Mqtt5PubCompEncoder mqtt5PubCompEncoder;
        private final @NotNull Mqtt5AuthEncoder mqtt5AuthEncoder;
        private final @NotNull Mqtt5UnsubackEncoder mqtt5UnsubackEncoder;
        private final @NotNull MqttPingrespEncoder mqttPingrespEncoder;

        Mqtt5EncoderFactory(
                final @NotNull MessageDroppedService messageDroppedService,
                final @NotNull SecurityConfigurationService securityConfigurationService) {

            mqtt5PublishEncoder = new Mqtt5PublishEncoder(messageDroppedService, securityConfigurationService);
            mqtt5DisconnectEncoder = new Mqtt5DisconnectEncoder(messageDroppedService, securityConfigurationService);
            mqtt5SubackEncoder = new Mqtt5SubackEncoder(messageDroppedService, securityConfigurationService);
            mqtt5ConnackEncoder = new Mqtt5ConnackEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubackEncoder = new Mqtt5PubackEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubrecEncoder = new Mqtt5PubrecEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubrelEncoder = new Mqtt5PubrelEncoder(messageDroppedService, securityConfigurationService);
            mqtt5PubCompEncoder = new Mqtt5PubCompEncoder(messageDroppedService, securityConfigurationService);
            mqtt5AuthEncoder = new Mqtt5AuthEncoder(messageDroppedService, securityConfigurationService);
            mqtt5UnsubackEncoder = new Mqtt5UnsubackEncoder(messageDroppedService, securityConfigurationService);
            mqttPingrespEncoder = new MqttPingrespEncoder();
        }

        private @Nullable MqttEncoder getEncoder(final @NotNull Message msg) {
            if (msg instanceof PUBLISH) {
                return mqtt5PublishEncoder;
            } else if (msg instanceof PINGRESP) {
                return mqttPingrespEncoder;
            } else if (msg instanceof PUBACK) {
                return mqtt5PubackEncoder;
            } else if (msg instanceof PUBREC) {
                return mqtt5PubrecEncoder;
            } else if (msg instanceof PUBREL) {
                return mqtt5PubrelEncoder;
            } else if (msg instanceof PUBCOMP) {
                return mqtt5PubCompEncoder;
            } else if (msg instanceof CONNACK) {
                return mqtt5ConnackEncoder;
            } else if (msg instanceof SUBACK) {
                return mqtt5SubackEncoder;
            } else if (msg instanceof UNSUBACK) {
                return mqtt5UnsubackEncoder;
            } else if (msg instanceof DISCONNECT) {
                return mqtt5DisconnectEncoder;
            } else if (msg instanceof AUTH) {
                return mqtt5AuthEncoder;
            }
            return null;
        }
    }

    /**
     * Factory for Mqtt3 encoders.
     */
    private static class Mqtt3EncoderFactory {

        private final @NotNull Mqtt3ConnackEncoder connackEncoder;
        private final @NotNull Mqtt3PubackEncoder pubackEncoder;
        private final @NotNull Mqtt3PubrecEncoder pubrecEncoder;
        private final @NotNull Mqtt3PubrelEncoder pubrelEncoder;
        private final @NotNull Mqtt3PubcompEncoder pubcompEncoder;
        private final @NotNull Mqtt3SubackEncoder subackEncoder;
        private final @NotNull Mqtt3UnsubackEncoder unsubackEncoder;
        private final @NotNull Mqtt3PublishEncoder publishEncoder;
        private final @NotNull Mqtt3DisconnectEncoder disconnectEncoder;
        private final @NotNull MqttPingrespEncoder pingrespEncoder;

        Mqtt3EncoderFactory(final @NotNull MqttServerDisconnector mqttServerDisconnector) {
            connackEncoder = new Mqtt3ConnackEncoder();
            pubackEncoder = new Mqtt3PubackEncoder();
            pubrecEncoder = new Mqtt3PubrecEncoder();
            pubrelEncoder = new Mqtt3PubrelEncoder();
            pubcompEncoder = new Mqtt3PubcompEncoder();
            subackEncoder = new Mqtt3SubackEncoder(mqttServerDisconnector);
            unsubackEncoder = new Mqtt3UnsubackEncoder();
            publishEncoder = new Mqtt3PublishEncoder();
            disconnectEncoder = new Mqtt3DisconnectEncoder();
            pingrespEncoder = new MqttPingrespEncoder();
        }

        private @Nullable MqttEncoder getEncoder(final @NotNull Message msg) {
            if (msg instanceof PUBLISH) {
                return publishEncoder;
            } else if (msg instanceof PINGRESP) {
                return pingrespEncoder;
            } else if (msg instanceof PUBACK) {
                return pubackEncoder;
            } else if (msg instanceof PUBREC) {
                return pubrecEncoder;
            } else if (msg instanceof PUBREL) {
                return pubrelEncoder;
            } else if (msg instanceof PUBCOMP) {
                return pubcompEncoder;
            } else if (msg instanceof CONNACK) {
                return connackEncoder;
            } else if (msg instanceof SUBACK) {
                return subackEncoder;
            } else if (msg instanceof UNSUBACK) {
                return unsubackEncoder;
            } else if (msg instanceof DISCONNECT) {
                return disconnectEncoder;
            }
            return null;
        }
    }
}
