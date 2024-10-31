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
package com.hivemq.edge.adapters.plc4x.types.ads.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.config.Plc4xToMqttMapping;
import com.hivemq.edge.adapters.plc4x.types.ads.ADSProtocolAdapterFactory;
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

class ADSAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());
    private final @NotNull ProtocolAdapterTagService protocolAdapterTagService = mock();
    private final @NotNull EventService eventService = mock();
    final @NotNull ProtocolAdapterFactoryInput protocolAdapterFactoryInput = new ProtocolAdapterFactoryInput() {
        @Override
        public boolean isWritingEnabled() {
            return true;
        }

        @Override
        public @NotNull ProtocolAdapterTagService protocolAdapterTagService() {
            return protocolAdapterTagService;
        }

        @Override
        public @NotNull EventService eventService() {
            return eventService;
        }
    };

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/ads-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ADSProtocolAdapterFactory adsProtocolAdapterFactory =
                new ADSProtocolAdapterFactory(protocolAdapterFactoryInput);
        final ADSAdapterConfig config =
                (ADSAdapterConfig) adsProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("ads"), false);

        assertThat(config.getId()).isEqualTo("my-ads-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.ads-server.com");
        assertThat(config.getTargetAmsPort()).isEqualTo(1234);
        assertThat(config.getSourceAmsPort()).isEqualTo(12345);
        assertThat(config.getTargetAmsNetId()).isEqualTo("1.2.3.4.5.6");
        assertThat(config.getSourceAmsNetId()).isEqualTo("1.2.3.4.5.7");
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagName()).isEqualTo("tag-name");

            assertThat(mapping.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value1");
            }, userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("name");
                assertThat(userProperty.getValue()).isEqualTo("value2");
            });
        }, mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagName()).isEqualTo("tag-name");

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
        final URL resource = getClass().getResource("/ads-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ADSProtocolAdapterFactory adsProtocolAdapterFactory =
                new ADSProtocolAdapterFactory(protocolAdapterFactoryInput);
        final ADSAdapterConfig config =
                (ADSAdapterConfig) adsProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("ads"), false);

        assertThat(config.getId()).isEqualTo("my-ads-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.ads-server.com");
        assertThat(config.getTargetAmsPort()).isEqualTo(123);
        assertThat(config.getSourceAmsPort()).isEqualTo(124);
        assertThat(config.getTargetAmsNetId()).isEqualTo("1.2.3.4.5.6");
        assertThat(config.getSourceAmsNetId()).isEqualTo("1.2.3.4.5.7");
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagName()).isEqualTo("tag-name");
            assertThat(mapping.getDataType()).isEqualTo(Plc4xDataType.DATA_TYPE.BOOL);
        });
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final Plc4xToMqttMapping pollingContext = new Plc4xToMqttMapping("my/destination",
                1,
                MQTTMessagePerSubscription,
                false,
                true,
                "tag-name",
                Plc4xDataType.DATA_TYPE.BOOL, List.of(new MqttUserProperty("my-name", "my-value")));

        final ADSAdapterConfig adsAdapterConfig = new ADSAdapterConfig("my-ads-adapter",
                14,
                "my.host.com",
                15,
                16,
                "1.2.3.4.5.6",
                "1.2.3.4.5.7",
                new ADSToMqttConfig(12, 13, true, List.of(pollingContext)));

        final ADSProtocolAdapterFactory adsProtocolAdapterFactory =
                new ADSProtocolAdapterFactory(protocolAdapterFactoryInput);
        final Map<String, Object> config = adsProtocolAdapterFactory.unconvertConfigObject(mapper, adsAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-ads-adapter");
        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("targetAmsPort")).isEqualTo(15);
        assertThat(config.get("sourceAmsPort")).isEqualTo(16);
        assertThat(config.get("targetAmsNetId")).isEqualTo("1.2.3.4.5.6");
        assertThat(config.get("sourceAmsNetId")).isEqualTo("1.2.3.4.5.7");
        final Map<String, Object> adsToMqtt = (Map<String, Object>) config.get("adsToMqtt");
        assertThat(adsToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(adsToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(adsToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) adsToMqtt.get("adsToMqttMappings")).satisfiesExactly((mapping) -> {

            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(1);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(false);
            assertThat(mapping.get("includeTagNames")).isEqualTo(true);
            assertThat(mapping.get("tagName")).isEqualTo("tag-name");
            assertThat(mapping.get("jsonPayloadCreator")).isNull();
            assertThat((List<Map<String, Object>>) mapping.get("mqttUserProperties")).satisfiesExactly((userProperty) -> {
                assertThat(userProperty.get("name")).isEqualTo("my-name");
                assertThat(userProperty.get("value")).isEqualTo("my-value");
            });
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
