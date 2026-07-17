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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.protocols.v2.config.SouthboundMappingEntity;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/**
 * The {@link SouthboundMqttIntake} over a real {@link LocalTopicTree} and the scripted client-queue stand-in: one
 * shared subscription per mapping, a durable backlog per write-mapped tag leasing from the mapping's queue with the
 * UTF-8→JSON-value payload seam, first-mapping-wins for a doubly-mapped tag, a loud failure for a tag with no
 * queue, and subscription removal on close.
 */
class SouthboundMqttIntakeTest {

    private static final @NotNull String ADAPTER_ID = "a1";
    private static final @NotNull String SHARE = "adapter-forwarder#adapter-writer-v2-" + ADAPTER_ID;

    private final @NotNull RecordingBrokerRuntime broker = new RecordingBrokerRuntime();
    private final @NotNull LocalTopicTree topicTree = broker.topicTree;
    private final @NotNull RecordingClientQueue clientQueue = broker.clientQueue;

    @Test
    void oneSharedSubscriptionPerMapping_andTheBacklogLeasesFromTheMappingQueue() {
        final SouthboundMqttIntake intake =
                newIntake(mapping("cmd/setpoint", "setpoint"), mapping("cmd/ramp", "ramp-rate"));

        // The share subscribes both topics — a publish to either finds the shared subscriber.
        assertThat(topicTree.getSharedSubscriber(SHARE, "cmd/setpoint")).isNotEmpty();
        assertThat(topicTree.getSharedSubscriber(SHARE, "cmd/ramp")).isNotEmpty();

        // Each write-mapped tag's backlog registers on its own mapping's queue id.
        intake.backlogFactory().create("setpoint", new TestNode("setpoint"));
        intake.backlogFactory().create("ramp-rate", new TestNode("ramp-rate"));
        assertThat(clientQueue.callbackQueueIds())
                .containsExactlyInAnyOrder(SHARE + "/cmd/setpoint", SHARE + "/cmd/ramp");
    }

    @Test
    void payloadSeam_utf8PayloadBecomesAJsonValue_missingPayloadDeadLetters() {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(1, "{\"value\":42}"));

