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

import com.hivemq.edge.modules.api.adapters.ModuleServices;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

public class ModuleServicesImpl implements ModuleServices {

    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull ScheduledExecutorService scheduledExecutorService;
    private final @NotNull ProtocolAdapterPollingService protocolAdapterPollingService;
    private final @NotNull EventService eventService;

    @Inject
    public ModuleServicesImpl(@NotNull final ProtocolAdapterPublishService adapterPublishService,
                              @NotNull final ScheduledExecutorService scheduledExecutorService,
                              @NotNull final ProtocolAdapterPollingService protocolAdapterPollingService,
                              @NotNull final EventService eventService) {

        this.adapterPublishService = adapterPublishService;
        this.scheduledExecutorService = scheduledExecutorService;
        this.protocolAdapterPollingService = protocolAdapterPollingService;
        this.eventService = eventService;
    }

    @Override
    public @NotNull ProtocolAdapterPublishService adapterPublishService() {
        return adapterPublishService;
    }

    @Override
    public @NotNull ScheduledExecutorService scheduledExecutorService() {
        return scheduledExecutorService;
    }

    @Override
    public @NotNull ProtocolAdapterPollingService protocolAdapterPollingService() {
        return protocolAdapterPollingService;
    }

    @Override
    public EventService eventService() {
        return eventService;
    }
}

