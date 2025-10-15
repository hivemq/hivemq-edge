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
package com.hivemq.configuration.entity.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * XML entity for LDAP-based username and roles source.
 * <p>
 * Configures the Admin API to authenticate users against an LDAP directory server
 * instead of using the static user list in the configuration file.
 * <p>
 * Example configuration:
 * <pre>{@code
 * <admin-api>
 *     <ldap>
 *         <ldap-authentication>
 *             <host>ldap.example.com</host>
 *             <port>636</port>
 *             <tls-mode>LDAPS</tls-mode>
 *             ...
 *         </ldap-authentication>
 *     </ldap>
 * </admin-api>
 * }</pre>
 */
@XmlRootElement(name = "ldap")
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
public class LdapBasedUsernameRolesSourceEntity {

    @XmlElementRef(required = true)
    private @NotNull LdapAuthenticationEntity ldapAuthentication = new LdapAuthenticationEntity();

    public @NotNull LdapAuthenticationEntity getLdapAuthentication() {
        return ldapAuthentication;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final LdapBasedUsernameRolesSourceEntity that = (LdapBasedUsernameRolesSourceEntity) o;
        return Objects.equals(ldapAuthentication, that.ldapAuthentication);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ldapAuthentication);
    }
}
