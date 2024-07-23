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
import com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpContentType.YAML;
import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpMethod.GET;
import static com.hivemq.edge.adapters.http.HttpAdapterConfig.HttpMethod.POST;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class HttpAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_defaults() throws Exception {
        final URL resource = getClass().getResource("/http-config-defaults.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final HttpAdapterConfig config =
                httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getQos()).isEqualTo(0);
        assertThat(config.getHttpRequestMethod()).isEqualTo(GET);
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(5);
        assertThat(config.getHttpRequestBodyContentType()).isEqualTo(JSON);
        assertThat(config.getHttpRequestBody()).isNull();
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
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final HttpAdapterConfig config =
                httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getQos()).isEqualTo(0);
        assertThat(config.getHttpRequestMethod()).isEqualTo(POST);
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(1337);
        assertThat(config.getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(config.getHttpRequestBody()).isEqualTo("my-body");
        assertThat(config.isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpHeaders()).isEmpty();
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
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(1337);
        assertThat(config.getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(config.getHttpRequestBody()).isEqualTo("my-body");
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


    @Test
    public void unconvertConfigObject_full() throws Exception {
        final HttpHeader httpHeader1 = new HttpHeader("foo 1", "bar 1");
        final HttpHeader httpHeader2 = new HttpHeader("foo 2", "bar 2");

        final HttpAdapterConfig httpAdapterConfig = new HttpAdapterConfig("http://192.168.0.02:777/?asdasd=asdasd",
                "my/destination",
                0,
                POST,
                1773,
                YAML,
                "my-body",
                true,
                false,
                List.of(httpHeader2, httpHeader1),
                "my-protocol-adapter",
                11,
                true,
                1337);

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactly( //
                (it) -> assertThat(it.getKey()).isEqualTo("url"),
                (it) -> assertThat(it.getKey()).isEqualTo("destination"),
                (it) -> assertThat(it.getKey()).isEqualTo("qos"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpRequestMethod"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeout"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpRequestBodyContentType"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpRequestBody"),
                (it) -> assertThat(it.getKey()).isEqualTo("assertResponseIsJson"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpPublishSuccessStatusCodeOnly"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpHeaders"),
                (it) -> assertThat(it.getKey()).isEqualTo("id"),
                (it) -> assertThat(it.getKey()).isEqualTo("maxPollingErrorsBeforeRemoval"),
                (it) -> assertThat(it.getKey()).isEqualTo("allowUntrustedCertificates"),
                (it) -> assertThat(it.getKey()).isEqualTo("pollingIntervalMillis"));

        assertThat(config.get("id")).isEqualTo("my-protocol-adapter");
        assertThat(config.get("pollingIntervalMillis")).isEqualTo(1337);
        assertThat(config.get("maxPollingErrorsBeforeRemoval")).isEqualTo(11);
        assertThat(config.get("url")).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.get("destination")).isEqualTo("my/destination");
        assertThat(config.get("qos")).isEqualTo(0);
        assertThat(config.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(config.get("httpRequestBodyContentType")).isEqualTo("YAML");
        assertThat(config.get("httpRequestBody")).isEqualTo("my-body");
        assertThat(config.get("httpConnectTimeout")).isEqualTo(1773);
        assertThat((List<Map<String, String>>) config.get("httpHeaders")).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.get("name")).isEqualTo("foo 1");
            assertThat(header1.get("value")).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.get("name")).isEqualTo("foo 2");
            assertThat(header2.get("value")).isEqualTo("bar 2");
        });
        assertThat((Boolean) config.get("httpPublishSuccessStatusCodeOnly")).isFalse();
        assertThat((Boolean) config.get("allowUntrustedCertificates")).isTrue();
        assertThat((Boolean) config.get("assertResponseIsJson")).isTrue();
    }

    @Test
    public void unconvertConfigObject_defaults() {
        final HttpAdapterConfig httpAdapterConfig = new HttpAdapterConfig("http://192.168.0.02:777/?asdasd=asdasd",
                "my/destination",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "my-protocol-adapter",
                null,
                null,
                null);

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactly( //
                (it) -> assertThat(it.getKey()).isEqualTo("url"),
                (it) -> assertThat(it.getKey()).isEqualTo("destination"),
                (it) -> assertThat(it.getKey()).isEqualTo("qos"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpRequestMethod"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeout"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpRequestBodyContentType"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpRequestBody"),
                (it) -> assertThat(it.getKey()).isEqualTo("assertResponseIsJson"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpPublishSuccessStatusCodeOnly"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpHeaders"),
                (it) -> assertThat(it.getKey()).isEqualTo("id"),
                (it) -> assertThat(it.getKey()).isEqualTo("maxPollingErrorsBeforeRemoval"),
                (it) -> assertThat(it.getKey()).isEqualTo("allowUntrustedCertificates"),
                (it) -> assertThat(it.getKey()).isEqualTo("pollingIntervalMillis"));

        assertThat(config.get("id")).isEqualTo("my-protocol-adapter");
        assertThat(config.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(config.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat(config.get("url")).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.get("destination")).isEqualTo("my/destination");
        assertThat(config.get("qos")).isEqualTo(0);
        assertThat(config.get("httpRequestMethod")).isEqualTo("GET");
        assertThat(config.get("httpRequestBodyContentType")).isEqualTo("JSON");
        assertThat(config.get("httpRequestBody")).isNull();
        assertThat(config.get("httpConnectTimeout")).isEqualTo(5);
        assertThat((List<Map<String, String>>) config.get("httpHeaders")).isEmpty();
        assertThat((Boolean) config.get("httpPublishSuccessStatusCodeOnly")).isTrue();
        assertThat((Boolean) config.get("allowUntrustedCertificates")).isFalse();
        assertThat((Boolean) config.get("assertResponseIsJson")).isFalse();
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
