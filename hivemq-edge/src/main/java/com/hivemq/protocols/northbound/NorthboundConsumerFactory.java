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
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterMetricsService;
import com.hivemq.edge.modules.adapters.impl.ProtocolAdapterPublishServiceImpl;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NorthboundConsumerFactory {

    private final @NotNull ObjectMapper objectMapper;
    private final @NotNull JsonPayloadDefaultCreator jsonPayloadCreator;
    private final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService;
    private final @NotNull EventService eventService;

    @Inject
    public NorthboundConsumerFactory(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadCreator,
            final @NotNull ProtocolAdapterPublishServiceImpl protocolAdapterPublishService,
            final @NotNull EventService eventService) {
        this.objectMapper = objectMapper;
        this.jsonPayloadCreator = jsonPayloadCreator;
        this.protocolAdapterPublishService = protocolAdapterPublishService;
        this.eventService = eventService;
    }

    public @NotNull NorthboundTagConsumer build(
            final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper,
            final @NotNull PollingContext pollingContext,
            final @NotNull ProtocolAdapterMetricsService protocolAdapterMetricsService) {
        return new NorthboundTagConsumer(pollingContext,
                protocolAdapterWrapper,
                objectMapper,
                jsonPayloadCreator,
                protocolAdapterPublishService,
                protocolAdapterMetricsService,
                eventService);
    }
}
