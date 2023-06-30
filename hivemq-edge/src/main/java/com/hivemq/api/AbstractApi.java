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
package com.hivemq.api;

import com.hivemq.api.auth.ApiPrincipal;
import com.hivemq.api.error.ApiException;
import com.hivemq.http.core.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import java.net.http.HttpRequest;
import java.security.Principal;

/**
 * Convenience class to expose common injection attributes into the instance.
 *
 * @author Simon L Johnson
 */
public abstract class AbstractApi {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    //-- The Security Context associated with the HttpRequest
    @Context protected SecurityContext securityContext;

    //-- The Headers associated with the HttpRequest
    @Context protected HttpHeaders headers;

    //-- The HttpRequest
    @Context protected HttpRequest request;

    //-- The ResourceContext
    @Context protected ResourceContext context;

    //-- The UriInfo associated with the HttpRequest
    @Context protected UriInfo uriInfo;

    protected ApiPrincipal getAuthenticatedPrincipalFromContext() throws ApiException {
        if(securityContext.isSecure() &&
                securityContext.getUserPrincipal() == null){
            throw new ApiException("secure principal not available on context",
                    HttpConstants.SC_UNAUTHORIZED);
        }
        Principal principal = securityContext.getUserPrincipal();
        if(!(principal instanceof ApiPrincipal)){
            throw new ApiException("invalid principal type set on context",
                    HttpConstants.SC_UNAUTHORIZED);
        }
        return (ApiPrincipal) principal;
    }
}
