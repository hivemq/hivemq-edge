/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.configuration.writer;

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
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.io.TempDir;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.mockito.Mockito.mock;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractConfigWriterTest {

    protected ConfigurationService configurationService;

    protected ConfigFileReaderWriter createFileReaderWriter(final @NotNull File file){

        configurationService = new TestConfigurationBootstrap().getConfigurationService();
        final ConfigurationFile configurationFile = new ConfigurationFile(file);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
                mock(RestrictionConfigurator.class),
                mock(SecurityConfigurator.class),
                mock(MqttConfigurator.class),
                mock(ListenerConfigurator.class),
                mock(PersistenceConfigurator.class),
                mock(MqttsnConfigurator.class),
                new BridgeConfigurator(configurationService.bridgeConfiguration()),
                mock(ApiConfigurator.class),
                new UnsConfigurator(configurationService.unsConfiguration()),
                new DynamicConfigConfigurator(configurationService.gatewayConfiguration()),
                new UsageTrackingConfigurator(configurationService.usageTrackingConfiguration()),
                new ProtocolAdapterConfigurator(configurationService.protocolAdapterConfigurationService()),
                new InternalConfigurator(configurationService.internalConfigurationService()));
        configFileReader.setDefaultBackupConfig(false);
        return configFileReader;
    }

    protected File loadTestConfigFile() throws IOException {
        try (final InputStream is =
                     AbstractConfigWriterTest.class.getResourceAsStream("/" + getTestConfigName())){
            final File tempFile = new File(System.getProperty("java.io.tmpdir"), "original-config.xml");
            tempFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(is, tempFile);
            return tempFile;
        }
    }

    protected String getTestConfigName(){
        return "test-config.xml";
    }
}
