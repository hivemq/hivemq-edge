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
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bridge.BridgeConstants;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.bridge.metrics.PerBridgeMetrics;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.codec.encoder.mqtt5.Mqtt5PayloadFormatIndicator;
import com.hivemq.common.topic.TopicFilterProcessor;
import com.hivemq.configuration.HivemqId;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.hivemq.bridge.BridgeConstants.HMQ_BRIDGE_HOP_COUNT;

class RemotePublishConsumer implements Consumer<Mqtt5Publish> {
    private static final Logger log = LoggerFactory.getLogger(RemotePublishConsumer.class);

    private final @NotNull RemoteSubscription remoteSubscription;
    private final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler;
    private final @NotNull MqttBridge bridge;
    private final @NotNull ExecutorService executorService;
    private final @NotNull HivemqId hivemqId;
    private final @NotNull PerBridgeMetrics perBridgeMetrics;

    public RemotePublishConsumer(
            final @NotNull RemoteSubscription remoteSubscription,
            final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler,
            final @NotNull MqttBridge bridge,
            final @NotNull ExecutorService executorService,
            final @NotNull HivemqId hivemqId,
            final @NotNull PerBridgeMetrics perBridgeMetrics) {
        this.remoteSubscription = remoteSubscription;
        this.bridgeInterceptorHandler = bridgeInterceptorHandler;
        this.bridge = bridge;
        this.executorService = executorService;
        this.hivemqId = hivemqId;
        this.perBridgeMetrics = perBridgeMetrics;
    }

    @Override
    public void accept(final @NotNull Mqtt5Publish mqtt5Publish) {
        try {
            perBridgeMetrics.getPublishRemoteReceivedCounter().inc();

            int hopCount = extractHopCount(mqtt5Publish);
            if (bridge.isLoopPreventionEnabled() && hopCount > 0 && hopCount >= bridge.getLoopPreventionHopCount()) {
                perBridgeMetrics.getLoopPreventionRemoteDropCounter().inc();
                if (log.isDebugEnabled()) {
                    log.debug("Remote message on topic '{}' ignored for bridge '{}', max hop count exceeded",
                            mqtt5Publish.getTopic(),
                            bridge.getId());
                }
                return;
            }

            final PUBLISH publish = convertPublish(mqtt5Publish, remoteSubscription, bridge, hopCount);

            final ListenableFuture<PublishReturnCode> publishFuture =
                    bridgeInterceptorHandler.interceptOrDelegateInbound(publish, executorService, bridge);

            publishFuture.addListener(() -> {
                try {
                    final PublishReturnCode publishReturnCode = publishFuture.get();
                    switch (publishReturnCode) {
                        case DELIVERED:
                            perBridgeMetrics.getPublishLocalSuccessCounter().inc();
                            break;
                        case NO_MATCHING_SUBSCRIBERS:
                            perBridgeMetrics.getPublishLocalSuccessCounter().inc();
                            perBridgeMetrics.getPublishLocalNoSubscriberCounter().inc();
                            break;
                        case FAILED:
                            perBridgeMetrics.getPublishLocalFailCounter().inc();
                            break;
                    }
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Not able to publish from remote subscription on bridge {}", bridge.getId(), e);
                    perBridgeMetrics.getPublishLocalFailCounter().inc();
                }

            }, executorService);
        } catch (Throwable e) {
            perBridgeMetrics.getPublishLocalFailCounter().inc();
            log.debug("Not able to publish from remote subscription on bridge {}", bridge.getId(), e);
        }
    }

