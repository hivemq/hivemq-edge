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

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509CRL;
import java.util.Base64;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the operator is told when the revocation list cannot be used.
 *
 * <p>Every failure here is a configuration mistake made once, at a keyboard, and paid for later in a
 * refused connection whose message does not mention the file. So each one names the file and the fix,
 * and none of them lets an unreadable list quietly become "check nothing" — the whole point of the
 * axis is that an unknown revocation status fails closed.
 */
class CertificateRevocationListsTest {

    @TempDir
    @NotNull
    Path tmp;

    @Test
    void aCrlFileIsLoaded() throws Exception {
        final Path crl = TestCertificates.writeCrl(tmp.resolve("ca.crl"), List.of());

        final List<X509CRL> loaded = CertificateRevocationLists.load(crl);

        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getIssuerX500Principal().getName()).contains("EDG-585");
    }

    @Test
    void aRevokedSerialSurvivesTheRoundTrip() throws Exception {
        final Path crl = TestCertificates.writeCrl(tmp.resolve("ca.crl"), List.of(BigInteger.valueOf(4242)));

        final List<X509CRL> loaded = CertificateRevocationLists.load(crl);

        assertThat(loaded.get(0).getRevokedCertificates()).hasSize(1);
        assertThat(loaded.get(0).getRevokedCertificates().iterator().next().getSerialNumber())
                .isEqualTo(BigInteger.valueOf(4242));
    }

    @Test
    void aDirectoryLoadsEveryCrlInIt() throws Exception {
        final Path dir = Files.createDirectory(tmp.resolve("crls"));
        TestCertificates.writeCrl(dir.resolve("a.crl"), List.of());
        TestCertificates.writeCrl(dir.resolve("b.crl"), List.of());

        assertThat(CertificateRevocationLists.load(dir)).hasSize(2);
    }

    @Test
    void aMissingPathNamesTheFileAndTheWayOut() {
        final Path absent = tmp.resolve("nope.crl");

        assertThatThrownBy(() -> CertificateRevocationLists.load(absent))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(absent.toString())
                .hasMessageContaining("does not exist")
                .as("an operator with no revocation infrastructure needs to be told the way out")
                .hasMessageContaining("revocation=NONE");
    }

    @Test
    void anEmptyDirectoryIsRefusedRatherThanReadAsNoRevocations() throws Exception {
        // The dangerous reading: "no CRLs found" is not "nothing is revoked".
        final Path dir = Files.createDirectory(tmp.resolve("empty"));

        assertThatThrownBy(() -> CertificateRevocationLists.load(dir))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(dir.toString())
                .hasMessageContaining("is empty");
    }

    @Test
    void aFileThatIsNotACrlIsRefusedNamingIt() throws Exception {
        // The likely mistake: pointing the path at the certificate or the truststore.
        final Path notACrl = Files.writeString(tmp.resolve("cert.pem"), TestCertificates.SOME_PEM_CERTIFICATE);

        assertThatThrownBy(() -> CertificateRevocationLists.load(notACrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(notACrl.toString())
                .hasMessageContaining("not a CRL");
    }

    @Test
    void garbageIsRefusedNamingTheFile() throws Exception {
        final Path garbage = Files.writeString(tmp.resolve("garbage.crl"), "this is not a revocation list\n");

        assertThatThrownBy(() -> CertificateRevocationLists.load(garbage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(garbage.toString());
    }

    @Test
    void derEncodingIsAcceptedAsWellAsPem() throws Exception {
        final Path pem = TestCertificates.writeCrl(tmp.resolve("ca.crl"), List.of());
        final X509CRL parsed = CertificateRevocationLists.load(pem).get(0);
        final Path der = Files.write(tmp.resolve("ca.der"), parsed.getEncoded());

        assertThat(CertificateRevocationLists.load(der)).hasSize(1);
    }

    /** Decoding helper kept beside the tests that need it rather than in the production surface. */
    static byte @NotNull [] decode(final @NotNull String base64) {
        return Base64.getMimeDecoder().decode(base64);
    }
}
