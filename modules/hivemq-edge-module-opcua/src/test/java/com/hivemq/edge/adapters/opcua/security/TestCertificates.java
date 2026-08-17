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

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds self-signed certificates with precisely the properties a given check is supposed to look at,
 * so each axis can be exercised in isolation rather than inferred from a certificate that happens to
 * carry several attributes at once.
 */
final class TestCertificates {

    static final @NotNull String APPLICATION_URI = "urn:hivemq:edge:test-server";
    static final @NotNull String HOSTNAME = "opcua.factory.local";

    /** A well-formed certificate in PEM, for the tests that need something that is not a CRL. */
    static final @NotNull String SOME_PEM_CERTIFICATE = pemCertificate();

    private TestCertificates() {}

    static @NotNull Builder builder() {
        return new Builder();
    }

    /** A certificate carrying the application URI and hostname, valid, with no key-usage extensions. */
    static @NotNull X509Certificate identityOnly() throws Exception {
        return builder()
                .withApplicationUri(APPLICATION_URI)
                .withHostname(HOSTNAME)
                .build();
    }

    /**
     * Writes a CRL signed by a throwaway CA, revoking the given serial numbers.
     *
     * <p>The issuer name carries {@code EDG-585} so a test can tell the CRL it wrote from one it did
     * not, without asserting on a whole distinguished name.
     */
    static @NotNull Path writeCrl(final @NotNull Path target, final @NotNull List<BigInteger> revokedSerials)
            throws Exception {
        final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        final KeyPair caKeyPair = keyGen.generateKeyPair();
        final X500Name issuer = new X500Name("CN=EDG-585 Test CA");

        final Date now = Date.from(Instant.now());
        final X509v2CRLBuilder crlBuilder = new X509v2CRLBuilder(issuer, now);
        crlBuilder.setNextUpdate(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)));
        for (final BigInteger serial : revokedSerials) {
            crlBuilder.addCRLEntry(serial, now, CRLReason.privilegeWithdrawn);
        }
        final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(caKeyPair.getPrivate());
        final byte[] der = crlBuilder.build(signer).getEncoded();

        Files.writeString(
                target,
                "-----BEGIN X509 CRL-----\n"
                        + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                                .encodeToString(der)
                        + "\n-----END X509 CRL-----\n");
        return target;
    }

    private static @NotNull String pemCertificate() {
        try {
            final byte[] der = identityOnly().getEncoded();
            return "-----BEGIN CERTIFICATE-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII))
                            .encodeToString(der)
                    + "\n-----END CERTIFICATE-----\n";
        } catch (final Exception e) {
            throw new IllegalStateException("could not build the sample certificate", e);
        }
    }

    static final class Builder {

        private @Nullable String applicationUri;
        private @Nullable String hostname;
        private @Nullable KeyUsage keyUsage;
        private @Nullable ExtendedKeyUsage extendedKeyUsage;
        private @NotNull Instant notBefore = Instant.now().minus(1, ChronoUnit.DAYS);
        private @NotNull Instant notAfter = Instant.now().plus(365, ChronoUnit.DAYS);

        @NotNull
        Builder withApplicationUri(final @NotNull String applicationUri) {
            this.applicationUri = applicationUri;
            return this;
        }

        @NotNull
        Builder withHostname(final @NotNull String hostname) {
            this.hostname = hostname;
            return this;
        }

        /**
         * The KeyUsage bits the OPC UA profile demands of an end-entity certificate. {@code keyCertSign}
         * is included because these certificates are self-signed, and the stack requires a self-signed
         * end entity to be able to sign its own certificate.
         */
        @NotNull
        Builder withKeyUsage() {
            this.keyUsage = new KeyUsage(KeyUsage.digitalSignature
                    | KeyUsage.nonRepudiation
                    | KeyUsage.keyEncipherment
                    | KeyUsage.dataEncipherment
                    | KeyUsage.keyCertSign);
            return this;
        }

        /** KeyUsage without {@code keyCertSign} - the shape the Miele certificates have. */
        @NotNull
        Builder withKeyUsageMissingKeyCertSign() {
            this.keyUsage = new KeyUsage(KeyUsage.digitalSignature
                    | KeyUsage.nonRepudiation
                    | KeyUsage.keyEncipherment
                    | KeyUsage.dataEncipherment);
            return this;
        }

        @NotNull
        Builder withServerAuthExtendedKeyUsage() {
            this.extendedKeyUsage = new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth);
            return this;
        }

        /** ExtendedKeyUsage that permits only client authentication - wrong for a server. */
        @NotNull
        Builder withClientAuthExtendedKeyUsage() {
            this.extendedKeyUsage = new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth);
            return this;
        }

        @NotNull
        Builder expired() {
            this.notBefore = Instant.now().minus(30, ChronoUnit.DAYS);
            this.notAfter = Instant.now().minus(1, ChronoUnit.DAYS);
            return this;
        }

        @NotNull
        Builder notYetValid() {
            this.notBefore = Instant.now().plus(1, ChronoUnit.DAYS);
            this.notAfter = Instant.now().plus(30, ChronoUnit.DAYS);
            return this;
        }

        @NotNull
        X509Certificate build() throws Exception {
            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            final KeyPair keyPair = keyGen.generateKeyPair();
            final X500Name subject = new X500Name("CN=" + (hostname == null ? "test-server" : hostname));

            final JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                    subject,
                    BigInteger.valueOf(System.nanoTime()),
                    Date.from(notBefore),
                    Date.from(notAfter),
                    subject,
                    keyPair.getPublic());

            if (applicationUri != null || hostname != null) {
                final GeneralName[] names = applicationUri != null && hostname != null
                        ? new GeneralName[] {
                            new GeneralName(GeneralName.uniformResourceIdentifier, applicationUri),
                            new GeneralName(GeneralName.dNSName, hostname)
                        }
                        : applicationUri != null
                                ? new GeneralName[] {
                                    new GeneralName(GeneralName.uniformResourceIdentifier, applicationUri)
                                }
                                : new GeneralName[] {new GeneralName(GeneralName.dNSName, hostname)};
                builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(names));
            }
            if (keyUsage != null) {
                builder.addExtension(Extension.keyUsage, true, keyUsage);
            }
            if (extendedKeyUsage != null) {
                builder.addExtension(Extension.extendedKeyUsage, false, extendedKeyUsage);
            }

            final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
            return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
        }
    }
}
