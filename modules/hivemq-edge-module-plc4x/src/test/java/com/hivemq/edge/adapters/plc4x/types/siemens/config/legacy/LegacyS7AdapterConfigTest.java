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
package com.hivemq.edge.adapters.plc4x.types.siemens.config.legacy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.plc4x.config.Plc4xDataType;
import com.hivemq.edge.adapters.plc4x.types.siemens.S7ProtocolAdapterFactory;
import com.hivemq.edge.adapters.plc4x.types.siemens.config.S7AdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.edge.adapters.plc4x.types.siemens.config.S7AdapterConfig.ControllerType.S7_1500;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyS7AdapterConfigTest {

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

    @BeforeEach
    void setUp() {
        when(protocolAdapterTagService.addTag(any(),
                any(),
                any())).thenReturn(ProtocolAdapterTagService.AddStatus.SUCCESS);
    }

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-s7-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();
        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory =
                new S7ProtocolAdapterFactory(protocolAdapterFactoryInput);
        final S7AdapterConfig config =
                (S7AdapterConfig) s7ProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("s7"), false);

        assertThat(config.getId()).isEqualTo("my-s7-id");
        assertThat(config.getPort()).isEqualTo(102);
        assertThat(config.getHost()).isEqualTo("my-ip-addr-or-host");
        assertThat(config.getControllerType()).isEqualTo(S7_1500);
        assertThat(config.getRemoteRack()).isEqualTo(1);
        assertThat(config.getRemoteRack2()).isEqualTo(2);
        assertThat(config.getRemoteSlot()).isEqualTo(3);
        assertThat(config.getRemoteSlot2()).isEqualTo(4);
        assertThat(config.getRemoteTsap()).isEqualTo(5);
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isFalse();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic/1");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getDataType()).isEqualTo(Plc4xDataType.DATA_TYPE.BOOL);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagName()).isEqualTo("my-tag-name-1");

        }, mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
            assertThat(mapping.getDataType()).isEqualTo(Plc4xDataType.DATA_TYPE.BOOL);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagName()).isEqualTo("my-tag-name-2");
        });

        verify(protocolAdapterTagService, times(2)).addTag(any(), any(), any());
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-s7-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory =
                new S7ProtocolAdapterFactory(protocolAdapterFactoryInput);
        final S7AdapterConfig config =
                (S7AdapterConfig) s7ProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("s7"), false);

        assertThat(config.getId()).isEqualTo("my-s7-id");
        assertThat(config.getPort()).isEqualTo(102);
        assertThat(config.getHost()).isEqualTo("my-ip-address-or-host");
        assertThat(config.getControllerType()).isEqualTo(S7_1500);
        assertThat(config.getRemoteRack()).isEqualTo(0);
        assertThat(config.getRemoteRack2()).isEqualTo(0);
        assertThat(config.getRemoteSlot()).isEqualTo(0);
        assertThat(config.getRemoteSlot2()).isEqualTo(0);
        assertThat(config.getRemoteTsap()).isEqualTo(0);
        assertThat(config.getPlc4xToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getPlc4xToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPlc4xToMqttConfig().getPublishChangedDataOnly()).isTrue();
        assertThat(config.getPlc4xToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic/1");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getDataType()).isEqualTo(Plc4xDataType.DATA_TYPE.SINT);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagName()).isEqualTo("my-tag-name-1");
        });

        verify(protocolAdapterTagService, times(1)).addTag(any(), any(), any());

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
