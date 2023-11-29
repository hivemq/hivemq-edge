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
package com.hivemq.bridge.mqtt;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.bridge.BridgeConstants;
import com.hivemq.bridge.MqttForwarder;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.datatypes.MqttTopic;
import com.hivemq.client.mqtt.datatypes.MqttTopicFilter;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties;
import com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserPropertiesBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.common.topic.TopicFilterProcessor;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.hivemq.bridge.BridgeConstants.HMQ_BRIDGE_HOP_COUNT;

public class RemoteMqttForwarder implements MqttForwarder {

    private static final Logger log = LoggerFactory.getLogger(RemoteMqttForwarder.class);
    public static final String DEFAULT_DESTINATION_PATTERN = "{#}";

    private final @NotNull String id;
    private final @NotNull MqttBridge bridge;
    private final @NotNull LocalSubscription localSubscription;
    private final @NotNull BridgeMqttClient remoteMqttClient;
    private final @NotNull PerBridgeMetrics perBridgeMetrics;
    private final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler;
    private final AtomicInteger inflightCounter = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(false);

    private @Nullable MqttForwarder.AfterForwardCallback afterForwardCallback;
    private @NotNull ResetInflightMarkerCallback resetInflightMarkerCallback;

    private @Nullable ExecutorService executorService;

    private final ConcurrentLinkedQueue<BufferedPublishInformation> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OutflightPublishInformation> outflightQueue = new ConcurrentLinkedQueue<>();


    public RemoteMqttForwarder(
            final @NotNull String id,
            final @NotNull MqttBridge bridge,
            final @NotNull LocalSubscription localSubscription,
            final @NotNull BridgeMqttClient remoteMqttClient,
            final @NotNull PerBridgeMetrics perBridgeMetrics,
            final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler) {
        this.id = id;
        this.bridge = bridge;
        this.localSubscription = localSubscription;
        this.remoteMqttClient = remoteMqttClient;
        this.perBridgeMetrics = perBridgeMetrics;
        this.bridgeInterceptorHandler = bridgeInterceptorHandler;
    }

    public void start() {
        running.set(true);
    }

    @Override
    public synchronized void stop() {
        running.set(false);
        BufferedPublishInformation bufferedMessage = queue.poll();
        while (bufferedMessage != null) {
            resetInflightMarkerCallback.afterMessage(bufferedMessage.queueId, bufferedMessage.publish.getUniqueId());
            bufferedMessage = queue.poll();
        }

        OutflightPublishInformation inClientBufferedMessage = outflightQueue.poll();
        while (inClientBufferedMessage != null) {
            resetInflightMarkerCallback.afterMessage(inClientBufferedMessage.queueId, inClientBufferedMessage.uniqueId);
            inClientBufferedMessage = outflightQueue.poll();
        }
    }


