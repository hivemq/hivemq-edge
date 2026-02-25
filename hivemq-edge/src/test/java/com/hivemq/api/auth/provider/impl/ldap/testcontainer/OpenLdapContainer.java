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

import com.google.common.base.Splitter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainer for OpenLDAP server using osixia/openldap Docker image.
 * <p>
 * OpenLDAP is a full-featured LDAP server implementation that supports:
 * <ul>
 *     <li>All standard LDAP operations (bind, search, modify, add, delete)</li>
 *     <li>START_TLS (upgrade from plain to encrypted connection)</li>
 *     <li>LDAPS (TLS from connection start)</li>
 *     <li>LDIF file seeding for initial data</li>
 *     <li>Multiple backends (mdb, hdb, etc.)</li>
 *     <li>Replication and advanced features</li>
 * </ul>
 * <p>
 * Example usage with defaults:
 * <pre>{@code
 * @Container
 * static OpenLdapContainer ldap = new OpenLdapContainer();
 *
 * @Test
 * void testLdapConnection() {
 *     String host = ldap.getHost();
 *     int port = ldap.getLdapPort();
 *     String baseDn = ldap.getBaseDn(); // dc=example,dc=org
 *     String adminDn = ldap.getAdminDn(); // cn=admin,dc=example,dc=org
 *     String password = ldap.getAdminPassword(); // admin
 * }
 * }</pre>
 * <p>
 * Example usage with builder and LDIF seeding:
 * <pre>{@code
 * @Container
 * static OpenLdapContainer ldap = OpenLdapContainer.builder()
 *     .domain("mycompany.org")
 *     .organisation("My Company Inc")
 *     .adminPassword("secret123")
 *     .withLdifFile("test-data.ldif")
 *     .build();
 * }</pre>
 * <p>
 * Example with custom configuration:
 * <pre>{@code
 * @Container
 * static OpenLdapContainer ldap = OpenLdapContainer.builder()
 *     .domain("example.com")
 *     .adminPassword("admin")
 *     .withTls(true)
 *     .withBackend("mdb")
 *     .withRfc2307bisSchema(false)
 *     .withReadonlyUser(false)
 *     .withReplication(false)
 *     .build();
 * }</pre>
 *
 * @see LldapContainer for a lightweight alternative (does not support START_TLS)
 */
public class OpenLdapContainer extends GenericContainer<OpenLdapContainer> {

    private static final String DEFAULT_IMAGE_NAME = "osixia/openldap:1.5.0";
    private static final int DEFAULT_LDAP_PORT = 389;
    private static final int DEFAULT_LDAPS_PORT = 636;
    private static final String DEFAULT_DOMAIN = "example.org";
    private static final String DEFAULT_ORGANISATION = "Example Inc";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";
    private static final String DEFAULT_CONFIG_PASSWORD = "config";
    private static final String DEFAULT_BACKEND = "mdb";
    private static final String DEFAULT_SSL_HELPER_PREFIX = "ldap";

    private final int ldapPort;
    private final String domain;
    private final String baseDn;
    private final String adminPassword;

    /**
     * Creates a new OpenLDAP container with default configuration.
     * <p>
     * Default configuration:
     * <ul>
     *     <li>Image: osixia/openldap:1.5.0</li>
     *     <li>LDAP Port: 389</li>
     *     <li>Domain: example.org</li>
     *     <li>Base DN: dc=example,dc=org</li>
     *     <li>Organisation: Example Inc</li>
     *     <li>Admin Password: admin</li>
     *     <li>Config Password: config</li>
     *     <li>Backend: mdb</li>
     *     <li>TLS: disabled</li>
     *     <li>Replication: disabled</li>
     * </ul>
     */
    public OpenLdapContainer() {
        this(
                DEFAULT_IMAGE_NAME,
                DEFAULT_LDAP_PORT,
                DEFAULT_DOMAIN,
                DEFAULT_ORGANISATION,
                DEFAULT_ADMIN_PASSWORD,
                DEFAULT_CONFIG_PASSWORD,
                false,
                DEFAULT_BACKEND,
                false,
                false,
                false,
                true,
                DEFAULT_SSL_HELPER_PREFIX,
                new ArrayList<>());
    }

