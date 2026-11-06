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

import static com.hivemq.security.tls.TLSFixtures.bridgeClient;
import static com.hivemq.security.tls.TLSFixtures.bridgeTlsEntity;
import static com.hivemq.security.tls.TLSFixtures.chainPolicy;
import static com.hivemq.security.tls.TLSFixtures.keyPair;
import static com.hivemq.security.tls.TLSFixtures.trustManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.hivemq.client.mqtt.MqttClientSslConfig;
import com.hivemq.configuration.entity.bridge.BridgeTlsEntity;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateRevokedException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/// Which certification paths a bridge's trust manager accepts: where the path may end, how the dates
/// of its certificates are judged, and when revocation is judged.
///
/// Every certificate is generated per test; truststores are written to a temporary directory.
class TLSValidatorBridgeTrustPathTest {

    private static final @NotNull String BRIDGE_ID = "a-bridge";

    /// The password of the PKCS#12 truststores written here; any non-empty value would do.
    private static final @NotNull String TRUSTSTORE_PASSWORD = "changeit";

    @TempDir
    private @NotNull Path directory;

    /// Proves that an intermediate authority in the truststore is an anchor in its own right: its
    /// root need be neither trusted nor presented.
    @Test
    void anIntermediateAuthorityInTheTruststoreAnchorsThePath() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final X509Certificate broker =
                pki.leaf("localhost", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        final X509ExtendedTrustManager manager = chainManager(StubTLSConfig.chain(), List.of(), pki.intermediate());

        assertThat(catchThrowable(() -> manager.checkServerTrusted(new X509Certificate[] {broker}, "RSA")))
                .isNull();
        assertThat(catchThrowable(() -> manager.checkServerTrusted(
                        new X509Certificate[] {broker, pki.intermediate(), pki.root()}, "RSA")))
                .isNull();
    }

    /// Proves that a broker presenting the root it is anchored at, as many do, is accepted.
    @Test
    void aPresentedRootThatIsTheAnchorIsAccepted() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final X509Certificate broker =
                pki.leaf("localhost", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        final X509ExtendedTrustManager manager = chainManager(StubTLSConfig.chain(), List.of(), pki.root());

        assertThat(catchThrowable(() -> manager.checkServerTrusted(
                        new X509Certificate[] {broker, pki.intermediate(), pki.root()}, "RSA")))
                .isNull();
    }

    /// Proves that a self-signed broker certificate placed in the truststore is trusted, and that a
    /// different certificate with the same name is not.
    @Test
    void aSelfSignedBrokerCertificateInTheTruststoreIsTrustedAndAnImpostorIsNot() throws Exception {
        final Instant now = Instant.now();
        final X509Certificate broker = selfSigned("localhost", now);
        final X509Certificate impostor = selfSigned("localhost", now);
        final X509ExtendedTrustManager manager = chainManager(StubTLSConfig.chain(), List.of(), broker);

        assertThat(catchThrowable(() -> manager.checkServerTrusted(new X509Certificate[] {broker}, "RSA")))
                .isNull();
        assertThat(catchThrowable(() -> manager.checkServerTrusted(new X509Certificate[] {impostor}, "RSA")))
                .isInstanceOf(CertificateException.class);
    }

