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
package com.hivemq.edge.adapters.plc4x.types.ads.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTag;
import com.hivemq.edge.adapters.plc4x.config.tag.Plc4xTagDefinition;
import com.hivemq.edge.adapters.plc4x.types.ads.ADSProtocolAdapterFactory;
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

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ADSProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/ads-adapter-full-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final ADSSpecificAdapterConfig config = (ADSSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-ads-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.ads-server.com");
        assertThat(config.getTargetAmsPort()).isEqualTo(1234);
        assertThat(config.getSourceAmsPort()).isEqualTo(12345);
        assertThat(config.getTargetAmsNetId()).isEqualTo("1.2.3.4.5.6");
        assertThat(config.getSourceAmsNetId()).isEqualTo("1.2.3.4.5.7");
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(protocolAdapterConfig.getFromEdgeMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagName()).isEqualTo("tag-name");

            assertThat(mapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });
        }, mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagName()).isEqualTo("tag-name");

            assertThat(mapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });
        });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/ads-adapter-minimal-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final ADSSpecificAdapterConfig config = (ADSSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-ads-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.ads-server.com");
        assertThat(config.getTargetAmsPort()).isEqualTo(123);
        assertThat(config.getSourceAmsPort()).isEqualTo(124);
        assertThat(config.getTargetAmsNetId()).isEqualTo("1.2.3.4.5.6");
        assertThat(config.getSourceAmsNetId()).isEqualTo("1.2.3.4.5.7");
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(protocolAdapterConfig.getFromEdgeMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagName()).isEqualTo("tag-name");
        });

        assertThat(protocolAdapterConfig.missingTags()).isEmpty();

        assertThat(protocolAdapterConfig.getTags().stream().map(t -> (Plc4xTag)t))
            .containsExactly(new Plc4xTag("tag-name", "description", new Plc4xTagDefinition("123", Plc4xDataType.DATA_TYPE.BOOL)));
    }

    @Test
    public void convertConfigObject_defaults_missing_tag() throws Exception {
        final URL resource = getClass().getResource("/ads-adapter-minimal-missing-tag-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        assertThat(protocolAdapterConfig.missingTags())
                .isPresent()
                .hasValueSatisfying(set -> assertThat(set).contains("tag-name"));
    }


    @Test
    public void unconvertConfigObject_full_valid() {


        final ADSSpecificAdapterConfig adapterConfig = new ADSSpecificAdapterConfig(
                14,
                "my.host.com",
                15,
                16,
                "1.2.3.4.5.6",
                "1.2.3.4.5.7",
                new ADSToMqttConfig(
                        12,
                        13,
                        true));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ADSProtocolAdapterFactory adsProtocolAdapterFactory =
                new ADSProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = adsProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("targetAmsPort")).isEqualTo(15);
        assertThat(config.get("sourceAmsPort")).isEqualTo(16);
        assertThat(config.get("targetAmsNetId")).isEqualTo("1.2.3.4.5.6");
        assertThat(config.get("sourceAmsNetId")).isEqualTo("1.2.3.4.5.7");
        final Map<String, Object> adsToMqtt = (Map<String, Object>) config.get("adsToMqtt");
        assertThat(adsToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(adsToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(adsToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) adsToMqtt.get("adsToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML
    }

    private @NotNull ProtocolAdapterConfig getProtocolAdapterConfig(final @NotNull URL resource) throws
            URISyntaxException {
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final ProtocolAdapterEntity adapterEntity = configEntity.getProtocolAdapterConfig().get(0);

        final ProtocolAdapterConfigConverter converter = createConverter();

        return converter.fromEntity(adapterEntity);
    }

    private @NotNull ProtocolAdapterConfigConverter createConverter() {
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        ADSProtocolAdapterFactory protocolAdapterFactory = new ADSProtocolAdapterFactory(mockInput);
        ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("ads")).thenReturn(Optional.of(protocolAdapterFactory));
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
