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

import java.math.BigInteger;
import java.nio.file.Path;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the validator is actually handed.
 *
 * <p>Milo decides revocation from {@code getTrustedCrls()} concatenated with {@code getIssuerCrls()}
 * and nothing else — see {@code DefaultClientCertificateValidator}. Both returned an empty list
 * unconditionally, which is why no revocation setting could ever be satisfied for a path through a
 * CA. These pin the wiring that carries a configured CRL as far as that call.
 */
class CertificateTrustListManagerTest {

    @TempDir
    @NotNull
    Path tmp;

    @Test
    void configuredCrlsReachBothListsMiloReads() throws Exception {
        final List<X509CRL> crls =
                CertificateRevocationLists.load(TestCertificates.writeCrl(tmp.resolve("ca.crl"), List.of()));

        final CertificateTrustListManager manager = new CertificateTrustListManager(List.of(), crls);

        assertThat(manager.getTrustedCrls()).isEqualTo(crls);
        assertThat(manager.getIssuerCrls()).isEqualTo(crls);
    }

    @Test
    void aRevokedSerialIsVisibleToTheValidator() throws Exception {
        final List<X509CRL> crls = CertificateRevocationLists.load(
                TestCertificates.writeCrl(tmp.resolve("ca.crl"), List.of(BigInteger.valueOf(7))));

        final CertificateTrustListManager manager = new CertificateTrustListManager(List.of(), crls);

        assertThat(manager.getTrustedCrls().get(0).getRevokedCertificates()).hasSize(1);
    }

    @Test
    void noConfiguredCrlsStillMeansEmpty() {
        // The default for every deployment that has no CA in the path, and it must stay empty rather
        // than becoming null - Milo calls addAll on both.
        final CertificateTrustListManager manager = new CertificateTrustListManager(List.of());

        assertThat(manager.getTrustedCrls()).isEmpty();
        assertThat(manager.getIssuerCrls()).isEmpty();
    }

    @Test
    void theTrustedCertificatesAreUnaffectedByTheCrls() throws Exception {
        final X509Certificate cert = TestCertificates.identityOnly();
        final List<X509CRL> crls =
                CertificateRevocationLists.load(TestCertificates.writeCrl(tmp.resolve("ca.crl"), List.of()));

        final CertificateTrustListManager manager = new CertificateTrustListManager(List.of(cert), crls);

        assertThat(manager.getTrustedCertificates()).containsExactly(cert);
    }

    @Test
    void theListsAreDefensivelyCopied() throws Exception {
        final List<X509CRL> crls = new java.util.ArrayList<>(
                CertificateRevocationLists.load(TestCertificates.writeCrl(tmp.resolve("ca.crl"), List.of())));

        final CertificateTrustListManager manager = new CertificateTrustListManager(List.of(), crls);
        crls.clear();

        assertThat(manager.getTrustedCrls()).hasSize(1);
    }
}
