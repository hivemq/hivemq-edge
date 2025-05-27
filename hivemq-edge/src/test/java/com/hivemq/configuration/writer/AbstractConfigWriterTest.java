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
import com.hivemq.configuration.reader.BridgeExtractor;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.configuration.reader.Configurator;
import com.hivemq.configuration.reader.DynamicConfigConfigurator;
import com.hivemq.configuration.reader.InternalConfigurator;
import com.hivemq.configuration.reader.ListenerConfigurator;
import com.hivemq.configuration.reader.ModuleConfigurator;
import com.hivemq.configuration.reader.MqttConfigurator;
import com.hivemq.configuration.reader.MqttsnConfigurator;
import com.hivemq.configuration.reader.PersistenceConfigurator;
import com.hivemq.configuration.reader.RestrictionConfigurator;
import com.hivemq.configuration.reader.SecurityConfigurator;
import com.hivemq.configuration.reader.UsageTrackingConfigurator;
import com.hivemq.configuration.service.ConfigurationService;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.io.FileUtils;
import util.TestConfigurationBootstrap;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractConfigWriterTest {

    protected ConfigurationService configurationService;

    protected ConfigFileReaderWriter createFileReaderWriter(final @NotNull File file){

        configurationService = new TestConfigurationBootstrap().getConfigurationService();

        final RestrictionConfigurator restrictionConfigurator = mock(RestrictionConfigurator.class);
        when(restrictionConfigurator.applyConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final SecurityConfigurator securityConfigurator = mock(SecurityConfigurator.class);
        when(securityConfigurator.applyConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final MqttConfigurator mqttConfigurator = mock(MqttConfigurator.class);
        when(mqttConfigurator.applyConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final ListenerConfigurator listenerConfigurator = mock(ListenerConfigurator.class);
        when(listenerConfigurator.applyConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final PersistenceConfigurator persistenceConfigurator = mock(PersistenceConfigurator.class);
        when(persistenceConfigurator.applyConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final MqttsnConfigurator mqttsnConfigurator = mock(MqttsnConfigurator.class);
        when(mqttsnConfigurator.applyConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final ApiConfigurator apiConfigurator = mock(ApiConfigurator.class);
        when(apiConfigurator.applyConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final ConfigurationFile configurationFile = new ConfigurationFile(file);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(
                configurationFile,
                List.of(
                    restrictionConfigurator,
                    securityConfigurator,
                    mqttConfigurator,
                    listenerConfigurator,
                    persistenceConfigurator,
                    mqttsnConfigurator,
                    apiConfigurator,
                    new DynamicConfigConfigurator(configurationService.gatewayConfiguration()),
                    new UsageTrackingConfigurator(configurationService.usageTrackingConfiguration()),
                    new ModuleConfigurator(configurationService.commercialModuleConfigurationService()),
                    new InternalConfigurator(configurationService.internalConfigurationService())));
        configFileReader.setDefaultBackupConfig(false);
        return configFileReader;
    }

    protected File loadTestConfigFile() throws IOException {
        try (final InputStream is = AbstractConfigWriterTest.class.getResourceAsStream("/test-config.xml")) {
            final File tempFile = new File(System.getProperty("java.io.tmpdir"), "original-config.xml");
            tempFile.deleteOnExit();
            FileUtils.copyInputStreamToFile(is, tempFile);
            return tempFile;
        }
    }
}
