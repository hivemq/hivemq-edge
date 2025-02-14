package com.hivemq.edge.adapters.s7.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.factories.ProtocolAdapterFactoryInput;
import com.hivemq.adapter.sdk.api.tag.Tag;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.s7.S7ProtocolAdapterFactory;
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

class S7AdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/s7-adapter-full-config.xml");

        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        final S7AdapterConfig config = (S7AdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.s7-server.com");
        assertThat(config.getControllerType()).isEqualTo(S7AdapterConfig.ControllerType.S7_400);
        assertThat(config.getS7ToMqttConfig().getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getS7ToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getS7ToMqttConfig().getPublishChangedDataOnly()).isFalse();

        assertThat(protocolAdapterConfig.getTags())
                .allSatisfy(t -> {
                    assertThat(t)
                            .isInstanceOf(S7Tag.class)
                            .extracting(Tag::getName, Tag::getDescription, Tag::getDefinition)
                            .contains("tag-name", "description", new S7TagDefinition("%IB1", S7DataType.INT));
                });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/s7-adapter-minimal-config.xml");
        final ProtocolAdapterConfig protocolAdapterConfig = getProtocolAdapterConfig(resource);

        final S7AdapterConfig config = (S7AdapterConfig) protocolAdapterConfig.getAdapterConfig();
        assertThat(protocolAdapterConfig.missingTags())
                .isEmpty();

        assertThat(config).isNotNull();
        assertThat(config.getPort()).isEqualTo(102);
        assertThat(config.getHost()).isEqualTo("my.s7-server.com");
        assertThat(config.getControllerType()).isEqualTo(S7AdapterConfig.ControllerType.S7_400);
        assertThat(config.getS7ToMqttConfig().getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getS7ToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getS7ToMqttConfig().getPublishChangedDataOnly()).isTrue();

        assertThat(protocolAdapterConfig.getTags())
                .allSatisfy(t -> {
                    assertThat(t)
                            .isInstanceOf(S7Tag.class)
                            .extracting(Tag::getName, Tag::getDescription, Tag::getDefinition)
                            .contains("tag-name", "description", new S7TagDefinition("tag-address", S7DataType.BOOL));
                });
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final S7ToMqttConfig pollingContext = new S7ToMqttConfig(
                3000,
                1,
                false
        );

        final S7AdapterConfig s7AdapterConfig = new S7AdapterConfig(
                14,
                "my.host.com",
                S7AdapterConfig.ControllerType.S7_1500,
                1,
                2,
                3,
                pollingContext);

        final ProtocolAdapterFactoryInput mockInput = mock(ProtocolAdapterFactoryInput.class);
        when(mockInput.isWritingEnabled()).thenReturn(false);
        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory = new S7ProtocolAdapterFactory(mockInput);
        final Map<String, Object> config =
                s7ProtocolAdapterFactory.unconvertConfigObject(mapper, s7AdapterConfig);

        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("controllerType")).isEqualTo("S7_1500");
        assertThat(config.get("remoteRack")).isEqualTo(1);
        assertThat(config.get("remoteSlot")).isEqualTo(2);

        assertThat((Map<String, Object>) config.get("s7ToMqtt"))
                .extracting("pollingIntervalMillis", "maxPollingErrorsBeforeRemoval", "publishChangedDataOnly")
                .containsExactly(3000, 1, false);
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

        S7ProtocolAdapterFactory protocolAdapterFactory = new S7ProtocolAdapterFactory(mockInput);
        ProtocolAdapterFactoryManager manager = mock(ProtocolAdapterFactoryManager.class);
        when(manager.get("s7-new")).thenReturn(Optional.of(protocolAdapterFactory));
        ProtocolAdapterConfigConverter converter = new ProtocolAdapterConfigConverter(manager, mapper);
        return converter;
    }

    private @NotNull HiveMQConfigEntity loadConfig(final @NotNull File configFile) {
        final ConfigFileReaderWriter readerWriter = new ConfigFileReaderWriter(
                new ConfigurationFile(configFile),
                List.of());
        return readerWriter.applyConfig();
    }

}
