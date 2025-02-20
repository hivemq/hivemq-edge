package com.hivemq.combining.runtime;

import com.hivemq.configuration.HivemqId;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.InternalPublishService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.hivemq.mqtt.message.publish.PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET;

@Singleton
public class DataCombiningPublishService {

    private static final Logger log = LoggerFactory.getLogger(DataCombiningPublishService.class);

    private final @NotNull HivemqId hiveMQId;
    private final @NotNull InternalPublishService internalPublishService;
    private final @NotNull ExecutorService executorService;

    @Inject
    public DataCombiningPublishService(
            final @NotNull HivemqId hiveMQId,
            final @NotNull InternalPublishService internalPublishService,
            final @NotNull ExecutorService executorService) {
        this.hiveMQId = hiveMQId;
        this.internalPublishService = internalPublishService;
        this.executorService = executorService;
    }

    public void publish(final @NotNull String clientId, final @NotNull String topic, final @NotNull byte[] payload) {
        var publish = new PUBLISHFactory.Mqtt5Builder()
                .withHivemqId(hiveMQId.get())
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withRetain(false)
                .withTopic(topic)
                .withPayload(payload)
                .withMessageExpiryInterval(MESSAGE_EXPIRY_INTERVAL_NOT_SET)
                .withResponseTopic(null)
                .withCorrelationData(null)
                .withPayload(payload)
                .withContentType(null)
                .withPayloadFormatIndicator(null)
                .withUserProperties(Mqtt5UserProperties.of())
                .build();

        try {
            //TODO
            log.error("CHANGE THIS TO NOT BE BLOCKING!!!!");

            internalPublishService
                    .publish(publish, executorService, clientId)
                    .get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
