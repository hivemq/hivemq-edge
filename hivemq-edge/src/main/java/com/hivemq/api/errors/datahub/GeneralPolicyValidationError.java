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

package com.hivemq.api.errors.datahub;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hivemq.api.errors.validation.ValidationError;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class GeneralPolicyValidationError extends ValidationError<GeneralPolicyValidationError> {
    @JsonProperty("title")
    private @NotNull String title;

    private GeneralPolicyValidationError(final @NotNull String detail, final @NotNull String title) {
        super(detail);
        setTitle(title);
    }

    public static @NotNull GeneralPolicyValidationError of(final @NotNull String detail, final @NotNull String title) {
        return new GeneralPolicyValidationError(detail, title);
    }

    public @NotNull String getTitle() {
        return title;
    }

    public @NotNull GeneralPolicyValidationError setTitle(@NotNull final String title) {
        this.title = Objects.requireNonNull(title);
        return this;
    }

    @Override
    public @NotNull String toString() {
        return "GeneralPolicyValidationError{" +
                "title='" +
                title +
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
        final GeneralPolicyValidationError that = (GeneralPolicyValidationError) o;
        return Objects.equals(title, that.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), title);
    }
}