    /// Proves that a CA-issued broker certificate placed directly in the truststore is trusted
    /// without its issuer, and that one from an authority nobody configured is refused.
    @Test
    void aDirectlyTrustedBrokerCertificateIsAcceptedAndAnUnrelatedOneIsRefused() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final TestPki unrelated = new TestPki(now);
        final X509Certificate broker =
                pki.leaf("localhost", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        final X509Certificate stranger =
                unrelated.leaf("localhost", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        final X509ExtendedTrustManager manager = chainManager(StubTLSConfig.chain(), List.of(), broker);

        assertThat(catchThrowable(() -> manager.checkServerTrusted(new X509Certificate[] {broker}, "RSA")))
                .isNull();
        assertThat(catchThrowable(() ->
                        manager.checkServerTrusted(new X509Certificate[] {stranger, unrelated.intermediate()}, "RSA")))
                .isInstanceOf(CertificateException.class);
    }

    /// Proves that a CA-issued broker certificate placed directly in a bridge's truststore, without
    /// its issuer, is accepted, as the JDK's own trust manager accepts it from the same truststore.
    @Test
    void aBrokerCertificateInTheTruststoreIsTrustedWithoutItsIssuer() throws Exception {
        final TestAuthority authority = authority();
        final X509Certificate broker = broker(authority);
        final KeyStore truststore = truststoreHolding(broker);
        final X509Certificate[] presented = {broker, authority.certificate()};

        assertThat(catchThrowable(() -> jdkTrustManager(truststore).checkServerTrusted(presented, "RSA")))
                .as("the JDK trust manager accepts a broker certificate trusted directly")
                .isNull();
        assertThat(catchThrowable(() -> supersededTrustManager(truststore).checkServerTrusted(presented, "RSA")))
                .as("a broker certificate in the truststore is trusted without its issuer, as the JDK trusts it")
                .isNull();
    }

    /// Proves the same broker and presented chain are accepted when the truststore holds the issuing
    /// authority, so only where the trust is anchored differs.
    @Test
    void aBrokerCertificateIsTrustedThroughItsIssuerInTheTruststore() throws Exception {
        final TestAuthority authority = authority();
        final X509Certificate broker = broker(authority);
        final KeyStore truststore = truststoreHolding(authority.certificate());
        final X509Certificate[] presented = {broker, authority.certificate()};

        assertThat(catchThrowable(() -> supersededTrustManager(truststore).checkServerTrusted(presented, "RSA")))
                .isNull();
    }

    /// Proves that a bridge on the compatibility preset, written by omission or by name, still
    /// accepts an expired broker certificate it trusts directly, because the runtime's path
    /// validator never looked at a trust anchor's dates and the preset keeps what a bridge
    /// validated before.
    @ParameterizedTest
    @ValueSource(strings = {"", "<tls-checks>TLS_BRIDGE_DEFAULT</tls-checks>"})
    void theCompatibilityPresetAcceptsAnExpiredDirectlyTrustedBrokerCertificate(final @NotNull String checks)
            throws Exception {
        final X509Certificate broker = expiredServerCertificate();

        assertThat(catchThrowable(() -> configuredTrustManager(checks, broker)
                        .checkServerTrusted(new X509Certificate[] {broker}, "RSA")))
                .isNull();
    }

    /// Proves that a bridge naming a preset with validity checked refuses the same certificate:
    /// the dates of the broker's own certificate are checked wherever it sits.
    @ParameterizedTest
    @ValueSource(strings = {"STANDARD", "ALL"})
    void anExplicitPresetRefusesAnExpiredDirectlyTrustedBrokerCertificate(final @NotNull String preset)
            throws Exception {
        final X509Certificate broker = expiredServerCertificate();

        assertThat(catchThrowable(() -> configuredTrustManager("<tls-checks>" + preset + "</tls-checks>", broker)
                        .checkServerTrusted(new X509Certificate[] {broker}, "RSA")))
                .isInstanceOf(CertificateException.class);
    }

    /// Proves that `validity=NONE` accepts a correctly signed chain whose certificates' validity
    /// periods do not overlap, which no single validation date can place in date; and that the same
    /// chain is refused with validity checked.
    @Test
    void aChainWithNonOverlappingValidityIsAcceptedOnlyWithValidityOff() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now, now.minus(Duration.ofDays(20)), now.minus(Duration.ofDays(10)));
        // The leaf became valid after its issuer had expired.
        final X509Certificate broker =
                pki.leaf("localhost", now.minus(Duration.ofDays(5)), now.plus(Duration.ofDays(30)));
        final X509Certificate[] presented = {broker, pki.intermediate()};

        final X509ExtendedTrustManager waived = chainManager(
                StubTLSConfig.chain().withValidity(TLSConstants.ValidityCheck.NONE), List.of(), pki.root());
        final X509ExtendedTrustManager checked = chainManager(StubTLSConfig.chain(), List.of(), pki.root());

