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
package com.hivemq.edge.adapters.etherip;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.legacy.ConfigTagsTuple;
import com.hivemq.adapter.sdk.api.config.legacy.LegacyConfigConversion;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.etherip.config.EipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttMapping;
import com.hivemq.edge.adapters.etherip.config.legacy.LegacyEipSpecificAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.tag.EipTag;
import com.hivemq.edge.adapters.etherip.config.tag.EipTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EipProtocolAdapterFactory
        implements ProtocolAdapterFactory<EipSpecificAdapterConfig>, LegacyConfigConversion {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EipProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public EipProtocolAdapterFactory(final @NotNull ProtocolAdapterFactoryInput input) {
        this.writingEnabled = input.isWritingEnabled();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return EipProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<EipSpecificAdapterConfig> input) {
        return new EipPollingProtocolAdapter(adapterInformation, input);
    }

    public @NotNull ConfigTagsTuple tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyEipSpecificAdapterConfig legacyEipAdapterConfig =
                objectMapper.convertValue(config, LegacyEipSpecificAdapterConfig.class);

        // reference tag in the config

        final List<EipToMqttMapping> eipToMqttMappings = new ArrayList<>();
        final List<EipTag> tags = new ArrayList<>();
        for (final LegacyEipSpecificAdapterConfig.PollingContextImpl context : legacyEipAdapterConfig.getSubscriptions()) {
            // create tag first
            tags.add(new EipTag(context.getTagName(),
                    "not available",
                    new EipTagDefinition(context.getTagAddress(), context.getDataType())));
            eipToMqttMappings.add(new EipToMqttMapping(context.getDestinationMqttTopic(),
                    context.getQos(),
                    context.getMessageHandlingOptions(),
                    context.getIncludeTimestamp(),
                    context.getIncludeTagNames(),
                    context.getTagName(),
                    context.getUserProperties()));
        }

        final EipToMqttConfig eipToMqttConfig = new EipToMqttConfig(legacyEipAdapterConfig.getPollingIntervalMillis(),
                legacyEipAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                legacyEipAdapterConfig.getPublishChangedDataOnly());

        final EipSpecificAdapterConfig eipSpecificAdapterConfig = new EipSpecificAdapterConfig(
                        legacyEipAdapterConfig.getPort(),
                        legacyEipAdapterConfig.getHost(),
                        legacyEipAdapterConfig.getBackplane(),
                        legacyEipAdapterConfig.getSlot(),
                        eipToMqttConfig);
        return new ConfigTagsTuple(legacyEipAdapterConfig.getId(), eipSpecificAdapterConfig, tags, eipToMqttMappings);
    }
}
