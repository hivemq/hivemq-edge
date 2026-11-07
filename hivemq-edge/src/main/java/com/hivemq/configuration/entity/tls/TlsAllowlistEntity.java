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

/// The individual certificates Edge accepts, by SHA-256 fingerprint.
///
/// For a deployment with no certificate authority. An operator writes the fingerprints down, one
/// per line, so a peer present at first connect cannot enrol itself.
///
/// No format element: the file is one hexadecimal fingerprint per line, with `#` comments, and
/// there is nothing else it could be.
@XmlRootElement(name = "tls-allowlist")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TlsAllowlistEntity {

    @XmlElement(name = "name")
    private @Nullable String name = null;

    @XmlElement(name = "uuid")
    private @Nullable String uuid = null;

    @XmlElement(name = "reference-name")
    private @Nullable String referenceName = null;

    @XmlElement(name = "reference-uuid")
    private @Nullable String referenceUuid = null;

    @XmlElement(name = "file")
    private @NotNull List<String> files = new ArrayList<>();

    @XmlElement(name = "directory")
    private @NotNull List<DirectoryEntity> directories = new ArrayList<>();

    @XmlElement(name = "pattern")
    private @NotNull List<PatternEntity> patterns = new ArrayList<>();

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

    public @NotNull List<String> getFiles() {
        return files;
    }

    public @NotNull List<DirectoryEntity> getDirectories() {
        return directories;
    }

    public @NotNull List<PatternEntity> getPatterns() {
        return patterns;
    }

    /// Value equality over every element, because a surface holding one of these compares its
    /// configuration against the previous one to decide what a reload changed. Identity comparison
    /// would make every reload look like a change to every bridge.
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TlsAllowlistEntity that)) {
            return false;
        }
        return Objects.equals(name, that.name)
                && Objects.equals(uuid, that.uuid)
                && Objects.equals(referenceName, that.referenceName)
                && Objects.equals(referenceUuid, that.referenceUuid)
                && Objects.equals(files, that.files)
                && Objects.equals(directories, that.directories)
                && Objects.equals(patterns, that.patterns);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, uuid, referenceName, referenceUuid, files, directories, patterns);
    }

    @Override
    public @NotNull String toString() {
        return "TlsAllowlistEntity{name=" + name + ", uuid=" + uuid + ", referenceName=" + referenceName + "}";
    }
}
