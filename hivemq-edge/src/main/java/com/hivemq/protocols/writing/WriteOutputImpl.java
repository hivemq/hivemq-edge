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
package com.hivemq.protocols.writing;

import com.hivemq.adapter.sdk.api.writing.WriteOutput;
import com.hivemq.exceptions.StackLessProtocolAdapterException;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WriteOutputImpl implements WriteOutput {

    private volatile @Nullable String message = null;
    private volatile @Nullable Throwable throwable = null;
    private final @NotNull CompletableFuture<Boolean> future =
            new CompletableFuture<Boolean>().orTimeout(30, TimeUnit.SECONDS);
    private boolean canBeRetried = true;

    public @Nullable Throwable getThrowable() {
        return throwable;
    }

    public @Nullable String getMessage() {
        return message;
    }

    public @NotNull CompletableFuture<Boolean> getFuture() {
        return future;
    }

    public boolean canBeRetried() {
        return canBeRetried;
    }

    @Override
    public void finish() {
        this.future.complete(true);
    }

    @Override
    public void fail(@NotNull final Throwable t, @Nullable final String errorMessage, boolean retry) {
        this.throwable = t;
        this.message = errorMessage;
        this.future.completeExceptionally(t);
        this.canBeRetried = retry;
    }

    @Override
    public void fail(@NotNull final String errorMessage, boolean retry) {
        this.message = errorMessage;
        this.future.completeExceptionally(new StackLessProtocolAdapterException(errorMessage));
        this.canBeRetried = retry;
    }
}
