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
package com.hivemq.protocols;

import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStartOutput;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProtocolAdapterStartOutputImpl implements ProtocolAdapterStartOutput {

    private @Nullable volatile String message = null;
    private @Nullable volatile Throwable throwable = null;
    private final @NotNull CompletableFuture<Boolean> startFuture = new CompletableFuture<Boolean>().orTimeout(
            InternalConfigurations.ADAPTER_START_TIMEOUT_SECONDS.get(),
            TimeUnit.SECONDS);

    @Override
    public void startedSuccessfully() {
        this.startFuture.complete(true);
    }

    @Override
    public void failStart(final @NotNull Throwable t, final @Nullable String errorMessage) {
        this.throwable = t;
        this.message = errorMessage;
        this.startFuture.complete(false);
    }

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @NotNull CompletableFuture<Boolean> getStartFuture() {
        return startFuture;
    }
}
