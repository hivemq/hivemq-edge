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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
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
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.nodes.UaFolderNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.function.Supplier;

public class EmbeddedOpcUaServerExtension implements BeforeEachCallback, AfterEachCallback {

    private int bindPort;
    private @Nullable OpcUaServer opcUaServer;
    private TestNamespace testNamespace;

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void beforeEach(final @NotNull ExtensionContext context) throws Exception {
        bindPort = RandomPortGenerator.get();
        final KeyPair keyPair = createKeyPair();
        final X509Certificate certificate = generateCert(keyPair);
        final EndpointConfiguration.Builder epBuilder = EndpointConfiguration.newBuilder()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindAddress("127.0.0.1")
                .setBindPort(bindPort)
                .setHostname("127.0.0.1")
                .setPath("opcuatest")
                .setCertificate(certificate)
                .addTokenPolicies(OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS,
                        OpcUaServerConfig.USER_TOKEN_POLICY_USERNAME,
                        OpcUaServerConfig.USER_TOKEN_POLICY_X509);

        final List<SecurityPolicy> securityPolicies =
                List.of(SecurityPolicy.None, SecurityPolicy.Basic256Sha256, SecurityPolicy.Aes256_Sha256_RsaPss);

        final LinkedHashSet<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();
        for (SecurityPolicy securityPolicy : securityPolicies) {
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

        final OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
                .setIdentityValidator(new CompositeValidator(new UsernameIdentityValidator(true,
                        auth -> "testuser".equals(auth.getUsername()) && "testpass".equals(auth.getPassword())),
                        new X509IdentityValidator(cert -> true)))
                .setEndpoints(endpointConfigurations)
                .setCertificateManager(new DefaultCertificateManager(keyPair, certificate))
                .build();

        opcUaServer = new OpcUaServer(serverConfig);

        testNamespace = new TestNamespace(opcUaServer);
        testNamespace.startup();

        opcUaServer.startup().get();
    }


    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        if (opcUaServer != null) {
            opcUaServer.shutdown().get();
        }
    }


    public int getBindPort() {
        return bindPort;
    }

    public @NotNull OpcUaServer getOpcUaServer() {
        return opcUaServer;
    }


    public @NotNull OpcUaClient createClient() throws UaException {

        return OpcUaClient.create(getServerUri(),
                endpointDescriptions -> endpointDescriptions.stream()
                        .filter(endpointDescription -> endpointDescription.getSecurityPolicyUri()
                                .equals(SecurityPolicy.None.getUri()))
                        .findFirst(),
                opcUaClientConfigBuilder -> {
                    opcUaClientConfigBuilder.setApplicationName(LocalizedText.english("Test-Client"));
                    opcUaClientConfigBuilder.setSessionName(() -> "test-" + UUID.randomUUID());
                    return opcUaClientConfigBuilder.build();
                });
    }

    public @NotNull String getServerUri() {
        return "opc.tcp://127.0.0.1:" + bindPort + "/opcuatest";
    }

