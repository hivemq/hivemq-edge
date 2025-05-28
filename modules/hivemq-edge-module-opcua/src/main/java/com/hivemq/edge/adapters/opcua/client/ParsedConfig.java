package com.hivemq.edge.adapters.opcua.client;

import com.hivemq.edge.adapters.opcua.config.*;
import com.hivemq.edge.adapters.opcua.security.CertificateTrustListManager;
import com.hivemq.edge.adapters.opcua.util.KeystoreUtil;
import com.hivemq.edge.adapters.opcua.util.result.Failure;
import com.hivemq.edge.adapters.opcua.util.result.Result;
import com.hivemq.edge.adapters.opcua.util.result.Success;
import org.eclipse.milo.opcua.sdk.client.identity.*;
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
import java.util.*;

public record ParsedConfig(boolean tlsEnabled, KeystoreUtil.KeyPairWithChain keyPairWithChain,
                    CertificateValidator clientCertificateValidator, IdentityProvider identityProvider) {

    private static final Logger log = LoggerFactory.getLogger(ParsedConfig.class);

    public static Result<ParsedConfig, String> fromConfig(final OpcUaSpecificAdapterConfig adapterConfig) {
        final boolean tlsEnabled = adapterConfig.getTls().isEnabled();
        final var keystore = adapterConfig.getTls().getKeystore();
        final var customKeystoreDefined = keystore != null && !keystore.getPath().isBlank();

        CertificateValidator certificateValidator = null;
        if (tlsEnabled) {
            final var truststore = adapterConfig.getTls().getTruststore();
            final var certOptional = getTrustedCerts(truststore)
                    .map(ParsedConfig::createServerCertificateValidator);
            if(certOptional.isEmpty()) {
                return Failure.of("Failed to create certificate validator, check truststore configuration");
            }
            certificateValidator = certOptional.get();
        }

        KeystoreUtil.KeyPairWithChain keyPairWithChain = null;
        if (customKeystoreDefined) {
            final var tempKeyPairWithChain = getKeyPairWithChain(keystore);
            if(tempKeyPairWithChain.isEmpty()) {
                return Failure.of("Failed to load keypair with chain from keystore, check keystore configuration");
            }
            keyPairWithChain = tempKeyPairWithChain.get();
        }

        final IdentityProvider identityProvider;
        final Optional<IdentityProvider> tempIdentityProvider;
        if (customKeystoreDefined) {
            tempIdentityProvider = createIdentityProvider(tlsEnabled, keyPairWithChain, adapterConfig.getAuth());

        } else {
            tempIdentityProvider = createIdentityProvider(tlsEnabled, null, adapterConfig.getAuth());
        }
        if(tempIdentityProvider.isEmpty()) {
            return Failure.of("Failed to create identity provider, check authentication configuration");
        }
        identityProvider = tempIdentityProvider.get();

        return Success.of(new ParsedConfig(tlsEnabled, keyPairWithChain, certificateValidator, identityProvider));
    }

    private static @NotNull Optional<List<X509Certificate>> getTrustedCerts(@Nullable final Truststore truststore) {
        final var noTruststoreProvided = truststore == null || truststore.getPath().isBlank();

        if (!noTruststoreProvided) {
            final File truststoreFile = new File(truststore.getPath());
            if (!truststoreFile.exists() || !truststoreFile.canRead()) {
                log.error(
                        "Truststore configuration is not valid. Truststore file does not exist or is not readable: {}",
                        truststoreFile.getPath());
                return Optional.empty();
            }
        }

        if (noTruststoreProvided) {
            log.debug("Using default truststore");
            return Optional.of(KeystoreUtil.getCertificatesFromDefaultTruststore());
        } else {
            log.debug("Loading truststore from path: {}", truststore.getPath());
            final String trustStorePath = truststore.getPath();
            final String trustStorePassword = truststore.getPassword();
            return Optional.of(KeystoreUtil.getCertificatesFromTruststore("JKS", trustStorePath, trustStorePassword));
        }
    }

    private static@NotNull CertificateValidator createServerCertificateValidator(@NotNull final List<X509Certificate> trustedCerts) {
        final CertificateTrustListManager trustListManager = new CertificateTrustListManager(trustedCerts);

        final var validationChecks =
                Set.of(ValidationCheck.VALIDITY, ValidationCheck.REVOCATION, ValidationCheck.REVOCATION_LISTS);

        return new DefaultClientCertificateValidator(trustListManager,
                validationChecks,
                new MemoryCertificateQuarantine());
    }

    private static Optional<KeystoreUtil.KeyPairWithChain> getKeyPairWithChain(final @NotNull Keystore keystore) {

        final File keystoreFile = new File(keystore.getPath());
        if (!keystoreFile.exists()) {
            log.error("Keystore file {} does not exist", keystoreFile.getAbsolutePath());
            return Optional.empty();
        } else if (!keystoreFile.canRead()) {
            log.error("Keystore file {} is not readable", keystoreFile.getAbsolutePath());
            return Optional.empty();
        }

        return Optional.of(KeystoreUtil.getKeysFromKeystore("JKS",
                keystore.getPath(),
                keystore.getPassword(),
                keystore.getPrivateKeyPassword()));
    }

    private static Optional<IdentityProvider> createIdentityProvider(
            final boolean tlsEnabled,
            final @Nullable KeystoreUtil.KeyPairWithChain keyPairWithChain,
            final @Nullable Auth auth) {
        log.debug(
                "Configuring Authentication with auth {} tlsEnabled {} and keyPairWithChain {}",
                auth != null,
                tlsEnabled,
                keyPairWithChain != null);
        final List<IdentityProvider> identityProviderBuilder = new ArrayList<>();

        if (auth != null) {
            final X509Auth x509Auth = auth.getX509Auth();
            final boolean x509AuthEnabled = x509Auth != null && x509Auth.isEnabled();
            if(x509AuthEnabled) {
                if (!tlsEnabled) {
                    log.error(
                            "X509 authentication is enabled but TLS is not enabled. X509 authentication will not work.");
                    return Optional.empty();
                } else if (keyPairWithChain == null) {
                    log.error("X509 authentication is enabled but keystore for TLS is not available");
                    return Optional.empty();
                } else {
                    log.debug("X509 authentication is enabled");
                    identityProviderBuilder.add(new X509IdentityProvider(Arrays.asList(keyPairWithChain.certificateChain()),
                            keyPairWithChain.privateKey()));
                }
            }

            if (auth.getBasicAuth() != null) {
                log.debug("Basic authentication is enabled");
                final BasicAuth basicAuth = auth.getBasicAuth();
                identityProviderBuilder.add(new UsernameProvider(basicAuth.getUsername(), basicAuth.getPassword()));
            }
        }

        final List<IdentityProvider> identityProviders = List.copyOf(identityProviderBuilder);
        if (identityProviders.size() == 1) {
            log.info("Using single identity provider: {}",
                    identityProviders.get(0).getClass().getSimpleName());
            return Optional.of(identityProviders.get(0));
        } else if (identityProviders.size() > 1) {
            log.info("Using composite identity provider");
            return Optional.of(new CompositeProvider(identityProviders));
        } else {
            log.info("Using default anonymous identity provider");
            return Optional.of(new AnonymousProvider());
        }
    }
}
