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
package com.hivemq.edge.adapters.opcua.config;

import org.jetbrains.annotations.NotNull;

/**
 * The result of projecting a {@link Tls} configuration onto the six validation axes: the flags that
 * directly drive the checks.
 *
 * <p>This is a read-time value. It is never serialized, never stored in the configuration, and has no
 * bearing on what a configuration writeback emits.
 */
public record EffectiveChecks(
        @NotNull TrustMode trustMode,
        @NotNull SanUriCheck sanUri,
        @NotNull HostnameCheck hostname,
        @NotNull ValidityCheck validity,
        @NotNull RevocationCheck revocation,
        @NotNull KeyUsageCheck keyUsage) {

    /** Whether the server's ApplicationUri must match the certificate's SubjectAltName URI. */
    public boolean isSanUri() {
        return sanUri == SanUriCheck.APPLICATION_URI;
    }

    /** Whether the endpoint hostname must appear in the certificate. */
    public boolean isHostname() {
        return hostname == HostnameCheck.HOSTNAME;
    }

    /** Whether the certificate's validity period is enforced. */
    public boolean isValidity() {
        return validity == ValidityCheck.NOT_BEFORE_OR_AFTER;
    }

    /** Whether revocation status is enforced at all. */
    public boolean isRevocation() {
        return revocation != RevocationCheck.NONE;
    }

    /** Whether a CRL is required for every non-end-entity CA in the path. */
    public boolean isRevocationLists() {
        return revocation == RevocationCheck.REQUIRE_CRLS;
    }

    /** Whether the KeyUsage extension is enforced. */
    public boolean isKeyUsage() {
        return keyUsage != KeyUsageCheck.NONE;
    }

    /** Whether the ExtendedKeyUsage extension is enforced. */
    public boolean isExtendedKeyUsage() {
        return keyUsage == KeyUsageCheck.SERVER_AUTH;
    }

    /** Whether a certification path is built at all. */
    public boolean isChainBuilt() {
        return trustMode == TrustMode.CHAIN;
    }

    /**
     * Whether any check that can be enforced on the end-entity certificate alone — without building a
     * certification path — is enabled: SubjectAltName URI, hostname, validity or key usage. These are
     * exactly the checks {@code CertificateChecks.apply} performs for the chainless trust modes.
     * Revocation is deliberately not part of the disjunction: it is decidable only while a path is
     * built, the projection rejects revocation without {@link TrustMode#CHAIN}, and counting it here
     * would claim a check the chainless validators cannot perform. When this is {@code false} under
     * {@link TrustMode#ANY_CERT}, nothing whatsoever is verified.
     */
    public boolean hasStandaloneEndEntityChecks() {
        return isSanUri() || isHostname() || isValidity() || isKeyUsage();
    }

    /** A compact, log-friendly rendering of all six axes. */
    public @NotNull String describe() {
        return "trustMode=%s, sanUri=%s, hostname=%s, validity=%s, revocation=%s, keyUsage=%s"
                .formatted(trustMode, sanUri, hostname, validity, revocation, keyUsage);
    }
}
