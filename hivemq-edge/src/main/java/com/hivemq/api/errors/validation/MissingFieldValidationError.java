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

package com.hivemq.api.errors.validation;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MissingFieldValidationError extends ValidationError<MissingFieldValidationError> {
    @JsonProperty("field")
    private @NotNull String field;

    private MissingFieldValidationError(final @NotNull String detail, @NotNull final String field) {
        super(detail);
        setField(field);
    }

    public static @NotNull MissingFieldValidationError of(final @NotNull String field) {
        return new MissingFieldValidationError("Required field '" + field + "' is missing", field);
    }

    public static @NotNull MissingFieldValidationError of(final @NotNull String detail, final @NotNull String field) {
        return new MissingFieldValidationError(detail, field);
    }

    public @NotNull String getField() {
        return field;
    }

    public @NotNull MissingFieldValidationError setField(@NotNull final String field) {
        this.field = Objects.requireNonNull(field);
        return this;
    }

    @Override
    public @NotNull String toString() {
        return "MissingFieldValidationError{" +
                "field='" +
                field +
                '\'' +
                ", detail='" +
                detail +
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
        final MissingFieldValidationError that = (MissingFieldValidationError) o;
        return Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), field);
    }
}
