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
import com.hivemq.http.error.Errors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.ws.rs.core.Response;
import java.util.List;

/**
 * @author Christoph Schäbel
 */
public class ErrorResponseUtil {

    public static @NotNull Response errorResponse(
            final int code, final @NotNull String title, final @Nullable String detail, List<Error> errors) {
        return Response.status(code)
                .entity(new Errors(null, title, detail, code, errors))
                .header("Content-Type", "application/json;charset=utf-8")
                .build();
    }

    public static @NotNull Response notFound(final @NotNull String type, final @NotNull String id) {
            return errorResponse(HttpStatus.NOT_FOUND_404, "Resource not found", type + " with id '" + id + "' not found", List.of());
    }

    public static @NotNull Response methodNotAllowed() {
            return errorResponse(HttpStatus.METHOD_NOT_ALLOWED_405, "Method not allowed", "", List.of());
    }

    public static @NotNull Response unsupportedMediaType() {
            return errorResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE_415, "Unsupported Media Type", "", List.of());
    }

    public static @NotNull Response notFoundWithMessage(final @NotNull String title, final @NotNull String details) {
            return errorResponse(HttpStatus.NOT_FOUND_404, title, details, List.of());
    }

    public static @NotNull Response urlParameterRequired(final @NotNull String parameterName) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Required parameter missing",
                "",
                List.of(new Error("Required URL parameter is missing", parameterName)));
    }

    public static @NotNull Response bodyParameterRequired(final @NotNull String parameterName) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Required request body parameter missing",
                "",
                List.of(new Error("Required URL parameter is missing", parameterName)));
    }

    public static @NotNull Response invalidQueryParameter(final @NotNull String parameterName) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Parameter invalid",
                "",
                List.of(new Error("Query parameter is invalid", parameterName)));
    }

    public static @NotNull Response invalidQueryParameter(
            final @NotNull String parameterName, final @NotNull String reason) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Parameter invalid",
                "",
                List.of(new Error(reason, parameterName)));
    }

    public static @NotNull Response validationErrors(final @NotNull String reason, final @NotNull List<Error> errors) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Invalid input",
                reason,
                errors);
    }

    public static @NotNull Response invalidInput(final @NotNull String reason) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Invalid input",
                reason,
                List.of());
    }

    public static @NotNull Response unauthorized(final @NotNull String reason) {
        return errorResponse(HttpStatus.UNAUTHORIZED,
                "Invalid input",
                reason,
                List.of());
    }

    public static @NotNull Response alreadyExists(final @NotNull String detail) {
        return errorResponse(HttpStatus.CONFLICT_409,
                "The resource already exists",
                detail,
                List.of());
    }

    public static @NotNull Response badRequest(final @NotNull String title, final @NotNull String details) {
        return errorResponse(
                HttpStatus.BAD_REQUEST_400,
                title,
                details,
                List.of());
    }

    public static @NotNull Response insufficientStorage(final @NotNull String title, final @NotNull String details) {
        return errorResponse(
                HttpStatus.INSUFFICIENT_STORAGE_507,
                title,
                details,
                List.of());
    }

    public static @NotNull Response preconditionFailed(final @NotNull String title, final @NotNull String details) {
        return errorResponse(
                HttpStatus.PRECONDITION_FAILED_412,
                title,
                details,
                List.of());
    }

    public static @NotNull Response genericError(final @Nullable String detail) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500,
                "Internal error",
                detail,
                List.of());
    }

    public static @NotNull Response temporarilyNotAvailable() {
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE_503,
                "The endpoint is temporarily not available",
                "The endpoint is temporarily not available, please try again later",
                List.of());
    }

}
