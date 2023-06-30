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
import java.util.stream.Collectors;

/**
 * Simple Authentication chain where the first Success wins, and the last denied wins. For more complex operations
 * you should create your own chain logic.
 *
 * @author Simon L Johnson
 */
public class ChainedAuthenticationHandler extends AbstractAuthenticationHandler implements IAuthenticationHandler {

    private final @NotNull Set<IAuthenticationHandler> handlers;

    public ChainedAuthenticationHandler(final @NotNull Set<IAuthenticationHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public AuthenticationResult authenticate(final ContainerRequestContext requestContext) {
        AuthenticationResult finalResult = null;
        for(IAuthenticationHandler handler : handlers){
            AuthenticationResult result = handler.authenticate(requestContext);
            if(result.isSuccess()){
                finalResult = result;
                break;
            } else {
                finalResult = result;
            }
        }
        return finalResult;
    }

    @Override
    public void decorateResponse(final AuthenticationResult result, final Response.ResponseBuilder builder) {
        result.getAuthenticationHandler().decorateResponse(result, builder);
    }

    @Override
    public String getMethod() {
        return handlers.stream().map(IAuthenticationHandler::getMethod).collect(Collectors.joining(","));
    }
}
