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
package com.hivemq.edge.adapters.plc4x.types.siemens.config.legacy;

import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapterFactory;
import com.hivemq.edge.modules.ModuleLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacyS7ProtocolAdapterConfigTest {

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-s7-adapter-full-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("s7", new S7ProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("s7");
                                assertThat(entity.getAdapterId()).isEqualTo("my-s7-id");
                                assertThat(entity.getConfig().get("port")).isEqualTo(102);
                                assertThat(entity.getConfig().get("host")).isEqualTo("my-ip-addr-or-host");
                                assertThat(entity.getConfig().get("controllerType")).isEqualTo("S7_1500");
                                assertThat(entity.getConfig().get("remoteRack")).isEqualTo(1);
                                assertThat(entity.getConfig().get("remoteRack2")).isEqualTo(2);
                                assertThat(entity.getConfig().get("remoteSlot")).isEqualTo(3);
                                assertThat(entity.getConfig().get("remoteSlot2")).isEqualTo(4);
                                assertThat(entity.getConfig().get("remoteTsap")).isEqualTo(5);
                                assertThat(entity.getTags())
                                        .hasSize(2)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).isEqualTo("my-tag-name-1");
                                            assertThat(tag.getDefinition())
                                                    .extracting("tagAddress", "dataType")
                                                    .containsExactly("%I204.0", "BOOL");
                                        })
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).isEqualTo("my-tag-name-2");
                                            assertThat(tag.getDefinition())
                                                    .extracting("tagAddress", "dataType")
                                                    .containsExactly("%I205.0", "BOOL");
                                        });
                                assertThat(entity.getNorthboundMappingEntities())
                                        .hasSize(2)
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).isEqualTo("my-tag-name-1");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic/1");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        })
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(0);
                                            assertThat(mapping.getTagName()).isEqualTo("my-tag-name-2");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic/2");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        });
                                assertThat(entity.getSouthboundMappingEntities()).isEmpty();
                            });
                });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-s7-adapter-minimal-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("s7", new S7ProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("s7");
                                assertThat(entity.getAdapterId()).isEqualTo("my-s7-id");
                                assertThat(entity.getConfig().get("port")).isEqualTo(102);
                                assertThat(entity.getConfig().get("host")).isEqualTo("my-ip-address-or-host");
                                assertThat(entity.getConfig().get("controllerType")).isEqualTo("S7_1500");
                                assertThat(entity.getConfig().get("remoteRack")).isEqualTo(0);
                                assertThat(entity.getConfig().get("remoteRack2")).isEqualTo(0);
                                assertThat(entity.getConfig().get("remoteSlot")).isEqualTo(0);
                                assertThat(entity.getConfig().get("remoteSlot2")).isEqualTo(0);
                                assertThat(entity.getConfig().get("remoteTsap")).isEqualTo(0);
                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .satisfiesExactly(tag -> {
                                            assertThat(tag.getName()).isEqualTo("my-tag-name-1");
                                            assertThat(tag.getDefinition())
                                                    .extracting("tagAddress", "dataType")
                                                    .containsExactly("%I204.0", "SINT");
                                        });
                                assertThat(entity.getNorthboundMappingEntities())
                                        .hasSize(1)
                                        .satisfiesExactly(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).isEqualTo("my-tag-name-1");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic/1");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        });
                                assertThat(entity.getSouthboundMappingEntities()).isEmpty();
                            });
                });
    }

}
