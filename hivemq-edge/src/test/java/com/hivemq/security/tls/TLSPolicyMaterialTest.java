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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.hivemq.security.tls.TLSConstants.HostnameCheck;
import com.hivemq.security.tls.TLSConstants.KeyUsageCheck;
import com.hivemq.security.tls.TLSConstants.RevocationCheck;
import com.hivemq.security.tls.TLSConstants.SanUriCheck;
import com.hivemq.security.tls.TLSConstants.TrustMode;
import com.hivemq.security.tls.TLSConstants.ValidityCheck;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * What the loader does with the files a configuration names.
 *
 * <p>The property worth pinning is that every way of failing to read material refuses the surface
 * rather than producing a policy that checks less than the configuration asked for. A truststore that
 * cannot be opened must not become an empty set of anchors, and an allow-list with one bad line must
 * not become an allow-list missing one entry.
 */
class TLSPolicyMaterialTest {

    @TempDir
    @SuppressWarnings("NullAway.Init")
    Path directory;

    // ----- allow-list -----

    @Test
    void anAllowListIsReadWithOrWithoutColons() throws Exception {
        // 64 hex digits, which is what a SHA-256 fingerprint is.
        final String withColons = String.join(":", java.util.Collections.nCopies(32, "EF"));
        final String without = "cd".repeat(32);
        final Path file = write("allow.txt", withColons + "\n" + without + "\n");

        final TLSPolicy policy = new TLSPolicy(allowListConfig(file));

        // Both forms normalise to lower-case hex without separators, which is what a fingerprint
        // taken from a certificate is compared against.
        assertThat(policy.allowedFingerprints())
                .containsExactlyInAnyOrder(withColons.replace(":", "").toLowerCase(java.util.Locale.ROOT), without);
    }

    @Test
    void anAllowListSkipsBlankLinesAndComments() throws Exception {
        final String fingerprint = "ab".repeat(32);
        final Path file = write("allow.txt", "# a comment\n\n   \n" + fingerprint + "  # trailing\n");

        assertThat(new TLSPolicy(allowListConfig(file)).allowedFingerprints()).containsExactly(fingerprint);
    }

    @Test
    void oneMalformedLineRefusesTheWholeAllowList() throws Exception {
        // The failure this trust mode exists to prevent: an entry an operator meant to trust that is
        // silently not trusted. Naming the line is what makes it fixable.
        final Path file = write("allow.txt", "ab".repeat(32) + "\nnot-a-fingerprint\n");

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("line 2")
                .hasMessageContaining("not-a-fingerprint");
    }

    @Test
    void anEmptyAllowListIsRefused() throws Exception {
        // An allow-list holding nothing would trust nothing, so every connection would fail with
        // nothing pointing at the empty file.
        final Path file = write("allow.txt", "# nothing but a comment\n");

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("no fingerprint");
    }

