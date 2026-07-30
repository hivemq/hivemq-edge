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
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * XML entity for OpenID Connect (OIDC) authentication configuration.
 * <p>
 * Configures login via an external OIDC Identity Provider (IdP). The IdP details
 * (authorization / token / JWKS endpoints) are resolved at runtime from the
 * {@code issuer-uri}'s {@code /.well-known/openid-configuration} discovery document,
 * so only the issuer, client credentials, and redirect URI are configured here.
 * <p>
 * Roles are read from the ID token's {@code role-claim-name} claim and translated to
 * Edge roles via {@code role-mappings}. When the stanza is present, {@code <enabled>} is
 * required; if the whole stanza is absent, OIDC is off.
 * <p>
 * Example configuration:
 * <pre>{@code
 * <oidc-authentication>
 *     <enabled>true</enabled>
 *     <issuer-uri>https://idp.example.com</issuer-uri>
 *     <client-id>hivemq-edge</client-id>
 *     <client-secret>secret</client-secret>
 *     <redirect-uri>https://edge.example.com/api/v1/auth/oidc/callback</redirect-uri>
 *     <role-claim-name>roles</role-claim-name>
 *     <extra-scopes>
 *         <extra-scope>email</extra-scope>
 *         <extra-scope>profile</extra-scope>
 *     </extra-scopes>
 *     <role-mappings>
 *         <mapping>
 *             <idp-role>hivemq-admin</idp-role>
 *             <edge-role>admin</edge-role>
 *         </mapping>
 *         <mapping>
 *             <idp-role>hivemq-user</idp-role>
 *             <edge-role>user</edge-role>
 *         </mapping>
 *     </role-mappings>
 * </oidc-authentication>
 * }</pre>
 */
@XmlRootElement(name = "oidc-authentication")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class OidcAuthenticationEntity {

    @XmlElement(name = "enabled", required = true)
    private boolean enabled = false;

    @XmlElement(name = "issuer-uri", required = true)
    private @Nullable String issuerUri = null;

    @XmlElement(name = "client-id", required = true)
    private @Nullable String clientId = null;

    @XmlElement(name = "client-secret")
    private @Nullable String clientSecret = null;

    @XmlElement(name = "redirect-uri", required = true)
    private @Nullable String redirectUri = null;

    @XmlElement(name = "role-claim-name", defaultValue = "roles")
    private @NotNull String roleClaimName = "roles";

    @XmlElementWrapper(name = "extra-scopes", required = false)
    @XmlElement(name = "extra-scope")
    private @Nullable List<String> extraScopes = null;

    @XmlElementWrapper(name = "role-mappings", required = false)
    @XmlElement(name = "role-mapping")
    private @Nullable List<OidcRoleMappingEntity> roleMappings = new ArrayList<>();

    @XmlElementWrapper(name = "id-token-signing-algorithms", required = false)
    @XmlElement(name = "id-token-signing-algorithm")
    private @Nullable List<String> idTokenSigningAlgorithms = null;

    @XmlElement(name = "truststore")
    private @Nullable OidcTruststoreEntity truststore = null;

    @XmlElement(name = "connection-timeout-millis", defaultValue = "5000")
    private int connectionTimeoutMillis = 5000;

    public boolean isEnabled() {
        return enabled;
    }

    public @Nullable String getIssuerUri() {
        return issuerUri;
    }

    public @Nullable String getClientId() {
        return clientId;
    }

    public @Nullable String getClientSecret() {
        return clientSecret;
    }

    public @Nullable String getRedirectUri() {
        return redirectUri;
    }

    public @NotNull String getRoleClaimName() {
        return roleClaimName;
    }

    public @Nullable List<String> getExtraScopes() {
        return extraScopes;
    }

    public @Nullable List<OidcRoleMappingEntity> getRoleMappings() {
        return roleMappings;
    }

    public @Nullable List<String> getIdTokenSigningAlgorithms() {
        return idTokenSigningAlgorithms;
    }

    public @Nullable OidcTruststoreEntity getTruststore() {
        return truststore;
    }

    public int getConnectionTimeoutMillis() {
        return connectionTimeoutMillis;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof OidcAuthenticationEntity that)) {
            return false;
        }
        return enabled == that.enabled
                && connectionTimeoutMillis == that.connectionTimeoutMillis
                && Objects.equals(getIssuerUri(), that.getIssuerUri())
                && Objects.equals(getClientId(), that.getClientId())
                && Objects.equals(getClientSecret(), that.getClientSecret())
                && Objects.equals(getRedirectUri(), that.getRedirectUri())
                && Objects.equals(getRoleClaimName(), that.getRoleClaimName())
                && Objects.equals(getExtraScopes(), that.getExtraScopes())
                && Objects.equals(getRoleMappings(), that.getRoleMappings())
                && Objects.equals(getIdTokenSigningAlgorithms(), that.getIdTokenSigningAlgorithms())
                && Objects.equals(getTruststore(), that.getTruststore());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                connectionTimeoutMillis,
                getIssuerUri(),
                getClientId(),
                getClientSecret(),
                getRedirectUri(),
                getRoleClaimName(),
                getExtraScopes(),
                getRoleMappings(),
                getIdTokenSigningAlgorithms(),
                getTruststore());
    }
}
