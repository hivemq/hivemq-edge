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

import com.google.common.collect.ImmutableList;
import com.hivemq.edge.adapters.opcua.config.Auth;
import com.hivemq.edge.adapters.opcua.config.BasicAuth;
import com.hivemq.edge.adapters.opcua.config.Keystore;
import com.hivemq.edge.adapters.opcua.config.OpcUaSpecificAdapterConfig;
import com.hivemq.edge.adapters.opcua.config.Tls;
import com.hivemq.edge.adapters.opcua.config.Truststore;
import com.hivemq.edge.adapters.opcua.config.X509Auth;
import com.hivemq.edge.adapters.opcua.security.CertificateTrustListManager;
import com.hivemq.edge.adapters.opcua.util.KeystoreUtil;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfig;
import org.eclipse.milo.opcua.sdk.client.api.config.OpcUaClientConfigBuilder;
import org.eclipse.milo.opcua.sdk.client.api.identity.CompositeProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.X509IdentityProvider;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class OpcUaClientConfigurator implements Function<OpcUaClientConfigBuilder, OpcUaClientConfig> {

    private final @NotNull OpcUaSpecificAdapterConfig adapterConfig;
    private final @NotNull String adapterId;

    public OpcUaClientConfigurator(
            final @NotNull OpcUaSpecificAdapterConfig adapterConfig, final @NotNull String adapterId) {
        this.adapterConfig = adapterConfig;
        this.adapterId = adapterId;
    }

    @Override
    public @NotNull OpcUaClientConfig apply(final OpcUaClientConfigBuilder opcUaClientConfigBuilder) {

        opcUaClientConfigBuilder.setApplicationName(LocalizedText.english("HiveMQ Edge"));
        opcUaClientConfigBuilder.setApplicationUri("urn:hivemq:edge:client");
        opcUaClientConfigBuilder.setProductUri("https://github.com/hivemq/hivemq-edge");
        opcUaClientConfigBuilder.setSessionName(() -> "HiveMQ Edge " + adapterId);

        final Tls tlsConfig = adapterConfig.getTls();
        final boolean tlsEnabled = tlsConfig != null && tlsConfig.isEnabled();
        final boolean keystoreAvailable = checkKeystoreAvailable(tlsConfig, tlsEnabled);
        final KeystoreUtil.KeyPairWithChain keyPairWithChain =
                configureTls(opcUaClientConfigBuilder, tlsConfig, tlsEnabled, keystoreAvailable);

        if (checkAuthEnabled()) {
            configureIdentityProvider(opcUaClientConfigBuilder, tlsEnabled, keyPairWithChain);
        }

        if (keyPairWithChain != null) {
            opcUaClientConfigBuilder.setKeyPair(new KeyPair(keyPairWithChain.getPublicKey().getPublicKey(),
                    keyPairWithChain.getPrivateKey()));
        }

        return opcUaClientConfigBuilder.build();
    }


    private @Nullable KeystoreUtil.KeyPairWithChain configureTls(
            final @NotNull OpcUaClientConfigBuilder opcUaClientConfigBuilder,
            final @NotNull Tls tlsConfig,
            final boolean tlsEnabled,
            final boolean keystoreAvailable) {
        KeystoreUtil.KeyPairWithChain keyPairWithChain = null;
        if (tlsEnabled) {

            //trusted certs, either from configured truststore or system default
            final DefaultClientCertificateValidator certificateValidator = createServerCertificateValidator(tlsConfig);
            opcUaClientConfigBuilder.setCertificateValidator(certificateValidator);

            if (keystoreAvailable) {
                final Keystore keystoreConfig = adapterConfig.getTls().getKeystore();
                //noinspection DataFlowIssue : alreay checked in checkKeystoreAvailable
                keyPairWithChain = KeystoreUtil.getKeysFromKeystore("JKS",
                        keystoreConfig.getPath(),
                        Objects.requireNonNullElse(keystoreConfig.getPassword(), ""),
                        Objects.requireNonNullElse(keystoreConfig.getPrivateKeyPassword(), ""));

                opcUaClientConfigBuilder.setCertificate(keyPairWithChain.getPublicKey());
                opcUaClientConfigBuilder.setCertificateChain(keyPairWithChain.getCertificateChain());
            }
        }
        return keyPairWithChain;
    }

    private boolean checkAuthEnabled() {
        //check that at least one auth method (Basic or X509) is enabled
        final Auth auth = adapterConfig.getAuth();
        return auth != null &&
                (auth.getBasicAuth() != null || (auth.getX509Auth() != null && auth.getX509Auth().isEnabled()));
    }

    private void configureIdentityProvider(
            final @NotNull OpcUaClientConfigBuilder opcUaClientConfigBuilder,
            final boolean tlsEnabled,
            final @Nullable KeystoreUtil.KeyPairWithChain keyPairWithChain) {

        final ImmutableList.Builder<IdentityProvider> identityProviderBuilder = ImmutableList.builder();
        final Auth auth = adapterConfig.getAuth();

        if (auth != null) {
            final X509Auth x509Auth = auth.getX509Auth();
            final boolean x509AuthEnabled = x509Auth != null && x509Auth.isEnabled();
            if (x509AuthEnabled && tlsEnabled && keyPairWithChain != null) {
                identityProviderBuilder.add(new X509IdentityProvider(Arrays.asList(keyPairWithChain.getCertificateChain()),
                        keyPairWithChain.getPrivateKey()));
            }

            if (auth.getBasicAuth() != null) {
                final BasicAuth basicAuth = auth.getBasicAuth();
                identityProviderBuilder.add(new UsernameProvider(basicAuth.getUsername(), basicAuth.getPassword()));
            }
        }

        final ImmutableList<IdentityProvider> identityProviders = identityProviderBuilder.build();
        if (identityProviders.size() == 1) {
            opcUaClientConfigBuilder.setIdentityProvider(identityProviders.get(0));
        } else if (identityProviders.size() > 1) {
            opcUaClientConfigBuilder.setIdentityProvider(new CompositeProvider(identityProviders));
        }
    }

    @NotNull
    private DefaultClientCertificateValidator createServerCertificateValidator(@NotNull final Tls tlsConfig) {
        final List<X509Certificate> trustedCerts;
        final boolean truststoreAvailable = checkTruststoreAvailable(tlsConfig);
        if (truststoreAvailable) {
            //if custom truststore is set
            //noinspection DataFlowIssue nullability is checked in checkTruststoreAvailable()
            final String trustStorePath = tlsConfig.getTruststore().getPath();
            final String trustStorePassword = tlsConfig.getTruststore().getPassword();
            trustedCerts = KeystoreUtil.getCertificatesFromTruststore("JKS", trustStorePath, trustStorePassword);
        } else {
            trustedCerts = KeystoreUtil.getCertificatesFromDefaultTruststore();
        }
        final CertificateTrustListManager trustListManager = new CertificateTrustListManager(trustedCerts);
        final DefaultClientCertificateValidator certificateValidator = new DefaultClientCertificateValidator(
                trustListManager,
                Set.of(ValidationCheck.VALIDITY, ValidationCheck.REVOCATION, ValidationCheck.REVOCATION_LISTS));
        return certificateValidator;
    }

    private boolean checkTruststoreAvailable(final @Nullable Tls tlsConfig) {
        final boolean tlsEnabled = tlsConfig != null && tlsConfig.isEnabled();
        if (!tlsEnabled) {
            return false;
        }

        final Truststore truststore = tlsConfig.getTruststore();
        if (truststore == null || truststore.getPath() == null || truststore.getPath().isBlank()) {
            return false;
        }

        final File truststoreFile = new File(truststore.getPath());
        return truststoreFile.exists() && truststoreFile.canRead();
    }

    private static boolean checkKeystoreAvailable(
            final @Nullable Tls tlsConfig, final boolean tlsEnabled) {
        if (!tlsEnabled) {
            return false;
        }

        final Keystore keystore = tlsConfig.getKeystore();
        if (keystore == null || keystore.getPath() == null || keystore.getPath().isBlank()) {
            return false;
        }

        final File keystoreFile = new File(keystore.getPath());
        return keystoreFile.exists() && keystoreFile.canRead();
    }
}
