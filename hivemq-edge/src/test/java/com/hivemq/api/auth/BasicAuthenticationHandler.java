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
import com.hivemq.api.auth.handler.AuthenticationResult;
import com.hivemq.api.auth.handler.impl.AbstractHeaderAuthenticationHandler;
import com.hivemq.api.auth.provider.IUsernameRolesProvider;
import com.hivemq.http.HttpConstants;
import com.hivemq.http.core.UsernamePasswordRoles;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public class BasicAuthenticationHandler extends AbstractHeaderAuthenticationHandler {

    static final String SEP = ":";
    static final String METHOD = "Basic";
    private final IUsernameRolesProvider provider;


    public static String getBasicAuthenticationHeaderValue(final @NotNull String username, final @NotNull String password) {
        final var valueToEncode = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(valueToEncode.getBytes());
    }

    public BasicAuthenticationHandler(final @NotNull IUsernameRolesProvider provider) {
        this.provider = provider;
    }

    @Override
    protected AuthenticationResult authenticateInternal(final @NotNull ContainerRequestContext requestContext,
                                                        final @NotNull String authValue) {
        return parseValue(authValue)
                .flatMap(supplied ->
                        provider.findByUsernameAndPassword(
                            supplied.getUserName(),
                            supplied.getPassword()))
                .map(record -> {
                    final var result = AuthenticationResult.allowed(this);
                    result.setPrincipal(record.toPrincipal());
                    return result;
                }).orElseGet(() -> AuthenticationResult.denied(this));
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
        final var userPass = new String(Base64.getDecoder().decode(headerValue.trim()));
        if(userPass.contains(SEP)){
            final var userNamePassword = userPass.split(SEP);
            if(userNamePassword.length == 2){
                final var usernamePassword = new UsernamePasswordRoles();
                usernamePassword.setUserName(userNamePassword[0]);
                usernamePassword.setPassword(userNamePassword[1].getBytes(StandardCharsets.UTF_8));
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
