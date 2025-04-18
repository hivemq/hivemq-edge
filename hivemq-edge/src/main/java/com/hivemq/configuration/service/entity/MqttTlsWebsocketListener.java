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

import com.google.common.collect.ImmutableList;
import com.hivemq.extension.sdk.api.annotations.Immutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A listener which allows to listen to MQTT traffic over secure websockets with TLS.
 * <p>
 * Use the builder if you want to create a new TLS websocket listener.
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @since 3.0
 */
@Immutable
public class MqttTlsWebsocketListener extends MqttWebsocketListener implements MqttTlsListener {

    private final @NotNull Tls tls;

    private MqttTlsWebsocketListener(
            final int port,
            final @NotNull String bindAddress,
            final @NotNull String path,
            final @NotNull Boolean allowExtensions,
            final @NotNull List<String> subprotocols,
            final @NotNull Tls tls,
            final @NotNull String name,
            final @Nullable String externalHostname) {
        super(port, bindAddress, path, allowExtensions, subprotocols, name, externalHostname);
        this.tls = tls;
    }

    public @NotNull Tls getTls() {
        return tls;
    }

    @Override
    public @NotNull String getReadableName() {
        return "MQTT Websocket Listener with TLS";
    }

    /**
     * A builder which allows to conveniently build a listener object with a fluent API
     */
    public static class Builder {

        protected @NotNull String path;
        protected @NotNull List<String> subprotocols;

        protected @Nullable String name;
        protected @Nullable Integer port;
        protected @Nullable String bindAddress;
        protected boolean allowExtensions;

        private @Nullable Tls tls;
        private @Nullable String externalHostname;

        public Builder() {
            path = "";
            subprotocols = new ArrayList<>();
            subprotocols.add("mqtt"); //Add default subprotocol which is required by the MQTT spec
            allowExtensions = false;
        }

        public @NotNull Builder from(final @NotNull MqttTlsWebsocketListener mqttTlsWebsocketListener) {
            port = mqttTlsWebsocketListener.getPort();
            bindAddress = mqttTlsWebsocketListener.getBindAddress();
            path = mqttTlsWebsocketListener.getPath();
            name = mqttTlsWebsocketListener.getName();
            allowExtensions = mqttTlsWebsocketListener.getAllowExtensions();
            subprotocols = new ArrayList<>(mqttTlsWebsocketListener.getSubprotocols());
            tls = mqttTlsWebsocketListener.getTls();
            return this;
        }

        /**
         * Sets the TLS configuration of the TLS Websocket listener
         *
         * @param tls the TLS configuration
         * @return the Builder
         */
        public @NotNull Builder tls(final @NotNull Tls tls) {
            checkNotNull(tls);
            this.tls = tls;
            return this;
        }

        /**
         * Sets the port of the TLS websocket listener
         *
         * @param port the port
         * @return the Builder
         */
        public @NotNull Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the bind address of the TLS websocket listener
         *
         * @param bindAddress the bind address
         * @return the Builder
         */
        public @NotNull Builder bindAddress(final @NotNull String bindAddress) {
            checkNotNull(bindAddress);
            this.bindAddress = bindAddress;
            return this;
        }

        /**
         * Sets the websocket path of the TLS websocket listener
         *
         * @param path the path
         * @return the Builder
         */
        public @NotNull Builder path(final @NotNull String path) {
            checkNotNull(path);
            this.path = path;
            return this;
        }

        /**
         * Sets the name of the websocket listener
         *
         * @param name the name
         * @return the Builder
         */
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
        public @NotNull Builder externalHostname(final @Nullable String externalHostname) {
            this.externalHostname = externalHostname;
            return this;
        }

        /**
         * Sets if websocket extensions should be allowed or not
         *
         * @param allowExtensions if websocket extensions should be allowed or not
         * @return the Builder
         */
        public @NotNull Builder allowExtensions(final boolean allowExtensions) {
            this.allowExtensions = allowExtensions;
            return this;
        }

        /**
         * Sets a list of subprotocols the websocket listener should support.
         * <p>
         * Typically you should use 'mqtt' and/or 'mqttv3.1
         *
         * @param subprotocols a list of websocket subprotocols
         * @return the Builder
         */
        public @NotNull Builder subprotocols(final @NotNull List<String> subprotocols) {
            checkNotNull(subprotocols);
            this.subprotocols = ImmutableList.copyOf(subprotocols);
            return this;
        }

        /**
         * Creates the TLS Websocket Listener
         *
         * @return the TLS Websocket Listener
         */
        public @NotNull MqttTlsWebsocketListener build() {
            if (port == null) {
                throw new IllegalStateException("The port for a TLS Websocket listener was not set.");
            }
            if (bindAddress == null) {
                throw new IllegalStateException("The bind address for a TLS Websocket listener was not set.");
            }
            if (name == null) {
                name = "tls-websocket-listener-" + port;
            }
            if (tls == null) {
                throw new IllegalStateException("The TLS settings for a TLS Websocket listener was not set.");
            }
            return new MqttTlsWebsocketListener(port, bindAddress, path, allowExtensions, subprotocols, tls, name, externalHostname);
        }
    }
}
