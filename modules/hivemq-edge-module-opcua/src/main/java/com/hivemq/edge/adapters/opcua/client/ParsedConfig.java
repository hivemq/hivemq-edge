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
import com.hivemq.edge.adapters.opcua.config.EffectiveChecks;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.SecPolicy;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.TlsChecksProjection;
import com.hivemq.edge.adapters.opcua.config.TrustMode;
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.security.AllowListCertificateValidator;
import com.hivemq.edge.adapters.opcua.security.CertificateFingerprints;
import com.hivemq.edge.adapters.opcua.security.CertificateTrustListManager;
import com.hivemq.edge.adapters.opcua.security.CheckOnlyCertificateValidator;
import com.hivemq.edge.adapters.opcua.util.KeystoreUtil;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.eclipse.milo.opcua.sdk.client.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.identity.CompositeProvider;
import org.eclipse.milo.opcua.sdk.client.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.MemoryCertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record ParsedConfig(
        boolean tlsEnabled,
        @NotNull EffectiveChecks effectiveChecks,
        @Nullable KeystoreUtil.KeyPairWithChain keyPairWithChain,
        @Nullable CertificateValidator clientCertificateValidator,
        @NotNull IdentityProvider identityProvider,
        @Nullable String applicationUri) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ParsedConfig.class);

    public static Result<ParsedConfig, String> fromConfig(final OpcUaSpecificAdapterConfig adapterConfig) {
        final Tls tlsConfig = adapterConfig.getTls();
        final boolean tlsEnabled = tlsConfig.enabled();
        final String endpointUri = adapterConfig.getUri();

        // Read-time projection: the configuration itself is never rewritten, so writing it back out
        // returns exactly what the operator wrote.
        final EffectiveChecks checks;
        try {
            checks = TlsChecksProjection.project(tlsConfig);
        } catch (final TlsChecksProjection.InvalidTlsChecksConfigException e) {
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
        if (truststore != null && isBlank(truststore.path())) {
            return Failure.of("Truststore is configured but has no 'path'. Add "
                    + "<truststore><path>...</path></truststore>, or remove the truststore element "
                    + "entirely to trust the JVM cacerts.");
        }
        if (truststore != null && truststore.password() == null) {
            return Failure.of("Truststore is configured but has no 'password'. Add "
                    + "<truststore><password>...</password></truststore>.");
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
        if (tlsEnabled) {
            switch (checks.trustMode()) {
                case CHAIN -> {
                    final var trustedCertsOpt = getTrustedCerts(truststore);
                    if (trustedCertsOpt.isEmpty()) {
                        // Reachable only when the user explicitly configured a truststore path that
                        // is missing or unreadable. "No truststore configured" silently falls back
                        // to JVM cacerts inside getTrustedCerts and does NOT land here.
                        return Failure.of("Truststore is configured but the file is missing or unreadable. "
                                + "Either correct the path, leave the truststore unset to use the JVM cacerts, "
                                + "or use trustMode=ALLOW_LIST to trust specific certificates by fingerprint.");
                    }
                    certValidator = createServerCertificateValidator(trustedCertsOpt.get(), checks);
                    if (!checks.isHostname()) {
                        // Logged once at start (not per-connect): the certificate is trusted via its
                        // chain, but is not verified to belong to this endpoint's hostname, so a
                        // substituted server whose certificate chains to the same anchor is accepted.
                        log.warn(
                                "OPC UA adapter endpoint '{}': TLS hostname verification is not enabled. The server "
                                        + "certificate is trusted via its chain but is not checked against the "
                                        + "endpoint hostname, so a substituted server whose certificate chains to the "
                                        + "same anchor would be accepted. Enable it with tlsChecks=ALL or "
                                        + "tlsChecksFull.hostname=HOSTNAME.",
                                endpointUri);
                    }
                }
                case ALLOW_LIST -> {
                    final String allowListPath;
                    try {
                        allowListPath = TlsChecksProjection.requireAllowListPath(tlsConfig.allowList());
                    } catch (final TlsChecksProjection.InvalidTlsChecksConfigException e) {
                        // Unreachable while project() above validates ALLOW_LIST configurations, but a
                        // future caller that skips projection gets the operator-facing error rather
                        // than a NullPointerException.
                        return Failure.of(e.reason());
                    }
                    final Set<String> fingerprints;
                    try {
                        fingerprints = CertificateFingerprints.loadAllowList(Path.of(allowListPath));
                    } catch (final IOException e) {
                        // The message is already operator-facing and names the path and the specific
                        // problem; interpolating the exception itself only added a Java class name and a
                        // second copy of the path.
                        return Failure.of(String.valueOf(e.getMessage()));
                    } catch (final IllegalArgumentException e) {
                        return Failure.of(String.valueOf(e.getMessage()));
                    }
                    log.info(
                            "OPC UA adapter endpoint '{}': server certificates are trusted by fingerprint; "
                                    + "{} fingerprint(s) loaded from '{}'.",
                            endpointUri,
                            fingerprints.size(),
                            allowListPath);
                    certValidator = new AllowListCertificateValidator(fingerprints, checks, endpointUri);
                }
                case ANY_CERT ->
                    // No trust is established at all. Whatever else is configured is still enforced on
                    // the presented certificate, which is what keeps the axes orthogonal.
                    certValidator = checks.hasStandaloneEndEntityChecks()
                            ? new CheckOnlyCertificateValidator(checks)
                            : new CertificateValidator.InsecureCertificateValidator();
            }

            final AllowList configuredAllowList = tlsConfig.allowList();
            if (checks.trustMode() != TrustMode.ALLOW_LIST
                    && configuredAllowList != null
                    && TlsChecksProjection.hasAllowListPath(configuredAllowList)) {
                // Logged once at start, not per connect. The file is read only under
                // trustMode=ALLOW_LIST, so an allow-list configured under any other trust mode is
                // inert - and an operator who believes they have pinned a specific set of
                // certificates is in fact trusting a much wider one. Deliberately not an error: the
                // trust mode that is in effect was written explicitly and is enforced in full, so the
                // adapter is not less safe than that configuration asks for, only less safe than the
                // operator probably intends.
                log.warn(
                        "OPC UA adapter endpoint '{}': a certificate allow-list is configured ('{}') but the "
                                + "effective trust mode is {}, which never reads it. No server certificate is "
                                + "pinned by its fingerprint. Set tlsChecks=SELF_SIGNED or "
                                + "tlsChecksFull.trustMode=ALLOW_LIST to enforce the allow-list, or remove the "
                                + "'allowList' element.",
                        endpointUri,
                        configuredAllowList.path(),
                        checks.trustMode());
            }
        }

        final Keystore keystore = adapterConfig.getTls().keystore();
        KeystoreUtil.KeyPairWithChain keyPairWithChain = null;
        // The component is declared non-null, but a keystore element whose <path> child is missing
        // (or misspelled, and therefore dropped by the unknown-setting handling core applies to this
        // record) binds a null anyway. Without this guard that null surfaces as a NullPointerException
        // at adapter start instead of a configuration error naming the element to fix.
        // Blank counts as absent here for the same reason it does on the truststore above: a present
        // element with an empty <path> used to skip every guard below and leave the adapter with no
        // client certificate, which surfaces as a connection failure under a security policy that
        // needs one, or under X509 authentication - far from the element that caused it.
        if (keystore != null && isBlank(keystore.path())) {
            return Failure.of("Keystore is configured but has no 'path'. Add "
                    + "<keystore><path>...</path></keystore>, or remove the keystore element entirely.");
        }
        if (keystore != null && keystore.password() == null) {
            return Failure.of("Keystore is configured but has no 'password'. Add "
                    + "<keystore><password>...</password></keystore>.");
        }
        if (keystore != null && keystore.privateKeyPassword() == null) {
            return Failure.of("Keystore is configured but has no 'privateKeyPassword'. Add "
                    + "<keystore><privateKeyPassword>...</privateKeyPassword></keystore>.");
        }
        if (keystore != null) {
            final var kpWithChain = getKeyPairWithChain(keystore);
            if (kpWithChain.isEmpty()) {
                return Failure.of("Failed to load keypair with chain from keystore, check keystore configuration");
            }
            keyPairWithChain = kpWithChain.get();
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
        return tlsEnabled && effectiveChecks.trustMode() == TrustMode.ANY_CERT;
    }

    /** A path that is null, empty or whitespace-only is no path at all. */
    private static boolean isBlank(final @Nullable String path) {
        return path == null || path.isBlank();
    }

    private static @NotNull Optional<List<X509Certificate>> getTrustedCerts(@Nullable final Truststore truststore) {
        // A blank path and a null password are ruled out by the caller's guards, which name the
        // missing element in the start-up failure instead of the generic missing-or-unreadable
        // message below. Only a genuinely absent truststore reaches the cacerts fallback.
        if (truststore != null) {
            final File truststoreFile = new File(truststore.path());
            if (!truststoreFile.exists() || !truststoreFile.canRead()) {
                log.error(
                        "Truststore configuration is not valid. Truststore file does not exist or is not readable: {}",
                        truststoreFile.getPath());
                return Optional.empty();
            }

            if (log.isDebugEnabled()) {
                log.debug("Loading truststore from path: {}", truststore.path());
            }
            final String trustStorePath = truststore.path();
            final String trustStorePassword = truststore.password();
            return Optional.of(KeystoreUtil.getCertificatesFromTruststore("JKS", trustStorePath, trustStorePassword));
        }

        log.info("OPC UA adapter has no user truststore configured; falling back to JVM cacerts. "
                + "If the server presents a self-signed certificate that does not chain to a public CA, "
                + "use tlsChecks=SELF_SIGNED to trust it by fingerprint instead.");
        return Optional.of(KeystoreUtil.getCertificatesFromDefaultTruststore());
    }

    /**
     * Builds the chain-validating validator for {@code trustMode=CHAIN}. The Milo
     * {@link ValidationCheck} set is assembled from the five non-trust axes, one contribution each, so
     * a change on one axis cannot disturb another.
     */
    private static @NotNull CertificateValidator createServerCertificateValidator(
            final @NotNull List<X509Certificate> trustedCerts, final @NotNull EffectiveChecks checks) {
        return new DefaultClientCertificateValidator(
                new CertificateTrustListManager(trustedCerts),
                toValidationChecks(checks),
                new MemoryCertificateQuarantine());
    }

    /**
     * Maps the effective axes onto the Milo optional-check set. Package-private so the mapping can be
     * asserted directly against the check sets the legacy presets have always produced.
     */
    static @NotNull Set<ValidationCheck> toValidationChecks(final @NotNull EffectiveChecks checks) {
        final EnumSet<ValidationCheck> validationChecks = EnumSet.noneOf(ValidationCheck.class);
        if (checks.isSanUri()) {
            validationChecks.add(ValidationCheck.APPLICATION_URI);
        }
        if (checks.isHostname()) {
            validationChecks.add(ValidationCheck.HOSTNAME);
        }
        if (checks.isValidity()) {
            validationChecks.add(ValidationCheck.VALIDITY);
        }
        if (checks.isRevocation()) {
            validationChecks.add(ValidationCheck.REVOCATION);
        }
        if (checks.isRevocationLists()) {
            validationChecks.add(ValidationCheck.REVOCATION_LISTS);
        }
        if (checks.isKeyUsage()) {
            validationChecks.add(ValidationCheck.KEY_USAGE_END_ENTITY);
        }
        if (checks.isExtendedKeyUsage()) {
            validationChecks.add(ValidationCheck.EXTENDED_KEY_USAGE_END_ENTITY);
        }
        return Set.copyOf(validationChecks);
    }

    private static @NotNull Optional<KeystoreUtil.KeyPairWithChain> getKeyPairWithChain(
            final @NotNull Keystore keystore) {
        final File keystoreFile = new File(keystore.path());
        if (!keystoreFile.exists()) {
            log.error("Keystore file {} does not exist", keystoreFile.getAbsolutePath());
            return Optional.empty();
        }
        if (!keystoreFile.canRead()) {
            log.error("Keystore file {} is not readable", keystoreFile.getAbsolutePath());
            return Optional.empty();
        }
        return Optional.of(KeystoreUtil.getKeysFromKeystore(
                "JKS", keystore.path(), keystore.password(), keystore.privateKeyPassword()));
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
