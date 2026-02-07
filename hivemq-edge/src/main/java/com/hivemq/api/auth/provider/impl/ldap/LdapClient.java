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

import com.google.common.collect.ImmutableList;
import com.hivemq.configuration.entity.api.ldap.UserRoleEntity;
import com.hivemq.logging.SecurityLog;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.DereferencePolicy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PruneUnneededConnectionsLDAPConnectionPoolHealthCheck;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.StartTLSPostConnectProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;

/**
 * LDAP client that manages connections and provides authentication operations.
 * <p>
 * This class wraps the UnboundID LDAP SDK and provides a simplified API for LDAP operations
 * with proper lifecycle management and connection pooling.
 * <p>
 * Usage:
 * <pre>{@code
 * LdapClient client = new LdapClient(connectionProperties);
 * client.start();
 * try {
 *     boolean authenticated = client.bindUser(userDn, password);
 *     // ... use client
 * } finally {
 *     client.stop();
 * }
 * }</pre>
 */
public class LdapClient {

    private static final @NotNull Logger log = LoggerFactory.getLogger(LdapClient.class);

    private final @NotNull LdapConnectionProperties connectionProperties;
    private final @NotNull SecurityLog securityLog;
    private volatile UserDnResolver userDnResolver;
    private volatile LDAPConnectionPool connectionPool;
    private volatile SimpleBindRequest adminBindRequest;
    private volatile boolean started = false;

    /**
     * Creates a new LDAP client with the specified connection properties.
     * <p>
     * Note: The DN resolver is created lazily during {@link #start()} to ensure
     * the connection pool is available for Directory Descent (search-based) resolution.
     *
     * @param connectionProperties The connection configuration
     */
    public LdapClient(final @NotNull LdapConnectionProperties connectionProperties,
                      final @NotNull SecurityLog securityLog) {
        this.connectionProperties = connectionProperties;
        this.securityLog = securityLog;
        // Resolver will be created after connection pool is established in start()
        this.userDnResolver = null;
    }

    /**
     * Starts the LDAP client and initializes the connection pool.
     *
     * @throws LDAPException            if the connection pool cannot be created
     * @throws GeneralSecurityException if there's an SSL/TLS configuration issue
     * @throws IllegalStateException    if the client is already started
     */
    public synchronized void start() throws LDAPException, GeneralSecurityException {
        if (started) {
            throw new IllegalStateException("LDAP client is already started");
        }

        log.debug("Starting LDAP client, connecting to {}:{}",
                connectionProperties.servers().hosts()[0], connectionProperties.servers().ports()[0]);

        final var connectionOptions = connectionProperties.createConnectionOptions();

        SocketFactory socketFactory = null;
        StartTLSPostConnectProcessor startTlsProcessor = null;
        switch (connectionProperties.tlsMode()) {
            case NONE -> {} //NOOP
            case LDAPS -> {
                final SSLContext sslContext = connectionProperties.createSSLContext();
                socketFactory = sslContext.getSocketFactory();
            }
            case START_TLS -> {
                final SSLContext sslContext = connectionProperties.createSSLContext();
                startTlsProcessor = new StartTLSPostConnectProcessor(sslContext);
            }
        }

        final var simpleBindEntity = connectionProperties.ldapSimpleBind();
        final var baseDn = new DN(connectionProperties.rdns());


        final var bindDn = new DN(ImmutableList.<RDN>builder()
                .add(new DN(simpleBindEntity.rdns()).getRDNs())
                .add(baseDn.getRDNs())
                .build());

        final var bindRequest = new SimpleBindRequest(bindDn, simpleBindEntity.userPassword());
        // Store admin bind request for re-authenticating connections after user authentication
        this.adminBindRequest = bindRequest;

        final int maxConnections = connectionProperties.maxConnections();
        final int minConnections = Math.min(1, maxConnections);

        final var serverSet = new RoundRobinServerSet(
                connectionProperties.servers().hosts(),
                connectionProperties.servers().ports(),
                socketFactory,
                connectionOptions,
                bindRequest,
                startTlsProcessor);

        final var ldapConnectionPoolHealthCheck =
                new PruneUnneededConnectionsLDAPConnectionPoolHealthCheck(
                        minConnections, 1_000L); //TODO configurable??

        try {
            connectionPool = new LDAPConnectionPool( //
                    serverSet,
                    bindRequest,
                    minConnections,
                    maxConnections,
                    minConnections,
                    null,
                    false,
                    ldapConnectionPoolHealthCheck);

            // Create the DN resolver now that the connection pool is available
            // This is required for Directory Descent (search-based) resolution
            userDnResolver = connectionProperties.createUserDnResolver(connectionPool);

            started = true;
            log.info("LDAP client started successfully, connected to {}:{}",
                    connectionProperties.servers().hosts()[0], connectionProperties.servers().ports()[0]);
        } catch (final Exception e) {
            // Close the connection if pool creation fails
            if(connectionPool != null) {
                connectionPool.close();
            }
            throw e;
        }
    }

