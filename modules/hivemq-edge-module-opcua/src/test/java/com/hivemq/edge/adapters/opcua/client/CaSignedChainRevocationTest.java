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
package com.hivemq.edge.adapters.opcua.client;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.edge.adapters.opcua.config.EffectiveChecks;
import com.hivemq.edge.adapters.opcua.config.HostnameCheck;
import com.hivemq.edge.adapters.opcua.config.KeyUsageCheck;
import com.hivemq.edge.adapters.opcua.config.RevocationCheck;
import com.hivemq.edge.adapters.opcua.config.SanUriCheck;
import com.hivemq.edge.adapters.opcua.config.TrustMode;
import com.hivemq.edge.adapters.opcua.config.ValidityCheck;
import com.hivemq.edge.adapters.opcua.security.CertificateRevocationLists;
import com.hivemq.edge.adapters.opcua.security.CertificateTrustListManager;
import java.nio.file.Path;
import java.security.cert.X509CRL;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.security.MemoryCertificateQuarantine;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.KeyChain;

/**
 * EDG-891: revocation against a genuinely CA-signed chain, through the real validation stack.
 *
 * <p>The other tests in this area stop at the trust-list manager — they prove a configured CRL reaches
 * the call Milo makes. This one goes the rest of the way and asks the validator itself, because the
 * defect being fixed was never in the wiring: chain trust worked, and revocation on top of it refused
 * every CA-signed server with {@code Bad_CertificateRevocationUnknown} because no CRL could be supplied.
 *
 * <p>Three outcomes have to hold together, and only the three together mean anything. Without CRLs the
 * status is unknown and must fail closed; with the right CRL the connection must actually succeed,
 * which is the half that was impossible before; and a CRL that revokes the server's certificate must
 * refuse it, which is what proves the first two are not just an on/off switch on the check itself.
 */
class CaSignedChainRevocationTest {

    private static final @NotNull String DOMAIN = "server";
    private static final @NotNull String APPLICATION_URI = "urn:hivemq:edge:" + DOMAIN;

    @TempDir
    @NotNull
    Path tmp;

    /** The default preset's axes: chain, ApplicationUri, validity and revocation. */
    private static @NotNull EffectiveChecks standard() {
        return new EffectiveChecks(
                TrustMode.CHAIN,
                SanUriCheck.APPLICATION_URI,
                HostnameCheck.NONE,
                ValidityCheck.NOT_BEFORE_OR_AFTER,
                RevocationCheck.REQUIRE_CRLS,
                KeyUsageCheck.NONE);
    }

    private static void validate(
            final @NotNull KeyChain keyChain, final @NotNull List<X509CRL> crls, final @NotNull EffectiveChecks checks)
            throws UaException {
        new DefaultClientCertificateValidator(
                        new CertificateTrustListManager(List.of(keyChain.getRootCertificate()), crls),
                        ParsedConfig.toValidationChecks(checks),
                        new MemoryCertificateQuarantine())
                .validateCertificateChain(
                        List.of(keyChain.getLeafCertificate(DOMAIN), keyChain.getRootCertificate()),
                        APPLICATION_URI,
                        new String[] {DOMAIN});
    }

    @Test
    void withoutACrl_theCaSignedServerIsRefusedBecauseRevocationIsUnknown() throws Exception {
        // The reported defect, pinned as a test so the fix cannot regress into it silently.
        final KeyChain keyChain = KeyChain.createKeyChain(DOMAIN);

        assertThatThrownBy(() -> validate(keyChain, List.of(), standard()))
                .isInstanceOf(UaException.class)
                .hasMessageContaining("revocation");
    }

    @Test
    void withTheCasCrl_theCaSignedServerIsAccepted() throws Exception {
        // The half that was unreachable before: the default preset connecting to a correctly
        // configured CA-signed server, with no security check turned off to get there.
        final KeyChain keyChain = KeyChain.createKeyChain(DOMAIN);
        final List<X509CRL> crls =
                CertificateRevocationLists.load(keyChain.writeRootCrl(tmp.resolve("ca.crl"), List.of()));

        assertThatCode(() -> validate(keyChain, crls, standard())).doesNotThrowAnyException();
    }

    @Test
    void withACrlRevokingTheServer_itIsRefused() throws Exception {
        // Without this, the test above would pass just as well against a build that had quietly
        // stopped checking revocation at all.
        final KeyChain keyChain = KeyChain.createKeyChain(DOMAIN);
        final List<X509CRL> crls = CertificateRevocationLists.load(
                keyChain.writeRootCrl(tmp.resolve("revoked.crl"), List.of(keyChain.getLeafSerial(DOMAIN))));

        assertThatThrownBy(() -> validate(keyChain, crls, standard())).isInstanceOf(UaException.class);
    }

    @Test
    void aCrlFromADifferentCaDoesNotSatisfyRevocation() throws Exception {
        // A CRL only answers for the issuer that signed it. One from elsewhere must leave the status
        // unknown rather than counting as "revocation information was supplied".
        final KeyChain keyChain = KeyChain.createKeyChain(DOMAIN);
        final KeyChain unrelated = KeyChain.createKeyChain("other");
        final List<X509CRL> crls =
                CertificateRevocationLists.load(unrelated.writeRootCrl(tmp.resolve("unrelated.crl"), List.of()));

        assertThatThrownBy(() -> validate(keyChain, crls, standard()))
                .isInstanceOf(UaException.class)
                .hasMessageContaining("revocation");
    }

    @Test
    void withRevocationOff_theCaSignedServerConnectsWithoutAnyCrl() throws Exception {
        // The documented escape hatch for a CA that publishes no CRLs at all. Chain validation itself
        // is unaffected, which is what makes it a narrowing of the checks rather than an opening.
        final KeyChain keyChain = KeyChain.createKeyChain(DOMAIN);
        final EffectiveChecks noRevocation = new EffectiveChecks(
                TrustMode.CHAIN,
                SanUriCheck.APPLICATION_URI,
                HostnameCheck.NONE,
                ValidityCheck.NOT_BEFORE_OR_AFTER,
                RevocationCheck.NONE,
                KeyUsageCheck.NONE);

        assertThatCode(() -> validate(keyChain, List.of(), noRevocation)).doesNotThrowAnyException();
    }

    @Test
    void anUntrustedCaIsStillRefusedEvenWithAValidCrl() throws Exception {
        // Revocation is a check on top of trust, never a substitute for it.
        final KeyChain trusted = KeyChain.createKeyChain(DOMAIN);
        final KeyChain untrusted = KeyChain.createKeyChain(DOMAIN);
        final List<X509CRL> crls =
                CertificateRevocationLists.load(untrusted.writeRootCrl(tmp.resolve("other.crl"), List.of()));

        assertThatThrownBy(() -> new DefaultClientCertificateValidator(
                                new CertificateTrustListManager(List.of(trusted.getRootCertificate()), crls),
                                ParsedConfig.toValidationChecks(standard()),
                                new MemoryCertificateQuarantine())
                        .validateCertificateChain(
                                List.of(untrusted.getLeafCertificate(DOMAIN), untrusted.getRootCertificate()),
                                APPLICATION_URI,
                                new String[] {DOMAIN}))
                .isInstanceOf(UaException.class);
    }
}
