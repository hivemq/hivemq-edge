/*
 * Copyright 2023-present HiveMQ GmbH
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
package com.hivemq.edge.adapters.postgresql;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TestPollingOutput implements PollingOutput {

    private final @NotNull Map<String, Object> dataPoints = new HashMap<>();

    final @NotNull CompletableFuture<Boolean> outputFuture = new CompletableFuture<>();
    private @Nullable String errorMessage = null;

    public TestPollingOutput() {
    }

    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        dataPoints.put(tagName, tagValue);
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        // NOOP
    }

    @Override
    public void finish() {
        outputFuture.complete(true);
    }

    @Override
    public void fail(final @NotNull Throwable t, @Nullable final String errorMessage) {
        this.errorMessage = errorMessage;
        outputFuture.completeExceptionally(t);
    }

    @Override
    public void fail(@NotNull final String errorMessage) {
        this.errorMessage = errorMessage;
        outputFuture.completeExceptionally(new RuntimeException());
    }

    public @NotNull CompletableFuture<Boolean> getOutputFuture() {
        return outputFuture;
    }

    public @NotNull Map<String, Object> getDataPoints() {
        return dataPoints;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }
}
