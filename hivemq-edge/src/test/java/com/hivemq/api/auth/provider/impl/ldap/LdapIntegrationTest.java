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

import com.hivemq.api.auth.provider.impl.ldap.testcontainer.LldapContainer;
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.BindRequest;
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
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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
    private static final String TEST_USERNAME = "test";
    private static final String TEST_PASSWORD = "test";
    private static final String KEYSTORE_PASSWORD = "changeit";

    private static File certFile;
    private static File keyFile;
    private static File trustStoreFile;

    // Initialize container with certificate generation
    static {
        try {
            generateSelfSignedCertificate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate self-signed certificate", e);
        }
    }

    @Container
    private static final LldapContainer LLDAP_CONTAINER = LldapContainer.builder()
            .withLdaps(certFile, keyFile)
            .build();

    private static LdapClient ldapClient;
    private static LdapConnectionProperties ldapConnectionProperties;

    @BeforeAll
    static void setUp() throws Exception {
        // Get the dynamically mapped LDAPS port and host from the container
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapsPort();

        // Create connection properties for LDAPS
        ldapConnectionProperties = new LdapConnectionProperties(
                host,
                port,
                TlsMode.LDAPS,
                trustStoreFile.getAbsolutePath(),
                KEYSTORE_PASSWORD,
                KeyStore.getDefaultType(),
                10000, // 10 second connect timeout
                30000, // 30 second response timeout
                LDAP_DN_TEMPLATE,
                LLDAP_CONTAINER.getBaseDn());

        // Create and start LDAP client
        ldapClient = new LdapClient(ldapConnectionProperties);
        ldapClient.start();
        
        // Create test user in LLDAP (using direct connection for admin operations)
        createTestUser();
    }

    @AfterAll
    static void tearDown() {
        // Stop LDAP client
        if (ldapClient != null && ldapClient.isStarted()) {
            ldapClient.stop();
        }

        LLDAP_CONTAINER.stop();

        // Clean up certificate and truststore files
        if (certFile != null && certFile.exists()) {
            certFile.delete();
        }
        if (keyFile != null && keyFile.exists()) {
            keyFile.delete();
        }
        if (trustStoreFile != null && trustStoreFile.exists()) {
            trustStoreFile.delete();
        }
    }

    /**
     * Generates a self-signed certificate for TLS testing using BouncyCastle.
     * <p>
     * This creates:
     * - A private key
     * - A self-signed X.509 certificate
     * - PEM-formatted files for LLDAP server configuration
     * - A TrustStore file containing the certificate for client-side certificate validation
     * <p>
     * The SSLContext will be created dynamically from the truststore by {@link LdapConnectionProperties}.
     */
    private static void generateSelfSignedCertificate() throws Exception {
        // Add BouncyCastle as security provider
        Security.addProvider(new BouncyCastleProvider());

        // Generate RSA key pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Build X.509 certificate using BouncyCastle
        Instant now = Instant.now();
        Date notBefore = Date.from(now);
        Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

        X500Name subject = new X500Name("CN=localhost");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                subject, // issuer
                serial,
                notBefore,
                notAfter,
                subject, // subject (same as issuer for self-signed)
                publicKeyInfo
        );

        // Sign the certificate
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder certHolder = certBuilder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        // Write certificate to PEM file
        certFile = Files.createTempFile("ldap-cert", ".pem").toFile();
        try (PemWriter pemWriter = new PemWriter(new FileWriter(certFile))) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
        }

        // Write private key to PEM file
        keyFile = Files.createTempFile("ldap-key", ".pem").toFile();
        try (PemWriter pemWriter = new PemWriter(new FileWriter(keyFile))) {
            pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        }

        // Create a TrustStore and add our self-signed certificate to it
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null); // Initialize empty truststore
        trustStore.setCertificateEntry("ldap-server", cert);

        // Save the TrustStore to a file
        // The SSLContext will be created dynamically from this truststore by LdapConnectionProperties
        trustStoreFile = Files.createTempFile("ldap-truststore", ".jks").toFile();
        try (var outputStream = new java.io.FileOutputStream(trustStoreFile)) {
            trustStore.store(outputStream, KEYSTORE_PASSWORD.toCharArray());
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
    private static void createTestUser() throws LDAPException, GeneralSecurityException {
        try (LDAPConnection adminConnection = ldapConnectionProperties.createConnection()) {
            // Bind as admin using LldapContainer's convenience methods
            final BindRequest bindRequest = new SimpleBindRequest(
                    LLDAP_CONTAINER.getAdminDn(),
                    LLDAP_CONTAINER.getAdminPassword());
            final BindResult bindResult = adminConnection.bind(bindRequest);

            assertThat(bindResult.getResultCode()).isEqualTo(ResultCode.SUCCESS);

            // Add test user using proper Attribute objects
            final String testUserDnString = "uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn();

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
        final boolean authenticated = ldapClient.authenticateUser(TEST_USERNAME, TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));

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
        try (final LDAPConnection adminConnection = ldapConnectionProperties.createConnection()) {
            // Bind as admin to create authenticated pool using LldapContainer's convenience methods
            final BindRequest bindRequest = new SimpleBindRequest(
                    LLDAP_CONTAINER.getAdminDn(),
                    LLDAP_CONTAINER.getAdminPassword());
            final BindResult bindResult = adminConnection.bind(bindRequest);
            assertThat(bindResult.getResultCode()).isEqualTo(ResultCode.SUCCESS);

            // Create an authenticated connection pool
            final LDAPConnectionPool authenticatedPool = new LDAPConnectionPool(adminConnection, 1, 5);

            try {
                // Test 1: Simple UID search
                final SearchFilterDnResolver uidResolver = new SearchFilterDnResolver(
                        authenticatedPool,
                        "ou=people," + LLDAP_CONTAINER.getBaseDn(),
                        "(uid={username})",
                        SearchScope.ONE,
                        5
                );

                String resolvedDn = uidResolver.resolveDn(TEST_USERNAME);
                assertThat(resolvedDn)
                        .as("Should resolve DN by UID")
                        .isEqualTo("uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn());

                // Verify we can authenticate with the resolved DN
                final boolean authenticated = ldapClient.bindUser(resolvedDn, TEST_PASSWORD.getBytes(StandardCharsets.UTF_8));
                assertThat(authenticated)
                        .as("Should authenticate with resolved DN")
                        .isTrue();

                // Test 2: Email search
                final SearchFilterDnResolver emailResolver = new SearchFilterDnResolver(
                        authenticatedPool,
                        LLDAP_CONTAINER.getBaseDn(),
                        "(mail={username})",
                        SearchScope.SUB,
                        5
                );

                resolvedDn = emailResolver.resolveDn(TEST_USERNAME + "@example.com");
                assertThat(resolvedDn)
                        .as("Should resolve DN by email")
                        .isEqualTo("uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn());

                // Test 3: Complex filter
                final SearchFilterDnResolver complexResolver = new SearchFilterDnResolver(
                        authenticatedPool,
                        LLDAP_CONTAINER.getBaseDn(),
                        "(&(objectClass=inetOrgPerson)(uid={username}))",
                        SearchScope.SUB,
                        5
                );

                resolvedDn = complexResolver.resolveDn(TEST_USERNAME);
                assertThat(resolvedDn)
                        .as("Should resolve DN with complex filter")
                        .isEqualTo("uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn());

                // Test 4: OR filter
                final SearchFilterDnResolver orResolver = new SearchFilterDnResolver(
                        authenticatedPool,
                        LLDAP_CONTAINER.getBaseDn(),
                        "(|(uid={username})(mail={username}))",
                        SearchScope.SUB,
                        5
                );

                resolvedDn = orResolver.resolveDn(TEST_USERNAME);
                assertThat(resolvedDn)
                        .as("Should resolve DN with OR filter")
                        .isEqualTo("uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn());

                // Test 5: User not found
                assertThatThrownBy(() -> uidResolver.resolveDn("nonexistent"))
                        .isInstanceOf(SearchFilterDnResolver.DnResolutionException.class)
                        .hasMessageContaining("No LDAP entry found for username: nonexistent");

            } finally {
                authenticatedPool.close();
            }
        }
    }
}
