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
package com.hivemq.combining.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.combining.model.DataCombiningDestination;
import com.hivemq.combining.model.DataCombiningSources;
import com.hivemq.combining.model.DataIdentifierReference;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.ProducerQueues;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import com.hivemq.persistence.mappings.fieldmapping.Instruction;
import com.hivemq.protocols.northbound.TagConsumer;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DataCombiningRuntimeTest {

    private @NonNull LocalTopicTree localTopicTree;
    private @NonNull TagManager tagManager;
    private @NonNull ClientQueuePersistence clientQueuePersistence;
    private @NonNull SingleWriterService singleWriterService;
    private @NonNull DataCombiningPublishService dataCombiningPublishService;
    private @NonNull DataCombiningTransformationService dataCombiningTransformationService;

    @BeforeEach
    void setUp() {
        localTopicTree = mock(LocalTopicTree.class);
        tagManager = mock(TagManager.class);
        clientQueuePersistence = mock(ClientQueuePersistence.class);
        singleWriterService = mock(SingleWriterService.class);
        final ProducerQueues producerQueues = mock(ProducerQueues.class);
        when(singleWriterService.getQueuedMessagesQueue()).thenReturn(producerQueues);
        dataCombiningPublishService = mock(DataCombiningPublishService.class);
        dataCombiningTransformationService = mock(DataCombiningTransformationService.class);
    }

    private DataCombiningRuntime createRuntime(final DataCombining combining) {
        return new DataCombiningRuntime(
                combining,
                localTopicTree,
                tagManager,
                clientQueuePersistence,
                singleWriterService,
                dataCombiningPublishService,
                dataCombiningTransformationService);
    }

    /*
     * Verifies that start() with an empty instruction list and a TAG primary
     * subscribes the primary tag via TagManager.addConsumer().
     * The transformation script should also be registered.
     * The captured consumer should have the correct tag name and scope (adapter ID).
     * Since this is the primary, accepting data should trigger a publish.
     */
    @Test
    void start_whenNoInstructions_andPrimaryIsTag_thenSubscribesPrimaryTag() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        verify(dataCombiningTransformationService).addScriptForDataCombining(combining);
        final ArgumentCaptor<TagConsumer> captor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager).addConsumer(captor.capture());
        assertThat(captor.getValue().getTagName()).isEqualTo("tag1");
        assertThat(captor.getValue().getScope()).isEqualTo("adapter1");

        // The only consumer is the primary — accepting data should trigger a publish
        captor.getValue().accept(List.of(new DataPointImpl("tag1", "{\"v\":1}")));
        verify(dataCombiningPublishService).publish(any(), any(), eq(combining));
    }

    /*
     * Verifies that start() with an empty instruction list and a TOPIC_FILTER primary
     * subscribes the primary via LocalTopicTree.addTopic() instead of TagManager.
     * No tag consumer should be registered since the primary is not a TAG.
     * The transformation script should still be registered.
     */
    @Test
    void start_whenNoInstructions_andPrimaryIsTopicFilter_thenSubscribesPrimaryTopicFilter() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of(), List.of("sensor/temp")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        verify(dataCombiningTransformationService).addScriptForDataCombining(combining);
        verify(localTopicTree).addTopic(anyString(), any(Topic.class), anyByte(), anyString());
        verify(tagManager, never()).addConsumer(any());
    }

    /*
     * Verifies that when an instruction references a TAG different from the primary,
     * both the instruction's tag and the primary tag are subscribed separately.
     * The instruction tag (tag2/adapter2) should be subscribed first,
     * followed by the primary tag (tag1/adapter1), resulting in 2 addConsumer calls.
     * Only the primary (last) consumer should trigger a publish on accept.
     */
    @Test
    void start_whenInstructionHasTagRef_notPrimary_thenSubscribesBoth() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataIdentifierReference instructionRef =
                new DataIdentifierReference("tag2", DataIdentifierReference.Type.TAG, "adapter2");
        final Instruction instruction = new Instruction("$.value", "output", instructionRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1", "tag2"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        final ArgumentCaptor<TagConsumer> captor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager, times(2)).addConsumer(captor.capture());
        final List<TagConsumer> consumers = captor.getAllValues();
        // First is the instruction tag (non-primary), second (last) is the primary
        assertThat(consumers.get(0).getTagName()).isEqualTo("tag2");
        assertThat(consumers.get(0).getScope()).isEqualTo("adapter2");
        assertThat(consumers.get(1).getTagName()).isEqualTo("tag1");
        assertThat(consumers.get(1).getScope()).isEqualTo("adapter1");

        // Non-primary consumer should NOT trigger a publish
        consumers.get(0).accept(List.of(new DataPointImpl("tag2", "{\"v\":1}")));
        verify(dataCombiningPublishService, never()).publish(any(), any(), any());

        // Primary consumer (last) should trigger a publish
        consumers.get(1).accept(List.of(new DataPointImpl("tag1", "{\"v\":2}")));
        verify(dataCombiningPublishService).publish(any(), any(), eq(combining));
    }

    /*
     * Verifies deduplication: when an instruction's DataIdentifierReference is equal
     * to the primary reference (same id, type, and scope), the stream filters it out
     * via .filter(ref -> !ref.equals(primaryRef)). Only the final subscribe(primaryRef)
     * call at the end of start() registers the consumer, so addConsumer is called once.
     */
    @Test
    void start_whenInstructionRefEqualsPrimary_thenSubscribesPrimaryOnlyOnce() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        // Same ref as primary
        final DataIdentifierReference instructionRef =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final Instruction instruction = new Instruction("$.value", "output", instructionRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // The instruction ref equals primary, so it's filtered out by the stream.
        // Only the primary subscribe call at the end should add a consumer.
        verify(tagManager, times(1)).addConsumer(any());
    }

    /*
     * Verifies that when an instruction references a TOPIC_FILTER while the primary is a TAG,
     * both subscription types are created: TagManager.addConsumer for the primary tag
     * and LocalTopicTree.addTopic for the instruction's topic filter.
     * This tests the mixed-type subscription path in start().
     */
    @Test
    void start_whenInstructionHasTopicFilterRef_thenSubscribesTopicFilter() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataIdentifierReference instructionRef =
                new DataIdentifierReference("sensor/#", DataIdentifierReference.Type.TOPIC_FILTER);
        final Instruction instruction = new Instruction("$.value", "output", instructionRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of("sensor/#")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // One tag consumer for primary, one topic filter subscription for instruction
        verify(tagManager, times(1)).addConsumer(any());
        verify(localTopicTree, times(1)).addTopic(anyString(), any(Topic.class), anyByte(), anyString());
    }

    /*
     * Verifies that instructions with a null DataIdentifierReference are safely ignored.
     * The stream's .filter(Objects::nonNull) skips null refs, so only the primary
     * tag is subscribed. This ensures no NullPointerException is thrown
     * when an instruction has no sourceRef configured.
     */
    @Test
    void start_whenInstructionRefIsNull_thenIgnored() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final Instruction instruction = new Instruction("$.value", "output", null);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // Only primary tag should be subscribed
        verify(tagManager, times(1)).addConsumer(any());
    }

    /*
     * Verifies that .distinct() in the instruction stream deduplicates identical refs.
     * Two instructions both reference the same tag2/adapter2, but only one consumer
     * is created for it. Combined with the primary tag1/adapter1, there should be
     * exactly 2 addConsumer calls total, not 3.
     * The primary (tag1) must be the last subscribed and the only one triggering publish.
     */
    @Test
    void start_whenDuplicateInstructionRefs_thenSubscribedOnce() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataIdentifierReference instructionRef =
                new DataIdentifierReference("tag2", DataIdentifierReference.Type.TAG, "adapter2");
        final Instruction instruction1 = new Instruction("$.value1", "output1", instructionRef);
        final Instruction instruction2 = new Instruction("$.value2", "output2", instructionRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1", "tag2"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction1, instruction2));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // tag2 appears twice in instructions but .distinct() deduplicates.
        // 1 for tag2 (instruction), 1 for tag1 (primary)
        final ArgumentCaptor<TagConsumer> captor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager, times(2)).addConsumer(captor.capture());
        final List<TagConsumer> consumers = captor.getAllValues();

        // Primary (tag1) must be the last consumer
        assertThat(consumers.get(0).getTagName()).isEqualTo("tag2");
        assertThat(consumers.get(1).getTagName()).isEqualTo("tag1");

        // Non-primary consumer should NOT trigger publish
        consumers.get(0).accept(List.of(new DataPointImpl("tag2", "{\"v\":1}")));
        verify(dataCombiningPublishService, never()).publish(any(), any(), any());

        // Primary consumer (last) should trigger publish
        consumers.get(1).accept(List.of(new DataPointImpl("tag1", "{\"v\":2}")));
        verify(dataCombiningPublishService).publish(any(), any(), eq(combining));
    }

    /*
     * Verifies complex scenario: 3 instructions with distinct refs (2 TAGs, 1 TOPIC_FILTER)
     * and a TOPIC_FILTER primary. The 2 TAG refs go through TagManager (2 addConsumer calls).
     * The instruction TOPIC_FILTER (other/#) and primary TOPIC_FILTER (sensor/temp) each create
     * a subscription in LocalTopicTree (2 addTopic calls).
     * The primary TOPIC_FILTER is not in the instruction stream since none match it.
     * All tag consumers are non-primary and should NOT trigger publish on accept.
     */
    @Test
    void start_whenMultipleDistinctInstructionRefs_thenAllSubscribed() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataIdentifierReference tagRef1 =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataIdentifierReference tagRef2 =
                new DataIdentifierReference("tag2", DataIdentifierReference.Type.TAG, "adapter2");
        final DataIdentifierReference topicRef =
                new DataIdentifierReference("other/#", DataIdentifierReference.Type.TOPIC_FILTER);
        final Instruction i1 = new Instruction("$.v1", "o1", tagRef1);
        final Instruction i2 = new Instruction("$.v2", "o2", tagRef2);
        final Instruction i3 = new Instruction("$.v3", "o3", topicRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1", "tag2"), List.of("sensor/temp", "other/#")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(i1, i2, i3));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // 2 tag consumers for instructions (tag1, tag2)
        final ArgumentCaptor<TagConsumer> captor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager, times(2)).addConsumer(captor.capture());
        // 2 topic filter subscriptions: one for instruction (other/#), one for primary (sensor/temp)
        verify(localTopicTree, times(2)).addTopic(anyString(), any(Topic.class), anyByte(), anyString());

        // Both tag consumers are non-primary (primary is a TOPIC_FILTER), so neither triggers publish
        for (final TagConsumer consumer : captor.getAllValues()) {
            consumer.accept(List.of(new DataPointImpl(consumer.getTagName(), "{\"v\":1}")));
        }
        verify(dataCombiningPublishService, never()).publish(any(), any(), any());
    }

    /*
     * Verifies that stop() removes all tag consumers that were registered during start().
     * The same consumer instance added via addConsumer should be passed to removeConsumer.
     * The transformation script should also be removed via removeScriptForDataCombining.
     */
    @Test
    void stop_whenStartedWithTags_thenConsumersRemoved() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();
        runtime.stop();

        final ArgumentCaptor<TagConsumer> addCaptor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager).addConsumer(addCaptor.capture());
        final ArgumentCaptor<TagConsumer> removeCaptor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager).removeConsumer(removeCaptor.capture());
        assertThat(addCaptor.getValue()).isEqualTo(removeCaptor.getValue());
        verify(dataCombiningTransformationService).removeScriptForDataCombining(combining);
    }

    /*
     * Verifies that stop() cleans up topic filter subscriptions created during start().
     * The QueueConsumer is closed and the subscriber is removed from LocalTopicTree
     * via removeSubscriber(subscriber, topic, sharedName).
     * The transformation script is also removed.
     */
    @Test
    void stop_whenStartedWithTopicFilters_thenSubscriptionsRemoved() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of(), List.of("sensor/temp")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();
        runtime.stop();

        verify(localTopicTree).removeSubscriber(anyString(), anyString(), anyString());
        verify(dataCombiningTransformationService).removeScriptForDataCombining(combining);
    }

    /*
     * Verifies that stop() cleans up both tag consumers and topic filter subscriptions
     * when both types were created during start(). The primary is a TAG (removed via
     * tagManager.removeConsumer) and an instruction creates a TOPIC_FILTER subscription
     * (removed via localTopicTree.removeSubscriber). The script is also removed.
     */
    @Test
    void stop_whenStartedWithTagsAndTopicFilters_thenAllCleaned() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataIdentifierReference topicRef =
                new DataIdentifierReference("sensor/#", DataIdentifierReference.Type.TOPIC_FILTER);
        final Instruction instruction = new Instruction("$.value", "output", topicRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of("sensor/#")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();
        runtime.stop();

        verify(tagManager).removeConsumer(any());
        verify(localTopicTree).removeSubscriber(anyString(), anyString(), anyString());
        verify(dataCombiningTransformationService).removeScriptForDataCombining(combining);
    }

    /*
     * Verifies that calling stop() without a prior start() does not throw exceptions.
     * Since no consumers or subscriptions were created, removeConsumer and removeSubscriber
     * should not be called. However, removeScriptForDataCombining is always called
     * because stop() unconditionally removes the script.
     */
    @Test
    void stop_whenNotStarted_thenNoErrors() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.stop();

        verify(tagManager, never()).removeConsumer(any());
        verify(localTopicTree, never()).removeSubscriber(anyString(), anyString(), anyString());
        verify(dataCombiningTransformationService).removeScriptForDataCombining(combining);
    }

    /*
     * Verifies idempotency: calling stop() twice after start() should only remove the
     * consumer once. The first stop() clears the internal consumers list, so the second
     * stop() iterates over an empty list and does nothing for consumers.
     * removeScriptForDataCombining is called twice since it's unconditional.
     */
    @Test
    void stop_calledTwice_thenSecondStopIsNoop() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();
        runtime.stop();
        runtime.stop();

        // removeConsumer called once during first stop, then lists are cleared
        verify(tagManager, times(1)).removeConsumer(any());
        verify(dataCombiningTransformationService, times(2)).removeScriptForDataCombining(combining);
    }

    /*
     * Verifies that triggerPublish() with no tag results and no topic filter publishes
     * produces an empty JSON object "{}"`. The DataCombiningPublishService.publish() is
     * called with the correct destination, the empty JSON payload bytes, and the
     * DataCombining instance for transformation.
     */
    @Test
    void triggerPublish_whenNoData_thenPublishesEmptyJson() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataCombiningDestination destination = new DataCombiningDestination(null, "dest/topic", "{}");
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                destination,
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.triggerPublish(combining);

        final ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(dataCombiningPublishService).publish(eq(destination), payloadCaptor.capture(), eq(combining));
        assertThat(new String(payloadCaptor.getValue())).isEqualTo("{}");
    }

    /*
     * Verifies that tag data fed via InternalTagConsumer.accept() is included in the
     * published payload. After start(), we capture the registered primary tag consumer,
     * feed it a JSON data point. Since it's the primary, accept() triggers triggerPublish()
     * automatically. The resulting payload should contain the fully qualified key
     * "adapter1/TAG:tag1" mapping to the data point value.
     */
    @Test
    void triggerPublish_whenTagDataPresent_thenIncludedInPayload() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataCombiningDestination destination = new DataCombiningDestination(null, "dest/topic", "{}");
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                destination,
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // Capture the primary tag consumer and feed data to it
        final ArgumentCaptor<TagConsumer> consumerCaptor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager).addConsumer(consumerCaptor.capture());
        final TagConsumer consumer = consumerCaptor.getValue();

        // accept() on the primary consumer triggers triggerPublish() automatically
        final DataPoint dataPoint = new DataPointImpl("tag1", "{\"temperature\":25}");
        consumer.accept(List.of(dataPoint));

        final ArgumentCaptor<byte[]> payloadCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(dataCombiningPublishService).publish(eq(destination), payloadCaptor.capture(), eq(combining));
        final String payload = new String(payloadCaptor.getValue());
        // The key should be the fully qualified name: adapter1/TAG:tag1
        assertThat(payload).contains("adapter1/TAG:tag1");
    }

    /*
     * Verifies that InternalTagConsumer exposes the correct tag name and scope
     * through the TagConsumer interface methods getTagName() and getScope().
     * These are used by TagManager to route data points to the correct consumer
     * based on the adapter-scoped tag identity.
     */
    @Test
    void internalTagConsumer_whenGetTagName_thenReturnsCorrectName() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of()),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        final ArgumentCaptor<TagConsumer> captor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager).addConsumer(captor.capture());
        assertThat(captor.getValue().getTagName()).isEqualTo("tag1");
        assertThat(captor.getValue().getScope()).isEqualTo("adapter1");
    }

    /*
     * Verifies that subscribeTopicFilter() creates an InternalSubscription with the
     * correct subscriber (combiningId + "#"), topic, and sharedName (combiningId).
     * It should also register the topic in LocalTopicTree via addTopic() with
     * the matching subscriber and shared name parameters.
     */
    @Test
    void subscribeTopicFilter_thenAddsTopicToTree() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final UUID combiningId = UUID.randomUUID();
        final DataCombining combining = new DataCombining(
                combiningId,
                new DataCombiningSources(primary, List.of(), List.of("sensor/temp")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        final DataCombiningRuntime.InternalSubscription subscription =
                runtime.subscribeTopicFilter(combining, "sensor/temp", false);

        assertThat(subscription.subscriber()).isEqualTo(combiningId + "#");
        assertThat(subscription.topic()).isEqualTo("sensor/temp");
        assertThat(subscription.sharedName()).isEqualTo(combiningId.toString());

        verify(localTopicTree).addTopic(eq(combiningId + "#"), any(Topic.class), anyByte(), eq(combiningId.toString()));
    }

    /*
     * Verifies that InternalSubscription.getQueueId() returns the format
     * "sharedName/topic" which is "combiningId/sensor/temp".
     * This queue ID is used by ClientQueuePersistence to identify the shared
     * subscription queue for reading and acknowledging messages.
     */
    @Test
    void internalSubscription_getQueueId_thenReturnsCorrectFormat() {
        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final UUID combiningId = UUID.randomUUID();
        final DataCombining combining = new DataCombining(
                combiningId,
                new DataCombiningSources(primary, List.of(), List.of("sensor/temp")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        final DataCombiningRuntime.InternalSubscription subscription =
                runtime.subscribeTopicFilter(combining, "sensor/temp", false);

        assertThat(subscription.getQueueId()).isEqualTo(combiningId + "/sensor/temp");
    }

    // --- Topic filter primary tests (via start()) ---

    /*
     * Sets up mocks so that QueueConsumer polling works synchronously during start().
     * ProducerQueues.submit() executes the task inline, readShared() returns a PUBLISH
     * on every other call (first call per consumer) and empty on the rest.
     * removeShared() is stubbed to avoid NPEs during message acknowledgment.
     */
    private void setupSynchronousPolling(final PUBLISH publishToDeliver) {
        final ProducerQueues producerQueues = mock(ProducerQueues.class);
        when(singleWriterService.getQueuedMessagesQueue()).thenReturn(producerQueues);
        when(producerQueues.submit(anyString(), any(SingleWriterService.Task.class)))
                .thenAnswer(invocation -> {
                    final SingleWriterService.Task<?> task = invocation.getArgument(1);
                    task.doTask(0);
                    return Futures.immediateFuture(null);
                });

        // Each queue consumer calls readShared twice: once getting the PUBLISH, once getting empty.
        // Use alternating pattern: odd calls return PUBLISH, even calls return empty.
        final AtomicInteger readCount = new AtomicInteger(0);
        when(clientQueuePersistence.readShared(anyString(), anyInt(), anyLong()))
                .thenAnswer(invocation -> {
                    if (readCount.getAndIncrement() % 2 == 0) {
                        return Futures.immediateFuture(ImmutableList.of(publishToDeliver));
                    }
                    return Futures.immediateFuture(ImmutableList.of());
                });

        when(clientQueuePersistence.removeShared(anyString(), anyString())).thenReturn(Futures.immediateFuture(null));
    }

    private PUBLISH createMockPublish() {
        final PUBLISH publish = mock(PUBLISH.class);
        when(publish.getPayload()).thenReturn("{\"v\":1}".getBytes());
        when(publish.getUniqueId()).thenReturn("unique-" + UUID.randomUUID());
        when(publish.getQoS()).thenReturn(QoS.AT_LEAST_ONCE);
        return publish;
    }

    /*
     * Verifies via start() that when the primary is a TOPIC_FILTER with no instructions,
     * the queue consumer receives a message and triggers dataCombiningPublishService.publish().
     * The synchronous polling mock delivers a PUBLISH during start(), which flows through
     * pollAndForward → processPublish → process (isPrimary=true) → triggerPublish.
     */
    @Test
    void start_whenPrimaryIsTopicFilter_noInstructions_thenPublishTriggered() {
        final PUBLISH mqttPublish = createMockPublish();
        setupSynchronousPolling(mqttPublish);

        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of(), List.of("sensor/temp")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of());

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // The primary queue consumer polled, received the PUBLISH, and triggered publish
        verify(dataCombiningPublishService).publish(any(), any(), eq(combining));
    }

    /*
     * Verifies via start() that when the primary is a TOPIC_FILTER and an instruction
     * references a TAG, the TAG consumer does not trigger publish on accept(), but the
     * primary TOPIC_FILTER does trigger publish when it receives a message via polling.
     * This validates the full end-to-end flow through start() for mixed types.
     */
    @Test
    void start_whenPrimaryIsTopicFilter_withTagInstruction_thenTagDoesNotTriggerButTopicFilterDoes() {
        final PUBLISH mqttPublish = createMockPublish();
        setupSynchronousPolling(mqttPublish);

        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataIdentifierReference tagRef =
                new DataIdentifierReference("tag1", DataIdentifierReference.Type.TAG, "adapter1");
        final Instruction instruction = new Instruction("$.value", "output", tagRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of("tag1"), List.of("sensor/temp")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // The primary TOPIC_FILTER triggered publish via polling during start()
        verify(dataCombiningPublishService).publish(any(), any(), eq(combining));

        // The TAG consumer is non-primary — accepting data should NOT trigger another publish
        final ArgumentCaptor<TagConsumer> tagCaptor = ArgumentCaptor.forClass(TagConsumer.class);
        verify(tagManager).addConsumer(tagCaptor.capture());
        tagCaptor.getValue().accept(List.of(new DataPointImpl("tag1", "{\"v\":2}")));

        // Still only 1 publish call (from the primary TOPIC_FILTER during start)
        verify(dataCombiningPublishService, times(1)).publish(any(), any(), any());
    }

    /*
     * Verifies via start() that when the primary is a TOPIC_FILTER and an instruction
     * also references a different TOPIC_FILTER, only the primary triggers publish.
     * Both topic filters receive a message via polling, but only the primary's process()
     * calls triggerPublish(). The non-primary's process() only stores the PUBLISH data.
     * The non-primary is subscribed first, then the primary is subscribed last.
     */
    @Test
    void start_whenPrimaryIsTopicFilter_withTopicFilterInstruction_thenOnlyPrimaryTriggersPublish() {
        final PUBLISH mqttPublish = createMockPublish();
        setupSynchronousPolling(mqttPublish);

        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataIdentifierReference instructionRef =
                new DataIdentifierReference("other/#", DataIdentifierReference.Type.TOPIC_FILTER);
        final Instruction instruction = new Instruction("$.value", "output", instructionRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of(), List.of("sensor/temp", "other/#")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // 2 topic filter subscriptions created
        verify(localTopicTree, times(2)).addTopic(anyString(), any(Topic.class), anyByte(), anyString());

        // Both queue consumers polled and received a message, but only the primary triggered publish.
        // The non-primary (other/#) stored the message without triggering.
        // The primary (sensor/temp) stored the message AND called triggerPublish().
        verify(dataCombiningPublishService, times(1)).publish(any(), any(), eq(combining));
    }

    /*
     * Verifies via start() that when the primary TOPIC_FILTER is also referenced in an
     * instruction, it is only subscribed once (deduplicated by .filter(ref -> !ref.equals(primaryRef)))
     * and still triggers publish as the primary. The instruction ref equals the primary ref,
     * so it's filtered out of the stream, and only subscribe(primaryRef, true) creates the subscription.
     */
    @Test
    void start_whenPrimaryTopicFilterAlsoInInstruction_thenSubscribedOnceAsPrimary() {
        final PUBLISH mqttPublish = createMockPublish();
        setupSynchronousPolling(mqttPublish);

        final DataIdentifierReference primary =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final DataIdentifierReference instructionRef =
                new DataIdentifierReference("sensor/temp", DataIdentifierReference.Type.TOPIC_FILTER);
        final Instruction instruction = new Instruction("$.value", "output", instructionRef);
        final DataCombining combining = new DataCombining(
                UUID.randomUUID(),
                new DataCombiningSources(primary, List.of(), List.of("sensor/temp")),
                new DataCombiningDestination(null, "dest/topic", "{}"),
                List.of(instruction));

        final DataCombiningRuntime runtime = createRuntime(combining);
        runtime.start();

        // Only 1 topic filter subscription (deduplicated)
        verify(localTopicTree, times(1)).addTopic(anyString(), any(Topic.class), anyByte(), anyString());

        // The single subscription is primary and triggered publish
        verify(dataCombiningPublishService, times(1)).publish(any(), any(), eq(combining));
    }
}
