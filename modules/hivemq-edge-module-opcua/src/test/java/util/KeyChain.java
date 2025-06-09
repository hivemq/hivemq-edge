package util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class KeyChain {

    public static final @NotNull String DOMAIN_PREFIX = "urn:hivemq:edge:";

    private static final @NotNull String CN_ROOT = "root";
    private static final @NotNull String CN_ISSUER = "issuer";
    private static final @NotNull String KEYPAIR_GEN_ALGO = "RSA";
    private static final @NotNull String CERT_SIGN_ALGO = "SHA256WithRSA";
    private static final @NotNull String KEYSTORE_TYPE = "JKS";
    private static final @NotNull String KEYSTORE_FILE_EXT = ".jks";
    private static final @NotNull String X500_DIR_NAME_PREFIX = "CN=";

    private final @NotNull GeneratedCert root;
    private final @NotNull GeneratedCert issuer;
    private final @NotNull Map<String, GeneratedCert> leafCerts;

    private KeyChain(
            @NotNull final GeneratedCert root,
            @NotNull final GeneratedCert issuer,
            @NotNull final Map<String, GeneratedCert> leafCerts) {
        this.root = root;
        this.issuer = issuer;
        this.leafCerts = leafCerts;
    }

    /**
     * Creates a KeyChain with a root CA, an issuer and leaf certificates for the given domains.
     *
     * @param leafCertDomains The domains for which to create leaf certificates.
     * @return A KeyChain containing the root CA, issuer, and leaf certificates.
     * @throws Exception If there is an error creating the certificates.
     */
    public static @NotNull KeyChain createKeyChain(final @NotNull String @NotNull ... leafCertDomains)
            throws Exception {
        final GeneratedCert rootCA = createCertificate(CN_ROOT, null, null, true);
        final GeneratedCert issuer = createCertificate(CN_ISSUER, null, rootCA, true);
        final Map<String, GeneratedCert> leafCerts = new HashMap<>();
        for (final String leafCertDomain : leafCertDomains) {
            leafCerts.put(leafCertDomain,
                    createCertificate(leafCertDomain, DOMAIN_PREFIX + leafCertDomain, rootCA, false));
        }
        return new KeyChain(rootCA, issuer, leafCerts);
    }

    /**
     * @param cnName The CN={name} of the certificate. When the certificate is for a domain it should be the domain name
     * @param domain Nullable. The DNS domain for the certificate.
     * @param issuer Issuer who signs this certificate. Null for a self-signed certificate
     * @param isCA   Can this certificate be used to sign other certificates
     * @return Newly created certificate with its private key
     */
    private static GeneratedCert createCertificate(
            final @NotNull String cnName,
            final @Nullable String domain,
            final @Nullable GeneratedCert issuer,
            final boolean isCA) throws Exception {

        // Generate the key-pair with the official Java API's
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEYPAIR_GEN_ALGO);
        final KeyPair certKeyPair = keyGen.generateKeyPair();
        final X500Name name = new X500Name(X500_DIR_NAME_PREFIX + cnName);

        // If you issue more than just test certificates, you might want a decent serial number schema ^.^
        final BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        final Instant validFrom = Instant.now();
        final Instant validUntil = validFrom.plus(10 * 360, ChronoUnit.DAYS);

        // If there is no issuer, we self-sign our certificate.
        final X500Name issuerName;
        final PrivateKey issuerKey;
        if (issuer == null) {
            issuerName = name;
            issuerKey = certKeyPair.getPrivate();
        } else {
            // Get issuer's subject DN directly from its certificate
            issuerName = new X500Name(issuer.certificate().getSubjectX500Principal().getName());
            issuerKey = issuer.keyPair().getPrivate();
        }

        // The cert builder to build up our certificate information
        final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(issuerName,
                serialNumber,
                Date.from(validFrom),
                Date.from(validUntil),
                name,
                certKeyPair.getPublic());

        if (isCA) {
            // Make the cert to a Cert Authority to sign more certs when needed
            builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        }

        // Subject Alternative Names (SANs) - Combine DNS and URI
        if (domain != null) {
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName[]{
                    new GeneralName(GeneralName.dNSName, domain),
                    new GeneralName(GeneralName.uniformResourceIdentifier, domain)}));
        }

        // Finally, sign the certificate:
        final ContentSigner signer = new JcaContentSignerBuilder(CERT_SIGN_ALGO).build(issuerKey);
        final X509Certificate cert = new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        return new GeneratedCert(cert, certKeyPair);
    }

//    public @NotNull File wrapInTrustStore(final @NotNull String trustStore) throws Exception {
//        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
//        keyStore.load(null, null);
//        keyStore.setCertificateEntry(CN_ROOT, root.certificate());
//        keyStore.setCertificateEntry(CN_ISSUER, issuer.certificate());
//        for (final Map.Entry<String, GeneratedCert> entry : leafCerts.entrySet()) {
//            keyStore.setCertificateEntry(entry.getKey(), entry.getValue().certificate());
//        }
//
//        final File keyStoreFile = File.createTempFile(trustStore + KEYSTORE_FILE_EXT, null);
//        keyStoreFile.deleteOnExit();
//        try (final FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
//            keyStore.store(fos, "password".toCharArray());
//        }
//        return keyStoreFile;
//    }

    @TestOnly
    public @NotNull File wrapInKeyStoreWithPrivateKey(
            final @NotNull String filename,
            final @NotNull String cnName,
            final @NotNull String keyStorePassword,
            final @NotNull String privateKeyPassword) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        keyStore.load(null, null);

        for (final Map.Entry<String, GeneratedCert> entry : leafCerts.entrySet()) {
            keyStore.setCertificateEntry(entry.getKey(), entry.getValue().certificate());
        }
        keyStore.setCertificateEntry(CN_ISSUER, issuer.certificate());
        keyStore.setCertificateEntry(CN_ROOT, root.certificate());

        final GeneratedCert generatedLeafCert = leafCerts.get(cnName);

        final Certificate[] chain = {
                generatedLeafCert.certificate(), issuer.certificate(), root.certificate()};
        keyStore.setKeyEntry(cnName, generatedLeafCert.keyPair().getPrivate(), privateKeyPassword.toCharArray(), chain);

        final File keyStoreFile = File.createTempFile(filename + KEYSTORE_FILE_EXT, null);
        keyStoreFile.deleteOnExit();
        try (final FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, keyStorePassword.toCharArray());
        }
        return keyStoreFile;
    }

    public @NotNull GeneratedCert getRoot() {
        return root;
    }

    // To create a certificate chain we need the issuers' certificate and private key. Keep these together to pass around
    public record GeneratedCert(X509Certificate certificate, KeyPair keyPair) {
    }
}
