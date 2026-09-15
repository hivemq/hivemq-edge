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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hivemq.edge.adapters.opcua.config.EffectiveChecks;
import com.hivemq.edge.adapters.opcua.config.HostnameCheck;
import com.hivemq.edge.adapters.opcua.config.KeyUsageCheck;
import com.hivemq.edge.adapters.opcua.config.RevocationCheck;
import com.hivemq.edge.adapters.opcua.config.SanUriCheck;
import com.hivemq.edge.adapters.opcua.config.TlsChecks;
import com.hivemq.edge.adapters.opcua.config.TlsChecksProjection;
import com.hivemq.edge.adapters.opcua.config.TrustMode;
import com.hivemq.edge.adapters.opcua.config.ValidityCheck;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * Trusting a server by fingerprint. The properties that matter: the listed certificate is accepted,
 * anything else is refused, the other axes still apply on top, and a refusal tells the operator the
 * fingerprint it saw so they can enrol it deliberately.
 */
class AllowListCertificateValidatorTest {

    private static final String ENDPOINT = "opc.tcp://factory.local:4840";
    private static final String[] HOSTNAMES = {TestCertificates.HOSTNAME};

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        appender = new ListAppender<>();
        appender.start();
        ((Logger) LoggerFactory.getLogger(AllowListCertificateValidator.class)).addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(AllowListCertificateValidator.class)).detachAppender(appender);
        appender.stop();
    }

    private static EffectiveChecks selfSigned() {
        return TlsChecksProjection.fromPreset(TlsChecks.SELF_SIGNED);
    }

    private static EffectiveChecks fingerprintOnly() {
        return new EffectiveChecks(
                TrustMode.ALLOW_LIST,
                SanUriCheck.NONE,
                HostnameCheck.NONE,
                ValidityCheck.NONE,
                RevocationCheck.NONE,
                KeyUsageCheck.NONE);
    }

    private static void validate(
            final Set<String> allowed, final EffectiveChecks checks, final X509Certificate certificate)
            throws UaException {
        new AllowListCertificateValidator(allowed, checks, ENDPOINT)
                .validateCertificateChain(List.of(certificate), TestCertificates.APPLICATION_URI, HOSTNAMES);
    }

    @Test
    void aListedCertificateIsAccepted() throws Exception {
        final X509Certificate certificate = TestCertificates.identityOnly();
        final String fingerprint = CertificateFingerprints.fingerprintOf(certificate);

        assertThatCode(() -> validate(Set.of(fingerprint), selfSigned(), certificate))
                .doesNotThrowAnyException();
    }

    @Test
    void anUnlistedCertificateIsRejected() throws Exception {
        final X509Certificate certificate = TestCertificates.identityOnly();

        assertThatThrownBy(() -> validate(Set.of("0".repeat(64)), fingerprintOnly(), certificate))
                .isInstanceOf(UaException.class)
                .hasMessageContaining("not in the configured allow-list");
    }

    @Test
    void aRejectionLogsTheSeenFingerprintInTheFileFormat() throws Exception {
        final X509Certificate certificate = TestCertificates.identityOnly();
        final String display =
                CertificateFingerprints.toDisplayForm(CertificateFingerprints.fingerprintOf(certificate));

        assertThatThrownBy(() -> validate(Set.of("0".repeat(64)), fingerprintOnly(), certificate))
                .isInstanceOf(UaException.class);

        assertThat(appender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            final String message = event.getFormattedMessage();
            assertThat(message).contains(display);
            assertThat(message).contains(ENDPOINT);
            // The operator needs to be able to tell which machine this was.
            assertThat(message).contains("Subject=");
        });
    }

    @Test
    void anAcceptedCertificateIsNotLogged() throws Exception {
        final X509Certificate certificate = TestCertificates.identityOnly();
        validate(Set.of(CertificateFingerprints.fingerprintOf(certificate)), selfSigned(), certificate);

        assertThat(appender.list)
                .as("a successful validation must stay quiet; every connect would otherwise log")
                .isEmpty();
    }

    @Test
    void aDifferentCertificateFromTheSameMachineIsRejected() throws Exception {
        // Certificate rotation is exactly the event this trust mode exists to detect, and the reason
        // it beats accepting anything.
        final X509Certificate enrolled = TestCertificates.identityOnly();
        final X509Certificate rotated = TestCertificates.identityOnly();

        assertThat(CertificateFingerprints.fingerprintOf(enrolled))
                .isNotEqualTo(CertificateFingerprints.fingerprintOf(rotated));

        assertThatThrownBy(() ->
                        validate(Set.of(CertificateFingerprints.fingerprintOf(enrolled)), fingerprintOnly(), rotated))
                .isInstanceOf(UaException.class);
    }

    @Test
    void anyListedCertificateIsAcceptedNotJustTheIntendedOne() throws Exception {
        // The allow-list is a trust SET, not endpoint pinning: with two fingerprints in the file,
        // either certificate passes the trust decision on this adapter. Telling two listed servers
        // apart is the job of the identity axes (application URI, hostname) - a deployment that
        // relaxes those and shares one allow-list file across adapters has pinned "one of these
        // machines", not "this machine". Single-certificate pinning needs a per-adapter file with
        // exactly one fingerprint; the documentation states the same.
        final X509Certificate intended = TestCertificates.identityOnly();
        final X509Certificate other = TestCertificates.identityOnly();
        final Set<String> shared =
                Set.of(CertificateFingerprints.fingerprintOf(intended), CertificateFingerprints.fingerprintOf(other));

        assertThatCode(() -> validate(shared, fingerprintOnly(), intended)).doesNotThrowAnyException();
        assertThatCode(() -> validate(shared, fingerprintOnly(), other))
                .as("either listed certificate passes the trust decision")
                .doesNotThrowAnyException();
    }

    @Test
    void identityChecksStillApplyToAListedCertificate() throws Exception {
        // Being on the list establishes trust, not identity: SELF_SIGNED also asserts the application
        // URI and hostname, and a listed certificate that fails them must still be refused.
        final X509Certificate wrongIdentity = TestCertificates.builder()
                .withApplicationUri("urn:somebody:else")
                .withHostname("attacker.example")
                .build();

        assertThatThrownBy(() -> validate(
                        Set.of(CertificateFingerprints.fingerprintOf(wrongIdentity)), selfSigned(), wrongIdentity))
                .isInstanceOf(UaException.class);

        // With the identity axes off, the same certificate is accepted - the axes are independent.
        assertThatCode(() -> validate(
                        Set.of(CertificateFingerprints.fingerprintOf(wrongIdentity)), fingerprintOnly(), wrongIdentity))
                .doesNotThrowAnyException();
    }

    @Test
    void expiryStillAppliesToAListedCertificate() throws Exception {
        final X509Certificate expired = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .expired()
                .build();

        assertThatThrownBy(
                        () -> validate(Set.of(CertificateFingerprints.fingerprintOf(expired)), selfSigned(), expired))
                .isInstanceOf(UaException.class);
    }

    @Test
    void aCertificateWithNoHostnameIsRefusedUnderSelfSigned() throws Exception {
        // Documents a real limit of the SELF_SIGNED preset rather than a defect in this validator.
        // SELF_SIGNED asserts the hostname, and factory device certificates frequently carry none -
        // the very deployment the preset is named for. Such a deployment cannot use the preset and
        // must drop to the axes with hostname=NONE, which is what the allow-list IT fixture and the
        // documentation's selection table both do. If SELF_SIGNED is ever redefined to leave the
        // hostname off, this test is the one that should change.
        final X509Certificate noHostname = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .build();
        final Set<String> allowed = Set.of(CertificateFingerprints.fingerprintOf(noHostname));

        assertThatThrownBy(() -> validate(allowed, selfSigned(), noHostname)).isInstanceOf(UaException.class);

        // The documented way out: same certificate, same allow-list, hostname axis off.
        final EffectiveChecks withoutHostname = new EffectiveChecks(
                TrustMode.ALLOW_LIST,
                SanUriCheck.APPLICATION_URI,
                HostnameCheck.NONE,
                ValidityCheck.NOT_BEFORE_OR_AFTER,
                RevocationCheck.NONE,
                KeyUsageCheck.NONE);
        assertThatCode(() -> validate(allowed, withoutHostname, noHostname)).doesNotThrowAnyException();
    }

    @Test
    void aCertificateWithoutKeyUsageIsAcceptedUnderSelfSigned() throws Exception {
        // The whole point for the factory case: these certificates carry no key-usage extensions, and
        // SELF_SIGNED deliberately does not demand them.
        final X509Certificate noKeyUsage = TestCertificates.identityOnly();
        assertThat(noKeyUsage.getKeyUsage()).isNull();

        assertThatCode(() ->
                        validate(Set.of(CertificateFingerprints.fingerprintOf(noKeyUsage)), selfSigned(), noKeyUsage))
                .doesNotThrowAnyException();
    }

    @Test
    void onlyTheEndEntityFingerprintCounts() throws Exception {
        // A server must not be able to smuggle a listed certificate in behind an unlisted one.
        final X509Certificate presented = TestCertificates.identityOnly();
        final X509Certificate listed = TestCertificates.identityOnly();

        assertThatThrownBy(() -> new AllowListCertificateValidator(
                                Set.of(CertificateFingerprints.fingerprintOf(listed)), fingerprintOnly(), ENDPOINT)
                        .validateCertificateChain(
                                List.of(presented, listed), TestCertificates.APPLICATION_URI, HOSTNAMES))
                .isInstanceOf(UaException.class);
    }

    @Test
    void anEmptyChainIsRejected() {
        assertThatThrownBy(() -> new AllowListCertificateValidator(Set.of("0".repeat(64)), fingerprintOnly(), ENDPOINT)
                        .validateCertificateChain(List.of(), TestCertificates.APPLICATION_URI, HOSTNAMES))
                .isInstanceOf(UaException.class)
                .hasMessageContaining("empty certificate chain");
    }

    @Test
    void theValidatorNeverMutatesItsAllowList() throws Exception {
        // No trust-on-first-use, ever: a rejected certificate must not end up trusted next time.
        final X509Certificate certificate = TestCertificates.identityOnly();
        final AllowListCertificateValidator validator =
                new AllowListCertificateValidator(Set.of("0".repeat(64)), fingerprintOnly(), ENDPOINT);

        assertThatThrownBy(() -> validator.validateCertificateChain(
                        List.of(certificate), TestCertificates.APPLICATION_URI, HOSTNAMES))
                .isInstanceOf(UaException.class);
        assertThatThrownBy(() -> validator.validateCertificateChain(
                        List.of(certificate), TestCertificates.APPLICATION_URI, HOSTNAMES))
                .as("a second attempt must fail exactly like the first")
                .isInstanceOf(UaException.class);
    }

    @Test
    void theAllowListIsCopiedDefensively() throws Exception {
        final X509Certificate certificate = TestCertificates.identityOnly();
        final java.util.Set<String> mutable = new java.util.HashSet<>(Set.of("0".repeat(64)));
        final AllowListCertificateValidator validator =
                new AllowListCertificateValidator(mutable, fingerprintOnly(), ENDPOINT);

        mutable.add(CertificateFingerprints.fingerprintOf(certificate));

        assertThatThrownBy(() -> validator.validateCertificateChain(
                        List.of(certificate), TestCertificates.APPLICATION_URI, HOSTNAMES))
                .as("the trust anchor must not be reachable for modification after construction")
                .isInstanceOf(UaException.class);
    }
}