        assertThat(catchThrowable(() -> waived.checkServerTrusted(presented, "RSA")))
                .isNull();
        assertThat(catchThrowable(() -> checked.checkServerTrusted(presented, "RSA")))
                .isInstanceOf(CertificateException.class);
    }

    /// Proves that with validity off, an expired and revoked broker certificate is refused as revoked
    /// by a current list, and the same certificate unrevoked is accepted: revocation is judged now.
    @Test
    void anExpiredCertificateIsJudgedForRevocationAtThePresentWhenValidityIsOff() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final X509Certificate broker =
                pki.leaf("localhost", now.minus(Duration.ofDays(9)), now.minus(Duration.ofDays(3)));
        final X509CRL revoking =
                pki.revocationList(broker, now.minus(Duration.ofHours(2)), now.minus(Duration.ofHours(1)));
        final X509CRL other = pki.revocationList(
                pki.leaf("other", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(1))),
                now.minus(Duration.ofHours(2)),
                now.minus(Duration.ofHours(1)));
        final StubTLSConfig config = StubTLSConfig.chain()
                .withValidity(TLSConstants.ValidityCheck.NONE)
                .withRevocation(TLSConstants.RevocationCheck.REQUIRE_CRLS);
        final X509Certificate[] presented = {broker, pki.intermediate()};

        assertThat(catchThrowable(() -> chainManager(config, List.of(revoking), pki.intermediate())
                        .checkServerTrusted(presented, "RSA")))
                .isInstanceOf(CertificateException.class)
                .hasRootCauseInstanceOf(CertificateRevokedException.class);
        assertThat(catchThrowable(() ->
                        chainManager(config, List.of(other), pki.intermediate()).checkServerTrusted(presented, "RSA")))
                .isNull();
    }

    /// Proves that turning the validity check off leaves revocation checking intact: a currently
    /// valid certificate revoked an hour ago by a current list is still refused as revoked.
    @Test
    void requiredRevocationRefusesARevokedCertificateWhenValidityIsOff() throws Exception {
        assertThat(revocationOutcome(TLSConstants.ValidityCheck.NONE))
                .as("with validity NONE, REQUIRE_CRLS still refuses a certificate revoked an hour ago")
                .isInstanceOf(CertificateException.class)
                .hasRootCauseInstanceOf(CertificateRevokedException.class);
    }

    /// Proves the same certificate and list are refused as revoked when validity is checked, so the
    /// fixture is a genuine revocation and only the validity setting differs.
    @Test
    void requiredRevocationRefusesARevokedCertificateWhenValidityIsOn() throws Exception {
        assertThat(revocationOutcome(TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER))
                .isInstanceOf(CertificateException.class)
                .hasRootCauseInstanceOf(CertificateRevokedException.class);
    }

    /// An authority valid since ten days ago, so that every date the tests rewind to still lies
    /// within its own validity.
    private static @NotNull TestAuthority authority() throws Exception {
        final Instant now = Instant.now();
        return TestAuthority.root("bridge-test-ca", now.minus(Duration.ofDays(10)), now.plus(Duration.ofDays(30)));
    }

    /// A broker certificate for `localhost` that the authority signed, valid since a day ago.
    private static @NotNull X509Certificate broker(final @NotNull TestAuthority authority) throws Exception {
        final Instant now = Instant.now();
        return authority.issue(
                "localhost", keyPair(), now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)), null, null);
    }

    private static @NotNull X509ExtendedTrustManager chainManager(
            final @NotNull StubTLSConfig config,
            final @NotNull List<X509CRL> lists,
            final @NotNull X509Certificate anchor)
            throws Exception {
        return trustManager(chainPolicy(config, lists, anchor));
    }

    /// A broker certificate that expired yesterday and otherwise passes every check a preset has.
    private static @NotNull X509Certificate expiredServerCertificate() throws Exception {
        final Instant now = Instant.now();
        return new TestPki(now)
                .leaf(
                        "localhost",
                        now.minus(Duration.ofDays(60)),
                        now.minus(Duration.ofDays(1)),
                        KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                        KeyPurposeId.id_kp_serverAuth);
    }

    /// The JDK's own trust manager over the given truststore, which is what a bridge used before it
    /// had a policy of its own.
    private static @NotNull X509TrustManager jdkTrustManager(final @NotNull KeyStore truststore) throws Exception {
        final TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(truststore);
        return (X509TrustManager) factory.getTrustManagers()[0];
    }

    /// What the bridge's trust manager throws for a revoked broker certificate under
    /// `revocation=REQUIRE_CRLS` and the given validity check, or `null` where it accepts it.
    ///
    /// The leaf became valid a day ago, the list has been current since two days ago and stays so
    /// for another day, and it revoked the leaf an hour ago.
    private static @Nullable Throwable revocationOutcome(final @NotNull TLSConstants.ValidityCheck validity)
            throws Exception {
        final Instant now = Instant.now();
        final TestAuthority authority = authority();
        final X509Certificate broker = broker(authority);
        final X509CRL revocations = authority.revocationList(
                broker, now.minus(Duration.ofDays(2)), now.plus(Duration.ofDays(1)), now.minus(Duration.ofHours(1)));
        final X509ExtendedTrustManager manager = chainManager(
                StubTLSConfig.chain().withValidity(validity).withRevocation(TLSConstants.RevocationCheck.REQUIRE_CRLS),
                List.of(revocations),
                authority.certificate());

        return catchThrowable(() -> manager.checkServerTrusted(new X509Certificate[] {broker}, "RSA"));
    }

    /// A self-signed end-entity certificate for the host, valid from a day ago.
    private static @NotNull X509Certificate selfSigned(final @NotNull String hostname, final @NotNull Instant now)
            throws Exception {
        final KeyPair keys = keyPair();
        return TLSFixtures.certificate(
                hostname,
                keys,
                null,
                keys,
                false,
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(30)),
                null,
                null);
    }

    private static @NotNull KeyStore truststoreHolding(final @NotNull X509Certificate certificate) throws Exception {
        final KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(null, null);
        truststore.setCertificateEntry("trusted", certificate);
        return truststore;
    }

    /// The trust manager a bridge hands the TLS handshake, for a configuration read from a `tls`
    /// element with the given checks and a truststore holding only the broker's certificate.
    private @NotNull X509ExtendedTrustManager configuredTrustManager(
            final @NotNull String checks, final @NotNull X509Certificate broker) throws Exception {
        final Path truststore = write(truststoreHolding(broker));
        final BridgeTlsEntity entity = bridgeTlsEntity("""
                <tls>
                    <enabled>true</enabled>
                    %s
                    <truststore><path>%s</path><password>%s</password></truststore>
                </tls>
                """.formatted(checks, truststore, TRUSTSTORE_PASSWORD));
        final TrustManagerFactory factory = bridgeClient(BRIDGE_ID, new TLSConfigBridge(BRIDGE_ID, entity), entity)
                .getMqtt5Client()
                .getConfig()
                .getSslConfig()
                .flatMap(MqttClientSslConfig::getTrustManagerFactory)
                .orElseThrow();
        return (X509ExtendedTrustManager) factory.getTrustManagers()[0];
    }

    /// The trust manager a bridge configured with the given truststore uses, built through the
    /// superseded `truststore` element exactly as an existing bridge's configuration is.
    private @NotNull X509TrustManager supersededTrustManager(final @NotNull KeyStore truststore) throws Exception {
        final Path file = write(truststore);
        // No key store, no protocol or cipher restriction, hostname off (the trust manager is called
        // without a connection), and the element's default handshake timeout of 10 seconds.
        final TLSConfigBridge config = TLSConfigBridge.fromSupersededSettings(
                BRIDGE_ID, null, null, null, file.toString(), TRUSTSTORE_PASSWORD, List.of(), List.of(), false, 10);
        return trustManager(new TLSPolicy(config));
    }

    private @NotNull Path write(final @NotNull KeyStore truststore) throws Exception {
        final Path file = directory.resolve("truststore.p12");
        try (final OutputStream out = Files.newOutputStream(file)) {
            truststore.store(out, TRUSTSTORE_PASSWORD.toCharArray());
        }
        return file;
    }
}
