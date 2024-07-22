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
import com.hivemq.configuration.reader.ModuleConfigurator;
import com.hivemq.configuration.reader.MqttConfigurator;
import com.hivemq.configuration.reader.MqttsnConfigurator;
import com.hivemq.configuration.reader.PersistenceConfigurator;
import com.hivemq.configuration.reader.ProtocolAdapterConfigurator;
import com.hivemq.configuration.reader.RestrictionConfigurator;
import com.hivemq.configuration.reader.SecurityConfigurator;
import com.hivemq.configuration.reader.UnsConfigurator;
import com.hivemq.configuration.reader.UsageTrackingConfigurator;
import com.hivemq.protocols.ProtocolAdapterUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Simon L Johnson
 */
public class TestHttpAdapterConfig {

    @TempDir
    protected @NotNull File tempDir;

    @Test
    public void testHttpAdapterReadsEmptyHeaders() throws IOException {

        File configFile = loadTestConfigFile(tempDir, "http-config-empty-header.xml");
        HiveMQConfigEntity configEntity = loadConfig(configFile);
        Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();
        assertNotNull(adapters.get("http"), "Adapter map should contain http adapter config");
        assertInstanceOf(Map.class, adapters.get("http"), "Adapter should be an instance of a List");
        //noinspection rawtypes
        Map map = (Map) adapters.get("http");
        ObjectMapper mapper = new ObjectMapper();
        mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(mapper);
        HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        HttpAdapterConfig config = httpProtocolAdapterFactory.convertConfigObject(mapper, map);
        assertTrue(config.getHttpHeaders().isEmpty(), "Header array should be null to match coercion");
    }

    @Test
    public void testHttpAdapterReadsPopulatedHeaders() throws IOException {

        File configFile = loadTestConfigFile(tempDir, "http-config-with-headers.xml");
        HiveMQConfigEntity configEntity = loadConfig(configFile);
        Map<String, Object> adapters = configEntity.getProtocolAdapterConfig();
        assertNotNull(adapters.get("http"), "Adapter map should contain http adapter config");
        assertInstanceOf(Map.class, adapters.get("http"), "Adapter should be an instance of a List");
        //noinspection rawtypes
        Map map = (Map) adapters.get("http");
        ObjectMapper mapper = new ObjectMapper();
        mapper = ProtocolAdapterUtils.createProtocolAdapterMapper(mapper);
        HttpProtocolAdapterFactory httpProtocolAdapterFactory = new HttpProtocolAdapterFactory();
        HttpAdapterConfig config = httpProtocolAdapterFactory.convertConfigObject(mapper, map);
        assertEquals(2, config.getHttpHeaders().size(), "Header array should contain 2 elements");
    }

    protected @NotNull HiveMQConfigEntity loadConfig(@NotNull final File configFile) {
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
        if (!fileName.startsWith("/")) {
            fileName = "/" + fileName;
        }
        try (final InputStream is = TestHttpAdapterConfig.class.getResourceAsStream(fileName)) {
            final File tempFile = new File(directory, "config.xml");
            FileUtils.copyInputStreamToFile(is, tempFile);
            return tempFile;
        }
    }

    private static class TestConfigReader extends ConfigFileReaderWriter {

        public TestConfigReader(
                final @NotNull ConfigurationFile configurationFile,
                final @NotNull RestrictionConfigurator restrictionConfigurator,
                final @NotNull SecurityConfigurator securityConfigurator,
                final @NotNull MqttConfigurator mqttConfigurator,
                final @NotNull ListenerConfigurator listenerConfigurator,
                final @NotNull PersistenceConfigurator persistenceConfigurator,
                final @NotNull MqttsnConfigurator mqttsnConfigurator,
                final @NotNull BridgeConfigurator bridgeConfigurator,
                final @NotNull ApiConfigurator apiConfigurator,
                final @NotNull UnsConfigurator unsConfigurator,
                final @NotNull DynamicConfigConfigurator dynamicConfigConfigurator,
                final @NotNull UsageTrackingConfigurator usageTrackingConfigurator,
                final @NotNull ProtocolAdapterConfigurator protocolAdapterConfigurator,
                final @NotNull InternalConfigurator internalConfigurator) {
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
                    usageTrackingConfigurator, protocolAdapterConfigurator, mock(ModuleConfigurator.class),
                    internalConfigurator);
        }
    }
}
