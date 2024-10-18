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
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.edge.adapters.etherip.config.EipAdapterConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttConfig;
import com.hivemq.edge.adapters.etherip.config.EipToMqttMapping;
import com.hivemq.edge.adapters.etherip.config.legacy.LegacyEipAdapterConfig;
import com.hivemq.edge.adapters.etherip.tag.EipAddress;
import com.hivemq.edge.adapters.etherip.tag.EipTag;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hivemq.edge.adapters.etherip.EipProtocolAdapterInformation.PROTOCOL_ID;

public class EipProtocolAdapterFactory implements ProtocolAdapterFactory<EipAdapterConfig> {

    private static final @NotNull Logger log = LoggerFactory.getLogger(EipProtocolAdapterFactory.class);

    final boolean writingEnabled;

    private final @NotNull ProtocolAdapterTagService tagService;

    public EipProtocolAdapterFactory(
            final boolean writingEnabled, final @NotNull ProtocolAdapterTagService tagService) {
        this.writingEnabled = writingEnabled;
        this.tagService = tagService;
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return EipProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            @NotNull final ProtocolAdapterInput<EipAdapterConfig> input) {
        return new EipPollingProtocolAdapter(adapterInformation, input);
    }

    @Override
    public @NotNull ProtocolAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        try {
            final ProtocolAdapterConfig protocolAdapterConfig =
                    ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
            System.err.println(protocolAdapterConfig);
            return protocolAdapterConfig;

        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn(
                        "Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'. Support for the legacy configuration will be removed in the beginning of 2025.",
                        EipProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config, tagService);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        EipProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
        }
    }

    @Override
    public @NotNull Class<EipAdapterConfig> getConfigClass() {
        return EipAdapterConfig.class;
    }

    private static @NotNull EipAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config,
            final @NotNull ProtocolAdapterTagService tagService) {
        final LegacyEipAdapterConfig legacyEipAdapterConfig =
                objectMapper.convertValue(config, LegacyEipAdapterConfig.class);

        // reference tag in the config

        final List<EipToMqttMapping> eipToMqttMappings = new ArrayList<>();
        for (final LegacyEipAdapterConfig.PollingContextImpl context : legacyEipAdapterConfig.getSubscriptions()) {
            // create tag first
            final ProtocolAdapterTagService.AddStatus addStatus = tagService.addTag(legacyEipAdapterConfig.getId(),
                    PROTOCOL_ID,
                    new EipTag(context.getTagName(), new EipAddress(context.getTagAddress())));
            switch (addStatus) {
                case SUCCESS:
                    eipToMqttMappings.add(new EipToMqttMapping(context.getDestinationMqttTopic(),
                            context.getQos(),
                            context.getMessageHandlingOptions(),
                            context.getIncludeTimestamp(),
                            context.getIncludeTagNames(),
                            context.getTagName(),
                            context.getDataType(),
                            context.getUserProperties()));
                    break;
                case ALREADY_PRESENT:
                    final String newTagName = legacyEipAdapterConfig.getId() + "-" + UUID.randomUUID().toString();
                    log.warn(
                            "While migrating the EIPConfig a tag could not be added because a tag with the same name '{}' was already present. Another tagName using an random Uuid is used instead: '{}'",
                            context.getTagName(),
                            newTagName);
                    tagService.addTag(legacyEipAdapterConfig.getId(), PROTOCOL_ID,
                            new EipTag(newTagName, new EipAddress(context.getTagAddress())));

                    eipToMqttMappings.add(new EipToMqttMapping(context.getDestinationMqttTopic(),
                            context.getQos(),
                            context.getMessageHandlingOptions(),
                            context.getIncludeTimestamp(),
                            context.getIncludeTagNames(),
                            newTagName,
                            context.getDataType(),
                            context.getUserProperties()));
                    break;
            }
        }

        final EipToMqttConfig eipToMqttConfig = new EipToMqttConfig(legacyEipAdapterConfig.getPollingIntervalMillis(),
                legacyEipAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                legacyEipAdapterConfig.getPublishChangedDataOnly(),
                eipToMqttMappings);

        return new EipAdapterConfig(legacyEipAdapterConfig.getId(),
                legacyEipAdapterConfig.getPort(),
                legacyEipAdapterConfig.getHost(),
                legacyEipAdapterConfig.getBackplane(),
                legacyEipAdapterConfig.getSlot(),
                eipToMqttConfig);
    }
}
