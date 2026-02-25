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
package com.hivemq.api.auth.provider.impl.ldap.testcontainer;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.hivemq.api.auth.provider.impl.ldap.LdapConnectionProperties;
import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainer for LLDAP (Light LDAP) server.
 * <p>
 * LLDAP is a lightweight LDAP server designed for authentication and user management.
 * It supports:
 * <ul>
 *     <li>Standard LDAP operations (bind, search, modify)</li>
 *     <li>LDAPS (TLS from connection start)</li>
 *     <li>Custom base DN and admin credentials</li>
 * </ul>
 * <p>
 * <strong>Limitations:</strong>
 * <ul>
 *     <li>Does NOT support START_TLS extended operation</li>
 *     <li>Limited schema support compared to OpenLDAP/Active Directory</li>
 * </ul>
 * <p>
 * Example usage with defaults:
 * <pre>{@code
 * @Container
 * static LldapContainer ldap = new LldapContainer();
 *
 * @Test
 * void testLdapConnection() {
 *     String host = ldap.getHost();
 *     int port = ldap.getLdapPort();
 *     String baseDn = ldap.getBaseDn(); // dc=example,dc=com
 *     String adminDn = ldap.getAdminDn(); // uid=admin,dc=example,dc=com
 *     String password = ldap.getAdminPassword(); // admin_password
 * }
 * }</pre>
 * <p>
 * Example usage with builder:
 * <pre>{@code
 * @Container
 * static LldapContainer ldap = LldapContainer.builder()
 *     .baseDn("dc=mycompany,dc=org")
 *     .adminUsername("admin")
 *     .adminPassword("secret123")
 *     .ldapPort(3890)
 *     .withLdaps(certFile, keyFile)
 *     .build();
 * }</pre>
 *
 * @see OpenLdapContainer for a full-featured alternative that supports START_TLS
 */
public class LldapContainer extends GenericContainer<LldapContainer> {

    public static final String KEYSTORE_PASSWORD = "changeit";

    private static final String DEFAULT_IMAGE_NAME = "lldap/lldap:v0.5.0";
    private static final int DEFAULT_LDAP_PORT = 3890;
    private static final int DEFAULT_LDAPS_PORT = 6360;
    private static final int DEFAULT_HTTP_PORT = 17170;
    private static final String DEFAULT_BASE_DN = "dc=example,dc=com";
    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin_password";

    private final int ldapPort;
    private final int ldapsPort;
    private final int httpPort;
    private final String baseDn;
    private final String adminUsername;
    private final String adminPassword;
    private final boolean ldapsEnabled;
    final @Nullable File trustStoreFile;
    private @Nullable File certFile;
    private @Nullable File keyFile;

    /**
     * Creates a new LLDAP container with default configuration.
     * <p>
     * Default configuration:
     * <ul>
     *     <li>Image: lldap/lldap:v0.5.0</li>
     *     <li>LDAP Port: 3890</li>
     *     <li>LDAPS Port: 6360</li>
     *     <li>HTTP Port: 17170</li>
     *     <li>Base DN: dc=example,dc=com</li>
     *     <li>Admin Username: admin</li>
     *     <li>Admin Password: admin_password</li>
     * </ul>
     */
    public LldapContainer() {
        this(
                DEFAULT_IMAGE_NAME,
                DEFAULT_LDAP_PORT,
                DEFAULT_LDAPS_PORT,
                DEFAULT_HTTP_PORT,
                DEFAULT_BASE_DN,
                DEFAULT_ADMIN_USERNAME,
                DEFAULT_ADMIN_PASSWORD,
                null,
                null,
                null);
    }

    private LldapContainer(
            final @NotNull String dockerImageName,
            final int ldapPort,
            final int ldapsPort,
            final int httpPort,
            final @NotNull String baseDn,
            final @NotNull String adminUsername,
            final @NotNull String adminPassword,
            final @Nullable File certFile,
            final @Nullable File keyFile,
            final @Nullable File trustStoreFile) {
        super(DockerImageName.parse(dockerImageName));
        this.ldapPort = ldapPort;
        this.ldapsPort = ldapsPort;
        this.httpPort = httpPort;
        this.baseDn = baseDn;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.ldapsEnabled = (certFile != null && keyFile != null);
        this.trustStoreFile = trustStoreFile;
        configure(certFile, keyFile);
    }

