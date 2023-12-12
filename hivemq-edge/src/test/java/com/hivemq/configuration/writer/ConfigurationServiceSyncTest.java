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

import com.google.common.collect.ImmutableList;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.CustomUserProperty;
import com.hivemq.bridge.config.LocalSubscription;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.config.RemoteSubscription;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.entity.uns.ISA95Entity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.uns.config.ISA95;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationServiceSyncTest extends AbstractConfigWriterTest {


    @Test
    public void test_sync_uns() throws IOException {

        File tempFile = loadTestConfigFile();
        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();
        configurationService.setConfigFileReaderWriter(configFileReader);

        ISA95 isa95 = new ISA95.Builder().withArea("testingArea")
                .withSite("testingSite")
                .withEnterprise("testingEnterprise")
                .withProductionLine("testProductionLine")
                .withWorkCell("testingWorkCell")
                .withEnabled(false)
                .build();

        configurationService.unsConfiguration().setISA95(isa95);

        //-- Check the writes have been proxied onto the configuration model
        assertISA95Equals(isa95, hiveMQConfigEntity.getUns().getIsa95());

        //-- Check the writes have been written to disk
        final ConfigFileReaderWriter updatedVersionFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity updatedHiveMQConfigEntity = updatedVersionFileReader.applyConfig();
        assertISA95Equals(isa95, updatedHiveMQConfigEntity.getUns().getIsa95());
    }

    @Test
    public void test_sync_adapters() throws IOException {

        File tempFile = loadTestConfigFile();
        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();
        configurationService.setConfigFileReaderWriter(configFileReader);

        Map<String, Object> config = configurationService.protocolAdapterConfigurationService().getAllConfigs();

        Assert.assertEquals("Adapter type count should match", 2, config.size());
        Assert.assertEquals("Modbus Adapter type value should match",
                ArrayList.class,
                config.get("modbus-adapter").getClass());
        Assert.assertEquals("OPC-UA Adapter type value should match",
                HashMap.class,
                config.get("opc-ua-adapter").getClass());
        Assert.assertEquals("Modbus instance count should match", 2, ((List) config.get("modbus-adapter")).size());
        //-- Check the writes have been proxied onto the configuration model

        //-- Remove first adapter
        ((List<?>) config.get("modbus-adapter")).remove(0);

        //-- Ensure the original config is NOT YET UPDATED
        Assert.assertEquals("Modbus instance count should NOT be reflected in configuration",
                2,
                ((List) hiveMQConfigEntity.getProtocolAdapterConfig().get("modbus-adapter")).size());

        configurationService.protocolAdapterConfigurationService().setAllConfigs(config);

        Assert.assertEquals("Modbus instance count be reflected in configuration",
                1,
                ((List) hiveMQConfigEntity.getProtocolAdapterConfig().get("modbus-adapter")).size());

        //-- Change the value of an adapter
        ((Map) config.get("opc-ua-adapter")).put("uri", "new-uri-in-here");

        configurationService.protocolAdapterConfigurationService().setAllConfigs(config);

        //-- Check the writes have been written to disk
        final ConfigFileReaderWriter updatedVersionFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity updatedHiveMQConfigEntity = updatedVersionFileReader.applyConfig();
        Assert.assertEquals("OPC-UA URI value should be updated",
                "new-uri-in-here",
                ((Map) updatedHiveMQConfigEntity.getProtocolAdapterConfig().get("opc-ua-adapter")).get("uri"));
    }

    @Test
    public void test_sync_bridge() throws IOException {

        File tempFile = loadTestConfigFile();
        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity hiveMQConfigEntity = configFileReader.applyConfig();
        configurationService.setConfigFileReaderWriter(configFileReader);

        ImmutableList.Builder<RemoteSubscription> remoteSubscriptionBuilder = ImmutableList.builder();
        remoteSubscriptionBuilder.add(new RemoteSubscription(List.of("filter1", "filter2"),
                "destination/filter",
                List.of(CustomUserProperty.of("someKey", "someValue")),
                true,
                2));

        ImmutableList.Builder<LocalSubscription> localSubscriptionBuilder = ImmutableList.builder();
        localSubscriptionBuilder.add(new LocalSubscription(List.of("filter1", "filter2"),
                "destination/filter",
                List.of("excludes1", "excludes2"),
                List.of(CustomUserProperty.of("someKey", "someValue")),
                true,
                2,
                1000L));

        MqttBridge newBridge = new MqttBridge.Builder().withId("MyNewBridge")
                .withClientId("MyNewBridgeClientId")
                .withHost("MyNewBridgeHost")
                .withKeepAlive(120)
                .withPort(999)
                .withUsername("MyBridgeUserName")
                .withPassword("MyBridgeUserPassword")
                .withSessionExpiry(88)
                .withCleanStart(false)
                .withLoopPreventionEnabled(true)
                .withLoopPreventionHopCount(44)
                .withBridgeTls(new BridgeTls("MyBridgeKeyStorePath",
                        "MyBridgeKeyStorePassword",
                        "MyBridgeKeyStorePath",
                        "MyBridgeTruststorePath",
                        "MyBridgeTruststorePassword",
                        List.of("p1", "p2"),
                        List.of("c1"),
                        "PSK",
                        "MyTrustType",
                        true,
                        8876))
                .withLocalSubscriptions(localSubscriptionBuilder.build())
                .withRemoteSubscriptions(remoteSubscriptionBuilder.build())
                .build();

        configurationService.bridgeConfiguration().addBridge(newBridge);

        //-- Check the writes have been proxied onto the configuration model
        Assert.assertEquals("New bridge should be in config model",
                1,
                hiveMQConfigEntity.getBridgeConfig().stream().filter(b -> b.getId().equals(newBridge.getId())).count());

        //-- Check the writes have been written to disk
        final ConfigFileReaderWriter updatedVersionFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity updatedHiveMQConfigEntity = updatedVersionFileReader.applyConfig();

        //-- Check the writes persist an XML  re-read
        Assert.assertEquals("New bridge count should be 2", 2, updatedHiveMQConfigEntity.getBridgeConfig().size());

        Assert.assertEquals("New bridge should be in re-read model",
                1,
                updatedHiveMQConfigEntity.getBridgeConfig()
                        .stream()
                        .filter(b -> b.getId().equals(newBridge.getId()))
                        .count());

        //TODO ensure the model details are correct

//        String copiedFileContent = FileUtils.readFileToString(tempFile, UTF_8);
//        System.err.println(copiedFileContent);

    }

    protected void assertISA95Equals(@NotNull final ISA95 isa95, @NotNull final ISA95Entity config) {
        Assert.assertEquals("Objects should be updated by flush", isa95.getArea(), config.getArea());
        Assert.assertEquals("Objects should be updated by flush",
                isa95.getProductionLine(),
                config.getProductionLine());
        Assert.assertEquals("Objects should be updated by flush", isa95.getSite(), config.getSite());
        Assert.assertEquals("Objects should be updated by flush", isa95.getEnterprise(), config.getEnterprise());
        Assert.assertEquals("Objects should be updated by flush", isa95.getWorkCell(), config.getWorkCell());
        Assert.assertEquals("Objects should be updated by flush",
                isa95.isPrefixAllTopics(),
                config.isPrefixAllTopics());
        Assert.assertEquals("Objects should be updated by flush", isa95.isEnabled(), config.isEnabled());
    }
}
