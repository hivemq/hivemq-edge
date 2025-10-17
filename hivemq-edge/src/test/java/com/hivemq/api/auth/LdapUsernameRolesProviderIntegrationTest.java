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
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link LdapUsernameRolesProvider} using LLDAP testcontainer.
 * <p>
 * Tests the high-level API for LDAP authentication that returns usernames and roles.
 * Uses plain LDAP (no TLS) for simplicity in testing.
 */
@Testcontainers
class LdapUsernameRolesProviderIntegrationTest {

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpassword123";
    private static final String LDAP_DN_TEMPLATE = "uid={username},ou=people,{baseDn}";

    @Container
    private static final LldapContainer LLDAP_CONTAINER = new LldapContainer();

    private static LdapUsernameRolesProvider provider;
    private static LdapConnectionProperties ldapConnectionProperties;

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped port from the container
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapPort();

        // Create connection properties for plain LDAP (no TLS for simplicity)
        ldapConnectionProperties = new LdapConnectionProperties(
                host,
                port,
                TlsMode.NONE,
                null,
                null,
                null,
                5000,  // 5 second connect timeout
                10000, // 10 second response timeout
                LDAP_DN_TEMPLATE,
                LLDAP_CONTAINER.getBaseDn(),
                "ADMIN");

        // Create test user in LLDAP
        createTestUser();

        // Create the LdapUsernameRolesProvider
        provider = new LdapUsernameRolesProvider(ldapConnectionProperties);
    }

    @AfterAll
    static void tearDown() {
        LLDAP_CONTAINER.stop();
    }

    /**
     * Creates a test user in LLDAP using the admin account.
     */
    private static void createTestUser() throws LDAPException, GeneralSecurityException {
        try (final LDAPConnection adminConnection = ldapConnectionProperties.createConnection()) {
            // Bind as admin
            final String adminUserDn = "uid=" + LLDAP_CONTAINER.getAdminUsername() + ",ou=people," + LLDAP_CONTAINER.getBaseDn();
            final BindRequest bindRequest = new SimpleBindRequest(adminUserDn, LLDAP_CONTAINER.getAdminPassword());
            final BindResult bindResult = adminConnection.bind(bindRequest);

            assertThat(bindResult.getResultCode()).isEqualTo(ResultCode.SUCCESS);

            // Add test user
            final String testUserDnString = "uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn();

            final AddRequest addRequest = new AddRequest(testUserDnString,
                    new Attribute("objectClass", "inetOrgPerson", "posixAccount"),
                    new Attribute("uid", TEST_USERNAME),
                    new Attribute("cn", TEST_USERNAME),
                    new Attribute("sn", "User"),
                    new Attribute("mail", TEST_USERNAME + "@example.com"),
                    new Attribute("uidNumber", "2000"),
                    new Attribute("gidNumber", "2000"),
                    new Attribute("homeDirectory", "/home/" + TEST_USERNAME)
            );

            adminConnection.add(addRequest);

            // Set password
            final ModifyRequest modifyRequest = new ModifyRequest(testUserDnString,
                    new Modification(ModificationType.REPLACE, "userPassword", TEST_PASSWORD));

            adminConnection.modify(modifyRequest);
        }
    }

    /**
     * Tests successful authentication with correct credentials.
     * <p>
     * Verifies that:
     * - findByUsernameAndPassword returns an Optional with UsernameRoles
     * - The username matches the authenticated user
     * - Roles are assigned (currently hardcoded to "ADMIN")
     */
    @Test
    void testSuccessfulAuthentication() {
        // Act
        final Optional<IUsernameRolesProvider.UsernameRoles> result =
                provider.findByUsernameAndPassword(TEST_USERNAME, TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should succeed with correct credentials")
                .isPresent();

        assertThat(result.get().username())
                .as("Username should match the authenticated user")
                .isEqualTo(TEST_USERNAME);

        assertThat(result.get().roles())
                .as("User should have ADMIN role (hardcoded for now)")
                .contains("ADMIN")
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
                provider.findByUsernameAndPassword(TEST_USERNAME, wrongPassword.getBytes(StandardCharsets.UTF_8));

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
                provider.findByUsernameAndPassword(TEST_USERNAME, TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        assertThat(result).isPresent();

        // Convert to ApiPrincipal
        final var principal = result.get().toPrincipal();

        // Assert
        assertThat(principal.getName())
                .as("Principal name should match username")
                .isEqualTo(TEST_USERNAME);

        assertThat(principal.getRoles())
                .as("Principal roles should match user roles")
                .contains("ADMIN")
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
                provider.findByUsernameAndPassword(TEST_USERNAME, emptyPassword.getBytes(StandardCharsets.UTF_8));

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
                provider.findByUsernameAndPassword(emptyUsername, TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(result)
                .as("Authentication should fail with empty username")
                .isEmpty();
    }
}
