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
package com.hivemq.configuration.reader;

import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.topicbuffer.TopicBufferSubscriptionEntity;
import com.hivemq.topicbuffer.model.TopicBufferSubscription;
import java.util.List;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TopicBufferExtractor
        implements ReloadableExtractor<List<TopicBufferSubscriptionEntity>, List<TopicBufferSubscription>> {

    private volatile @NotNull List<TopicBufferSubscriptionEntity> config = List.of();
    private volatile @Nullable Consumer<List<TopicBufferSubscription>> consumer = null;

    public TopicBufferExtractor() {}

    @Override
    public boolean needsRestartWithConfig(final @NotNull HiveMQConfigEntity config) {
        return false;
    }

    @Override
    public Configurator.ConfigResult updateConfig(final @NotNull HiveMQConfigEntity config) {
        this.config = List.copyOf(config.getTopicBufferSubscriptions());
        notifyConsumer();
        return Configurator.ConfigResult.SUCCESS;
    }

    @Override
    public void registerConsumer(final @NotNull Consumer<List<TopicBufferSubscription>> consumer) {
        this.consumer = consumer;
        notifyConsumer();
    }

    @Override
    public void sync(final @NotNull HiveMQConfigEntity entity) {
        entity.getTopicBufferSubscriptions().clear();
        entity.getTopicBufferSubscriptions().addAll(this.config);
    }

    private void notifyConsumer() {
        final Consumer<List<TopicBufferSubscription>> consumer = this.consumer;
        if (consumer != null) {
            consumer.accept(config.stream()
                    .map(e -> new TopicBufferSubscription(e.getTopicFilter(), e.getMaxMessages()))
                    .toList());
        }
    }
}
