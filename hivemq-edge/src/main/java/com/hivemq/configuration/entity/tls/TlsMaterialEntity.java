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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// TLS material declared once for several surfaces to share.
///
/// Each of the four types also appears inside a surface's own `tls` element, where an operator
/// writes it where it is used. This element is where the same material is declared instead for a
/// bridge and an adapter to name, so a plant authority both of them trust is written once.
///
/// Repeats, so several parties can each manage their own material independently - which is why
/// every reference carries the material's name as well as the entry's: two parties may both name
/// an entry `corporate-ca`, and every reference then says which one it means.
@XmlRootElement(name = "tls-material")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class TlsMaterialEntity {

    /// What a reference names this material by, in the first half of `material-name::entry-name`.
    /// May not contain `::`, since that is what separates the two halves.
    @XmlElement(name = "name")
    private @Nullable String name = null;

    @XmlElement(name = "uuid")
    private @Nullable String uuid = null;

    @XmlElement(name = "tls-identity")
    private @NotNull List<TlsIdentityEntity> identities = new ArrayList<>();

    @XmlElement(name = "tls-truststore")
    private @NotNull List<TlsTruststoreEntity> truststores = new ArrayList<>();

    @XmlElement(name = "tls-allowlist")
    private @NotNull List<TlsAllowlistEntity> allowlists = new ArrayList<>();

    @XmlElement(name = "tls-revocationlist")
    private @NotNull List<TlsRevocationlistEntity> revocationlists = new ArrayList<>();

    public @Nullable String getName() {
        return name;
    }

    public @Nullable String getUuid() {
        return uuid;
    }

    public @NotNull List<TlsIdentityEntity> getIdentities() {
        return identities;
    }

    public @NotNull List<TlsTruststoreEntity> getTruststores() {
        return truststores;
    }

    public @NotNull List<TlsAllowlistEntity> getAllowlists() {
        return allowlists;
    }

    public @NotNull List<TlsRevocationlistEntity> getRevocationlists() {
        return revocationlists;
    }

    @Override
    public @NotNull String toString() {
        return "TlsMaterialEntity{name="
                + name
                + ", uuid="
                + uuid
                + ", identities="
                + identities.size()
                + ", truststores="
                + truststores.size()
                + ", allowlists="
                + allowlists.size()
                + ", revocationlists="
                + revocationlists.size()
                + "}";
    }
}
