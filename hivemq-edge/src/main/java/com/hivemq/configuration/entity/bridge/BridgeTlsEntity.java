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
package com.hivemq.configuration.entity.bridge;

import com.hivemq.configuration.entity.listener.tls.KeystoreEntity;
import com.hivemq.configuration.entity.listener.tls.TruststoreEntity;
import com.hivemq.configuration.entity.tls.TlsAllowlistEntity;
import com.hivemq.configuration.entity.tls.TlsIdentityEntity;
import com.hivemq.configuration.entity.tls.TlsRevocationlistEntity;
import com.hivemq.configuration.entity.tls.TlsTruststoreEntity;
import jakarta.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "tls")
@XmlAccessorType(XmlAccessType.NONE)
public class BridgeTlsEntity {

    @XmlElement(name = "enabled")
    private boolean enabled = false;

    @XmlElementRef(required = false)
    private @Nullable KeystoreEntity keyStore;

    @XmlElementRef(required = false)
    private @Nullable TruststoreEntity trustStore;

    @XmlElementWrapper(name = "protocols")
    @XmlElement(name = "protocol")
    private @NotNull List<String> protocols = new ArrayList<>();

    @XmlElementWrapper(name = "cipher-suites")
    @XmlElement(name = "cipher-suite")
    private @NotNull List<String> cipherSuites = new ArrayList<>();

    @XmlElement(name = "handshake-timeout", defaultValue = "10")
    private int handshakeTimeout = 10;

    /// Whether the server certificate is checked to belong to the host this bridge connects to.
    ///
    /// Superseded by `tls-checks` and `tls-checks-full`, and refused alongside either of them. On its
    /// own it still works and feeds the hostname check; left out, the compatibility preset supplies
    /// it. Declared as an object rather than a primitive so that "not written" is distinguishable
    /// from "written false", which is what the refusal rests on.
    @XmlElement(name = "verify-hostname")
    private @Nullable Boolean verifyHostname = null;

    /// A named certificate-validation profile. Superseded spellings of the material may not appear
    /// beside it.
    @XmlElement(name = "tls-checks")
    private @Nullable String tlsChecks = null;

    /// The five independent validation checks, for what the presets do not cover.
    @XmlElementRef(required = false)
    private @Nullable BridgeTlsChecksEntity tlsChecksFull = null;

    /// The private key and certificate chain this bridge presents. Supersedes `keystore`, and adds
    /// PEM, passwords read from files, and references to shared TLS material.
    @XmlElement(name = "tls-identity")
    private @Nullable TlsIdentityEntity tlsIdentity = null;

    /// The authorities whose certificates this bridge accepts. Supersedes `truststore`, and adds PEM,
    /// several files, directories, glob patterns and the runtime's own authorities.
    @XmlElement(name = "tls-truststore")
    private @Nullable TlsTruststoreEntity tlsTruststore = null;

    /// The individual server certificates this bridge accepts, by SHA-256 fingerprint. A bridge has
    /// never had this; it arrives with the trust mode that reads it.
    @XmlElement(name = "tls-allowlist")
    private @Nullable TlsAllowlistEntity tlsAllowlist = null;

    /// The certificates authorities have withdrawn. A bridge has never had this either; it arrives
    /// with the revocation check that reads it.
    @XmlElement(name = "tls-revocationlist")
    private @Nullable TlsRevocationlistEntity tlsRevocationlist = null;

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable KeystoreEntity getKeyStore() {
        return keyStore;
    }

    public @Nullable TruststoreEntity getTrustStore() {
        return trustStore;
    }

    public @NotNull List<String> getCipherSuites() {
        return cipherSuites;
    }

    public @NotNull List<String> getProtocols() {
        return protocols;
    }

    public int getHandshakeTimeout() {
        return handshakeTimeout;
    }

    /// Whether the hostname check is on, with the default applied: an operator who writes nothing
    /// gets it, which is what a bridge has always done.
    public boolean isVerifyHostname() {
        return !Boolean.FALSE.equals(verifyHostname);
    }

    /// Whether the operator wrote the element at all, which decides whether writing it beside the
    /// superseding checks elements is a mixture to refuse or simply the default taking effect.
    public boolean isVerifyHostnameWritten() {
        return verifyHostname != null;
    }

    public @Nullable String getTlsChecks() {
        return tlsChecks;
    }

    public @Nullable BridgeTlsChecksEntity getTlsChecksFull() {
        return tlsChecksFull;
    }

    public @Nullable TlsIdentityEntity getTlsIdentity() {
        return tlsIdentity;
    }

    public @Nullable TlsTruststoreEntity getTlsTruststore() {
        return tlsTruststore;
    }

    public @Nullable TlsAllowlistEntity getTlsAllowlist() {
        return tlsAllowlist;
    }

    public @Nullable TlsRevocationlistEntity getTlsRevocationlist() {
        return tlsRevocationlist;
    }

    /// Whether any element that supersedes the older `keystore` / `truststore` / `verify-hostname`
    /// spelling is written: settings the REST model cannot express.
    public boolean hasSupersedingElements() {
        return tlsChecks != null
                || tlsChecksFull != null
                || tlsIdentity != null
                || tlsTruststore != null
                || tlsAllowlist != null
                || tlsRevocationlist != null;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setKeyStore(final KeystoreEntity keyStore) {
        this.keyStore = keyStore;
    }

    public void setTrustStore(final TruststoreEntity trustStore) {
        this.trustStore = trustStore;
    }

    public void setProtocols(final List<String> protocols) {
        this.protocols = protocols;
    }

    public void setCipherSuites(final List<String> cipherSuites) {
        this.cipherSuites = cipherSuites;
    }

    public void setHandshakeTimeout(final int handshakeTimeout) {
        this.handshakeTimeout = handshakeTimeout;
    }

    public void setVerifyHostname(final boolean verifyHostname) {
        this.verifyHostname = verifyHostname;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof BridgeTlsEntity that)) return false;
        // On the raw field, not the defaulted accessor: a bridge that wrote nothing and one that
        // wrote the default are the same configuration to run but not the same file, and a reload
        // compares these to decide what changed.
        return isEnabled() == that.isEnabled()
                && getHandshakeTimeout() == that.getHandshakeTimeout()
                && Objects.equals(verifyHostname, that.verifyHostname)
                && Objects.equals(getKeyStore(), that.getKeyStore())
                && Objects.equals(getTrustStore(), that.getTrustStore())
                && Objects.equals(getProtocols(), that.getProtocols())
                && Objects.equals(getCipherSuites(), that.getCipherSuites())
                && Objects.equals(getTlsChecks(), that.getTlsChecks())
                && Objects.equals(getTlsChecksFull(), that.getTlsChecksFull())
                && Objects.equals(getTlsIdentity(), that.getTlsIdentity())
                && Objects.equals(getTlsTruststore(), that.getTlsTruststore())
                && Objects.equals(getTlsAllowlist(), that.getTlsAllowlist())
                && Objects.equals(getTlsRevocationlist(), that.getTlsRevocationlist());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                isEnabled(),
                getKeyStore(),
                getTrustStore(),
                getProtocols(),
                getCipherSuites(),
                getHandshakeTimeout(),
                verifyHostname,
                getTlsChecks(),
                getTlsChecksFull(),
                getTlsIdentity(),
                getTlsTruststore(),
                getTlsAllowlist(),
                getTlsRevocationlist());
    }
}
