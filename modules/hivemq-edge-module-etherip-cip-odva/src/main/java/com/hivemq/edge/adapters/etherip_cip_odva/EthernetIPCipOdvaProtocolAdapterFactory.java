/*
 * Copyright 2023-present HiveMQ GmbH
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.hivemq.edge.adapters.etherip_cip_odva;

import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.etherip_cip_odva.config.EipSpecificAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;

public class EthernetIPCipOdvaProtocolAdapterFactory implements ProtocolAdapterFactory<EipSpecificAdapterConfig> {

    final boolean writingEnabled;

    final EventService eventService;

    public EthernetIPCipOdvaProtocolAdapterFactory(final @NotNull ProtocolAdapterFactoryInput input) {
        this.writingEnabled = input.isWritingEnabled();
        this.eventService = input.eventService();

        LoggerFactory.getLogger(this.getClass()).warn("#### Writing enabled=" + writingEnabled);
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return EthernetIPCipOdvaProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<EipSpecificAdapterConfig> input) {

        // FIXME: Pass EventService + WriteEnabled?
        return new EthernetIPCipOdvaPollingProtocolAdapter(adapterInformation, input);
    }
}
