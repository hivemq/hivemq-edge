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
package com.hivemq.mqtt.handler.subscribe;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bridge.MessageForwarder;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.RestrictionsConfigurationService;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extensions.packets.general.ModifiableDefaultPermissionsImpl;
import com.hivemq.extensions.services.builder.TopicPermissionBuilderImpl;
import com.hivemq.logging.EventLog;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnectorImpl;
import com.hivemq.mqtt.handler.subscribe.retained.RetainedMessagesSender;
import com.hivemq.mqtt.message.ProtocolVersion;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.reason.Mqtt5SubAckReasonCode;
import com.hivemq.mqtt.message.suback.SUBACK;
import com.hivemq.mqtt.message.subscribe.SUBSCRIBE;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.persistence.clientsession.ClientSessionSubscriptionPersistence;
import com.hivemq.persistence.clientsession.SharedSubscriptionService;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import com.hivemq.sampling.SamplingService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.concurrent.ImmediateEventExecutor;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestConfigurationBootstrap;

@SuppressWarnings({"ALL", "MockNotUsedInProduction"})
public class IncomingSubscribeServiceTest {

    @Mock
    private ClientSessionSubscriptionPersistence clientSessionSubscriptionPersistence;

    @Mock
    private RetainedMessagePersistence retainedMessagePersistence;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private ChannelFuture channelFuture;

    @Mock
    private EventLog eventLog;

    @Mock
    private RetainedMessagesSender retainedMessagesSender;

    @Mock
    private SharedSubscriptionService sharedSubscriptionService;

    @Mock
    private MqttConfigurationService mqttConfigurationService;

    @Mock
    private RestrictionsConfigurationService restrictionsConfigurationService;

    @Mock
    private MessageForwarder messageForwarder;

    @Mock
    private SamplingService samplingService;

    private EmbeddedChannel channel;
    private IncomingSubscribeService incomingSubscribeService;

    private @NotNull ClientConnection clientConnection;

    @BeforeEach
    public void setUp() throws Exception {

        MockitoAnnotations.initMocks(this);
        incomingSubscribeService = new IncomingSubscribeService(
                clientSessionSubscriptionPersistence,
                retainedMessagePersistence,
                sharedSubscriptionService,
                retainedMessagesSender,
                mqttConfigurationService,
                restrictionsConfigurationService,
                new MqttServerDisconnectorImpl(eventLog),
                () -> messageForwarder,
                () -> samplingService);

        channel = new EmbeddedChannel();
        clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setClientId("client");

        when(clientSessionSubscriptionPersistence.addSubscription(anyString(), any(Topic.class)))
                .thenReturn(Futures.immediateFuture(null));
        when(clientSessionSubscriptionPersistence.addSubscriptions(anyString(), any(ImmutableSet.class)))
                .thenReturn(Futures.<Void>immediateFuture(null));
        when(ctx.channel()).thenReturn(channel);
        when(ctx.writeAndFlush(any())).thenReturn(channelFuture);
        when(ctx.executor()).thenReturn(ImmediateEventExecutor.INSTANCE);
        when(restrictionsConfigurationService.maxTopicLength()).thenReturn(65535);
    }

    @Test
    public void test_subscribe_single_and_acknowledge() throws Exception {

        final Topic topic = new Topic("test", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(1, response.getReasonCodes().size());
        assertEquals(
                (byte) QoS.AT_LEAST_ONCE.getQosNumber(),
                response.getReasonCodes().get(0).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), same(topic));
    }

