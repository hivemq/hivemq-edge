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
package com.hivemq.configuration.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.helpers.ValidationEventImpl;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public interface EntityValidatable {
    static boolean notMatch(
            final @NotNull List<ValidationEvent> validationEvents,
            final @NotNull BooleanSupplier assertionSupplier,
            final @NotNull Supplier<String> errorMessageSupplier) {
        if (!assertionSupplier.getAsBoolean()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR,
                    errorMessageSupplier.get(),
                    null));
            return false;
        }
        return true;
    }

    static boolean notEmpty(
            final @NotNull List<ValidationEvent> validationEvents,
            final @Nullable String value,
            final @NotNull String propertyName) {
        if (value == null || value.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR,
                    propertyName + " is missing",
                    null));
            return false;
        }
        return true;
    }

    static <T> boolean notEmpty(
            final @NotNull List<ValidationEvent> validationEvents,
            final @Nullable List<T> value,
            final @NotNull String propertyName) {
        if (value == null || value.isEmpty()) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR,
                    propertyName + " is missing",
                    null));
            return false;
        }
        return true;
    }

    static <T> boolean notNull(
            final @NotNull List<ValidationEvent> validationEvents,
            final @Nullable T value,
            final @NotNull String propertyName) {
        if (value == null) {
            validationEvents.add(new ValidationEventImpl(ValidationEvent.FATAL_ERROR,
                    propertyName + " is invalid",
                    null));
            return false;
        }
        return true;
    }

    void validate(final @NotNull List<ValidationEvent> validationEvents);
}
