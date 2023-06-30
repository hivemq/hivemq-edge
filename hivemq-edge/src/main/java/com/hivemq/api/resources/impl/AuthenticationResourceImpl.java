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
package com.hivemq.api.resources.impl;

import com.google.common.base.Preconditions;
import com.hivemq.api.AbstractApi;
import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.auth.AuthenticationException;
import com.hivemq.api.auth.provider.ITokenGenerator;
import com.hivemq.api.auth.provider.ITokenVerifier;
import com.hivemq.api.auth.provider.IUsernamePasswordProvider;
import com.hivemq.api.error.ApiException;
import com.hivemq.api.model.ApiErrorMessage;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.model.auth.ApiBearerToken;
import com.hivemq.api.model.auth.UsernamePasswordCredentials;
import com.hivemq.api.resources.AuthenticationApi;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.UsernamePasswordRoles;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
@Singleton
public class AuthenticationResourceImpl extends AbstractApi implements AuthenticationApi {

    private final @NotNull ITokenGenerator tokenGenerator;
    private final @NotNull ITokenVerifier tokenVerifier;
    private final @NotNull IUsernamePasswordProvider usernamePasswordProvider;

    @Inject
    public AuthenticationResourceImpl(final @NotNull IUsernamePasswordProvider usernamePasswordProvider,
                                      final @NotNull ITokenGenerator tokenGenerator,
                                      final @NotNull ITokenVerifier tokenVerifier) {
        this.usernamePasswordProvider = usernamePasswordProvider;
        this.tokenGenerator = tokenGenerator;
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public Response authenticate(final UsernamePasswordCredentials credentials) {

        ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();

        ApiErrorUtils.validateRequiredEntity(errorMessages, "credentials", credentials);
        ApiErrorUtils.validateRequiredField(errorMessages, "userName", credentials.getUserName(), false);
        ApiErrorUtils.validateRequiredField(errorMessages, "password", credentials.getPassword(), false);

        if(ApiErrorUtils.hasRequestErrors(errorMessages)){
            return ApiErrorUtils.badRequest(errorMessages);
        } else {
            final String userName = credentials.getUserName();
            final String password = credentials.getPassword();
            Optional<UsernamePasswordRoles> usernamePasswordRoles =
                    usernamePasswordProvider.findByUsername(userName);
            if(usernamePasswordRoles.isPresent()){
                UsernamePasswordRoles user = usernamePasswordRoles.get();
                if(user.getPassword().equals(password)){
                    try {
                        ApiBearerToken token = new ApiBearerToken(tokenGenerator.generateToken(user.toPrincipal()));
                        if(logger.isTraceEnabled()){
                            logger.trace("Bearer authentication was success, token generated for {}", user.getUserName());
                        }
                        return Response.status(200).entity(token).build();
                    } catch (AuthenticationException e){
                        logger.warn("Authentication failed with error", e);
                        throw new ApiException("error encountered during authentication", e);
                    }
                }
            }
            return Response.status(401).entity(
                    ApiErrorMessage.from("Invalid username and/or password")).build();
        }
    }

    @Override
    public Response validate(final ApiBearerToken token) {
        Preconditions.checkNotNull(token);
        Preconditions.checkState(token.getToken() != null, "Token value cannot be <null>");
        Optional<ApiPrincipal> principal = tokenVerifier.verify(token.getToken());
        if(principal.isPresent()){
            return Response.ok().build();
        } else {
            return Response.status(401).entity(
                    ApiErrorMessage.from("Invalid token")).build();
        }
    }

    @Override
    public Response reissueToken() {
        try {
            ApiPrincipal principal = getAuthenticatedPrincipalFromContext();
            ApiBearerToken token = new ApiBearerToken(tokenGenerator.generateToken(principal));
            if(logger.isTraceEnabled()){
                logger.trace("Token reissue requested for {}", principal.getName());
            }
            return Response.status(200).entity(token).build();
        } catch (AuthenticationException e){
            logger.warn("Authentication failed with error", e);
            throw new ApiException("error encountered during authentication", e);
        }
    }
}
