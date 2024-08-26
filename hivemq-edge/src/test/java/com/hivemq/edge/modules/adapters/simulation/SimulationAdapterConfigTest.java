package com.hivemq.edge.modules.adapters.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerSubscription;
import static com.hivemq.adapter.sdk.api.config.MessageHandlingOptions.MQTTMessagePerTag;
import static com.hivemq.protocols.ProtocolAdapterUtils.createProtocolAdapterMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SimulationAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/simulation-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final SimulationProtocolAdapterFactory simulationProtocolAdapterFactory =
                new SimulationProtocolAdapterFactory();
        final SimulationAdapterConfig config =
                simulationProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("simulation"));

        assertThat(config.getId()).isEqualTo("my-simulation-protocol-adapter");
        assertThat(config.getSimulationToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getSimulationToMqttConfig().getSimulationToMqttMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("my-name");
                assertThat(userProperty.getValue()).isEqualTo("my-value");
            });
        }, subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic/2");
            assertThat(subscription.getQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
                assertThat(userProperty.getName()).isEqualTo("my-name");
                assertThat(userProperty.getValue()).isEqualTo("my-value");
            });
        });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/configs/simulation/simulation-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final SimulationProtocolAdapterFactory simulationProtocolAdapterFactory =
                new SimulationProtocolAdapterFactory();
        final SimulationAdapterConfig config =
                simulationProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("simulation"));

        assertThat(config.getId()).isEqualTo("my-simulation-protocol-adapter");
        assertThat(config.getSimulationToMqttConfig().getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getSimulationToMqttConfig().getSimulationToMqttMappings()).satisfiesExactly(subscription -> {
            assertThat(subscription.getMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getQos()).isEqualTo(0);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.getIncludeTimestamp()).isTrue();
            assertThat(subscription.getIncludeTagNames()).isFalse();

            assertThat(subscription.getUserProperties()).isEmpty();
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
