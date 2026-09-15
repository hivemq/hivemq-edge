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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.mqtt.event.PublishDroppedEvent;
import com.hivemq.mqtt.event.PubrelDroppedEvent;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.pubrel.PUBREL;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.LogbackCapturingAppender;
import util.TestMessageUtil;

/**
 * @author Florian Limpöck
 * @since 4.0.1
 */
public class MessageExpiryHandlerTest {

    // ELAPSED_MILLIS must exceed EXPIRY_INTERVAL, and by more than a second: the handler computes the
    // elapsed time in whole seconds by integer division, so 1500ms elapsed still reads as 1s and would
    // leave a 1s interval intact rather than expiring it.
    private static final long EXPIRY_INTERVAL = 1;
    private static final long ELAPSED_MILLIS = 2000;

    @Mock
    private ChannelHandlerContext ctx;

    private EmbeddedChannel channel;

    LogbackCapturingAppender logCapture;

    // Several tests below flip these two mutable public statics. They are process-wide, so without the
    // save/restore in setUp and tearDown a test leaks its setting into whatever runs next in the same
    // JVM. Harmless while this suite ran sequentially in one JVM; a hazard now that it forks.
    private boolean expireInflightMessages;
    private boolean expireInflightPubrels;

    private boolean dropped;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final MessageExpiryHandler messageExpiryHandler = new MessageExpiryHandler();
        channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        clientConnection.setProtocolVersion(ProtocolVersion.MQTTv5);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("ClientId");
        channel.pipeline().addLast(messageExpiryHandler);
        when(ctx.channel()).thenReturn(channel);
        logCapture = LogbackCapturingAppender.Factory.weaveInto(MessageExpiryHandler.log);
        expireInflightMessages = InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES_ENABLED;
        expireInflightPubrels = InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED;
    }

    @AfterEach
    public void tearDown() throws Exception {
        LogbackCapturingAppender.Factory.cleanUp();
        InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES_ENABLED = expireInflightMessages;
        InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED = expireInflightPubrels;
    }

    /**
     * A PUBLISH whose expiry interval has already lapsed.
     * <p>
     * The handler compares the message timestamp against the current time, so backdating the timestamp is
     * equivalent to waiting for the interval to pass, and costs nothing.
     */
    private static @NotNull PUBLISH expiredPublish(final @NotNull QoS qos, final boolean duplicateDelivery) {
        // The timestamp can only be set at construction, so rebuild the standard test PUBLISH with a
        // backdated one rather than hand-rolling a builder here.
        final PUBLISH publish = new PUBLISHFactory.Mqtt5Builder()
                .fromPublish(TestMessageUtil.createMqtt5Publish("topic", qos))
                .withTimestamp(System.currentTimeMillis() - ELAPSED_MILLIS)
                .build();
        publish.setMessageExpiryInterval(EXPIRY_INTERVAL);
        publish.setDuplicateDelivery(duplicateDelivery);
        return publish;
    }

    /**
     * Writes the message and reports whether the handler fired the given drop event.
     * <p>
     * A plain boolean rather than a CountDownLatch: EmbeddedChannel runs the pipeline on the calling
     * thread, so by the time write returns the event has either fired or it never will. Waiting on a
     * latch here bought nothing but wall-clock time -- and worse, the "should not be dropped" cases had
     * to wait out the full timeout to prove a negative.
     * <p>
     * Each call adds another listener to the pipeline and resets the flag, so a second call reports only
     * its own write -- but the listeners from earlier calls are still installed and will also see it.
     * Every test here writes once.
     */
    private boolean writeAndCheckDropped(final @NotNull Object message, final @NotNull Class<?> dropEventType) {
        // A field rather than a local: the anonymous handler below cannot assign to a local variable.
        // A plain boolean suffices -- EmbeddedChannel fires the event on the thread that calls
        // writeOutbound, so it is written and read by the same thread.
        dropped = false;
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            public void userEventTriggered(final ChannelHandlerContext ctx, final @NotNull Object evt) {
                if (dropEventType.isInstance(evt)) {
                    dropped = true;
                }
            }
        });
        channel.writeOutbound(ctx, message, channel.newPromise());
        return dropped;
    }

    @Test
    public void test_message_expired_qos_0() {
        final PUBLISH publish = expiredPublish(QoS.AT_MOST_ONCE, false);

        assertTrue(writeAndCheckDropped(publish, PublishDroppedEvent.class));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_1() {
        final PUBLISH publish = expiredPublish(QoS.AT_LEAST_ONCE, false);

        assertTrue(writeAndCheckDropped(publish, PublishDroppedEvent.class));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_2_not_dup() {
        final PUBLISH publish = expiredPublish(QoS.EXACTLY_ONCE, false);

        assertTrue(writeAndCheckDropped(publish, PublishDroppedEvent.class));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_qos_2_dup() {
        final PUBLISH publish = expiredPublish(QoS.EXACTLY_ONCE, true);

        assertFalse(writeAndCheckDropped(publish, PublishDroppedEvent.class));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    @Test
    public void test_message_expired_qos_2_dup() {
        InternalConfigurations.EXPIRE_INFLIGHT_MESSAGES_ENABLED = true;
        final PUBLISH publish = expiredPublish(QoS.EXACTLY_ONCE, true);

        assertTrue(writeAndCheckDropped(publish, PublishDroppedEvent.class));
        assertEquals(0, publish.getMessageExpiryInterval());
    }

    // The two PUBREL tests deliberately do not use expiredPublish() and do not backdate: an interval of 0
    // is already expired whatever the timestamp says, so they never needed to wait in the first place.
    // They were slow only because of the latch, not because of a sleep.
    @Test
    public void test_pubrel_expired() {
        InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED = true;

        final PUBREL pubrel = new PUBREL(1);
        pubrel.setMessageExpiryInterval(0L);
        pubrel.setPublishTimestamp(System.currentTimeMillis());

        assertTrue(writeAndCheckDropped(pubrel, PubrelDroppedEvent.class));
        assertEquals(0L, pubrel.getMessageExpiryInterval().longValue());
    }

    @Test
    public void test_pubrel_dont_expired() {
        InternalConfigurations.EXPIRE_INFLIGHT_PUBRELS_ENABLED = false;

        final PUBREL pubrel = new PUBREL(1);
        pubrel.setMessageExpiryInterval(0L);
        pubrel.setPublishTimestamp(System.currentTimeMillis());

        assertFalse(writeAndCheckDropped(pubrel, PubrelDroppedEvent.class));
        assertEquals(0L, pubrel.getMessageExpiryInterval().longValue());
    }
}