    /**
     * Stops the LDAP client and closes all connections in the pool.
     *
     * @throws IllegalStateException if the client is not started
     */
    @TestOnly
    public synchronized void stop() {
        log.debug("Stopping LDAP client");

        if (connectionPool != null) {
            connectionPool.close();
            connectionPool = null;
        }

        userDnResolver = null;
        adminBindRequest = null;
        started = false;
        log.info("LDAP client stopped successfully");
    }

    /**
     * Authenticates a user by performing an LDAP bind operation.
     * <p>
     * <strong>Important:</strong> After testing the user's credentials, this method re-authenticates
     * the connection as admin before returning it to the pool. This ensures that subsequent operations
     * requiring admin privileges (like DN resolution searches) work correctly.
     *
     * @param userDn   The user's Distinguished Name
     * @param password The user's password
     * @return {@code true} if authentication was successful, {@code false} otherwise
     * @throws LDAPException            if there's an LDAP protocol error (not authentication failure)
     * @throws IllegalStateException    if the client is not started
     */
    public boolean bindUser(final @NotNull String userDn, final byte @NotNull [] password) throws LDAPException {
        ensureStarted();

        log.debug("Attempting to bind user: {}", userDn);

        try {
            final var bindRequest = new SimpleBindRequest(userDn, password);
            final var bindResult = connectionPool.bindAndRevertAuthentication(bindRequest);

            final boolean success = bindResult.getResultCode() == ResultCode.SUCCESS;
            if (success) {
                log.debug("User bind successful: {}", userDn);
            } else {
                log.debug("User bind failed: {}, result code: {}", userDn, bindResult.getResultCode());
            }

            return success;
        } catch (final LDAPException e) {
            // INVALID_CREDENTIALS is expected for wrong password, return false
            if (e.getResultCode() == ResultCode.INVALID_CREDENTIALS) {
                log.debug("User bind failed due to invalid credentials: {}", userDn);
                return false;
            }
            // Other errors are unexpected, throw them
            log.error("LDAP error during bind operation for user {}: {}", userDn, e.getMessage());
            throw e;
        }
    }

    /**
     * Authenticates a user by username, resolving the DN using the configured DN resolver.
     * <p>
     * This is the recommended method for authentication. The username will be resolved to a full DN
     * using the configured DN template (e.g., "uid={username},ou=people,dc=example,dc=com").
     *
     * @param username The username to authenticate (e.g., "jdoe")
     * @param password The user's password
     * @return {@code true} if authentication was successful, {@code false} otherwise
     * @throws LDAPException            if there's an LDAP protocol error (not authentication failure)
     * @throws IllegalStateException    if the client is not started
     */
    public boolean authenticateUser(final @NotNull String username, final byte @NotNull [] password)
            throws LDAPException {
        try {
            final var resolved = userDnResolver.resolveDn(username);
            final var bindResult = bindUser(resolved, password);
            if (bindResult) {
                securityLog.authenticationSucceeded("LDAP", username);
            } else {
                securityLog.authenticationFailed("LDAP", username, "Wrong password");
            }
            return bindResult;
        } catch (final SearchFilterDnResolver.DnResolutionException e) {
            securityLog.authenticationFailed("LDAP", username, "Failed to resolve DN for user");
            throw e;
        }

    }

