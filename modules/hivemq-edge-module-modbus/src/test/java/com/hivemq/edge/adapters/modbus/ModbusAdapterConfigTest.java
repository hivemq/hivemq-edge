package com.hivemq.edge.adapters.modbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@SuppressWarnings({"unchecked", "rawtypes"})
public class ModbusAdapterConfigTest {

    private final @NotNull ObjectMapper mapper = createProtocolAdapterMapper(new ObjectMapper());

    @Test
    public void convertConfigObject_fullConfig_valid() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-full-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        final ModbusAdapterConfig config =
                modbusProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("modbus"));

        assertThat(config.getId()).isEqualTo("my-modbus-protocol-adapter");
        assertThat(config.getPollingIntervalMillis()).isEqualTo(10);
        assertThat(config.getMaxPollingErrorsBeforeRemoval()).isEqualTo(9);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeout()).isEqualTo(1337);
        assertThat(config.getPublishChangedDataOnly()).isFalse();
        assertThat(config.getSubscriptions()).satisfiesExactly(subscription -> {
            assertThat(subscription.getDestinationMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getQos()).isEqualTo(1);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerTag);
            assertThat(subscription.getIncludeTimestamp()).isFalse();
            assertThat(subscription.getIncludeTagNames()).isTrue();

//            TODO: https://hivemq.kanbanize.com/ctrl_board/57/cards/24704/details/
//            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty -> {
//                assertThat(userProperty.getName()).isEqualTo("my-name");
//                assertThat(userProperty.getValue()).isEqualTo("my-value");
//            });

            assertThat(subscription.getAddressRange().startIdx).isEqualTo(11);
            assertThat(subscription.getAddressRange().endIdx).isEqualTo(13);
        });
    }

    @Test
    public void convertConfigObject_defaults_valid() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-minimal-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        final ModbusAdapterConfig config =
                modbusProtocolAdapterFactory.convertConfigObject(mapper, (Map) adapters.get("modbus"));

        assertThat(config.getId()).isEqualTo("my-modbus-protocol-adapter");
        assertThat(config.getPollingIntervalMillis()).isEqualTo(1000);
        assertThat(config.getMaxPollingErrorsBeforeRemoval()).isEqualTo(10);
        assertThat(config.getPort()).isEqualTo(1234);
        assertThat(config.getHost()).isEqualTo("my.modbus-server.com");
        assertThat(config.getTimeout()).isEqualTo(5000);
        assertThat(config.getPublishChangedDataOnly()).isTrue();
        assertThat(config.getSubscriptions()).satisfiesExactly(subscription -> {
            assertThat(subscription.getDestinationMqttTopic()).isEqualTo("my/topic");
            assertThat(subscription.getQos()).isEqualTo(0);
            assertThat(subscription.getMessageHandlingOptions()).isEqualTo(MQTTMessagePerSubscription);
            assertThat(subscription.getIncludeTimestamp()).isTrue();
            assertThat(subscription.getIncludeTagNames()).isFalse();
            assertThat(subscription.getUserProperties()).isEmpty();
            assertThat(subscription.getAddressRange().startIdx).isEqualTo(11);
            assertThat(subscription.getAddressRange().endIdx).isEqualTo(13);
        });
    }

    @Test
    public void convertConfigObject_idMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-id-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        assertThatThrownBy(() -> modbusProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("modbus"))).hasMessageContaining("Missing required creator property 'id'");
    }

    @Test
    public void convertConfigObject_hostMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-host-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        assertThatThrownBy(() -> modbusProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("modbus"))).hasMessageContaining("Missing required creator property 'host'");
    }

    @Test
    public void convertConfigObject_portMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-port-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        assertThatThrownBy(() -> modbusProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("modbus"))).hasMessageContaining("Missing required creator property 'port'");
    }

    @Test
    public void convertConfigObject_destinationMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-destination-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        assertThatThrownBy(() -> modbusProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("modbus"))).hasMessageContaining("Missing required creator property 'destination'");
    }

    @Test
    public void convertConfigObject_addressRangeMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-address-range-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        assertThatThrownBy(() -> modbusProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("modbus")));
    }

    @Test
    public void convertConfigObject_startIdxMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-startIdx-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        assertThatThrownBy(() -> modbusProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("modbus"))).hasMessageContaining("Missing required creator property 'startIdx'");
    }

    @Test
    public void convertConfigObject_endIdxMissing_exception() throws Exception {
        final URL resource = getClass().getResource("/modbus-adapter-missing-endIdx-config.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final HiveMQConfigEntity configEntity = loadConfig(path);
        final Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        assertThatThrownBy(() -> modbusProtocolAdapterFactory.convertConfigObject(mapper,
                (Map) adapters.get("modbus"))).hasMessageContaining("Missing required creator property 'endIdx'");
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
