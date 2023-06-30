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
package com.hivemq.api.error;

import com.hivemq.api.model.ApiErrorMessage;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Mapper that handles Uncaught exceptions
 *
 * @author Simon L Johnson
 */
@Provider
public class UncaughtExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = LoggerFactory.getLogger(UncaughtExceptionMapper.class);

    @Override
    public Response toResponse(@NotNull final Throwable exception) {


        Response response = null;
        if (exception instanceof WebApplicationException) {
            WebApplicationException e = (WebApplicationException) exception;
            response = e.getResponse();
        }

        if (response == null) {
            ApiErrorMessage apiError = new ApiErrorMessage();
            apiError.setTitle("The API encountered unexpected error.");
            logger.error("Uncaught Error was handled by Api Mapper", exception);
            return Response.status(HttpConstants.SC_INTERNAL_SERVER_ERROR)
                    .entity(apiError)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .build();
        } else {
            return response;
        }
    }
}
