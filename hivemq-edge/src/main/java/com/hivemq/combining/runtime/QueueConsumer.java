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
package com.hivemq.combining.runtime;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
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
        singleWriterService.getQueuedMessagesQueue().submit(queueId, bucketIndex -> {
            pollAndForward();
            return null;
        });
    }

    private void pollAndForward() {
        try {
            final ListenableFuture<ImmutableList<PUBLISH>> publishesFuture =
                    clientQueuePersistence.readShared(queueId, READ_LIMIT, PUBLISH_POLL_BATCH_SIZE_BYTES);

            Futures.transform(publishesFuture, publishes -> {
                if (publishes.isEmpty()) {
                    return null;
                }
                //we know it's 1 message as READ_LIMIT = 1
                final PUBLISH publish = publishes.get(0);
                processPublish(publish);
                return null;
            }, MoreExecutors.directExecutor());
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
        log.error("Failed to handle message. Message will be dropped because: {}", message);
        if (log.isDebugEnabled()) {
            log.debug("Original Exception: ", throwable);
        }
    }

    private void removeMessage(final @NotNull String queueId, final @NotNull String uniqueId, final @NotNull QoS qos) {
        if (qos != QoS.AT_MOST_ONCE) {
            //-- 15665 - > QoS 0 causes republishing
            FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, uniqueId));
        }
    }
}
