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
package com.hivemq.edge.adapters.plc4x.types.ads.config.legacy;

import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.plc4x.types.ads.ADSProtocolAdapterFactory;
import com.hivemq.edge.modules.ModuleLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacyADSProtocolAdapterConfigTest {

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-ads-adapter-full-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("ads", new ADSProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("ads");
                                assertThat(entity.getAdapterId()).isEqualTo("asd");
                                assertThat(entity.getConfig().get("port")).isEqualTo(48898);
                                assertThat(entity.getConfig().get("host")).isEqualTo("172.16.10.54");
                                assertThat(entity.getConfig().get("targetAmsPort")).isEqualTo(850);
                                assertThat(entity.getConfig().get("sourceAmsPort")).isEqualTo(49999);
                                assertThat(entity.getConfig().get("targetAmsNetId")).isEqualTo("2.3.4.5.1.1");
                                assertThat(entity.getConfig().get("sourceAmsNetId")).isEqualTo("5.4.3.2.1.1");
                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).isEqualTo("my-tag-name");
                                            assertThat(tag.getDefinition())
                                                    .extracting("tagAddress", "dataType")
                                                    .containsExactly("MYPROGRAM.MyStringVar", "STRING");
                                        });
                                assertThat(entity.getNorthboundMappingEntities())
                                        .hasSize(1)
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).isEqualTo("my-tag-name");
                                            assertThat(mapping.getTopic()).isEqualTo("my/mqtt/topic");
                                            assertThat(mapping.getUserProperties())
                                                    .hasSize(2)
                                                    .containsExactly(
                                                            new MqttUserPropertyEntity("name", "value1"),
                                                            new MqttUserPropertyEntity("name", "value2")
                                                    );
                                        });
                                assertThat(entity.getSouthboundMappingEntities()).isEmpty();
                            });
                });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-ads-adapter-minimal-config.xml");



        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("ads", new ADSProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("ads");
                                assertThat(entity.getAdapterId()).isEqualTo("my-ads-id");
                                assertThat(entity.getConfig().get("port")).isEqualTo(48898);
                                assertThat(entity.getConfig().get("host")).isEqualTo("172.16.10.53");
                                assertThat(entity.getConfig().get("targetAmsPort")).isEqualTo(850);
                                assertThat(entity.getConfig().get("sourceAmsPort")).isEqualTo(49999);
                                assertThat(entity.getConfig().get("targetAmsNetId")).isEqualTo("2.3.4.5.1.1");
                                assertThat(entity.getConfig().get("sourceAmsNetId")).isEqualTo("5.4.3.2.1.1");
                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).isEqualTo("my-tag-name");
                                            assertThat(tag.getDefinition())
                                                    .extracting("tagAddress", "dataType")
                                                    .containsExactly("MYPROGRAM.MyStringVar", "STRING");
                                        });
                                assertThat(entity.getNorthboundMappingEntities())
                                        .hasSize(1)
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).isEqualTo("my-tag-name");
                                            assertThat(mapping.getTopic()).isEqualTo("my/mqtt/topic");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        });
                                assertThat(entity.getSouthboundMappingEntities()).isEmpty();
                            });
                });
    }

}