    @Test
    void anAbsentAllowListIsRefusedByName() {
        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(directory.resolve("absent.txt"))))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("absent.txt")
                .hasMessageContaining("does not exist");
    }

    @Test
    void severalAllowListFilesMerge() throws Exception {
        final String first = "ab".repeat(32);
        final String second = "cd".repeat(32);
        write("one.txt", first + "\n");
        write("two.txt", second + "\n");

        final TLSConfig config = new StubConfig(
                TrustMode.ALLOW_LIST,
                Optional.of(new MaterialRef.Inline(
                        null,
                        new FileSet(
                                List.of(
                                        directory.resolve("one.txt").toString(),
                                        directory.resolve("two.txt").toString()),
                                List.of(),
                                List.of(),
                                List.of()),
                        null,
                        null,
                        null)));

        assertThat(new TLSPolicy(config).allowedFingerprints()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void aDirectoryOfAllowListsMerges() throws Exception {
        write("one.txt", "ab".repeat(32) + "\n");
        write("two.txt", "cd".repeat(32) + "\n");

        final TLSConfig config = new StubConfig(
                TrustMode.ALLOW_LIST,
                Optional.of(new MaterialRef.Inline(
                        null,
                        new FileSet(
                                List.of(), List.of(new FileSet.Directory(directory.toString())), List.of(), List.of()),
                        null,
                        null,
                        null)));

        assertThat(new TLSPolicy(config).allowedFingerprints()).hasSize(2);
    }

    // ----- what the trust mode does not consult -----

    @Test
    void anAllowListIsNotReadUnderChainTrust() throws Exception {
        // A broken file an operator left behind must not refuse a surface that would never read it.
        write("allow.txt", "this is not a fingerprint\n");

        final TLSConfig config = new StubConfig(
                TrustMode.CHAIN,
                Optional.of(new MaterialRef.Inline(
                        null, FileSet.of(directory.resolve("allow.txt").toString()), null, null, null)));

        assertThat(new TLSPolicy(config).allowedFingerprints()).isEmpty();
    }

    // ----- truststore -----

    @Test
    void aTruststoreYieldsOneAnchorPerCertificate() throws Exception {
        final X509Certificate certificate = selfSigned("CN=anchor", Duration.ofDays(1));
        final Path file = directory.resolve("trust.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, "secret".toCharArray());
        store.setCertificateEntry("anchor", certificate);
        try (final var out = Files.newOutputStream(file)) {
            store.store(out, "secret".toCharArray());
        }

        final TLSConfig config = new StubConfig(
                TrustMode.CHAIN,
                Optional.empty(),
                Optional.of(new MaterialRef.Inline(
                        "p12", FileSet.of(file.toString()), new Secret.Literal("secret"), null, null)));

        assertThat(new TLSPolicy(config).trustAnchors())
                .singleElement()
                .satisfies(anchor -> assertThat(anchor.getTrustedCert()).isEqualTo(certificate));
    }

    @Test
    void aTruststoreWithTheWrongPasswordIsRefusedNamingBothCauses() throws Exception {
        final Path file = directory.resolve("trust.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, "right".toCharArray());
        store.setCertificateEntry("anchor", selfSigned("CN=anchor", Duration.ofDays(1)));
        try (final var out = Files.newOutputStream(file)) {
            store.store(out, "right".toCharArray());
        }

        final TLSConfig config = new StubConfig(
                TrustMode.CHAIN,
                Optional.empty(),
                Optional.of(new MaterialRef.Inline(
                        "p12", FileSet.of(file.toString()), new Secret.Literal("wrong"), null, null)));

        // A wrong password and a wrong declared format both surface the same way and have different
        // fixes, so the message names both rather than guessing.
        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("password")
                .hasMessageContaining("format");
    }

    // ----- passwords from files -----

    @Test
    void aPasswordIsReadFromItsFileWithoutTheTrailingNewline() throws Exception {
        // How a password reaches a container on Kubernetes: projected into the pod as a file, which
        // an editor or the projection commonly terminates with a newline.
        final Path passwordFile = write("password", "secret\n");
        final Path file = directory.resolve("trust.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, "secret".toCharArray());
        store.setCertificateEntry("anchor", selfSigned("CN=anchor", Duration.ofDays(1)));
        try (final var out = Files.newOutputStream(file)) {
            store.store(out, "secret".toCharArray());
        }

        final TLSConfig config = new StubConfig(
                TrustMode.CHAIN,
                Optional.empty(),
                Optional.of(new MaterialRef.Inline(
                        "p12", FileSet.of(file.toString()), new Secret.FromFile(passwordFile.toString()), null, null)));

        assertThat(new TLSPolicy(config).trustAnchors()).hasSize(1);
    }

    @Test
    void anAbsentPasswordFileIsRefusedByName() throws Exception {
        final Path file = write("trust.p12", "not a container");

        final TLSConfig config = new StubConfig(
                TrustMode.CHAIN,
                Optional.empty(),
                Optional.of(new MaterialRef.Inline(
                        "p12",
                        FileSet.of(file.toString()),
                        new Secret.FromFile(directory.resolve("absent").toString()),
                        null,
                        null)));

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("password file")
                .hasMessageContaining("does not exist");
    }

    // ----- identity -----

    @Test
    void anIdentityHoldingTwoKeysIsRefusedUnlessOneIsNamed() throws Exception {
        // Neither PKCS#12 nor JKS specifies the order entries come back in, so picking one would be
        // arbitrary and could change when the file is re-exported - presenting a different
        // certificate with nothing to say it had changed.
        final Path file = directory.resolve("identity.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, "secret".toCharArray());
        addKey(store, "first");
        addKey(store, "second");
        try (final var out = Files.newOutputStream(file)) {
            store.store(out, "secret".toCharArray());
        }

        final TLSConfig withoutEntry = identityConfig(file, null);
        assertThatThrownBy(() -> new TLSPolicy(withoutEntry))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("ambiguous");

        assertThat(new TLSPolicy(identityConfig(file, "first")).identity()).isPresent();
    }

    @Test
    void anIdentityHoldingOneKeyNeedsNoEntryName() throws Exception {
        final Path file = directory.resolve("identity.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, "secret".toCharArray());
        addKey(store, "only");
        try (final var out = Files.newOutputStream(file)) {
            store.store(out, "secret".toCharArray());
        }

        assertThat(new TLSPolicy(identityConfig(file, null)).identity()).isPresent();
    }

    @Test
    void aContainerIdentityOffersTheKeyAndTheChainAsWellAsAFactory() throws Exception {
        // Milo signs the OPC UA handshake itself and wants the key and certificates as objects; a
        // bridge hands the JDK a factory. Both come from the one read, so they cannot disagree.
        final Path file = directory.resolve("identity.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, "secret".toCharArray());
        addKey(store, "only");
        try (final var out = Files.newOutputStream(file)) {
            store.store(out, "secret".toCharArray());
        }

        final TLSPolicy.Identity identity =
                new TLSPolicy(identityConfig(file, null)).identity().orElseThrow();

        assertThat(identity.privateKey()).isNotNull();
        assertThat(identity.keyManagerFactory()).isNotNull();
        assertThat(identity.chain()).isNotEmpty();
        assertThat(identity.chain().get(0)).isEqualTo(identity.certificate());
    }

    // ----- identity from PEM -----

    @Test
    void aPemIdentityIsReadFromSeparateKeyAndCertificateFiles() throws Exception {
        final KeyPair pair = rsaPair();
        final Path keyFile = writePemKey("key.pem", pair);
        final Path certFile = writePemCertificate("cert.pem", selfSigned("CN=leaf", Duration.ofDays(1), pair));

        final TLSPolicy.Identity identity = new TLSPolicy(pemIdentityConfig(null, keyFile, certFile))
                .identity()
                .orElseThrow();

        assertThat(identity.privateKey().getAlgorithm()).isEqualTo("RSA");
        assertThat(identity.certificate().getSubjectX500Principal().getName()).contains("CN=leaf");
        assertThat(identity.keyManagerFactory()).isNotNull();
    }

    @Test
    void aPemIdentityMayHoldTheKeyAndCertificateInOneFile() throws Exception {
        // What `openssl` writes by default, and what a Kubernetes secret often projects.
        final KeyPair pair = rsaPair();
        final Path combined = directory.resolve("both.pem");
        Files.writeString(combined, pemKey(pair) + pemCertificate(selfSigned("CN=leaf", Duration.ofDays(1), pair)));

        assertThat(new TLSPolicy(pemIdentityConfig(null, combined)).identity()).isPresent();
    }

    @Test
    void aPemChainPutsTheKeysOwnCertificateFirstWhateverTheFileOrder() throws Exception {
        // A PEM file imposes no order, so the leaf is found by matching the key rather than by
        // position: Milo and the TLS handshake both require the leaf first. The other certificate is
        // deliberately one that does not sign the leaf, which is also what pins that loading our own
        // key does not pre-judge whether its chain reaches a trusted authority.
        final KeyPair leafPair = rsaPair();
        final KeyPair otherPair = rsaPair();
        final Path file = directory.resolve("chain.pem");
        Files.writeString(
                file,
                pemCertificate(selfSigned("CN=other", Duration.ofDays(1), otherPair))
                        + pemKey(leafPair)
                        + pemCertificate(selfSigned("CN=leaf", Duration.ofDays(1), leafPair)));

        final TLSPolicy.Identity identity =
                new TLSPolicy(pemIdentityConfig(null, file)).identity().orElseThrow();

        assertThat(identity.certificate().getSubjectX500Principal().getName()).contains("CN=leaf");
        assertThat(identity.chain()).hasSize(2);
        assertThat(identity.chain().get(0).getSubjectX500Principal().getName()).contains("CN=leaf");
    }

    @Test
    void anEncryptedPemKeyIsReadWithItsPassword() throws Exception {
        // Written by `openssl pkcs8 -topk8`, which is the common way to hold a key at rest.
        final KeyPair pair = rsaPair();
        final Path keyFile = writeEncryptedPemKey("key.pem", pair, "secret");
        final Path certFile = writePemCertificate("cert.pem", selfSigned("CN=leaf", Duration.ofDays(1), pair));

        assertThat(new TLSPolicy(pemIdentityConfig("secret", keyFile, certFile)).identity())
                .isPresent();
    }

    @Test
    void anEncryptedPemKeyWithNoPasswordIsRefused() throws Exception {
        final KeyPair pair = rsaPair();
        final Path keyFile = writeEncryptedPemKey("key.pem", pair, "secret");
        final Path certFile = writePemCertificate("cert.pem", selfSigned("CN=leaf", Duration.ofDays(1), pair));
        final TLSConfig config = pemIdentityConfig(null, keyFile, certFile);

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("encrypted");
    }

    @Test
    void aPemIdentityWithNoKeyIsRefused() throws Exception {
        final KeyPair pair = rsaPair();
        final Path certFile = writePemCertificate("cert.pem", selfSigned("CN=leaf", Duration.ofDays(1), pair));
        final TLSConfig config = pemIdentityConfig(null, certFile);

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("no private key");
    }

    @Test
    void aPemIdentityWithNoCertificateIsRefused() throws Exception {
        final Path keyFile = writePemKey("key.pem", rsaPair());
        final TLSConfig config = pemIdentityConfig(null, keyFile);

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("no certificate");
    }

    @Test
    void aPemCertificateThatDoesNotBelongToTheKeyIsRefused() throws Exception {
        // The pair has to be a pair. Presenting a certificate that does not match the key produces a
        // handshake failure far from the configuration that caused it.
        final Path keyFile = writePemKey("key.pem", rsaPair());
        final Path certFile = writePemCertificate("cert.pem", selfSigned("CN=other", Duration.ofDays(1), rsaPair()));
        final TLSConfig config = pemIdentityConfig(null, keyFile, certFile);

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("belongs to the private key");
    }

    @Test
    void aPemIdentityNamingTwoKeysIsRefused() throws Exception {
        final Path file = directory.resolve("two.pem");
        Files.writeString(file, pemKey(rsaPair()) + pemKey(rsaPair()));
        final TLSConfig config = pemIdentityConfig(null, file);

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("more than one private key");
    }

    @Test
    void anIdentityNamingAnAbsentEntryIsRefused() throws Exception {
        final Path file = directory.resolve("identity.p12");
        final KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(null, "secret".toCharArray());
        addKey(store, "present");
        try (final var out = Files.newOutputStream(file)) {
            store.store(out, "secret".toCharArray());
        }

        assertThatThrownBy(() -> new TLSPolicy(identityConfig(file, "absent")))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("absent");
    }

    // ----- what an operator sees when an allow-list is slightly wrong -----

    @Test
    void duplicateFingerprintsCollapse() throws Exception {
        final String fingerprint = "ab".repeat(32);
        final Path file = write("allow.txt", fingerprint + "\n" + TLSPolicy.toDisplayForm(fingerprint) + "\n");

        assertThat(new TLSPolicy(allowListConfig(file)).allowedFingerprints()).containsExactly(fingerprint);
    }

    @Test
    void aTruncatedFingerprintIsMalformed() throws Exception {
        final Path file = write("allow.txt", "a".repeat(63) + "\n");

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class);
    }

    @Test
    void anSha1FingerprintIsMalformedAndSaysWhichIsWanted() throws Exception {
        // A plausible mistake: pasting the SHA-1 fingerprint a tool happened to show.
        final Path file = write("allow.txt", "a".repeat(40) + "\n");

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("SHA-256");
    }

    @Test
    void nonHexCharactersAreMalformed() throws Exception {
        final Path file = write("allow.txt", "g".repeat(64) + "\n");

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class);
    }

    @Test
    void aLeadingByteOrderMarkIsStripped() throws Exception {
        // A Windows editor saving "UTF-8" commonly writes one first; the file must not fail with a
        // message about an invisible character.
        final String fingerprint = "ab".repeat(32);
        final Path file = directory.resolve("bom.txt");
        Files.write(file, ("﻿" + fingerprint + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThat(new TLSPolicy(allowListConfig(file)).allowedFingerprints()).containsExactly(fingerprint);
    }

    @Test
    void aFileThatIsNotUtf8IsRejectedNamingTheEncoding() throws Exception {
        // Named apart from a read failure: that message sends an operator to check paths and
        // permissions, which is the wrong hunt for an encoding problem.
        final Path file = directory.resolve("utf16.txt");
        Files.write(file, new byte[] {(byte) 0xFF, (byte) 0xFE, 'a', 0, 'b', 0});

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("not UTF-8");
    }

    @Test
    void aMissingFileSaysSoAndNamesThePathWithoutAJavaClassName() {
        final Path absent = directory.resolve("absent.txt");

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(absent)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("does not exist")
                .hasMessageContaining(absent.toString())
                .hasMessageNotContaining("NoSuchFileException")
                .hasMessageNotContaining("java.nio");
    }

    @Test
    void aDirectoryWhereAFileWasExpectedIsNamedAsSuch() throws Exception {
        // Three ways to be unreadable, three different fixes, so each is named rather than one being
        // guessed at.
        final Path asDirectory = Files.createDirectory(directory.resolve("allow-list-dir"));

        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(asDirectory)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("is a directory")
                .hasMessageContaining(asDirectory.toString())
                .hasMessageNotContaining("does not exist");
    }

    @Test
    void anUnreadableFileNamesPermissionsRatherThanAMissingFile() throws Exception {
        final Path file = write("allow.txt", "ab".repeat(32) + "\n");
        assumeTrue(
                file.getFileSystem().supportedFileAttributeViews().contains("posix"),
                "POSIX permissions are needed to make a file unreadable");
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("-w--w----"));
        assumeTrue(!Files.isReadable(file), "running as a user that can read regardless of permissions");

        try {
            assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("not readable")
                    .hasMessageContaining(file.toString())
                    .hasMessageNotContaining("does not exist")
                    .hasMessageNotContaining("AccessDeniedException");
        } finally {
            // Left readable so the temporary directory can be cleaned up.
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        }
    }

    @Test
    void theUnderlyingIoErrorIsKeptAsTheCause() {
        // So a genuinely unexpected failure is still diagnosable from a stack trace.
        assertThatThrownBy(() -> new TLSPolicy(allowListConfig(directory.resolve("absent.txt"))))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    @Test
    void theDisplayFormIsColonSeparatedAndReadsBack() throws Exception {
        // The form an operator copies out of a rejection message is one the file accepts.
        final String fingerprint = "ab".repeat(32);
        final String display = TLSPolicy.toDisplayForm(fingerprint);

        assertThat(display).contains(":").hasSize(64 + 31);
        assertThat(new TLSPolicy(allowListConfig(write("allow.txt", display + "\n"))).allowedFingerprints())
                .containsExactly(fingerprint);
    }

    @Test
    void theLoadedFingerprintsAreImmutable() throws Exception {
        final Set<String> loaded =
                new TLSPolicy(allowListConfig(write("allow.txt", "ab".repeat(32) + "\n"))).allowedFingerprints();

        assertThatThrownBy(() -> loaded.add("cd".repeat(32))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aFingerprintIsTheSha256OfTheDerEncoding() throws Exception {
        final X509Certificate certificate = selfSigned("CN=anchor", Duration.ofDays(1));
        final byte[] expected =
                java.security.MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());

        assertThat(TLSPolicy.fingerprintOf(certificate))
                .isEqualTo(java.util.HexFormat.of().formatHex(expected))
                .isLowerCase()
                .hasSize(64);
    }

    @Test
    void twoDifferentCertificatesHaveDifferentFingerprints() throws Exception {
        assertThat(TLSPolicy.fingerprintOf(selfSigned("CN=one", Duration.ofDays(1))))
                .isNotEqualTo(TLSPolicy.fingerprintOf(selfSigned("CN=two", Duration.ofDays(1))));
    }

    // ----- revocation lists -----

    @Test
    void aRevocationListIsReadAsPemOrDer() throws Exception {
        // One call reads both encodings, so no format needs declaring on a revocation list - which is
        // why the model has no format element there.
        final X509CRL crl = emptyCrl();
        final Path pem = write("ca.crl", toPem(crl));
        final Path der = Files.write(directory.resolve("ca.der"), crl.getEncoded());

        assertThat(new TLSPolicy(revocationConfig(pem)).revocationLists()).hasSize(1);
        assertThat(new TLSPolicy(revocationConfig(der)).revocationLists()).hasSize(1);
    }

    @Test
    void aFileThatIsNotARevocationListIsRefusedNamingIt() throws Exception {
        // The likely mistake: pointing the path at the certificate or the truststore instead.
        final Path notACrl = write("cert.pem", toPem(selfSigned("CN=anchor", Duration.ofDays(1))));

        assertThatThrownBy(() -> new TLSPolicy(revocationConfig(notACrl)))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining(notACrl.toString());
    }

    // ----- an empty directory or pattern -----

    @Test
    void anEmptyDirectoryIsRefusedByDefault() throws Exception {
        // The dangerous reading this prevents: for a revocation list, "no files found" silently
        // becoming "nothing is revoked".
        final Path empty = Files.createDirectory(directory.resolve("empty"));
        final TLSConfig config = new StubConfig(
                TrustMode.ALLOW_LIST,
                Optional.of(new MaterialRef.Inline(
                        null,
                        new FileSet(List.of(), List.of(new FileSet.Directory(empty.toString())), List.of(), List.of()),
                        null,
                        null,
                        null)));

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining(empty.toString())
                .hasMessageContaining("may-be-empty");
    }

    @Test
    void anEmptyDirectoryIsAcceptedWhereTheOperatorPermittedIt() throws Exception {
        // A directory a deployment populates later is legitimately empty today, so an operator can
        // say so - and then it is a warning rather than a refusal.
        final Path empty = Files.createDirectory(directory.resolve("later"));
        final Path withOne = write("allow.txt", "ab".repeat(32) + "\n");

        final TLSConfig config = new StubConfig(
                TrustMode.ALLOW_LIST,
                Optional.of(new MaterialRef.Inline(
                        null,
                        new FileSet(
                                List.of(withOne.toString()),
                                List.of(new FileSet.Directory(empty.toString(), true)),
                                List.of(),
                                List.of()),
                        null,
                        null,
                        null)));

        assertThat(new TLSPolicy(config).allowedFingerprints()).hasSize(1);
    }

    @Test
    void anEmptyPatternIsRefusedByDefault() {
        final TLSConfig config = new StubConfig(
                TrustMode.ALLOW_LIST,
                Optional.of(new MaterialRef.Inline(
                        null,
                        new FileSet(
                                List.of(),
                                List.of(),
                                List.of(new FileSet.Pattern(
                                        directory.resolve("*.nothing").toString())),
                                List.of()),
                        null,
                        null,
                        null)));

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("may-be-empty");
    }

    // ----- the runtime's own truststore -----

    @Test
    void theRuntimesOwnTruststoreNeedsNoPathAndNoPassword() throws Exception {
        // How an operator asks for the public certificate authorities: by name, not by path, so the
        // configuration is portable between JVM builds and container images.
        final StubConfig config = new StubConfig(TrustMode.CHAIN, Optional.empty());
        config.truststore = Optional.of(new MaterialRef.Inline(null, FileSet.ofJvmCacerts(), null, null, null));

        assertThat(new TLSPolicy(config).trustAnchors()).isNotEmpty();
    }

    @Test
    void aMisspelledRuntimeTruststoreIsRefusedNamingTheOneItKnows() {
        // Refused as a missing file is refused: silently yielding no anchors would leave a truststore
        // that trusts nothing while the configuration says otherwise.
        final StubConfig config = new StubConfig(TrustMode.CHAIN, Optional.empty());
        config.truststore = Optional.of(new MaterialRef.Inline(
                null, new FileSet(List.of(), List.of(), List.of(), List.of("jvm-cacerts")), null, null, null));

        assertThatThrownBy(() -> new TLSPolicy(config))
                .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                .hasMessageContaining("jvm-cacerts")
                .hasMessageContaining(FileSet.JVM_CACERTS);
    }

    @Test
    void anAbsentTruststoreTrustsNothing() throws Exception {
        // The default that matters: a configuration whose truststore was forgotten, or whose path
        // went missing with a new image, trusts nothing rather than every public authority.
        assertThat(new TLSPolicy(new StubConfig(TrustMode.CHAIN, Optional.empty())).trustAnchors())
                .isEmpty();
    }

    // ----- statelessness -----

    @Test
    void loadingTwiceYieldsTheSameMaterial() throws Exception {
        final Path file = write("allow.txt", "ab".repeat(32) + "\n");

        assertThat(new TLSPolicy(allowListConfig(file)).allowedFingerprints())
                .isEqualTo(new TLSPolicy(allowListConfig(file)).allowedFingerprints());
    }

    // ----- helpers -----

    private @NotNull Path write(final @NotNull String name, final @NotNull String content) throws IOException {
        final Path file = directory.resolve(name);
        Files.writeString(file, content);
        return file;
    }

    private @NotNull TLSConfig revocationConfig(final @NotNull Path file) {
        final StubConfig config = new StubConfig(TrustMode.CHAIN, Optional.empty());
        config.revocationList =
                Optional.of(new MaterialRef.Inline(null, FileSet.of(file.toString()), null, null, null));
        return config;
    }

    /** A revocation list with nothing revoked, which is still a revocation list. */
    private static @NotNull X509CRL emptyCrl() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        final KeyPair pair = generator.generateKeyPair();
        final Instant now = Instant.now();
        return new org.bouncycastle.cert.jcajce.JcaX509CRLConverter()
                .getCRL(new org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder(
                                new X500Principal("CN=issuer"), Date.from(now))
                        .setNextUpdate(Date.from(now.plus(Duration.ofDays(7))))
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
    }

    private static @NotNull String toPem(final @NotNull X509CRL crl) throws Exception {
        return "-----BEGIN X509 CRL-----\n"
                + java.util.Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(crl.getEncoded())
                + "\n-----END X509 CRL-----\n";
    }

    private static @NotNull String toPem(final @NotNull X509Certificate certificate) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n"
                + java.util.Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
    }

    private @NotNull TLSConfig allowListConfig(final @NotNull Path file) {
        return new StubConfig(
                TrustMode.ALLOW_LIST,
                Optional.of(new MaterialRef.Inline(null, FileSet.of(file.toString()), null, null, null)));
    }

    private @NotNull TLSConfig identityConfig(
            final @NotNull Path file, final @org.jetbrains.annotations.Nullable String entry) {
        final StubConfig config = new StubConfig(TrustMode.ANY_CERT, Optional.empty());
        config.identity = Optional.of(new MaterialRef.Inline(
                "p12", FileSet.of(file.toString()), new Secret.Literal("secret"), entry, new Secret.Literal("secret")));
        return config;
    }

    /// A PEM identity over the given files, with an optional key password.
    private @NotNull TLSConfig pemIdentityConfig(
            final @org.jetbrains.annotations.Nullable String keyPassword, final @NotNull Path... files) {
        final StubConfig config = new StubConfig(TrustMode.ANY_CERT, Optional.empty());
        config.identity = Optional.of(new MaterialRef.Inline(
                "pem",
                new FileSet(
                        java.util.Arrays.stream(files).map(Path::toString).toList(), List.of(), List.of(), List.of()),
                null,
                null,
                keyPassword == null ? null : new Secret.Literal(keyPassword)));
        return config;
    }

    private static @NotNull KeyPair rsaPair() throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private @NotNull Path writePemKey(final @NotNull String name, final @NotNull KeyPair pair) throws Exception {
        return write(name, pemKey(pair));
    }

    private @NotNull Path writePemCertificate(
            final @NotNull String name, final @NotNull java.security.cert.X509Certificate certificate)
            throws Exception {
        return write(name, pemCertificate(certificate));
    }

    /// An unencrypted PKCS#8 key, which is what `openssl pkcs8 -topk8 -nocrypt` writes.
    private static @NotNull String pemKey(final @NotNull KeyPair pair) {
        return "-----BEGIN PRIVATE KEY-----\n"
                + java.util.Base64.getMimeEncoder(64, new byte[] {'\n'})
                        .encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }

    private static @NotNull String pemCertificate(final @NotNull java.security.cert.X509Certificate certificate)
            throws Exception {
        return "-----BEGIN CERTIFICATE-----\n"
                + java.util.Base64.getMimeEncoder(64, new byte[] {'\n'}).encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
    }

    /// An encrypted PKCS#8 key, which is what `openssl pkcs8 -topk8` writes by default.
    private @NotNull Path writeEncryptedPemKey(
            final @NotNull String name, final @NotNull KeyPair pair, final @NotNull String password) throws Exception {
        final java.io.StringWriter out = new java.io.StringWriter();
        try (final org.bouncycastle.openssl.jcajce.JcaPEMWriter writer =
                new org.bouncycastle.openssl.jcajce.JcaPEMWriter(out)) {
            writer.writeObject(new org.bouncycastle.openssl.jcajce.JcaPKCS8Generator(
                    pair.getPrivate(),
                    new org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder(
                                    org.bouncycastle.asn1.nist.NISTObjectIdentifiers.id_aes256_CBC)
                            .setProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
                            .setPassword(password.toCharArray())
                            .build()));
        }
        return write(name, out.toString());
    }

    private static void addKey(final @NotNull KeyStore store, final @NotNull String alias) throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        final KeyPair pair = generator.generateKeyPair();
        store.setKeyEntry(alias, pair.getPrivate(), "secret".toCharArray(), new java.security.cert.Certificate[] {
            selfSigned("CN=" + alias, Duration.ofDays(1), pair)
        });
    }

    private static @NotNull X509Certificate selfSigned(final @NotNull String subject, final @NotNull Duration validFor)
            throws Exception {
        final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return selfSigned(subject, validFor, generator.generateKeyPair());
    }

    private static @NotNull X509Certificate selfSigned(
            final @NotNull String subject, final @NotNull Duration validFor, final @NotNull KeyPair pair)
            throws Exception {
        final Instant now = Instant.now();
        final X500Principal principal = new X500Principal(subject);
        return new JcaX509CertificateConverter()
                .getCertificate(new JcaX509v3CertificateBuilder(
                                principal,
                                BigInteger.valueOf(now.toEpochMilli()),
                                Date.from(now.minus(Duration.ofHours(1))),
                                Date.from(now.plus(validFor)),
                                principal,
                                pair.getPublic())
                        .build(new JcaContentSignerBuilder("SHA256withRSA").build(pair.getPrivate())));
    }

    /** A configuration naming only what a case needs; every check at its strictest. */
    private static final class StubConfig implements TLSConfig {

        private final @NotNull TrustMode trustMode;
        private @NotNull Optional<MaterialRef> allowList;
        private @NotNull Optional<MaterialRef> truststore = Optional.empty();
        private @NotNull Optional<MaterialRef> identity = Optional.empty();
        private @NotNull Optional<MaterialRef> revocationList = Optional.empty();

        private StubConfig(final @NotNull TrustMode trustMode, final @NotNull Optional<MaterialRef> allowList) {
            this.trustMode = trustMode;
            this.allowList = allowList;
        }

        private StubConfig(
                final @NotNull TrustMode trustMode,
                final @NotNull Optional<MaterialRef> allowList,
                final @NotNull Optional<MaterialRef> truststore) {
            this(trustMode, allowList);
            this.truststore = truststore;
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
            return SanUriCheck.STRICTEST;
        }

        @Override
        public @NotNull HostnameCheck hostname() {
            return HostnameCheck.STRICTEST;
        }

        @Override
        public @NotNull ValidityCheck validity() {
            return ValidityCheck.STRICTEST;
        }

        @Override
        public @NotNull RevocationCheck revocation() {
            return RevocationCheck.NONE;
        }

        @Override
        public @NotNull KeyUsageCheck keyUsage() {
            return KeyUsageCheck.STRICTEST;
        }

        @Override
        public @NotNull Optional<MaterialRef> identity() {
            return identity;
        }

        @Override
        public @NotNull Optional<MaterialRef> truststore() {
            return truststore;
        }

        @Override
        public @NotNull Optional<MaterialRef> allowList() {
            return allowList;
        }

        @Override
        public @NotNull Optional<MaterialRef> revocationList() {
            return revocationList;
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
