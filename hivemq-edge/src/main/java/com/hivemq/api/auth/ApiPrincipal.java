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
import com.hivemq.extension.sdk.api.annotations.NotNull;

import java.security.Principal;
import java.util.Set;

/**
 * @author Simon L Johnson
 */
public class ApiPrincipal implements Principal {

    private @NotNull String name;
    private Set<String> roles;

    public ApiPrincipal() {
    }

    public ApiPrincipal(final @NotNull String name, final @NotNull Set<String> roles) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(roles);
        this.name = name;
        this.roles = roles;
    }

    @Override
    public String getName() {
        return name;
    }

    public boolean hasRole(String role){
        return roles.contains(role);
    }

    public @NotNull Set<String> getRoles(){
        return roles;
    }

    public void setRoles(final Set<String> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiPrincipal{");
        sb.append("name='").append(name).append('\'');
        sb.append(", roles=").append(roles);
        sb.append('}');
        return sb.toString();
    }
}
