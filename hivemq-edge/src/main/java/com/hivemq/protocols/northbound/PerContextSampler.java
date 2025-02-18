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
package com.hivemq.protocols.northbound;

import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.edge.modules.adapters.data.TagManager;
import com.hivemq.edge.modules.adapters.impl.polling.PollingInputImpl;
import com.hivemq.edge.modules.adapters.impl.polling.PollingOutputImpl;
import com.hivemq.protocols.AbstractSubscriptionSampler;
import com.hivemq.protocols.ProtocolAdapterWrapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PerContextSampler extends AbstractSubscriptionSampler {

    private static final Logger log = LoggerFactory.getLogger(PerContextSampler.class);


    private final @NotNull PollingProtocolAdapter pollingProtocolAdapter;
    private final @NotNull PollingContext pollingContext;
    private final @NotNull TagManager tagManager;

    public PerContextSampler(
            final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper,
            final @NotNull PollingContext pollingContext,
            final @NotNull EventService eventService,
            final @NotNull TagManager tagManager) {
        super(protocolAdapterWrapper, eventService);
        this.pollingProtocolAdapter = (PollingProtocolAdapter) protocolAdapterWrapper.getAdapter();
        this.pollingContext = pollingContext;
        this.tagManager = tagManager;
    }


    @Override
    public @NotNull CompletableFuture<?> execute() {
        if (Thread.currentThread().isInterrupted()) {
            return CompletableFuture.failedFuture(new InterruptedException());
        }
        final PollingOutputImpl pollingOutput = new PollingOutputImpl(new ProtocolAdapterDataSampleImpl());
        try {
            pollingProtocolAdapter.poll(new PollingInputImpl((List<PollingContext>) pollingContext), pollingOutput);
        } catch (final Throwable t) {
            pollingOutput.fail(t, null);
            throw t;
        }


        // TODO fixed timeout??
        final CompletableFuture<PollingOutputImpl.PollingResult> outputFuture =
                pollingOutput.getOutputFuture().orTimeout(10_000, TimeUnit.MILLISECONDS);
        return outputFuture.thenCompose(((pollingResult) -> {
            if (pollingResult == PollingOutputImpl.PollingResult.SUCCESS) {

                final ProtocolAdapterDataSample dataSample = pollingOutput.getDataSample();
                final Map<String, List<DataPoint>> dataPoints = dataSample.getDataPoints();

                for (final Map.Entry<String, List<DataPoint>> tagNameTpDataPoints : dataPoints.entrySet()) {
                    tagManager.feed(tagNameTpDataPoints.getKey(), tagNameTpDataPoints.getValue());
                }
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.completedFuture(null);
            }
        })).whenComplete((aVoid, throwable) -> {
            if (throwable != null) {
                if (pollingOutput.getErrorMessage() == null) {
                    log.warn("During the polling for adapter with id '{}' an exception occurred: ",
                            getAdapterId(),
                            throwable.getCause());
                    eventService.createAdapterEvent(protocolAdapter.getId(),
                                    protocolAdapter.getProtocolAdapterInformation().getProtocolId())
                            .withSeverity(Event.SEVERITY.WARN)
                            .withMessage("During the polling for adapter with id '" +
                                    protocolAdapter.getId() +
                                    "' an exception occurred: " +
                                    throwable.getClass().getSimpleName() +
                                    ":" +
                                    throwable.getMessage());
                } else {
                    log.warn(
                            "During the polling for adapter with id '{}' an exception occurred. Detailed error message: {}.",
                            getAdapterId(),
                            pollingOutput.getErrorMessage(),
                            throwable.getCause());
                    eventService.createAdapterEvent(protocolAdapter.getId(),
                                    protocolAdapter.getProtocolAdapterInformation().getProtocolId())
                            .withSeverity(Event.SEVERITY.WARN)
                            .withMessage("During the polling for adapter with id '" +
                                    protocolAdapter.getId() +
                                    "' an exception occurred. Detailed error message:" +
                                    pollingOutput.getErrorMessage());
                }
            }
        });
    }
}
