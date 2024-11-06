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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.modbus.ModbusProtocolAdapterFactory;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.protocols.ProtocolAdapterConfigPersistence;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"unchecked", "rawtypes"})
public class LegacyModbusAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-modbus-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);

        final ProtocolAdapterConfigPersistence protocolAdapterConfigPersistence =
                ProtocolAdapterConfigPersistence.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                        false,
                        mapper,
                        modbusProtocolAdapterFactory);
        assertThat(protocolAdapterConfigPersistence.missingTags())
                .isEmpty();

        final ModbusAdapterConfig config = (ModbusAdapterConfig) protocolAdapterConfigPersistence.getAdapterConfig();

        assertThat(config.getId()).isEqualTo("my-modbus-protocol-adapter-full");
        assertThat(config.getModbusToMQTTConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getModbusToMQTTConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeoutMillis()).isEqualTo(1337);
        assertThat(config.getModbusToMQTTConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(config.getModbusToMQTTConfig().getMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMqttQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });

        }, subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(subscription.getMqttQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
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
        final URL resource = getClass().getResource("/legacy-modbus-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory =
                new ModbusProtocolAdapterFactory(mockInput);

        final ProtocolAdapterConfigPersistence protocolAdapterConfigPersistence =
                ProtocolAdapterConfigPersistence.fromAdapterConfigMap((Map<String, Object>) adapters.get("modbus"),
                        false,
                        mapper,
                        modbusProtocolAdapterFactory);
        assertThat(protocolAdapterConfigPersistence.missingTags())
                .isEmpty();

        final ModbusAdapterConfig config = (ModbusAdapterConfig) protocolAdapterConfigPersistence.getAdapterConfig();

        assertThat(config.getId()).isEqualTo("my-modbus-protocol-adapter-min");
        assertThat(config.getModbusToMQTTConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getModbusToMQTTConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeoutMillis()).isEqualTo(5000);
        assertThat(config.getModbusToMQTTConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(config.getModbusToMQTTConfig().getMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getMqttQos()).isEqualTo(0);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.getIncludeTimestamp()).isTrue();
            assertThat(subscription.getIncludeTagNames()).isFalse();
            assertThat(subscription.getUserProperties()).isEmpty();
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
