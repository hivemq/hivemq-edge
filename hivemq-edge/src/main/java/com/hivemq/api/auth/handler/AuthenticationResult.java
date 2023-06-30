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
package com.hivemq.api.auth.handler;

import com.google.common.base.Preconditions;
import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.extension.sdk.api.annotations.NotNull;

/**
 * @author Simon L Johnson
 */
public class AuthenticationResult {

    enum STATUS {
        ALLOWED, DENIED, INSUFFICIENT_INFORMATION }
    private ApiPrincipal principal;
    private STATUS status;
    private final IAuthenticationHandler authenticationHandler;

    public AuthenticationResult(final @NotNull STATUS status, final @NotNull IAuthenticationHandler authenticationHandler) {
        Preconditions.checkNotNull(status);
        Preconditions.checkNotNull(authenticationHandler);
        this.authenticationHandler = authenticationHandler;
        this.status = status;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(final @NotNull STATUS status) {
        this.status = status;
    }

    public ApiPrincipal getPrincipal() {
        return principal;
    }

    public void setPrincipal(final @NotNull ApiPrincipal principal) {
        this.principal = principal;
    }

    public boolean isSuccess(){
        return status == STATUS.ALLOWED;
    }

    public String getAuthenticationMethod() {
        return authenticationHandler.getMethod();
//        return authenticationHandlers.stream().map(s -> s.getMethod()).collect(Collectors.joining(","));
    }

    public IAuthenticationHandler getAuthenticationHandler() {
        return authenticationHandler;
    }

    public static AuthenticationResult denied(final @NotNull IAuthenticationHandler authenticationHandler){
        Preconditions.checkNotNull(authenticationHandler);
        return new AuthenticationResult(STATUS.DENIED, authenticationHandler);
    }

    public static AuthenticationResult allowed(final @NotNull IAuthenticationHandler authenticationHandler){
        Preconditions.checkNotNull(authenticationHandler);
        return new AuthenticationResult(STATUS.ALLOWED, authenticationHandler);
    }

    public static AuthenticationResult noinfo(final @NotNull IAuthenticationHandler authenticationHandler){
        Preconditions.checkNotNull(authenticationHandler);
        return new AuthenticationResult(STATUS.INSUFFICIENT_INFORMATION, authenticationHandler);
    }
}
