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
package com.hivemq.configuration.reader;

import com.google.common.io.Files;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.adapter.MqttUserPropertyEntity;
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.exceptions.UnrecoverableException;
import org.jetbrains.annotations.NotNull;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("NullabilityAnnotations")
public class ConfigFileReaderTest {

    @TempDir
    public File tempDir;

    @Test
    public void whenConfigDoesNotExist_thenDoNotStartHiveMQ() throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter(tempFile);
        assertThrows(UnrecoverableException.class, configFileReader::applyConfig);
    }

    @Test
    public void whenConfigIsEmpty_thenDoNotStartHiveMQ() throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final BufferedWriter writer = Files.newWriter(tempFile, UTF_8);
        writer.write("");
        writer.close();
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter(tempFile);
        assertThrows(UnrecoverableException.class, configFileReader::applyConfig);
    }

    @Test
    public void whenMinimalConfig_thenNoException() throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final BufferedWriter writer = Files.newWriter(tempFile, UTF_8);
        writer.write("<hivemq></hivemq>");
        writer.close();
        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter(tempFile);
        assertDoesNotThrow(configFileReader::applyConfig);
    }


    /**
     * Checks the downwards compatability for 'userPropertie'.
     */
    @SuppressWarnings({"unchecked", "SpellCheckingInspection"})
    @Test
    public void whenUserPropertie_thenMapCorrectlyFilled() throws Exception {
        final File tempFile = new File(tempDir, "conf.xml");
        FileUtils.writeStringToFile(tempFile,
                "<hivemq>\n" +
                        "    <protocol-adapters>\n" +
                        "        <protocol-adapter>\n" +
                        "            <adapterId>test</adapterId>" +
                        "            <protocolId>http</protocolId>" +
                        "            <northboundMappings>" +
                        "                <northboundMapping>" +
                        "                   <topic>test</topic>\n" +
                        "                   <tagName>test</tagName>\n" +
                        "                   <mqttUserProperties>\n" +
                        "                   <mqttUserProperty>\n" +
                        "                        <name>my-name</name>\n" +
                        "                       <value>my-value1</value>\n" +
                        "                   </mqttUserProperty>\n" +
                        "                   <mqttUserProperty>\n" +
                        "                        <name>my-name</name>\n" +
                        "                       <value>my-value2</value>\n" +
                        "                   </mqttUserProperty>\n" +
                        "                   </mqttUserProperties>" +
                        "                </northboundMapping>" +
                        "            </northboundMappings>" +
                        "        </protocol-adapter>\n" +
                        "    </protocol-adapters>\n" +
                        "</hivemq>",
                UTF_8);

        final ConfigFileReaderWriter configFileReader = getConfigFileReaderWriter(tempFile);
        final HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();

        final @NotNull List<ProtocolAdapterEntity> config = hiveMQConfigEntity.getProtocolAdapterConfig();

        assertNotNull(config);
        System.out.println(config);
        assertEquals(1, config.size());

        final ProtocolAdapterEntity protocolAdapterEntity = config.get(0);

        final List<MqttUserPropertyEntity> userProperties =
                protocolAdapterEntity.getNorthboundMappingEntities().get(0).getUserProperties();
        assertTrue(userProperties.contains(new MqttUserPropertyEntity("my-name", "my-value2")));
        assertTrue(userProperties.contains(new MqttUserPropertyEntity("my-name", "my-value2")));

        configFileReader.writeConfig();
        final String afterReload = FileUtils.readFileToString(tempFile, UTF_8);
        assertThat(afterReload).contains("mqttUserProperty");
        final @NotNull List<ProtocolAdapterEntity> config2 = hiveMQConfigEntity.getProtocolAdapterConfig();
        assertEquals(1, config2.size());
        configFileReader.applyConfig();

        final ProtocolAdapterEntity protocolAdapterEntityAfterReload = config.get(0);
        final List<MqttUserPropertyEntity> userPropertiesAfterReload =
                protocolAdapterEntityAfterReload.getNorthboundMappingEntities().get(0).getUserProperties();
        assertTrue(userPropertiesAfterReload.contains(new MqttUserPropertyEntity("my-name", "my-value2")));
        assertTrue(userPropertiesAfterReload.contains(new MqttUserPropertyEntity("my-name", "my-value2")));
    }

    private static @NotNull ConfigFileReaderWriter getConfigFileReaderWriter(File tempFile) {
        final ConfigurationFile configurationFile = new ConfigurationFile(tempFile);

        final RestrictionConfigurator restrictionConfigurator = mock(RestrictionConfigurator.class);
        when(restrictionConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final SecurityConfigurator securityConfigurator = mock(SecurityConfigurator.class);
        when(securityConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final MqttConfigurator mqttConfigurator = mock(MqttConfigurator.class);
        when(mqttConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final ListenerConfigurator listenerConfigurator = mock(ListenerConfigurator.class);
        when(listenerConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final PersistenceConfigurator persistenceConfigurator = mock(PersistenceConfigurator.class);
        when(persistenceConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final MqttsnConfigurator mqttsnConfigurator = mock(MqttsnConfigurator.class);
        when(mqttsnConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final BridgeConfigurator bridgeConfigurator = mock(BridgeConfigurator.class);
        when(bridgeConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final ApiConfigurator apiConfigurator = mock(ApiConfigurator.class);
        when(apiConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final UnsConfigurator unsConfigurator = mock(UnsConfigurator.class);
        when(unsConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final DynamicConfigConfigurator dynamicConfigConfigurator = mock(DynamicConfigConfigurator.class);
        when(dynamicConfigConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final UsageTrackingConfigurator usageTrackingConfigurator = mock(UsageTrackingConfigurator.class);
        when(usageTrackingConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final ProtocolAdapterConfigurator protocolAdapterConfigurator = mock(ProtocolAdapterConfigurator.class);
        when(protocolAdapterConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final ModuleConfigurator moduleConfigurator = mock(ModuleConfigurator.class);
        when(moduleConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);

        final InternalConfigurator internalConfigurator = mock(InternalConfigurator.class);
        when(internalConfigurator.setConfig(any())).thenReturn(Configurator.ConfigResult.SUCCESS);


        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(
                configurationFile,
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
                moduleConfigurator,
                internalConfigurator);
        return configFileReader;
    }
}
