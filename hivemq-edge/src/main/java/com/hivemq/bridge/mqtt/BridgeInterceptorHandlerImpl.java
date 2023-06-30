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

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.bridge.config.MqttBridge;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.extension.sdk.api.annotations.NotNull;
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
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.mqtt.services.InternalPublishService;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class BridgeInterceptorHandlerImpl implements BridgeInterceptorHandler {

    private static final Logger log = LoggerFactory.getLogger(BridgeInterceptorHandlerImpl.class);

    private final @NotNull InternalPublishService internalPublishService;
    private final @NotNull Interceptors interceptors;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull ServerInformation serverInformation;

    @Inject
    public BridgeInterceptorHandlerImpl(
            final @NotNull InternalPublishService internalPublishService,
            final @NotNull Interceptors interceptors,
            final @NotNull ConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull ServerInformation serverInformation) {
        this.internalPublishService = internalPublishService;
        this.interceptors = interceptors;
        this.configurationService = configurationService;
        this.asyncer = asyncer;
        this.hiveMQExtensions = hiveMQExtensions;
        this.messageDroppedService = messageDroppedService;
        this.pluginTaskExecutorService = pluginTaskExecutorService;
        this.serverInformation = serverInformation;
    }

    public @NotNull ListenableFuture<PublishReturnCode> interceptOrDelegateInbound(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @NotNull MqttBridge bridge) {

        final ImmutableMap<String, BridgePublishInboundInterceptorProvider> providerMap =
                interceptors.bridgeInboundInterceptorProviders();
        if (providerMap.isEmpty()) {
            return internalPublishService.publish(publish, executorService, bridge.getClientId());
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

        final PublishInboundInterceptorContext context = new PublishInboundInterceptorContext(bridge,
                providerMap.size(),
                publish,
                inputHolder,
                outputHolder,
                resultFuture,
                executorService);

        for (final BridgePublishInboundInterceptorProvider interceptorProvider : providerMap.values()) {

            final HiveMQExtension extension =
                    hiveMQExtensions.getExtensionForClassloader(interceptorProvider.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final BridgeInboundProviderInput providerInput =
                    new BridgeInboundProviderInputImpl(serverInformation, bridgeInfo);

            final BridgeInboundInterceptorTask task =
                    new BridgeInboundInterceptorTask(interceptorProvider, providerInput, extension.getId());
            pluginTaskExecutorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }

        return resultFuture;
    }

    public @NotNull ListenableFuture<InterceptorResult> interceptOrDelegateOutbound(
            final @NotNull PUBLISH publish,
            final @NotNull ExecutorService executorService,
            final @NotNull MqttBridge bridge) {
        final ImmutableMap<String, BridgePublishOutboundInterceptorProvider> providerMap =
                interceptors.bridgeOutboundInterceptorProviders();
        if (providerMap.isEmpty()) {
            return Futures.immediateFuture(new InterceptorResult(InterceptorOutcome.SUCCESS, publish));
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

        final PublishOutboundInterceptorContext context = new PublishOutboundInterceptorContext(bridge,
                providerMap.size(),
                publish,
                inputHolder,
                outputHolder,
                resultFuture,
                executorService);

        for (final BridgePublishOutboundInterceptorProvider interceptorProvider : providerMap.values()) {

            final HiveMQExtension extension =
                    hiveMQExtensions.getExtensionForClassloader(interceptorProvider.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final BridgeOutboundProviderInput providerInput =
                    new BridgeOutboundProviderInputImpl(serverInformation, bridgeInfo);

            final BridgeOutboundInterceptorTask task =
                    new BridgeOutboundInterceptorTask(interceptorProvider, providerInput, extension.getId());
            pluginTaskExecutorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }

        return resultFuture;
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
                dropMessage(output);
                resultFuture.set(PublishReturnCode.FAILED);
            } else {
                final PUBLISH finalPublish = PUBLISHFactory.merge(inputHolder.get().getPublishPacket(), publish);
                resultFuture.setFuture(internalPublishService.publish(finalPublish,
                        executorService,
                        bridge.getClientId()));
            }
        }

        private void dropMessage(final @NotNull BridgePublishInboundOutputImpl output) {
            messageDroppedService.extensionPrevented(bridge.getClientId(),
                    publish.getTopic(),
                    publish.getQoS().getQosNumber());
        }
    }

    private class PublishOutboundInterceptorContext extends PluginInOutTaskContext<BridgePublishOutboundOutputImpl>
            implements Runnable {

        private final @NotNull MqttBridge bridge;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull PUBLISH publish;
        private final @NotNull ExtensionParameterHolder<BridgePublishOutboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<BridgePublishOutboundOutputImpl> outputHolder;
        private final @NotNull SettableFuture<InterceptorResult> resultFuture;
        private final @NotNull ExecutorService executorService;

        PublishOutboundInterceptorContext(
                final @NotNull MqttBridge bridge,
                final int interceptorCount,
                final @NotNull PUBLISH publish,
                final @NotNull ExtensionParameterHolder<BridgePublishOutboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<BridgePublishOutboundOutputImpl> outputHolder,
                final @NotNull SettableFuture<InterceptorResult> resultFuture,
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
                dropMessage(output);
                resultFuture.set(new InterceptorResult(InterceptorOutcome.DROP, null));
            } else {
                final PUBLISH finalPublish = PUBLISHFactory.merge(inputHolder.get().getPublishPacket(), publish);
                resultFuture.set(new InterceptorResult(InterceptorOutcome.SUCCESS, finalPublish));
            }
        }

        private void dropMessage(final @NotNull BridgePublishOutboundOutputImpl output) {
            messageDroppedService.extensionPrevented(bridge.getClientId(),
                    publish.getTopic(),
                    publish.getQoS().getQosNumber());
        }
    }

    private static class BridgeInboundInterceptorTask
            implements PluginInOutTask<BridgePublishInboundInputImpl, BridgePublishInboundOutputImpl> {

        private final @NotNull BridgePublishInboundInterceptorProvider interceptorProvider;
        private final @NotNull BridgeInboundProviderInput providerInput;
        private final @NotNull String extensionId;

        private BridgeInboundInterceptorTask(
                final @NotNull BridgePublishInboundInterceptorProvider interceptorProvider,
                final @NotNull BridgeInboundProviderInput providerInput,
                final @NotNull String extensionId) {

            this.interceptorProvider = interceptorProvider;
            this.providerInput = providerInput;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull BridgePublishInboundOutputImpl apply(
                final @NotNull BridgePublishInboundInputImpl input,
                final @NotNull BridgePublishInboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                final BridgePublishInboundInterceptor interceptor =
                        interceptorProvider.getBridgePublishInboundInterceptor(providerInput);

                if (interceptor != null) {
                    interceptor.onInboundPublish(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on MQTT bridge inbound PUBLISH interception. " +
                                "Extensions are responsible for their own exception handling.",
                        extensionId,
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


    private static class BridgeOutboundInterceptorTask
            implements PluginInOutTask<BridgePublishOutboundInputImpl, BridgePublishOutboundOutputImpl> {

        private final @NotNull BridgePublishOutboundInterceptorProvider interceptorProvider;
        private final @NotNull BridgeOutboundProviderInput providerInput;
        private final @NotNull String extensionId;

        private BridgeOutboundInterceptorTask(
                final @NotNull BridgePublishOutboundInterceptorProvider interceptorProvider,
                final @NotNull BridgeOutboundProviderInput providerInput,
                final @NotNull String extensionId) {

            this.interceptorProvider = interceptorProvider;
            this.providerInput = providerInput;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull BridgePublishOutboundOutputImpl apply(
                final @NotNull BridgePublishOutboundInputImpl input,
                final @NotNull BridgePublishOutboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                final BridgePublishOutboundInterceptor interceptor =
                        interceptorProvider.getBridgePublishOutboundInterceptor(providerInput);

                if (interceptor != null) {
                    interceptor.onOutboundPublish(input, output);
                }
            } catch (final Throwable e) {
                log.warn(
                        "Uncaught exception was thrown from extension with id \"{}\" on MQTT bridge outbound PUBLISH interception. " +
                                "Extensions are responsible for their own exception handling.",
                        extensionId,
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

}
