/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.legacy.LegacyPlc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7AdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7ToMqttConfig;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.legacy.LegacyS7AdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class S7ProtocolAdapterFactory implements ProtocolAdapterFactory<S7AdapterConfig>, LegacyConfigConversion {

    private static final @NotNull Logger log = LoggerFactory.getLogger(S7ProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public S7ProtocolAdapterFactory(@NotNull final ProtocolAdapterFactoryInput input) {
        this.writingEnabled = input.isWritingEnabled();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return S7ProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<S7AdapterConfig> input) {
        return new S7ProtocolAdapter(adapterInformation, input);
    }

    public @NotNull ConfigTagsTuple tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyS7AdapterConfig legacyS7AdapterConfig =
                objectMapper.convertValue(config, LegacyS7AdapterConfig.class);


        final List<Plc4xToMqttMapping> plc4xToMqttMappings = new ArrayList<>();
        final List<Plc4xTag> tags = new ArrayList<>();
        for (LegacyPlc4xAdapterConfig.PollingContextImpl subscription : legacyS7AdapterConfig.getSubscriptions()) {
            tags.add(new Plc4xTag(subscription.getTagName(),"not set",
                    new Plc4xTagDefinition(subscription.getTagAddress())));
                    plc4xToMqttMappings.add(new Plc4xToMqttMapping(subscription.getMqttTopic(),
                            subscription.getMqttQos(),
                            subscription.getMessageHandlingOptions(),
                            subscription.getIncludeTimestamp(),
                            subscription.getIncludeTagNames(),
                            subscription.getTagName(),
                            subscription.getDataType(),
                            subscription.getUserProperties()));
        }

        final S7ToMqttConfig s7ToMqttConfig = new S7ToMqttConfig(legacyS7AdapterConfig.getPollingIntervalMillis(),
                legacyS7AdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                legacyS7AdapterConfig.getPublishChangedDataOnly(),
                plc4xToMqttMappings);

        return new ConfigTagsTuple(new S7AdapterConfig(legacyS7AdapterConfig.getId(),
                legacyS7AdapterConfig.getPort(),
                legacyS7AdapterConfig.getHost(),
                legacyS7AdapterConfig.getControllerType(),
                legacyS7AdapterConfig.getRemoteRack(),
                legacyS7AdapterConfig.getRemoteRack2(),
                legacyS7AdapterConfig.getRemoteSlot(),
                legacyS7AdapterConfig.getRemoteSlot2(),
                legacyS7AdapterConfig.getRemoteTsap(),
                s7ToMqttConfig),
                tags);
    }
}