    private OpenLdapContainer(
            final @NotNull String dockerImageName,
            final int ldapPort,
            final @NotNull String domain,
            final @NotNull String organisation,
            final @NotNull String adminPassword,
            final @NotNull String configPassword,
            final boolean readonlyUser,
            final @NotNull String backend,
            final boolean rfc2307bisSchema,
            final boolean tls,
            final boolean replication,
            final boolean removeConfigAfterSetup,
            final @NotNull String sslHelperPrefix,
            final @NotNull List<String> ldifFiles) {
        super(DockerImageName.parse(dockerImageName));
        this.ldapPort = ldapPort;
        this.domain = domain;
        this.baseDn = domainToBaseDn(domain);
        this.adminPassword = adminPassword;

        configure(
                organisation,
                configPassword,
                readonlyUser,
                backend,
                rfc2307bisSchema,
                tls,
                replication,
                removeConfigAfterSetup,
                sslHelperPrefix,
                ldifFiles);
    }

    private void configure(
            final @NotNull String organisation,
            final @NotNull String configPassword,
            final boolean readonlyUser,
            final @NotNull String backend,
            final boolean rfc2307bisSchema,
            final boolean tls,
            final boolean replication,
            final boolean removeConfigAfterSetup,
            final @NotNull String sslHelperPrefix,
            final @NotNull List<String> ldifFiles) {

        // Expose LDAP port (and LDAPS port if TLS is enabled)
        if (tls) {
            withExposedPorts(ldapPort, DEFAULT_LDAPS_PORT);
            withEnv("LDAP_TLS_VERIFY_CLIENT", "never"); // Don't require client certificates
        } else {
            withExposedPorts(ldapPort);
        }

        withEnv("LDAP_ORGANISATION", organisation);
        withEnv("LDAP_DOMAIN", domain);
        withEnv("LDAP_ADMIN_PASSWORD", adminPassword);
        withEnv("LDAP_CONFIG_PASSWORD", configPassword);
        withEnv("LDAP_READONLY_USER", String.valueOf(readonlyUser));
        withEnv("LDAP_RFC2307BIS_SCHEMA", String.valueOf(rfc2307bisSchema));
        withEnv("LDAP_BACKEND", backend);
        withEnv("LDAP_TLS", String.valueOf(tls));
        withEnv("LDAP_REPLICATION", String.valueOf(replication));
        withEnv("KEEP_EXISTING_CONFIG", "false");
        withEnv("LDAP_REMOVE_CONFIG_AFTER_SETUP", String.valueOf(removeConfigAfterSetup));
        withEnv("LDAP_SSL_HELPER_PREFIX", sslHelperPrefix);

        // Copy LDIF files to bootstrap directory - OpenLDAP will load them on startup
        for (int i = 0; i < ldifFiles.size(); i++) {
            final String ldifFile = ldifFiles.get(i);
            final String targetPath = String.format(
                    "/container/service/slapd/assets/config/bootstrap/ldif/custom/%02d-custom.ldif", i + 1);
            withCopyFileToContainer(MountableFile.forClasspathResource(ldifFile), targetPath);
        }

        // Wait for slapd to start
        // Note: The container waits for "slapd starting" but TLS configuration might take additional time
        waitingFor(Wait.forLogMessage(".*slapd starting.*", 1).withStartupTimeout(Duration.ofSeconds(tls ? 90 : 60)));
    }

