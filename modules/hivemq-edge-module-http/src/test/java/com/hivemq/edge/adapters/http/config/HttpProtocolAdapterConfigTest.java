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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.NorthboundMappingEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.entity.adapter.SouthboundMappingEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.http.HttpProtocolAdapterFactory;
import com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpHeader;
import com.hivemq.edge.adapters.http.config.http2mqtt.HttpToMqttConfig;
import com.hivemq.edge.adapters.http.config.mqtt2http.MqttToHttpConfig;
import com.hivemq.edge.adapters.http.config.mqtt2http.MqttToHttpMapping;
import com.hivemq.edge.adapters.http.tag.HttpTag;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.persistence.mappings.NorthboundMapping;
import com.hivemq.protocols.ProtocolAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigConverter;
import com.hivemq.protocols.ProtocolAdapterFactoryManager;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpContentType.JSON;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpContentType.YAML;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpMethod.GET;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpMethod.POST;
import static com.hivemq.edge.adapters.http.config.HttpSpecificAdapterConfig.HttpMethod.PUT;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
public class HttpProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_tagNameNull_exception() throws Exception {
        final URL resource = getClass().getResource("/http-config-tagname-null.xml");
        final File path = Path.of(resource.toURI()).toFile();
        assertThatThrownBy(() -> loadConfig(path))
                .isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void convertConfigObject_adapterIdNull_exception() throws Exception {
        final URL resource = getClass().getResource("/http-config-id-null.xml");
        final File path = Path.of(resource.toURI()).toFile();

        assertThatThrownBy(() -> loadConfig(path))
                .isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void convertConfigObject_defaults() throws Exception {
        final URL resource = getClass().getResource("/http-config-defaults.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final ProtocolAdapterEntity adapter = configEntity.getProtocolAdapterConfig().get(0);

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(mockInput);

        final BidirectionalHttpSpecificAdapterConfig config =
                (BidirectionalHttpSpecificAdapterConfig)httpProtocolAdapterFactory.convertConfigObject(mapper, adapter.getConfig(), true);

        final List<Map<String, Object>> tagMaps =
                adapter.getTags().stream().map(tagEntity -> tagEntity.toMap()).collect(Collectors.toList());
        final List<? extends Tag> tags = httpProtocolAdapterFactory.convertTagDefinitionObjects(mapper, tagMaps);

        assertThat(adapter.getAdapterId()).isEqualTo("my-protocol-adapter");
        assertThat(adapter.getProtocolId()).isEqualTo("http");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(5);
        assertThat(config.isAllowUntrustedCertificates()).isFalse();

        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);

        final NorthboundMappingEntity httpToMqttMapping = adapter.getNorthboundMappingEntities().get(0);
        assertThat(httpToMqttMapping.getTagName()).isEqualTo("tag1");
        assertThat(httpToMqttMapping.getTopic()).isEqualTo("my/destination");
        assertThat(httpToMqttMapping.getMaxQoS()).isEqualTo(1);

        final HttpTag tag = (HttpTag)tags.get(0);
        assertThat(tag.getName()).isEqualTo("tag1");
        assertThat(tag.getDefinition().getHttpRequestMethod()).isEqualTo(GET);
        assertThat(tag.getDefinition().getHttpRequestBodyContentType()).isEqualTo(JSON);
        assertThat(tag.getDefinition().getHttpRequestBody()).isNull();
        assertThat(tag.getDefinition().getHttpHeaders()).isEmpty();
        assertThat(tag.getDefinition().getHttpRequestTimeoutSeconds()).isEqualTo(5);

        final HttpTag tag2 = (HttpTag)tags.get(0);
        assertThat(tag2.getName()).isEqualTo("tag1");
        assertThat(tag2.getDefinition().getHttpRequestMethod()).isEqualTo(GET);
        assertThat(tag2.getDefinition().getHttpRequestBodyContentType()).isEqualTo(JSON);
        assertThat(tag2.getDefinition().getHttpRequestBody()).isNull();
        assertThat(tag2.getDefinition().getHttpHeaders()).isEmpty();
        assertThat(tag2.getDefinition().getHttpRequestTimeoutSeconds()).isEqualTo(5);

        final SouthboundMappingEntity mqttToHttpMapping = adapter.getSouthboundMappingEntities().get(0);
        assertThat(mqttToHttpMapping.getTagName()).isEqualTo("tag2");
        assertThat(mqttToHttpMapping.getTopicFilter()).isEqualTo("my/#");
        assertThat(mqttToHttpMapping.getMaxQos()).isEqualTo(1);
    }

    @Test
    public void convertConfigObject_missingTag() throws Exception {
        final URL resource = getClass().getResource("/http-config-defaults-missing-tag.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        assertThat(protocolAdapterConfig.missingTags())
                .isPresent()
                .hasValueSatisfying(set -> assertThat(set).contains("tag1"));
    }

    @Test
    public void convertConfigObject_emptyHeaders() throws Exception {
        final URL resource = getClass().getResource("/http-config-empty-header.xml");

        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final BidirectionalHttpSpecificAdapterConfig config = (BidirectionalHttpSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpConnectTimeoutSeconds()).isEqualTo(50);

        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);

        final NorthboundMapping httpToMqttMapping = protocolAdapterConfig.getFromEdgeMappings().get(0);
        assertThat(httpToMqttMapping.getTagName()).isEqualTo("tag1");
        assertThat(httpToMqttMapping.getMqttQos()).isEqualTo(0);

        final HttpTag tag = (HttpTag) protocolAdapterConfig.getTags().get(0);
        assertThat(tag.getDefinition().getHttpRequestMethod()).isEqualTo(POST);
        assertThat(tag.getDefinition().getHttpRequestBodyContentType()).isEqualTo(YAML);
        assertThat(tag.getDefinition().getHttpRequestBody()).isNull();
        assertThat(tag.getDefinition().getHttpHeaders()).isEmpty();
        assertThat(httpToMqttMapping.getMqttTopic()).isEqualTo("my/destination");
    }

    @Test
    public void convertConfigObject_full() throws Exception {
        final URL resource = getClass().getResource("/http-config-with-headers.xml");

        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final BidirectionalHttpSpecificAdapterConfig config = (BidirectionalHttpSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-protocol-adapter");
        assertThat(config.getHttpToMqttConfig().isHttpPublishSuccessStatusCodeOnly()).isTrue();
        assertThat(config.getHttpToMqttConfig().getPollingIntervalMillis()).isEqualTo(1773);
        assertThat(config.getHttpToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(13);
        assertThat(config.isAllowUntrustedCertificates()).isTrue();

        assertThat(protocolAdapterConfig.getFromEdgeMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("tag1");
            assertThat(mapping.getMqttTopic()).isEqualTo("my/destination");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
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
            assertThat(mapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });
        });

        assertThat(protocolAdapterConfig.getToEdgeMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("tag3");
            assertThat(mapping.getTopicFilter()).isEqualTo("my/#");
            assertThat(mapping.getMaxQoS()).isEqualTo(0);
        }, mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("tag4");
            assertThat(mapping.getTopicFilter()).isEqualTo("my/#");
            assertThat(mapping.getMaxQoS()).isEqualTo(1);
        });

