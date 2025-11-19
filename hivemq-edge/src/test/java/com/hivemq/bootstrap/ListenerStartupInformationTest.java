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
package com.hivemq.bootstrap;

import com.hivemq.configuration.service.entity.MqttTcpListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ListenerStartupInformationTest {


    @Test
    public void test_successful_listener_startup_original_listener_null() {
    
        assertThrows(NullPointerException.class, () -> ListenerStartupInformation.successfulListenerStartup(1883, null));
    }

    @Test
    public void test_successful_listener() throws Exception {
        final MqttTcpListener listener = new MqttTcpListener(1883, "0.0.0.0");
        final ListenerStartupInformation info = ListenerStartupInformation.successfulListenerStartup(listener.getPort(), listener);

        assertTrue(info.isSuccessful());
        assertEquals(1883, info.getPort());
        assertSame(listener, info.getOriginalListener());
        assertFalse(info.getException().isPresent());
    }

    @Test
    public void test_failed_listener_startup_original_listener_null() {
    
        assertThrows(NullPointerException.class, () -> ListenerStartupInformation.failedListenerStartup(1883, null, null));
    }

    @Test
    public void test_failed_listener_no_exception() throws Exception {
        final MqttTcpListener listener = new MqttTcpListener(1883, "0.0.0.0");
        final ListenerStartupInformation info = ListenerStartupInformation.failedListenerStartup(listener.getPort(), listener, null);

        assertFalse(info.isSuccessful());
        assertEquals(1883, info.getPort());
        assertSame(listener, info.getOriginalListener());
        assertFalse(info.getException().isPresent());
    }

    @Test
    public void test_failed_listener_exception() throws Exception {
        final MqttTcpListener listener = new MqttTcpListener(1883, "0.0.0.0");
        final ListenerStartupInformation info = ListenerStartupInformation.failedListenerStartup(listener.getPort(), listener, new IllegalArgumentException("illegal"));

        assertFalse(info.isSuccessful());
        assertEquals(1883, info.getPort());
        assertSame(listener, info.getOriginalListener());
        assertTrue(info.getException().isPresent());
        assertEquals(IllegalArgumentException.class, info.getException().get().getClass());
    }

}
