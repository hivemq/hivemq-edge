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
package com.hivemq.protocols.v2.southbound;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One adapter's MQTT front door for southbound writes: for each {@code <southbound-mapping>} it registers a
 * <b>shared subscription</b> on the broker's topic tree, so a publish to the mapped topic lands in a durable
 * client queue — the queue a {@link ClientQueueSouthboundWriteBacklog} then leases from. This is the v2
 * counterpart of the v1 writing service's {@code createSubscription} (same {@code adapter-forwarder#} naming
 * family, {@code -v2-} infix so a v1 adapter with the same id never collides; QoS 2 subscription — QoS ≥ 1 is the
 * durability precondition, QoS 0 commands are at-most-once by broker semantics).
 * <p>
 * {@link #backlogFactory()} is the plug for the {@link SouthboundWritePlane}: each write-mapped tag's channel gets
 * a durable backlog over its mapping's queue. A tag referenced by <b>several</b> southbound mappings keeps only
 * the first (warned observably): one tag = one delivery queue; multi-queue fan-in to a single-in-flight tag is a
 * deliberately deferred concern. Because {@code write-used} is derived from these same mappings, every
 * write-mapped tag is guaranteed a queue here.
 * <p>
 * The payload seam is minimal for now: the publish payload is decoded as UTF-8 and carried as a JSON-flagged
 * {@link com.hivemq.adapter.sdk.api.data.DataPoint} — schema validation and field mapping (v1: DataHub policies)
 * are not this class's concern. A payload-less publish is untranslatable and dead-letters.
 * <p>
 * Closing removes the subscriptions; the queues and their contents are deliberately left in place — they are the
 * durability. Southbound-mapping <b>changes</b> recreate the adapter (they classify as connection-critical), so
 * this intake never needs to mutate in place.
 */
public final class SouthboundMqttIntake implements AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SouthboundMqttIntake.class);

    /** The v1 writing service's naming family, kept so operators recognize the queues. */
    private static final @NotNull String SHARE_PREFIX = "adapter-forwarder#";

    private final @NotNull String adapterId;
    private final @NotNull LocalTopicTree topicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull String shareName;
    private final @NotNull String clientId;
    private final @NotNull Map<String, String> queueIdByTag = new LinkedHashMap<>();
    private final @NotNull Map<String, String> topicByTag = new LinkedHashMap<>();

    /**
     * Registers one shared subscription per mapping (first mapping wins for a tag mapped more than once).
     *
     * @param adapterId              the owning adapter's id.
     * @param topicTree              the broker topic tree subscriptions are registered on.
     * @param clientQueuePersistence the durable client queue store backlogs lease from.
     * @param dataPointFactory       builds the values queued publishes are translated into.
     * @param mappings               the adapter's southbound mappings.
     */
    public SouthboundMqttIntake(
            final @NotNull String adapterId,
            final @NotNull LocalTopicTree topicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull List<SouthboundMappingEntity> mappings) {
        this.adapterId = adapterId;
        this.topicTree = topicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.dataPointFactory = dataPointFactory;
        this.shareName = SHARE_PREFIX + "adapter-writer-v2-" + adapterId;
        this.clientId = shareName + "#";
        for (final SouthboundMappingEntity mapping : mappings) {
            final String tagName = mapping.getTagName();
            if (queueIdByTag.containsKey(tagName)) {
                log.warn(
                        "Tag '{}' on adapter '{}' is referenced by more than one southbound mapping; only the first "
                                + "(topic '{}') delivers — the mapping on topic '{}' is ignored",
                        tagName,
                        adapterId,
                        topicByTag.get(tagName),
                        mapping.getTopic());
                continue;
            }
            final String topic = mapping.getTopic();
            topicTree.addTopic(
                    clientId,
                    new Topic(topic, QoS.EXACTLY_ONCE, false, true),
                    SubscriptionFlag.getDefaultFlags(true, true, false),
                    shareName);
            queueIdByTag.put(tagName, shareName + "/" + topic);
            topicByTag.put(tagName, topic);
        }
    }

    /**
     * @return the plug for the {@link SouthboundWritePlane}: a durable backlog over the tag's mapping queue. Every
     *         write-mapped tag has one by construction ({@code write-used} derives from the same mappings).
     */
    public @NotNull SouthboundWriteBacklogFactory backlogFactory() {
        return (tagName, node) -> {
            final String queueId = queueIdByTag.get(tagName);
            if (queueId == null) {
                // write-used derives from the southbound mappings this intake was built from; a channel for a tag
                // with no queue means those two views diverged — fail loudly rather than deliver nothing silently.
                throw new IllegalStateException(
                        "tag [" + tagName + "] on adapter [" + adapterId + "] is write-mapped but has no southbound "
                                + "queue");
            }
            return new ClientQueueSouthboundWriteBacklog(
                    clientQueuePersistence, queueId, translator(tagName), adapterId, tagName);
        };
    }

    /**
     * Remove the subscriptions. The queues and their contents stay — a recreated adapter's intake re-subscribes and
     * its backlogs lease the surviving commands (that is the durability across recreate and restart).
     */
    @Override
    public void close() {
        for (final Map.Entry<String, String> entry : topicByTag.entrySet()) {
            topicTree.removeSubscriber(clientId, entry.getValue(), shareName);
        }
    }

    /** The minimal payload seam: UTF-8 payload carried as a JSON-flagged value; no payload → untranslatable. */
    private @NotNull SouthboundPublishTranslator translator(final @NotNull String tagName) {
        return publish -> {
            final byte[] payload = publish.getPayload();
            if (payload == null) {
                return null;
            }
            return dataPointFactory.createJsonDataPoint(tagName, new String(payload, UTF_8));
        };
    }
}
