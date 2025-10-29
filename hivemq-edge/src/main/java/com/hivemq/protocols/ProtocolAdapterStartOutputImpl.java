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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProtocolAdapterStartOutputImpl implements ProtocolAdapterStartOutput {

    private @Nullable volatile String message;
    private @Nullable volatile Throwable throwable;
    private final @NotNull CompletableFuture<Void> startFuture = new CompletableFuture<>();
    private final @NotNull AtomicBoolean completed = new AtomicBoolean(false);

    @Override
    public void startedSuccessfully() {
        if (completed.compareAndSet(false, true)) {
            startFuture.complete(null);
        }
    }

    @Override
    public void failStart(final @NotNull Throwable t, final @Nullable String errorMessage) {
        if (completed.compareAndSet(false, true)) {
            throwable = t;
            message = errorMessage;
            startFuture.completeExceptionally(t);
        }
    }

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @NotNull CompletableFuture<Void> getStartFuture() {
        return startFuture;
    }
}
