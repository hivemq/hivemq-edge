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
 * How the trustworthiness of a server certificate is established. This is the "trust" axis of
 * certificate validation and is orthogonal to {@link TlsChecks}, which governs the "identity" axis
 * (which server the certificate belongs to).
 */
public enum TrustLevel {

    /**
     * Accept any server certificate without building a trust chain. An explicit policy choice for
     * environments with no PKI at all (e.g. factories with self-signed per-machine certs). The
     * connection is vulnerable to MITM. Identity checks selected via {@link TlsChecks} are still
     * applied to the presented certificate.
     */
    @JsonProperty("TRUST")
    TRUST("TRUST"),

    /**
     * The certificate must chain to a trust anchor in the configured truststore (or the JVM's
     * {@code cacerts} if no truststore is configured). No PKI hygiene checks are performed.
     */
    @JsonProperty("CHAIN")
    CHAIN("CHAIN"),

    /**
     * As {@link #CHAIN}, plus PKI hygiene: certificate validity period, revocation, and key-usage
     * extensions.
     */
    @JsonProperty("CHAIN_PKI")
    CHAIN_PKI("CHAIN_PKI");

    private final @NotNull String trustLevel;

    TrustLevel(final @NotNull String trustLevel) {
        this.trustLevel = trustLevel;
    }

    @Override
    public String toString() {
        return trustLevel;
    }

    /**
     * Jackson creator method for deserialization.
     *
     * @param value the string value from JSON
     * @return the corresponding TrustLevel, or {@code null} if the value is null/blank/unknown
     */
    @JsonCreator
    public static @Nullable TrustLevel fromString(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (final var level : values()) {
            if (level.name().equalsIgnoreCase(value)
                    || level.name().replace("_", "").equalsIgnoreCase(value)) {
                return level;
            }
        }
        return null;
    }
}
