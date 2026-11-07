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

/// The certificate authorities whose certificates Edge accepts from the OPC UA server.
///
/// Resolves to a set, so files, directories, glob patterns and the runtime's own truststore all
/// repeat and merge: a surface trusting the public authorities and a plant authority together names
/// both and gets the union.
///
/// The superseded spelling is `truststore`, which holds a single container and its password.
public record TlsTruststore(
        @JsonProperty(value = "format")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Format", description = "p12, jks or pem.")
        @Nullable
        MaterialFormat format,

        @JsonProperty(value = "file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Files", description = "Files holding trusted certificates.")
        @Nullable
        List<String> files,

        @JsonProperty(value = "directory")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Directories",
                description = "Directories whose readable entries all hold trusted certificates.")
        @Nullable
        List<FileLocation> directories,

        @JsonProperty(value = "pattern")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Patterns", description = "Glob patterns whose matches hold trusted certificates.")
        @Nullable
        List<FileLocation> patterns,

        /// A truststore the runtime carries rather than the operator supplies. `JVM-cacerts` is the
        /// only name, and it is the public certificate authorities the JVM ships with.
        @JsonProperty(value = "builtin")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Built-in truststores",
                description = "A truststore the runtime carries. JVM-cacerts is the only name, and it is "
                        + "the public certificate authorities the JVM ships with.")
        @Nullable
        List<String> builtins,

        @JsonProperty(value = "password")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Password", description = "The password that opens a p12 or jks container.")
        @Nullable
        String password,

        @JsonProperty(value = "password-file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Password file", description = "A file holding the container password.")
        @Nullable
        String passwordFile,

        /// Whether to read the certificates without verifying that the file is unchanged.
        ///
        /// A truststore password keys a check over the file rather than encrypting it, so it proves
        /// nobody has added an authority since the file was built. An operator with no password for a
        /// truststore writes this, and giving the check up is then a line somebody wrote rather than a
        /// silent default.
        @JsonProperty(value = "skip-integrity-check")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Skip integrity check",
                description = "Read the certificates without verifying the file is unchanged. For a "
                        + "truststore an operator has no password for.",
                defaultValue = "false")
        @Nullable
        Boolean skipIntegrityCheck,

        @JsonProperty(value = "reference-name")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Reference by name",
                description = "Name of a truststore declared in a shared tls-material, written "
                        + "'material-name::entry-name'.")
        @Nullable
        String referenceName,

        @JsonProperty(value = "reference-uuid")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Reference by uuid",
                description = "The uuid of a truststore declared in a shared tls-material.")
        @Nullable
        String referenceUuid,

        /// The entries this element carried that the model does not know, or an empty map when every
        /// entry was recognized. Same contract and rationale as [Tls#unknownSettings()]: the
        /// application-wide mapper drops an unknown property silently, so a misspelled `<buildin>`
        /// would leave the truststore without the authorities it was meant to hold - and a surface
        /// that trusts less than its configuration says is one that stops connecting.
        @JsonIgnore @NotNull Map<String, Object> unknownSettings) {

    public TlsTruststore {
        files = files == null ? null : List.copyOf(files);
        directories = directories == null ? null : List.copyOf(directories);
        patterns = patterns == null ? null : List.copyOf(patterns);
        builtins = builtins == null ? null : List.copyOf(builtins);
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
    public TlsTruststore(
            @JsonAnySetter final @Nullable Map<String, Object> unknownSettings,
            @JsonProperty("format") final @Nullable MaterialFormat format,
            @JsonProperty("file") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY) final @Nullable List<String> files,
            @JsonProperty("directory") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    final @Nullable List<FileLocation> directories,
            @JsonProperty("pattern") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    final @Nullable List<FileLocation> patterns,
            @JsonProperty("builtin") @JsonFormat(with = ACCEPT_SINGLE_VALUE_AS_ARRAY)
                    final @Nullable List<String> builtins,
            @JsonProperty("password") final @Nullable String password,
            @JsonProperty("password-file") final @Nullable String passwordFile,
            @JsonProperty("skip-integrity-check") final @Nullable Boolean skipIntegrityCheck,
            @JsonProperty("reference-name") final @Nullable String referenceName,
            @JsonProperty("reference-uuid") final @Nullable String referenceUuid) {
        this(
                format,
                files,
                directories,
                patterns,
                builtins,
                password,
                passwordFile,
                skipIntegrityCheck,
                referenceName,
                referenceUuid,
                unknownSettings == null ? Map.of() : unknownSettings);
    }

    /// The shape this is built from in code; only deserialization can fill the trap.
    public TlsTruststore(
            final @Nullable MaterialFormat format,
            final @Nullable List<String> files,
            final @Nullable List<FileLocation> directories,
            final @Nullable List<FileLocation> patterns,
            final @Nullable List<String> builtins,
            final @Nullable String password,
            final @Nullable String passwordFile,
            final @Nullable Boolean skipIntegrityCheck,
            final @Nullable String referenceName,
            final @Nullable String referenceUuid) {
        this(
                format,
                files,
                directories,
                patterns,
                builtins,
                password,
                passwordFile,
                skipIntegrityCheck,
                referenceName,
                referenceUuid,
                Map.of());
    }

    /// Whether this names nothing at all; see [TlsFileList#namesNothing()].
    public boolean namesNothing() {
        return (files == null || files.isEmpty())
                && (directories == null || directories.isEmpty())
                && (patterns == null || patterns.isEmpty())
                && (builtins == null || builtins.isEmpty());
    }

    /// Serializes the trapped entries back out verbatim; see [Tls#unknownSettings()].
    @JsonAnyGetter
    @Override
    public @NotNull Map<String, Object> unknownSettings() {
        return unknownSettings;
    }

    /// Whether the integrity check was given up, with the default applied.
    public boolean isSkipIntegrityCheck() {
        return Boolean.TRUE.equals(skipIntegrityCheck);
    }

    @Override
    public @NotNull String toString() {
        return "TlsTruststore{format=" + format + ", files=" + files + ", directories=" + directories + ", patterns="
                + patterns + ", builtins=" + builtins + ", skipIntegrityCheck=" + isSkipIntegrityCheck()
                + ", referenceName=" + referenceName + ", referenceUuid=" + referenceUuid + "}";
    }
}
