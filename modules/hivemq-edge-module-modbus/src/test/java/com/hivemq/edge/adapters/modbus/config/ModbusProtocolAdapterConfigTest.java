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
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.modbus.ModbusProtocolAdapterFactory;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ModbusProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-full-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final ModbusSpecificAdapterConfig config = (ModbusSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-modbus-protocol-adapter");
        assertThat(config.getModbusToMQTTConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getModbusToMQTTConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeoutMillis()).isEqualTo(1337);
        assertThat(config.getModbusToMQTTConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(protocolAdapterConfig.getNorthboundMappings()).satisfiesExactly(modbusToMqttMapping -> {
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
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final ModbusSpecificAdapterConfig config = (ModbusSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-modbus-protocol-adapter");
        assertThat(config.getModbusToMQTTConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getModbusToMQTTConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeoutMillis()).isEqualTo(5000);
        assertThat(config.getModbusToMQTTConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(protocolAdapterConfig.getNorthboundMappings()).satisfiesExactly(modbusToMqttMapping -> {
            assertThat(modbusToMqttMapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(modbusToMqttMapping.getMqttQos()).isEqualTo(1);
            assertThat(modbusToMqttMapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(modbusToMqttMapping.getIncludeTimestamp()).isTrue();
            assertThat(modbusToMqttMapping.getIncludeTagNames()).isFalse();
            assertThat(modbusToMqttMapping.getUserProperties()).isEmpty();
            assertThat(modbusToMqttMapping.getTagName()).isEqualTo("tag1");
        });
    }

    @Test
    public void convertConfigObject_defaults_missingTag() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-minimal-config-missing-tag.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        assertThat(protocolAdapterConfig.missingTags())
                .isPresent()
                .hasValueSatisfying(set -> assertThat(set).contains("tag1"));
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final ModbusSpecificAdapterConfig adapterConfig = new ModbusSpecificAdapterConfig(
                14,
                "my.host.com",
                15,
                new ModbusToMqttConfig(
                        12,
                        13,
                        true
                        ));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory fileProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = fileProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("timeoutMillis")).isEqualTo(15);
        final Map<String, Object> modbusToMqtt = (Map<String, Object>) config.get("modbusToMqtt");
        assertThat(modbusToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(modbusToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(modbusToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat(modbusToMqtt.get("modbusToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML
    }


    @Test
    public void unconvertConfigObject_defaults() {
        final ModbusSpecificAdapterConfig adapterConfig = new ModbusSpecificAdapterConfig(
                13,
                "my.host.com",
                null,
                new ModbusToMqttConfig(
                        null,
                        null,
                        null));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory fileProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = fileProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        assertThat(config.get("port")).isEqualTo(13);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("timeoutMillis")).isEqualTo(5000);
        final Map<String, Object> modbusToMqtt = (Map<String, Object>) config.get("modbusToMqtt");
        assertThat(modbusToMqtt.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(modbusToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat(modbusToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat(modbusToMqtt.get("modbusToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML


    }

    private @NotNull ProtocolAdapterConfig getProtocolAdapterConfig(final @NotNull URL resource) throws
            URISyntaxException {
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final ProtocolAdapterEntity adapterEntity = configEntity.getProtocolAdapterConfig().get(0);

        final ProtocolAdapterConfigConverter converter = createConverter();

        return converter.fromEntity(adapterEntity);
    }

    private @NotNull ProtocolAdapterConfigConverter createConverter() {
        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(true);

        ModbusProtocolAdapterFactory protocolAdapterFactory = new ModbusProtocolAdapterFactory(mockInput);
        ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("modbus")).thenReturn(Optional.of(protocolAdapterFactory));
        ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, mapper);
        return converter;
    }

    private @NotNull HiveMQConfigEntity loadConfig(final @NotNull File configFile) {
        final ConfigFileReaderWriter readerWriter = new ConfigFileReaderWriter(new ConfigurationFile(configFile), List.of());
        return readerWriter.applyConfig();
    }
}
