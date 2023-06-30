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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

/**
 * @author Simon L Johnson
 */
public class ApiSecurityContext implements SecurityContext {

    private @NotNull ApiPrincipal principal;
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
        return principal != null && principal.hasRole(role);
    }

    @Override
    public boolean isSecure() {
        return isSecure;
    }

    @Override
    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    public void setAuthenticationScheme(final String authenticationScheme) {
        this.authenticationScheme = authenticationScheme;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApiSecurityContext{");
        sb.append("principal=").append(principal);
        sb.append(", authenticationScheme='").append(authenticationScheme).append('\'');
        sb.append(", isSecure=").append(isSecure);
        sb.append('}');
        return sb.toString();
    }
}
