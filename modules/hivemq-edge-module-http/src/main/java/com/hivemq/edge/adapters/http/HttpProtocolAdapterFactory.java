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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactory;
import com.hivemq.adapter.sdk.api.model.ProtocolAdapterInput;
import com.hivemq.edge.adapters.http.config.HttpAdapterConfig;
import com.hivemq.edge.adapters.http.config.HttpToMqttConfig;
import com.hivemq.edge.adapters.http.config.HttpToMqttMapping;
import com.hivemq.edge.adapters.http.config.legacy.LegacyHttpAdapterConfig;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * @author HiveMQ Adapter Generator
 */
public class HttpProtocolAdapterFactory implements ProtocolAdapterFactory<HttpAdapterConfig> {

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
    public @NotNull HttpAdapterConfig convertConfigObject(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        if (config.get("HttpToMqtt") != null || config.get("mqttToHttp") != null) {
            return ProtocolAdapterFactory.super.convertConfigObject(objectMapper, config);
        } else {
            return tryConvertLegacyConfig(objectMapper, config);
        }
    }

    @Override
    public @NotNull Class<HttpAdapterConfig> getConfigClass() {
        return HttpAdapterConfig.class;
    }

    private static @NotNull HttpAdapterConfig tryConvertLegacyConfig(
            final @NotNull ObjectMapper objectMapper, final @NotNull Map<String, Object> config) {
        final LegacyHttpAdapterConfig legacyHttpAdapterConfig =
                objectMapper.convertValue(config, LegacyHttpAdapterConfig.class);

        final HttpToMqttMapping httpToMqttMapping = new HttpToMqttMapping(legacyHttpAdapterConfig.getDestination(),
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
                        legacyHttpAdapterConfig.isAllowUntrustedCertificates(),
                        legacyHttpAdapterConfig.isAssertResponseIsJson(),
                        legacyHttpAdapterConfig.isHttpPublishSuccessStatusCodeOnly(),
                        List.of(httpToMqttMapping));

        return new HttpAdapterConfig(legacyHttpAdapterConfig.getId(),
                legacyHttpAdapterConfig.getUrl(),
                legacyHttpAdapterConfig.getHttpConnectTimeoutSeconds(),
                httpToMqttConfig);
    }
}
