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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

/// What a bridge's policy actually enforces, driven through the trust manager the TLS handshake
/// calls.
///
/// Real certificates and a real certification path throughout: this is the code that decides whether
/// Edge talks to the right broker, and a test over mocks would prove only that the mocks agree with
/// each other. Each case presents a chain the way a handshake would and asserts whether it is
/// accepted.
class TLSValidatorBridgeTest {

    // ----- chain trust -----

    @Test
    void whenTheChainReachesAConfiguredAuthority_thenItIsAccepted() throws Exception {
        final Authority authority = new Authority("CN=plant-ca");
        final X509Certificate broker = authority.issue("CN=broker.plant.example", Duration.ofDays(30));

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.CHAIN, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                Set.of(new TrustAnchor(authority.certificate(), null)),
                Set.of(),
                List.of()));

        assertThatCode(() -> manager.checkServerTrusted(new X509Certificate[] {broker}, "RSA"))
                .doesNotThrowAnyException();
    }

    @Test
    void whenTheChainReachesADifferentAuthority_thenItIsRefused() throws Exception {
        final Authority configured = new Authority("CN=plant-ca");
        final Authority other = new Authority("CN=somebody-else");
        final X509Certificate broker = other.issue("CN=broker.plant.example", Duration.ofDays(30));

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.CHAIN, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                Set.of(new TrustAnchor(configured.certificate(), null)),
                Set.of(),
                List.of()));

        assertThatThrownBy(() -> manager.checkServerTrusted(new X509Certificate[] {broker}, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void whenAnIntermediateIsPresented_thenTheWholePathIsBuilt() throws Exception {
        // The common shape: a root the operator trusts, an intermediate it signed, and the broker's
        // certificate under that. Only the root is configured.
        final Authority root = new Authority("CN=plant-root");
        final Authority intermediate = root.issueAuthority("CN=plant-issuing");
        final X509Certificate broker = intermediate.issue("CN=broker.plant.example", Duration.ofDays(30));

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.CHAIN, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                Set.of(new TrustAnchor(root.certificate(), null)),
                Set.of(),
                List.of()));

        assertThatCode(() ->
                        manager.checkServerTrusted(new X509Certificate[] {broker, intermediate.certificate()}, "RSA"))
                .doesNotThrowAnyException();
    }

    @Test
    void whenTheCertificateHasExpired_thenValidityRefusesIt() throws Exception {
        final Authority authority = new Authority("CN=plant-ca");
        final X509Certificate expired = authority.issueExpired("CN=broker.plant.example");

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.CHAIN, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                Set.of(new TrustAnchor(authority.certificate(), null)),
                Set.of(),
                List.of()));

        assertThatThrownBy(() -> manager.checkServerTrusted(new X509Certificate[] {expired}, "RSA"))
                .isInstanceOf(CertificateException.class)
                // Names the certificate and its dates rather than reporting a generic path failure.
                .hasMessageContaining("expired");
    }

    @Test
    void whenValidityIsTurnedOff_thenAnExpiredCertificateIsAccepted() throws Exception {
        // The setting has to mean something: a deployment whose device clocks are wrong can turn it
        // off and still have the chain verified.
        final Authority authority = new Authority("CN=plant-ca");
        final X509Certificate expired = authority.issueExpired("CN=broker.plant.example");

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.CHAIN, TLSConstants.ValidityCheck.NONE),
                Set.of(new TrustAnchor(authority.certificate(), null)),
                Set.of(),
                List.of()));

        assertThatCode(() -> manager.checkServerTrusted(new X509Certificate[] {expired}, "RSA"))
                .doesNotThrowAnyException();
    }

    @Test
    void whenServerAuthenticationIsDemandedAndTheCertificateForbidsIt_thenItIsRefused() throws Exception {
        // A certificate issued for signing code must not stand in for a broker.
        final Authority authority = new Authority("CN=plant-ca");
        final X509Certificate codeSigning =
                authority.issueForPurpose("CN=broker.plant.example", KeyPurposeId.id_kp_codeSigning);

        final X509ExtendedTrustManager manager = managerFor(policy(
                checksWithKeyUsage(TLSConstants.KeyUsageCheck.SERVER_AUTH),
                Set.of(new TrustAnchor(authority.certificate(), null)),
                Set.of(),
                List.of()));

        assertThatThrownBy(() -> manager.checkServerTrusted(new X509Certificate[] {codeSigning}, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void whenServerAuthenticationIsDemandedAndTheCertificatePermitsIt_thenItIsAccepted() throws Exception {
        final Authority authority = new Authority("CN=plant-ca");
        final X509Certificate serverAuth =
                authority.issueForPurpose("CN=broker.plant.example", KeyPurposeId.id_kp_serverAuth);

        final X509ExtendedTrustManager manager = managerFor(policy(
                checksWithKeyUsage(TLSConstants.KeyUsageCheck.SERVER_AUTH),
                Set.of(new TrustAnchor(authority.certificate(), null)),
                Set.of(),
                List.of()));

        assertThatCode(() -> manager.checkServerTrusted(new X509Certificate[] {serverAuth}, "RSA"))
                .doesNotThrowAnyException();
    }

    @Test
    void whenNoTrustAnchorsWereLoaded_thenBuildingTheValidatorIsRefused() {
        // A path with nothing to build to refuses every connection. Caught at start rather than at
        // connect time, where it arrives as a certificate error naming no cause.
        assertThatThrownBy(() -> TLSValidatorBridge.trustManagerFactoryFor(policy(
                        checks(TLSConstants.TrustMode.CHAIN, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                        Set.of(),
                        Set.of(),
                        List.of())))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("no trust anchors");
    }

    // ----- fingerprint trust -----

    @Test
    void whenTheCertificateWasWrittenDown_thenItIsAcceptedWithoutAnyAuthority() throws Exception {
        // What makes a deployment with no certificate authority workable at all.
        final Authority selfSigner = new Authority("CN=broker.plant.example");
        final X509Certificate broker = selfSigner.certificate();

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.ALLOW_LIST, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                Set.of(),
                Set.of(TLSPolicy.fingerprintOf(broker)),
                List.of()));

        assertThatCode(() -> manager.checkServerTrusted(new X509Certificate[] {broker}, "RSA"))
                .doesNotThrowAnyException();
    }

    @Test
    void whenAnotherCertificateIsPresented_thenFingerprintTrustRefusesIt() throws Exception {
        final Authority expected = new Authority("CN=broker.plant.example");
        final Authority impostor = new Authority("CN=broker.plant.example");

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.ALLOW_LIST, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                Set.of(),
                Set.of(TLSPolicy.fingerprintOf(expected.certificate())),
                List.of()));

        // Same subject name, different key: only the fingerprint tells them apart.
        assertThatThrownBy(() -> manager.checkServerTrusted(new X509Certificate[] {impostor.certificate()}, "RSA"))
                .isInstanceOf(CertificateException.class);
    }

    @Test
    void whenNoFingerprintsWereLoaded_thenBuildingTheValidatorIsRefused() {
        assertThatThrownBy(() -> TLSValidatorBridge.trustManagerFactoryFor(policy(
                        checks(TLSConstants.TrustMode.ALLOW_LIST, TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER),
                        Set.of(),
                        Set.of(),
                        List.of())))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("no fingerprints");
    }

    // ----- no trust decision -----

    @Test
    void whenAnyCertificateIsAccepted_thenAnUnrelatedOnePasses() throws Exception {
        final Authority anybody = new Authority("CN=anybody-at-all");

        final X509ExtendedTrustManager manager = managerFor(policy(
                checks(TLSConstants.TrustMode.ANY_CERT, TLSConstants.ValidityCheck.NONE),
                Set.of(),
                Set.of(),
                List.of()));

        assertThatCode(() -> manager.checkServerTrusted(new X509Certificate[] {anybody.certificate()}, "RSA"))
                .doesNotThrowAnyException();
    }

    @Test
    void whenAnyCertificateIsAccepted_thenATrustManagerIsStillProduced() throws Exception {
        // Not "no trust manager": the TLS implementation reads an absent manager as the runtime's
        // own authorities, which is a different and stricter policy than the one configured.
        final TrustManagerFactory factory = TLSValidatorBridge.trustManagerFactoryFor(policy(
                checks(TLSConstants.TrustMode.ANY_CERT, TLSConstants.ValidityCheck.NONE),
                Set.of(),
                Set.of(),
                List.of()));

        assertThat(factory.getTrustManagers()).hasSize(1);
    }

    // ----- the hostname check -----

    @Test
    void whenTheHostnameCheckIsOn_thenTheLibrarysOwnVerifierIsUsed() throws Exception {
        final TLSPolicy policy =
                policy(checksWithHostname(TLSConstants.HostnameCheck.HOSTNAME), Set.of(), Set.of(), List.of());

        // Null means "use the default", which is the library's own RFC-compliant verifier. Writing
        // one here would be reimplementing wildcard matching, where a subtle mistake is a hole.
        assertThat(TLSValidatorBridge.hostnameVerifierFor(policy)).isNull();
    }

    @Test
    void whenTheHostnameCheckIsOff_thenEveryHostnameIsAccepted() throws Exception {
        final TLSPolicy policy =
                policy(checksWithHostname(TLSConstants.HostnameCheck.NONE), Set.of(), Set.of(), List.of());

        final var verifier = TLSValidatorBridge.hostnameVerifierFor(policy);
        assertThat(verifier).isNotNull();
        assertThat(verifier.verify("anything.at.all", null)).isTrue();
    }

    // ----- helpers -----

    private static @NotNull X509ExtendedTrustManager managerFor(final @NotNull TLSPolicy policy) throws Exception {
        return (X509ExtendedTrustManager)
                TLSValidatorBridge.trustManagerFactoryFor(policy).getTrustManagers()[0];
    }

    private static @NotNull TLSPolicy policy(
            final @NotNull TLSConfig config,
            final @NotNull Set<TrustAnchor> anchors,
            final @NotNull Set<String> fingerprints,
            final @NotNull List<X509CRL> revocationLists) {
        return new TLSPolicy(config, Optional.empty(), anchors, fingerprints, revocationLists);
    }

    private static @NotNull TLSConfig checks(
            final @NotNull TLSConstants.TrustMode trustMode, final @NotNull TLSConstants.ValidityCheck validity) {
        return new StubConfig(trustMode, validity, TLSConstants.KeyUsageCheck.NONE, TLSConstants.HostnameCheck.NONE);
    }

    private static @NotNull TLSConfig checksWithKeyUsage(final @NotNull TLSConstants.KeyUsageCheck keyUsage) {
        return new StubConfig(
                TLSConstants.TrustMode.CHAIN,
                TLSConstants.ValidityCheck.NOT_BEFORE_OR_AFTER,
                keyUsage,
                TLSConstants.HostnameCheck.NONE);
    }

    private static @NotNull TLSConfig checksWithHostname(final @NotNull TLSConstants.HostnameCheck hostname) {
        return new StubConfig(
                TLSConstants.TrustMode.ANY_CERT,
                TLSConstants.ValidityCheck.NONE,
                TLSConstants.KeyUsageCheck.NONE,
                hostname);
    }

    /// A configuration varying only the checks each test is about.
    private record StubConfig(
            @NotNull TLSConstants.TrustMode trustModeValue,
            @NotNull TLSConstants.ValidityCheck validityValue,
            @NotNull TLSConstants.KeyUsageCheck keyUsageValue,
            @NotNull TLSConstants.HostnameCheck hostnameValue)
            implements TLSConfig {

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public @NotNull TLSConstants.TrustMode trustMode() {
            return trustModeValue;
        }

        @Override
        public @NotNull TLSConstants.SanUriCheck sanUri() {
            return TLSConstants.SanUriCheck.NONE;
        }

        @Override
        public @NotNull TLSConstants.HostnameCheck hostname() {
            return hostnameValue;
        }

        @Override
        public @NotNull TLSConstants.ValidityCheck validity() {
            return validityValue;
        }

        @Override
        public @NotNull TLSConstants.RevocationCheck revocation() {
            return TLSConstants.RevocationCheck.NONE;
        }

        @Override
        public @NotNull TLSConstants.KeyUsageCheck keyUsage() {
            return keyUsageValue;
        }

        @Override
        public @NotNull Optional<MaterialRef> identity() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<MaterialRef> truststore() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<MaterialRef> allowList() {
            return Optional.empty();
        }

        @Override
        public @NotNull Optional<MaterialRef> revocationList() {
            return Optional.empty();
        }

        @Override
        public @NotNull List<String> protocols() {
            return List.of();
        }

        @Override
        public @NotNull List<String> cipherSuites() {
            return List.of();
        }

        @Override
        public @NotNull Duration handshakeTimeout() {
            return Duration.ZERO;
        }
    }

    /// A certificate authority that can sign for others, so a test can build a real path rather than
    /// assert over self-signed certificates that happen to be their own issuer.
    private static final class Authority {

        private final @NotNull KeyPair pair;
        private final @NotNull X509Certificate certificate;
        private final @Nullable Authority issuer;

        Authority(final @NotNull String subject) throws Exception {
            this(subject, null);
        }

        private Authority(final @NotNull String subject, final @Nullable Authority issuer) throws Exception {
            this.pair = rsaPair();
            this.issuer = issuer;
            this.certificate = build(subject, issuer, pair, Duration.ofDays(365), true, null, false);
        }

        @NotNull
        X509Certificate certificate() {
            return certificate;
        }

        @NotNull
        Authority issueAuthority(final @NotNull String subject) throws Exception {
            return new Authority(subject, this);
        }

        @NotNull
        X509Certificate issue(final @NotNull String subject, final @NotNull Duration validFor) throws Exception {
            return build(subject, this, rsaPair(), validFor, false, null, false);
        }

        @NotNull
        X509Certificate issueExpired(final @NotNull String subject) throws Exception {
            return build(subject, this, rsaPair(), Duration.ofDays(1), false, null, true);
        }

        @NotNull
        X509Certificate issueForPurpose(final @NotNull String subject, final @NotNull KeyPurposeId purpose)
                throws Exception {
            return build(subject, this, rsaPair(), Duration.ofDays(30), false, purpose, false);
        }

        private static @NotNull X509Certificate build(
                final @NotNull String subject,
                final @Nullable Authority issuer,
                final @NotNull KeyPair subjectPair,
                final @NotNull Duration validFor,
                final boolean isAuthority,
                final @Nullable KeyPurposeId purpose,
                final boolean expired)
                throws Exception {
            final Instant now = Instant.now();
            final Instant notBefore = expired ? now.minus(Duration.ofDays(400)) : now.minus(Duration.ofHours(1));
            final Instant notAfter = expired ? now.minus(Duration.ofDays(1)) : now.plus(validFor);
            final X500Principal subjectName = new X500Principal(subject);
            final X500Principal issuerName =
                    issuer == null ? subjectName : issuer.certificate.getSubjectX500Principal();
            final KeyPair signingPair = issuer == null ? subjectPair : issuer.pair;

            final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    issuerName,
                    BigInteger.valueOf(System.nanoTime()),
                    Date.from(notBefore),
                    Date.from(notAfter),
                    subjectName,
                    subjectPair.getPublic());
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isAuthority));
            if (purpose != null) {
                builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(purpose));
            }
            return new JcaX509CertificateConverter()
                    .getCertificate(builder.build(
                            new JcaContentSignerBuilder("SHA256withRSA").build(signingPair.getPrivate())));
        }

        private static @NotNull KeyPair rsaPair() throws Exception {
            final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }
    }
}
