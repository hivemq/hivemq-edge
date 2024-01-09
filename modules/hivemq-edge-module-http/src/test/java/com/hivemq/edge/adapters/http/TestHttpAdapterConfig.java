package com.hivemq.edge.adapters.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ApiConfigurator;
import com.hivemq.configuration.reader.BridgeConfigurator;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.configuration.reader.DynamicConfigConfigurator;
import com.hivemq.configuration.reader.InternalConfigurator;
import com.hivemq.configuration.reader.ListenerConfigurator;
import com.hivemq.configuration.reader.MqttConfigurator;
import com.hivemq.configuration.reader.MqttsnConfigurator;
import com.hivemq.configuration.reader.PersistenceConfigurator;
import com.hivemq.configuration.reader.ProtocolAdapterConfigurator;
import com.hivemq.configuration.reader.RestrictionConfigurator;
import com.hivemq.configuration.reader.SecurityConfigurator;
import com.hivemq.configuration.reader.UnsConfigurator;
import com.hivemq.configuration.reader.UsageTrackingConfigurator;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.protocols.ProtocolAdapterUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * @author Simon L Johnson
 */
public class TestHttpAdapterConfig {

    @TempDir
    protected File tempDir;

    @Test
    public void testHttpAdapterReadsEmptyHeaders() throws IOException {

        File configFile = loadTestConfigFile(tempDir, "http-config-empty-header.xml");
        HiveMQConfigEntity configEntity  = loadConfig(configFile);
        Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();
        Assertions.assertNotNull(adapters.get("http"), "Adapter map should contain http adapter config");
        Assertions.assertTrue(adapters.get("http") instanceof Map, "Adapter should be an instance of a List");
        Map map = (Map) adapters.get("http");
        ObjectMapper mapper = new ObjectMapper();
        mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(mapper);
        HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        HttpAdapterConfig config = httpProtocolAdapterFactory.convertConfigObject(mapper, map);
        Assertions.assertNull(config.getHttpHeaders(), "Header array should be null to match coercion");
    }

    @Test
    public void testHttpAdapterReadsPopulatedHeaders() throws IOException {

        File configFile = loadTestConfigFile(tempDir, "http-config-with-headers.xml");
        HiveMQConfigEntity configEntity  = loadConfig(configFile);
        Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();
        Assertions.assertNotNull(adapters.get("http"), "Adapter map should contain http adapter config");
        Assertions.assertTrue(adapters.get("http") instanceof Map, "Adapter should be an instance of a List");
        Map map = (Map) adapters.get("http");
        ObjectMapper mapper = new ObjectMapper();
        mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(mapper);
        HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        HttpAdapterConfig config = httpProtocolAdapterFactory.convertConfigObject(mapper, map);
        Assertions.assertEquals(2, config.getHttpHeaders().size(), "Header array should contain 2 elements");
    }

    protected HiveMQConfigEntity loadConfig(@NotNull final File configFile){
        ConfigFileReaderWriter readerWriter = new TestConfigReader(new ConfigurationFile(configFile),
                mock(RestrictionConfigurator.class),
                mock(SecurityConfigurator.class),
                mock(MqttConfigurator.class),
                mock(ListenerConfigurator.class),
                mock(PersistenceConfigurator.class),
                mock(MqttsnConfigurator.class),
                mock(BridgeConfigurator.class),
                mock(ApiConfigurator.class),
                mock(UnsConfigurator.class),
                mock(DynamicConfigConfigurator.class),
                mock(UsageTrackingConfigurator.class),
                mock(ProtocolAdapterConfigurator.class),
                mock(InternalConfigurator.class));
        return readerWriter.applyConfig();
    }

    public static File loadTestConfigFile(@NotNull final File directory, @NotNull String fileName) throws IOException {
        if(!fileName.startsWith("/")){
            fileName = "/"+fileName;
        }
        try (final InputStream is =
                     TestHttpAdapterConfig.class.getResourceAsStream(fileName)){
            final File tempFile = new File(directory, "config.xml");
            FileUtils.copyInputStreamToFile(is, tempFile);
            return tempFile;
        }
    }

    private static class TestConfigReader extends ConfigFileReaderWriter {

        public TestConfigReader(
                final ConfigurationFile configurationFile,
                final RestrictionConfigurator restrictionConfigurator,
                final SecurityConfigurator securityConfigurator,
                final MqttConfigurator mqttConfigurator,
                final ListenerConfigurator listenerConfigurator,
                final PersistenceConfigurator persistenceConfigurator,
                final MqttsnConfigurator mqttsnConfigurator,
                final BridgeConfigurator bridgeConfigurator,
                final ApiConfigurator apiConfigurator,
                final UnsConfigurator unsConfigurator,
                final DynamicConfigConfigurator dynamicConfigConfigurator,
                final UsageTrackingConfigurator usageTrackingConfigurator,
                final ProtocolAdapterConfigurator protocolAdapterConfigurator,
                final InternalConfigurator internalConfigurator) {
            super(configurationFile,
                    restrictionConfigurator,
                    securityConfigurator,
                    mqttConfigurator,
                    listenerConfigurator,
                    persistenceConfigurator,
                    mqttsnConfigurator,
                    bridgeConfigurator,
                    apiConfigurator,
                    unsConfigurator,
                    dynamicConfigConfigurator,
                    usageTrackingConfigurator,
                    protocolAdapterConfigurator,
                    internalConfigurator);
        }
    }
}
