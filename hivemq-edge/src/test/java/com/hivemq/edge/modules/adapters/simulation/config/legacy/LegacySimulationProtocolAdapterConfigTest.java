/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.edge.modules.adapters.simulation.config.legacy;

import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.migration.ConfigurationMigrator;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.modules.ModuleLoader;
import com.hivemq.edge.modules.adapters.simulation.SimulationProtocolAdapterFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LegacySimulationProtocolAdapterConfigTest {

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/legacy-simulation-adapter-full-config.xml");
        final ConfigurationMigrator migrator =
                new ConfigurationMigrator(new ConfigurationFile(new File(resource.toURI())), mock(ModuleLoader.class));
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        assertThat(migrator.migrateIfNeeded(Map.of("simulation",
                new SimulationProtocolAdapterFactory(mockInput)))).isNotEmpty().get().satisfies(cfg -> {
            assertThat(cfg.getProtocolAdapterConfig()).hasSize(1);
                             /*
                            .allSatisfy(entity -> {
                                assertThat(entity.getProtocolId()).isEqualTo("simulation");
                                assertThat(entity.getAdapterId()).isEqualTo("my-eip-protocol-adapter");
                                assertThat(entity.getTags())
                                        .hasSize(2)
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("tag-name");
                                            assertThat(tag.getDefinition())
                                                    .extracting("address", "dataType")
                                                    .containsExactly("tag-address", "BOOL");
                                        })
                                        .anySatisfy(tag -> {
                                            assertThat(tag.getName()).startsWith("tag-name2");
                                            assertThat(tag.getDefinition())
                                                    .extracting("address", "dataType")
                                                    .containsExactly("tag-address2", "BOOL");
                                        });
                                assertThat(entity.getFromEdgeMappingEntities())
                                        .hasSize(2)
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).startsWith("tag-name");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic");
                                            assertThat(mapping.getUserProperties()).containsExactly(
                                                    new MqttUserPropertyEntity("name", "value1"),
                                                    new MqttUserPropertyEntity ("name", "value2")
                                            );
                                        })
                                        .anySatisfy(mapping -> {
                                            assertThat(mapping.getMaxQoS()).isEqualTo(1);
                                            assertThat(mapping.getTagName()).startsWith("tag-name2");
                                            assertThat(mapping.getTopic()).isEqualTo("my/topic/2");
                                            assertThat(mapping.getUserProperties()).containsExactly(
                                                    new MqttUserPropertyEntity("name", "value1"),
                                                    new MqttUserPropertyEntity ("name", "value2")
                                            );
                                        });
                                assertThat(entity.getFieldMappings()).isEmpty();
                                assertThat(entity.getToEdgeMappingEntities()).isEmpty();
                            });

                     */
        });
        //TODO
        /*
        final SimulationSpecificAdapterConfig config =
                (SimulationSpecificAdapterConfig) simulationProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("simulation"), false);

        assertThat(config.getId()).isEqualTo("my-simulation-protocol-adapter");
        assertThat(config.getMinValue()).isEqualTo(0);
        assertThat(config.getMaxValue()).isEqualTo(1000);
        assertThat(config.getMinDelay()).isEqualTo(0);
        assertThat(config.getMaxDelay()).isEqualTo(1000);
        assertThat(config.getSimulationToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getSimulationToMqttConfig().getSimulationToMqttMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMqttQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("my-name");
                assertThat(userProperty.getValue()).isEqualTo("my-value");
            });
        }, subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(subscription.getMqttQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("my-name");
                assertThat(userProperty.getValue()).isEqualTo("my-value");
            });
        });

         */
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/legacy-simulation-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final @NotNull List<ProtocolAdapterEntity> adapters = configEntity.getProtocolAdapterConfig();
//TODO
        /*
        final SimulationProtocolAdapterFactory simulationProtocolAdapterFactory =
                new SimulationProtocolAdapterFactory(protocolAdapterFactoryInput);
        final SimulationSpecificAdapterConfig config =
                (SimulationSpecificAdapterConfig) simulationProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("simulation"), false);

        assertThat(config.getId()).isEqualTo("my-simulation-protocol-adapter");
        assertThat(config.getSimulationToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getSimulationToMqttConfig().getSimulationToMqttMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMqttQos()).isEqualTo(0);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isTrue();
            assertThat(subscription.getIncludeTagNames()).isFalse();

            assertThat(subscription.getUserProperties()).isEmpty();
        });

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
