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
package com.hivemq.api.auth.handler.impl;

import com.google.common.base.Preconditions;
import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.auth.handler.AuthenticationResult;
import com.hivemq.api.auth.provider.IUsernamePasswordProvider;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import com.hivemq.http.core.UsernamePasswordRoles;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Base64;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
@Singleton
public class BasicAuthenticationHandler extends AbstractHeaderAuthenticationHandler {

    static final String SEP = ":";
    static final String METHOD = "Basic";
    private final IUsernamePasswordProvider provider;

    @Inject
    public BasicAuthenticationHandler(final @NotNull IUsernamePasswordProvider provider) {
        this.provider = provider;
    }

    @Override
    protected AuthenticationResult authenticateInternal(final @NotNull ContainerRequestContext requestContext, String authValue) {
        Optional<UsernamePasswordRoles> usernamePassword = parseValue(authValue);
        if(usernamePassword.isPresent()){
            UsernamePasswordRoles supplied = usernamePassword.get();
            Optional<UsernamePasswordRoles> record = provider.findByUsername(supplied.getUserName());
            if(record.isPresent() && record.get().getPassword().equals(supplied.getPassword())){
                AuthenticationResult result = AuthenticationResult.allowed(this);
                ApiPrincipal principal = new ApiPrincipal(supplied.getUserName(), record.get().getRoles());
                result.setPrincipal(principal);
                return result;
            }
        }
        return AuthenticationResult.denied(this);
    }

    @Override
    public void decorateResponse(final AuthenticationResult result, final Response.ResponseBuilder builder) {
        if(!result.isSuccess()){
            builder.header(HttpConstants.BASIC_AUTH_CHALLENGE_HEADER,
                    String.format(HttpConstants.BASIC_AUTH_REALM, SecurityContext.BASIC_AUTH));
        }
    }

    protected static Optional<UsernamePasswordRoles> parseValue(final @NotNull String headerValue){
        Preconditions.checkNotNull(headerValue);
        String userPass = headerValue.trim();
        userPass = new String(Base64.getDecoder().decode(userPass));
        if(userPass.contains(SEP)){
            String[] userNamePassword = userPass.split(SEP);
            if(userNamePassword.length == 2){
                UsernamePasswordRoles usernamePassword = new UsernamePasswordRoles();
                usernamePassword.setUserName(userNamePassword[0]);
                usernamePassword.setPassword(userNamePassword[1]);
                return Optional.of(usernamePassword);
            }
        }
        return Optional.empty();
    }

    @Override
    public String getMethod() {
        return METHOD;
    }
}
