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
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.OpenLdapContainer;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.security.KeyStore;
import java.util.stream.Stream;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Directory Descent (search-based DN resolution) 
 * using both OpenLDAP and LLDAP containers.
 * 
 * <p>Tests verify that:
 * <ul>
 *     <li>Search-based resolution works with users in different OUs</li>
 *     <li>Both Direct Reference and Directory Descent work correctly</li>
 *     <li>Complex search filters function properly</li>
 *     <li>Both LDAP server implementations behave consistently</li>
 * </ul>
 */
@Testcontainers
class DirectoryDescentIntegrationTest {

    @Container
    private static final OpenLdapContainer OPENLDAP = OpenLdapContainer.builder()
            .withTls(true)
            .withLdifFile("ldap/test-data-nested-ous.ldif")
            .build();

    @Container
    private static final LldapContainer LLDAP = LldapContainer.builder()
            .withLdaps()
            .build();

    private static LDAPConnectionPool openLdapPool;
    private static LDAPConnectionPool lldapPool;

    static class LdapTestConfig {
        String name;
        LDAPConnectionPool pool;
        String baseDn;
        String adminDn;
        String adminPassword;

        LdapTestConfig(final @NotNull String name, final @NotNull LDAPConnectionPool pool, final @NotNull String baseDn,
                       final @NotNull String adminDn, final @NotNull String adminPassword) {
            this.name = name;
            this.pool = pool;
            this.baseDn = baseDn;
            this.adminDn = adminDn;
            this.adminPassword = adminPassword;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<LdapTestConfig> ldapProviders() {
        return Stream.of(
            new LdapTestConfig("OpenLDAP", openLdapPool, 
                OPENLDAP.getBaseDn(), 
                OPENLDAP.getAdminDn(), 
                OPENLDAP.getAdminPassword()),
            new LdapTestConfig("LLDAP", lldapPool, 
                LLDAP.getBaseDn(), 
                LLDAP.getAdminDn(), 
                LLDAP.getAdminPassword())
        );
    }

    @BeforeAll
    static void setUp() throws Exception {
        // Wait for containers to be ready
        Thread.sleep(8000);

        openLdapPool = createOpenLdapPool();
        lldapPool = createLldapPool();
    }

    @AfterAll
    static void tearDown() {
        if (openLdapPool != null) openLdapPool.close();
        if (lldapPool != null) lldapPool.close();
    }

    private static LDAPConnectionPool createOpenLdapPool() throws Exception {
        final LdapConnectionProperties props = new LdapConnectionProperties(
            new LdapConnectionProperties.LdapServers(
                new String[]{OPENLDAP.getHost()},
                new int[]{OPENLDAP.getLdapPort()}),
            TlsMode.START_TLS,
            null,
            5000,
            10000,
            5,
            "uid",
            OPENLDAP.getBaseDn(),
            null,
            null,
            SearchScope.SUB,
            5,
            ADMIN,
            true,
            new LdapConnectionProperties.LdapSimpleBind(
                "cn=admin",
                OPENLDAP.getAdminPassword()),
            null
        );

        final LdapTestConnection testConn = new LdapTestConnection(props);
        final LDAPConnection conn = testConn.createConnection();
        conn.bind(OPENLDAP.getAdminDn(), OPENLDAP.getAdminPassword());
        return new LDAPConnectionPool(conn, 1, 5);
    }

    private static LDAPConnectionPool createLldapPool() throws Exception {
        final LdapConnectionProperties props = new LdapConnectionProperties(
            new LdapConnectionProperties.LdapServers(new String[]{LLDAP.getHost()}, new int[]{LLDAP.getLdapsPort()}),
            TlsMode.LDAPS,
            new LdapConnectionProperties.TrustStore(
                LLDAP.getTrustStoreFile().getAbsolutePath(),
                LldapContainer.KEYSTORE_PASSWORD,
                KeyStore.getDefaultType()),
            5000,
            10000,
            5,
            "uid",
            "ou=people," + LLDAP.getBaseDn(),
            null,
            null,
            SearchScope.SUB,
            5,
            ADMIN,
            false,
            new LdapConnectionProperties.LdapSimpleBind(
                "uid=" + LLDAP.getAdminUsername(),
                LLDAP.getAdminPassword()),
            null
        );

        final LdapTestConnection testConn = new LdapTestConnection(props);
        final LDAPConnection conn = testConn.createConnection();
        conn.bind(LLDAP.getAdminDn(), LLDAP.getAdminPassword());

        createLldapTestUsers(conn, LLDAP.getBaseDn());

        return new LDAPConnectionPool(conn, 1, 5);
    }

    private static void createLldapTestUsers(final @NotNull LDAPConnection conn, final @NotNull String baseDn) {
        // LLDAP doesn't support nested OUs, so create users directly in ou=people
        // This tests Directory Descent search functionality but not the nested OU scenario
        createUserIfNotExists(conn, baseDn, "alice", "alice123",
            "ou=people", "Alice Anderson", "Anderson");
        createUserIfNotExists(conn, baseDn, "bob", "bob456",
            "ou=people", "Bob Brown", "Brown");
        createUserIfNotExists(conn, baseDn, "charlie", "charlie789",
            "ou=people", "Charlie Chen", "Chen");
    }

    private static void createUserIfNotExists(final @NotNull LDAPConnection conn, final @NotNull String baseDn,
                                              final @NotNull String uid, final @NotNull String password,
                                              final @NotNull String ou, final @NotNull String cn, final @NotNull String sn) {
        try {
            final var userDn = "uid=" + uid + "," + ou + "," + baseDn;
            conn.add(new AddRequest(userDn,
                new Attribute("objectClass", "inetOrgPerson", "posixAccount"),
                new Attribute("uid", uid),
                new Attribute("cn", cn),
                new Attribute("sn", sn),
                new Attribute("mail", uid + "@example.org"),
                new Attribute("uidNumber", String.valueOf(10000 + uid.hashCode())),
                new Attribute("gidNumber", "1000"),
                new Attribute("homeDirectory", "/home/" + uid)));

            conn.modify(new ModifyRequest(userDn,
                new Modification(ModificationType.REPLACE, "userPassword", password)));
        } catch (final LDAPException e) {
            if (e.getResultCode() != ResultCode.ENTRY_ALREADY_EXISTS) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    @DisplayName("Direct Reference fails with users in nested OUs (OpenLDAP only)")
    void directReferenceFailsWithNestedOus() throws LDAPException {
        // This test demonstrates that Direct Reference can't find users in nested OUs
        // because it constructs the DN with a fixed template
        final TemplateDnResolver templateResolver = new TemplateDnResolver(
            "uid",
            "ou=people," + OPENLDAP.getBaseDn());

        final String resolvedDn = templateResolver.resolveDn("alice");

        // The template produces a DN that assumes users are directly in ou=people
        assertThat(resolvedDn)
            .isEqualTo("uid=alice,ou=people," + OPENLDAP.getBaseDn())
            .isNotEqualTo("uid=alice,ou=engineering,ou=people," + OPENLDAP.getBaseDn());

        // Try to bind with this wrong DN - should either fail or not find the user
        try (final var conn = openLdapPool.getConnection()) {
            try {
                final BindResult result = conn.bind(new SimpleBindRequest(resolvedDn, "alice123"));
                // If bind succeeds, it means alice exists at this location (shouldn't happen with our nested OU structure)
                assertThat(result.getResultCode())
                    .as("Direct Reference should not find user in nested OU")
                    .isNotEqualTo(ResultCode.SUCCESS);
            } catch (final LDAPException e) {
                // Expected: Either NO_SUCH_OBJECT (user doesn't exist at this DN)
                // or INVALID_CREDENTIALS (wrong DN or password)
                assertThat(e.getResultCode())
                    .as("Direct Reference should fail to authenticate user in nested OU")
                    .isIn(ResultCode.NO_SUCH_OBJECT, ResultCode.INVALID_CREDENTIALS);
            }
        }
    }

    @ParameterizedTest
    @MethodSource("ldapProviders")
    @DisplayName("Directory Descent succeeds with search-based resolution")
    void directoryDescentSucceedsWithSearch(final @NotNull LdapTestConfig config) throws LDAPException {
        final SearchFilterDnResolver searchResolver = new SearchFilterDnResolver(
            config.pool,
            config.baseDn,
            "uid",
            null,
            SearchScope.SUB,
            5);

        final String resolvedDn = searchResolver.resolveDn("alice");

        // For OpenLDAP: alice is in nested OU (ou=engineering,ou=people)
        // For LLDAP: alice is directly in ou=people (LLDAP doesn't support nested OUs)
        final String expectedDn = config.name.equals("OpenLDAP")
            ? "uid=alice,ou=engineering,ou=people," + config.baseDn
            : "uid=alice,ou=people," + config.baseDn;

        assertThat(resolvedDn)
            .as("Should find alice using Directory Descent")
            .isEqualTo(expectedDn);

        try (final var conn = config.pool.getConnection()) {
            final boolean success = conn.bind(
                new SimpleBindRequest(resolvedDn, "alice123")
            ).getResultCode() == ResultCode.SUCCESS;

            assertThat(success)
                .as("Should authenticate with correctly resolved DN")
                .isTrue();
        }
    }

    @ParameterizedTest
    @MethodSource("ldapProviders")
    @DisplayName("Directory Descent finds all users")
    void directoryDescentFindsAllUsers(final @NotNull LdapTestConfig config) {
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
            config.pool,
            config.baseDn,
            "uid",
            null,
            SearchScope.SUB,
            5);

        if (config.name.equals("OpenLDAP")) {
            // OpenLDAP has users in nested OUs
            assertThat(resolver.resolveDn("alice"))
                .isEqualTo("uid=alice,ou=engineering,ou=people," + config.baseDn);

            assertThat(resolver.resolveDn("bob"))
                .isEqualTo("uid=bob,ou=engineering,ou=people," + config.baseDn);

            assertThat(resolver.resolveDn("charlie"))
                .isEqualTo("uid=charlie,ou=sales,ou=people," + config.baseDn);
        } else {
            // LLDAP has users directly in ou=people
            assertThat(resolver.resolveDn("alice"))
                .isEqualTo("uid=alice,ou=people," + config.baseDn);

            assertThat(resolver.resolveDn("bob"))
                .isEqualTo("uid=bob,ou=people," + config.baseDn);

            assertThat(resolver.resolveDn("charlie"))
                .isEqualTo("uid=charlie,ou=people," + config.baseDn);
        }
    }

    @ParameterizedTest
    @MethodSource("ldapProviders")
    @DisplayName("Directory Descent can search by email")
    void directoryDescentSearchByEmail(final @NotNull LdapTestConfig config) {
        final SearchFilterDnResolver emailResolver = new SearchFilterDnResolver(
            config.pool,
            config.baseDn,
            "mail",
            null,
            SearchScope.SUB,
            5);

        final String dn = emailResolver.resolveDn("alice@example.org");
        assertThat(dn).contains("uid=alice");
    }

    @ParameterizedTest
    @MethodSource("ldapProviders")
    @DisplayName("Directory Descent search scope SUB finds nested users")
    void directoryDescentSearchScopeSubtree(final @NotNull LdapTestConfig config) {
        final SearchFilterDnResolver subResolver = new SearchFilterDnResolver(
            config.pool,
            config.baseDn,
            "uid",
            null,
            SearchScope.SUB,
            5);

        assertThatNoException().isThrownBy(() -> subResolver.resolveDn("alice"));
    }

    @Test
    @DisplayName("Directory Descent search scope ONE fails for nested users (OpenLDAP only)")
    void directoryDescentSearchScopeOneLevelFails() {
        // This test only makes sense with OpenLDAP which has nested OUs
        // With ONE scope searching from base DN, it won't find users in nested OUs
        final SearchFilterDnResolver oneResolver = new SearchFilterDnResolver(
            openLdapPool,
            OPENLDAP.getBaseDn(),
            "uid",
            null,
            SearchScope.ONE,
            5);

        assertThatThrownBy(() -> oneResolver.resolveDn("alice"))
            .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
            .hasMessageContaining("No LDAP entry found");
    }

    @ParameterizedTest
    @MethodSource("ldapProviders")
    @DisplayName("Directory Descent throws exception for non-existent users")
    void directoryDescentUserNotFound(final @NotNull LdapTestConfig config) {
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
            config.pool,
            config.baseDn,
            "uid",
            null,
            SearchScope.SUB,
            5);

        assertThatThrownBy(() -> resolver.resolveDn("nonexistent"))
            .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
            .hasMessageContaining("No LDAP entry found for username: nonexistent");
    }

    @ParameterizedTest
    @MethodSource("ldapProviders")
    @DisplayName("Directory Descent prevents LDAP injection attacks")
    void directoryDescentPreventsInjection(final @NotNull LdapTestConfig config) {
        final SearchFilterDnResolver resolver = new SearchFilterDnResolver(
            config.pool,
            config.baseDn,
            "uid",
            null,
            SearchScope.SUB,
            5);

        assertThatThrownBy(() -> resolver.resolveDn("alice)(|(uid=*"))
            .as("Should escape special characters and fail to find user")
            .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class);
    }

    @Test
    @DisplayName("Performance comparison: Direct Reference vs Directory Descent")
    void performanceComparison() {
        long start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            final TemplateDnResolver template = new TemplateDnResolver(
                "uid={username},ou=people,{baseDn}", OPENLDAP.getBaseDn());
            template.resolveDn("alice");
        }
        final var directMs = (System.nanoTime() - start) / 1_000_000;

        start = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            final SearchFilterDnResolver search = new SearchFilterDnResolver(
                openLdapPool,
                OPENLDAP.getBaseDn(),
                "uid",
                null,
                SearchScope.SUB,
                5);
            search.resolveDn("alice");
        }
        final var searchMs = (System.nanoTime() - start) / 1_000_000;

        System.out.println("Direct Reference: " + directMs + "ms (100 iterations)");
        System.out.println("Directory Descent: " + searchMs + "ms (100 iterations)");
        System.out.println("Overhead: " + (searchMs - directMs) + "ms (~" + 
            ((searchMs - directMs) / 100) + "ms per lookup)");

        assertThat(directMs).as("Direct Reference should be faster").isLessThan(searchMs);
    }
}
