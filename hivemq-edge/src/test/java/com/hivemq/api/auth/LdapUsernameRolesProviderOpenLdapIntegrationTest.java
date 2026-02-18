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

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static com.hivemq.api.auth.ApiRoles.SUPER;
import static com.hivemq.api.auth.ApiRoles.USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.auth.provider.impl.ldap.LdapUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.TlsMode;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.OpenLdapContainer;
import com.hivemq.configuration.entity.api.ldap.UserRoleEntity;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.SearchScope;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for {@link LdapUsernameRolesProvider} using OpenLDAP testcontainer.
 * <p>
 * Tests the high-level API for LDAP authentication that returns usernames and roles.
 * Uses plain LDAP (no TLS) for simplicity in testing.
 * <p>
 * This test uses users loaded from the LDIF file (alice, bob, charlie) instead of
 * dynamically created test users.
 */
@Testcontainers
class LdapUsernameRolesProviderOpenLdapIntegrationTest {

    // Test user credentials from the LDIF file (ldap/test-data.ldif)
    private static final String ALICE_USERNAME = "alice";
    private static final String ALICE_PASSWORD = "alice123";
    private static final String BOB_USERNAME = "bob";
    private static final String BOB_PASSWORD = "bob456";
    private static final String CHARLIE_USERNAME = "charlie";
    private static final String CHARLIE_PASSWORD = "charlie789";
    private static final String DON_USERNAME = "don";
    private static final String DON_PASSWORD = "don012";

    @Container
    private static final OpenLdapContainer OPENLDAP_CONTAINER = OpenLdapContainer.builder()
            .withTls(true)
            .withLdifFile("ldap/test-data.ldif")
            .build();

    private static LdapUsernameRolesProvider provider;

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped port from the container
        final var host = OPENLDAP_CONTAINER.getHost();
        final var port = OPENLDAP_CONTAINER.getLdapPort();

        // Create LdapSimpleBind for OpenLDAP admin authentication
        // OpenLDAP admin DN: cn=admin,{baseDn}
        final var ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind("cn=admin", OPENLDAP_CONTAINER.getAdminPassword());

        // Create user role rules based on LDAP group membership
        // The queries below search for the user entry for memberOf attributes (requires memberOf overlay)
        // Alternatively it is possible to use queries for the groups with "member" or "uniqueMember"
        // "(&(objectClass=groupOfNames)(cn=administrators)(member={userDn}))")
        final var userRoleRules = List.of(
                new UserRoleEntity(
                        ADMIN, "(&(entryDN={userDn})(memberOf=cn=administrators,ou=groups,dc=example,dc=org))"),
                new UserRoleEntity(SUPER, "(&(entryDN={userDn})(memberOf=cn=developers,ou=groups,dc=example,dc=org))"),
                new UserRoleEntity(
                        USER, "(&(entryDN={userDn})(memberOf=cn=data-scientists,ou=groups,dc=example,dc=org))"));

