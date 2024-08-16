package com.hivemq.edge.adapters.modbus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.UserProperty;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.adapters.modbus.config.AddressRange;
import com.hivemq.edge.adapters.modbus.config.ModbusAdapterConfig;
import com.hivemq.edge.adapters.modbus.config.PollingContextImpl;
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

            assertThat(subscription.getUserProperties()).satisfiesExactly(userProperty1 -> {
                assertThat(userProperty1.getName()).isEqualTo("my-name");
                assertThat(userProperty1.getValue()).isEqualTo("my-value1");
            }, userProperty2 -> {
                assertThat(userProperty2.getName()).isEqualTo("my-name");
                assertThat(userProperty2.getValue()).isEqualTo("my-value2");
            });

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

    @Test
    public void unconvertConfigObject_full_valid() {
        final PollingContextImpl pollingContext = new PollingContextImpl("my/destination",
                1,
                MQTTMessagePerSubscription,
                false,
                true,
                List.of(new UserProperty("my-name", "my-value")),
                new AddressRange(1, 2));

        final ModbusAdapterConfig modbusAdapterConfig = new ModbusAdapterConfig("my-modbus-adapter",
                12,
                13,
                14,
                "my.host.com",
                15,
                true,
                List.of(pollingContext));

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        final Map<String, Object> config =
                modbusProtocolAdapterFactory.unconvertConfigObject(mapper, modbusAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-modbus-adapter");
        assertThat(config.get("pollingIntervalMillis")).isEqualTo(12);
        assertThat(config.get("maxPollingErrorsBeforeRemoval")).isEqualTo(13);
        assertThat(config.get("port")).isEqualTo(14);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("timeout")).isEqualTo(15);
        assertThat(config.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) config.get("subscriptions")).satisfiesExactly((subscription) -> {
            assertThat(subscription.get("destination")).isEqualTo("my/destination");
            assertThat(subscription.get("qos")).isEqualTo(1);
            assertThat(subscription.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(subscription.get("includeTimestamp")).isEqualTo(false);
            assertThat(subscription.get("includeTagNames")).isEqualTo(true);
            assertThat((List<Map<String, Object>>) subscription.get("userProperties")).satisfiesExactly((userProperty) -> {
                assertThat(userProperty.get("name")).isEqualTo("my-name");
                assertThat(userProperty.get("value")).isEqualTo("my-value");
            });
            assertThat((Map<String, Object>) subscription.get("addressRange")).satisfies((addressRange) -> {
                assertThat(addressRange.get("startIdx")).isEqualTo(1);
                assertThat(addressRange.get("endIdx")).isEqualTo(2);
            });
        });
    }

    @Test
    public void unconvertConfigObject_defaults() {
        final PollingContextImpl pollingContext =
                new PollingContextImpl("my/destination", null, null, null, null, null, new AddressRange(1, 2));

        final ModbusAdapterConfig modbusAdapterConfig = new ModbusAdapterConfig("my-modbus-adapter",
                null,
                null,
                13,
                "my.host.com",
                null,
                null,
                List.of(pollingContext));

        final ModbusProtocolAdapterFactory modbusProtocolAdapterFactory = new ModbusProtocolAdapterFactory();
        final Map<String, Object> config =
                modbusProtocolAdapterFactory.unconvertConfigObject(mapper, modbusAdapterConfig);

        assertThat(config.get("id")).isEqualTo("my-modbus-adapter");
        assertThat(config.get("pollingIntervalMillis")).isEqualTo(1000);
        assertThat(config.get("maxPollingErrorsBeforeRemoval")).isEqualTo(10);
        assertThat(config.get("port")).isEqualTo(13);
        assertThat(config.get("host")).isEqualTo("my.host.com");
        assertThat(config.get("timeout")).isEqualTo(5000);
        assertThat(config.get("publishChangedDataOnly")).isEqualTo(true);

        assertThat((List<Map<String, Object>>) config.get("subscriptions")).satisfiesExactly((subscription) -> {
            assertThat(subscription.get("destination")).isEqualTo("my/destination");
            assertThat(subscription.get("qos")).isEqualTo(0);
            assertThat(subscription.get("messageHandlingOptions")).isEqualTo("MQTTMessagePerSubscription");
            assertThat(subscription.get("includeTimestamp")).isEqualTo(true);
            assertThat(subscription.get("includeTagNames")).isEqualTo(false);
            assertThat((List<Map<String, Object>>) subscription.get("userProperties")).isEmpty();
            assertThat((Map<String, Object>) subscription.get("addressRange")).satisfies((addressRange) -> {
                assertThat(addressRange.get("startIdx")).isEqualTo(1);
                assertThat(addressRange.get("endIdx")).isEqualTo(2);
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