    /**
     * Executes LDAP role queries for a user and returns the set of roles for which queries succeed.
     * <p>
     * Each user-role query is executed with the {userDn} placeholder replaced by the user's actual DN.
     * If a query returns one or more results, the corresponding role is included in the result set.
     *
     * @param username  The username to resolve and query roles for
     * @param userRoles The list of user-role configurations to execute
     * @return A set of role names for which the LDAP queries returned results (empty if no matches or null input)
     * @throws LDAPException         if there's an LDAP protocol error during DN resolution
     * @throws IllegalStateException if the client is not started
     */
    public @NotNull Set<String> getRolesForUser(
            final @NotNull String username,
            final @Nullable List<UserRoleEntity> userRoles)
            throws LDAPException {
        ensureStarted();
        if (userRoles == null || userRoles.isEmpty()) {
            log.debug("No user roles configured, returning empty set");
            return Set.of();
        }
        // Resolve username to DN
        final String userDn;
        try {
            userDn = userDnResolver.resolveDn(username);
            log.debug("Resolved username '{}' to DN: {}", username, userDn);
        } catch (final SearchFilterDnResolver.DnResolutionException e) {
            log.error("Failed to resolve DN for user '{}': {}", username, e.getMessage());
            throw e;
        }
        final Set<String> matchedRoles = new HashSet<>();
        // Execute each role query
        for (final var userRole : userRoles) {
            final String role = userRole.getRole();
            final String queryTemplate = userRole.getQuery();
            try {
                // Substitute {userDn} placeholder in query
                final String query = queryTemplate.replace("{userDn}", userDn);
                log.debug("Executing role query for role '{}': {}", role, query);

                // Parse the query as an LDAP filter
                final var filter = Filter.create(query);

                // Create search request
                // We search from the base DN with subtree scope to find any matching entries
                final var searchRequest = new SearchRequest(
                        connectionProperties.rdns(),
                        SearchScope.SUB,
                        DereferencePolicy.NEVER,
                        1, // Size limit - we only need to know if there's at least one match
                        connectionProperties.searchTimeoutSeconds(),
                        false, // Types only = false
                        filter,
                        "1.1" // Return no attributes, we only need to know if entries exist
                );

                final var searchResult = connectionPool.search(searchRequest);
                if (searchResult.getResultCode() == ResultCode.SUCCESS &&
                        searchResult.getEntryCount() > 0) {
                    log.debug("Role query for '{}' matched {} entries, adding role", role, searchResult.getEntryCount());
                    matchedRoles.add(role);
                } else {
                    log.debug("Role query for '{}' returned no matches", role);
                }
            } catch (final LDAPException e) {
                // Log error but continue with other queries
                log.error("LDAP error executing role query for role '{}': {}", role, e.getMessage(), e);
            } catch (final Exception e) {
                // Catch any other exceptions (e.g., invalid filter syntax)
                log.error("Error executing role query for role '{}': {}", role, e.getMessage(), e);
            }
        }
        log.debug("User '{}' matched {} roles: {}", username, matchedRoles.size(), matchedRoles);
        return matchedRoles;
    }

    /**
     * Checks if the LDAP client is currently started.
     *
     * @return {@code true} if the client is started, {@code false} otherwise
     */
    public boolean isStarted() {
        return started;
    }

    /**
     * Ensures that the client is started, throwing an exception if not.
     *
     * @throws IllegalStateException if the client is not started
     */
    private void ensureStarted() {
        if (!started) {
            throw new IllegalStateException("LDAP client is not started. Call start() first.");
        }
    }
}