    private void configure(final @Nullable File certFile, final @Nullable File keyFile) {
        if (ldapsEnabled) {
            withExposedPorts(ldapPort, ldapsPort, httpPort);
        } else {
            withExposedPorts(ldapPort, httpPort);
        }

        withEnv("LLDAP_LDAP_PORT", String.valueOf(ldapPort));
        withEnv("LLDAP_LDAP_BASE_DN", baseDn);
        withEnv("LLDAP_LDAP_USER_DN", adminUsername);
        withEnv("LLDAP_LDAP_USER_PASS", adminPassword);

        // Configure LDAPS if certificates are provided
        if (ldapsEnabled) {
            withEnv("LLDAP_LDAPS_PORT", String.valueOf(ldapsPort));
            withEnv("LLDAP_LDAPS_OPTIONS__ENABLED", "true");
            withEnv("LLDAP_LDAPS_OPTIONS__CERT_FILE", "/data/cert.pem");
            withEnv("LLDAP_LDAPS_OPTIONS__KEY_FILE", "/data/key.pem");
            withCopyFileToContainer(MountableFile.forHostPath(certFile.toPath()), "/data/cert.pem");
            withCopyFileToContainer(MountableFile.forHostPath(keyFile.toPath()), "/data/key.pem");
        }

        waitingFor(Wait.forLogMessage(".*Starting LLDAP.*", 1).withStartupTimeout(Duration.ofSeconds(60)));
    }

