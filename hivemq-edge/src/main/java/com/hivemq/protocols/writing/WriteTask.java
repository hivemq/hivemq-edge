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
package com.hivemq.protocols.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.WriteContext;
import com.hivemq.adapter.sdk.api.writing.WriteInput;
import com.hivemq.adapter.sdk.api.writing.WritePayload;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class WriteTask {

    private static final Logger log = LoggerFactory.getLogger(WriteTask.class);
    private final @NotNull WritingProtocolAdapter protocolAdapter;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull ObjectMapper objectMapper;

    WriteTask(
            final @NotNull WritingProtocolAdapter protocolAdapter,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull ObjectMapper objectMapper) {
        this.protocolAdapter = protocolAdapter;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.objectMapper = objectMapper;
    }

    public @NotNull CompletableFuture<Boolean> onMessage(
            final @NotNull PUBLISH publish, final @NotNull String queueId, final @NotNull WriteContext writeContext) {
        try {
            final Class<? extends WritePayload> payloadClass = protocolAdapter.getPayloadClass();
            final WritePayload writePayload = objectMapper.readValue(publish.getPayload(), payloadClass);
            final WriteInput writeInput = new WriteInputImpl<>(writePayload, writeContext);
            final WriteOutputImpl writeOutput = new WriteOutputImpl();
            final CompletableFuture<Boolean> writeOutputFuture = writeOutput.getFuture();
            protocolAdapter.write(writeInput, writeOutput);
            writeOutputFuture.whenComplete(new AfterWriteCallback(publish, queueId, writeOutput));
            return writeOutputFuture;
        } catch (Exception e) {
            log.warn(
                    "Write invocation on adapter '{}' threw exception. Adapters should not throw Exceptions in the write method, but set them on the output object. ",
                    protocolAdapter.getId());
            return CompletableFuture.failedFuture(e);
        }
    }

    public class AfterWriteCallback implements BiConsumer<Boolean, Throwable> {
        private final @NotNull PUBLISH publish;
        private final @NotNull String queueId;
        private final @NotNull WriteOutputImpl writeOutput;

        public AfterWriteCallback(
                final @NotNull PUBLISH publish,
                final @NotNull String queueId,
                final @NotNull WriteOutputImpl writeOutput) {
            this.publish = publish;
            this.queueId = queueId;
            this.writeOutput = writeOutput;
        }

        @Override
        public void accept(final @Nullable Boolean aBoolean, final @Nullable Throwable throwable) {
            if (throwable != null) {
                // TODO adapter event
                if (writeOutput.canBeRetried()) {
                    log.warn(
                            "Exception happened during the write for adapter '{}', but the message consumption will be retried:",
                            protocolAdapter.getId(),
                            throwable);
                    // reset the inflight marker so that we can consume the publish again on the next occasion
                    removeInflightMarker(queueId, publish.getUniqueId(), publish.getQoS());
                } else {
                    log.warn(
                            "Exception happened during the write for adapter '{}', message consumption can not be retried :",
                            protocolAdapter.getId(),
                            throwable);
                    removeMessage(queueId, publish.getUniqueId(), publish.getQoS());
                    clientQueuePersistence.removeShared(queueId, publish.getUniqueId());
                }
                return;
            } else {
                clientQueuePersistence.removeShared(queueId, publish.getUniqueId());
            }
            if (publish.getQoS() != QoS.AT_MOST_ONCE) {
                //-- 15665 - > QoS 0 causes republishing
                FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, publish.getUniqueId()));
            }
        }
    }


    private void removeInflightMarker(String queueId, String uniqueId, QoS qos) {
        singleWriterService.callbackExecutor(queueId).execute(() -> {
            //QoS 0 has no inflight marker
            if (qos != QoS.AT_MOST_ONCE) {
                //-- 15665 - > QoS 0 causes republishing
                FutureUtils.addExceptionLogger(clientQueuePersistence.removeInFlightMarker(queueId, uniqueId));
            }
        });
    }


    private void removeMessage(String queueId, String uniqueId, QoS qos) {
        singleWriterService.callbackExecutor(queueId).execute(() -> {
            //QoS 0 has no inflight marker
            if (qos != QoS.AT_MOST_ONCE) {
                //-- 15665 - > QoS 0 causes republishing
                FutureUtils.addExceptionLogger(clientQueuePersistence.removeShared(queueId, uniqueId));
            }
        });
    }

}
