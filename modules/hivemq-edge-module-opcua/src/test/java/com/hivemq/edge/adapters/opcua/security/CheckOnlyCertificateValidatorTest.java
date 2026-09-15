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

import com.hivemq.edge.adapters.opcua.config.EffectiveChecks;
import com.hivemq.edge.adapters.opcua.config.HostnameCheck;
import com.hivemq.edge.adapters.opcua.config.KeyUsageCheck;
import com.hivemq.edge.adapters.opcua.config.RevocationCheck;
import com.hivemq.edge.adapters.opcua.config.SanUriCheck;
import com.hivemq.edge.adapters.opcua.config.TrustMode;
import com.hivemq.edge.adapters.opcua.config.ValidityCheck;
import java.security.cert.X509Certificate;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.junit.jupiter.api.Test;

/**
 * Under {@code ANY_CERT} no trust is established, but every other axis must still do exactly what it
 * says. This is what keeps the axes orthogonal instead of collapsing into "all or nothing".
 */
class CheckOnlyCertificateValidatorTest {

    private static final String[] HOSTNAMES = {TestCertificates.HOSTNAME};

    private static EffectiveChecks checks(
            final SanUriCheck sanUri,
            final HostnameCheck hostname,
            final ValidityCheck validity,
            final KeyUsageCheck keyUsage) {
        return new EffectiveChecks(TrustMode.ANY_CERT, sanUri, hostname, validity, RevocationCheck.NONE, keyUsage);
    }

