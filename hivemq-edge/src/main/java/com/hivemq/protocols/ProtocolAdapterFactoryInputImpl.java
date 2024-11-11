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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import org.jetbrains.annotations.NotNull;

public class ProtocolAdapterFactoryInputImpl implements ProtocolAdapterFactoryInput {

    private final boolean writingEnabled;
    private final @NotNull EventService eventService;

    public ProtocolAdapterFactoryInputImpl(
            final boolean writingEnabled,
            final @NotNull EventService eventService) {
        this.writingEnabled = writingEnabled;
        this.eventService = eventService;
    }

    @Override
    public boolean isWritingEnabled() {
        return writingEnabled;
    }

    @Override
    public @NotNull EventService eventService() {
        return eventService;
    }
}
