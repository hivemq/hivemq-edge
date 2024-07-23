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
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpContentType.YAML;
import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpMethod.GET;
import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpMethod.POST;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class HttpAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_defaults() throws Exception {
        final URL resource = getClass().getResource("/http-config-defaults.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final HttpAdapterConfig config =
                httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getQos()).isEqualTo(0);
        assertThat(config.getHttpRequestMethod()).isEqualTo(GET);
        assertThat(config.getHttpConnectTimeout()).isEqualTo(5);
        assertThat(config.getHttpRequestBodyContentType()).isEqualTo(JSON);
        assertThat(config.isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpHeaders()).isEmpty();
        assertThat(config.getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.getDestination()).isEqualTo("my/destination");
        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
    }

    @Test
    public void convertConfigObject_emptyHeaders() throws Exception {
        final URL resource = getClass().getResource("/http-config-empty-header.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final HttpAdapterConfig config =
                httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getQos()).isEqualTo(0);
        assertThat(config.getHttpRequestMethod()).isEqualTo(POST);
        assertThat(config.getHttpConnectTimeout()).isEqualTo(1337);
        assertThat(config.getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(config.isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpHeaders()).isNull();
        assertThat(config.getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);
        assertThat(config.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.getDestination()).isEqualTo("my/destination");
        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
    }

    @Test
    public void convertConfigObject_full() throws Exception {
        final URL resource = getClass().getResource("/http-config-with-headers.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        assertThat(adapters.get("http")).isNotNull();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final HttpAdapterConfig config =
                httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getQos()).isEqualTo(0);
        assertThat(config.getHttpRequestMethod()).isEqualTo(POST);
        assertThat(config.getHttpConnectTimeout()).isEqualTo(1337);
        assertThat(config.getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(config.isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.getName()).isEqualTo("foo 1");
            assertThat(header1.getValue()).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.getName()).isEqualTo("foo 2");
            assertThat(header2.getValue()).isEqualTo("bar 2");
        });
        assertThat(config.getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);
        assertThat(config.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.getDestination()).isEqualTo("my/destination");
        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
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
