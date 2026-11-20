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
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The private key and certificate chain Edge presents to a peer.
///
/// Appears in two places: inside a `tls-material` element, where it carries a name and a uuid so a
/// surface can refer to it, and inside a surface's own `tls` element, where it either carries a
/// reference to one declared elsewhere or its own fields directly.
///
/// Every element is optional here, and deliberately so. One schema describes this element wherever
/// it appears, and what is required differs between the two places, so the schema cannot express
/// it and the resolving stage enforces it instead - naming the element and the place in its
/// refusal.
@XmlRootElement(name = "tls-identity")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TlsIdentityEntity {

    @XmlElement(name = "name")
    private @Nullable String name = null;

    @XmlElement(name = "uuid")
    private @Nullable String uuid = null;

    /// A reference to an identity declared in a `tls-material`, written `material-name::entry-name`.
    @XmlElement(name = "reference-name")
    private @Nullable String referenceName = null;

    @XmlElement(name = "reference-uuid")
    private @Nullable String referenceUuid = null;

    /// `p12`, `jks` or `pem`. Declared rather than guessed from an extension: a `.crt` file holds
    /// PEM or DER, a `.pem` file holds a key or a certificate or both, and a file Kubernetes mounts
    /// is named after the key in the secret.
    @XmlElement(name = "format")
    private @Nullable String format = null;

    /// The container, with `p12` or `jks`.
    @XmlElement(name = "file")
    private @Nullable String file = null;

    /// The private key, with `pem`.
    @XmlElement(name = "key-file")
    private @Nullable String keyFile = null;

    /// The certificate, with `pem`.
    @XmlElement(name = "certificate-file")
    private @Nullable String certificateFile = null;

    /// The intermediate certificates, with `pem`. Optional even there: a self-signed identity has
    /// none, and some exporters append them to the certificate file instead.
    @XmlElement(name = "chain-file")
    private @Nullable String chainFile = null;

    /// The container password, which opens a `p12` or `jks` file.
    @XmlElement(name = "password")
    private @Nullable String password = null;

    /// A file holding the container password, for a deployment that projects secrets as files.
    @XmlElement(name = "password-file")
    private @Nullable String passwordFile = null;

    /// Which key entry inside a container to present, read from the *Alias name* heading that
    /// `keytool -list` prints. Required where a container holds more than one key.
    @XmlElement(name = "key-entry")
    private @Nullable String keyEntry = null;

    /// The password unlocking one key entry inside a container.
    @XmlElement(name = "keyentry-password")
    private @Nullable String keyEntryPassword = null;

    @XmlElement(name = "keyentry-password-file")
    private @Nullable String keyEntryPasswordFile = null;

    /// The password decrypting a PEM key file.
    @XmlElement(name = "key-password")
    private @Nullable String keyPassword = null;

    @XmlElement(name = "key-password-file")
    private @Nullable String keyPasswordFile = null;

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

    public @Nullable String getFile() {
        return file;
    }

    public @Nullable String getKeyFile() {
        return keyFile;
    }

    public @Nullable String getCertificateFile() {
        return certificateFile;
    }

    public @Nullable String getChainFile() {
        return chainFile;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public @Nullable String getPasswordFile() {
        return passwordFile;
    }

    public @Nullable String getKeyEntry() {
        return keyEntry;
    }

    public @Nullable String getKeyEntryPassword() {
        return keyEntryPassword;
    }

    public @Nullable String getKeyEntryPasswordFile() {
        return keyEntryPasswordFile;
    }

    public @Nullable String getKeyPassword() {
        return keyPassword;
    }

    public @Nullable String getKeyPasswordFile() {
        return keyPasswordFile;
    }

    /// Value equality over every element; see [TlsAllowlistEntity#equals] for why.
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TlsIdentityEntity that)) {
            return false;
        }
        return Objects.equals(name, that.name)
                && Objects.equals(uuid, that.uuid)
                && Objects.equals(referenceName, that.referenceName)
                && Objects.equals(referenceUuid, that.referenceUuid)
                && Objects.equals(format, that.format)
                && Objects.equals(file, that.file)
                && Objects.equals(keyFile, that.keyFile)
                && Objects.equals(certificateFile, that.certificateFile)
                && Objects.equals(chainFile, that.chainFile)
                && Objects.equals(password, that.password)
                && Objects.equals(passwordFile, that.passwordFile)
                && Objects.equals(keyEntry, that.keyEntry)
                && Objects.equals(keyEntryPassword, that.keyEntryPassword)
                && Objects.equals(keyEntryPasswordFile, that.keyEntryPasswordFile)
                && Objects.equals(keyPassword, that.keyPassword)
                && Objects.equals(keyPasswordFile, that.keyPasswordFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                name,
                uuid,
                referenceName,
                referenceUuid,
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
                keyPasswordFile);
    }

    @Override
    public @NotNull String toString() {
        // No password or password file: a logged configuration must not carry a credential.
        return "TlsIdentityEntity{name=" + name + ", uuid=" + uuid + ", referenceName=" + referenceName + "}";
    }
}
