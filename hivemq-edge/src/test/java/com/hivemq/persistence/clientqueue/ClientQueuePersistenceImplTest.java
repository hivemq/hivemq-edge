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
package com.hivemq.persistence.clientqueue;

import static com.hivemq.configuration.service.MqttConfigurationService.QueuedMessagesStrategy;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.ImmutableIntArray;
import com.hivemq.bootstrap.ClientConnection;
import com.hivemq.bridge.MessageForwarder;
import com.hivemq.bridge.MessageForwarderImpl;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.service.InternalConfigurationService;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.configuration.service.MqttConfigurationService;
import com.hivemq.configuration.service.impl.InternalConfigurationServiceImpl;
import com.hivemq.mqtt.message.MessageWithID;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.PublishPollService;
import com.hivemq.mqtt.topic.SubscriberWithQoS;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientsession.ClientSession;
import com.hivemq.persistence.connection.ConnectionPersistence;
import com.hivemq.persistence.local.ClientSessionLocalPersistence;
import com.hivemq.persistence.local.memory.ClientQueueMemoryLocalPersistence;
import com.hivemq.persistence.local.xodus.bucket.BucketUtils;
import com.hivemq.persistence.payload.PublishPayloadPersistence;
import com.hivemq.sampling.SamplingService;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import util.TestSingleWriterFactory;

@SuppressWarnings({"NullabilityAnnotations", "FutureReturnValueIgnored"})
public class ClientQueuePersistenceImplTest {

    private AutoCloseable closeableMock;

    @Mock
    ClientQueueMemoryLocalPersistence localPersistence;

    @Mock
    PublishPayloadPersistence payloadPersistence;

    @Mock
    MqttConfigurationService mqttConfigurationService;

    @Mock
    ClientSessionLocalPersistence clientSessionLocalPersistence;

    @Mock
    MessageDroppedService messageDroppedService;

    @Mock
    LocalTopicTree topicTree;

    @Mock
    private ConnectionPersistence connectionPersistence;

    @Mock
    private PublishPollService publishPollService;

    @Mock
    private MessageForwarder messageForwarder;

    private final @NotNull ShutdownHooks shutdownHooks = new ShutdownHooks();

    private ClientQueuePersistenceImpl clientQueuePersistence;

    final int bucketSize = 4;

    private SingleWriterService singleWriterService;
    private final @NotNull InternalConfigurationService internalConfigurationService =
            new InternalConfigurationServiceImpl();

    @BeforeEach
    public void setUp() throws Exception {
        closeableMock = MockitoAnnotations.openMocks(this);
        internalConfigurationService.set(InternalConfigurations.PERSISTENCE_BUCKET_COUNT, "" + bucketSize);
        singleWriterService = TestSingleWriterFactory.defaultSingleWriter(internalConfigurationService);
        when(mqttConfigurationService.maxQueuedMessages()).thenReturn(1000L);
        when(mqttConfigurationService.getQueuedMessagesStrategy()).thenReturn(QueuedMessagesStrategy.DISCARD);
        // the bridges have started in every test but the one that says otherwise: before that, forwarder
        // queues are deliberately left alone (EDG-882)
        when(messageForwarder.hasAppliedBridgeConfiguration()).thenReturn(true);
        clientQueuePersistence = new ClientQueuePersistenceImpl(
                localPersistence,
                singleWriterService,
                mqttConfigurationService,
                clientSessionLocalPersistence,
                messageDroppedService,
                topicTree,
                connectionPersistence,
                () -> publishPollService,
                messageForwarder,
                shutdownHooks);
    }

    @AfterEach
    public void tearDown() throws Exception {
        clientQueuePersistence.closeDB();
        singleWriterService.stop();
        closeableMock.close();
    }

    @Test
    @Timeout(5)
    public void test_add() throws ExecutionException, InterruptedException {
        clientQueuePersistence
                .add("client", false, createPublish(1, QoS.AT_LEAST_ONCE, "topic"), false, 1000L, QueuePolicy.DEFAULT)
                .get();
        verify(localPersistence)
                .add(
                        eq("client"),
                        eq(false),
                        any(PUBLISH.class),
                        eq(1000L),
                        eq(QueuedMessagesStrategy.DISCARD),
                        anyBoolean(),
                        eq(false), // applyMaxToQos0: only sampler queues opt in (EDG-885)
                        anyInt());
        verify(messageDroppedService, never()).queueFull("client", "topic", 1);
    }

