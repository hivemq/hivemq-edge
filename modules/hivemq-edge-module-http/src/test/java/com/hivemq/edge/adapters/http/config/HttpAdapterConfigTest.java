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
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.http.HttpProtocolAdapterFactory;
import com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpHeader;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttConfig;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttMapping;
import com.hivemq.edge.adapters.http.config.mqtt2http.MqttToHttpConfig;
import com.hivemq.edge.adapters.http.config.mqtt2http.MqttToHttpMapping;
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
import static com.hivemq.edge.adapters.http.config.HttpAdapterConfig.HttpMethod.PUT;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class HttpAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService = mock();
    private final @NotNull EventService eventService = mock();


    @Test
    public void convertConfigObject_urlNull_exception() throws Exception {
        final URL resource = getClass().getResource("/http-config-url-null.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(new ProtocolAdapterFactoryTestInput(false));
        assertThatThrownBy(() -> httpProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("http"), false)).hasMessageContaining("Missing required creator property 'tagName'");
    }

    @Test
    public void convertConfigObject_idNull_exception() throws Exception {
        final URL resource = getClass().getResource("/http-config-id-null.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(new ProtocolAdapterFactoryTestInput(false));
        assertThatThrownBy(() -> httpProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("http"), false)).hasMessageContaining("Missing required creator property 'id'");
    }

    @Test
    public void convertConfigObject_defaults() throws Exception {
        final URL resource = getClass().getResource("/http-config-defaults.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(new ProtocolAdapterFactoryTestInput(true));
        final BidirectionalHttpAdapterConfig config =
                (BidirectionalHttpAdapterConfig) httpProtocolAdapterFactory.convertConfigObject(mapper,
                        (Map) adapters.get("http"), false);

        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(5);
        assertThat(config.isAllowUntrustedCertificates()).isFalse();

        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);

        final HttpToMqttMapping httpToMqttMapping = config.getHttpToMqttConfig().getMappings().get(0);
        assertThat(httpToMqttMapping.getTagName()).isEqualTo("tag1");
        assertThat(httpToMqttMapping.getMqttTopic()).isEqualTo("my/destination");
        assertThat(httpToMqttMapping.getMqttQos()).isEqualTo(1);
        assertThat(httpToMqttMapping.getHttpRequestMethod()).isEqualTo(GET);
        assertThat(httpToMqttMapping.getHttpRequestBodyContentType()).isEqualTo(JSON);
        assertThat(httpToMqttMapping.getHttpRequestBody()).isNull();
        assertThat(httpToMqttMapping.getHttpHeaders()).isEmpty();
        assertThat(httpToMqttMapping.getHttpRequestTimeoutSeconds()).isEqualTo(5);

        final MqttToHttpMapping mqttToHttpMapping = config.getMqttToHttpConfig().getMappings().get(0);
        assertThat(mqttToHttpMapping.getTagName()).isEqualTo("tag1");
        assertThat(mqttToHttpMapping.getMqttTopicFilter()).isEqualTo("my/#");
        assertThat(mqttToHttpMapping.getMqttMaxQos()).isEqualTo(1);
        assertThat(mqttToHttpMapping.getHttpRequestMethod()).isEqualTo(POST);
        assertThat(mqttToHttpMapping.getHttpHeaders()).isEmpty();
        assertThat(mqttToHttpMapping.getHttpRequestTimeoutSeconds()).isEqualTo(5);
    }

    @Test
    public void convertConfigObject_emptyHeaders() throws Exception {
        final URL resource = getClass().getResource("/http-config-empty-header.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(new ProtocolAdapterFactoryTestInput(false));
        final HttpAdapterConfig config =
                (HttpAdapterConfig) httpProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("http"), false);

        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(50);

        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);

        final HttpToMqttMapping httpToMqttMapping = config.getHttpToMqttConfig().getMappings().get(0);
        assertThat(httpToMqttMapping.getTagName()).isEqualTo("tag1");
        assertThat(httpToMqttMapping.getMqttQos()).isEqualTo(0);
        assertThat(httpToMqttMapping.getHttpRequestMethod()).isEqualTo(POST);
        assertThat(httpToMqttMapping.getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(httpToMqttMapping.getHttpRequestBody()).isNull();
        assertThat(httpToMqttMapping.getHttpHeaders()).isEmpty();
        assertThat(httpToMqttMapping.getMqttTopic()).isEqualTo("my/destination");
    }

    @Test
    public void convertConfigObject_full() throws Exception {
        final URL resource = getClass().getResource("/http-config-with-headers.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        assertThat(adapters.get("http")).isNotNull();

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(new ProtocolAdapterFactoryTestInput(true));
        final BidirectionalHttpAdapterConfig config =
                (BidirectionalHttpAdapterConfig) httpProtocolAdapterFactory.convertConfigObject(mapper,
                        (Map) adapters.get("http"), false);

        assertThat(config.getId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);
        assertThat(config.isAllowUntrustedCertificates()).isTrue();

        assertThat(config.getHttpToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("tag1");
            assertThat(mapping.getMqttTopic()).isEqualTo("my/destination");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
            assertThat(mapping.getHttpRequestMethod()).isEqualTo(GET);
            assertThat(mapping.getHttpRequestTimeoutSeconds()).isEqualTo(50);
            assertThat(mapping.getHttpRequestBodyContentType()).isEqualTo(YAML);
            assertThat(mapping.getHttpRequestBody()).isEqualTo("my-body");
            assertThat(mapping.getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
                assertThat(header1.getName()).isEqualTo("foo 1");
                assertThat(header1.getValue()).isEqualTo("bar 1");
            }, header2 -> {
                assertThat(header2.getName()).isEqualTo("foo 2");
                assertThat(header2.getValue()).isEqualTo("bar 2");
            });
            assertThat(mapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });
        }, mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("tag2");
            assertThat(mapping.getMqttTopic()).isEqualTo("my/destination2");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
            assertThat(mapping.getHttpRequestMethod()).isEqualTo(GET);
            assertThat(mapping.getHttpRequestBodyContentType()).isEqualTo(YAML);
            assertThat(mapping.getHttpRequestBody()).isEqualTo("my-body2");
            assertThat(mapping.getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
                assertThat(header1.getName()).isEqualTo("foo 1");
                assertThat(header1.getValue()).isEqualTo("bar 1");
            }, header2 -> {
                assertThat(header2.getName()).isEqualTo("foo 2");
                assertThat(header2.getValue()).isEqualTo("bar 2");
            });
            assertThat(mapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });
        });

        assertThat(config.getMqttToHttpConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("tag3");
            assertThat(mapping.getMqttTopicFilter()).isEqualTo("my/#");
            assertThat(mapping.getMqttMaxQos()).isEqualTo(0);
            assertThat(mapping.getHttpRequestMethod()).isEqualTo(POST);
            assertThat(mapping.getHttpRequestTimeoutSeconds()).isEqualTo(59);
            assertThat(mapping.getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
                assertThat(header1.getName()).isEqualTo("foo 1");
                assertThat(header1.getValue()).isEqualTo("bar 1");
            }, header2 -> {
                assertThat(header2.getName()).isEqualTo("foo 2");
                assertThat(header2.getValue()).isEqualTo("bar 2");
            });
        }, mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("tag4");
            assertThat(mapping.getMqttTopicFilter()).isEqualTo("my/#");
            assertThat(mapping.getMqttMaxQos()).isEqualTo(1);
            assertThat(mapping.getHttpRequestMethod()).isEqualTo(PUT);
            assertThat(mapping.getHttpRequestTimeoutSeconds()).isEqualTo(58);
            assertThat(mapping.getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
                assertThat(header1.getName()).isEqualTo("foo 1");
                assertThat(header1.getValue()).isEqualTo("bar 1");
            }, header2 -> {
                assertThat(header2.getName()).isEqualTo("foo 2");
                assertThat(header2.getValue()).isEqualTo("bar 2");
            });
        });
    }

    @Test
    public void unconvertConfigObject_full() throws Exception {
        final HttpHeader httpHeader1 = new HttpHeader("foo 1", "bar 1");
        final HttpHeader httpHeader2 = new HttpHeader("foo 2", "bar 2");

        final HttpToMqttConfig httpToMqttConfig = new HttpToMqttConfig(1337,
                11,
                true,
                true,
                List.of(new HttpToMqttMapping("tag1",
                                "my/destination",
                                0,
                                List.of(),
                                true,
                                POST,
                                1774,
                                YAML,
                                "my-body",
                                List.of(httpHeader2, httpHeader1)),
                        new HttpToMqttMapping("tag2",
                                "my/destination2",
                                0,
                                List.of(),
                                true,
                                POST,
                                59,
                                YAML,
                                "my-body",
                                List.of(httpHeader2, httpHeader1))));


        final MqttToHttpConfig mqttToHttpConfig = new MqttToHttpConfig(List.of(new MqttToHttpMapping("tag3",
                        "my0/#",
                        1,
                        POST,
                        11,
                        List.of(httpHeader1, httpHeader2)),
                new MqttToHttpMapping("tag4", "my1/#", 2, POST, 11, List.of(httpHeader1, httpHeader2))));

        final BidirectionalHttpAdapterConfig httpAdapterConfig =
                new BidirectionalHttpAdapterConfig("my-protocol-adapter", 50, httpToMqttConfig, mqttToHttpConfig, true);

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(new ProtocolAdapterFactoryTestInput(false));
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactlyInAnyOrder( //
                (it) -> assertThat(it.getKey()).isEqualTo("id"),
                (it) -> assertThat(it.getKey()).isEqualTo("allowUntrustedCertificates"),
                (it) -> assertThat(it.getKey()).isEqualTo("mqttToHttp"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeoutSeconds"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpToMqtt"));

        assertThat(config.get("id")).isEqualTo("my-protocol-adapter");
        assertThat(config.get("httpConnectTimeoutSeconds")).isEqualTo(50);
        assertThat((Boolean) config.get("allowUntrustedCertificates")).isTrue();

        final Map<String, Object> httpToMqtt = (Map<String, Object>) config.get("httpToMqtt");

        assertThat(httpToMqtt.get("pollingIntervalMillis")).isEqualTo(1337);
        assertThat(httpToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(11);
        assertThat((Boolean) httpToMqtt.get("httpPublishSuccessStatusCodeOnly")).isTrue();
        assertThat((Boolean) httpToMqtt.get("assertResponseIsJson")).isTrue();

        final Map<String, Object> mapping0 = (Map<String, Object>) ((List) httpToMqtt.get("httpToMqttMappings")).get(0);
        assertThat(mapping0.get("tagName")).isEqualTo("tag1");
        assertThat(mapping0.get("mqttTopic")).isEqualTo("my/destination");
        assertThat(mapping0.get("mqttQos")).isEqualTo(0);
        assertThat(mapping0.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(mapping0.get("httpRequestBodyContentType")).isEqualTo("YAML");
        assertThat(mapping0.get("httpRequestBody")).isEqualTo("my-body");
        assertThat(mapping0.get("httpRequestTimeoutSeconds")).isEqualTo(60);
        assertThat((List<Map<String, String>>) mapping0.get("httpHeaders")).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.get("name")).isEqualTo("foo 1");
            assertThat(header1.get("value")).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.get("name")).isEqualTo("foo 2");
            assertThat(header2.get("value")).isEqualTo("bar 2");
        });

        final Map<String, Object> mapping1 = (Map<String, Object>) ((List) httpToMqtt.get("httpToMqttMappings")).get(1);
        assertThat(mapping1.get("tagName")).isEqualTo("tag2");
        assertThat(mapping1.get("mqttTopic")).isEqualTo("my/destination2");
        assertThat(mapping1.get("mqttQos")).isEqualTo(0);
        assertThat(mapping1.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(mapping1.get("httpRequestBodyContentType")).isEqualTo("YAML");
        assertThat(mapping1.get("httpRequestBody")).isEqualTo("my-body");
        assertThat(mapping1.get("httpRequestTimeoutSeconds")).isEqualTo(59);
        assertThat((List<Map<String, String>>) mapping1.get("httpHeaders")).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.get("name")).isEqualTo("foo 1");
            assertThat(header1.get("value")).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.get("name")).isEqualTo("foo 2");
            assertThat(header2.get("value")).isEqualTo("bar 2");
        });

        final Map<String, Object> mqttToHttp = (Map<String, Object>) config.get("mqttToHttp");

        final Map<String, Object> mqttToHttpMapping0 =
                (Map<String, Object>) ((List) mqttToHttp.get("mqttToHttpMappings")).get(0);
        assertThat(mqttToHttpMapping0.get("tagName")).isEqualTo("tag3");
        assertThat(mqttToHttpMapping0.get("mqttTopicFilter")).isEqualTo("my0/#");
        assertThat(mqttToHttpMapping0.get("mqttMaxQos")).isEqualTo(1);
        assertThat(mqttToHttpMapping0.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(mqttToHttpMapping0.get("httpRequestTimeoutSeconds")).isEqualTo(11);
        assertThat((List<Map<String, String>>) mqttToHttpMapping0.get("httpHeaders")).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.get("name")).isEqualTo("foo 1");
            assertThat(header1.get("value")).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.get("name")).isEqualTo("foo 2");
            assertThat(header2.get("value")).isEqualTo("bar 2");
        });

        final Map<String, Object> mqttToHttpMapping1 =
                (Map<String, Object>) ((List) mqttToHttp.get("mqttToHttpMappings")).get(1);
        assertThat(mqttToHttpMapping1.get("tagName")).isEqualTo("tag4");
        assertThat(mqttToHttpMapping1.get("mqttTopicFilter")).isEqualTo("my1/#");
        assertThat(mqttToHttpMapping1.get("mqttMaxQos")).isEqualTo(2);
        assertThat(mqttToHttpMapping1.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(mqttToHttpMapping1.get("httpRequestTimeoutSeconds")).isEqualTo(11);
        assertThat((List<Map<String, String>>) mqttToHttpMapping1.get("httpHeaders")).satisfiesExactlyInAnyOrder(header1 -> {
            assertThat(header1.get("name")).isEqualTo("foo 1");
            assertThat(header1.get("value")).isEqualTo("bar 1");
        }, header2 -> {
            assertThat(header2.get("name")).isEqualTo("foo 2");
            assertThat(header2.get("value")).isEqualTo("bar 2");
        });
    }

    @Test
    public void unconvertConfigObject_defaults() {
        final HttpToMqttConfig httpToMqttConfig = new HttpToMqttConfig(null,
                null,
                null,
                null,
                List.of(new HttpToMqttMapping("tag1",
                        "my/destination",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));

        final MqttToHttpConfig mqttToHttpConfig =
                new MqttToHttpConfig(List.of(new MqttToHttpMapping("tag1", "my/#", null, null, null, null)));

        final BidirectionalHttpAdapterConfig httpAdapterConfig = new BidirectionalHttpAdapterConfig(
                "my-protocol-adapter",
                null,
                httpToMqttConfig,
                mqttToHttpConfig,
                null);

        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(new ProtocolAdapterFactoryTestInput(false));
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactlyInAnyOrder( //
                (it) -> assertThat(it.getKey()).isEqualTo("mqttToHttp"),
                (it) -> assertThat(it.getKey()).isEqualTo("allowUntrustedCertificates"),
                (it) -> assertThat(it.getKey()).isEqualTo("id"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeoutSeconds"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpToMqtt"));

        assertThat(config.get("id")).isEqualTo("my-protocol-adapter");
        assertThat(config.get("httpConnectTimeoutSeconds")).isEqualTo(5);
        assertThat((Boolean) config.get("allowUntrustedCertificates")).isFalse();

        final Map<String, Object> httpToMqtt = (Map<String, Object>) config.get("httpToMqtt");

        assertThat(httpToMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(httpToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat((Boolean) httpToMqtt.get("httpPublishSuccessStatusCodeOnly")).isTrue();
        assertThat((Boolean) httpToMqtt.get("assertResponseIsJson")).isFalse();

        final Map<String, Object> mapping = (Map<String, Object>) ((List) httpToMqtt.get("httpToMqttMappings")).get(0);
        assertThat(mapping.get("tagName")).isEqualTo("tag1");
        assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
        assertThat(mapping.get("mqttQos")).isEqualTo(0);
        assertThat(mapping.get("httpRequestMethod")).isEqualTo("GET");
        assertThat(mapping.get("httpRequestBodyContentType")).isEqualTo("JSON");
        assertThat(mapping.get("httpRequestBody")).isNull();
        assertThat(mapping.get("httpRequestTimeoutSeconds")).isEqualTo(5);
        assertThat((List<Map<String, String>>) mapping.get("httpHeaders")).isEmpty();


        final Map<String, Object> mqttToHttp = (Map<String, Object>) config.get("mqttToHttp");
        final Map<String, Object> mqttToHttpMapping =
                (Map<String, Object>) ((List) mqttToHttp.get("mqttToHttpMappings")).get(0);
        assertThat(mqttToHttpMapping.get("tagName")).isEqualTo("tag1");
        assertThat(mqttToHttpMapping.get("mqttTopicFilter")).isEqualTo("my/#");
        assertThat(mqttToHttpMapping.get("mqttMaxQos")).isEqualTo(1);
        assertThat(mqttToHttpMapping.get("httpRequestMethod")).isEqualTo("POST");
        assertThat(mqttToHttpMapping.get("httpRequestTimeoutSeconds")).isEqualTo(5);
        assertThat((List<Map<String, String>>) mqttToHttpMapping.get("httpHeaders")).isEmpty();
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


    private class ProtocolAdapterFactoryTestInput implements ProtocolAdapterFactoryInput {

        private final Boolean writingEnabled;

        private ProtocolAdapterFactoryTestInput(final Boolean writingEnabled) {
            this.writingEnabled = writingEnabled;
        }

        @Override
        public boolean isWritingEnabled() {
            return writingEnabled;
        }



        @Override
        public @NotNull ProtocolAdapterTagService protocolAdapterTagService() {
            return protocolAdapterTagService;
        }

        @Override
        public @NotNull EventService eventService() {
            return eventService;
        }
    }
}
