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
 * This class represents a user role mapping for LDAP authentication.
 * Each user role contains a role name and an LDAP query.
 */
@XmlAccessorType(XmlAccessType.NONE)
@SuppressWarnings("NotNullFieldNotInitialized")
public class UserRoleEntity {

    @XmlElement(name = "role", required = true)
    private @NotNull String role;

    @XmlElement(name = "query", required = true)
    private @NotNull String query;

    public UserRoleEntity() {}

    public UserRoleEntity(final @NotNull String role, final @NotNull String query) {
        this.role = role;
        this.query = query;
    }

    public @NotNull String getRole() {
        return role;
    }

    public @NotNull String getQuery() {
        return query;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof UserRoleEntity)) return false;
        final UserRoleEntity that = (UserRoleEntity) o;
        return Objects.equals(getRole(), that.getRole()) && Objects.equals(getQuery(), that.getQuery());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRole(), getQuery());
    }
}
