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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The "identity" axis of certificate validation: does the presented certificate belong to the
 * server we think we are talking to? This is orthogonal to {@link TrustLevel}, which governs the
 * "trust" axis (how the certificate is established as trustworthy at all).
 *
 * <p>The canonical values ({@link #NONE}, {@link #APPLICATION_URI}, {@link #HOSTNAME},
 * {@link #APPLICATION_URI_AND_HOSTNAME}) describe identity checks only. The legacy values
 * {@link #STANDARD} and {@link #ALL} are deprecated compatibility aliases that additionally imply
 * a trust level; they are normalized to a {@code (trustLevel, tlsChecks)} pair at config parse time
 * (see {@link Tls}).
 */
public enum TlsChecks {

    /** No identity check is performed. */
    @JsonProperty("NONE")
    NONE("NONE"),

    /**
     * The OPC UA {@code ApplicationUri} announced by the server must match the SubjectAltName URI in
     * its certificate.
     */
    @JsonProperty("APPLICATION_URI")
    APPLICATION_URI("APPLICATION_URI"),

    /** The endpoint hostname must match a SubjectAltName DNSName / IP address in the certificate. */
    @JsonProperty("HOSTNAME")
    HOSTNAME("HOSTNAME"),

    /** Both {@link #APPLICATION_URI} and {@link #HOSTNAME} identity checks are performed. */
    @JsonProperty("APPLICATION_URI_AND_HOSTNAME")
    APPLICATION_URI_AND_HOSTNAME("APPLICATION_URI_AND_HOSTNAME"),

    /**
     * Deprecated alias. Normalizes to identity {@link #APPLICATION_URI} and forces
     * {@link TrustLevel#CHAIN} to {@link TrustLevel#CHAIN_PKI}. Use {@code trustLevel} +
     * {@code tlsChecks} explicitly instead.
     */
    @Deprecated
    @JsonProperty("STANDARD")
    STANDARD("STANDARD"),

    /**
     * Deprecated alias. Normalizes to identity {@link #APPLICATION_URI_AND_HOSTNAME} and forces
     * {@link TrustLevel#CHAIN} to {@link TrustLevel#CHAIN_PKI}. Use {@code trustLevel} +
     * {@code tlsChecks} explicitly instead.
     */
    @Deprecated
    @JsonProperty("ALL")
    ALL("ALL");

    private final @NotNull String tlsChecks;

    TlsChecks(final @NotNull String tlsChecks) {
        this.tlsChecks = tlsChecks;
    }

    /**
     * @return {@code true} if this is a deprecated compatibility alias ({@link #STANDARD} /
     *     {@link #ALL}) rather than a canonical identity value.
     */
    public boolean isDeprecatedAlias() {
        return this == STANDARD || this == ALL;
    }

    @Override
    public String toString() {
        return tlsChecks;
    }

    /**
     * Jackson creator method for deserialization.
     *
     * @param value the string value from JSON
     * @return the corresponding TlsChecks
     */
    @JsonCreator
    public static @Nullable TlsChecks fromString(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (final var mode : values()) {
            if (mode.name().equalsIgnoreCase(value)
                    || mode.name().replace("_", "").equalsIgnoreCase(value)) {
                return mode;
            }
        }
        return null;
    }
}
