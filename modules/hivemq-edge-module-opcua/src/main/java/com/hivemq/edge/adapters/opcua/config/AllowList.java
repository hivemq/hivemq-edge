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
 * Location of the server-certificate allow-list used by {@link TrustMode#ALLOW_LIST}.
 *
 * <p>The file is authored offline and read only — the adapter never writes to it. That is what makes
 * it deployable as a read-only secret and keeps the root of trust out of reach of anything running at
 * runtime.
 */
public record AllowList(
        @JsonProperty(value = "path")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Allow-list path",
                description = "Path on the local file system to the server-certificate allow-list: one SHA-256 "
                        + "certificate fingerprint per line, hexadecimal, optional ':' separators, '#' for "
                        + "comments. Required when the effective trust mode is ALLOW_LIST.",
                required = true)
        @Nullable
        String path,

        /**
         * The entries this element carried besides {@code path}, or an empty map when every entry was
         * recognized. Same contract and same rationale as {@link Tls#unknownSettings()}: a misspelled
         * entry — {@code <pth>} for {@code <path>} — refuses the adapter in
         * {@link TlsChecksProjection#project(Tls)} instead of silently leaving the path unset, and the
         * entries are serialized back out verbatim so a writeback cannot delete them.
         */
        @JsonIgnore @NotNull Map<String, Object> unknownSettings) {

    private static final @NotNull Logger log = LoggerFactory.getLogger(AllowList.class);

    public AllowList {
        // The path is nullable because an operator can write `<allowList/>` with nothing inside it, and
        // Jackson binds that to a null component. Neither rejecting nor defaulting it here would be
        // right: throwing aborts the conversion of every other adapter in the same refresh, and
        // substituting a value would rewrite what the operator wrote. A missing path is instead caught
        // by TlsChecksProjection, which reports it as the same actionable start-up error as an absent
        // allowList element. The trap map is canonicalized so equal configurations compare equal.
        unknownSettings = unknownSettings == null || unknownSettings.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(unknownSettings));
    }

    /**
     * What a configuration file binds to. The trap parameter comes first only to give this
     * constructor a signature distinct from the canonical one; see {@link Tls} for why the component
     * carries {@code @JsonIgnore} rather than {@code @JsonAnySetter}.
     */
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public AllowList(
            @JsonAnySetter final @Nullable Map<String, Object> unknownSettings,
            @JsonProperty("path") final @Nullable String path) {
        this(path, unknownSettings == null ? Map.of() : unknownSettings);
    }

    /** The shape an allow-list is built from in code; only deserialization can fill the trap. */
    public AllowList(final @Nullable String path) {
        this(path, Map.of());
    }

    /** Serializes the trapped entries back out verbatim; see {@link Tls#unknownSettings()}. */
    @JsonAnyGetter
    @Override
    public @NotNull Map<String, Object> unknownSettings() {
        return unknownSettings;
    }

    /**
     * Accepts text where an object was expected.
     *
     * <p>Edge's XML-to-map conversion collapses a nested element to its text content whenever the
     * element's first child element is itself empty, so both {@code <allowList/>} and
     * {@code <allowList><path></path></allowList>} arrive here as {@code ""}. Without this creator
     * Jackson refuses the coercion, and because every adapter is converted inside a single stream the
     * exception stops <em>all</em> adapters from being reconfigured, not just this one.
     *
     * <p>The result is an allow-list with no path, which {@link TlsChecksProjection} turns into the
     * same actionable start-up error as an absent {@code allowList}. Text is deliberately not read as
     * the path: an operator who wrote {@code <allowList>/some/file</allowList>} instead of nesting it
     * in {@code <path>} should be told, not silently guessed at.
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static @NotNull AllowList fromText(final @Nullable String value) {
        if (value != null && !value.isBlank()) {
            log.warn(
                    "OPC UA adapter TLS configuration: 'allowList' was read as the text '{}' rather than as an "
                            + "object. The path belongs in a nested element, as "
                            + "<allowList><path>...</path></allowList>. No allow-list path has been configured.",
                    value.trim());
        }
        return new AllowList(null);
    }
}
