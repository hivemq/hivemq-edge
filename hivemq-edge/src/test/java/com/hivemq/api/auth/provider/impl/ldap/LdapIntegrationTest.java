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
package com.hivemq.api.auth.provider.impl.ldap;

import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_PASSWORD;
import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for secure LDAP authentication using LLDAP testcontainer with TLS.
 * <p>
 * This test demonstrates:
 * - Setting up an LLDAP server in a Docker container with TLS/SSL enabled
 * - Generating a self-signed certificate for secure connections
 * - Configuring LDAPS (LDAP over TLS) for encrypted communication
 * - Creating a test user programmatically
 * - Performing successful authentication (bind) with correct credentials over secure connection
 * - Verifying authentication failure with incorrect credentials over secure connection
 * <p>
 * LLDAP (Light LDAP) is a lightweight LDAP server implementation perfect for testing.
 * The test uses UnboundID LDAP SDK for all LDAP operations and demonstrates proper
 * TLS/SSL certificate handling in a test environment.
 */
@Testcontainers
class LdapIntegrationTest {

    private static final String LDAP_DN_TEMPLATE = "uid={username},ou=people,{baseDn}";

    @Container
    private static final LldapContainer LLDAP_CONTAINER = LldapContainer.builder()
            .withLdaps()
            .build();

    private static LdapClient ldapClient;
    private static LdapConnectionProperties ldapConnectionProperties;

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped LDAPS port and host from the container
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapsPort();

