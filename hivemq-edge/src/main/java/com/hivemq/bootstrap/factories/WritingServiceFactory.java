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
import com.hivemq.persistence.topicfilter.TopicFilterPersistence;
import org.jetbrains.annotations.NotNull;
import com.hivemq.mqtt.topic.tree.LocalTopicTree;
import com.hivemq.persistence.SingleWriterService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import com.hivemq.protocols.InternalProtocolAdapterWritingService;

public interface WritingServiceFactory {

    @NotNull InternalProtocolAdapterWritingService build(
            @NotNull ObjectMapper objectMapper,
            @NotNull LocalTopicTree localTopicTree,
            @NotNull SingleWriterService singleWriterService);
}
