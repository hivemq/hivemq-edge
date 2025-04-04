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
package com.hivemq.edge.modules.api.adapters.model;

import com.google.common.base.Preconditions;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapterValidationFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public class ProtocolAdapterValidationFailureImpl implements ProtocolAdapterValidationFailure {

    private final @NotNull String message;
    private @Nullable Class origin;
    private @Nullable Throwable cause;
    private @Nullable String fieldName;

    public ProtocolAdapterValidationFailureImpl(final @NotNull String message) {
        Preconditions.checkNotNull(message);
        this.message = message;
    }

    public ProtocolAdapterValidationFailureImpl(
            final @NotNull String message,
            final @Nullable String fieldName,
            final @Nullable Class origin,
            final @Nullable Throwable cause) {
        Preconditions.checkNotNull(message);
        this.message = message;
        this.origin = origin;
        this.cause = cause;
        this.fieldName = fieldName;
    }

    public ProtocolAdapterValidationFailureImpl(
            final @NotNull String message,
            final @Nullable String fieldName,
            final @Nullable Class origin) {
        Preconditions.checkNotNull(message);
        this.message = message;
        this.origin = origin;
        this.fieldName = fieldName;
    }

    @Override
    public @NotNull String getMessage() {
        return message;
    }

    @Override
    public @Nullable Class getOrigin() {
        return origin;
    }

    @Override
    public @Nullable Throwable getCause() {
        return cause;
    }

    @Override
    public @Nullable String getFieldName() {
        return fieldName;
    }
}
