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

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * This class represents the simple bind credentials for an LDAP connection.
 */
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings("NotNullFieldNotInitialized")
public class LdapSimpleBindEntity {

    @XmlElement(name = "rdns", required = true)
    private @NotNull String rdns;

    @XmlElement(name = "userPassword", required = true)
    private @NotNull String userPassword;

    public LdapSimpleBindEntity() {}

    public LdapSimpleBindEntity(final @NotNull String rdns, final @NotNull String password) {
        this.rdns = rdns;
        this.userPassword = password;
    }

    public @NotNull String getRdns() {
        return rdns;
    }

    public @NotNull String getUserPassword() {
        return userPassword;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof LdapSimpleBindEntity)) return false;
        final LdapSimpleBindEntity that = (LdapSimpleBindEntity) o;
        return Objects.equals(getRdns(), that.getRdns()) && Objects.equals(getUserPassword(), that.getUserPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRdns(), getUserPassword());
    }
}