        // Create LdapSimpleBind for LLDAP admin authentication
        // LLDAP admin DN: uid=admin,ou=people,{baseDn}
        // Note: The RDN should be just the user's identifier, the organizational unit
        // will be added from the rdns parameter in LdapConnectionProperties
        final LdapConnectionProperties.LdapSimpleBind ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername(),
                        LLDAP_CONTAINER.getAdminPassword());

        // Create connection properties for LDAPS
        ldapConnectionProperties = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore(LLDAP_CONTAINER.getTrustStoreFile().getAbsolutePath(), LldapContainer.KEYSTORE_PASSWORD, KeyStore.getDefaultType()),
                10000, // 10 second connect timeout
                30000, // 30 second response timeout
                1,
                "uid",    // uidAttribute
                "ou=people," + LLDAP_CONTAINER.getBaseDn(),  // rdns
                null,
                SearchScope.SUB,
                5,
                ADMIN,  // assignedRole
                false,
                ldapSimpleBind);

        // Create and start LDAP client
        ldapClient = new LdapClient(ldapConnectionProperties, new SecurityLog());
        ldapClient.start();

        // Create test user in LLDAP (using direct connection for admin operations)
        new LdapTestConnection(ldapConnectionProperties).createTestUser(
                LLDAP_CONTAINER.getAdminDn(),
                LLDAP_CONTAINER.getAdminPassword(),
                LLDAP_CONTAINER.getBaseDn());
    }

    @AfterAll
    static void tearDown() {
        // Stop LDAP client
        if (ldapClient != null && ldapClient.isStarted()) {
            ldapClient.stop();
        }

        LLDAP_CONTAINER.stop();
    }


    /**
     * Tests successful LDAP bind with correct credentials over secure TLS connection.
     * <p>
     * This demonstrates the typical authentication flow with TLS using the LdapClient:
     * 1. Client uses connection pool to get a connection
     * 2. Attempt to bind with user DN and password
     * 3. Verify the bind was successful
     */
    @Test
    void testSuccessfulBind() throws LDAPException {
        // Act
        final boolean authenticated = ldapClient.authenticateUser(TEST_USERNAME, LdapTestConnection.TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(authenticated)
                .as("Bind should succeed with correct credentials over secure TLS connection")
                .isTrue();
    }

    /**
     * Tests failed LDAP bind with incorrect password over secure TLS connection.
     * <p>
     * This demonstrates authentication failure handling with TLS using the LdapClient:
     * 1. Client uses connection pool to get a connection
     * 2. Attempt to bind with user DN and WRONG password
     * 3. Verify the bind failed (returns false)
     */
    @Test
    void testFailedBindWithWrongPassword() throws LDAPException {
        // Arrange
        final String wrongPassword = "wrong_password";

        // Act
        final boolean authenticated = ldapClient.authenticateUser(TEST_USERNAME, wrongPassword.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(authenticated)
                .as("Bind should fail with wrong password even over secure TLS connection")
                .isFalse();
    }

    /**
     * Tests SearchFilterDnResolver with an authenticated connection pool.
     * <p>
     * Note: SearchFilterDnResolver requires an authenticated connection pool to perform searches.
     * Most LDAP servers (including LLDAP) require authentication for search operations.
     * <p>
     * This test demonstrates:
     * - Creating an authenticated connection pool with a service account
     * - Using SearchFilterDnResolver to find user DNs
     * - Authenticating with the resolved DN
     */
    @Test
    void testSearchFilterDnResolver_withAuthenticatedPool() throws Exception {
        // Create an authenticated connection pool using the admin account
        // In production, you would use a dedicated service account with read-only permissions
        final var testconnection = new LdapTestConnection(ldapConnectionProperties);
        final var ldapClient = new LdapClient(ldapConnectionProperties, new SecurityLog());
        ldapClient.start();
        try (final LDAPConnection adminConnection = testconnection.createConnection()) {
            // Bind as admin to create authenticated pool using LldapContainer's convenience methods
            final BindRequest bindRequest = new SimpleBindRequest(
                    LLDAP_CONTAINER.getAdminDn(),
                    LLDAP_CONTAINER.getAdminPassword());
            final BindResult bindResult = adminConnection.bind(bindRequest);
            assertThat(bindResult.getResultCode()).isEqualTo(ResultCode.SUCCESS);

            // Create an authenticated connection pool

            try (final var authenticatedPool = new LDAPConnectionPool(adminConnection, 1, 5)) {
                // Test 1: Simple UID search
                final SearchFilterDnResolver uidResolver = new SearchFilterDnResolver(authenticatedPool,
                        "ou=people," + LLDAP_CONTAINER.getBaseDn(),
                        "uid",
                        null,
                        SearchScope.ONE,
                        5);

                String resolvedDn = uidResolver.resolveDn(TEST_USERNAME);
                assertThat(resolvedDn).as("Should resolve DN by UID")
                        .isEqualTo("uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn());

                // Verify we can authenticate with the resolved DN
                final boolean authenticated =
                        ldapClient.bindUser(resolvedDn, TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));
                assertThat(authenticated).as("Should authenticate with resolved DN").isTrue();

                // Test 2: Email search
                final SearchFilterDnResolver emailResolver = new SearchFilterDnResolver(authenticatedPool,
                        LLDAP_CONTAINER.getBaseDn(),
                        "mail",
                        null,
                        SearchScope.SUB,
                        5);

                resolvedDn = emailResolver.resolveDn(TEST_USERNAME + "@example.com");
                assertThat(resolvedDn).as("Should resolve DN by email")
                        .isEqualTo("uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn());

                // Test 3: Complex filter
                final SearchFilterDnResolver complexResolver = new SearchFilterDnResolver(authenticatedPool,
                        LLDAP_CONTAINER.getBaseDn(),
                        "uid",
                        "inetOrgPerson",
                        SearchScope.SUB,
                        5);

                resolvedDn = complexResolver.resolveDn(TEST_USERNAME);
                assertThat(resolvedDn).as("Should resolve DN with complex filter")
                        .isEqualTo("uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn());

                // Test 5: User not found
                assertThatThrownBy(() -> uidResolver.resolveDn("nonexistent")).isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
                        .hasMessageContaining("No LDAP entry found for username: nonexistent");

            }
        }
    }
}
