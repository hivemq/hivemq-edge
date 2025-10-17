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

import com.hivemq.configuration.entity.api.LdapAuthenticationEntity;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;

/**
 * Record representing LDAP connection properties.
 * <p>
 * Encapsulates all the connection details needed to connect to an LDAP server,
 * including TLS configuration, timeouts, truststore information, and DN resolution.
 *
 * @param host                           The LDAP server hostname
 * @param port                           The LDAP server port
 * @param tlsMode                        The TLS/SSL mode to use
 * @param trustStorePath                 The path to the truststore file (null to use system default CA certificates for TLS)
 * @param trustStorePassword             The password for the truststore (null if not needed)
 * @param trustStoreType                 The type of the truststore (e.g., "JKS", "PKCS12", null if not needed)
 * @param connectTimeoutMillis           Connection timeout in milliseconds (0 = use default)
 * @param responseTimeoutMillis          Response timeout in milliseconds (0 = use default)
 * @param userDnTemplate                 The DN template for resolving usernames to DNs (e.g., "uid={username},ou=people,{baseDn}")
 * @param baseDn                         The base DN of the LDAP directory (e.g., "dc=example,dc=com")
 * @param acceptAnyCertificateForTesting <strong>⚠️ TEST ONLY</strong> - When true, disables all certificate validation
 *                                       and accepts any certificate including self-signed and expired certificates.
 *                                       <strong>NEVER use in production!</strong> Only for integration tests with
 *                                       testcontainers. Default: false
 */