    private static EffectiveChecks nothing() {
        return checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.NONE);
    }

    private static void validate(final EffectiveChecks checks, final X509Certificate certificate) throws UaException {
        new CheckOnlyCertificateValidator(checks)
                .validateCertificateChain(List.of(certificate), TestCertificates.APPLICATION_URI, HOSTNAMES);
    }

    @Test
    void withNoChecks_anyCertificateIsAccepted() throws Exception {
        // Including one that is expired, has the wrong identity and no key usage at all.
        final X509Certificate hostile = TestCertificates.builder()
                .withApplicationUri("urn:somebody:else")
                .withHostname("attacker.example")
                .expired()
                .build();

        assertThatCode(() -> validate(nothing(), hostile)).doesNotThrowAnyException();
    }

    @Test
    void applicationUriIsEnforcedWhenAsked() throws Exception {
        final X509Certificate wrongUri = TestCertificates.builder()
                .withApplicationUri("urn:somebody:else")
                .withHostname(TestCertificates.HOSTNAME)
                .build();

        assertThatThrownBy(() -> validate(
                        checks(SanUriCheck.APPLICATION_URI, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.NONE),
                        wrongUri))
                .isInstanceOf(UaException.class);

        assertThatCode(() -> validate(nothing(), wrongUri)).doesNotThrowAnyException();
    }

    @Test
    void applicationUriPassesForTheRightCertificate() throws Exception {
        assertThatCode(() -> validate(
                        checks(SanUriCheck.APPLICATION_URI, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.NONE),
                        TestCertificates.identityOnly()))
                .doesNotThrowAnyException();
    }

    @Test
    void hostnameIsEnforcedWhenAsked() throws Exception {
        final X509Certificate wrongHost = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname("someone.else.local")
                .build();

        assertThatThrownBy(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.HOSTNAME, ValidityCheck.NONE, KeyUsageCheck.NONE),
                        wrongHost))
                .isInstanceOf(UaException.class);

        assertThatCode(() -> validate(nothing(), wrongHost)).doesNotThrowAnyException();
    }

    @Test
    void hostnamePassesForTheRightCertificate() throws Exception {
        assertThatCode(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.HOSTNAME, ValidityCheck.NONE, KeyUsageCheck.NONE),
                        TestCertificates.identityOnly()))
                .doesNotThrowAnyException();
    }

    @Test
    void expiryIsEnforcedWhenAsked() throws Exception {
        final X509Certificate expired = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .expired()
                .build();

        assertThatThrownBy(() -> validate(
                        checks(
                                SanUriCheck.NONE,
                                HostnameCheck.NONE,
                                ValidityCheck.NOT_BEFORE_OR_AFTER,
                                KeyUsageCheck.NONE),
                        expired))
                .isInstanceOf(UaException.class);

        assertThatCode(() -> validate(nothing(), expired)).doesNotThrowAnyException();
    }

    @Test
    void notYetValidIsAlsoRejected() throws Exception {
        final X509Certificate future = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .notYetValid()
                .build();

        assertThatThrownBy(() -> validate(
                        checks(
                                SanUriCheck.NONE,
                                HostnameCheck.NONE,
                                ValidityCheck.NOT_BEFORE_OR_AFTER,
                                KeyUsageCheck.NONE),
                        future))
                .isInstanceOf(UaException.class);
    }

    @Test
    void keyUsageIsEnforcedWhenAsked() throws Exception {
        // The Miele certificate class: no KeyUsage extension at all.
        final X509Certificate noKeyUsage = TestCertificates.identityOnly();

        assertThatThrownBy(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.KEY_USAGE),
                        noKeyUsage))
                .isInstanceOf(UaException.class);

        final X509Certificate withKeyUsage = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .withKeyUsage()
                .build();

        assertThatCode(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.KEY_USAGE),
                        withKeyUsage))
                .doesNotThrowAnyException();
    }

    @Test
    void aSelfSignedCertificateWithoutKeyCertSignFailsKeyUsage() throws Exception {
        // The exact certificate shape from the customer report. It is why the SELF_SIGNED preset leaves
        // the key-usage axis off: demanding it would reject the very certificates that preset exists for.
        final X509Certificate mieleShaped = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .withKeyUsageMissingKeyCertSign()
                .build();

        assertThatThrownBy(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.KEY_USAGE),
                        mieleShaped))
                .isInstanceOf(UaException.class);

        assertThatCode(() -> validate(
                        checks(
                                SanUriCheck.APPLICATION_URI,
                                HostnameCheck.HOSTNAME,
                                ValidityCheck.NOT_BEFORE_OR_AFTER,
                                KeyUsageCheck.NONE),
                        mieleShaped))
                .as("everything the SELF_SIGNED preset asks for must still pass on such a certificate")
                .doesNotThrowAnyException();
    }

    @Test
    void serverAuthAlsoRequiresTheExtendedKeyUsage() throws Exception {
        final X509Certificate keyUsageOnly = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .withKeyUsage()
                .build();

        // KEY_USAGE is satisfied, SERVER_AUTH is not: the difference between the two values.
        assertThatCode(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.KEY_USAGE),
                        keyUsageOnly))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.SERVER_AUTH),
                        keyUsageOnly))
                .isInstanceOf(UaException.class);

        final X509Certificate serverAuth = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .withKeyUsage()
                .withServerAuthExtendedKeyUsage()
                .build();

        assertThatCode(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.SERVER_AUTH),
                        serverAuth))
                .doesNotThrowAnyException();
    }

    @Test
    void aClientAuthOnlyCertificateIsNotAcceptedAsAServer() throws Exception {
        final X509Certificate clientAuth = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .withKeyUsage()
                .withClientAuthExtendedKeyUsage()
                .build();

        assertThatThrownBy(() -> validate(
                        checks(SanUriCheck.NONE, HostnameCheck.NONE, ValidityCheck.NONE, KeyUsageCheck.SERVER_AUTH),
                        clientAuth))
                .isInstanceOf(UaException.class);
    }

    @Test
    void everyEnabledCheckMustPass() throws Exception {
        final EffectiveChecks everything = checks(
                SanUriCheck.APPLICATION_URI,
                HostnameCheck.HOSTNAME,
                ValidityCheck.NOT_BEFORE_OR_AFTER,
                KeyUsageCheck.SERVER_AUTH);

        final X509Certificate good = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .withKeyUsage()
                .withServerAuthExtendedKeyUsage()
                .build();
        assertThatCode(() -> validate(everything, good)).doesNotThrowAnyException();

        // One attribute wrong at a time is enough to fail the whole validation.
        final X509Certificate expired = TestCertificates.builder()
                .withApplicationUri(TestCertificates.APPLICATION_URI)
                .withHostname(TestCertificates.HOSTNAME)
                .withKeyUsage()
                .withServerAuthExtendedKeyUsage()
                .expired()
                .build();
        assertThatThrownBy(() -> validate(everything, expired)).isInstanceOf(UaException.class);
    }

    @Test
    void aCheckWhoseInputTheStackDidNotSupplyIsSkipped() throws Exception {
        // Mirrors the default validator: it cannot compare against something it was not given, and
        // failing every connection in that case would be worse than skipping the comparison.
        final X509Certificate certificate = TestCertificates.identityOnly();
        final EffectiveChecks identity =
                checks(SanUriCheck.APPLICATION_URI, HostnameCheck.HOSTNAME, ValidityCheck.NONE, KeyUsageCheck.NONE);

        assertThatCode(() -> new CheckOnlyCertificateValidator(identity)
                        .validateCertificateChain(List.of(certificate), null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void anEmptyChainIsRejected() {
        assertThatThrownBy(() -> new CheckOnlyCertificateValidator(nothing())
                        .validateCertificateChain(List.of(), TestCertificates.APPLICATION_URI, HOSTNAMES))
                .isInstanceOf(UaException.class)
                .hasMessageContaining("empty certificate chain");
    }

    @Test
    void onlyTheEndEntityIsInspected() throws Exception {
        // Whatever else the server sends along must not be able to satisfy a check on its behalf.
        final X509Certificate endEntity = TestCertificates.builder()
                .withApplicationUri("urn:somebody:else")
                .withHostname("attacker.example")
                .build();
        final X509Certificate decoy = TestCertificates.identityOnly();

        assertThatThrownBy(() -> new CheckOnlyCertificateValidator(checks(
                                SanUriCheck.APPLICATION_URI,
                                HostnameCheck.NONE,
                                ValidityCheck.NONE,
                                KeyUsageCheck.NONE))
                        .validateCertificateChain(
                                List.of(endEntity, decoy), TestCertificates.APPLICATION_URI, HOSTNAMES))
                .isInstanceOf(UaException.class);
    }

    @Test
    void theValidatorNeverBuildsAChain() throws Exception {
        // The defining property of ANY_CERT: a certificate that chains to nothing is still accepted
        // when no per-certificate check objects to it.
        final X509Certificate orphan = TestCertificates.identityOnly();
        assertThat(orphan.getIssuerX500Principal()).isEqualTo(orphan.getSubjectX500Principal());

        assertThatCode(() -> validate(
                        checks(
                                SanUriCheck.APPLICATION_URI,
                                HostnameCheck.HOSTNAME,
                                ValidityCheck.NOT_BEFORE_OR_AFTER,
                                KeyUsageCheck.NONE),
                        orphan))
                .doesNotThrowAnyException();
    }
}