    @Override
    public void stop() {
        super.stop();
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
     * Creates a new builder for LldapContainer.
     *
     * @return a new builder instance
     */
    public static @NotNull Builder builder() {
        return new Builder();
    }

    public @Nullable File getTrustStoreFile() {
        return trustStoreFile;
    }

    /**
     * Gets the dynamically mapped LDAP port.
     * <p>
     * Can only be called after the container has started.
     *
     * @return the mapped LDAP port
     */
    public int getLdapPort() {
        return getMappedPort(ldapPort);
    }

    /**
     * Gets the dynamically mapped LDAPS port.
     * <p>
     * Can only be called after the container has started and if LDAPS is enabled.
     *
     * @return the mapped LDAPS port
     * @throws IllegalStateException if LDAPS is not enabled
     */
    public int getLdapsPort() {
        if (!ldapsEnabled) {
            throw new IllegalStateException("LDAPS is not enabled. Use withLdaps() builder method to enable LDAPS.");
        }
        return getMappedPort(ldapsPort);
    }

    /**
     * Returns whether LDAPS is enabled for this container.
     *
     * @return true if LDAPS is enabled, false otherwise
     */
    public boolean isLdapsEnabled() {
        return ldapsEnabled;
    }

    /**
     * Gets the dynamically mapped HTTP port.
     * <p>
     * Can only be called after the container has started.
     *
     * @return the mapped HTTP port
     */
    public int getHttpPort() {
        return getMappedPort(httpPort);
    }

    /**
     * Gets the base DN configured for this container.
     *
     * @return the base DN
     */
    public @NotNull String getBaseDn() {
        return baseDn;
    }

    /**
     * Gets the admin username configured for this container.
     *
     * @return the admin username
     */
    public @NotNull String getAdminUsername() {
        return adminUsername;
    }

    /**
     * Gets the admin password configured for this container.
     *
     * @return the admin password
     */
    public @NotNull String getAdminPassword() {
        return adminPassword;
    }

    /**
     * Gets the admin DN (Distinguished Name) for binding as admin.
     * <p>
     * Format: uid={adminUsername},ou=people,{baseDn}
     *
     * @return the admin DN
     */
    public @NotNull String getUserRdns(String username) {
        return "uid=" + username + ",ou=people";
    }

    public @NotNull String getAdminRdns() {
        return getUserRdns(getAdminUsername());
    }

    public @NotNull String getUserDn(String username) {
        return getUserRdns(username) + "," + getBaseDn();
    }

    public @NotNull String getAdminDn() {
        return getUserDn(getAdminUsername());
    }

    /**
     * Builder for LldapContainer.
     * <p>
     * Provides a fluent API for configuring the container with custom settings.
     * All settings are optional and have reasonable defaults.
     */
    public static class Builder {
        private String dockerImageName = DEFAULT_IMAGE_NAME;
        private int ldapPort = DEFAULT_LDAP_PORT;
        private int ldapsPort = DEFAULT_LDAPS_PORT;
        private int httpPort = DEFAULT_HTTP_PORT;
        private String baseDn = DEFAULT_BASE_DN;
        private String adminUsername = DEFAULT_ADMIN_USERNAME;
        private String adminPassword = DEFAULT_ADMIN_PASSWORD;
        private File certFile;
        private File keyFile;
        private File trustStoreFile;

        private Builder() {}

        /**
         * Sets the Docker image name.
         * <p>
         * Default: lldap/lldap:v0.5.0
         *
         * @param dockerImageName the Docker image name
         * @return this builder
         */
        public @NotNull Builder dockerImageName(final @NotNull String dockerImageName) {
            this.dockerImageName = dockerImageName;
            return this;
        }

        /**
         * Sets the LDAP port inside the container.
         * <p>
         * Default: 3890
         * <p>
         * Note: This is the internal port. Use {@link LldapContainer#getLdapPort()} to get the mapped external port.
         *
         * @param ldapPort the LDAP port
         * @return this builder
         */
        public @NotNull Builder ldapPort(final int ldapPort) {
            this.ldapPort = ldapPort;
            return this;
        }

        /**
         * Sets the LDAPS port inside the container.
         * <p>
         * Default: 6360
         * <p>
         * Note: This is the internal port. Use {@link LldapContainer#getLdapsPort()} to get the mapped external port.
         * This port is only exposed if LDAPS is enabled via {@link #withLdaps()}.
         *
         * @param ldapsPort the LDAPS port
         * @return this builder
         */
        public @NotNull Builder ldapsPort(final int ldapsPort) {
            this.ldapsPort = ldapsPort;
            return this;
        }

        /**
         * Sets the HTTP port inside the container.
         * <p>
         * Default: 17170
         * <p>
         * Note: This is the internal port. Use {@link LldapContainer#getHttpPort()} to get the mapped external port.
         *
         * @param httpPort the HTTP port
         * @return this builder
         */
        public @NotNull Builder httpPort(final int httpPort) {
            this.httpPort = httpPort;
            return this;
        }

        /**
         * Sets the base DN for the LDAP directory.
         * <p>
         * Default: dc=example,dc=com
         *
         * @param baseDn the base DN (e.g., "dc=example,dc=com")
         * @return this builder
         */
        public @NotNull Builder baseDn(final @NotNull String baseDn) {
            this.baseDn = baseDn;
            return this;
        }

        /**
         * Sets the admin username.
         * <p>
         * Default: admin
         *
         * @param adminUsername the admin username
         * @return this builder
         */
        public @NotNull Builder adminUsername(final @NotNull String adminUsername) {
            this.adminUsername = adminUsername;
            return this;
        }

        /**
         * Sets the admin password.
         * <p>
         * Default: admin_password
         *
         * @param adminPassword the admin password
         * @return this builder
         */
        public @NotNull Builder adminPassword(final @NotNull String adminPassword) {
            this.adminPassword = adminPassword;
            return this;
        }

        /**
         * Enables LDAPS (TLS from connection start) with custom certificates.
         * <p>
         * When enabled, LLDAP will accept TLS connections using an internally generated certificate and key.
         *
         * @return this builder
         */
        public @NotNull Builder withLdaps() {
            try {
                final var certFiles = generateSelfSignedCertificate();
                certFile = certFiles.certFile();
                keyFile = certFiles.keyFile();
                trustStoreFile = certFiles.trustStoreFile();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        /**
         * Builds the LldapContainer with the configured settings.
         *
         * @return a new LldapContainer instance
         */
        public @NotNull LldapContainer build() {
            return new LldapContainer(
                    dockerImageName,
                    ldapPort,
                    ldapsPort,
                    httpPort,
                    baseDn,
                    adminUsername,
                    adminPassword,
                    certFile,
                    keyFile,
                    trustStoreFile);
        }
    }

    public record CertFiles(
            @com.hivemq.extension.sdk.api.annotations.NotNull
            File certFile,

            @com.hivemq.extension.sdk.api.annotations.NotNull
            File keyFile,

            @com.hivemq.extension.sdk.api.annotations.NotNull
            File trustStoreFile) {}
    ;
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
    public static CertFiles generateSelfSignedCertificate() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Generate RSA key pair
        final var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        final var keyPair = keyPairGenerator.generateKeyPair();

        // Build X.509 certificate using BouncyCastle
        final var now = Instant.now();
        final var notBefore = Date.from(now);
        final var notAfter = Date.from(now.plus(365, ChronoUnit.DAYS));

        final var subject = new X500Name("CN=localhost");
        final var serial = BigInteger.valueOf(System.currentTimeMillis());
        final var publicKeyInfo =
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded());

        final var certBuilder = new X509v3CertificateBuilder(
                subject, // issuer
                serial,
                notBefore,
                notAfter,
                subject, // subject (same as issuer for self-signed)
                publicKeyInfo);

        // Sign the certificate
        final var signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        final var certHolder = certBuilder.build(signer);
        final var cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);

        // Write certificate to PEM file
        final var certFile = Files.createTempFile("ldap-cert", ".pem").toFile();
        try (final var pemWriter = new PemWriter(new FileWriter(certFile, UTF_8))) {
            pemWriter.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
        }

        // Write private key to PEM file
        final var keyFile = Files.createTempFile("ldap-key", ".pem").toFile();
        try (final var pemWriter = new PemWriter(new FileWriter(keyFile, UTF_8))) {
            pemWriter.writeObject(
                    new PemObject("PRIVATE KEY", keyPair.getPrivate().getEncoded()));
        }

        // Create a TrustStore and add our self-signed certificate to it
        final var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null); // Initialize empty truststore
        trustStore.setCertificateEntry("ldap-server", cert);

        // Save the TrustStore to a file
        // The SSLContext will be created dynamically from this truststore by LdapConnectionProperties
        final var trustStoreFile =
                Files.createTempFile("ldap-truststore", ".jks").toFile();
        try (final var outputStream = new java.io.FileOutputStream(trustStoreFile)) {
            trustStore.store(outputStream, KEYSTORE_PASSWORD.toCharArray());
        }
        return new CertFiles(certFile, keyFile, trustStoreFile);
    }
}
