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

import com.unboundid.ldap.sdk.SearchScope;
import org.junit.jupiter.api.Test;

import static com.hivemq.api.auth.ApiRoles.ADMIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link LdapConnectionProperties} baseDn functionality.
 * Tests the dual-mode behavior: legacy mode (no baseDn) vs new mode (with baseDn).
 */
class LdapConnectionPropertiesBaseDnTest {

    private static final LdapConnectionProperties.LdapSimpleBind DEFAULT_SIMPLE_BIND =
            new LdapConnectionProperties.LdapSimpleBind("cn=admin", "admin");

    @Test
    void testLegacyMode_nullBaseDn_rdnsIsAbsolute() {
        // Legacy mode: baseDn is null, rdns is absolute DN
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people,dc=example,dc=com",
                null,  // baseDn is null = legacy mode
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        // Create DN resolver - should use rdns as-is (absolute)
        final var resolver = props.createUserDnResolver(null);
        assertThat(resolver.resolveDn("jdoe"))
                .isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");
    }

    @Test
    void testLegacyMode_emptyBaseDn_rdnsIsAbsolute() {
        // Legacy mode: baseDn is empty string, rdns is absolute DN
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people,dc=example,dc=com",
                "",  // baseDn is empty = legacy mode
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        // Create DN resolver - should use rdns as-is (absolute)
        final var resolver = props.createUserDnResolver(null);
        assertThat(resolver.resolveDn("jdoe"))
                .isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");
    }

    @Test
    void testLegacyMode_blankBaseDn_rdnsIsAbsolute() {
        // Legacy mode: baseDn is whitespace, rdns is absolute DN
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people,dc=example,dc=com",
                "   ",  // baseDn is whitespace = legacy mode
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        // Create DN resolver - should use rdns as-is (absolute)
        final var resolver = props.createUserDnResolver(null);
        assertThat(resolver.resolveDn("jdoe"))
                .isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");
    }

    @Test
    void testNewMode_baseDnSpecified_rdnsIsRelative() {
        // New mode: baseDn is specified, rdns is relative to baseDn
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people",  // Relative rdns
                "dc=example,dc=com",  // baseDn
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        // Create DN resolver - should combine rdns + baseDn
        final var resolver = props.createUserDnResolver(null);
        assertThat(resolver.resolveDn("jdoe"))
                .isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");
    }

    @Test
    void testNewMode_multipleRdnsComponents() {
        // New mode: rdns with multiple components
        final LdapConnectionProperties props = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=users,ou=people",  // Multiple RDN components
                "dc=example,dc=com",
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        // Create DN resolver - should combine rdns + baseDn
        final var resolver = props.createUserDnResolver(null);
        assertThat(resolver.resolveDn("jdoe"))
                .isEqualTo("uid=jdoe,ou=users,ou=people,dc=example,dc=com");
    }

    @Test
    void testValidationRejectsInvalidBaseDn() {
        // Invalid baseDn should throw IllegalArgumentException
        assertThatThrownBy(() -> new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people",
                "invalid-dn-format",  // Invalid DN format
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid base DN");
    }

    @Test
    void testValidationRejectsInvalidRdns() {
        // Invalid rdns should throw IllegalArgumentException
        assertThatThrownBy(() -> new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "invalid-rdns-format",  // Invalid DN format
                null,
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid rdns");
    }

    @Test
    void testEqualsAndHashCode_withBaseDn() {
        final LdapConnectionProperties props1 = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people",
                "dc=example,dc=com",
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        final LdapConnectionProperties props2 = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people",
                "dc=example,dc=com",
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        final LdapConnectionProperties props3 = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people",
                "dc=different,dc=org",  // Different baseDn
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        assertThat(props1).isEqualTo(props2);
        assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
        assertThat(props1).isNotEqualTo(props3);
    }

    @Test
    void testEqualsAndHashCode_nullVsNonNullBaseDn() {
        final LdapConnectionProperties propsWithBaseDn = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people",
                "dc=example,dc=com",
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        final LdapConnectionProperties propsWithoutBaseDn = new LdapConnectionProperties(
                new LdapConnectionProperties.LdapServers(new String[]{"localhost"}, new int[]{389}),
                TlsMode.NONE,
                null,
                0,
                0,
                1,
                "uid",
                "ou=people",
                null,  // No baseDn
                null,
                SearchScope.BASE,
                5,
                ADMIN,
                false,
                DEFAULT_SIMPLE_BIND,
                null);

        assertThat(propsWithBaseDn).isNotEqualTo(propsWithoutBaseDn);
    }
}
