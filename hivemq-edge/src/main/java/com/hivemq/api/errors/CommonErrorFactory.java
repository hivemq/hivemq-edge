/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hivemq.api.errors;

import com.hivemq.edge.api.model.InsufficientStorageError;
import com.hivemq.edge.api.model.InternalServerError;
import com.hivemq.edge.api.model.InvalidQueryParameterError;
import com.hivemq.edge.api.model.PreconditionFailedError;
import com.hivemq.edge.api.model.RequestBodyMissingError;
import com.hivemq.edge.api.model.TemporaryNotAvailableError;
import com.hivemq.edge.api.model.UrlParameterMissingError;
import com.hivemq.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

public final class CommonErrorFactory {
    private static final String CLASS_PREFIX = "com.hivemq";
    private static final String TYPE_PREFIX = "https://hivemq.com";

    private CommonErrorFactory() {
    }

    public static @NotNull URI type(final @NotNull Class<?> clazz) {
        return URI.create(TYPE_PREFIX + clazz.getName().substring(CLASS_PREFIX.length()).replace(".", "/"));
    }

    public static @NotNull InsufficientStorageError insufficientStorageError() {
        return insufficientStorageError(null);
    }

    public static @NotNull InsufficientStorageError insufficientStorageError(final @Nullable String reason) {
        return InsufficientStorageError.builder()
                .type(type(InsufficientStorageError.class))
                .title("Insufficient Storage")
                .detail(reason == null ? "Insufficient Storage." : "Insufficient Storage: " + reason)
                .reason(reason)
                .status(HttpStatus.INSUFFICIENT_STORAGE_507)
                .build();
    }

    public static @NotNull InternalServerError internalServerError(final @Nullable String reason) {
        return InternalServerError.builder()
                .type(type(InternalServerError.class))
                .title("Internal Error")
                .detail(reason == null ? "An unexpected error occurred, check the logs." : reason)
                .reason(reason)
                .status(HttpStatus.INTERNAL_SERVER_ERROR_500)
                .build();
    }

    public static @NotNull InvalidQueryParameterError invalidQueryParameterError(
            final @NotNull String parameter,
            final @NotNull String reason) {
        return InvalidQueryParameterError.builder()
                .type(type(InvalidQueryParameterError.class))
                .title("Query parameter is invalid")
                .detail("Query parameter '" + parameter + "' is invalid: " + reason)
                .parameter(parameter)
                .reason(reason)
                .status(HttpStatus.BAD_REQUEST_400)
                .build();
    }

    public static @NotNull PreconditionFailedError preconditionFailedError(
            final @NotNull String reason) {
        return PreconditionFailedError.builder()
                .type(type(PreconditionFailedError.class))
                .title("Precondition Failed")
                .detail("A precondition required for fulfilling the request was not fulfilled: " + reason)
                .reason(reason)
                .status(HttpStatus.PRECONDITION_FAILED_412)
                .build();
    }

    public static @NotNull RequestBodyMissingError requestBodyMissingError() {
        return RequestBodyMissingError.builder()
                .type(type(RequestBodyMissingError.class))
                .title("Required request body missing")
                .detail("Required request body missing")
                .status(HttpStatus.BAD_REQUEST_400)
                .build();
    }

    public static @NotNull RequestBodyMissingError requestBodyMissingError(final @NotNull String parameter) {
        return RequestBodyMissingError.builder()
                .type(type(RequestBodyMissingError.class))
                .title("Required request body parameter " + parameter + " missing")
                .detail("Required request body parameter " + parameter + " missing")
                .parameter(parameter)
                .status(HttpStatus.BAD_REQUEST_400)
                .build();
    }

    public static @NotNull TemporaryNotAvailableError temporaryNotAvailableError() {
        return TemporaryNotAvailableError.builder()
                .type(type(TemporaryNotAvailableError.class))
                .title("The endpoint is temporarily not available")
                .detail("The endpoint is temporarily not available, please try again later")
                .status(HttpStatus.SERVICE_UNAVAILABLE_503)
                .build();
    }

    public static @NotNull UrlParameterMissingError urlParameterMissingError(final @NotNull String parameter) {
        return UrlParameterMissingError.builder()
                .type(type(UrlParameterMissingError.class))
                .title("Required url parameter missing")
                .detail("Required url parameter '" + parameter + "' missing")
                .parameter(parameter)
                .status(HttpStatus.BAD_REQUEST_400)
                .build();
    }
}
