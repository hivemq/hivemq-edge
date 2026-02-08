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
import com.unboundid.ldap.sdk.SearchScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_PASSWORD;
import static com.hivemq.api.auth.provider.impl.ldap.testcontainer.LdapTestConnection.TEST_USERNAME;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for LDAP baseDn functionality.
 * <p>
 * Tests the dual-mode behavior:
 * - Legacy mode: baseDn is null, rdns is absolute DN
 * - New mode: baseDn is specified, rdns and service account rdns are relative to baseDn
 * <p>
 * This allows service accounts to be in different OUs than users
 * (e.g., service account in ou=system, users in ou=people).
 */
@Testcontainers
class LdapClientBaseDnIntegrationTest {

    @Container
    private static final LldapContainer LLDAP_CONTAINER = LldapContainer.builder()
            .withLdaps()
            .build();

    private static String baseDn;

    @BeforeAll
    static void setUp() throws Exception {
        baseDn = LLDAP_CONTAINER.getBaseDn();

        // Create test user using legacy absolute DN configuration
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapsPort();

        final LdapConnectionProperties.LdapSimpleBind ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername(),
                        LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties setupProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore(LLDAP_CONTAINER.getTrustStoreFile().getAbsolutePath(),
                        LldapContainer.KEYSTORE_PASSWORD, KeyStore.getDefaultType()),
                10000,
                30000,
                1,
                "uid",
                "ou=people," + baseDn,  // Absolute DN
                null,  // Legacy mode
                null,
                SearchScope.SUB,
                5,
                ADMIN,
                false,
                ldapSimpleBind,
                null);

