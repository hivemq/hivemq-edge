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
package com.hivemq.edge.adapters.plc4x.types.ads;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.config.legacy.LegacyPlc4xAdapterConfig;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSToMqttConfig;
import com.hivemq.edge.adapters.plc4x.types.ads.config.legacy.LegacyADSAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class ADSProtocolAdapterFactory implements ProtocolAdapterFactory<ADSAdapterConfig> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ADSProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public ADSProtocolAdapterFactory(final @NotNull boolean writingEnabled) {
        this.writingEnabled = writingEnabled;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return ADSProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            @NotNull final ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<ADSAdapterConfig> input) {
        return new ADSProtocolAdapter(adapterInformation, input);
    }

    @Override
    public @NotNull ProtocolAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config, final boolean writingEnabled) {
        try {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config, writingEnabled);
        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn(
                        "Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'. Support for the legacy configuration will be removed in the beginning of 2025.",
                        ADSProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        ADSProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
        }
    }

    private @NotNull ADSAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyADSAdapterConfig legacyAdsAdapterConfig =
                objectMapper.convertValue(config, LegacyADSAdapterConfig.class);

        final List<Plc4xToMqttMapping> plc4xToMqttMappings = new ArrayList<>();
        final List<Plc4xTag> tags = new ArrayList<>();
        for (LegacyPlc4xAdapterConfig.PollingContextImpl subscription : legacyAdsAdapterConfig.getSubscriptions()) {
            tags.add(new Plc4xTag(subscription.getTagName(), "not set", new Plc4xTagDefinition(subscription.getTagAddress())));
            plc4xToMqttMappings.add(new Plc4xToMqttMapping(subscription.getMqttTopic(),
                    subscription.getMqttQos(),
                    subscription.getMessageHandlingOptions(),
                    subscription.getIncludeTimestamp(),
                    subscription.getIncludeTagNames(),
                    subscription.getTagName(),
                    subscription.getDataType(),
                    subscription.getUserProperties()));
        }


        final ADSToMqttConfig modbusToMqttConfig =
                new ADSToMqttConfig(legacyAdsAdapterConfig.getPollingIntervalMillis(),
                        legacyAdsAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                        legacyAdsAdapterConfig.getPublishChangedDataOnly(),
                        plc4xToMqttMappings);


        return new ADSAdapterConfig(legacyAdsAdapterConfig.getId(),
                legacyAdsAdapterConfig.getPort(),
                legacyAdsAdapterConfig.getHost(),
                legacyAdsAdapterConfig.getTargetAmsPort(),
                legacyAdsAdapterConfig.getSourceAmsPort(),
                legacyAdsAdapterConfig.getTargetAmsNetId(),
                legacyAdsAdapterConfig.getSourceAmsNetId(),
                modbusToMqttConfig,
                tags);
    }
}
