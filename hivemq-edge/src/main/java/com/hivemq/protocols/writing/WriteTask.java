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

    public void onMessage(
            final @NotNull PUBLISH publish, final @NotNull String queueId, final @NotNull WriteContext writeContext) {
        try {
            final Class<? extends WritePayload> payloadClass = protocolAdapter.getPayloadClass();
            final WritePayload writePayload = objectMapper.readValue(publish.getPayload(), payloadClass);
            final WriteInput writeInput = new WriteInputImpl<>(writePayload, writeContext);
            final WriteOutputImpl writeOutput = new WriteOutputImpl();
            writeOutput.getFuture().whenComplete(new AfterWriteCallback(publish, queueId, writeOutput));
            protocolAdapter.write(writeInput, writeOutput);
        } catch (Exception e) {
            e.printStackTrace();
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
                // TODO adapter event and adapter id in log
                if (writeOutput.canBeRetried()) {
                    log.warn("Exception happened during the write, but the message consumption will be retried:",
                            throwable);
                    // reset the inflight marker so that we can consume the publish again on the next occasion
                    clientQueuePersistence.removeInFlightMarker(queueId, publish.getUniqueId());

                } else {
                    log.warn("Exception happened during the write, message consumption can not be retried :",
                            throwable);
                    clientQueuePersistence.removeShared(queueId, publish.getUniqueId());
                }
                return;
            } else {
                clientQueuePersistence.removeShared(queueId, publish.getUniqueId());
            }
            System.err.println("throwable:" + throwable);
            System.err.println("booleam:" + aBoolean);
            if (publish.getOnwardQoS() != QoS.AT_MOST_ONCE) {
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
