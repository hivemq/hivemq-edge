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
package com.hivemq.edge.adapters.opcua.config.legacy;

import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import com.hivemq.edge.modules.ModuleLoader;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class LegacyOpcUaProtocolAdapterConfigTest {

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-opcua-adapter-full-config.xml");

        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("opcua", new OpcUaProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("opcua");
                                assertThat(entity.getAdapterId()).isEqualTo("simulation-server-2");
                                assertThat(entity.getConfig().get("uri")).isEqualTo("opc.tcp://CSM1.local:53530/OPCUA/SimulationServer");
                                assertThat(entity.getConfig().get("overrideUri")).isEqualTo(true);
                                assertThat(((Map<String,Object>)entity.getConfig().get("security")).get("policy")).isEqualTo("BASIC128RSA15");

                                final Map<String, Object> auth =
                                        (Map<String, Object>) entity.getConfig().get("auth");
                                assertThat(((Map<String,Object>) auth.get("basic")).get("username")).isEqualTo("edge");
                                assertThat(((Map<String,Object>) auth.get("basic")).get("password")).isEqualTo("password");
                                assertThat(((Map<String,Object>) auth.get("x509")).get("enabled")).isEqualTo(true);

                                final Map<String, Object> tls =
                                        (Map<String, Object>) entity.getConfig().get("tls");
                                assertThat(tls.get("enabled")).isEqualTo(true);

                                final Map<String, Object> keystore =
                                        (Map<String, Object>) tls.get("keystore");
                                assertThat(keystore.get("path")).isEqualTo("path/to/keystore");
                                assertThat(keystore.get("password")).isEqualTo("keystore-password");
                                assertThat(keystore.get("privateKeyPassword")).isEqualTo("private-key-password");

                                assertThat(((Map<String,Object>) tls.get("truststore")).get("path")).isEqualTo("path/to/truststore");
                                assertThat(((Map<String,Object>) tls.get("truststore")).get("password")).isEqualTo("truststore-password");


                                final Map<String,Object> opcuaToMqtt = (Map<String,Object>) entity.getConfig().get("opcuaToMqtt");
                                final List<Map<String,Object>> mappings = (List<Map<String,Object>>)(opcuaToMqtt).get("opcuaToMqttMappings");
                                assertThat(mappings).isNull(); // mappings have been moved

                                assertThat(opcuaToMqtt.get("publishingInterval")).isEqualTo(13);
                                assertThat(opcuaToMqtt.get("serverQueueSize")).isEqualTo(13);
                                
                                assertThat(entity.getFromEdgeMappingEntities())
                                        .satisfiesExactly(mapping -> {
                                                    assertThat(mapping.getTagName()).startsWith("simulation-server-2-");
                                                    assertThat(mapping.getTopic()).isEqualTo("test/blubb/#");
                                                    assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                                    assertThat(mapping.getMessageExpiryInterval()).isEqualTo(15L);
                                                },
                                                mapping -> {
                                                    assertThat(mapping.getTagName()).startsWith("simulation-server-2-");
                                                    assertThat(mapping.getTopic()).isEqualTo("test/blubbb/#");
                                                    assertThat(mapping.getMaxQoS()).isEqualTo(2);
                                                    assertThat(mapping.getMessageExpiryInterval()).isEqualTo(16L);
                                                }    
                                        );

                                assertThat(entity.getTags())
                                        .hasSize(2)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("simulation-server-2-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("node")
                                                    .isEqualTo("ns=1;i=1004");
                                        })
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("simulation-server-2-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("node")
                                                    .isEqualTo("ns=2;i=1004");
                                        });

                                assertThat(entity.getToEdgeMappingEntities()).isEmpty();
                            });
                });
    }


    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-opcua-adapter-minimal-config.xml");


        final ConfigurationMigrator migrator = new ConfigurationMigrator(
                new ConfigurationFile(new File(resource.toURI())),
                mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("opcua", new OpcUaProtocolAdapterFactory(mockInput))))
                .isNotEmpty()
                .get()
                .satisfies(cfg -> {
                    assertThat(cfg.getProtocolAdapterConfig())
                            .hasSize(1)
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("opcua");
                                assertThat(entity.getAdapterId()).isEqualTo("simulation-server-2");
                                assertThat(entity.getConfig().get("uri")).isEqualTo("opc.tcp://CSM1.local:53530/OPCUA/SimulationServer");
                                assertThat(entity.getConfig().get("overrideUri")).isEqualTo(false);
                                assertThat(((Map<String,Object>)entity.getConfig().get("security")).get("policy")).isEqualTo("NONE");

                                final Map<String, Object> auth =
                                        (Map<String, Object>) entity.getConfig().get("auth");
                                assertThat(((Map<String,Object>) auth.get("basic"))).isNull();
                                assertThat(((Map<String,Object>) auth.get("x509"))).isNull();
                                final Map<String, Object> tls =
                                        (Map<String, Object>) entity.getConfig().get("tls");
                                assertThat(tls.get("enabled")).isEqualTo(false);
                                final Map<String,Object> opcuaToMqtt = (Map<String,Object>) entity.getConfig().get("opcuaToMqtt");
                                final List<Map<String,Object>> mappings = (List<Map<String,Object>>)(opcuaToMqtt).get("opcuaToMqttMappings");
                                assertThat(mappings).isNull(); // mappings have been moved

                                assertThat(opcuaToMqtt.get("publishingInterval")).isEqualTo(1000);
                                assertThat(opcuaToMqtt.get("serverQueueSize")).isEqualTo(1);
                                assertThat(entity.getFromEdgeMappingEntities())
                                        .satisfiesExactly(mapping -> {
                                                    assertThat(mapping.getTagName()).startsWith("simulation-server-2-");
                                                    assertThat(mapping.getTopic()).isEqualTo("test/blubb/#");
                                                    assertThat(mapping.getMaxQoS()).isEqualTo(0);
                                                    assertThat(mapping.getMessageExpiryInterval()).isEqualTo(4294967295L);
                                                }
                                        );

                                assertThat(entity.getTags())
                                        .hasSize(1)
                                        .satisfiesExactly(tag -> {
                                            assertThat(tag.getName()).startsWith("simulation-server-2-");
                                            assertThat(tag.getDefinition())
                                                    .extracting("node")
                                                    .isEqualTo("ns=1;i=1004");
                                        });

                                assertThat(entity.getToEdgeMappingEntities()).isEmpty();
                            });
                });
    }
}
