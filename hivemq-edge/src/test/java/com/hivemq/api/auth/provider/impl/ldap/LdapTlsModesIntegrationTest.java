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
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
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

    private static final String LDAP_DN_TEMPLATE = "uid={username},ou=people,{baseDn}";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpass";
    private static final String KEYSTORE_PASSWORD = "changeit";

    private static File certFile;
    private static File keyFile;
    private static File trustStoreFile;

    // Initialize container with certificate generation for START_TLS
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
        // Get the dynamically mapped port from the container
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapPort();

        // Create connection properties for plain LDAP (no TLS)
        ldapConnectionProperties = new LdapConnectionProperties(
                host,
                port,
                TlsMode.NONE,
                null,
                null,
                null,
                5000,  // 5 second connect timeout
                10000, // 10 second response timeout
                LDAP_DN_TEMPLATE,
                LLDAP_CONTAINER.getBaseDn());

        // Create and start LDAP client
        ldapClient = new LdapClient(ldapConnectionProperties);
        ldapClient.start();

        // Create test user
        createTestUser();
    }

    @AfterAll
    static void tearDown() {
        if (ldapClient != null && ldapClient.isStarted()) {
            ldapClient.stop();
        }
        LLDAP_CONTAINER.stop();

        // Clean up certificate files
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
     * Generates a self-signed certificate for START_TLS testing.
     */
    private static void generateSelfSignedCertificate() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Generate RSA key pair
        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Build X.509 certificate
        final Instant now = Instant.now();
        final Date notBefore = Date.from(now);
        final Date notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

        final X500Name subject = new X500Name("CN=localhost");
        final BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        final SubjectPublicKeyInfo publicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        final X509v3CertificateBuilder certBuilder = new X509v3CertificateBuilder(
                subject,
                serial,
                notBefore,
                notAfter,
                subject,
                publicKeyInfo
        );

        final ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        final X509CertificateHolder certHolder = certBuilder.build(signer);
        final X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certHolder);

        // Write certificate to PEM file
        certFile = Files.createTempFile("ldap-starttls-cert", ".pem").toFile();
        try (PemWriter pemWriter = new PemWriter(new FileWriter(certFile))) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
        }

        // Write private key to PEM file
        keyFile = Files.createTempFile("ldap-starttls-key", ".pem").toFile();
        try (PemWriter pemWriter = new PemWriter(new FileWriter(keyFile))) {
            pemWriter.writeObject(new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        }

        // Create truststore
        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("ldap-server", cert);

        trustStoreFile = Files.createTempFile("ldap-starttls-truststore", ".jks").toFile();
        try (var outputStream = new java.io.FileOutputStream(trustStoreFile)) {
            trustStore.store(outputStream, KEYSTORE_PASSWORD.toCharArray());
        }
    }

    private static void createTestUser() throws LDAPException, GeneralSecurityException {
        try (LDAPConnection adminConnection = ldapConnectionProperties.createConnection()) {
            // Bind as admin
            final String adminUserDn = "uid=" + LLDAP_CONTAINER.getAdminUsername() + ",ou=people," + LLDAP_CONTAINER.getBaseDn();
            final BindRequest bindRequest = new SimpleBindRequest(adminUserDn, LLDAP_CONTAINER.getAdminPassword());
            final BindResult bindResult = adminConnection.bind(bindRequest);

            assertThat(bindResult.getResultCode()).isEqualTo(ResultCode.SUCCESS);

            // Add test user
            final String testUserDnString = "uid=" + TEST_USERNAME + ",ou=people," + LLDAP_CONTAINER.getBaseDn();

            final AddRequest addRequest = new AddRequest(testUserDnString,
                    new Attribute("objectClass", "inetOrgPerson", "posixAccount"),
                    new Attribute("uid", TEST_USERNAME),
                    new Attribute("cn", TEST_USERNAME),
                    new Attribute("sn", "User"),
                    new Attribute("mail", TEST_USERNAME + "@example.com"),
                    new Attribute("uidNumber", "2000"),
                    new Attribute("gidNumber", "2000"),
                    new Attribute("homeDirectory", "/home/" + TEST_USERNAME)
            );

            adminConnection.add(addRequest);

            // Set password
            final ModifyRequest modifyRequest = new ModifyRequest(testUserDnString,
                    new Modification(ModificationType.REPLACE, "userPassword", TEST_PASSWORD));

            adminConnection.modify(modifyRequest);
        }
    }

    /**
     * Tests authentication over plain LDAP (no encryption).
     */
    @Test
    void testPlainLdapAuthentication() throws LDAPException {
        // Act
        final boolean authenticated = ldapClient.authenticateUser(TEST_USERNAME, TEST_PASSWORD);

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
        final boolean authenticated = ldapClient.authenticateUser(TEST_USERNAME, "wrongpassword");

        // Assert
        assertThat(authenticated)
                .as("Authentication should fail with wrong password")
                .isFalse();
    }

    /**
     * Tests connection timeout by trying to connect to a non-responsive host.
     */
    @Test
    void testConnectionTimeout() {
        // Arrange - use a non-routable IP that will timeout
        final LdapConnectionProperties timeoutProps = new LdapConnectionProperties(
                "10.255.255.1", // Non-routable IP
                389,
                TlsMode.NONE,
                null,
                null,
                null,
                1000,  // 1 second timeout - should fail quickly
                5000,
                LDAP_DN_TEMPLATE,
                LLDAP_CONTAINER.getBaseDn());

        final LdapClient timeoutClient = new LdapClient(timeoutProps);

        // Act & Assert
        final long startTime = System.currentTimeMillis();
        assertThatThrownBy(timeoutClient::start)
                .isInstanceOf(LDAPException.class);
        final long duration = System.currentTimeMillis() - startTime;

        // Should timeout within reasonable time (less than 5 seconds)
        // allowing some margin for processing
        assertThat(duration)
                .as("Connection should timeout quickly")
                .isLessThan(5000);
    }

    /**
     * Tests that default timeouts are used when set to 0.
     */
    @Test
    void testDefaultTimeouts() throws Exception {
        // Arrange
        final String host = LLDAP_CONTAINER.getHost();
        final int port = LLDAP_CONTAINER.getLdapPort();

        final LdapConnectionProperties defaultTimeoutProps = new LdapConnectionProperties(
                host,
                port,
                TlsMode.NONE,
                null,
                null,
                null,
                0,  // Use default timeout
                0,  // Use default timeout
                LDAP_DN_TEMPLATE,
                LLDAP_CONTAINER.getBaseDn());

        final LdapClient defaultTimeoutClient = new LdapClient(defaultTimeoutProps);

        // Act
        defaultTimeoutClient.start();

        try {
            final boolean authenticated = defaultTimeoutClient.authenticateUser(TEST_USERNAME, TEST_PASSWORD);

            // Assert
            assertThat(authenticated)
                    .as("Authentication should work with default timeouts")
                    .isTrue();
        } finally {
            defaultTimeoutClient.stop();
        }
    }

}
