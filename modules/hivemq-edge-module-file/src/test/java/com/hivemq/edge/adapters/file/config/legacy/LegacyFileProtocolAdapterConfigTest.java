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

import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.file.FileProtocolAdapterFactory;
import com.hivemq.edge.modules.ModuleLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class LegacyFileProtocolAdapterConfigTest {

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-file-adapter-full-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("file", new FileProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("file");
                                assertThat(entity.getAdapterId()).isEqualTo("my-file-protocol-adapter");
                                assertThat(entity.getTags())
                                        .hasSize(2)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("my-file-protocol-adapter-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("filePath", "contentType")
                                                    .containsExactly("path/to/file1", "BINARY");
                                        })
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("my-file-protocol-adapter-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("filePath", "contentType")
                                                    .containsExactly("path/to/file2", "TEXT_CSV");
                                        });
                                assertThat(entity.getNorthboundMappingEntities())
                                        .hasSize(2)
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).startsWith("my-file-protocol-adapter-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic");
                                            assertThat(mapping.getUserProperties()).containsExactly(
                                                    new MqttUserPropertyEntity("name", "value1"),
                                                    new MqttUserPropertyEntity ("name", "value2")
                                            );
                                        })
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).startsWith("my-file-protocol-adapter-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic/2");
                                            assertThat(mapping.getUserProperties()).containsExactly(
                                                    new MqttUserPropertyEntity("name", "value1"),
                                                    new MqttUserPropertyEntity ("name", "value2")
                                            );
                                        });
                                assertThat(entity.getSouthboundMappingEntities()).isEmpty();
                            });
                });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-file-adapter-minimal-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("file", new FileProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("file");
                                assertThat(entity.getAdapterId()).isEqualTo("my-file-protocol-adapter");
                                assertThat((Map<String, Object>)entity.getConfig().get("fileToMqtt"))
                                        .extracting("maxPollingErrorsBeforeRemoval", "pollingIntervalMillis")
                                        .containsExactly(10, 1000);
                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("my-file-protocol-adapter-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("filePath", "contentType")
                                                    .containsExactly("path/to/file1", "BINARY");
                                        });
                                assertThat(entity.getNorthboundMappingEntities())
                                        .hasSize(1)
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(0);
                                            assertThat(mapping.getTagName()).startsWith("my-file-protocol-adapter-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        });
                                assertThat(entity.getSouthboundMappingEntities()).isEmpty();
                            });
                });
    }
}
