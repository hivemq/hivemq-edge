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
package com.hivemq.bridge.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MqttBridge {

    private final @NotNull String id;
    private final @NotNull String host;
    private final int port;
    private final @NotNull String clientId;
    private final int keepAlive;
    private final long sessionExpiry;
    private final boolean cleanStart;
    private final @Nullable String username;
    private final @Nullable String password;
    private final @Nullable BridgeTls bridgeTls;
    private final @Nullable BridgeWebsocketConfig bridgeWebsocketConfig;
    private final @NotNull List<RemoteSubscription> remoteSubscriptions;
    private final @NotNull List<LocalSubscription> localSubscriptions;
    private final boolean loopPreventionEnabled;
    private final int loopPreventionHopCount;
    private final boolean persist;

    private MqttBridge(
            final @NotNull String id,
            final @NotNull String host,
            final int port,
            final @NotNull String clientId,
            final int keepAlive,
            final long sessionExpiry,
            final boolean cleanStart,
            final @Nullable String username,
            final @Nullable String password,
            final @Nullable BridgeTls bridgeTls,
            final @Nullable BridgeWebsocketConfig bridgeWebsocketConfig,
            final @NotNull List<RemoteSubscription> remoteSubscriptions,
            final @NotNull List<LocalSubscription> localSubscriptions,
            final boolean loopPreventionEnabled,
            final int loopPreventionHopCount,
            final boolean persist) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.clientId = clientId;
        this.keepAlive = keepAlive;
        this.sessionExpiry = sessionExpiry;
        this.cleanStart = cleanStart;
        this.username = username;
        this.password = password;
        this.bridgeTls = bridgeTls;
        this.bridgeWebsocketConfig = bridgeWebsocketConfig;
        this.remoteSubscriptions = remoteSubscriptions;
        this.localSubscriptions = localSubscriptions;
        this.loopPreventionEnabled = loopPreventionEnabled;
        this.loopPreventionHopCount = loopPreventionHopCount;
        this.persist = persist;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public @NotNull String getClientId() {
        return clientId;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public long getSessionExpiry() {
        return sessionExpiry;
    }

    public boolean isCleanStart() {
        return cleanStart;
    }

    public @Nullable String getUsername() {
        return username;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public @Nullable BridgeWebsocketConfig getBridgeWebsocketConfig() {
        return bridgeWebsocketConfig;
    }

    public @Nullable BridgeTls getBridgeTls() {
        return bridgeTls;
    }

    public @NotNull List<RemoteSubscription> getRemoteSubscriptions() {
        return remoteSubscriptions;
    }

    public @NotNull List<LocalSubscription> getLocalSubscriptions() {
        return localSubscriptions;
    }

    public boolean isLoopPreventionEnabled() {
        return loopPreventionEnabled;
    }

    public int getLoopPreventionHopCount() {
        return loopPreventionHopCount;
    }

    public boolean isPersist() {
        return persist;
    }

    public static class Builder {
        private @Nullable String id;
        private @Nullable String host;
        private int port;
        private @Nullable String clientId;
        private int keepAlive;
        private long sessionExpiry;
        private boolean cleanStart;
        private @Nullable String username = null;
        private @Nullable String password = null;
        private @Nullable BridgeTls bridgeTls = null;
        private @Nullable BridgeWebsocketConfig bridgeWebsocketConfig = null;
        private @NotNull List<RemoteSubscription> remoteSubscriptions = List.of();
        private @NotNull List<LocalSubscription> localSubscriptions = List.of();
        private boolean loopPreventionEnabled = true;
        private int loopPreventionHopCount = 1;
        private boolean persist = true;

        public @NotNull Builder withId(@NotNull final String id) {
            this.id = id;
            return this;
        }

        public @NotNull Builder withHost(@NotNull final String host) {
            this.host = host;
            return this;
        }

        public @NotNull Builder withPort(final int port) {
            this.port = port;
            return this;
        }

        public @NotNull Builder withClientId(@NotNull final String clientId) {
            this.clientId = clientId;
            return this;
        }

        public @NotNull Builder withKeepAlive(final int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public @NotNull Builder withSessionExpiry(final long sessionExpiry) {
            this.sessionExpiry = sessionExpiry;
            return this;
        }

        public @NotNull Builder withCleanStart(final boolean cleanStart) {
            this.cleanStart = cleanStart;
            return this;
        }

        public @NotNull Builder withUsername(@Nullable final String username) {
            this.username = username;
            return this;
        }

        public @NotNull Builder withPassword(@Nullable final String password) {
            this.password = password;
            return this;
        }

        public @NotNull Builder withBridgeTls(final @Nullable BridgeTls bridgeTls) {
            this.bridgeTls = bridgeTls;
            return this;
        }

        public @NotNull Builder withWebsocketConfiguration(final @Nullable BridgeWebsocketConfig bridgeWebsocketConfig) {
            this.bridgeWebsocketConfig = bridgeWebsocketConfig;
            return this;
        }

        public @NotNull Builder withRemoteSubscriptions(final @NotNull List<RemoteSubscription> remoteSubscriptions) {
            this.remoteSubscriptions = remoteSubscriptions;
            return this;
        }

        public @NotNull Builder withLocalSubscriptions(final @NotNull List<LocalSubscription> localSubscriptions) {
            this.localSubscriptions = localSubscriptions;
            return this;
        }

        public @NotNull Builder withLoopPreventionEnabled(final boolean loopPreventionEnabled) {
            this.loopPreventionEnabled = loopPreventionEnabled;
            return this;
        }

        public @NotNull Builder withLoopPreventionHopCount(final int loopPreventionHopCount) {
            this.loopPreventionHopCount = loopPreventionHopCount;
            return this;
        }

        public @NotNull Builder persist(final boolean persist) {
            this.persist = persist;
            return this;
        }

        public @NotNull MqttBridge build() {
            return new MqttBridge(Objects.requireNonNull(id),
                    Objects.requireNonNull(host),
                    port,
                    Objects.requireNonNull(clientId),
                    keepAlive,
                    sessionExpiry,
                    cleanStart,
                    username,
                    password,
                    bridgeTls,
                    bridgeWebsocketConfig,
                    remoteSubscriptions,
                    localSubscriptions,
                    loopPreventionEnabled,
                    loopPreventionHopCount,
                    persist);
        }
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MqttBridge)) {
            return false;
        }

        final MqttBridge that = (MqttBridge) o;

        if (port != that.port) {
            return false;
        }
        if (keepAlive != that.keepAlive) {
            return false;
        }
        if (sessionExpiry != that.sessionExpiry) {
            return false;
        }
        if (cleanStart != that.cleanStart) {
            return false;
        }
        if (loopPreventionEnabled != that.loopPreventionEnabled) {
            return false;
        }
        if (loopPreventionHopCount != that.loopPreventionHopCount) {
            return false;
        }
        if (!id.equals(that.id)) {
            return false;
        }
        if (!host.equals(that.host)) {
            return false;
        }
        if (!clientId.equals(that.clientId)) {
            return false;
        }
        if (!Objects.equals(username, that.username)) {
            return false;
        }
        if (!Objects.equals(password, that.password)) {
            return false;
        }
        if (!Objects.equals(bridgeTls, that.bridgeTls)) {
            return false;
        }
        if (!Objects.equals(bridgeWebsocketConfig, that.bridgeWebsocketConfig)) {
            return false;
        }
        if (!remoteSubscriptions.equals(that.remoteSubscriptions)) {
            return false;
        }
        return localSubscriptions.equals(that.localSubscriptions);
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + host.hashCode();
        result = 31 * result + port;
        result = 31 * result + clientId.hashCode();
        result = 31 * result + keepAlive;
        result = 31 * result + Long.hashCode(sessionExpiry);
        result = 31 * result + Boolean.hashCode(cleanStart);
        result = 31 * result + Objects.hashCode(username);
        result = 31 * result + Objects.hashCode(password);
        result = 31 * result + Objects.hashCode(bridgeTls);
        result = 31 * result + Objects.hashCode(bridgeWebsocketConfig);
        result = 31 * result + remoteSubscriptions.hashCode();
        result = 31 * result + localSubscriptions.hashCode();
        result = 31 * result + Boolean.hashCode(loopPreventionEnabled);
        result = 31 * result + loopPreventionHopCount;
        result = 31 * result + Boolean.hashCode(persist);
        return result;
    }

    @Override
    public @NotNull String toString() {
        final String sb = "MqttBridge{" +
                "id='" +
                id +
                '\'' +
                ", host='" +
                host +
                '\'' +
                ", port=" +
                port +
                ", clientId='" +
                clientId +
                '\'' +
                ", keepAlive=" +
                keepAlive +
                ", sessionExpiry=" +
                sessionExpiry +
                ", cleanStart=" +
                cleanStart +
                ", username='" +
                username +
                '\'' +
                ", password='" +
                password +
                '\'' +
                ", bridgeTls=" +
                bridgeTls +
                ", remoteSubscriptions=" +
                remoteSubscriptions +
                ", localSubscriptions=" +
                localSubscriptions +
                ", loopPreventionEnabled=" +
                loopPreventionEnabled +
                ", loopPreventionHopCount=" +
                loopPreventionHopCount +
                '}';
        return sb;
    }
}
