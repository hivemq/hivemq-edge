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

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.bridge.config.BridgeTls;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.bridge.mqtt.BridgeInterceptorHandler;
import com.hivemq.bridge.mqtt.BridgeMqttClient;
import com.hivemq.configuration.HivemqId;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import com.hivemq.configuration.info.SystemInformation;
import jakarta.xml.bind.JAXBContext;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import javax.xml.transform.stream.StreamSource;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// Keys, certificates, PEM text, bridge configurations and a loopback TLS handshake for the tests of
/// this package. Every key and certificate is generated per call.
final class TLSFixtures {

    /// Bounds the client's read and the stop of the server thread in [#handshake]. A loopback
    /// handshake takes milliseconds; 5 s absorbs a loaded CI agent.
    static final @NotNull Duration HANDSHAKE_TIMEOUT = Duration.ofSeconds(5);

    private static final @NotNull String BRIDGE_HOST = "broker.example";
    private static final int BRIDGE_PORT = 8883;
    private static final @NotNull String LOOPBACK_HOST = "localhost";
    private static final @NotNull String SERVER_THREAD_NAME = "tls-fixtures-handshake-server";

    private TLSFixtures() {}

    /// A policy that trusts any certificate under the given configuration's checks.
    static @NotNull TLSPolicy anyCertificatePolicy(final @NotNull TLSConfig config) {
        return new TLSPolicy(config, Optional.empty(), Set.of(), Set.of(), List.of());
    }

    /// The MQTT client of a bridge to `broker.example:8883` with the given effective TLS settings and,
    /// where given, the element they were read from.
    static @NotNull BridgeMqttClient bridgeClient(
            final @NotNull String bridgeId,
            final @NotNull TLSConfig effectiveTls,
            final @Nullable BridgeTlsEntity configuredTls) {
        final MqttBridge bridge = new MqttBridge.Builder()
                .withId(bridgeId)
                .withHost(BRIDGE_HOST)
                .withPort(BRIDGE_PORT)
                .withClientId(bridgeId + "-client")
                .withLocalSubscriptions(List.of())
                .withRemoteSubscriptions(List.of())
                .withBridgeTls(new BridgeTls.Builder()
                        .withEffectiveTls(effectiveTls)
                        .withConfiguredTls(configuredTls)
                        .build())
                .build();
        return new BridgeMqttClient(
                mock(SystemInformation.class),
                bridge,
                mock(BridgeInterceptorHandler.class),
                new HivemqId(),
                new MetricRegistry(),
                mock(EventService.class, RETURNS_DEEP_STUBS));
    }

    /// The bridge TLS entity the given `tls` element binds to.
    static @NotNull BridgeTlsEntity bridgeTlsEntity(final @NotNull String tls) throws Exception {
        return JAXBContext.newInstance(BridgeTlsEntity.class)
                .createUnmarshaller()
                .unmarshal(new StreamSource(new StringReader(tls)), BridgeTlsEntity.class)
                .getValue();
    }

    /// A certificate named `CN=<name>`, valid from an hour ago for thirty days. An authority's key
    /// usage permits signing certificates and revocation lists; an end entity has no key usage.
    static @NotNull X509Certificate certificate(
            final @NotNull String name,
            final @NotNull KeyPair subjectKeys,
            final @Nullable X509Certificate issuerCertificate,
            final @NotNull KeyPair signingKeys,
            final boolean authority)
            throws Exception {
        final Instant now = Instant.now();
        return certificate(
                name,
                subjectKeys,
                issuerCertificate,
                signingKeys,
                authority,
                now.minus(Duration.ofHours(1)),
                now.plus(Duration.ofDays(30)),
                authority ? KeyUsage.keyCertSign | KeyUsage.cRLSign : null,
                null);
    }

