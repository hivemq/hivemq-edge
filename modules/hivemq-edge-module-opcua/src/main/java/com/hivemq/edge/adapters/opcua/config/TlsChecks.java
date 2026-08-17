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
 * Named presets for certificate validation — the "common cases" door, mutually exclusive with the
 * {@link TlsChecksFull} axes.
 *
 * <p>Each preset is an exact combination of the six axes; see {@link TlsChecksProjection} for the
 * mapping, which is the single source of truth. Presets are stored verbatim and expanded at read
 * time, never rewritten into the configuration.
 */
public enum TlsChecks {

    /**
     * Chain validation only: the certificate must chain to a trust anchor, and nothing else is
     * checked.
     *
     * <p>The name is a trap we are stuck with for backward compatibility: it does <b>not</b> mean "no
     * validation" — the chain is still built. Reading it as "no validation" is the misunderstanding
     * that originated EDG-585. Use {@link #NO_VERIFICATION} if that is what is actually wanted.
     */
    @JsonProperty("NONE")
    NONE,

    /** Chain validation plus the ApplicationUri identity check. */
    @JsonProperty("APPLICATION_URI")
    APPLICATION_URI,

    /** The default. Chain validation, ApplicationUri, validity period and revocation including CRLs. */
    @JsonProperty("STANDARD")
    STANDARD,

    /** {@link #STANDARD} plus hostname verification and key-usage enforcement. */
    @JsonProperty("ALL")
    ALL,

    /**
     * For environments with no CA: trust is established from an offline-authored allow-list of
     * certificate fingerprints, and the certificate must still assert the right application, host and
     * validity period. Revocation and key-usage machinery, which such deployments typically cannot
     * provide, is not required.
     */
    @JsonProperty("SELF_SIGNED")
    SELF_SIGNED,

    /**
     * Accept anything: no trust, no identity, no hygiene. The honest spelling of "do not verify".
     *
     * <p>WARNING: a deployment running this is vulnerable to man-in-the-middle attacks. Prefer
     * {@link #SELF_SIGNED}, which costs one fingerprint per server and closes that hole.
     */
    @JsonProperty("NO_VERIFICATION")
    NO_VERIFICATION;

    @JsonCreator
    public static @Nullable TlsChecks fromString(final @Nullable String value) {
        return EnumParsing.parse(TlsChecks.class, values(), value);
    }

    @Override
    public @NotNull String toString() {
        return name();
    }
}
