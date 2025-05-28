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

public final class InternalServerError extends ApiError<InternalServerError> {
    @JsonProperty("reason")
    private @Nullable String reason;

    private InternalServerError(
            final @NotNull String title,
            final @Nullable String detail,
            final @Nullable String reason,
            final int status,
            final @Nullable String code) {
        super(title, detail, status, code);
        setReason(reason);
    }

    public static InternalServerError of() {
        return of(null);
    }

    public static InternalServerError of(final @Nullable String reason) {
        return new InternalServerError("InternalError",
                reason == null ? "An unexpected error occurred, check the logs." : reason,
                reason,
                HttpStatus.INTERNAL_SERVER_ERROR_500,
                null);
    }

    @Override
    public @NotNull String toString() {
        return "InternalServerError{" +
                "reason='" +
                reason +
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
        final InternalServerError that = (InternalServerError) o;
        return Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), reason);
    }

    public @Nullable String getReason() {
        return reason;
    }

    public @NotNull InternalServerError setReason(final @Nullable String reason) {
        this.reason = reason;
        return this;
    }
}
