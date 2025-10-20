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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LdapConnectionProperties}.
 */
class LdapConnectionPropertiesTest {

    private static final String DEFAULT_DN_TEMPLATE = "uid={username},ou=people,{baseDn}";
    private static final String DEFAULT_BASE_DN = "dc=example,dc=com";
    private static final LdapConnectionProperties.LdapSimpleBind DEFAULT_SIMPLE_BIND =
            new LdapConnectionProperties.LdapSimpleBind("cn=admin", "admin");

    @Test
    void testTlsModeDefaults() {
        Assertions.assertThat(TlsMode.NONE.defaultPort).isEqualTo(389);
        assertThat(TlsMode.START_TLS.defaultPort).isEqualTo(389);
        assertThat(TlsMode.LDAPS.defaultPort).isEqualTo(636);
    }

    @Test
    void testValidationRejectsInvalidPort() {
        assertThatThrownBy(() -> new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{0}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Port must be between 1 and 65535");

        assertThatThrownBy(() -> new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{65536}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Port must be between 1 and 65535");
    }

    @Test
    void testValidationRejectsNegativeTimeouts() {
        assertThatThrownBy(() -> new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                -1,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Connect timeout cannot be negative");

        assertThatThrownBy(() -> new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                -1,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Response timeout cannot be negative");
    }

    @Test
    void testTlsModesAllowNullTruststore() {
        // LDAPS allows null truststore (will use system CAs)
        final LdapConnectionProperties ldapsProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{636}),
                TlsMode.LDAPS,
                null, // no custom truststore - will use system CAs
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);
        assertThat(ldapsProps.trustStore()).isNull();

        // START_TLS allows null truststore (will use system CAs)
        final LdapConnectionProperties startTlsProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.START_TLS,
                null, // no custom truststore - will use system CAs
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);
        assertThat(startTlsProps.trustStore()).isNull();

        // NONE doesn't need truststore
        final LdapConnectionProperties noneProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);
        assertThat(noneProps.trustStore()).isNull();
    }

    @Test
    void testConvenienceConstructorUsesDefaultTimeouts() {
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{636}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore("/path/to/truststore", "password", "JKS"),
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);

        assertThat(props.connectTimeoutMillis()).isEqualTo(0);
        assertThat(props.responseTimeoutMillis()).isEqualTo(0);
    }

    @Test
    void testCreateSSLContextThrowsForNoneTlsMode() {
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);

        assertThatThrownBy(props::createSSLContext)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SSLContext is not needed for TLS mode: NONE");
    }

    @Test
    void testCreateSSLContextWithSystemCAs() throws Exception {
        // LDAPS with null truststore should use system CAs
        final LdapConnectionProperties ldapsProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{636}),
                TlsMode.LDAPS,
                null, // Use system default CAs
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);

        final javax.net.ssl.SSLContext sslContext = ldapsProps.createSSLContext();
        assertThat(sslContext)
                .as("SSLContext should be created with system CAs")
                .isNotNull();

        // START_TLS with null truststore should use system CAs
        final LdapConnectionProperties startTlsProps = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.START_TLS,
                null, // Use system default CAs
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);

        final javax.net.ssl.SSLContext startTlsSslContext = startTlsProps.createSSLContext();
        assertThat(startTlsSslContext)
                .as("SSLContext should be created with system CAs")
                .isNotNull();
    }

    @Test
    void testCreateSSLContextWithCustomTruststore() {
        // Verify that custom truststore path is stored correctly
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{636}),
                TlsMode.LDAPS,
                new LdapConnectionProperties.TrustStore("/path/to/custom/truststore.jks", "password", "JKS"),
                0,
                0,
                1,
                DEFAULT_DN_TEMPLATE,
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND);

        assertThat(props.trustStore().trustStorePath())
                .as("Custom truststore path should be stored")
                .isEqualTo("/path/to/custom/truststore.jks");
        assertThat(props.trustStore().trustStorePassword())
                .as("Truststore password should be stored")
                .isNotNull();
        assertThat(props.trustStore().trustStoreType())
                .as("Truststore type should be stored")
                .isEqualTo("JKS");

        // Note: We can't easily test createSSLContext() with a non-existent file
        // because UnboundID SDK creates the SSLContext successfully and only fails
        // when actually using it to connect. Integration tests cover this scenario.
    }

    @Test
    void testValidationRejectsDnTemplateWithoutPlaceholder() {
        assertThatThrownBy(() -> new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid=fixed,ou=people,{baseDn}", // missing {username}
                DEFAULT_BASE_DN,
                "ADMIN",
                DEFAULT_SIMPLE_BIND))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User DN template must contain {username} placeholder");
    }

    @Test
    void testCreateUserDnResolver() {
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid={username},ou=people,{baseDn}",
                "dc=example,dc=com",
                "ADMIN",
                DEFAULT_SIMPLE_BIND);

        final var resolver = props.createUserDnResolver();
        assertThat(resolver).isNotNull();
        assertThat(resolver.resolveDn("jdoe"))
                .isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");
    }
}
