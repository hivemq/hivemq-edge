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
 * this intake never needs to mutate in place. Because a closed intake therefore leaves a subscriber-less queue
 * behind on every recreate, those queues are exempt from the broker's orphan cleanup (see
 * {@link #INTERNAL_SHARE_PREFIX}) and are reclaimed explicitly instead.
 */
public final class SouthboundMqttIntake implements AutoCloseable {

    private static final @NotNull Logger log = LoggerFactory.getLogger(SouthboundMqttIntake.class);

    /**
     * The share-name prefix of every queue this intake feeds — the v1 writing service's naming family
     * ({@code adapter-forwarder#}, kept so operators recognize the queues) plus the {@code -v2-} infix that keeps a
     * v1 adapter of the same id from colliding.
     * <p>
     * <b>It is also a protection marker.</b> These are not ordinary shared subscriptions: their consumer is an
     * adapter that can legitimately be absent for a stretch — the whole span of a recreate, from
     * {@link #close() closing} the predecessor's subscriptions to the successor registering its own. The broker's
     * orphan cleanup ({@code ClientQueuePersistenceImpl.cleanUp}) reads "no shared subscriber" as "nobody will ever
     * consume this" and clears the queue, which for these queues means destroying durable commands mid-handoff.
     * Cleanup therefore skips this prefix, and reclamation is explicit instead — the manager discards an adapter's
     * queues when the adapter is removed for good.
     */
    public static final @NotNull String INTERNAL_SHARE_PREFIX = "adapter-forwarder#adapter-writer-v2-";

    private final @NotNull String adapterId;
    private final @NotNull SouthboundBrokerRuntime brokerRuntime;
    private final @NotNull DataPointFactory dataPointFactory;
    private final @NotNull String shareName;
    private final @NotNull String clientId;
    private final @NotNull Map<String, String> queueIdByTag = new LinkedHashMap<>();
    private final @NotNull Map<String, String> topicByTag = new LinkedHashMap<>();

    /**
     * Registers one shared subscription per mapping (first mapping wins for a tag mapped more than once).
     *
     * @param adapterId        the owning adapter's id.
     * @param brokerRuntime    the broker collaborators: topic tree and client queues.
     * @param dataPointFactory builds the values queued publishes are translated into.
     * @param mappings         the adapter's southbound mappings.
     */
    public SouthboundMqttIntake(
            final @NotNull String adapterId,
            final @NotNull SouthboundBrokerRuntime brokerRuntime,
            final @NotNull DataPointFactory dataPointFactory,
            final @NotNull List<SouthboundMappingEntity> mappings) {
        this.adapterId = adapterId;
        this.brokerRuntime = brokerRuntime;
        this.dataPointFactory = dataPointFactory;
        final LocalTopicTree topicTree = brokerRuntime.topicTree();
        this.shareName = shareName(adapterId);
        this.clientId = shareName + "#";
        try {
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
                queueIdByTag.put(tagName, queueId(adapterId, topic));
                topicByTag.put(tagName, topic);
            }
        } catch (final Throwable failure) {
            // A throw partway through the loop leaves the earlier subscriptions registered on a half-built object the
            // constructor never returns — so nobody holds a reference to close() it, and the factory's own cleanup
            // (closeQuietly on a still-null field) is a no-op. Those subscriptions would then feed durable queues for
            // an adapter that does not exist, for the life of the process. Undo them here: only this constructor can.
            removeSubscriptions();
            throw failure;
        }
    }

    /**
     * @param adapterId the owning adapter's id.
     * @return the share name every one of that adapter's southbound queues lives under.
     */
    public static @NotNull String shareName(final @NotNull String adapterId) {
        return INTERNAL_SHARE_PREFIX + adapterId;
    }

    /**
     * The queue id of one southbound mapping — derived from the adapter id and the mapping <b>topic</b> alone, never
     * from the node the tag addresses. That is why a retargeted tag keeps the same queue, and why the id can be
     * rebuilt from configuration without the intake that created it.
     *
     * @param adapterId the owning adapter's id.
     * @param topic     the mapping's command topic.
     * @return the shared-subscription queue id commands on that topic are queued under.
     */
    public static @NotNull String queueId(final @NotNull String adapterId, final @NotNull String topic) {
        return shareName(adapterId) + "/" + topic;
    }

    /**
     * @return the plug for the {@link SouthboundWritePlane}: a durable backlog over the tag's mapping queue. Every
     *         write-mapped tag has one by construction ({@code write-used} derives from the same mappings).
     */
    public @NotNull SouthboundWriteBacklogFactory backlogFactory() {
        return (tagName, node, wrapperSender) -> {
            final String queueId = queueIdByTag.get(tagName);
            final String commandTopic = topicByTag.get(tagName);
            if (queueId == null || commandTopic == null) {
                // write-used derives from the southbound mappings this intake was built from; a channel for a tag
                // with no queue means those two views diverged — fail loudly rather than deliver nothing silently.
                throw new IllegalStateException("tag [" + tagName + "] on adapter [" + adapterId
                        + "] is write-mapped but has no southbound" + " queue");
            }
            return new ClientQueueSouthboundWriteBacklog(
                    brokerRuntime.clientQueuePersistence(),
                    queueId,
                    translator(tagName),
                    adapterId,
                    tagName,
                    wrapperSender);
        };
    }

    /**
     * Remove the subscriptions. The queues and their contents stay — a recreated adapter's intake re-subscribes and
     * its backlogs lease the surviving commands (that is the durability across recreate and restart).
     */
    @Override
    public void close() {
        removeSubscriptions();
    }

    /**
     * Unsubscribe every topic registered so far. Shared by {@link #close()} and the constructor's unwind, so a
     * partially-built intake leaves exactly as little behind as a closed one.
     */
    private void removeSubscriptions() {
        for (final Map.Entry<String, String> entry : topicByTag.entrySet()) {
            try {
                brokerRuntime.topicTree().removeSubscriber(clientId, entry.getValue(), shareName);
            } catch (final Exception failure) {
                // Each subscription is unsubscribed independently: one failure must not leave the rest registered,
                // still feeding queues whose adapter is gone. The container logs this close() as a whole, so the
                // tag and topic are recorded here to say which subscription actually leaked.
                log.warn(
                        "Failed to remove the southbound subscription of tag '{}' (topic '{}') on adapter '{}'",
                        entry.getKey(),
                        entry.getValue(),
                        adapterId,
                        failure);
            }
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
