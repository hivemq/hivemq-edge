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
package com.hivemq.persistence.domain;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DomainTagDeleteResult {

    private final @NotNull DomainTagDeleteStatus dataPolicyUpdateStatus;
    private final @Nullable String errorMessage;

    public DomainTagDeleteResult(
            final @NotNull DomainTagDeleteStatus dataPolicyUpdateStatus, final @Nullable String errorMessage) {
        this.dataPolicyUpdateStatus = dataPolicyUpdateStatus;
        this.errorMessage = errorMessage;
    }

    public static @NotNull DomainTagDeleteResult success() {
        return new DomainTagDeleteResult(DomainTagDeleteStatus.SUCCESS, null);
    }

    public static @NotNull DomainTagDeleteResult failed(final @NotNull DomainTagDeleteStatus putStatus) {
        return new DomainTagDeleteResult(putStatus,  null);
    }

    public static @NotNull DomainTagDeleteResult failed(
            final @NotNull DomainTagDeleteStatus deleteResult, final @Nullable String errorMessage) {
        return new DomainTagDeleteResult(deleteResult, errorMessage);
    }

    public @NotNull DomainTagDeleteStatus getDomainTagDeleteStatus() {
        return dataPolicyUpdateStatus;
    }


    public @Nullable String getErrorMessage() {
        return errorMessage;
    }


    public enum DomainTagDeleteStatus {
        SUCCESS(),
        NOT_FOUND()
    }
}