public record LdapConnectionProperties(
        @NotNull String host,
        int port,
        @NotNull TlsMode tlsMode,
        @Nullable String trustStorePath,
        @Nullable String trustStorePassword,
        @Nullable String trustStoreType,
        int connectTimeoutMillis,
        int responseTimeoutMillis,
        @NotNull String userDnTemplate,
        @NotNull String baseDn,
        @NotNull String assignedRole,
        boolean acceptAnyCertificateForTesting) {

    /**
     * Creates connection properties with default timeouts and secure certificate validation.
     *
     * @param host               The LDAP server hostname
     * @param port               The LDAP server port
     * @param tlsMode            The TLS/SSL mode to use
     * @param trustStorePath     The path to the truststore file
     * @param trustStorePassword The password for the truststore
     * @param trustStoreType     The type of the truststore
     * @param userDnTemplate     The DN template for resolving usernames
     * @param baseDn             The base DN of the LDAP directory
     */
    public LdapConnectionProperties(
            final @NotNull String host,
            final int port,
            final @NotNull TlsMode tlsMode,
            final @Nullable String trustStorePath,
            final @Nullable String trustStorePassword,
            final @Nullable String trustStoreType,
            final @NotNull String userDnTemplate,
            final @NotNull String baseDn,
            final @NotNull String assignedRole) {
        this(host, port, tlsMode, trustStorePath, trustStorePassword, trustStoreType, 0, 0, userDnTemplate, baseDn, assignedRole, false);
    }

    /**
     * Creates connection properties with explicit timeouts and secure certificate validation.
     *
     * @param host                  The LDAP server hostname
     * @param port                  The LDAP server port
     * @param tlsMode               The TLS/SSL mode to use
     * @param trustStorePath        The path to the truststore file
     * @param trustStorePassword    The password for the truststore
     * @param trustStoreType        The type of the truststore
     * @param connectTimeoutMillis  Connection timeout in milliseconds (0 = use default)
     * @param responseTimeoutMillis Response timeout in milliseconds (0 = use default)
     * @param userDnTemplate        The DN template for resolving usernames
     * @param baseDn                The base DN of the LDAP directory
     * @param assignedRole                The base DN of the LDAP directory
     */
    public LdapConnectionProperties(
            final @NotNull String host,
            final int port,
            final @NotNull TlsMode tlsMode,
            final @Nullable String trustStorePath,
            final @Nullable String trustStorePassword,
            final @Nullable String trustStoreType,
            final int connectTimeoutMillis,
            final int responseTimeoutMillis,
            final @NotNull String userDnTemplate,
            final @NotNull String baseDn,
            final @NotNull String assignedRole) {
        this(host, port, tlsMode, trustStorePath, trustStorePassword, trustStoreType, connectTimeoutMillis, responseTimeoutMillis, userDnTemplate, baseDn, assignedRole, false);
    }

    /**
     * Creates LdapConnectionProperties from XML configuration entity.
     * <p>
     * This factory method converts an {@link com.hivemq.configuration.entity.api.LdapAuthenticationEntity}
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

        // Determine port: use configured port, or default based on TLS mode if 0
        final int port = entity.getPort() > 0 ? entity.getPort() : tlsMode.defaultPort;

        // Extract TLS configuration if present
        final String trustStorePath;
        final String trustStorePassword;
        final String trustStoreType;
        if (entity.getTls() != null) {
            trustStorePath = entity.getTls().getTrustStorePath();
            trustStorePassword = entity.getTls().getTrustStorePassword();
            trustStoreType = entity.getTls().getTrustStoreType();
        } else {
            trustStorePath = null;
            trustStorePassword = null;
            trustStoreType = null;
        }

        return new LdapConnectionProperties(
                entity.getHost(),
                port,
                tlsMode,
                trustStorePath,
                trustStorePassword,
                trustStoreType,
                entity.getConnectTimeoutMillis(),
                entity.getResponseTimeoutMillis(),
                entity.getUserDnTemplate(),
                entity.getBaseDn(),
                entity.getAssignedRole(),
                false  // Never allow test-only certificate acceptance from XML config
        );
    }

    /**
     * Validates the connection properties.
     *
     * @throws IllegalArgumentException if the configuration is invalid
     */
    public LdapConnectionProperties {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535, got: " + port);
        }
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

        if (trustStorePath == null) {
            // Use system default CA certificates (Java's default truststore)
            final SSLUtil sslUtil = new SSLUtil();
            return sslUtil.createSSLContext();
        }

        // Use custom truststore for self-signed certificates or internal CAs
        final SSLUtil sslUtil = new SSLUtil(new TrustStoreTrustManager(
                trustStorePath,
                trustStorePassword != null ? trustStorePassword.toCharArray() : null,
                trustStoreType,
                true));
        return sslUtil.createSSLContext();
    }

    /**
     * Creates connection options with configured timeouts.
     *
     * @return Configured LDAPConnectionOptions
     */
    private @NotNull LDAPConnectionOptions createConnectionOptions() {
        final LDAPConnectionOptions options = new LDAPConnectionOptions();

        if (connectTimeoutMillis > 0) {
            options.setConnectTimeoutMillis(connectTimeoutMillis);
        }

        if (responseTimeoutMillis > 0) {
            options.setResponseTimeoutMillis(responseTimeoutMillis);
        }

        return options;
    }

    /**
     * Creates a new LDAP connection using the configured properties.
     * <p>
     * The connection type depends on the configured TLS mode:
     * <ul>
     *     <li>NONE: Plain LDAP connection without encryption</li>
     *     <li>LDAPS: TLS connection established from the start</li>
     *     <li>START_TLS: Plain connection upgraded to TLS using StartTLS</li>
     * </ul>
     *
     * @return A new LDAPConnection instance
     * @throws LDAPException            if the connection fails
     * @throws GeneralSecurityException if there's an SSL/TLS issue
     */
    public @NotNull LDAPConnection createConnection() throws LDAPException, GeneralSecurityException {
        final LDAPConnectionOptions connectionOptions = createConnectionOptions();

        return switch (tlsMode) {
            case NONE -> createPlainConnection(connectionOptions);
            case LDAPS -> createLdapsConnection(connectionOptions);
            case START_TLS -> createStartTlsConnection(connectionOptions);
        };
    }

    /**
     * Creates a plain LDAP connection without encryption.
     */
    private @NotNull LDAPConnection createPlainConnection(final @NotNull LDAPConnectionOptions options)
            throws LDAPException {
        return new LDAPConnection(options, host, port);
    }

    /**
     * Creates an LDAPS connection (TLS from start).
     */
    private @NotNull LDAPConnection createLdapsConnection(final @NotNull LDAPConnectionOptions options)
            throws LDAPException, GeneralSecurityException {
        final SSLContext sslContext = createSSLContext();
        return new LDAPConnection(sslContext.getSocketFactory(), options, host, port);
    }

    /**
     * Creates a connection and upgrades it to TLS using StartTLS.
     */
    private @NotNull LDAPConnection createStartTlsConnection(final @NotNull LDAPConnectionOptions options)
            throws LDAPException, GeneralSecurityException {
        // First create plain connection
        final LDAPConnection connection = new LDAPConnection(options, host, port);

        try {
            // Upgrade to TLS using StartTLS extended operation
            final SSLContext sslContext = createSSLContext();
            final StartTLSExtendedRequest startTLSRequest = new StartTLSExtendedRequest(sslContext);
            final ExtendedResult startTLSResult = connection.processExtendedOperation(startTLSRequest);

            if (startTLSResult.getResultCode() != ResultCode.SUCCESS) {
                throw new LDAPException(startTLSResult.getResultCode(),
                        "StartTLS failed: " + startTLSResult.getDiagnosticMessage());
            }

            return connection;
        } catch (final Exception e) {
            // Close the connection if StartTLS fails
            connection.close();
            throw e;
        }
    }
}
