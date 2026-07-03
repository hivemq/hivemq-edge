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

import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.BasicAuth;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TrustLevel;
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.security.CertificateTrustListManager;
import com.hivemq.edge.adapters.opcua.security.TrustAnyIdentityCertificateValidator;
import com.hivemq.edge.adapters.opcua.util.KeystoreUtil;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
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
        boolean trustAnyServerCertificate,
        @Nullable KeystoreUtil.KeyPairWithChain keyPairWithChain,
        @Nullable CertificateValidator clientCertificateValidator,
        @NotNull IdentityProvider identityProvider,
        @Nullable String applicationUri) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ParsedConfig.class);

    public static Result<ParsedConfig, String> fromConfig(final OpcUaSpecificAdapterConfig adapterConfig) {
        final boolean tlsEnabled = adapterConfig.getTls().enabled();
        // tlsChecks/trustLevel are normalized (aliases expanded, never null) inside Tls.
        final TrustLevel trustLevel =
                Objects.requireNonNull(adapterConfig.getTls().trustLevel());
        final TlsChecks identity = Objects.requireNonNull(adapterConfig.getTls().tlsChecks());
        final boolean trustAnyServerCertificate = trustLevel == TrustLevel.TRUST;

        CertificateValidator certValidator = null;
        if (tlsEnabled) {
            switch (trustLevel) {
                case TRUST ->
                    // Accept any server certificate (no chain build). Identity checks, if any, are
                    // still enforced on the presented certificate.
                    certValidator = (identity == TlsChecks.NONE)
                            ? new CertificateValidator.InsecureCertificateValidator()
                            : new TrustAnyIdentityCertificateValidator(
                                    checksApplicationUri(identity), checksHostname(identity));
                case CHAIN, CHAIN_PKI -> {
                    final var truststore = adapterConfig.getTls().truststore();
                    final var trustedCertsOpt = getTrustedCerts(truststore);
                    if (trustedCertsOpt.isEmpty()) {
                        // Reachable only when the user explicitly configured a truststore path that
                        // is missing or unreadable. "No truststore configured" silently falls back
                        // to JVM cacerts inside getTrustedCerts and does NOT land here.
                        return Failure.of("Truststore is configured but the file is missing or unreadable. "
                                + "Either correct the path, leave the truststore unset to use JVM cacerts, "
                                + "or set trustLevel=TRUST to accept any server certificate.");
                    }
                    certValidator = createServerCertificateValidator(trustedCertsOpt.get(), trustLevel, identity);
                }
            }
        }

        final Keystore keystore = adapterConfig.getTls().keystore();
        KeystoreUtil.KeyPairWithChain keyPairWithChain = null;
        if (keystore != null && !keystore.path().isBlank()) {
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
                tlsEnabled,
                trustAnyServerCertificate,
                keyPairWithChain,
                certValidator,
                identityProvider.get(),
                applicationUri));
    }

    private static boolean checksApplicationUri(final @NotNull TlsChecks identity) {
        return identity == TlsChecks.APPLICATION_URI || identity == TlsChecks.APPLICATION_URI_AND_HOSTNAME;
    }

    private static boolean checksHostname(final @NotNull TlsChecks identity) {
        return identity == TlsChecks.HOSTNAME || identity == TlsChecks.APPLICATION_URI_AND_HOSTNAME;
    }

    private static @NotNull Optional<List<X509Certificate>> getTrustedCerts(@Nullable final Truststore truststore) {
        if (truststore != null && !truststore.path().isBlank()) {
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
                + "set trustLevel=TRUST to bypass chain validation.");
        return Optional.of(KeystoreUtil.getCertificatesFromDefaultTruststore());
    }

    /**
     * Builds a chain-validating validator for {@code trustLevel} CHAIN / CHAIN_PKI. The Milo
     * {@link ValidationCheck} set is composed from the two orthogonal knobs: the identity checks
     * ({@code tlsChecks}) and, for CHAIN_PKI, the PKI-hygiene checks (validity, revocation). CHAIN
     * contributes no optional checks beyond the chain build itself.
     */
    private static @NotNull CertificateValidator createServerCertificateValidator(
            final @NotNull List<X509Certificate> trustedCerts,
            final @NotNull TrustLevel trustLevel,
            final @NotNull TlsChecks identity) {
        final EnumSet<ValidationCheck> checks = EnumSet.noneOf(ValidationCheck.class);

        // Identity axis.
        if (checksApplicationUri(identity)) {
            checks.add(ValidationCheck.APPLICATION_URI);
        }
        if (checksHostname(identity)) {
            checks.add(ValidationCheck.HOSTNAME);
        }

        // PKI-hygiene axis (only CHAIN_PKI). Mirrors the legacy STANDARD check set exactly
        // (validity + revocation); key-usage is intentionally NOT enforced so that STANDARD's
        // on-upgrade behavior is preserved for server certs without a KeyUsage extension.
        if (trustLevel == TrustLevel.CHAIN_PKI) {
            checks.add(ValidationCheck.VALIDITY);
            checks.add(ValidationCheck.REVOCATION);
            checks.add(ValidationCheck.REVOCATION_LISTS);
        }

        return new DefaultClientCertificateValidator(
                new CertificateTrustListManager(trustedCerts), Set.copyOf(checks), new MemoryCertificateQuarantine());
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
