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

import java.util.Optional;

public record AddResult(PutStatus putStatus, Optional<String> errorMessage) {

    public static @NotNull AddResult success() {
        return new AddResult(PutStatus.SUCCESS, null);
    }

    public static @NotNull AddResult failed(
            final @NotNull PutStatus putStatus) {
        return new AddResult(putStatus, null);
    }

    public static @NotNull AddResult failed(
            final @NotNull PutStatus putStatus, final @Nullable String errorMessage) {
        return new AddResult(putStatus, Optional.ofNullable(errorMessage));
    }

    public @NotNull PutStatus getPutStatus() {
        return putStatus;
    }

    public @Nullable Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public enum PutStatus {
        SUCCESS(),
        ALREADY_EXISTS(),
        ADAPTER_MISSING()
    }


}
