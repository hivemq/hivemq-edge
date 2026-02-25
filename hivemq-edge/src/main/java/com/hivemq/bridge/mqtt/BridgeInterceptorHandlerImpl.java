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
package com.hivemq.bridge.mqtt;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.api.mqtt.PublishReturnCode;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.bridge.BridgePublishOutboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.bridge.parameter.BridgeInboundProviderInput;
import com.hivemq.extension.sdk.api.interceptor.bridge.parameter.BridgeOutboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.handler.ExtensionParameterHolder;
import com.hivemq.extensions.interceptor.bridge.parameter.BridgeInboundProviderInputImpl;
import com.hivemq.extensions.interceptor.bridge.parameter.BridgeInformationImpl;
import com.hivemq.extensions.interceptor.bridge.parameter.BridgeOutboundProviderInputImpl;
import com.hivemq.extensions.interceptor.bridge.parameter.BridgePublishInboundInputImpl;
import com.hivemq.extensions.interceptor.bridge.parameter.BridgePublishInboundOutputImpl;
import com.hivemq.extensions.interceptor.bridge.parameter.BridgePublishOutboundInputImpl;
import com.hivemq.extensions.interceptor.bridge.parameter.BridgePublishOutboundOutputImpl;
import com.hivemq.extensions.packets.publish.ModifiableOutboundPublishImpl;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.mqtt.handler.publish.PublishingResult;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.PrePublishProcessorService;
import com.hivemq.util.Exceptions;
import jakarta.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BridgeInterceptorHandlerImpl implements BridgeInterceptorHandler {

    private static final @NotNull Logger log = LoggerFactory.getLogger(BridgeInterceptorHandlerImpl.class);

    private final @NotNull PrePublishProcessorService prePublishProcessorService;
    private final @NotNull Interceptors interceptors;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull ServerInformation serverInformation;

    @Inject
    public BridgeInterceptorHandlerImpl(
            final @NotNull PrePublishProcessorService prePublishProcessorService,
            final @NotNull Interceptors interceptors,
            final @NotNull ConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull ServerInformation serverInformation) {
        this.prePublishProcessorService = prePublishProcessorService;
        this.interceptors = interceptors;
        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.messageDroppedService = messageDroppedService;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.serverInformation = serverInformation;
    }

    @Override
    public @NotNull ListenableFuture<PublishReturnCode> interceptOrDelegateInbound(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @NotNull MqttBridge bridge) {

        final ImmutableMap<String, BridgePublishInboundInterceptorProvider> providerMap =
                interceptors.bridgeInboundInterceptorProviders();
        if (providerMap.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace(
                        "No inbound interceptors registered for bridge '{}', topic '{}', proceeding with direct publish",
                        bridge.getId(),
                        publish.getTopic());
            }
            return Futures.transform(
                    prePublishProcessorService.publish(publish, executorService, bridge.getClientId()),
                    (Function<? super PublishingResult, PublishReturnCode>) PublishingResult::getPublishReturnCode,
                    MoreExecutors.directExecutor());
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Processing {} inbound interceptor(s) for bridge '{}', topic '{}'",
                    providerMap.size(),
                    bridge.getId(),
                    publish.getTopic());
        }

        final SettableFuture<PublishReturnCode> resultFuture = SettableFuture.create();

        final PublishPacketImpl packet = new PublishPacketImpl(publish);
        final BridgeInformationImpl bridgeInfo = new BridgeInformationImpl(bridge.getId());
        final BridgePublishInboundInputImpl input = new BridgePublishInboundInputImpl(bridgeInfo, packet);
        final ExtensionParameterHolder<BridgePublishInboundInputImpl> inputHolder =
                new ExtensionParameterHolder<>(input);

        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);
        final BridgePublishInboundOutputImpl output = new BridgePublishInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<BridgePublishInboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final PublishInboundInterceptorContext context = new PublishInboundInterceptorContext(
                bridge, providerMap.size(), publish, inputHolder, outputHolder, resultFuture, executorService);

        for (final BridgePublishInboundInterceptorProvider interceptorProvider : providerMap.values()) {
            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    interceptorProvider.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                if (log.isTraceEnabled()) {
                    log.trace(
                            "Skipping disabled/unregistered inbound interceptor for bridge '{}', topic '{}'",
                            bridge.getId(),
                            publish.getTopic());
                }
                context.finishInterceptor();
                continue;
            }
            final BridgeInboundProviderInput providerInput =
                    new BridgeInboundProviderInputImpl(serverInformation, bridgeInfo);

            if (log.isTraceEnabled()) {
                log.trace(
                        "Executing inbound interceptor from extension '{}' for bridge '{}', topic '{}'",
                        extension.getId(),
                        bridge.getId(),
                        publish.getTopic());
            }

            final BridgeInboundInterceptorTask task =
                    new BridgeInboundInterceptorTask(interceptorProvider, providerInput, extension.getId());
            pluginTaskExecutorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }
        return resultFuture;
    }

    @Override
    public @NotNull ListenableFuture<InterceptorResult> interceptOrDelegateOutbound(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @NotNull MqttBridge bridge) {
        final ImmutableMap<String, BridgePublishOutboundInterceptorProvider> providerMap =
                interceptors.bridgeOutboundInterceptorProviders();
        if (providerMap.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace(
                        "No outbound interceptors registered for bridge '{}', topic '{}', proceeding with direct forward",
                        bridge.getId(),
                        publish.getTopic());
            }
            return Futures.immediateFuture(new InterceptorResult(InterceptorOutcome.SUCCESS, publish));
        }

        if (log.isDebugEnabled()) {
            log.debug(
                    "Processing {} outbound interceptor(s) for bridge '{}', topic '{}'",
                    providerMap.size(),
                    bridge.getId(),
                    publish.getTopic());
        }

        final SettableFuture<InterceptorResult> resultFuture = SettableFuture.create();

        final PublishPacketImpl packet = new PublishPacketImpl(publish);
        final BridgeInformationImpl bridgeInfo = new BridgeInformationImpl(bridge.getId());
        final BridgePublishOutboundInputImpl input = new BridgePublishOutboundInputImpl(bridgeInfo, packet);
        final ExtensionParameterHolder<BridgePublishOutboundInputImpl> inputHolder =
                new ExtensionParameterHolder<>(input);

        final ModifiableOutboundPublishImpl modifiablePacket =
                new ModifiableOutboundPublishImpl(packet, configurationService);
        final BridgePublishOutboundOutputImpl output = new BridgePublishOutboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<BridgePublishOutboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final PublishOutboundInterceptorContext context = new PublishOutboundInterceptorContext(
                bridge,
                providerMap.size(),
                publish,
                inputHolder,
                outputHolder,
                resultFuture,
                executorService,
                messageDroppedService);

        for (final BridgePublishOutboundInterceptorProvider interceptorProvider : providerMap.values()) {
            final HiveMQExtension extension = hiveMQExtensions.getExtensionForClassloader(
                    interceptorProvider.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                if (log.isTraceEnabled()) {
                    log.trace(
                            "Skipping disabled/unregistered outbound interceptor for bridge '{}', topic '{}'",
                            bridge.getId(),
                            publish.getTopic());
                }
                context.finishInterceptor();
                continue;
            }

            if (log.isTraceEnabled()) {
                log.trace(
                        "Executing outbound interceptor from extension '{}' for bridge '{}', topic '{}'",
                        extension.getId(),
                        bridge.getId(),
                        publish.getTopic());
            }

            pluginTaskExecutorService.handlePluginInOutTaskExecution(
                    context,
                    inputHolder,
                    outputHolder,
                    new BridgeOutboundInterceptorTask(
                            interceptorProvider,
                            new BridgeOutboundProviderInputImpl(serverInformation, bridgeInfo),
                            extension.getId()));
        }

        return resultFuture;
    }

    private record BridgeInboundInterceptorTask(
            @NotNull BridgePublishInboundInterceptorProvider interceptorProvider,
            @NotNull BridgeInboundProviderInput providerInput,
            @NotNull String extensionId)
            implements PluginInOutTask<BridgePublishInboundInputImpl, BridgePublishInboundOutputImpl> {

        @Override
        public @NotNull BridgePublishInboundOutputImpl apply(
                final @NotNull BridgePublishInboundInputImpl input,
                final @NotNull BridgePublishInboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                if (log.isTraceEnabled()) {
                    log.trace(
                            "Skipping inbound interceptor execution from extension '{}' as delivery is already prevented",
                            extensionId);
                }
                return output;
            }

            final long startTime = log.isDebugEnabled() ? System.nanoTime() : 0;
            try {
                final BridgePublishInboundInterceptor interceptor =
                        interceptorProvider.getBridgePublishInboundInterceptor(providerInput);
                if (interceptor != null) {
                    interceptor.onInboundPublish(input, output);

                    if (log.isDebugEnabled()) {
                        final long durationMicros = (System.nanoTime() - startTime) / 1000;
                        log.debug(
                                "Inbound interceptor from extension '{}' completed in {} μs",
                                extensionId,
                                durationMicros);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Inbound interceptor provider from extension '{}' returned null interceptor",
                                extensionId);
                    }
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on MQTT bridge inbound PUBLISH interception "
                                + "for topic '{}'. Extensions are responsible for their own exception handling.",
                        extensionId,
                        input.getPublishPacket().getTopic(),
                        e);
                output.forciblyPreventPublishDelivery();
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptorProvider.getClass().getClassLoader();
        }
    }

    private record BridgeOutboundInterceptorTask(
            @NotNull BridgePublishOutboundInterceptorProvider interceptorProvider,
            @NotNull BridgeOutboundProviderInput providerInput,
            @NotNull String extensionId)
            implements PluginInOutTask<BridgePublishOutboundInputImpl, BridgePublishOutboundOutputImpl> {

        @Override
        public @NotNull BridgePublishOutboundOutputImpl apply(
                final @NotNull BridgePublishOutboundInputImpl input,
                final @NotNull BridgePublishOutboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                if (log.isTraceEnabled()) {
                    log.trace(
                            "Skipping outbound interceptor execution from extension '{}' as delivery is already prevented",
                            extensionId);
                }
                return output;
            }

            final long startTime = log.isDebugEnabled() ? System.nanoTime() : 0;
            try {
                final BridgePublishOutboundInterceptor interceptor =
                        interceptorProvider.getBridgePublishOutboundInterceptor(providerInput);

                if (interceptor != null) {
                    interceptor.onOutboundPublish(input, output);

                    if (log.isDebugEnabled()) {
                        final long durationMicros = (System.nanoTime() - startTime) / 1000;
                        log.debug(
                                "Outbound interceptor from extension '{}' completed in {} μs",
                                extensionId,
                                durationMicros);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(
                                "Outbound interceptor provider from extension '{}' returned null interceptor",
                                extensionId);
                    }
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on MQTT bridge outbound PUBLISH interception "
                                + "for topic '{}'. Extensions are responsible for their own exception handling.",
                        extensionId,
                        input.getPublishPacket().getTopic(),
                        e);
                output.forciblyPreventPublishDelivery();
                Exceptions.rethrowError(e);
            }
            return output;
        }

        @Override
        public @NotNull ClassLoader getPluginClassLoader() {
            return interceptorProvider.getClass().getClassLoader();
        }
    }

    private static class PublishOutboundInterceptorContext
            extends PluginInOutTaskContext<BridgePublishOutboundOutputImpl> implements Runnable {

        private final @NotNull MqttBridge bridge;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull PUBLISH publish;
        private final @NotNull ExtensionParameterHolder<BridgePublishOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<BridgePublishOutboundOutputImpl> outputHolder;
        private final @NotNull SettableFuture<InterceptorResult> resultFuture;
        private final @NotNull ExecutorService executorService;
        private final @NotNull MessageDroppedService messageDroppedService;

        PublishOutboundInterceptorContext(
                final @NotNull MqttBridge bridge,
                final int interceptorCount,
                final @NotNull PUBLISH publish,
                final @NotNull ExtensionParameterHolder<BridgePublishOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<BridgePublishOutboundOutputImpl> outputHolder,
                final @NotNull SettableFuture<InterceptorResult> resultFuture,
                final @NotNull ExecutorService executorService,
                final @NotNull MessageDroppedService messageDroppedService) {
            super(bridge.getClientId());
            this.bridge = bridge;
            this.interceptorCount = interceptorCount;
            this.resultFuture = resultFuture;
            this.executorService = executorService;
            this.messageDroppedService = messageDroppedService;
            this.counter = new AtomicInteger(0);
            this.publish = publish;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull BridgePublishOutboundOutputImpl output) {
            if (output.isPreventDelivery()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.forciblyPreventPublishDelivery();
                finishInterceptor();
            } else {
                if (output.getPublishPacket().isModified()) {
                    inputHolder.set(inputHolder.get().update(output));
                }
                if (!finishInterceptor()) {
                    outputHolder.set(output.update(inputHolder.get()));
                }
            }
        }

        public boolean finishInterceptor() {
            if (counter.incrementAndGet() == interceptorCount) {
                executorService.execute(this);
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            final BridgePublishOutboundOutputImpl output = outputHolder.get();
            if (output.isPreventDelivery()) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Outbound message delivery prevented by interceptor for bridge '{}', topic '{}'",
                            bridge.getId(),
                            publish.getTopic());
                }
                messageDroppedService.extensionPrevented(
                        bridge.getClientId(),
                        publish.getTopic(),
                        publish.getQoS().getQosNumber());
                resultFuture.set(new InterceptorResult(InterceptorOutcome.DROP, null));
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(
                            "All {} outbound interceptor(s) completed successfully for bridge '{}', topic '{}'",
                            interceptorCount,
                            bridge.getId(),
                            publish.getTopic());
                }
                resultFuture.set(new InterceptorResult(
                        InterceptorOutcome.SUCCESS,
                        PUBLISHFactory.merge(inputHolder.get().getPublishPacket(), publish)));
            }
        }
    }

    private class PublishInboundInterceptorContext extends PluginInOutTaskContext<BridgePublishInboundOutputImpl>
            implements Runnable {

        private final @NotNull MqttBridge bridge;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull PUBLISH publish;
        private final @NotNull ExtensionParameterHolder<BridgePublishInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<BridgePublishInboundOutputImpl> outputHolder;
        private final @NotNull SettableFuture<PublishReturnCode> resultFuture;
        private final @NotNull ExecutorService executorService;

        PublishInboundInterceptorContext(
                final @NotNull MqttBridge bridge,
                final int interceptorCount,
                final @NotNull PUBLISH publish,
                final @NotNull ExtensionParameterHolder<BridgePublishInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<BridgePublishInboundOutputImpl> outputHolder,
                final @NotNull SettableFuture<PublishReturnCode> resultFuture,
                final @NotNull ExecutorService executorService) {

            super(bridge.getClientId());
            this.bridge = bridge;
            this.interceptorCount = interceptorCount;
            this.resultFuture = resultFuture;
            this.executorService = executorService;
            this.counter = new AtomicInteger(0);
            this.publish = publish;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull BridgePublishInboundOutputImpl output) {
            if (output.isPreventDelivery()) {
                finishInterceptor();
            } else if (output.isTimedOut() && (output.getTimeoutFallback() == TimeoutFallback.FAILURE)) {
                output.forciblyPreventPublishDelivery();
                finishInterceptor();
            } else {
                if (output.getPublishPacket().isModified()) {
                    inputHolder.set(inputHolder.get().update(output));
                }
                if (!finishInterceptor()) {
                    outputHolder.set(output.update(inputHolder.get()));
                }
            }
        }

        public boolean finishInterceptor() {
            if (counter.incrementAndGet() == interceptorCount) {
                executorService.execute(this);
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            final BridgePublishInboundOutputImpl output = outputHolder.get();
            if (output.isPreventDelivery()) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            "Inbound message delivery prevented by interceptor for bridge '{}', topic '{}'",
                            bridge.getId(),
                            publish.getTopic());
                }
                dropMessage();
                resultFuture.set(PublishReturnCode.FAILED);
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(
                            "All {} inbound interceptor(s) completed successfully for bridge '{}', topic '{}', proceeding with publish",
                            interceptorCount,
                            bridge.getId(),
                            publish.getTopic());
                }
                final PUBLISH finalPublish =
                        PUBLISHFactory.merge(inputHolder.get().getPublishPacket(), publish);
                resultFuture.setFuture(Futures.transform(
                        prePublishProcessorService.publish(finalPublish, executorService, bridge.getClientId()),
                        (Function<? super PublishingResult, PublishReturnCode>) PublishingResult::getPublishReturnCode,
                        MoreExecutors.directExecutor()));
            }
        }

        private void dropMessage() {
            messageDroppedService.extensionPrevented(
                    bridge.getClientId(), publish.getTopic(), publish.getQoS().getQosNumber());
        }
    }
}
