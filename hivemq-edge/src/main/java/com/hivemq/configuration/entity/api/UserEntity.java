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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})
@XmlRootElement(name = "user")
@XmlAccessorType(XmlAccessType.NONE)
public class UserEntity {
    @XmlElementWrapper(name = "roles")
    @XmlElement(name = "role")
    private final @NotNull List<String> roles = new ArrayList<>();

    @XmlElement(name = "username")
    private @Nullable String userName = null;

    @XmlElement(name = "password")
    private @Nullable String password = null;

    public @Nullable String getUserName() {
        return userName;
    }

    public @Nullable String getPassword() {
        return password;
    }

    public @NotNull List<String> getRoles() {
        return roles;
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof final UserEntity that) {
            return Objects.equals(userName, that.userName) &&
                    Objects.equals(password, that.password) &&
                    Objects.equals(roles, that.roles);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(userName, password, roles);
    }
}
