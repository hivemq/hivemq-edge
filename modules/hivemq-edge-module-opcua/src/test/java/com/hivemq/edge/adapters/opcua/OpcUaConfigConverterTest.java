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
package com.hivemq.edge.adapters.opcua;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
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
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class OpcUaConfigConverterTest {

    @SuppressWarnings("unchecked")
    @Test
    public void whenArbitraryField_thenMapCorrectlyFilled(@TempDir final @NotNull File tempDir) throws IOException {
        final File tempFile = new File(tempDir, "conf.xml");
        final BufferedWriter writer = Files.newWriter(tempFile, UTF_8);
        writer.write("<hivemq>\n" +
                "    <protocol-adapters>\n" +
                "        <opc-ua-adapter>\n" +
                "            <id>simulation-server-1</id>\n" +
                "            <uri>opc.tcp://CSM1.local:53530/OPCUA/SimulationServer</uri>\n" +
                "            <auth>\n" +
                "                <basic>\n" +
                "                    <username>edge</username>\n" +
                "                    <password>password</password>\n" +
                "                </basic>\n" +
                "            </auth>\n" +
                "            <subscriptions>\n" +
                "                <subscription>\n" +
                "                    <node>ns=3;i=1004</node>\n" +
                "                    <mqtt-topic>test</mqtt-topic>\n" +
                "                </subscription>\n" +
                "            </subscriptions>\n" +
                "        </opc-ua-adapter>\n" +
                "        <opc-ua-adapter>\n" +
                "            <id>simulation-server-2</id>\n" +
                "            <uri>opc.tcp://CSM1.local:53530/OPCUA/SimulationServer</uri>\n" +
                "            <auth>\n" +
                "                <basic>\n" +
                "                    <username>edge</username>\n" +
                "                    <password>password</password>\n" +
                "                </basic>\n" +
                "            </auth>\n" +
                "            <subscriptions>\n" +
                "                <subscription>\n" +
                "                    <node>ns=1;i=1004</node>\n" +
                "                    <mqtt-topic>test/blubb/#</mqtt-topic>\n" +
                "                </subscription>\n" +
                "            </subscriptions>\n" +
                "        </opc-ua-adapter>\n" +
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
        assertThat(config).isNotNull();

        final List<Map<String, Object>> adapters = (List<Map<String, Object>>) config.get("opc-ua-adapter");
        for (Map<String, Object> objectMap : adapters) {
            final ObjectMapper objectMapper = new ObjectMapper();
            final OpcUaAdapterConfig adapterConfig = objectMapper.convertValue(objectMap, OpcUaAdapterConfig.class);

            assertThat(adapterConfig).isNotNull();
        }
    }
}
