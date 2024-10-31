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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterTagService;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import com.hivemq.edge.adapters.opcua.config.OpcUaAdapterConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC128RSA15;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.NONE;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
class LegacyOpcUaAdapterConfigTest {

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
        final URL resource = getClass().getResource("/legacy-opcua-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final OpcUaProtocolAdapterFactory opcUaProtocolAdapterFactory =
                new OpcUaProtocolAdapterFactory(protocolAdapterFactoryInput);
        final OpcUaAdapterConfig config = (OpcUaAdapterConfig) opcUaProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("opcua"), false);

        assertThat(config.getId()).isEqualTo("simulation-server-2");
        assertThat(config.getUri()).isEqualTo("opc.tcp://CSM1.local:53530/OPCUA/SimulationServer");
        assertThat(config.getOverrideUri()).isTrue();
        assertThat(config.getSecurity()).satisfies(security -> {
            assertThat(security.getPolicy()).isEqualTo(BASIC128RSA15);
        });

        assertThat(config.getAuth()).satisfies(auth -> {
            assertThat(auth.getBasicAuth()).isNotNull();
            assertThat(auth.getBasicAuth().getUsername()).isEqualTo("edge");
            assertThat(auth.getBasicAuth().getPassword()).isEqualTo("password");

            assertThat(auth.getX509Auth()).isNotNull();
            assertThat(auth.getX509Auth().isEnabled()).isTrue();
        });

        assertThat(config.getTls()).satisfies(tls -> {
            assertThat(tls.isEnabled()).isTrue();

            assertThat(tls.getKeystore()).isNotNull();
            assertThat(tls.getKeystore().getPath()).isEqualTo("path/to/keystore");
            assertThat(tls.getKeystore().getPassword()).isEqualTo("keystore-password");
            assertThat(tls.getKeystore().getPrivateKeyPassword()).isEqualTo("private-key-password");

            assertThat(tls.getTruststore()).isNotNull();
            assertThat(tls.getTruststore().getPath()).isEqualTo("path/to/truststore");
            assertThat(tls.getTruststore().getPassword()).isEqualTo("truststore-password");
        });

        assertThat(config.getOpcuaToMqttConfig()).isNotNull();
        assertThat(config.getOpcuaToMqttConfig().getOpcuaToMqttMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubb/#");
            assertThat(mapping.getQos()).isEqualTo(1);
            assertThat(mapping.getPublishingInterval()).isEqualTo(12);
            assertThat(mapping.getServerQueueSize()).isEqualTo(13);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(15);
        }, mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=2;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubbb/#");
            assertThat(mapping.getQos()).isEqualTo(2);
            assertThat(mapping.getPublishingInterval()).isEqualTo(13);
            assertThat(mapping.getServerQueueSize()).isEqualTo(14);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(16);
        });

        verify(protocolAdapterTagService, times(2)).addTag(any(), any(), any());
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/legacy-opcua-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final OpcUaProtocolAdapterFactory opcUaProtocolAdapterFactory =
                new OpcUaProtocolAdapterFactory(protocolAdapterFactoryInput);
        final OpcUaAdapterConfig config = (OpcUaAdapterConfig) opcUaProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("opcua"), false);

        assertThat(config.getId()).isEqualTo("simulation-server-2");
        assertThat(config.getUri()).isEqualTo("opc.tcp://CSM1.local:53530/OPCUA/SimulationServer");
        assertThat(config.getOverrideUri()).isFalse();
        assertThat(config.getSecurity().getPolicy()).isEqualTo(NONE);

        assertThat(config.getAuth()).satisfies(auth -> {
            assertThat(auth.getBasicAuth()).isNull();
            assertThat(auth.getX509Auth()).isNull();
        });

        assertThat(config.getTls()).satisfies(tls -> {
            assertThat(tls.isEnabled()).isFalse();
        });

        assertThat(config.getOpcuaToMqttConfig()).isNotNull();
        assertThat(config.getOpcuaToMqttConfig().getOpcuaToMqttMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubb/#");
            assertThat(mapping.getQos()).isEqualTo(0);
            assertThat(mapping.getPublishingInterval()).isEqualTo(1000);
            assertThat(mapping.getServerQueueSize()).isEqualTo(1);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(4294967295L);
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
