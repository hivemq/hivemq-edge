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
package com.hivemq.mqtt.message.connack;

import static com.hivemq.mqtt.message.connack.CONNACK.KEEP_ALIVE_NOT_SET;
import static com.hivemq.mqtt.message.connack.CONNACK.SESSION_EXPIRY_NOT_SET;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hivemq.mqtt.message.MessageType;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.connect.CONNECT;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.reason.Mqtt5ConnAckReasonCode;
import org.junit.jupiter.api.Test;

/**
 * @author Waldemar Ruck
 * @since 4.0
 */
public class CONNACKBuilderTest {

    private final CONNACK.Mqtt5Builder builder =
            new CONNACK.Mqtt5Builder().withReasonCode(Mqtt5ConnAckReasonCode.SUCCESS);
    private final String dataExceeded = new String(new char[65535 + 1]);
    private final int sizeExceeded = 65535 + 1;

    @Test
    public void test_builder_default_values() {

        final CONNACK connack = builder.build();

        assertEquals(Mqtt3ConnAckReturnCode.ACCEPTED, connack.getReturnCode());
        assertNull(connack.getAssignedClientIdentifier());
        assertNull(connack.getAuthData());
        assertNull(connack.getAuthMethod());
        assertEquals(CONNECT.DEFAULT_MAXIMUM_PACKET_SIZE_NO_LIMIT, connack.getMaximumPacketSize());
        assertNull(connack.getMaximumQoS());
        assertEquals(CONNECT.DEFAULT_RECEIVE_MAXIMUM, connack.getReceiveMaximum());
        assertNull(connack.getResponseInformation());
        assertEquals(KEEP_ALIVE_NOT_SET, connack.getServerKeepAlive());
        assertNull(connack.getServerReference());
        assertEquals(SESSION_EXPIRY_NOT_SET, connack.getSessionExpiryInterval());
        assertEquals(CONNECT.DEFAULT_TOPIC_ALIAS_MAXIMUM, connack.getTopicAliasMaximum());
        assertEquals(MessageType.CONNACK, connack.getType());
        assertEquals(0, connack.getPacketIdentifier());
        assertEquals(Mqtt5ConnAckReasonCode.SUCCESS, connack.getReasonCode());
        assertEquals(0, connack.getUserProperties().asList().size());
        assertFalse(connack.isSessionPresent());
        assertNull(connack.getReasonString());
        assertTrue(connack.isRetainAvailable());
        assertTrue(connack.isSharedSubscriptionAvailable());
        assertTrue(connack.isSubscriptionIdentifierAvailable());
        assertTrue(connack.isWildcardSubscriptionAvailable());
    }