    private @NotNull PUBLISH convertPublish(
            final @NotNull Mqtt5Publish mqtt5Publish,
            final @NotNull RemoteSubscription remoteSubscription,
            final @NotNull MqttBridge bridge,
            final int hopCount) {
        final Integer payloadFormatInidicatorCode = mqtt5Publish.getPayloadFormatIndicator()
                .map(com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PayloadFormatIndicator::getCode)
                .orElse(null);
        final QoS qos = Objects.requireNonNullElse(QoS.valueOf(Math.min(mqtt5Publish.getQos().getCode(),
                remoteSubscription.getMaxQoS())), QoS.AT_MOST_ONCE);
        return new PUBLISHFactory.Mqtt5Builder().withHivemqId(hivemqId.get())
                .withTopic(convertTopic(mqtt5Publish, remoteSubscription, bridge))
                .withContentType(mqtt5Publish.getContentType().map(Object::toString).orElse(null))
                .withCorrelationData(Bytes.getBytesFromReadOnlyBuffer(mqtt5Publish.getCorrelationData()))
                .withQoS(qos)
                .withOnwardQos(qos)
                .withPayload(mqtt5Publish.getPayloadAsBytes())
                .withMessageExpiryInterval(mqtt5Publish.getMessageExpiryInterval()
                        .orElse(PUBLISH.MESSAGE_EXPIRY_INTERVAL_NOT_SET))
                .withPayloadFormatIndicator(payloadFormatInidicatorCode != null ?
                        Mqtt5PayloadFormatIndicator.fromCode(payloadFormatInidicatorCode) :
                        null)
                .withRetain(remoteSubscription.isPreserveRetain() && mqtt5Publish.isRetain())
                .withResponseTopic(mqtt5Publish.getResponseTopic().map(Object::toString).orElse(null))
                .withUserProperties(convertUserProperties(mqtt5Publish.getUserProperties(), hopCount))
                .build();
    }

    private static @NotNull String convertTopic(
            final @NotNull Mqtt5Publish mqtt5Publish,
            final @NotNull RemoteSubscription remoteSubscription,
            final @NotNull MqttBridge bridge) {

        return TopicFilterProcessor.modifyTopic(remoteSubscription.getDestination(),
                mqtt5Publish.getTopic(),
                Map.of(BridgeConstants.BRIDGE_NAME_TOPIC_REPLACEMENT_TOKEN, bridge.getId())).toString();
    }

    private @NotNull Mqtt5UserProperties convertUserProperties(
            final @NotNull com.hivemq.client.mqtt.mqtt5.datatypes.Mqtt5UserProperties userProperties,
            final int hopCount) {
        if (userProperties.asList().isEmpty() && remoteSubscription.getCustomUserProperties().isEmpty()) {
            if (bridge.isLoopPreventionEnabled()) {
                return Mqtt5UserProperties.of(MqttUserProperty.of(HMQ_BRIDGE_HOP_COUNT, "1"));
            } else {
                return Mqtt5UserProperties.NO_USER_PROPERTIES;
            }
        }

        final List<MqttUserProperty> filteredProps = userProperties.asList()
                .stream()
                .filter(mqtt5UserProperty -> !mqtt5UserProperty.getName().toString().equals(HMQ_BRIDGE_HOP_COUNT))
                .map(originalProp -> MqttUserProperty.of(originalProp.getName().toString(),
                        originalProp.getValue().toString()))
                .collect(Collectors.toList());
        for (CustomUserProperty customUserProperty : remoteSubscription.getCustomUserProperties()) {
            filteredProps.add(MqttUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue()));
        }
        if (bridge.isLoopPreventionEnabled()) {
            filteredProps.add(MqttUserProperty.of(HMQ_BRIDGE_HOP_COUNT, Integer.toString(hopCount + 1)));
        }
        return Mqtt5UserProperties.of(ImmutableList.copyOf(filteredProps));
    }

    private int extractHopCount(@NotNull Mqtt5Publish mqtt5Publish) {
        if (!bridge.isLoopPreventionEnabled()) {
            return 0;
        }
        try {
            final Optional<Integer> originalHopCount = mqtt5Publish.getUserProperties()
                    .asList()
                    .stream()
                    .filter(prop -> prop.getName().toString().equals(HMQ_BRIDGE_HOP_COUNT))
                    .map(prop -> Integer.parseInt(prop.getValue().toString()))
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
}
