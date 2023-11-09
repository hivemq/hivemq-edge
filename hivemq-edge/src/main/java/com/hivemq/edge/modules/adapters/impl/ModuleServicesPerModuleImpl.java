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
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPollingService;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishBuilder;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.api.events.EventService;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.util.concurrent.ScheduledExecutorService;

public class ModuleServicesPerModuleImpl implements ModuleServices {

    private final @NotNull ModuleServicesImpl delegate;
    private final @NotNull ProtocolAdapterPublishService adapterPublishService;
    private final @NotNull EventService eventService;

    public ModuleServicesPerModuleImpl(
            final @NotNull ProtocolAdapter protocolAdapter,
            final @NotNull ModuleServicesImpl delegate,
            final @NotNull EventService eventService) {
        this.delegate = delegate;
        this.eventService = eventService;
        this.adapterPublishService =
                new ProtocolAdapterPublishServicePerAdapter(delegate.adapterPublishService(), protocolAdapter);
    }

    @Override
    public @NotNull ProtocolAdapterPublishService adapterPublishService() {
        return adapterPublishService;
    }

    @Override
    public @NotNull ScheduledExecutorService scheduledExecutorService() {
        return delegate.scheduledExecutorService();
    }

    @Override
    public @NotNull ProtocolAdapterPollingService protocolAdapterPollingService() {
        return delegate.protocolAdapterPollingService();
    }

    @Override
    public EventService eventService() {
        return eventService;
    }

    private static class ProtocolAdapterPublishServicePerAdapter implements ProtocolAdapterPublishService {

        private final @NotNull ProtocolAdapterPublishService delegate;

        public ProtocolAdapterPublishServicePerAdapter(
                @NotNull final ProtocolAdapterPublishService delegate, @NotNull final ProtocolAdapter adapter) {
            this.delegate = delegate;
            this.adapter = adapter;
        }

        private final @NotNull ProtocolAdapter adapter;


        @Override
        public @NotNull ProtocolAdapterPublishBuilder publish() {
            final ProtocolAdapterPublishBuilderImpl builder = (ProtocolAdapterPublishBuilderImpl) delegate.publish();
            return builder.withAdapter(adapter);
        }
    }
}

