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

import com.codahale.metrics.MetricRegistry;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import org.jetbrains.annotations.NotNull;

/**
 * The scripted broker side for southbound unit tests: a real {@link LocalTopicTree} and the
 * {@link RecordingClientQueue}. Bundled as a {@link SouthboundBrokerRuntime} through {@link #runtime()}.
 */
final class RecordingBrokerRuntime {

    final @NotNull LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
    final @NotNull RecordingClientQueue clientQueue = new RecordingClientQueue();

    @NotNull
    SouthboundBrokerRuntime runtime() {
        return new SouthboundBrokerRuntime(topicTree, clientQueue);
    }
}
