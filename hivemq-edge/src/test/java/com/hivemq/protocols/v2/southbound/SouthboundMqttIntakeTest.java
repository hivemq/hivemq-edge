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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.factories.DataPointFactory;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
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
        intake.backlogFactory().create("setpoint", new TestNode("setpoint"), new CapturingSender());
        intake.backlogFactory().create("ramp-rate", new TestNode("ramp-rate"), new CapturingSender());
        assertThat(clientQueue.callbackQueueIds())
                .containsExactlyInAnyOrder(SHARE + "/cmd/setpoint", SHARE + "/cmd/ramp");
    }

    @Test
    void payloadSeam_utf8PayloadBecomesAJsonValue_missingPayloadDeadLetters() {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(1, "{\"value\":42}"));

        final CapturingSender answers = new CapturingSender();
        final SouthboundWriteBacklog backlog =
                intake.backlogFactory().create("setpoint", new TestNode("setpoint"), answers);

        backlog.requestRead(1);
        final SouthboundCommand head = answers.reads().getFirst().command();
        assertThat(head).isNotNull();
        assertThat(head.value().getTagName()).isEqualTo("setpoint");
        assertThat(head.value().getTagValue()).isEqualTo("{\"value\":42}");
        backlog.delete(head.id());

        // A payload-less publish is untranslatable: reported by name, for the delivery side to dead-letter.
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(2, null));
        backlog.requestRead(2);
        assertThat(answers.reads().get(1).command()).isNull();
        assertThat(answers.reads().get(1).undeliverableCommandId()).isNotNull();
        assertThat(clientQueue.removed).hasSize(1); // only the one the delivery side committed
    }

    @Test
    void aTagMappedTwice_keepsOnlyTheFirstMapping() {
        final SouthboundMqttIntake intake =
                newIntake(mapping("cmd/first", "setpoint"), mapping("cmd/second", "setpoint"));

        intake.backlogFactory().create("setpoint", new TestNode("setpoint"), new CapturingSender());

        assertThat(clientQueue.callbackQueueIds()).containsExactly(SHARE + "/cmd/first");
        assertThat(topicTree.getSharedSubscriber(SHARE, "cmd/second")).isEmpty(); // never subscribed
    }

    @Test
    void aWriteMappedTagWithNoQueue_failsLoudly() {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));

        assertThatThrownBy(() ->
                        intake.backlogFactory().create("unmapped", new TestNode("unmapped"), new CapturingSender()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void terminalOutcomes_deleteTheCommandFromTheQueue() {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(1, "a"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(2, "b"));
        final CapturingSender answers = new CapturingSender();
        final SouthboundWriteBacklog backlog =
                intake.backlogFactory().create("setpoint", new TestNode("setpoint"), answers);

        // A commit deletes; so does a dead-letter. The store only removes — which of the two it was is the
        // delivery channel's business.
        backlog.requestRead(1);
        final SouthboundCommand first = answers.reads().getFirst().command();
        assertThat(first).isNotNull();
        backlog.delete(first.id());
        assertThat(clientQueue.removed).containsExactly(first.id());

        backlog.requestRead(2);
        final SouthboundCommand second = answers.reads().get(1).command();
        assertThat(second).isNotNull();
        backlog.delete(second.id());
        assertThat(clientQueue.removed).containsExactly(first.id(), second.id());
    }

    @Test
    void close_removesTheSubscriptions_butLeavesTheQueuesAlone() {
        final SouthboundMqttIntake intake = newIntake(mapping("cmd/setpoint", "setpoint"));
        clientQueue.enqueue(SHARE + "/cmd/setpoint", publish(1, "pending"));

        intake.close();

        assertThat(topicTree.getSharedSubscriber(SHARE, "cmd/setpoint")).isEmpty();
        assertThat(clientQueue.pending(SHARE + "/cmd/setpoint")).isEqualTo(1); // durable contents untouched
    }

    @Test
    void aConstructorFailingPartway_leavesNoSubscriptionBehind() {
        final ThrowingTopicTree throwingTree = new ThrowingTopicTree(2);
        final SouthboundBrokerRuntime runtime = new SouthboundBrokerRuntime(throwingTree, new RecordingClientQueue());

        assertThatThrownBy(() -> new SouthboundMqttIntake(
                        ADAPTER_ID,
                        runtime,
                        dataPointFactory(),
                        List.of(mapping("cmd/first", "setpoint"), mapping("cmd/second", "ramp-rate"))))
                .isInstanceOf(IllegalStateException.class);

        // The subscription registered before the failure must not survive it. The constructor never returns, so no
        // reference to the half-built intake exists anywhere — nothing else could ever close it, and the leaked
        // subscription would keep feeding a durable queue for an adapter that does not exist.
        assertThat(throwingTree.getSharedSubscriber(SHARE, "cmd/first")).isEmpty();
    }

    // ── helpers ─────────────────────────────────────────────────────────────────────────────────────────────────

    /** A real topic tree that refuses the n-th {@code addTopic}, to fail a construction partway through. */
    private static final class ThrowingTopicTree extends LocalTopicTree {

        private final int failOnAdd;
        private int adds;

        private ThrowingTopicTree(final int failOnAdd) {
            super(new MetricsHolder(new MetricRegistry()));
            this.failOnAdd = failOnAdd;
        }

        @Override
        public boolean addTopic(
                final @NotNull String subscriber,
                final @NotNull Topic topic,
                final byte flags,
                final @Nullable String sharedName) {
            if (++adds == failOnAdd) {
                throw new IllegalStateException("scripted topic-tree failure");
            }
            return super.addTopic(subscriber, topic, flags, sharedName);
        }
    }

    private @NotNull SouthboundMqttIntake newIntake(final @NotNull SouthboundMappingEntity... mappings) {
        return new SouthboundMqttIntake(ADAPTER_ID, broker.runtime(), dataPointFactory(), List.of(mappings));
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
