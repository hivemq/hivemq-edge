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
package com.hivemq.security.tls;

import com.hivemq.security.tls.TLSConstants.HostnameCheck;
import com.hivemq.security.tls.TLSConstants.KeyUsageCheck;
import com.hivemq.security.tls.TLSConstants.RevocationCheck;
import com.hivemq.security.tls.TLSConstants.SanUriCheck;
import com.hivemq.security.tls.TLSConstants.TrustMode;
import com.hivemq.security.tls.TLSConstants.ValidityCheck;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/// What one surface's TLS configuration amounts to, with every preset already resolved.
///
/// An interface rather than a record, and deliberately so. Every implementation is the result of a
/// pure function from that surface's own configuration objects, and the signature is what enforces
/// that: a function returning a `TLSConfig` cannot answer by mutating what it read, because the
/// result is of a different type. A builder would leave room to wire the values up while walking the
/// configuration, and the override and default rules would then be tempting to implement by writing
/// back into the configuration objects. That would cost the configuration layer its round-trip
/// property - reading a file and writing it out again must produce the same file - which is a rule
/// the certificate-validation model states and which nothing here may break.
///
/// **Presets do not appear.** A preset is a spelling of the six checks, resolved while this is built
/// and never stored. Every accessor below returns a definite value, so nothing downstream decides
/// what an omitted check meant.
///
/// **Material is named, not read.** The four material accessors return references: a name, a uuid,
/// or files named directly. No file has been opened and no directory scanned, so an implementation
/// needs no filesystem and a test needs no fixture directory. Resolving references against the
/// declared `tls-material` is a later stage; reading the files is the stage after that, whose output
/// is a `TLSPolicy`.
///
/// Each surface adapts its own names, defaults and compatibility rules - a bridge's `verify-hostname`
/// boolean, an adapter's camelCase JSON - and nothing downstream can tell which surface it came from.
public interface TLSConfig {

    /// Whether TLS is used at all. When false, every other accessor is meaningless.
    boolean isEnabled();

    // --- the elementary checks, every one resolved ---------------------------------------------

    /// Where trust comes from.
    @NotNull
    TrustMode trustMode();

    /// Whether the announced application identifier must match the certificate.
    ///
    /// A surface whose protocol carries no application identifier returns [SanUriCheck#NONE]. MQTT
    /// carries none, so a bridge always does.
    @NotNull
    SanUriCheck sanUri();

    /// Whether the host connected to must appear in the certificate.
    @NotNull
    HostnameCheck hostname();

    /// Whether the certificate's validity period is enforced.
    @NotNull
    ValidityCheck validity();

    /// Whether an authority's withdrawal of the certificate is consulted.
    ///
    /// Any value other than [RevocationCheck#NONE] requires [TrustMode#CHAIN]; a configuration
    /// asking for revocation under another trust mode is rejected rather than quietly ignored.
    @NotNull
    RevocationCheck revocation();

    /// Whether the certificate's declared purpose is enforced.
    @NotNull
    KeyUsageCheck keyUsage();

    // --- the material, named but not read -----------------------------------------------------

    /// The key and certificate chain Edge presents to the peer, if any.
    ///
    /// Absent where Edge authenticates by other means, which is the common case for a bridge.
    @NotNull
    Optional<MaterialRef> identity();

    /// The authorities whose certificates Edge accepts.
    ///
    /// Read only under [TrustMode#CHAIN]. Absent there means nothing can be trusted by chaining: the
    /// runtime's own authorities are named explicitly, with `JVM-cacerts`, and never substituted for
    /// absence at this point. A surface that trusted them whenever no truststore was configured
    /// resolves that where it derives this configuration, so what arrives here already names them.
    @NotNull
    Optional<MaterialRef> truststore();

    /// The individual certificates Edge accepts, by SHA-256 fingerprint.
    ///
    /// Read only under [TrustMode#ALLOW_LIST], where it is required.
    @NotNull
    Optional<MaterialRef> allowList();

