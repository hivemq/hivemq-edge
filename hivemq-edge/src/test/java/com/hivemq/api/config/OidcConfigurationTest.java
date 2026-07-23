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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.configuration.entity.api.oidc.OidcAuthenticationEntity;
import com.hivemq.configuration.entity.api.oidc.OidcRoleMappingEntity;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcConfiguration#fromEntity}.
 */
class OidcConfigurationTest {

    @Test
    void fromEntity_mapsAllFields() {
        final OidcAuthenticationEntity entity = entity(
                "https://idp.example.com",
                "hivemq-edge",
                "the-secret",
                "https://edge.example.com/api/v1/auth/oidc/callback",
                "roles",
                "email profile",
                new OidcRoleMappingEntity("hivemq-admin", "admin"));

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getIssuerUri()).isEqualTo(URI.create("https://idp.example.com"));
        assertThat(config.getClientId()).isEqualTo("hivemq-edge");
        assertThat(config.getClientSecret()).isEqualTo("the-secret");
        assertThat(config.getRedirectUri()).isEqualTo(URI.create("https://edge.example.com/api/v1/auth/oidc/callback"));
        assertThat(config.getRoleClaimName()).isEqualTo("roles");
    }

    @Test
    void fromEntity_parsesWhitespaceSeparatedScopes() {
        final OidcConfiguration config = OidcConfiguration.fromEntity(
                entity("https://idp", "client", null, "https://edge/cb", "roles", "  email   profile  "));

        assertThat(config.getExtraScopes()).containsExactly("email", "profile");
    }

    @Test
    void fromEntity_blankScopes_yieldsEmptyList() {
        final OidcConfiguration config =
                OidcConfiguration.fromEntity(entity("https://idp", "client", null, "https://edge/cb", "roles", "   "));

        assertThat(config.getExtraScopes()).isEmpty();
    }

    @Test
    void fromEntity_roleMappingKeysAreLowerCasedForCaseInsensitiveLookup() {
        final OidcAuthenticationEntity entity = entity(
                "https://idp",
                "client",
                null,
                "https://edge/cb",
                "roles",
                null,
                new OidcRoleMappingEntity("HiveMQ-Admin", "admin"),
                new OidcRoleMappingEntity("HIVEMQ-USER", "user"));

        final OidcConfiguration config = OidcConfiguration.fromEntity(entity);

        assertThat(config.getRoleMappings())
                .containsEntry("hivemq-admin", "admin")
                .containsEntry("hivemq-user", "user");
    }

    @Test
    void fromEntity_missingIssuer_throws() {
        assertThatThrownBy(() ->
                        OidcConfiguration.fromEntity(entity(null, "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer-uri");
    }

    @Test
    void fromEntity_missingClientId_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "  ", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client-id");
    }

    @Test
    void fromEntity_missingRedirect_throws() {
        assertThatThrownBy(
                        () -> OidcConfiguration.fromEntity(entity("https://idp", "client", null, null, "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("redirect-uri");
    }

    @Test
    void fromEntity_relativeIssuerUri_throws() {
        // URI.create accepts a relative reference; it must not survive into the runtime config, where it
        // would produce a null://null postMessage origin.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("idp.example.com/auth", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute");
    }

    @Test
    void fromEntity_relativeRedirectUri_throws() {
        assertThatThrownBy(() ->
                        OidcConfiguration.fromEntity(entity("https://idp", "client", null, "callback", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("absolute");
    }

    @Test
    void fromEntity_nonHttpScheme_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("ftp://idp.example.com", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http or https");
    }

    @Test
    void fromEntity_uriWithoutHost_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https:///path", "client", null, "https://edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");
    }

    @Test
    void fromEntity_redirectWithUserInfo_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "client", null, "https://user:pw@edge/cb", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user information");
    }

    @Test
    void fromEntity_redirectWithFragment_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "client", null, "https://edge/cb#frag", "roles", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fragment");
    }

    @Test
    void fromEntity_plainHttpIsAccepted() {
        // http is legitimate for local QA and for TLS terminated at a reverse proxy: warn, do not reject.
        final OidcConfiguration config = OidcConfiguration.fromEntity(
                entity("http://localhost:8080/realms/edge", "client", null, "http://localhost:8080/cb", "roles", null));

        assertThat(config.getIssuerUri()).isEqualTo(URI.create("http://localhost:8080/realms/edge"));
    }

    @Test
    void fromEntity_blankRoleClaimName_throws() {
        // A blank claim name extracts no roles at all, denying every user for a non-obvious reason.
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(
                        entity("https://idp", "client", null, "https://edge/cb", "  ", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role-claim-name");
    }

    @Test
    void fromEntity_invalidEdgeRole_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity(
                        "https://idp",
                        "client",
                        null,
                        "https://edge/cb",
                        "roles",
                        null,
                        new OidcRoleMappingEntity("idp-role", "wizard"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("edge-role");
    }

    @Test
    void fromEntity_acceptsAllValidEdgeRolesCaseInsensitively() {
        final OidcConfiguration config = OidcConfiguration.fromEntity(entity(
                "https://idp",
                "client",
                null,
                "https://edge/cb",
                "roles",
                null,
                new OidcRoleMappingEntity("a", "ADMIN"),
                new OidcRoleMappingEntity("b", "Super"),
                new OidcRoleMappingEntity("c", "user")));

        assertThat(config.getRoleMappings()).containsValues("ADMIN", "Super", "user");
    }

    @Test
    void fromEntity_duplicateIdpRole_throws() {
        assertThatThrownBy(() -> OidcConfiguration.fromEntity(entity(
                        "https://idp",
                        "client",
                        null,
                        "https://edge/cb",
                        "roles",
                        null,
                        new OidcRoleMappingEntity("Team-Admins", "admin"),
                        // same IdP role in a different case → duplicate after normalization
                        new OidcRoleMappingEntity("team-admins", "user"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    // -- helpers: OidcAuthenticationEntity fields are populated by JAXB, so set them reflectively for tests.

    private static @org.jetbrains.annotations.NotNull OidcAuthenticationEntity entity(
            final String issuer,
            final String clientId,
            final String clientSecret,
            final String redirect,
            final String roleClaim,
            final String extraScopes,
            final OidcRoleMappingEntity... mappings) {
        final OidcAuthenticationEntity entity = new OidcAuthenticationEntity();
        set(entity, "enabled", true);
        set(entity, "issuerUri", issuer);
        set(entity, "clientId", clientId);
        set(entity, "clientSecret", clientSecret);
        set(entity, "redirectUri", redirect);
        set(entity, "roleClaimName", roleClaim);
        set(entity, "extraScopes", extraScopes);
        set(entity, "roleMappings", List.of(mappings));
        return entity;
    }

    private static void set(final Object target, final String fieldName, final Object value) {
        try {
            final Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException("could not set test field " + fieldName, e);
        }
    }
}
