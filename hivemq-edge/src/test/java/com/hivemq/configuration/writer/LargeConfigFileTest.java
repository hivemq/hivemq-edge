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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.configuration.entity.HiveMQConfigEntity;
import com.hivemq.configuration.reader.ConfigFileReaderWriter;
import com.hivemq.configuration.reader.ConfigurationFile;
import com.hivemq.edge.modules.adapters.annotations.ModuleConfigField;
import com.hivemq.edge.modules.adapters.simulation.SimulationAdapterConfig;
import com.hivemq.edge.modules.adapters.simulation.SimulationConfigConverter;
import com.hivemq.edge.modules.config.impl.AbstractProtocolAdapterConfig;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("NullabilityAnnotations")
public class LargeConfigFileTest extends AbstractConfigWriterTest {

    protected String getTestConfigName(){
        return "large-config.xml";
    }

    @Test
    @Ignore
    @Disabled
    public void readSmallWriteLarge_reread_config() throws IOException {

        File tempFile = loadTestConfigFile();
        ObjectMapper mapper = new ObjectMapper();

        long start = System.currentTimeMillis();

        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
        HiveMQConfigEntity entity = configFileReader.applyConfig();

        start = printTimer("Initial Read", System.out, start);

        SimulationAdapterConfig config = readConfig(mapper, entity.getProtocolAdapterConfig());
        List<AbstractProtocolAdapterConfig.Subscription> subscriptions  = config.getSubscriptions();

        for (int i = 0; i < 100000; i++){
            subscriptions.add(new AbstractProtocolAdapterConfig.Subscription("foo" + i, 1));
        }

        start = printTimer("Population", System.out, start);

        entity.getProtocolAdapterConfig().put("simulation", SimulationConfigConverter.unconvertConfig(mapper, config));

        start = printTimer("Convert Config", System.out, start);

        final File tempCopyFile = new File(System.getProperty("java.io.tmpdir"), "copy-config.xml");
        tempFile.deleteOnExit();
        configFileReader.writeConfig(new ConfigurationFile(tempCopyFile), false);

        start = printTimer("Write Config", System.out, start);

        ConfigFileReaderWriter largeRW = createFileReaderWriter(tempCopyFile);

        start = printTimer("Create RW", System.out, start);

        HiveMQConfigEntity newConfig = largeRW.applyConfig();

        start = printTimer("Apply Config", System.out, start);

        config = readConfig(mapper, newConfig.getProtocolAdapterConfig());

        start = printTimer("Read Adapters", System.out, start);
        subscriptions  = config.getSubscriptions();
        Assert.assertTrue("List should contain 100001 subs", subscriptions.size() == 100001);
    }

//    @Test
//    public void createOpcSubscriptions() throws IOException {
//
//        File tempFile = loadTestConfigFile();
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//        long start = System.currentTimeMillis();
//
//        final ConfigFileReaderWriter configFileReader = createFileReaderWriter(tempFile);
//        HiveMQConfigEntity entity = configFileReader.applyConfig();
//
//        start = printTimer("Initial Read", System.out, start);
//
//        OPCConfig config = mapper.convertValue(entity.getProtocolAdapterConfig().get("opc-ua-client"), OPCConfig.class);
//
//        for (int i = 0; i < 1000; i++){
//            config.getSubscriptions().add(new OPCUASubscription());
//        }
//
//        start = printTimer("Population", System.out, start);
//
//        entity.getProtocolAdapterConfig().put("opc-ua-client", mapper.convertValue(config, Map.class));
//
//        start = printTimer("Convert Config", System.out, start);
//
//        final File tempCopyFile = new File(System.getProperty("java.io.tmpdir"), "copy-config.xml");
//        tempFile.deleteOnExit();
//        configFileReader.writeConfig(new ConfigurationFile(tempCopyFile), false);
//
//        start = printTimer("Write Config", System.out, start);
//    }

    protected SimulationAdapterConfig readConfig(final ObjectMapper mapper, final  Map<String, Object> protocolAdapterConfig){
        Map o = (Map) protocolAdapterConfig.get("simulation");
        SimulationAdapterConfig config = SimulationConfigConverter.convertConfig(mapper, o);
        return config;
    }

    protected static long printTimer(String name, PrintStream printStream,  long since){
        printStream.println(name + " Timer: " + (System.currentTimeMillis() - since) + "ms");
        return System.currentTimeMillis();
    }

    @JsonIgnoreProperties
    static class OPCConfig {

        @JsonProperty("id")
        @ModuleConfigField(title = "id")
        private @NotNull String id = "opc-ua-client-id";

        @JsonProperty("subscriptions")
        @ModuleConfigField(title = "Subscriptions",
                           description = "List of subscriptions for the simulation",
                           required = true)
        private @NotNull List<OPCUASubscription> subscriptions = new ArrayList<>();

        public List<OPCUASubscription> getSubscriptions(){
            if(subscriptions == null){
                subscriptions = new ArrayList<>();
            }
            return subscriptions;
        }
    }

    static class OPCUASubscription {
        @JsonProperty("node")
        @ModuleConfigField(title = "Source Node ID",
                           description = "identifier of the node on the OPC-UA server. Example: \"ns=3;s=85/0:Temperature\"",
                           required = true)
        private @NotNull String node = "ns=3;s=85/0:Temperature";

        @JsonProperty("mqtt-topic")
        @ModuleConfigField(title = "Destination MQTT topic",
                           description = "The MQTT topic to publish to",
                           format = ModuleConfigField.FieldType.MQTT_TOPIC,
                           required = true)
        private @NotNull String mqttTopic = "mqtt-topic";

        @JsonProperty("publishing-interval")
        @ModuleConfigField(title = "OPC UA publishing interval [ms]",
                           description = "OPC UA publishing interval in milliseconds for this subscription on the server",
                           numberMin = 1,
                           defaultValue = "1000")
        private int publishingInterval = 1; //1 second

        @JsonProperty("server-queue-size")
        @ModuleConfigField(title = "OPC UA server queue size",
                           description = "OPC UA queue size for this subscription on the server",
                           numberMin = 1,
                           defaultValue = "1")
        private int serverQueueSize = 1;

        @JsonProperty("qos")
        @ModuleConfigField(title = "MQTT QoS",
                           description = "MQTT quality of service level",
                           numberMin = 0,
                           numberMax = 2,
                           defaultValue = "0")
        private int qos = 0;

        @JsonProperty("message-expiry-interval")
        @ModuleConfigField(title = "MQTT message expiry interval [s]",
                           description = "Time in seconds until a MQTT message expires",
                           numberMin = 1,
                           numberMax = 4294967295L)
        private @Nullable Integer messageExpiryInterval = 1;

        public OPCUASubscription() {
        }

        public OPCUASubscription(
                final String node,
                final String mqttTopic,
                final int publishingInterval,
                final int serverQueueSize,
                final int qos,
                final Integer messageExpiryInterval) {
            this.node = node;
            this.mqttTopic = mqttTopic;
            this.publishingInterval = publishingInterval;
            this.serverQueueSize = serverQueueSize;
            this.qos = qos;
            this.messageExpiryInterval = messageExpiryInterval;
        }
    }
}
