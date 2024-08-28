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
package com.hivemq.edge.adapters.plc4x.types.siemens;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.plc4x.model.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.model.Plc4xPollingContext;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.edge.adapters.plc4x.types.siemens.S7AdapterConfig.ControllerType.S7_1500;
import static com.hivemq.edge.adapters.plc4x.types.siemens.S7AdapterConfig.ControllerType.S7_400;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class S7AdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/s7-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory = new S7ProtocolAdapterFactory();
        final S7AdapterConfig config =
                s7ProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("s7"));

        assertThat(config.getId()).isEqualTo("my-s7-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.s7-server.com");
        assertThat(config.getControllerType()).isEqualTo(S7_400);
        assertThat(config.getRemoteRack()).isEqualTo(1);
        assertThat(config.getRemoteRack2()).isEqualTo(2);
        assertThat(config.getRemoteSlot()).isEqualTo(3);
        assertThat(config.getRemoteSlot2()).isEqualTo(4);
        assertThat(config.getRemoteTsap()).isEqualTo(5);
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagAddress()).isEqualTo("tag-address");
            assertThat(mapping.getTagName()).isEqualTo("tag-name");

        }, mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagAddress()).isEqualTo("tag-address");
            assertThat(mapping.getTagName()).isEqualTo("tag-name");
        });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/s7-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory = new S7ProtocolAdapterFactory();
        final S7AdapterConfig config =
                s7ProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("s7"));

        assertThat(config.getId()).isEqualTo("my-s7-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.s7-server.com");
        assertThat(config.getControllerType()).isEqualTo(S7_400);
        assertThat(config.getRemoteRack()).isEqualTo(0);
        assertThat(config.getRemoteRack2()).isEqualTo(0);
        assertThat(config.getRemoteSlot()).isEqualTo(0);
        assertThat(config.getRemoteSlot2()).isEqualTo(0);
        assertThat(config.getRemoteTsap()).isEqualTo(0);
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagAddress()).isEqualTo("tag-address");
            assertThat(mapping.getTagName()).isEqualTo("tag-name");
            assertThat(mapping.getDataType()).isEqualTo(Plc4xDataType.DATA_TYPE.BOOL);
        });
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final Plc4xPollingContext pollingContext = new Plc4xPollingContext("my/destination",
                1,
                MQTTMessagePerSubscription,
                false,
                true,
                "tag-name",
                "tag-address",
                Plc4xDataType.DATA_TYPE.BOOL,
                List.of(new UserProperty("my-name", "my-value"))
        );

        final S7AdapterConfig s7AdapterConfig = new S7AdapterConfig("my-s7-adapter",
                14,
                "my.host.com",
                S7_1500,
                1,
                2,
                3,
                4,
                5,
                new S7ToMqttConfig(12, 13, true, List.of(pollingContext)));

        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory = new S7ProtocolAdapterFactory();
        final Map<String, Object> config =
                s7ProtocolAdapterFactory.unconvertConfigObject(mapper, s7AdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-s7-adapter");
        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("controllerType")).isEqualTo("S7_1500");
        assertThat(config.get("remoteRack")).isEqualTo(1);
        assertThat(config.get("remoteRack2")).isEqualTo(2);
        assertThat(config.get("remoteSlot")).isEqualTo(3);
        assertThat(config.get("remoteSlot2")).isEqualTo(4);
        assertThat(config.get("remoteTsap")).isEqualTo(5);
        final Map<String, Object> s7ToMqtt = (Map<String, Object>) config.get("s7ToMqtt");
        assertThat(s7ToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(s7ToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(s7ToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) s7ToMqtt.get("s7ToMqttMappings")).satisfiesExactly((mappings) -> {

            Map<String, Object> mapping = (Map<String, Object>) mappings.get("s7ToMqttMapping");

            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(1);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(false);
            assertThat(mapping.get("includeTagNames")).isEqualTo(true);
            assertThat(mapping.get("tagName")).isEqualTo("tag-name");
            assertThat(mapping.get("tagAddress")).isEqualTo("tag-address");
            assertThat(mapping.get("jsonPayloadCreator")).isNull();
            assertThat((List<Map<String, Object>>) mapping.get("userProperties")).satisfiesExactly((userProperty) -> {
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
