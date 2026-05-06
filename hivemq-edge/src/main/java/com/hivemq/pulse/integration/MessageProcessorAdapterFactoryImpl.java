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
package com.hivemq.pulse.integration;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bootstrap.factories.HandlerResult;
import com.hivemq.bootstrap.factories.PrePublishProcessorHandling;
import com.hivemq.bootstrap.factories.PrePublishProcessorHandlingFactory;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.edge.integration.api.message.MessageProcessor;
import com.hivemq.mqtt.handler.connack.MqttConnacker;
import com.hivemq.mqtt.handler.disconnect.MqttServerDisconnector;
import com.hivemq.mqtt.message.dropping.IncomingPublishDropper;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Adapts a {@link MessageProcessor} (the integration-api contract) to {@link PrePublishProcessorHandlingFactory}
 * (the existing edge contract). The adapter always passes the publish through unchanged — Pulse only observes.
 */
public final class MessageProcessorAdapterFactoryImpl implements PrePublishProcessorHandlingFactory {

    private final @NotNull MessageProcessor processor;

    public MessageProcessorAdapterFactoryImpl(final @NotNull MessageProcessor processor) {
        this.processor = processor;
    }

    @Override
    public @NotNull PrePublishProcessorHandling build(
            final @NotNull MqttConnacker mqttConnacker,
            final @NotNull MqttServerDisconnector mqttServerDisconnector,
            final @NotNull IncomingPublishDropper incomingPublishDropper,
            final @NotNull ConfigurationService configurationService,
            final @NotNull MessageDroppedService messageDroppedService) {
        return new PassThroughHandling(processor);
    }

    private static final class PassThroughHandling implements PrePublishProcessorHandling {

        private final @NotNull MessageProcessor processor;

        private PassThroughHandling(final @NotNull MessageProcessor processor) {
            this.processor = processor;
        }

        @SuppressWarnings("FutureReturnValueIgnored")
        @Override
        public @NotNull ListenableFuture<HandlerResult> apply(
                final @NotNull PUBLISH originalPublish,
                final @Nullable String sender,
                final @NotNull ExecutorService executorService,
                final @NotNull MessageDroppedService messageDroppedService) {
            final SettableFuture<HandlerResult> resultFuture = SettableFuture.create();
            try {
                processor
                        .process(new IncomingMessageImpl(originalPublish), sender, executorService)
                        .whenCompleteAsync(
                                (ignored, throwable) -> {
                                    if (throwable != null) {
                                        resultFuture.setException(throwable);
                                    } else {
                                        resultFuture.set(new HandlerResult(false, false, originalPublish, null, null));
                                    }
                                },
                                executorService);
            } catch (final RejectedExecutionException e) {
                if (executorService.isShutdown()) {
                    resultFuture.setException(e);
                } else {
                    throw e;
                }
            }
            return resultFuture;
        }
    }
}
