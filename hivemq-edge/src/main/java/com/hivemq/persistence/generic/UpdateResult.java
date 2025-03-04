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

public record UpdateResult (UpdateStatus updateStatus, String errorMessage){

    public static @NotNull UpdateResult success() {
        return new UpdateResult(UpdateStatus.SUCCESS, null);
    }

    public static @NotNull UpdateResult failed(final @NotNull UpdateStatus putStatus) {
        return new UpdateResult(putStatus,  null);
    }

    public static @NotNull UpdateResult failed(
            final @NotNull UpdateStatus putStatus, final @Nullable String errorMessage) {
        return new UpdateResult(putStatus, errorMessage);
    }

    public @NotNull UpdateStatus getUpdateStatus() {
        return updateStatus;
    }


    public @Nullable String getErrorMessage() {
        return errorMessage;
    }


    public enum UpdateStatus {
        SUCCESS(),
        ADAPTER_NOT_FOUND(),
        TAG_NOT_FOUND(),
        ALREADY_USED_BY_ANOTHER_ADAPTER(),
        INTERNAL_ERROR()
    }
}
