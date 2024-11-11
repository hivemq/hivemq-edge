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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartInput;
import com.hivemq.adapter.sdk.api.services.ModuleServices;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesImpl;
import com.hivemq.edge.modules.adapters.impl.ModuleServicesPerModuleImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;

class ProtocolAdapterStartInputImpl implements ProtocolAdapterStartInput {

    private final @NotNull ModuleServicesImpl moduleServices;
    private final @NotNull EventService eventService;

    ProtocolAdapterStartInputImpl(
            final @NotNull ModuleServicesImpl moduleServices, final @NotNull EventService eventService) {
        this.moduleServices = moduleServices;
        this.eventService = eventService;
    }

    @Override
    public @NotNull ModuleServices moduleServices() {
        return new ModuleServicesPerModuleImpl(moduleServices.adapterPublishService(),
                eventService,
                moduleServices.protocolAdapterWritingService());
    }
}
