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

import com.hivemq.adapter.sdk.api.model.ProtocolAdapterStopOutput;
import com.hivemq.configuration.service.InternalConfigurations;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProtocolAdapterStopOutputImpl implements ProtocolAdapterStopOutput {


    final @NotNull CompletableFuture<Void> outputFuture =
            new CompletableFuture<Void>().orTimeout(InternalConfigurations.ADAPTER_STOP_TIMEOUT_SECONDS.get(),
                    TimeUnit.SECONDS);
    private @Nullable String errorMessage = null;

    public ProtocolAdapterStopOutputImpl() {
    }

    public @NotNull CompletableFuture<Void> getOutputFuture() {
        return outputFuture;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public void stoppedSuccessfully() {
        outputFuture.complete(null);
    }

    @Override
    public void failStop(@NotNull final Throwable throwable, @Nullable final String errorMessage) {
        this.errorMessage = errorMessage;
        this.outputFuture.completeExceptionally(throwable);
    }
}
