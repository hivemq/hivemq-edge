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
package com.hivemq.util;

import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.ErrorType;
import com.hivemq.http.error.Errors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.List;

/**
 * @author Christoph Schäbel
 */
public class ErrorResponseUtil {

    public static final @NotNull ErrorType ERROR_TYPE_METHOD_NOT_ALLOWED = new ErrorType(null, "Method not allowed", "Method not allowed");
    public static final @NotNull ErrorType ERROR_TYPE_UNSUPPORTED_MEDIA_TYPE = new ErrorType(null, "Unsupported Media Type", "Unsupported Media Type");
    public static final @NotNull ErrorType ERROR_TYPE_REQUIRED_QUERY_PARAMETER_MISSING = new ErrorType(null, "Required query parameter missing", "Required query parameter missing");
    public static final @NotNull ErrorType ERROR_TYPE_MISSING_BODY = new ErrorType(null, "Required request body parameter missing", "Required request body parameter missing");
    public static final @NotNull ErrorType ERROR_TYPE_QUERY_PARAMETER_INVALID = new ErrorType(null, "Query parameter invalid", "A query parameter failed validation");
    public static final @NotNull ErrorType ERROR_TYPE_INPUT_INVALID = new ErrorType(null, "Invalid input", "Invalid input");
    public static final @NotNull ErrorType ERROR_TYPE_UNAUTHORIZED = new ErrorType(null, "Unauthorized", "Unauthorized");
    public static final @NotNull ErrorType ERROR_TYPE_RESOURCE_ALREADY_EXISTS = new ErrorType(null, "The resource already exists", "Requested to create a resource which already exists");
    public static final @NotNull ErrorType ERROR_TYPE_ENDPOINT_IS_TEMPORARILY_NOT_AVAILABLE = new ErrorType(null, "The endpoint is temporarily not available", "The endpoint is temporarily not available, please try again later");
    public static final @NotNull ErrorType ERROR_TYPE_INTERNAL_ERROR = new ErrorType(null, "Internal error","An unexpected error occurred, check the logs");
    public static final @NotNull ErrorType ERROR_TYPE_INSUFFICIENT_STORAGE = new ErrorType(null, "Insufficient Storage","Insufficient Storage");
    public static final @NotNull ErrorType ERROR_TYPE_PRECONDITION_FAILED = new ErrorType(null, "Precondition Failed","A precondition required for fulfilling the request was not fulfilled");
    public static final @NotNull ErrorType ERROR_TYPE_BAD_REQUEST = new ErrorType(null, "Bad Request","Parameters failed validation");

    public static @NotNull Response errorResponse(
            final int code, final @NotNull ErrorType errorType, List<Error> errors) {
        return Response.status(code)
                .entity(new Errors(
                        URI.create("http://nowhere/" + errorType.getType()),
                        errorType.getTitle(),
                        errorType.getDetail(),
                        code,
                        errors))
                .header("Content-Type", "application/json;charset=utf-8")
                .build();
    }

    public static @NotNull Response notFound(final @NotNull ErrorType errorType, final @NotNull String error) {
            return errorResponse(
                    HttpStatus.NOT_FOUND_404,
                    errorType,
                    List.of(new Error(error, null, null, null)));
    }

    public static @NotNull Response notFound(final @NotNull ErrorType errorType) {
            return errorResponse(
                    HttpStatus.NOT_FOUND_404,
                    errorType,
                    List.of());
    }

    public static @NotNull Response methodNotAllowed() {
            return errorResponse(
                    HttpStatus.METHOD_NOT_ALLOWED_405,
                    ERROR_TYPE_METHOD_NOT_ALLOWED,
                    List.of());
    }

    public static @NotNull Response unsupportedMediaType() {
            return errorResponse(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE_415,
                    ERROR_TYPE_UNSUPPORTED_MEDIA_TYPE,
                    List.of());
    }

    public static @NotNull Response urlParameterRequired(final @NotNull String parameterName) {
        return errorResponse(
                HttpStatus.BAD_REQUEST_400,
                ERROR_TYPE_REQUIRED_QUERY_PARAMETER_MISSING,
                List.of(new Error("URL parameter missing: " + parameterName,null , null, null)));
    }

    public static @NotNull Response bodyParameterRequired(final @NotNull String parameterName) {
        return errorResponse(
                HttpStatus.BAD_REQUEST_400,
                ERROR_TYPE_MISSING_BODY,
                List.of(new Error("Request body parameter missing: " + parameterName, parameterName, null, null)));
    }

    public static @NotNull Response invalidQueryParameter(final @NotNull String parameterName, final @Nullable String reason) {
        return errorResponse(
                HttpStatus.BAD_REQUEST_400,
                ERROR_TYPE_QUERY_PARAMETER_INVALID,
                List.of(new Error("Query parameter is invalid: " + parameterName, parameterName, null, reason)));
    }

    public static @NotNull Response invalidQueryParameter(final @NotNull String parameterName) {
        return invalidQueryParameter(parameterName, null);
    }

    public static @NotNull Response validationErrors(final @NotNull ErrorType errorType, final @NotNull List<Error> errors) {
        return errorResponse(
                HttpStatus.BAD_REQUEST_400,
                errorType,
                errors);
    }

    public static @NotNull Response invalidInput(final @NotNull String reason) {
        return errorResponse(
                HttpStatus.BAD_REQUEST_400,
                ERROR_TYPE_INPUT_INVALID,
                List.of(new Error(reason, null, null, null)));
    }

    public static @NotNull Response unauthorized(final @NotNull String reason) {
        return errorResponse(
                HttpStatus.UNAUTHORIZED,
                ERROR_TYPE_UNAUTHORIZED,
                List.of(new Error(reason, null, null, null)));
    }

    public static @NotNull Response alreadyExists(final @NotNull String detail) {
        return errorResponse(
                HttpStatus.CONFLICT_409,
                ERROR_TYPE_RESOURCE_ALREADY_EXISTS,
                List.of(new Error(detail, null, null, null)));
    }

    public static @NotNull Response badRequest(final @NotNull String detail) {
        return errorResponse(
                HttpStatus.BAD_REQUEST_400,
                ERROR_TYPE_BAD_REQUEST,
                List.of(new Error(detail, null, null, null)));
    }

    public static @NotNull Response insufficientStorage(final @Nullable String details) {
        return errorResponse(
                HttpStatus.INSUFFICIENT_STORAGE_507,
                ERROR_TYPE_INSUFFICIENT_STORAGE,
                List.of(new Error(details, null, null, null)));
    }

    public static @NotNull Response preconditionFailed(final @NotNull String details) {
        return errorResponse(
                HttpStatus.PRECONDITION_FAILED_412,
                ERROR_TYPE_PRECONDITION_FAILED,
                List.of(new Error(details, null, null, null)));
    }

    public static @NotNull Response genericError(final @Nullable String detail) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
                ERROR_TYPE_INTERNAL_ERROR,
                List.of(new Error(detail, null, null, null)));
    }

    public static @NotNull Response temporarilyNotAvailable() {
        return errorResponse(
                HttpStatus.SERVICE_UNAVAILABLE_503,
                ERROR_TYPE_ENDPOINT_IS_TEMPORARILY_NOT_AVAILABLE,
                List.of());
    }

}
