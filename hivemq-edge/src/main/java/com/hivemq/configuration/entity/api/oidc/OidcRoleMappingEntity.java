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
package com.hivemq.configuration.entity.api.oidc;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * XML entity for a single OIDC role mapping.
 * <p>
 * Maps a role name as emitted by the external Identity Provider ({@code idp-role})
 * onto one of the HiveMQ Edge roles ({@code edge-role}: {@code admin}, {@code super},
 * {@code user}). Without configurable mappings the operator would be forced to make the
 * IdP emit the exact Edge role strings, which is impractical.
 * <p>
 * Example configuration:
 * <pre>{@code
 * <mapping>
 *     <idp-role>hivemq-admin</idp-role>
 *     <edge-role>admin</edge-role>
 * </mapping>
 * }</pre>
 */
@XmlRootElement(name = "mapping")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class OidcRoleMappingEntity {

    @XmlElement(name = "idp-role", required = true)
    private @Nullable String idpRole = null;

    @XmlElement(name = "edge-role", required = true)
    private @Nullable String edgeRole = null;

    public OidcRoleMappingEntity() {}

    public OidcRoleMappingEntity(final @NotNull String idpRole, final @NotNull String edgeRole) {
        this.idpRole = idpRole;
        this.edgeRole = edgeRole;
    }

    public @Nullable String getIdpRole() {
        return idpRole;
    }

    public @Nullable String getEdgeRole() {
        return edgeRole;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof OidcRoleMappingEntity that)) {
            return false;
        }
        return Objects.equals(getIdpRole(), that.getIdpRole()) && Objects.equals(getEdgeRole(), that.getEdgeRole());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getIdpRole(), getEdgeRole());
    }
}
