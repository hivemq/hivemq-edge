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
package com.hivemq.api.errors.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.ApiError;
import com.hivemq.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class UrlParameterMissingError extends ApiError<UrlParameterMissingError> {
    @JsonProperty("parameter")
    private @NotNull String parameter;

    private UrlParameterMissingError(
            final @NotNull String title,
            final @Nullable String detail,
            final @NotNull String parameter,
            final int status,
            final @Nullable String code) {
        super(title, detail, status, code);
        setParameter(parameter);
    }

    public static UrlParameterMissingError of(final @NotNull String parameter) {
        return new UrlParameterMissingError("Required url parameter missing",
                "Required url parameter '" + parameter + "' missing",
                parameter,
                HttpStatus.BAD_REQUEST_400,
                null);
    }

    public @NotNull String getParameter() {
        return parameter;
    }

    public @NotNull UrlParameterMissingError setParameter(@NotNull final String parameter) {
        this.parameter = Objects.requireNonNull(parameter);
        return this;
    }
}
