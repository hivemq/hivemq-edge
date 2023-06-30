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
package com.hivemq.configuration.service.entity;

import com.hivemq.extension.sdk.api.annotations.Immutable;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A listener which allows to listen to MQTT traffic via TCP
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @author Georg Held
 * @since 3.0
 */
@Immutable
public class MqttTcpListener implements Listener {

    private int port;

    private final @NotNull String name;
    private final @NotNull String bindAddress;
    private final @Nullable String externalHostname;

    /**
     * Creates a new TCP listener which listens to a specific port and bind address
     *
     * @param port        the port
     * @param bindAddress the bind address
     */
    @Deprecated
    public MqttTcpListener(final int port, @NotNull final String bindAddress) {
        this(port, bindAddress, "tcp-listener-" + port, null);
    }

    /**
     * Creates a new TCP listener which listens to a specific port and bind address
     *
     * @param port             the port
     * @param bindAddress      the bind address
     * @param name             the name of the listener
     * @param externalHostname the external hostname of the listener
     */
    public MqttTcpListener(final int port, final @NotNull String bindAddress, final @NotNull String name,
                           final @Nullable String externalHostname) {
        this.externalHostname = externalHostname;

        checkNotNull(bindAddress, "bindAddress must not be null");
        checkNotNull(name, "name must not be null");

        this.port = port;
        this.bindAddress = bindAddress;
        this.name = name;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public void setPort(final int port) {
        this.port = port;
    }

    @Override
    public @NotNull String getBindAddress() {
        return bindAddress;
    }

    @Override
    public @NotNull String getReadableName() {
        return "MQTT TCP Listener";
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @Nullable String getExternalHostname() {
        return externalHostname;
    }

    public static class Builder {

        private @Nullable String name;
        private @Nullable Integer port;
        private @Nullable String bindAddress;
        private @Nullable String externalHostname;

        public @NotNull Builder from(final @NotNull MqttTcpListener mqttTcpListener) {
            port = mqttTcpListener.getPort();
            bindAddress = mqttTcpListener.getBindAddress();
            name = mqttTcpListener.getName();
            return this;
        }

        public @NotNull Builder port(final int port) {
            this.port = port;
            return this;
        }

        public @NotNull Builder bindAddress(final @NotNull String bindAddress) {
            checkNotNull(bindAddress);
            this.bindAddress = bindAddress;
            return this;
        }

        public @NotNull Builder name(final @NotNull String name) {
            checkNotNull(name);
            this.name = name;
            return this;
        }

        /**
         * Sets the external hostname of the websocket listener
         *
         * @param externalHostname the external hostname
         * @return the Builder
         */
        public @NotNull Builder externalHostname(final @NotNull String externalHostname) {
            this.externalHostname = externalHostname;
            return this;
        }

        public @NotNull MqttTcpListener build() throws IllegalStateException {
            if (port == null) {
                throw new IllegalStateException("The port for a TCP listener was not set.");
            }
            if (bindAddress == null) {
                throw new IllegalStateException("The bind address for a TCP listener was not set.");
            }
            if (name == null) {
                name = "tcp-listener-" + port;
            }
            return new MqttTcpListener(port, bindAddress, name, externalHostname);
        }
    }
}
