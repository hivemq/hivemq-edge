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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.http.core.HttpConstants;

/**
 * @author Simon L Johnson
 */
public class ApiException extends RuntimeException {

    private Object subject = null;
    private String errorMessage;
    private int httpStatusCode = HttpConstants.SC_INTERNAL_SERVER_ERROR;
    private Throwable cause;
    private String fieldName;

    public ApiException(@NotNull final String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ApiException(@NotNull final String errorMessage, final int httpStatusCode) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
    }

    public ApiException(@NotNull final Object subject, @NotNull final String errorMessage, final int httpStatusCode) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
        this.subject = subject;
    }

    public ApiException(@NotNull final String errorMessage, int httpStatusCode, @NotNull final Throwable cause, @NotNull final Object subject) {
        super(errorMessage);
        this.errorMessage = errorMessage;
        this.httpStatusCode = httpStatusCode;
        this.subject = subject;
        this.cause = cause;
    }

    public ApiException(@NotNull final String errorMessage, @NotNull final Throwable cause) {
        super(errorMessage, cause);
        this.errorMessage = errorMessage;
        this.cause = cause;
    }

    public Object getSubject() {
        return subject;
    }

    public void setSubject(final Object subject) {
        this.subject = subject;
    }

    public String getErrorMessage() {
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
    public Throwable getCause() {
        return cause;
    }

    public void setCause(final Throwable cause) {
        this.cause = cause;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }
}
