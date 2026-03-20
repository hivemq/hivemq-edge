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

import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public class ApiSecurityContext implements SecurityContext {

    private final @NotNull ApiPrincipal principal;
    private @Nullable String authenticationScheme;
    private final boolean isSecure;

    public ApiSecurityContext(
            final @NotNull ApiPrincipal principal,
            final @Nullable String authenticationScheme,
            final boolean isSecure) {
        this.principal = principal;
        this.authenticationScheme = authenticationScheme;
        this.isSecure = isSecure;
    }

    @Override
    public Principal getUserPrincipal() {
        return principal;
    }

    @Override
    public boolean isUserInRole(final String role) {
        return principal.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public @Nullable String getAuthenticationScheme() {
        return authenticationScheme;
    }

    public void setAuthenticationScheme(final @NotNull String authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

    @Override
    public String toString() {
        return "ApiSecurityContext{" +
                "principal=" +
                principal +
                ", authenticationScheme='" +
                authenticationScheme +
                '\'' +
                ", isSecure=" +
                isSecure +
                '}';
    }
}