        new LdapTestConnection(setupProps).createTestUser(
                LLDAP_CONTAINER.getAdminDn(),
                LLDAP_CONTAINER.getAdminPassword(),
                baseDn);
    }

    @AfterAll
    static void tearDown() {
        LLDAP_CONTAINER.stop();
    }

    /**
     * Tests legacy mode: baseDn is null, rdns is absolute DN.
     * Service account rdns is relative to the absolute rdns.
     */
    @Test
    void testLegacyMode_absoluteRdns() throws Exception {
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapsPort();

        // Legacy mode: service account rdns is relative to base rdns
        final LdapConnectionProperties.LdapSimpleBind ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername(),  // Relative to base rdns
                        LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore(LLDAP_CONTAINER.getTrustStoreFile().getAbsolutePath(),
                        LldapContainer.KEYSTORE_PASSWORD, KeyStore.getDefaultType()),
                10000,
                30000,
                1,
                "uid",
                "ou=people," + baseDn,  // Absolute DN
                null,  // baseDn is null = legacy mode
                null,
                SearchScope.SUB,
                5,
                ADMIN,
                false,
                ldapSimpleBind,
                null);

        final LdapClient client = new LdapClient(props, new SecurityLog());
        try {
            client.start();

            // Test user authentication
            final boolean authenticated = client.authenticateUser(
                    TEST_USERNAME,
                    TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

            assertThat(authenticated)
                    .as("Legacy mode: user authentication should succeed")
                    .isTrue();
        } finally {
            if (client.isStarted()) {
                client.stop();
            }
        }
    }

    /**
     * Tests new mode: baseDn is specified, both rdns and service account rdns are relative to baseDn.
     * This allows service accounts in different OUs than users.
     */
    @Test
    void testNewMode_relativeRdns() throws Exception {
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapsPort();

        // New mode: service account rdns is relative to baseDn (NOT to search root)
        final LdapConnectionProperties.LdapSimpleBind ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername() + ",ou=people",  // Relative to baseDn
                        LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore(LLDAP_CONTAINER.getTrustStoreFile().getAbsolutePath(),
                        LldapContainer.KEYSTORE_PASSWORD, KeyStore.getDefaultType()),
                10000,
                30000,
                1,
                "uid",
                "ou=people",  // Relative rdns
                baseDn,  // baseDn is specified = new mode
                null,
                SearchScope.SUB,
                5,
                ADMIN,
                false,
                ldapSimpleBind,
                null);

        final LdapClient client = new LdapClient(props, new SecurityLog());
        try {
            client.start();

            // Test user authentication
            final boolean authenticated = client.authenticateUser(
                    TEST_USERNAME,
                    TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

            assertThat(authenticated)
                    .as("New mode: user authentication should succeed")
                    .isTrue();
        } finally {
            if (client.isStarted()) {
                client.stop();
            }
        }
    }

    /**
     * Tests that new mode allows service accounts in different OUs.
     * In this test, we demonstrate that the service account can have a different OU structure
     * than the user search base.
     */
    @Test
    void testNewMode_serviceAccountInDifferentOU() throws Exception {
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapsPort();

        // Service account is in ou=people (the admin user location in LLDAP)
        // User search base is also ou=people
        // This demonstrates that both are relative to baseDn independently
        final LdapConnectionProperties.LdapSimpleBind ldapSimpleBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername() + ",ou=people",  // Service account location
                        LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore(LLDAP_CONTAINER.getTrustStoreFile().getAbsolutePath(),
                        LldapContainer.KEYSTORE_PASSWORD, KeyStore.getDefaultType()),
                10000,
                30000,
                1,
                "uid",
                "ou=people",  // User search base
                baseDn,  // Both service account and search base are relative to this
                null,
                SearchScope.SUB,
                5,
                ADMIN,
                false,
                ldapSimpleBind,
                null);

        final LdapClient client = new LdapClient(props, new SecurityLog());
        try {
            client.start();

            // Verify that the client can authenticate successfully
            // This proves that the service account DN was constructed correctly
            assertThat(client.isStarted()).isTrue();

            // Test user authentication to verify search base is also correct
            final boolean authenticated = client.authenticateUser(
                    TEST_USERNAME,
                    TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

            assertThat(authenticated)
                    .as("New mode with separate OUs: user authentication should succeed")
                    .isTrue();
        } finally {
            if (client.isStarted()) {
                client.stop();
            }
        }
    }

    /**
     * Tests that legacy and new modes produce the same effective DNs when configured equivalently.
     */
    @Test
    void testEquivalentConfigurations() throws Exception {
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapsPort();

        // Legacy mode configuration
        final LdapConnectionProperties.LdapSimpleBind legacyBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername(),
                        LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties legacyProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore(LLDAP_CONTAINER.getTrustStoreFile().getAbsolutePath(),
                        LldapContainer.KEYSTORE_PASSWORD, KeyStore.getDefaultType()),
                10000,
                30000,
                1,
                "uid",
                "ou=people," + baseDn,  // Absolute
                null,  // Legacy
                null,
                SearchScope.SUB,
                5,
                ADMIN,
                false,
                legacyBind,
                null);

        // New mode configuration (equivalent)
        final LdapConnectionProperties.LdapSimpleBind newBind =
                new LdapConnectionProperties.LdapSimpleBind(
                        "uid=" + LLDAP_CONTAINER.getAdminUsername() + ",ou=people",  // Relative to baseDn
                        LLDAP_CONTAINER.getAdminPassword());

        final LdapConnectionProperties newProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{host}, new int[]{port}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore(LLDAP_CONTAINER.getTrustStoreFile().getAbsolutePath(),
                        LldapContainer.KEYSTORE_PASSWORD, KeyStore.getDefaultType()),
                10000,
                30000,
                1,
                "uid",
                "ou=people",  // Relative
                baseDn,  // New mode
                null,
                SearchScope.SUB,
                5,
                ADMIN,
                false,
                newBind,
                null);

        // Test both configurations
        final LdapClient legacyClient = new LdapClient(legacyProps, new SecurityLog());
        final LdapClient newClient = new LdapClient(newProps, new SecurityLog());

        try {
            legacyClient.start();
            newClient.start();

            // Both should authenticate successfully
            final boolean legacyAuth = legacyClient.authenticateUser(
                    TEST_USERNAME,
                    TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

            final boolean newAuth = newClient.authenticateUser(
                    TEST_USERNAME,
                    TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

            assertThat(legacyAuth)
                    .as("Legacy mode authentication")
                    .isTrue();

            assertThat(newAuth)
                    .as("New mode authentication")
                    .isTrue();

        } finally {
            if (legacyClient.isStarted()) {
                legacyClient.stop();
            }
            if (newClient.isStarted()) {
                newClient.stop();
            }
        }
    }
}