        assertThat(protocolAdapterConfig.getTags().stream().map(tag -> (HttpTag)tag).collect(Collectors.toList()))
                .satisfiesExactly(
                        tag -> {
                            assertThat(tag.getName()).isEqualTo("tag1");
                            assertThat(tag.getDefinition().getHttpRequestMethod()).isEqualTo(GET);
                            assertThat(tag.getDefinition().getHttpRequestTimeoutSeconds()).isEqualTo(50);
                            assertThat(tag.getDefinition().getHttpRequestBodyContentType()).isEqualTo(YAML);
                            assertThat(tag.getDefinition().getHttpRequestBody()).isEqualTo("my-body");
                            assertThat(tag.getDefinition().getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
                                assertThat(header1.getName()).isEqualTo("foo 1");
                                assertThat(header1.getValue()).isEqualTo("bar 1");
                            }, header2 -> {
                                assertThat(header2.getName()).isEqualTo("foo 2");
                                assertThat(header2.getValue()).isEqualTo("bar 2");
                            });
                        },
                        tag -> {
                            assertThat(tag.getName()).isEqualTo("tag2");
                            assertThat(tag.getDefinition().getHttpRequestMethod()).isEqualTo(GET);
                            assertThat(tag.getDefinition().getHttpRequestBodyContentType()).isEqualTo(YAML);
                            assertThat(tag.getDefinition().getHttpRequestBody()).isEqualTo("my-body2");
                            assertThat(tag.getDefinition().getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
                                assertThat(header1.getName()).isEqualTo("foo 1");
                                assertThat(header1.getValue()).isEqualTo("bar 1");
                            }, header2 -> {
                                assertThat(header2.getName()).isEqualTo("foo 2");
                                assertThat(header2.getValue()).isEqualTo("bar 2");
                            });
                        },
                        tag -> {
                            assertThat(tag.getName()).isEqualTo("tag3");
                            assertThat(tag.getDefinition().getHttpRequestMethod()).isEqualTo(POST);
                            assertThat(tag.getDefinition().getHttpRequestTimeoutSeconds()).isEqualTo(59);
                            assertThat(tag.getDefinition().getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
                                assertThat(header1.getName()).isEqualTo("foo 1");
                                assertThat(header1.getValue()).isEqualTo("bar 1");
                            }, header2 -> {
                                assertThat(header2.getName()).isEqualTo("foo 2");
                                assertThat(header2.getValue()).isEqualTo("bar 2");
                            });
                        },
                        tag -> {
                            assertThat(tag.getName()).isEqualTo("tag4");
                            assertThat(tag.getDefinition().getHttpRequestMethod()).isEqualTo(PUT);
                            assertThat(tag.getDefinition().getHttpRequestTimeoutSeconds()).isEqualTo(58);
                            assertThat(tag.getDefinition().getHttpHeaders()).satisfiesExactlyInAnyOrder(header1 -> {
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

        final BidirectionalHttpSpecificAdapterConfig httpAdapterConfig = new BidirectionalHttpSpecificAdapterConfig(
                50,
                new HttpToMqttConfig(
                        1337,
                        11,
                        true,
                        true
                ),
                new MqttToHttpConfig(List.of(
                        new MqttToHttpMapping(
                        "tag3",
                        "my0/#",
                        1,
                            POST,
                            12,
                            List.of(
                                    new HttpHeader("foo 1", "bar 1"),
                                    new HttpHeader("foo 2", "bar 2")
                            )
                        ),
                        new MqttToHttpMapping(
                                "tag4",
                                "my1/#",
                                2,
                                POST,
                                11,
                                List.of(
                                        new HttpHeader("foo 1", "bar 1"),
                                        new HttpHeader("foo 2", "bar 2")
                                )
                        ))),
                true
                );

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactlyInAnyOrder(
                (it) -> assertThat(it.getKey()).isEqualTo("allowUntrustedCertificates"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeoutSeconds"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpToMqtt")
        );

        assertThat(config.get("httpConnectTimeoutSeconds")).isEqualTo(50);
        assertThat((Boolean) config.get("allowUntrustedCertificates")).isTrue();

        final Map<String, Object> httpToMqtt = (Map<String, Object>) config.get("httpToMqtt");

        assertThat(httpToMqtt.get("pollingIntervalMillis")).isEqualTo(1337);
        assertThat(httpToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(11);
        assertThat((Boolean) httpToMqtt.get("httpPublishSuccessStatusCodeOnly")).isTrue();
        assertThat((Boolean) httpToMqtt.get("assertResponseIsJson")).isTrue();

        assertThat(httpToMqtt.get("httpToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML

        assertThat(config.get("mqttToHttp")).isNull(); //mappings are supposed to be ignored when rendered to XML
    }

    @Test
    public void unconvertConfigObject_defaults() {

        final BidirectionalHttpSpecificAdapterConfig httpAdapterConfig = new BidirectionalHttpSpecificAdapterConfig(
                null,
                new HttpToMqttConfig(
                        null,
                        null,
                        null,
                        null
                ),
                new MqttToHttpConfig(List.of(
                    new MqttToHttpMapping(
                            "tag1",
                            "my/#",
                            null,
                            null,
                            null,
                            null
                    ))),
                null
        );

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final HttpProtocolAdapterFactory httpProtocolAdapterFactory =
                new HttpProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = httpProtocolAdapterFactory.unconvertConfigObject(mapper, httpAdapterConfig);

        assertThat(config.entrySet()).satisfiesExactlyInAnyOrder(
                (it) -> assertThat(it.getKey()).isEqualTo("httpToMqtt"),
                (it) -> assertThat(it.getKey()).isEqualTo("allowUntrustedCertificates"),
                (it) -> assertThat(it.getKey()).isEqualTo("httpConnectTimeoutSeconds")
        );

        assertThat(config.get("httpConnectTimeoutSeconds")).isEqualTo(5);
        assertThat((Boolean) config.get("allowUntrustedCertificates")).isFalse();

        final Map<String, Object> httpToMqtt = (Map<String, Object>) config.get("httpToMqtt");

        assertThat(httpToMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(httpToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat((Boolean) httpToMqtt.get("httpPublishSuccessStatusCodeOnly")).isTrue();
        assertThat((Boolean) httpToMqtt.get("assertResponseIsJson")).isFalse();

        assertThat(httpToMqtt.get("httpToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML

        assertThat(config.get("mqttToHttp")).isNull(); //mappings are supposed to be ignored when rendered to XML
    }

    private @NotNull ProtocolAdapterConfig getProtocolAdapterConfig(final @NotNull URL resource) throws URISyntaxException {
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final ProtocolAdapterEntity adapterEntity = configEntity.getProtocolAdapterConfig().get(0);

        final ProtocolAdapterConfigConverter converter = createConverter();

        return converter.fromEntity(adapterEntity);
    }

    private @NotNull ProtocolAdapterConfigConverter createConverter() {
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory(mockInput);
        ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("http")).thenReturn(Optional.of(httpProtocolAdapterFactory));
        ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, mapper);
        return converter;
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
