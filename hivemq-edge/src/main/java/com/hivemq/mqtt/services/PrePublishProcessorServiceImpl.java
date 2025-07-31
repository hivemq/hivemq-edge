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
package com.hivemq.mqtt.services;


import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.bootstrap.factories.HandlerResult;
import com.hivemq.bootstrap.factories.InternalPublishServiceHandlingProvider;
import com.hivemq.bootstrap.factories.PrePublishProcessorHandling;
import com.hivemq.bootstrap.factories.PrePublishProcessorHandlingProvider;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.publish.PUBLISH;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrePublishProcessorServiceImpl implements PrePublishProcessorService {

    private final @NotNull InternalPublishService internalPublishService;
    private final @NotNull PrePublishProcessorHandlingProvider processorHandlingProvider;
    private final @NotNull InternalPublishServiceHandlingProvider internalPublishServiceHandlingProvider;

    //FIXME: write tests

    @Inject
    public PrePublishProcessorServiceImpl(
            final @NotNull InternalPublishService internalPublishService,
            final @NotNull PrePublishProcessorHandlingProvider processorHandlingProvider,
            final @NotNull InternalPublishServiceHandlingProvider internalPublishServiceHandlingProvider) {
        this.internalPublishService = internalPublishService;
        this.processorHandlingProvider = processorHandlingProvider;
        this.internalPublishServiceHandlingProvider = internalPublishServiceHandlingProvider;
    }

    public @NotNull ListenableFuture<PublishingResult> publish(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @Nullable String sender) {

        final @NotNull List<PrePublishProcessorHandling> prePublishProcessorHandling = processorHandlingProvider.get();
        if (prePublishProcessorHandling.isEmpty()) {
            return internalPublishService.publish(publish, executorService, sender);
        }

        ListenableFuture<HandlerResult> future =
                prePublishProcessorHandling.get(0).apply(publish, sender, executorService);
        for (int i = 1; i < prePublishProcessorHandling.size(); i++) {
            final PrePublishProcessorHandling handler = prePublishProcessorHandling.get(i);
            future = Futures.transformAsync(future, result -> {
                //FIXME: check when modifiedPublish can be null
                if (result.isPreventPublish() || result.getModifiedPublish() == null) {
                    //skip further processing and return the previous result
                    return Futures.immediateFuture(result);
                } else {
                    return handler.apply(result.getModifiedPublish(), sender, executorService);
                }
            }, executorService);
        }

        return Futures.transformAsync(future, handlerResult -> {
            final PUBLISH modifiedPublish = handlerResult.getModifiedPublish();
            if (handlerResult.isPreventPublish() || modifiedPublish == null) {
                return Futures.immediateFuture(PublishingResult.failed(handlerResult.getReasonString(),
                        handlerResult.getAckReasonCode()));
            } else {
                // already merged the original and modified Publish.
                return internalPublishService.publish(modifiedPublish, executorService, sender);
            }
        }, executorService);
    }


    //TODO: remove this method once the DataHub module is migrated to the new PrePublishProcessorService
    @Override
    public @NotNull ListenableFuture<PublishingResult> applyDataHubAndPublish(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @NotNull String sender) {


        final ListenableFuture<HandlerResult> handlerFuture =
                internalPublishServiceHandlingProvider.get().apply(publish, sender);
        return Futures.transformAsync(handlerFuture, handlerResult -> {
            final PUBLISH modifiedPublish = handlerResult.getModifiedPublish();
            if (handlerResult.isPreventPublish() || modifiedPublish == null) {
                return Futures.immediateFuture(PublishingResult.failed(handlerResult.getReasonString(),
                        handlerResult.getAckReasonCode()));
            } else {
                // already merged the original and modified Publish.
                return publish(modifiedPublish, executorService, sender);
            }
        }, Executors.newSingleThreadExecutor());
    }
}
