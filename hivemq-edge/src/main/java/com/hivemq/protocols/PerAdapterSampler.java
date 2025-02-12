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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.adapter.sdk.api.config.PollingContext;
import com.hivemq.adapter.sdk.api.data.DataPoint;
import com.hivemq.adapter.sdk.api.data.JsonPayloadCreator;
import com.hivemq.adapter.sdk.api.data.ProtocolAdapterDataSample;
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PerAdapterSampler extends AbstractSubscriptionSampler {

    private static final Logger log = LoggerFactory.getLogger(PerAdapterSampler.class);


    private final @NotNull PollingProtocolAdapter pollingProtocolAdapter;
    private final @NotNull List<PollingContext> pollingContext;

    public PerAdapterSampler(
            final @NotNull ProtocolAdapterWrapper protocolAdapterWrapper,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull List<PollingContext> pollingContexts,
            final @NotNull EventService eventService,
            final @NotNull JsonPayloadCreator jsonPayloadCreator) {
        super(protocolAdapterWrapper,
                objectMapper,
                adapterPublishService,
                eventService,
                jsonPayloadCreator);
        this.pollingProtocolAdapter = (PollingProtocolAdapter) protocolAdapterWrapper.getAdapter();
        this.pollingContext = pollingContexts;
    }


    @Override
    public @NotNull CompletableFuture<?> execute() {
        if (Thread.currentThread().isInterrupted()) {
            return CompletableFuture.failedFuture(new InterruptedException());
        }
        final PollingOutputImpl pollingOutput =
                new PollingOutputImpl(new ProtocolAdapterDataSampleImpl());
        try {
            pollingProtocolAdapter.poll(new PollingInputImpl(pollingContext), pollingOutput);
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
                    // TODO feed to the next layer

                }

                return CompletableFuture.completedFuture(null);
                return this.captureDataSample(pollingOutput.getDataSample(), pollingContext);
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
