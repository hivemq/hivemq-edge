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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.http.HttpStatus;
import com.hivemq.http.error.Error;
import com.hivemq.http.error.Errors;

import javax.ws.rs.core.Response;

/**
 * @author Christoph Schäbel
 */
public class ErrorResponseUtil {

    public static @NotNull Response errorResponse(
            final int code, final @NotNull String title, final @Nullable String detail) {
        return Response.status(code)
                .entity(new Errors(new Error(title, detail)))
                .header("Content-Type", "application/json;charset=utf-8")
                .build();
    }

    public static @NotNull Response notFound(final @NotNull String type, final @NotNull String id) {
            return errorResponse(HttpStatus.NOT_FOUND_404, "Resource not found", type + " with id '" + id + "' not found");
    }

    public static @NotNull Response urlParameterRequired(final @NotNull String parameterName) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Required parameter missing",
                "Required URL parameter '" + parameterName + "' is missing");
    }

    public static @NotNull Response bodyParameterRequired(final @NotNull String parameterName) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Required parameter missing",
                "Required request body parameter " + parameterName + " is missing");
    }

    public static @NotNull Response invalidQueryParameter(final @NotNull String parameterName) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Parameter invalid",
                "Query parameter '" + parameterName + "' is invalid");
    }

    public static @NotNull Response invalidQueryParameter(
            final @NotNull String parameterName, final @NotNull String reason) {
        return errorResponse(HttpStatus.BAD_REQUEST_400,
                "Parameter invalid",
                "Query parameter '" + parameterName + "' is invalid. " + reason);
    }

    public static @NotNull Response invalidInput(final @NotNull String reason) {
        return errorResponse(HttpStatus.BAD_REQUEST_400, "Invalid input", reason);
    }

    public static @NotNull Response alreadyExists(final @NotNull String detail) {
        return errorResponse(HttpStatus.FORBIDDEN_403, "The resource already exists", detail);
    }

    public static @NotNull Response cursorInvalid() {
        return errorResponse(HttpStatus.GONE_410,
                "Cursor not valid anymore",
                "The passed cursor is not valid anymore, you can request this resource without a cursor to start from the beginning");
    }

    public static @NotNull Response genericError(final @Nullable String detail) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, "Internal error", detail);
    }

    public static @NotNull Response timedOut(final @NotNull String detail) {
        return errorResponse(HttpStatus.INTERNAL_SERVER_ERROR_500, "Processing of request timed out", detail);
    }

    public static @NotNull Response temporarilyNotAvailable() {
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE_503,
                "The endpoint is temporarily not available",
                "The endpoint is temporarily not available, please try again later");
    }

    public static @NotNull Response notAllClusterNodesSupport() {
        return errorResponse(HttpStatus.SERVICE_UNAVAILABLE_503,
                "Endpoint not active yet",
                "Not all cluster nodes support this endpoint yet, please try again later");
    }

}
