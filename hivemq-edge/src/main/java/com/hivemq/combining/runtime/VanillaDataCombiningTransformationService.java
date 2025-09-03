/*
 *  Copyright 2019-present HiveMQ GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.hivemq.combining.runtime;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.hivemq.combining.mapping.DataCombiningTransformationService;
import com.hivemq.combining.model.DataCombining;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class VanillaDataCombiningTransformationService implements DataCombiningTransformationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VanillaDataCombiningTransformationService.class);
    private final @NotNull PrePublishProcessorService prePublishProcessorService;

    public VanillaDataCombiningTransformationService(
            final @NotNull PrePublishProcessorService prePublishProcessorService) {
        this.prePublishProcessorService = prePublishProcessorService;
    }

    @Override
    public @NotNull CompletableFuture<Void> applyMappings(
            final @NotNull PUBLISH mergedPublish,
            final @NotNull DataCombining dataCombining) {
        LOGGER.debug("Applying data combining {} to publish {}", dataCombining, mergedPublish.getTopic());
        final PUBLISHFactory.Mqtt5Builder publish = new PUBLISHFactory.Mqtt5Builder().fromPublish(mergedPublish)
                .withTopic(dataCombining.destination().topic())
                .withPayload("{ \"a\": 1, \"b\": 2 }".getBytes(StandardCharsets.UTF_8));
        final ListenableFuture<PublishingResult> listenableFuture = prePublishProcessorService.publish(publish.build(),
                MoreExecutors.newDirectExecutorService(),
                "vanilla-data-combining-" + dataCombining.id());
        final CompletableFuture<Void> returnFuture = new CompletableFuture<>();
        listenableFuture.addListener(() -> {
            try {
                listenableFuture.get();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                returnFuture.completeExceptionally(e);
            } catch (final ExecutionException e) {
                returnFuture.completeExceptionally(e);
            }
        }, MoreExecutors.directExecutor());
        return returnFuture;
    }

    @Override
    public void removeScriptForDataCombining(final @NotNull DataCombining combining) {
        LOGGER.debug("Removing data combining {}", combining);
    }

    @Override
    public void addScriptForDataCombining(final @NotNull DataCombining combining) {
        LOGGER.debug("Adding data combining {}", combining);
    }
}
