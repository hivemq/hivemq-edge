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
import com.hivemq.configuration.entity.adapter.ProtocolAdapterEntity;
import com.hivemq.exceptions.UnrecoverableException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    public void whenArbitraryField_thenMapCorrectlyFilled() throws Exception {
        final URL resource = getClass().getResource("/arbitrary-properties-config1.xml");
        final File path = Path.of(resource.toURI()).toFile();

        final ConfigurationFile configurationFile = new ConfigurationFile(path);
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

        final @NotNull List<ProtocolAdapterEntity> config = hiveMQConfigEntity.getProtocolAdapterConfig();

        //TODO
        /*
        assertNotNull(config);
        assertEquals(5, config.keySet().size());

        assertThat((Map<String, Object>) config.get("my-protocol-adapter1")).satisfies(map -> {
            assertThat(map.get("numval")).isEqualTo("3");
            assertThat(map.get("textval")).isEqualTo("thisisatext");
            assertThat(map.get("listvals")).isEqualTo(List.of("entry1", "entry2"));
        });

        assertThat((Map<String, Object>) config.get("my-protocol-adapter2")).satisfies(map -> {
            assertThat(map.get("boolval")).isEqualTo("true");
            assertThat(map.get("listvals")).isEqualTo(List.of("entry3"));
        });

        assertThat((Map<String, Object>) config.get("my-protocol-adapter3")).satisfies(map -> {
            assertThat((List<Map<String, Object>>) map.get("persons")).satisfiesExactly(person -> {
                assertThat(person.get("name")).isEqualTo("john");
                assertThat(person.get("lastName")).isEqualTo("doe");
            }, person -> {
                assertThat(person.get("name")).isEqualTo("boris");
            });
        });

        assertThat((Map<String, Object>) config.get("my-protocol-adapter4")).satisfies(map -> {
            assertThat(map.get("boolval")).isEqualTo("false");
            assertThat((List<Map<String, Object>>) map.get("cats")).satisfiesExactly(cat -> {
                assertThat(cat.get("name")).isEqualTo("leo");
            }, cat -> {
                assertThat(cat.get("name")).isEqualTo("karli");
            });
        });

        assertThat((Map<String, Object>) config.get("my-protocol-adapter5")).satisfies(map -> {
            assertThat((List<Map<String, Object>>) map.get("cats")).satisfiesExactly(cat -> {
                assertThat(cat.get("name")).isEqualTo("emma");
            });
        });

         */
    }

    @SuppressWarnings("unchecked")
    @Test
    public void whenUserProperties_thenMapCorrectlyFilled() throws Exception {
        final File tempFile = new File(tempDir, "conf.xml");
        FileUtils.writeStringToFile(tempFile,
                "<hivemq>\n" +
                        "    <protocol-adapters>\n" +
                        "        <test-node>\n" +
                        "            <mqttUserProperties>\n" +
                        "                <mqttUserProperty>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value1</value>\n" +
                        "                </mqttUserProperty>\n" +
                        "                <mqttUserProperty>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value2</value>\n" +
                        "                </mqttUserProperty>\n" +
                        "            </mqttUserProperties>" +
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

        final @NotNull List<ProtocolAdapterEntity> config = hiveMQConfigEntity.getProtocolAdapterConfig();

        assertNotNull(config);
        System.out.println(config);
        //TODO
        /*
        assertEquals(1, config.keySet().size());

        final List<Map<String, String>> userProperties1 =
                (List<Map<String, String>>) ((Map<String, Object>) config.get("test-node")).get("mqttUserProperties");
        assertThat(userProperties1).satisfiesExactly(userProperty1 -> {
            assertThat(userProperty1.get("name")).isEqualTo("my-name");
            assertThat(userProperty1.get("value")).isEqualTo("my-value1");
        }, userProperty2 -> {
            assertThat(userProperty2.get("name")).isEqualTo("my-name");
            assertThat(userProperty2.get("value")).isEqualTo("my-value2");
        });

        configFileReader.writeConfig();
        final String afterReload = FileUtils.readFileToString(tempFile, UTF_8);
        assertThat(afterReload).contains("mqttUserProperty");

         */
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
                        "        <modbus>\n" +
                        "            <mqttUserProperties>\n" +
                        "                <mqttUserPropertie>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value1</value>\n" +
                        "                </mqttUserPropertie>\n" +
                        "                <mqttUserPropertie>\n" +
                        "                    <name>my-name</name>\n" +
                        "                    <value>my-value2</value>\n" +
                        "                </mqttUserPropertie>\n" +
                        "            </mqttUserProperties>" +
                        "        </modbus>\n" +
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

        final @NotNull List<ProtocolAdapterEntity> config = hiveMQConfigEntity.getProtocolAdapterConfig();

        //TODO
        /*
        assertNotNull(config);
        System.out.println(config);
        assertEquals(1, config.keySet().size());

        final List<Map<String, String>> userProperties1 =
                (List<Map<String, String>>) ((Map<String, Object>) config.get("modbus")).get("mqttUserProperties");
        assertThat(userProperties1).satisfiesExactly(userProperty1 -> {
            assertThat(userProperty1.get("name")).isEqualTo("my-name");
            assertThat(userProperty1.get("value")).isEqualTo("my-value1");
        }, userProperty2 -> {
            assertThat(userProperty2.get("name")).isEqualTo("my-name");
            assertThat(userProperty2.get("value")).isEqualTo("my-value2");
        });

        configFileReader.writeConfig();
        final String afterReload = FileUtils.readFileToString(tempFile, UTF_8);
        assertThat(afterReload).contains("mqttUserProperty");
        final Map<String, Object> config2 = hiveMQConfigEntity.getProtocolAdapterConfig();
        assertEquals(1, config2.keySet().size());
        configFileReader.applyConfig();
        final List<Map<String, String>> userProperties2 =
                (List<Map<String, String>>) ((Map<String, Object>) config2.get("modbus")).get("mqttUserProperties");
        assertThat(userProperties2).satisfiesExactly(userProperty1 -> {
            assertThat(userProperty1.get("name")).isEqualTo("my-name");
            assertThat(userProperty1.get("value")).isEqualTo("my-value1");
        }, userProperty2 -> {
            assertThat(userProperty2.get("name")).isEqualTo("my-name");
            assertThat(userProperty2.get("value")).isEqualTo("my-value2");
        });

         */
    }
}
