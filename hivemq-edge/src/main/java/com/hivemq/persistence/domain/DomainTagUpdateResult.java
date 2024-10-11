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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

public class DomainTagUpdateResult {

    private final @NotNull DomainTagUpdateStatus dataPolicyUpdateStatus;
    private final @Nullable String errorMessage;

    public DomainTagUpdateResult(
            final @NotNull DomainTagUpdateStatus dataPolicyUpdateStatus, final @Nullable String errorMessage) {
        this.dataPolicyUpdateStatus = dataPolicyUpdateStatus;
        this.errorMessage = errorMessage;
    }

    public static @NotNull DomainTagUpdateResult success() {
        return new DomainTagUpdateResult(DomainTagUpdateStatus.SUCCESS, null);
    }

    public static @NotNull DomainTagUpdateResult failed(final @NotNull DomainTagUpdateStatus putStatus) {
        return new DomainTagUpdateResult(putStatus,  null);
    }

    public static @NotNull DomainTagUpdateResult failed(
            final @NotNull DomainTagUpdateStatus putStatus, final @Nullable String errorMessage) {
        return new DomainTagUpdateResult(putStatus, errorMessage);
    }

    public @NotNull DomainTagUpdateStatus getDomainTagUpdateStatus() {
        return dataPolicyUpdateStatus;
    }


    public @Nullable String getErrorMessage() {
        return errorMessage;
    }


    public enum DomainTagUpdateStatus {
        SUCCESS(),
        NOT_FOUND(),
        ALREADY_USED_BY_ANOTHER_ADAPTER(),
        INTERNAL_ERROR();
    }
}
