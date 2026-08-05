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

/**
 * TLS configuration of the OPC UA adapter.
 *
 * <p>Certificate validation is configured through exactly one of two mutually exclusive doors:
 *
 * <ul>
 *   <li>{@link #tlsChecks()} — a named preset for the common combinations;
 *   <li>{@link #tlsChecksFull()} — the six raw axes, for full control.
 * </ul>
 *
 * <p>Setting both is a configuration error. Setting neither means {@code tlsChecks=STANDARD}, so
 * every configuration written before these settings existed keeps behaving exactly as it did.
 *
 * <p><b>This record holds what the operator wrote, and nothing else.</b> There is deliberately no
 * normalization in the constructor: presets are not expanded into axes, omitted axes are not filled
 * in, nothing is derived. That is what makes writing an unchanged configuration back out a no-op.
 * Resolution to the flags that actually drive the checks happens at read time, in
 * {@link TlsChecksProjection#project(Tls)}.
 *
 * <p>For the same reason neither {@link #tlsChecks()} nor {@link #tlsChecksFull()} declares a
 * {@code defaultValue}: it would surface as a JSON-schema {@code default}, and the UI's form library
 * writes schema defaults back into the configuration it submits — which would set both doors at once
 * on an adapter that had set neither. See the note on {@link TlsChecksFull}.
 */
public record Tls(
        @JsonProperty("enabled")
        @ModuleConfigField(
                title = "Enable TLS",
                description = "Enables TLS encrypted connection",
                defaultValue = "false")
        boolean enabled,

        @JsonProperty("tlsChecks")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Certificate validation preset",
                description = "Named certificate-validation profile: "
                        + "STANDARD (default; chain + ApplicationUri + validity + revocation), "
                        + "ALL (STANDARD + hostname + key usage), "
                        + "APPLICATION_URI (chain + ApplicationUri), "
                        + "NONE (chain only - note that this still builds the trust chain), "
                        + "SELF_SIGNED (fingerprint allow-list + ApplicationUri + hostname + validity), or "
                        + "NO_VERIFICATION (accept any certificate; vulnerable to MITM). "
                        + "Leaving this unset means STANDARD. "
                        + "Mutually exclusive with tlsChecksFull.")
        @Nullable
        TlsChecks tlsChecks,

        @JsonProperty("tlsChecksFull")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Certificate validation (full control)",
                description = "The six independent validation axes, for configurations the presets do not cover. "
                        + "Every axis is optional and defaults to its strictest value when omitted, so an empty "
                        + "tlsChecksFull means maximum validation. Mutually exclusive with tlsChecks.")
        @Nullable
        TlsChecksFull tlsChecksFull,

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

        @JsonProperty("allowList")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Certificate allow-list",
                description = "Allow-list of permitted server-certificate SHA-256 fingerprints. Required when the "
                        + "effective trust mode is ALLOW_LIST (preset SELF_SIGNED, or "
                        + "tlsChecksFull.trustMode=ALLOW_LIST).")
        @Nullable
        AllowList allowList) {

    @JsonCreator
    public Tls {
        // Intentionally empty. See the class javadoc: nothing is normalized here, because writing an
        // unchanged configuration back out must return exactly what the operator wrote.
    }

    public static @NotNull Tls defaultTls() {
        return new Tls(false, null, null, null, null, null);
    }
}
