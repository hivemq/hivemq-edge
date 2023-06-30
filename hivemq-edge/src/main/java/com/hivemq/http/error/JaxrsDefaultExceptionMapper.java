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
package com.hivemq.http.error;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Simon L Johnson
 */
@Provider
@Singleton
public class JaxrsDefaultExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger =
            LoggerFactory.getLogger(JaxrsDefaultExceptionMapper.class);

    @Override
    public Response toResponse(@NotNull final Throwable exception) {
        Response r = null;
        if(exception instanceof WebApplicationException){
            WebApplicationException e = (WebApplicationException) exception;
            r = e.getResponse();
        }

        if(r == null){
            logger.info("Unhandled API Error dealt with by catch all mapper", exception);
            UnhandledMessage message = new UnhandledMessage();
            message.message = "An unknown error occurred processing your request";
            message.cause = exception.getMessage();
            r = Response.status(HttpConstants.SC_INTERNAL_SERVER_ERROR)
                    .entity(message)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        return r;
    }

    class UnhandledMessage {
        public String message;
        public String cause;
    }
}
