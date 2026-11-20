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

import static com.hivemq.security.tls.TLSFixtures.anyCertificatePolicy;
import static com.hivemq.security.tls.TLSFixtures.chainPolicy;
import static com.hivemq.security.tls.TLSFixtures.handshake;
import static com.hivemq.security.tls.TLSFixtures.keyPair;
import static com.hivemq.security.tls.TLSFixtures.serverContext;
import static com.hivemq.security.tls.TLSFixtures.trustManager;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.hivemq.security.tls.TLSFixtures.HandshakeResult;
import java.security.KeyPair;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.X509ExtendedTrustManager;
import org.assertj.core.api.SoftAssertions;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/// The checks a bridge's trust manager applies to the broker certificate itself: the host it names,
/// its declared key usages, and its dates, under each trust mode.
///
/// The hostname cases use a real TLS client and server because endpoint identification is performed
/// by the TLS stack after the bridge's trust manager accepts the certificate. The remaining cases
/// call that trust manager directly, which isolates each policy check from transport negotiation.
class TLSValidatorBridgeCertificateChecksTest {

    /// Proves that both configured bridge trust modes refuse a trusted certificate for another host.
    @ParameterizedTest
    @EnumSource(
            value = TLSConstants.TrustMode.class,
            names = {"CHAIN", "ALLOW_LIST"})
    void hostnameVerificationRefusesACertificateForAnotherHost(final @NotNull TLSConstants.TrustMode trustMode)
            throws Exception {
        final TestAuthority authority = authority();
        final KeyPair keys = keyPair();
        final X509Certificate broker = issue(authority, "wrong.example", keys, false, null, false);

        final HandshakeResult result = handshake(
                hostnamePolicy(trustMode, authority, broker),
                serverContext(keys, broker, authority.certificate()),
                false);

        assertThat(result.clientFailure()).isInstanceOf(SSLHandshakeException.class);
    }

    /// Proves that hostname verification permits the broker certificate for the host Edge connects to.
    @ParameterizedTest
    @EnumSource(
            value = TLSConstants.TrustMode.class,
            names = {"CHAIN", "ALLOW_LIST"})
    void hostnameVerificationAcceptsACertificateForTheConnectedHost(final @NotNull TLSConstants.TrustMode trustMode)
            throws Exception {
        final TestAuthority authority = authority();
        final KeyPair keys = keyPair();
        final X509Certificate broker = issue(authority, "localhost", keys, false, null, false);

        final HandshakeResult result = handshake(
                hostnamePolicy(trustMode, authority, broker),
                serverContext(keys, broker, authority.certificate()),
                false);

        assertThat(result.clientFailure()).isNull();
        assertThat(result.serverFailure()).isNull();
    }

    /// Proves that KEY_USAGE refuses a chain-valid broker certificate that declares no KeyUsage.
    @Test
    void keyUsageRefusesACertificateWithoutTheKeyUsageExtension() throws Exception {
        final TestAuthority authority = authority();
        final X509Certificate broker = issue(authority, "localhost", keyPair(), false, null, false);

        assertThatThrownBy(() -> trustManager(keyUsagePolicy(TLSConstants.KeyUsageCheck.KEY_USAGE, authority))
                        .checkServerTrusted(new X509Certificate[] {broker}, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    /// Proves that SERVER_AUTH requires both KeyUsage and an ExtendedKeyUsage permitting server authentication.
    @Test
    void serverAuthenticationRefusesCertificatesMissingRequiredUsageExtensions() throws Exception {
        final TestAuthority authority = authority();
        final X509Certificate noKeyUsage = issue(authority, "localhost", keyPair(), false, null, false);
        final X509Certificate noExtendedKeyUsage = issue(authority, "localhost", keyPair(), true, null, false);
        final X509ExtendedTrustManager manager =
                trustManager(keyUsagePolicy(TLSConstants.KeyUsageCheck.SERVER_AUTH, authority));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThatThrownBy(() -> manager.checkServerTrusted(new X509Certificate[] {noKeyUsage}, "RSA"))
                    .isInstanceOf(CertificateException.class);
            softly.assertThatThrownBy(
                            () -> manager.checkServerTrusted(new X509Certificate[] {noExtendedKeyUsage}, "RSA"))
                    .isInstanceOf(CertificateException.class);
        });
    }

    /// Proves that the configuration predating the checks refuses what the JDK's own trust manager
    /// refused: a chain-validated broker certificate whose extended key usage excludes TLS servers.
    @Test
    void withoutAKeyUsageCheckAChainedCodeSigningCertificateIsStillRefusedAsTheJdkRefusesIt() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final X509Certificate codeSigning = pki.leaf(
                "localhost",
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(30)),
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                KeyPurposeId.id_kp_codeSigning);
        final X509ExtendedTrustManager manager = chainManager(StubTLSConfig.chain(), pki.root());

