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

import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_PASSWORD;
import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchScope;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for different TLS modes and timeouts.
 * <p>
 * Tests three TLS modes:
 * <ul>
 *     <li>NONE - Plain LDAP without encryption (port 389)</li>
 *     <li>START_TLS - Plain connection upgraded to TLS (port 389)</li>
 *     <li>LDAPS - TLS from connection start (tested in LdapIntegrationTest)</li>
 * </ul>
 */
@Testcontainers
class LdapTlsModesIntegrationTest {

    @Container
    private static final LldapContainer LLDAP_CONTAINER =
            LldapContainer.builder().withLdaps().build();

    private static LdapClient ldapClient;

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped port from the container
        final var host = LLDAP_CONTAINER.getHost();
        final var port = LLDAP_CONTAINER.getLdapPort();

        // Create LdapSimpleBind for LLDAP admin authentication
        // LLDAP admin DN: uid=admin,ou=people,{baseDn}
        // Note: The RDN should be just the user's identifier, the organizational unit
        // will be added from the rdns parameter in LdapConnectionProperties
        final var ldapSimpleBind = new LdapConnectionProperties.LdapSimpleBind(
                "uid=" + LLDAP_CONTAINER.getAdminUsername(), LLDAP_CONTAINER.getAdminPassword());

        // Create connection properties for plain LDAP (no TLS)
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
                "ou=people," + LLDAP_CONTAINER.getBaseDn(), // rdns
                null,
                null,
                SearchScope.SUB,
                5,
                "ADMIN", // assignedRole
                false,
                ldapSimpleBind,
                null);

        // Create and start LDAP client
        ldapClient = new LdapClient(ldapConnectionProperties, new SecurityLog());
        ldapClient.start();

        // Create test user
        new LdapTestConnection(ldapConnectionProperties)
                .createTestUser(
                        LLDAP_CONTAINER.getAdminDn(), LLDAP_CONTAINER.getAdminPassword(), LLDAP_CONTAINER.getBaseDn());
    }

    @AfterAll
    static void tearDown() {
        if (ldapClient != null && ldapClient.isStarted()) {
            ldapClient.stop();
        }
        LLDAP_CONTAINER.stop();
    }

    /**
     * Tests authentication over plain LDAP (no encryption).
     */
    @Test
    void testPlainLdapAuthentication() throws LDAPException {
        // Act
        final boolean authenticated = ldapClient.authenticateUser(
                TEST_USERNAME, LdapTestConnection.TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(authenticated)
                .as("Authentication should succeed over plain LDAP")
                .isTrue();
    }

    /**
     * Tests that authentication fails with wrong password over plain LDAP.
     */
    @Test
    void testPlainLdapAuthenticationFailsWithWrongPassword() throws LDAPException {
        // Act
        final boolean authenticated =
                ldapClient.authenticateUser(TEST_USERNAME, "wrongpassword".getBytes(StandardCharsets.UTF_8));

        // Assert
        assertThat(authenticated)
                .as("Authentication should fail with wrong password")
                .isFalse();
    }

    /**
     * Tests connection timeout by trying to connect to a non-responsive host.
     */
    @Test
    void testConnectionTimeout() throws Exception {
        // Arrange - use a non-routable IP that will timeout
        final LdapConnectionProperties.LdapSimpleBind ldapSimpleBind = new LdapConnectionProperties.LdapSimpleBind(
                "uid=" + LLDAP_CONTAINER.getAdminUsername(), LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties timeoutProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(
                        new String[] {"10.255.255.1"}, new int[] {389}), // Non-routable IP
                TlsMode.NONE,
                null,
                1000, // 1 second timeout - should fail quickly
                5000,
                1,
                "uid", // uidAttribute
                "ou=people," + LLDAP_CONTAINER.getBaseDn(), // rdns
                null,
                null,
                SearchScope.SUB,
                5,
                "ADMIN", // assignedRole
                false,
                ldapSimpleBind,
                null);

        final LdapClient timeoutClient = new LdapClient(timeoutProps, new SecurityLog());

        timeoutClient.start();

        final long startTime = System.currentTimeMillis();
        // With Directory Descent enabled by default, the timeout occurs during DN resolution
        // which wraps the LDAPException in a DnResolutionException
        assertThatThrownBy(() -> timeoutClient.authenticateUser("test", "test".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
                .hasCauseInstanceOf(LDAPException.class);
        final long duration = System.currentTimeMillis() - startTime;

        // Should timeout within reasonable time (less than 5 seconds)
        // allowing some margin for processing
        assertThat(duration).as("Connection should timeout quickly").isLessThan(5000);
    }

    /**
     * Tests that default timeouts are used when set to 0.
     */
    @Test
    void testDefaultTimeouts() throws Exception {
        // Arrange
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapPort();

        final LdapConnectionProperties.LdapSimpleBind ldapSimpleBind = new LdapConnectionProperties.LdapSimpleBind(
                "uid=" + LLDAP_CONTAINER.getAdminUsername(), LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties defaultTimeoutProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[] {host}, new int[] {port}),
                TlsMode.NONE,
                null,
                0, // Use default timeout
                0, // Use default timeout
                1,
                "uid", // uidAttribute
                "ou=people," + LLDAP_CONTAINER.getBaseDn(), // rdns
                null,
                null,
                SearchScope.SUB,
                5,
                "ADMIN", // assignedRole
                false,
                ldapSimpleBind,
                null);

        final LdapClient defaultTimeoutClient = new LdapClient(defaultTimeoutProps, new SecurityLog());

        // Act
        defaultTimeoutClient.start();

        try {
            final boolean authenticated = defaultTimeoutClient.authenticateUser(
                    TEST_USERNAME, TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

            // Assert
            assertThat(authenticated)
                    .as("Authentication should work with default timeouts")
                    .isTrue();
        } finally {
            defaultTimeoutClient.stop();
        }
    }
}
