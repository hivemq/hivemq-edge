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
package com.hivemq.edge.modules.adapters.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.configuration.service.ConfigurationService;
import com.hivemq.datagov.DataGovernanceContext;
import com.hivemq.datagov.DataGovernanceService;
import com.hivemq.datagov.impl.DataGovernanceContextImpl;
import com.hivemq.datagov.model.DataGovernanceData;
import com.hivemq.datagov.model.impl.DataGovernanceDataImpl;
import com.hivemq.edge.modules.adapters.ProtocolAdapterConstants;
import com.hivemq.edge.modules.api.adapters.ProtocolAdapter;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.client.parameter.ServerInformation;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.ProtocolAdapterPublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.ProtocolAdapterPublishInboundInterceptorProvider;
import com.hivemq.extension.sdk.api.interceptor.protocoladapter.parameter.ProtocolAdapterInboundProviderInput;
import com.hivemq.extensions.HiveMQExtension;
import com.hivemq.extensions.HiveMQExtensions;
import com.hivemq.extensions.executor.PluginOutPutAsyncer;
import com.hivemq.extensions.executor.PluginTaskExecutorService;
import com.hivemq.extensions.executor.task.PluginInOutTask;
import com.hivemq.extensions.executor.task.PluginInOutTaskContext;
import com.hivemq.extensions.handler.ExtensionParameterHolder;
import com.hivemq.extensions.interceptor.protocoladapter.parameter.ProtocolAdapterDynamicContextImpl;
import com.hivemq.extensions.interceptor.protocoladapter.parameter.ProtocolAdapterInboundProviderInputImpl;
import com.hivemq.extensions.interceptor.protocoladapter.parameter.ProtocolAdapterInformationImpl;
import com.hivemq.extensions.interceptor.protocoladapter.parameter.ProtocolAdapterPublishInboundInputImpl;
import com.hivemq.extensions.interceptor.protocoladapter.parameter.ProtocolAdapterPublishInboundOutputImpl;
import com.hivemq.extensions.packets.publish.ModifiablePublishPacketImpl;
import com.hivemq.extensions.packets.publish.PublishPacketImpl;
import com.hivemq.extensions.services.interceptor.Interceptors;
import com.hivemq.mqtt.handler.publish.PublishReturnCode;
import com.hivemq.mqtt.message.dropping.MessageDroppedService;
import com.hivemq.mqtt.message.publish.PUBLISH;
import com.hivemq.mqtt.message.publish.PUBLISHFactory;
import com.hivemq.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtocolAdapterInterceptorHandler {

    private static final Logger log = LoggerFactory.getLogger(ProtocolAdapterInterceptorHandler.class);

    private final @NotNull DataGovernanceService dataGovernanceService;
    private final @NotNull Interceptors interceptors;
    private final @NotNull ConfigurationService configurationService;
    private final @NotNull PluginOutPutAsyncer asyncer;
    private final @NotNull HiveMQExtensions hiveMQExtensions;
    private final @NotNull MessageDroppedService messageDroppedService;
    private final @NotNull PluginTaskExecutorService pluginTaskExecutorService;
    private final @NotNull ServerInformation serverInformation;

    @Inject
    public ProtocolAdapterInterceptorHandler(
            final @NotNull DataGovernanceService dataGovernanceService,
            final @NotNull Interceptors interceptors,
            final @NotNull ConfigurationService configurationService,
            final @NotNull PluginOutPutAsyncer asyncer,
            final @NotNull HiveMQExtensions hiveMQExtensions,
            final @NotNull MessageDroppedService messageDroppedService,
            final @NotNull PluginTaskExecutorService pluginTaskExecutorService,
            final @NotNull ServerInformation serverInformation) {
        this.dataGovernanceService = dataGovernanceService;
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
            final @NotNull ProtocolAdapter protocolAdapter,
            final @NotNull ImmutableMap<String, String> dynamicContext) {

        final ImmutableMap<String, ProtocolAdapterPublishInboundInterceptorProvider> providerMap =
                interceptors.protocolAdapterOutboundInterceptorProviders();
        if (providerMap.isEmpty()) {
            return processPublish(publish, protocolAdapter);
        }

        final SettableFuture<PublishReturnCode> resultFuture = SettableFuture.create();

        final PublishPacketImpl packet = new PublishPacketImpl(publish);
        final ProtocolAdapterInformationImpl bridgeInfo = new ProtocolAdapterInformationImpl(protocolAdapter.getId(),
                protocolAdapter.getProtocolAdapterInformation().getProtocolId());
        final ProtocolAdapterPublishInboundInputImpl input = new ProtocolAdapterPublishInboundInputImpl(bridgeInfo,
                packet,
                new ProtocolAdapterDynamicContextImpl(dynamicContext));
        final ExtensionParameterHolder<ProtocolAdapterPublishInboundInputImpl> inputHolder =
                new ExtensionParameterHolder<>(input);

        final ModifiablePublishPacketImpl modifiablePacket =
                new ModifiablePublishPacketImpl(packet, configurationService);
        final ProtocolAdapterPublishInboundOutputImpl output =
                new ProtocolAdapterPublishInboundOutputImpl(asyncer, modifiablePacket);
        final ExtensionParameterHolder<ProtocolAdapterPublishInboundOutputImpl> outputHolder =
                new ExtensionParameterHolder<>(output);

        final ProtocolAdapterInterceptorHandler.PublishInboundInterceptorContext context =
                new ProtocolAdapterInterceptorHandler.PublishInboundInterceptorContext(protocolAdapter,
                        providerMap.size(),
                        publish,
                        inputHolder,
                        outputHolder,
                        resultFuture,
                        executorService);

        for (final ProtocolAdapterPublishInboundInterceptorProvider interceptorProvider : providerMap.values()) {

            final HiveMQExtension extension =
                    hiveMQExtensions.getExtensionForClassloader(interceptorProvider.getClass().getClassLoader());
            if (extension == null) { // disabled extension would be null
                context.finishInterceptor();
                continue;
            }

            final ProtocolAdapterInboundProviderInput providerInput =
                    new ProtocolAdapterInboundProviderInputImpl(serverInformation, bridgeInfo);

            final ProtocolAdapterInterceptorHandler.ProtocolAdapterInboundInterceptorTask task =
                    new ProtocolAdapterInterceptorHandler.ProtocolAdapterInboundInterceptorTask(interceptorProvider,
                            providerInput,
                            extension.getId());
            pluginTaskExecutorService.handlePluginInOutTaskExecution(context, inputHolder, outputHolder, task);
        }

        return resultFuture;
    }


    private @NotNull ListenableFuture<PublishReturnCode> processPublish(
            final @NotNull PUBLISH publish, final @NotNull ProtocolAdapter protocolAdapter) {
        DataGovernanceData data =
                new DataGovernanceDataImpl.Builder().withClientId(protocolAdapter.getId()).withPublish(publish).build();
        DataGovernanceContext context = new ProtocolAdapterContext(data, protocolAdapter);
        return dataGovernanceService.applyAndPublish(context);
    }

    static class ProtocolAdapterContext extends DataGovernanceContextImpl {

        final @NotNull ProtocolAdapter adapter;

        public ProtocolAdapterContext(final @NotNull DataGovernanceData input, final @NotNull ProtocolAdapter adapter) {
            super(input, populateAdapterContextReplacements(adapter));
            this.adapter = adapter;
        }
    }

    protected static Map<String, String> populateAdapterContextReplacements(final @NotNull ProtocolAdapter protocolAdapter) {

        return ImmutableMap.<String, String>builder()
                .put(ProtocolAdapterConstants.ADAPTER_NAME_TOKEN,
                        protocolAdapter.getProtocolAdapterInformation().getDisplayName())
                .put(ProtocolAdapterConstants.ADAPTER_PROTOCOL_ID_TOKEN,
                        protocolAdapter.getProtocolAdapterInformation().getProtocolId())
                .put(ProtocolAdapterConstants.ADAPTER_INSTANCE_ID_TOKEN, protocolAdapter.getId())
                .build();
    }

    private class PublishInboundInterceptorContext
            extends PluginInOutTaskContext<ProtocolAdapterPublishInboundOutputImpl> implements Runnable {

        private final @NotNull ProtocolAdapter protocolAdapter;
        private final int interceptorCount;
        private final @NotNull AtomicInteger counter;
        private final @NotNull PUBLISH publish;
        private final @NotNull ExtensionParameterHolder<ProtocolAdapterPublishInboundInputImpl> inputHolder;
        private final @NotNull ExtensionParameterHolder<ProtocolAdapterPublishInboundOutputImpl> outputHolder;
        private final @NotNull SettableFuture<PublishReturnCode> resultFuture;
        private final @NotNull ExecutorService executorService;

        PublishInboundInterceptorContext(
                final @NotNull ProtocolAdapter protocolAdapter,
                final int interceptorCount,
                final @NotNull PUBLISH publish,
                final @NotNull ExtensionParameterHolder<ProtocolAdapterPublishInboundInputImpl> inputHolder,
                final @NotNull ExtensionParameterHolder<ProtocolAdapterPublishInboundOutputImpl> outputHolder,
                final @NotNull SettableFuture<PublishReturnCode> resultFuture,
                final @NotNull ExecutorService executorService) {

            super(protocolAdapter.getId());
            this.protocolAdapter = protocolAdapter;
            this.interceptorCount = interceptorCount;
            this.resultFuture = resultFuture;
            this.executorService = executorService;
            this.counter = new AtomicInteger(0);
            this.publish = publish;
            this.inputHolder = inputHolder;
            this.outputHolder = outputHolder;
        }

        @Override
        public void pluginPost(final @NotNull ProtocolAdapterPublishInboundOutputImpl output) {
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
            final ProtocolAdapterPublishInboundOutputImpl output = outputHolder.get();
            if (output.isPreventDelivery()) {
                dropMessage(output);
                resultFuture.set(PublishReturnCode.FAILED);
            } else {
                final PUBLISH finalPublish = PUBLISHFactory.merge(inputHolder.get().getPublishPacket(), publish);
                resultFuture.setFuture(processPublish(finalPublish, protocolAdapter));
            }
        }

        private void dropMessage(final @NotNull ProtocolAdapterPublishInboundOutputImpl output) {
            messageDroppedService.extensionPrevented(protocolAdapter.getId(),
                    publish.getTopic(),
                    publish.getQoS().getQosNumber());
        }
    }

    private static class ProtocolAdapterInboundInterceptorTask implements
            PluginInOutTask<ProtocolAdapterPublishInboundInputImpl, ProtocolAdapterPublishInboundOutputImpl> {

        private final @NotNull ProtocolAdapterPublishInboundInterceptorProvider interceptorProvider;
        private final @NotNull ProtocolAdapterInboundProviderInput providerInput;
        private final @NotNull String extensionId;

        private ProtocolAdapterInboundInterceptorTask(
                final @NotNull ProtocolAdapterPublishInboundInterceptorProvider interceptorProvider,
                final @NotNull ProtocolAdapterInboundProviderInput providerInput,
                final @NotNull String extensionId) {

            this.interceptorProvider = interceptorProvider;
            this.providerInput = providerInput;
            this.extensionId = extensionId;
        }

        @Override
        public @NotNull ProtocolAdapterPublishInboundOutputImpl apply(
                final @NotNull ProtocolAdapterPublishInboundInputImpl input,
                final @NotNull ProtocolAdapterPublishInboundOutputImpl output) {

            if (output.isPreventDelivery()) {
                // it's already prevented so no further interceptors must be called.
                return output;
            }
            try {
                final ProtocolAdapterPublishInboundInterceptor interceptor =
                        interceptorProvider.getProtocolAdapterPublishInboundInterceptor(providerInput);

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

}
