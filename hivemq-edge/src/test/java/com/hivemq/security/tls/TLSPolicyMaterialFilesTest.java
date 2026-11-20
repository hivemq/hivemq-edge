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

import static com.hivemq.security.tls.TLSFixtures.certificate;
import static com.hivemq.security.tls.TLSFixtures.keyPair;
import static com.hivemq.security.tls.TLSFixtures.pemKey;
import static com.hivemq.security.tls.TLSFixtures.toPem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.security.tls.TLSConstants.TrustMode;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.Principal;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509KeyManager;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// How a policy reads the material files a configuration names once the configuration is accepted:
/// superseded paths, password-protected containers, key entries, PEM chains, password files,
/// allow-lists, truststore unions and base64-encoded stores.
///
/// Fixtures are generated per test into a temporary directory.
class TLSPolicyMaterialFilesTest {

    private static final @NotNull String BRIDGE_ID = "a-bridge";
    private static final @NotNull String PASSWORD = "changeit";

    @TempDir
    private @NotNull Path directory;

    private static @NotNull StubTLSConfig allowListConfig(final @NotNull Path file) {
        return StubTLSConfig.strictest(TrustMode.ALLOW_LIST)
                .withAllowList(new MaterialRef.Inline(null, FileSet.of(file.toString()), null, null, null));
    }

    /// A self-signed authority certificate named `CN=<name>`.
    private static @NotNull X509Certificate authority(final @NotNull String name, final @NotNull KeyPair keys)
            throws Exception {
        return certificate(name, keys, null, keys, true);
    }

    private static byte @NotNull [] keystoreBytes(
            final @NotNull String type, final @NotNull KeyPair pair, final @NotNull X509Certificate certificate)
            throws Exception {
        final KeyStore store = KeyStore.getInstance(type);
        store.load(null, null);
        store.setKeyEntry("client", pair.getPrivate(), PASSWORD.toCharArray(), new X509Certificate[] {certificate});
        return serialised(store, PASSWORD);
    }

    /// The policy of a bridge whose superseded `truststore` setting names the file.
    private static @NotNull TLSPolicy policyOverTruststore(final @NotNull Path file, final @NotNull String password)
            throws Exception {
        return new TLSPolicy(TLSConfigBridge.fromSupersededSettings(
                BRIDGE_ID, null, null, null, file.toString(), password, List.of(), List.of(), true, 10));
    }

    private static @NotNull StubTLSConfig revocationConfig(final @NotNull FileSet files) {
        return StubTLSConfig.strictest(TrustMode.CHAIN)
                .withRevocationList(new MaterialRef.Inline(null, files, null, null, null));
    }

    /// An empty revocation list naming the issuer, signed by an unrelated key: these tests read lists,
    /// they never verify them.
    @SuppressWarnings("JavaUtilDate") // JcaX509v2CRLBuilder requires java.util.Date.
    private static @NotNull X509CRL revocationList(final @NotNull String issuer) throws Exception {
        final Instant now = Instant.now();
        return new JcaX509CRLConverter()
                .getCRL(new JcaX509v2CRLBuilder(new X500Principal(issuer), Date.from(now))
                        .setNextUpdate(Date.from(now.plus(Duration.ofDays(7))))
                        .build(new JcaContentSignerBuilder("SHA256withRSA")
                                .build(keyPair().getPrivate())));
    }

