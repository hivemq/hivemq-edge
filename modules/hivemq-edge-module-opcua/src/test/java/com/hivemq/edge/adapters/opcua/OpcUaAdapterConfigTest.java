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
package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OpcUaAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final OpcUaProtocolAdapterFactory oopcUaProtocolAdapterFactory = new OpcUaProtocolAdapterFactory();
        final OpcUaAdapterConfig config =
                oopcUaProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("opcua"));

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
        assertThat(config.getOpcuaToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getNode()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubb/#");
            assertThat(mapping.getQos()).isEqualTo(1);
            assertThat(mapping.getPublishingInterval()).isEqualTo(12);
            assertThat(mapping.getServerQueueSize()).isEqualTo(13);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(15);
        }, mapping -> {
            assertThat(mapping.getNode()).isEqualTo("ns=2;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubbb/#");
            assertThat(mapping.getQos()).isEqualTo(2);
            assertThat(mapping.getPublishingInterval()).isEqualTo(13);
            assertThat(mapping.getServerQueueSize()).isEqualTo(14);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(16);
        });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final OpcUaProtocolAdapterFactory oopcUaProtocolAdapterFactory = new OpcUaProtocolAdapterFactory();
        final OpcUaAdapterConfig config =
                oopcUaProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("opcua"));

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
        assertThat(config.getOpcuaToMqttConfig().getMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getNode()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubb/#");
            assertThat(mapping.getQos()).isEqualTo(0);
            assertThat(mapping.getPublishingInterval()).isEqualTo(1000);
            assertThat(mapping.getServerQueueSize()).isEqualTo(1);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(4294967295L);
        });
    }

    @Test
    public void convertConfigObject_opcuaIdMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-missing-id.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final  OpcUaProtocolAdapterFactory opcUaProtocolAdapterFactory= new OpcUaProtocolAdapterFactory();
        assertThatThrownBy(() -> opcUaProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("opcua"))).hasMessageContaining("Missing required creator property 'id'");
    }

    @Test
    public void convertConfigObject_opcuaMissingMqttTopic_exception() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-missing-mqtt-topic.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final  OpcUaProtocolAdapterFactory opcUaProtocolAdapterFactory= new OpcUaProtocolAdapterFactory();
        assertThatThrownBy(() -> opcUaProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("opcua"))).hasMessageContaining("Missing required creator property 'mqttTopic'");
    }

    @Test
    public void convertConfigObject_opcuaMissingNode_exception() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-missing-node.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final  OpcUaProtocolAdapterFactory opcUaProtocolAdapterFactory= new OpcUaProtocolAdapterFactory();
        assertThatThrownBy(() -> opcUaProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("opcua"))).hasMessageContaining("Missing required creator property 'node'");
    }

    @Test
    public void convertConfigObject_opcuaMissingUri_exception() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-missing-uri.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final  OpcUaProtocolAdapterFactory opcUaProtocolAdapterFactory= new OpcUaProtocolAdapterFactory();
        assertThatThrownBy(() -> opcUaProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("opcua"))).hasMessageContaining("Missing required creator property 'uri'");
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
