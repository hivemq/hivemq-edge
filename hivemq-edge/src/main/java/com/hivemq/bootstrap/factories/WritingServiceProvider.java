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
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import com.hivemq.adapter.sdk.api.writing.WritingContext;
import com.hivemq.adapter.sdk.api.writing.WritingProtocolAdapter;
import com.hivemq.bootstrap.services.EdgeCoreFactoryService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CompletableFuture;

@Singleton
public class WritingServiceProvider {

    private final @NotNull EdgeCoreFactoryService edgeCoreFactoryService;
    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull SingleWriterService singleWriterService;

    @Inject
    public WritingServiceProvider(
            final @NotNull EdgeCoreFactoryService edgeCoreFactoryService,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull SingleWriterService singleWriterService) {
        this.edgeCoreFactoryService = edgeCoreFactoryService;
        this.objectMapper = objectMapper;
        this.localTopicTree = localTopicTree;
        this.singleWriterService = singleWriterService;
    }

    public @NotNull ProtocolAdapterWritingService get() {
        final WritingServiceFactory writingServiceFactory = edgeCoreFactoryService.getWritingServiceFactory();
        if (writingServiceFactory == null) {
            return new WritingServiceNoop();
        }
        return writingServiceFactory.build(objectMapper,
                localTopicTree,
                singleWriterService);
    }


    public static class WritingServiceNoop implements ProtocolAdapterWritingService {

        private static final @NotNull Logger log = LoggerFactory.getLogger(WritingServiceNoop.class);

        @Override
        public void addWritingChangedCallback(final @NotNull ProtocolAdapterWritingService.WritingChangedCallback callback) {

        }

        @Override
        public boolean writingEnabled() {
            return false;
        }

        @Override
        public @NotNull CompletableFuture<Void> startWriting(@NotNull final WritingProtocolAdapter<WritingContext> writingProtocolAdapter,
                                                             final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
            log.warn("No bidirectional module is currently installed. Writing to PLCs is currently not supported.");
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public @NotNull CompletableFuture<Void> stopWriting(@NotNull final WritingProtocolAdapter<WritingContext> writingProtocolAdapter) {
            // NOOP as nothing was started.
            return CompletableFuture.completedFuture(null);
        }
    }
}
