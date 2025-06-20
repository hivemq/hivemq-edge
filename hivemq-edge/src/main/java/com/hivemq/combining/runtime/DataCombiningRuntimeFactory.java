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

import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.persistence.clientqueue.ClientQueuePersistence;
import org.jetbrains.annotations.NotNull;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DataCombiningRuntimeFactory {

    private final @NotNull LocalTopicTree localTopicTree;
    private final @NotNull ClientQueuePersistence clientQueuePersistence;
    private final @NotNull SingleWriterService singleWriterService;
    private final @NotNull DataCombiningPublishService dataCombiningPublishService;
    private final @NotNull TagManager tagManager;
    private final @NotNull DataCombiningTransformationService dataCombiningTransformationService;

    @Inject
    public DataCombiningRuntimeFactory(
            final @NotNull LocalTopicTree localTopicTree,
            final @NotNull ClientQueuePersistence clientQueuePersistence,
            final @NotNull SingleWriterService singleWriterService,
            final @NotNull DataCombiningPublishService dataCombiningPublishService,
            final @NotNull TagManager tagManager,
            final @NotNull DataCombiningTransformationService dataCombiningTransformationService) {

        this.localTopicTree = localTopicTree;
        this.clientQueuePersistence = clientQueuePersistence;
        this.singleWriterService = singleWriterService;
        this.dataCombiningPublishService = dataCombiningPublishService;
        this.tagManager = tagManager;
        this.dataCombiningTransformationService = dataCombiningTransformationService;
    }

    public @NotNull DataCombiningRuntime build(
            final @NotNull DataCombining dataCombining) {
        return new DataCombiningRuntime(dataCombining,
                localTopicTree,
                tagManager,
                clientQueuePersistence,
                singleWriterService,
                dataCombiningPublishService,
                dataCombiningTransformationService);
    }

}
