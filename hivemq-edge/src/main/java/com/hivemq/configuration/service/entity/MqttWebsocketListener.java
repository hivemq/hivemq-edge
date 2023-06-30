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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A listener which allows to listen to MQTT traffic over websockets.
 * <p>
 * Use the builder if you want to create a new websocket listener.
 *
 * @author Dominik Obermaier
 * @author Christoph Schaebel
 * @since 3.0
 */
@Immutable
public class MqttWebsocketListener implements Listener {

    private int port;
    private final @NotNull String bindAddress;
    private final @NotNull String path;
    private final boolean allowExtensions;
    private final @NotNull List<String> subprotocols;
    private final @NotNull String name;
    private final @Nullable String externalHostname;

    protected MqttWebsocketListener(
            final int port,
            final @NotNull String bindAddress,
            final @NotNull String path,
            final boolean allowExtensions,
            final @NotNull List<String> subprotocols,
            final @NotNull String name,
            final @Nullable String externalHostname) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.path = path;
        this.allowExtensions = allowExtensions;
        this.subprotocols = subprotocols;
        this.name = name;
        this.externalHostname = externalHostname;
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
        return "MQTT Websocket Listener";
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    @Override
    public @Nullable String getExternalHostname() {
        return externalHostname;
    }

    /**
     * @return the path of the websocket
     */
    public @NotNull String getPath() {
        return path;
    }

    /**
     * @return if websocket extensions are allowed or not
     */
    public boolean getAllowExtensions() {
        return allowExtensions;
    }

    /**
     * @return a list of all supported subprotocols
     */
    public @NotNull List<String> getSubprotocols() {
        return subprotocols;
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
        protected @Nullable String externalHostname;

        public Builder() {
            path = "";
            subprotocols = new ArrayList<>();
            subprotocols.add("mqtt"); //Add default subprotocol which is required by the MQTT spec
            allowExtensions = false;
        }

        public @NotNull Builder from(final @NotNull MqttWebsocketListener mqttWebsocketListener) {
            port = mqttWebsocketListener.getPort();
            bindAddress = mqttWebsocketListener.getBindAddress();
            path = mqttWebsocketListener.getPath();
            name = mqttWebsocketListener.getName();
            allowExtensions = mqttWebsocketListener.getAllowExtensions();
            subprotocols = new ArrayList<>(mqttWebsocketListener.getSubprotocols());
            return this;
        }

        /**
         * Sets the port of the websocket listener
         *
         * @param port the port
         * @return the Builder
         */
        public @NotNull Builder port(final int port) {
            this.port = port;
            return this;
        }

        /**
         * Sets the bind address of the websocket listener
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
         * Sets the websocket path of the websocket listener
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
         * Creates the Websocket Listener
         *
         * @return the Websocket Listener
         */
        public @NotNull MqttWebsocketListener build() throws IllegalStateException {
            if (port == null) {
                throw new IllegalStateException("The port for a Websocket listener was not set.");
            }
            if (bindAddress == null) {
                throw new IllegalStateException("The bind address for a Websocket listener was not set.");
            }
            if (name == null) {
                name = "websocket-listener-" + port;
            }
            return new MqttWebsocketListener(port,
                    bindAddress,
                    path,
                    allowExtensions,
                    subprotocols,
                    name,
                    externalHostname);
        }
    }
}
