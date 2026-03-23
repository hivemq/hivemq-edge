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
package com.hivemq.protocols.northbound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.NotNull;

@Singleton
public class NorthboundConsumerFactory {

    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService;
    private final @NotNull EventService eventService;

    @Inject
    public NorthboundConsumerFactory(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService,
            final @NotNull EventService eventService) {
        this.objectMapper = objectMapper;
        this.protocolAdapterPublishService = protocolAdapterPublishService;
        this.eventService = eventService;
    }

    public @NotNull NorthboundTagConsumer build(
            final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper,
            final @NotNull NorthboundMapping northboundMapping,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
        return new NorthboundTagConsumer(
                northboundMapping,
                protocolAdapterWrapper,
                objectMapper,
                protocolAdapterPublishService,
                protocolAdapterMetricsService,
                eventService);
    }
}
