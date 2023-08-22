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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Response filter that obtains the details of the inbound HttpRequest & the output HttpResponse. When enabled, details are
 * logged to the JaxrsResponseFilter logger at info.
 *
 * @author Simon L Johnson
 */
@Provider
public class JaxrsResponseFilter implements ContainerResponseFilter {

    protected final Logger logger = LoggerFactory.getLogger(JaxrsResponseFilter.class);
    private static final boolean DEBUG = Boolean.getBoolean(HiveMQEdgeConstants.DEVELOPMENT_MODE);

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext responseContext)
            throws IOException {
        if (DEBUG) {
            if (logger.isInfoEnabled()) {
                if(responseContext.getStatus() >= 400){
                    logger.info("Http [{}] Error Response [{}] [{}] -> url [{}]",
                            requestContext.getRequest().getMethod(),
                            responseContext.getStatus(),
                            responseContext.getStatusInfo().getReasonPhrase(),
                            requestContext.getUriInfo().getRequestUri());
                } else {
                    logger.info("Http [{}] Response [{}] -> url [{}]",
                            requestContext.getRequest().getMethod(),
                            responseContext.getStatus(),
                            requestContext.getUriInfo().getRequestUri());
                }
            }
        }
    }
}
