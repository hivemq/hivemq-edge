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
package com.hivemq.edge.adapters.http.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.http.HttpProtocolAdapterFactory;
import com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpHeader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpContentType.YAML;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpMethod.GET;
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpMethod.POST;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class HttpAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_urlNull_exception() throws Exception {
        final URL resource = getClass().getResource("/http-config-url-null.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        assertThatThrownBy(() -> httpProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("http"))).hasMessageContaining("Missing required creator property 'url'");
    }

    @Test
    public void convertConfigObject_httpToMqttNull_exception() throws Exception {
        final URL resource = getClass().getResource("/http-config-destination-null.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        assertThatThrownBy(() -> httpProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("http"))).hasMessageContaining("Missing required creator property 'httpToMqtt'");
    }

    @Test
    public void convertConfigObject_idNull_exception() throws Exception {
        final URL resource = getClass().getResource("/http-config-id-null.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        assertThatThrownBy(() -> httpProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("http"))).hasMessageContaining("Missing required creator property 'id'");
    }

    @Test
    public void convertConfigObject_defaults() throws Exception {
        final URL resource = getClass().getResource("/http-config-defaults.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final HttpAdapterConfig config =
                httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"));

        assertThat(config.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(5);

        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);

        final HttpToMqttMapping httpPollingContext = config.getHttpToMqttConfig().getMappings().get(0);

        assertThat(httpPollingContext.getMqttTopic()).isEqualTo("my/destination");
        assertThat(httpPollingContext.getMqttQos()).isEqualTo(1);
        assertThat(httpPollingContext.getHttpRequestMethod()).isEqualTo(GET);
        assertThat(httpPollingContext.getHttpRequestBodyContentType()).isEqualTo(JSON);
        assertThat(httpPollingContext.getHttpRequestBody()).isNull();
        assertThat(httpPollingContext.getHttpHeaders()).isEmpty();
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

        assertThat(config.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(1337);

        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);

        final HttpToMqttMapping httpPollingContext = config.getHttpToMqttConfig().getMappings().get(0);

        assertThat(httpPollingContext.getMqttQos()).isEqualTo(0);
        assertThat(httpPollingContext.getHttpRequestMethod()).isEqualTo(POST);
        assertThat(httpPollingContext.getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(httpPollingContext.getHttpRequestBody()).isNull();
        assertThat(httpPollingContext.getHttpHeaders()).isEmpty();
        assertThat(httpPollingContext.getMqttTopic()).isEqualTo("my/destination");
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

        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getUrl()).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(1337);
        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);

        final HttpToMqttMapping httpPollingContext = config.getHttpToMqttConfig().getMappings().get(0);

        assertThat(httpPollingContext.getMqttTopic()).isEqualTo("my/destination");
        assertThat(httpPollingContext.getMqttQos()).isEqualTo(0);
        assertThat(httpPollingContext.getHttpRequestMethod()).isEqualTo(GET);
        assertThat(httpPollingContext.getHttpRequestTimeout()).isEqualTo(1338);
        assertThat(httpPollingContext.getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(httpPollingContext.getHttpRequestBody()).isEqualTo("my-body");
        assertThat(httpPollingContext.getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.getName()).isEqualTo("foo 1");
            assertThat(header1.getValue()).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.getName()).isEqualTo("foo 2");
            assertThat(header2.getValue()).isEqualTo("bar 2");
        });
    }


    @Test
    public void unconvertConfigObject_full() throws Exception {
        final HttpHeader httpHeader1 = new HttpHeader("foo 1", "bar 1");
        final HttpHeader httpHeader2 = new HttpHeader("foo 2", "bar 2");

        final HttpAdapterConfig httpAdapterConfig = new HttpAdapterConfig("my-protocol-adapter",
                "http://192.168.0.02:777/?asdasd=asdasd",
                1773,
                new HttpToMqttConfig(1337,
                        11,
                        true,
                        true,
                        true,
                        List.of(new HttpToMqttMapping("my/destination",
                                        0,
                                        List.of(),
                                        true,
                                        POST,
                                        1774,
                                        YAML,
                                        "my-body",
                                        List.of(httpHeader2, httpHeader1)),
                                new HttpToMqttMapping("my/destination2",
                                        0,
                                        List.of(),
                                        true,
                                        POST,
                                        1774,
                                        YAML,
                                        "my-body",
                                        List.of(httpHeader2, httpHeader1)))));

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactly( //
                (it) -> assertThat(it.getKey()).isEqualTo("id"),
                (it) -> assertThat(it.getKey()).isEqualTo("url"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeout"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpToMqtt"));

        assertThat(config.get("id")).isEqualTo("my-protocol-adapter");
        assertThat(config.get("url")).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.get("httpConnectTimeout")).isEqualTo(1773);

        final Map<String, Object> httpToMqtt = (Map<String, Object>) config.get("httpToMqtt");

        assertThat(httpToMqtt.get("pollingIntervalMillis")).isEqualTo(1337);
        assertThat(httpToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(11);
        assertThat((Boolean) httpToMqtt.get("httpPublishSuccessStatusCodeOnly")).isTrue();
        assertThat((Boolean) httpToMqtt.get("allowUntrustedCertificates")).isTrue();
        assertThat((Boolean) httpToMqtt.get("assertResponseIsJson")).isTrue();

        final Map<String, Object> mapping =
                (Map<String, Object>) ((Map<String, Object>) ((List) httpToMqtt.get("httpToMqttMappings")).get(0)).get(
                        "httpToMqttMapping");

        assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
        assertThat(mapping.get("mqttQos")).isEqualTo(0);
        assertThat(mapping.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(mapping.get("httpRequestBodyContentType")).isEqualTo("YAML");
        assertThat(mapping.get("httpRequestBody")).isEqualTo("my-body");
        assertThat((List<Map<String, String>>) mapping.get("httpHeaders")).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.get("name")).isEqualTo("foo 1");
            assertThat(header1.get("value")).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.get("name")).isEqualTo("foo 2");
            assertThat(header2.get("value")).isEqualTo("bar 2");
        });
        final Map<String, Object> mapping2 =
                (Map<String, Object>) ((Map<String, Object>) ((List) httpToMqtt.get("httpToMqttMappings")).get(1)).get(
                        "httpToMqttMapping");

        assertThat(mapping2.get("mqttTopic")).isEqualTo("my/destination2");
        assertThat(mapping2.get("mqttQos")).isEqualTo(0);
        assertThat(mapping2.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(mapping2.get("httpRequestBodyContentType")).isEqualTo("YAML");
        assertThat(mapping2.get("httpRequestBody")).isEqualTo("my-body");
        assertThat((List<Map<String, String>>) mapping2.get("httpHeaders")).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.get("name")).isEqualTo("foo 1");
            assertThat(header1.get("value")).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.get("name")).isEqualTo("foo 2");
            assertThat(header2.get("value")).isEqualTo("bar 2");
        });
    }

    @Test
    public void unconvertConfigObject_defaults() {
        final HttpAdapterConfig httpAdapterConfig = new HttpAdapterConfig("my-protocol-adapter",
                "http://192.168.0.02:777/?asdasd=asdasd",
                null,
                new HttpToMqttConfig(null,
                        null,
                        null,
                        null,
                        null,
                        List.of(new HttpToMqttMapping("my/destination",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null))));

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactly( //
                (it) -> assertThat(it.getKey()).isEqualTo("id"),
                (it) -> assertThat(it.getKey()).isEqualTo("url"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeout"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpToMqtt"));

        assertThat(config.get("id")).isEqualTo("my-protocol-adapter");
        assertThat(config.get("url")).isEqualTo("http://192.168.0.02:777/?asdasd=asdasd");
        assertThat(config.get("httpConnectTimeout")).isEqualTo(5);

        final Map<String, Object> httpToMqtt = (Map<String, Object>) config.get("httpToMqtt");

        assertThat(httpToMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(httpToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat((Boolean) httpToMqtt.get("httpPublishSuccessStatusCodeOnly")).isTrue();
        assertThat((Boolean) httpToMqtt.get("allowUntrustedCertificates")).isFalse();
        assertThat((Boolean) httpToMqtt.get("assertResponseIsJson")).isFalse();

        final Map<String, Object> mapping =
                (Map<String, Object>) ((Map<String, Object>) ((List) httpToMqtt.get("httpToMqttMappings")).get(0)).get(
                        "httpToMqttMapping");

        assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
        assertThat(mapping.get("mqttQos")).isEqualTo(0);
        assertThat(mapping.get("httpRequestMethod")).isEqualTo("GET");
        assertThat(mapping.get("httpRequestBodyContentType")).isEqualTo("JSON");
        assertThat(mapping.get("httpRequestBody")).isNull();
        assertThat((List<Map<String, String>>) mapping.get("httpHeaders")).isEmpty();
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
