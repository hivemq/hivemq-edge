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
import com.hivemq.api.auth.handler.AuthenticationResult;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Optional;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractHeaderAuthenticationHandler extends AbstractAuthenticationHandler {

    @Override
    public AuthenticationResult authenticate(final ContainerRequestContext requestContext) {
        Preconditions.checkNotNull(requestContext);
        final @NotNull MultivaluedMap<String, String> headers = requestContext.getHeaders();
        String value = headers.getFirst(HttpConstants.AUTH_HEADER);
        if(value != null){
            Optional<String> authValue = extractAuthValue(getMethod(), value);
            if(authValue.isPresent()){
                return authenticateInternal(requestContext, authValue.get());
            }
        }
        return AuthenticationResult.noinfo(this);
    }

    protected static Optional<String> extractAuthValue(String expectedMethod, String headerValue){
        headerValue = headerValue.trim();
        String[] val = headerValue.split(" ");
        if(val.length == 2){
            if(expectedMethod.equals(val[0])){
                return Optional.of(val[1]);
            }
        }
        return Optional.empty();
    }


    protected abstract AuthenticationResult authenticateInternal(final @NotNull ContainerRequestContext requestContext, String authHeaderValue);
}