    @Test
    public void test_builder_set_values() {

        final int serverKeepAlive = 30;
        final int topicAliasMaximum = 5;
        final int maximumPacketSize = 300;
        final long sessionExpiryInterval = 100;
        final String serverReference = "HiveMQ 4";
        final String responseInformation = "INFO";
        final byte[] authData = new byte[65535];
        final String authMethod = "Method";
        final int receiveMaximum = 100;
        final QoS maximumQoS = QoS.AT_MOST_ONCE;
        final boolean sessionPresent = true;
        final String assignedClientIdentifier = "subscriber";
        final String reasonString = "human readable ...";
        final boolean retainAvailable = true;
        final boolean sharedSubscriptionAvailable = true;
        final boolean subscriptionIdentifierAvailable = true;

        final MqttUserProperty userProperty = new MqttUserProperty("test1", "value");
        final Mqtt5UserProperties userProperties = Mqtt5UserProperties.of(userProperty, userProperty);

        final boolean wildcardSubscriptionAvailable = true;

        final CONNACK connack = builder.withServerKeepAlive(serverKeepAlive)
                .withTopicAliasMaximum(topicAliasMaximum)
                .withMaximumPacketSize(maximumPacketSize)
                .withSessionExpiryInterval(sessionExpiryInterval)
                .withServerReference(serverReference)
                .withResponseInformation(responseInformation)
                .withAuthData(authData)
                .withAuthMethod(authMethod)
                .withReceiveMaximum(receiveMaximum)
                .withMaximumQoS(maximumQoS)
                .withSessionPresent(sessionPresent)
                .withAssignedClientIdentifier(assignedClientIdentifier)
                .withReasonString(reasonString)
                .withRetainAvailable(retainAvailable)
                .withSharedSubscriptionAvailable(sharedSubscriptionAvailable)
                .withSubscriptionIdentifierAvailable(subscriptionIdentifierAvailable)
                .withUserProperties(userProperties)
                .withWildcardSubscriptionAvailable(wildcardSubscriptionAvailable)
                .build();

        assertEquals(serverKeepAlive, connack.getServerKeepAlive());
        assertEquals(topicAliasMaximum, connack.getTopicAliasMaximum());
        assertEquals(maximumPacketSize, connack.getMaximumPacketSize());
        assertEquals(sessionExpiryInterval, connack.getSessionExpiryInterval());
        assertEquals(serverReference, connack.getServerReference());
        assertEquals(responseInformation, connack.getResponseInformation());
        assertEquals(connack.getAuthData(), authData);
        assertEquals(authMethod, connack.getAuthMethod());
        assertEquals(receiveMaximum, connack.getReceiveMaximum());
        assertEquals(maximumQoS, connack.getMaximumQoS());
        assertEquals(sessionPresent, connack.isSessionPresent());
        assertEquals(assignedClientIdentifier, connack.getAssignedClientIdentifier());
        assertEquals(reasonString, connack.getReasonString());
        assertEquals(retainAvailable, connack.isRetainAvailable());
        assertEquals(sharedSubscriptionAvailable, connack.isSharedSubscriptionAvailable());
        assertEquals(subscriptionIdentifierAvailable, connack.isSubscriptionIdentifierAvailable());
        assertEquals(connack.getUserProperties(), userProperties);
        assertEquals(wildcardSubscriptionAvailable, connack.isWildcardSubscriptionAvailable());

        assertEquals(Mqtt3ConnAckReturnCode.ACCEPTED, connack.getReturnCode());
        assertEquals(MessageType.CONNACK, connack.getType());
        assertEquals(0, connack.getPacketIdentifier());
        assertEquals(Mqtt5ConnAckReasonCode.SUCCESS, connack.getReasonCode());
    }

    @Test
    public void test_receiveMaximum_precondition() {
        assertThatThrownBy(() -> builder.withReceiveMaximum(0).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Receive maximum must never be zero");
    }

    @Test
    public void test_authMethod_precondition() {
        assertThatThrownBy(() -> builder.withAuthMethod(dataExceeded).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An auth method must never exceed 65.535 bytes");
    }

    @Test
    public void test_authData_method_precondition() {
        final byte[] dataExceeded = new byte[65535];
        assertThatThrownBy(() -> builder.withAuthData(dataExceeded).build())
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Auth method must be set if auth data is set");
    }

    @Test
    public void test_authData_precondition() {
        final byte[] dataExceeded = new byte[sizeExceeded];
        assertThatThrownBy(() -> builder.withAuthMethod("Method")
                        .withAuthData(dataExceeded)
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("An auth data must never exceed 65.535 bytes");
    }

    @Test
    public void test_responseInformation_precondition() {
        assertThatThrownBy(() -> builder.withResponseInformation(dataExceeded).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A response information must never exceed 65.535 bytes");
    }

    @Test
    public void test_serverReference_precondition() {
        assertThatThrownBy(() -> builder.withServerReference(dataExceeded).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A server reference must never exceed 65.535 bytes");
    }

    @Test
    public void test_sessionExpiryInterval_precondition() {
        assertThatThrownBy(
                        () -> builder.withSessionExpiryInterval(4294967296L + 1).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A session expiry interval must never be larger than 4.294.967.296");
    }

    @Test
    public void test_maximumPacketSize_precondition() {
        assertThatThrownBy(() -> builder.withMaximumPacketSize(268435460 + 1).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A maximum packet size must never be larger than 268.435.460");
    }

    @Test
    public void test_topicAliasMaximum_precondition() {
        assertThatThrownBy(() -> builder.withTopicAliasMaximum(sizeExceeded).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A topic alias maximum must never be larger than 65.535");
    }

    @Test
    public void test_serverKeepAlive_precondition() {
        assertThatThrownBy(() -> builder.withServerKeepAlive(sizeExceeded).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A server keep alive must never be larger than 65.535");
    }
}
