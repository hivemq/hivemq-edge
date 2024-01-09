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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class MqttBridge {

    private final @NotNull String id;
    private final @NotNull String host;
    private final int port;
    private final @NotNull String clientId;
    private final int keepAlive;
    private final int sessionExpiry;
    private final boolean cleanStart;
    private final @Nullable String username;
    private final @Nullable String password;
    private final @Nullable BridgeTls bridgeTls;
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
            final int sessionExpiry,
            final boolean cleanStart,
            final @Nullable String username,
            final @Nullable String password,
            final @Nullable BridgeTls bridgeTls,
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

    public int getSessionExpiry() {
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
        private int sessionExpiry;
        private boolean cleanStart;
        private @Nullable String username = null;
        private @Nullable String password = null;
        private @Nullable BridgeTls bridgeTls = null;
        private @NotNull List<RemoteSubscription> remoteSubscriptions = List.of();
        private @NotNull List<LocalSubscription> localSubscriptions = List.of();
        private boolean loopPreventionEnabled = true;
        private int loopPreventionHopCount = 1;
        private boolean persist = true;

        public @NotNull Builder withId(@NotNull String id) {
            this.id = id;
            return this;
        }

        public @NotNull Builder withHost(@NotNull String host) {
            this.host = host;
            return this;
        }

        public @NotNull Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public @NotNull Builder withClientId(@NotNull String clientId) {
            this.clientId = clientId;
            return this;
        }

        public @NotNull Builder withKeepAlive(int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public @NotNull Builder withSessionExpiry(int sessionExpiry) {
            this.sessionExpiry = sessionExpiry;
            return this;
        }

        public @NotNull Builder withCleanStart(boolean cleanStart) {
            this.cleanStart = cleanStart;
            return this;
        }

        public @NotNull Builder withUsername(@Nullable String username) {
            this.username = username;
            return this;
        }

        public @NotNull Builder withPassword(@Nullable String password) {
            this.password = password;
            return this;
        }

        public @NotNull Builder withBridgeTls(final @NotNull BridgeTls bridgeTls) {
            this.bridgeTls = bridgeTls;
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

        public @NotNull Builder withLoopPreventionEnabled(boolean loopPreventionEnabled) {
            this.loopPreventionEnabled = loopPreventionEnabled;
            return this;
        }

        public @NotNull Builder withLoopPreventionHopCount(int loopPreventionHopCount) {
            this.loopPreventionHopCount = loopPreventionHopCount;
            return this;
        }

        public @NotNull Builder persist(boolean persist) {
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

        MqttBridge that = (MqttBridge) o;

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
        result = 31 * result + sessionExpiry;
        result = 31 * result + (cleanStart ? 1 : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (bridgeTls != null ? bridgeTls.hashCode() : 0);
        result = 31 * result + remoteSubscriptions.hashCode();
        result = 31 * result + localSubscriptions.hashCode();
        result = 31 * result + (loopPreventionEnabled ? 1 : 0);
        result = 31 * result + loopPreventionHopCount;
        return result;
    }

    @Override
    public @NotNull String toString() {
        final StringBuilder sb = new StringBuilder("MqttBridge{");
        sb.append("id='").append(id).append('\'');
        sb.append(", host='").append(host).append('\'');
        sb.append(", port=").append(port);
        sb.append(", clientId='").append(clientId).append('\'');
        sb.append(", keepAlive=").append(keepAlive);
        sb.append(", sessionExpiry=").append(sessionExpiry);
        sb.append(", cleanStart=").append(cleanStart);
        sb.append(", username='").append(username).append('\'');
        sb.append(", password='").append(password).append('\'');
        sb.append(", bridgeTls=").append(bridgeTls);
        sb.append(", remoteSubscriptions=").append(remoteSubscriptions);
        sb.append(", localSubscriptions=").append(localSubscriptions);
        sb.append(", loopPreventionEnabled=").append(loopPreventionEnabled);
        sb.append(", loopPreventionHopCount=").append(loopPreventionHopCount);
        sb.append('}');
        return sb.toString();
    }
}
