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
package com.hivemq.http;

import com.hivemq.edge.HiveMQEdgeConstants;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.glassfish.jersey.server.ContainerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Request filter that obtains the details of the inbound HttpRequest. When enabled, details are
 * logged to the JaxrsRequestFilter logger at DEBUG.
 *
 * @author Simon L Johnson
 */
@Provider
public class JaxrsRequestFilter implements ContainerRequestFilter {

    protected final Logger logger = LoggerFactory.getLogger(JaxrsRequestFilter.class);
    private static final boolean DEBUG = Boolean.getBoolean(HiveMQEdgeConstants.DEVELOPMENT_MODE);

    @Override
    public void filter(@NotNull final ContainerRequestContext ctx) throws IOException {
        if (DEBUG) {
//            printUriInfo(ctx.getUriInfo());
//            printRequest(ctx.getRequest());
//            printHeaders(ctx.getHeaders());
//            printSecurityContext(ctx.getSecurityContext());
            if (logger.isInfoEnabled()) {
                logger.info("Http Request [{}] -> [{}]",
                        ctx.getRequest().getMethod(),
                        ctx.getUriInfo().getRequestUri());
            }
        }
    }

    protected void printHeaders(@NotNull final MultivaluedMap<String, String> headers) {
        for (String header : headers.keySet()) {
            logger.debug("*** Http-Header: {} -> {}",
                    header,
                    headers.get(header).stream().map(Object::toString).collect(Collectors.joining()));
        }
    }

    protected void printUriInfo(@NotNull final UriInfo info) {
        logger.debug("*** Http-Uri-Absolute: {}", info.getAbsolutePath());
        logger.debug("*** Http-Uri-Base: {}", info.getBaseUri());
        logger.debug("*** Http-Uri-Request: {}", info.getRequestUri());
        logger.debug("*** Http-Uri-Path: {}", info.getPath());
        logger.debug("*** Http-Uri-QueryParams: {}", info.getQueryParameters());
        logger.debug("*** Http-Uri-Resources: {}", info.getMatchedResources());
        logger.debug("*** Http-Uri-Matched-Uri: {}", info.getMatchedURIs());

    }

    protected void printSecurityContext(@NotNull final SecurityContext securityContext) {
        logger.debug("*** Http-Security-Context-Principal: {}", securityContext.getUserPrincipal());
        logger.debug("*** Http-Security-Context-Scheme: {}", securityContext.getAuthenticationScheme());
    }

    protected void printRequest(@NotNull final Request request) {

        if (request instanceof ContainerRequest) {
            ContainerRequest r = (ContainerRequest) request;
            logger.debug("*** Http-Request-Length: {}", r.getLength());
            logger.debug("*** Http-Request-Language: {}", r.getLanguage());
            logger.debug("*** Http-Request-Media-Types: {}", r.getAcceptableMediaTypes());
        }
        logger.debug("*** Http-Request-Method: {}", request.getMethod());
    }
}
