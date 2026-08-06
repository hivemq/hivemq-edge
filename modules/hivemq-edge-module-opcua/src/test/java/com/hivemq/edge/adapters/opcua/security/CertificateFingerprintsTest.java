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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.HexFormat;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import util.KeyChain;

/**
 * The allow-list is transcribed by hand from a value a vendor e-mailed over, so the parser has to
 * accept the shapes humans actually produce — and refuse, loudly, anything it cannot be sure about.
 */
class CertificateFingerprintsTest {

    @TempDir
    Path tempDir;

    private static final String VALID = "a".repeat(64);
    private static final String OTHER = "b".repeat(64);

    @Test
    void fingerprintIsTheSha256OfTheDerEncoding() throws Exception {
        final X509Certificate certificate = KeyChain.createKeyChain("host").getRootCertificate();

        final String expected =
                HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded()));

        assertThat(CertificateFingerprints.fingerprintOf(certificate)).isEqualTo(expected);
    }

    @Test
    void fingerprintIsStableAndLowerCaseHex() throws Exception {
        final X509Certificate certificate = KeyChain.createKeyChain("host").getRootCertificate();

        final String first = CertificateFingerprints.fingerprintOf(certificate);
        assertThat(first).hasSize(64).isLowerCase().matches("[0-9a-f]{64}");
        assertThat(CertificateFingerprints.fingerprintOf(certificate)).isEqualTo(first);
    }

    @Test
    void twoDifferentCertificatesHaveDifferentFingerprints() throws Exception {
        assertThat(CertificateFingerprints.fingerprintOf(
                        KeyChain.createKeyChain("a").getRootCertificate()))
                .isNotEqualTo(CertificateFingerprints.fingerprintOf(
                        KeyChain.createKeyChain("b").getRootCertificate()));
    }

    @Test
    void plainFingerprintsAreRead() throws Exception {
        final Path file = write(VALID + "\n" + OTHER + "\n");
        assertThat(CertificateFingerprints.loadAllowList(file)).containsExactlyInAnyOrder(VALID, OTHER);
    }

    @Test
    void colonSeparatedUpperCaseFingerprintsAreRead() throws Exception {
        // The form openssl and most UIs print.
        final Path file = write(CertificateFingerprints.toDisplayForm(VALID).toUpperCase(java.util.Locale.ROOT) + "\n");
        assertThat(CertificateFingerprints.loadAllowList(file)).containsExactly(VALID);
    }

    @Test
    void commentsBlankLinesAndSurroundingWhitespaceAreIgnored() throws Exception {
        final Path file = write("# machine 1, from the vendor\n"
                + "\n"
                + "   " + VALID + "   # inline comment\n"
                + "\n"
                + "   \n"
                + OTHER + "\n");

        assertThat(CertificateFingerprints.loadAllowList(file)).containsExactlyInAnyOrder(VALID, OTHER);
    }

    @Test
    void duplicatesAreCollapsed() throws Exception {
        final Path file = write(VALID + "\n" + CertificateFingerprints.toDisplayForm(VALID) + "\n");
        assertThat(CertificateFingerprints.loadAllowList(file)).containsExactly(VALID);
    }

    @Test
    void aMalformedLineFailsTheWholeFile() throws Exception {
        // Skipping the bad line would shrink the allow-list without telling anyone, and the operator
        // would then debug a rejected connection whose entry is visibly present in the file.
        final Path file = write(VALID + "\n" + "not-a-fingerprint\n" + OTHER + "\n");

        assertThatThrownBy(() -> CertificateFingerprints.loadAllowList(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("line 2")
                .hasMessageContaining("not-a-fingerprint");
    }

    @Test
    void aTruncatedFingerprintIsMalformed() throws Exception {
        final Path file = write("a".repeat(63) + "\n");
        assertThatThrownBy(() -> CertificateFingerprints.loadAllowList(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anSha1FingerprintIsMalformed() throws Exception {
        // A plausible operator mistake: pasting the SHA-1 fingerprint a tool showed instead.
        final Path file = write("a".repeat(40) + "\n");
        assertThatThrownBy(() -> CertificateFingerprints.loadAllowList(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SHA-256");
    }

    @Test
    void nonHexCharactersAreMalformed() throws Exception {
        final Path file = write("g".repeat(64) + "\n");
        assertThatThrownBy(() -> CertificateFingerprints.loadAllowList(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anEmptyFileIsRejected() throws Exception {
        final Path file = write("# nothing here yet\n\n");
        assertThatThrownBy(() -> CertificateFingerprints.loadAllowList(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no fingerprints");
    }

    @Test
    void aLeadingUtf8BomIsStripped() throws Exception {
        // A Windows editor saving "UTF-8" typically prepends a byte order mark; the file must not
        // fail with an invisible-character message.
        final Path file = tempDir.resolve("bom-allow-list.txt");
        Files.write(file, ("\uFEFF" + VALID + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(CertificateFingerprints.loadAllowList(file)).containsExactly(VALID);
    }

    @Test
    void aFileThatIsNotUtf8IsRejectedNamingTheEncoding() throws Exception {
        // 0xFF 0xFE is a UTF-16 BOM and malformed as UTF-8. The error must name the encoding: the
        // generic could-not-be-read message points at paths and permissions, the wrong hunt entirely.
        final Path file = tempDir.resolve("utf16-allow-list.txt");
        Files.write(file, new byte[] {(byte) 0xFF, (byte) 0xFE, 'a', 0, 'b', 0});

        assertThatThrownBy(() -> CertificateFingerprints.loadAllowList(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not UTF-8");
    }

    @Test
    void aMissingFileIsAnIoError() {
        assertThatThrownBy(() -> CertificateFingerprints.loadAllowList(tempDir.resolve("absent.txt")))
                .isInstanceOf(IOException.class);
    }

    @Test
    void displayFormIsColonSeparatedAndParsesBack() throws Exception {
        final String display = CertificateFingerprints.toDisplayForm(VALID);

        assertThat(display).contains(":").hasSize(64 + 31);
        final Path file = write(display + "\n");
        assertThat(CertificateFingerprints.loadAllowList(file)).containsExactly(VALID);
    }

    @Test
    void theLoadedSetIsImmutable() throws Exception {
        final Set<String> loaded = CertificateFingerprints.loadAllowList(write(VALID + "\n"));
        assertThatThrownBy(() -> loaded.add(OTHER)).isInstanceOf(UnsupportedOperationException.class);
    }

    private Path write(final String content) throws IOException {
        final Path file = tempDir.resolve("allow-list-" + content.hashCode() + ".txt");
        Files.writeString(file, content);
        return file;
    }
}
