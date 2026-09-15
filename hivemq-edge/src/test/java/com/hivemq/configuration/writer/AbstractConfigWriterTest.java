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

import static java.util.Objects.requireNonNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.hivemq.configuration.info.SystemInformation;
import com.hivemq.configuration.reader.ApiConfigurator;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.TestConfigurationBootstrap;

public abstract class AbstractConfigWriterTest {

    protected @Nullable ConfigurationService configurationService;

    protected @NotNull ConfigFileReaderWriter createFileReaderWriter(final @NotNull File file) {

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
                mock(SystemInformation.class),
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

    /**
     * The fixture, staged in a directory of this test's own.
     * <p>
     * It used to be one fixed path under {@code java.io.tmpdir}, shared by every test in every class and
     * every Gradle fork on the machine — so two of them running at once wrote each other's configuration,
     * and a leftover from a previous run was picked up by the next (EDG-882 review v02, R2-19).
     */
    protected @NotNull File loadTestConfigFile() throws IOException {
        try (final InputStream is =
                requireNonNull(AbstractConfigWriterTest.class.getResourceAsStream("/test-config.xml"))) {
            final File tempFile = Files.createTempDirectory("edge-config-writer-test")
                    .resolve("original-config.xml")
                    .toFile();
            tempFile.deleteOnExit();
            tempFile.getParentFile().deleteOnExit();
            FileUtils.copyInputStreamToFile(is, tempFile);
            return tempFile;
        }
    }
}
