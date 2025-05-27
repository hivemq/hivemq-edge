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
package com.hivemq.edge.adapters.opcua.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.opcua.OpcUaProtocolAdapterFactory;
import com.hivemq.edge.adapters.opcua.config.opcua2mqtt.OpcUaToMqttConfig;
import com.hivemq.exceptions.UnrecoverableException;
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

import static com.hivemq.edge.adapters.opcua.config.SecPolicy.BASIC128RSA15;
import static com.hivemq.edge.adapters.opcua.config.SecPolicy.NONE;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class OpcUaProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-full-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        final BidirectionalOpcUaSpecificAdapterConfig config = (BidirectionalOpcUaSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("simulation-server-2");
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



        assertThat(protocolAdapterConfig.getNorthboundMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubb/a");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(15);
        }, mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=2;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubbb/b");
            assertThat(mapping.getMqttQos()).isEqualTo(2);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(16);
        });

        assertThat(config.getOpcuaToMqttConfig()).satisfies(mapping -> {
            assertThat(mapping.getPublishingInterval()).isEqualTo(12);
            assertThat(mapping.getServerQueueSize()).isEqualTo(13);
        });

        assertThat(protocolAdapterConfig.getSouthboundMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getTopicFilter()).isEqualTo("test/blubb/#");
        }, mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=2;i=1004");
            assertThat(mapping.getTopicFilter()).isEqualTo("test/blubbb/#");
        });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-minimal-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        final BidirectionalOpcUaSpecificAdapterConfig config = (BidirectionalOpcUaSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("simulation-server-2");
        assertThat(config.getUri()).isEqualTo("opc.tcp://CSM1.local:53530/OPCUA/SimulationServer");
        assertThat(config.getOverrideUri()).isFalse();
        assertThat(config.getSecurity().getPolicy()).isEqualTo(NONE);

        assertThat(config.getAuth()).isNull();

        assertThat(config.getTls()).satisfies(tls -> {
            assertThat(tls.isEnabled()).isFalse();
        });

        assertThat(config.getOpcuaToMqttConfig()).isNotNull();
        assertThat(protocolAdapterConfig.getNorthboundMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getMqttTopic()).isEqualTo("test/blubb");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageExpiryInterval()).isEqualTo(9223372036854775807L);
        });

        assertThat(config.getOpcuaToMqttConfig()).isNotNull();
        assertThat(config.getOpcuaToMqttConfig()).satisfies(mapping -> {
            assertThat(mapping.getPublishingInterval()).isEqualTo(1000);
            assertThat(mapping.getServerQueueSize()).isEqualTo(1);
        });

        assertThat(protocolAdapterConfig.getSouthboundMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getTagName()).isEqualTo("ns=1;i=1004");
            assertThat(mapping.getTopicFilter()).isEqualTo("test/blubb/#");
        });
    }

    @Test
    public void convertConfigObject_defaults_missing_tag() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-minimal-config-missing-tag.xml");
        assertThatThrownBy(() -> getProtocolAdapterConfig(resource)).isInstanceOf(UnrecoverableException.class);
    }

    @Test
    public void convertConfigObject_opcuaMissingUri_exception() throws Exception {
        final URL resource = getClass().getResource("/opcua-adapter-missing-uri.xml");
        assertThatThrownBy(() -> getProtocolAdapterConfig(resource)).hasMessageContaining("Missing required creator property 'uri'");
    }

    @Test
    public void unconvertConfigObject_full_valid() {

        final BidirectionalOpcUaSpecificAdapterConfig adapterConfig = new BidirectionalOpcUaSpecificAdapterConfig(
                "my.uri.com",
                true,
                new Auth(new BasicAuth("my-username", "my-password"), new X509Auth(true)),
                new Tls(true,
                        new Keystore("my/keystore/path", "keystore-password", "private-key-password"),
                        new Truststore("my/truststore/path", "truststore-password")),
                new OpcUaToMqttConfig(null, null),
                new Security(BASIC128RSA15)
        );

        final OpcUaProtocolAdapterFactory opcuaProtocolAdapterFactory =
                new OpcUaProtocolAdapterFactory(mock(ProtocolAdapterFactoryInput.class));
        final Map<String, Object> config =
                opcuaProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        assertThat(config.get("uri")).isEqualTo("my.uri.com");

        final Map<String, Object> opcuaToMqtt = (Map<String, Object>) config.get("opcuaToMqtt");
        assertThat((List<Map<String, Object>>) opcuaToMqtt.get("opcuaToMqttMappings")).isNull();

        final Map<String, Object> authMap = (Map<String, Object>) config.get("auth");
        assertThat((Map<String, Object>) authMap.get("basic")).satisfies(basic -> {
            assertThat(basic.get("username")).isEqualTo("my-username");
            assertThat(basic.get("password")).isEqualTo("my-password");
        });
        assertThat((Map<String, Object>) authMap.get("x509")).satisfies(basic -> {
            assertThat(basic.get("enabled")).isEqualTo(true);
        });

        final Map<String, Object> tlsMap = (Map<String, Object>) config.get("tls");
        assertThat(tlsMap.get("enabled")).isEqualTo(true);
        assertThat((Map<String, Object>) tlsMap.get("keystore")).satisfies(basic -> {
            assertThat(basic.get("path")).isEqualTo("my/keystore/path");
            assertThat(basic.get("password")).isEqualTo("keystore-password");
            assertThat(basic.get("privateKeyPassword")).isEqualTo("private-key-password");
        });
        assertThat((Map<String, Object>) tlsMap.get("truststore")).satisfies(basic -> {
            assertThat(basic.get("path")).isEqualTo("my/truststore/path");
            assertThat(basic.get("password")).isEqualTo("truststore-password");
        });
    }


    @Test
    public void unconvertConfigObject_default_valid() {
        final BidirectionalOpcUaSpecificAdapterConfig adapterConfig = new BidirectionalOpcUaSpecificAdapterConfig(
                "my.uri.com",
                true,
                null,
                null,
                new OpcUaToMqttConfig(null, null),
                null
        );

        final OpcUaProtocolAdapterFactory opcuaProtocolAdapterFactory =
                new OpcUaProtocolAdapterFactory(mock(ProtocolAdapterFactoryInput.class));
        final Map<String, Object> config =
                opcuaProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        assertThat(config.get("uri")).isEqualTo("my.uri.com");

        final Map<String, Object> opcuaToMqtt = (Map<String, Object>) config.get("opcuaToMqtt");
        assertThat((List<Map<String, Object>>) opcuaToMqtt.get("opcuaToMqttMappings")).isNull(); // must be empty

        assertThat(config.get("auth")).isNull();

        final Map<String, Object> tlsMap = (Map<String, Object>) config.get("tls");
        assertThat(tlsMap.get("enabled")).isEqualTo(false);
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

        final OpcUaProtocolAdapterFactory protocolAdapterFactory = new OpcUaProtocolAdapterFactory(mockInput);
        final ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("opcua")).thenReturn(Optional.of(protocolAdapterFactory));
        final ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, mapper);
        return converter;
    }

    private @NotNull HiveMQConfigEntity loadConfig(final @NotNull File configFile) {
        final ConfigFileReaderWriter readerWriter = new ConfigFileReaderWriter(new ConfigurationFile(configFile), List.of());
        return readerWriter.applyConfig();
    }
}
