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
import io.swagger.v3.oas.annotations.media.Schema;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class RequestBodyMissingError extends ApiError<RequestBodyMissingError> {
    @JsonProperty("parameter")
    @Schema(description = "Parameter")
    private @Nullable String parameter;

    private RequestBodyMissingError(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable String parameter) {
        super(title, detail, HttpStatus.BAD_REQUEST_400, null);
        setParameter(parameter);
    }

    public static RequestBodyMissingError of(final @NotNull String parameter) {
        return new RequestBodyMissingError("Required request body parameter " + parameter + " missing",
                "Required request body parameter " + parameter + " missing",
                parameter);
    }

    public static RequestBodyMissingError of() {
        return new RequestBodyMissingError("Required request body missing", "Required request body missing", null);
    }

    public @Nullable String getParameter() {
        return parameter;
    }

    public @NotNull RequestBodyMissingError setParameter(final @Nullable String parameter) {
        this.parameter = parameter;
        return this;
    }

    @Override
    public @NotNull String toString() {
        return "RequestBodyMissingError{" +
                "parameter='" +
                parameter +
                '\'' +
                ", code='" +
                code +
                '\'' +
                ", detail='" +
                detail +
                '\'' +
                ", status=" +
                status +
                ", title='" +
                title +
                '\'' +
                ", type='" +
                type +
                '\'' +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final RequestBodyMissingError that = (RequestBodyMissingError) o;
        return Objects.equals(parameter, that.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parameter);
    }
}
