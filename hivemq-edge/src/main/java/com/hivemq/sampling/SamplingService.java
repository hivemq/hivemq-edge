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

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.message.QoS;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.subscribe.Topic;
import com.hivemq.mqtt.topic.SubscriptionFlag;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Singleton
public class SamplingService {

    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private static final @NotNull String SAMPLER_PREFIX = "Sampler#";


    @Inject
    public SamplingService(final @NotNull LocalTopicTree localTopicTree,
                           final @NotNull ClientQueuePersistence clientQueuePersistence) {
        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
    }

    public void startSampling(final @NotNull String topic) {
        final String clientId = SAMPLER_PREFIX + topic;
        localTopicTree.addTopic(clientId,
                new Topic(topic, QoS.AT_LEAST_ONCE, false, true),
                SubscriptionFlag.getDefaultFlags(true, true, false),
                clientId);
    }

    public void stopSampling(final @NotNull String topic) {
        final String clientId = SAMPLER_PREFIX + topic;
        localTopicTree.removeSubscriber(clientId, topic, null);
    }

    public @NotNull List<byte[]> getSamples(final @NotNull String topic) {
        final String clientId = SAMPLER_PREFIX + topic;
        final String queueId = clientId + "/" + topic;
        final ListenableFuture<ImmutableList<PUBLISH>> publishes = clientQueuePersistence.peek(queueId, true, 10_000, 10);
        try {
         return publishes.get().stream().map(PUBLISH::getPayload).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            //TODO
            throw new RuntimeException(e);
        }
    }


}
