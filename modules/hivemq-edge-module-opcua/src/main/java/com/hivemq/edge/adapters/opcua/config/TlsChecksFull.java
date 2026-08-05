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

/**
 * The six independent axes of certificate validation — the "full control" door, mutually exclusive
 * with the {@code tlsChecks} presets.
 *
 * <p><b>Every axis is optional, and an omitted axis takes its strictest value.</b> A forgotten entry
 * must fail safe: omitting {@code hostname} yields more verification, never less. An empty
 * {@code tlsChecksFull} therefore means maximum validation, and every relaxation is an explicit,
 * visible choice in the configuration file.
 *
 * <p>Axes are stored exactly as written and are never rewritten. Resolution of an omitted axis to its
 * default happens at read time in {@link TlsChecksProjection}, so writing an unchanged configuration
 * back out is a no-op.
 *
 * <p><b>No axis declares a {@code defaultValue}, deliberately — do not add one.</b> A
 * {@code defaultValue} becomes a JSON-schema {@code default}, and React JSON Schema Form materializes
 * schema defaults into the form data it submits. Defaults here would therefore make the UI conjure a
 * complete {@code tlsChecksFull} object for every adapter, including adapters whose configuration only
 * sets {@code tlsChecks} — producing the "both doors set" configuration error, and filling in axes the
 * operator deliberately left omitted. Each default is stated in the axis description instead, where it
 * informs the operator without being submitted back.
 */
public record TlsChecksFull(
        @JsonProperty("trustMode")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Trust mode",
                description = "How the server certificate is established as trustworthy: "
                        + "CHAIN (must chain to a trust anchor in the truststore or the JVM cacerts), "
                        + "ALLOW_LIST (its SHA-256 fingerprint must appear in the configured allow-list file), or "
                        + "ANY_CERT (accept any certificate). "
                        + "WARNING: ANY_CERT establishes no trust at all and is vulnerable to MITM. "
                        + "Defaults to the strictest value, CHAIN, when omitted.")
        @Nullable
        TrustMode trustMode,

        @JsonProperty("sanUri")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "SubjectAltName URI check",
                description = "Whether the OPC UA ApplicationUri announced by the server must match the "
                        + "SubjectAltName URI in its certificate: NONE or APPLICATION_URI. "
                        + "Defaults to the strictest value, APPLICATION_URI, when omitted.")
        @Nullable
        SanUriCheck sanUri,

        @JsonProperty("hostname")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Hostname check",
                description = "Whether the endpoint hostname must match a SubjectAltName DNS name or IP address "
                        + "in the certificate: NONE or HOSTNAME. "
                        + "Defaults to the strictest value, HOSTNAME, when omitted.")
        @Nullable
        HostnameCheck hostname,

        @JsonProperty("validity")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Validity period check",
                description = "Whether the certificate's validity period is enforced: NONE or NOT_BEFORE_OR_AFTER. "
                        + "Defaults to the strictest value, NOT_BEFORE_OR_AFTER, when omitted.")
        @Nullable
        ValidityCheck validity,

        @JsonProperty("revocation")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Revocation check",
                description = "How hard revocation status is enforced: NONE, CHECK, or REQUIRE_CRLS. "
                        + "Only enforceable while a certification path is built, so any value other than NONE "
                        + "requires trustMode=CHAIN. "
                        + "Defaults to the strictest value, REQUIRE_CRLS, when omitted.")
        @Nullable
        RevocationCheck revocation,

        @JsonProperty("keyUsage")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Key usage check",
                description = "How strictly the certificate's declared purpose is enforced: NONE, KEY_USAGE "
                        + "(KeyUsage extension present and appropriate), or SERVER_AUTH (additionally requires "
                        + "an ExtendedKeyUsage permitting server authentication). "
                        + "Defaults to the strictest value, SERVER_AUTH, when omitted.")
        @Nullable
        KeyUsageCheck keyUsage) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(TlsChecksFull.class);

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TlsChecksFull {
        // Intentionally empty: axes are stored verbatim. Defaulting happens at read time in
        // TlsChecksProjection so that a configuration writeback cannot alter what the operator wrote.
    }

    /** Every axis unset, which resolves to the strictest value on each — maximum validation. */
    public static @NotNull TlsChecksFull allAxesUnset() {
        return new TlsChecksFull(null, null, null, null, null, null);
    }

    /**
     * Accepts text where an object was expected, because the configuration layer sometimes hands one
     * over.
     *
     * <p>Edge's XML-to-map conversion collapses a nested element to its text content whenever the
     * element's first child element is itself empty. So {@code <tlsChecksFull/>} arrives here as
     * {@code ""}, and {@code <tlsChecksFull><trustMode></trustMode><revocation>NONE</revocation></tlsChecksFull>}
     * arrives as {@code "NONE"} — the concatenated text of the remaining axes. Without this creator
     * Jackson refuses the coercion, and because every adapter is converted inside a single stream the
     * exception stops <em>all</em> adapters from being reconfigured, not just this one.
     *
     * <p>The empty form is the documented spelling of "maximum validation", so it is honoured exactly.
     * Any other text means the first axis element was left empty; which axis the surviving text belonged
     * to is unrecoverable, so it is reported and then treated the same way, for the reason
     * {@link EnumParsing} gives: falling back to unset can only ever produce more validation than was
     * asked for, never less, and throwing here would take every other adapter down.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static @NotNull TlsChecksFull fromText(final @Nullable String value) {
        if (value != null && !value.isBlank()) {
            log.warn(
                    "OPC UA adapter TLS configuration: 'tlsChecksFull' was read as the text '{}' rather than as a "
                            + "set of axes, which happens when its first axis element is left empty (for example "
                            + "<trustMode></trustMode>). Every axis has been treated as unset, which means the "
                            + "strictest value on each. Remove the empty element, or give it a value.",
                    value.trim());
        }
        return allAxesUnset();
    }
}
