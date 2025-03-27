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

import java.util.Optional;

public record DomainTagAddResult(@NotNull DomainTagPutStatus domainTagPutStatus, @NotNull Optional<String> errorMessage, @NotNull Optional<String> adapterIdOfOwningAdapter) {

    public static @NotNull DomainTagAddResult success() {
        return new DomainTagAddResult(DomainTagPutStatus.SUCCESS, Optional.empty(),Optional.empty());
    }

    public static @NotNull DomainTagAddResult failed(
            final @NotNull DomainTagPutStatus putStatus,
            final @NotNull String adapterIdOfOwningAdapter) {
        return new DomainTagAddResult(putStatus, Optional.empty(), Optional.of(adapterIdOfOwningAdapter));
    }

    public static @NotNull DomainTagAddResult failed(
            final @NotNull DomainTagPutStatus putStatus,
            final @Nullable String adapterIdOfOwningAdapter,
            final @Nullable String errorMessage) {
        return new DomainTagAddResult(putStatus, Optional.of(errorMessage), Optional.of(adapterIdOfOwningAdapter));
    }

    public enum DomainTagPutStatus {
        SUCCESS(),
        ALREADY_EXISTS(),
        ADAPTER_MISSING()
    }


}
