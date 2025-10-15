package com.hivemq.configuration.entity.api;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

@XmlRootElement(name = "userlist")
@XmlAccessorType(XmlAccessType.NONE)
public class UserlistBasedUsernameRolesSourceEntity extends UsernameRolesSourceEntity {

    @XmlElementWrapper(name = "users")
    @XmlElementRef(required = false)
    private @NotNull List<UserEntity> users;

    public @NotNull List<UserEntity> getUsers() {
        return users;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        final UserlistBasedUsernameRolesSourceEntity that = (UserlistBasedUsernameRolesSourceEntity) o;
        return Objects.equals(getUsers(), that.getUsers());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUsers());
    }
}
