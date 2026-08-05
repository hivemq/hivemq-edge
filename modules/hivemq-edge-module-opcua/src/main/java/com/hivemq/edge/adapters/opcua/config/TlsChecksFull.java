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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        KeyUsageCheck keyUsage,

        /**
         * The text this element collapsed into, or {@code null} when the axes were read as written.
         *
         * <p>Not a setting and not part of the configuration surface: it never appears in the JSON
         * schema, is never serialized, and cannot be set from a configuration file. It exists so that
         * "these axes could not be read" survives deserialization as far as
         * {@link TlsChecksProjection#project(Tls)}, which is where the adapter is refused. Carrying the
         * text rather than a flag lets that refusal quote back what was actually found.
         */
        @JsonIgnore @Nullable String collapsedText) {

    /**
     * The axes exactly as the operator wrote them. This is the shape a configuration file binds to;
     * the canonical seven-argument constructor is reserved for {@link #fromText}.
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TlsChecksFull(
            @JsonProperty("trustMode") final @Nullable TrustMode trustMode,
            @JsonProperty("sanUri") final @Nullable SanUriCheck sanUri,
            @JsonProperty("hostname") final @Nullable HostnameCheck hostname,
            @JsonProperty("validity") final @Nullable ValidityCheck validity,
            @JsonProperty("revocation") final @Nullable RevocationCheck revocation,
            @JsonProperty("keyUsage") final @Nullable KeyUsageCheck keyUsage) {
        // Axes are stored verbatim. Defaulting happens at read time in TlsChecksProjection so that a
        // configuration writeback cannot alter what the operator wrote.
        this(trustMode, sanUri, hostname, validity, revocation, keyUsage, null);
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
     * arrives as {@code "NONE"} — the concatenated text of whatever axes remain.
     *
     * <p>The two cases are not the same mistake and are not treated alike:
     *
     * <ul>
     *   <li><b>Empty</b> is the documented spelling of "maximum validation". Nothing was lost, so it is
     *       honoured exactly.
     *   <li><b>Anything else</b> means the operator configured axes that can no longer be read. Which
     *       axis each value belonged to is unrecoverable — {@code revocation=NONE} and
     *       {@code hostname=NONE} both arrive as {@code "NONE"} — so the text is carried through to
     *       {@link TlsChecksProjection}, which refuses to start the adapter. Silently resolving it to
     *       maximum validation would be safe in the validation direction but would discard a security
     *       setting the operator explicitly wrote, leaving nothing but a start-up log line to say so.
     * </ul>
     *
     * <p>Deliberately does not throw. Throwing here happens during configuration conversion, which
     * would leave the adapter absent or silently stale rather than visibly failed, and reports nothing
     * on the adapter itself.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static @NotNull TlsChecksFull fromText(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return allAxesUnset();
        }
        return new TlsChecksFull(null, null, null, null, null, null, value.trim());
    }
}