    private static byte @NotNull [] serialised(final @NotNull KeyStore store, final @NotNull String password)
            throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (final OutputStream out = bytes) {
            store.store(out, password.toCharArray());
        }
        return bytes.toByteArray();
    }

    /// A truststore of the given type holding one self-signed authority, protected by the password.
    private static byte @NotNull [] truststoreBytes(final @NotNull String type, final @NotNull String password)
            throws Exception {
        final KeyStore store = KeyStore.getInstance(type);
        store.load(null, null);
        store.setCertificateEntry("authority", authority("authority", keyPair()));
        return serialised(store, password);
    }

    private static @NotNull StubTLSConfig truststoreConfig(
            final @NotNull String format, final @NotNull Path file, final @Nullable Secret password) {
        return StubTLSConfig.strictest(TrustMode.CHAIN)
                .withTruststore(new MaterialRef.Inline(format, FileSet.of(file.toString()), password, null, null));
    }

    /// A JKS truststore file in the test directory, holding one authority, protected by the password.
    private @NotNull Path jksTruststore(final @NotNull String password) throws Exception {
        final Path file = Files.createTempFile(directory, "truststore", ".jks");
        Files.write(file, truststoreBytes("JKS", password));
        return file;
    }

    /// How a superseded revocation-list path is read.
    @Nested
    class SupersededPath {

        /// Proves that a path naming a directory reads every file in it.
        @Test
        void whenTheSupersededPathNamesADirectory_thenEveryFileInItIsRead() throws Exception {
            final Path crls = Files.createDirectory(directory.resolve("crls"));
            Files.writeString(crls.resolve("a.crl"), toPem(revocationList("CN=a")));
            Files.writeString(crls.resolve("b.crl"), toPem(revocationList("CN=b")));

            final TLSPolicy policy = new TLSPolicy(revocationConfig(FileSet.ofLegacyPath(crls.toString())));

            assertThat(policy.revocationLists()).hasSize(2);
        }

        /// Proves that a path naming a file reads that file.
        @Test
        void whenTheSupersededPathNamesAFile_thenThatFileIsRead() throws Exception {
            final Path file = Files.writeString(directory.resolve("one.crl"), toPem(revocationList("CN=a")));

            final TLSPolicy policy = new TLSPolicy(revocationConfig(FileSet.ofLegacyPath(file.toString())));

            assertThat(policy.revocationLists()).hasSize(1);
        }

        /// Proves that a path naming an empty directory is refused, naming the directory.
        @Test
        void whenTheSupersededPathNamesAnEmptyDirectory_thenTheSurfaceIsRefused() throws Exception {
            final Path empty = Files.createDirectory(directory.resolve("empty"));

            assertThatThrownBy(() -> new TLSPolicy(revocationConfig(FileSet.ofLegacyPath(empty.toString()))))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining(empty.toString())
                    .hasMessageContaining("holds no file");
        }
    }

    /// Whether a password-protected container is opened with its integrity checked.
    @Nested
    class IntegrityCheck {

        /// Proves that a waived check opens a password-protected JKS without its password.
        @Test
        void whenTheCheckIsWaived_thenAPasswordProtectedJksIsReadUnchecked() throws Exception {
            final Path file = jksTruststore(PASSWORD);

            final TLSPolicy policy = new TLSPolicy(truststoreConfig("jks", file, new Secret.Waived()));

            assertThat(policy.trustAnchors()).hasSize(1);
        }

        /// Proves that with neither a password nor the waiver a password-protected JKS is checked,
        /// and so refused.
        @Test
        void whenNoPasswordIsGivenAndNothingWaived_thenThePasswordProtectedJksIsChecked() throws Exception {
            final Path file = jksTruststore(PASSWORD);

            assertThatThrownBy(() -> new TLSPolicy(truststoreConfig("jks", file, null)))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("Check the password");
        }
    }

    /// Which entry of an identity container is presented.
    @Nested
    class KeyEntry {

        /// Proves that a named key entry is the only one the key manager presents, even to a server
        /// whose accepted issuers favour another entry.
        @Test
        void whenAKeyEntryIsNamed_thenTheKeyManagerPresentsOnlyThatEntry() throws Exception {
            final KeyPair authorityA = keyPair();
            final KeyPair authorityB = keyPair();
            final X509Certificate caA = authority("ca-a", authorityA);
            final X509Certificate caB = authority("ca-b", authorityB);
            final KeyPair serverKey = keyPair();
            final KeyPair otherKey = keyPair();
            final KeyStore store = KeyStore.getInstance("JKS");
            store.load(null, null);
            store.setKeyEntry("server", serverKey.getPrivate(), PASSWORD.toCharArray(), new X509Certificate[] {
                certificate("server", serverKey, caA, authorityA, false), caA
            });
            store.setKeyEntry("other", otherKey.getPrivate(), PASSWORD.toCharArray(), new X509Certificate[] {
                certificate("other", otherKey, caB, authorityB, false), caB
            });
            final Path file = directory.resolve("identity.jks");
            try (final OutputStream out = Files.newOutputStream(file)) {
                store.store(out, PASSWORD.toCharArray());
            }

            final StubTLSConfig config = StubTLSConfig.strictest(TrustMode.ANY_CERT)
                    .withIdentity(new MaterialRef.Inline(
                            "jks",
                            FileSet.of(file.toString()),
                            new Secret.Literal(PASSWORD),
                            "server",
                            new Secret.Literal(PASSWORD)));
            final TLSPolicy.Identity identity = new TLSPolicy(config).identity().orElseThrow();
            final X509KeyManager manager =
                    (X509KeyManager) identity.keyManagerFactory().getKeyManagers()[0];
            final Principal[] favouringOther = {new X500Principal("CN=ca-b")};

            final String[] aliases = manager.getClientAliases("RSA", null);
            assertThat(aliases).hasSize(1);
            assertThat(manager.getPrivateKey(aliases[0])).isEqualTo(serverKey.getPrivate());
            assertPresentsOnly(
                    manager.chooseClientAlias(new String[] {"RSA"}, favouringOther, null), manager, serverKey);
            if (manager instanceof final X509ExtendedKeyManager extended) {
                assertPresentsOnly(
                        extended.chooseEngineClientAlias(new String[] {"RSA"}, favouringOther, null),
                        manager,
                        serverKey);
            }
        }

        private static void assertPresentsOnly(
                final @Nullable String alias, final @NotNull X509KeyManager manager, final @NotNull KeyPair expected) {
            if (alias != null) {
                assertThat(manager.getPrivateKey(alias)).isEqualTo(expected.getPrivate());
            }
        }
    }

    /// The order a PEM identity's chain is presented in.
    @Nested
    class PemChainOrder {

        /// Proves that a chain written out of order is presented leaf first, then each issuer, with a
        /// certificate off the path kept after it.
        @Test
        void whenTheChainIsWrittenOutOfOrder_thenItIsPresentedLeafFirstThenEachIssuer() throws Exception {
            final KeyPair rootKey = keyPair();
            final KeyPair intermediateKey = keyPair();
            final KeyPair leafKey = keyPair();
            final X509Certificate root = authority("root", rootKey);
            final X509Certificate intermediate = certificate("intermediate", intermediateKey, root, rootKey, true);
            final X509Certificate leaf = certificate("leaf", leafKey, intermediate, intermediateKey, false);
            final X509Certificate stray = authority("stray", keyPair());
            final Path key = Files.writeString(directory.resolve("leaf.key"), pemKey(leafKey));
            final Path chain = Files.writeString(
                    directory.resolve("chain.pem"), toPem(root) + toPem(stray) + toPem(leaf) + toPem(intermediate));

            final StubTLSConfig config = StubTLSConfig.strictest(TrustMode.ANY_CERT)
                    .withIdentity(new MaterialRef.Inline(
                            "pem",
                            new FileSet(List.of(key.toString(), chain.toString()), List.of(), List.of(), List.of()),
                            null,
                            null,
                            null));
            final TLSPolicy.Identity identity = new TLSPolicy(config).identity().orElseThrow();

            // The certificate that is not on the path stays, after it.
            assertThat(identity.chain()).containsExactly(leaf, intermediate, root, stray);
            assertThat(identity.certificate()).isEqualTo(leaf);
        }
    }

    /// How the line ending of a password file is removed.
    @Nested
    class PasswordFile {

        /// Proves that a final line feed is removed and trailing spaces before it are kept.
        @Test
        void whenThePasswordEndsInSpacesAndTheFileInALineFeed_thenOnlyTheLineFeedIsRemoved() throws Exception {
            final Path file = jksTruststore("pw ");
            final Path passwordFile = Files.writeString(directory.resolve("pw-lf"), "pw \n");

            final TLSPolicy policy =
                    new TLSPolicy(truststoreConfig("jks", file, new Secret.FromFile(passwordFile.toString())));

            assertThat(policy.trustAnchors()).hasSize(1);
        }

        /// Proves that a final carriage return and line feed are both removed.
        @Test
        void whenTheFileEndsInCarriageReturnAndLineFeed_thenBothAreRemoved() throws Exception {
            final Path file = jksTruststore("pw\t");
            final Path passwordFile = Files.writeString(directory.resolve("pw-crlf"), "pw\t\r\n");

            final TLSPolicy policy =
                    new TLSPolicy(truststoreConfig("jks", file, new Secret.FromFile(passwordFile.toString())));

            assertThat(policy.trustAnchors()).hasSize(1);
        }

        /// Proves that of two final line feeds only one is removed, so the password does not match.
        @Test
        void whenTheFileEndsInTwoLineFeeds_thenOnlyOneIsRemoved() throws Exception {
            final Path file = jksTruststore("pw");
            final Path passwordFile = Files.writeString(directory.resolve("pw-2lf"), "pw\n\n");

            assertThatThrownBy(() ->
                            new TLSPolicy(truststoreConfig("jks", file, new Secret.FromFile(passwordFile.toString()))))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class);
        }
    }

    /// How the lines of an allow-list file are read.
    @Nested
    class AllowList {

        /// Proves that the line `openssl x509 -fingerprint -sha256` prints is read as the fingerprint.
        @Test
        void whenTheLineIsWhatOpensslPrints_thenTheFingerprintIsRead() throws Exception {
            final KeyPair pair = keyPair();
            final String fingerprint = TLSPolicy.fingerprintOf(certificate("broker", pair, null, pair, false));
            final Path file = Files.writeString(
                    directory.resolve("pinned.txt"),
                    "sha256 Fingerprint=" + TLSPolicy.toDisplayForm(fingerprint) + "\n");

            final TLSPolicy policy = new TLSPolicy(allowListConfig(file));

            assertThat(policy.allowedFingerprints()).containsExactly(fingerprint);
        }

        /// Proves that a line labelled with another digest is refused, naming the line.
        @Test
        void whenTheLabelNamesAnotherDigest_thenTheLineIsRefused() throws Exception {
            final Path file = Files.writeString(directory.resolve("pinned.txt"), "sha1 Fingerprint=" + "AB".repeat(32));

            assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("line 1")
                    .hasMessageContaining("is not a SHA-256 fingerprint");
        }

        /// Proves that a file of comments and blank lines is refused, naming the file.
        @Test
        void whenAFileHoldsNoFingerprint_thenTheMessageNamesTheFile() throws Exception {
            final Path file = Files.writeString(directory.resolve("pinned.txt"), "# rotated out\n\n");

            assertThatThrownBy(() -> new TLSPolicy(allowListConfig(file)))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("Certificate allow-list '" + file + "' contains no fingerprints");
        }
    }

    /// A truststore made of several files.
    @Nested
    class TruststoreUnion {

        /// Proves that a union of a container and a PEM file is refused, naming the rule it breaks.
        @Test
        void whenAUnionMixesAContainerAndAPemFile_thenTheMessageNamesTheUnionRule() throws Exception {
            final Path container = jksTruststore(PASSWORD);
            final Path pem = Files.writeString(directory.resolve("plant.pem"), toPem(authority("plant", keyPair())));
            final StubTLSConfig config = StubTLSConfig.strictest(TrustMode.CHAIN)
                    .withTruststore(new MaterialRef.Inline(
                            "jks",
                            new FileSet(List.of(container.toString(), pem.toString()), List.of(), List.of(), List.of()),
                            new Secret.Literal(PASSWORD),
                            null,
                            null));

            assertThatThrownBy(() -> new TLSPolicy(config))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("'" + pem + "' holds PEM text")
                    .hasMessageContaining("cannot mix formats or passwords");
        }
    }

    /// A bridge store mounted base64-encoded (a Kubernetes secret) is read as the superseded `keystore`
    /// and `truststore` settings have always read it: the container is tried as it is, and a file that
    /// does not open that way is decoded and tried once more. A file that is neither fails with the
    /// message of the first attempt, because that is the failure the operator can act on.
    @Nested
    class Base64Store {

        /// Proves that a store that opens directly is not affected by the second attempt.
        @Test
        void givenATruststoreThatOpensDirectly_whenThePolicyIsBuilt_thenItsAuthorityIsATrustAnchor() throws Exception {
            final Path file = directory.resolve("trust.jks");
            Files.write(file, truststoreBytes("JKS", PASSWORD));

            assertThat(policyOverTruststore(file, PASSWORD).trustAnchors()).hasSize(1);
        }

        /// Proves that a base64-encoded JKS truststore is decoded and read.
        @Test
        void givenABase64EncodedJksTruststore_whenThePolicyIsBuilt_thenItsAuthorityIsATrustAnchor() throws Exception {
            final Path file = directory.resolve("trust.jks");
            Files.write(file, Base64.getEncoder().encode(truststoreBytes("JKS", PASSWORD)));

            assertThat(policyOverTruststore(file, PASSWORD).trustAnchors()).hasSize(1);
        }

        /// Proves that a base64-encoded PKCS#12 keystore is decoded and its identity read.
        @Test
        void givenABase64EncodedPkcs12Keystore_whenThePolicyIsBuilt_thenTheIdentityIsRead() throws Exception {
            final KeyPair pair = keyPair();
            final X509Certificate certificate = authority("client", pair);
            final Path file = directory.resolve("client.p12");
            Files.write(file, Base64.getEncoder().encode(keystoreBytes("PKCS12", pair, certificate)));

            final TLSConfigBridge config = TLSConfigBridge.fromSupersededSettings(
                    BRIDGE_ID, file.toString(), PASSWORD, PASSWORD, null, null, List.of(), List.of(), true, 10);
            final TLSPolicy.Identity identity = new TLSPolicy(config).identity().orElseThrow();

            assertThat(identity.privateKey()).isEqualTo(pair.getPrivate());
            assertThat(identity.certificate()).isEqualTo(certificate);
        }

        /// Proves that the decoded container is opened with the configured password: a wrong one is
        /// still reported as the failure of the first attempt.
        @Test
        void givenABase64EncodedStoreAndAWrongPassword_whenThePolicyIsBuilt_thenTheOriginalFailureIsReported()
                throws Exception {
            final Path file = directory.resolve("trust.jks");
            Files.write(file, Base64.getEncoder().encode(truststoreBytes("JKS", PASSWORD)));

            assertThatThrownBy(() -> policyOverTruststore(file, "not-the-password"))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("'" + file + "' could not be opened as JKS")
                    .hasMessageContaining("Check the password");
        }

        /// Proves that a file that is neither a store nor base64 fails with the first attempt's message.
        @Test
        void givenAFileThatIsNeitherAStoreNorBase64_whenThePolicyIsBuilt_thenTheOriginalFailureIsReported()
                throws Exception {
            final Path file = directory.resolve("trust.jks");
            Files.writeString(file, "this is -- not a store, and not base64 either!", StandardCharsets.UTF_8);

            assertThatThrownBy(() -> policyOverTruststore(file, PASSWORD))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("'" + file + "' could not be opened as JKS")
                    .hasMessageContaining("Check the password, and check that the declared format matches the file.");
        }

        /// Proves that base64 text decoding to something that is not a store fails as the file did.
        @Test
        void givenBase64TextThatDecodesToNoStore_whenThePolicyIsBuilt_thenTheOriginalFailureIsReported()
                throws Exception {
            final Path file = directory.resolve("client.p12");
            Files.write(file, Base64.getEncoder().encode("not a PKCS#12 container".getBytes(StandardCharsets.UTF_8)));

            final TLSConfigBridge config = TLSConfigBridge.fromSupersededSettings(
                    BRIDGE_ID, file.toString(), PASSWORD, PASSWORD, null, null, List.of(), List.of(), true, 10);

            assertThatThrownBy(() -> new TLSPolicy(config))
                    .isInstanceOf(TLSPolicy.TLSMaterialException.class)
                    .hasMessageContaining("'" + file + "' could not be opened as PKCS12");
        }
    }
}
