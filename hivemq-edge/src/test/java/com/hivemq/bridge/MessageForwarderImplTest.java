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
package com.hivemq.bridge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class MessageForwarderImplTest {

    // EDG-882: the Base64 subscription hash may contain '/'
    private static final String FORWARDER_ID = "bridge-Bt80p78iNo/w7W1W7bGwcg==";
    private static final String TOPIC = "miele/v1/production/sapdm/dev/+/+/from-plc-to-dm";
    private static final String QUEUE_ID = MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID + "/" + TOPIC;

    // a second bridge whose hash also carries a '/', to pin cross-forwarder isolation
    private static final String OTHER_FORWARDER_ID = "other-LHHqscdwUV/C1PCBYfZ6Bg==";
    private static final String OTHER_TOPIC = "other/topic/+";
    private static final String OTHER_QUEUE_ID =
            MessageForwarderImpl.FORWARDER_PREFIX + OTHER_FORWARDER_ID + "/" + OTHER_TOPIC;

    private MessageForwarderImpl messageForwarder;
    private MqttForwarder mqttForwarder;
    private MqttForwarder otherMqttForwarder;

    @BeforeEach
    public void setUp() {
        messageForwarder = new MessageForwarderImpl(
                mock(LocalTopicTree.class),
                new HivemqId(),
                () -> null,
                mock(SingleWriterService.class),
                mock(ShutdownHooks.class));
        mqttForwarder = forwarder(FORWARDER_ID, TOPIC);
        otherMqttForwarder = forwarder(OTHER_FORWARDER_ID, OTHER_TOPIC);
    }

    private static MqttForwarder forwarder(final String id, final String topic) {
        final MqttForwarder forwarder = mock(MqttForwarder.class);
        when(forwarder.getId()).thenReturn(id);
        when(forwarder.getTopics()).thenReturn(List.of(topic));
        return forwarder;
    }

    @Test
    @Timeout(5)
    public void test_isForwarderQueue_follows_forwarder_registration() {
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));

        messageForwarder.addForwarder(mqttForwarder);
        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertFalse(messageForwarder.isForwarderQueue(MessageForwarderImpl.FORWARDER_PREFIX + "other/" + TOPIC));

        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
    }

    @Test
    @Timeout(5)
    public void test_isForwarderQueue_resolves_each_forwarder_independently() {
        messageForwarder.addForwarder(mqttForwarder);
        messageForwarder.addForwarder(otherMqttForwarder);

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertTrue(messageForwarder.isForwarderQueue(OTHER_QUEUE_ID));
        // a queue may not be claimed by the wrong forwarder's registration
        assertFalse(messageForwarder.isForwarderQueue(
                MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID + "/" + OTHER_TOPIC));

        // removing one forwarder must not deregister the other's queue
        messageForwarder.removeForwarder(mqttForwarder, false);
        assertFalse(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertTrue(messageForwarder.isForwarderQueue(OTHER_QUEUE_ID));
    }

    @Test
    @Timeout(5)
    public void test_isForwarderQueue_covers_every_topic_of_a_multi_topic_forwarder() {
        final MqttForwarder multiTopic = mock(MqttForwarder.class);
        when(multiTopic.getId()).thenReturn(FORWARDER_ID);
        when(multiTopic.getTopics()).thenReturn(List.of(TOPIC, OTHER_TOPIC));

        messageForwarder.addForwarder(multiTopic);

        assertTrue(messageForwarder.isForwarderQueue(QUEUE_ID));
        assertTrue(messageForwarder.isForwarderQueue(
                MessageForwarderImpl.FORWARDER_PREFIX + FORWARDER_ID + "/" + OTHER_TOPIC));
    }
}
