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

public final class InvalidFieldLengthValidationError extends ValidationError<InvalidFieldLengthValidationError> {
    @JsonProperty("actualLength")
    private int actualLength;
    @JsonProperty("expectedMinimumLength")
    private int expectedMinimumLength;
    @JsonProperty("expectedMaximumLength")
    private int expectedMaximumLength;
    @JsonProperty("field")
    private @NotNull String field;
    @JsonProperty("value")
    private @NotNull String value;

    private InvalidFieldLengthValidationError(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String value,
            final int actualLength,
            final int expectedMinimumLength,
            final int expectedMaximumLength) {
        super(detail);
        setActualLength(actualLength);
        setExpectedMinimumLength(expectedMinimumLength);
        setExpectedMaximumLength(expectedMaximumLength);
        setField(field);
        setValue(value);
    }

    public static @NotNull InvalidFieldLengthValidationError of(
            final @NotNull String detail,
            final @NotNull String field,
            final @NotNull String value,
            final int actualLength,
            final int expectedMinimumLength,
            final int expectedMaximumLength) {
        return new InvalidFieldLengthValidationError(detail,
                field,
                value,
                actualLength,
                expectedMinimumLength,
                expectedMaximumLength);
    }

    @Override
    public @NotNull String toString() {
        return "InvalidFieldLengthValidationError{" +
                "actualLength=" +
                actualLength +
                ", expectedMinimumLength=" +
                expectedMinimumLength +
                ", expectedMaximumLength=" +
                expectedMaximumLength +
                ", field='" +
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
        final InvalidFieldLengthValidationError that = (InvalidFieldLengthValidationError) o;
        return actualLength == that.actualLength &&
                expectedMinimumLength == that.expectedMinimumLength &&
                expectedMaximumLength == that.expectedMaximumLength &&
                Objects.equals(field, that.field) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), actualLength, expectedMinimumLength, expectedMaximumLength, field, value);
    }

    public int getActualLength() {
        return actualLength;
    }

    public @NotNull InvalidFieldLengthValidationError setActualLength(final int actualLength) {
        this.actualLength = actualLength;
        return this;
    }

    public int getExpectedMinimumLength() {
        return expectedMinimumLength;
    }

    public @NotNull InvalidFieldLengthValidationError setExpectedMinimumLength(final int expectedMinimumLength) {
        this.expectedMinimumLength = expectedMinimumLength;
        return this;
    }

    public int getExpectedMaximumLength() {
        return expectedMaximumLength;
    }

    public @NotNull InvalidFieldLengthValidationError setExpectedMaximumLength(final int expectedMaximumLength) {
        this.expectedMaximumLength = expectedMaximumLength;
        return this;
    }

    public @NotNull String getValue() {
        return value;
    }

    public @NotNull InvalidFieldLengthValidationError setValue(final @NotNull String value) {
        this.value = Objects.requireNonNull(value);
        return this;
    }

    public @NotNull String getField() {
        return field;
    }

    public @NotNull InvalidFieldLengthValidationError setField(final @NotNull String field) {
        this.field = Objects.requireNonNull(field);
        return this;
    }
}
