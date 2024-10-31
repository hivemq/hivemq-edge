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
package com.hivemq.edge.modules.adapters.impl;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;

public class ModuleServicesImpl implements ModuleServices {

    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService;
    private final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService;

    @Inject
    public ModuleServicesImpl(
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterTagService protocolAdapterTagService,
            final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService) {
        this.adapterPublishService = adapterPublishService;
        this.eventService = eventService;
        this.protocolAdapterTagService = protocolAdapterTagService;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
    }

    @Override
    public @NotNull ProtocolAdapterPublishService adapterPublishService() {
        return adapterPublishService;
    }

    @Override
    public @NotNull EventService eventService() {
        return eventService;
    }

    @Override
    public @NotNull ProtocolAdapterTagService protocolAdapterTagService() {
        return protocolAdapterTagService;
    }

    @Override
    public @NotNull ProtocolAdapterWritingService protocolAdapterWritingService() {
        return protocolAdapterWritingService;
    }
}

