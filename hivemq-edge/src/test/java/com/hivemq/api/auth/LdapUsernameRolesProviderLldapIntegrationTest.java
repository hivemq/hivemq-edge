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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.auth.provider.impl.ldap.LdapUsernameRolesProvider;
import com.hivemq.api.auth.provider.impl.ldap.TlsMode;
import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.hivemq.configuration.entity.api.ldap.UserRoleEntity;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test for {@link LdapUsernameRolesProvider} using LLDAP testcontainer.
 * <p>
 * Tests the high-level API for LDAP authentication that returns usernames and roles.
 * Uses plain LDAP (no TLS) for simplicity in testing.
 * <p>
 * This test uses dynamically created users and groups, with group membership
 * determined by querying groupOfUniqueNames with uniqueMember attribute.
 * <p>
 * Unlike the OpenLDAP test which uses the memberOf overlay, this test demonstrates
 * role resolution by querying groups directly using uniqueMember.
 */
@Testcontainers
class LdapUsernameRolesProviderLldapIntegrationTest {

    // Test user credentials
    private static final String ALICE_USERNAME = "alice";
    private static final String ALICE_PASSWORD = "alice123";
    private static final String BOB_USERNAME = "bob";
    private static final String BOB_PASSWORD = "bob456";
    private static final String CHARLIE_USERNAME = "charlie";
    private static final String CHARLIE_PASSWORD = "charlie789";
    private static final String DON_USERNAME = "don";
    private static final String DON_PASSWORD = "don012";

    @Container
    private static final LldapContainer LLDAP_CONTAINER = new LldapContainer();

    private static LdapUsernameRolesProvider provider;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped port from the container
        final var host = LLDAP_CONTAINER.getHost();
        final var port = LLDAP_CONTAINER.getLdapPort();
        final var baseDn = LLDAP_CONTAINER.getBaseDn();

        // Create LdapSimpleBind for LLDAP admin authentication
        final var ldapSimpleBind = new LdapConnectionProperties.LdapSimpleBind(
                LLDAP_CONTAINER.getAdminRdns(), LLDAP_CONTAINER.getAdminPassword());

        // Create user role rules based on LDAP group membership using memberOf attribute
        // LLDAP maintains memberOf on user entries when users are added to groups via the API
        // Note: LLDAP group DNs use format: cn=groupname,ou=groups,{baseDn}
        // "(&(objectClass=groupOfNames)(cn=administrators)(member={userDn}))"
        final var userRoleRules = List.of(
                new UserRoleEntity(ADMIN, "(&(objectClass=groupOfNames)(cn=administrators)(member={userDn}))"),
                new UserRoleEntity(SUPER, "(&(objectClass=groupOfNames)(cn=developers)(member={userDn}))"),
                new UserRoleEntity(USER, "(&(objectClass=groupOfNames)(cn=data-scientists)(member={userDn}))"));

