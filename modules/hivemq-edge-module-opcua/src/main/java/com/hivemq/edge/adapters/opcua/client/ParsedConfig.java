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
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.security.CertificateTrustListManager;
import com.hivemq.edge.adapters.opcua.util.KeystoreUtil;
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

import java.io.File;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public record ParsedConfig(boolean tlsEnabled, KeystoreUtil.KeyPairWithChain keyPairWithChain,
                           CertificateValidator clientCertificateValidator, IdentityProvider identityProvider) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(ParsedConfig.class);

    public static Result<ParsedConfig, String> fromConfig(final OpcUaSpecificAdapterConfig adapterConfig) {
        final boolean tlsEnabled = adapterConfig.getTls().enabled();

        CertificateValidator certValidator = null;
        if (tlsEnabled) {
            final var truststore = adapterConfig.getTls().truststore();
            final var certOptional = getTrustedCerts(truststore).map(ParsedConfig::createServerCertificateValidator);
            if (certOptional.isEmpty()) {
                return Failure.of("Failed to create certificate validator, check truststore configuration");
            }
            certValidator = certOptional.get();
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

        return Success.of(new ParsedConfig(tlsEnabled, keyPairWithChain, certValidator, identityProvider.get()));
    }

    private static @NotNull Optional<List<X509Certificate>> getTrustedCerts(@Nullable final Truststore truststore) {
        if (truststore != null && !truststore.path().isBlank()) {
            final File truststoreFile = new File(truststore.path());
            if (!truststoreFile.exists() || !truststoreFile.canRead()) {
                log.error("Truststore configuration is not valid. Truststore file does not exist or is not readable: {}",
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

        if (log.isDebugEnabled()) {
            log.debug("Using default truststore");
        }
        return Optional.of(KeystoreUtil.getCertificatesFromDefaultTruststore());
    }

    private static @NotNull CertificateValidator createServerCertificateValidator(final @NotNull List<X509Certificate> trustedCerts) {
        return new DefaultClientCertificateValidator(new CertificateTrustListManager(trustedCerts),
                Set.of(ValidationCheck.VALIDITY, ValidationCheck.REVOCATION, ValidationCheck.REVOCATION_LISTS),
                new MemoryCertificateQuarantine());
    }

    private static @NotNull Optional<KeystoreUtil.KeyPairWithChain> getKeyPairWithChain(final @NotNull Keystore keystore) {
        final File keystoreFile = new File(keystore.path());
        if (!keystoreFile.exists()) {
            log.error("Keystore file {} does not exist", keystoreFile.getAbsolutePath());
            return Optional.empty();
        }
        if (!keystoreFile.canRead()) {
            log.error("Keystore file {} is not readable", keystoreFile.getAbsolutePath());
            return Optional.empty();
        }
        return Optional.of(KeystoreUtil.getKeysFromKeystore("JKS",
                keystore.path(),
                keystore.password(),
                keystore.privateKeyPassword()));
    }

    private static @NotNull Optional<IdentityProvider> createIdentityProvider(
            final boolean tlsEnabled,
            final @Nullable KeystoreUtil.KeyPairWithChain keyPairWithChain,
            final @Nullable Auth auth) {
        if (log.isDebugEnabled()) {
            log.debug("Configuring Authentication with auth {} tlsEnabled {} and keyPairWithChain {}",
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
                identityProviders.add(new X509IdentityProvider(Arrays.asList(keyPairWithChain.certificateChain()),
                        keyPairWithChain.privateKey()));
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
            log.info("Using single identity provider: {}", singleProvider.getClass().getSimpleName());
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
