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
package com.hivemq.edge.adapters.file.config.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.file.FileProtocolAdapterFactory;
import com.hivemq.edge.adapters.file.config.ContentType;
import com.hivemq.edge.adapters.file.config.FileAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
class LegacyFileAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-file-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final FileProtocolAdapterFactory fileProtocolAdapterFactory = new FileProtocolAdapterFactory(false);
        final FileAdapterConfig config =
                (FileAdapterConfig) fileProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("file"));

        assertThat(config.getId()).isEqualTo("my-file-protocol-adapter");
        assertThat(config.getFileToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getFileToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isFalse();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getFilePath()).isEqualTo("path/to/file1");
            assertThat(mapping.getContentType()).isEqualTo(ContentType.BINARY);

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
            assertThat(mapping.getFilePath()).isEqualTo("path/to/file2");
            assertThat(mapping.getContentType()).isEqualTo(ContentType.TEXT_CSV);

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
        final URL resource = getClass().getResource("/legacy-file-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final FileProtocolAdapterFactory fileProtocolAdapterFactory = new FileProtocolAdapterFactory(false);
        final FileAdapterConfig config =
                (FileAdapterConfig) fileProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("file"));

        assertThat(config.getId()).isEqualTo("my-file-protocol-adapter");
        assertThat(config.getFileToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getFileToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getFileToMqttConfig().getMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMqttQos()).isEqualTo(0);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isTrue();
            assertThat(subscription.getIncludeTagNames()).isFalse();
            assertThat(subscription.getFilePath()).isEqualTo("path/to/file1");
            assertThat(subscription.getContentType()).isEqualTo(ContentType.BINARY);
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
