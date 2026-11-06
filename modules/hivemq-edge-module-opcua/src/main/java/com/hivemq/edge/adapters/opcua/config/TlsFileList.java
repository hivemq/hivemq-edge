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

import static com.fasterxml.jackson.annotation.JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.adapter.sdk.api.annotations.ModuleConfigField;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A set of files whose contents describe themselves, so no format is declared.
///
/// One type for `tls-allowlist` and `tls-revocationlist`, which the model gives the same three
/// elements: an allow-list holds one SHA-256 fingerprint per line, and a revocation list is read as
/// either PEM or DER by the same call. Neither has anything to declare about its encoding, and a
/// second identical record would be two places to change.
///
/// The superseded spellings are `allowList` and `revocationList`, each holding a single path.
public record TlsFileList(
        @JsonProperty(value = "file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Files", description = "The files belonging to this material.")
        @Nullable
        List<String> files,

        @JsonProperty(value = "directory")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Directories",
                description = "Directories whose readable entries all belong to this material.")
        @Nullable
        List<FileLocation> directories,

        @JsonProperty(value = "pattern")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Patterns", description = "Glob patterns whose matches belong to this material.")
        @Nullable
        List<FileLocation> patterns,

        @JsonProperty(value = "reference-name")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Reference by name",
                description =
                        "Name of an entry declared in a shared tls-material, written " + "'material-name::entry-name'.")
        @Nullable
        String referenceName,

        @JsonProperty(value = "reference-uuid")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Reference by uuid",
                description = "The uuid of an entry declared in a shared tls-material.")
        @Nullable
        String referenceUuid,

        /// The entries this element carried that the model does not know, or an empty map when every
        /// entry was recognized. Same contract and rationale as [Tls#unknownSettings()]: the
        /// application-wide mapper drops an unknown property silently, so a misspelled `<fil>` would
        /// leave this material unconfigured with nothing but a log line to say so - and on a
        /// certificate allow-list that means trusting more than the configuration says.
        @JsonIgnore @NotNull Map<String, Object> unknownSettings) {

    public TlsFileList {
        files = files == null ? null : List.copyOf(files);
        directories = directories == null ? null : List.copyOf(directories);
        patterns = patterns == null ? null : List.copyOf(patterns);
        unknownSettings = unknownSettings == null || unknownSettings.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(unknownSettings));
    }

    /// What a configuration file binds to; see [Tls] for why the trap comes first.
    ///
    /// The list-valued entries accept a single value as a one-element list. The configuration file
    /// reader turns repeated sibling elements into a list but a single element into a scalar, so one
    /// `<file>` would otherwise refuse to bind.
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TlsFileList(
            @JsonAnySetter final @Nullable Map<String, Object> unknownSettings,
            @JsonProperty("file") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY) final @Nullable List<String> files,
            @JsonProperty("directory") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    final @Nullable List<FileLocation> directories,
            @JsonProperty("pattern") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    final @Nullable List<FileLocation> patterns,
            @JsonProperty("reference-name") final @Nullable String referenceName,
            @JsonProperty("reference-uuid") final @Nullable String referenceUuid) {
        this(
                files,
                directories,
                patterns,
                referenceName,
                referenceUuid,
                unknownSettings == null ? Map.of() : unknownSettings);
    }

    /// The shape this is built from in code; only deserialization can fill the trap.
    public TlsFileList(
            final @Nullable List<String> files,
            final @Nullable List<FileLocation> directories,
            final @Nullable List<FileLocation> patterns,
            final @Nullable String referenceName,
            final @Nullable String referenceUuid) {
        this(files, directories, patterns, referenceName, referenceUuid, Map.of());
    }

    /// Serializes the trapped entries back out verbatim; see [Tls#unknownSettings()].
    @JsonAnyGetter
    @Override
    public @NotNull Map<String, Object> unknownSettings() {
        return unknownSettings;
    }

    /// Whether this names nothing at all. The three lists stay nullable so that writing an unchanged
    /// configuration back out returns what the operator wrote rather than empty elements they never
    /// typed, which is why asking "is anything named" needs a method rather than three null checks
    /// at every call site.
    public boolean namesNothing() {
        return (files == null || files.isEmpty())
                && (directories == null || directories.isEmpty())
                && (patterns == null || patterns.isEmpty());
    }

    @Override
    public @NotNull String toString() {
        return "TlsFileList{files=" + files + ", directories=" + directories + ", patterns=" + patterns
                + ", referenceName=" + referenceName + ", referenceUuid=" + referenceUuid + "}";
    }
}
