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

import com.hivemq.api.auth.handler.AuthenticationResult;
import com.hivemq.api.auth.handler.IAuthenticationHandler;
import com.hivemq.extension.sdk.api.annotations.NotNull;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * @author Simon L Johnson
 */
public abstract class AbstractAuthenticationHandler implements IAuthenticationHandler {

    @Override
    public boolean authorized(
            final @NotNull ContainerRequestContext requestContext,
            final @NotNull Set<String> permissions) {
        if(requestContext.getSecurityContext() == null){
            return false;
        }
        boolean authorized = false;
        for(String permission : permissions){
            authorized |= requestContext.getSecurityContext().isUserInRole(permission);
        }
        return authorized;
    }

    @Override
    public void decorateResponse(final AuthenticationResult result, final Response.ResponseBuilder builder) {
        //Hook method for IAuthenticationHandler's to send back custom/modified responses,
        //by default do nothing
    }
}
