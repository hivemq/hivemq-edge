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

import com.hivemq.configuration.entity.api.ldap.LdapAuthenticationEntity;
import com.hivemq.configuration.entity.api.ldap.LdapServerEntity;
import com.hivemq.configuration.entity.api.ldap.LdapSimpleBindEntity;
import com.hivemq.configuration.entity.api.ldap.TrustStoreEntity;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.util.List;

import static java.util.Arrays.stream;

/**
 * Record representing LDAP connection properties.
 * <p>
 * Encapsulates all the connection details needed to connect to an LDAP server,
 * including TLS configuration, timeouts, truststore information, and DN resolution.
 *
 * @param servers                        List of LDapServers to connect to
 * @param tlsMode                        The TLS/SSL mode to use
 * @param trustStore                     An optional truststore for connecting to an LDAP server
 * @param connectTimeoutMillis           Connection timeout in milliseconds (0 = use default)
 * @param responseTimeoutMillis          Response timeout in milliseconds (0 = use default)
 * @param maxConnections                 Maximum number of connections in the connection pool
 * @param userDnTemplate                 The DN template for resolving usernames to DNs (e.g., "uid={username},ou=people,{baseDn}")
 * @param baseDn                         The base DN of the LDAP directory (e.g., "dc=example,dc=com")
 * @param acceptAnyCertificateForTesting <strong>⚠️ TEST ONLY</strong> - When true, disables all certificate validation
 *                                       and accepts any certificate including self-signed and expired certificates.
 *                                       <strong>NEVER use in production!</strong> Only for integration tests with
 *                                       testcontainers. Default: false
 */
