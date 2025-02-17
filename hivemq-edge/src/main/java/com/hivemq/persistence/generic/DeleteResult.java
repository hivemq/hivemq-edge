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
package com.hivemq.persistence.generic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DeleteResult (DeleteStatus updateStatus, String errorMessage){

    public static @NotNull DeleteResult success() {
        return new DeleteResult(DeleteStatus.SUCCESS, null);
    }

    public static @NotNull DeleteResult failed(final @NotNull DeleteStatus putStatus) {
        return new DeleteResult(putStatus,  null);
    }

    public static @NotNull DeleteResult failed(
            final @NotNull DeleteStatus deleteResult, final @Nullable String errorMessage) {
        return new DeleteResult(deleteResult, errorMessage);
    }

    public @NotNull DeleteStatus getDeleteStatus() {
        return updateStatus;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    public enum DeleteStatus {
        SUCCESS(),
        NOT_FOUND()
    }
}