    /// A certificate named `CN=<name>`, self-signed where `issuerCertificate` is null, with key
    /// identifiers, and with the name as its DNS subject alternative name when it is no authority.
    @SuppressWarnings("JavaUtilDate") // JcaX509v3CertificateBuilder requires java.util.Date.
    static @NotNull X509Certificate certificate(
            final @NotNull String name,
            final @NotNull KeyPair subjectKeys,
            final @Nullable X509Certificate issuerCertificate,
            final @NotNull KeyPair signingKeys,
            final boolean authority,
            final @NotNull Instant notBefore,
            final @NotNull Instant notAfter,
            final @Nullable Integer keyUsage,
            final @Nullable KeyPurposeId extendedKeyUsage)
            throws Exception {
        final X500Principal subject = new X500Principal("CN=" + name);
        final X500Principal issuer = issuerCertificate == null ? subject : issuerCertificate.getSubjectX500Principal();
        final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                BigInteger.valueOf(System.nanoTime()),
                Date.from(notBefore),
                Date.from(notAfter),
                subject,
                subjectKeys.getPublic());
        final JcaX509ExtensionUtils extensions = new JcaX509ExtensionUtils();
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(authority));
        builder.addExtension(
                Extension.subjectKeyIdentifier, false, extensions.createSubjectKeyIdentifier(subjectKeys.getPublic()));
        if (issuerCertificate != null) {
            builder.addExtension(
                    Extension.authorityKeyIdentifier,
                    false,
                    extensions.createAuthorityKeyIdentifier(issuerCertificate));
        }
        if (!authority) {
            builder.addExtension(
                    Extension.subjectAlternativeName,
                    false,
                    new GeneralNames(new GeneralName(GeneralName.dNSName, name)));
        }
        if (keyUsage != null) {
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(keyUsage));
        }
        if (extendedKeyUsage != null) {
            builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(extendedKeyUsage));
        }
        return new JcaX509CertificateConverter()
                .getCertificate(
                        builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(signingKeys.getPrivate())));
    }

    /// A policy that anchors trust at the given certificate and reads the given revocation lists.
    static @NotNull TLSPolicy chainPolicy(
            final @NotNull TLSConfig config,
            final @NotNull List<X509CRL> lists,
            final @NotNull X509Certificate anchor) {
        return new TLSPolicy(config, Optional.empty(), Set.of(new TrustAnchor(anchor, null)), Set.of(), lists);
    }

    /// Connects to a TLS server on a local ephemeral port with the policy's trust manager and, as the
    /// MQTT client library does when the hostname check is on, HTTPS endpoint identification; then
    /// reads, because a server refusing a client certificate under TLS 1.3 says so only after the
    /// client's handshake is done.
    ///
    /// A refusal on either side is the result. Any other client failure is thrown, after the server
    /// thread is stopped, with a failure to stop it suppressed into it.
    static @NotNull HandshakeResult handshake(
            final @NotNull TLSPolicy policy, final @NotNull SSLContext serverContext, final boolean needClientAuth)
            throws Exception {
        final SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(
                null, TLSValidatorBridge.trustManagerFactoryFor(policy).getTrustManagers(), null);
        final AtomicReference<Exception> serverFailure = new AtomicReference<>();
        try (final SSLServerSocket server =
                (SSLServerSocket) serverContext.getServerSocketFactory().createServerSocket(0)) {
            server.setNeedClientAuth(needClientAuth);
            final Thread serving =
                    Thread.ofPlatform().name(SERVER_THREAD_NAME).start(() -> serve(server, serverFailure));
            final IOException clientFailure;
            try {
                clientFailure = connect(
                        clientContext, server.getLocalPort(), policy.config().isHostname());
            } catch (final Throwable primary) {
                final Throwable stopFailure = stopServer(server, serving);
                if (stopFailure != null) {
                    primary.addSuppressed(stopFailure);
                }
                throw primary;
            }
            final Throwable stopFailure = stopServer(server, serving);
            if (stopFailure != null) {
                throw new AssertionError("the TLS test server did not stop", stopFailure);
            }
            return new HandshakeResult(clientFailure, serverFailure.get());
        }
    }

    /// A fresh 2048-bit RSA key pair.
    static @NotNull KeyPair keyPair() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    /// The private key as PEM text.
    static @NotNull String pemKey(final @NotNull KeyPair pair) {
        return pem("PRIVATE KEY", pair.getPrivate().getEncoded());
    }

    /// A TLS server context presenting the broker certificate and its issuer.
    static @NotNull SSLContext serverContext(
            final @NotNull KeyPair keys, final @NotNull X509Certificate broker, final @NotNull X509Certificate issuer)
            throws Exception {
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, null);
        store.setKeyEntry("broker", keys.getPrivate(), new char[0], new X509Certificate[] {broker, issuer});
        final KeyManagerFactory managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        managers.init(store, new char[0]);
        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(managers.getKeyManagers(), null, null);
        return context;
    }

    /// The certificate as PEM text.
    static @NotNull String toPem(final @NotNull X509Certificate certificate) throws Exception {
        return pem("CERTIFICATE", certificate.getEncoded());
    }

    /// The revocation list as PEM text.
    static @NotNull String toPem(final @NotNull X509CRL crl) throws Exception {
        return pem("X509 CRL", crl.getEncoded());
    }

    /// The trust manager a bridge with the given policy hands the TLS handshake.
    static @NotNull X509ExtendedTrustManager trustManager(final @NotNull TLSPolicy policy) throws Exception {
        return (X509ExtendedTrustManager)
                TLSValidatorBridge.trustManagerFactoryFor(policy).getTrustManagers()[0];
    }

    private static @Nullable IOException connect(
            final @NotNull SSLContext context, final int port, final boolean hostname) throws IOException {
        try (final SSLSocket client = (SSLSocket) context.getSocketFactory().createSocket(LOOPBACK_HOST, port)) {
            client.setSoTimeout((int) HANDSHAKE_TIMEOUT.toMillis());
            if (hostname) {
                final SSLParameters parameters = client.getSSLParameters();
                parameters.setEndpointIdentificationAlgorithm("HTTPS");
                client.setSSLParameters(parameters);
            }
            try {
                client.startHandshake();
                client.getInputStream().read();
                return null;
            } catch (final IOException refusal) {
                return refusal;
            }
        }
    }

    private static @NotNull String pem(final @NotNull String type, final byte @NotNull [] der) {
        return "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                        .encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }

    private static void serve(
            final @NotNull SSLServerSocket server, final @NotNull AtomicReference<Exception> failure) {
        try (final SSLSocket connection = (SSLSocket) server.accept()) {
            connection.startHandshake();
            connection.getOutputStream().write(1);
            connection.getOutputStream().flush();
        } catch (final Exception e) {
            failure.set(e);
        }
    }

    /// Closes the listening socket, which ends a server thread still waiting in `accept`, then joins
    /// the thread. Both steps are attempted; the first failure is returned with any later one
    /// suppressed into it, or null when both succeed.
    private static @Nullable Throwable stopServer(
            final @NotNull SSLServerSocket server, final @NotNull Thread serving) {
        Throwable failure = null;
        try {
            server.close();
        } catch (final IOException e) {
            failure = e;
        }
        Throwable joinFailure = null;
        try {
            if (!serving.join(HANDSHAKE_TIMEOUT)) {
                joinFailure = new AssertionError("the TLS test server thread did not end within " + HANDSHAKE_TIMEOUT);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            joinFailure = e;
        }
        if (failure == null) {
            return joinFailure;
        }
        if (joinFailure != null) {
            failure.addSuppressed(joinFailure);
        }
        return failure;
    }

    /// What each side of [#handshake] failed with, null for a side that completed.
    record HandshakeResult(
            @Nullable IOException clientFailure, @Nullable Exception serverFailure) {}
}
