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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Location of the certificate revocation lists used by the {@code revocation} axis under
 * {@link TrustMode#CHAIN}.
 *
 * <p>The counterpart to {@link Truststore}: the truststore says which issuers are trusted, this says
 * which certificates those issuers have since withdrawn. Without it, {@code revocation=CHECK} and
 * {@code revocation=REQUIRE_CRLS} cannot be satisfied for any path through a CA — the status of every
 * issuer is unknown, and unknown fails closed.
 *
 * <p>Read once at adapter start and never written, like {@link AllowList}.
 */
public record RevocationList(
        @JsonProperty(value = "path")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Revocation list path",
                description = "Path on the local file system to a certificate revocation list, or to a directory "
                        + "of them. PEM or DER. Required for revocation=CHECK or revocation=REQUIRE_CRLS whenever "
                        + "the certification path runs through a CA; a path that is trusted directly has no "
                        + "issuer to check and needs none.",
                required = true)
        @Nullable
        String path,

        /**
         * The entries this element carried besides {@code path}, or an empty map when every entry was
         * recognized. Same contract and rationale as {@link AllowList#unknownSettings()}: a misspelled
         * entry refuses the adapter rather than silently leaving the path unset, and the entries are
         * serialized back out verbatim so a writeback cannot delete them.
         */
        @JsonIgnore @NotNull Map<String, Object> unknownSettings) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(RevocationList.class);

    public RevocationList {
        // Nullable path for the same reason as AllowList: `<revocationList/>` binds to a null
        // component, and neither throwing nor substituting a value here would be right.
        unknownSettings = unknownSettings == null || unknownSettings.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(unknownSettings));
    }

    /** What a configuration file binds to; see {@link AllowList} for why the trap comes first. */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RevocationList(
            @JsonAnySetter final @Nullable Map<String, Object> unknownSettings,
            @JsonProperty("path") final @Nullable String path) {
        this(path, unknownSettings == null ? Map.of() : unknownSettings);
    }

    /** The shape a revocation list is built from in code; only deserialization can fill the trap. */
    public RevocationList(final @Nullable String path) {
        this(path, Map.of());
    }

    /** Serializes the trapped entries back out verbatim; see {@link Tls#unknownSettings()}. */
    @JsonAnyGetter
    @Override
    public @NotNull Map<String, Object> unknownSettings() {
        return unknownSettings;
    }

    /**
     * Accepts text where an object was expected — the same XML collapse {@link AllowList#fromText}
     * documents. The result is a revocation list with no path, which is reported as an actionable
     * start-up error rather than guessed at.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static @NotNull RevocationList fromText(final @Nullable String value) {
        if (value != null && !value.isBlank()) {
            log.warn(
                    "OPC UA adapter TLS configuration: 'revocationList' was read as the text '{}' rather than as "
                            + "an object. The path belongs in a nested element, as "
                            + "<revocationList><path>...</path></revocationList>. No revocation-list path has been "
                            + "configured.",
                    value.trim());
        }
        return new RevocationList(null);
    }
}
