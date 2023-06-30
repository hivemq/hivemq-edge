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
 * A listener which allows to listen to MQTT traffic via TLS
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @since 3.0
 */
@Immutable
public class MqttTlsTcpListener extends MqttTcpListener implements MqttTlsListener {

    private final @NotNull Tls tls;

    /**
     * Creates a new TLS Listener which listens to a specific port and bind address
     *
     * @param port        the port
     * @param bindAddress the bind address
     * @param tls         the TLS configuration
     */
    @Deprecated
    public MqttTlsTcpListener(final int port, final @NotNull String bindAddress, final @NotNull Tls tls) {
        this(port, bindAddress, tls, "tls-tcp-listener-" + port, null);
    }

    /**
     * Creates a new TLS Listener which listens to a specific port and bind address.
     *
     * @param port        the port
     * @param bindAddress the bind address
     * @param tls         the TLS configuration
     */
    public MqttTlsTcpListener(
            final int port, final @NotNull String bindAddress, final @NotNull Tls tls, final @NotNull String name, final @Nullable String externalHostname) {
        super(port, bindAddress, name, externalHostname);
        checkNotNull(tls);
        this.tls = tls;
    }

    @Override
    public @NotNull Tls getTls() {
        return tls;
    }

    @Override
    public @NotNull String getReadableName() {
        return "MQTT TCP Listener with TLS";
    }

    public static class Builder {

        private @Nullable String name;
        private @Nullable Integer port;
        private @Nullable String bindAddress;
        private @Nullable Tls tls;
        private @Nullable String externalHostname;

        public @NotNull Builder from(final @NotNull MqttTlsTcpListener mqttTlsTcpListener) {
            port = mqttTlsTcpListener.getPort();
            bindAddress = mqttTlsTcpListener.getBindAddress();
            name = mqttTlsTcpListener.getName();
            tls = mqttTlsTcpListener.getTls();
            return this;
        }

        public @NotNull Builder tls(final @NotNull Tls tls) {
            checkNotNull(tls);
            this.tls = tls;
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

        public @NotNull Builder externalHostname(final @NotNull String externalHostname) {
            this.externalHostname = externalHostname;
            return this;
        }

        public @NotNull MqttTlsTcpListener build() throws IllegalStateException {
            if (port == null) {
                throw new IllegalStateException("The port for a TLS TCP listener was not set.");
            }
            if (bindAddress == null) {
                throw new IllegalStateException("The bind address for a TLS TCP listener was not set.");
            }
            if (name == null) {
                name = "tls-tcp-listener-" + port;
            }
            if (tls == null) {
                throw new IllegalStateException("The TLS settings for a TLS TCP listener was not set.");
            }
            return new MqttTlsTcpListener(port, bindAddress, tls, name, externalHostname);
        }
    }
}
