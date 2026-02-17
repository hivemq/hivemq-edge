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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TemplateDnResolver}.
 */
class TemplateDnResolverTest {

    @Test
    void testOpenLdapTemplate() {
        final TemplateDnResolver resolver = new TemplateDnResolver("uid", "ou=people,dc=example,dc=com");

        assertThat(resolver.resolveDn("jdoe")).isEqualTo("uid=jdoe,ou=people,dc=example,dc=com");
    }

    @Test
    void testActiveDirectoryTemplate() {
        final TemplateDnResolver resolver = new TemplateDnResolver("cn", "cn=Users,dc=company,dc=com");

        assertThat(resolver.resolveDn("John Doe")).isEqualTo("cn=John Doe,cn=Users,dc=company,dc=com");
    }

    @Test
    void testEmailBasedTemplate() {
        final TemplateDnResolver resolver = new TemplateDnResolver("mail", "ou=staff,dc=company,dc=com");

        assertThat(resolver.resolveDn("jdoe@company.com"))
                .isEqualTo("mail=jdoe@company.com,ou=staff,dc=company,dc=com");
    }

    @Test
    void testCustomAttributeTemplate() {
        final TemplateDnResolver resolver = new TemplateDnResolver("employeeNumber", "ou=employees,dc=corp,dc=com");

        assertThat(resolver.resolveDn("12345")).isEqualTo("employeeNumber=12345,ou=employees,dc=corp,dc=com");
    }

    @Test
    void testMultipleOuTemplate() {
        final TemplateDnResolver resolver = new TemplateDnResolver("uid", "ou=engineering,ou=staff,dc=company,dc=com");

        assertThat(resolver.resolveDn("jdoe")).isEqualTo("uid=jdoe,ou=engineering,ou=staff,dc=company,dc=com");
    }

    @Test
    void testValidationRejectsEmptyTemplate() {
        assertThatThrownBy(() -> new TemplateDnResolver("", "dc=example,dc=com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uidAttribute cannot be empty");
    }

    @Test
    void testValidationRejectsBlankTemplate() {
        assertThatThrownBy(() -> new TemplateDnResolver("   ", "dc=example,dc=com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("uidAttribute cannot be empty");
    }

    @Test
    void testValidationRejectsEmptyBaseDn() {
        assertThatThrownBy(() -> new TemplateDnResolver("uid", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Base DN cannot be empty");
    }

    @Test
    void testResolveDnRejectsEmptyUsername() {
        final TemplateDnResolver resolver = new TemplateDnResolver("uid", "dc=example,dc=com");

        assertThatThrownBy(() -> resolver.resolveDn(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void testResolveDnRejectsBlankUsername() {
        final TemplateDnResolver resolver = new TemplateDnResolver("uid", "dc=example,dc=com");

        assertThatThrownBy(() -> resolver.resolveDn("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username cannot be empty");
    }

    @Test
    void testGetUidAttribute() {
        final String uidAttribute = "uid";
        final TemplateDnResolver resolver = new TemplateDnResolver(uidAttribute, "dc=example,dc=com");

        assertThat(resolver.getUidAttribute()).isEqualTo(uidAttribute);
    }

    @Test
    void testGetBaseDn() {
        final String baseDn = "dc=example,dc=com";
        final TemplateDnResolver resolver = new TemplateDnResolver("uid={username},ou=people,{baseDn}", baseDn);

        assertThat(resolver.getBaseDn()).isEqualTo(baseDn);
    }
}