public record LdapConnectionProperties(
        @NotNull LdapServers servers,
        @NotNull TlsMode tlsMode,
        @Nullable TrustStore trustStore,
        int connectTimeoutMillis,
        int responseTimeoutMillis,
        int maxConnections,
        @NotNull String userDnTemplate,
        @NotNull String baseDn,
        @NotNull String assignedRole,
        boolean acceptAnyCertificateForTesting,
        @NotNull LdapSimpleBind ldapSimpleBind) {

    /**
     * This class represents the simple bind credentials for an LDAP connection.
     */
    public record LdapSimpleBind (@NotNull String rdns, @NotNull String userPassword){
        public static LdapSimpleBind fromEntity(final @NotNull LdapSimpleBindEntity ldapSimpleBindEntity) {
            return new LdapSimpleBind(
                    ldapSimpleBindEntity.getRdns(),
                    ldapSimpleBindEntity.getUserPassword());
        }
    }

    /**
     * This class represents the simple bind credentials for an LDAP connection.
     */
    public record LdapServers (@NotNull String[] hosts, int @NotNull [] ports){
        public static LdapServers fromEntity(final @NotNull List<LdapServerEntity> ldapServerEntities) {
            final String[] hosts = ldapServerEntities.stream().map(LdapServerEntity::getHost).toArray(String[]::new);
            final int[] ports = ldapServerEntities.stream().mapToInt(LdapServerEntity::getPort).toArray();
            return new LdapServers(hosts, ports);
        }
    }

    /**
     * This class represents the simple bind credentials for an LDAP connection.
     */
    public record TrustStore (@NotNull String trustStorePath, @Nullable String trustStorePassword, @Nullable String trustStoreType){
        public static TrustStore fromEntity(final @NotNull TrustStoreEntity trustStoreEntity) {
            return new TrustStore(
                    trustStoreEntity.getTrustStorePath(),
                    trustStoreEntity.getTrustStorePassword(),
                    trustStoreEntity.getTrustStoreType());
        }
    }

    /**
     * Creates connection properties with default timeouts and secure certificate validation.
     *
     * @param servers            List of LDapServers to connect to
     * @param tlsMode            The TLS/SSL mode to use
     * @param trustStore         An optional truststore for connecting to an LDAP server
     * @param userDnTemplate     The DN template for resolving usernames
     * @param baseDn             The base DN of the LDAP directory
     */
    public LdapConnectionProperties(
            final @NotNull LdapServers servers,
            final @NotNull TlsMode tlsMode,
            final @Nullable TrustStore trustStore,
            final @NotNull String userDnTemplate,
            final @NotNull String baseDn,
            final @NotNull String assignedRole,
            final @NotNull LdapSimpleBind ldapSimpleBind) {
        this(servers, tlsMode, trustStore, 0, 0, 10, userDnTemplate, baseDn, assignedRole, false, ldapSimpleBind);
    }

    /**
     * Creates connection properties with explicit timeouts and secure certificate validation.
     *
     * @param servers               List of LDapServers to connect to
     * @param tlsMode               The TLS/SSL mode to use
     * @param trustStore            An optional truststore for connecting to an LDAP server
     * @param connectTimeoutMillis  Connection timeout in milliseconds (0 = use default)
     * @param responseTimeoutMillis Response timeout in milliseconds (0 = use default)
     * @param userDnTemplate        The DN template for resolving usernames
     * @param baseDn                The base DN of the LDAP directory
     * @param assignedRole                The base DN of the LDAP directory
     */
    public LdapConnectionProperties(
            final @NotNull LdapServers servers,
            final @NotNull TlsMode tlsMode,
            final @Nullable TrustStore trustStore,
            final int connectTimeoutMillis,
            final int responseTimeoutMillis,
            final int maxConnections,
            final @NotNull String userDnTemplate,
            final @NotNull String baseDn,
            final @NotNull String assignedRole,
            final @NotNull LdapSimpleBind ldapSimpleBind) {
        this(servers, tlsMode, trustStore, connectTimeoutMillis, responseTimeoutMillis, maxConnections, userDnTemplate, baseDn, assignedRole, false, ldapSimpleBind);
    }

    /**
     * Creates LdapConnectionProperties from XML configuration entity.
     * <p>
     * This factory method converts an {@link LdapAuthenticationEntity}
     * (loaded from config.xml) into runtime connection properties.
     *
     * @param entity The LDAP authentication configuration from XML
     * @return Configured LdapConnectionProperties for runtime use
     * @throws IllegalArgumentException if the entity configuration is invalid
     */
    public static @NotNull LdapConnectionProperties fromEntity(
            final @NotNull LdapAuthenticationEntity entity) {
        // Parse TLS mode from string
        final TlsMode tlsMode;
        try {
            tlsMode = TlsMode.valueOf(entity.getTlsMode().toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid TLS mode: " + entity.getTlsMode() + ". Must be one of: NONE, LDAPS, START_TLS", e);
        }

        return new LdapConnectionProperties(
                LdapServers.fromEntity(entity.getServers()),
                tlsMode,
                entity.getTrustStore() != null ? TrustStore.fromEntity(entity.getTrustStore()) : null,
                entity.getConnectTimeoutMillis(),
                entity.getResponseTimeoutMillis(), entity.getMaxConnections(),
                entity.getUserDnTemplate(),
                entity.getBaseDn(),
                entity.getAssignedRole(),
                false,  // Never allow test-only certificate acceptance from XML config
                LdapSimpleBind.fromEntity(entity.getSimpleBindEntity())
        );
    }

    /**
     * Validates the connection properties.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public LdapConnectionProperties {
        stream(servers.ports()).forEach(port -> {
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
            }
        });
        if (connectTimeoutMillis < 0) {
            throw new IllegalArgumentException("Connect timeout cannot be negative: " + connectTimeoutMillis);
        }
        if (responseTimeoutMillis < 0) {
            throw new IllegalArgumentException("Response timeout cannot be negative: " + responseTimeoutMillis);
        }
        if (userDnTemplate.isBlank()) {
            throw new IllegalArgumentException("User DN template cannot be empty");
        }
        if (!userDnTemplate.contains("{username}")) {
            throw new IllegalArgumentException("User DN template must contain {username} placeholder");
        }
        if (baseDn.isBlank()) {
            throw new IllegalArgumentException("Base DN cannot be empty");
        }
        if (assignedRole.isBlank()) {
            throw new IllegalArgumentException("Assigned Role cannot be empty");
        }
    }

    /**
     * Creates a DN resolver using the configured template and base DN.
     *
     * @return A UserDnResolver configured with the template from this properties object
     */
    public @NotNull UserDnResolver createUserDnResolver() {
        return new TemplateDnResolver(userDnTemplate, baseDn);
    }

    /**
     * Creates an SSLContext from the truststore configuration.
     * <p>
     * Certificate validation behavior depends on configuration:
     * <ul>
     *     <li><strong>acceptAnyCertificateForTesting = true:</strong> ⚠️ Disables ALL certificate validation.
     *         Accepts any certificate including self-signed, expired, or invalid certificates.
     *         <strong>NEVER use in production!</strong> Only for integration tests.</li>
     *     <li><strong>trustStorePath = null:</strong> Uses system's default CA certificates
     *         (e.g., Let's Encrypt, DigiCert). Suitable for production with properly signed certificates.</li>
     *     <li><strong>trustStorePath provided:</strong> Uses custom truststore.
     *         Useful for self-signed certificates or internal CAs in production.</li>
     * </ul>
     *
     * @return A configured SSLContext
     * @throws GeneralSecurityException if there's an issue creating the SSLContext
     * @throws IllegalStateException    if called when TLS mode is NONE
     */
    public @NotNull SSLContext createSSLContext() throws GeneralSecurityException {
        if (tlsMode.equals(TlsMode.NONE)) {
            throw new IllegalStateException("SSLContext is not needed for TLS mode: " + tlsMode);
        }

        // TEST ONLY: Accept any certificate without validation
        if (acceptAnyCertificateForTesting) {
            // WARNING: This disables certificate validation - only for testing!
            // Configure SSLUtil to accept any certificate and hostname
            final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            // Set default SSLContext protocol to work with OpenLDAP container
            return sslUtil.createSSLContext("TLS");
        }

        if (trustStore() == null || trustStore().trustStorePath().isBlank()) {
            // Use system default CA certificates (Java's default truststore)
            final SSLUtil sslUtil = new SSLUtil();
            return sslUtil.createSSLContext();
        }

        // Use custom truststore for self-signed certificates or internal CAs
        final SSLUtil sslUtil = new SSLUtil(new TrustStoreTrustManager(
                trustStore().trustStorePath(),
                trustStore().trustStorePassword() != null ? trustStore().trustStorePassword().toCharArray() : null,
                trustStore().trustStoreType(),
                true));
        return sslUtil.createSSLContext();
    }

    /**
     * Creates connection options with configured timeouts.
     *
     * @return Configured LDAPConnectionOptions
     */
    public @NotNull LDAPConnectionOptions createConnectionOptions() {
        final LDAPConnectionOptions options = new LDAPConnectionOptions();

        if (connectTimeoutMillis() > 0) {
            options.setConnectTimeoutMillis(connectTimeoutMillis());
        }

        if (connectTimeoutMillis() > 0) {
            options.setResponseTimeoutMillis(connectTimeoutMillis());
        }

        if (responseTimeoutMillis() > 0) {
            options.setResponseTimeoutMillis(responseTimeoutMillis());
        }

        return options;
    }
}
