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
import com.hivemq.adapter.sdk.api.events.EventService;
import com.hivemq.adapter.sdk.api.events.model.Event;
import com.hivemq.adapter.sdk.api.polling.PollingProtocolAdapter;
import com.hivemq.adapter.sdk.api.services.ProtocolAdapterPublishService;
import com.hivemq.edge.modules.adapters.data.ProtocolAdapterDataSampleImpl;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class PerSubscriptionSampler<T extends PollingContext> extends AbstractSubscriptionSampler {

    private static final Logger log = LoggerFactory.getLogger(PerSubscriptionSampler.class);


    private final @NotNull PollingProtocolAdapter<PollingContext> perSubscriptionProtocolAdapter;
    private final @NotNull T pollingContext;

    public PerSubscriptionSampler(
            final @NotNull ProtocolAdapterWrapper<PollingProtocolAdapter<PollingContext>> protocolAdapter,
            final @NotNull ObjectMapper objectMapper,
            final @NotNull ProtocolAdapterPublishService adapterPublishService,
            final @NotNull T pollingContext,
            final @NotNull EventService eventService,
            final @NotNull JsonPayloadDefaultCreator jsonPayloadDefaultCreator) {
        super(protocolAdapter,
                objectMapper,
                adapterPublishService,
                eventService,
                jsonPayloadDefaultCreator);
        this.perSubscriptionProtocolAdapter = protocolAdapter.getAdapter();
        this.pollingContext = pollingContext;
    }


    @Override
    public @NotNull CompletableFuture<?> execute() {
        if (Thread.currentThread().isInterrupted()) {
            return CompletableFuture.failedFuture(new InterruptedException());
        }
        final PollingOutputImpl pollingOutput =
                new PollingOutputImpl(new ProtocolAdapterDataSampleImpl(pollingContext));
        try {
            perSubscriptionProtocolAdapter.poll(new PollingInputImpl<>(pollingContext), pollingOutput);
        } catch (Throwable t) {
            pollingOutput.fail(t, null);
            throw t;
        }
        final CompletableFuture<PollingOutputImpl.PollingResult> outputFuture =
                pollingOutput.getOutputFuture().orTimeout(10_000, TimeUnit.MILLISECONDS);
        return outputFuture.thenCompose(((pollingResult) -> {
            if (pollingResult == PollingOutputImpl.PollingResult.SUCCESS) {
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
