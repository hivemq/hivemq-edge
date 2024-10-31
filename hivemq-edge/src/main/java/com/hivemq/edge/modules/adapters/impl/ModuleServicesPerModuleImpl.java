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

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterPublishBuilder;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterWritingService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import dagger.internal.Preconditions;

public class ModuleServicesPerModuleImpl implements ModuleServices {

    private final @NotNull ProtocolAdapterPublishServicePerAdapter adapterPublishServicePerAdapter;
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService;
    private final @NotNull EventService eventService;
    private final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService;

    public ModuleServicesPerModuleImpl(
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull EventService eventService,
            final @NotNull ProtocolAdapterTagService protocolAdapterTagService,
            final @NotNull ProtocolAdapterWritingService protocolAdapterWritingService
    ) {
        this.eventService = eventService;
        this.adapterPublishServicePerAdapter = new ProtocolAdapterPublishServicePerAdapter(adapterPublishService);
        this.protocolAdapterTagService = protocolAdapterTagService;
        this.protocolAdapterWritingService = protocolAdapterWritingService;
    }

    @Override
    public @NotNull ProtocolAdapterPublishService adapterPublishService() {
        return adapterPublishServicePerAdapter;
    }

    @Override
    public @NotNull EventService eventService() {
        return eventService;
    }

    @Override
    public @NotNull ProtocolAdapterTagService protocolAdapterTagService() {
        return protocolAdapterTagService;
    }

    public void setAdapter(final @NotNull ProtocolAdapter protocolAdapter) {
        this.adapterPublishServicePerAdapter.setAdapter(protocolAdapter);
    }

    @Override
    public @NotNull ProtocolAdapterWritingService protocolAdapterWritingService() {
        return protocolAdapterWritingService;
    }

    private static class ProtocolAdapterPublishServicePerAdapter implements ProtocolAdapterPublishService {

        private final @NotNull ProtocolAdapterPublishService delegate;
        private @Nullable ProtocolAdapter adapter;

        public ProtocolAdapterPublishServicePerAdapter(
                @NotNull final ProtocolAdapterPublishService delegate) {
            this.delegate = delegate;
        }

        public void setAdapter(final @NotNull ProtocolAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public @NotNull ProtocolAdapterPublishBuilder createPublish() {
            Preconditions.checkNotNull(adapter, "Adapter must not be null");
            final ProtocolAdapterPublishBuilderImpl builder =
                    (ProtocolAdapterPublishBuilderImpl) delegate.createPublish();
            return builder.withAdapter(adapter);
        }
    }
}

