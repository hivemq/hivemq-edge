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
package com.hivemq.api.config;

import com.google.common.base.Preconditions;
import com.hivemq.api.auth.ApiRoles;
import com.hivemq.configuration.entity.api.oidc.OidcAuthenticationEntity;
import com.hivemq.configuration.entity.api.oidc.OidcRoleMappingEntity;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime configuration for OpenID Connect (OIDC) authentication.
 * <p>
 * Immutable, derived from an {@link OidcAuthenticationEntity} via {@link #fromEntity}.
 * The IdP endpoints (authorization / token / JWKS) are not held here — they are resolved
 * at runtime from {@code issuerUri}'s discovery document.
 * <p>
 * {@code roleMappings} maps a lower-cased IdP role name onto an Edge role string. Lookups
 * should lower-case the incoming IdP role first (role matching is case-insensitive).
 */
public class OidcConfiguration {

    private final @NotNull URI issuerUri;
    private final @NotNull String clientId;
    private final @Nullable String clientSecret;
    private final @NotNull URI redirectUri;
    private final @NotNull String roleClaimName;
    private final @NotNull List<String> extraScopes;
    private final @NotNull Map<String, String> roleMappings;

    public OidcConfiguration(
            final @NotNull URI issuerUri,
            final @NotNull String clientId,
            final @Nullable String clientSecret,
            final @NotNull URI redirectUri,
            final @NotNull String roleClaimName,
            final @NotNull List<String> extraScopes,
            final @NotNull Map<String, String> roleMappings) {
        this.issuerUri = Preconditions.checkNotNull(issuerUri);
        this.clientId = Preconditions.checkNotNull(clientId);
        this.clientSecret = clientSecret;
        this.redirectUri = Preconditions.checkNotNull(redirectUri);
        this.roleClaimName = Preconditions.checkNotNull(roleClaimName);
        this.extraScopes = List.copyOf(extraScopes);
        this.roleMappings = Map.copyOf(roleMappings);
    }

    /**
     * Builds an {@link OidcConfiguration} from its XML entity.
     * <p>
     * Precondition: the entity is enabled and its required fields ({@code issuer-uri},
     * {@code client-id}, {@code redirect-uri}) are populated — the caller
     * ({@code ApiConfigurator}) is responsible for validating and skipping a disabled or
     * incomplete entity.
     *
     * @throws IllegalArgumentException if a required URI field is missing or malformed
     */
    public static @NotNull OidcConfiguration fromEntity(final @NotNull OidcAuthenticationEntity entity) {
        final String issuer = entity.getIssuerUri();
        final String clientId = entity.getClientId();
        final String redirect = entity.getRedirectUri();
        Preconditions.checkArgument(issuer != null && !issuer.isBlank(), "OIDC issuer-uri must be configured");
        Preconditions.checkArgument(clientId != null && !clientId.isBlank(), "OIDC client-id must be configured");
        Preconditions.checkArgument(redirect != null && !redirect.isBlank(), "OIDC redirect-uri must be configured");

        final List<String> scopes = parseScopes(entity.getExtraScopes());

        // Keys are lower-cased for case-insensitive lookup. Duplicate IdP roles and unknown Edge roles are
        // rejected rather than silently accepted, so authorization is never opened up by a config mistake.
        final Map<String, String> mappings = new LinkedHashMap<>();
        final List<OidcRoleMappingEntity> mappingEntities = entity.getRoleMappings();
        if (mappingEntities != null) {
            for (final OidcRoleMappingEntity mapping : mappingEntities) {
                final String idpRole = mapping.getIdpRole();
                final String edgeRole = mapping.getEdgeRole();
                Preconditions.checkArgument(
                        idpRole != null && !idpRole.isBlank(), "OIDC role mapping is missing an <idp-role>");
                Preconditions.checkArgument(
                        edgeRole != null && !edgeRole.isBlank(), "OIDC role mapping is missing an <edge-role>");
                Preconditions.checkArgument(
                        isValidEdgeRole(edgeRole),
                        "OIDC role mapping has an invalid <edge-role> '%s'; must be one of admin, super, user",
                        edgeRole);
                final String key = idpRole.toLowerCase(Locale.ROOT);
                Preconditions.checkArgument(
                        !mappings.containsKey(key), "OIDC role mapping has a duplicate <idp-role> '%s'", idpRole);
                mappings.put(key, edgeRole);
            }
        }

        return new OidcConfiguration(
                URI.create(issuer.trim()),
                clientId.trim(),
                entity.getClientSecret(),
                URI.create(redirect.trim()),
                entity.getRoleClaimName(),
                scopes,
                mappings);
    }

    private static boolean isValidEdgeRole(final @NotNull String edgeRole) {
        return edgeRole.equalsIgnoreCase(ApiRoles.ADMIN)
                || edgeRole.equalsIgnoreCase(ApiRoles.SUPER)
                || edgeRole.equalsIgnoreCase(ApiRoles.USER);
    }

    private static @NotNull List<String> parseScopes(final @Nullable String extraScopes) {
        if (extraScopes == null || extraScopes.isBlank()) {
            return List.of();
        }
        return List.of(extraScopes.trim().split("\\s+"));
    }

    public @NotNull URI getIssuerUri() {
        return issuerUri;
    }

    public @NotNull String getClientId() {
        return clientId;
    }

    public @Nullable String getClientSecret() {
        return clientSecret;
    }

    public @NotNull URI getRedirectUri() {
        return redirectUri;
    }

    public @NotNull String getRoleClaimName() {
        return roleClaimName;
    }

    public @NotNull List<String> getExtraScopes() {
        return extraScopes;
    }

    public @NotNull Map<String, String> getRoleMappings() {
        return roleMappings;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OidcConfiguration that)) {
            return false;
        }
        return issuerUri.equals(that.issuerUri)
                && clientId.equals(that.clientId)
                && Objects.equals(clientSecret, that.clientSecret)
                && redirectUri.equals(that.redirectUri)
                && roleClaimName.equals(that.roleClaimName)
                && extraScopes.equals(that.extraScopes)
                && roleMappings.equals(that.roleMappings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issuerUri, clientId, clientSecret, redirectUri, roleClaimName, extraScopes, roleMappings);
    }
}
