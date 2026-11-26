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

import java.security.KeyPair;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v2CRLBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A certificate authority for the tests of this package: its keys and certificate, and the
/// certificates and revocation lists it signs. Its key usage permits signing both.
final class TestAuthority {

    private final @NotNull X509Certificate certificate;
    private final @NotNull KeyPair keys;

    private TestAuthority(final @NotNull X509Certificate certificate, final @NotNull KeyPair keys) {
        this.certificate = certificate;
        this.keys = keys;
    }

    /// A self-signed root authority named `CN=<name>`, valid between the two moments.
    static @NotNull TestAuthority root(
            final @NotNull String name, final @NotNull Instant notBefore, final @NotNull Instant notAfter)
            throws Exception {
        final KeyPair keys = TLSFixtures.keyPair();
        return new TestAuthority(
                TLSFixtures.certificate(
                        name,
                        keys,
                        null,
                        keys,
                        true,
                        notBefore,
                        notAfter,
                        KeyUsage.keyCertSign | KeyUsage.cRLSign,
                        null),
                keys);
    }

    /// This authority's certificate.
    @NotNull
    X509Certificate certificate() {
        return certificate;
    }

    /// An authority named `CN=<name>` that this one signed, valid between the two moments.
    @NotNull
    TestAuthority intermediate(
            final @NotNull String name, final @NotNull Instant notBefore, final @NotNull Instant notAfter)
            throws Exception {
        final KeyPair intermediateKeys = TLSFixtures.keyPair();
        return new TestAuthority(
                TLSFixtures.certificate(
                        name,
                        intermediateKeys,
                        certificate,
                        keys,
                        true,
                        notBefore,
                        notAfter,
                        KeyUsage.keyCertSign | KeyUsage.cRLSign,
                        null),
                intermediateKeys);
    }

    /// A broker certificate for the host, over the given keys, that this authority signed.
    @NotNull
    X509Certificate issue(
            final @NotNull String hostname,
            final @NotNull KeyPair brokerKeys,
            final @NotNull Instant notBefore,
            final @NotNull Instant notAfter,
            final @Nullable Integer keyUsage,
            final @Nullable KeyPurposeId extendedKeyUsage)
            throws Exception {
        return TLSFixtures.certificate(
                hostname, brokerKeys, certificate, keys, false, notBefore, notAfter, keyUsage, extendedKeyUsage);
    }

    /// A revocation list this authority signed, current between the two given moments, revoking the
    /// given certificate at the given moment.
    @SuppressWarnings("JavaUtilDate") // JcaX509v2CRLBuilder requires java.util.Date.
    @NotNull
    X509CRL revocationList(
            final @NotNull X509Certificate revoked,
            final @NotNull Instant thisUpdate,
            final @NotNull Instant nextUpdate,
            final @NotNull Instant revokedAt)
            throws Exception {
        final X509v2CRLBuilder builder =
                new JcaX509v2CRLBuilder(certificate.getSubjectX500Principal(), Date.from(thisUpdate));
        builder.setNextUpdate(Date.from(nextUpdate));
        builder.addCRLEntry(revoked.getSerialNumber(), Date.from(revokedAt), CRLReason.keyCompromise);
        builder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                new JcaX509ExtensionUtils().createAuthorityKeyIdentifier(certificate));
        return new JcaX509CRLConverter()
                .getCRL(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate())));
    }
}
