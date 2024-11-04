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
package com.hivemq.edge.adapters.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.ProtocolAdapter;
import com.hivemq.adapter.sdk.api.ProtocolAdapterInformation;
import com.hivemq.adapter.sdk.api.config.ProtocolAdapterConfig;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.edge.adapters.http.config.BidirectionalHttpAdapterConfig;
import com.hivemq.edge.adapters.http.config.HttpAdapterConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttMapping;
import com.hivemq.edge.adapters.http.config.legacy.LegacyHttpAdapterConfig;
import com.hivemq.edge.adapters.http.tag.HttpTag;
import com.hivemq.edge.adapters.http.tag.HttpTagDefinition;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.hivemq.edge.adapters.http.HttpProtocolAdapterInformation.PROTOCOL_ID;

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapterFactory implements ProtocolAdapterFactory<HttpAdapterConfig> {

    private static final Logger log = LoggerFactory.getLogger(HttpProtocolAdapterFactory.class);

    final boolean writingEnabled;

    public HttpProtocolAdapterFactory(@NotNull final ProtocolAdapterFactoryInput input) {
        this.writingEnabled = input.isWritingEnabled();
    }

    @Override
    public @NotNull ProtocolAdapterInformation getInformation() {
        return HttpProtocolAdapterInformation.INSTANCE;
    }

    @Override
    public @NotNull ProtocolAdapter createAdapter(
            final @NotNull ProtocolAdapterInformation adapterInformation,
            final @NotNull ProtocolAdapterInput<HttpAdapterConfig> input) {
        return new HttpProtocolAdapter(adapterInformation, input);
    }

    @Override
    public @NotNull ProtocolAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config,
            final boolean writingEnabled) {
        try {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config, writingEnabled);
        } catch (final Exception currentConfigFailedException) {
            try {
                log.warn("Could not load '{}' configuration, trying to load legacy configuration. Because: '{}'. Support for the legacy configuration will be removed in the beginning of 2025.",
                        HttpProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        currentConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", currentConfigFailedException);
                }
                return tryConvertLegacyConfig(objectMapper, config);
            } catch (final Exception legacyConfigFailedException) {
                log.warn("Could not load legacy '{}' configuration. Because: '{}'",
                        HttpProtocolAdapterInformation.INSTANCE.getDisplayName(),
                        legacyConfigFailedException.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("Original Exception:", legacyConfigFailedException);
                }
                //we rethrow the exception from the current config conversation, to have a correct rest response.
                throw currentConfigFailedException;
            }
        }
    }

    private static @NotNull HttpAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper,
            final @NotNull Map<String, Object> config) {
        final LegacyHttpAdapterConfig legacyHttpAdapterConfig =
                objectMapper.convertValue(config, LegacyHttpAdapterConfig.class);

        // create tag first
        final String newTagName = legacyHttpAdapterConfig.getId() + "-" + UUID.randomUUID();
        ArrayList<HttpTag> tags = new ArrayList<>();
        tags.add(new HttpTag(newTagName, "not set", new HttpTagDefinition(legacyHttpAdapterConfig.getUrl())));

        final HttpToMqttMapping httpToMqttMapping = new HttpToMqttMapping(newTagName,
                legacyHttpAdapterConfig.getDestination(),
                legacyHttpAdapterConfig.getQos(),
                List.of(),
                false,
                legacyHttpAdapterConfig.getHttpRequestMethod(),
                legacyHttpAdapterConfig.getHttpConnectTimeoutSeconds(),
                legacyHttpAdapterConfig.getHttpRequestBodyContentType(),
                legacyHttpAdapterConfig.getHttpRequestBody(),
                legacyHttpAdapterConfig.getHttpHeaders());

        final HttpToMqttConfig httpToMqttConfig =
                new HttpToMqttConfig(legacyHttpAdapterConfig.getPollingIntervalMillis(),
                        legacyHttpAdapterConfig.getMaxPollingErrorsBeforeRemoval(),
                        legacyHttpAdapterConfig.isAssertResponseIsJson(),
                        legacyHttpAdapterConfig.isHttpPublishSuccessStatusCodeOnly(),
                        List.of(httpToMqttMapping));

        return new HttpAdapterConfig(legacyHttpAdapterConfig.getId(),
                legacyHttpAdapterConfig.getHttpConnectTimeoutSeconds(),
                httpToMqttConfig,
                legacyHttpAdapterConfig.isAllowUntrustedCertificates(),
                tags);
    }
}
