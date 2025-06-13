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
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
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
import org.eclipse.milo.opcua.sdk.server.identity.IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultApplicationGroup;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultServerCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.MemoryCertificateQuarantine;
import org.eclipse.milo.opcua.stack.core.security.MemoryCertificateStore;
import org.eclipse.milo.opcua.stack.core.security.MemoryTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.RsaSha256CertificateFactory;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
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
import java.util.Set;

public class EmbeddedOpcUaServerExtension implements BeforeEachCallback, AfterEachCallback {

    public static final @NotNull String NS_URI = "urn:hivemq:edge:opcua:test";

    private static final @NotNull GeneralNames SUBJECT_ALTERNATIVE_NAME = new GeneralNames(new GeneralName[]{
            new GeneralName(GeneralName.uniformResourceIdentifier, EmbeddedOpcUaServerExtension.NS_URI)});
    private static final @NotNull String SERVER_PATH = "/opcua/test";
    private static final @NotNull String BIND_ADDRESS = "127.0.0.1";
    private static final @NotNull String USERNAME = "testuser";
    private static final @NotNull String PASSWORD = "testpass";
    private static final @NotNull IdentityValidator IDENTITY_VALIDATOR =
            new CompositeValidator(new AnonymousIdentityValidator(),
                    new UsernameIdentityValidator(auth -> USERNAME.equals(auth.getUsername()) &&
                            PASSWORD.equals(auth.getPassword())),
                    new X509IdentityValidator(cert -> true));
    private static final @NotNull List<SecurityPolicy> SECURITY_POLICIES =
            List.of(SecurityPolicy.None, SecurityPolicy.Basic256Sha256, SecurityPolicy.Aes256_Sha256_RsaPss);

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }

    private int bindPort;
    private @Nullable OpcUaServer opcUaServer;
    private @Nullable TestNamespace testNamespace;

    private static @NotNull X509Certificate generateServerCertificate(final KeyPair keyPair) throws Exception {
        final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(new X500Name(
                "CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                BigInteger.valueOf(123456789),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 10000),
                new X500Name(
                        "CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                keyPair.getPublic());
        certificateBuilder.addExtension(Extension.subjectAlternativeName, false, SUBJECT_ALTERNATIVE_NAME);
        final ContentSigner contentSigner =
                new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(keyPair.getPrivate());
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(certificateBuilder.build(contentSigner));
    }

    private static @NotNull KeyPair createServerKeyPair() throws InvalidKeySpecException, NoSuchAlgorithmException {
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
        final KeyPair keyPair = createServerKeyPair();
        final X509Certificate certificate = generateServerCertificate(keyPair);

        final var trustManager = new MemoryTrustListManager();
        trustManager.addTrustedCertificate(certificate);
        final var quarantine = new MemoryCertificateQuarantine();
        final OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
                .setEndpoints(createEndpointConfigs(certificate))
                .setIdentityValidator(IDENTITY_VALIDATOR)
                .setApplicationName(LocalizedText.english("HiveMQ OPC UA Test Server"))
                .setProductUri(NS_URI)
                .setBuildInfo(new BuildInfo(NS_URI,
                        "HiveMQ",
                        EmbeddedOpcUaServerExtension.class.getSimpleName(),
                        OpcUaServer.SDK_VERSION,
                        "",
                        DateTime.now()))
                .setApplicationUri(CertificateUtil.getSanUri(certificate)
                        .orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError,
                                "certificate is missing the application URI")))
                .setCertificateManager(new DefaultCertificateManager(quarantine,
                        DefaultApplicationGroup.createAndInitialize(trustManager,
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
                                new DefaultServerCertificateValidator(trustManager, quarantine))))
                .build();

        opcUaServer = new OpcUaServer(serverConfig, transport -> {
            if (transport != TransportProfile.TCP_UASC_UABINARY) {
                throw new RuntimeException("unexpected TransportProfile: " + transport);
            }
            return new OpcTcpServerTransport(OpcTcpServerTransportConfig.newBuilder().build());
        });
        testNamespace = new TestNamespace(opcUaServer);
        testNamespace.startup();
        opcUaServer.startup().get();
    }

    @Override
    public void afterEach(final @NotNull ExtensionContext context) throws Exception {
        if (testNamespace != null) {
            testNamespace.shutdown();
            testNamespace = null;

        }
        if (opcUaServer != null) {
            opcUaServer.shutdown().get();
            opcUaServer = null;

        }
    }

    public @NotNull String getServerUri() {
        return "opc.tcp://127.0.0.1:" + bindPort + SERVER_PATH;
    }

    public @Nullable TestNamespace getTestNamespace() {
        return testNamespace;
    }

    private @NotNull Set<EndpointConfig> createEndpointConfigs(final @NotNull X509Certificate certificate) {
        final EndpointConfig.Builder builder = EndpointConfig.newBuilder()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindAddress(BIND_ADDRESS)
                .setBindPort(bindPort)
                .setHostname(BIND_ADDRESS)
                .setPath(SERVER_PATH)
                .setCertificate(certificate)
                .addTokenPolicies(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
                        OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME,
                        OpcUaServerConfig.USER_TOKEN_POLICY_X509);

        final LinkedHashSet<EndpointConfig> endpoints = new LinkedHashSet<>();
        for (final SecurityPolicy securityPolicy : SECURITY_POLICIES) {
            if (securityPolicy == SecurityPolicy.None) {
                endpoints.add(builder.copy()
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None)
                        .build());
            } else {
                endpoints.add(builder.copy()
                        .setSecurityPolicy(securityPolicy)
                        .setSecurityMode(MessageSecurityMode.Sign)
                        .build());
                endpoints.add(builder.copy()
                        .setSecurityPolicy(securityPolicy)
                        .setSecurityMode(MessageSecurityMode.SignAndEncrypt)
                        .build());
            }
        }
        return endpoints;
    }
}