        // Create connection properties for plain LDAP (no TLS for simplicity)
        // Search base is ou=people for finding users, role rules search from baseDn for groups
        final var ldapConnectionProperties = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[] {host}, new int[] {port}),
                TlsMode.NONE,
                null,
                5000, // 5 second connect timeout
                10000, // 10 second response timeout
                1,
                "uid", // uidAttribute
                baseDn, // Search in ou=people for users
                null,
                SearchScope.SUB,
                5,
                ADMIN, // assignedRole (fallback, not used when userRoleRules are provided)
                false,
                ldapSimpleBind,
                userRoleRules);

        // Create test users and groups in LLDAP using the HTTP API
        createTestUsersAndGroups();

        // Create the LdapUsernameRolesProvider
        provider = new LdapUsernameRolesProvider(ldapConnectionProperties, new SecurityLog());
    }

    private static void createTestUsersAndGroups() throws Exception {
        final String baseDn = LLDAP_CONTAINER.getBaseDn();

        // Create users via LDAP (this works in LLDAP)
        createUsersViaLdap(baseDn);

        // Get authentication token from LLDAP HTTP API for group management
        final String token = authenticate();

        // Create groups via GraphQL API (LLDAP doesn't support creating groups via LDAP)
        final int developersGroupId = createGroup(token, "developers");
        final int administratorsGroupId = createGroup(token, "administrators");
        final int dataScientistsGroupId = createGroup(token, "data-scientists");

        // Add users to groups
        // developers: alice, bob
        addUserToGroup(token, ALICE_USERNAME, developersGroupId);
        addUserToGroup(token, BOB_USERNAME, developersGroupId);

        // administrators: bob
        addUserToGroup(token, BOB_USERNAME, administratorsGroupId);

        // data-scientists: don
        addUserToGroup(token, DON_USERNAME, dataScientistsGroupId);
    }

    private static void createUsersViaLdap(final String baseDn) throws Exception {
        final var connectionOptions = new LDAPConnectionOptions();
        connectionOptions.setConnectTimeoutMillis(5000);
        connectionOptions.setResponseTimeoutMillis(10000);

        try (final var connection =
                new LDAPConnection(connectionOptions, LLDAP_CONTAINER.getHost(), LLDAP_CONTAINER.getLdapPort())) {

            // Bind as admin
            final var bindResult = connection.bind(
                    new SimpleBindRequest(LLDAP_CONTAINER.getAdminDn(), LLDAP_CONTAINER.getAdminPassword()));
            assertThat(bindResult.getResultCode())
                    .as("Admin bind should succeed")
                    .isEqualTo(ResultCode.SUCCESS);

            // Create users with passwords

            createUserViaLdap(
                    connection, ALICE_USERNAME, "Alice", "Anderson", "alice@example.com", ALICE_PASSWORD, baseDn);
            createUserViaLdap(connection, BOB_USERNAME, "Bob", "Brown", "bob@example.com", BOB_PASSWORD, baseDn);
            createUserViaLdap(
                    connection, CHARLIE_USERNAME, "Charlie", "Chen", "charlie@example.com", CHARLIE_PASSWORD, baseDn);
            createUserViaLdap(connection, DON_USERNAME, "Don", "Duncan", "don@example.com", DON_PASSWORD, baseDn);
        }
    }

    private static void createUserViaLdap(
            final LDAPConnection connection,
            final String uid,
            final String firstName,
            final String lastName,
            final String email,
            final String password,
            final String baseDn)
            throws Exception {

        final String userDn = "uid=" + uid + ",ou=people," + baseDn;

        // Add user entry
        final AddRequest addRequest = new AddRequest(
                userDn,
                new Attribute("objectClass", "inetOrgPerson", "posixAccount"),
                new Attribute("uid", uid),
                new Attribute("cn", firstName + " " + lastName),
                new Attribute("sn", lastName),
                new Attribute("givenName", firstName),
                new Attribute("mail", email),
                new Attribute("uidNumber", String.valueOf(1000 + uid.hashCode() % 1000)),
                new Attribute("gidNumber", "1000"),
                new Attribute("homeDirectory", "/home/" + uid));
        final var addResult = connection.add(addRequest);
        assertThat(addResult.getResultCode())
                .as("Adding user " + uid + " should succeed")
                .isEqualTo(ResultCode.SUCCESS);

        // Set password
        final ModifyRequest modifyRequest =
                new ModifyRequest(userDn, new Modification(ModificationType.REPLACE, "userPassword", password));
        final var modifyResult = connection.modify(modifyRequest);
        assertThat(modifyResult.getResultCode())
                .as("Setting password for " + uid + " should succeed")
                .isEqualTo(ResultCode.SUCCESS);
    }

    private static String authenticate() throws Exception {
        final String baseUrl = "http://" + LLDAP_CONTAINER.getHost() + ":" + LLDAP_CONTAINER.getHttpPort();
        final String loginUrl = baseUrl + "/auth/simple/login";

        final String loginBody = OBJECT_MAPPER.writeValueAsString(
                new LoginRequest(LLDAP_CONTAINER.getAdminUsername(), LLDAP_CONTAINER.getAdminPassword()));

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(loginUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(loginBody))
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("Login should succeed").isEqualTo(200);

        final JsonNode jsonResponse = OBJECT_MAPPER.readTree(response.body());
        return jsonResponse.get("token").asText();
    }

    private static int createGroup(final String token, final String groupName) throws Exception {
        final String graphqlQuery = """
                mutation CreateGroup($name: String!) {
                    createGroup(name: $name) {
                        id
                    }
                }
                """;

        final String variables = String.format("{\"name\": \"%s\"}", groupName);

        final String responseBody = executeGraphQL(token, graphqlQuery, variables);
        final JsonNode jsonResponse = OBJECT_MAPPER.readTree(responseBody);
        return jsonResponse.get("data").get("createGroup").get("id").asInt();
    }

    private static void addUserToGroup(final String token, final String userId, final int groupId) throws Exception {
        final String graphqlQuery = """
                mutation AddUserToGroup($userId: String!, $groupId: Int!) {
                    addUserToGroup(userId: $userId, groupId: $groupId) {
                        ok
                    }
                }
                """;

        final String variables = String.format("{\"userId\": \"%s\", \"groupId\": %d}", userId, groupId);

        executeGraphQL(token, graphqlQuery, variables);
    }

    private static String executeGraphQL(final String token, final String query, final String variables)
            throws Exception {
        final String baseUrl = "http://" + LLDAP_CONTAINER.getHost() + ":" + LLDAP_CONTAINER.getHttpPort();
        final String graphqlUrl = baseUrl + "/api/graphql";

        final String requestBody =
                String.format("{\"query\": %s, \"variables\": %s}", OBJECT_MAPPER.writeValueAsString(query), variables);

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(graphqlUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode())
                .as("GraphQL request should succeed: " + response.body())
                .isEqualTo(200);

        return response.body();
    }

    // Helper record for JSON serialization
    private record LoginRequest(String username, String password) {}

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
