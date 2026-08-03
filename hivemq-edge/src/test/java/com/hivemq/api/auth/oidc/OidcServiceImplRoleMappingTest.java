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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OidcServiceImpl#resolveEdgeRoles}: role mapping and its literal matching. Only an
 * explicitly mapped IdP role produces an Edge role; there is no verbatim mode.
 */
class OidcServiceImplRoleMappingTest {

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

    /**
     * {@link OidcServiceImpl#isSupportedRoleClaimShape}: a role claim yields role names only when it is a
     * string or an array of strings. A nested object (e.g. Keycloak's realm_access) is unsupported and, when
     * present, is an unambiguous role-claim-name misconfiguration — distinct from an absent or empty claim.
     */
    @Nested
    class RoleClaimShape {

        @Test
        void aStringIsSupported() {
            assertThat(OidcServiceImpl.isSupportedRoleClaimShape("admin")).isTrue();
        }

        @Test
        void anArrayOfStringsIsSupported() {
            assertThat(OidcServiceImpl.isSupportedRoleClaimShape(List.of("admin", "user")))
                    .isTrue();
        }

        @Test
        void anEmptyArrayIsSupported() {
            // An empty array is a role-less user, not a wrong shape.
            assertThat(OidcServiceImpl.isSupportedRoleClaimShape(List.of())).isTrue();
        }

        @Test
        void aNestedObjectIsNotSupported() {
            // Keycloak's realm_access is {"roles": [...]} -- a container, not the role list itself.
            assertThat(OidcServiceImpl.isSupportedRoleClaimShape(Map.of("roles", List.of("admin"))))
                    .isFalse();
        }

        @Test
        void anArrayContainingANonStringIsNotSupported() {
            assertThat(OidcServiceImpl.isSupportedRoleClaimShape(List.of("admin", 42)))
                    .isFalse();
        }

        @Test
        void aNumberIsNotSupported() {
            assertThat(OidcServiceImpl.isSupportedRoleClaimShape(42)).isFalse();
        }

        @Test
        void nullIsNotSupported() {
            assertThat(OidcServiceImpl.isSupportedRoleClaimShape(null)).isFalse();
        }
    }
}
