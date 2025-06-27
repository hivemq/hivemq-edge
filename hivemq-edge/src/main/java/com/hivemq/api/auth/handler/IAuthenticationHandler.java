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

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import java.util.Set;

/**
 * @author Simon L Johnson
 */
public interface IAuthenticationHandler {

    AuthenticationResult authenticate(ContainerRequestContext requestContext);

    boolean authorized(ContainerRequestContext requestContext, Set<String> permissions);

    void decorateResponse(final AuthenticationResult result, final Response.ResponseBuilder builder);

    String getMethod();

}
