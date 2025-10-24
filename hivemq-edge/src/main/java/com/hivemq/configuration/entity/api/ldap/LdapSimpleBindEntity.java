package com.hivemq.configuration.entity.api.ldap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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

    public LdapSimpleBindEntity() {
    }

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
        if (o == null || getClass() != o.getClass()) return false;
        final LdapSimpleBindEntity that = (LdapSimpleBindEntity) o;
        return Objects.equals(getRdns(), that.getRdns()) && Objects.equals(getUserPassword(), that.getUserPassword());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRdns(), getUserPassword());
    }
}
