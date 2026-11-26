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
import com.hivemq.adapter.sdk.api.datapoint.DataPointListBuilder;
import com.hivemq.adapter.sdk.api.polling.PollingOutput;
import com.hivemq.adapter.sdk.api.polling.batch.BatchPollingOutput;
import com.hivemq.datapoint.DataPointListBuilderImpl;
import com.hivemq.edge.modules.adapters.data.DataPointImpl;
import com.hivemq.exceptions.StackLessProtocolAdapterException;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PollingOutputImpl implements PollingOutput, BatchPollingOutput {

    // The built-in adapters that end a poll they deliberately did not carry out with a cancellation, and wrap every
    // cancellation of a request they sent: Modbus and the PLC4X adapters (S7, ADS). From any other adapter a
    // cancellation is an ordinary polling error. The SDK has no outcome of its own for a skipped poll.
    private static final @NotNull Set<String> NOT_CARRIED_OUT_PROTOCOL_IDS = Set.of("modbus", "s7", "ads");

    public enum PollingResult {
        SUCCESS,
        NO_DATA
    }

    private final @NotNull ProtocolAdapterDataSample dataSample;
    final @NotNull CompletableFuture<PollingResult> outputFuture = new CompletableFuture<>();
    private @Nullable String errorMessage = null;
    private final String adapterId;

    public PollingOutputImpl(final @NotNull ProtocolAdapterDataSample dataSample, final @NotNull String adapterId) {
        this.dataSample = dataSample;
        this.adapterId = adapterId;
    }

    /**
     * Whether a poll's outcome says the poll was not carried out: a built-in Modbus or PLC4X adapter ended it with
     * {@code fail(CancellationException, ...)} because nothing could be read within the poll's time, for instance a
     * reconnect that completed too late to start a read. Such a poll is neither a polling error nor a successful poll,
     * so it leaves the polling-error count as it is. Those adapters wrap a request they sent that was cancelled, so it
     * still counts. Any other adapter (HTTP, a third party) keeps its cancellations as polling errors: they may be
     * requests it sent. The SDK has no outcome of its own for a skipped poll.
     */
    public static boolean isNotCarriedOut(final @Nullable String protocolId, final @NotNull Throwable outcome) {
        // Set.of rejects a null lookup; an adapter that reports no protocol id is not one of the built-in ones.
        if (protocolId == null || !NOT_CARRIED_OUT_PROTOCOL_IDS.contains(protocolId)) {
            return false;
        }
        Throwable cause = outcome;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause instanceof CancellationException;
    }

    /**
     * A polling adapter's datapoints, enriched with nothing.
     * <p>
     * The enricher is deliberately empty. {@code context} is the <em>protocol's</em> namespace, for what
     * describes a tag or a device, and no polling adapter has anything to say there yet -- so the aspect stays
     * absent rather than carrying a placeholder onto every message.
     */
    @Override
    public @NotNull DataPointListBuilder dataPointListPublisher() {
        return new DataPointListBuilderImpl(adapterId, toEnrich -> {}, dataPoints -> {
            dataPoints.forEach(dataSample::addDataPoint);
            finish();
        });
    }

    @Override
    public void addDataPoint(final @NotNull String tagName, final @NotNull Object tagValue) {
        dataSample.addDataPoint(new DataPointImpl(tagName, tagValue, adapterId));
    }

    @Override
    public void addDataPoint(final @NotNull DataPoint dataPoint) {
        dataSample.addDataPoint(dataPoint);
    }

    @Override
    public void finish() {
        if (dataSample.getDataPointList().isEmpty()) {
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