    /**
     * Converts a domain name to an LDAP base DN.
     * <p>
     * Example: "example.org" -> "dc=example,dc=org"
     *
     * @param domain the domain name
     * @return the base DN
     */
    private static @NotNull String domainToBaseDn(final @NotNull String domain) {
        final List<String> parts = Splitter.on('.').splitToList(domain);
        final StringBuilder baseDn = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                baseDn.append(",");
            }
            baseDn.append("dc=").append(parts.get(i));
        }
        return baseDn.toString();
    }

    /**
     * Creates a new builder for OpenLdapContainer.
     *
     * @return a new builder instance
     */
    public static @NotNull Builder builder() {
        return new Builder();
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
     * Gets the domain configured for this container.
     *
     * @return the domain (e.g., "example.org")
     */
    public @NotNull String getDomain() {
        return domain;
    }

    /**
     * Gets the base DN derived from the configured domain.
     * <p>
     * The base DN is automatically derived from the domain.
     * Example: "example.org" -> "dc=example,dc=org"
     *
     * @return the base DN
     */
    public @NotNull String getBaseDn() {
        return baseDn;
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
     * Format: cn=admin,{baseDn}
     *
     * @return the admin DN
     */
    public @NotNull String getAdminUsername() {
        return "admin";
    }

    public @NotNull String getAdminRdns() {
        return "cn=" + getAdminUsername();
    }

    public @NotNull String getAdminDn() {
        return getAdminRdns() + "," + getBaseDn();
    }

    /**
     * Builder for OpenLdapContainer.
     * <p>
     * Provides a fluent API for configuring the container with custom settings.
     * All settings are optional and have reasonable defaults.
     */
    public static class Builder {
        private String dockerImageName = DEFAULT_IMAGE_NAME;
        private int ldapPort = DEFAULT_LDAP_PORT;
        private String domain = DEFAULT_DOMAIN;
        private String organisation = DEFAULT_ORGANISATION;
        private String adminPassword = DEFAULT_ADMIN_PASSWORD;
        private String configPassword = DEFAULT_CONFIG_PASSWORD;
        private boolean readonlyUser = false;
        private String backend = DEFAULT_BACKEND;
        private boolean rfc2307bisSchema = false;
        private boolean tls = false;
        private boolean replication = false;
        private boolean removeConfigAfterSetup = true;
        private String sslHelperPrefix = DEFAULT_SSL_HELPER_PREFIX;
        private final List<String> ldifFiles = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the Docker image name.
         * <p>
         * Default: osixia/openldap:1.5.0
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
         * Default: 389
         * <p>
         * Note: This is the internal port. Use {@link OpenLdapContainer#getLdapPort()} to get the mapped external port.
         *
         * @param ldapPort the LDAP port
         * @return this builder
         */
        public @NotNull Builder ldapPort(final int ldapPort) {
            this.ldapPort = ldapPort;
            return this;
        }

        /**
         * Sets the domain for the LDAP directory.
         * <p>
         * The base DN is automatically derived from this domain.
         * Example: "example.org" -> base DN will be "dc=example,dc=org"
         * <p>
         * Default: example.org
         *
         * @param domain the domain (e.g., "example.org")
         * @return this builder
         */
        public @NotNull Builder domain(final @NotNull String domain) {
            this.domain = domain;
            return this;
        }

        /**
         * Sets the organisation name.
         * <p>
         * This is used for the organisation attribute in the directory.
         * <p>
         * Default: Example Inc
         *
         * @param organisation the organisation name
         * @return this builder
         */
        public @NotNull Builder organisation(final @NotNull String organisation) {
            this.organisation = organisation;
            return this;
        }

        /**
         * Sets the admin password.
         * <p>
         * Default: admin
         *
         * @param adminPassword the admin password
         * @return this builder
         */
        public @NotNull Builder adminPassword(final @NotNull String adminPassword) {
            this.adminPassword = adminPassword;
            return this;
        }

        /**
         * Sets the config password.
         * <p>
         * This is the password for the config admin user (cn=admin,cn=config).
         * <p>
         * Default: config
         *
         * @param configPassword the config password
         * @return this builder
         */
        public @NotNull Builder configPassword(final @NotNull String configPassword) {
            this.configPassword = configPassword;
            return this;
        }

        /**
         * Enables or disables the readonly user.
         * <p>
         * When enabled, creates a readonly user account in the directory.
         * <p>
         * Default: false
         *
         * @param readonlyUser true to enable readonly user, false otherwise
         * @return this builder
         */
        public @NotNull Builder withReadonlyUser(final boolean readonlyUser) {
            this.readonlyUser = readonlyUser;
            return this;
        }

        /**
         * Sets the database backend.
         * <p>
         * Common values: "mdb" (default, recommended), "hdb" (deprecated), "bdb" (deprecated)
         * <p>
         * Default: mdb
         *
         * @param backend the backend type
         * @return this builder
         */
        public @NotNull Builder withBackend(final @NotNull String backend) {
            this.backend = backend;
            return this;
        }

        /**
         * Enables or disables RFC 2307bis schema.
         * <p>
         * RFC 2307bis extends the standard POSIX schema with group membership.
         * <p>
         * Default: false
         *
         * @param rfc2307bisSchema true to enable RFC 2307bis schema, false otherwise
         * @return this builder
         */
        public @NotNull Builder withRfc2307bisSchema(final boolean rfc2307bisSchema) {
            this.rfc2307bisSchema = rfc2307bisSchema;
            return this;
        }

        /**
         * Enables or disables TLS.
         * <p>
         * When enabled, the server will support LDAPS and START_TLS.
         * <p>
         * Default: false
         *
         * @param tls true to enable TLS, false otherwise
         * @return this builder
         */
        public @NotNull Builder withTls(final boolean tls) {
            this.tls = tls;
            return this;
        }

        /**
         * Enables or disables replication.
         * <p>
         * Default: false
         *
         * @param replication true to enable replication, false otherwise
         * @return this builder
         */
        public @NotNull Builder withReplication(final boolean replication) {
            this.replication = replication;
            return this;
        }

        /**
         * Enables or disables removal of config after setup.
         * <p>
         * Default: true
         *
         * @param removeConfigAfterSetup true to remove config after setup, false otherwise
         * @return this builder
         */
        public @NotNull Builder withRemoveConfigAfterSetup(final boolean removeConfigAfterSetup) {
            this.removeConfigAfterSetup = removeConfigAfterSetup;
            return this;
        }

        /**
         * Sets the SSL helper prefix.
         * <p>
         * Default: ldap
         *
         * @param sslHelperPrefix the SSL helper prefix
         * @return this builder
         */
        public @NotNull Builder withSslHelperPrefix(final @NotNull String sslHelperPrefix) {
            this.sslHelperPrefix = sslHelperPrefix;
            return this;
        }

        /**
         * Adds an LDIF file to be loaded on container startup.
         * <p>
         * The LDIF file must be available as a classpath resource.
         * Multiple LDIF files can be added and will be loaded in the order they are added.
         * <p>
         * Example:
         * <pre>{@code
         * OpenLdapContainer ldap = OpenLdapContainer.builder()
         *     .withLdifFile("ldap/users.ldif")
         *     .withLdifFile("ldap/groups.ldif")
         *     .build();
         * }</pre>
         *
         * @param classpathResourcePath the classpath path to the LDIF file (e.g., "ldap/test-data.ldif")
         * @return this builder
         */
        public @NotNull Builder withLdifFile(final @NotNull String classpathResourcePath) {
            this.ldifFiles.add(classpathResourcePath);
            return this;
        }

        /**
         * Builds the OpenLdapContainer with the configured settings.
         *
         * @return a new OpenLdapContainer instance
         */
        public @NotNull OpenLdapContainer build() {
            return new OpenLdapContainer(
                    dockerImageName,
                    ldapPort,
                    domain,
                    organisation,
                    adminPassword,
                    configPassword,
                    readonlyUser,
                    backend,
                    rfc2307bisSchema,
                    tls,
                    replication,
                    removeConfigAfterSetup,
                    sslHelperPrefix,
                    ldifFiles);
        }
    }
}