    /**
     * EDG-885: a sample ring is a ring buffer of the most recent samples, so it alone asks the
     * persistence to apply the count limit to QoS 0 as well. Without it, a sampled topic whose
     * publishers use QoS 0 is bounded only by the node-wide QoS 0 memory budget and can starve every
     * other QoS 0 consumer on the node.
     */
    @Test
    @Timeout(5)
    public void test_add_sample_ring_opts_into_the_qos0_count_limit() throws ExecutionException, InterruptedException {
        clientQueuePersistence
                .add(
                        "$SAMPLER::topic/topic",
                        true,
                        createPublish(1, QoS.AT_MOST_ONCE, "topic"),
                        false,
                        10L,
                        QueuePolicy.SAMPLE_RING)
                .get();
        verify(localPersistence)
                .add(
                        eq("$SAMPLER::topic/topic"),
                        eq(true),
                        any(PUBLISH.class),
                        eq(10L),
                        eq(QueuedMessagesStrategy.DISCARD_OLDEST),
                        anyBoolean(),
                        eq(true),
                        anyInt());
    }

    /**
     * EDG-882 F-05, the regression. The policy comes from the producer, so a queue ID that merely
     * looks like a sampler's gets ordinary treatment: the configured overflow strategy, and QoS 0 left
     * to the node-wide memory budget.
     * <p>
     * The ID here is what a client subscribing to {@code $share/$SAMPLER::customer/alerts} produces —
     * a legal subscription, and the client's own messages. Deciding from the ID meant discarding them
     * under a policy meant for diagnostics, and there was nothing the client could do about it but
     * rename its subscription group.
     */
    @Test
    @Timeout(5)
    public void test_add_client_shared_queue_named_like_a_sampler_is_treated_normally()
            throws ExecutionException, InterruptedException {
        clientQueuePersistence
                .add(
                        "$SAMPLER::customer/alerts",
                        true,
                        createPublish(1, QoS.AT_MOST_ONCE, "alerts"),
                        false,
                        1000L,
                        QueuePolicy.DEFAULT)
                .get();
        verify(localPersistence)
                .add(
                        eq("$SAMPLER::customer/alerts"),
                        eq(true),
                        any(PUBLISH.class),
                        eq(1000L),
                        eq(QueuedMessagesStrategy.DISCARD),
                        anyBoolean(),
                        eq(false),
                        anyInt());
    }

    /** The batch path must read the policy the same way; it had its own copy of the inference. */
    @Test
    @Timeout(5)
    public void test_add_batch_client_shared_queue_named_like_a_sampler_is_treated_normally()
            throws ExecutionException, InterruptedException {
        clientQueuePersistence
                .add(
                        "$SAMPLER::customer/alerts",
                        true,
                        ImmutableList.of(createPublish(1, QoS.AT_MOST_ONCE, "alerts")),
                        false,
                        1000L,
                        QueuePolicy.DEFAULT)
                .get();
        verify(localPersistence)
                .add(
                        eq("$SAMPLER::customer/alerts"),
                        eq(true),
                        anyList(),
                        eq(1000L),
                        eq(QueuedMessagesStrategy.DISCARD),
                        anyBoolean(),
                        eq(false),
                        anyInt());
    }

    /** And the batch path must still honour a sample ring when the producer asks for one. */
    @Test
    @Timeout(5)
    public void test_add_batch_sample_ring_opts_into_the_qos0_count_limit()
            throws ExecutionException, InterruptedException {
        clientQueuePersistence
                .add(
                        "$SAMPLER::topic/topic",
                        true,
                        ImmutableList.of(createPublish(1, QoS.AT_MOST_ONCE, "topic")),
                        false,
                        10L,
                        QueuePolicy.SAMPLE_RING)
                .get();
        verify(localPersistence)
                .add(
                        eq("$SAMPLER::topic/topic"),
                        eq(true),
                        anyList(),
                        eq(10L),
                        eq(QueuedMessagesStrategy.DISCARD_OLDEST),
                        anyBoolean(),
                        eq(true),
                        anyInt());
    }

