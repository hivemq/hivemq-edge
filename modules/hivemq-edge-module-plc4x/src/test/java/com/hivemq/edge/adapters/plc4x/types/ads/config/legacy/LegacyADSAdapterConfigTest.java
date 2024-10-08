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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.types.ads.ADSProtocolAdapterFactory;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSAdapterConfig;
import com.hivemq.edge.adapters.plc4x.types.ads.config.ADSToMqttConfig;
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
import static org.mockito.Mockito.mock;

class LegacyADSAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-ads-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ADSProtocolAdapterFactory adsProtocolAdapterFactory = new ADSProtocolAdapterFactory(false);
        final ADSAdapterConfig config =
                (ADSAdapterConfig) adsProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("ads"));

        assertThat(config.getId()).isEqualTo("asd");
        assertThat(config.getHost()).isEqualTo("172.16.10.54");
        assertThat(config.getPort()).isEqualTo(48898);
        assertThat(config.getTargetAmsPort()).isEqualTo(850);
        assertThat(config.getSourceAmsPort()).isEqualTo(49999);
        assertThat(config.getTargetAmsNetId()).isEqualTo("2.3.4.5.1.1");
        assertThat(config.getSourceAmsNetId()).isEqualTo("5.4.3.2.1.1");
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/mqtt/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagAddress()).isEqualTo("MYPROGRAM.MyStringVar");
            assertThat(mapping.getTagName()).isEqualTo("my-tag-name");

            assertThat(mapping.getUserProperties()).satisfiesExactly(userProperty -> {
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
        final URL resource = getClass().getResource("/legacy-ads-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ADSProtocolAdapterFactory adsProtocolAdapterFactory = new ADSProtocolAdapterFactory(false);
        final ADSAdapterConfig config =
                (ADSAdapterConfig) adsProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("ads"));

        assertThat(config.getId()).isEqualTo("my-ads-id");
        assertThat(config.getPort()).isEqualTo(48898);
        assertThat(config.getHost()).isEqualTo("172.16.10.53");
        assertThat(config.getTargetAmsPort()).isEqualTo(850);
        assertThat(config.getSourceAmsPort()).isEqualTo(49999);
        assertThat(config.getTargetAmsNetId()).isEqualTo("2.3.4.5.1.1");
        assertThat(config.getSourceAmsNetId()).isEqualTo("5.4.3.2.1.1");
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/mqtt/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagAddress()).isEqualTo("MYPROGRAM.MyStringVar");
            assertThat(mapping.getTagName()).isEqualTo("my-tag-name");
            assertThat(mapping.getDataType()).isEqualTo(Plc4xDataType.DATA_TYPE.STRING);
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
