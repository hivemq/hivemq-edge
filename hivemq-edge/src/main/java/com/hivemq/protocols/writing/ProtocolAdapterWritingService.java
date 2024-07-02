package com.hivemq.protocols.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;

@Singleton
public class ProtocolAdapterWritingService {



    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ExecutorService executorService;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull HivemqId hivemqId;
    public static final String FORWARDER_PREFIX = "adapter-forwarder#";
    //TODO
    final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);

    @Inject
    public ProtocolAdapterWritingService(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ExecutorService executorService,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull HivemqId hivemqId) {
        this.objectMapper = objectMapper;
        this.executorService = executorService;
        this.clientQueuePersistence = clientQueuePersistence;
        this.localTopicTree = localTopicTree;
        this.hivemqId = hivemqId;
    }

    public void startWriting(final @NotNull WritingProtocolAdapter<?> writingProtocolAdapter) {
        final String queueId = createSubscription();
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            pollForQueue(queueId, new WriteTask(writingProtocolAdapter, objectMapper));
        },1,1, TimeUnit.SECONDS);
    }

    public String createSubscription(){
        final String forwarderId = "adapter-writer";
        final String clientId = FORWARDER_PREFIX + forwarderId + "#" + hivemqId.get();
        final String shareName = FORWARDER_PREFIX + forwarderId;
        final String topic = "test";
        localTopicTree.addTopic(clientId,
                new Topic(topic, QoS.AT_LEAST_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                shareName);
        final String queueId = createQueueId(forwarderId, topic);
        return queueId;
    }



    @NotNull
    private ListenableFuture<Boolean> pollForQueue(
            final @NotNull String queueId, final @NotNull WriteTask writeTask) {
        final ListenableFuture<ImmutableList<PUBLISH>> pollFuture = clientQueuePersistence
                .readShared(queueId, 1, PUBLISH_POLL_BATCH_SIZE_BYTES);
        return Futures.transformAsync(pollFuture, publishes -> {
            if (publishes == null) {
                return Futures.immediateFuture(false);
            }
            for (final PUBLISH publish : publishes) {
                writeTask.onMessage(publish.getPayload());
            }
            return Futures.immediateFuture(!publishes.isEmpty());
        }, executorService);
    }

    private static @NotNull String createQueueId(final @NotNull String forwarderId, final @NotNull String topic) {
        return FORWARDER_PREFIX + forwarderId + "/" + topic;
    }


}
