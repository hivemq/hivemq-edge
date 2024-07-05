package com.hivemq.protocols.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.common.shutdown.ShutdownHooks;
import com.hivemq.configuration.HivemqId;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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
    private final @NotNull SingleWriterService singleWriterService;
    private final EventService eventService;
    private final QueuePollingTaskFactory queuePollingTaskFactory;

    @Inject
    public ProtocolAdapterWritingService(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ShutdownHooks shutdownHooks,
            final @NotNull ExecutorService executorService,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull HivemqId hivemqId,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull EventService eventService,
            final @NotNull QueuePollingTaskFactory queuePollingTaskFactory) {
        this.objectMapper = objectMapper;
        this.executorService = executorService;
        this.clientQueuePersistence = clientQueuePersistence;
        this.localTopicTree = localTopicTree;
        this.hivemqId = hivemqId;
        this.singleWriterService = singleWriterService;
        this.eventService = eventService;
        this.queuePollingTaskFactory = queuePollingTaskFactory;
    }

    public void startWriting(final @NotNull WritingProtocolAdapter<?, ?> writingProtocolAdapter) {
        writingProtocolAdapter.getWriteContexts().forEach(writeContext -> {
            final String queueId = createSubscription(writeContext);
            final QueuePollingTask queuePollingTask =
                    queuePollingTaskFactory.create(writingProtocolAdapter, queueId, writeContext);
            queuePollingTask.run();
        });
    }

    public @NotNull String createSubscription(final WriteContext writeContext) {
        final String forwarderId = "adapter-writer";
        final String clientId = FORWARDER_PREFIX + forwarderId + "#" + hivemqId.get();
        final String shareName = FORWARDER_PREFIX + forwarderId;
        final String topic = writeContext.getSourceMqttTopic();
        localTopicTree.addTopic(clientId,
                new Topic(topic, QoS.AT_LEAST_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                shareName);
        final String queueId = createQueueId(forwarderId, topic);
        return queueId;
    }

    private static @NotNull String createQueueId(final @NotNull String forwarderId, final @NotNull String topic) {
        return FORWARDER_PREFIX + forwarderId + "/" + topic;
    }


}
