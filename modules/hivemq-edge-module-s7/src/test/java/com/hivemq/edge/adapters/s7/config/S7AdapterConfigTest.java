package com.hivemq.edge.adapters.s7.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.MqttUserProperty;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.s7.S7ProtocolAdapterFactory;
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

class S7AdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/s7-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory = new S7ProtocolAdapterFactory(false);
        final S7AdapterConfig config =
                (S7AdapterConfig) s7ProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("s7-new"));

        assertThat(config.getId()).isEqualTo("my-s7-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.s7-server.com");
        assertThat(config.getControllerType()).isEqualTo(S7AdapterConfig.ControllerType.S7_400);
        assertThat(config.getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPublishChangedDataOnly()).isFalse();
        assertThat(config.getS7ToMqttMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isTrue();
            assertThat(mapping.getTagAddress()).isEqualTo("tag-address");
            assertThat(mapping.getTagName()).isEqualTo("tag-name");

        }, mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(mapping.getMqttQos()).isEqualTo(1);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
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

        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory = new S7ProtocolAdapterFactory(false);
        final S7AdapterConfig config =
                (S7AdapterConfig) s7ProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("s7-new"));

        assertThat(config).isNotNull();
        assertThat(config.getId()).isEqualTo("my-s7-protocol-adapter");
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.s7-server.com");
        assertThat(config.getControllerType()).isEqualTo(S7AdapterConfig.ControllerType.S7_400);
        assertThat(config.getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPublishChangedDataOnly()).isTrue();
        assertThat(config.getS7ToMqttMappings()).satisfiesExactly(mapping -> {
            assertThat(mapping.getMqttTopic()).isEqualTo("my/topic");
            assertThat(mapping.getMqttQos()).isEqualTo(0);
            assertThat(mapping.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(mapping.getIncludeTimestamp()).isTrue();
            assertThat(mapping.getIncludeTagNames()).isFalse();
            assertThat(mapping.getTagAddress()).isEqualTo("tag-address");
            assertThat(mapping.getTagName()).isEqualTo("tag-name");
            assertThat(mapping.getDataType()).isEqualTo(S7DataType.BOOL);
        });
    }

    @Test
    public void unconvertConfigObject_full_valid() {
        final S7ToMqttConfig pollingContext = new S7ToMqttConfig("my/destination",
                1,
                MQTTMessagePerSubscription,
                false,
                true,
                "tag-name",
                "tag-address",
                S7DataType.BOOL,
                List.of(new MqttUserProperty("my-name", "my-value"))
        );

        final S7AdapterConfig s7AdapterConfig = new S7AdapterConfig("my-s7-adapter",
                14,
                "my.host.com",
                S7AdapterConfig.ControllerType.S7_1500,
                1,
                2,
                3,
                4,
                5,
                false,
                List.of(pollingContext));

        final S7ProtocolAdapterFactory s7ProtocolAdapterFactory = new S7ProtocolAdapterFactory(false);
        final Map<String, Object> config =
                s7ProtocolAdapterFactory.unconvertConfigObject(mapper, s7AdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-s7-adapter");
        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("controllerType")).isEqualTo("S7_1500");
        assertThat(config.get("remoteRack")).isEqualTo(2);
        assertThat(config.get("remoteSlot")).isEqualTo(1);
        assertThat(config.get("pollingIntervalMillis")).isEqualTo(4);
        assertThat(config.get("maxPollingErrorsBeforeRemoval")).isEqualTo(5);
        assertThat(config.get("publishChangedDataOnly")).isEqualTo(false);

        assertThat((List<Map<String, Object>>) config.get("s7ToMqttMappings")).satisfiesExactly((mapping) -> {

            assertThat(mapping.get("mqttTopic")).isEqualTo("my/destination");
            assertThat(mapping.get("mqttQos")).isEqualTo(1);
            assertThat(mapping.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(mapping.get("includeTimestamp")).isEqualTo(false);
            assertThat(mapping.get("includeTagNames")).isEqualTo(true);
            assertThat(mapping.get("tagName")).isEqualTo("tag-name");
            assertThat(mapping.get("tagAddress")).isEqualTo("tag-address");
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
