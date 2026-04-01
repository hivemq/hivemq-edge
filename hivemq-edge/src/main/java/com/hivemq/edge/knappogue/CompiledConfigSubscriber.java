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
package com.hivemq.edge.knappogue;

import com.hivemq.combining.runtime.QueueConsumer;
import com.hivemq.edge.compiler.lib.model.CompiledConfig;
import com.hivemq.edge.compiler.lib.serialization.CompiledConfigSerializer;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Subscribes to the Edge configuration MQTT topic and applies received compiled configs.
 *
 * <p>The topic is configurable via the {@code HIVEMQ_COMPILED_CONFIG_TOPIC} environment variable.
 * Default: {@code $EDGE-CONFIGURATION/-/-/apply} where the two {@code -} segments are placeholders for a future
 * fleet ID and edge ID.
 */
public class CompiledConfigSubscriber {

    private static final @NotNull Logger log = LoggerFactory.getLogger(CompiledConfigSubscriber.class);

    static final @NotNull String DEFAULT_TOPIC = "$EDGE-CONFIGURATION/-/-/apply";
    static final @NotNull String ENV_VAR = "HIVEMQ_COMPILED_CONFIG_TOPIC";

    private static final @NotNull String SUBSCRIBER_ID = "edge-compiled-config-subscriber";
    private static final @NotNull String QUEUE_ID = "edge-compiled-config-queue";
    private static final byte DEFAULT_FLAGS = SubscriptionFlag.getDefaultFlags(true, true, false);

    private final @NotNull CompiledConfigApplier applier;
    private final @NotNull CompiledConfigSerializer serializer;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull AtomicBoolean started = new AtomicBoolean(false);

    public CompiledConfigSubscriber(
            final @NotNull CompiledConfigApplier applier,
            final @NotNull CompiledConfigSerializer serializer,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService) {
        this.applier = applier;
        this.serializer = serializer;
        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
    }

    /**
     * Starts the subscriber. Idempotent — safe to call multiple times; only subscribes once.
     */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        final String topic = resolvedTopic();
        log.info("Subscribing to compiled config topic: {}", topic);

        localTopicTree.addTopic(
                SUBSCRIBER_ID, new Topic(topic, QoS.AT_LEAST_ONCE, false, true), DEFAULT_FLAGS, QUEUE_ID);

        new QueueConsumer(clientQueuePersistence, QUEUE_ID, singleWriterService) {
            @Override
            public void process(final @NotNull PUBLISH publish) {
                handlePayload(publish.getPayload());
            }
        }.start();
    }

    private void handlePayload(final @Nullable byte[] payload) {
        if (payload == null) {
            log.warn("Received compiled config message with null payload — ignoring.");
            return;
        }
        try {
            final String json = new String(payload, StandardCharsets.UTF_8);
            final CompiledConfig compiledConfig = serializer.fromJson(json);
            applier.apply(compiledConfig);
        } catch (final Exception e) {
            log.error("Failed to parse or apply compiled config payload: {}", e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Original exception:", e);
            }
        }
    }

    static @NotNull String resolvedTopic() {
        final String envValue = System.getenv(ENV_VAR);
        return (envValue != null && !envValue.isBlank()) ? envValue : DEFAULT_TOPIC;
    }
}
