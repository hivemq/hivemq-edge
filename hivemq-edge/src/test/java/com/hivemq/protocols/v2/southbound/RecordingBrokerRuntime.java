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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.extensions.iteration.ChunkCursor;
import com.hivemq.extensions.iteration.MultipleChunkResult;
import com.hivemq.metrics.MetricsHolder;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.RetainedMessage;
import com.hivemq.persistence.retained.RetainedMessagePersistence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The scripted broker side for southbound unit tests: a real {@link LocalTopicTree}, the
 * {@link RecordingClientQueue}, a publish service that records every verdict publish, and a retained store holding
 * whatever a test seeds. Bundled as a {@link SouthboundBrokerRuntime} through {@link #runtime()}.
 */
final class RecordingBrokerRuntime {

    final @NotNull LocalTopicTree topicTree = new LocalTopicTree(new MetricsHolder(new MetricRegistry()));
    final @NotNull RecordingClientQueue clientQueue = new RecordingClientQueue();
    final @NotNull RecordingPublishService publishService = new RecordingPublishService();
    final @NotNull SeededRetainedStore retainedStore = new SeededRetainedStore();

    @NotNull
    SouthboundBrokerRuntime runtime() {
        return new SouthboundBrokerRuntime(topicTree, clientQueue, publishService, retainedStore);
    }

    /** Records every publish (the verdicts) and reports each as delivered. */
    static final class RecordingPublishService implements InternalPublishService {

        final @NotNull List<PUBLISH> published = new ArrayList<>();

        @Override
        public @NotNull ListenableFuture<PublishingResult> publish(
                final @NotNull PUBLISH publish,
                final @NotNull ExecutorService executorService,
                final @Nullable String sender) {
            published.add(publish);
            return Futures.immediateFuture(PublishingResult.DELIVERED);
        }
    }

    /** A retained store answering {@code get} from a seeded map; everything else is unsupported. */
    static final class SeededRetainedStore implements RetainedMessagePersistence {

        final @NotNull Map<String, RetainedMessage> retained = new HashMap<>();

        @Override
        public @NotNull ListenableFuture<RetainedMessage> get(final @NotNull String topic) {
            return Futures.immediateFuture(retained.get(topic));
        }

        @Override
        public long size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListenableFuture<Void> remove(final @NotNull String topic) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListenableFuture<Void> persist(
                final @NotNull String topic, final @NotNull RetainedMessage retainedMessage) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListenableFuture<Set<String>> getWithWildcards(final @NotNull String topicWithWildcards) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListenableFuture<Void> closeDB() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListenableFuture<Void> cleanUp(final int bucketIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListenableFuture<Void> clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public @NotNull ListenableFuture<MultipleChunkResult<Map<String, @NotNull RetainedMessage>>>
                getAllLocalRetainedMessagesChunk(final @NotNull ChunkCursor cursor) {
            throw new UnsupportedOperationException();
        }
    }
}
