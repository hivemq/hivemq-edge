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
package com.hivemq.api.auth;

import com.google.common.base.Preconditions;
import java.security.Principal;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public class ApiPrincipal implements Principal {

    private @NotNull String name;
    private @NotNull Set<String> roles;

    @SuppressWarnings("NullAway.Init")
    public ApiPrincipal() {
        name = "";
        roles = Set.of();
    }

    public ApiPrincipal(final @NotNull String name, final @NotNull Set<String> roles) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(roles);
        this.name = name;
        this.roles = roles;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    public boolean hasRole(final @NotNull String wantedRole) {
        for (final String role : roles) {
            if (role.equalsIgnoreCase(wantedRole)) {
                return true;
            }
        }
        return false;
    }

    public @NotNull Set<String> getRoles() {
        return roles;
    }

    public void setRoles(final @NotNull Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "ApiPrincipal{" + "name='" + name + '\'' + ", roles=" + roles + '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof final ApiPrincipal that)) return false;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getRoles(), that.getRoles());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getRoles());
    }
}
