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
package com.hivemq.edge.adapters.etherip.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MessageHandlingOptions;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.etherip.EipProtocolAdapterFactory;
import com.hivemq.edge.adapters.etherip.config.tag.EipTag;
import com.hivemq.edge.adapters.etherip.config.tag.EipTagDefinition;
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

import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EipProtocolAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/eip-adapter-full-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final EipSpecificAdapterConfig config = (EipSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-eip-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.eip-server.com");
        assertThat(config.getBackplane()).isEqualTo(4);
        assertThat(config.getSlot()).isEqualTo(5);
        assertThat(config.getEipToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getEipToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getEipToMqttConfig().getPublishChangedDataOnly()).isFalse();

        assertThat(config.getEipToMqttConfig().getMappings()).isEmpty(); //mappings are supposed to be ignored when rendered to XML

        assertThat(protocolAdapterConfig.getTags())
                .allSatisfy(t -> {
                    assertThat(t)
                            .isInstanceOf(EipTag.class)
                            .extracting("name", "description", "definition")
                            .contains("tag-name", "description", new EipTagDefinition("addressy", EipDataType.BOOL));
                });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/eip-adapter-minimal-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        final EipSpecificAdapterConfig config = (EipSpecificAdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(protocolAdapterConfig.getAdapterId()).isEqualTo("my-eip-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.eip-server.com");
        assertThat(config.getBackplane()).isEqualTo(1);
        assertThat(config.getSlot()).isEqualTo(0);
        assertThat(config.getEipToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getEipToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getEipToMqttConfig().getPublishChangedDataOnly()).isTrue();

        assertThat(config.getEipToMqttConfig().getMappings()).isEmpty(); //mappings are supposed to be ignored when rendered to XML

        assertThat(protocolAdapterConfig.getTags())
                .allSatisfy(t -> {
                    assertThat(t)
                            .isInstanceOf(EipTag.class)
                            .extracting("name", "description", "definition")
                            .contains("tag-name", "description", new EipTagDefinition("addressy", EipDataType.BOOL));
                });
    }



    @Test
    public void unconvertConfigObject_full_valid() {
        final EipSpecificAdapterConfig adapterConfig = new EipSpecificAdapterConfig(
                14,
                "my.host.com",
                15,
                16,
                new EipToMqttConfig(
                        12,
                        13,
                        true,
                        List.of(
                                new EipToMqttMapping(
                                        "tag1",
                                        1,
                                        null,
                                        false,
                                        false,
                                        "tag1",
                                        null)
                        )));

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final EipProtocolAdapterFactory eipProtocolAdapterFactory =
                new EipProtocolAdapterFactory(mockInput);
        final Map<String, Object> config = eipProtocolAdapterFactory.unconvertConfigObject(mapper, adapterConfig);

        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("backplane")).isEqualTo(15);
        assertThat(config.get("slot")).isEqualTo(16);
        final Map<String, Object> eipToMqtt = (Map<String, Object>) config.get("eipToMqtt");
        assertThat(eipToMqtt.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(eipToMqtt.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(eipToMqtt.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) eipToMqtt.get("eipToMqttMappings")).isNull(); //mappings are supposed to be ignored when rendered to XML
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

        EipProtocolAdapterFactory protocolAdapterFactory = new EipProtocolAdapterFactory(mockInput);
        ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("eip")).thenReturn(Optional.of(protocolAdapterFactory));
        ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, mapper);
        return converter;
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
