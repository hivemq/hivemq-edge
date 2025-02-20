package com.hivemq.combining.runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.util.FutureUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.hivemq.configuration.service.InternalConfigurations.PUBLISH_POLL_BATCH_SIZE_BYTES;

public abstract class QueueConsumer {

    private static final int READ_LIMIT = 1;

    private static final @NotNull Logger log = LoggerFactory.getLogger(QueueConsumer.class);

    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull String queueId;
    private final @NotNull ClientQueuePersistence.PublishAvailableCallback callback;

    public QueueConsumer(
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull String queueId,
            final @NotNull SingleWriterService singleWriterService) {
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.queueId = queueId;
        this.callback = queueId1 -> {
            if (!queueId1.equals(queueId)) {
                return;
            }
            submitPoll();
        };
    }

    void start() {
        clientQueuePersistence.addPublishAvailableCallback(callback, queueId);
        submitPoll();
    }

    void close() {
        clientQueuePersistence.removePublishAvailableCallback(queueId);
    }

    void submitPoll() {
        singleWriterService.callbackExecutor(queueId).execute(this::pollAndForward);
    }

    private void pollAndForward() {
        try {
            final ListenableFuture<ImmutableList<PUBLISH>> publishesFuture =
                    clientQueuePersistence.readShared(queueId, READ_LIMIT, PUBLISH_POLL_BATCH_SIZE_BYTES);

            Futures.addCallback(publishesFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(final @NotNull ImmutableList<PUBLISH> publishes) {
                    if (publishes.isEmpty()) {
                        return;
                    }
                    //we know it's 1 message as READ_LIMIT = 1
                    final PUBLISH publish = publishes.get(0);
                    processPublish(publish);
                }

                @Override
                public void onFailure(final @NotNull Throwable t) {
                    handleExceptionDuringPolling(t);
                }
            }, singleWriterService.callbackExecutor(queueId));


        } catch (final Throwable t) {
            // the writer shouldn't throw an exception, but better safe than sorry as we might to miss rescheduling the task otherwise.
            handleExceptionDuringPolling(t);
        }

    }

    private void processPublish(final @NotNull PUBLISH publish) {
        try {
            process(publish);
            removeMessage(queueId, publish.getUniqueId(), publish.getQoS());
            submitPoll();
        } catch (final Exception e) {
            handleExceptionDuringHandling(e, e.getMessage());
            removeMessage(queueId, publish.getUniqueId(), publish.getQoS());
            submitPoll();
        }
    }

    public abstract void process(final @NotNull PUBLISH publish);

    private void handleExceptionDuringPolling(final @NotNull Throwable throwable) {
        log.error("Failed to poll message because: {}", throwable.getMessage());
        if (log.isDebugEnabled()) {
            log.debug("Original Exception: ", throwable);
        }
        submitPoll();
    }

    private void handleExceptionDuringHandling(final @NotNull Throwable throwable, final @NotNull String message) {
        log.error("Failed to handle message. Message will be dropped because: {}",  message);
        if (log.isDebugEnabled()) {
            log.debug("Original Exception: ", throwable);
        }
    }

    private void removeMessage(final @NotNull String queueId, final @NotNull String uniqueId, final @NotNull QoS qos) {
        if (qos != QoS.AT_MOST_ONCE) {
            singleWriterService.callbackExecutor(queueId).execute(() -> {
                //-- 15665 - > QoS 0 causes republishing
                FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, uniqueId));
            });
        }
    }


}
