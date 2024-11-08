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
package com.hivemq.edge.adapters.modbus.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.modbus.ModbusProtocolAdapterFactory;
import com.hivemq.protocols.AdapterConfigAndTags;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ModbusAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);

        final AdapterConfigAndTags adapterConfigAndTags =
                AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                        false,
                        mapper,
                        modbusProtocolAdapterFactory);
        assertThat(adapterConfigAndTags.missingTags())
                .isEmpty();

        final ModbusAdapterConfig config = (ModbusAdapterConfig) adapterConfigAndTags.getAdapterConfig();

        assertThat(config.getId()).isEqualTo("my-modbus-protocol-adapter");
        assertThat(config.getModbusToMQTTConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getModbusToMQTTConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeoutMillis()).isEqualTo(1337);
        assertThat(config.getModbusToMQTTConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(config.getModbusToMQTTConfig().getMappings()).satisfiesExactly(modbusToMqttMapping -> {
            assertThat(modbusToMqttMapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(modbusToMqttMapping.getMqttQos()).isEqualTo(1);
            assertThat(modbusToMqttMapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(modbusToMqttMapping.getIncludeTimestamp()).isFalse();
            assertThat(modbusToMqttMapping.getIncludeTagNames()).isTrue();

            assertThat(modbusToMqttMapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });

            assertThat(modbusToMqttMapping.getTagName()).isEqualTo("tag1");

        }, modbusToMqttMapping -> {
            assertThat(modbusToMqttMapping.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(modbusToMqttMapping.getMqttQos()).isEqualTo(1);
            assertThat(modbusToMqttMapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(modbusToMqttMapping.getIncludeTimestamp()).isFalse();
            assertThat(modbusToMqttMapping.getIncludeTagNames()).isTrue();

            assertThat(modbusToMqttMapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });

            assertThat(modbusToMqttMapping.getTagName()).isEqualTo("tag2");

        });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);

        final AdapterConfigAndTags adapterConfigAndTags =
                AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                        false,
                        mapper,
                        modbusProtocolAdapterFactory);
        assertThat(adapterConfigAndTags.missingTags())
                .isEmpty();

        final ModbusAdapterConfig config = (ModbusAdapterConfig) adapterConfigAndTags.getAdapterConfig();

        assertThat(config.getId()).isEqualTo("my-modbus-protocol-adapter");
        assertThat(config.getModbusToMQTTConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getModbusToMQTTConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeoutMillis()).isEqualTo(5000);
        assertThat(config.getModbusToMQTTConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(config.getModbusToMQTTConfig().getMappings()).satisfiesExactly(modbusToMqttMapping -> {
            assertThat(modbusToMqttMapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(modbusToMqttMapping.getMqttQos()).isEqualTo(0);
            assertThat(modbusToMqttMapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(modbusToMqttMapping.getIncludeTimestamp()).isTrue();
            assertThat(modbusToMqttMapping.getIncludeTagNames()).isFalse();
            assertThat(modbusToMqttMapping.getUserProperties()).isEmpty();
            assertThat(modbusToMqttMapping.getTagName()).isEqualTo("tag1");
        });
    }

    @Test
    public void convertConfigObject_defaults_missingTag() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-minimal-config-missing-tag.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);

        final AdapterConfigAndTags adapterConfigAndTags =
                AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                        false,
                        mapper,
                        modbusProtocolAdapterFactory);

        assertThat(adapterConfigAndTags.missingTags())
                .isPresent()
                .hasValueSatisfying(set -> assertThat(set).contains("tag1"));
    }

    @Test
    public void convertConfigObject_idMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-id-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);

        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                false,
                mapper,
                modbusProtocolAdapterFactory))
                .hasMessageContaining("Missing required creator property 'id'");
    }

    @Test
    public void convertConfigObject_hostMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-host-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                false,
                mapper,
                modbusProtocolAdapterFactory))
                .hasMessageContaining("Missing required creator property 'host'");
    }

    @Test
    public void convertConfigObject_portMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-port-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                false,
                mapper,
                modbusProtocolAdapterFactory))
                .hasMessageContaining("Missing required creator property 'port'");
    }

    @Test
    public void convertConfigObject_destinationMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-destination-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                false,
                mapper,
                modbusProtocolAdapterFactory))
                .hasMessageContaining("Missing required creator property 'mqttTopic'");
    }

    @Test
    public void convertConfigObject_addressRangeMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-address-range-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                false,
                mapper,
                modbusProtocolAdapterFactory));
    }

    @Test
    public void convertConfigObject_tagNameMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-startIdx-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        assertThatThrownBy(() -> AdapterConfigAndTags.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                false,
                mapper,
                modbusProtocolAdapterFactory))
                .hasMessageContaining("Missing required creator property 'tagName'");
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final ModbusToMqttMapping pollingContext = new ModbusToMqttMapping("my/destination",
                1, "tag1",
                MQTTMessagePerSubscription,
                false,
                true, List.of(new MqttUserProperty("my-name", "my-value")));

        final ModbusAdapterConfig modbusAdapterConfig = new ModbusAdapterConfig("my-modbus-adapter",
                14,
                "my.host.com",
                15,
                new ModbusToMqttConfig(12, 13, true, List.of(pollingContext)));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        final Map<String, Object> config =
                modbusProtocolAdapterFactory.unconvertConfigObject(mapper, modbusAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-modbus-adapter");
        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("timeoutMillis")).isEqualTo(15);
        final Map<String, Object> modbusToMqtt = (Map<String, Object>) config.get("modbusToMqtt");
        assertThat(modbusToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(modbusToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(modbusToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) modbusToMqtt.get("modbusToMqttMappings")).satisfiesExactly((mapping) -> {

            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(1);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(false);
            assertThat(mapping.get("includeTagNames")).isEqualTo(true);
            assertThat((List<Map<String, Object>>) mapping.get("mqttUserProperties")).satisfiesExactly((userProperty) -> {
                assertThat(userProperty.get("name")).isEqualTo("my-name");
                assertThat(userProperty.get("value")).isEqualTo("my-value");
            });
            assertThat(mapping.get("tagName")).isEqualTo("tag1");
        });
    }

    @Test
    public void unconvertConfigObject_defaults() {
        final ModbusToMqttMapping pollingContext =
                new ModbusToMqttMapping("my/destination", null, "tag1", null, null, null, null);

        final ModbusToMqttMapping pollingContext2 =
                new ModbusToMqttMapping("my/destination/2", null, "tag1", null, null, null, null);


        final ModbusAdapterConfig modbusAdapterConfig = new ModbusAdapterConfig("my-modbus-adapter",
                13,
                "my.host.com",
                null,
                new ModbusToMqttConfig(null, null, null, List.of(pollingContext, pollingContext2)));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        final Map<String, Object> config =
                modbusProtocolAdapterFactory.unconvertConfigObject(mapper, modbusAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-modbus-adapter");
        assertThat(config.get("port")).isEqualTo(13);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("timeoutMillis")).isEqualTo(5000);
        final Map<String, Object> modbusToMqtt = (Map<String, Object>) config.get("modbusToMqtt");
        assertThat(modbusToMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(modbusToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat(modbusToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat(((List<Map<String, Object>>) modbusToMqtt.get("modbusToMqttMappings"))).satisfiesExactly(mapping -> {
            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(0);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(true);
            assertThat(mapping.get("includeTagNames")).isEqualTo(false);
            assertThat((List<Map<String, Object>>) mapping.get("mqttUserProperties")).isEmpty();
        }, mapping -> {
            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination/2");
            assertThat(mapping.get("mqttQos")).isEqualTo(0);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(true);
            assertThat(mapping.get("includeTagNames")).isEqualTo(false);
            assertThat((List<Map<String, Object>>) mapping.get("mqttUserProperties")).isEmpty();
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
