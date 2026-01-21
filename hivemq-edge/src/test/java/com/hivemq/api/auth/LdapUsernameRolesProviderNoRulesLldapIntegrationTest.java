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
package com.hivemq.api.auth;

import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.auth.provider.impl.ldap.LdapUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.TlsMode;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.SearchScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link LdapUsernameRolesProvider} using LLDAP testcontainer.
 * <p>
 * Tests the high-level API for LDAP authentication that returns usernames and roles.
 * Uses plain LDAP (no TLS) for simplicity in testing.
 */
@Testcontainers
class LdapUsernameRolesProviderNoRulesLldapIntegrationTest {

    @Container
    private static final LldapContainer LLDAP_CONTAINER = new LldapContainer();

    private static LdapUsernameRolesProvider provider;

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped port from the container
        final var host = LLDAP_CONTAINER.getHost();
        final var port = LLDAP_CONTAINER.getLdapPort();

        // Create LdapSimpleBind for LLDAP admin authentication
        // LLDAP admin DN: uid=admin
        final var ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername(),
                        LLDAP_CONTAINER.getAdminPassword());

        // Create connection properties for plain LDAP (no TLS for simplicity)
        // 5 second connect timeout
        // 10 second response timeout
        final var ldapConnectionProperties =
                new LdapConnectionProperties(
                        new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                        TlsMode.NONE,
                        null,
                        5000,  // 5 second connect timeout
                        10000, // 10 second response timeout
                        1,
                        "uid",       // uidAttribute
                        getBaseDn(), // rdns
                        null,
                        SearchScope.SUB,
                        5,
                        ADMIN,  // assignedRole
                        false,
                        ldapSimpleBind,
                        null);

        // Create test user in LLDAP
        new LdapTestConnection(ldapConnectionProperties).createTestUser(
                LLDAP_CONTAINER.getAdminDn(),
                LLDAP_CONTAINER.getAdminPassword(),
                LLDAP_CONTAINER.getBaseDn());

        // Create the LdapUsernameRolesProvider
        provider = new LdapUsernameRolesProvider(ldapConnectionProperties, new SecurityLog());
    }

    public static String getBaseDn() {
        return "ou=people," + LLDAP_CONTAINER.getBaseDn();
    }

    @AfterAll
    static void tearDown() {
        LLDAP_CONTAINER.stop();
    }

    /**
     * Tests successful authentication with correct credentials.
     * <p>
     * Verifies that:
     * - findByUsernameAndPassword returns an Optional with UsernameRoles
     * - The username matches the authenticated user
     * - Roles are assigned (currently hardcoded to ADMIN)
     */
    @Test
    void testSuccessfulAuthentication() {
        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(LdapTestConnection.TEST_USERNAME, LdapTestConnection.TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should succeed with correct credentials")
                .isPresent();

        assertThat(result.get().username())
                .as("Username should match the authenticated user")
                .isEqualTo(LdapTestConnection.TEST_USERNAME);

        assertThat(result.get().roles())
                .as("User should have ADMIN role (hardcoded for now)")
                .contains(ADMIN)
                .hasSize(1);
    }

    /**
     * Tests authentication failure with incorrect password.
     * <p>
     * Verifies that:
     * - findByUsernameAndPassword returns an empty Optional
     * - No exception is thrown (errors are logged internally)
     */
    @Test
    void testFailedAuthenticationWithWrongPassword() {
        // Arrange
        final String wrongPassword = "wrongpassword";

        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(LdapTestConnection.TEST_USERNAME, wrongPassword.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should fail with wrong password")
                .isEmpty();
    }

    /**
     * Tests authentication failure with non-existent user.
     * <p>
     * Verifies that:
     * - findByUsernameAndPassword returns an empty Optional
     * - No exception is thrown (errors are logged internally)
     */
    @Test
    void testFailedAuthenticationWithNonExistentUser() {
        // Arrange
        final String nonExistentUser = "nonexistent";
        final String somePassword = "somepassword";

        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(nonExistentUser, somePassword.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should fail with non-existent user")
                .isEmpty();
    }

    /**
     * Tests that UsernameRoles can be converted to ApiPrincipal.
     * <p>
     * Verifies the toPrincipal() method works correctly.
     */
    @Test
    void testUsernameRolesToPrincipalConversion() {
        // Arrange & Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(LdapTestConnection.TEST_USERNAME, LdapTestConnection.TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        assertThat(result).isPresent();

        // Convert to ApiPrincipal
        final var principal = result.get().toPrincipal();

        // Assert
        assertThat(principal.getName())
                .as("Principal name should match username")
                .isEqualTo(LdapTestConnection.TEST_USERNAME);

        assertThat(principal.getRoles())
                .as("Principal roles should match user roles")
                .contains(ADMIN)
                .hasSize(1);
    }

    /**
     * Tests authentication with empty password.
     * <p>
     * Verifies that empty passwords are rejected.
     */
    @Test
    void testFailedAuthenticationWithEmptyPassword() {
        // Arrange
        final String emptyPassword = "";

        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(LdapTestConnection.TEST_USERNAME, emptyPassword.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should fail with empty password")
                .isEmpty();
    }

    /**
     * Tests authentication with empty username.
     * <p>
     * Verifies that empty usernames are rejected.
     */
    @Test
    void testFailedAuthenticationWithEmptyUsername() {
        // Arrange
        final String emptyUsername = "";

        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(emptyUsername, LdapTestConnection.TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should fail with empty username")
                .isEmpty();
    }
}
