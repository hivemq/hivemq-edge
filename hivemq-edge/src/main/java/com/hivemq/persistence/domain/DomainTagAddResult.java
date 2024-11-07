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

import java.util.Optional;

public class DomainTagAddResult {

    private final @NotNull DomainTagPutStatus dataPolicyPutStatus;
    private final @Nullable Optional<String> errorMessage;
    private final @NotNull Optional<String> adapterIdOfOwningAdapter;

    public DomainTagAddResult(
            final @NotNull DomainTagPutStatus dataPolicyPutStatus, final @Nullable String adapterIdOfOwningAdapter, final @Nullable String errorMessage) {
        this.dataPolicyPutStatus = dataPolicyPutStatus;
        this.errorMessage = Optional.ofNullable(errorMessage);
        this.adapterIdOfOwningAdapter = Optional.ofNullable(adapterIdOfOwningAdapter);
    }

    public static @NotNull DomainTagAddResult success() {
        return new DomainTagAddResult(DomainTagPutStatus.SUCCESS, null,null);
    }

    public static @NotNull DomainTagAddResult failed(
            final @NotNull DomainTagPutStatus putStatus,
            final @NotNull String adapterIdOfOwningAdapter) {
        return new DomainTagAddResult(putStatus, adapterIdOfOwningAdapter, null);
    }

    public static @NotNull DomainTagAddResult failed(
            final @NotNull DomainTagPutStatus putStatus,
            final @Nullable String adapterIdOfOwningAdapter,
            final @Nullable String errorMessage) {
        return new DomainTagAddResult(putStatus, adapterIdOfOwningAdapter, errorMessage);
    }

    public @NotNull DomainTagPutStatus getDomainTagPutStatus() {
        return dataPolicyPutStatus;
    }

    public @Nullable Optional<String> getErrorMessage() {
        return errorMessage;
    }

    public @NotNull Optional<String> getAdapterIdOfOwningAdapter() {
        return adapterIdOfOwningAdapter;
    }

    public enum DomainTagPutStatus {
        SUCCESS(),
        ALREADY_EXISTS(),
        ADAPTER_MISSING()
    }


}
