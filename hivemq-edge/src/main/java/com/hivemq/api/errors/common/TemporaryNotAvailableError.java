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

package com.hivemq.api.errors.common;

import com.hivemq.api.errors.ApiError;
import com.hivemq.http.HttpStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class TemporaryNotAvailableError extends ApiError<TemporaryNotAvailableError> {
    private TemporaryNotAvailableError(
            final @NotNull String title,
            final @Nullable String detail,
            final int status,
            final @Nullable String code) {
        super(title, detail, status, code);
    }

    public static TemporaryNotAvailableError of() {
        return new TemporaryNotAvailableError("The endpoint is temporarily not available",
                "The endpoint is temporarily not available, please try again later",
                HttpStatus.SERVICE_UNAVAILABLE_503,
                null);
    }

    @Override
    public @NotNull String toString() {
        return "TemporaryNotAvailableError{" +
                "code='" +
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
}
