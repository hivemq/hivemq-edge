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
package com.hivemq.edge.adapters.opcua.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.security.tls.MaterialRef;
import com.hivemq.security.tls.TLSConfig;
import com.hivemq.security.tls.TLSConstants.HostnameCheck;
import com.hivemq.security.tls.TLSConstants.KeyUsageCheck;
import com.hivemq.security.tls.TLSConstants.RevocationCheck;
import com.hivemq.security.tls.TLSConstants.SanUriCheck;
import com.hivemq.security.tls.TLSConstants.TrustMode;
import com.hivemq.security.tls.TLSConstants.ValidityCheck;
import com.hivemq.security.tls.TLSPolicy;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.milo.opcua.stack.core.security.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.TrustListManager;
import org.eclipse.milo.opcua.stack.core.util.validation.ValidationCheck;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

/**
 * How a policy reaches Eclipse Milo.
 *
 * <p>Two things are worth pinning. Which validator a trust mode selects, because that decides
 * whether a certification path is built at all. And which optional checks the policy turns into,
 * because a check an operator switched on that does not reach Milo is validation that silently did
 * not happen.
 *
 * <p>The checks are stated directly rather than taken from a preset: these cases are about the
 * mapping onto Milo, and a preset table belongs to the configuration layer above.
 */
class TLSValidatorOpcUATest {

    private static final @NotNull String ENDPOINT = "opc.tcp://plant-a:4840";

    // ----- which validator a trust mode selects -----

    @Test
    void chainTrustBuildsMilosOwnValidator() {
        // Only chain trust builds a certification path, and only Milo's validator does that.
        assertThat(TLSValidatorOpcUA.validatorFor(policy(checks().trustMode(TrustMode.CHAIN)), ENDPOINT))
                .isInstanceOf(DefaultClientCertificateValidator.class);
    }

    @Test
    void fingerprintTrustBuildsOurOwnValidator() {
        // Milo has no notion of trusting a fingerprint, so this case is ours.
        assertThat(TLSValidatorOpcUA.validatorFor(policy(checks().trustMode(TrustMode.ALLOW_LIST)), ENDPOINT))
                .isNotNull()
                .isNotInstanceOf(DefaultClientCertificateValidator.class);
    }

    @Test
    void noTrustAndNoOtherCheckStillYieldsAValidatorThatAcceptsEverything() {
        // The one configuration that verifies nothing whatever - and it still gets a validator. The
        // stack is handed one unconditionally, so a null would fail every handshake rather than skip
        // the checks: a connection that never works instead of one that checks nothing.
        assertThat(TLSValidatorOpcUA.validatorFor(policy(checks().trustMode(TrustMode.ANY_CERT)), ENDPOINT))
                .isInstanceOf(CertificateValidator.InsecureCertificateValidator.class);
    }

    @Test
    void noTrustStillAppliesTheChecksThatNeedNoPath() {
        // What keeps the checks independent: switching trust off does not switch the hostname check
        // off with it.
        final TLSPolicy policy = policy(checks().trustMode(TrustMode.ANY_CERT).hostname(HostnameCheck.HOSTNAME));

        assertThat(TLSValidatorOpcUA.validatorFor(policy, ENDPOINT)).isNotNull();
    }

    // ----- which optional checks reach Milo -----

    @Test
    void everyCheckOnAsksMiloForEveryOptionalCheck() {
        final TLSPolicy policy = policy(checks().trustMode(TrustMode.CHAIN)
                .sanUri(SanUriCheck.APPLICATION_URI)
                .hostname(HostnameCheck.HOSTNAME)
                .validity(ValidityCheck.NOT_BEFORE_OR_AFTER)
                .revocation(RevocationCheck.REQUIRE_CRLS)
                .keyUsage(KeyUsageCheck.SERVER_AUTH));

        assertThat(TLSValidatorOpcUA.validationChecks(policy)).isEqualTo(ValidationCheck.ALL_OPTIONAL_CHECKS);
    }

    @Test
    void everyCheckOffAsksMiloForNothing() {
        assertThat(TLSValidatorOpcUA.validationChecks(policy(checks().trustMode(TrustMode.CHAIN))))
                .isEmpty();
    }

