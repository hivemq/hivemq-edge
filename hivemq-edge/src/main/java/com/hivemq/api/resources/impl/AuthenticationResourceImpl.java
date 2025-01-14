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
import com.hivemq.api.errors.authentication.AuthenticationValidationError;
import com.hivemq.api.errors.authentication.UnauthorizedError;
import com.hivemq.api.model.ApiErrorMessages;
import com.hivemq.api.utils.ApiErrorUtils;
import com.hivemq.edge.api.AuthenticationApi;
import com.hivemq.edge.api.model.ApiBearerToken;
import com.hivemq.edge.api.model.UsernamePasswordCredentials;
import com.hivemq.http.core.UsernamePasswordRoles;
import com.hivemq.util.ErrorResponseUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public AuthenticationResourceImpl(
            final @NotNull IUsernamePasswordProvider usernamePasswordProvider,
            final @NotNull ITokenGenerator tokenGenerator,
            final @NotNull ITokenVerifier tokenVerifier) {
        this.usernamePasswordProvider = usernamePasswordProvider;
        this.tokenGenerator = tokenGenerator;
        this.tokenVerifier = tokenVerifier;
    }



    @Override
    public @NotNull Response authenticate(final @Nullable UsernamePasswordCredentials credentials) {

        final ApiErrorMessages errorMessages = ApiErrorUtils.createErrorContainer();
        ApiErrorUtils.validateRequiredField(errorMessages, "userName", credentials != null ? credentials.getUserName() : null, false);
        ApiErrorUtils.validateRequiredField(errorMessages, "password", credentials != null ? credentials.getPassword() : null, false);

        if (ApiErrorUtils.hasRequestErrors(errorMessages)) {
            return ErrorResponseUtil.errorResponse(new AuthenticationValidationError(errorMessages.toErrorList()));
        } else {
            final String userName = credentials.getUserName();
            final String password = credentials.getPassword();
            final Optional<UsernamePasswordRoles> usernamePasswordRoles = usernamePasswordProvider.findByUsername(userName);
            if (usernamePasswordRoles.isPresent()) {
                final UsernamePasswordRoles user = usernamePasswordRoles.get();
                if (user.getPassword().equals(password)) {
                    try {
                        final ApiBearerToken token = new ApiBearerToken().token(tokenGenerator.generateToken(user.toPrincipal()));
                        if (logger.isTraceEnabled()) {
                            logger.trace("Bearer authentication was success, token generated for {}",
                                    user.getUserName());
                        }
                        return Response.ok(token).build();
                    } catch (final AuthenticationException e) {
                        logger.warn("Authentication failed with error", e);
                        throw new ApiException("error encountered during authentication", e);
                    }
                }
            }
            return ErrorResponseUtil.errorResponse(new UnauthorizedError("Invalid username and/or password"));
        }
    }

    @Override
    public @NotNull Response validateToken(final @Nullable ApiBearerToken token) {
        Preconditions.checkNotNull(token);
        Preconditions.checkState(token.getToken() != null, "Token value cannot be <null>");
        final Optional<ApiPrincipal> principal = tokenVerifier.verify(token.getToken());
        if (principal.isPresent()) {
            return Response.ok().build();
        } else {
            return ErrorResponseUtil.errorResponse(new UnauthorizedError("Invalid username and/or password"));
        }
    }

    @Override
    public @NotNull Response refreshToken() {
        try {
            final ApiPrincipal principal = getAuthenticatedPrincipalFromContext();
            final ApiBearerToken token = new ApiBearerToken().token(tokenGenerator.generateToken(principal));
            if (logger.isTraceEnabled()) {
                logger.trace("Token reissue requested for {}", principal.getName());
            }
            return Response.ok(token).build();
        } catch (final AuthenticationException e) {
            logger.warn("Authentication failed with error", e);
            throw new ApiException("error encountered during authentication", e);
        }
    }
}
