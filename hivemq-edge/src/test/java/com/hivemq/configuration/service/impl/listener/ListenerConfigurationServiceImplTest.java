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
package com.hivemq.configuration.service.impl.listener;

import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.entity.MqttTcpListener;
import com.hivemq.configuration.service.entity.MqttTlsTcpListener;
import com.hivemq.configuration.service.entity.MqttTlsWebsocketListener;
import com.hivemq.configuration.service.entity.MqttWebsocketListener;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static util.TlsTestUtil.createDefaultTLS;

public class ListenerConfigurationServiceImplTest {

    private ListenerConfigurationServiceImpl listenerConfigurationService;

    @Before
    public void setUp() throws Exception {
        listenerConfigurationService = new ListenerConfigurationServiceImpl();
    }

    /*
     * Adding listeners
     */
    @Test
    public void test_add_listeners() {

        final MqttTcpListener mqttTcpListener = new MqttTcpListener(1883, "localhost");
        final MqttWebsocketListener mqttWebsocketListener =
                new MqttWebsocketListener.Builder().port(1884).bindAddress("localhost").build();

        final MqttTlsTcpListener mqttTlsTcpListener = new MqttTlsTcpListener(1885, "localhost", createDefaultTLS());

        final MqttTlsWebsocketListener mqttTlsWebsocketListener = new MqttTlsWebsocketListener.Builder().port(1886)
                .bindAddress("localhost")
                .tls(createDefaultTLS())
                .build();

        listenerConfigurationService.addListener(mqttTcpListener);
        listenerConfigurationService.addListener(mqttWebsocketListener);
        listenerConfigurationService.addListener(mqttTlsTcpListener);
        listenerConfigurationService.addListener(mqttTlsWebsocketListener);

        final List<Listener> listeners = listenerConfigurationService.getListeners();

        assertEquals(4, listeners.size());

        assertEquals(1, listenerConfigurationService.getTcpListeners().size());
        assertEquals(1, listenerConfigurationService.getTlsTcpListeners().size());
        assertEquals(1, listenerConfigurationService.getWebsocketListeners().size());
        assertEquals(1, listenerConfigurationService.getTlsWebsocketListeners().size());

        assertSame(listenerConfigurationService.getTcpListeners().get(0), mqttTcpListener);
        assertSame(listenerConfigurationService.getTlsTcpListeners().get(0), mqttTlsTcpListener);
        assertSame(listenerConfigurationService.getWebsocketListeners().get(0), mqttWebsocketListener);
        assertSame(listenerConfigurationService.getTlsWebsocketListeners().get(0), mqttTlsWebsocketListener);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type() {

        listenerConfigurationService.addListener(new Listener() {
            @Override
            public int getPort() {
                return 0;
            }

            @Override
            public void setPort(final int port) {

            }

            @Override
            public String getBindAddress() {
                return null;
            }

            @Override
            public String getReadableName() {
                return null;
            }

            @Override
            public @NotNull String getName() {
                return "name";
            }

            @Override
            public @Nullable String getExternalHostname() {
                return null;
            }

        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type_subclass_of_tcplistener() {

        listenerConfigurationService.addListener(new MqttTcpListener(1883, "localhost") {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type_subclass_of_tlstcplistener() {

        listenerConfigurationService.addListener(new MqttTlsTcpListener(1883, "localhost", createDefaultTLS()) {
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_add_invalid_listener_type_subclass_of_websocketlistener() {

        final MqttWebsocketListener subclass = new MqttWebsocketListener(123, null, null, false, null, null, null) {
        };

        listenerConfigurationService.addListener(subclass);
    }

    @Test
    public void test_get_listeners_immutable() {

        listenerConfigurationService.addListener(new MqttTcpListener(1883, "localhost"));

        final List<Listener> listeners = listenerConfigurationService.getListeners();

        try {
            listeners.add(new MqttTcpListener(1884, "localhost"));
            fail();
        } catch (final Exception e) {
            //Expected
        }

        try {
            listeners.clear();
            fail();
        } catch (final Exception e) {
            //Expected
        }
    }
}
