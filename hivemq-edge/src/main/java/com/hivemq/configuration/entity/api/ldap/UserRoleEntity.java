package com.hivemq.configuration.entity.api.ldap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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

    public UserRoleEntity() {
    }

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
        if (o == null || getClass() != o.getClass())
            return false;
        final UserRoleEntity that = (UserRoleEntity) o;
        return Objects.equals(getRole(), that.getRole()) && Objects.equals(getQuery(), that.getQuery());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRole(), getQuery());
    }
}
