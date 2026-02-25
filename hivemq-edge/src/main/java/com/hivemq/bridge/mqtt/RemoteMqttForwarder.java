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

import static com.hivemq.bridge.BridgeConstants.HMQ_BRIDGE_HOP_COUNT;
import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElse;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
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
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.mqtt5.MqttUserProperty;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("FutureReturnValueIgnored")
public class RemoteMqttForwarder implements MqttForwarder {

    public static final String DEFAULT_DESTINATION_PATTERN = "{#}";
    private static final Logger log = LoggerFactory.getLogger(RemoteMqttForwarder.class);
    private final @NotNull String id;
    private final @NotNull MqttBridge bridge;
    private final @NotNull LocalSubscription localSubscription;
    private final @NotNull BridgeMqttClient remoteMqttClient;
    private final @NotNull PerBridgeMetrics perBridgeMetrics;
    private final @NotNull BridgeInterceptorHandler bridgeInterceptorHandler;
    private final AtomicInteger inflightCounter = new AtomicInteger(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean draining = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<BufferedPublishInformation> queue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<OutflightPublishInformation> outflightQueue = new ConcurrentLinkedQueue<>();
    private volatile @Nullable MqttForwarder.AfterForwardCallback afterForwardCallback;
    private volatile @Nullable ResetInflightMarkerCallback resetInflightMarkerCallback;
    private volatile @Nullable ResetAllInflightMarkersCallback resetAllInflightMarkersCallback;
    private volatile @Nullable OnReconnectCallback onReconnectCallback;
    private volatile @Nullable ExecutorService executorService;

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

    private static @NotNull QoS convertQos(final int maxQos, final @NotNull QoS qos) {
        // fallback to QoS1 should never be used
        final QoS mqttQos = requireNonNullElse(QoS.valueOf(qos.getQosNumber()), QoS.AT_LEAST_ONCE);
        if (mqttQos.getQosNumber() < maxQos) {
            return mqttQos;
        } else {
            return requireNonNull(requireNonNullElse(QoS.valueOf(maxQos), QoS.AT_LEAST_ONCE));
        }
    }

    @Override
    public synchronized void start() {
        running.set(true);
        if (log.isDebugEnabled()) {
            log.debug("Forwarder '{}' started for bridge '{}'", id, bridge.getId());
        }
    }

    @Override
    public synchronized void stop() {
        if (running.compareAndSet(true, false)) {
            final int queuedMessages = queue.size();
            final int outflightMessages = outflightQueue.size();

            if (log.isDebugEnabled()) {
                log.debug(
                        "Stopping forwarder '{}' for bridge '{}', clearing {} queued and {} outflight message(s)",
                        id,
                        bridge.getId(),
                        queuedMessages,
                        outflightMessages);
            }

            int clearedQueued = 0;
            BufferedPublishInformation queueMessage = queue.poll();
            while (queueMessage != null) {
                final var resetInflightMarkerCallback = this.resetInflightMarkerCallback;
                if (resetInflightMarkerCallback != null) {
                    resetInflightMarkerCallback.afterMessage(queueMessage.queueId, queueMessage.publish.getUniqueId());
                }
                clearedQueued++;
                queueMessage = queue.poll();
            }

            int clearedOutflight = 0;
            OutflightPublishInformation outflightMessage = outflightQueue.poll();
            while (outflightMessage != null) {
                if (resetInflightMarkerCallback != null) {
                    resetInflightMarkerCallback.afterMessage(outflightMessage.queueId, outflightMessage.uniqueId);
                }
                clearedOutflight++;
                outflightMessage = outflightQueue.poll();
            }

            if (log.isInfoEnabled() && (clearedQueued > 0 || clearedOutflight > 0)) {
                log.info(
                        "Forwarder '{}' stopped, cleared {} queued and {} outflight message(s)",
                        id,
                        clearedQueued,
                        clearedOutflight);
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Forwarder '{}' already stopped", id);
            }
        }
    }

    @Override
    public void onMessage(final @NotNull PUBLISH publish, final @NotNull String queueId) {
        perBridgeMetrics.getPublishLocalReceivedCounter().inc();

        if (log.isTraceEnabled()) {
            log.trace(
                    "Forwarder '{}' received message on topic '{}' with QoS {} for bridge '{}'",
                    id,
                    publish.getTopic(),
                    publish.getQoS(),
                    bridge.getId());
        }

        final QoS originalQoS = publish.getQoS();
        final String originalUniqueId = publish.getUniqueId();
        if (!running.get()) {
            // the forwarder is already stopped, we reset the marker, but do not checkBuffer() again to prevent a loop
            if (log.isTraceEnabled()) {
                log.trace("Forwarder '{}' not running, dropping message on topic '{}'", id, publish.getTopic());
            }
            final var resetInflightMarkerCallback = this.resetInflightMarkerCallback;
            if (resetInflightMarkerCallback != null) {
                resetInflightMarkerCallback.afterMessage(queueId, publish.getUniqueId());
            }
            return;
        }

        inflightCounter.incrementAndGet();

        try {
            final int hopCount = extractHopCount(publish);
            if (bridge.isLoopPreventionEnabled() && hopCount > 0 && hopCount >= bridge.getLoopPreventionHopCount()) {
                perBridgeMetrics.getLoopPreventionForwardDropCounter().inc();
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Local message on topic '{}' ignored for bridge '{}', max hop count exceeded",
                            publish.getTopic(),
                            bridge.getId());
                }
                finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                return;
            }

            // filter out excludes
            for (final String exclude : localSubscription.getExcludes()) {
                if (MqttTopicFilter.of(exclude).matches(MqttTopicFilter.of(publish.getTopic()))) {
                    perBridgeMetrics.getRemotePublishExcludedCounter().inc();
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Message on topic '{}' excluded by filter '{}' for bridge '{}'",
                                publish.getTopic(),
                                exclude,
                                bridge.getId());
                    }
                    finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                    return;
                }
            }

            final PUBLISH convertedPublish = convertPublishAfterBridge(publish, hopCount);

            // run interceptors
            final long interceptorStartTime = log.isDebugEnabled() ? System.nanoTime() : 0;
            Futures.addCallback(
                    bridgeInterceptorHandler.interceptOrDelegateOutbound(
                            convertedPublish, MoreExecutors.newDirectExecutorService(), bridge),
                    new FutureCallback<>() {
                        @Override
                        public void onSuccess(final @NotNull BridgeInterceptorHandler.InterceptorResult result) {
                            try {
                                if (log.isDebugEnabled()) {
                                    final long durationMicros = (System.nanoTime() - interceptorStartTime) / 1000;
                                    log.debug(
                                            "Interceptor chain completed in {} μs with outcome {} for message on topic '{}' for bridge '{}'",
                                            durationMicros,
                                            result.getOutcome(),
                                            publish.getTopic(),
                                            bridge.getId());
                                }

                                switch (result.getOutcome()) {
                                    case DROP -> {
                                        if (log.isDebugEnabled()) {
                                            log.debug(
                                                    "Message on topic '{}' dropped by interceptor for bridge '{}'",
                                                    publish.getTopic(),
                                                    bridge.getId());
                                        }
                                        finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                                    }
                                    case SUCCESS ->
                                        sendPublishToRemote(
                                                Objects.requireNonNull(result.getPublish()),
                                                queueId,
                                                publish.getQoS(),
                                                originalUniqueId);
                                }
                            } catch (final Throwable t) {
                                handlePublishError(publish, t);
                                finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                            }
                        }

                        @Override
                        public void onFailure(final @NotNull Throwable t) {
                            handlePublishError(publish, t);
                            finishProcessing(originalQoS, publish.getUniqueId(), queueId);
                        }
                    },
                    executorService);
        } catch (final Exception e) {
            handlePublishError(publish, e);
            finishProcessing(originalQoS, publish.getUniqueId(), queueId);
        }
    }

    private void finishProcessing(
            final @NotNull QoS originalQoS, final @NotNull String uniqueId, final @NotNull String queueId) {
        inflightCounter.decrementAndGet();
        final var afterForwardCallback = this.afterForwardCallback;
        if (afterForwardCallback != null) {
            afterForwardCallback.afterMessage(originalQoS, uniqueId, queueId, false);
        }
    }

    /**
     * Called when a publish fails - resets the inflight marker so the message can be retried
     * instead of being removed from persistence. This prevents message loss on transient failures.
     */
    private void finishProcessingWithRetry(
            final @NotNull QoS originalQoS, final @NotNull String uniqueId, final @NotNull String queueId) {
        inflightCounter.decrementAndGet();
        // Reset inflight marker instead of removing the message, allowing retry
        final var resetInflightMarkerCallback = this.resetInflightMarkerCallback;
        if (resetInflightMarkerCallback != null) {
            resetInflightMarkerCallback.afterMessage(queueId, uniqueId);
        }
    }

    private @NotNull PUBLISH convertPublishAfterBridge(final @NotNull PUBLISH publish, final int hopCount) {
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
            if (log.isTraceEnabled()) {
                log.trace(
                        "Remote client disconnected, buffering message on topic '{}' for bridge '{}', buffer size: {}",
                        publish.getTopic(),
                        bridge.getId(),
                        queue.size());
            }
            return;
        }

        // First send any buffered messages that accumulated while disconnected.
        // Note: This does NOT reset inflight markers - these messages were already
        // marked as inflight when they were originally polled from persistence.
        sendBufferedMessages();

        final long publishStartTime = log.isDebugEnabled() ? System.nanoTime() : 0;
        final Mqtt5Publish mqtt5Publish = convertPublishForClient(publish);
        final CompletableFuture<Mqtt5PublishResult> publishResult =
                remoteMqttClient.getMqtt5Client().publish(mqtt5Publish);
        final OutflightPublishInformation outflightPublishInformation =
                new OutflightPublishInformation(queueId, publish.getUniqueId());
        outflightQueue.add(outflightPublishInformation);
        publishResult.whenComplete((mqtt5PublishResult, throwable) -> {
            if (throwable != null) {
                handlePublishError(publish, throwable);
                // On failure, reset the inflight marker so the message can be retried
                // instead of being removed from persistence
                finishProcessingWithRetry(originalQoS, originalUniqueId, queueId);
            } else {
                perBridgeMetrics.getPublishForwardSuccessCounter().inc();
                if (log.isDebugEnabled()) {
                    final long durationMicros = (System.nanoTime() - publishStartTime) / 1000;
                    log.debug(
                            "Successfully published message on topic '{}' to remote broker for bridge '{}' in {} μs",
                            publish.getTopic(),
                            bridge.getId(),
                            durationMicros);
                }
                finishProcessing(originalQoS, originalUniqueId, queueId);
            }
            outflightQueue.remove(outflightPublishInformation);
        });
    }

    /**
     * Sends buffered messages that accumulated while disconnected.
     * Does NOT reset inflight markers - these messages were already marked as inflight
     * when they were originally polled from persistence.
     */
    private synchronized void sendBufferedMessages() {
        final int bufferSize = queue.size();
        if (bufferSize > 0 && log.isDebugEnabled()) {
            log.debug("Sending {} buffered message(s) for bridge '{}'", bufferSize, bridge.getId());
        }

        BufferedPublishInformation buffered = queue.poll();
        while (buffered != null && remoteMqttClient.isConnected()) {
            final BufferedPublishInformation current = buffered;
            final long publishStartTime = log.isDebugEnabled() ? System.nanoTime() : 0;
            final Mqtt5Publish mqtt5Publish = convertPublishForClient(current.publish);
            final CompletableFuture<Mqtt5PublishResult> publishResult =
                    remoteMqttClient.getMqtt5Client().publish(mqtt5Publish);
            final OutflightPublishInformation outflightPublishInformation =
                    new OutflightPublishInformation(current.queueId, current.uniqueId);
            outflightQueue.add(outflightPublishInformation);
            publishResult.whenComplete((mqtt5PublishResult, throwable) -> {
                if (throwable != null) {
                    handlePublishError(current.publish, throwable);
                    finishProcessingWithRetry(current.originalQoS, current.uniqueId, current.queueId);
                } else {
                    perBridgeMetrics.getPublishForwardSuccessCounter().inc();
                    if (log.isDebugEnabled()) {
                        final long durationMicros = (System.nanoTime() - publishStartTime) / 1000;
                        log.debug(
                                "Successfully published buffered message on topic '{}' to remote broker for bridge '{}' in {} μs",
                                current.publish.getTopic(),
                                bridge.getId(),
                                durationMicros);
                    }
                    finishProcessing(current.originalQoS, current.uniqueId, current.queueId);
                }
                outflightQueue.remove(outflightPublishInformation);
            });
            buffered = queue.poll();
        }
    }

    @Override
    public void flushBufferedMessages() {
        // This method is called on initial connection to send messages that were buffered
        // while waiting for the remote broker to become available.
        // Unlike drainQueue(), this does NOT reset persistence inflight markers.
        if (log.isDebugEnabled()) {
            log.debug(
                    "Flushing {} buffered message(s) for forwarder '{}' on bridge '{}'",
                    queue.size(),
                    id,
                    bridge.getId());
        }
        sendBufferedMessages();
    }

    @Override
    public void drainQueue() {
        // Called on reconnection to reset all in-flight state.
        // This method is NOT synchronized to avoid deadlock with sendPublishToRemote callbacks.
        // Instead, we use a draining flag to coordinate with message processing.

        // Use compareAndSet to ensure only one drain operation runs at a time
        if (!draining.compareAndSet(false, true)) {
            if (log.isDebugEnabled()) {
                log.debug("drainQueue() already in progress for forwarder '{}', skipping", id);
            }
            return;
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("drainQueue() entered for forwarder '{}' on bridge '{}'", id, bridge.getId());
            }

            // Clear stale outflight messages from the previous connection FIRST.
            // These were sent but their completion callbacks might not have been called.
            // Clearing these before resetting inflight markers prevents race conditions.
            int clearedOutflight = 0;
            OutflightPublishInformation staleOutflight = outflightQueue.poll();
            while (staleOutflight != null) {
                clearedOutflight++;
                staleOutflight = outflightQueue.poll();
            }

            // Clear the in-memory queue buffer as well.
            // These messages were buffered while disconnected and should be retried from persistence
            // to maintain proper ordering and avoid duplicates.
            int clearedQueued = 0;
            BufferedPublishInformation queuedMessage = queue.poll();
            while (queuedMessage != null) {
                clearedQueued++;
                queuedMessage = queue.poll();
            }

            // Reset the inflight counter since we've just reconnected and there are no
            // messages actually in flight anymore. Any pending completion handlers from
            // the old connection will decrement harmlessly.
            final int previousInflight = inflightCounter.getAndSet(0);

            if (log.isDebugEnabled() && (clearedOutflight > 0 || clearedQueued > 0 || previousInflight > 0)) {
                log.debug(
                        "Reconnection reset for bridge '{}': cleared {} outflight, {} queued messages, "
                                + "reset inflightCounter from {} to 0",
                        bridge.getId(),
                        clearedOutflight,
                        clearedQueued,
                        previousInflight);
            }

            // CRITICAL: Reset ALL inflight markers in persistence for all queues this forwarder handles.
            // This handles the case where messages were read from persistence (marking them as in-flight)
            // but never made it to our local queues (e.g., they were in the interceptor chain when
            // the connection dropped). Without this, those messages would remain stuck in persistence
            // with inflight markers and never be re-delivered.
            //
            // This is done AFTER clearing local state to ensure any concurrent message processing
            // sees the reset state before new messages arrive from persistence.
            final var resetAllInflightMarkersCallback = this.resetAllInflightMarkersCallback;
            if (resetAllInflightMarkersCallback != null) {
                if (log.isDebugEnabled()) {
                    log.debug("drainQueue() calling resetAllInflightMarkersCallback for forwarder '{}'", id);
                }
                resetAllInflightMarkersCallback.resetAll(id);
                if (log.isDebugEnabled()) {
                    log.debug("drainQueue() resetAllInflightMarkersCallback completed for forwarder '{}'", id);
                }
            }
        } finally {
            draining.set(false);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NotNull
    private Mqtt5Publish convertPublishForClient(final @NotNull PUBLISH publish) {
        final Mqtt5PublishBuilder.Complete publishBuilder =
                Mqtt5Publish.builder().topic(publish.getTopic());
        publishBuilder
                .payload(publish.getPayload())
                .qos(MqttQos.fromCode(publish.getQoS().getQosNumber()));

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
        return publishBuilder.build();
    }

    private @NotNull MqttTopic convertTopic(final @Nullable String destination, final @NotNull String topic) {
        if (destination == null || destination.equals(DEFAULT_DESTINATION_PATTERN)) {
            return MqttTopic.of(topic);
        }
        return TopicFilterProcessor.modifyTopic(
                destination,
                MqttTopic.of(topic),
                Map.of(BridgeConstants.BRIDGE_NAME_TOPIC_REPLACEMENT_TOKEN, bridge.getId()));
    }

    private void handlePublishError(final @NotNull PUBLISH publish, final @NotNull Throwable throwable) {
        perBridgeMetrics.getPublishForwardFailCounter().inc();
        log.warn(
                "Unable to forward message on topic '{}' for bridge '{}', reason: {}",
                publish.getTopic(),
                id,
                throwable.getMessage());
        log.debug("original exception", throwable);
    }

    private com.hivemq.mqtt.message.mqtt5.@NotNull Mqtt5UserProperties convertUserProperties(
            final @NotNull com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties userProperties, final int hopCount) {
        if (userProperties.asList().isEmpty()
                && localSubscription.getCustomUserProperties().isEmpty()) {
            if (bridge.isLoopPreventionEnabled()) {
                return com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.of(
                        MqttUserProperty.of(HMQ_BRIDGE_HOP_COUNT, "1"));
            }
            return com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.of();
        }

        final ImmutableList.Builder<@NotNull MqttUserProperty> builder = ImmutableList.builder();
        for (final MqttUserProperty mqttUserProperty : userProperties.asList()) {
            if (mqttUserProperty.getName().equals(HMQ_BRIDGE_HOP_COUNT)) {
                continue;
            }
            builder.add(MqttUserProperty.of(mqttUserProperty.getName(), mqttUserProperty.getValue()));
        }
        for (final CustomUserProperty customUserProperty : localSubscription.getCustomUserProperties()) {
            builder.add(MqttUserProperty.of(customUserProperty.getKey(), customUserProperty.getValue()));
        }
        if (bridge.isLoopPreventionEnabled()) {
            builder.add(MqttUserProperty.of(HMQ_BRIDGE_HOP_COUNT, Integer.toString(hopCount + 1)));
        }
        return com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties.build(builder);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private @NotNull Mqtt5UserProperties convertUserPropertiesForClient(
            final @NotNull com.hivemq.mqtt.message.mqtt5.Mqtt5UserProperties userProperties) {
        final Mqtt5UserPropertiesBuilder builder = Mqtt5UserProperties.builder();
        for (final MqttUserProperty mqttUserProperty : userProperties.asList()) {
            builder.add(mqttUserProperty.getName(), mqttUserProperty.getValue());
        }
        return builder.build();
    }

    private int extractHopCount(final @NotNull PUBLISH publish) {
        if (!bridge.isLoopPreventionEnabled()) {
            return 0;
        }
        try {
            return publish.getUserProperties().asList().stream()
                    .filter(prop -> prop.getName().equals(HMQ_BRIDGE_HOP_COUNT))
                    .map(prop -> Integer.parseInt(prop.getValue()))
                    .findFirst()
                    .orElse(0);
        } catch (final NumberFormatException e) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Max hop count could not be determined, user property `{}` is not a number",
                        HMQ_BRIDGE_HOP_COUNT);
            }
            return 0;
        }
    }

    @Override
    public void setAfterForwardCallback(final @NotNull AfterForwardCallback callback) {
        afterForwardCallback = callback;
    }

    @Override
    public void setResetInflightMarkerCallback(final @NotNull ResetInflightMarkerCallback callback) {
        resetInflightMarkerCallback = callback;
    }

    @Override
    public void setResetAllInflightMarkersCallback(final @NotNull ResetAllInflightMarkersCallback callback) {
        resetAllInflightMarkersCallback = callback;
    }

    @Override
    public void setOnReconnectCallback(final @NotNull OnReconnectCallback callback) {
        onReconnectCallback = callback;
    }

    @Override
    public void onReconnect() {
        if (log.isDebugEnabled()) {
            log.debug("Forwarder '{}' notified of reconnection for bridge '{}'", id, bridge.getId());
        }
        // Trigger the callback to poll from persistence queue for messages that need to be retried
        final var onReconnectCallback = this.onReconnectCallback;
        if (onReconnectCallback != null) {
            onReconnectCallback.onReconnect();
        }
    }

    @Override
    public void forceReconnect() {
        if (remoteMqttClient.isConnected()) {
            log.warn(
                    "Force reconnect triggered for forwarder '{}' on bridge '{}' - disconnecting to trigger auto-reconnect",
                    id,
                    bridge.getId());
            remoteMqttClient.getMqtt5Client().disconnect();
        } else {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Force reconnect requested but client already disconnected for forwarder '{}' on bridge '{}'",
                        id,
                        bridge.getId());
            }
        }
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
    public void setExecutorService(final @NotNull ExecutorService service) {
        executorService = service;
    }

    private record BufferedPublishInformation(
            @NotNull String queueId,
            String uniqueId,
            @NotNull QoS originalQoS,
            @NotNull PUBLISH publish) {
        private BufferedPublishInformation(
                final @NotNull String queueId,
                final @NotNull String uniqueId,
                final @NotNull QoS originalQoS,
                final @NotNull PUBLISH publish) {
            this.queueId = queueId;
            this.uniqueId = uniqueId;
            this.originalQoS = originalQoS;
            this.publish = publish;
        }
    }

    private record OutflightPublishInformation(
            @NotNull String queueId, @NotNull String uniqueId) {}
}
