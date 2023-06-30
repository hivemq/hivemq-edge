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
import com.hivemq.api.auth.provider.ITokenVerifier;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.Token;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.Optional;

/**
 * @author Simon L Johnson
 * Authorization: Bearer {token}
 */
@Singleton
public class BearerTokenAuthenticationHandler extends AbstractHeaderAuthenticationHandler  {

    static final String METHOD = "Bearer";
    public static final String TOKEN = "Token";
    public static final String REISSUE = "X-Bearer-Token-Reissue";

    private final @NotNull ITokenVerifier tokenVerifier;

    @Inject
    public BearerTokenAuthenticationHandler(final @NotNull ITokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public AuthenticationResult authenticateInternal(
            final @NotNull ContainerRequestContext requestContext,
            final @NotNull String headerValue) {
        Optional<Token> token = parseValue(headerValue);
        if(token.isPresent()){
            String tokenString = token.get().getValue().trim();
            Optional<ApiPrincipal> principal = tokenVerifier.verify(tokenString);
            if(principal.isPresent()){
                AuthenticationResult result = AuthenticationResult.allowed(this);
                if(result.isSuccess()){
                    //-- If we succeed, add the token to the context for other filters to access conveniently.
                    requestContext.setProperty(TOKEN, tokenString);
                }
                result.setPrincipal(principal.get());
                return result;
            }
        }
        return AuthenticationResult.denied(this);
    }

    protected static Optional<Token> parseValue(final @NotNull String headerValue){
        Preconditions.checkNotNull(headerValue);
        return Optional.of(new Token(headerValue));
    }

    @Override
    public String getMethod() {
        return METHOD;
    }
}
