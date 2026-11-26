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

/// The private key and certificate chain Edge presents to the OPC UA server.
///
/// Every element is optional here, and deliberately so: which are required depends on the format,
/// and on whether the material is written here or referenced from a shared `tls-material`. A schema
/// cannot express that, so the effective-configuration stage enforces it and names the element and
/// the reason in its refusal.
///
/// The superseded spelling of this element is `keystore`, which holds a single container and the two
/// passwords that open it. That one keeps exactly the reach it always had; everything new - PEM, a
/// password read from a file, a reference to shared material - is written here.
public record TlsIdentity(
        @JsonProperty(value = "format")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Format",
                description = "p12, jks or pem. Declared rather than guessed from the file name.")
        @Nullable
        MaterialFormat format,

        /// The container, with `p12` or `jks`.
        @JsonProperty(value = "file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "File",
                description = "The container holding the key and its certificate chain. For p12 or jks.")
        @Nullable
        String file,

        /// The private key, with `pem`.
        @JsonProperty(value = "key-file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Key file", description = "The private key. For pem.")
        @Nullable
        String keyFile,

        /// The certificate, with `pem`.
        @JsonProperty(value = "certificate-file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Certificate file",
                description = "The certificate belonging to the key. For pem, where it is required.")
        @Nullable
        String certificateFile,

        /// The intermediates, with `pem`. Optional even there: a self-signed identity has none, and
        /// some exporters append them to the certificate file instead.
        @JsonProperty(value = "chain-file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Chain file",
                description = "The intermediate certificates vouching for the certificate. For pem, "
                        + "where it is optional.")
        @Nullable
        String chainFile,

        @JsonProperty(value = "password")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Password", description = "The password that opens a p12 or jks container.")
        @Nullable
        String password,

        @JsonProperty(value = "password-file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Password file",
                description = "A file holding the container password, for a deployment that projects "
                        + "secrets as files rather than writing them in the configuration.")
        @Nullable
        String passwordFile,

        /// Which key entry inside a container to present, read from the *Alias name* heading that
        /// `keytool -list` prints. Required where a container holds more than one key.
        @JsonProperty(value = "key-entry")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Key entry",
                description = "Which key inside the container to present, by the name keytool -list "
                        + "prints under 'Alias name'. Required where the container holds several keys.")
        @Nullable
        String keyEntry,

        @JsonProperty(value = "keyentry-password")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Key entry password",
                description = "The password unlocking one key entry inside a p12 or jks container.")
        @Nullable
        String keyEntryPassword,

        @JsonProperty(value = "keyentry-password-file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Key entry password file", description = "A file holding the key entry password.")
        @Nullable
        String keyEntryPasswordFile,

        @JsonProperty(value = "key-password")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Key password", description = "The password decrypting a PEM key file.")
        @Nullable
        String keyPassword,

        @JsonProperty(value = "key-password-file")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(title = "Key password file", description = "A file holding the PEM key password.")
        @Nullable
        String keyPasswordFile,

        @JsonProperty(value = "reference-name")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Reference by name",
                description = "Name of an identity declared in a shared tls-material, written "
                        + "'material-name::entry-name'. An alternative to naming the files here.")
        @Nullable
        String referenceName,

        @JsonProperty(value = "reference-uuid")
        @JsonInclude(NON_NULL)
        @ModuleConfigField(
                title = "Reference by uuid",
                description = "The uuid of an identity declared in a shared tls-material.")
        @Nullable
        String referenceUuid,

        /// The entries this element carried that the model does not know, or an empty map when every
        /// entry was recognized. Same contract and rationale as [Tls#unknownSettings()]: the
        /// application-wide mapper drops an unknown property silently, so a misspelled `<key-fil>`
        /// would leave Edge with no client certificate and nothing but a log line to say so.
        @JsonIgnore @NotNull Map<String, Object> unknownSettings) {

    public TlsIdentity {
        unknownSettings = unknownSettings == null || unknownSettings.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(unknownSettings));
    }

    /// What a configuration file binds to; see [Tls] for why the trap comes first.
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @SuppressWarnings("checkstyle:ParameterNumber")
    public TlsIdentity(
            @JsonAnySetter final @Nullable Map<String, Object> unknownSettings,
            @JsonProperty("format") final @Nullable MaterialFormat format,
            @JsonProperty("file") final @Nullable String file,
            @JsonProperty("key-file") final @Nullable String keyFile,
            @JsonProperty("certificate-file") final @Nullable String certificateFile,
            @JsonProperty("chain-file") final @Nullable String chainFile,
            @JsonProperty("password") final @Nullable String password,
            @JsonProperty("password-file") final @Nullable String passwordFile,
            @JsonProperty("key-entry") final @Nullable String keyEntry,
            @JsonProperty("keyentry-password") final @Nullable String keyEntryPassword,
            @JsonProperty("keyentry-password-file") final @Nullable String keyEntryPasswordFile,
            @JsonProperty("key-password") final @Nullable String keyPassword,
            @JsonProperty("key-password-file") final @Nullable String keyPasswordFile,
            @JsonProperty("reference-name") final @Nullable String referenceName,
            @JsonProperty("reference-uuid") final @Nullable String referenceUuid) {
        this(
                format,
                file,
                keyFile,
                certificateFile,
                chainFile,
                password,
                passwordFile,
                keyEntry,
                keyEntryPassword,
                keyEntryPasswordFile,
                keyPassword,
                keyPasswordFile,
                referenceName,
                referenceUuid,
                unknownSettings == null ? Map.of() : unknownSettings);
    }

    /// A container identity, which is what most configurations name.
    public TlsIdentity(
            final @Nullable MaterialFormat format,
            final @Nullable String file,
            final @Nullable String password,
            final @Nullable String keyEntry,
            final @Nullable String keyEntryPassword) {
        this(
                format,
                file,
                null,
                null,
                null,
                password,
                null,
                keyEntry,
                keyEntryPassword,
                null,
                null,
                null,
                null,
                null,
                Map.of());
    }

    /// A reference to an identity declared in a shared `tls-material`.
    public static @NotNull TlsIdentity referencing(
            final @Nullable String referenceName, final @Nullable String referenceUuid) {
        return new TlsIdentity(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                referenceName,
                referenceUuid,
                Map.of());
    }

    /// Serializes the trapped entries back out verbatim; see [Tls#unknownSettings()].
    @JsonAnyGetter
    @Override
    public @NotNull Map<String, Object> unknownSettings() {
        return unknownSettings;
    }

    @Override
    public @NotNull String toString() {
        // No password or password file: a logged configuration must not carry a credential.
        return "TlsIdentity{format=" + format + ", file=" + file + ", keyFile=" + keyFile + ", certificateFile="
                + certificateFile + ", chainFile=" + chainFile + ", keyEntry=" + keyEntry + ", referenceName="
                + referenceName + ", referenceUuid=" + referenceUuid + "}";
    }
}
