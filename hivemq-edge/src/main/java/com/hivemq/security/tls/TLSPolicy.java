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

import com.hivemq.security.tls.TLSConstants.TrustMode;
import com.hivemq.security.tls.TLSConstants.ValidityCheck;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.CRL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.EncryptionException;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A [TLSConfig] with its material read: the same checks, with certificates, revocation lists and
/// fingerprints in place of the file names that named them.
///
/// Constructing one reads the files the configuration names, so this is the only place in the chain
/// that touches a filesystem, and the last thing built before a connection exists. Reading happens
/// here rather than in a separate step so that a policy cannot exist without its material: there is
/// no way to hold one whose certificates were never opened.
///
/// Each surface wraps a policy in a facade of its own that drives whatever library terminates its
/// TLS - Eclipse Milo for an OPC UA adapter, the JDK's own trust-manager machinery for a bridge.
/// The facade reads the checks and the material from here and calls its library; nothing in this
/// class knows which surface it serves, and no library appears in its signatures.
///
/// Nothing is cached and nothing is remembered, so constructing a second policy from the same
/// configuration is harmless, and a reload is exactly that - a second construction whose material
/// can be compared with the first.
///
/// Every failure refuses the surface rather than yielding a policy that checks less than the
/// configuration asked for: a file that cannot be read is a configuration error, not a reason to
/// fall back to checking nothing.
///
/// The two `require` methods are the only checks that need no library at all. Every other check -
/// the application identifier, the hostname and its wildcard rules, the declared purpose - stays in
/// the facades, where the surface's own library already implements it against its conformance
/// suite. Reimplementing those here would trade tested code for duplicated code. Both throw on
/// failure and return on success, so the plain call is the strict one: a caller that ignores the
/// result still rejects a bad certificate. Neither has a variant returning a boolean.
public final class TLSPolicy {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TLSPolicy.class);

    private static final @NotNull String DIGEST_ALGORITHM = "SHA-256";
    private static final int FINGERPRINT_HEX_LENGTH = 64;

    private final @NotNull TLSConfig config;
    private final @NotNull Optional<Identity> identity;
    private final @NotNull Set<TrustAnchor> trustAnchors;
    private final @NotNull Set<String> allowedFingerprints;
    private final @NotNull List<X509CRL> revocationLists;

    /// Reads everything the configuration names.
    ///
    /// Reads only what the trust mode actually consults: a truststore under chain trust, an
    /// allow-list under fingerprint trust, revocation lists only where a certification path is
    /// built. Material named under a trust mode that never looks at it is left unopened, so a
    /// broken file an operator left behind does not refuse a surface that would not have read it.
    ///
    /// @throws TLSMaterialException if any file the configuration names cannot be read as what it
    ///     was declared to be
    public TLSPolicy(final @NotNull TLSConfig config) throws TLSMaterialException {
        // Every collection is copied unmodifiable: a policy is read by whatever validates a
        // certificate, per connection, and nothing may change what an operator configured after it
        // was read.
        this.config = config;
        this.identity = config.identity().isPresent()
                ? Optional.of(loadIdentity(config.identity().get()))
                : Optional.empty();
        this.trustAnchors = Set.copyOf(
                config.trustMode() == TrustMode.CHAIN && config.truststore().isPresent()
                        ? loadTrustAnchors(config.truststore().get())
                        : Set.of());
        this.allowedFingerprints = Set.copyOf(
                config.trustMode() == TrustMode.ALLOW_LIST && config.allowList().isPresent()
                        ? loadFingerprints(config.allowList().get())
                        : Set.of());
        this.revocationLists = List.copyOf(
                config.trustMode() == TrustMode.CHAIN && config.revocationList().isPresent()
                        ? loadRevocationLists(config.revocationList().get())
                        : List.of());
    }

    /// A policy over material already in hand, for a caller that has no files to read.
    ///
    /// The one way to build a policy without touching a filesystem, which is what a test of a
    /// facade needs: it exercises what a facade does with material, not where the material came
    /// from.
    public TLSPolicy(
            final @NotNull TLSConfig config,
            final @NotNull Optional<Identity> identity,
            final @NotNull Set<TrustAnchor> trustAnchors,
            final @NotNull Set<String> allowedFingerprints,
            final @NotNull List<X509CRL> revocationLists) {
        this.config = config;
        this.identity = identity;
        this.trustAnchors = Set.copyOf(trustAnchors);
        this.allowedFingerprints = Set.copyOf(allowedFingerprints);
        this.revocationLists = List.copyOf(revocationLists);
    }

    /// The checks, resolved. Held rather than extended, so a facade can still read the configuration
    /// this policy came from.
    public @NotNull TLSConfig config() {
        return config;
    }

    /// Edge's own key and certificate chain, for the TLS implementation to present.
    public @NotNull Optional<Identity> identity() {
        return identity;
    }

    /// The anchors a certification path must reach. Empty means nothing can be trusted by chaining:
    /// an absent truststore trusts nothing, and the runtime's own authorities are named explicitly
    /// with `JVM-cacerts` rather than being a silent fallback.
    public @NotNull Set<TrustAnchor> trustAnchors() {
        return trustAnchors;
    }

    /// Lower-case hexadecimal SHA-256 fingerprints, over the DER encoding, of the certificates
    /// accepted individually.
    public @NotNull Set<String> allowedFingerprints() {
        return allowedFingerprints;
    }

    /// The revocation lists, each matched to its issuer by the issuer name it carries.
    public @NotNull List<X509CRL> revocationLists() {
        return revocationLists;
    }

    /// Edge's own key and the chain that vouches for it, read once and offered in both the shapes a
    /// surface's library asks for.
    ///
    /// A bridge hands the [#keyManagerFactory()] to the JDK, which wants a factory and nothing else.
    /// Eclipse Milo wants the key and the certificates as objects, because it signs the OPC UA
    /// handshake itself rather than delegating to a TLS stack. Both come from one read, so the two
    /// surfaces cannot disagree about which key was loaded.
    ///
    /// @param privateKey the key Edge signs with
    /// @param certificate the certificate belonging to that key
    /// @param chain that certificate and the intermediates vouching for it, leaf first, as the TLS
    ///     and OPC UA handshakes both require
    /// @param applicationUri the URI in the certificate's subject alternative name, which OPC UA
    ///     requires to match the application identifier, or `null` where it carries none
    public record Identity(
            @NotNull PrivateKey privateKey,
            @NotNull X509Certificate certificate,
            @NotNull List<X509Certificate> chain,
            @Nullable String applicationUri,
            @NotNull KeyManagerFactory keyManagerFactory) {

        public Identity {
            chain = List.copyOf(chain);
        }
    }

    /// Requires the certificate's SHA-256 fingerprint to be one an operator wrote down.
    ///
    /// A fingerprint enters the allow-list only because a human put it there, so a peer present at
    /// first connect cannot enrol itself, and the list can be deployed as a read-only secret.
    ///
    /// The message names the fingerprint seen, in the form the allow-list file accepts, so that an
    /// operator who recognises the peer can add it without extracting the certificate first. It is
    /// named on rejection only, and never added.
    public void requireAllowedFingerprint(final @NotNull X509Certificate certificate) throws TLSCheckFailedException {
        final String fingerprint;
        try {
            fingerprint = fingerprintOf(certificate);
        } catch (final CertificateEncodingException e) {
            throw new TLSCheckFailedException("the peer certificate could not be encoded for fingerprinting", e);
        }
        if (!allowedFingerprints.contains(fingerprint)) {
            throw new TLSCheckFailedException(("the peer certificate's SHA-256 fingerprint is not in the allow-list. "
                            + "Subject='%s', issuer='%s', fingerprint=%s. If this is the expected peer, add that "
                            + "fingerprint to the allow-list and restart - the allow-list is read once at start.")
                    .formatted(
                            certificate.getSubjectX500Principal().getName(),
                            certificate.getIssuerX500Principal().getName(),
                            toDisplayForm(fingerprint)));
        }
    }

    /// Requires the current time to lie within the certificate's own validity period.
    ///
    /// Needs no authority and no material: every certificate states its own dates.
    public void requireValidity(final @NotNull X509Certificate certificate) throws TLSCheckFailedException {
        if (config.validity() == ValidityCheck.NONE) {
            return;
        }
        try {
            certificate.checkValidity();
        } catch (final CertificateExpiredException e) {
            throw new TLSCheckFailedException("the peer certificate expired on " + certificate.getNotAfter() + ".", e);
        } catch (final CertificateNotYetValidException e) {
            throw new TLSCheckFailedException(
                    "the peer certificate is not valid before "
                            + certificate.getNotBefore()
                            + ", which is in the future. Check the clock at both ends.",
                    e);
        }
    }

    /// The lower-case hexadecimal SHA-256 fingerprint over a certificate's DER encoding - the form
    /// an allow-list file holds and the form [#allowedFingerprints()] is keyed by.
    public static @NotNull String fingerprintOf(final @NotNull X509Certificate certificate)
            throws CertificateEncodingException {
        try {
            final MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(certificate.getEncoded()));
        } catch (final NoSuchAlgorithmException e) {
            // Every JRE is required to provide SHA-256, so its absence is not a configuration fault.
            throw new IllegalStateException(DIGEST_ALGORITHM + " is unavailable", e);
        }
    }

    /// A fingerprint as an operator reads it: upper case, colon-separated byte pairs, which is what
    /// `openssl x509 -fingerprint` prints and what the allow-list file also accepts.
    public static @NotNull String toDisplayForm(final @NotNull String fingerprint) {
        final StringBuilder display = new StringBuilder(fingerprint.length() * 3 / 2);
        for (int i = 0; i < fingerprint.length(); i += 2) {
            if (i > 0) {
                display.append(':');
            }
            display.append(fingerprint, i, Math.min(i + 2, fingerprint.length()));
        }
        return display.toString().toUpperCase(Locale.ROOT);
    }

    /// A check the peer's certificate did not pass. The message names what failed and what to do, so
    /// a facade can hand it to an operator unchanged.
    public static final class TLSCheckFailedException extends Exception {
        public TLSCheckFailedException(final @NotNull String reason) {
            super(reason);
        }

        public TLSCheckFailedException(final @NotNull String reason, final @NotNull Throwable cause) {
            super(reason, cause);
        }
    }
    // --- identity ---------------------------------------------------------------------------------

    /// Edge's own key and certificate chain, from a container or from PEM files.
    private static @NotNull Identity loadIdentity(final @NotNull MaterialRef ref) throws TLSMaterialException {
        final MaterialRef.Inline inline = requireInline(ref, "identity");
        if (inline.format() != null && inline.format().equalsIgnoreCase("pem")) {
            return loadPemIdentity(inline);
        }
        return loadContainerIdentity(inline);
    }

    /// Opens the container holding Edge's own key and reads the key with its chain.
    ///
    /// The key entry is selected by name where the configuration names one. Where it does not and
    /// the container holds exactly one key, that key is used; where it holds several, the surface is
    /// refused rather than one being picked, because the choice would not be the operator's and
    /// could change when the file is re-exported.
    private static @NotNull Identity loadContainerIdentity(final @NotNull MaterialRef.Inline inline)
            throws TLSMaterialException {
        final Path file = singleFile(inline, "identity");
        final char[] containerPassword = secret(inline.password());
        final char[] keyPassword = secret(inline.entryPassword());

        final KeyStore store = openKeyStore(file, inline.format(), containerPassword);
        final String entry = inline.entry() == null ? soleKeyEntry(store, file) : inline.entry();
        requireKeyEntry(store, entry, file);

        try {
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            // The whole container is handed over, not a copy holding only the named entry. Copying
            // would mean re-storing the key and its chain, and a container format that validates a
            // chain on write rejects chains the original held quite happily - so a configuration
            // that worked would stop working on a detail of the copy rather than of the material.
            //
            // Which entry is presented is therefore still the factory's choice where a container
            // holds several keys, which is why naming one is required there: the check above refuses
            // an unnamed entry in that case rather than letting the factory pick.
            factory.init(store, keyPassword);

            // The same entry the factory will present, read again as objects for a surface whose
            // library signs its own handshake. Read from the same store and the same entry name, so
            // the two cannot be different keys.
            final Key key = store.getKey(entry, keyPassword);
            if (!(key instanceof PrivateKey privateKey)) {
                throw new TLSMaterialException("The entry '%s' in '%s' is not a private key.".formatted(entry, file));
            }
            final List<X509Certificate> chain = chainOf(store, entry, file);
            return new Identity(privateKey, chain.get(0), chain, applicationUriOf(chain.get(0)), factory);
        } catch (final GeneralSecurityException e) {
            throw new TLSMaterialException(
                    "The private key '%s' in '%s' could not be read. Check the key password.".formatted(entry, file),
                    e);
        }
    }

    /// Edge's own key and chain from PEM files, which a deployment projecting secrets as files has
    /// instead of a container.
    ///
    /// The key, the certificate and any intermediates are named separately, because PEM has no
    /// container to hold them together - which is also why a file set is what the model gives here,
    /// rather than the single file a container is. Every certificate found across the files forms the
    /// chain; the one belonging to the key is identified by its public key rather than by position,
    /// since nothing orders a PEM file.
    private static @NotNull Identity loadPemIdentity(final @NotNull MaterialRef.Inline inline)
            throws TLSMaterialException {
        final List<Path> files = expandPaths(inline.files(), "identity");
        if (files.isEmpty()) {
            throw new TLSMaterialException("The identity names no file.");
        }

        PrivateKey privateKey = null;
        final List<X509Certificate> certificates = new ArrayList<>();
        for (final Path file : files) {
            for (final PrivateKey found : readPemPrivateKeys(file, inline.entryPassword())) {
                if (privateKey != null) {
                    // Which one Edge would present is not the operator's choice, and a renamed or
                    // re-exported file could change it.
                    throw new TLSMaterialException(("The identity names more than one private key, in '%s' among "
                                    + "others. Name exactly one.")
                            .formatted(file));
                }
                privateKey = found;
            }
            certificates.addAll(readPemCertificatesIfAny(file));
        }
        if (privateKey == null) {
            throw new TLSMaterialException(
                    "The identity names no private key. A PEM identity needs the key as " + "well as the certificate.");
        }
        if (certificates.isEmpty()) {
            throw new TLSMaterialException(
                    "The identity names no certificate. A PEM identity needs the " + "certificate as well as the key.");
        }

        final List<X509Certificate> chain = chainFromLeaf(certificates, privateKey);
        return new Identity(
                privateKey,
                chain.get(0),
                chain,
                applicationUriOf(chain.get(0)),
                keyManagerFactoryFor(privateKey, chain));
    }

    private static @NotNull String soleKeyEntry(final @NotNull KeyStore store, final @NotNull Path file)
            throws TLSMaterialException {
        final List<String> keyEntries = new ArrayList<>();
        try {
            for (final String alias : java.util.Collections.list(store.aliases())) {
                if (store.isKeyEntry(alias)) {
                    keyEntries.add(alias);
                }
            }
        } catch (final GeneralSecurityException e) {
            throw new TLSMaterialException("The entries of '" + file + "' could not be listed.", e);
        }
        if (keyEntries.isEmpty()) {
            throw new TLSMaterialException(("'%s' holds no private key, so it cannot be an identity. A truststore "
                            + "holds certificates only; an identity holds a key and its certificate chain.")
                    .formatted(file));
        }
        if (keyEntries.size() > 1) {
            // Picking one would be arbitrary: neither PKCS#12 nor JKS specifies the order its entries
            // come back in, so the choice could change when somebody re-exports the file, and the
            // surface would then present a different certificate with nothing to say it had changed.
            throw new TLSMaterialException(("'%s' holds %d private keys (%s), so which one to present is "
                            + "ambiguous. Name the one to use with a key entry.")
                    .formatted(file, keyEntries.size(), String.join(", ", keyEntries)));
        }
        return keyEntries.get(0);
    }

    private static void requireKeyEntry(
            final @NotNull KeyStore store, final @NotNull String entry, final @NotNull Path file)
            throws TLSMaterialException {
        try {
            if (!store.isKeyEntry(entry)) {
                throw new TLSMaterialException(("'%s' holds no private key under the name '%s'. The names it holds "
                                + "are printed by `keytool -list` under 'Alias name'.")
                        .formatted(file, entry));
            }
        } catch (final GeneralSecurityException e) {
            throw new TLSMaterialException("The entries of '" + file + "' could not be listed.", e);
        }
    }

    /// The certificate chain a container holds under one key entry, leaf first.
    private static @NotNull List<X509Certificate> chainOf(
            final @NotNull KeyStore store, final @NotNull String entry, final @NotNull Path file)
            throws TLSMaterialException {
        try {
            final java.security.cert.Certificate[] chain = store.getCertificateChain(entry);
            if (chain == null || chain.length == 0) {
                throw new TLSMaterialException(("The key '%s' in '%s' has no certificate. A key Edge presents needs "
                                + "the certificate that vouches for it.")
                        .formatted(entry, file));
            }
            final List<X509Certificate> certificates = new ArrayList<>();
            for (final java.security.cert.Certificate certificate : chain) {
                if (certificate instanceof X509Certificate x509) {
                    certificates.add(x509);
                }
            }
            if (certificates.isEmpty()) {
                throw new TLSMaterialException("The key '%s' in '%s' has no X.509 certificate.".formatted(entry, file));
            }
            return certificates;
        } catch (final GeneralSecurityException e) {
            throw new TLSMaterialException(
                    "The certificate chain of '%s' in '%s' could not be read.".formatted(entry, file), e);
        }
    }

    /// The chain with the key's own certificate first, which is the order both the TLS and the OPC UA
    /// handshake require.
    ///
    /// The leaf is found by matching public keys rather than by position, because a PEM file imposes
    /// no order and an operator may name the intermediates before the certificate.
    private static @NotNull List<X509Certificate> chainFromLeaf(
            final @NotNull List<X509Certificate> certificates, final @NotNull PrivateKey privateKey)
            throws TLSMaterialException {
        final List<X509Certificate> ordered = new ArrayList<>();
        for (final X509Certificate certificate : certificates) {
            if (belongsTo(certificate, privateKey)) {
                ordered.add(certificate);
            }
        }
        if (ordered.isEmpty()) {
            throw new TLSMaterialException("None of the certificates named belongs to the private key named. A "
                    + "certificate and the key it certifies have to be a pair.");
        }
        if (ordered.size() > 1) {
            throw new TLSMaterialException("More than one certificate named belongs to the private key named, so "
                    + "which one Edge would present is ambiguous. Name one.");
        }
        for (final X509Certificate certificate : certificates) {
            if (!ordered.contains(certificate)) {
                ordered.add(certificate);
            }
        }
        return ordered;
    }

    /// Whether a certificate certifies this key, decided by signing a fixed block with the key and
    /// verifying it with the certificate.
    ///
    /// Comparing the public keys would be simpler, but a private key does not portably carry its
    /// public half: deriving it is algorithm-specific and some providers do not expose it at all.
    /// Signing is the question actually being asked - can Edge use this certificate to speak for
    /// this key - and it answers it for any algorithm the JVM can sign with.
    ///
    /// An algorithm neither side can sign with answers no, which refuses the identity naming both
    /// halves rather than presenting a key whose certificate may not match it.
    private static boolean belongsTo(final @NotNull X509Certificate certificate, final @NotNull PrivateKey key) {
        final String algorithm =
                switch (key.getAlgorithm()) {
                    case "RSA" -> "SHA256withRSA";
                    case "EC" -> "SHA256withECDSA";
                    case "EdDSA", "Ed25519" -> "Ed25519";
                    default -> null;
                };
        if (algorithm == null) {
            return false;
        }
        try {
            final byte[] probe = "hivemq-edge-identity-probe".getBytes(StandardCharsets.UTF_8);
            final Signature signer = Signature.getInstance(algorithm);
            signer.initSign(key);
            signer.update(probe);
            final byte[] signature = signer.sign();

            final Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(certificate.getPublicKey());
            verifier.update(probe);
            return verifier.verify(signature);
        } catch (final GeneralSecurityException e) {
            return false;
        }
    }

    /// A key manager factory over one key and its chain, for a surface that hands the JDK a factory.
    ///
    /// Built over a key store created in memory, because that is the only input a factory takes. The
    /// store never reaches a disk and carries a password only because the API requires one.
    ///
    /// JKS rather than PKCS#12, although PKCS#12 is otherwise the better format: PKCS#12 validates
    /// the chain as the entry is written and refuses one whose certificates do not each sign the
    /// next. An operator's PEM files legitimately hold a chain this strict reading rejects - an extra
    /// cross-signed root, or intermediates in an order nothing in PEM defines - and refusing those
    /// here would reject material the handshake itself accepts. Whether the chain actually leads to a
    /// trusted authority is the peer's judgement, not ours to pre-empt while loading our own key.
    private static @NotNull KeyManagerFactory keyManagerFactoryFor(
            final @NotNull PrivateKey privateKey, final @NotNull List<X509Certificate> chain)
            throws TLSMaterialException {
        try {
            final KeyStore store = KeyStore.getInstance("JKS");
            store.load(null, new char[0]);
            store.setKeyEntry("identity", privateKey, new char[0], chain.toArray(new X509Certificate[0]));
            final KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            factory.init(store, new char[0]);
            return factory;
        } catch (final GeneralSecurityException | IOException e) {
            throw new TLSMaterialException(
                    "The private key and certificate named could not be used together. The "
                            + "certificate has to be the one belonging to the key.",
                    e);
        }
    }

    /// The URI in a certificate's subject alternative name, which OPC UA compares against the
    /// application identifier.
    private static @Nullable String applicationUriOf(final @NotNull X509Certificate certificate) {
        try {
            final Collection<List<?>> names = certificate.getSubjectAlternativeNames();
            if (names == null) {
                return null;
            }
            for (final List<?> name : names) {
                // Type 6 is uniformResourceIdentifier, per RFC 5280's GeneralName.
                if (name.size() >= 2 && Integer.valueOf(6).equals(name.get(0)) && name.get(1) instanceof String uri) {
                    return uri;
                }
            }
            return null;
        } catch (final GeneralSecurityException e) {
            return null;
        }
    }

    /// Every private key in a PEM file, which is normally none or one.
    ///
    /// All of them are returned rather than the first, so that a file holding two is refused naming
    /// the real problem: stopping at the first would silently present whichever key the file happened
    /// to list earlier.
    ///
    /// The modern `PRIVATE KEY` form and the older `RSA PRIVATE KEY` form are both read, encrypted or
    /// not, because an operator's key comes from whatever tool their deployment uses and all of these
    /// appear in practice.
    private static @NotNull List<PrivateKey> readPemPrivateKeys(
            final @NotNull Path file, final @Nullable Secret password) throws TLSMaterialException {
        final List<PrivateKey> keys = new ArrayList<>();
        try (final PEMParser parser = new PEMParser(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            // Named explicitly rather than left to the JVM's provider list. An encrypted PEM key uses
            // ciphers and key-derivation functions the platform providers do not all offer, and
            // whether one happens to be registered is not something a TLS configuration should turn
            // on: the library that parses the file also supplies the ciphers to open it.
            final java.security.Provider provider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
            final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(provider);
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof PEMEncryptedKeyPair encrypted) {
                    keys.add(converter
                            .getKeyPair(encrypted.decryptKeyPair(new JcePEMDecryptorProviderBuilder()
                                    .setProvider(provider)
                                    .build(requirePassword(password, file))))
                            .getPrivate());
                } else if (object instanceof PKCS8EncryptedPrivateKeyInfo encrypted) {
                    keys.add(converter.getPrivateKey(
                            encrypted.decryptPrivateKeyInfo(new JceOpenSSLPKCS8DecryptorProviderBuilder()
                                    .setProvider(provider)
                                    .build(requirePassword(password, file)))));
                } else if (object instanceof PEMKeyPair pair) {
                    keys.add(converter.getKeyPair(pair).getPrivate());
                } else if (object instanceof PrivateKeyInfo info) {
                    keys.add(converter.getPrivateKey(info));
                }
            }
            return keys;
        } catch (final NoSuchFileException e) {
            throw new TLSMaterialException("'" + file + "' does not exist.", e);
        } catch (final EncryptionException e) {
            throw new TLSMaterialException(
                    ("The private key in '%s' could not be decrypted. Check the key " + "password.").formatted(file),
                    e);
        } catch (final MalformedInputException e) {
            throw new TLSMaterialException(
                    ("'%s' is not PEM. Declare the format the file actually has, or name a " + "PEM file.")
                            .formatted(file),
                    e);
        } catch (final IOException | OperatorCreationException | PKCSException e) {
            throw new TLSMaterialException("'" + file + "' could not be read as PEM.", e);
        }
    }

    /// The password an encrypted PEM key needs, refused where the configuration named none.
    private static char @NotNull [] requirePassword(final @Nullable Secret password, final @NotNull Path file)
            throws TLSMaterialException {
        final char[] characters = secret(password);
        if (characters.length == 0) {
            throw new TLSMaterialException(
                    ("The private key in '%s' is encrypted but no key password is " + "configured.").formatted(file));
        }
        return characters;
    }

    /// Every certificate in a PEM file, or none where it holds only a key.
    ///
    /// Read block by block with the PEM parser rather than through `CertificateFactory`, because a
    /// file may hold a key and its certificate together - which `openssl` writes by default and a
    /// Kubernetes secret often projects - and the factory fails on the whole stream as soon as it
    /// meets the key block, yielding no certificate at all.
    ///
    /// A file holding no certificate is not an error here: a PEM identity may name its key and its
    /// certificates in separate files, and the key's file legitimately has none.
    private static @NotNull List<X509Certificate> readPemCertificatesIfAny(final @NotNull Path file)
            throws TLSMaterialException {
        final List<X509Certificate> certificates = new ArrayList<>();
        try (final PEMParser parser = new PEMParser(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            final JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
            Object object;
            while ((object = parser.readObject()) != null) {
                if (object instanceof X509CertificateHolder holder) {
                    certificates.add(converter.getCertificate(holder));
                }
            }
            return certificates;
        } catch (final NoSuchFileException e) {
            throw new TLSMaterialException("'" + file + "' does not exist.", e);
        } catch (final MalformedInputException e) {
            // Not text at all, so not PEM: a DER file named where PEM was declared.
            throw new TLSMaterialException(
                    ("'%s' is not PEM. Declare the format the file actually has, or name a " + "PEM file.")
                            .formatted(file),
                    e);
        } catch (final IOException | GeneralSecurityException e) {
            throw new TLSMaterialException("'" + file + "' could not be read as PEM.", e);
        }
    }

    private static @NotNull KeyStore openKeyStore(
            final @NotNull Path file, final @Nullable String format, final char @NotNull [] password)
            throws TLSMaterialException {
        final String type = keyStoreType(format);
        try (final InputStream in = Files.newInputStream(file)) {
            final KeyStore store = KeyStore.getInstance(type);
            store.load(in, password);
            return store;
        } catch (final NoSuchFileException e) {
            throw new TLSMaterialException("'" + file + "' does not exist.", e);
        } catch (final IOException e) {
            // A wrong password and a wrong format both surface here, and they have different fixes,
            // so both are named rather than one being guessed at.
            throw new TLSMaterialException(
                    ("'%s' could not be opened as %s. Check the password, and check that the "
                                    + "declared format matches the file.")
                            .formatted(file, type),
                    e);
        } catch (final GeneralSecurityException e) {
            throw new TLSMaterialException("'" + file + "' could not be read as " + type + ".", e);
        }
    }

    /// The declared format as a key-store type.
    ///
    /// Declared rather than guessed from the file extension: a `.crt` file holds PEM or DER, a
    /// `.pem` file holds a key or a certificate or both, and a file Kubernetes mounts is named after
    /// the key in the secret. Guessing produces a parse failure naming the wrong format, which sends
    /// an operator hunting for a corrupt file.
    private static @NotNull String keyStoreType(final @Nullable String format) throws TLSMaterialException {
        if (format == null) {
            throw new TLSMaterialException("No format is declared. Declare p12, jks or pem.");
        }
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "p12", "pkcs12" -> "PKCS12";
            case "jks" -> "JKS";
            case "pem" ->
                throw new TLSMaterialException(
                        "PEM material is not read through a key store; name its parts separately.");
            default ->
                throw new TLSMaterialException("'" + format + "' is not a format this reads. Declare p12, jks or pem.");
        };
    }

    // --- truststore -------------------------------------------------------------------------------

    /// Every certificate the truststore names, as trust anchors.
    ///
    /// Files, directories, glob patterns and runtime truststores merge into one set, so an operator
    /// may trust a corporate container, a plant bundle and the public authorities together.
    private static @NotNull Set<TrustAnchor> loadTrustAnchors(final @NotNull MaterialRef ref)
            throws TLSMaterialException {
        final MaterialRef.Inline inline = requireInline(ref, "truststore");
        final Set<TrustAnchor> anchors = new LinkedHashSet<>();

        for (final Path file : expandPaths(inline.files(), "truststore")) {
            for (final X509Certificate certificate : readCertificates(file, inline.format(), inline.password())) {
                anchors.add(new TrustAnchor(certificate, null));
            }
        }
        for (final String builtin : inline.files().builtins()) {
            for (final X509Certificate certificate : readBuiltinCertificates(builtin)) {
                anchors.add(new TrustAnchor(certificate, null));
            }
        }
        if (anchors.isEmpty()) {
            throw new TLSMaterialException("The truststore holds no certificate, so nothing could be trusted "
                    + "through it. Point it at a file holding certificates, or name the runtime's own with "
                    + "builtin=" + FileSet.JVM_CACERTS + ".");
        }
        return anchors;
    }

    /// The certificates a truststore the runtime carries holds.
    ///
    /// `JVM-cacerts` is the only name, and it is the public certificate authorities the JVM ships
    /// with. Obtained by initialising a trust manager factory with no key store, which is how the
    /// JVM resolves its own default - so an operator who overrode that default through the usual
    /// system properties gets what they chose, and Edge need not know where the file lives.
    ///
    /// Any other name is refused, as a file that does not exist is refused: an operator who
    /// misspelled it would otherwise get a truststore holding nothing.
    private static @NotNull List<X509Certificate> readBuiltinCertificates(final @NotNull String name)
            throws TLSMaterialException {
        if (!FileSet.JVM_CACERTS.equals(name)) {
            throw new TLSMaterialException(("'%s' is not a truststore this runtime carries. The only name is '%s', "
                            + "the public certificate authorities the JVM ships with.")
                    .formatted(name, FileSet.JVM_CACERTS));
        }
        try {
            final TrustManagerFactory factory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            factory.init((KeyStore) null);
            final List<X509Certificate> certificates = new ArrayList<>();
            for (final TrustManager manager : factory.getTrustManagers()) {
                if (manager instanceof X509TrustManager x509) {
                    certificates.addAll(List.of(x509.getAcceptedIssuers()));
                }
            }
            if (certificates.isEmpty()) {
                throw new TLSMaterialException("The runtime's own truststore holds no certificate.");
            }
            return certificates;
        } catch (final GeneralSecurityException e) {
            throw new TLSMaterialException("The runtime's own truststore could not be read.", e);
        }
    }

    private static @NotNull List<X509Certificate> readCertificates(
            final @NotNull Path file, final @Nullable String format, final @Nullable Secret password)
            throws TLSMaterialException {
        if (format != null && format.equalsIgnoreCase("pem")) {
            return readPemCertificates(file);
        }
        final KeyStore store = openKeyStore(file, format, secret(password));
        final List<X509Certificate> certificates = new ArrayList<>();
        try {
            for (final String alias : java.util.Collections.list(store.aliases())) {
                if (store.getCertificate(alias) instanceof X509Certificate certificate) {
                    certificates.add(certificate);
                }
            }
        } catch (final GeneralSecurityException e) {
            throw new TLSMaterialException("The certificates in '" + file + "' could not be read.", e);
        }
        return certificates;
    }

    /// Every certificate in a PEM file. A PEM file may hold any number of them in any combination,
    /// so all are taken.
    private static @NotNull List<X509Certificate> readPemCertificates(final @NotNull Path file)
            throws TLSMaterialException {
        try (final InputStream in = Files.newInputStream(file)) {
            final Collection<? extends java.security.cert.Certificate> read =
                    CertificateFactory.getInstance("X.509").generateCertificates(in);
            final List<X509Certificate> certificates = new ArrayList<>();
            for (final java.security.cert.Certificate certificate : read) {
                if (certificate instanceof X509Certificate x509) {
                    certificates.add(x509);
                }
            }
            if (certificates.isEmpty()) {
                throw new TLSMaterialException("'" + file + "' holds no certificate.");
            }
            return certificates;
        } catch (final NoSuchFileException e) {
            throw new TLSMaterialException("'" + file + "' does not exist.", e);
        } catch (final IOException | GeneralSecurityException e) {
            throw new TLSMaterialException("'" + file + "' could not be read as PEM.", e);
        }
    }

    // --- allow-list -------------------------------------------------------------------------------

    /// The SHA-256 fingerprints an operator wrote down, one per line.
    ///
    /// Blank lines and `#` comments are skipped; a fingerprint may be written with or without the
    /// colons `openssl` prints, in either case. Anything else refuses the surface naming the file and
    /// the line, because a fingerprint that was meant to be trusted and silently was not is the
    /// failure this trust mode exists to prevent.
    private static @NotNull Set<String> loadFingerprints(final @NotNull MaterialRef ref) throws TLSMaterialException {
        final MaterialRef.Inline inline = requireInline(ref, "allow-list");
        final Set<String> fingerprints = new LinkedHashSet<>();

        for (final Path file : expandPaths(inline.files(), "allow-list")) {
            final List<String> lines;
            try {
                lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            } catch (final MalformedInputException e) {
                // Named apart from the generic read failure: that one sends an operator to check
                // paths and permissions, which is the wrong hunt for an encoding problem.
                throw new TLSMaterialException(
                        ("'%s' is not UTF-8 encoded. Save it as UTF-8; fingerprints are "
                                        + "hexadecimal, so plain ASCII will do.")
                                .formatted(file),
                        e);
            } catch (final IOException e) {
                // The three ways to be unreadable have three different fixes, so each is named
                // rather than collapsed into one message - and none is spelled as a Java class name,
                // which tells an operator nothing about what to do.
                throw unreadable(file, e);
            }

            for (int i = 0; i < lines.size(); i++) {
                // A Windows editor saving "UTF-8" commonly writes a byte order mark first; left in
                // place it fails the first fingerprint with a message about an invisible character.
                final String line = i == 0 ? stripByteOrderMark(lines.get(i)) : lines.get(i);
                final String content = stripComment(line);
                if (content.isBlank()) {
                    continue;
                }
                fingerprints.add(parseFingerprint(content, i + 1, file));
            }
        }
        if (fingerprints.isEmpty()) {
            throw new TLSMaterialException("The allow-list holds no fingerprint, so no certificate could be "
                    + "trusted through it. Add one SHA-256 fingerprint per line, or use a different trust mode.");
        }
        return fingerprints;
    }

    private static @NotNull String parseFingerprint(
            final @NotNull String entry, final int line, final @NotNull Path file) throws TLSMaterialException {
        final String normalized = entry.trim().replace(":", "").replace(" ", "").toLowerCase(Locale.ROOT);
        if (normalized.length() != FINGERPRINT_HEX_LENGTH || !normalized.chars().allMatch(TLSPolicy::isHex)) {
            throw new TLSMaterialException(("'%s', line %d: '%s' is not a SHA-256 fingerprint. A fingerprint is 64 "
                            + "hexadecimal digits, with or without colons, as `openssl x509 -fingerprint -sha256` "
                            + "prints it.")
                    .formatted(file, line, entry.trim()));
        }
        return normalized;
    }

    private static boolean isHex(final int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
    }

    private static @NotNull String stripByteOrderMark(final @NotNull String line) {
        return line.isEmpty() || line.charAt(0) != '﻿' ? line : line.substring(1);
    }

    private static @NotNull String stripComment(final @NotNull String line) {
        final int hash = line.indexOf('#');
        return hash < 0 ? line.trim() : line.substring(0, hash).trim();
    }

    // --- revocation lists -------------------------------------------------------------------------

    /// Every revocation list the named files hold.
    ///
    /// A file may be PEM- or DER-encoded and may hold several lists; the one call reads both
    /// encodings, so no format needs declaring. The path builder matches each list to its issuer by
    /// the issuer name the list carries.
    private static @NotNull List<X509CRL> loadRevocationLists(final @NotNull MaterialRef ref)
            throws TLSMaterialException {
        final MaterialRef.Inline inline = requireInline(ref, "revocation list");
        final List<X509CRL> crls = new ArrayList<>();

        for (final Path file : expandPaths(inline.files(), "revocation list")) {
            try (final InputStream in = Files.newInputStream(file)) {
                final Collection<? extends CRL> read =
                        CertificateFactory.getInstance("X.509").generateCRLs(in);
                if (read.isEmpty()) {
                    throw new TLSMaterialException("'" + file + "' holds no revocation list.");
                }
                for (final CRL crl : read) {
                    if (crl instanceof X509CRL x509) {
                        crls.add(x509);
                    }
                }
            } catch (final NoSuchFileException e) {
                throw new TLSMaterialException("'" + file + "' does not exist.", e);
            } catch (final IOException | GeneralSecurityException e) {
                throw new TLSMaterialException(
                        "'" + file + "' could not be read as a revocation list; PEM and DER are both accepted.", e);
            }
        }
        return crls;
    }

    // --- naming files -----------------------------------------------------------------------------

    /// Every path a file set names: the files themselves, every readable entry of each directory,
    /// and every match of each glob pattern. A runtime truststore names no path and is read
    /// elsewhere.
    ///
    /// Expansion happens here and nowhere earlier, so that two configurations naming the same paths
    /// are equal however the filesystem looks when they are compared.
    private static @NotNull List<Path> expandPaths(final @NotNull FileSet fileSet, final @NotNull String what)
            throws TLSMaterialException {
        if (fileSet.isEmpty()) {
            throw new TLSMaterialException("The " + what + " names no file.");
        }
        if (!fileSet.namesAPath()) {
            // Names only a runtime truststore, which the caller reads for itself.
            return List.of();
        }
        final List<Path> paths = new ArrayList<>();

        for (final String file : fileSet.files()) {
            paths.add(Path.of(file));
        }
        for (final FileSet.Directory directory : fileSet.directories()) {
            final Path dir = Path.of(directory.location());
            if (!Files.isDirectory(dir)) {
                throw new TLSMaterialException("'" + dir + "' is not a directory.");
            }
            final List<Path> entries = new ArrayList<>();
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, Files::isRegularFile)) {
                for (final Path entry : stream) {
                    entries.add(entry);
                }
            } catch (final IOException e) {
                throw new TLSMaterialException("'" + dir + "' could not be listed.", e);
            }
            requireNotSilentlyEmpty(entries, dir.toString(), directory.mayBeEmpty(), what);
            paths.addAll(entries);
        }
        for (final FileSet.Pattern pattern : fileSet.patterns()) {
            final Path dir = Path.of(pattern.location()).getParent();
            final Path glob = Path.of(pattern.location()).getFileName();
            if (dir == null || glob == null) {
                throw new TLSMaterialException(
                        "'" + pattern.location() + "' is not a pattern of the form directory/glob.");
            }
            final List<Path> matches = new ArrayList<>();
            try (final DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob.toString())) {
                for (final Path match : stream) {
                    if (Files.isRegularFile(match)) {
                        matches.add(match);
                    }
                }
            } catch (final IOException e) {
                throw new TLSMaterialException("'" + pattern.location() + "' could not be expanded.", e);
            }
            requireNotSilentlyEmpty(matches, pattern.location(), pattern.mayBeEmpty(), what);
            paths.addAll(matches);
        }
        return paths;
    }

    /// Why a file could not be read, said in a way an operator can act on.
    ///
    /// A missing file, a directory where a file was expected and a file the process may not read
    /// have three different fixes, and the raw exception names a Java class rather than any of them.
    /// The cause is kept, so a genuinely unexpected failure is still diagnosable from a stack trace.
    private static @NotNull TLSMaterialException unreadable(
            final @NotNull Path file, final @NotNull IOException cause) {
        if (Files.isDirectory(file)) {
            return new TLSMaterialException(
                    "'" + file + "' is a directory, not a file. Name the file inside it, or use a directory entry.",
                    cause);
        }
        if (!Files.exists(file)) {
            return new TLSMaterialException(
                    "'" + file + "' does not exist. Correct the path, or create the file.", cause);
        }
        if (!Files.isReadable(file)) {
            return new TLSMaterialException(
                    "'" + file + "' is not readable by the user Edge runs as. Correct its permissions.", cause);
        }
        return new TLSMaterialException("'" + file + "' could not be read.", cause);
    }

    /// Decides what an empty directory or pattern means, which only the operator can say.
    ///
    /// Strictly by default: material that was configured and is then absent leaves Edge checking
    /// less than the configuration says, and the operator has nothing pointing at the empty
    /// directory. The dangerous reading this prevents is "no revocation lists found" quietly
    /// becoming "nothing is revoked".
    ///
    /// Where the operator permitted emptiness - a directory a deployment populates later - it is a
    /// warning instead, naming the material and the location, because the consequence is the same
    /// and only the decision differs.
    private static void requireNotSilentlyEmpty(
            final @NotNull List<Path> found,
            final @NotNull String location,
            final boolean mayBeEmpty,
            final @NotNull String what)
            throws TLSMaterialException {
        if (!found.isEmpty()) {
            return;
        }
        if (!mayBeEmpty) {
            throw new TLSMaterialException(("The %s names '%s', which holds no file. Material that was configured "
                            + "and is then absent would leave less checked than the configuration says, so it is "
                            + "refused. Write may-be-empty on it to accept that it can hold nothing.")
                    .formatted(what, location));
        }
        log.warn(
                "The {} names '{}', which holds no file and is marked as permitted to be empty. Nothing was "
                        + "loaded from it.",
                what,
                location);
    }

    private static @NotNull Path singleFile(final @NotNull MaterialRef.Inline inline, final @NotNull String what)
            throws TLSMaterialException {
        final List<Path> paths = expandPaths(inline.files(), what);
        if (paths.size() > 1) {
            throw new TLSMaterialException(("The %s names %d files, but Edge presents exactly one certificate in a "
                            + "handshake, so it resolves to one key. Name a single file.")
                    .formatted(what, paths.size()));
        }
        return paths.get(0);
    }

    private static @NotNull MaterialRef.Inline requireInline(final @NotNull MaterialRef ref, final @NotNull String what)
            throws TLSMaterialException {
        if (ref instanceof MaterialRef.Inline inline) {
            return inline;
        }
        // A reference should have been resolved against the declared material before reaching here.
        // Reading one at this point would mean resolving it twice, in two places, with two chances to
        // disagree about what it named.
        throw new TLSMaterialException(("The %s is still a reference to material declared elsewhere. References "
                        + "are resolved before the files are read.")
                .formatted(what));
    }

    /// A password, read from its file where the configuration named one rather than wrote one.
    private static char @NotNull [] secret(final @Nullable Secret secret) throws TLSMaterialException {
        if (secret == null) {
            return new char[0];
        }
        return switch (secret) {
            case Secret.Literal literal -> literal.value().toCharArray();
            case Secret.FromFile fromFile -> {
                final Path file = Path.of(fromFile.path());
                try {
                    // Trailing newline removed: a password file written by an editor or projected by
                    // Kubernetes commonly ends with one, and it is not part of the password.
                    yield Files.readString(file, StandardCharsets.UTF_8)
                            .stripTrailing()
                            .toCharArray();
                } catch (final NoSuchFileException e) {
                    throw new TLSMaterialException("The password file '" + file + "' does not exist.", e);
                } catch (final IOException e) {
                    throw new TLSMaterialException("The password file '" + file + "' could not be read.", e);
                }
            }
        };
    }

    /// Material that could not be read. The message names the file and the fix, because it reaches
    /// an operator as the reason their surface did not start.
    public static final class TLSMaterialException extends Exception {

        private static final long serialVersionUID = 1L;

        private final @NotNull String reason;

        public TLSMaterialException(final @NotNull String reason) {
            super(reason);
            this.reason = reason;
        }

        public TLSMaterialException(final @NotNull String reason, final @NotNull Throwable cause) {
            super(reason, cause);
            this.reason = reason;
        }

        public @NotNull String reason() {
            return reason;
        }
    }
}