        assertThat(catchThrowable(() ->
                        manager.checkServerTrusted(new X509Certificate[] {codeSigning, pki.intermediate()}, "RSA")))
                .isInstanceOf(CertificateException.class);
    }

    /// Proves that `KEY_USAGE` refuses a key usage that permits no TLS server use, and accepts one
    /// that does.
    @Test
    void keyUsageRefusesAnAuthorityOnlyKeyUsageAndAcceptsAServerOne() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final X509Certificate signingOnly = pki.leaf(
                "localhost", now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)), KeyUsage.keyCertSign, null);
        final X509Certificate server = pki.leaf(
                "localhost",
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(30)),
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                null);
        final X509ExtendedTrustManager manager =
                chainManager(StubTLSConfig.chain().withKeyUsage(TLSConstants.KeyUsageCheck.KEY_USAGE), pki.root());

        assertThat(catchThrowable(() ->
                        manager.checkServerTrusted(new X509Certificate[] {signingOnly, pki.intermediate()}, "RSA")))
                .isInstanceOf(CertificateException.class);
        assertThat(catchThrowable(
                        () -> manager.checkServerTrusted(new X509Certificate[] {server, pki.intermediate()}, "RSA")))
                .isNull();
    }

    /// Proves that `SERVER_AUTH` applies under trust by fingerprint too: a pinned certificate for
    /// code signing is refused, a pinned server certificate accepted.
    @Test
    void serverAuthenticationAppliesToPinnedCertificates() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final X509Certificate codeSigning = pki.leaf(
                "localhost",
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(30)),
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                KeyPurposeId.id_kp_codeSigning);
        final X509Certificate server = pki.leaf(
                "localhost",
                now.minus(Duration.ofDays(1)),
                now.plus(Duration.ofDays(30)),
                KeyUsage.digitalSignature | KeyUsage.keyEncipherment,
                KeyPurposeId.id_kp_serverAuth);
        final X509ExtendedTrustManager manager = trustManager(new TLSPolicy(
                StubTLSConfig.of(TLSConstants.TrustMode.ALLOW_LIST)
                        .withKeyUsage(TLSConstants.KeyUsageCheck.SERVER_AUTH),
                Optional.empty(),
                Set.of(),
                Set.of(TLSPolicy.fingerprintOf(codeSigning), TLSPolicy.fingerprintOf(server)),
                List.of()));

        assertThat(catchThrowable(() -> manager.checkServerTrusted(new X509Certificate[] {codeSigning}, "RSA")))
                .isInstanceOf(CertificateException.class);
        assertThat(catchThrowable(() -> manager.checkServerTrusted(new X509Certificate[] {server}, "RSA")))
                .isNull();
    }

    /// Proves that validity remains enforced when trust mode accepts any certificate.
    @Test
    void anyCertificateTrustStillRefusesAnExpiredCertificateWhenValidityIsEnabled() throws Exception {
        final X509Certificate expired = issue(authority(), "localhost", keyPair(), false, null, true);

        assertThatThrownBy(() -> trustManager(anyCertificatePolicy(StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT)))
                        .checkServerTrusted(new X509Certificate[] {expired}, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    /// Proves that under ANY_CERT a certificate not yet valid is refused with validity on, and an
    /// expired one accepted with validity off.
    @Test
    void anyCertificateTrustFollowsTheValiditySetting() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final X509Certificate notYetValid =
                pki.leaf("localhost", now.plus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)));
        final X509Certificate expired =
                pki.leaf("localhost", now.minus(Duration.ofDays(9)), now.minus(Duration.ofDays(3)));
        final X509ExtendedTrustManager checked =
                trustManager(anyCertificatePolicy(StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT)));
        final X509ExtendedTrustManager waived = trustManager(anyCertificatePolicy(
                StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT).withValidity(TLSConstants.ValidityCheck.NONE)));

        assertThat(catchThrowable(() -> checked.checkServerTrusted(new X509Certificate[] {notYetValid}, "RSA")))
                .isInstanceOf(CertificateException.class);
        assertThat(catchThrowable(() -> waived.checkServerTrusted(new X509Certificate[] {expired}, "RSA")))
                .isNull();
    }

    /// Proves that the hostname check applies under ANY_CERT: a certificate for another host is
    /// refused in the handshake, one for the connected host accepted.
    @Test
    void anyCertificateTrustStillChecksTheHostname() throws Exception {
        final Instant now = Instant.now();
        final TestPki pki = new TestPki(now);
        final KeyPair wrongKeys = keyPair();
        final KeyPair rightKeys = keyPair();
        final X509Certificate wrong = pki.leaf(
                "wrong.example", wrongKeys, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)), null, null);
        final X509Certificate right = pki.leaf(
                "localhost", rightKeys, now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(30)), null, null);
        final TLSPolicy policy = anyCertificatePolicy(
                StubTLSConfig.of(TLSConstants.TrustMode.ANY_CERT).withHostname(TLSConstants.HostnameCheck.HOSTNAME));

        assertThat(handshake(policy, serverContext(wrongKeys, wrong, pki.intermediate()), false)
                        .clientFailure())
                .isInstanceOf(SSLHandshakeException.class);
        assertThat(handshake(policy, serverContext(rightKeys, right, pki.intermediate()), false)
                        .clientFailure())
                .isNull();
    }

    /// A self-signed authority valid from an hour ago for thirty days.
    private static @NotNull TestAuthority authority() throws Exception {
        final Instant now = Instant.now();
        return TestAuthority.root("bridge-test-ca", now.minus(Duration.ofHours(1)), now.plus(Duration.ofDays(30)));
    }

    private static @NotNull X509ExtendedTrustManager chainManager(
            final @NotNull StubTLSConfig config, final @NotNull X509Certificate anchor) throws Exception {
        return trustManager(chainPolicy(config, List.of(), anchor));
    }

    private static @NotNull TLSPolicy hostnamePolicy(
            final @NotNull TLSConstants.TrustMode trustMode,
            final @NotNull TestAuthority authority,
            final @NotNull X509Certificate broker)
            throws Exception {
        final StubTLSConfig config = StubTLSConfig.of(trustMode).withHostname(TLSConstants.HostnameCheck.HOSTNAME);
        return new TLSPolicy(
                config,
                Optional.empty(),
                trustMode == TLSConstants.TrustMode.CHAIN
                        ? Set.of(new TrustAnchor(authority.certificate(), null))
                        : Set.of(),
                trustMode == TLSConstants.TrustMode.ALLOW_LIST ? Set.of(TLSPolicy.fingerprintOf(broker)) : Set.of(),
                List.<X509CRL>of());
    }

    /// A broker certificate for the host that the authority signed: valid now or expired since
    /// yesterday, with a server key usage when asked, and the given extended key usage.
    private static @NotNull X509Certificate issue(
            final @NotNull TestAuthority authority,
            final @NotNull String hostname,
            final @NotNull KeyPair keys,
            final boolean keyUsage,
            final @Nullable KeyPurposeId extendedKeyUsage,
            final boolean expired)
            throws Exception {
        final Instant now = Instant.now();
        return authority.issue(
                hostname,
                keys,
                expired ? now.minus(Duration.ofDays(2)) : now.minus(Duration.ofHours(1)),
                expired ? now.minus(Duration.ofDays(1)) : now.plus(Duration.ofDays(30)),
                keyUsage ? KeyUsage.digitalSignature | KeyUsage.keyEncipherment : null,
                extendedKeyUsage);
    }

    private static @NotNull TLSPolicy keyUsagePolicy(
            final @NotNull TLSConstants.KeyUsageCheck keyUsage, final @NotNull TestAuthority authority) {
        return chainPolicy(StubTLSConfig.chain().withKeyUsage(keyUsage), List.of(), authority.certificate());
    }
}
