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
package com.hivemq.edge.adapters.http.config.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.http.HttpProtocolAdapterFactory;
import com.hivemq.edge.adapters.http.config.HttpAdapterConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttMapping;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpContentType.YAML;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpMethod.GET;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class LegacyHttpAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_defaults() throws Exception {
        final URL resource = getClass().getResource("/legacy-http-config-minimal.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory(false);
        final HttpAdapterConfig config =
                (HttpAdapterConfig) httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(5);

        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);

        final HttpToMqttMapping httpToMqttMapping = config.getHttpToMqttConfig().getMappings().get(0);
        assertThat(httpToMqttMapping.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(httpToMqttMapping.getMqttTopic()).isEqualTo("my/destination");
        assertThat(httpToMqttMapping.getMqttQos()).isEqualTo(1);
        assertThat(httpToMqttMapping.getHttpRequestMethod()).isEqualTo(GET);
        assertThat(httpToMqttMapping.getHttpRequestBodyContentType()).isEqualTo(JSON);
        assertThat(httpToMqttMapping.getHttpRequestBody()).isNull();
        assertThat(httpToMqttMapping.getHttpHeaders()).isEmpty();
    }

    @Test
    public void convertConfigObject_full() throws Exception {
        final URL resource = getClass().getResource("/legacy-http-config-with-headers.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        assertThat(adapters.get("http")).isNotNull();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory(false);
        final HttpAdapterConfig config =
                (HttpAdapterConfig) httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(50);
        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);

        assertThat(config.getHttpToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
            assertThat(mapping.getMqttTopic()).isEqualTo("my/destination");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
            assertThat(mapping.getHttpRequestMethod()).isEqualTo(GET);
            assertThat(mapping.getHttpRequestTimeoutSeconds()).isEqualTo(50);
            assertThat(mapping.getHttpRequestBodyContentType()).isEqualTo(YAML);
            assertThat(mapping.getHttpRequestBody()).isEqualTo("my-body");
        });
    }

    private @NotNull HiveMQConfigEntity loadConfig(final @NotNull File configFile) {
        final ConfigFileReaderWriter readerWriter = new ConfigFileReaderWriter(new ConfigurationFile(configFile),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock(),
                mock());
        return readerWriter.applyConfig();
    }
}