        final SouthboundWriteBacklog backlog = intake.backlogFactory().create("setpoint", new TestNode("setpoint"));

        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagName()).isEqualTo("setpoint");
        assertThat(head.value().getTagValue()).isEqualTo("{\"value\":42}");
        backlog.removeHead(head.id());

        // A payload-less publish is untranslatable: self-dead-lettered, never delivered.
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(2, null));
        clientQueue.firePublishAvailable(SHARE + "/cmd/setpoint");
        assertThat(backlog.head()).isNull();
        assertThat(clientQueue.removed).hasSize(2); // the delivered one and the dead-lettered one
    }

    @Test
    void aTagMappedTwice_keepsOnlyTheFirstMapping() {
        final SouthboundMqttIntake intake =
                newIntake(mapping("cmd/first", "setpoint"), mapping("cmd/second", "setpoint"));

        intake.backlogFactory().create("setpoint", new TestNode("setpoint"));

        assertThat(clientQueue.callbackQueueIds()).containsExactly(SHARE + "/cmd/first");
        assertThat(topicTree.getSharedSubscriber(SHARE, "cmd/second")).isEmpty(); // never subscribed
    }

    @Test
    void aWriteMappedTagWithNoQueue_failsLoudly() {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));

        assertThatThrownBy(() -> intake.backlogFactory().create("unmapped", new TestNode("unmapped")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void terminalOutcomes_publishRetainedVerdicts_onTheResultTopic() throws Exception {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(1, "a"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(2, "b"));
        final SouthboundWriteBacklog backlog = intake.backlogFactory().create("setpoint", new TestNode("setpoint"));

        // Success: a retained QoS 1 verdict on the /result sibling with the full correlation payload.
        final SouthboundCommand first = backlog.head();
        assertThat(first).isNotNull();
        backlog.removeHead(first.id());
        assertThat(broker.publishService.published).hasSize(1);
        final PUBLISH successVerdict = broker.publishService.published.getFirst();
        assertThat(successVerdict.getTopic()).isEqualTo("cmd/setpoint/result");
        assertThat(successVerdict.isRetain()).isTrue();
        assertThat(successVerdict.getQoS()).isEqualTo(QoS.AT_LEAST_ONCE);
        final var success = new ObjectMapper().readTree(successVerdict.getPayload());
        assertThat(success.get("adapterId").asText()).isEqualTo(ADAPTER_ID);
        assertThat(success.get("tag").asText()).isEqualTo("setpoint");
        assertThat(success.get("commandTopic").asText()).isEqualTo("cmd/setpoint");
        assertThat(success.get("commandId").asText()).isEqualTo(first.id());
        assertThat(success.get("outcome").asText()).isEqualTo("SUCCEEDED");
        assertThat(success.get("reason").isNull()).isTrue();
        assertThat(success.get("deduplicated").asBoolean()).isFalse();
        // The echo: what was executed, so a publisher recognizes its own command's reply.
        assertThat(success.get("command").asText()).isEqualTo("a");
        assertThat(success.get("correlationData").isNull()).isTrue(); // an MQTT 3 command carries none
        assertThat(success.get("completedAtMillis").asLong()).isPositive();

        // Device rejection: a FAILED verdict carrying the reason.
        final SouthboundCommand second = backlog.head();
        assertThat(second).isNotNull();
        backlog.deadLetterHead(second.id(), "device rejected the value");
        final var failure = new ObjectMapper()
                .readTree(broker.publishService.published.get(1).getPayload());
        assertThat(failure.get("outcome").asText()).isEqualTo("FAILED");
        assertThat(failure.get("reason").asText()).isEqualTo("device rejected the value");
    }

    @Test
    void crashReplayedCommand_isRecognizedByTheRetainedVerdict_andRecommittedWithoutExecuting() throws Exception {
        // The crash window: the device executed command 1 and its verdict was retained, but the crash landed before
        // the command was deleted from the durable queue — on restart it is still there, at the head.
        final PUBLISH replayed = publish(1, "a");
        clientQueue.enqueue(SHARE + "/cmd/setpoint", replayed);
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(2, "b"));
        final String verdictJson = "{\"commandId\":\"" + replayed.getUniqueId() + "\",\"outcome\":\"SUCCEEDED\"}";
        broker.retainedStore.retained.put(
                "cmd/setpoint/result", new RetainedMessage(verdictJson.getBytes(UTF_8), QoS.AT_LEAST_ONCE, 1L, 3600));

        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        final SouthboundWriteBacklog backlog = intake.backlogFactory().create("setpoint", new TestNode("setpoint"));

        // The replayed command never surfaces: it is re-committed and its verdict re-reported as deduplicated;
        // the next command leases normally.
        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("b");
        assertThat(clientQueue.removed).containsExactly(replayed.getUniqueId());
        assertThat(broker.publishService.published).hasSize(1);
        final var dedupVerdict = new ObjectMapper()
                .readTree(broker.publishService.published.getFirst().getPayload());
        assertThat(dedupVerdict.get("commandId").asText()).isEqualTo(replayed.getUniqueId());
        assertThat(dedupVerdict.get("outcome").asText()).isEqualTo("SUCCEEDED");
        assertThat(dedupVerdict.get("deduplicated").asBoolean()).isTrue();
    }

    @Test
    void aNonMatchingRetainedVerdict_neverSuppressesTheHead() {
        // The retained verdict names an older, already-deleted command: the head was never executed and must run.
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(7, "fresh"));
        broker.retainedStore.retained.put(
                "cmd/setpoint/result",
                new RetainedMessage(
                        "{\"commandId\":\"some-older-command\",\"outcome\":\"SUCCEEDED\"}".getBytes(UTF_8),
                        QoS.AT_LEAST_ONCE,
                        1L,
                        3600));

        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        final SouthboundWriteBacklog backlog = intake.backlogFactory().create("setpoint", new TestNode("setpoint"));

        final SouthboundCommand head = backlog.head();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagValue()).isEqualTo("fresh");
        assertThat(clientQueue.removed).isEmpty();
        assertThat(broker.publishService.published).isEmpty();
    }

    @Test
    void close_removesTheSubscriptions_butLeavesTheQueuesAlone() {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(1, "pending"));

        intake.close();

        assertThat(topicTree.getSharedSubscriber(SHARE, "cmd/setpoint")).isEmpty();
        assertThat(clientQueue.pending(SHARE + "/cmd/setpoint")).isEqualTo(1); // durable contents untouched
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    private @NotNull SouthboundMqttIntake newIntake(final @NotNull SouthboundMappingEntity... mappings) {
        return new SouthboundMqttIntake(
                ADAPTER_ID, broker.runtime(), dataPointFactory(), new ObjectMapper(), List.of(mappings));
    }

    private static @NotNull SouthboundMappingEntity mapping(final @NotNull String topic, final @NotNull String tag) {
        return new SouthboundMappingEntity(topic, tag);
    }

    private static @NotNull DataPointFactory dataPointFactory() {
        return new DataPointFactory() {
            @Override
            public @NotNull DataPoint create(final @NotNull String tagName, final @NotNull Object tagValue) {
                return new TestDataPoint(tagName, tagValue);
            }

            @Override
            public @NotNull DataPoint createJsonDataPoint(
                    final @NotNull String tagName, final @NotNull Object tagValue) {
                return new TestDataPoint(tagName, tagValue);
            }
        };
    }

    private static @NotNull PUBLISH publish(final long publishId, final @Nullable String payload) {
        final PUBLISHFactory.Mqtt3Builder builder = new PUBLISHFactory.Mqtt3Builder()
                .withQoS(QoS.AT_LEAST_ONCE)
                .withOnwardQos(QoS.AT_LEAST_ONCE)
                .withTopic("cmd/setpoint")
                .withPublishId(publishId)
                .withHivemqId("hivemqId");
        builder.withPayload(payload == null ? null : payload.getBytes(UTF_8));
        return builder.build();
    }
}