    /// The certificates authorities have withdrawn.
    ///
    /// Read only while a certification path is built, so only under [TrustMode#CHAIN].
    @NotNull
    Optional<MaterialRef> revocationList();

    // --- how the tunnel is negotiated ---------------------------------------------------------

    /// The TLS versions Edge offers, or empty to let the TLS implementation choose.
    ///
    /// Empty is not the same as a list Edge picked: it means Edge says nothing, so the
    /// implementation's own defaults apply and track its updates. A list baked in here would freeze
    /// today's opinion and keep a retired version alive after the implementation dropped it.
    @NotNull
    List<String> protocols();

    /// The cipher suites Edge offers, or empty to let the TLS implementation choose. Empty carries
    /// the same meaning as for [#protocols()].
    @NotNull
    List<String> cipherSuites();

    /// How long a handshake may take before Edge abandons it.
    @NotNull
    java.time.Duration handshakeTimeout();

    // --- derived, for readers that would otherwise repeat the comparison ----------------------

    /// Whether a certification path is built, which decides whether revocation can be enforced and
    /// whether the truststore is read.
    default boolean isChainBuilt() {
        return trustMode() == TrustMode.CHAIN;
    }

    /// Whether the announced application identifier must match the certificate.
    default boolean isSanUri() {
        return sanUri() == SanUriCheck.APPLICATION_URI;
    }

    /// Whether the host Edge connected to must appear in the certificate.
    default boolean isHostname() {
        return hostname() == HostnameCheck.HOSTNAME;
    }

    /// Whether the certificate's validity period is enforced.
    default boolean isValidity() {
        return validity() == ValidityCheck.NOT_BEFORE_OR_AFTER;
    }

    /// Whether revocation status is consulted at all.
    default boolean isRevocation() {
        return revocation() != RevocationCheck.NONE;
    }

    /// Whether a revocation list is required for every issuing authority in the path, rather than
    /// consulted where one happens to be available.
    default boolean isRevocationLists() {
        return revocation() == RevocationCheck.REQUIRE_CRLS;
    }

    /// Whether the KeyUsage extension is enforced.
    default boolean isKeyUsage() {
        return keyUsage() != KeyUsageCheck.NONE;
    }

    /// Whether the ExtendedKeyUsage extension is enforced in addition.
    default boolean isExtendedKeyUsage() {
        return keyUsage() == KeyUsageCheck.SERVER_AUTH;
    }

    /// Whether any check applies to the peer's own certificate without a path being built: the
    /// application identifier, the hostname, the validity period or the declared purpose.
    ///
    /// Revocation is deliberately not among them. It is decidable only while a path is built, so
    /// counting it would claim a check that the pathless trust modes cannot perform. When this is
    /// false under [TrustMode#ANY_CERT], nothing whatever is verified.
    default boolean hasCertificateOnlyChecks() {
        return sanUri() != SanUriCheck.NONE
                || hostname() != HostnameCheck.NONE
                || validity() != ValidityCheck.NONE
                || keyUsage() != KeyUsageCheck.NONE;
    }

    /// The six checks, for a start-up log line that lets an operator see what is in force.
    default @NotNull String describeChecks() {
        return "trustMode=%s, sanUri=%s, hostname=%s, validity=%s, revocation=%s, keyUsage=%s"
                .formatted(trustMode(), sanUri(), hostname(), validity(), revocation(), keyUsage());
    }

    /// Whether two configurations enforce the same six checks, whatever material they name and
    /// whichever surface they came from.
    ///
    /// What a reader usually means by "the same validation": the material is where the certificates
    /// come from, not what is done with them, and two surfaces resolving the same preset should
    /// agree here even though nothing else about them matches.
    default boolean enforcesSameChecks(final @NotNull TLSConfig other) {
        return trustMode() == other.trustMode()
                && sanUri() == other.sanUri()
                && hostname() == other.hostname()
                && validity() == other.validity()
                && revocation() == other.revocation()
                && keyUsage() == other.keyUsage();
    }
}