    @Test
    public void test_subscribe_three_and_acknowledge() throws Exception {

        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(
                (byte) QoS.AT_LEAST_ONCE.getQosNumber(),
                response.getReasonCodes().get(0).getCode());
        assertEquals(
                (byte) QoS.AT_MOST_ONCE.getQosNumber(),
                response.getReasonCodes().get(1).getCode());
        assertEquals(
                (byte) QoS.EXACTLY_ONCE.getQosNumber(),
                response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));
    }

    @Test
    public void test_subscribe_batched_and_acknowledge() throws Exception {

        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);
        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(
                (byte) QoS.AT_LEAST_ONCE.getQosNumber(),
                response.getReasonCodes().get(0).getCode());
        assertEquals(
                (byte) QoS.AT_MOST_ONCE.getQosNumber(),
                response.getReasonCodes().get(1).getCode());
        assertEquals(
                (byte) QoS.EXACTLY_ONCE.getQosNumber(),
                response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));
    }

    @Test
    public void test_subscribe_batched_to_non_batched_with_same_filter_and_acknowledge() throws Exception {

        final Topic topic1 = new Topic("test", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("test", QoS.AT_LEAST_ONCE);
        final Topic topic3 = new Topic("test", QoS.AT_MOST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(
                (byte) QoS.EXACTLY_ONCE.getQosNumber(),
                response.getReasonCodes().get(0).getCode());
        assertEquals(
                (byte) QoS.AT_LEAST_ONCE.getQosNumber(),
                response.getReasonCodes().get(1).getCode());
        assertEquals(
                (byte) QoS.AT_MOST_ONCE.getQosNumber(),
                response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), same(topic3));
    }

    @Test
    public void test_subscribe_batched_to_batched_with_same_filter_and_acknowledge() throws Exception {

        final Topic topic1 = new Topic("test1", QoS.EXACTLY_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_LEAST_ONCE);
        final Topic topic3 = new Topic("test2", QoS.AT_MOST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        final ImmutableSet<Topic> persistedTopics = ImmutableSet.of(topic1, topic3);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final Queue<Object> objects = channel.outboundMessages();

        assertEquals(1, objects.size());

        final SUBACK response = (SUBACK) objects.element();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(
                (byte) QoS.EXACTLY_ONCE.getQosNumber(),
                response.getReasonCodes().get(0).getCode());
        assertEquals(
                (byte) QoS.AT_LEAST_ONCE.getQosNumber(),
                response.getReasonCodes().get(1).getCode());
        assertEquals(
                (byte) QoS.AT_MOST_ONCE.getQosNumber(),
                response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), eq(persistedTopics));
    }

    @Test
    public void test_subscription_metric() throws Exception {

        final SUBSCRIBE subscribe = new SUBSCRIBE(
                ImmutableList.copyOf(
                        Lists.newArrayList(new Topic("t1", QoS.AT_LEAST_ONCE), new Topic("t2", QoS.AT_LEAST_ONCE))),
                1);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);
    }

    @Test
    public void test_send_invalid_subscribe_message() throws Exception {

        final SUBSCRIBE subscribe = new SUBSCRIBE(
                ImmutableList.copyOf(Lists.newArrayList(new Topic("not/#/allowed", QoS.AT_LEAST_ONCE))), 1);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        // We need to make sure we got disconnected
        assertFalse(channel.isActive());
        verify(eventLog).clientWasDisconnected(any(Channel.class), anyString());
    }

    /**
     * EDG-882 QA round 2: shared subscribers of one group take turns, so a client that joins a live
     * bridge forwarder's group receives the bridge's messages instead of the bridge — and they are
     * never forwarded to the remote broker. The group name embeds a digest a client can compute from
     * the bridge's own configuration, so it is reachable rather than theoretical.
     */
    @Test
    public void test_subscribe_colliding_with_a_live_forwarder_queue_is_refused() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        final String shareName = "$FORWARDER::bridge-DNkmbgZ6ni59NniT9XDvig==";
        when(messageForwarder.isForwarderQueue(shareName + "/plant/temp")).thenReturn(true);
        final SUBSCRIBE subscribe = new SUBSCRIBE(
                ImmutableList.copyOf(
                        Lists.newArrayList(new Topic("$share/" + shareName + "/plant/temp", QoS.AT_LEAST_ONCE))),
                1);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_subscribe_colliding_with_a_live_sampler_queue_is_refused() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        when(samplingService.isSamplerQueue("$SAMPLER::plant/line1/plant/line1"))
                .thenReturn(true);
        final SUBSCRIBE subscribe = new SUBSCRIBE(
                ImmutableList.copyOf(
                        Lists.newArrayList(new Topic("$share/$SAMPLER::plant/line1/plant/line1", QoS.AT_LEAST_ONCE))),
                1);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    /**
     * The control, and the reason this is a collision check rather than a reserved-namespace rule: a
     * share name is the client's to choose, and a group that merely looks internal but collides with
     * nothing Edge is using belongs to the client (EDG-882 F-05, pinned end to end by
     * {@code SamplerNamedSharedSubscriptionIT}).
     */
    @Test
    public void test_subscribe_to_an_unused_group_that_looks_internal_is_accepted() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        final SUBSCRIBE subscribe = new SUBSCRIBE(
                ImmutableList.copyOf(Lists.newArrayList(
                        new Topic("$share/group/plant/temp", QoS.AT_LEAST_ONCE),
                        new Topic("$share/$SAMPLER::customer/alerts", QoS.AT_LEAST_ONCE),
                        new Topic("$share/$FORWARDER::nothing-here/plant/temp", QoS.AT_LEAST_ONCE))),
                1);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        // Asserted on the subscriptions, not only on the channel staying up: a client that is neither
        // disconnected nor subscribed would look identical from the channel alone, so this test passed
        // with the collision check deleted and with it never reached (EDG-882 review v02, R2-21).
        assertTrue(channel.isActive());
        final ArgumentCaptor<ImmutableSet<Topic>> accepted = ArgumentCaptor.captor();
        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), accepted.capture());
        assertEquals(
                Set.of(
                        "$share/group/plant/temp",
                        "$share/$SAMPLER::customer/alerts",
                        "$share/$FORWARDER::nothing-here/plant/temp"),
                accepted.getValue().stream().map(Topic::getTopic).collect(Collectors.toSet()));
    }

    @Test
    public void single_topic_dont_batch() throws Exception {

        final HashSet<Topic> topics = Sets.newHashSet(Topic.topicFromString("topic1"));
        assertFalse(IncomingSubscribeService.batch(topics));
    }

    @Test
    public void test_batch() throws Exception {

        final HashSet<Topic> topics = Sets.newHashSet(Topic.topicFromString("topic1"), Topic.topicFromString("topic2"));
        assertTrue(IncomingSubscribeService.batch(topics));
    }

    @Test
    public void test_subscribe_wildcard_disabled_mqtt5() {
        when(mqttConfigurationService.wildcardSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_subscribe_wildcard_disabled_mqtt3_1_1() {
        when(mqttConfigurationService.wildcardSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_subscribe_wildcard_disabled_mqtt3_1() {
        when(mqttConfigurationService.wildcardSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
        final Topic topic = new Topic("#", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_shared_subscription_disabled_mqtt5() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(false);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv5);
        final Topic topic = new Topic("$share/group1/topic1", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_shared_subscription_disabled_mqtt3_1_1() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(false);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        final Topic topic = new Topic("$share/group1/topic1", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_shared_subscription_disabled_mqtt3_1() {
        when(mqttConfigurationService.sharedSubscriptionsEnabled()).thenReturn(false);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setProtocolVersion(ProtocolVersion.MQTTv3_1);
        final Topic topic = new Topic("$share/group1/topic1", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());

        verify(clientSessionSubscriptionPersistence, never()).addSubscriptions(any(), any());
    }

    @Test
    public void test_subscribe_single_authorized() throws Exception {

        final Topic topic = new Topic("test", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getConfigurationService())
                .topicFilter("#")
                .type(TopicPermission.PermissionType.ALLOW)
                .build());

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthPermissions(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();

        assertEquals(1, response.getReasonCodes().size());
        assertEquals(
                (byte) QoS.AT_LEAST_ONCE.getQosNumber(),
                response.getReasonCodes().get(0).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscription(eq("client"), same(topic));
    }

    @Test
    public void test_subscribe_single_not_authorized() throws Exception {

        final Topic topic = new Topic("test", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getConfigurationService())
                .topicFilter("#")
                .type(TopicPermission.PermissionType.DENY)
                .build());

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthPermissions(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();

        assertEquals(1, response.getReasonCodes().size());
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(0));

        verify(clientSessionSubscriptionPersistence, never()).addSubscription(eq("client"), same(topic));
    }

    @Test
    public void test_subscribe_multiple_authorized() throws Exception {

        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getConfigurationService())
                .topicFilter("#")
                .type(TopicPermission.PermissionType.ALLOW)
                .build());

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthPermissions(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(
                (byte) QoS.AT_LEAST_ONCE.getQosNumber(),
                response.getReasonCodes().get(0).getCode());
        assertEquals(
                (byte) QoS.AT_MOST_ONCE.getQosNumber(),
                response.getReasonCodes().get(1).getCode());
        assertEquals(
                (byte) QoS.EXACTLY_ONCE.getQosNumber(),
                response.getReasonCodes().get(2).getCode());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), any(ImmutableSet.class));
    }

    @Test
    public void test_subscribe_multiple_all_not_authorized() throws Exception {

        final ArgumentCaptor<ImmutableSet> captor = ArgumentCaptor.forClass(ImmutableSet.class);
        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getConfigurationService())
                .topicFilter("#")
                .type(TopicPermission.PermissionType.DENY)
                .build());

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthPermissions(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();
        assertEquals(3, response.getReasonCodes().size());
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(0));
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(1));
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(2));
        assertEquals(
                "Not authorized to subscribe to topic 'test1' with QoS '1'. "
                        + "Not authorized to subscribe to topic 'test2' with QoS '0'. "
                        + "Not authorized to subscribe to topic 'test3' with QoS '2'. ",
                response.getReasonString());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), captor.capture());
        assertEquals(0, captor.getValue().size());
    }

    @Test
    public void test_subscribe_multiple_some_not_authorized() throws Exception {

        final ArgumentCaptor<ImmutableSet> captor = ArgumentCaptor.forClass(ImmutableSet.class);
        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);
        final Topic topic4 = new Topic("test4", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe =
                new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3, topic4)), 10);

        final ModifiableDefaultPermissionsImpl permissions = new ModifiableDefaultPermissionsImpl();
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getConfigurationService())
                .topicFilter("test1")
                .type(TopicPermission.PermissionType.ALLOW)
                .build());
        permissions.add(new TopicPermissionBuilderImpl(new TestConfigurationBootstrap().getConfigurationService())
                .topicFilter("test4")
                .type(TopicPermission.PermissionType.ALLOW)
                .build());
        permissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.DENY);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthPermissions(permissions);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        final SUBACK response = channel.readOutbound();
        assertEquals(4, response.getReasonCodes().size());
        assertEquals(
                Mqtt5SubAckReasonCode.GRANTED_QOS_1, response.getReasonCodes().get(0));
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(1));
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(2));
        assertEquals(
                Mqtt5SubAckReasonCode.GRANTED_QOS_2, response.getReasonCodes().get(3));
        assertEquals(
                "Not authorized to subscribe to topic 'test2' with QoS '0'. "
                        + "Not authorized to subscribe to topic 'test3' with QoS '2'. ",
                response.getReasonString());

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), captor.capture());
        assertEquals(2, captor.getValue().size());
        final ImmutableList<Topic> immutableList = captor.getValue().asList();
        for (Topic topic : immutableList) {
            assertTrue(topic.getTopic().equals("test1") || topic.getTopic().equals("test4"));
        }
    }

    @Test
    public void test_subscribe_topic_length_exceeded() throws Exception {
        when(restrictionsConfigurationService.maxTopicLength()).thenReturn(5);

        final Topic topic1 = new Topic("123456", QoS.AT_LEAST_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1)), 10);

        incomingSubscribeService.processSubscribe(ctx, subscribe, false);

        assertFalse(channel.isActive());
    }

    @Test
    public void test_process_authorizers_present_no_default() throws Exception {

        final ArgumentCaptor<ImmutableSet> authorizedTopicsCaptor = ArgumentCaptor.forClass(ImmutableSet.class);
        final Topic topic1 = new Topic("test1", QoS.AT_LEAST_ONCE);
        final Topic topic2 = new Topic("test2", QoS.AT_MOST_ONCE);
        final Topic topic3 = new Topic("test3", QoS.EXACTLY_ONCE);

        final SUBSCRIBE subscribe = new SUBSCRIBE(ImmutableList.copyOf(Lists.newArrayList(topic1, topic2, topic3)), 10);

        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setAuthPermissions(null);

        incomingSubscribeService.processSubscribe(
                ctx,
                subscribe,
                new Mqtt5SubAckReasonCode[] {Mqtt5SubAckReasonCode.GRANTED_QOS_1, null, null},
                new String[3],
                true);

        final SUBACK response = channel.readOutbound();

        assertEquals(3, response.getReasonCodes().size());
        assertEquals(
                Mqtt5SubAckReasonCode.GRANTED_QOS_1, response.getReasonCodes().get(0));
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(1));
        assertEquals(
                Mqtt5SubAckReasonCode.NOT_AUTHORIZED, response.getReasonCodes().get(2));

        verify(clientSessionSubscriptionPersistence).addSubscriptions(eq("client"), authorizedTopicsCaptor.capture());
        assertEquals(1, authorizedTopicsCaptor.getValue().size());
    }
}
