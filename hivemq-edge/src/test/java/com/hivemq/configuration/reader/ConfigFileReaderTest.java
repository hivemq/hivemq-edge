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
import com.hivemq.exceptions.UnrecoverableException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@SuppressWarnings("NullabilityAnnotations")
public class ConfigFileReaderTest {

    @TempDir
    public File tempDir;

    @Test
    public void whenConfigDoesNotExist_thenDoNotStartHiveMQ() throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final ConfigurationFile configurationFile = new ConfigurationFile(tempFile);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
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
                mock(ModuleConfigurator.class),
                mock(InternalConfigurator.class));
        assertThrows(UnrecoverableException.class, configFileReader::applyConfig);
    }

    @Test
    public void whenConfigIsEmpty_thenDoNotStartHiveMQ() throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final BufferedWriter writer = Files.newWriter(tempFile, UTF_8);
        writer.write("");
        writer.close();
        final ConfigurationFile configurationFile = new ConfigurationFile(tempFile);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
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
                mock(ModuleConfigurator.class),
                mock(InternalConfigurator.class));
        assertThrows(UnrecoverableException.class, configFileReader::applyConfig);
    }

    @Test
    public void whenMinimalConfig_thenNoException() throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final BufferedWriter writer = Files.newWriter(tempFile, UTF_8);
        writer.write("<hivemq></hivemq>");
        writer.close();
        final ConfigurationFile configurationFile = new ConfigurationFile(tempFile);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
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
                mock(ModuleConfigurator.class),
                mock(InternalConfigurator.class));
        assertDoesNotThrow(configFileReader::applyConfig);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whenArbitraryField_thenMapCorrectlyFilled() throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final BufferedWriter writer = Files.newWriter(tempFile, UTF_8);
        writer.write("<hivemq>\n" +
                "    <protocol-adapters>\n" +
                "        <test-node>\n" +
                "            <textval>thisisatext</textval>\n" +
                "            <numval>3</numval>\n" +
                "            <listvals>\n" +
                "                <listval>entry1</listval>\n" +
                "                <listval>entry2</listval>\n" +
                "            </listvals>\n" +
                "        </test-node>\n" +
                "        <test-node>\n" +
                "            <boolval>true</boolval>\n" +
                "            <listvals>\n" +
                "                <listval>entry3</listval>\n" +
                "            </listvals>\n" +
                "        </test-node>\n" +
                "    </protocol-adapters>\n" +
                "</hivemq>");
        writer.close();
        final ConfigurationFile configurationFile = new ConfigurationFile(tempFile);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
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
                mock(ModuleConfigurator.class),
                mock(InternalConfigurator.class));
        final HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();

        final Map<String, Object> config = hiveMQConfigEntity.getProtocolAdapterConfig();

        assertNotNull(config);
        System.out.println(config);
        assertEquals(1, config.keySet().size());
        final Object testnode = config.get("test-node");
        assertNotNull(testnode);
        assertTrue(testnode instanceof List);
        List<Object> testnodeList = (List<Object>) testnode;
        assertEquals(2, testnodeList.size());
        assertEquals(Map.of("numval", "3", "textval", "thisisatext", "listvals", List.of("entry1", "entry2")),
                testnodeList.get(0));

        assertEquals(Map.of("boolval", "true", "listvals", List.of("entry3")), testnodeList.get(1));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whenUserProperties_thenMapCorrectlyFilled() throws Exception {
        final File tempFile = new File(tempDir, "conf.xml");
        FileUtils.writeStringToFile(tempFile,
                "<hivemq>\n" +
                        "    <protocol-adapters>\n" +
                        "        <test-node>\n" +
                        "            <userProperties>\n" +
                        "                <userProperty>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value1</value>\n" +
                        "                </userProperty>\n" +
                        "                <userProperty>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value2</value>\n" +
                        "                </userProperty>\n" +
                        "            </userProperties>" +
                        "        </test-node>\n" +
                        "    </protocol-adapters>\n" +
                        "</hivemq>",
                UTF_8);

        final ConfigurationFile configurationFile = new ConfigurationFile(tempFile);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
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
                mock(ModuleConfigurator.class),
                mock(InternalConfigurator.class));
        final HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();

        final Map<String, Object> config = hiveMQConfigEntity.getProtocolAdapterConfig();

        assertNotNull(config);
        System.out.println(config);
        assertEquals(1, config.keySet().size());

        final List<Map<String, String>> userProperties1 =
                (List<Map<String, String>>) ((Map<String, Object>) config.get("test-node")).get("userProperties");
        assertThat(userProperties1).satisfiesExactly(userProperty1 -> {
            assertThat(userProperty1.get("name")).isEqualTo("my-name");
            assertThat(userProperty1.get("value")).isEqualTo("my-value1");
        }, userProperty2 -> {
            assertThat(userProperty2.get("name")).isEqualTo("my-name");
            assertThat(userProperty2.get("value")).isEqualTo("my-value2");
        });

        configFileReader.writeConfig();
        final String afterReload = FileUtils.readFileToString(tempFile, UTF_8);
        assertThat(afterReload).contains("userProperty");
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
                        "        <test-node>\n" +
                        "            <userProperties>\n" +
                        "                <userPropertie>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value1</value>\n" +
                        "                </userPropertie>\n" +
                        "                <userPropertie>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value2</value>\n" +
                        "                </userPropertie>\n" +
                        "            </userProperties>" +
                        "        </test-node>\n" +
                        "    </protocol-adapters>\n" +
                        "</hivemq>",
                UTF_8);

        final ConfigurationFile configurationFile = new ConfigurationFile(tempFile);
        final ConfigFileReaderWriter configFileReader = new ConfigFileReaderWriter(configurationFile,
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
                mock(ModuleConfigurator.class),
                mock(InternalConfigurator.class));
        final HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();

        final Map<String, Object> config = hiveMQConfigEntity.getProtocolAdapterConfig();

        assertNotNull(config);
        System.out.println(config);
        assertEquals(1, config.keySet().size());

        final List<Map<String, String>> userProperties1 =
                (List<Map<String, String>>) ((Map<String, Object>) config.get("test-node")).get("userProperties");
        assertThat(userProperties1).satisfiesExactly(userProperty1 -> {
            assertThat(userProperty1.get("name")).isEqualTo("my-name");
            assertThat(userProperty1.get("value")).isEqualTo("my-value1");
        }, userProperty2 -> {
            assertThat(userProperty2.get("name")).isEqualTo("my-name");
            assertThat(userProperty2.get("value")).isEqualTo("my-value2");
        });

        configFileReader.writeConfig();
        final String afterReload = FileUtils.readFileToString(tempFile, UTF_8);
        assertThat(afterReload).contains("userProperty");
    }
}