        // Create connection properties for plain LDAP (no TLS for simplicity)
        // 5 second connect timeout
        // 10 second response timeout
        final var ldapConnectionProperties = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[] {host}, new int[] {port}),
                TlsMode.NONE,
                null,
                5000, // 5 second connect timeout
                10000, // 10 second response timeout
                1,
                "uid", // uidAttribute
                OPENLDAP_CONTAINER.getBaseDn(), // Use full base DN, SearchScope.SUB will find users in ou=people
                null,
                SearchScope.SUB,
                5,
                ADMIN, // assignedRole (fallback, not used when userRoleRules are provided)
                false,
                ldapSimpleBind,
                userRoleRules);

        // Wait for OpenLDAP to finish loading LDIF files
        Thread.sleep(8000);

        // Create the LdapUsernameRolesProvider
        provider = new LdapUsernameRolesProvider(ldapConnectionProperties, new SecurityLog());
    }

    @AfterAll
    static void tearDown() {
        OPENLDAP_CONTAINER.stop();
    }

    /**
     * Tests successful authentication with correct credentials.
     * <p>
     * Verifies that:
     * - findByUsernameAndPassword returns an Optional with UsernameRoles
     * - The username matches the authenticated user
     * - Roles are assigned based on LDAP group membership (alice is in 'developers' group)
     */
    @Test
    void testSuccessfulAuthentication() {
        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(ALICE_USERNAME, ALICE_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should succeed with correct credentials")
                .isPresent();

        assertThat(result.get().username())
                .as("Username should match the authenticated user")
                .isEqualTo(ALICE_USERNAME);

        // Alice is only in the 'developers' group, so should only have SUPER role
        assertThat(result.get().roles())
                .as("Alice should have SUPER role (member of developers group)")
                .contains(SUPER)
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
                provider.findByUsernameAndPassword(ALICE_USERNAME, wrongPassword.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result).as("Authentication should fail with wrong password").isEmpty();
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
                provider.findByUsernameAndPassword(ALICE_USERNAME, ALICE_PASSWORD.getBytes(StandardCharsets.UTF_8));

        assertThat(result).isPresent();

        // Convert to ApiPrincipal
        final var principal = result.get().toPrincipal();

        // Assert
        assertThat(principal.getName())
                .as("Principal name should match username")
                .isEqualTo(ALICE_USERNAME);

        // Alice is only in 'developers' group, so should have SUPER role
        assertThat(principal.getRoles())
                .as("Principal roles should match user roles from LDAP groups")
                .contains(SUPER)
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
                provider.findByUsernameAndPassword(ALICE_USERNAME, emptyPassword.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result).as("Authentication should fail with empty password").isEmpty();
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
                provider.findByUsernameAndPassword(emptyUsername, ALICE_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result).as("Authentication should fail with empty username").isEmpty();
    }

    /**
     * Tests authentication and role assignment for bob.
     * <p>
     * Bob is a member of both 'administrators' and 'developers' groups,
     * so should have both ADMIN and SUPER roles.
     */
    @Test
    void testBobHasBothAdminAndSuperRoles() {
        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> bobResult =
                provider.findByUsernameAndPassword(BOB_USERNAME, BOB_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(bobResult)
                .as("Bob should authenticate with correct credentials")
                .isPresent();
        assertThat(bobResult.get().username()).isEqualTo(BOB_USERNAME);

        // Bob is in both 'administrators' and 'developers' groups
        assertThat(bobResult.get().roles())
                .as("Bob should have both ADMIN and SUPER roles")
                .containsExactlyInAnyOrder(ADMIN, SUPER);
    }

    /**
     * Tests authentication and role assignment for charlie.
     * <p>
     * Charlie is not a member of any groups, so should have no roles.
     */
    @Test
    void testCharlieHasNoRoles() {
        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> charlieResult =
                provider.findByUsernameAndPassword(CHARLIE_USERNAME, CHARLIE_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(charlieResult)
                .as("Charlie should authenticate with correct credentials")
                .isPresent();
        assertThat(charlieResult.get().username()).isEqualTo(CHARLIE_USERNAME);

        // Charlie is not in any groups
        assertThat(charlieResult.get().roles())
                .as("Charlie should have no roles (not in any groups)")
                .isEmpty();
    }

    /**
     * Tests authentication and role assignment for don.
     * <p>
     * Don is a member of the 'data-scientists' group, so should have the USER role.
     */
    @Test
    void testDonHasUserRole() {
        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> donResult =
                provider.findByUsernameAndPassword(DON_USERNAME, DON_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(donResult)
                .as("Don should authenticate with correct credentials")
                .isPresent();
        assertThat(donResult.get().username()).isEqualTo(DON_USERNAME);

        // Don is in the 'data-scientists' group
        assertThat(donResult.get().roles())
                .as("Don should have USER role (member of data-scientists group)")
                .containsExactly(USER);
    }
}
