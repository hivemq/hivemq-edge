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
package com.hivemq.edge.adapters.etherip.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.etherip.EipProtocolAdapterFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EipProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/eip-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final @NotNull List<ProtocolAdapterEntity> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final EipProtocolAdapterFactory eipProtocolAdapterFactory = new EipProtocolAdapterFactory(mockInput);

        /*
        final ProtocolAdapterConfig protocolAdapterConfig =
                ProtocolAdapterConfig.fro((Map<String, Object>) adapters.get("eip"),
                        false,
                        mapper,
                        eipProtocolAdapterFactory);
        final EipSpecificAdapterConfig config = (EipSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(config.getId()).isEqualTo("my-eip-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.eip-server.com");
        assertThat(config.getBackplane()).isEqualTo(4);
        assertThat(config.getSlot()).isEqualTo(5);
        assertThat(config.getEipToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getEipToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getEipToMqttConfig().getPublishChangedDataOnly()).isFalse();


        assertThat(config.getEipToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
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

        final List<? extends Tag> tags = eipProtocolAdapterFactory.convertTagDefinitionObjects(mapper,
                (List<Map<String, Object>>) ((Map<String, Object>) adapters.get("eip")).get("tags"));

        assertThat(tags)
                .allSatisfy(t -> {
                    assertThat(t)
                            .isInstanceOf(EipTag.class)
                            .extracting(Tag::getName, Tag::getDescription, Tag::getDefinition)
                            .contains("tag-name", "description", new EipTagDefinition("addressy", EipDataType.BOOL));
                });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/eip-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final EipProtocolAdapterFactory eipProtocolAdapterFactory =
                new EipProtocolAdapterFactory(mockInput);
        final ProtocolAdapterConfig protocolAdapterConfig =
                ProtocolAdapterConfig.fromAdapterConfigMap((Map<String, Object>) adapters.get("eip"),
                        false,
                        mapper,
                        eipProtocolAdapterFactory);
        final EipSpecificAdapterConfig config = (EipSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(config.getId()).isEqualTo("my-eip-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.eip-server.com");
        assertThat(config.getBackplane()).isEqualTo(1);
        assertThat(config.getSlot()).isEqualTo(0);
        assertThat(config.getEipToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getEipToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getEipToMqttConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(config.getEipToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagName()).isEqualTo("tag-name");
        });

        final List<? extends Tag> tags = eipProtocolAdapterFactory.convertTagDefinitionObjects(mapper,
                (List<Map<String, Object>>) ((Map<String, Object>) adapters.get("eip")).get("tags"));

        assertThat(tags)
                .allSatisfy(t -> {
                    assertThat(t)
                            .isInstanceOf(EipTag.class)
                            .extracting(Tag::getName, Tag::getDescription, Tag::getDefinition)
                            .contains("tag-name", "description", new EipTagDefinition("addressy", EipDataType.BOOL));
                });
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final EipToMqttMapping pollingContext = new EipToMqttMapping("my/destination",
                1,
                MQTTMessagePerSubscription,
                false,
                true,
                "tag-name",
                List.of(new MqttUserProperty("my-name", "my-value")));

        final EipSpecificAdapterConfig eipAdapterConfig = new EipSpecificAdapterConfig("my-eip-adapter",
                14,
                "my.host.com",
                15,
                16,
                new EipToMqttConfig(12, 13, true, List.of(pollingContext)));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final EipProtocolAdapterFactory eipProtocolAdapterFactory =
                new EipProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = eipProtocolAdapterFactory.unconvertConfigObject(mapper, eipAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-eip-adapter");
        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("backplane")).isEqualTo(15);
        assertThat(config.get("slot")).isEqualTo(16);
        final Map<String, Object> eipToMqtt = (Map<String, Object>) config.get("eipToMqtt");
        assertThat(eipToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(eipToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(eipToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) eipToMqtt.get("eipToMqttMappings")).satisfiesExactly((mapping) -> {

            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(1);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(false);
            assertThat(mapping.get("includeTagNames")).isEqualTo(true);
            assertThat(mapping.get("tagName")).isEqualTo("tag-name");
            assertThat(mapping.get("jsonPayloadCreator")).isNull();
            assertThat((List<Map<String, Object>>) mapping.get("mqttUserProperties")).satisfiesExactly((userProperty) -> {
                assertThat(userProperty.get("name")).isEqualTo("my-name");
                assertThat(userProperty.get("value")).isEqualTo("my-value");
            });
        });
    }
*/

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