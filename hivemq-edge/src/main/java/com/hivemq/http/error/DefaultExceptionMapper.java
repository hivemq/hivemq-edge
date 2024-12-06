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
import com.hivemq.util.ErrorResponseUtil;
import com.hivemq.util.Exceptions;
import org.jetbrains.annotations.NotNull;
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
import java.util.List;


@Provider
@Singleton
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionMapper.class);

    public static final @NotNull ErrorType ERROR_TYPE_NOT_FOUND = new ErrorType(null, "Not found", "Resource wasn't found.");
    public static final @NotNull ErrorType ERROR_TYPE_VALIDATION_ERROR = new ErrorType(null, "Validatin failed", "JSON failed validation.");

    @NotNull
    @Override
    public Response toResponse(final @NotNull Throwable exception) {

        //matches all default exceptions, e.g. NotFoundException, BadRequestException,...
        if (exception instanceof WebApplicationException) {
            log.trace("WebApplicationException in REST API: {}", exception.getMessage());
            final Response response = ((WebApplicationException) exception).getResponse();
            final int status = response.getStatus();

            if (exception instanceof NotFoundException) {
                return ErrorResponseUtil.notFound(ERROR_TYPE_NOT_FOUND);
            } else if (exception instanceof BadRequestException) {
                return ErrorResponseUtil.badRequest(exception.getMessage());
            } else if (exception instanceof NotAllowedException) {
                return ErrorResponseUtil.methodNotAllowed();
            } else if (exception instanceof NotSupportedException) {
                return ErrorResponseUtil.unsupportedMediaType();
            }
            //build a new response to prevent additional information from being passed out to the http clients by the exception
            return ErrorResponseUtil.genericError("Internal error");
        }

        if (exception instanceof JsonProcessingException) {
            if (exception instanceof UnrecognizedPropertyException) {
                return ErrorResponseUtil.validationErrors(ERROR_TYPE_VALIDATION_ERROR, List.of(new Error("Unrecognized field", ((UnrecognizedPropertyException) exception).getPropertyName(), null, null)));
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
