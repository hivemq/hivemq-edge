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
 * The {@code trustMode} axis: how the server certificate is established as trustworthy at all.
 *
 * <p>Strictest value is {@link #CHAIN}, which is therefore the default when the axis is omitted.
 */
public enum TrustMode {

    /**
     * The certificate must chain to a trust anchor in the configured truststore, or to the JVM's
     * {@code cacerts} bundle when no truststore is configured.
     */
    @JsonProperty("CHAIN")
    CHAIN,

    /**
     * The SHA-256 fingerprint of the presented certificate must appear in the configured allow-list
     * file. The list is authored offline and read only; the adapter never adds to it.
     */
    @JsonProperty("ALLOW_LIST")
    ALLOW_LIST,

    /**
     * Any certificate is accepted; no trust is established at all. Vulnerable to MITM; intended for
     * environments with no PKI whatsoever.
     */
    @JsonProperty("ANY_CERT")
    ANY_CERT;

    /**
     * Jackson creator. Blank, absent or unrecognized yields {@code null}: the axis is unset and the
     * caller applies its strictest default, so a typo can never downgrade the security posture.
     */
    @JsonCreator
    public static @Nullable TrustMode fromString(final @Nullable String value) {
        return EnumParsing.parse(TrustMode.class, values(), value);
    }

    @Override
    public @NotNull String toString() {
        return name();
    }
}