    @Test
    @Timeout(5)
    public void test_add_shared() throws ExecutionException, InterruptedException {
        clientQueuePersistence
                .add(
                        "name/topic",
                        true,
                        createPublish(1, QoS.AT_LEAST_ONCE, "topic"),
                        false,
                        1000L,
                        QueuePolicy.DEFAULT)
                .get();
        verify(localPersistence)
                .add(
                        eq("name/topic"),
                        eq(true),
                        any(PUBLISH.class),
                        eq(1000L),
                        eq(QueuedMessagesStrategy.DISCARD),
                        anyBoolean(),
                        eq(false), // applyMaxToQos0: only sampler queues opt in (EDG-885)
                        anyInt());
    }

    @Test
    @Timeout(5)
    public void test_publish_avaliable() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessagesSent(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(0));

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);
        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();

        verify(publishPollService, timeout(2000)).pollNewMessages("client", channel);
    }

    @Test
    @Timeout(5)
    public void test_publish_avaliable_channel_inactive() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessagesSent(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(0));

        channel.close();

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);
        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();
        verify(publishPollService, never()).pollNewMessages("client", channel);
    }

    @Test
    @Timeout(5)
    public void test_publish_avaliable_inflight_messages_not_sent() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(0));

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);

        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();
        verify(publishPollService, never()).pollNewMessages("client", channel);
    }

    @Test
    @Timeout(5)
    public void test_publish_avaliable_inflight_messages_sending() {

        final EmbeddedChannel channel = new EmbeddedChannel();
        final ClientConnection clientConnection = new ClientConnection(channel, null);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).set(clientConnection);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessagesSent(true);
        channel.attr(ClientConnection.CHANNEL_ATTRIBUTE_NAME).get().setInFlightMessageCount(new AtomicInteger(10));

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(clientConnection);

        clientQueuePersistence.publishAvailable("client");
        channel.runPendingTasks();
        verify(publishPollService, never()).pollNewMessages("client", channel);
    }

    @Test
    @Timeout(5)
    public void test_publish_avaliable_channel_null() {

        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(true, 1000L));
        when(connectionPersistence.get("client")).thenReturn(null);
        clientQueuePersistence.publishAvailable("client");
        verify(publishPollService, never()).pollNewMessages(eq("client"), any(Channel.class));
    }

    @Test
    @Timeout(5)
    public void test_publish_avaliable_not_connected() {
        when(clientSessionLocalPersistence.getSession("client")).thenReturn(new ClientSession(false, 1000L));
        clientQueuePersistence.publishAvailable("client");
        verify(publishPollService, never()).pollNewMessages(eq("client"), any(Channel.class));
    }

    @Test
    @Timeout(5)
    public void test_read_new() throws ExecutionException, InterruptedException {

        when(localPersistence.readNew(anyString(), anyBoolean(), any(ImmutableIntArray.class), anyLong(), anyInt()))
                .thenReturn(ImmutableList.of(
                        createPublish(1, QoS.AT_MOST_ONCE, "topic"), createPublish(2, QoS.AT_LEAST_ONCE, "topic")));

        final ImmutableList<PUBLISH> publishes = clientQueuePersistence
                .readNew("client", false, ImmutableIntArray.of(1, 2), 1000)
                .get();

        assertEquals(2, publishes.size());
    }

    @Test
    @Timeout(5)
    public void test_clear() throws ExecutionException, InterruptedException {

        clientQueuePersistence.clear("client", false).get();
        verify(localPersistence).clear("client", false, BucketUtils.getBucket("client", bucketSize));
    }

    @Test
    @Timeout(5)
    public void test_read_inflight() throws ExecutionException, InterruptedException {
        when(localPersistence.readInflight(anyString(), anyBoolean(), anyInt(), anyLong(), anyInt()))
                .thenReturn(ImmutableList.of(createPublish(1, QoS.AT_LEAST_ONCE, "topic")));
        final ImmutableList<MessageWithID> messages =
                clientQueuePersistence.readInflight("client", 10, 11).get();
        assertEquals(1, messages.size());
        verify(localPersistence).readInflight(eq("client"), eq(false), eq(11), eq(10L), anyInt());
    }

    @Test
    @Timeout(5)
    public void test_clean_up() throws ExecutionException, InterruptedException {

        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of("group/topic"));
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());

        clientQueuePersistence.cleanUp(0).get();

        verify(topicTree).getSharedSubscriber(anyString(), anyString());
    }

    @Test
    @Timeout(5)
    public void test_clean_up_active_forwarder_queue_with_slash_in_hash_not_cleared()
            throws ExecutionException, InterruptedException {
        // EDG-882: the Base64 subscription hash in a forwarder queue ID may contain '/', which made
        // the clean-up misparse the queue ID and wipe a live bridge queue
        final String queueId = MessageForwarderImpl.FORWARDER_PREFIX
                + "bridge-Bt80p78iNo/w7W1W7bGwcg==/miele/v1/production/sapdm/dev/+/+/from-plc-to-dm";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(queueId));
        when(messageForwarder.isForwarderQueue(queueId)).thenReturn(true);

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence, never()).clear(anyString(), anyBoolean(), anyInt());
        verify(topicTree, never()).getSharedSubscriber(anyString(), anyString());
    }

    /**
     * EDG-882: before any bridge configuration has been applied, a forwarder queue nobody owns is a
     * bridge that has not started yet, not an abandoned queue. The clean-up service is scheduled
     * during persistence bootstrap and the bridge subsystem is built after it, so a sweep in that
     * window would delete the queues of every bridge on the node while they wait to be started.
     */
    @Test
    @Timeout(5)
    public void test_clean_up_before_any_bridge_configuration_leaves_forwarder_queues_alone()
            throws ExecutionException, InterruptedException {
        final String queueId = MessageForwarderImpl.FORWARDER_PREFIX + "bridge-Bt80p78iNo/w7W1W7bGwcg==/topic";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(queueId));
        when(messageForwarder.hasAppliedBridgeConfiguration()).thenReturn(false);

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence, never()).clear(anyString(), anyBoolean(), anyInt());
        verify(topicTree, never()).getSharedSubscriber(anyString(), anyString());
    }

    @Test
    @Timeout(5)
    public void test_clean_up_orphaned_forwarder_queue_cleared() throws ExecutionException, InterruptedException {
        final String queueId = MessageForwarderImpl.FORWARDER_PREFIX
                + "bridge-Bt80p78iNo/w7W1W7bGwcg==/miele/v1/production/sapdm/dev/+/+/from-plc-to-dm";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(queueId));
        when(messageForwarder.isForwarderQueue(queueId)).thenReturn(false);
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence).clear(queueId, true, 0);
    }

    @Test
    @Timeout(5)
    public void test_clean_up_active_sampler_queue_for_topic_with_slash_not_cleared()
            throws ExecutionException, InterruptedException {
        // the sampler share name is $SAMPLER::<topic>, so for a hierarchical topic the share-name
        // boundary is not the first '/': splitting there resolves an owner that never existed
        final String topic = "a/b/c";
        final String queueId = SamplingService.createQueueId(topic);
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(queueId));
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());
        when(topicTree.getSharedSubscriber(SamplingService.SAMPLER_PREFIX + topic, topic))
                .thenReturn(ImmutableSet.of(
                        new SubscriberWithQoS(SamplingService.SAMPLER_PREFIX + topic, 1, (byte) 0, null)));

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence, never()).clear(anyString(), anyBoolean(), anyInt());
    }

    @Test
    @Timeout(5)
    public void test_clean_up_orphaned_sampler_queue_cleared() throws ExecutionException, InterruptedException {
        final String queueId = SamplingService.createQueueId("a/b/c");
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(queueId));
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence).clear(queueId, true, 0);
    }

    @Test
    @Timeout(5)
    public void test_clean_up_client_shared_subscription_colliding_with_sampler_prefix_not_cleared()
            throws ExecutionException, InterruptedException {
        // symmetric to the $FORWARDER:: case: a client may legally use "$SAMPLER::grp" as a share
        // group, and the generic split resolves it correctly — it must not be treated as a sampler
        final String queueId = SamplingService.SAMPLER_PREFIX + "grp/topic";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(queueId));
        when(topicTree.getSharedSubscriber(SamplingService.SAMPLER_PREFIX + "grp", "topic"))
                .thenReturn(ImmutableSet.of(new SubscriberWithQoS("client", 1, (byte) 0, null)));

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence, never()).clear(anyString(), anyBoolean(), anyInt());
    }

    @Test
    @Timeout(5)
    public void test_clean_up_client_shared_subscription_colliding_with_forwarder_prefix_not_cleared()
            throws ExecutionException, InterruptedException {
        // a client may legally subscribe to $share/$FORWARDER::group/topic: no forwarder owns that
        // queue, so ownership must still be resolved through the topic tree rather than assumed absent
        final String queueId = MessageForwarderImpl.FORWARDER_PREFIX + "group/topic";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(queueId));
        when(messageForwarder.isForwarderQueue(queueId)).thenReturn(false);
        when(topicTree.getSharedSubscriber(MessageForwarderImpl.FORWARDER_PREFIX + "group", "topic"))
                .thenReturn(ImmutableSet.of(new SubscriberWithQoS("client", 1, (byte) 0, null)));

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence, never()).clear(anyString(), anyBoolean(), anyInt());
    }

    /**
     * EDG-882 QA round 1: the other end of the start-up gate. The bridge shutdown hook has priority
     * HIGH and un-registers every forwarder, while this job stays scheduled until the persistence hook
     * runs — so between them every live bridge queue reads as unowned, and a sweep landing there clears
     * the backlog the shutdown deliberately did not clear.
     */
    @Test
    @Timeout(5)
    public void test_clean_up_while_shutting_down_reclaims_nothing() throws ExecutionException, InterruptedException {
        final String forwarderQueueId = MessageForwarderImpl.FORWARDER_PREFIX + "bridge-Bt80p78iNo/w7W1W7bGwcg==/topic";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(forwarderQueueId, "group/topic"));
        when(messageForwarder.isForwarderQueue(forwarderQueueId)).thenReturn(false);
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());
        shutdownHooks.runShutdownHooks();

        clientQueuePersistence.cleanUp(0).get();

        // expiry still ran; nothing was reclaimed, and ownership was not even asked about
        verify(localPersistence).cleanUp(0);
        verify(localPersistence, never()).clear(anyString(), anyBoolean(), anyInt());
        verify(topicTree, never()).getSharedSubscriber(anyString(), anyString());
    }

    /**
     * The shutdown starting <em>inside</em> the sweep, which is the case the loop-top guard cannot cover
     * (EDG-882 QA round 4, review v02 R2-21).
     * <p>
     * The test above trips the guard at the top of the iteration: the node is already shutting down when
     * the sweep begins. The window that costs messages is narrower — the bridge shutdown hook
     * un-registers every forwarder <em>while</em> {@code isOrphaned} is running, so a queue that was live
     * when the iteration started reads as orphaned by the time the clear is reached. That is why the
     * guard is re-read immediately before the destructive call, and this is what pins it: the shutdown is
     * triggered from inside the ownership lookup itself.
     */
    @Test
    @Timeout(5)
    public void test_a_shutdown_beginning_during_the_ownership_lookup_still_reclaims_nothing()
            throws ExecutionException, InterruptedException {
        final String forwarderQueueId = MessageForwarderImpl.FORWARDER_PREFIX + "bridge-Bt80p78iNo/w7W1W7bGwcg==/topic";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(forwarderQueueId));
        when(messageForwarder.isForwarderQueue(forwarderQueueId)).thenReturn(false);
        // the hook fires while ownership is being resolved, exactly as the bridge hook does
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenAnswer(invocation -> {
            shutdownHooks.runShutdownHooks();
            return ImmutableSet.of();
        });

        clientQueuePersistence.cleanUp(0).get();

        verify(topicTree, atLeastOnce()).getSharedSubscriber(anyString(), anyString());
        verify(localPersistence, never()).clear(anyString(), anyBoolean(), anyInt());
    }

    /**
     * The positive control for the test above: the skip must be tied to the shutdown, not to the
     * queue — otherwise a passing "nothing was cleared" would only prove that nothing is ever cleared.
     */
    @Test
    @Timeout(5)
    public void test_clean_up_while_running_still_reclaims_orphans() throws ExecutionException, InterruptedException {
        final String forwarderQueueId = MessageForwarderImpl.FORWARDER_PREFIX + "bridge-Bt80p78iNo/w7W1W7bGwcg==/topic";
        when(localPersistence.cleanUp(eq(0))).thenReturn(ImmutableSet.of(forwarderQueueId));
        when(messageForwarder.isForwarderQueue(forwarderQueueId)).thenReturn(false);
        when(topicTree.getSharedSubscriber(anyString(), anyString())).thenReturn(ImmutableSet.of());

        clientQueuePersistence.cleanUp(0).get();

        verify(localPersistence).clear(forwarderQueueId, true, 0);
    }

    @Test
    @Timeout(5)
    public void test_shared_publish_available() {
        clientQueuePersistence.sharedPublishAvailable("group/topic");
        verify(publishPollService).pollSharedPublishes("group/topic");
    }

    @Test
    @Timeout(5)
    public void test_shared_publish_available_for_a_registered_forwarder_queue_notifies_the_forwarder() {
        final String queueId = MessageForwarderImpl.FORWARDER_PREFIX + "bridge-abc/topic";
        when(messageForwarder.isForwarderQueue(queueId)).thenReturn(true);

        clientQueuePersistence.sharedPublishAvailable(queueId);

        verify(messageForwarder).messageAvailable(queueId);
        verify(publishPollService, never()).pollSharedPublishes(anyString());
    }

    /**
     * EDG-882 QA round 2: a client may legally choose "$FORWARDER::anything" as its share group. The
     * notification used to be handed to the message forwarder purely because of how the ID was spelled
     * — so the client was never told to poll, and the ID stayed in the forwarder's notEmptyQueues set
     * for the life of the node, once for every distinct share name the client cared to invent.
     */
    @Test
    @Timeout(5)
    public void test_shared_publish_available_for_a_client_queue_named_like_a_forwarder_polls_the_client() {
        final String queueId = MessageForwarderImpl.FORWARDER_PREFIX + "anything/topic";
        when(messageForwarder.isForwarderQueue(queueId)).thenReturn(false);

        clientQueuePersistence.sharedPublishAvailable(queueId);

        verify(publishPollService).pollSharedPublishes(queueId);
        verify(messageForwarder, never()).messageAvailable(anyString());
    }

    @Test
    @Timeout(5)
    public void test_remove_all_qos0() throws ExecutionException, InterruptedException {
        clientQueuePersistence.removeAllQos0Messages("client", false).get();
        verify(localPersistence).removeAllQos0Messages(eq("client"), eq(false), anyInt());
    }

    @Test
    @Timeout(5)
    public void test_batched_add_no_new_message() throws ExecutionException, InterruptedException {
        when(localPersistence.size(eq("client"), anyBoolean(), anyInt())).thenReturn(1);
        final ImmutableList<PUBLISH> publishes = ImmutableList.of(
                createPublish(1, QoS.AT_LEAST_ONCE, "topic1"), createPublish(2, QoS.AT_LEAST_ONCE, "topic2"));
        clientQueuePersistence
                .add("client", false, publishes, false, 1000L, QueuePolicy.DEFAULT)
                .get();
        verify(localPersistence)
                .add(
                        eq("client"),
                        eq(false),
                        eq(publishes),
                        eq(1000L),
                        eq(QueuedMessagesStrategy.DISCARD),
                        anyBoolean(),
                        eq(false), // applyMaxToQos0: only sampler queues opt in (EDG-885)
                        anyInt());
        verify(clientSessionLocalPersistence, never())
                .getSession("client"); // Get session because new publishes are available
        verify(messageDroppedService, never()).queueFull("client", "topic", 1);
    }

    @Test
    @Timeout(5)
    public void test_batched_add_new_message() throws ExecutionException, InterruptedException {
        when(localPersistence.size(eq("client"), anyBoolean(), anyInt())).thenReturn(0);
        final ImmutableList<PUBLISH> publishes = ImmutableList.of(
                createPublish(1, QoS.AT_LEAST_ONCE, "topic1"), createPublish(2, QoS.AT_LEAST_ONCE, "topic2"));
        clientQueuePersistence
                .add("client", false, publishes, false, 1000L, QueuePolicy.DEFAULT)
                .get();
        verify(localPersistence)
                .add(
                        eq("client"),
                        eq(false),
                        eq(publishes),
                        eq(1000L),
                        eq(QueuedMessagesStrategy.DISCARD),
                        anyBoolean(),
                        eq(false), // applyMaxToQos0: only sampler queues opt in (EDG-885)
                        anyInt());
        verify(clientSessionLocalPersistence).getSession("client"); // Get session because new publishes are available
        verify(messageDroppedService, never()).queueFull("client", "topic", 1);
    }

    private PUBLISH createPublish(final int packetId, final QoS qos, final String topic) {
        return new PUBLISHFactory.Mqtt5Builder()
                .withPacketIdentifier(packetId)
                .withQoS(qos)
                .withOnwardQos(qos)
                .withPublishId(1L)
                .withPayload("message".getBytes(UTF_8))
                .withTopic(topic)
                .withHivemqId("hivemqId")
                .withPersistence(payloadPersistence)
                .build();
    }
}