    @Test
    void requiringRevocationListsAsksForBothRevocationChecks() {
        // Milo splits revocation into two: consult a list where there is one, and require one for
        // every authority in the path. The stricter setting means both.
        final TLSPolicy policy = policy(checks().trustMode(TrustMode.CHAIN).revocation(RevocationCheck.REQUIRE_CRLS));

        assertThat(TLSValidatorOpcUA.validationChecks(policy))
                .contains(ValidationCheck.REVOCATION, ValidationCheck.REVOCATION_LISTS);
    }

    @Test
    void consultingRevocationAsksOnlyForTheWeakerOfTheTwo() {
        final TLSPolicy policy = policy(checks().trustMode(TrustMode.CHAIN).revocation(RevocationCheck.CHECK));

        assertThat(TLSValidatorOpcUA.validationChecks(policy))
                .contains(ValidationCheck.REVOCATION)
                .doesNotContain(ValidationCheck.REVOCATION_LISTS);
    }

    @Test
    void requiringServerAuthenticationAsksForBothKeyUsageChecks() {
        final TLSPolicy policy = policy(checks().trustMode(TrustMode.CHAIN).keyUsage(KeyUsageCheck.SERVER_AUTH));

        assertThat(TLSValidatorOpcUA.validationChecks(policy))
                .contains(ValidationCheck.KEY_USAGE_END_ENTITY, ValidationCheck.EXTENDED_KEY_USAGE_END_ENTITY);
    }

    @Test
    void requiringKeyUsageAloneDoesNotAskForExtendedKeyUsage() {
        final TLSPolicy policy = policy(checks().trustMode(TrustMode.CHAIN).keyUsage(KeyUsageCheck.KEY_USAGE));

        assertThat(TLSValidatorOpcUA.validationChecks(policy))
                .contains(ValidationCheck.KEY_USAGE_END_ENTITY)
                .doesNotContain(ValidationCheck.EXTENDED_KEY_USAGE_END_ENTITY);
    }

    // ----- what the trust list hands Milo -----

    @Test
    void theRevocationListsReachBothListsMiloReads() throws Exception {
        // Milo distinguishes trusted from issuer lists; Edge does not expose that distinction, so a
        // revocation list an operator configured has to appear in both or a path through an issuer
        // would consult nothing.
        final X509CRL crl = emptyCrl();
        final TrustListManager trustList = trustListOf(policy(checks().trustMode(TrustMode.CHAIN), List.of(crl)));

        assertThat(trustList.getTrustedCrls()).containsExactly(crl);
        assertThat(trustList.getIssuerCrls()).containsExactly(crl);
    }

    @Test
    void noRevocationListsMeansEmptyRatherThanNull() throws Exception {
        // The default for every deployment with no authority in the path. Milo calls addAll on both
        // lists, so null would fail the connection rather than check nothing.
        final TrustListManager trustList = trustListOf(policy(checks().trustMode(TrustMode.CHAIN)));

        assertThat(trustList.getTrustedCrls()).isEmpty();
        assertThat(trustList.getIssuerCrls()).isEmpty();
    }

    @Test
    void theTrustAnchorsReachBothCertificateListsMiloReads() throws Exception {
        // Same reason as the revocation lists: an operator names certificates to trust, and Milo may
        // use any of them at either position in a path.
        final X509Certificate anchor = selfSigned();
        final TrustListManager trustList = trustListOf(policy(
                checks().trustMode(TrustMode.CHAIN),
                List.of(),
                Set.of(new java.security.cert.TrustAnchor(anchor, null))));

        assertThat(trustList.getTrustedCertificates()).containsExactly(anchor);
        assertThat(trustList.getIssuerCertificates()).containsExactly(anchor);
    }

