/*
 * Copyright 2019-present HiveMQ GmbH
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
package com.hivemq.configuration.entity.tls;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The authorities whose certificates Edge accepts.
///
/// Resolves to a set, so files, directories, glob patterns and the runtime's own truststore all
/// repeat and merge - a surface trusting the public authorities and a plant authority together
/// names both and gets the union.
@XmlRootElement(name = "tls-truststore")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TlsTruststoreEntity {

    @XmlElement(name = "name")
    private @Nullable String name = null;

    @XmlElement(name = "uuid")
    private @Nullable String uuid = null;

    @XmlElement(name = "reference-name")
    private @Nullable String referenceName = null;

    @XmlElement(name = "reference-uuid")
    private @Nullable String referenceUuid = null;

    @XmlElement(name = "format")
    private @Nullable String format = null;

    @XmlElement(name = "file")
    private @NotNull List<String> files = new ArrayList<>();

    @XmlElement(name = "directory")
    private @NotNull List<DirectoryEntity> directories = new ArrayList<>();

    @XmlElement(name = "pattern")
    private @NotNull List<PatternEntity> patterns = new ArrayList<>();

    /// A truststore the runtime carries rather than the operator supplies. `JVM-cacerts` is the
    /// only name, and it is the public certificate authorities the JVM ships with.
    @XmlElement(name = "builtin")
    private @NotNull List<String> builtins = new ArrayList<>();

    @XmlElement(name = "password")
    private @Nullable String password = null;

    @XmlElement(name = "password-file")
    private @Nullable String passwordFile = null;

    /// Whether to read the certificates without verifying that the file is unchanged.
    ///
    /// A truststore password keys a check over the file rather than encrypting it, so it proves
    /// nobody has added an authority since the file was built. An operator with no password for a
    /// truststore writes this, and giving the check up is then a line somebody wrote rather than a
    /// silent default.
    @XmlElement(name = "skip-integrity-check", defaultValue = "false")
    private boolean skipIntegrityCheck = false;

    public @Nullable String getName() {
        return name;
    }

    public @Nullable String getUuid() {
        return uuid;
    }

    public @Nullable String getReferenceName() {
        return referenceName;
    }

    public @Nullable String getReferenceUuid() {
        return referenceUuid;
    }

    public @Nullable String getFormat() {
        return format;
    }

    public @NotNull List<String> getFiles() {
        return files;
    }

    public @NotNull List<DirectoryEntity> getDirectories() {
        return directories;
    }

    public @NotNull List<PatternEntity> getPatterns() {
        return patterns;
    }

    public @NotNull List<String> getBuiltins() {
        return builtins;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public @Nullable String getPasswordFile() {
        return passwordFile;
    }

    public boolean isSkipIntegrityCheck() {
        return skipIntegrityCheck;
    }

    /// Value equality over every element; see [TlsAllowlistEntity#equals] for why.
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TlsTruststoreEntity that)) {
            return false;
        }
        return skipIntegrityCheck == that.skipIntegrityCheck
                && Objects.equals(name, that.name)
                && Objects.equals(uuid, that.uuid)
                && Objects.equals(referenceName, that.referenceName)
                && Objects.equals(referenceUuid, that.referenceUuid)
                && Objects.equals(format, that.format)
                && Objects.equals(files, that.files)
                && Objects.equals(directories, that.directories)
                && Objects.equals(patterns, that.patterns)
                && Objects.equals(builtins, that.builtins)
                && Objects.equals(password, that.password)
                && Objects.equals(passwordFile, that.passwordFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                uuid,
                referenceName,
                referenceUuid,
                format,
                files,
                directories,
                patterns,
                builtins,
                password,
                passwordFile,
                skipIntegrityCheck);
    }

    @Override
    public @NotNull String toString() {
        return "TlsTruststoreEntity{name=" + name + ", uuid=" + uuid + ", referenceName=" + referenceName + "}";
    }
}
