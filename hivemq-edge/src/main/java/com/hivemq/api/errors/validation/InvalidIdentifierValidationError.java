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

public final class InvalidIdentifierValidationError extends ValidationError<InvalidIdentifierValidationError> {
    @JsonProperty("field")
    private @NotNull String field;
    @JsonProperty("value")
    private @NotNull String value;

    private InvalidIdentifierValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String value) {
        super(detail);
        setField(field);
        setValue(value);
    }

    public static @NotNull InvalidIdentifierValidationError of(
            final @NotNull String field,
            final @NotNull String value) {
        return new InvalidIdentifierValidationError("Identifier " +
                field +
                " must begin with a letter and may only consist of lowercase letters," +
                " uppercase letters, numbers, periods, hyphens, and underscores", field, value);
    }

    public static @NotNull InvalidIdentifierValidationError of(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String value) {
        return new InvalidIdentifierValidationError(detail, field, value);
    }

    @Override
    public @NotNull String toString() {
        return "InvalidIdentifierValidationError{" +
                "field='" +
                field +
                '\'' +
                ", value='" +
                value +
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
        final InvalidIdentifierValidationError that = (InvalidIdentifierValidationError) o;
        return Objects.equals(field, that.field) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), field, value);
    }

    public @NotNull String getValue() {
        return value;
    }

    public @NotNull InvalidIdentifierValidationError setValue(final @NotNull String value) {
        this.value = Objects.requireNonNull(value);
        return this;
    }

    public @NotNull String getField() {
        return field;
    }

    public @NotNull InvalidIdentifierValidationError setField(final @NotNull String field) {
        this.field = Objects.requireNonNull(field);
        return this;
    }
}
