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

package com.hivemq.configuration.entity.api.ldap;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LdapAuthenticationEntityTest {

    @Test
    public void whenUnmarshallingXmlWithUserRoles_thenUserRolesArePopulated() throws Exception {
        final String xml = """
                <ldap>
                    <servers>
                        <ldap-server>
                            <host>ldap.example.com</host>
                            <port>389</port>
                        </ldap-server>
                    </servers>
                    <tls-mode>NONE</tls-mode>
                    <simple-bind>
                        <rdns>cn=admin,dc=example,dc=com</rdns>
                        <userPassword>password</userPassword>
                    </simple-bind>
                    <rdns>dc=example,dc=com</rdns>
                    <user-roles>
                        <user-role>
                            <role>admin</role>
                            <query>(&amp;(objectClass=person)(memberOf=cn=admins,ou=groups,dc=example,dc=com))</query>
                        </user-role>
                        <user-role>
                            <role>user</role>
                            <query>(&amp;(objectClass=person)(memberOf=cn=users,ou=groups,dc=example,dc=com))</query>
                        </user-role>
                    </user-roles>
                </ldap>
                """;

        final JAXBContext context = JAXBContext.newInstance(LdapAuthenticationEntity.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        final LdapAuthenticationEntity entity = (LdapAuthenticationEntity) unmarshaller
                .unmarshal(new StringReader(xml));

        assertThat(entity).isNotNull();
        assertThat(entity.getUserRoles()).isNotNull();
        assertThat(entity.getUserRoles()).hasSize(2);
        assertThat(entity.getUserRoles().get(0).getRole()).isEqualTo("admin");
        assertThat(entity.getUserRoles().get(0).getQuery())
                .isEqualTo("(&(objectClass=person)(memberOf=cn=admins,ou=groups,dc=example,dc=com))");
        assertThat(entity.getUserRoles().get(1).getRole()).isEqualTo("user");
        assertThat(entity.getUserRoles().get(1).getQuery())
                .isEqualTo("(&(objectClass=person)(memberOf=cn=users,ou=groups,dc=example,dc=com))");
    }

    @Test
    public void whenUnmarshallingXmlWithoutUserRoles_thenUserRolesIsNull() throws Exception {
        final String xml = """
                <ldap>
                    <servers>
                        <ldap-server>
                            <host>ldap.example.com</host>
                            <port>389</port>
                        </ldap-server>
                    </servers>
                    <tls-mode>NONE</tls-mode>
                    <simple-bind>
                        <rdns>cn=admin,dc=example,dc=com</rdns>
                        <userPassword>password</userPassword>
                    </simple-bind>
                    <rdns>dc=example,dc=com</rdns>
                </ldap>
                """;

        final JAXBContext context = JAXBContext.newInstance(LdapAuthenticationEntity.class);
        final Unmarshaller unmarshaller = context.createUnmarshaller();
        final LdapAuthenticationEntity entity = (LdapAuthenticationEntity) unmarshaller
                .unmarshal(new StringReader(xml));

        assertThat(entity).isNotNull();
        assertThat(entity.getUserRoles()).isNull();
    }

    @Test
    public void whenMarshallingEntityWithUserRoles_thenXmlContainsUserRoles() throws Exception {
        final LdapAuthenticationEntity entity = new LdapAuthenticationEntity();
        final List<LdapServerEntity> servers = new ArrayList<>();
        servers.add(new LdapServerEntity("ldap.example.com", 389));

        final List<UserRoleEntity> userRoles = new ArrayList<>();
        userRoles.add(
                new UserRoleEntity("admin", "(&(objectClass=person)(memberOf=cn=admins,ou=groups,dc=example,dc=com))"));
        userRoles.add(
                new UserRoleEntity("user", "(&(objectClass=person)(memberOf=cn=users,ou=groups,dc=example,dc=com))"));

        // Use reflection to set private fields for testing
        java.lang.reflect.Field serversField = LdapAuthenticationEntity.class.getDeclaredField("servers");
        serversField.setAccessible(true);
        serversField.set(entity, servers);

        java.lang.reflect.Field simpleBindField = LdapAuthenticationEntity.class.getDeclaredField("simpleBindEntity");
        simpleBindField.setAccessible(true);
        simpleBindField.set(entity, new LdapSimpleBindEntity("cn=admin,dc=example,dc=com", "password"));

        java.lang.reflect.Field rdnsField = LdapAuthenticationEntity.class.getDeclaredField("rdns");
        rdnsField.setAccessible(true);
        rdnsField.set(entity, "dc=example,dc=com");

        java.lang.reflect.Field userRolesField = LdapAuthenticationEntity.class.getDeclaredField("userRoles");
        userRolesField.setAccessible(true);
        userRolesField.set(entity, userRoles);

        final JAXBContext context = JAXBContext.newInstance(LdapAuthenticationEntity.class);
        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        final StringWriter writer = new StringWriter();
        marshaller.marshal(entity, writer);
        final String xml = writer.toString();

        assertThat(xml).contains("<user-roles>");
        assertThat(xml).contains("<user-role>");
        assertThat(xml).contains("<role>admin</role>");
        assertThat(xml)
                .contains("<query>(&amp;(objectClass=person)(memberOf=cn=admins,ou=groups,dc=example,dc=com))</query>");
        assertThat(xml).contains("<role>user</role>");
        assertThat(xml)
                .contains("<query>(&amp;(objectClass=person)(memberOf=cn=users,ou=groups,dc=example,dc=com))</query>");
    }

    @Test
    public void whenMarshallingEntityWithoutUserRoles_thenXmlDoesNotContainUserRoles() throws Exception {
        final LdapAuthenticationEntity entity = new LdapAuthenticationEntity();
        final List<LdapServerEntity> servers = new ArrayList<>();
        servers.add(new LdapServerEntity("ldap.example.com", 389));

        // Use reflection to set private fields for testing
        java.lang.reflect.Field serversField = LdapAuthenticationEntity.class.getDeclaredField("servers");
        serversField.setAccessible(true);
        serversField.set(entity, servers);

        java.lang.reflect.Field simpleBindField = LdapAuthenticationEntity.class.getDeclaredField("simpleBindEntity");
        simpleBindField.setAccessible(true);
        simpleBindField.set(entity, new LdapSimpleBindEntity("cn=admin,dc=example,dc=com", "password"));

        java.lang.reflect.Field rdnsField = LdapAuthenticationEntity.class.getDeclaredField("rdns");
        rdnsField.setAccessible(true);
        rdnsField.set(entity, "dc=example,dc=com");

        final JAXBContext context = JAXBContext.newInstance(LdapAuthenticationEntity.class);
        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        final StringWriter writer = new StringWriter();
        marshaller.marshal(entity, writer);
        final String xml = writer.toString();

        assertThat(xml).doesNotContain("<user-roles>");
        assertThat(xml).doesNotContain("<user-role>");
    }
}