    @Override
    public void onMessage(final @NotNull PUBLISH publish, @NotNull final String queueId) {
        perBridgeMetrics.getPublishLocalReceivedCounter().inc();

        final QoS originalQoS = publish.getQoS();
        final String originalUniqueId = publish.getUniqueId();
        if (!running.get()) {
            if (afterForwardCallback != null) {
                afterForwardCallback.afterMessage(originalQoS, originalUniqueId, queueId, true);
            }
            return;
        }

        inflightCounter.incrementAndGet();

        try {
            int hopCount = extractHopCount(publish);
            if (bridge.isLoopPreventionEnabled() && hopCount > 0 && hopCount >= bridge.getLoopPreventionHopCount()) {
                perBridgeMetrics.getLoopPreventionForwardDropCounter().inc();
                if (log.isDebugEnabled()) {
                    log.debug("Local message on topic '{}' ignored for bridge '{}', max hop count exceeded",
                            publish.getTopic(),
                            bridge.getId());
                }
                finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                return;
            }

            //filter out excludes
            for (String exclude : localSubscription.getExcludes()) {
                if (MqttTopicFilter.of(exclude).matches(MqttTopicFilter.of(publish.getTopic()))) {
                    perBridgeMetrics.getRemotePublishExcludedCounter().inc();
                    finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                    return;
                }
            }

            final PUBLISH convertedPublish = convertPublishAfterBridge(publish, hopCount);

            //run interceptors
            final ListenableFuture<BridgeInterceptorHandler.InterceptorResult> publishFuture =
                    bridgeInterceptorHandler.interceptOrDelegateOutbound(convertedPublish,
                            MoreExecutors.newDirectExecutorService(),
                            bridge);
            Futures.addCallback(publishFuture, new FutureCallback<>() {
                @Override
                public void onSuccess(final @NotNull BridgeInterceptorHandler.InterceptorResult result) {
                    try {

                        switch (result.getOutcome()) {
                            case DROP:
                                finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                                break;
                            case SUCCESS:
                                sendPublishToRemote(Objects.requireNonNull(result.getPublish()),
                                        queueId,
                                        publish.getQoS(),
                                        originalUniqueId);
                                break;
                        }
                    } catch (Throwable t) {
                        handlePublishError(publish, t);
                        finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    handlePublishError(publish, t);
                    finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                }
            }, executorService);

        } catch (Exception e) {
            handlePublishError(publish, e);
            finishProcessing(originalQoS, publish.getUniqueId(), queueId);
        }

    }

    private void finishProcessing(
            final @NotNull QoS originalQoS, final @NotNull String uniqueId, final @NotNull String queueId) {
        inflightCounter.decrementAndGet();
        if (afterForwardCallback != null) {
            afterForwardCallback.afterMessage(originalQoS, uniqueId, queueId, false);
        }
    }

    @NotNull
    private PUBLISH convertPublishAfterBridge(@NotNull PUBLISH publish, int hopCount) {
        final MqttTopic modifiedTopic = convertTopic(localSubscription.getDestination(), publish.getTopic());
        final QoS modifiedQoS = convertQos(localSubscription.getMaxQoS(), publish.getQoS());
        final PUBLISHFactory.Mqtt5Builder mqtt5Builder = new PUBLISHFactory.Mqtt5Builder();
        mqtt5Builder.fromPublish(publish);
        mqtt5Builder.withTopic(modifiedTopic.toString());
        mqtt5Builder.withQoS(modifiedQoS);
        mqtt5Builder.withOnwardQos(modifiedQoS);
        mqtt5Builder.withRetain(localSubscription.isPreserveRetain() && publish.isRetain());
        mqtt5Builder.withUserProperties(convertUserProperties(publish.getUserProperties(), hopCount));
        return mqtt5Builder.build();
    }


    private synchronized void sendPublishToRemote(
            final @NotNull PUBLISH publish,
            final @NotNull String queueId,
            final @NotNull QoS originalQoS,
            final @NotNull String originalUniqueId) {

        if (!remoteMqttClient.isConnected()) {
            queue.add(new BufferedPublishInformation(queueId, originalUniqueId, originalQoS, publish));
            return;
        }

        // first send the publishes that are inflight
        drainQueue();

        final Mqtt5Publish mqtt5Publish = convertPublishForClient(publish);
        final CompletableFuture<Mqtt5PublishResult> publishResult =
                remoteMqttClient.getMqtt5Client().publish(mqtt5Publish);
        final OutflightPublishInformation outflightPublishInformation =
                new OutflightPublishInformation(queueId, publish.getUniqueId());
        outflightQueue.add(outflightPublishInformation);
        publishResult.whenComplete((mqtt5PublishResult, throwable) -> {
            if (throwable != null) {
                handlePublishError(publish, throwable);
            } else {
                perBridgeMetrics.getPublishForwardSuccessCounter().inc();
            }
            finishProcessing(originalQoS, originalUniqueId, queueId);
            outflightQueue.remove(outflightPublishInformation);
        });
    }

    @Override
    public synchronized void drainQueue() {
        BufferedPublishInformation bufferedPublishInformation = queue.poll();
        while (bufferedPublishInformation!=null) {
            final CompletableFuture<Mqtt5PublishResult> publishResult = remoteMqttClient.getMqtt5Client()
                    .publish(convertPublishForClient(bufferedPublishInformation.publish));
            final OutflightPublishInformation outflightPublishInformation = new OutflightPublishInformation(
                    bufferedPublishInformation.queueId,
                    bufferedPublishInformation.publish.getUniqueId());
            outflightQueue.add(outflightPublishInformation);

            // lambdas hate this trick. (we need a final variable for the lamdba)
            final BufferedPublishInformation finalBufferedPublishInformation = bufferedPublishInformation;
            publishResult.whenComplete((mqtt5PublishResult, throwable) -> {
                if (throwable != null) {
                    handlePublishError(finalBufferedPublishInformation.publish, throwable);
                } else {
                    perBridgeMetrics.getPublishForwardSuccessCounter().inc();
                }
                finishProcessing(finalBufferedPublishInformation.originalQqS,
                        finalBufferedPublishInformation.uniqueId,
                        finalBufferedPublishInformation.queueId);
                outflightQueue.remove(outflightPublishInformation);
            });
            bufferedPublishInformation = queue.poll();
        }
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NotNull
    private Mqtt5Publish convertPublishForClient(@NotNull PUBLISH publish) {
        final Mqtt5PublishBuilder.Complete publishBuilder = Mqtt5Publish.builder().topic(publish.getTopic());
        publishBuilder.payload(publish.getPayload()).qos(MqttQos.fromCode(publish.getQoS().getQosNumber()));

        if (publish.getMessageExpiryInterval() <= PUBLISH.MESSAGE_EXPIRY_INTERVAL_MAX) {
            publishBuilder.messageExpiryInterval(publish.getMessageExpiryInterval());
        }

        if (localSubscription.isPreserveRetain()) {
            publishBuilder.retain(publish.isRetain());
        } else {
            publishBuilder.retain(false);
        }

        if (publish.getContentType() != null) {
            publishBuilder.contentType(publish.getContentType());
        }
        if (publish.getCorrelationData() != null) {
            publishBuilder.correlationData(publish.getCorrelationData());
        }

        if (publish.getPayloadFormatIndicator() != null) {
            final int payloadIndicatorCode = publish.getPayloadFormatIndicator().getCode();
            publishBuilder.payloadFormatIndicator(Mqtt5PayloadFormatIndicator.fromCode(payloadIndicatorCode));
        }

        if (publish.getResponseTopic() != null) {
            publishBuilder.responseTopic(publish.getResponseTopic());
        }

        publishBuilder.userProperties(convertUserPropertiesForClient(publish.getUserProperties()));
        final Mqtt5Publish mqtt5Publish = publishBuilder.build();
        return mqtt5Publish;
    }


    private @NotNull QoS convertQos(final int maxQos, @NotNull QoS qos) {
        //fallback to QoS1 should never be used
        final QoS mqttQos = Objects.requireNonNullElse(QoS.valueOf(qos.getQosNumber()), QoS.AT_LEAST_ONCE);
        if (mqttQos.getQosNumber() < maxQos) {
            return mqttQos;
        } else {
            return Objects.requireNonNullElse(QoS.valueOf(maxQos), QoS.AT_LEAST_ONCE);
        }
    }

    private @NotNull MqttTopic convertTopic(@Nullable String destination, @NotNull String topic) {
        if (destination == null || destination.equals(DEFAULT_DESTINATION_PATTERN)) {
            return MqttTopic.of(topic);
        }
        return TopicFilterProcessor.modifyTopic(destination,
                MqttTopic.of(topic),
                Map.of(BridgeConstants.BRIDGE_NAME_TOPIC_REPLACEMENT_TOKEN, bridge.getId()));
    }

    private void handlePublishError(@NotNull PUBLISH publish, Throwable throwable) {
        perBridgeMetrics.getPublishForwardFailCounter().inc();
        log.warn("Unable to forward message on topic '{}' for bridge '{}', reason: {}",
                publish.getTopic(),
                id,
                throwable.getMessage());
        log.debug("original exception", throwable);
    }

    private com.hivemq.mqtt.message.mqtt5.@NotNull Mqtt5UserProperties convertUserProperties(
            @NotNull com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties userProperties, final int hopCount) {

        if (userProperties.asList().isEmpty() && localSubscription.getCustomUserProperties().isEmpty()) {
            if (bridge.isLoopPreventionEnabled()) {
                return com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.of(MqttUserProperty.of(HMQ_BRIDGE_HOP_COUNT,
                        "1"));
            }
            return com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.of();
        }

        final ImmutableList.Builder<MqttUserProperty> builder = ImmutableList.builder();
        for (MqttUserProperty mqttUserProperty : userProperties.asList()) {
            if (mqttUserProperty.getName().equals(HMQ_BRIDGE_HOP_COUNT)) {
                continue;
            }
            builder.add(MqttUserProperty.of(mqttUserProperty.getName(), mqttUserProperty.getValue()));
        }
        for (CustomUserProperty customUserProperty : localSubscription.getCustomUserProperties()) {
            builder.add(MqttUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue()));
        }
        if (bridge.isLoopPreventionEnabled()) {
            builder.add(MqttUserProperty.of(HMQ_BRIDGE_HOP_COUNT, Integer.toString(hopCount + 1)));
        }
        return com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.build(builder);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private @NotNull Mqtt5UserProperties convertUserPropertiesForClient(@NotNull com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties userProperties) {

        final Mqtt5UserPropertiesBuilder mqtt5UserPropertiesBuilder = Mqtt5UserProperties.builder();
        for (MqttUserProperty mqttUserProperty : userProperties.asList()) {
            mqtt5UserPropertiesBuilder.add(mqttUserProperty.getName(), mqttUserProperty.getValue());
        }
        return mqtt5UserPropertiesBuilder.build();
    }

    private int extractHopCount(@NotNull PUBLISH publish) {
        if (!bridge.isLoopPreventionEnabled()) {
            return 0;
        }
        try {
            final Optional<Integer> originalHopCount = publish.getUserProperties()
                    .asList()
                    .stream()
                    .filter(prop -> prop.getName().equals(HMQ_BRIDGE_HOP_COUNT))
                    .map(prop -> Integer.parseInt(prop.getValue()))
                    .findFirst();
            return originalHopCount.orElse(0);
        } catch (NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug("Max hop count could not be determined, user property `{}` is not a number",
                        HMQ_BRIDGE_HOP_COUNT);
            }
            return 0;
        }
    }

    @Override
    public void setAfterForwardCallback(@NotNull final AfterForwardCallback callback) {
        this.afterForwardCallback = callback;
    }

    @Override
    public void setResetInflightMarkerCallback(@NotNull final ResetInflightMarkerCallback callback) {
        this.resetInflightMarkerCallback = callback;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull List<String> getTopics() {
        return localSubscription.getFilters();
    }


    @Override
    public int getInflightCount() {
        return inflightCounter.get();
    }

    @Override
    public void setExecutorService(final @NotNull ExecutorService executorService) {
        this.executorService = executorService;
    }


    private static class BufferedPublishInformation {
        private final @NotNull String queueId;
        private final String uniqueId;
        private final @NotNull QoS originalQqS;
        private final @NotNull PUBLISH publish;


        private BufferedPublishInformation(
                final @NotNull String queueId,
                final @NotNull String uniqueId,
                final @NotNull QoS originalQqS,
                final @NotNull PUBLISH origPublish) {
            this.queueId = queueId;
            this.uniqueId = uniqueId;
            this.originalQqS = originalQqS;
            this.publish = origPublish;
        }
    }

    private static class OutflightPublishInformation {
        private final @NotNull String queueId;
        private final @NotNull String uniqueId;


        private OutflightPublishInformation(
                final @NotNull String queueId, final @NotNull String uniqueId) {
            this.queueId = queueId;
            this.uniqueId = uniqueId;
        }
    }
}