    private @NotNull X509Certificate generateCert(final KeyPair keyPair) throws Exception {

        final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(new X500Name(
                "CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                BigInteger.valueOf(123456789),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 10000),
                new X500Name(
                        "CN=Test commonName, C=DE, O=Test organization, OU=Test Unit, T=Test Title, L=Test locality, ST=Test state"),
                keyPair.getPublic());

        return getCertificate(keyPair, certificateBuilder);
    }

    private @NotNull X509Certificate getCertificate(
            final KeyPair keyPair, final JcaX509v3CertificateBuilder certificateBuilder)
            throws OperatorCreationException, CertificateException {

        Security.addProvider(new BouncyCastleProvider());

        JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA256WithRSAEncryption");
        signerBuilder = signerBuilder.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        final ContentSigner contentSigner = signerBuilder.build(keyPair.getPrivate());

        JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
        converter = converter.setProvider(BouncyCastleProvider.PROVIDER_NAME);

        return converter.getCertificate(certificateBuilder.build(contentSigner));
    }

    private @NotNull KeyPair createKeyPair() throws InvalidKeySpecException, NoSuchAlgorithmException {

        final RSAKeyPairGenerator gen = new RSAKeyPairGenerator();

        gen.init(new RSAKeyGenerationParameters(BigInteger.valueOf(3), new SecureRandom(), 1024, 80));
        final AsymmetricCipherKeyPair keypair = gen.generateKeyPair();

        final RSAKeyParameters publicKey = (RSAKeyParameters) keypair.getPublic();
        final RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters) keypair.getPrivate();

        final PublicKey pubKey = KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getExponent()));

        final PrivateKey privKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new RSAPrivateCrtKeySpec(publicKey.getModulus(),
                        publicKey.getExponent(),
                        privateKey.getExponent(),
                        privateKey.getP(),
                        privateKey.getQ(),
                        privateKey.getDP(),
                        privateKey.getDQ(),
                        privateKey.getQInv()));

        return new KeyPair(pubKey, privKey);
    }

    public @NotNull TestNamespace getTestNamespace() {
        return testNamespace;
    }

    public static class TestNamespace extends ManagedNamespaceWithLifecycle {

        public static final String NS_URI = "urn:hivemq:test:testns";
        private final @NotNull SubscriptionModel subscriptionModel;
        private @Nullable UaFolderNode dynamicFolder;
        private @Nullable UaFolderNode testFolder;

        public TestNamespace(final @NotNull OpcUaServer server) {
            super(server, NS_URI);
            subscriptionModel = new SubscriptionModel(server, this);
            getLifecycleManager().addLifecycle(subscriptionModel);
            getLifecycleManager().addStartupTask(() -> {
                // Create a "HelloWorld" folder and add it to the node manager
                NodeId folderNodeId = newNodeId("TestFolder");

                dynamicFolder = new UaFolderNode(getNodeContext(),
                        folderNodeId,
                        newQualifiedName("DynamicFolder"),
                        LocalizedText.english("DynamicFolder"));

                getNodeManager().addNode(dynamicFolder);

                // Make sure our new folder shows up under the server's Objects folder.
                dynamicFolder.addReference(new Reference(dynamicFolder.getNodeId(),
                        Identifiers.Organizes,
                        Identifiers.ObjectsFolder.expanded(),
                        false));

                testFolder = new UaFolderNode(getNodeContext(),
                        folderNodeId,
                        newQualifiedName("TestFolder"),
                        LocalizedText.english("TestFolder"));

                getNodeManager().addNode(testFolder);

                // Make sure our new folder shows up under the server's Objects folder.
                testFolder.addReference(new Reference(testFolder.getNodeId(),
                        Identifiers.Organizes,
                        Identifiers.ObjectsFolder.expanded(),
                        false));

                addDynamicNodes();

            });
        }

        @Override
        public void onDataItemsCreated(final @NotNull List<DataItem> dataItems) {
            subscriptionModel.onDataItemsCreated(dataItems);
        }

        @Override
        public void onDataItemsModified(final @NotNull List<DataItem> dataItems) {
            subscriptionModel.onDataItemsModified(dataItems);
        }

        @Override
        public void onDataItemsDeleted(final @NotNull List<DataItem> dataItems) {
            subscriptionModel.onDataItemsDeleted(dataItems);
        }

        @Override
        public void onMonitoringModeChanged(final @NotNull List<MonitoredItem> monitoredItems) {
            subscriptionModel.onMonitoringModeChanged(monitoredItems);
        }

        private void addDynamicNodes() {
            final Random random = new Random();
            addDefaultNode("Bool", Identifiers.Boolean, true, random::nextBoolean, newNodeId(10));
            addDefaultNode("Int32", Identifiers.Int32, 50, random::nextInt, newNodeId(11));
            addDefaultNode("Int64", Identifiers.Int64, 5000, random::nextLong, newNodeId(12));
            addDefaultNode("Double", Identifiers.Double, 123.4d, random::nextDouble, newNodeId(13));
            addDefaultNode("String", Identifiers.String, "abc", () -> DateTime.now().toString(), newNodeId("abc"));
        }

        private @NotNull void addDefaultNode(
                final @NotNull String name,
                final @NotNull NodeId typeId,
                final @NotNull Object intialValue,
                final @NotNull Supplier<Object> valueCallback,
                final @NotNull NodeId nodeId) {
            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext()).setNodeId(nodeId)
                    .setAccessLevel(AccessLevel.READ_WRITE)
                    .setBrowseName(newQualifiedName(name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(new Variant(intialValue)));

            node.getFilterChain()
                    .addLast(AttributeFilters.getValue(ctx -> new DataValue(new Variant(valueCallback.get()))));

            getNodeManager().addNode(node);
            dynamicFolder.addOrganizes(node);
        }


        private @NotNull String addTestNode(
                final @NotNull String name,
                final @NotNull NodeId typeId,
                final @NotNull Supplier<Object> valueCallback,
                final @NotNull NodeId nodeId) {
            UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext()).setNodeId(nodeId)
                    .setAccessLevel(AccessLevel.READ_WRITE)
                    .setBrowseName(newQualifiedName(name))
                    .setDisplayName(LocalizedText.english(name))
                    .setDataType(typeId)
                    .setTypeDefinition(Identifiers.BaseDataVariableType)
                    .build();

            node.setValue(new DataValue(new Variant(null)));

            node.getFilterChain()
                    .addLast(AttributeFilters.getValue(ctx -> new DataValue(new Variant(valueCallback.get()))));

            getNodeManager().addNode(node);
            dynamicFolder.addOrganizes(node);

            return nodeId.toParseableString();
        }

        public @NotNull String addNode(
                final @NotNull String name,
                final @NotNull NodeId typeId,
                final @NotNull Supplier<Object> valueCallback,
                final @NotNull String nodeIdPart) {
            return addTestNode(name, typeId, valueCallback, newNodeId(nodeIdPart));
        }

        public @NotNull String addNode(
                final @NotNull String name,
                final @NotNull NodeId typeId,
                final @NotNull Supplier<Object> valueCallback,
                final @NotNull long nodeIdPart) {
            return addTestNode(name, typeId, valueCallback, newNodeId(nodeIdPart));
        }

        public @NotNull String addNode(
                final @NotNull String name,
                final @NotNull NodeId typeId,
                final @NotNull Supplier<Object> valueCallback,
                final @NotNull UUID nodeIdPart) {
            return addTestNode(name, typeId, valueCallback, newNodeId(nodeIdPart));
        }

        public @NotNull String addNode(
                final @NotNull String name,
                final @NotNull NodeId typeId,
                final @NotNull Supplier<Object> valueCallback,
                final @NotNull UInteger nodeIdPart) {
            return addTestNode(name, typeId, valueCallback, newNodeId(nodeIdPart));
        }

        public @NotNull String addNode(
                final @NotNull String name,
                final @NotNull NodeId typeId,
                final @NotNull Supplier<Object> valueCallback,
                final @NotNull ByteString nodeIdPart) {
            return addTestNode(name, typeId, valueCallback, newNodeId(nodeIdPart));
        }

    }
}
