/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.opcua.client;

import com.hivemq.edge.adapters.opcua.config.AllowList;
import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.BasicAuth;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.TLSConfigOpcUA;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.security.TLSValidatorOpcUA;
import com.hivemq.edge.adapters.opcua.util.KeystoreUtil;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConfigResolved;
import com.hivemq.security.tls.TLSConstants;
import com.hivemq.security.tls.TLSMaterialRegistry;
import com.hivemq.security.tls.TLSPolicy;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.identity.CompositeProvider;
import org.eclipse.milo.opcua.sdk.client.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ParsedConfig(
        boolean tlsEnabled,
        /// The checks and the material, with any reference into shared material already resolved.
        @NotNull TLSConfig effectiveChecks,
        @Nullable KeystoreUtil.KeyPairWithChain keyPairWithChain,
        @Nullable CertificateValidator clientCertificateValidator,
        @NotNull IdentityProvider identityProvider,
        @Nullable String applicationUri) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ParsedConfig.class);

    /// For a deployment that declares no shared TLS material, where every reference is therefore
    /// unresolvable and every adapter writes its material where it is used.
    public static Result<ParsedConfig, String> fromConfig(final OpcUaSpecificAdapterConfig adapterConfig) {
        return fromConfig(adapterConfig, TLSMaterialRegistry.empty());
    }

    /// @param materials the TLS material this deployment declares, which the adapter's references
    ///     are resolved against
    public static Result<ParsedConfig, String> fromConfig(
            final OpcUaSpecificAdapterConfig adapterConfig, final @NotNull TLSMaterialRegistry materials) {
        final Tls tlsConfig = adapterConfig.getTls();
        final boolean tlsEnabled = tlsConfig.enabled();
        final String endpointUri = adapterConfig.getUri();

        // Read-time projection: the configuration itself is never rewritten, so writing it back out
        // returns exactly what the operator wrote.
        final TLSConfigOpcUA written;
        try {
            written = new TLSConfigOpcUA(tlsConfig);
        } catch (final TLSConfigOpcUA.InvalidTlsConfigException e) {
            return Failure.of(e.reason());
        }

        // Whatever the adapter named in a shared tls-material, replaced by the material itself.
        // Resolved before anything is read, and resolved here only: a reference resolved in two
        // places is a reference that can be resolved two ways.
        final TLSConfig checks;
        try {
            checks = new TLSConfigResolved(written, materials);
        } catch (final TLSMaterialRegistry.InvalidReferenceException e) {
            return Failure.of(e.reason());
        }

        final Truststore truststore = tlsConfig.truststore();
        // The components are declared non-null, but a truststore element with a missing
        // (or misspelled, and therefore dropped) child binds nulls anyway. Failing here
        // with the element named beats a NullPointerException - and silently falling back
        // to the JVM cacerts would swap the trust anchors the operator meant to use.
        // Checked unconditionally, mirroring the keystore guards below: a configured store must be
        // structurally valid even when the effective trust mode leaves it inert, so the operator is
        // told about the broken element now rather than the first time a mode change makes it load.
        //
        // Blank counts as absent. A present element with an empty <path> used to fall through to the
        // cacerts fallback, so the adapter ran on the public CA bundle while the configuration said
        // it had a truststore - the same fail-open shape as a missing path, which was already
        // refused. Nothing generates that shape: the UI form drops empty strings and submits no path
        // at all, no fixture or documented example has one, and the documentation already states that
        // a store without a path stops the adapter. Absence is still spelled by leaving the element
        // out, which is what the "system truststore" example in the documentation does.
        //
        // Guards the superseded element only. Its superseding counterpart, <tls-truststore>, takes
        // several files, a directory, a glob pattern, the runtime's own authorities or a reference, so
        // "has no path" is not a defect there; what it requires is checked where it is translated.
        if (truststore != null) {
            if (isBlank(truststore.path())) {
                return Failure.of("Truststore is configured but has no 'path'. Add "
                        + "<truststore><path>...</path></truststore>, or remove the truststore element "
                        + "entirely to trust the JVM cacerts.");
            }
            if (truststore.password() == null) {
                return Failure.of("Truststore is configured but has no 'password'. Add "
                        + "<truststore><password>...</password></truststore>.");
            }
        }

        if (tlsEnabled && adapterConfig.getSecurity().policy() == SecPolicy.NONE) {
            // Logged once at start, not per connect. With SecurityPolicy None the endpoint exchanges
            // no certificate at all, so every TLS and certificate-validation setting below is built
            // but never exercised - the operator who configured them believes they are protected.
            log.warn(
                    "OPC UA adapter endpoint '{}': TLS and certificate-validation settings are configured, but "
                            + "the security policy is NONE, which exchanges no certificates - none of those "
                            + "settings take effect. Set <security><policy> to a policy other than NONE to "
                            + "enforce them.",
                    endpointUri);
        }

        CertificateValidator certValidator = null;
        KeystoreUtil.KeyPairWithChain keyPairWithChain = null;
        if (tlsEnabled) {
            // Everything the configuration named, read once, before any connection exists. The one
            // step that touches a filesystem, and the one that refuses a surface whose material
            // cannot be read: a truststore that will not open is a configuration error, never a
            // reason to fall back to trusting less.
            final Optional<String> allowListDirectory = supersededAllowListDirectory(checks, tlsConfig);
            if (allowListDirectory.isPresent()) {
                return Failure.of(("The allowList path '%s' is a directory. allowList reads one file of "
                                + "fingerprints: name that file, or write <tls-allowlist><directory><location>%s"
                                + "</location></directory></tls-allowlist> instead of allowList to read every file "
                                + "in the directory.")
                        .formatted(allowListDirectory.get(), allowListDirectory.get()));
            }
            final TLSPolicy policy;
            try {
                policy = new TLSPolicy(checks);
            } catch (final TLSPolicy.TLSMaterialException e) {
                return Failure.of(e.reason());
            }

            // Edge's own key, from the one read above. Reshaped rather than read again, so there is
            // no second place that could open a different file or pick a different key entry.
            if (policy.identity().isPresent()) {
                keyPairWithChain = keyPairWithChainFrom(policy.identity().get());
            }

            // Which validator the trust mode calls for, and what it asks the OPC UA stack to check.
            certValidator = TLSValidatorOpcUA.validatorFor(policy, endpointUri);

            if (!policy.revocationLists().isEmpty()) {
                log.info(
                        "OPC UA adapter endpoint '{}': {} certificate revocation list(s) loaded.",
                        endpointUri,
                        policy.revocationLists().size());
            } else if (checks.isChainBuilt() && checks.isRevocation() && anchorsIncludeACa(policy)) {
                // Logged once at start, not per connect. Revocation is decided while the path is
                // built, from the revocation lists the trust list offers; with none, the status of
                // every issuing CA is "unknown", and unknown fails closed. That refusal is correct,
                // but it arrives at connect time as Bad_CertificateRevocationUnknown with nothing
                // pointing at the missing input, which is how a correctly trusted CA-signed server
                // looks indistinguishable from an untrusted one.
                //
                // Deliberately a warning and not a start-up refusal, and deliberately conditional on
                // the truststore actually holding a CA: a truststore of end-entity certificates is
                // direct trust, where no issuer appears in the path and revocation is satisfied
                // vacuously. Warning there would fire on the majority of working deployments and
                // train operators to ignore it.
                log.warn(
                        "OPC UA adapter endpoint '{}': revocation={} is enforced and the truststore contains "
                                + "a CA, but no certificate revocation list is configured. The issuer's "
                                + "revocation status is therefore unknown, and unknown fails closed - a "
                                + "server certificate signed by that CA will be refused with "
                                + "Bad_CertificateRevocationUnknown however correct the rest of the "
                                + "configuration is. Point <revocationList><path>...</path></revocationList> "
                                + "at the CA's CRL (a file, or a directory of them), or set "
                                + "tlsChecksFull.revocation=NONE to accept an unchecked revocation status.",
                        endpointUri,
                        checks.revocation());
            }

            // No warning here for a path built with no trust anchors: it cannot happen. Where the
            // trust mode builds a path and the configuration names no truststore, the effective
            // configuration substitutes the runtime's own authorities and warns there, and a
            // truststore that holds no certificate is refused when it is read.

            if (checks.isChainBuilt() && !checks.isHostname()) {
                // Logged once at start (not per-connect): the certificate is trusted via its chain,
                // but is not verified to belong to this endpoint's hostname, so a substituted server
                // whose certificate chains to the same anchor is accepted.
                log.warn(
                        "OPC UA adapter endpoint '{}': TLS hostname verification is not enabled. The server "
                                + "certificate is trusted via its chain but is not checked against the "
                                + "endpoint hostname, so a substituted server whose certificate chains to the "
                                + "same anchor would be accepted. Enable it with tlsChecks=ALL or "
                                + "tlsChecksFull.hostname=HOSTNAME.",
                        endpointUri);
            }

            if (checks.trustMode() != com.hivemq.security.tls.TLSConstants.TrustMode.ALLOW_LIST
                    && TLSConfigOpcUA.hasAllowList(tlsConfig)) {
                // Logged once at start, not per connect. The file is read only under
                // trustMode=ALLOW_LIST, so an allow-list configured under any other trust mode is
                // inert - and an operator who believes they have pinned a specific set of
                // certificates is in fact trusting a much wider one. Deliberately not an error: the
                // trust mode that is in effect was written explicitly and is enforced in full, so the
                // adapter is not less safe than that configuration asks for, only less safe than the
                // operator probably intends.
                log.warn(
                        "OPC UA adapter endpoint '{}': a certificate allow-list is configured but the effective "
                                + "trust mode is {}, which never reads it. No server certificate is pinned by its "
                                + "fingerprint. Set tlsChecks=SELF_SIGNED or tlsChecksFull.trustMode=ALLOW_LIST to "
                                + "enforce the allow-list, or remove the allow-list element.",
                        endpointUri,
                        checks.trustMode());
            }

            if (checks.trustMode() != com.hivemq.security.tls.TLSConstants.TrustMode.CHAIN
                    && TLSConfigOpcUA.hasRevocationList(tlsConfig)) {
                // The same shape as the inert allow-list above, and inert for the same structural
                // reason: CRLs are consulted only while a certification path is built, which only
                // CHAIN does. An operator who supplied CRLs believes revocation is being enforced.
                // Not an error - the effective configuration already refuses revocation without
                // CHAIN, so what runs is exactly what was asked for, just not what was meant.
                log.warn(
                        "OPC UA adapter endpoint '{}': certificate revocation lists are configured but the "
                                + "effective trust mode is {}, which builds no certification path and never reads "
                                + "them. No revocation status is checked. Set tlsChecksFull.trustMode=CHAIN to "
                                + "enforce revocation, or remove the revocation-list element.",
                        endpointUri,
                        checks.trustMode());
            }
        }

        final Keystore keystore = adapterConfig.getTls().keystore();
        // Guards the superseded element only. Its superseding counterpart, <tls-identity>, names a
        // PEM key and certificate in separate elements and may carry no container password at all, so
        // these three say nothing about it; what it requires is checked where it is translated.
        //
        // The components are declared nullable, but a keystore element whose <path> child is missing
        // (or misspelled, and therefore dropped by the unknown-setting handling core applies to this
        // record) binds a null anyway. Without these guards that null surfaces as a
        // NullPointerException at adapter start instead of a configuration error naming the element.
        // Blank counts as absent here for the same reason it does on the truststore above: a present
        // element with an empty <path> used to skip every guard below and leave the adapter with no
        // client certificate, which surfaces as a connection failure under a security policy that
        // needs one, or under X509 authentication - far from the element that caused it.
        if (keystore != null) {
            if (isBlank(keystore.path())) {
                return Failure.of("Keystore is configured but has no 'path'. Add "
                        + "<keystore><path>...</path></keystore>, or remove the keystore element entirely.");
            }
            if (keystore.password() == null) {
                return Failure.of("Keystore is configured but has no 'password'. Add "
                        + "<keystore><password>...</password></keystore>.");
            }
            if (keystore.privateKeyPassword() == null) {
                return Failure.of("Keystore is configured but has no 'privateKeyPassword'. Add "
                        + "<keystore><privateKeyPassword>...</privateKeyPassword></keystore>.");
            }
        }
        // An identity configured while TLS is disabled is never read: the policy is built only when
        // TLS is on, and with it off no certificate is exchanged for the key to appear in.
        if ((keystore != null || adapterConfig.getTls().tlsIdentity() != null) && !tlsEnabled) {
            log.warn(
                    "OPC UA adapter endpoint '{}': a keystore is configured but TLS is disabled, so no client "
                            + "certificate is presented. Enable TLS with <tls><enabled>true</enabled></tls>, or "
                            + "remove the keystore element.",
                    endpointUri);
        }

        final Optional<IdentityProvider> identityProvider =
                createIdentityProvider(tlsEnabled, keyPairWithChain, adapterConfig.getAuth());
        if (identityProvider.isEmpty()) {
            return Failure.of("Failed to create identity provider, check authentication configuration");
        }

        // Determine Application URI with priority: configured > certificate SAN > default
        final String applicationUri;
        if (adapterConfig.getApplicationUri() != null
                && !adapterConfig.getApplicationUri().isBlank()) {
            // Priority 1: Use configured override
            applicationUri = adapterConfig.getApplicationUri();
            log.info("Using configured Application URI override: {}", applicationUri);
        } else if (keyPairWithChain != null && keyPairWithChain.applicationUri() != null) {
            // Priority 2: Use certificate SAN URI
            applicationUri = keyPairWithChain.applicationUri();
            log.info("Using Application URI from certificate: {}", applicationUri);
        } else {
            // Priority 3: Will use default in OpcUaClientConfigurator
            applicationUri = null;
            if (tlsEnabled && keyPairWithChain != null) {
                log.warn("Certificate does not contain Application URI in SAN extension, will use default URI");
            }
        }

        return Success.of(new ParsedConfig(
                tlsEnabled, checks, keyPairWithChain, certValidator, identityProvider.get(), applicationUri));
    }

    /**
     * Whether the validator that accepts any certificate is actually installed. Named for what is
     * running, not for what the configuration says: with TLS disabled no certificate validator is
     * built at all, so a {@code trustMode=ANY_CERT} left in a disabled TLS block is inert — and
     * warning about certificates that are never examined teaches operators to ignore the warning
     * that matters.
     */
    public boolean anyCertificateValidatorActive() {
        return tlsEnabled && effectiveChecks.trustMode() == com.hivemq.security.tls.TLSConstants.TrustMode.ANY_CERT;
    }

    /// The superseded `allowList` path, where it names a directory that fingerprint trust would read.
    ///
    /// `allowList` has only ever read a single file; a directory there fails when it is read anyway,
    /// so this changes the message, not the outcome, and only where the allow-list is read at all.
    /// Unlike `revocationList`, which has always taken a directory too.
    private static @NotNull Optional<String> supersededAllowListDirectory(
            final @NotNull TLSConfig checks, final @NotNull Tls tlsConfig) {
        final AllowList allowList = tlsConfig.allowList();
        final String path = allowList == null ? null : allowList.path();
        if (checks.trustMode() != TLSConstants.TrustMode.ALLOW_LIST || path == null || path.isBlank()) {
            return Optional.empty();
        }
        try {
            return Files.isDirectory(Path.of(path)) ? Optional.of(path) : Optional.empty();
        } catch (final InvalidPathException _) {
            // Not a path at all; reading it reports that.
            return Optional.empty();
        }
    }

    /** A path that is null, empty or whitespace-only is no path at all. */
    private static boolean isBlank(final @Nullable String path) {
        return path == null || path.isBlank();
    }

    /// Whether any trust anchor is a certificate authority, and so whether an issuer can appear in a
    /// certification path at all - which is what decides whether a missing revocation list matters.
    ///
    /// The discriminator between the two shapes of a chain-trust truststore: a CA to chain through,
    /// where revocation is a real question and needs a revocation list to answer, and directly
    /// trusted end-entity certificates, where the server's own certificate is the anchor, no issuer
    /// is consulted and revocation is satisfied vacuously.
    ///
    /// `getBasicConstraints()` returns the path-length constraint for a CA and `-1` for anything
    /// else, which is exactly this question.
    private static boolean anchorsIncludeACa(final @NotNull TLSPolicy policy) {
        return policy.trustAnchors().stream()
                .anyMatch(anchor -> anchor.getTrustedCert().getBasicConstraints() != -1);
    }

    /// Edge's own key and certificate chain, in the shape Eclipse Milo asks for.
    ///
    /// Reads nothing: the policy already opened the material, whatever format it was in and whether
    /// it was written here or declared in a shared `tls-material`. This only reshapes what the
    /// policy holds, so the key Milo signs with is the key the policy loaded.
    private static @NotNull KeystoreUtil.KeyPairWithChain keyPairWithChainFrom(
            final @NotNull TLSPolicy.Identity identity) {
        return new KeystoreUtil.KeyPairWithChain(
                identity.privateKey(),
                identity.certificate(),
                identity.chain().toArray(new X509Certificate[0]),
                identity.applicationUri());
    }

    private static @NotNull Optional<IdentityProvider> createIdentityProvider(
            final boolean tlsEnabled,
            final @Nullable KeystoreUtil.KeyPairWithChain keyPairWithChain,
            final @Nullable Auth auth) {
        if (log.isDebugEnabled()) {
            log.debug(
                    "Configuring Authentication with auth {} tlsEnabled {} and keyPairWithChain {}",
                    auth != null,
                    tlsEnabled,
                    keyPairWithChain != null);
        }

        final List<IdentityProvider> identityProviders = new ArrayList<>();

        if (auth != null) {
            final X509Auth x509Auth = auth.x509Auth();
            if (x509Auth != null && x509Auth.enabled()) {
                if (!tlsEnabled) {
                    log.error(
                            "X509 authentication is enabled but TLS is not enabled. X509 authentication will not work.");
                    return Optional.empty();
                }

                if (keyPairWithChain == null) {
                    log.error("X509 authentication is enabled but keystore for TLS is not available");
                    return Optional.empty();
                }

                if (log.isDebugEnabled()) {
                    log.debug("X509 authentication is enabled");
                }
                identityProviders.add(new X509IdentityProvider(
                        Arrays.asList(keyPairWithChain.certificateChain()), keyPairWithChain.privateKey()));
            }

            final BasicAuth basicAuth = auth.basicAuth();
            if (basicAuth != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Basic authentication is enabled");
                }
                identityProviders.add(new UsernameProvider(basicAuth.username(), basicAuth.password()));
            }
        }

        if (identityProviders.size() == 1) {
            final IdentityProvider singleProvider = identityProviders.get(0);
            log.info(
                    "Using single identity provider: {}",
                    singleProvider.getClass().getSimpleName());
            return Optional.of(singleProvider);
        }

        if (identityProviders.size() > 1) {
            log.info("Using composite identity provider");
            return Optional.of(new CompositeProvider(List.copyOf(identityProviders)));
        }

        log.info("Using default anonymous identity provider");
        return Optional.of(new AnonymousProvider());
    }
}
