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
package com.hivemq.bootstrap.factories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.bootstrap.services.EdgeCoreFactoryService;
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;
import com.hivemq.protocols.InternalWritingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class WritingServiceProvider {

    private final @NotNull EdgeCoreFactoryService edgeCoreFactoryService;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull TopicFilterPersistence topicFilterPersistence;

    @Inject
    public WritingServiceProvider(
            final @NotNull EdgeCoreFactoryService edgeCoreFactoryService,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull TopicFilterPersistence topicFilterPersistence) {
        this.edgeCoreFactoryService = edgeCoreFactoryService;
        this.objectMapper = objectMapper;
        this.localTopicTree = localTopicTree;
        this.singleWriterService = singleWriterService;
        this.topicFilterPersistence = topicFilterPersistence;
    }

    public @NotNull InternalProtocolAdapterWritingService get() {
        final WritingServiceFactory writingServiceFactory = edgeCoreFactoryService.getWritingServiceFactory();
        if (writingServiceFactory == null) {
            return new WritingServiceNoop();
        }
        return writingServiceFactory.build(objectMapper,
                localTopicTree,
                singleWriterService);
    }


    public static class WritingServiceNoop implements InternalProtocolAdapterWritingService {

        private static final @NotNull Logger log = LoggerFactory.getLogger(WritingServiceNoop.class);

        @Override
        public void addWritingChangedCallback(final @NotNull InternalProtocolAdapterWritingService.WritingChangedCallback callback) {

        }

        @Override
        public boolean writingEnabled() {
            return false;
        }


        @Override
        public @NotNull CompletableFuture<Void> startWriting(
                final @NotNull WritingProtocolAdapter writingProtocolAdapter,
                final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService,
                final @NotNull List<InternalWritingContext> southboundMappings) {
            log.warn("No bidirectional module is currently installed. Writing to PLCs is currently not supported.");
            return CompletableFuture.completedFuture(null);        }

        @Override
        public @NotNull CompletableFuture<Void> stopWriting(
                final @NotNull WritingProtocolAdapter writingProtocolAdapter,
                final @NotNull List<InternalWritingContext> writingContexts) {
            // NOOP as nothing was started.
            return CompletableFuture.completedFuture(null);
        }
    }
}
