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
package com.hivemq.edge.modules.adapters.simulation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.modules.adapters.simulation.SimulationProtocolAdapterFactory;
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

@SuppressWarnings("unchecked")
class SimulationProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());
    private final @NotNull EventService eventService = mock();
    final @NotNull ProtocolAdapterFactoryInput protocolAdapterFactoryInput = new ProtocolAdapterFactoryInput() {
        @Override
        public boolean isWritingEnabled() {
            return true;
        }

        @Override
        public @NotNull EventService eventService() {
            return eventService;
        }
    };

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/simulation-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final @NotNull List<ProtocolAdapterEntity> adapters = configEntity.getProtocolAdapterConfig();

        final SimulationProtocolAdapterFactory simulationProtocolAdapterFactory =
                new SimulationProtocolAdapterFactory(protocolAdapterFactoryInput);
        final ProtocolAdapterEntity protocolAdapterEntity = adapters.get(0);

        final Map<String, Object> configAsMaps = protocolAdapterEntity.getConfig();
        final SimulationSpecificAdapterConfig config =
                (SimulationSpecificAdapterConfig) simulationProtocolAdapterFactory.convertConfigObject(mapper,
                        configAsMaps,
                        false);

        assertThat(protocolAdapterEntity.getAdapterId()).isEqualTo("my-simulation-protocol-adapter");
        assertThat(config.getMinValue()).isEqualTo(0);
        assertThat(config.getMaxValue()).isEqualTo(1000);
        assertThat(config.getMinDelay()).isEqualTo(0);
        assertThat(config.getMaxDelay()).isEqualTo(1000);
        assertThat(config.getSimulationToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(protocolAdapterEntity.getFromEdgeMappingEntities()).satisfiesExactly(subscription -> {
            assertThat(subscription.getTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMaxQoS()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.isIncludeTimestamp()).isFalse();
            assertThat(subscription.isIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("my-name");
                assertThat(userProperty.getValue()).isEqualTo("my-value");
            });
        }, subscription -> {
            assertThat(subscription.getTopic()).isEqualTo("my/topic/2");
            assertThat(subscription.getMaxQoS()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.isIncludeTimestamp()).isFalse();
            assertThat(subscription.isIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("my-name");
                assertThat(userProperty.getValue()).isEqualTo("my-value");
            });
        });


    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/simulation-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final @NotNull List<ProtocolAdapterEntity> adapters = configEntity.getProtocolAdapterConfig();

        final SimulationProtocolAdapterFactory simulationProtocolAdapterFactory =
                new SimulationProtocolAdapterFactory(protocolAdapterFactoryInput);
        final ProtocolAdapterEntity protocolAdapterEntity = adapters.get(0);

        final Map<String, Object> configAsMaps = protocolAdapterEntity.getConfig();
        final SimulationSpecificAdapterConfig config =
                (SimulationSpecificAdapterConfig) simulationProtocolAdapterFactory.convertConfigObject(mapper,
                        configAsMaps,
                        false);


        assertThat(protocolAdapterEntity.getAdapterId()).isEqualTo("my-simulation-protocol-adapter");
        assertThat(config.getSimulationToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(protocolAdapterEntity.getFromEdgeMappingEntities()).satisfiesExactly(subscription -> {
            assertThat(subscription.getTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMaxQoS()).isEqualTo(0);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.isIncludeTimestamp()).isTrue();
            assertThat(subscription.isIncludeTagNames()).isTrue();
            assertThat(subscription.getUserProperties()).isEmpty();
        });
    }

    @Test
    public void convertConfigObject_idMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/simulation-adapter-missing-id.xml");
        final File path = Path.of(resource.toURI()).toFile();
        assertThatThrownBy(() -> loadConfig(path));
    }

    @Test
    public void convertConfigObject_mqttTopicMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/simulation-adapter-missing-mqttTopic.xml");
        final File path = Path.of(resource.toURI()).toFile();
        assertThatThrownBy(() -> loadConfig(path));
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final SimulationToMqttMapping pollingContext = new SimulationToMqttMapping("my/destination",
                1,
                MQTTMessagePerSubscription,
                false,
                true,
                List.of(new MqttUserProperty("my-name", "my-value")));

        final SimulationSpecificAdapterConfig simulationAdapterConfig =
                new SimulationSpecificAdapterConfig(new SimulationToMqttConfig(List.of(pollingContext), 11, 12),
                        12,
                        13,
                        14,
                        15);

        final SimulationProtocolAdapterFactory factory =
                new SimulationProtocolAdapterFactory(protocolAdapterFactoryInput);
        final Map<String, Object> config = factory.unconvertConfigObject(mapper, simulationAdapterConfig);

        assertThat(config.get("minValue")).isEqualTo(12);
        assertThat(config.get("maxValue")).isEqualTo(13);
        assertThat(config.get("minDelay")).isEqualTo(14);
        assertThat(config.get("maxDelay")).isEqualTo(15);


        final Map<String, Object> simulationToMqtt = (Map<String, Object>) config.get("simulationToMqtt");
        assertThat(simulationToMqtt.get("pollingIntervalMillis")).isEqualTo(11);
        assertThat(simulationToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(12);
    }

    @Test
    public void unconvertConfigObject_defaults_valid() {
        final SimulationToMqttMapping pollingContext =
                new SimulationToMqttMapping("my/destination", null, null, null, null, null);

        final SimulationSpecificAdapterConfig simulationAdapterConfig =
                new SimulationSpecificAdapterConfig(new SimulationToMqttConfig(List.of(pollingContext), null, null),
                        null,
                        null,
                        null,
                        null);

        final SimulationProtocolAdapterFactory factory =
                new SimulationProtocolAdapterFactory(protocolAdapterFactoryInput);
        final Map<String, Object> config = factory.unconvertConfigObject(mapper, simulationAdapterConfig);

        assertThat(config.get("minValue")).isEqualTo(0);
        assertThat(config.get("maxValue")).isEqualTo(1000);
        assertThat(config.get("minDelay")).isEqualTo(0);
        assertThat(config.get("maxDelay")).isEqualTo(0);

        final Map<String, Object> simulationToMqtt = (Map<String, Object>) config.get("simulationToMqtt");
        assertThat(simulationToMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(simulationToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
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