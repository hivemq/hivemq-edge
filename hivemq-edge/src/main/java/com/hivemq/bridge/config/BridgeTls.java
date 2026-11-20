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

import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConfigBridge;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BridgeTls {

    private final @Nullable String keystorePath;
    private final @NotNull String keystorePassword;
    private final @NotNull String privateKeyPassword;
    private final @Nullable String truststorePath;
    private final @NotNull String truststorePassword;
    private final @NotNull List<String> protocols;
    private final @NotNull List<String> cipherSuites;
    private final @Nullable String keystoreType;
    private final @Nullable String truststoreType;
    private final boolean verifyHostname;
    private final int handshakeTimeout;

    /// The bridge's TLS settings as the five-stage validation chain sees them, or null for a bridge
    /// built without going through it.
    ///
    /// Carried beside the fields above rather than replacing them: those are read by the REST API and
    /// the configuration write-back, which report what the operator wrote, while this is what the
    /// connection validates against. The two describe the same configuration, derived once in the
    /// configuration reader, so they cannot disagree about one bridge.
    ///
    /// Null only where a bridge was assembled directly in code rather than read from a configuration
    /// file, which is every caller that predates the chain. Such a bridge connects the way it always
    /// did; a bridge whose configuration was read carries this and validates through it.
    private final @Nullable TLSConfig effectiveTls;

    /// The `<tls>` element exactly as the operator wrote it, where this bridge was read from a
    /// configuration file; null for a bridge built in code or over the REST API.
    ///
    /// The fields above can express only the superseded settings, so a configuration write-back that
    /// rebuilt the element from them would drop every newer element (`tls-checks`, `tls-truststore`,
    /// `tls-allowlist` and the rest) from the file. Not compared in `equals`: it describes the same
    /// settings as {@link #effectiveTls}, which is.
    private final @Nullable BridgeTlsEntity configuredTls;

    public BridgeTls(
            final @Nullable String keystorePath,
            final @NotNull String keystorePassword,
            final @NotNull String privateKeyPassword,
            final @Nullable String truststorePath,
            final @NotNull String truststorePassword,
            final @NotNull List<String> protocols,
            final @NotNull List<String> cipherSuites,
            final @Nullable String keystoreType,
            final @Nullable String truststoreType,
            final boolean verifyHostname,
            final int handshakeTimeout) {
        this(
                keystorePath,
                keystorePassword,
                privateKeyPassword,
                truststorePath,
                truststorePassword,
                protocols,
                cipherSuites,
                keystoreType,
                truststoreType,
                verifyHostname,
                handshakeTimeout,
                null);
    }

    /// As above, for a bridge whose configuration was read through the validation chain.
    @SuppressWarnings("checkstyle:ParameterNumber")
    public BridgeTls(
            final @Nullable String keystorePath,
            final @NotNull String keystorePassword,
            final @NotNull String privateKeyPassword,
            final @Nullable String truststorePath,
            final @NotNull String truststorePassword,
            final @NotNull List<String> protocols,
            final @NotNull List<String> cipherSuites,
            final @Nullable String keystoreType,
            final @Nullable String truststoreType,
            final boolean verifyHostname,
            final int handshakeTimeout,
            final @Nullable TLSConfig effectiveTls) {
        this(
                keystorePath,
                keystorePassword,
                privateKeyPassword,
                truststorePath,
                truststorePassword,
                protocols,
                cipherSuites,
                keystoreType,
                truststoreType,
                verifyHostname,
                handshakeTimeout,
                effectiveTls,
                null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private BridgeTls(
            final @Nullable String keystorePath,
            final @NotNull String keystorePassword,
            final @NotNull String privateKeyPassword,
            final @Nullable String truststorePath,
            final @NotNull String truststorePassword,
            final @NotNull List<String> protocols,
            final @NotNull List<String> cipherSuites,
            final @Nullable String keystoreType,
            final @Nullable String truststoreType,
            final boolean verifyHostname,
            final int handshakeTimeout,
            final @Nullable TLSConfig effectiveTls,
            final @Nullable BridgeTlsEntity configuredTls) {
        this.effectiveTls = effectiveTls;
        this.configuredTls = configuredTls;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        this.privateKeyPassword = privateKeyPassword;
        this.truststorePath = truststorePath;
        this.truststorePassword = truststorePassword;
        this.protocols = protocols;
        this.cipherSuites = cipherSuites;
        this.keystoreType = keystoreType;
        this.truststoreType = truststoreType;
        this.verifyHostname = verifyHostname;
        this.handshakeTimeout = handshakeTimeout;
    }

    /// The settings as the validation chain sees them, where this bridge was read from a
    /// configuration file.
    public @Nullable TLSConfig getEffectiveTls() {
        return effectiveTls;
    }

    /// The `<tls>` element as written, for the configuration write-back to return to the file
    /// unchanged. Treated as read-only.
    public @Nullable BridgeTlsEntity getConfiguredTls() {
        return configuredTls;
    }

    public @Nullable String getKeystorePath() {
        return keystorePath;
    }

    public @NotNull String getKeystorePassword() {
        return keystorePassword;
    }

    public @NotNull String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    public @Nullable String getTruststorePath() {
        return truststorePath;
    }

    public @NotNull String getTruststorePassword() {
        return truststorePassword;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    public @Nullable String getKeystoreType() {
        return keystoreType;
    }

    public @Nullable String getTruststoreType() {
        return truststoreType;
    }

    public boolean isVerifyHostname() {
        return verifyHostname;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BridgeTls bridgeTls)) {
            return false;
        }

        if (!Objects.equals(keystorePath, bridgeTls.keystorePath)) {
            return false;
        }
        if (!Objects.equals(keystorePassword, bridgeTls.keystorePassword)) {
            return false;
        }
        if (!Objects.equals(privateKeyPassword, bridgeTls.privateKeyPassword)) {
            return false;
        }
        if (!Objects.equals(truststorePath, bridgeTls.truststorePath)) {
            return false;
        }
        if (!Objects.equals(truststorePassword, bridgeTls.truststorePassword)) {
            return false;
        }
        if (!protocols.equals(bridgeTls.protocols)) {
            return false;
        }
        if (!cipherSuites.equals(bridgeTls.cipherSuites)) {
            return false;
        }
        // The superseding settings live only here; without it two bridges that differ only in them
        // compare equal, and a reload keeps the old connection running on the old settings.
        return isCompatibilityPreset(configuredTls) == isCompatibilityPreset(bridgeTls.configuredTls)
                && sameEffectiveTls(effectiveTls, bridgeTls.effectiveTls);
    }

    /// Whether a bridge is on the preset that keeps what a bridge validated before upgrading: it
    /// names neither `tls-checks-full` nor a preset other than `TLS_BRIDGE_DEFAULT`. A bridge without
    /// the configuration it was read from was built from the superseded settings, which mean the
    /// same. Part of equality: writing the preset's values out in `tls-checks-full` validates a
    /// directly trusted certificate's dates, so the switch must restart the bridge.
    public static boolean isCompatibilityPreset(final @Nullable BridgeTlsEntity configuredTls) {
        if (configuredTls == null) {
            return true;
        }
        return TLSConfigBridge.isCompatibilityPreset(configuredTls);
    }

    /// Compared by value through the interface rather than by the implementations' `equals`: the
    /// same file read twice must compare equal, or every reload restarts every TLS bridge, whichever
    /// class carries the settings.
    private static boolean sameEffectiveTls(final @Nullable TLSConfig left, final @Nullable TLSConfig right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.isEnabled() == right.isEnabled()
                && left.enforcesSameChecks(right)
                && left.identity().equals(right.identity())
                && left.truststore().equals(right.truststore())
                && left.allowList().equals(right.allowList())
                && left.revocationList().equals(right.revocationList())
                && left.protocols().equals(right.protocols())
                && left.cipherSuites().equals(right.cipherSuites())
                && left.handshakeTimeout().equals(right.handshakeTimeout());
    }

    private static int effectiveTlsHash(final @Nullable TLSConfig tls) {
        if (tls == null) {
            return 0;
        }
        return Objects.hash(
                tls.isEnabled(),
                tls.trustMode(),
                tls.sanUri(),
                tls.hostname(),
                tls.validity(),
                tls.revocation(),
                tls.keyUsage(),
                tls.identity(),
                tls.truststore(),
                tls.allowList(),
                tls.revocationList(),
                tls.protocols(),
                tls.cipherSuites(),
                tls.handshakeTimeout());
    }

    @Override
    public int hashCode() {
        int result = keystorePath != null ? keystorePath.hashCode() : 0;
        result = 31 * result + (keystorePassword != null ? keystorePassword.hashCode() : 0);
        result = 31 * result + (privateKeyPassword != null ? privateKeyPassword.hashCode() : 0);
        result = 31 * result + (truststorePath != null ? truststorePath.hashCode() : 0);
        result = 31 * result + (truststorePassword != null ? truststorePassword.hashCode() : 0);
        result = 31 * result + protocols.hashCode();
        result = 31 * result + cipherSuites.hashCode();
        result = 31 * result + Boolean.hashCode(isCompatibilityPreset(configuredTls));
        result = 31 * result + effectiveTlsHash(effectiveTls);
        return result;
    }

    @Override
    public String toString() {
        return "BridgeTls{" + "keystorePath='"
                + keystorePath
                + '\''
                + ", keystorePassword='"
                + keystorePassword
                + '\''
                + ", privateKeyPassword=HIDDEN"
                + ", truststorePath='"
                + truststorePath
                + '\''
                + ", truststorePassword=HIDDEN"
                + ", protocols="
                + protocols
                + ", cipherSuites="
                + cipherSuites
                + ", keystoreType='"
                + keystoreType
                + '\''
                + ", truststoreType='"
                + truststoreType
                + '\''
                + ", verifyHostname="
                + verifyHostname
                + ", handshakeTimeout="
                + handshakeTimeout
                + '}';
    }

    public static class Builder {
        private @Nullable String keystorePath;
        private @NotNull String keystorePassword = "";
        private @NotNull String privateKeyPassword = "";
        private @Nullable String truststorePath;
        private @NotNull String truststorePassword = "";
        private @NotNull List<String> protocols = List.of();
        private @NotNull List<String> cipherSuites = List.of();
        private @NotNull String keystoreType = "JKS";
        private @NotNull String truststoreType = "JKS";
        private boolean verifyHostname = true;
        private int handshakeTimeoutSeconds = 10;
        private @Nullable TLSConfig effectiveTls;
        private @Nullable BridgeTlsEntity configuredTls;

        /// The settings as the validation chain sees them. Set by the configuration reader; a caller
        /// assembling a bridge in code leaves it alone and gets the connection path that predates
        /// the chain.
        public @NotNull Builder withEffectiveTls(final @Nullable TLSConfig effectiveTls) {
            this.effectiveTls = effectiveTls;
            return this;
        }

        /// The `<tls>` element as read. Set by the configuration reader, so that the write-back returns
        /// it to the file as written.
        public @NotNull Builder withConfiguredTls(final @Nullable BridgeTlsEntity configuredTls) {
            this.configuredTls = configuredTls;
            return this;
        }

        public @NotNull Builder withKeystorePath(@Nullable String keystorePath) {
            this.keystorePath = keystorePath;
            return this;
        }

        public @NotNull Builder withKeystorePassword(@NotNull String keystorePassword) {
            this.keystorePassword = keystorePassword;
            return this;
        }

        public @NotNull Builder withPrivateKeyPassword(@NotNull String privateKeyPassword) {
            this.privateKeyPassword = privateKeyPassword;
            return this;
        }

        public @NotNull Builder withTruststorePath(@Nullable String truststorePath) {
            this.truststorePath = truststorePath;
            return this;
        }

        public @NotNull Builder withTruststorePassword(@NotNull String truststorePassword) {
            this.truststorePassword = truststorePassword;
            return this;
        }

        public @NotNull Builder withProtocols(@NotNull List<String> protocols) {
            this.protocols = protocols;
            return this;
        }

        public @NotNull Builder withCipherSuites(@NotNull List<String> cipherSuites) {
            this.cipherSuites = cipherSuites;
            return this;
        }

        public @NotNull Builder withKeystoreType(@NotNull String keystoreType) {
            this.keystoreType = keystoreType;
            return this;
        }

        public @NotNull Builder withTruststoreType(@NotNull String truststoreType) {
            this.truststoreType = truststoreType;
            return this;
        }

        public @NotNull Builder withVerifyHostname(boolean verifyHostname) {
            this.verifyHostname = verifyHostname;
            return this;
        }

        public @NotNull Builder withHandshakeTimeout(int handshakeTimeoutMs) {
            this.handshakeTimeoutSeconds = handshakeTimeoutMs;
            return this;
        }

        public @NotNull BridgeTls build() {
            return new BridgeTls(
                    keystorePath,
                    keystorePassword,
                    privateKeyPassword,
                    truststorePath,
                    truststorePassword,
                    protocols,
                    cipherSuites,
                    keystoreType,
                    truststoreType,
                    verifyHostname,
                    handshakeTimeoutSeconds,
                    effectiveTls,
                    configuredTls);
        }
    }
}
