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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.cert.X509Certificate;
import java.util.List;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import util.KeyChain;

/**
 * Verifies the {@code trustLevel=TRUST} + identity cell (EDG-594): the chain is never built (any
 * certificate is accepted), but the requested identity checks are enforced on the end-entity
 * certificate.
 */
class TrustAnyIdentityCertificateValidatorTest {

    private static final String DOMAIN = "server";
    // KeyChain writes the same value into both the URI and the DNS-name SubjectAltName entries.
    private static final String SAN_VALUE = "urn:hivemq:edge:" + DOMAIN;

    private X509Certificate serverCert;

    @BeforeEach
    void setUp() throws Exception {
        // A leaf cert that does NOT chain to any provided trust anchor: if the validator built a
        // chain it would fail. Its SubjectAltName carries SAN_VALUE as both a URI and a DNS name.
        serverCert = KeyChain.createKeyChain(DOMAIN).getLeafCertificate(DOMAIN);
    }

    @Test
    void noChecks_acceptsAnyCertificateWithoutChainBuild() {
        final var validator = new TrustAnyIdentityCertificateValidator(false, false);
        assertThatCode(() ->
                        validator.validateCertificateChain(List.of(serverCert), "urn:whatever", new String[] {"nope"}))
                .doesNotThrowAnyException();
    }

    @Test
    void applicationUriCheck_matches_passes() {
        final var validator = new TrustAnyIdentityCertificateValidator(true, false);
        assertThatCode(() -> validator.validateCertificateChain(List.of(serverCert), SAN_VALUE, null))
                .doesNotThrowAnyException();
    }

    @Test
    void applicationUriCheck_mismatch_throws() {
        final var validator = new TrustAnyIdentityCertificateValidator(true, false);
        assertThatThrownBy(() -> validator.validateCertificateChain(List.of(serverCert), "urn:wrong:uri", null))
                .isInstanceOf(UaException.class);
    }

    @Test
    void applicationUriCheck_nullUri_isSkipped() {
        // Mirror DefaultClientCertificateValidator: an unsupplied applicationUri cannot be enforced.
        final var validator = new TrustAnyIdentityCertificateValidator(true, false);
        assertThatCode(() -> validator.validateCertificateChain(List.of(serverCert), null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void hostnameCheck_matches_passes() {
        final var validator = new TrustAnyIdentityCertificateValidator(false, true);
        assertThatCode(() -> validator.validateCertificateChain(List.of(serverCert), null, new String[] {SAN_VALUE}))
                .doesNotThrowAnyException();
    }

    @Test
    void hostnameCheck_mismatch_throws() {
        final var validator = new TrustAnyIdentityCertificateValidator(false, true);
        assertThatThrownBy(() ->
                        validator.validateCertificateChain(List.of(serverCert), null, new String[] {"not-the-server"}))
                .isInstanceOf(UaException.class);
    }

    @Test
    void bothChecks_bothMatch_passes() {
        final var validator = new TrustAnyIdentityCertificateValidator(true, true);
        assertThatCode(() ->
                        validator.validateCertificateChain(List.of(serverCert), SAN_VALUE, new String[] {SAN_VALUE}))
                .doesNotThrowAnyException();
    }

    @Test
    void bothChecks_hostnameMismatch_throws() {
        final var validator = new TrustAnyIdentityCertificateValidator(true, true);
        assertThatThrownBy(() ->
                        validator.validateCertificateChain(List.of(serverCert), SAN_VALUE, new String[] {"wrong-host"}))
                .isInstanceOf(UaException.class);
    }
}
