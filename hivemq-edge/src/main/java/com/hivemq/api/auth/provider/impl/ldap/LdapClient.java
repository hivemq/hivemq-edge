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

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private final @NotNull UserDnResolver userDnResolver;
    private volatile LDAPConnectionPool connectionPool;
    private volatile boolean started = false;

    /**
     * Creates a new LDAP client with the specified connection properties.
     *
     * @param connectionProperties The connection configuration
     */
    public LdapClient(final @NotNull LdapConnectionProperties connectionProperties) {
        this.connectionProperties = connectionProperties;
        this.userDnResolver = connectionProperties.createUserDnResolver();
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
                connectionProperties.host(), connectionProperties.port());

        // Create initial connection
        final LDAPConnection connection = connectionProperties.createConnection();

        try {
            // Create connection pool with initial connection
            // Pool size: initial=1, max=10 (can be made configurable later)
            connectionPool = new LDAPConnectionPool(connection, 1, 10);
            started = true;
            log.info("LDAP client started successfully, connected to {}:{}",
                    connectionProperties.host(), connectionProperties.port());
        } catch (final LDAPException e) {
            // Close the connection if pool creation fails
            connection.close();
            throw e;
        }
    }

    /**
     * Stops the LDAP client and closes all connections in the pool.
     *
     * @throws IllegalStateException if the client is not started
     */
    public synchronized void stop() {
        if (!started) {
            throw new IllegalStateException("LDAP client is not started");
        }

        log.debug("Stopping LDAP client");

        if (connectionPool != null) {
            connectionPool.close();
            connectionPool = null;
        }

        started = false;
        log.info("LDAP client stopped successfully");
    }

    /**
     * Authenticates a user by performing an LDAP bind operation.
     *
     * @param userDn   The user's Distinguished Name
     * @param password The user's password
     * @return {@code true} if authentication was successful, {@code false} otherwise
     * @throws LDAPException            if there's an LDAP protocol error (not authentication failure)
     * @throws IllegalStateException    if the client is not started
     */
    public boolean bindUser(final @NotNull String userDn, final @NotNull String password) throws LDAPException {
        ensureStarted();

        log.debug("Attempting to bind user: {}", userDn);

        LDAPConnection connection = null;
        try {
            connection = connectionPool.getConnection();
            final BindRequest bindRequest = new SimpleBindRequest(userDn, password);
            final BindResult bindResult = connection.bind(bindRequest);

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
        } finally {
            if (connection != null) {
                connectionPool.releaseConnection(connection);
            }
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
    public boolean authenticateUser(final @NotNull String username, final @NotNull String password)
            throws LDAPException {
        final String userDn = userDnResolver.resolveDn(username);
        return bindUser(userDn, password);
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
