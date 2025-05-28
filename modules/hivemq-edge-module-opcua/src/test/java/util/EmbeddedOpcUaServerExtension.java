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
package util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.milo.opcua.sdk.server.EndpointConfig;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.AnonymousIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultApplicationGroup;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultServerCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.MemoryCertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.security.MemoryCertificateStore;
import org.eclipse.milo.opcua.stack.core.security.MemoryTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.RsaSha256CertificateFactory;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransport;
import org.eclipse.milo.opcua.stack.transport.server.tcp.OpcTcpServerTransportConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;

public class EmbeddedOpcUaServerExtension implements BeforeEachCallback, AfterEachCallback {

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }

    private int bindPort;
    private @Nullable OpcUaServer opcUaServer;
    private @Nullable TestNamespace testNamespace;

    private static @NotNull X509Certificate generateCert(final KeyPair keyPair) throws Exception {
        final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(new X500Name(
                "CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                BigInteger.valueOf(123456789),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 10000),
                new X500Name(
                        "CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                keyPair.getPublic());

        final ContentSigner contentSigner =
                new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateBuilder.build(contentSigner));
    }

    private static @NotNull KeyPair createKeyPair() throws InvalidKeySpecException, NoSuchAlgorithmException {
        final RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
        gen.init(new RSAKeyGenerationParameters(BigInteger.valueOf(3), new SecureRandom(), 1024, 80));

        final AsymmetricCipherKeyPair keypair = gen.generateKeyPair();
        final RSAKeyParameters publicKey = (RSAKeyParameters) keypair.getPublic();
        final RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters) keypair.getPrivate();
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        return new KeyPair(keyFactory.generatePublic(new RSAPublicKeySpec(publicKey.getModulus(),
                publicKey.getExponent())),
                keyFactory.generatePrivate(new RSAPrivateCrtKeySpec(publicKey.getModulus(),
                        publicKey.getExponent(),
                        privateKey.getExponent(),
                        privateKey.getP(),
                        privateKey.getQ(),
                        privateKey.getDP(),
                        privateKey.getDQ(),
                        privateKey.getQInv())));
    }

    @Override
    public void beforeEach(final @NotNull ExtensionContext context) throws Exception {
        bindPort = RandomPortGenerator.get();
        final KeyPair keyPair = createKeyPair();
        final X509Certificate certificate = generateCert(keyPair);

        final EndpointConfig.Builder epBuilder = EndpointConfig.newBuilder()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindAddress("127.0.0.1")
                .setBindPort(bindPort)
                .setHostname("127.0.0.1")
                .setPath("opcuatest")
                .setCertificate(certificate)
                .addTokenPolicies()
                .addTokenPolicies(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
                        OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME,
                        OpcUaServerConfig.USER_TOKEN_POLICY_X509);

        final List<SecurityPolicy> securityPolicies =
                List.of(SecurityPolicy.None, SecurityPolicy.Basic256Sha256, SecurityPolicy.Aes256_Sha256_RsaPss);

        final LinkedHashSet<EndpointConfig> endpointConfigurations = new LinkedHashSet<>();
        for (final SecurityPolicy securityPolicy : securityPolicies) {
            if (securityPolicy == SecurityPolicy.None) {
                endpointConfigurations.add(epBuilder.copy()
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None)
                        .build());
            } else {
                endpointConfigurations.add(epBuilder.copy()
                        .setSecurityPolicy(securityPolicy)
                        .setSecurityMode(MessageSecurityMode.Sign)
                        .build());
                endpointConfigurations.add(epBuilder.copy()
                        .setSecurityPolicy(securityPolicy)
                        .setSecurityMode(MessageSecurityMode.SignAndEncrypt)
                        .build());
            }
        }

        final var trustListManager = new MemoryTrustListManager();
        trustListManager.addTrustedCertificate(certificate);
        final var certificateQuarantine = new MemoryCertificateQuarantine();
        final var defaultGroup = DefaultApplicationGroup.createAndInitialize(trustListManager,
                new MemoryCertificateStore(),
                new RsaSha256CertificateFactory() {
                    @Override
                    protected @NotNull KeyPair createRsaSha256KeyPair() {
                        return keyPair;
                    }

                    @Override
                    protected @NotNull X509Certificate @NotNull [] createRsaSha256CertificateChain(final @NotNull KeyPair keyPair) {
                        // For a self-signed certificate, the chain consists of just the certificate itself
                        return new X509Certificate[]{certificate};
                    }
                },
                new DefaultServerCertificateValidator(trustListManager, certificateQuarantine));

        final OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
                .setIdentityValidator(new CompositeValidator(new AnonymousIdentityValidator(),
                        new UsernameIdentityValidator(auth -> "testuser".equals(auth.getUsername()) &&
                                "testpass".equals(auth.getPassword())),
                        new X509IdentityValidator(cert -> true)))
                .setEndpoints(endpointConfigurations)
                .setCertificateManager(new DefaultCertificateManager(certificateQuarantine, defaultGroup))
                .build();

        opcUaServer = new OpcUaServer(serverConfig, transportProfile -> {
            assert transportProfile == TransportProfile.TCP_UASC_UABINARY;
            return new OpcTcpServerTransport(OpcTcpServerTransportConfig.newBuilder().build());
        });
        testNamespace = new TestNamespace(opcUaServer);
        testNamespace.startup();
        opcUaServer.startup().get();
    }

    @Override
    public void afterEach(final @NotNull ExtensionContext context) throws Exception {
        if (opcUaServer != null) {
            opcUaServer.shutdown().get();
        }
    }

    public @NotNull String getServerUri() {
        return "opc.tcp://127.0.0.1:" + bindPort + "/opcuatest";
    }

    public @Nullable TestNamespace getTestNamespace() {
        return testNamespace;
    }
}
