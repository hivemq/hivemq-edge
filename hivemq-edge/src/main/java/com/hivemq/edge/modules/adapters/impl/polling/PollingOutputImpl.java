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
package com.hivemq.edge.modules.adapters.impl.polling;

import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.exceptions.StackLessProtocolAdapterException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class PollingOutputImpl implements PollingOutput, BatchPollingOutput {

    public enum PollingResult {
        SUCCESS,
        NO_DATA
    }

    private final @NotNull ProtocolAdapterDataSample dataSample;
    final @NotNull CompletableFuture<PollingResult> outputFuture = new CompletableFuture<>();
    private @Nullable String errorMessage = null;

    public PollingOutputImpl(final @NotNull ProtocolAdapterDataSample dataSample) {
        this.dataSample = dataSample;
    }


    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        dataSample.addDataPoint(new DataPointImpl(tagName, tagValue));
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        dataSample.addDataPoint(dataPoint);
    }

    @Override
    public void finish() {
        if (dataSample.getDataPoints().isEmpty()) {
            outputFuture.complete(PollingResult.NO_DATA);
        } else {
            outputFuture.complete(PollingResult.SUCCESS);
        }
    }

    @Override
    public void fail(final @NotNull Throwable t, final @Nullable String errorMessage) {
        this.errorMessage = errorMessage;
        outputFuture.completeExceptionally(t);
    }

    @Override
    public void fail(final @NotNull String errorMessage) {
        this.errorMessage = errorMessage;
        outputFuture.completeExceptionally(new StackLessProtocolAdapterException(errorMessage));
    }

    public @NotNull CompletableFuture<PollingResult> getOutputFuture() {
        return outputFuture;
    }

    public @NotNull ProtocolAdapterDataSample getDataSample() {
        return dataSample;
    }

    public @Nullable String getErrorMessage() {
        return errorMessage;
    }
}
