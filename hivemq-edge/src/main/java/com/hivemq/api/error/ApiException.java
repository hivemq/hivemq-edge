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
package com.hivemq.api.error;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Simon L Johnson
 */
public class ApiException extends RuntimeException {

    private @Nullable Object subject = null;
    private @Nullable String errorMessage;
    private int httpStatusCode = 500;
    private @Nullable Throwable cause;
    private @Nullable String fieldName;

    public ApiException(final @NotNull String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ApiException(final @NotNull String errorMessage, final int httpStatusCode) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
    }

    public ApiException(final @NotNull Object subject, final @NotNull String errorMessage, final int httpStatusCode) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
        this.subject = subject;
    }

    public ApiException(
            final @NotNull String errorMessage,
            int httpStatusCode,
            final @NotNull Throwable cause,
            final @NotNull Object subject) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
        this.subject = subject;
        this.cause = cause;
    }

    public ApiException(final @NotNull String errorMessage, final @NotNull Throwable cause) {
        super(errorMessage, cause);
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    public @Nullable Object getSubject() {
        return subject;
    }

    public void setSubject(final Object subject) {
        this.subject = subject;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(final int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    @JsonIgnore
    @SuppressWarnings("UnsynchronizedOverridesSynchronized") // ApiException is not designed for concurrent access
    @Override
    public @Nullable Throwable getCause() {
        return cause;
    }

    public void setCause(final Throwable cause) {
        this.cause = cause;
    }

    public @Nullable String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }
}
