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
import com.hivemq.bridge.config.*;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class BridgeConfiguratorTest extends AbstractConfigurationTest {

    @Test
    public void whenMinimalConfigRemoteSubs_thenDefaultsSet() throws IOException {
        final String contents = "" +
                "<hivemq>\n" +
                "<mqtt-bridges>\n" +
                "    <mqtt-bridge>\n" +
                "        <id>test-bridge</id>\n" +
                "        <remote-broker>\n" +
                "            <host>testhost</host>\n" +
                "        </remote-broker>\n" +
                "        <remote-subscriptions>\n" +
                "            <remote-subscription>\n" +
                "                <filters>\n" +
                "                    <mqtt-topic-filter>machine-types/1/#</mqtt-topic-filter>\n" +
                "                </filters>\n" +
                "            </remote-subscription>\n" +
                "        </remote-subscriptions>\n" +
                "    </mqtt-bridge>\n" +
                "</mqtt-bridges>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final List<MqttBridge> bridges = bridgeConfigurationService.getBridges();

        assertEquals(1, bridges.size());
        final MqttBridge mqttBridge = bridges.get(0);
        assertEquals("testhost", mqttBridge.getHost());
        assertEquals(1883, mqttBridge.getPort());
        assertEquals(60, mqttBridge.getKeepAlive());
        assertEquals(3600, mqttBridge.getSessionExpiry());
        assertNull(mqttBridge.getBridgeTls());
        assertEquals("test-bridge", mqttBridge.getClientId());
        assertEquals("test-bridge", mqttBridge.getId());
        assertEquals(1, mqttBridge.getRemoteSubscriptions().size());
        final RemoteSubscription remoteSubscription = mqttBridge.getRemoteSubscriptions().get(0);
        assertEquals(1, remoteSubscription.getFilters().size());
        assertEquals("machine-types/1/#", remoteSubscription.getFilters().get(0));

    }

    @Test
    public void whenMinimalConfigWithForwarded_thenDefaultsSet() throws IOException {
        final String contents = "" +
                "<hivemq>\n" +
                "<mqtt-bridges>\n" +
                "    <mqtt-bridge>\n" +
                "        <id>test-bridge</id>\n" +
                "        <remote-broker>\n" +
                "            <host>testhost</host>\n" +
                "        </remote-broker>\n" +
                "        <forwarded-topics>\n" +
                "            <forwarded-topic>\n" +
                "                <filters>\n" +
                "                    <mqtt-topic-filter>machine-types/1/#</mqtt-topic-filter>\n" +
                "                </filters>\n" +
                "            </forwarded-topic>\n" +
                "        </forwarded-topics>\n" +
                "    </mqtt-bridge>\n" +
                "</mqtt-bridges>" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final List<MqttBridge> bridges = bridgeConfigurationService.getBridges();

        assertEquals(1, bridges.size());
        final MqttBridge mqttBridge = bridges.get(0);
        assertEquals("testhost", mqttBridge.getHost());
        assertEquals(1883, mqttBridge.getPort());
        assertEquals(60, mqttBridge.getKeepAlive());
        assertEquals(3600, mqttBridge.getSessionExpiry());
        assertNull(mqttBridge.getBridgeTls());
        assertEquals("test-bridge", mqttBridge.getClientId());
        assertEquals("test-bridge", mqttBridge.getId());
        assertEquals(1, mqttBridge.getLocalSubscriptions().size());
        final LocalSubscription remoteSubscription = mqttBridge.getLocalSubscriptions().get(0);
        assertEquals(1, remoteSubscription.getFilters().size());
        assertEquals("machine-types/1/#", remoteSubscription.getFilters().get(0));
    }

    @Test
    public void whenFullConfig_thenNoDefaultsSet() throws IOException {
        final String contents = "<hivemq>\n" +
                "    <mqtt-bridges>\n" +
                "        <mqtt-bridge>\n" +
                "            <id>my-bridge-1</id>\n" +
                "            <remote-broker>\n" +
                "                <host>localhost</host>\n" +
                "                <port>8000</port>\n" +
                "                <mqtt>\n" +
                "                    <client-id>my-test-bridge</client-id>\n" +
                "                    <clean-start>true</clean-start>\n" +
                "                    <session-expiry>100</session-expiry>\n" +
                "                    <keep-alive>10</keep-alive>\n" +
                "                </mqtt>\n" +
                "                <authentication>\n" +
                "                    <mqtt-simple-authentication>\n" +
                "                        <username>a-username</username>\n" +
                "                        <password>a-user-password</password>\n" +
                "                    </mqtt-simple-authentication>\n" +
                "                </authentication>\n" +
                "                <tls>\n" +
                "                    <enabled>true</enabled>\n" +
                "                    <keystore>\n" +
                "                        <path>path/to/keystore.jks</path>\n" +
                "                        <password>keystorepw</password>\n" +
                "                        <private-key-password>keypw</private-key-password>\n" +
                "                    </keystore>\n" +
                "                    <truststore>\n" +
                "                        <path>path/to/truststore.jks</path>\n" +
                "                        <password>trustpw</password>\n" +
                "                    </truststore>\n" +
                "                    <cipher-suites>\n" +
                "                        <cipher-suite>TLS_RSA_WITH_AES_128_CBC_SHA</cipher-suite>\n" +
                "                        <cipher-suite>TLS_RSA_WITH_AES_256_CBC_SHA</cipher-suite>\n" +
                "                    </cipher-suites>\n" +
                "                    <protocols>\n" +
                "                        <protocol>TLSv1.2</protocol>\n" +
                "                        <protocol>TLSv1.1</protocol>\n" +
                "                    </protocols>\n" +
                "                    <handshake-timeout>30</handshake-timeout>\n" +
                "                    <verify-hostname>false</verify-hostname>\n" +
                "                </tls>\n" +
                "            </remote-broker>\n" +
                "            <remote-subscriptions>\n" +
                "                <remote-subscription>\n" +
                "                    <filters>\n" +
                "                        <mqtt-topic-filter>machine-types/1/#</mqtt-topic-filter>\n" +
                "                        <mqtt-topic-filter>broadcasts/#</mqtt-topic-filter>\n" +
                "                    </filters>\n" +
                "                    <max-qos>0</max-qos>\n" +
                "                    <preserve-retain>true</preserve-retain>\n" +
                "                    <custom-user-properties>\n" +
                "                        <user-property>\n" +
                "                            <key>key-1-1</key>\n" +
                "                            <value>value-1-1</value>\n" +
                "                        </user-property>\n" +
                "                        <user-property>\n" +
                "                            <key>key-1-2</key>\n" +
                "                            <value>value-1-2</value>\n" +
                "                        </user-property>\n" +
                "                    </custom-user-properties>\n" +
                "                    <destination>prefix/{#}/suffix</destination>\n" +
                "                </remote-subscription>\n" +
                "                <remote-subscription>\n" +
                "                    <filters>\n" +
                "                        <mqtt-topic-filter>topic3</mqtt-topic-filter>\n" +
                "                    </filters>\n" +
                "                </remote-subscription>\n" +
                "            </remote-subscriptions>\n" +
                "            <forwarded-topics>\n" +
                "                <forwarded-topic>\n" +
                "                    <filters>\n" +
                "                        <mqtt-topic-filter>topic1/#</mqtt-topic-filter>\n" +
                "                        <mqtt-topic-filter>topic2/#</mqtt-topic-filter>\n" +
                "                    </filters>\n" +
                "                    <max-qos>1</max-qos>\n" +
                "                    <preserve-retain>true</preserve-retain>\n" +
                "                    <custom-user-properties>\n" +
                "                        <user-property>\n" +
                "                            <key>key-2-1</key>\n" +
                "                            <value>value-2-1</value>\n" +
                "                        </user-property>\n" +
                "                        <user-property>\n" +
                "                            <key>key-2-2</key>\n" +
                "                            <value>value-2-2</value>\n" +
                "                        </user-property>\n" +
                "                    </custom-user-properties>\n" +
                "                    <excludes>\n" +
                "                        <mqtt-topic-filter>t1/+/t2/#</mqtt-topic-filter>\n" +
                "                        <mqtt-topic-filter>t2/t3/+/t4</mqtt-topic-filter>\n" +
                "                    </excludes>\n" +
                "                    <destination>prefix/{#}/suffix</destination>\n" +
                "                </forwarded-topic>\n" +
                "                <forwarded-topic>\n" +
                "                    <filters>\n" +
                "                        <mqtt-topic-filter>topic2</mqtt-topic-filter>\n" +
                "                    </filters>\n" +
                "                </forwarded-topic>\n" +
                "            </forwarded-topics>\n" +
                "        </mqtt-bridge>\n" +
                "        <mqtt-bridge>\n" +
                "            <id>my-bridge</id>\n" +
                "            <remote-broker>\n" +
                "                <host>localhost</host>\n" +
                "                <port>8000</port>\n" +
                "            </remote-broker>\n" +
                "            <remote-subscriptions>\n" +
                "                <remote-subscription>\n" +
                "                    <filters>\n" +
                "                        <mqtt-topic-filter>machine-types/1/#</mqtt-topic-filter>\n" +
                "                    </filters>\n" +
                "                </remote-subscription>\n" +
                "            </remote-subscriptions>\n" +
                "            <forwarded-topics>\n" +
                "                <forwarded-topic>\n" +
                "                    <filters>\n" +
                "                        <mqtt-topic-filter>#</mqtt-topic-filter>\n" +
                "                    </filters>\n" +
                "                </forwarded-topic>\n" +
                "            </forwarded-topics>\n" +
                "        </mqtt-bridge>\n" +
                "    </mqtt-bridges>\n" +
                "</hivemq>";

        Files.write(contents.getBytes(UTF_8), xmlFile);

        reader.applyConfig();

        final List<MqttBridge> bridges = bridgeConfigurationService.getBridges();

        assertEquals(2, bridges.size());
        final MqttBridge mqttBridge = bridges.get(0);
        assertEquals("my-bridge-1", mqttBridge.getId());

        //remote broker
        assertEquals("localhost", mqttBridge.getHost());
        assertEquals(8000, mqttBridge.getPort());
        assertEquals(10, mqttBridge.getKeepAlive());
        assertEquals(100, mqttBridge.getSessionExpiry());
        assertEquals("my-test-bridge", mqttBridge.getClientId());

        //TLS
        final BridgeTls bridgeTls = mqttBridge.getBridgeTls();
        assertNotNull(bridgeTls);
        assertThat(bridgeTls.getCipherSuites()).hasSize(2)
                .contains("TLS_RSA_WITH_AES_128_CBC_SHA")
                .contains("TLS_RSA_WITH_AES_256_CBC_SHA");
        assertEquals("path/to/keystore.jks", bridgeTls.getKeystorePath());
        assertEquals("keystorepw", bridgeTls.getKeystorePassword());
        assertEquals("path/to/truststore.jks", bridgeTls.getTruststorePath());
        assertEquals("keypw", bridgeTls.getPrivateKeyPassword());
        assertEquals("trustpw", bridgeTls.getTruststorePassword());
        assertThat(bridgeTls.getProtocols()).hasSize(2).containsOnly("TLSv1.1", "TLSv1.2");
        assertEquals(30, bridgeTls.getHandshakeTimeout());
        assertFalse(bridgeTls.isVerifyHostname());

        //Auth
        assertThat(mqttBridge.getUsername()).isNotNull().isEqualTo("a-username");
        assertThat(mqttBridge.getPassword()).isNotNull().isEqualTo("a-user-password");

        //Local Subs
        assertEquals(2, mqttBridge.getLocalSubscriptions().size());
        final LocalSubscription localSubscription1 = mqttBridge.getLocalSubscriptions().get(0);
        assertThat(localSubscription1.getFilters()).hasSize(2).containsOnly("topic1/#", "topic2/#");
        assertEquals(1, localSubscription1.getMaxQoS());
        assertEquals("prefix/{#}/suffix", localSubscription1.getDestination());
        assertThat(localSubscription1.getExcludes()).hasSize(2).containsOnly("t1/+/t2/#", "t2/t3/+/t4");
        assertThat(localSubscription1.getCustomUserProperties()).hasSize(2)
                .containsOnly(CustomUserProperty.of("key-2-1", "value-2-1"),
                        CustomUserProperty.of("key-2-2", "value-2-2"));
        assertTrue(localSubscription1.isPreserveRetain());

        final LocalSubscription localSubscription2 = mqttBridge.getLocalSubscriptions().get(1);
        assertThat(localSubscription2.getFilters()).hasSize(1).containsOnly("topic2");

        //Remote Subs
        assertEquals(2, mqttBridge.getRemoteSubscriptions().size());
        final RemoteSubscription remoteSubscription1 = mqttBridge.getRemoteSubscriptions().get(0);
        assertThat(remoteSubscription1.getFilters()).hasSize(2).containsOnly("machine-types/1/#", "broadcasts/#");
        assertEquals(0, remoteSubscription1.getMaxQoS());
        assertEquals("prefix/{#}/suffix", remoteSubscription1.getDestination());
        assertThat(remoteSubscription1.getCustomUserProperties()).hasSize(2)
                .containsOnly(CustomUserProperty.of("key-1-1", "value-1-1"),
                        CustomUserProperty.of("key-1-2", "value-1-2"));
        assertTrue(remoteSubscription1.isPreserveRetain());

        final RemoteSubscription remoteSubscription2 = mqttBridge.getRemoteSubscriptions().get(1);
        assertThat(remoteSubscription2.getFilters()).hasSize(1).containsOnly("topic3");
    }
}
