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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
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
                description = "Keystore that contains the client certificate including the chain. Required whenever "
                        + "the security policy is not NONE, and for X509 authentication.")
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
        AllowList allowList,

        @JsonProperty("revocationList")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Certificate revocation lists",
                description = "Location of the certificate revocation lists used by revocation=CHECK and "
                        + "revocation=REQUIRE_CRLS. A file or a directory of them, PEM or DER. Needed whenever the "
                        + "certification path runs through a CA: without CRLs the issuer's revocation status is "
                        + "unknown, and unknown fails closed.")
        @Nullable
        RevocationList revocationList,

        /**
         * The settings this element carried that the model does not know, or an empty map when every
         * setting was recognized.
         *
         * <p>Not a setting and not part of the configuration surface: a configuration file cannot
         * fill it directly — a literal {@code unknownSettings} element is itself an unknown name, so
         * it lands <em>inside</em> the map and is refused like any other unknown entry (pinned in
         * {@code TlsChecksParsingTest}). It exists so that "the operator wrote a setting this element
         * does not have" survives deserialization as far as
         * {@link TlsChecksProjection#project(Tls)}, which refuses the adapter naming the entry. The
         * application-wide unknown-setting handling only warns and drops, which is the wrong posture
         * here: a dropped certificate-validation setting silently runs the adapter under the
         * {@code STANDARD} default instead of whatever the entry was meant to select.
         *
         * <p>On serialization the entries are written back out verbatim, in place (see the
         * {@code @JsonAnyGetter} accessor). Dropping them would make the writeback of an unrelated
         * edit delete the misspelled entry from the file — after which the configuration is valid and
         * the adapter quietly starts under the default, which is exactly the outcome the trap exists
         * to prevent.
         */
        @JsonIgnore @NotNull Map<String, Object> unknownSettings) {

    public Tls {
        // The trap map is canonicalized (never null, insertion-ordered, unmodifiable) so that equal
        // configurations compare equal however they were built. Nothing else is normalized here - see
        // the class javadoc: writing an unchanged configuration back out must return exactly what the
        // operator wrote, and the trap is not operator configuration.
        unknownSettings = unknownSettings == null || unknownSettings.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(unknownSettings));
    }

    /**
     * What a configuration file binds to. The trap parameter comes first only to give this
     * constructor a signature distinct from the canonical one — Jackson binds by name and any-setter,
     * not by position. The component itself carries {@code @JsonIgnore} rather than
     * {@code @JsonAnySetter} deliberately: an any-setter-annotated field is picked up by the JSON
     * schema generator and would surface the trap in the UI form.
     */
    @JsonCreator
    public Tls(
            @JsonAnySetter final @Nullable Map<String, Object> unknownSettings,
            @JsonProperty("enabled") final boolean enabled,
            @JsonProperty("tlsChecks") final @Nullable TlsChecks tlsChecks,
            @JsonProperty("tlsChecksFull") final @Nullable TlsChecksFull tlsChecksFull,
            @JsonProperty("keystore") final @Nullable Keystore keystore,
            @JsonProperty("truststore") final @Nullable Truststore truststore,
            @JsonProperty("allowList") final @Nullable AllowList allowList,
            @JsonProperty("revocationList") final @Nullable RevocationList revocationList) {
        this(
                enabled,
                tlsChecks,
                tlsChecksFull,
                keystore,
                truststore,
                allowList,
                revocationList,
                unknownSettings == null ? Map.of() : unknownSettings);
    }

    /**
     * The shape configuration is built from in code; only deserialization can fill the trap.
     *
     * <p>Kept at six arguments when {@code revocationList} was added, rather than growing: it has
     * forty-odd call sites that have nothing to say about revocation lists, and a configuration that
     * does not mention one is exactly what {@code null} means here.
     */
    public Tls(
            final boolean enabled,
            final @Nullable TlsChecks tlsChecks,
            final @Nullable TlsChecksFull tlsChecksFull,
            final @Nullable Keystore keystore,
            final @Nullable Truststore truststore,
            final @Nullable AllowList allowList) {
        this(enabled, tlsChecks, tlsChecksFull, keystore, truststore, allowList, null, Map.of());
    }

    /** As above, for the configurations that do supply revocation lists. */
    public Tls(
            final boolean enabled,
            final @Nullable TlsChecks tlsChecks,
            final @Nullable TlsChecksFull tlsChecksFull,
            final @Nullable Keystore keystore,
            final @Nullable Truststore truststore,
            final @Nullable AllowList allowList,
            final @Nullable RevocationList revocationList) {
        this(enabled, tlsChecks, tlsChecksFull, keystore, truststore, allowList, revocationList, Map.of());
    }

    /**
     * Accepts text where an object was expected, because the configuration layer sometimes hands one
     * over — the same collapse {@link TlsChecksFull#fromText} documents, one element further up:
     * {@code <tls>} whose <em>first</em> child element is empty arrives here as the concatenated text
     * of whatever elements remain, and {@code <tls/>} arrives as {@code ""}.
     *
     * <ul>
     *   <li><b>Empty</b> means no TLS configuration at all and binds to {@link #defaultTls()} — TLS
     *       disabled, exactly what the REST path has always produced for an empty value.
     *   <li><b>Anything else</b> is a TLS configuration that can no longer be read, and it
     *       <b>throws</b> with an operator-facing message: the adapter's configuration is rejected at
     *       conversion, contained to that adapter, and a running instance is left unchanged. A
     *       carried-through sentinel — the {@code tlsChecksFull} treatment — is deliberately NOT used
     *       here: a sentinel makes the conversion succeed, which puts the unreadable configuration on
     *       the GET/PUT surface, where its serialized form is a clean {@code enabled=false} — and one
     *       routine save away from silently running without TLS.
     * </ul>
     *
     * <p>The collapsed text is deliberately <b>not</b> quoted back, and this is the worst of the three
     * places that rule applies: the collapse takes the element's whole {@code textContent}, which is
     * the concatenation of <em>every descendant's</em> text, so a collapsed {@code <tls>} carries the
     * flattened contents of any nested {@link Keystore} and {@link Truststore} - store password and
     * private-key password together. See {@link Truststore#fromText} for the disclosure path.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static @NotNull Tls fromText(final @Nullable String value) {
        if (value == null || value.isBlank()) {
            return defaultTls();
        }
        throw new IllegalArgumentException("The 'tls' configuration could not be read: it arrived as text rather "
                + "than as a set of elements, which happens when the first child element is left empty (for example "
                + "<enabled></enabled>, or a misspelled empty element). Which element each value belonged to cannot "
                + "be recovered, so the adapter configuration has been rejected. The text is not repeated here "
                + "because it can carry the keystore and truststore passwords. Give every element a value or remove "
                + "it entirely. An empty <tls/> is valid and means TLS is disabled.");
    }

    /**
     * Serializes the trapped entries back out verbatim, each under its own original name — never
     * under a literal {@code unknownSettings}. Writing an unchanged configuration back out must
     * return exactly what the operator wrote, including the entries that will refuse the adapter.
     */
    @JsonAnyGetter
    @Override
    public @NotNull Map<String, Object> unknownSettings() {
        return unknownSettings;
    }

    public static @NotNull Tls defaultTls() {
        return new Tls(false, null, null, null, null, null);
    }
}
