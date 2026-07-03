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

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Tls(
        @JsonProperty("enabled")
        @ModuleConfigField(
                title = "Enable TLS",
                description = "Enables TLS encrypted connection",
                defaultValue = "false")
        boolean enabled,

        @JsonProperty("tlsChecks")
        @ModuleConfigField(
                title = "Certificate identity checks",
                description = "Identity checks performed on the server certificate: NONE, APPLICATION_URI, "
                        + "HOSTNAME, or APPLICATION_URI_AND_HOSTNAME. This is orthogonal to trustLevel. "
                        + "The legacy values STANDARD and ALL are deprecated aliases that are normalized to a "
                        + "trustLevel/tlsChecks pair.",
                defaultValue = "APPLICATION_URI")
        @Nullable
        TlsChecks tlsChecks,

        @JsonProperty("keystore")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Keystore",
                description =
                        "Keystore that contains the client certificate including the chain. Required for X509 authentication.")
        @Nullable
        Keystore keystore,

        @JsonProperty("truststore")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Truststore",
                description = "Truststore which contains the trusted server certificates or trusted intermediates.")
        @Nullable
        Truststore truststore,

        @JsonProperty("trustLevel")
        @ModuleConfigField(
                title = "Trust level",
                description = "How the server certificate is established as trustworthy: "
                        + "CHAIN (must chain to a trust anchor in the truststore or JVM cacerts), "
                        + "CHAIN_PKI (as CHAIN, plus validity and revocation checks), or "
                        + "TRUST (accept any server certificate without chain validation). "
                        + "WARNING: trustLevel=TRUST is vulnerable to MITM and is intended for environments "
                        + "without a CA (e.g. factories with self-signed per-machine certs) only. "
                        + "Orthogonal to tlsChecks.",
                defaultValue = "CHAIN")
        @Nullable
        TrustLevel trustLevel) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(Tls.class);

    @JsonCreator
    @SuppressWarnings("deprecation") // intentionally references the STANDARD/ALL aliases to normalize them
    public Tls {
        // Normalize the two orthogonal knobs (trust x identity), expanding the deprecated
        // STANDARD/ALL aliases and applying backward-compatible defaults. After construction,
        // tlsChecks always holds a canonical identity value and trustLevel is never null.
        //
        // Legacy behavior preserved: an unset config == STANDARD == CHAIN_PKI + APPLICATION_URI.
        // An unset trustLevel implies CHAIN for canonical identity values, but CHAIN_PKI whenever
        // PKI hygiene was implied (deprecated alias or unset). A deprecated alias combined with an
        // explicit CHAIN is promoted to CHAIN_PKI so the alias's PKI-hygiene part survives.
        final boolean aliasImpliesPki = tlsChecks == null || tlsChecks.isDeprecatedAlias();

        final TlsChecks identity =
                switch (tlsChecks == null ? TlsChecks.STANDARD : tlsChecks) {
                    case STANDARD -> TlsChecks.APPLICATION_URI;
                    case ALL -> TlsChecks.APPLICATION_URI_AND_HOSTNAME;
                    case NONE -> TlsChecks.NONE;
                    case APPLICATION_URI -> TlsChecks.APPLICATION_URI;
                    case HOSTNAME -> TlsChecks.HOSTNAME;
                    case APPLICATION_URI_AND_HOSTNAME -> TlsChecks.APPLICATION_URI_AND_HOSTNAME;
                };

        TrustLevel resolvedTrust;
        if (trustLevel == null) {
            resolvedTrust = aliasImpliesPki ? TrustLevel.CHAIN_PKI : TrustLevel.CHAIN;
        } else if (trustLevel == TrustLevel.CHAIN && tlsChecks != null && tlsChecks.isDeprecatedAlias()) {
            // Deprecated alias forces CHAIN -> CHAIN_PKI to preserve legacy PKI hygiene.
            resolvedTrust = TrustLevel.CHAIN_PKI;
        } else {
            resolvedTrust = trustLevel;
        }

        if (tlsChecks != null && tlsChecks.isDeprecatedAlias()) {
            // The legacy ALL level enforced key-usage / extended-key-usage checks that CHAIN_PKI does
            // not; surface that security change explicitly so it is not applied silently on upgrade.
            // STANDARD maps to CHAIN_PKI + APPLICATION_URI with an identical check set, so it needs no note.
            final String keyUsageNote = (tlsChecks == TlsChecks.ALL && resolvedTrust == TrustLevel.CHAIN_PKI)
                    ? " SECURITY CHANGE: the legacy ALL level also enforced certificate key-usage and "
                            + "extended-key-usage checks, which trustLevel=CHAIN_PKI does NOT perform. Server "
                            + "certificates with missing or non-conforming key-usage extensions that ALL "
                            + "previously rejected will now be accepted."
                    : "";
            log.warn(
                    "OPC UA adapter TLS config: tlsChecks={} is deprecated and was normalized to "
                            + "trustLevel={}, tlsChecks={}. Update the configuration to the explicit values; "
                            + "the STANDARD/ALL aliases will be removed in a future release.{}",
                    tlsChecks,
                    resolvedTrust,
                    identity,
                    keyUsageNote);
        }

        tlsChecks = identity;
        trustLevel = resolvedTrust;
    }

    public static @NotNull Tls defaultTls() {
        return new Tls(false, null, null, null, null);
    }
}
