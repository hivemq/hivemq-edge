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
package com.hivemq.edge.adapters.file.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.file.FileProtocolAdapterFactory;
import com.hivemq.edge.adapters.file.tag.FileTag;
import com.hivemq.edge.adapters.file.tag.FileTagDefinition;
import com.hivemq.protocols.AdapterConfigAndTags;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class FileAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);
        final AdapterConfigAndTags adapterConfigAndTags =
                AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("file"),
                        false,
                        mapper,
                        fileProtocolAdapterFactory);
        final FileAdapterConfig config = (FileAdapterConfig) adapterConfigAndTags.getAdapterConfig();
        assertThat(adapterConfigAndTags.missingTags())
                .isEmpty();

        assertThat(config.getId()).isEqualTo("my-file-protocol-adapter");
        assertThat(config.getFileToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getFileToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isFalse();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagName()).isEqualTo("tag1");

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
            assertThat(mapping.getIncludeTimestamp()).isFalse();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagName()).isEqualTo("tag2");

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
        final URL resource = getClass().getResource("/file-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);
        final AdapterConfigAndTags adapterConfigAndTags =
                AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("file"),
                        false,
                        mapper,
                        fileProtocolAdapterFactory);
        final FileAdapterConfig config = (FileAdapterConfig) adapterConfigAndTags.getAdapterConfig();
        assertThat(adapterConfigAndTags.missingTags())
                .isEmpty();

        assertThat(config.getId()).isEqualTo("my-file-protocol-adapter");
        assertThat(config.getFileToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getFileToMqttConfig().getMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMqttQos()).isEqualTo(0);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isTrue();
            assertThat(subscription.getIncludeTagNames()).isFalse();
            assertThat(subscription.getTagName()).isEqualTo("tag1");
        });

        assertThat(adapterConfigAndTags.missingTags()).isEmpty();
        assertThat(adapterConfigAndTags.getTags().stream().map(t -> (FileTag)t))
                .contains(new FileTag("1", "2", new FileTagDefinition("123", ContentType.BINARY)));
    }

    @Test
    public void convertConfigObject_defaults_missing_tag() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-minimal-config-missing-tag.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);
        final AdapterConfigAndTags adapterConfigAndTags =
                AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("file"),
                        false,
                        mapper,
                        fileProtocolAdapterFactory);
;
        assertThat(adapterConfigAndTags.missingTags())
                .isPresent()
                .hasValueSatisfying(set -> assertThat(set).contains("tag1"));

    }

    @Test
    public void convertConfigObject_contentTypeMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-content-type-missing.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);

        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("file"),
                false,
                mapper,
                fileProtocolAdapterFactory)).hasMessageContaining("Missing required creator property 'contentType'");
    }

    @Test
    public void convertConfigObject_tagNameMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-tag-name-missing.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);

        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("file"),
                false,
                mapper,
                fileProtocolAdapterFactory)).hasMessageContaining("Missing required creator property 'tagName'");
    }

    @Test
    public void convertConfigObject_mqttTopicMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-mqtt-topic-missing.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);

        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("file"),
                false,
                mapper,
                fileProtocolAdapterFactory)).hasMessageContaining("Missing required creator property 'mqttTopic'");
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final FileToMqttMapping pollingContext = new FileToMqttMapping("my/destination",
                1,
                MQTTMessagePerTag,
                false,
                true,
                List.of(new MqttUserProperty("my-name", "my-value")), "tag");

        final FileAdapterConfig modbusAdapterConfig =
                new FileAdapterConfig("my-modbus-adapter", new FileToMqttConfig(12, 13, List.of(pollingContext)));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);
        final Map<String, Object> config =
                fileProtocolAdapterFactory.unconvertConfigObject(mapper, modbusAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-modbus-adapter");
        final Map<String, Object> modbusToMqtt = (Map<String, Object>) config.get("fileToMqtt");
        assertThat(modbusToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(modbusToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat((List<Map<String, Object>>) modbusToMqtt.get("fileToMqttMappings")).satisfiesExactly((mapping) -> {
            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(1);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerTag");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(false);
            assertThat(mapping.get("includeTagNames")).isEqualTo(true);
            assertThat((List<Map<String, Object>>) mapping.get("mqttUserProperties")).satisfiesExactly((userProperty) -> {
                assertThat(userProperty.get("name")).isEqualTo("my-name");
                assertThat(userProperty.get("value")).isEqualTo("my-value");
            });
            assertThat(mapping.get("tagName")).isEqualTo("tag");
            assertThat(mapping.get("contentType")).isEqualTo("BINARY");
        });
    }

    @Test
    public void unconvertConfigObject_defaults_valid() {
        final FileToMqttMapping pollingContext = new FileToMqttMapping("my/destination",
                null,
                null,
                null,
                null,
                null, "tag");

        final FileAdapterConfig modbusAdapterConfig =
                new FileAdapterConfig("my-modbus-adapter", new FileToMqttConfig(null, null, List.of(pollingContext)));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory =
                new FileProtocolAdapterFactory(mockInput);
        final Map<String, Object> config =
                fileProtocolAdapterFactory.unconvertConfigObject(mapper, modbusAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-modbus-adapter");
        final Map<String, Object> modbusToMqtt = (Map<String, Object>) config.get("fileToMqtt");
        assertThat(modbusToMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(modbusToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat((List<Map<String, Object>>) modbusToMqtt.get("fileToMqttMappings")).satisfiesExactly((mapping) -> {
            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(0);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerTag");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(true);
            assertThat(mapping.get("includeTagNames")).isEqualTo(false);
            assertThat((List<Map<String, Object>>) mapping.get("mqttUserProperties")).isEmpty();
            assertThat(mapping.get("tagName")).isEqualTo("tag");
            assertThat(mapping.get("contentType")).isEqualTo("BINARY");
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
