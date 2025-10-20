package com.hivemq.api.auth.provider.impl.ldap.testcontainer;

import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import com.hivemq.api.auth.provider.impl.ldap.TlsMode;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import com.unboundid.util.ssl.TrustStoreTrustManager;
import org.jetbrains.annotations.NotNull;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapTestConnection {
    public static final String TEST_USERNAME = "test";
    public static final String TEST_PASSWORD = "test";

    private final LdapConnectionProperties ldapConnectionProperties;

    public LdapTestConnection(final LdapConnectionProperties ldapConnectionProperties) {
        this.ldapConnectionProperties = ldapConnectionProperties;
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
        if (ldapConnectionProperties.tlsMode().equals(TlsMode.NONE)) {
            throw new IllegalStateException("SSLContext is not needed for TLS mode: " + ldapConnectionProperties.tlsMode());
        }

        // TEST ONLY: Accept any certificate without validation
        if (ldapConnectionProperties.acceptAnyCertificateForTesting()) {
            // WARNING: This disables certificate validation - only for testing!
            // Configure SSLUtil to accept any certificate and hostname
            final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            // Set default SSLContext protocol to work with OpenLDAP container
            return sslUtil.createSSLContext("TLS");
        }

        if (ldapConnectionProperties.trustStore() == null) {
            // Use system default CA certificates (Java's default truststore)
            final SSLUtil sslUtil = new SSLUtil();
            return sslUtil.createSSLContext();
        }

        // Use custom truststore for self-signed certificates or internal CAs
        final SSLUtil sslUtil = new SSLUtil(new TrustStoreTrustManager(
                ldapConnectionProperties.trustStore().trustStorePath(),
                ldapConnectionProperties.trustStore().trustStorePassword() != null ? ldapConnectionProperties.trustStore().trustStorePassword().toCharArray() : null,
                ldapConnectionProperties.trustStore().trustStoreType(),
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

        if (ldapConnectionProperties.connectTimeoutMillis() > 0) {
            options.setConnectTimeoutMillis(ldapConnectionProperties.connectTimeoutMillis());
        }

        if (ldapConnectionProperties.responseTimeoutMillis() > 0) {
            options.setResponseTimeoutMillis(ldapConnectionProperties.responseTimeoutMillis());
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

        return switch (ldapConnectionProperties.tlsMode()) {
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
        return new LDAPConnection(options, ldapConnectionProperties.servers().hosts()[0], ldapConnectionProperties.servers().ports()[0]);
    }

    /**
     * Creates an LDAPS connection (TLS from start).
     */
    private @NotNull LDAPConnection createLdapsConnection(final @NotNull LDAPConnectionOptions options)
            throws LDAPException, GeneralSecurityException {
        final SSLContext sslContext = createSSLContext();
        return new LDAPConnection(sslContext.getSocketFactory(), options, ldapConnectionProperties.servers().hosts()[0], ldapConnectionProperties.servers().ports()[0]);
    }

    /**
     * Creates a connection and upgrades it to TLS using StartTLS.
     */
    private @NotNull LDAPConnection createStartTlsConnection(final @NotNull LDAPConnectionOptions options)
            throws LDAPException, GeneralSecurityException {
        // First create plain connection
        final LDAPConnection connection = new LDAPConnection(options, ldapConnectionProperties.servers().hosts()[0], ldapConnectionProperties.servers().ports()[0]);

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


    /**
     * Creates a test user in LLDAP using the admin account.
     * <p>
     * LLDAP provides a simplified user management approach. We need to:
     * 1. Connect as admin
     * 2. Add the test user to the people organizational unit
     * 3. Set the user's password
     * <p>
     * Note: This method uses the low-level connection API directly since it performs
     * administrative operations (adding users) that are not part of the normal client API.
     */
    public void createTestUser(@NotNull final String adminDn, @NotNull final String adminPassword, @NotNull final String baseDn) throws LDAPException, GeneralSecurityException {
        final var testconnection = new LdapTestConnection(ldapConnectionProperties);
        try (final var adminConnection = testconnection.createConnection()) {
            // Bind as admin using LldapContainer's convenience methods
            final BindRequest bindRequest = new SimpleBindRequest(
                    adminDn,
                    adminPassword);
            final BindResult bindResult = adminConnection.bind(bindRequest);

            assertThat(bindResult.getResultCode()).isEqualTo(ResultCode.SUCCESS);

            // Add test user using proper Attribute objects
            final String testUserDnString = "uid=" + TEST_USERNAME + ",ou=people," + baseDn;

            final AddRequest addRequest = new AddRequest(testUserDnString,
                    new Attribute("objectClass", "inetOrgPerson", "posixAccount"),
                    new Attribute("uid", TEST_USERNAME),
                    new Attribute("cn", TEST_USERNAME),
                    new Attribute("sn", "User"),
                    new Attribute("mail", TEST_USERNAME + "@example.com"), // Required by LLDAP
                    new Attribute("uidNumber", "1000"),
                    new Attribute("gidNumber", "1000"),
                    new Attribute("homeDirectory", "/home/" + TEST_USERNAME)
            );

            adminConnection.add(addRequest);

            // Set the password using a ModifyRequest (this ensures proper password hashing)
            final ModifyRequest modifyRequest = new ModifyRequest(testUserDnString,
                    new Modification(ModificationType.REPLACE,
                            "userPassword",
                            TEST_PASSWORD));

            adminConnection.modify(modifyRequest);
        }
    }

}

