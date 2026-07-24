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
package com.hivemq.api.auth.oidc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcServiceImpl#resolveEdgeRoles}: the two role-mapping modes and their literal
 * matching.
 */
class OidcServiceImplRoleMappingTest {

    // -- Verbatim mode (no mappings): claim values are Edge roles directly.

    @Test
    void verbatim_keepsEdgeRolesFromTheClaim() {
        assertThat(OidcServiceImpl.resolveEdgeRoles(List.of("admin", "user"), null))
                .containsExactlyInAnyOrder("admin", "user");
    }

    @Test
    void verbatim_isCaseInsensitiveForTheKnownEdgeRoles() {
        // The three Edge role names are a fixed set and authorization compares them case-insensitively.
        assertThat(OidcServiceImpl.resolveEdgeRoles(List.of("ADMIN", "Super"), null))
                .containsExactlyInAnyOrder("ADMIN", "Super");
    }

    @Test
    void verbatim_dropsClaimValuesThatAreNotEdgeRoles() {
        assertThat(OidcServiceImpl.resolveEdgeRoles(List.of("admin", "some-idp-group"), null))
                .containsExactly("admin");
    }

    @Test
    void verbatim_dropsAnEdgeRoleWithSurroundingWhitespace() {
        // Matching is literal: " admin " is not "admin". It is dropped (with a warning), not trimmed.
        assertThat(OidcServiceImpl.resolveEdgeRoles(List.of(" admin "), null)).isEmpty();
    }

    // -- Strict mode (mappings present): only mapped IdP roles produce an Edge role.

    @Test
    void strict_mapsAConfiguredIdpRole() {
        assertThat(OidcServiceImpl.resolveEdgeRoles(List.of("hivemq-admins"), Map.of("hivemq-admins", "admin")))
                .containsExactly("admin");
    }

    @Test
    void strict_dropsAnUnmappedIdpRole() {
        // An unrelated IdP role must never grant access, even one literally named "admin".
        assertThat(OidcServiceImpl.resolveEdgeRoles(List.of("admin"), Map.of("hivemq-admins", "admin")))
                .isEmpty();
    }

    @Test
    void strict_matchesTheKeyLiterally() {
        // Case differs: no match, so the role is dropped (a warning is logged for the near miss).
        assertThat(OidcServiceImpl.resolveEdgeRoles(List.of("HiveMQ-Admins"), Map.of("hivemq-admins", "admin")))
                .isEmpty();
    }
}
