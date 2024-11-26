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
package com.hivemq.edge.adapters.modbus.config.legacy;

import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.modbus.ModbusProtocolAdapterFactory;
import com.hivemq.edge.modules.ModuleLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
public class LegacyModbusProtocolAdapterConfigTest {

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-modbus-adapter-full-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("modbus", new ModbusProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("modbus");
                                assertThat(entity.getAdapterId()).isEqualTo("my-modbus-protocol-adapter-full");
                                assertThat(entity.getConfig().get("host")).isEqualTo("my.modbus-server.com");
                                assertThat(entity.getConfig().get("port")).isEqualTo(1234);
                                assertThat(entity.getConfig().get("timeoutMillis")).isEqualTo(1337);

                                assertThat(entity.getTags())
                                        .hasSize(2)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("my-modbus-protocol-adapter-full-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("startIdx", "readType", "unitId", "flipRegisters", "dataType")
                                                    .containsExactly(11, "HOLDING_REGISTERS", 0, false, "INT_32");
                                        })
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("my-modbus-protocol-adapter-full-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("startIdx", "readType", "unitId", "flipRegisters", "dataType")
                                                    .containsExactly(11, "HOLDING_REGISTERS", 0, false, "INT_32");
                                        });
                                assertThat(entity.getFromEdgeMappingEntities())
                                        .hasSize(2)
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).startsWith("my-modbus-protocol-adapter-full-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic");
                                            assertThat(mapping.getUserProperties()).containsExactly(
                                                    new MqttUserPropertyEntity("name", "value1"),
                                                    new MqttUserPropertyEntity ("name", "value2")
                                            );
                                        })
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).startsWith("my-modbus-protocol-adapter-full-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic/2");
                                            assertThat(mapping.getUserProperties()).containsExactly(
                                                    new MqttUserPropertyEntity("name", "value1"),
                                                    new MqttUserPropertyEntity ("name", "value2")
                                            );
                                        });

                                assertThat(entity.getFieldMappings()).isEmpty();
                                assertThat(entity.getToEdgeMappingEntities()).isEmpty();
                            });
                });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-modbus-adapter-minimal-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("modbus", new ModbusProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("modbus");
                                assertThat(entity.getAdapterId()).isEqualTo("my-modbus-protocol-adapter-min");
                                assertThat(entity.getConfig().get("host")).isEqualTo("my.modbus-server.com");
                                assertThat(entity.getConfig().get("port")).isEqualTo(1234);
                                assertThat(entity.getConfig().get("timeoutMillis")).isEqualTo(5000);

                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .satisfiesExactly(tag -> {
                                            assertThat(tag.getName()).startsWith("my-modbus-protocol-adapter-min-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("startIdx", "readType", "unitId", "flipRegisters", "dataType")
                                                    .containsExactly(11, "HOLDING_REGISTERS", 0, false, "INT_32");
                                        });
                                assertThat(entity.getFromEdgeMappingEntities())
                                        .hasSize(1)
                                        .satisfiesExactly(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(0);
                                            assertThat(mapping.getTagName()).startsWith("my-modbus-protocol-adapter-min-");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic");
                                            assertThat(mapping.getUserProperties()).isEmpty();
                                        });

                                assertThat(entity.getFieldMappings()).isEmpty();
                                assertThat(entity.getToEdgeMappingEntities()).isEmpty();
                            });
                });

    }
}
