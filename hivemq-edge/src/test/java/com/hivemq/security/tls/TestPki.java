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
import java.time.Duration;
import java.time.Instant;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A root authority, an intermediate it signed, and broker certificates the intermediate signs.
final class TestPki {

    private final @NotNull TestAuthority intermediate;
    private final @NotNull TestAuthority root;

    /// The root valid from thirty days ago to thirty days ahead; the intermediate from ten days ago
    /// to thirty days ahead.
    TestPki(final @NotNull Instant now) throws Exception {
        this(now, now.minus(Duration.ofDays(10)), now.plus(Duration.ofDays(30)));
    }

    /// The root valid from thirty days ago to thirty days ahead; the intermediate as given.
    TestPki(final @NotNull Instant now, final @NotNull Instant intermediateFrom, final @NotNull Instant intermediateTo)
            throws Exception {
        this.root =
                TestAuthority.root("bridge-test-root", now.minus(Duration.ofDays(30)), now.plus(Duration.ofDays(30)));
        this.intermediate = root.intermediate("bridge-test-intermediate", intermediateFrom, intermediateTo);
    }

    /// The intermediate authority's certificate.
    @NotNull
    X509Certificate intermediate() {
        return intermediate.certificate();
    }

    /// A broker certificate for the host, with no key-usage extensions.
    @NotNull
    X509Certificate leaf(
            final @NotNull String hostname, final @NotNull Instant notBefore, final @NotNull Instant notAfter)
            throws Exception {
        return leaf(hostname, TLSFixtures.keyPair(), notBefore, notAfter, null, null);
    }

    /// A broker certificate for the host, with the given key usages.
    @NotNull
    X509Certificate leaf(
            final @NotNull String hostname,
            final @NotNull Instant notBefore,
            final @NotNull Instant notAfter,
            final @Nullable Integer keyUsage,
            final @Nullable KeyPurposeId extendedKeyUsage)
            throws Exception {
        return leaf(hostname, TLSFixtures.keyPair(), notBefore, notAfter, keyUsage, extendedKeyUsage);
    }

    /// A broker certificate for the host over the given keys, with the given key usages.
    @NotNull
    X509Certificate leaf(
            final @NotNull String hostname,
            final @NotNull KeyPair keys,
            final @NotNull Instant notBefore,
            final @NotNull Instant notAfter,
            final @Nullable Integer keyUsage,
            final @Nullable KeyPurposeId extendedKeyUsage)
            throws Exception {
        return intermediate.issue(hostname, keys, notBefore, notAfter, keyUsage, extendedKeyUsage);
    }

    /// A revocation list the intermediate signed, current from the given moment for a day, revoking
    /// the given certificate at the given moment.
    @NotNull
    X509CRL revocationList(
            final @NotNull X509Certificate revoked, final @NotNull Instant thisUpdate, final @NotNull Instant revokedAt)
            throws Exception {
        return intermediate.revocationList(revoked, thisUpdate, thisUpdate.plus(Duration.ofDays(1)), revokedAt);
    }

    /// The root authority's certificate.
    @NotNull
    X509Certificate root() {
        return root.certificate();
    }
}
