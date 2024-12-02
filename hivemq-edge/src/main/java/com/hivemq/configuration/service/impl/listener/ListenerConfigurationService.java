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

import com.hivemq.annotations.ReadOnly;
import com.hivemq.configuration.service.entity.Listener;
import com.hivemq.configuration.service.entity.MqttTcpListener;
import com.hivemq.configuration.service.entity.MqttTlsTcpListener;
import com.hivemq.configuration.service.entity.MqttTlsWebsocketListener;
import com.hivemq.configuration.service.entity.MqttsnUdpListener;
import com.hivemq.configuration.service.entity.MqttWebsocketListener;
import com.hivemq.configuration.service.exception.ConfigurationValidationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ListenerConfigurationService {

    /**
     * Adds a new Listener at runtime.
     *
     * @param listener the listener
     * @param <T>      the concrete listener subclass
     * @throws ConfigurationValidationException if the validation of the listener wasn't successful
     * @throws IllegalArgumentException         when the listener has not a known type.
     */
    <T extends Listener> void addListener(final @NotNull T listener)
            throws ConfigurationValidationException, IllegalArgumentException;

    /**
     * @return a unmodifiable list of all active listeners
     */
    @ReadOnly
    @NotNull List<Listener> getListeners();

    /**
     * @return a unmodifiable list of all active TCP listeners
     */
    @ReadOnly
    @NotNull List<MqttTcpListener> getTcpListeners();

    /**
     * @return a unmodifiable list of all active TLS listeners
     */
    @ReadOnly
    @NotNull List<MqttTlsTcpListener> getTlsTcpListeners();

    /**
     * @return a unmodifiable list of all active Websocket listeners
     */
    @ReadOnly
    @NotNull List<MqttWebsocketListener> getWebsocketListeners();

    /**
     * @return a unmodifiable list of all active TLS Websocket listeners
     */
    @ReadOnly
    @NotNull List<MqttTlsWebsocketListener> getTlsWebsocketListeners();

    /**
     * @return a unmodifiable list of all active UDP listeners
     */
    @ReadOnly
    @NotNull List<MqttsnUdpListener> getUdpListeners();

}
