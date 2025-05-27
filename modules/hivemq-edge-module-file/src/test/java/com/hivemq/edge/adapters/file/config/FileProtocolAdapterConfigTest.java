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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.file.FileProtocolAdapterFactory;
import com.hivemq.edge.adapters.file.tag.FileTag;
import com.hivemq.edge.adapters.file.tag.FileTagDefinition;
import com.hivemq.exceptions.UnrecoverableException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class FileProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-full-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        final FileSpecificAdapterConfig config = (FileSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags()).isEmpty();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-file-protocol-adapter");
        assertThat(config.getFileToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);

        assertThat(protocolAdapterConfig.getNorthboundMappings()).satisfiesExactly(mapping -> {
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
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        final FileSpecificAdapterConfig config = (FileSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags()).isEmpty();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-file-protocol-adapter");
        assertThat(config.getFileToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);


        assertThat(protocolAdapterConfig.getNorthboundMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMqttQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isTrue();
            assertThat(subscription.getIncludeTagNames()).isFalse();
            assertThat(subscription.getTagName()).isEqualTo("tag1");
        });

        assertThat(protocolAdapterConfig.getTags().stream().map(t -> (FileTag) t)).contains(new FileTag("tag1",
                "decsription",
                new FileTagDefinition("pathy", ContentType.BINARY)));
    }

    @Test
    public void convertConfigObject_defaults_missing_tag() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-minimal-config-missing-tag.xml");
        assertThatThrownBy(() -> getProtocolAdapterConfig(resource)).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void convertConfigObject_tagNameMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-tag-name-missing.xml");

        assertThatThrownBy(() -> getProtocolAdapterConfig(resource)).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void convertConfigObject_mqttTopicMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/file-adapter-mqtt-topic-missing.xml");

        assertThatThrownBy(() -> getProtocolAdapterConfig(resource)).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final FileSpecificAdapterConfig adapterConfig = new FileSpecificAdapterConfig(
                new FileToMqttConfig(12,
                        13,
                        List.of(new FileToMqttMapping("my/destination", 1, null, false, true, null, "tag1"))));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory = new FileProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = fileProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        final Map<String, Object> toMqtt = (Map<String, Object>) config.get("fileToMqtt");
        assertThat(toMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(toMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(toMqtt.get("fileToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML
    }

    @Test
    public void unconvertConfigObject_defaults_valid() {
        final FileSpecificAdapterConfig adapterConfig = new FileSpecificAdapterConfig(new FileToMqttConfig(null,
                null,
                List.of(new FileToMqttMapping("my/destination", null, null, null, null, null, "tag1"))));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final FileProtocolAdapterFactory fileProtocolAdapterFactory = new FileProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = fileProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        final Map<String, Object> toMqtt = (Map<String, Object>) config.get("fileToMqtt");
        assertThat(toMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(toMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat(toMqtt.get("fileToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML

    }

    private @NotNull ProtocolAdapterConfig getProtocolAdapterConfig(final @NotNull URL resource)
            throws URISyntaxException {
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final ProtocolAdapterEntity adapterEntity = configEntity.getProtocolAdapterConfig().get(0);

        final ProtocolAdapterConfigConverter converter = createConverter();

        return converter.fromEntity(adapterEntity);
    }

    private @NotNull ProtocolAdapterConfigConverter createConverter() {
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        FileProtocolAdapterFactory protocolAdapterFactory = new FileProtocolAdapterFactory(mockInput);
        ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("file")).thenReturn(Optional.of(protocolAdapterFactory));
        ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, mapper);
        return converter;
    }

    private @NotNull HiveMQConfigEntity loadConfig(final @NotNull File configFile) {
        final ConfigFileReaderWriter readerWriter = new ConfigFileReaderWriter(new ConfigurationFile(configFile), List.of());
        return readerWriter.applyConfig();
    }
}
