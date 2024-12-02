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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.jetbrains.annotations.NotNull;
import com.hivemq.http.HttpStatus;
import com.hivemq.util.ErrorResponseUtil;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.io.EOFException;


@Provider
@Singleton
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    @NotNull
    @Override
    public Response toResponse(final @NotNull Throwable exception) {

        //matches all default exceptions, e.g. NotFoundException, BadRequestException,...
        if (exception instanceof WebApplicationException) {
            log.trace("WebApplicationException in REST API: {}", exception.getMessage());
            final Response response = ((WebApplicationException) exception).getResponse();
            final int status = response.getStatus();

            if (exception instanceof NotFoundException) {
                return ErrorResponseUtil.errorResponse(HttpStatus.NOT_FOUND_404, "Resource not found", null);
            } else if (exception instanceof BadRequestException) {
                return ErrorResponseUtil.errorResponse(HttpStatus.BAD_REQUEST_400,
                        "Bad request",
                        exception.getMessage());
            } else if (exception instanceof NotAllowedException) {
                return ErrorResponseUtil.errorResponse(HttpStatus.METHOD_NOT_ALLOWED_405, "Method not allowed", null);
            } else if (exception instanceof NotSupportedException) {
                return ErrorResponseUtil.errorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415,
                        "Unsupported Media Type",
                        null);
            }
            //build a new response to prevent additional information from being passed out to the http clients by the exception
            return ErrorResponseUtil.errorResponse(status, "Internal error", null);
        }

        if (exception instanceof JsonProcessingException) {
            if (exception instanceof UnrecognizedPropertyException) {
                return ErrorResponseUtil.errorResponse(HttpStatus.BAD_REQUEST_400,
                        "Invalid input",
                        "Unrecognized field \"" +
                                ((UnrecognizedPropertyException) exception).getPropertyName() +
                                "\", not marked as ignorable");
            }

            log.trace("Not able to parse JSON request for REST API", exception);
            return ErrorResponseUtil.invalidInput("Unable to parse JSON body");
        }

        //handle EOF exception if connection is closed while the request/response is in progress
        if (exception instanceof EOFException) {
            log.trace("EOF in REST API, connection has been closed before request/response could complete");
            //build a new response to prevent additional information from being passed out to the http clients by the exception
            //response code does not really matter here as it cannot be transmitted anyway
            return ErrorResponseUtil.genericError(null);
        }

        log.debug("Uncaught exception in REST API", exception);
        Exceptions.rethrowError(exception);
        return ErrorResponseUtil.genericError(null);
    }
}