    @Test
    void theTrustListRefusesToBeChanged() throws Exception {
        // The material came from files an operator wrote, and Edge never writes back to them. Milo
        // offers these for its own trust-list management API, which Edge does not expose.
        final TrustListManager trustList = trustListOf(policy(checks().trustMode(TrustMode.CHAIN)));

        assertThatThrownBy(() -> trustList.setTrustedCrls(List.of())).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> trustList.addTrustedCertificate(selfSigned()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ----- helpers -----

    private static @NotNull StubConfig checks() {
        return new StubConfig();
    }

    /** A policy over a configuration's checks, with no material: these cases are about the mapping. */
    private static @NotNull TLSPolicy policy(final @NotNull TLSConfig config) {
        return policy(config, List.of(), Set.of());
    }

    private static @NotNull TLSPolicy policy(
            final @NotNull TLSConfig config, final @NotNull List<X509CRL> revocationLists) {
        return policy(config, revocationLists, Set.of());
    }

    private static @NotNull TLSPolicy policy(
            final @NotNull TLSConfig config,
            final @NotNull List<X509CRL> revocationLists,
            final @NotNull Set<java.security.cert.TrustAnchor> anchors) {
        return new TLSPolicy(config, Optional.empty(), anchors, Set.of(), revocationLists);
    }

    private static @NotNull TrustListManager trustListOf(final @NotNull TLSPolicy policy) {
        return TLSValidatorOpcUA.trustListManager(policy);
    }

    /** A revocation list with nothing revoked, which is still a revocation list. */
    private static @NotNull X509CRL emptyCrl() throws Exception {
        final KeyPair pair = keyPair();
        final Instant now = Instant.now();
        return new JcaX509CRLConverter()
                .getCRL(new JcaX509v2CRLBuilder(new X500Principal("CN=issuer"), Date.from(now))
                        .setNextUpdate(Date.from(now.plus(Duration.ofDays(7))))
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
    }

    private static @NotNull X509Certificate selfSigned() throws Exception {
        final KeyPair pair = keyPair();
        final Instant now = Instant.now();
        final X500Principal principal = new X500Principal("CN=anchor");
        return new JcaX509CertificateConverter()
                .getCertificate(new JcaX509v3CertificateBuilder(
                                principal,
                                BigInteger.valueOf(now.toEpochMilli()),
                                Date.from(now.minus(Duration.ofHours(1))),
                                Date.from(now.plus(Duration.ofDays(1))),
                                principal,
                                pair.getPublic())
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
    }

    private static @NotNull KeyPair keyPair() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    /**
     * A configuration stating the six checks directly, every one off until set.
     *
     * <p>Off rather than strictest, so that each case switches on only the check it is about and
     * nothing else can account for the result.
     */
    private static final class StubConfig implements TLSConfig {

        private @NotNull TrustMode trustMode = TrustMode.ANY_CERT;
        private @NotNull SanUriCheck sanUri = SanUriCheck.NONE;
        private @NotNull HostnameCheck hostname = HostnameCheck.NONE;
        private @NotNull ValidityCheck validity = ValidityCheck.NONE;
        private @NotNull RevocationCheck revocation = RevocationCheck.NONE;
        private @NotNull KeyUsageCheck keyUsage = KeyUsageCheck.NONE;

        private @NotNull StubConfig trustMode(final @NotNull TrustMode value) {
            trustMode = value;
            return this;
        }

        private @NotNull StubConfig sanUri(final @NotNull SanUriCheck value) {
            sanUri = value;
            return this;
        }

        private @NotNull StubConfig hostname(final @NotNull HostnameCheck value) {
            hostname = value;
            return this;
        }

        private @NotNull StubConfig validity(final @NotNull ValidityCheck value) {
            validity = value;
            return this;
        }

        private @NotNull StubConfig revocation(final @NotNull RevocationCheck value) {
            revocation = value;
            return this;
        }

        private @NotNull StubConfig keyUsage(final @NotNull KeyUsageCheck value) {
            keyUsage = value;
            return this;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public @NotNull TrustMode trustMode() {
            return trustMode;
        }

        @Override
        public @NotNull SanUriCheck sanUri() {
            return sanUri;
        }

        @Override
        public @NotNull HostnameCheck hostname() {
            return hostname;
        }

        @Override
        public @NotNull ValidityCheck validity() {
            return validity;
        }

        @Override
        public @NotNull RevocationCheck revocation() {
            return revocation;
        }

        @Override
        public @NotNull KeyUsageCheck keyUsage() {
            return keyUsage;
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
}
