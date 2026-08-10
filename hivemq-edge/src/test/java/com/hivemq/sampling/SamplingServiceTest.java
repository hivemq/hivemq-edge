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
package com.hivemq.sampling;

import static com.hivemq.sampling.SamplingService.SAMPLER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

public class SamplingServiceTest {

    @ParameterizedTest
    @ValueSource(strings = {"topic", "a/b", "a/b/c", "a//b", "sport/tennis/player1/#", "$share", "/", "//"})
    public void test_extractSampledTopic_recovers_every_sampled_topic(final String topic) {
        assertEquals(topic, SamplingService.extractSampledTopic(SamplingService.createQueueId(topic)));
    }

    @Test
    public void test_extractSampledTopic_rejects_ids_that_are_not_sampler_queues() {
        assertNull(SamplingService.extractSampledTopic("group/topic"));
        assertNull(SamplingService.extractSampledTopic("$FORWARDER::bridge-hash/topic"));
        // right prefix, but not the <topic>/<topic> shape a sampler queue has
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "a/b"));
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "ab/ba"));
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX + "topic"));
        assertNull(SamplingService.extractSampledTopic(SAMPLER_PREFIX));
    }

    /**
     * Binds the queue-ID convention to what {@code startSampling} actually registers: the queue is
     * named {@code <share name>/<topic filter>}, so if the share name ever stops being
     * {@code $SAMPLER::<topic>} the clean-up would resolve the wrong owner and wipe live samples.
     */
    @ParameterizedTest
    @ValueSource(strings = {"topic", "a/b/c"})
    public void test_createQueueId_matches_the_subscription_startSampling_registers(final String topic) {
        final LocalTopicTree topicTree = mock(LocalTopicTree.class);
        new SamplingService(topicTree, mock(ClientQueuePersistence.class)).startSampling(topic);

        final ArgumentCaptor<Topic> registeredTopic = ArgumentCaptor.forClass(Topic.class);
        final ArgumentCaptor<String> shareName = ArgumentCaptor.forClass(String.class);
        verify(topicTree).addTopic(anyString(), registeredTopic.capture(), anyByte(), shareName.capture());

        assertEquals(
                shareName.getValue() + "/" + registeredTopic.getValue().getTopic(),
                SamplingService.createQueueId(topic));
    }
}
